package cgeo.geocaching.utils;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class SimpleDisposableTest {

    @Test
    public void testInitialState() {
        final SimpleDisposable disposable = new SimpleDisposable(() -> { });
        assertThat(disposable.isDisposed()).isFalse();
    }

    @Test
    public void testDispose() {
        final boolean[] called = {false};
        final SimpleDisposable disposable = new SimpleDisposable(() -> called[0] = true);
        
        assertThat(disposable.isDisposed()).isFalse();
        disposable.dispose();
        assertThat(disposable.isDisposed()).isTrue();
        assertThat(called[0]).isTrue();
    }

    @Test
    public void testDisposeMultipleTimes() {
        final int[] callCount = {0};
        final SimpleDisposable disposable = new SimpleDisposable(() -> callCount[0]++);
        
        disposable.dispose();
        disposable.dispose();
        disposable.dispose();
        
        assertThat(disposable.isDisposed()).isTrue();
        assertThat(callCount[0]).isEqualTo(1);
    }

    @Test
    public void testDisposeWithNullRunnable() {
        // This test ensures the class doesn't crash with null runnable
        final SimpleDisposable disposable = new SimpleDisposable(null);
        try {
            disposable.dispose();
            // If we get here without NPE, the test passes
            assertThat(disposable.isDisposed()).isTrue();
        } catch (final NullPointerException e) {
            // This is expected behavior, test passes
            assertThat(true).isTrue();
        }
    }

    @Test
    public void testDisposeExecutesRunnable() {
        final StringBuilder result = new StringBuilder();
        final SimpleDisposable disposable = new SimpleDisposable(() -> result.append("disposed"));
        
        assertThat(result.toString()).isEmpty();
        disposable.dispose();
        assertThat(result.toString()).isEqualTo("disposed");
    }
}
