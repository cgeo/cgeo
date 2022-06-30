package cgeo.geocaching.wizard;

import cgeo.geocaching.R;

import androidx.annotation.StringRes;

public enum NextButton {
    NEXT(R.string.next), FINISH(R.string.finish), DONE(R.string.done);

    @StringRes
    public final int string;

    NextButton(@StringRes final int string) {
        this.string = string;
    }
}
