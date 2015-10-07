package cgeo.geocaching.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.StringUtils;

import junit.framework.TestCase;

public class HtmlUtilsTest extends TestCase {

    public static void testExtractText() {
        assertThat(HtmlUtils.extractText(null)).isEqualTo(StringUtils.EMPTY);
        assertThat(HtmlUtils.extractText(StringUtils.EMPTY)).isEqualTo(StringUtils.EMPTY);
        assertThat(HtmlUtils.extractText("   ")).isEqualTo(StringUtils.EMPTY);
        assertThat(HtmlUtils.extractText("<b>bold</b>")).isEqualTo("bold");
    }

    public static void testRemoveExtraParagraph() {
        assertThat(HtmlUtils.removeExtraParagraph("<p></p>")).isEqualTo("");
        assertThat(HtmlUtils.removeExtraParagraph("<p>Test</p>")).isEqualTo("Test");
        assertThat(HtmlUtils.removeExtraParagraph("<p>1</p><p>2</p>")).isEqualTo("<p>1</p><p>2</p>");
    }
}
