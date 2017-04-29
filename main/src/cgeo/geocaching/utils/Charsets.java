package cgeo.geocaching.utils;

import java.nio.charset.Charset;

/**
 * utility class, just for disabling the deprecated warning at a single place
 */
public final class Charsets {
    /**
     * We cannot use the recommend java.nio.charset.StandardCharsets, since that is only available for target >= 19.
     */
    // CHECKSTYLE IGNORE StaticVariableNameCheck FOR NEXT 1 LINES
    public static final Charset UTF_8 = Charset.forName("UTF-8");

    private Charsets() {
        // utility class
    }

}
