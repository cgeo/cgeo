package cgeo.geocaching.connector.gc;

import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class ImportBookmarkLinksTest {

    final ImportBookmarkLinks.UrlToIdParser intentUrlParser = ImportBookmarkLinks.defaultBookmarkListUrlToIdParser();

    @Test
    public void testParseValidUrls() {
        //// HTTPS

        // Normal case
        assertThat(intentUrlParser.tryExtractFromIntentUrl("https://www.geocaching.com/plan/lists/BM2MKFM"))
                .isEqualTo("BM2MKFM");
        assertThat(intentUrlParser.tryExtractFromIntentUrl("https://coord.info/BM2MKFM"))
                .isEqualTo("BM2MKFM");

        // All UPPER case
        assertThat(intentUrlParser.tryExtractFromIntentUrl("HTTPS://WWW.GEOCACHING.COM/PLAN/LISTS/BM2MKFM"))
                .isEqualTo("BM2MKFM");
        assertThat(intentUrlParser.tryExtractFromIntentUrl("HTTPS://COORD.INFO/BM2MKFM"))
                .isEqualTo("BM2MKFM");

        // All lower case
        assertThat(intentUrlParser.tryExtractFromIntentUrl("https://www.geocaching.com/plan/lists/bm2mkfm"))
                .isEqualTo("BM2MKFM");
        assertThat(intentUrlParser.tryExtractFromIntentUrl("https://coord.info/bm2mkfm"))
                .isEqualTo("BM2MKFM");

        // Trailing characters
        assertThat(intentUrlParser.tryExtractFromIntentUrl("https://www.geocaching.com/plan/lists/BM2MKFM?someoption=foo"))
                .isEqualTo("BM2MKFM");
        assertThat(intentUrlParser.tryExtractFromIntentUrl("https://coord.info/BM2MKFM?someoption=foo"))
                .isEqualTo("BM2MKFM");


        //// HTTP

        // Normal case
        assertThat(intentUrlParser.tryExtractFromIntentUrl("http://www.geocaching.com/plan/lists/BM2MKFM"))
                .isEqualTo("BM2MKFM");
        assertThat(intentUrlParser.tryExtractFromIntentUrl("http://coord.info/BM2MKFM"))
                .isEqualTo("BM2MKFM");

        // All UPPER case
        assertThat(intentUrlParser.tryExtractFromIntentUrl("HTTP://WWW.GEOCACHING.COM/PLAN/LISTS/BM2MKFM"))
                .isEqualTo("BM2MKFM");
        assertThat(intentUrlParser.tryExtractFromIntentUrl("HTTP://COORD.INFO/BM2MKFM"))
                .isEqualTo("BM2MKFM");

        // All lower case
        assertThat(intentUrlParser.tryExtractFromIntentUrl("http://www.geocaching.com/plan/lists/bm2mkfm"))
                .isEqualTo("BM2MKFM");
        assertThat(intentUrlParser.tryExtractFromIntentUrl("http://coord.info/bm2mkfm"))
                .isEqualTo("BM2MKFM");

        // Trailing characters
        assertThat(intentUrlParser.tryExtractFromIntentUrl("http://www.geocaching.com/plan/lists/BM2MKFM?someoption=foo"))
                .isEqualTo("BM2MKFM");
        assertThat(intentUrlParser.tryExtractFromIntentUrl("http://coord.info/BM2MKFM?someoption=foo"))
                .isEqualTo("BM2MKFM");
    }
}
