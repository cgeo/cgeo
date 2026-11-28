package cgeo.geocaching.utils.html;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.LocalizationUtils;

import android.text.Editable;
import android.text.Html.TagHandler;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.util.Pair;

import java.util.ArrayDeque;
import java.util.Deque;

import org.xml.sax.XMLReader;

public class UnknownTagsHandler implements TagHandler {

    /** private markers before and after every ul, see {@link HtmlUtils#markUnorderedLists(String)}; only their opening matters */
    public static final String UL_START_TAG = "cgeo-ul-start";
    public static final String UL_END_TAG = "cgeo-ul-end";

    /** Text range of one HTML list (ol or ul); turned into list item markers by {@link HtmlUtils#fromHtmlWithLists}. */
    public static final class ListRange {
        public final boolean ordered;

        ListRange(final boolean ordered) {
            this.ordered = ordered;
        }
    }

    private static final int UNDEFINED_POSITION = -1;

    private int countCells = 0;
    private int strikePos = UNDEFINED_POSITION;
    private boolean problematicDetected = false;
    private final Deque<Pair<ListRange, Integer>> openLists = new ArrayDeque<>();

    @Override
    public void handleTag(final boolean opening, final String tag, final Editable output,
                          final XMLReader xmlReader) {
        if ("strike".equalsIgnoreCase(tag) || "s".equals(tag)) {
            handleStrike(opening, output);
        } else if ("table".equalsIgnoreCase(tag)) {
            handleProblematic();
        } else if ("td".equalsIgnoreCase(tag) || "th".equalsIgnoreCase(tag)) {
            handleTd(opening, output);
        } else if ("tr".equalsIgnoreCase(tag)) {
            handleTr(opening, output);
        } else if ("pre".equalsIgnoreCase(tag)) {
            handleProblematic();
        } else if ("ol".equalsIgnoreCase(tag)) {
            handleList(opening, true, output);
        } else if (UL_START_TAG.equalsIgnoreCase(tag) && opening) {
            handleList(true, false, output);
        } else if (UL_END_TAG.equalsIgnoreCase(tag) && opening) {
            handleList(false, false, output);
        } else if ("dt".equalsIgnoreCase(tag)) {
            handleDT(opening, output);
        } else if ("dd".equalsIgnoreCase(tag)) {
            handleDD(output);
        } else if ("hr".equalsIgnoreCase(tag)) {
            handleHr(opening, output);
        }
    }

    private void handleStrike(final boolean opening, final Editable output) {
        final int length = output.length();
        if (opening) {
            strikePos = length;
        } else if (strikePos > UNDEFINED_POSITION) {
            output.setSpan(new StrikethroughSpan(), strikePos, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            strikePos = UNDEFINED_POSITION;
        }
    }

    public boolean isProblematicDetected() {
        return problematicDetected;
    }

    private void handleProblematic() {
        problematicDetected = true;
    }

    private void handleTd(final boolean opening, final Editable output) {
        // insert bar for each table column, see https://en.wikipedia.org/wiki/Box-drawing_characters
        if (opening && countCells++ > 0) {
            output.append(LocalizationUtils.getPlainString(R.string.triple_dash_vertical));
        }
    }

    private void handleTr(final boolean opening, final Editable output) {
        // insert new line for each table row
        if (opening) {
            output.append('\n');
            countCells = 0;
        }
    }

    // li itself is rendered by Android (it never reaches a TagHandler), so only the list boundaries are recorded here
    private void handleList(final boolean opening, final boolean ordered, final Editable output) {
        if (opening) {
            if (openLists.isEmpty()) {
                ensureBlankLine(output);
            }
            openLists.push(new Pair<>(new ListRange(ordered), output.length()));
        } else if (!openLists.isEmpty()) {
            final Pair<ListRange, Integer> list = openLists.pop();
            if (output.length() > list.second) {
                output.setSpan(list.first, list.second, output.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (openLists.isEmpty()) {
                ensureBlankLine(output);
            }
        }
    }

    // a top-level list is separated from the surrounding text like a paragraph (items themselves get one line break only)
    private static void ensureBlankLine(final Editable output) {
        if (output.length() == 0) {
            return;
        }
        int newlines = 0;
        for (int i = output.length() - 1; i >= 0 && output.charAt(i) == '\n' && newlines < 2; i--) {
            newlines++;
        }
        for (; newlines < 2; newlines++) {
            output.append('\n');
        }
    }

    private void handleDT(final boolean opening, final Editable output) {
        output.append(opening ? "\n>> " : "");
    }

    private void handleDD(final Editable output) {
        output.append("\n");
    }

    private static void handleHr(final boolean opening, final Editable output) {
        if (opening) {
            // If we are in the middle of a line, add a line feed.
            if (output.length() > 0 && output.charAt(output.length() - 1) != '\n') {
                output.append('\n');
            }
            final int start = output.length();
            output.append("                              ");
            output.setSpan(new StrikethroughSpan(), start, output.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            output.append("\n");
        }
    }

}
