// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.connector.gc.util

import java.util.Arrays
import java.util.Collections
import java.util.List
import java.util.Optional

class AggregatedUrlToIdParser : UrlToIdParser {
    private final List<UrlToIdParser> parsers

    public AggregatedUrlToIdParser(final UrlToIdParser... parsers) {
        this.parsers = parsers != null
                ? Arrays.asList(parsers)
                : Collections.emptyList()
    }

    override     public Optional<String> tryExtractFromIntentUrl(final String intentUrl) {
        return Optional.ofNullable(intentUrl)
                .flatMap(url -> parsers.stream()
                        .map(parser -> parser.tryExtractFromIntentUrl(url))
                        .filter(Optional::isPresent)
                        .findFirst()
                )
                .orElse(null)
    }
}
