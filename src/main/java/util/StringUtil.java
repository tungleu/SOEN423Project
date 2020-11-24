package util;

import java.util.Arrays;

public final class StringUtil {
    private StringUtil(){}

    public static String sortStr(String str) {
        char[] chars = str.toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }
}
