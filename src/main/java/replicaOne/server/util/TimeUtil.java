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
        int month = Integer.parseInt(date.substring(2, 4)) - 1;
        int year = Integer.parseInt(date.substring(4));

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);

        return calendar.getTime();
    }

    public static String parseDateToString(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String monthString = (month > 9) ? Integer.toString(month) : "0" + month;
        String dayString = (day > 9) ? Integer.toString(day) : "0" + day;
        return dayString + monthString + calendar.get(Calendar.YEAR);
    }

    public static Date getDateNow() {
        return Calendar.getInstance().getTime();
    }

}
