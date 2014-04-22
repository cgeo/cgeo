package cgeo.geocaching.connector.trackable;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Trackable;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;

public class GeokretyParserTest extends AbstractResourceInstrumentationTestCase {

    public void testParse() {
        Trackable trackable = GeokretyParser.parse(getFileContent(R.raw.geokret141_xml));
        assertThat(trackable).isNotNull();
        assertThat(trackable.getName()).isEqualTo("WeltenbummlerKret");
        assertThat(trackable.getGeocode()).isEqualTo("GK008D");
        assertThat(trackable.getDistance()).isEqualTo(2235f);
        assertThat(trackable.getType()).isEqualTo(CgeoApplication.getInstance().getString(cgeo.geocaching.R.string.geokret_type_traditional));
    }

}
