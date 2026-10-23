package cgeo.geocaching;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class RefreshCacheStatusTest {

    @Test
    public void successfulRefreshKeepsCompletionText() {
        final RefreshCacheStatus status = new RefreshCacheStatus();

        assertThat(status.getCompletionText("refreshed")).isEqualTo("refreshed");
    }

    @Test
    public void failedRefreshSuppressesCompletionText() {
        final RefreshCacheStatus status = new RefreshCacheStatus();

        status.markFailed();

        assertThat(status.getCompletionText("refreshed")).isNull();
    }

    @Test
    public void multipleErrorsAndCompletionsRemainFailed() {
        final RefreshCacheStatus status = new RefreshCacheStatus();

        status.markFailed();
        status.markFailed();

        assertThat(status.getCompletionText("first completion")).isNull();
        assertThat(status.getCompletionText("second completion")).isNull();
    }
}
