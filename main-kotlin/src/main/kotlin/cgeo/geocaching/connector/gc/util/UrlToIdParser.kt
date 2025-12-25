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

import cgeo.geocaching.utils.functions.Func1

import androidx.annotation.Nullable

import java.util.Optional

interface UrlToIdParser : Func1()<String, Optional<String>> {
    Optional<String> tryExtractFromIntentUrl(String intentUrl)

    override     default Optional<String> call(String intentUrl) {
        return tryExtractFromIntentUrl(intentUrl)
    }
}
