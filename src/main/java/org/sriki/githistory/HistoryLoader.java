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
    private DBHandler dbHandler;
    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryLoader.class);
    private Server jetty;

    public HistoryLoader() {
        this.dbHandler = DBHandler.getInstance();
    }

    public static void main(String[] args) throws Exception {
        String gitPath = args.length == 0 ? null : args[0];

        LoaderProperties loaderProps = LoaderProperties.getInstance();
        loaderProps.init();

        HistoryLoader historyLoader = new HistoryLoader();
        if (gitPath != null) {
            historyLoader.loadGit(gitPath);
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

    private void loadGit(String gitPath) throws IOException, GitAPIException, SQLException {
        dbHandler.initDB();
        GitHandler gitHandler = new GitHandler(gitPath, dbHandler);
        gitHandler.init();
    }

}
