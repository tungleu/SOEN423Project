package replicaOne.server.logger;

import replicaOne.server.util.LogFileUtil;

import java.io.File;

/**
 * Created by Kevin Tan 2020-09-21
 */
public final class UserLogger {

    public static final String DATA_DIR = "./src/main/java/data";
    public static final String USER_DIR = "/user/";
    public static final String LOG_FILE = "/log.txt";

    private UserLogger() { }

    public static void logActionForUser(String userID, String action) {
        File logFile = new File(DATA_DIR + USER_DIR + userID + LOG_FILE);
        LogFileUtil.logAction(logFile, userID, action);
    }

}
