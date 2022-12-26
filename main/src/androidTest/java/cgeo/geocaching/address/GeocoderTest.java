package cgeo.geocaching.address;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.utils.Log;

import android.location.Address;
import android.location.Geocoder;

import androidx.test.filters.Suppress;

import java.util.Locale;

import io.reactivex.rxjava3.core.Single;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.data.Offset;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GeocoderTest {

    private static final String TEST_ADDRESS = "46 rue Barrault, Paris, France";
    private static final double TEST_LATITUDE = 48.82677;
    private static final double TEST_LONGITUDE = 2.34644;
    private static final Geopoint TEST_COORDS = new Geopoint(TEST_LATITUDE, TEST_LONGITUDE);
    private static final Offset<Double> TEST_OFFSET = Offset.offset(0.00050);

    @Test
    public void testAndroidGeocoder() {
        final Locale locale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            // Some emulators don't have access to Google Android geocoder
            if (Geocoder.isPresent()) {
                final AndroidGeocoder geocoder = new AndroidGeocoder(CgeoApplication.getInstance());
                assertGeocoder(geocoder.getFromLocationName(TEST_ADDRESS).firstOrError(), "Android", true);
                assertGeocoder(geocoder.getFromLocation(TEST_COORDS), "Android reverse", true);
            } else {
                Log.i("not testing absent Android geocoder");
            }
        } finally {
            Locale.setDefault(locale);
        }
    }

    @Suppress // Do not run test on MapQuest, quotas are very low and exhaust easily.
    @Test
    public void testMapQuestGeocoder() {
        final Locale locale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            assertGeocoder(MapQuestGeocoder.getFromLocationName(TEST_ADDRESS).firstOrError(), "MapQuest", true);
            assertGeocoder(MapQuestGeocoder.getFromLocation(TEST_COORDS), "MapQuest reverse", true);
        } finally {
            Locale.setDefault(locale);
        }
    }

    private void assertGeocoder(final Single<Address> addressObservable, final String geocoder, final boolean withAddress) {
        try {
            final Address address = addressObservable.blockingGet();
            assertThat(address.getLatitude()).as(describe("latitude", geocoder)).isCloseTo(TEST_LATITUDE, TEST_OFFSET);
            assertThat(address.getLongitude()).as(describe("longitude", geocoder)).isCloseTo(TEST_LONGITUDE, TEST_OFFSET);
            if (withAddress) {
                assertThat(StringUtils.lowerCase(address.getAddressLine(0))).as(describe("street address", geocoder)).contains("barrault");
                assertThat(address.getLocality()).as(describe("locality", geocoder)).isEqualTo("Paris");
                assertThat(address.getCountryCode()).as(describe("country code", geocoder)).isEqualTo("FR");
                // don't assert on country name, as this can be localized, e.g. with the mapquest geocoder
            }
        } catch (RuntimeException re) {
            //protect against known Android bug (as of July 2020). For more information see:
            // https://stackoverflow.com/questions/47331480/geocoder-getfromlocation-grpc-failed-on-android-real-device?rq=1
            // https://issuetracker.google.com/issues/64418751?pli=1
            // https://issuetracker.google.com/issues/64247769
            //Check may be removed when this bug is resolved
            if (re.getMessage() != null && re.getMessage().endsWith("grpc failed")) {
                Log.i("AndroidGeocoder test failed with known bug 'grpc failed', is ignored");
            } else {
                throw re;
            }
        }
    }

    private static String describe(final String field, final String geocoder) {
        return field + " for " + geocoder + " .geocoder";
    }

}
