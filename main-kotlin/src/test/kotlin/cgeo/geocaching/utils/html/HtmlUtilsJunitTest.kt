// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils.html

import cgeo.geocaching.utils.TextUtils

import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.util.Pair

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class HtmlUtilsJunitTest {

    @Test
    public Unit testFormattedHtml() {
        //check whether and how bad-formatted HTML is cleaned up
        assertFormattedHtml(false, false, "<p att1='de\"f'   att2=\"value<>'&\"> text </p>", "<p att1=\"de&quot;f\" att2=\"value&lt;&gt;'&amp;\"> text </p>")
        assertFormattedHtml(false, false, "<p> text with line breaks <br> <br/> <div> no ending paragraph", "<p> text with line breaks <br> <br> </p><div> no ending paragraph</div>")
        assertFormattedHtml(false, false, "<p> text with comment <!-- my comment <>'& - -> --> </p> <!-- non-ending comment", "<p> text with comment <!-- my comment <>'& - -> --> </p> <!-- non-ending comment-->")
        assertFormattedHtml(false, false, "<p> Start a list <ul><li>first end the paragraph </p>", "<p> Start a list </p><ul><li>first end the paragraph <p></p></li></ul>")

        //check coloring
        assertFormattedHtml(false, true, "<p att1='de\"f'   att2=\"value<>'&\"> text </p>", "[<p] [att1=\"][de&quot;f][\" att2=\"][value&lt;&gt;'&amp;][\"][>] text [</p>]")
        assertFormattedHtml(false, true, "<p> text with line breaks <br> <br/> <div> no ending paragraph", "[<p>] text with line breaks [<br>] [<br>] [</p>][<div>] no ending paragraph[</div>]")
        assertFormattedHtml(false, true, "<p> text with comment <!-- my comment <>'& - -> --> </p> <!-- non-ending comment", "[<p>] text with comment [<!-- my comment <>'& - -> -->] [</p>] [<!-- non-ending comment-->]")
        assertFormattedHtml(false, true, "<p> Start a list <ul><li>first end the paragraph </p>", "[<p>] Start a list [</p>][<ul>][<li>]first end the paragraph [<p>][</p>][</li>][</ul>]")

        //check pretty print
        assertFormattedHtml(true, true, "<p>Hello world<br>again</p>", "[<p>]\n  Hello world\n  [<br>]\n  again\n[</p>]")
    }

    private Unit assertFormattedHtml(final Boolean prettyPrint, final Boolean colorize, final String html, final String expectedResult) {
        val span: Spannable = HtmlUtils.getFormattedHtml(html, prettyPrint, colorize, false)
        val annotatedSpan: String = TextUtils.annotateSpans(span, o -> {
            if (o is ForegroundColorSpan) {
                return Pair<>("[", "]")
            }
            return null
        })
        assertThat(annotatedSpan).isEqualTo(expectedResult)
    }

}
