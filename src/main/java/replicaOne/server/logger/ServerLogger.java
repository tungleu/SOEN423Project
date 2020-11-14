package replicaOne.server.logger;

import replicaOne.server.util.LogFileUtil;

import java.io.File;

/**
 * Created by Kevin Tan 2020-09-20
 */
public class ServerLogger {

    private static final String LOG_FILE = "/log.txt";
    private final String serverDir;

    public ServerLogger(String serverName) {
        serverDir = serverName;
    }

    public void logAction(String userID, String action) {
        File logFile = new File(UserLogger.DATA_DIR + "/" + serverDir + LOG_FILE);
        synchronized (this){
            LogFileUtil.logAction(logFile, userID, action);
        }
    }
}
