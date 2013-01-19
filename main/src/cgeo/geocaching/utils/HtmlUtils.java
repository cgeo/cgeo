package cgeo.geocaching.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import android.text.Spanned;
import android.text.style.ImageSpan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class HtmlUtils {

    /**
     * Extract the text from a HTML based string. This is similar to what HTML.fromHtml(...) does, but this method also
     * removes the embedded images instead of replacing them by a small rectangular representation character.
     *
     * @param html
     * @return
     */
    public static String extractText(CharSequence html) {
        String result = html.toString();

        // recognize images in textview HTML contents
        if (html instanceof Spanned) {
            Spanned text = (Spanned) html;
            Object[] styles = text.getSpans(0, text.length(), Object.class);
            ArrayList<Pair<Integer, Integer>> removals = new ArrayList<Pair<Integer, Integer>>();
            for (Object style : styles) {
                if (style instanceof ImageSpan) {
                    int start = text.getSpanStart(style);
                    int end = text.getSpanEnd(style);
                    removals.add(Pair.of(start, end));
                }
            }

            // sort reversed and delete image spans
            Collections.sort(removals, new Comparator<Pair<Integer, Integer>>() {

                @Override
                public int compare(Pair<Integer, Integer> lhs, Pair<Integer, Integer> rhs) {
                    return rhs.getRight().compareTo(lhs.getRight());
                }
            });
            result = text.toString();
            for (Pair<Integer, Integer> removal : removals) {
                result = result.substring(0, removal.getLeft()) + result.substring(removal.getRight());
            }
        }

        // some line breaks are still in the text, source is unknown
        return StringUtils.replace(result, "<br />", "\n").trim();
    }

    /**
     * Convert any non-Latin characters into their HTML escaped equivalents
     * 
     * @param input
     *            String
     * @return output String
     */
    public static String convertNonLatinCharactersToHTML(final String input) {
        final int inputLen = input.length();
        final StringBuilder output = new StringBuilder();

        for (int i = 0; i < inputLen; i++) {
            char c = input.charAt(i);

            if (c > 300) {
                output.append("&#");
                output.append(Integer.toString(c));
                output.append(';');
            } else {
                output.append(c);
            }
        }

        return output.toString();
    }
}
