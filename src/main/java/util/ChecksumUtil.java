package util;

import com.google.common.hash.Hashing;

import java.nio.charset.Charset;

public final class ChecksumUtil {

    private ChecksumUtil() {
    }

    public static String generateChecksumSHA256(String value) {
        return Hashing.sha256().hashString(value, Charset.defaultCharset()).toString();
    }

}
