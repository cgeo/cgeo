package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.settings.Settings.PrefCustomMap;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.unifiedmap.tileproviders.CustomMapUrl;
import cgeo.geocaching.utils.LocalizationUtils;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import org.apache.commons.lang3.StringUtils;

public class CustomMapPreference extends Preference {

    private final PrefCustomMap customMap;

    public CustomMapPreference(final Context context, final PrefCustomMap customMap) {
        super(context);
        this.customMap = customMap;
        setKey(customMap.getId());
        setTitle(customMap.getName());
        setSummary(R.string.settings_custom_map_configured);
        setIconSpaceReserved(false);
        setWidgetLayoutResource(R.layout.button_icon_view);
    }

    @Override
    public void onBindViewHolder(@NonNull final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        setOnPreferenceClickListener(preference -> {
            launchEditDialog();
            return true;
        });
        final MaterialButton button = (MaterialButton) holder.findViewById(R.id.iconview);
        button.setIconResource(R.drawable.ic_menu_delete);
        button.setOnClickListener(v -> SimpleDialog.ofContext(getContext())
                .setTitle(R.string.settings_custom_maps_title)
                .setMessage(R.string.settings_custom_map_remove_confirm)
                .confirm(() -> {
                    Settings.removeCustomMap(customMap);
                    callChangeListener(null);
                }));
    }

    public void launchEditDialog() {
        final View view = LayoutInflater.from(getContext()).inflate(R.layout.custom_map_preference_dialog, null);
        final TextInputLayout nameLayout = view.findViewById(R.id.custom_map_name_layout);
        final TextInputLayout urlLayout = view.findViewById(R.id.custom_map_url_layout);
        final EditText name = view.findViewById(R.id.custom_map_name);
        final EditText url = view.findViewById(R.id.custom_map_url);
        name.setText(customMap.getName());
        url.setText(customMap.getUrl());

        final boolean focusUrl = StringUtils.isNotBlank(customMap.getName());
        Keyboard.show(getContext(), focusUrl ? url : name);

        final AlertDialog dialog = Dialogs.newBuilder(getContext())
                .setTitle(StringUtils.isBlank(customMap.getName()) ? LocalizationUtils.getString(R.string.settings_custom_maps_add) : customMap.getName())
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            final String newName = name.getText().toString().trim();
            final String newUrl = url.getText().toString().trim();
            nameLayout.setError(null);
            urlLayout.setError(null);
            if (StringUtils.isBlank(newName)) {
                nameLayout.setError(LocalizationUtils.getString(R.string.settings_custom_map_missing_name));
                return;
            }
            if (!CustomMapUrl.isValidTemplate(newUrl)) {
                urlLayout.setError(LocalizationUtils.getString(R.string.settings_custom_map_invalid_url));
                return;
            }
            Settings.putCustomMap(new PrefCustomMap(customMap.getId(), newName, CustomMapUrl.normalizeTemplate(newUrl)));
            callChangeListener(newUrl);
            dialog.dismiss();
        });
    }
}
