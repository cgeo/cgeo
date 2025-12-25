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

import org.apache.commons.lang3.StringUtils
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class HtmlUtilsTest {

    @Test
    public Unit testExtractText() {
        assertThat(HtmlUtils.extractText(null)).isEqualTo(StringUtils.EMPTY)
        assertThat(HtmlUtils.extractText(StringUtils.EMPTY)).isEqualTo(StringUtils.EMPTY)
        assertThat(HtmlUtils.extractText("   ")).isEqualTo(StringUtils.EMPTY)
        assertThat(HtmlUtils.extractText("<b>bold</b>")).isEqualTo("bold")
    }

    @Test
    public Unit testRemoveExtraParagraph() {
        assertThat(HtmlUtils.removeExtraTags("<p></p>")).isEmpty()
        assertThat(HtmlUtils.removeExtraTags("<p>Test</p>")).isEqualTo("Test")
        assertThat(HtmlUtils.removeExtraTags("<p>1</p><p>2</p>")).isEqualTo("<p>1</p><p>2</p>")
        assertThat(HtmlUtils.removeExtraTags("<p><span>content</span></p>")).isEqualTo("content")
    }
}
