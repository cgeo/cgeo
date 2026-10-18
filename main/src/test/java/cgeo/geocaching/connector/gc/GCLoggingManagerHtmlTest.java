package cgeo.geocaching.connector.gc;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for {@link GCLoggingManager#htmlLogToEditableText(String)} — the jsoup-based
 * conversion of the HTML a log comes back as from geocaching.com into editable markdown (#16147).
 * Pure (jsoup is plain Java), so no emulator is needed and the conversion is tested directly on the
 * intermediate HTML forms gc.com actually returns.
 */
public class GCLoggingManagerHtmlTest {

    @Test
    public void singleHeadingKeepsDocumentedForm() {
        assertThat(GCLoggingManager.htmlLogToEditableText("<h1>Test</h1>")).isEqualTo("# Test #");
    }

    @Test
    public void consecutiveHeadingsDoNotMerge() {
        // gc.com returns them adjacent, without separating whitespace
        assertThat(GCLoggingManager.htmlLogToEditableText("<h1>H1</h1><h1>H2</h1><h1>H3</h1>"))
                .isEqualTo("# H1 #\n\n# H2 #\n\n# H3 #");
    }

    @Test
    public void headingFollowedByTextHasNoTrailingSpace() {
        assertThat(GCLoggingManager.htmlLogToEditableText("<h1>H1</h1><p>test</p><h1>H2</h1><p>test</p><h1>H3</h1>"))
                .isEqualTo("# H1 #\n\ntest\n\n# H2 #\n\ntest\n\n# H3 #");
    }

    @Test
    public void h2AndH3KeepDocumentedForm() {
        assertThat(GCLoggingManager.htmlLogToEditableText("<h2>H2</h2><h3>H3</h3>"))
                .isEqualTo("## H2 ##\n\n### H3 ###");
    }

    @Test
    public void boldItalicAndLink() {
        assertThat(GCLoggingManager.htmlLogToEditableText("<p><strong>b</strong> and <em>i</em></p>"))
                .isEqualTo("**b** and *i*");
        assertThat(GCLoggingManager.htmlLogToEditableText("<p><a href=\"https://coord.info/GC1\">link</a></p>"))
                .isEqualTo("[link](https://coord.info/GC1)");
    }

    @Test
    public void headingWithInlineFormatting() {
        assertThat(GCLoggingManager.htmlLogToEditableText("<h1><strong>Bold</strong> title</h1>"))
                .isEqualTo("# **Bold** title #");
    }

    @Test
    public void listAndHorizontalRule() {
        assertThat(GCLoggingManager.htmlLogToEditableText("<ul><li>a</li><li>b</li></ul>"))
                .isEqualTo("* a\n* b");
        assertThat(GCLoggingManager.htmlLogToEditableText("<p>above</p><hr /><p>below</p>"))
                .isEqualTo("above\n\n---\n\nbelow");
    }

    @Test
    public void orderedListKeepsNumbering() {
        // gc.com renders "1." input as <ol>; keep the numbering so the round-trip stays idempotent
        assertThat(GCLoggingManager.htmlLogToEditableText("<ol><li>a</li><li>b</li></ol>"))
                .isEqualTo("1. a\n2. b");
    }

    @Test
    public void lineBreakBecomesNewline() {
        // <br> -> plain newline; gc.com renders it via white-space:pre-line, so the break round-trips without accumulating backslashes
        assertThat(GCLoggingManager.htmlLogToEditableText("<p>line1<br />line2</p>"))
                .isEqualTo("line1\nline2");
    }

    @Test
    public void blockquoteBecomesQuotePrefix() {
        // GC editor "Quote" button -> <blockquote> -> CommonMark "> " so the round-trip keeps the quote
        assertThat(GCLoggingManager.htmlLogToEditableText("<blockquote><p>quote</p></blockquote>"))
                .isEqualTo("> quote");
        assertThat(GCLoggingManager.htmlLogToEditableText("<blockquote><p>l1</p><p>l2</p></blockquote>"))
                .isEqualTo("> l1\n> l2");
    }

    @Test
    public void emojiIsPreserved() {
        assertThat(GCLoggingManager.htmlLogToEditableText("<p>nice \uD83D\uDE42 cache</p>"))
                .isEqualTo("nice \uD83D\uDE42 cache");
    }

    @Test
    public void allEditorToolbarFeaturesRoundTrip() {
        // every formatting the gc.com log editor toolbar offers: Headers, Bold, Italic, Hyperlink, Emojis, Quote, horizontal rule
        final String html = "<h1>Title</h1>"
                + "<p><strong>bold</strong> <em>italic</em> <a href=\"https://coord.info/GC1\">link</a> \uD83D\uDE42</p>"
                + "<blockquote><p>quote</p></blockquote>"
                + "<hr />";
        assertThat(GCLoggingManager.htmlLogToEditableText(html))
                .isEqualTo("# Title #\n\n**bold** *italic* [link](https://coord.info/GC1) \uD83D\uDE42\n\n> quote\n\n---");
    }

    @Test
    public void resultHasNoTrailingWhitespaceAndCollapsesBlankLines() {
        final String out = GCLoggingManager.htmlLogToEditableText("<h1>H1</h1>\n\n<p>x</p>\n\n<h2>H2</h2>");
        assertThat(out).doesNotContainPattern("(?m)[ \\t]+$");
        assertThat(out).doesNotContain("\n\n\n");
        assertThat(out).isEqualTo("# H1 #\n\nx\n\n## H2 ##");
    }

    @Test
    public void plainTextWithoutBlockTagsIsPreserved() {
        assertThat(GCLoggingManager.htmlLogToEditableText("just <strong>a</strong> note"))
                .isEqualTo("just **a** note");
    }
}
