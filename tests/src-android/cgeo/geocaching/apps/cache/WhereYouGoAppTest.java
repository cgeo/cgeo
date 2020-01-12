package cgeo.geocaching.apps.cache;

import cgeo.geocaching.models.Geocache;

import junit.framework.TestCase;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class WhereYouGoAppTest extends TestCase {
    public static void testGetWhereIGoUrl() throws Exception {
        final Geocache cache = new Geocache();
        cache.setDescription("<p style=\"max-width:670px;\"><a href=\"http://www.wherigo.com/cartridge/details.aspx?CGUID=c4577c31-09e9-44f0-ae48-83737e57adbd\"><img class=\"InsideTable\"");
        assertThat(WhereYouGoApp.getWhereIGoUrl(cache)).isEqualTo("http://www.wherigo.com/cartridge/details.aspx?CGUID=c4577c31-09e9-44f0-ae48-83737e57adbd");
    }

    public static void testGetWhereIGoUrlMultipleURLs() throws Exception {
        final Geocache cache = new Geocache();
        cache.setDescription("Lade dir den <font color=\"#FF0000\"><strong><a href=\"http://www.wherigo.com/cartridge/details.aspx?CGUID=22e7ef35-d8b6-4d4b-b506-8d8b520316b3\" target=\"_blank\"><img src=\"http://surfstoff.de/wherigo/gifs/wherigo_logo_klein.gif\" width=\"14\" height=\"14\" border=\"0\" /></a> <a href=\"http://www.wherigo.com/cartridge/details.aspx?CGUID=8fc0fb5e-7310-4685-ad06-143edf873ab0\" target=\"_blank\">Wherigo</a>");
        assertThat(WhereYouGoApp.getWhereIGoUrl(cache)).isNull();
    }
}
