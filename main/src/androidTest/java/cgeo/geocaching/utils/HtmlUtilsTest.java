package cgeo.geocaching.utils;

import junit.framework.TestCase;
import org.apache.commons.lang3.StringUtils;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class HtmlUtilsTest extends TestCase {

    public static void testExtractText() {
        assertThat(HtmlUtils.extractText(null)).isEqualTo(StringUtils.EMPTY);
        assertThat(HtmlUtils.extractText(StringUtils.EMPTY)).isEqualTo(StringUtils.EMPTY);
        assertThat(HtmlUtils.extractText("   ")).isEqualTo(StringUtils.EMPTY);
        assertThat(HtmlUtils.extractText("<b>bold</b>")).isEqualTo("bold");
    }

    public static void testRemoveExtraParagraph() {
        assertThat(HtmlUtils.removeExtraTags("<p></p>")).isEmpty();
        assertThat(HtmlUtils.removeExtraTags("<p>Test</p>")).isEqualTo("Test");
        assertThat(HtmlUtils.removeExtraTags("<p>1</p><p>2</p>")).isEqualTo("<p>1</p><p>2</p>");
        assertThat(HtmlUtils.removeExtraTags("<p><span>content</span></p>")).isEqualTo("content");
    }
}
