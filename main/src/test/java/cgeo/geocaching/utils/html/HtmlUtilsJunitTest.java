package cgeo.geocaching.utils.html;

import cgeo.geocaching.utils.TextUtils;

import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class HtmlUtilsJunitTest {

    @Test
    public void testFormattedHtml() {
        //check whether and how bad-formatted HTML is cleaned up
        assertFormattedHtml(false, false, "<p att1='de\"f'   att2=\"value<>'&\"> text </p>", "<p att1=\"de&quot;f\" att2=\"value<>'&amp;\"> text </p>");
        assertFormattedHtml(false, false, "<p> text with line breaks <br> <br/> <div> no ending paragraph", "<p> text with line breaks <br> <br> </p><div> no ending paragraph</div>");
        assertFormattedHtml(false, false, "<p> text with comment <!-- my comment <>'& - -> --> </p> <!-- non-ending comment", "<p> text with comment <!-- my comment <>'& - -> --> </p> <!-- non-ending comment-->");
        assertFormattedHtml(false, false, "<p> Start a list <ul><li>first end the paragraph </p>", "<p> Start a list </p><ul><li>first end the paragraph <p></p></li></ul>");

        //check ooloring
        assertFormattedHtml(false, true, "<p att1='de\"f'   att2=\"value<>'&\"> text </p>", "[<p] [att1=\"][de&quot;f][\" att2=\"][value<>'&amp;][\"][>] text [</p>]");
        assertFormattedHtml(false, true, "<p> text with line breaks <br> <br/> <div> no ending paragraph", "[<p>] text with line breaks [<br>] [<br>] [</p>][<div>] no ending paragraph[</div>]");
        assertFormattedHtml(false, true, "<p> text with comment <!-- my comment <>'& - -> --> </p> <!-- non-ending comment", "[<p>] text with comment [<!-- my comment <>'& - -> -->] [</p>] [<!-- non-ending comment-->]");
        assertFormattedHtml(false, true, "<p> Start a list <ul><li>first end the paragraph </p>", "[<p>] Start a list [</p>][<ul>][<li>]first end the paragraph [<p>][</p>][</li>][</ul>]");

        //check pretty print
        assertFormattedHtml(true, true, "<p>Hello world<br>again</p>", "\n[<p>]\n  Hello world\n  [<br>]\n  again\n[</p>]");
    }

    private void assertFormattedHtml(final boolean prettyPrint, final boolean colorize, final String html, final String expectedResult) {
        final Spannable span = HtmlUtils.getFormattedHtml(html, prettyPrint, colorize, false);
        final String annotatedSpan = TextUtils.annotateSpans(span, o -> {
            if (o instanceof ForegroundColorSpan) {
                return new Pair<>("[", "]");
            }
            return null;
        });
        assertThat(annotatedSpan).isEqualTo(expectedResult);
    }

}
