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

package cgeo.geocaching.utils.offlinetranslate

import cgeo.geocaching.R
import cgeo.geocaching.utils.LocalizationUtils

import androidx.annotation.StringRes

enum class class TranslatorError {
    NONE(0),
    SOURCE_LANGUAGE_NOT_IDENTIFIED(R.string.translator_language_unknown),
    LANGUAGE_UNSUPPORTED(R.string.translator_language_unsupported),
    OTHER(R.string.err_auth_gc_unknown_error_generic)

    @StringRes private final Int textId

    TranslatorError(final Int textId) {
        this.textId = textId
    }

    public String getUserDisplayableString() {
        return LocalizationUtils.getString(textId)
    }

}
