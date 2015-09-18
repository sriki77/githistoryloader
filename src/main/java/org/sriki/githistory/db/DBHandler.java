package org.sriki.githistory.db;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sriki.githistory.LoaderProperties;
import org.sriki.githistory.model.Commit;

import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Provider
public final class DBHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBHandler.class);
    public static final String DB_DRIVER = "org.postgresql.Driver";
    public static final String DEFAULT_PG_URL = "jdbc:postgresql://localhost:5432/git_history?user=postgres&password=postgres";
    public static final String DBSCHEMA_SQL = "dbschema.sql";
    public static final LoaderProperties loaderProperties = LoaderProperties.getInstance();
    public static final String LOADER_DB_URL_PROPERTY = "loader.dbUrl";
    private final BasicDataSource basicDataSource;
    private static final DBHandler ME = new DBHandler();
    private List<Commit> commitList = new ArrayList<>();

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

    private BasicDataSource initDriver() throws Exception {
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setDriverClassName(DB_DRIVER);
        basicDataSource.setUrl(getDBURL());
        return basicDataSource;
    }

    private Connection getConnection() throws SQLException {
        return basicDataSource.getConnection();
    }

    private String getDBURL() {
        return loaderProperties.getProperty(LOADER_DB_URL_PROPERTY, DEFAULT_PG_URL);
    }

    public void initDB() throws SQLException, IOException {
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
                            Date mergerDate,
                            Date authDate,
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
                    statement.setTimestamp(6, new Timestamp(authDate.getTime()));
                    statement.setTimestamp(7, new Timestamp(mergerDate.getTime()));
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
            return new CommitFilesTracker(connection, statement, id);
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
            statement.addBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add to batch: " + path, e);
        }
    }

    public void persistCommitFiles(CommitFilesTracker tracker) {
        try (Connection connection = tracker.connection) {
            try (PreparedStatement preparedStatement = tracker.statement) {
                preparedStatement.executeBatch();
                connection.commit();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to commit files ", e);
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
        if (!commitList.isEmpty()) {
            return commitList;
        }
        try (Connection connection = getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement("Select * from commits")) {
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    Commit commit = new Commit();
                    commit.setId(resultSet.getInt("ID"));
                    commit.setCommitId(resultSet.getString("COMMITID"));
                    commit.setAuthor(resultSet.getString("author"));
                    commit.setReviewer(resultSet.getString("reviewer"));
                    commit.setParent(resultSet.getString("parent"));
                    commit.setMessage(resultSet.getString("message"));
                    commit.setAuthTime(resultSet.getTimestamp("authTime"));
                    commit.setMergeTime(resultSet.getTimestamp("mergeTime"));
                    commit.setAuthEmail(resultSet.getString("authEmail"));
                    commit.setMergeEmail(resultSet.getString("mergeEmail"));
                    commit.setTag(resultSet.getString("tag"));
                    commit.setTickerNum(resultSet.getInt("ticketNum"));
                    commit.setProject(resultSet.getString("project"));
                    commitList.add(commit);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to commit files ", e);
        }
        LOGGER.info("Total Commits Retrieved: {}", commitList.size());
        return commitList;
    }

    public static class CommitFilesTracker {
        private final Connection connection;
        private final PreparedStatement statement;
        private final int id;

        public CommitFilesTracker(Connection connection, PreparedStatement statement, int id) {
            this.connection = connection;
            this.statement = statement;
            this.id = id;
        }
    }
}
