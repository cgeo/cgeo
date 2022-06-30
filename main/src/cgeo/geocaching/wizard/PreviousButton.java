package cgeo.geocaching.wizard;

import cgeo.geocaching.R;

import androidx.annotation.StringRes;

public enum PreviousButton {
    PREVIOUS(R.string.previous), NOT_NOW(R.string.wizard_not_now);

    @StringRes
    public final int string;

    PreviousButton(@StringRes final int string) {
        this.string = string;
    }
}
