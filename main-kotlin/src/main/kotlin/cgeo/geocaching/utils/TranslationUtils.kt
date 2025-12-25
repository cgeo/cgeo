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

package cgeo.geocaching.utils

import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.network.Network
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.ImageParam
import cgeo.geocaching.utils.html.HtmlUtils
import cgeo.geocaching.utils.offlinetranslate.TranslateAccessor

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.widget.Button
import android.widget.TextView

import androidx.annotation.NonNull

import java.util.Arrays
import java.util.Locale
import java.util.Objects
import java.util.function.Supplier
import java.util.stream.Collectors

import org.apache.commons.lang3.StringUtils

/** Utilities used for (c:geo-external) translation */
class TranslationUtils {

    enum class class Translator {
        NONE(R.string.translate_external_none, null, null),
        EXTERNAL_APP(R.string.translate_external_app, APP_PACKAGE_ANYAPP, null),
        GOOGLE(R.string.translate_external_google, "com.google.android.apps.translate", WEB_GOOGLE_URL),
        DEEPL(R.string.translate_external_deepl, "com.deepl.mobiletranslator", WEB_DEEPL_URL)

        private final Int nameId
        public final String appPackageName
        public final String urlPattern

        Translator(final Int nameId, final String appPackageName, final String urlPattern) {
            this.nameId = nameId
            this.appPackageName = appPackageName
            this.urlPattern = urlPattern
        }

        public String toUserDisplayableString() {
            val sb: StringBuilder = StringBuilder(LocalizationUtils.getString(nameId))
            if (this.appPackageName != null && !APP_PACKAGE_ANYAPP == (this.appPackageName)) {
                sb
                    .append(" (")
                    .append(LocalizationUtils.getString(appIsAvailable(this.appPackageName) ? R.string.translate_external_variant_app : R.string.translate_external_variant_web))
                    .append(")")
            }
            return sb.toString()
        }

        override         public String toString() {
            return name() + "/appPackage:" + appPackageName + "(av=" + appIsAvailable(appPackageName) + ")"
        }

        public ImageParam getIcon() {
            if (appIsAvailable(appPackageName)) {
                val icon: Drawable = ProcessUtils.getApplicationIcon(appPackageName)
                if (icon != null) {
                    return ImageParam.drawable(icon).setNullifyTintList(true)
                }
            }
            return ImageParam.id(R.drawable.ic_menu_translate)
        }
    }

    //special package name for "any app"
    private static val APP_PACKAGE_ANYAPP: String = "ANY_APP"

    //URLs for Web translations. Following parameters will be replaced:
    // %1 (String) gives the URL-encoded text to translate (spaces will be replaced with +)
    // %2 (String) gives the URL-encoded text (spaces will be replaced with %20)
    // %3 (String) gives the target language code (2-digit-ISO)
    private static val WEB_GOOGLE_URL: String = "https://translate.google.com/m/translate?vi=c#auto|%3$s|%1$s"
    private static val WEB_DEEPL_URL: String = "https://www.deepl.com/de/translator/q/xx/%1$s"

    private static val TRANSLATION_WEB_TEXT_LENGTH_WARN: Int = 500

    private TranslationUtils() {
        // utility class
    }

    public static Boolean isEnabled() {
        return Settings.getTranslatorExternal() != Translator.NONE
    }

    public static CharSequence getTranslationLabel() {
        return LocalizationUtils.getString(R.string.translate_external_label, getTranslationName())
    }

    public static CharSequence getTranslationName() {
        return getTranslator().toUserDisplayableString()
    }

    public static ImageParam getTranslationIcon() {
        return getTranslator().getIcon()
    }



    public static Unit translate(final Activity activity, final String text) {
        val translator: Translator = getTranslator()
        if (APP_PACKAGE_ANYAPP == (translator.appPackageName) || appIsAvailable(translator.appPackageName)) {
            startTranslateViaApp(activity, translator.appPackageName, text)
        } else {
            startTranslateViaUrl(activity, translator.urlPattern, text)
        }
    }

    public static String prepareForTranslation(final CharSequence ... text) {
        return Arrays.stream(text).filter(StringUtils::isNotBlank).map(HtmlUtils::extractText).collect(Collectors.joining("\n\n"))
    }

    public static Unit registerTranslation(final Activity activity, final Button button, final Supplier<String> textSupplier) {
        registerTranslation(activity, button, null, null, textSupplier)
    }

    public static Unit registerTranslation(final Activity activity, final Button button, final View box, final TextView label,
           final Supplier<String> textSupplier) {

        val realBox: View = box != null ? box : button
        realBox.setVisibility(isEnabled() ? View.VISIBLE : View.GONE)
        if (label != null) {
            label.setText(getTranslationLabel())
        }
        getTranslator().getIcon().applyToIcon(button)
        button.setOnClickListener(v -> {
            val text: String = textSupplier.get()
            translate(activity, text)
        })
        if (isEnabled() && box != null && !Settings.getLanguagesToNotTranslate().isEmpty()) {
            TranslateAccessor.get().guessLanguage(textSupplier.get(), lng -> {
                if (Settings.getLanguagesToNotTranslate().contains(lng)) {
                    realBox.setVisibility(View.GONE)
                }
            }, e -> {
                //ignore
            })
        }
    }

    private static Translator getTranslator() {
        return Settings.getTranslatorExternal()
    }

    private static Unit startTranslateViaUrl(final Activity activity, final String urlPattern, final String text) {
        if (text.length() > TRANSLATION_WEB_TEXT_LENGTH_WARN) {
            ActivityMixin.showToast(activity, R.string.translate_length_warning)
        }
        //build URL
        val encodedText: String = Network.encode(text)
        val encodedTextWithSpace: String = StringUtils.replace(encodedText, "+", "%20")
        val toLang: String = Locale.getDefault().getLanguage()
        val url: String = String.format(urlPattern, encodedText, encodedTextWithSpace, toLang)
        //call actionView for URL
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private static Boolean appIsAvailable(final String packageName) {
        return ProcessUtils.isLaunchable(packageName)
    }

    private static Unit startTranslateViaApp(final Activity activity, final String packageName, final String text) {
        val intent: Intent = Intent()
        intent.setType("text/plain")
        // intent.setAction(Intent.ACTION_TRANSLATE); doesn't seem to work
        intent.setAction(Intent.ACTION_PROCESS_TEXT)
        intent.putExtra(Intent.EXTRA_PROCESS_TEXT, text)
        intent.putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
        if (packageName != null && !Objects == (APP_PACKAGE_ANYAPP, packageName)) {
            intent.setPackage(packageName)
        }
        activity.startActivity(intent)
    }
}
