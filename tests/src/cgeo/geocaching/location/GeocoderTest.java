package cgeo.geocaching.location;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.CGeoTestCase;
import cgeo.geocaching.CgeoApplication;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.data.Offset;

import rx.Observable;

import android.location.Address;
import android.test.suitebuilder.annotation.Suppress;

public class GeocoderTest extends CGeoTestCase {

    private static final String TEST_ADDRESS = "46 rue Barrault, Paris, France";
    private static final double TEST_LATITUDE = 48.82677;
    private static final double TEST_LONGITUDE = 2.34644;
    private static final Offset<Double> TEST_OFFSET = Offset.offset(0.00010);

    @Suppress // Some emulators don't have access to Google Android geocoder
    public void testAndroidGeocoder() {
        testGeocoder(new AndroidGeocoder(CgeoApplication.getInstance()).getFromLocationName(TEST_ADDRESS), "Android");
    }

    public void testGCGeocoder() {
        testGeocoder(GCGeocoder.getFromLocationName(TEST_ADDRESS), "GC");
    }

    public void testMapQuestGeocoder() {
        testGeocoder(MapQuestGeocoder.getFromLocationName(TEST_ADDRESS), "MapQuest");
    }

    public static void testGeocoder(final Observable<Address> addressObservable, final String geocoder) {
        final Address address = addressObservable.toBlocking().first();
        assertThat(address.getLatitude()).as("latitude for " + geocoder + " geocoder").isCloseTo(TEST_LATITUDE, TEST_OFFSET);
        assertThat(address.getLongitude()).as("longitude for " + geocoder + " geocoder").isCloseTo(TEST_LONGITUDE, TEST_OFFSET);
        assertThat(StringUtils.lowerCase(address.getAddressLine(0))).as("street address for " + geocoder + " geocoder").startsWith("46 rue barrault");
    }

}
