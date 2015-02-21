package cgeo.geocaching.utils;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.Geocache;

import junit.framework.TestCase;

public class CheckerUtilsTest extends TestCase {

    public static void testGetCheckerUrl() throws Exception {
        assertUrl("<p style=\"text-align:center;\"><a href=\"http://geocheck.org/geo_inputchkcoord.php?gid=618932716cc7e68-c4bb-4f41-8bb1-3e0a3e374a1f\" target=\"_blank\"><img", "http://geocheck.org/geo_inputchkcoord.php?gid=618932716cc7e68-c4bb-4f41-8bb1-3e0a3e374a1f");
        assertUrl("<p style=\"text-align:center;\"><a href=\"http://google.com/geo_inputchkcoord.php?gid=618932716cc7e68-c4bb-4f41-8bb1-3e0a3e374a1f\" target=\"_blank\"><img", null);
        assertUrl("http://www.certitudes.org/certitude?wp=GC5MVX7", "http://www.certitudes.org/certitude?wp=GC5MVX7");
        assertUrl("http://geochecker.com/index.php?code=e001928e3c2682ec2bae0f24b9d02cfb&action=check&wp=474350573454&name=47656f636865636b6572205465737420666f72204e33382030302e303030205737362030302e303030", "http://geochecker.com/index.php?code=e001928e3c2682ec2bae0f24b9d02cfb&action=check&wp=474350573454&name=47656f636865636b6572205465737420666f72204e33382030302e303030205737362030302e303030");
    }

    private static void assertUrl(final String description, final String expected) {
        final Geocache geocache = new Geocache();
        geocache.setDescription(description);
        assertThat(CheckerUtils.getCheckerUrl(geocache)).isEqualTo(expected);
    }

}
