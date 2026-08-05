package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.WherigoThingDetailsBinding;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.wherigo.kahlua.vm.LuaClosure;
import cgeo.geocaching.wherigo.openwig.Engine;
import cgeo.geocaching.wherigo.openwig.Media;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import org.apache.commons.lang3.StringUtils;



/** Handles Wherigo/OpenWIG push dialog */
public class WherigoPushDialogProvider implements IWherigoDialogProvider {

    @NonNull private final String[] texts;
    @NonNull private final Media[] media;
    @NonNull private final String button1;
    @Nullable private final String button2;
    @Nullable private final LuaClosure callback;


    /**
     * Handles display of an OpenWIG push dialog. The following description is copied for reference from OpenWIG code
     * <p>
     * Shows a multi-page dialog to the user.
     * <p>
     * If another dialog or input is open, it should be closed before displaying this dialog.
     * <p>
     * While the dialog is open, user should only be able to continue by clicking one of its buttons. Button1 flips to
     * next page, and when at end, invokes the callback with parameter "Button1", regardless of value of button1.
     * Button2 immediately closes the dialog and invokes callback with parameter "Button2".
     * If the dialog is closed by another API call, callback should be invoked with null parameter.
     * @param texts texts of individual pages of dialog
     * @param media pictures for individual pages of dialog
     * @param button1 label for primary button. If null, "OK" is used.
     * @param button2 label for secondary button. If null, the button is not displayed.
     * @param callback callback to call when closing the dialog, or null
     */
    WherigoPushDialogProvider(final String[] texts, final Media[] media, final String button1, final String button2, final LuaClosure callback) {
        this.texts = texts == null || texts.length == 0 ? new String[]{"---" } : texts;
        this.media = media == null ? new Media[0] : media;
        this.button1 = StringUtils.isBlank(button1) ? LocalizationUtils.getString(R.string.ok) : button1.trim();
        this.button2 = StringUtils.isBlank(button2) ? null : button2.trim();
        this.callback = callback;
    }

    @Override
    public Dialog createAndShowDialog(final Activity activity, final IWherigoDialogControl control) {
        control.setPauseOnDismiss(true);
        final AlertDialog dialog = WherigoViewUtils.createFullscreenDialog(activity, LocalizationUtils.getString(R.string.wherigo_player));
        final WherigoThingDetailsBinding binding = WherigoThingDetailsBinding.inflate(LayoutInflater.from(activity));
        dialog.setView(binding.getRoot());
        final int[] page = new int[]{ 0 };

        refreshGui(binding, 0);

        final List<Boolean> options = button2 == null ? Collections.singletonList(TRUE) : Arrays.asList(FALSE, TRUE);

        WherigoViewUtils.setViewActions(options, binding.dialogActionlist, button2 == null ? 1 : 2, item -> TRUE.equals(item) ?
                TextParam.text(button1).setImage(ImageParam.id(R.drawable.ic_menu_done)) :
                TextParam.text(button2).setImage(ImageParam.id(R.drawable.ic_menu_cancel)),
            item -> {
                if (FALSE.equals(item)) {
                    control.setPauseOnDismiss(false);
                    control.dismiss();
                    if (callback != null) {
                        Engine.invokeCallback(callback, "Button2");
                    }
                } else if (page[0] + 1 < texts.length) {
                    page[0] ++;
                    refreshGui(binding, page[0]);
                } else {
                    control.setPauseOnDismiss(false);
                    control.dismiss();
                    if (callback != null) {
                        Engine.invokeCallback(callback, "Button1");
                    }
                }
            }
        );

        dialog.show();
        return dialog;
    }

    private void refreshGui(final WherigoThingDetailsBinding binding, final int pageToDisplay) {
        final int page = pageToDisplay < 0 || pageToDisplay >= texts.length ? 0 : pageToDisplay;
        final String message = this.texts[page];
        final Media media = this.media == null || this.media.length == 0 ? null : (page >= this.media.length ? this.media[0] : this.media[page]);

        binding.description.setText(WherigoGame.get().toDisplayText(message));
        if (media != null) {
            binding.media.setMedia(media);
        }
        binding.debugBox.setVisibility(WherigoGame.get().isDebugModeForCartridge() ? VISIBLE : GONE);
        if (WherigoGame.get().isDebugModeForCartridge()) {
            //noinspection SetTextI18n (debug info only)
            binding.debugInfo.setText("Wherigo Dialog");
        }

        binding.headerInformation.setVisibility(texts.length > 1 ? View.VISIBLE : View.GONE);
        binding.headerInformation.setText(LocalizationUtils.getString(R.string.wherigo_dialog_push_page, String.valueOf(page + 1), String.valueOf(texts.length)));
    }

}
