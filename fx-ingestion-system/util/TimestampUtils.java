package util;

import java.time.Instant;

public class TimestampUtils {
    public static String getCurrentIst() {
        return Instant.now().toString();
    }
}
