package util;
import java.security.SecureRandom;
import java.util.Locale;

public class Url {
    private static final SecureRandom rand = new SecureRandom();

    private static String encodeBase36(Long number) {
        if (number == null) {
            throw new IllegalArgumentException("Number must not be null");
        }
        return Long.toString(number, 36).toLowerCase();
    }


    public static String generateSlug(String baseName, int suffixLength) {
        String normalizedBaseName = baseName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("-$", "");
        return normalizedBaseName + "-" + encodeBase36(rand.nextLong());
    }
}
