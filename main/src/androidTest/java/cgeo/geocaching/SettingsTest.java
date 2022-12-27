package cgeo.geocaching;

import cgeo.geocaching.settings.Settings;

import android.annotation.TargetApi;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

@TargetApi(8)
public class SettingsTest {

    @Test
    public void testSettings() {
        // unfortunately, several other tests depend on being a premium member and will fail if run by a basic member
        assertThat(Settings.isGCPremiumMember()).isTrue();
    }

    @Test
    public void testDeviceHasNormalLogin() {
        // if the unit tests were interrupted in a previous run, the device might still have the "temporary" login data from the last tests
        assertThat(Settings.getUserName()).isNotEqualTo("c:geo");
    }
}
