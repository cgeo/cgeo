package cgeo.geocaching.apps.cache;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.Geocache;

import junit.framework.TestCase;

public class WhereYouGoAppTest extends TestCase {
    public static void testGetWhereIGoUrl() throws Exception {
        Geocache cache = new Geocache();
        cache.setDescription("<p style=\"max-width:670px;\"><a href=\"http://www.wherigo.com/cartridge/details.aspx?CGUID=c4577c31-09e9-44f0-ae48-83737e57adbd\"><img class=\"InsideTable\"");
        assertThat(WhereYouGoApp.getWhereIGoUrl(cache)).isEqualTo("http://www.wherigo.com/cartridge/details.aspx?CGUID=c4577c31-09e9-44f0-ae48-83737e57adbd");
    }
}
