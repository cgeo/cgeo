package cgeo.geocaching.utils;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ProcessUtilsTest {

    @Test
    public void testIsInstalled() {
        assertThat(ProcessUtils.isInstalled("com.android.settings")).isTrue();
    }

    @Test
    public void testIsInstalledNotLaunchable() {
        final String packageName = "com.android.systemui";
        assertThat(ProcessUtils.isInstalled(packageName)).isTrue();
        assertThat(ProcessUtils.isLaunchable(packageName)).isFalse();
    }

    @Test
    public void testIsLaunchable() {
        assertThat(ProcessUtils.isLaunchable("com.android.settings")).isTrue();
    }

}
