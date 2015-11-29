package cgeo.geocaching;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider;
import cgeo.geocaching.settings.Settings;

import android.annotation.TargetApi;
import android.test.ActivityInstrumentationTestCase2;

@TargetApi(8)
public class SettingsTest extends ActivityInstrumentationTestCase2<MainActivity> {

    public SettingsTest() {
        super(MainActivity.class);
    }

    /**
     * access settings.
     * this should work fine without an exception (once there was an exception because of the empty map file string)
     */
    public static void testSettingsException() {
        // We just want to ensure that it does not throw any exception but we do not know anything about the result
        MapsforgeMapProvider.isValidMapFile(Settings.getMapFile());
    }

    public static void testSettings() {
        // unfortunately, several other tests depend on being a premium member and will fail if run by a basic member
        assertThat(Settings.isGCPremiumMember()).isTrue();
    }

    public static void testDeviceHasNormalLogin() {
        // if the unit tests were interrupted in a previous run, the device might still have the "temporary" login data from the last tests
        assertThat("c:geo".equals(Settings.getUsername())).isFalse();
    }
}
