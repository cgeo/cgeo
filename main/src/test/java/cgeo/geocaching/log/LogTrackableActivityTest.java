package cgeo.geocaching.log;

import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.StatusCode;

import android.app.Activity;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class LogTrackableActivityTest {

    @Test
    public void successfulPostReturnsOk() {
        assertThat(LogTrackableActivity.getResultCodeForPost(LogResult.ok("log-id")))
                .isEqualTo(Activity.RESULT_OK);
    }

    @Test
    public void failedPostRemainsCanceled() {
        assertThat(LogTrackableActivity.getResultCodeForPost(LogResult.error(StatusCode.LOG_POST_ERROR)))
                .isEqualTo(Activity.RESULT_CANCELED);
    }
}
