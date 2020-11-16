package util;

import com.google.common.hash.Hashing;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;

public final class ChecksumUtil {

    private ChecksumUtil() {
    }

    public static String generateChecksumSHA256(String value) {
        return Hashing.sha256().hashString(value, Charset.defaultCharset()).toString();
    }

    public static String generateChecksumSHA256(List<String> strings){
        StringBuilder sb = new StringBuilder();
        for(String string : strings){
            sb.append(string);
        }
        return Hashing.sha256().hashString(sb.toString(), Charset.defaultCharset()).toString();
    }

}
