package replicaTwo.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class StoreUtils {
    private final static String DATE_FORMAT = "ddMMyyyy";
    private final static int RETURN_POLICY_DAYS = 30;
    private final static DateFormat sourceFormat = new SimpleDateFormat(DATE_FORMAT);

    public static Long parseDate(String date) {
        Date parsedDate = new Date();
        try {
            parsedDate = sourceFormat.parse(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return parsedDate.getTime();
    }

    public static String getCurrentDateString() {
        return sourceFormat.format(new Date(System.currentTimeMillis()));
    }

    public static long generateReturnWindow(String dateOfReturn) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(parseDate(dateOfReturn)));
        cal.add(Calendar.DATE, -RETURN_POLICY_DAYS);
        return cal.getTime().getTime();
    }

    public static String getStoreFromDescriptor(String storeDescriptor) {
        return storeDescriptor.substring(0, 2);
    }
}
