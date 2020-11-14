package replicaOne.server.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by Kevin Tan 2020-09-26
 */
public final class LogFileUtil {

    private LogFileUtil() {
    }

    public static void logAction(File logFile, String userID, String action) {
        try (RandomAccessFile reader = new RandomAccessFile(logFile, "rws")) {
            reader.seek(reader.length());
            reader.writeChars("[" + userID + "]" + action + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
