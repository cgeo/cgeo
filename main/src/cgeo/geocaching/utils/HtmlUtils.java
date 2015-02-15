package cgeo.geocaching.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import android.text.Html;
import android.text.Spanned;
import android.text.style.ImageSpan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public final class HtmlUtils {

    private HtmlUtils() {
        // utility class
    }

    /**
     * Extract the text from a HTML based string. This is similar to what HTML.fromHtml(...) does, but this method also
     * removes the embedded images instead of replacing them by a small rectangular representation character.
     *
     */
    public static String extractText(final CharSequence html) {
        if (StringUtils.isBlank(html)) {
            return StringUtils.EMPTY;
        }
        String result = html.toString();

        // recognize images in textview HTML contents
        if (html instanceof Spanned) {
            final Spanned text = (Spanned) html;
            final Object[] styles = text.getSpans(0, text.length(), Object.class);
            final ArrayList<Pair<Integer, Integer>> removals = new ArrayList<>();
            for (final Object style : styles) {
                if (style instanceof ImageSpan) {
                    final int start = text.getSpanStart(style);
                    final int end = text.getSpanEnd(style);
                    removals.add(Pair.of(start, end));
                }
            }

            // sort reversed and delete image spans
            Collections.sort(removals, new Comparator<Pair<Integer, Integer>>() {

                @Override
                public int compare(final Pair<Integer, Integer> lhs, final Pair<Integer, Integer> rhs) {
                    return rhs.getRight().compareTo(lhs.getRight());
                }
            });
            result = text.toString();
            for (final Pair<Integer, Integer> removal : removals) {
                result = result.substring(0, removal.getLeft()) + result.substring(removal.getRight());
            }
        }

        // now that images are gone, do a normal html to text conversion
        return Html.fromHtml(result).toString().trim();
    }

    public static String removeExtraParagraph(final String htmlIn) {
        final String html = StringUtils.trim(htmlIn);
        if (StringUtils.startsWith(html, "<p>") && StringUtils.endsWith(html, "</p>")) {
            final String paragraph = StringUtils.substring(html, "<p>".length(), html.length() - "</p>".length()).trim();
            if (extractText(paragraph).equals(paragraph)) {
                return paragraph;
            }
        }
        return html;
    }
}
