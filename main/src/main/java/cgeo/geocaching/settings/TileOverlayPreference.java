package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.databinding.TileOverlayEditDialogBinding;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.unifiedmap.overlays.TileOverlay;
import cgeo.geocaching.unifiedmap.overlays.TileOverlays;
import cgeo.geocaching.unifiedmap.tiles.TileUrlShare;
import cgeo.geocaching.unifiedmap.tiles.TileUrlTester;
import cgeo.geocaching.unifiedmap.tiles.TileUrlUtils;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.LocalizationUtils;

import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.google.android.material.button.MaterialButton;
import org.apache.commons.lang3.StringUtils;

/**
 * One configured tile overlay, shown as a row in the settings.
 *
 * <p>Follows the layout of the log templates: tapping the row opens the edit dialog, the trailing
 * icon deletes the entry. Whether an overlay is actually drawn is decided in the map's layer
 * selection, not here.</p>
 */
public class TileOverlayPreference extends Preference {

    private final TileOverlay overlay;
    private final Runnable onChanged;

    public TileOverlayPreference(final Context context, @NonNull final TileOverlay overlay, @NonNull final Runnable onChanged) {
        super(context);
        this.overlay = overlay;
        this.onChanged = onChanged;

        setKey(overlay.getKey());
        setTitle(overlay.getDisplayName());
        setSummary(overlay.getUrl());
        setIconSpaceReserved(false);
        setPersistent(false);
        setWidgetLayoutResource(R.layout.preference_copy_delete_buttons);
    }

    @Override
    public void onBindViewHolder(@NonNull final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        setOnPreferenceClickListener(preference -> {
            showEditDialog();
            return true;
        });
        final MaterialButton copyButton = (MaterialButton) holder.findViewById(R.id.copyview);
        copyButton.setIconResource(R.drawable.ic_menu_copy);
        copyButton.setOnClickListener(v -> {
            ClipboardUtils.copyToClipboard(TileUrlShare.format(overlay.getName(), overlay.getUrl()));
            ViewUtils.showShortToast(getContext(), R.string.settings_tileUrl_copied);
        });

        final MaterialButton button = (MaterialButton) holder.findViewById(R.id.deleteview);
        button.setIconResource(R.drawable.ic_menu_delete);
        button.setOnClickListener(v -> SimpleDialog.ofContext(getContext())
                .setTitle(R.string.settings_tileOverlays_delete)
                .setMessage(R.string.settings_tileOverlays_delete_confirm, overlay.getDisplayName())
                .confirm(() -> {
                    TileOverlays.remove(overlay.getKey());
                    onChanged.run();
                }));
    }

    /** Opens the edit dialog. Also used for new overlays, which are not shown as a row until saved. */
    public void showEditDialog() {
        final TileOverlayEditDialogBinding binding = TileOverlayEditDialogBinding.inflate(LayoutInflater.from(getContext()));
        binding.overlayName.setText(overlay.getName());
        binding.overlayUrl.setText(overlay.getUrl());

        // start on the name for a new overlay, on the uri when editing an existing one
        final boolean isNew = TileOverlays.get(overlay.getKey()) == null;
        Keyboard.show(getContext(), isNew ? binding.overlayName : binding.overlayUrl);

        final AlertDialog dialog = Dialogs.newBuilder(getContext())
                .setTitle(isNew ? R.string.settings_tileOverlays_add : R.string.settings_tileOverlays_edit)
                .setView(binding.getRoot())
                // no listener here: it is attached after show(), so that neither invalid input nor
                // a test run closes the dialog and discards what was typed
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.settings_tileUrl_test, null)
                .setNegativeButton(android.R.string.cancel, (d, which) -> d.dismiss())
                .show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> save(dialog, binding));
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v -> test(dialog, binding));
        acceptSharedOverlayOnPaste(binding.overlayName, binding);
        acceptSharedOverlayOnPaste(binding.overlayUrl, binding);
    }

    /**
     * Lets a shared overlay be pasted into either field, filling both.
     *
     * <p>Hooks the paste action rather than reading the clipboard by itself: from Android 13 on,
     * reading it unprompted shows a system notice and is restricted to the focused app, while
     * handling what the user pastes needs no permission and stays silent.</p>
     */
    private void acceptSharedOverlayOnPaste(@NonNull final EditText field, @NonNull final TileOverlayEditDialogBinding binding) {
        ViewCompat.setOnReceiveContentListener(field, new String[]{"text/*"}, (view, payload) -> {
            final ClipData clip = payload.getClip();
            final CharSequence text = clip.getItemCount() > 0 ? clip.getItemAt(0).getText() : null;
            final String[] shared = TileUrlShare.parse(text == null ? null : text.toString());
            if (shared == null) {
                // not one of ours, let the field handle the paste as usual
                return payload;
            }
            // only fill a name that the paste actually carries, and never replace one the user
            // has already typed
            if (!shared[0].isEmpty() && StringUtils.isBlank(binding.overlayName.getText())) {
                binding.overlayName.setText(shared[0]);
            }
            binding.overlayUrl.setText(shared[1]);
            binding.overlayUrlFrame.setError(null);
            return null; // consumed
        });
    }

    private void save(@NonNull final AlertDialog dialog, @NonNull final TileOverlayEditDialogBinding binding) {
        // a url without placeholders gets the default tile path, as tile providers have always
        // done, so that both features accept the same input
        final String normalized = TileUrlUtils.normalize(String.valueOf(binding.overlayUrl.getText()));
        final String url = normalized == null ? null : TileUrlUtils.withDefaultTilePath(normalized);
        if (url == null) {
            binding.overlayUrlFrame.setError(LocalizationUtils.getString(R.string.settings_tileUrl_invalid));
            return;
        }
        binding.overlayUrlFrame.setError(null);
        store(dialog, binding, url);
    }

    /**
     * Fetches one real tile from the url in the dialog, on request.
     *
     * <p>A syntactically valid url can still be wrong, which shows up only as an overlay that
     * draws nothing. Saving deliberately does not wait for this: it needs the network, and an
     * overlay may well be typed in somewhere without one.</p>
     */
    private void test(@NonNull final AlertDialog dialog, @NonNull final TileOverlayEditDialogBinding binding) {
        final String normalized = TileUrlUtils.normalize(String.valueOf(binding.overlayUrl.getText()));
        final String url = normalized == null ? null : TileUrlUtils.withDefaultTilePath(normalized);
        if (url == null) {
            binding.overlayUrlFrame.setError(LocalizationUtils.getString(R.string.settings_tileUrl_invalid));
            return;
        }
        binding.overlayUrlFrame.setError(null);

        final Button testButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        testButton.setEnabled(false);
        binding.overlayUrlFrame.setHelperText(LocalizationUtils.getString(R.string.settings_tileUrl_testing));
        AndroidRxUtils.andThenOnUi(AndroidRxUtils.networkScheduler,
                () -> TileUrlTester.test(url),
                error -> {
                    testButton.setEnabled(true);
                    binding.overlayUrlFrame.setHelperText(null);
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

    private void store(@NonNull final AlertDialog dialog, @NonNull final TileOverlayEditDialogBinding binding, @NonNull final String url) {
        TileOverlays.addOrUpdate(overlay
                .withName(String.valueOf(binding.overlayName.getText()).trim())
                .withUrl(url));
        dialog.dismiss();
        onChanged.run();
    }
}
