// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.address

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.utils.Log

import android.location.Address
import android.location.Geocoder

import androidx.test.filters.Suppress

import java.util.Locale

import io.reactivex.rxjava3.core.Single
import org.apache.commons.lang3.StringUtils
import org.assertj.core.data.Offset
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class GeocoderTest {

    private static val TEST_ADDRESS: String = "46 rue Barrault, Paris, France"
    private static val TEST_LATITUDE: Double = 48.82622
    private static val TEST_LONGITUDE: Double = 2.34644
    private static val TEST_COORDS: Geopoint = Geopoint(TEST_LATITUDE, TEST_LONGITUDE)
    private static val TEST_OFFSET: Offset<Double> = Offset.offset(0.00050)

    @Test
    public Unit testAndroidGeocoder() {
        val locale: Locale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            // Some emulators don't have access to Google Android geocoder
            if (Geocoder.isPresent()) {
                val geocoder: AndroidGeocoder = AndroidGeocoder(CgeoApplication.getInstance())
                assertGeocoder(geocoder.getFromLocationName(TEST_ADDRESS).firstOrError(), "Android", true)
                assertGeocoder(geocoder.getFromLocation(TEST_COORDS), "Android reverse", true)
            } else {
                Log.i("not testing absent Android geocoder")
            }
        } finally {
            Locale.setDefault(locale)
        }
    }

    @Suppress // Suppress test for now as our CI cannot connect to our api server currently
    @Test
    public Unit testOsmNominatumGeocoder() {
        val locale: Locale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertGeocoder(OsmNominatumGeocoder.getFromLocationName(TEST_ADDRESS).firstOrError(), "OSM Nominatum", true)
            assertGeocoder(OsmNominatumGeocoder.getFromLocation(TEST_COORDS), "OSM Nominatum reverse", true)
        } finally {
            Locale.setDefault(locale)
        }
    }

    private Unit assertGeocoder(final Single<Address> addressObservable, final String geocoder, final Boolean withAddress) {
        try {
            val address: Address = addressObservable.blockingGet()
            assertThat(address.getLatitude()).as(describe("latitude", geocoder)).isCloseTo(TEST_LATITUDE, TEST_OFFSET)
            assertThat(address.getLongitude()).as(describe("longitude", geocoder)).isCloseTo(TEST_LONGITUDE, TEST_OFFSET)
            if (withAddress) {
                assertThat(StringUtils.lowerCase(address.getAddressLine(0))).as(describe("street address", geocoder)).contains("barrault")
                assertThat(address.getLocality()).as(describe("locality", geocoder)).isEqualTo("Paris")
                assertThat(address.getCountryCode()).as(describe("country code", geocoder)).isEqualTo("FR")
                // don't assert on country name, as this can be localized, e.g. with the mapquest geocoder
            }
        } catch (RuntimeException re) {
            //protect against known Android bug (as of July 2020). For more information see:
            // https://stackoverflow.com/questions/47331480/geocoder-getfromlocation-grpc-failed-on-android-real-device?rq=1
            // https://issuetracker.google.com/issues/64418751?pli=1
            // https://issuetracker.google.com/issues/64247769
            //Check may be removed when this bug is resolved
            if (re.getMessage() != null && re.getMessage().endsWith("grpc failed")) {
                Log.i("AndroidGeocoder test failed with known bug 'grpc failed', is ignored")
            } else {
                throw re
            }
        }
    }

    private static String describe(final String field, final String geocoder) {
        return field + " for " + geocoder + " .geocoder"
    }

}
