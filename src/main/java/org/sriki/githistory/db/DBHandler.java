package org.sriki.githistory.db;

import org.apache.commons.io.IOUtils;
import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sriki.githistory.model.Commit;

import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.DateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

//-Dloader.dbUrl=jdbc:postgresql://localhost:5432/git_history?user=postgres
@Provider
public final class DBHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBHandler.class);
    public static final String DB_USER = "sa";
    public static final String DEFAULT_DB_URL = "jdbc:h2:~/githistoryloader;LOG=0;CACHE_SIZE=65536;LOCK_MODE=0;UNDO_LOG=0;MVCC=TRUE";
    public static final String DBSCHEMA_SQL = "dbschema.sql";
    private final JdbcDataSource basicDataSource;
    private static final DBHandler ME = new DBHandler();
    private List<Commit> commitList = new ArrayList<>();
    private String gitBranch;
    private Date from;
    private Date to;

    private DBHandler() {
        try {
            basicDataSource = initDriver();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public static final DBHandler getInstance() {
        return ME;
    }

    private JdbcDataSource initDriver() throws Exception {
        JdbcDataSource basicDataSource = new JdbcDataSource();
        basicDataSource.setUrl(DEFAULT_DB_URL);
        basicDataSource.setUser(DB_USER);
        basicDataSource.setPassword(DB_USER);
        return basicDataSource;
    }

    private Connection getConnection() throws SQLException {
        return basicDataSource.getConnection();
    }

    public void initDB() throws SQLException, IOException {
        LOGGER.info("Creating Database....");
        try (Connection connection = getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String sqls = loadDbSchema();
                for (String sql : sqls.split(";")) {
                    try {
                        statement.execute(sql);
                    } catch (SQLException e) {
                        LOGGER.error("Failed to execute sql: " + sql, e);
                    }
                }
            }
        }
        LOGGER.info("Database Created And Ready For Use....");
    }

    private String loadDbSchema() throws IOException {
        InputStream stream = DBHandler.class.getResourceAsStream("/" + DBSCHEMA_SQL);
        if (stream == null) {
            throw new RuntimeException("DB Schema File: " + DBSCHEMA_SQL + "not found");
        }
        return IOUtils.toString(stream);
    }

    public int insertCommit(String commitId,
                            String authorName,
                            String reviewerName,
                            String parent,
                            String message,
                            ZonedDateTime mergerDate,
                            ZonedDateTime authDate,
                            String mergeEmail,
                            String authorEmail,
                            String tag, int ticketNum, String project) {
        try {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement
                             = connection.prepareStatement(
                        "INSERT INTO COMMITS(COMMITID,AUTHOR,REVIEWER,PARENT,MESSAGE,AUTHTIME,MERGETIME,AUTHEMAIL,MERGEEMAIL,TAG,TICKETNUM,PROJECT)" +
                                " VALUES(?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, commitId);
                    statement.setString(2, authorName);
                    statement.setString(3, reviewerName);
                    statement.setString(4, parent);
                    statement.setString(5, message);
                    statement.setString(6, authDate.toString());
                    statement.setString(7, mergerDate.toString());
                    statement.setString(8, authorEmail);
                    statement.setString(9, mergeEmail);
                    if (tag == null) {
                        statement.setNull(10, Types.VARCHAR);
                    } else {
                        statement.setString(10, tag);
                    }
                    statement.setInt(11, ticketNum);
                    statement.setString(12, project);
                    statement.executeUpdate();
                    ResultSet generatedKeys = statement.getGeneratedKeys();
                    generatedKeys.next();
                    return generatedKeys.getInt(1);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to insert commit: " + message, e);
        }

    }

    public CommitFilesTracker prepareCommitFiles(int id) {
        try {
            Connection connection = getConnection();
            connection.setAutoCommit(false);
            PreparedStatement statement
                    = connection.prepareStatement(
                    "INSERT INTO COMMIT_FILES(CID,FILENAME,CHANGETYPE)" +
                            " VALUES(?,?,?)");
            return new CommitFilesTracker(statement, id);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to prepare commit files", e);
        }
    }

    public void addCommitFiles(CommitFilesTracker tracker, String changeType, String path) {
        try {
            PreparedStatement statement = tracker.statement;
            statement.setInt(1, tracker.id);
            statement.setString(2, path);
            statement.setString(3, changeType);
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add to batch: " + path, e);
        }
    }

    //
//id serial PRIMARY KEY,
//        commitId character varying(255),
//        author character varying(255),
//        reviewer character varying(255),
//        parent character varying(1024),
//        message character varying(4096),
//        authTime timestamp,
//        mergeTime timestamp,
//        authEmail character varying(255),
//        mergeEmail character varying(255),
//        tag character varying(255),
//        ticketNum integer,
//        project character varying(255)
    public synchronized List<Commit> commits() {
        LOGGER.info("Loading commits to calculate stats. This might take sometime ...");
        if (!commitList.isEmpty()) {
            return commitList;
        }
        try (Connection connection = getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement("Select * from commits order by mergetime")) {
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    Commit commit = new Commit();
                    commit.setId(resultSet.getInt("ID"));
                    commit.setCommitId(resultSet.getString("COMMITID"));
                    commit.setAuthor(resultSet.getString("author"));
                    commit.setReviewer(resultSet.getString("reviewer"));
                    commit.setParent(resultSet.getString("parent"));
                    commit.setMessage(resultSet.getString("message"));
                    commit.setAuthTime(ZonedDateTime.parse(resultSet.getString("authTime")));
                    commit.setMergeTime(ZonedDateTime.parse(resultSet.getString("mergeTime")));
                    commit.setAuthEmail(resultSet.getString("authEmail"));
                    commit.setMergeEmail(resultSet.getString("mergeEmail"));
                    commit.setTag(resultSet.getString("tag"));
                    commit.setTickerNum(resultSet.getInt("ticketNum"));
                    commit.setProject(resultSet.getString("project"));
                    int fileCount = getFileCount(connection, commit.getId());
                    commit.setFileCount(fileCount);
                    commitList.add(commit);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to commit files ", e);
        }
        LOGGER.info("Total Commits Retrieved: {}", commitList.size());
        return commitList;
    }

    private int getFileCount(Connection connection, int commitId) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("Select count(*) from COMMIT_FILES where CID=?")) {
            preparedStatement.setInt(1, commitId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    public void setGitBranch(String gitBranch) {
        this.gitBranch = gitBranch;
    }

    public String getGitBranch() {
        return gitBranch;
    }

    public String getDateRange() {
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
        return dateFormat.format(from) + " - " + dateFormat.format(to);
    }

    public void setDateRange(Date from, Date to) {
        this.from = from;
        this.to = to;
    }

    public static class CommitFilesTracker {
        private final PreparedStatement statement;
        private final int id;

        public CommitFilesTracker(PreparedStatement statement, int id) {
            this.statement = statement;
            this.id = id;
        }
    }
}
