package cgeo.geocaching.connector.gc.util;

import cgeo.geocaching.utils.functions.Func1;

import androidx.annotation.Nullable;

import java.util.Optional;

public interface UrlToIdParser extends Func1<String, Optional<String>> {
    Optional<String> tryExtractFromIntentUrl(@Nullable String intentUrl);

    @Override
    default Optional<String> call(String intentUrl) {
        return tryExtractFromIntentUrl(intentUrl);
    }
}
