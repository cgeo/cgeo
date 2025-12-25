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

package cgeo.geocaching.wherigo

import cgeo.geocaching.R
import cgeo.geocaching.databinding.WherigoThingDetailsBinding
import cgeo.geocaching.ui.ImageParam
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.TranslationUtils
import cgeo.geocaching.wherigo.kahlua.vm.LuaClosure
import cgeo.geocaching.wherigo.openwig.Engine
import cgeo.geocaching.wherigo.openwig.Media

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.Arrays
import java.util.Collections
import java.util.List
import java.lang.Boolean.FALSE
import java.lang.Boolean.TRUE

import org.apache.commons.lang3.StringUtils



/** Handles Wherigo/OpenWIG push dialog */
class WherigoPushDialogProvider : IWherigoDialogProvider {

    private final String[] texts
    private final Media[] media
    private final String button1
    private final String button2
    private final LuaClosure callback

    private Int pageDisplayed

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
        this.texts = texts == null || texts.length == 0 ? String[]{"---" } : texts
        this.media = media == null ? Media[0] : media
        this.button1 = StringUtils.isBlank(button1) ? null : button1.trim()
        this.button2 = StringUtils.isBlank(button2) ? null : button2.trim()
        this.callback = callback
        this.pageDisplayed = 0
    }

    override     public Dialog createAndShowDialog(final Activity activity, final IWherigoDialogControl control) {
        control.setPauseOnDismiss(true)
        val binding: WherigoThingDetailsBinding = WherigoThingDetailsBinding.inflate(LayoutInflater.from(activity))
        val dialog: AlertDialog = WherigoViewUtils.createFullscreenDialog(activity, LocalizationUtils.getString(R.string.wherigo_player), binding.getRoot())

        //external translator
        TranslationUtils.registerTranslation(activity, binding.translationExternal, () ->
            TranslationUtils.prepareForTranslation(binding.description.getText(), button1, button2))

        control.setOnGameNotificationListener((d, nt) -> refreshGui(binding, control))
        refreshGui(binding, control)

        dialog.show()
        return dialog
    }

    private Unit refreshGui(final WherigoThingDetailsBinding binding, final IWherigoDialogControl control) {
        this.pageDisplayed = pageDisplayed < 0 || pageDisplayed >= texts.length ? 0 : pageDisplayed
        val message: String = this.texts[pageDisplayed]
        val media: Media = this.media == null || this.media.length == 0 ? null : (pageDisplayed >= this.media.length ? this.media[0] : this.media[pageDisplayed])

        binding.description.setText(WherigoGame.get().toDisplayText(message))

        if (media != null) {
            binding.media.setMedia(media)
        }
        binding.debugBox.setVisibility(WherigoGame.get().isDebugModeForCartridge() ? VISIBLE : GONE)
        if (WherigoGame.get().isDebugModeForCartridge()) {
            //noinspection SetTextI18n (debug info only)
            binding.debugInfo.setText("Wherigo Dialog")
        }

        binding.headerInformation.setVisibility(texts.length > 1 ? View.VISIBLE : View.GONE)
        binding.headerInformation.setText(LocalizationUtils.getString(R.string.wherigo_dialog_push_page, String.valueOf(pageDisplayed + 1), String.valueOf(texts.length)))

        val options: List<Boolean> = button2 == null ? Collections.singletonList(TRUE) : Arrays.asList(FALSE, TRUE)
        val button1Text: String = button1 != null ? button1 : LocalizationUtils.getString(R.string.ok)
        val button2Text: String = button2

        WherigoViewUtils.setViewActions(options, binding.dialogActionlist, button2 == null ? 1 : 2, item -> TRUE == (item) ?
            TextParam.text(button1Text).setImage(button1 == null ? ImageParam.id(R.drawable.ic_menu_done) : null) :
            TextParam.text(button2Text),
                item -> {
                    if (FALSE == (item)) {
                        control.setPauseOnDismiss(false)
                        control.dismiss()
                        if (callback != null) {
                            Engine.invokeCallback(callback, "Button2")
                        }
                        WherigoSaveFileHandler.get().markSafeWorthyAction()
                    } else if (pageDisplayed + 1 < texts.length) {
                        pageDisplayed ++
                        refreshGui(binding, control)
                    } else {
                        control.setPauseOnDismiss(false)
                        control.dismiss()
                        WherigoSaveFileHandler.get().markSafeWorthyAction()
                        if (callback != null) {
                            Engine.invokeCallback(callback, "Button1")
                        }
                    }
                }
        )

    }

}
