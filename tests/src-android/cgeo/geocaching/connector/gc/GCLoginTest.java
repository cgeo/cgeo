package cgeo.geocaching.connector.gc;

import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.ui.AvatarUtils;
import cgeo.geocaching.utils.TextUtils;

import junit.framework.TestCase;
import org.apache.commons.lang3.StringUtils;
import static org.assertj.core.api.Java6Assertions.assertThat;

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
        assertThat(homeLocation).isEqualTo(TextUtils.stripHtml(homeLocation));
    }

    public static void testLanguageSwitch() {
        final String userLanguage = GCLogin.getInstance().getWebsiteLanguage();
        assertThat(userLanguage).isNotNull();

        // make sure language is set to english
        if (!userLanguage.equals("en-US")) {
            GCLogin.getInstance().switchToLanguage("en-US");
        }
        assertThat(GCLogin.getInstance().getWebsiteLanguage()).isEqualTo("en-US");

        // test switching
        assertThat(GCLogin.getInstance().switchToLanguage("de-DE")).isTrue();
        assertThat(GCLogin.getInstance().getWebsiteLanguage()).isEqualTo("de-DE");

        // reset to user preference
        GCLogin.getInstance().switchToLanguage(userLanguage);
        assertThat(GCLogin.getInstance().getWebsiteLanguage()).isEqualTo(userLanguage);
    }

    public void testAvatar() {
        instance.resetServerParameters();
        AvatarUtils.changeAvatar(GCConnector.getInstance(), null);
        assertThat(AvatarUtils.getAvatar(GCConnector.getInstance())).isNull();
        instance.getServerParameters(); // avatar should automatically be updated here...
        assertThat(AvatarUtils.getAvatar(GCConnector.getInstance())).isNotNull();
    }

}
