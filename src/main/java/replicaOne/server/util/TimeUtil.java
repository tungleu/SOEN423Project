package replicaOne.server.util;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Kevin Tan 2020-09-21
 */
public final class TimeUtil {

    private TimeUtil() {
    }

    public static String generateTimestamp() {
        return "[" + LocalDateTime.now().toString() + "]";
    }

    public static Date parseStringToDate(String date) {
        int day = Integer.parseInt(date.substring(0, 2));
        int month = Integer.parseInt(date.substring(2, 4));
        int year = Integer.parseInt(date.substring(4));

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);

        return calendar.getTime();
    }

    public static String parseDateToString(Date date) {
        return date.getYear() + "," + date.getMonth() + "," + date.getDay();
    }

    public static Date getDateNow() {
        Calendar calendar = Calendar.getInstance();
        return new Date(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
    }

}
