package cgeo.geocaching.log;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class LogUtilsTest {

    @Test
    public void trimForPublishingRemovesLeadingAndTrailingWhitespace() {
        // #6631: the offline draft is kept verbatim, but publishing trims outer whitespace
        assertThat(LogUtils.trimForPublishing("\n\n\nmy signature")).isEqualTo("my signature");
        assertThat(LogUtils.trimForPublishing("text\n\n")).isEqualTo("text");
    }

    @Test
    public void trimForPublishingKeepsInnerNewlines() {
        assertThat(LogUtils.trimForPublishing("my text\n\n\nmy signature")).isEqualTo("my text\n\n\nmy signature");
    }

    @Test
    public void trimForPublishingHandlesNull() {
        assertThat(LogUtils.trimForPublishing(null)).isEqualTo("");
    }
}
