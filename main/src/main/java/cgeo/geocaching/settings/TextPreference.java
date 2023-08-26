package cgeo.geocaching.settings;

import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

public class TextPreference extends Preference {

    public TextPreference(final Context context) {
        super(context);
    }

    public TextPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public TextPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final Preference pref = findPreferenceInHierarchy(getKey());

        if (pref != null) {
            pref.setOnPreferenceClickListener(preference -> {
                launchTextDialog(pref.getTitle().toString());
                return false;
            });
        }
    }

    public void launchTextDialog(final String title) {

        SimpleDialog.ofContext(getContext()).setTitle(TextParam.text(title))
                .input(InputType.TYPE_CLASS_TEXT, getPersistedString(null),
                        null, null, null, null, s -> {
                    persistString(s);
                    callChangeListener(s);
                });
    }

}
