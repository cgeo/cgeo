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
    public void markdownSingleHeadingKeepsDocumentedForm() {
        assertThat(HtmlUtils.htmlToGCMarkdown("<h1>Test</h1>")).isEqualTo("# Test #");
    }

    @Test
    public void markdownConsecutiveHeadingsDoNotMerge() {
        // gc.com returns them adjacent, without separating whitespace
        assertThat(HtmlUtils.htmlToGCMarkdown("<h1>H1</h1><h1>H2</h1><h1>H3</h1>"))
                .isEqualTo("# H1 #\n\n# H2 #\n\n# H3 #");
    }

    @Test
    public void markdownHeadingFollowedByTextHasNoTrailingSpace() {
        assertThat(HtmlUtils.htmlToGCMarkdown("<h1>H1</h1><p>test</p><h1>H2</h1><p>test</p><h1>H3</h1>"))
                .isEqualTo("# H1 #\n\ntest\n\n# H2 #\n\ntest\n\n# H3 #");
    }

    @Test
    public void markdownH2AndH3KeepDocumentedForm() {
        assertThat(HtmlUtils.htmlToGCMarkdown("<h2>H2</h2><h3>H3</h3>"))
                .isEqualTo("## H2 ##\n\n### H3 ###");
    }

    @Test
    public void markdownBoldItalicAndLink() {
        assertThat(HtmlUtils.htmlToGCMarkdown("<p><strong>b</strong> and <em>i</em></p>"))
                .isEqualTo("**b** and *i*");
        assertThat(HtmlUtils.htmlToGCMarkdown("<p><a href=\"https://coord.info/GC1\">link</a></p>"))
                .isEqualTo("[link](https://coord.info/GC1)");
    }

    @Test
    public void markdownHeadingWithInlineFormatting() {
        assertThat(HtmlUtils.htmlToGCMarkdown("<h1><strong>Bold</strong> title</h1>"))
                .isEqualTo("# **Bold** title #");
    }

    @Test
    public void markdownListAndHorizontalRule() {
        assertThat(HtmlUtils.htmlToGCMarkdown("<ul><li>a</li><li>b</li></ul>"))
                .isEqualTo("* a\n* b");
        assertThat(HtmlUtils.htmlToGCMarkdown("<p>above</p><hr /><p>below</p>"))
                .isEqualTo("above\n\n---\n\nbelow");
    }

    @Test
    public void markdownOrderedListKeepsNumbering() {
        // gc.com renders "1." input as <ol>; keep the numbering so the round-trip stays idempotent
        assertThat(HtmlUtils.htmlToGCMarkdown("<ol><li>a</li><li>b</li></ol>"))
                .isEqualTo("1. a\n2. b");
    }

    @Test
    public void markdownLineBreakBecomesNewline() {
        // <br> -> plain newline; gc.com renders it via white-space:pre-line, so the break round-trips without accumulating backslashes
        assertThat(HtmlUtils.htmlToGCMarkdown("<p>line1<br />line2</p>"))
                .isEqualTo("line1\nline2");
    }

    @Test
    public void markdownBlockquoteBecomesQuotePrefix() {
        // GC editor "Quote" button -> <blockquote> -> CommonMark "> " so the round-trip keeps the quote
        assertThat(HtmlUtils.htmlToGCMarkdown("<blockquote><p>quote</p></blockquote>"))
                .isEqualTo("> quote");
        assertThat(HtmlUtils.htmlToGCMarkdown("<blockquote><p>l1</p><p>l2</p></blockquote>"))
                .isEqualTo("> l1\n> l2");
    }

    @Test
    public void markdownEmojiIsPreserved() {
        assertThat(HtmlUtils.htmlToGCMarkdown("<p>nice \uD83D\uDE42 cache</p>"))
                .isEqualTo("nice \uD83D\uDE42 cache");
    }

    @Test
    public void markdownAllEditorToolbarFeaturesRoundTrip() {
        // every formatting the gc.com log editor toolbar offers: Headers, Bold, Italic, Hyperlink, Emojis, Quote, horizontal rule
        final String html = "<h1>Title</h1>"
                + "<p><strong>bold</strong> <em>italic</em> <a href=\"https://coord.info/GC1\">link</a> \uD83D\uDE42</p>"
                + "<blockquote><p>quote</p></blockquote>"
                + "<hr />";
        assertThat(HtmlUtils.htmlToGCMarkdown(html))
                .isEqualTo("# Title #\n\n**bold** *italic* [link](https://coord.info/GC1) \uD83D\uDE42\n\n> quote\n\n---");
    }

    @Test
    public void markdownHasNoTrailingWhitespaceAndCollapsesBlankLines() {
        final String out = HtmlUtils.htmlToGCMarkdown("<h1>H1</h1>\n\n<p>x</p>\n\n<h2>H2</h2>");
        assertThat(out).doesNotContainPattern("(?m)[ \\t]+$");
        assertThat(out).doesNotContain("\n\n\n");
        assertThat(out).isEqualTo("# H1 #\n\nx\n\n## H2 ##");
    }

    @Test
    public void markdownPlainTextWithoutBlockTagsIsPreserved() {
        assertThat(HtmlUtils.htmlToGCMarkdown("just <strong>a</strong> note"))
                .isEqualTo("just **a** note");
    }

}
