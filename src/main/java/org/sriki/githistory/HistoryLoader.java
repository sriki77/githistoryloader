package org.sriki.githistory;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sriki.githistory.db.DBHandler;
import org.sriki.githistory.git.GitHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

public class HistoryLoader {

    public static final String JETTY_XML = "/jetty.xml";
    public static final String GIT_REPOS_PROPERTY = "loader.git.repos";
    private DBHandler dbHandler;
    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryLoader.class);
    private Server jetty;

    public HistoryLoader() {
        this.dbHandler = DBHandler.getInstance();
    }

    public static void main(String[] args) throws Exception {
        LoaderProperties loaderProps = LoaderProperties.getInstance();
        loaderProps.init();

        HistoryLoader historyLoader = new HistoryLoader();
        String gitRepos = loaderProps.getProperty(GIT_REPOS_PROPERTY);
        if (gitRepos != null) {
            historyLoader.loadGit(gitRepos.split(","));
        }
        historyLoader.startServer();
    }

    private void startServer() {
        LOGGER.info("Starting Jetty Server....");
        try {
            jetty = new Server();
            final XmlConfiguration xmlConfiguration = getJettyConfig();
            xmlConfiguration.configure(jetty);
            jetty.setStopAtShutdown(true);
            jetty.start();
        } catch (Exception e) {
            LOGGER.error("Failed to start jetty server", e);
            System.exit(-1);
        }
    }

    private XmlConfiguration getJettyConfig() throws SAXException, IOException {
        return new XmlConfiguration(this.getClass().getResourceAsStream(JETTY_XML));
    }

    private void loadGit(String[] gitPath) throws IOException, GitAPIException, SQLException {
        dbHandler.initDB();
        for (String g : gitPath) {
            LOGGER.info("Loading git path: {}",g);
            GitHandler gitHandler = new GitHandler(g, dbHandler);
            gitHandler.init();
        }
    }

}
