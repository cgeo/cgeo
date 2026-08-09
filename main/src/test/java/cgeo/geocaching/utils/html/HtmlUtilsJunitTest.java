package cgeo.geocaching.utils.html;

import cgeo.geocaching.utils.TextUtils;

import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class HtmlUtilsJunitTest {



    @Test
    public void testFormattedHtml() {
        //check whether and how bad-formatted HTML is cleaned up
        assertFormattedHtml(false, false, "<p att1='de\"f'   att2=\"value<>'&\"> text </p>", "<p att1=\"de&quot;f\" att2=\"value&lt;&gt;'&amp;\"> text </p>");
        assertFormattedHtml(false, false, "<p> text with line breaks <br> <br/> <div> no ending paragraph", "<p> text with line breaks <br> <br> </p><div> no ending paragraph</div>");
        assertFormattedHtml(false, false, "<p> text with comment <!-- my comment <>'& - -> --> </p> <!-- non-ending comment", "<p> text with comment <!-- my comment <>'& - -> --> </p> <!-- non-ending comment-->");
        assertFormattedHtml(false, false, "<p> Start a list <ul><li>first end the paragraph </p>", "<p> Start a list </p><ul><li>first end the paragraph <p></p></li></ul>");

        //check coloring
        assertFormattedHtml(false, true, "<p att1='de\"f'   att2=\"value<>'&\"> text </p>", "[<p] [att1=\"][de&quot;f][\" att2=\"][value&lt;&gt;'&amp;][\"][>] text [</p>]");
        assertFormattedHtml(false, true, "<p> text with line breaks <br> <br/> <div> no ending paragraph", "[<p>] text with line breaks [<br>] [<br>] [</p>][<div>] no ending paragraph[</div>]");
        assertFormattedHtml(false, true, "<p> text with comment <!-- my comment <>'& - -> --> </p> <!-- non-ending comment", "[<p>] text with comment [<!-- my comment <>'& - -> -->] [</p>] [<!-- non-ending comment-->]");
        assertFormattedHtml(false, true, "<p> Start a list <ul><li>first end the paragraph </p>", "[<p>] Start a list [</p>][<ul>][<li>]first end the paragraph [<p>][</p>][</li>][</ul>]");

        //check pretty print
        assertFormattedHtml(true, true, "<p>Hello world<br>again</p>", "[<p>]\n  Hello world\n  [<br>]\n  again\n[</p>]");
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

    @Test
    public void testHtmlListRenderHelperFlatLists() {
        final HtmlUtils.HtmlListRenderHelper helper = new HtmlUtils.HtmlListRenderHelper();

        //before any list is started, there is no indentation and no item
        assertThat(helper.intend()).isEqualTo(0);
        assertThat(helper.listItem()).isNull();

        //a simple numbered list
        helper.startList(true);
        assertThat(helper.intend()).isEqualTo(1);
        assertThat(helper.listItem()).isEqualTo(1);
        assertThat(helper.listItem()).isEqualTo(2);
        assertThat(helper.listItem()).isEqualTo(3);
        helper.endList();
        assertThat(helper.intend()).isEqualTo(0);

        //a simple unnumbered list
        helper.startList(false);
        assertThat(helper.intend()).isEqualTo(1);
        assertThat(helper.listItem()).isNull();
        assertThat(helper.listItem()).isNull();
        helper.endList();
        assertThat(helper.intend()).isEqualTo(0);

        //a numbered list with a custom start number
        helper.startList(true, 5);
        assertThat(helper.listItem()).isEqualTo(5);
        assertThat(helper.listItem()).isEqualTo(6);
        helper.endList();
        assertThat(helper.intend()).isEqualTo(0);
    }

    @Test
    public void testHtmlListRenderHelperNestedEntangledLists() {
        final HtmlUtils.HtmlListRenderHelper helper = new HtmlUtils.HtmlListRenderHelper();

        //outer numbered list
        helper.startList(true);
        assertThat(helper.intend()).isEqualTo(1);
        assertThat(helper.listItem()).isEqualTo(1);

        //nested unnumbered list inside the numbered one
        helper.startList(false);
        assertThat(helper.intend()).isEqualTo(2);
        assertThat(helper.listItem()).isNull();
        assertThat(helper.listItem()).isNull();

        //nested numbered list (custom start) inside the unnumbered one
        helper.startList(true, 10);
        assertThat(helper.intend()).isEqualTo(3);
        assertThat(helper.listItem()).isEqualTo(10);
        assertThat(helper.listItem()).isEqualTo(11);
        helper.endList();
        assertThat(helper.intend()).isEqualTo(2);

        //back in unnumbered list, still not numbered
        assertThat(helper.listItem()).isNull();
        helper.endList();
        assertThat(helper.intend()).isEqualTo(1);

        //back in outer numbered list, numbering continues where it left off
        assertThat(helper.listItem()).isEqualTo(2);

        //another nested numbered list, restarting at 1
        helper.startList(true);
        assertThat(helper.intend()).isEqualTo(2);
        assertThat(helper.listItem()).isEqualTo(1);
        helper.endList();
        assertThat(helper.intend()).isEqualTo(1);

        //outer list continues its own numbering unaffected by the nested one
        assertThat(helper.listItem()).isEqualTo(3);
        helper.endList();
        assertThat(helper.intend()).isEqualTo(0);
    }

    @Test
    public void testHtmlListRenderHelperEdgeCases() {
        final HtmlUtils.HtmlListRenderHelper helper = new HtmlUtils.HtmlListRenderHelper();

        //calling endList() without a started list must not throw and must not change indentation
        helper.endList();
        assertThat(helper.intend()).isEqualTo(0);

        //calling listItem() without a started list returns null
        assertThat(helper.listItem()).isNull();

        //start and immediately end several lists, checking the depth at each step
        helper.startList(true);
        helper.startList(false);
        helper.startList(true);
        assertThat(helper.intend()).isEqualTo(3);
        assertThat(helper.listItem()).isEqualTo(1);

        helper.endList();
        assertThat(helper.intend()).isEqualTo(2);
        assertThat(helper.listItem()).isNull();

        helper.endList();
        assertThat(helper.intend()).isEqualTo(1);
        assertThat(helper.listItem()).isEqualTo(1);
        assertThat(helper.listItem()).isEqualTo(2);

        helper.endList();
        assertThat(helper.intend()).isEqualTo(0);

        //list stack fully closed: further endList() calls are no-ops
        helper.endList();
        assertThat(helper.intend()).isEqualTo(0);
        assertThat(helper.listItem()).isNull();
    }

    @Test
    public void testReplaceListTagsWithCustomTagsBasic() {
        assertThat(HtmlUtils.replaceListTagsWithCustomTags("<ol><li>Item1</li></ol>"))
                .isEqualTo("<custom-ol><custom-li>Item1</custom-li></custom-ol>");
        assertThat(HtmlUtils.replaceListTagsWithCustomTags("<ul><li>Item1</li><li>Item2</li></ul>"))
                .isEqualTo("<custom-ul><custom-li>Item1</custom-li><custom-li>Item2</custom-li></custom-ul>");

        //text without any list tags must remain unchanged
        assertThat(HtmlUtils.replaceListTagsWithCustomTags("<p>no list here</p>")).isEqualTo("<p>no list here</p>");

        //words containing "ol"/"ul"/"li" as part of an actual tag name must not be touched
        assertThat(HtmlUtils.replaceListTagsWithCustomTags("<table><tr><td>cell</td></tr></table>"))
                .isEqualTo("<table><tr><td>cell</td></tr></table>");
    }

    @Test
    public void testReplaceListTagsWithCustomTagsPreservesAttributesAndCase() {
        //attributes on opening tags must be preserved, matching is case-insensitive
        assertThat(HtmlUtils.replaceListTagsWithCustomTags("<OL start=\"3\"><LI class='x'>Item</LI></OL>"))
                .isEqualTo("<custom-ol start=\"3\"><custom-li class='x'>Item</custom-li></custom-ol>");
        assertThat(HtmlUtils.replaceListTagsWithCustomTags("<Ul id=\"myList\" data-x='1'><Li>a</Li></Ul>"))
                .isEqualTo("<custom-ul id=\"myList\" data-x='1'><custom-li>a</custom-li></custom-ul>");
    }

    @Test
    public void testReplaceListTagsWithCustomTagsNestedLists() {
        //nested/entangled lists: all ol/ul/li tags at any depth must be replaced consistently
        final String html = "<ol><li>one<ul><li>nested1</li><li>nested2</li></ul></li><li>two</li></ol>";
        final String expected = "<custom-ol><custom-li>one<custom-ul><custom-li>nested1</custom-li><custom-li>nested2</custom-li></custom-ul></custom-li><custom-li>two</custom-li></custom-ol>";
        assertThat(HtmlUtils.replaceListTagsWithCustomTags(html)).isEqualTo(expected);
    }

}
