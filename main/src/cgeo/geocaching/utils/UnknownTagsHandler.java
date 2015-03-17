package cgeo.geocaching.utils;

import org.xml.sax.XMLReader;

import android.text.Editable;
import android.text.Html.TagHandler;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;

public class UnknownTagsHandler implements TagHandler {

    private enum ListType {
        Ordered, Unordered
    }

    private static final int UNDEFINED_POSITION = -1;

    private int countCells = 0;
    private int strikePos = UNDEFINED_POSITION;
    private boolean problematicDetected = false;
    private int listIndex = 0;
    private ListType listType = ListType.Unordered;

    @Override
    public void handleTag(final boolean opening, final String tag, final Editable output,
            final XMLReader xmlReader) {
        if (tag.equalsIgnoreCase("strike") || tag.equals("s")) {
            handleStrike(opening, output);
        } else if (tag.equalsIgnoreCase("table")) {
            handleProblematic();
        } else if (tag.equalsIgnoreCase("td")) {
            handleTd(opening, output);
        } else if (tag.equalsIgnoreCase("tr")) {
            handleTr(opening, output);
        } else if (tag.equalsIgnoreCase("pre")) {
            handleProblematic();
        } else if (tag.equalsIgnoreCase("ol")) {
            handleOl(opening);
        } else if (tag.equalsIgnoreCase("li")) {
            handleLi(opening, output);
        }
    }

    private void handleStrike(final boolean opening, final Editable output) {
        final int length = output.length();
        if (opening) {
            strikePos = length;
        } else {
            if (strikePos > UNDEFINED_POSITION) {
                output.setSpan(new StrikethroughSpan(), strikePos, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                strikePos = UNDEFINED_POSITION;
            }
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
        if (opening) {
            if (countCells++ > 0) {
                output.append('┆');
            }
        }
    }

    private void handleTr(final boolean opening, final Editable output) {
        // insert new line for each table row
        if (opening) {
            output.append('\n');
            countCells = 0;
        }
    }

    // Ordered lists are handled in a simple manner. They are rendered as Arabic numbers starting at 1
    // with no handling for alpha or Roman numbers or arbitrary numbering.
    private void handleOl(final boolean opening) {
        if (opening) {
            listIndex = 1;
            listType = ListType.Ordered;
        } else {
            listType = ListType.Unordered;
        }
    }

    private void handleLi(final boolean opening, final Editable output) {
        if (opening) {
            if (listType == ListType.Ordered) {
                output.append("\n  ").append(String.valueOf(listIndex++)).append(". ");
            } else {
                output.append("\n  • ");
            }
        }
    }

}
