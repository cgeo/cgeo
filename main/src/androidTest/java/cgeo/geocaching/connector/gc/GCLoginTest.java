package cgeo.geocaching.connector.gc;

import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.ui.AvatarUtils;
import cgeo.geocaching.utils.TextUtils;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GCLoginTest {

    private final GCLogin instance = GCLogin.getInstance();

    @Before
    public void setUp() throws Exception {
        assertThat(instance.login()).isEqualTo(StatusCode.NO_ERROR);
    }

    private static String blockingHomeLocation() {
        return GCLogin.retrieveHomeLocation().blockingGet();
    }

    @Test
    public void testRetrieveHomeLocation() {
        assertThat(StringUtils.isNotBlank(blockingHomeLocation())).isTrue();
    }

    @Test
    public void testValidHomeLocation() {
        assertThat(new Geopoint(blockingHomeLocation())).isInstanceOf(Geopoint.class);
    }

    @Test
    public void testNoHtmlInHomeLocation() {
        final String homeLocation = blockingHomeLocation();
        assertThat(homeLocation).isEqualTo(TextUtils.stripHtml(homeLocation));
    }

    @Test
    public void testLanguageSwitch() {
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

    @Test
    public void testAvatar() {
        instance.resetServerParameters();
        AvatarUtils.changeAvatar(GCConnector.getInstance(), null);
        assertThat(AvatarUtils.getAvatar(GCConnector.getInstance())).isNull();
        instance.getServerParameters(); // avatar should automatically be updated here...
        assertThat(AvatarUtils.getAvatar(GCConnector.getInstance())).isNotNull();
    }

}
