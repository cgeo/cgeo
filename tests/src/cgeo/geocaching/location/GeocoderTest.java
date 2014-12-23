package cgeo.geocaching.location;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.CGeoTestCase;
import cgeo.geocaching.CgeoApplication;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.data.Offset;

import rx.Observable;

import android.annotation.TargetApi;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;

public class GeocoderTest extends CGeoTestCase {

    private static final String TEST_ADDRESS = "46 rue Barrault, Paris, France";
    private static final double TEST_LATITUDE = 48.82677;
    private static final double TEST_LONGITUDE = 2.34644;
    private static final Offset<Double> TEST_OFFSET = Offset.offset(0.00050);

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static void testAndroidGeocoder() {
        // Some emulators don't have access to Google Android geocoder
        if (Geocoder.isPresent()) {
            testGeocoder(new AndroidGeocoder(CgeoApplication.getInstance()).getFromLocationName(TEST_ADDRESS), "Android");
        }
    }

    public static void testGCGeocoder() {
        testGeocoder(GCGeocoder.getFromLocationName(TEST_ADDRESS), "GC");
    }

    public static void testMapQuestGeocoder() {
        testGeocoder(MapQuestGeocoder.getFromLocationName(TEST_ADDRESS), "MapQuest");
    }

    public static void testGeocoder(final Observable<Address> addressObservable, final String geocoder) {
        final Address address = addressObservable.toBlocking().first();
        assertThat(address.getLatitude()).as("latitude for " + geocoder + " geocoder").isCloseTo(TEST_LATITUDE, TEST_OFFSET);
        assertThat(address.getLongitude()).as("longitude for " + geocoder + " geocoder").isCloseTo(TEST_LONGITUDE, TEST_OFFSET);
        assertThat(StringUtils.lowerCase(address.getAddressLine(0))).as("street address for " + geocoder + " geocoder").startsWith("46 rue barrault");
    }

}
