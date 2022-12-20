package cgeo.geocaching.settings;

import android.os.Bundle;

import androidx.preference.PreferenceDialogFragmentCompat;

import org.apache.commons.lang3.NotImplementedException;

// This is required for the CustomDialog to show correctly (Colorpicker for example)
// See: https://stackoverflow.com/a/45524893/4730773

public class DialogPrefFragCompat extends PreferenceDialogFragmentCompat {
    public static DialogPrefFragCompat newInstance(final String key) {
        final DialogPrefFragCompat fragment = new DialogPrefFragCompat();
        final Bundle bundle = new Bundle(1);
        bundle.putString(ARG_KEY, key);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onDialogClosed(final boolean positiveResult) {
        if (positiveResult) {
            // do things
            throw new NotImplementedException("Colorpicker not implemented");
        }
    }
}
