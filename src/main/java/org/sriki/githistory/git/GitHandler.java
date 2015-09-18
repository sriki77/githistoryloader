package org.sriki.githistory.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sriki.githistory.LoaderProperties;
import org.sriki.githistory.db.DBHandler;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHandler.class);
    public static final LoaderProperties loaderProperties = LoaderProperties.getInstance();
    public static final int ONE_YEAR = 366;
    private final File gitPath;
    private final DBHandler dbHandler;

    public GitHandler(String gitPath, DBHandler dbHandler) {
        this.gitPath = new File(new File(gitPath), ".git");
        this.dbHandler = dbHandler;
        validateGitPath(gitPath);
    }

    private void validateGitPath(String gitPath) {
        if (gitPath == null || !this.gitPath.exists()) {
            throw new IllegalArgumentException("Invalid git repo: " + this.gitPath);
        }
    }

    public void init() throws IOException, GitAPIException {
        Repository repository = new FileRepositoryBuilder().setGitDir(gitPath).readEnvironment().findGitDir().build();
        Git git = new Git(repository);
        Iterable<RevCommit> commits = git.log()
                .add(repository.resolve(Constants.HEAD))
                .call();

        Date untilDate = untilDate();
        Map<String, String> idRevTagMap = revTagMap(repository, git);

        int num = 0;
        for (RevCommit commit : commits) {
            Date when = commit.getCommitterIdent().getWhen();
            if (when.before(untilDate)) {
                break;
            }
            insertCommitFiles(repository, commit, insertCommit(idRevTagMap, num, commit));
            ++num;
        }
        System.err.println("Total Commits: " + num);
    }

    private void insertCommitFiles(Repository repository, RevCommit commit, int id) throws IOException {
        RevCommit parent = commit.getParent(0);
        DiffFormatter diffFormatter = new DiffFormatter(System.out);
        diffFormatter.setRepository(repository);
        List<DiffEntry> diffs = diffFormatter.scan(parent.getId(), commit.getId());
        DBHandler.CommitFilesTracker commitFilesTracker = dbHandler.prepareCommitFiles(id);
        for (DiffEntry entry : diffs) {
            String changeType = entry.getChangeType().toString();
            String path = changeType.startsWith("DELETE") ? entry.getOldPath() : entry.getNewPath();

            dbHandler.addCommitFiles(commitFilesTracker, changeType, path);
        }
        dbHandler.persistCommitFiles(commitFilesTracker);
    }

    private int insertCommit(Map<String, String> idRevTagMap, int num, RevCommit commit) {
        String message = commit.getShortMessage();
        String[] splits = null;
        String project;
        int ticket;
        try {
            splits = getTicketDetails(message);
            project = splits[0];
            ticket = Integer.parseInt(splits[1]);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse message: " + message + " Splits: " + Arrays.toString(splits), e);
        }

        LOGGER.info("Importing commit: {}.{}", num, message);
        return dbHandler.insertCommit(
                commit.getId().getName(),
                commit.getCommitterIdent().getName(),
                commit.getAuthorIdent().getName(),
                commit.getParent(0).getName(),
                message.trim(),
                commit.getCommitterIdent().getWhen(),
                commit.getAuthorIdent().getWhen(),
                commit.getCommitterIdent().getEmailAddress(),
                commit.getAuthorIdent().getEmailAddress(),
                idRevTagMap.get(commit.getId().getName()), ticket, project);
    }

    private Map<String, String> revTagMap(Repository repository, Git git) throws GitAPIException {
        List<Ref> tags = git.tagList().call();
        Map<String, String> idRevTagMap = new HashMap<>();
        for (Ref tag : tags) {
            try {
                idRevTagMap.put(new RevWalk(repository).parseTag(
                        tag.getObjectId()).getObject().getId().getName(), tag.getName());
            } catch (IOException e) {
            }
        }
        return idRevTagMap;
    }

    private Date untilDate() {
        LocalDate till = LocalDate.now().minusDays(untilDays());
        return Date.from(till.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private String[] getTicketDetails(String message) {
        Pattern pattern = Pattern.compile("\\[([^\\]]+)\\]");
        Matcher matcher = pattern.matcher(message);
        if (!matcher.find()) {
            return new String[]{"", "-1"};
        }
        ;
        String ticketDetails = matcher.group(1);
        return ticketDetails.split("-|_");
    }

    private int untilDays() {
        return loaderProperties.getIntProperty("loader.history.untilDays", ONE_YEAR);
    }
}