package cgeo.geocaching.connector.gc;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ImportBookmarkLinksTest {

    final ImportBookmarkLinks.UrlToIdParser intentUrlParser = ImportBookmarkLinks.defaultBookmarkListUrlToIdParser();

    enum CaseTransform {
        NONE,
        TO_LOWER,
        TO_UPPER
    }

    private String composeUsing(
            final String protocol,
            final String hostAndPath,
            final String bookmarkListId,
            final String suffix,
            final CaseTransform caseTransform
    ) {
        final String rawResult = protocol + "://" + hostAndPath + bookmarkListId + suffix;
        switch (caseTransform) {
            case TO_LOWER:
                return rawResult.toLowerCase();
            case TO_UPPER:
                return rawResult.toUpperCase();
            default:
                return rawResult;
        }
    }

    @Test
    public void testParseValidUrls() {
        final String[] validProtocols = {"http", "https"};
        final String[] validHostsAndPath = {
                "coord.info/",
                "geocaching.com/plan/lists/",
                "www.coord.info/",
                "www.geocaching.com/plan/lists/"
        };
        final String[] validSuffixes = { "", "?someoption=foo"};
        final String bookmarkListId = "BM2MKFM";

        for (final String protocol : validProtocols) {
            for (final String hostAndPath : validHostsAndPath) {
                for (final String validSuffix : validSuffixes) {
                    for (final CaseTransform caseTransform : CaseTransform.values()) {
                        final String testUrl = composeUsing(
                                protocol,
                                hostAndPath,
                                bookmarkListId,
                                validSuffix,
                                caseTransform
                        );
                        assertThat(intentUrlParser.tryExtractFromIntentUrl(testUrl))
                                .isEqualTo(bookmarkListId);
                    }
                }
            }
        }
    }
}
