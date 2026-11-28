package cgeo.geocaching.utils.html;

import android.text.Layout;
import android.text.Spannable;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

/** Lists in rendered HTML (#11992, #12527). Instrumented because the result is built by Android's Html converter. */
public class HtmlListRenderingTest {

    private static Spannable render(final String html) {
        return HtmlUtils.renderHtml(html, HtmlUtils.DUMMY_IMAGE_GETTER).first;
    }

    private static List<ListItemSpan> items(final Spannable text) {
        final ListItemSpan[] spans = text.getSpans(0, text.length(), ListItemSpan.class);
        Arrays.sort(spans, Comparator.comparingInt(text::getSpanStart));
        return Arrays.asList(spans);
    }

    private static List<String> numbers(final Spannable text) {
        return items(text).stream().map(ListItemSpan::getNumber).collect(Collectors.toList());
    }

    private static String itemText(final Spannable text, final ListItemSpan item) {
        return text.subSequence(text.getSpanStart(item), text.getSpanEnd(item)).toString().trim();
    }

    @Test
    public void orderedListIsNumbered() {
        final Spannable text = render("<ol><li>first</li><li>second</li></ol>");
        assertThat(numbers(text)).containsExactly("1.", "2.");
        assertThat(text.getSpans(0, text.length(), BulletSpan.class)).isEmpty();
    }

    @Test
    public void unorderedListGetsBullets() {
        final Spannable text = render("<ul><li>first</li><li>second</li></ul>");
        assertThat(numbers(text)).containsExactly(null, null);
        assertThat(text.getSpans(0, text.length(), BulletSpan.class)).isEmpty();
    }

    @Test
    public void oneLineBreakPerItem() {
        assertThat(render("<ol><li>first</li><li>second</li></ol>").toString()).contains("first\nsecond").doesNotContain("first\n\nsecond");
        assertThat(render("<ul><li>first</li><li>second</li></ul>").toString()).contains("first\nsecond").doesNotContain("first\n\nsecond");
    }

    @Test
    public void topLevelListIsSeparatedFromSurroundingTextButNestedListIsNot() {
        assertThat(lines("text<ol><li>a</li><li>b</li></ol>more")).contains("text\n\na\nb\n\nmore");
        assertThat(lines("<ul><li>a</li></ul><ol><li>b</li></ol>")).contains("a\n\nb");
        assertThat(lines("<ul><li>one<ul><li>x</li></ul></li></ul>")).contains("one\nx").doesNotContain("one\n\nx");
    }

    // ignores the trailing spaces that jsoup's pretty printing in renderHtml leaves before line breaks
    private static String lines(final String html) {
        return render(html).toString().replaceAll("[ \\t]+\\n", "\n").replaceAll("\\n[ \\t]+", "\n");
    }

    @Test
    public void lineBreakInsideItemStaysInsideTheItem() {
        // GC7KPHM: all lines of an item must get the indent, so the item span must cover the text after <br>
        final Spannable text = render("<ul><li>line1<br />line2</li><li>next</li></ul>");
        final List<ListItemSpan> items = items(text);
        assertThat(items).hasSize(2);
        assertThat(itemText(text, items.get(0))).matches("line1\\s*\\nline2");
    }

    @Test
    public void nestedAndMixedListsKeepTheirOwnNumbering() {
        final Spannable text = render("<ol><li>one<ul><li>a</li><li>b</li></ul></li><li>two<ol><li>x</li><li>y</li></ol></li><li>three</li></ol>");
        assertThat(numbers(text)).containsExactly("1.", null, null, "2.", "1.", "2.", "3.");
        // the outer item contains the nested ones -> margins add up -> nested items are indented further
        final List<ListItemSpan> items = items(text);
        assertThat(text.getSpanEnd(items.get(0))).isGreaterThanOrEqualTo(text.getSpanEnd(items.get(2)));
    }

    @Test
    public void numberedItemsShareTheIndentOfTheWidestNumber() {
        final StringBuilder html = new StringBuilder("<ol>");
        for (int i = 1; i <= 120; i++) {
            html.append("<li>i</li>");
        }
        final List<ListItemSpan> items = items(render(html.append("</ol>").toString()));
        assertThat(items).hasSize(120);
        assertThat(items.get(0).getLeadingMargin(true)).isEqualTo(items.get(119).getLeadingMargin(true));
        assertThat(items.get(0).getLeadingMargin(true)).isGreaterThan(items(render("<ol><li>i</li></ol>")).get(0).getLeadingMargin(true));
    }

    @Test
    public void outerItemComesBeforeNestedItemInSpanOrder() {
        // Android draws the leading margins in span order, so a nested marker is only drawn at its own indent if the outer item comes first
        final Spannable text = render("<ol><li>one<ul><li>a</li></ul></li></ol>");
        final ListItemSpan[] unsorted = text.getSpans(0, text.length(), ListItemSpan.class);
        assertThat(unsorted).hasSize(2);
        assertThat(unsorted[0].getNumber()).isEqualTo("1.");
        assertThat(unsorted[1].getNumber()).isNull();
    }

    @Test
    public void separateListsStartAtOne() {
        assertThat(numbers(render("<ol><li>a</li><li>b</li></ol><p>text</p><ol><li>c</li></ol>"))).containsExactly("1.", "2.", "1.");
    }

    @Test
    public void listItemStylingIsKept() {
        // the styles Android's Html applies to li (startLi -> startBlockElement + startCssStyle), see #18454
        for (final String list : new String[] {"ul", "ol"}) {
            final Spannable text = render("<" + list + "><li style=\"color:#ff0000\">red</li><li style=\"background-color:#00ff00\">green</li>"
                    + "<li style=\"text-decoration:line-through\">struck</li><li style=\"text-align:center\">centered</li></" + list + ">");
            assertThat(items(text)).as(list).hasSize(4);
            assertThat(styledText(text, ForegroundColorSpan.class)).as(list).isEqualTo("red");
            assertThat(styledText(text, BackgroundColorSpan.class)).as(list).isEqualTo("green");
            assertThat(styledText(text, StrikethroughSpan.class)).as(list).isEqualTo("struck");
            final AlignmentSpan[] alignments = text.getSpans(0, text.length(), AlignmentSpan.class);
            assertThat(alignments).as(list).hasSize(1);
            assertThat(alignments[0].getAlignment()).isEqualTo(Layout.Alignment.ALIGN_CENTER);
            assertThat(text.subSequence(text.getSpanStart(alignments[0]), text.getSpanEnd(alignments[0])).toString().trim()).isEqualTo("centered");
        }
    }

    private static String styledText(final Spannable text, final Class<?> type) {
        final Object[] spans = text.getSpans(0, text.length(), type);
        assertThat(spans).hasSize(1);
        return text.subSequence(text.getSpanStart(spans[0]), text.getSpanEnd(spans[0])).toString().trim();
    }

    @Test
    public void textWithoutListsIsUnchanged() {
        assertThat(render("<p>just <b>text</b></p>").toString().trim()).isEqualTo("just text");
    }
}
