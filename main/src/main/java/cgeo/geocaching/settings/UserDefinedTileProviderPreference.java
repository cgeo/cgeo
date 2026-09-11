package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.unifiedmap.tileproviders.PrefUserDefinedTileProvider;
import cgeo.geocaching.unifiedmap.tiles.TileUrlTester;
import cgeo.geocaching.unifiedmap.tiles.TileUrlUtils;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.LocalizationUtils;

import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import org.apache.commons.lang3.StringUtils;

/** Preference to add / edit / remove a single user-defined tile provider */
public class UserDefinedTileProviderPreference extends Preference {

    public UserDefinedTileProviderPreference(final Context context) {
        super(context);
        setWidgetLayoutResource(R.layout.preference_copy_delete_buttons);
    }

    public UserDefinedTileProviderPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public UserDefinedTileProviderPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onBindViewHolder(@NonNull final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        setOnPreferenceClickListener(preference -> {
            launchEditDialog();
            return false;
        });
        final MaterialButton copyButton = (MaterialButton) holder.findViewById(R.id.copyview);
        copyButton.setIconResource(R.drawable.ic_menu_copy);
        copyButton.setOnClickListener(v -> {
            final PrefUserDefinedTileProvider provider = getProvider();
            if (provider != null) {
                ClipboardUtils.copyToClipboard(provider.toShareableText());
                ViewUtils.showShortToast(getContext(), R.string.settings_tileUrl_copied);
            }
        });

        final MaterialButton button = (MaterialButton) holder.findViewById(R.id.deleteview);
        button.setIconResource(R.drawable.ic_menu_delete);
        button.setOnClickListener(v -> SimpleDialog.ofContext(getContext())
                .setTitle(R.string.settings_userDefinedTileProvider)
                .setMessage(R.string.settings_userDefinedTileProvider_remove_confirm)
                .confirm(() -> {
                    // a provider without Uri gets removed
                    Settings.putUserDefinedTileProvider(new PrefUserDefinedTileProvider(getKey(), null, null));
                    callChangeListener(null);
                }));
    }

    public void launchEditDialog() {
        final View v = LayoutInflater.from(getContext()).inflate(R.layout.userdefined_tileprovider_preference_dialog, null);
        final TextInputLayout uriLayout = v.findViewById(R.id.editLayout);
        final EditText editTitle = v.findViewById(R.id.title);
        final EditText editUri = v.findViewById(R.id.edit);

        final PrefUserDefinedTileProvider provider = getProvider();
        final boolean isNew = provider == null;
        if (!isNew) {
            editTitle.setText(provider.getName());
            editUri.setText(provider.getUri());
        }
        Dialogs.moveCursorToEnd(editUri);
        Keyboard.show(getContext(), isNew ? editTitle : editUri);

        final AlertDialog dialog = Dialogs.newBuilder(getContext())
                .setView(v)
                .setTitle(isNew ? R.string.settings_userDefinedTileProvider_addnew : R.string.settings_userDefinedTileProvider_edit)
                // listeners are attached after show(), so that neither an invalid Uri nor a test
                // run dismisses the dialog and discards what was typed
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.settings_tileUrl_test, null)
                .setNegativeButton(android.R.string.cancel, (d, which) -> d.dismiss())
                .show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v2 -> save(dialog, uriLayout, editTitle, editUri));
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v2 -> test(dialog, uriLayout, editUri));
        acceptSharedProviderOnPaste(editTitle, uriLayout, editTitle, editUri);
        acceptSharedProviderOnPaste(editUri, uriLayout, editTitle, editUri);
    }

    /**
     * Lets a shared tile provider be pasted into either field, filling both.
     *
     * <p>Hooks the paste action rather than reading the clipboard by itself: from Android 13 on,
     * reading it unprompted shows a system notice and is restricted to the focused app, while
     * handling what the user pastes needs no permission and stays silent.</p>
     */
    private void acceptSharedProviderOnPaste(@NonNull final EditText field, @NonNull final TextInputLayout uriLayout,
                                             @NonNull final EditText editTitle, @NonNull final EditText editUri) {
        ViewCompat.setOnReceiveContentListener(field, new String[]{"text/*"}, (view, payload) -> {
            final ClipData clip = payload.getClip();
            final CharSequence text = clip.getItemCount() > 0 ? clip.getItemAt(0).getText() : null;
            final PrefUserDefinedTileProvider shared = PrefUserDefinedTileProvider.fromShareableText(text == null ? null : text.toString());
            if (shared == null) {
                // not one of ours, let the field handle the paste as usual
                return payload;
            }
            // only fill a name that the paste actually carries, and never replace one the user
            // has already typed
            if (StringUtils.isNotEmpty(shared.getName()) && StringUtils.isBlank(editTitle.getText())) {
                editTitle.setText(shared.getName());
            }
            editUri.setText(shared.getUri());
            uriLayout.setError(null);
            return null; // consumed
        });
    }

    private void save(@NonNull final AlertDialog dialog, @NonNull final TextInputLayout uriLayout,
                      @NonNull final EditText editTitle, @NonNull final EditText editUri) {
        final String uri = TileUrlUtils.normalize(editUri.getText().toString());
        if (uri == null) {
            uriLayout.setError(LocalizationUtils.getString(R.string.settings_tileUrl_invalid));
            return;
        }
        uriLayout.setError(null);
        store(dialog, editTitle, uri);
    }

    /**
     * Fetches one real tile from the Uri in the dialog, on request.
     *
     * <p>A syntactically valid Uri can still be wrong, which shows up only as a map that stays
     * empty. Saving deliberately does not wait for this: it needs the network, and an entry may
     * well be typed somewhere without one.</p>
     */
    private void test(@NonNull final AlertDialog dialog, @NonNull final TextInputLayout uriLayout, @NonNull final EditText editUri) {
        final String uri = TileUrlUtils.normalize(editUri.getText().toString());
        if (uri == null) {
            uriLayout.setError(LocalizationUtils.getString(R.string.settings_tileUrl_invalid));
            return;
        }
        uriLayout.setError(null);

        final Button testButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        testButton.setEnabled(false);
        uriLayout.setHelperText(LocalizationUtils.getString(R.string.settings_tileUrl_testing));
        AndroidRxUtils.andThenOnUi(AndroidRxUtils.networkScheduler,
                () -> TileUrlTester.test(TileUrlUtils.withDefaultTilePath(uri)),
                error -> {
                    testButton.setEnabled(true);
                    uriLayout.setHelperText(null);
                    if (error == null) {
                        ViewUtils.showShortToast(getContext(), R.string.settings_tileUrl_testok);
                    } else {
                        SimpleDialog.ofContext(getContext())
                                .setTitle(R.string.settings_tileUrl_testfailed_title)
                                .setMessage(R.string.settings_tileUrl_testfailed, error)
                                .show();
                    }
                });
    }

    private void store(@NonNull final AlertDialog dialog, @NonNull final EditText editTitle, @NonNull final String uri) {
        Settings.putUserDefinedTileProvider(new PrefUserDefinedTileProvider(getKey(), editTitle.getText().toString().trim(), uri));
        callChangeListener(uri);
        dialog.dismiss();
    }

    private PrefUserDefinedTileProvider getProvider() {
        for (PrefUserDefinedTileProvider provider : Settings.getUserDefinedTileProviders()) {
            if (provider.getKey().equals(getKey())) {
                return provider;
            }
        }
        return null;
    }
}
