package cgeo.geocaching.utils;

import org.xml.sax.XMLReader;

import android.text.Editable;
import android.text.Html.TagHandler;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;

public class UnknownTagsHandler implements TagHandler {

    private static final int UNDEFINED_POSITION = -1;
    int strikePos = UNDEFINED_POSITION;
    private boolean tableDetected = false;

    public void handleTag(boolean opening, String tag, Editable output,
            XMLReader xmlReader) {
        if (tag.equalsIgnoreCase("strike") || tag.equals("s")) {
            handleStrike(opening, output);
        } else if (tag.equalsIgnoreCase("table")) {
            handleTable();
        } else if (tag.equalsIgnoreCase("td")) {
            handleTd(opening, output);
        } else if (tag.equalsIgnoreCase("tr")) {
            handleTr(opening, output);
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

    public boolean isTableDetected() {
        return tableDetected;
    }

    private void handleTable() {
        tableDetected = true;
    }

    private static void handleTd(boolean opening, Editable output) {
        // insert space for each table column
        if (opening) {
            output.insert(output.length(), " ");
        }
    }

    private static void handleTr(boolean opening, Editable output) {
        // insert new line for each table row
        if (opening) {
            output.insert(output.length(), "\n");
        }
    }

}
