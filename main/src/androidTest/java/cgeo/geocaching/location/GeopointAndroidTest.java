package cgeo.geocaching.location;

import android.os.Bundle;

import junit.framework.TestCase;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GeopointAndroidTest extends TestCase {

    public static void testParcelable() {
        final Geopoint gp = new Geopoint(1.2, 3.4);
        final String key = "geopoint";
        final Bundle bundle = new Bundle();
        bundle.putParcelable(key, gp);
        assertThat(bundle.<Geopoint>getParcelable(key)).isEqualTo(gp);
    }

}
