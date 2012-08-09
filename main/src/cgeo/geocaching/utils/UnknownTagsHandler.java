package cgeo.geocaching.utils;

import org.xml.sax.XMLReader;

import android.text.Editable;
import android.text.Html.TagHandler;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;

public class UnknownTagsHandler implements TagHandler {

    private static final int UNDEFINED_POSITION = -1;
    private static int countCells = 0;
    int strikePos = UNDEFINED_POSITION;
    private boolean problematicDetected = false;
    private enum ListType {
        Ordered, Unordered
    }
    private static int listIndex = 0;
    private static ListType listType;

    @Override
    public void handleTag(boolean opening, String tag, Editable output,
            XMLReader xmlReader) {
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

    private void handleStrike(boolean opening, Editable output) {
        int length = output.length();
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

    private static void handleTd(boolean opening, Editable output) {
        // insert bar for each table column, see https://en.wikipedia.org/wiki/Box-drawing_characters
        if (opening) {
            if (countCells++ > 0) {
                output.append('┆');
            }
        }
    }

    private static void handleTr(boolean opening, Editable output) {
        // insert new line for each table row
        if (opening) {
            output.append('\n');
            countCells = 0;
        }
    }

    // Ordered lists are handled in a simple manner. They are rendered as Arabic numbers starting at 1
    // with no handling for alpha or Roman numbers or arbitrary numbering.
    private static void handleOl(boolean opening) {
        if (opening) {
            listIndex = 1;
            listType = ListType.Ordered;
        } else {
            listType = ListType.Unordered;
        }
    }

    private static void handleLi(boolean opening, Editable output) {
        if (opening) {
            if (listType == ListType.Ordered) {
                output.append("\n  " + (listIndex++) + ". ");
            } else {
                output.append("\n  • ");
            }
        }
    }

}
