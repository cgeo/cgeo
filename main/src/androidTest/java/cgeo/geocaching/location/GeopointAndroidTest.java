package cgeo.geocaching.location;

import android.os.Bundle;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GeopointAndroidTest {

    @Test
    public void testParcelable() {
        final Geopoint gp = new Geopoint(1.2, 3.4);
        final String key = "geopoint";
        final Bundle bundle = new Bundle();
        bundle.putParcelable(key, gp);
        assertThat(bundle.<Geopoint>getParcelable(key)).isEqualTo(gp);
    }

}
