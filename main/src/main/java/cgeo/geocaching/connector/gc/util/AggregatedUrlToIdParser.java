package cgeo.geocaching.connector.gc.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AggregatedUrlToIdParser implements UrlToIdParser {
    private final List<UrlToIdParser> parsers;

    public AggregatedUrlToIdParser(final UrlToIdParser... parsers) {
        this.parsers = parsers != null
                ? Arrays.asList(parsers)
                : Collections.emptyList();
    }

    @Override
    public Optional<String> tryExtractFromIntentUrl(final String intentUrl) {
        return Optional.ofNullable(intentUrl)
                .flatMap(url -> parsers.stream()
                        .map(parser -> parser.tryExtractFromIntentUrl(url))
                        .filter(Optional::isPresent)
                        .findFirst()
                )
                .orElse(null);
    }
}
