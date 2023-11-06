package cgeo.geocaching.connector.gc;

import cgeo.geocaching.connector.gc.util.UrlToIdParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(Parameterized.class)
public class ImportBookmarkLinksTest {

    final UrlToIdParser intentUrlParser = ImportBookmarkLinks.defaultBookmarkListUrlToIdParser();

    private final String protocol;
    private final String hostAndPath;
    private final String bookmarkListId;
    private final String suffix;

    private final String expectedOutcome;

    public ImportBookmarkLinksTest(
            final String protocol,
            final String hostAndPath,
            final String bookmarkListId,
            final String suffix,
            final String expectedOutcome
    ) {
        this.protocol = protocol;
        this.hostAndPath = hostAndPath;
        this.bookmarkListId = bookmarkListId;
        this.suffix = suffix;
        this.expectedOutcome = expectedOutcome;
    }

    enum CaseTransform {
        NONE,
        TO_LOWER,
        TO_UPPER;

        public String transform(final String input) {
            switch (this) {
                case TO_LOWER:
                    return input.toLowerCase();
                case TO_UPPER:
                    return input.toUpperCase();
                default:
                    return input;
            }
        }
    }

    private String composeUsing(
            final String protocol,
            final String hostAndPath,
            final String bookmarkListId,
            final String suffix
    ) {
        return protocol + "://" + hostAndPath + bookmarkListId + suffix;
    }

    @Parameterized.Parameters(name = "{0}://{1}{2}{3} -> {4}")
    public static Collection<Object[]> testData() {
        final String[] validProtocols = {"http", "https"};
        final String[] validHostsAndPath = {
                "coord.info/",
                "geocaching.com/plan/lists/",
                "www.coord.info/",
                "www.geocaching.com/plan/lists/"
        };
        final String[] validSuffixes = { "", "?someoption=foo"};
        final ArrayList<Object[]> testData = new ArrayList<>();

        for (final String protocol : validProtocols) {
            for (final String hostAndPath : validHostsAndPath) {
                for (final String validSuffix : validSuffixes) {
                    for (final CaseTransform caseTransform : CaseTransform.values()) {
                        testData.add(makeTestDataSet(protocol, hostAndPath, "BM2MKFM", validSuffix, caseTransform));
                    }
                }
            }
        }
        return testData;
    }

    private static Object[] makeTestDataSet(
            final String protocol,
            final String hostAndPath,
            final String bookmarkListId,
            final String suffix,
            final CaseTransform caseTransform
    ) {
        return new Object[] {
                caseTransform.transform(protocol),
                caseTransform.transform(hostAndPath),
                caseTransform.transform(bookmarkListId),
                caseTransform.transform(suffix),
                bookmarkListId
        };
    }

    @Test
    public void testUrls() {
        final String testUrl = composeUsing(
                protocol,
                hostAndPath,
                bookmarkListId,
                suffix
        );
        final Optional<String> fromIntentUrl = intentUrlParser.tryExtractFromIntentUrl(testUrl);
        assertThat(fromIntentUrl.isPresent()).isTrue();
        assertThat(fromIntentUrl.orElse(null)).isEqualTo(expectedOutcome);
    }
}
