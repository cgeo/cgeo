package cgeo.geocaching.service;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class LogPostingServiceTest {

    @Test
    public void successfulEditPreservesOfflineLog() {
        final AtomicInteger clearCalls = new AtomicInteger();

        LogPostingService.clearOfflineLogAfterSuccess(true, clearCalls::incrementAndGet);

        assertThat(clearCalls).hasValue(0);
    }

    @Test
    public void successfulCreateClearsOfflineLog() {
        final AtomicInteger clearCalls = new AtomicInteger();

        LogPostingService.clearOfflineLogAfterSuccess(false, clearCalls::incrementAndGet);

        assertThat(clearCalls).hasValue(1);
    }
}
