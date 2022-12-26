package cgeo.geocaching.utils;

import cgeo.geocaching.models.Geocache;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class CheckerUtilsTest {

    @Test
    public void testGetCheckerUrl() {
        assertUrl("<p style=\"text-align:center;\"><a href=\"http://geocheck.org/geo_inputchkcoord.php?gid=618932716cc7e68-c4bb-4f41-8bb1-3e0a3e374a1f\" target=\"_blank\"><img", "http://geocheck.org/geo_inputchkcoord.php?gid=618932716cc7e68-c4bb-4f41-8bb1-3e0a3e374a1f");
        assertUrl("<p style=\"text-align:center;\"><a href=\"http://google.com/geo_inputchkcoord.php?gid=618932716cc7e68-c4bb-4f41-8bb1-3e0a3e374a1f\" target=\"_blank\"><img", null);
        assertUrl("http://www.certitudes.org/certitude?wp=GC5MVX7", "http://www.certitudes.org/certitude?wp=GC5MVX7");
        assertUrl("http://geochecker.com/index.php?code=e001928e3c2682ec2bae0f24b9d02cfb&action=check&wp=474350573454&name=47656f636865636b6572205465737420666f72204e33382030302e303030205737362030302e303030", "http://geochecker.com/index.php?code=e001928e3c2682ec2bae0f24b9d02cfb&action=check&wp=474350573454&name=47656f636865636b6572205465737420666f72204e33382030302e303030205737362030302e303030");
        assertUrl("<p>Haarige Aussichten gibt es <a href=\"http://www.geochecker.com/index.php?code=cd52752a8649c5e385a624b5341176f9&amp;action=check&amp;wp=4743314a43384b&amp;name=4b61747a656e&amp;language=German\">hier</a>.</p></span>", "http://www.geochecker.com/index.php?code=cd52752a8649c5e385a624b5341176f9&action=check&wp=4743314a43384b&name=4b61747a656e&language=German");
        assertUrl("<p>Haarige Aussichten gibt es <a href=\"http://www.geochecker.com/index.php?code=cd52752a8649c5e385a624b5341176f9&amp;action=check&amp;wp=4743314a43384b&amp;name=4b61747a656e&amp;language=German\">hier</a>.</p></span>", "http://www.geochecker.com/index.php?code=cd52752a8649c5e385a624b5341176f9&action=check&wp=4743314a43384b&name=4b61747a656e&language=German");
        assertUrl("Deine Lösung für die Koordinaten dieses Multis kannst du auf geochecker.com überprüfen. <a href=\"http://www.geochecker.com/index.php?code=3a08a604fe68fd8d09417ae530bb671a&amp;action=check&amp;wp=4743354d523847&amp;name=4e534720556e74657265732046657565726261636874616c&amp;language=german\">GeoChecker.com</a>", "http://www.geochecker.com/index.php?code=3a08a604fe68fd8d09417ae530bb671a&action=check&wp=4743354d523847&name=4e534720556e74657265732046657565726261636874616c&language=german");
    }

    @Test
    public void testAvoidNonLink() {
        assertUrl("some text... geochecker.com ... some more text", null);
    }

    private static void assertUrl(final String description, final String expected) {
        final Geocache geocache = new Geocache();
        geocache.setDescription(description);
        assertThat(CheckerUtils.getCheckerUrl(geocache)).isEqualTo(expected);
    }

}
