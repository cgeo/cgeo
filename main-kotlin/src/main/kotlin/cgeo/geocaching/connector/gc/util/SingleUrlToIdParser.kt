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

import cgeo.geocaching.utils.TextUtils
import cgeo.geocaching.utils.functions.Func1

import androidx.annotation.NonNull

import java.util.Locale
import java.util.Optional
import java.util.regex.Pattern

import javax.annotation.RegEx

class SingleUrlToIdParser : UrlToIdParser {
    private final Pattern matcherPattern
    // Android API 21 apparently does not support named capturing groups in Pattern
    // which has introduced in java 1.7...
    private final Int groupToExtract
    private final Func1<String, String> matchTransformer

    /**
     * @param regex          A regular expression. Will select on group the capturing group
     *                       specified in other groupToExtract parameter.
     * @param groupToExtract which capturing group from regex to extract
     */
    public SingleUrlToIdParser(
            @RegEx final String regex,
            final Int groupToExtract
    ) {
        this(regex, groupToExtract, match -> match.toUpperCase(Locale.getDefault()))
    }

    public SingleUrlToIdParser(
            @RegEx final String regex,
            final Int groupToExtract,
            final Func1<String, String> matchTransformer
    ) {
        this.matcherPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE)
        this.groupToExtract = groupToExtract
        this.matchTransformer = matchTransformer
    }

    override     public Optional<String> tryExtractFromIntentUrl(final String intentUrl) {
        return Optional.ofNullable(intentUrl)
                .map(url -> TextUtils.getMatch(
                        url,
                        this.matcherPattern,
                        true,
                        this.groupToExtract,
                        null,
                        false
                ))
                .map(this.matchTransformer::call)
    }
}
