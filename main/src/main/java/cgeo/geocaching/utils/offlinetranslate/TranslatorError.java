package cgeo.geocaching.utils.offlinetranslate;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.LocalizationUtils;

import androidx.annotation.StringRes;

public enum TranslatorError {
    NONE(0),
    SOURCE_LANGUAGE_NOT_IDENTIFIED(R.string.translator_language_unknown),
    LANGUAGE_UNSUPPORTED(R.string.translator_error_language_unsupported),
    LANGUAGE_MODEL_UNAVAILABLE(R.string.translator_error_model_unavailable),
    OTHER(R.string.err_auth_gc_unknown_error_generic);

    @StringRes private final int textId;

    TranslatorError(final int textId) {
        this.textId = textId;
    }

    public String getUserDisplayableString() {
        return LocalizationUtils.getString(textId);
    }

}
