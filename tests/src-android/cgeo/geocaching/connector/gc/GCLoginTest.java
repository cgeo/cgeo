package cgeo.geocaching.connector.gc;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.location.Geopoint;

import android.test.suitebuilder.annotation.Suppress;
import android.text.Html;

import junit.framework.TestCase;
import org.apache.commons.lang3.StringUtils;

public class GCLoginTest extends TestCase {

    private final GCLogin instance = GCLogin.getInstance();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        assertThat(instance.login()).isEqualTo(StatusCode.NO_ERROR);
    }

    private static String blockingHomeLocation() {
        return GCLogin.retrieveHomeLocation().blockingGet();
    }

    public static void testRetrieveHomeLocation() {
        assertThat(StringUtils.isNotBlank(blockingHomeLocation())).isTrue();
    }

    public static void testValidHomeLocation() {
        assertThat(new Geopoint(blockingHomeLocation())).isInstanceOf(Geopoint.class);
    }

    public static void testNoHtmlInHomeLocation() {
        final String homeLocation = blockingHomeLocation();
        assertThat(homeLocation).isEqualTo(Html.fromHtml(homeLocation).toString());
    }

    @Suppress // It currently fails on CI
    public void testAvatar() {
        assertThat(instance.downloadAvatar()).isNotNull();
    }

}
