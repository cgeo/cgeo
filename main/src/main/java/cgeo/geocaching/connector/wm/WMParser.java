package cgeo.geocaching.connector.wm;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cgeo.geocaching.utils.Log;

public final class WMParser {
    public static String getUsername(final String loginHtml) {
        if (StringUtils.isBlank(loginHtml)) {
            Log.w("WMParser: could not retrieve username (blank)");
            return "";
        }

        if (!loginHtml.contains("<a href=\"./default.aspx?RESET=Y\">")) {
            Log.w("WMParser: could not retrieve username (not on right page?)");
            return "";
        }

        final Document document = Jsoup.parse(loginHtml);
        return document.select("#ctl00_ContentBody_lbMessageText strong").text();
    }

    public static int getFindsCount(final String statsHtml) {
        if (StringUtils.isBlank(statsHtml)) {
            Log.w("WMParser: could not retrieve waymarking visits data (blank)");
            return -1;
        }

        if (!statsHtml.contains("<div id=\"BasicFinds\"")) {
            if (statsHtml.contains("<span id=\"ctl00_ContentBody_uxChronologyMessage\"")) {
                return 0; // no finds
            }

            Log.w("WMParser: could not retrieve waymarking visits data (parsing error)");
            return -1;
        }

        final Document document = Jsoup.parse(statsHtml);
        final String value = document.select("div#BasicFinds").text();

        final Matcher distinctMatcher = Pattern.compile("\\(\\D*(\\d+)\\D*\\)").matcher(value);
        if (distinctMatcher.find()) {
            return Integer.parseInt(Objects.requireNonNull(distinctMatcher.group(1)));
        }

        final Matcher strongMatcher = Pattern.compile("<strong>(\\d+)</strong>").matcher(value);
        if (strongMatcher.find()) {
            return Integer.parseInt(Objects.requireNonNull(strongMatcher.group(1)));
        }

        Log.w("WMParser: could not retrieve waymarking visits data (no match)");
        return 0;
    }
}
