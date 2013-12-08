package cgeo.geocaching.connector.ec;

import java.util.regex.Pattern;

/**
 * For further information about patterns have a look at
 * http://download.oracle.com/javase/1.4.2/docs/api/java/util/regex/Pattern.html
 */
public final class ECConstants {

    public static final Pattern PATTERN_LOGIN_NAME = Pattern.compile("\"mod_login_greetingfrontpage-teaser\">Hallo, ([^<]+)</span>");
    public static final Pattern PATTERN_LOGIN_SECURITY = Pattern.compile("<input type=\"hidden\" name=\"return\" value=\"(.*)\" />[^<]*<input type=\"hidden\" name=\"(.*)\" value=\"1\" />");
    public static final Pattern PATTERN_CACHES_FOUND = Pattern.compile("Gefundene Caches::</label><div class=\"cb_field\"><div id=\"cbfv_71\">([0-9]+)</div>");

    private ECConstants() {
        // this class shall not have instances
    }

}
