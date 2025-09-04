package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.html.HtmlUtils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/** Utilities used for (c:geo-external) translatinn */
public final class TranslationUtils {

    public enum Translator {
        NONE(R.string.translate_external_none, 0, () -> true, (a, t) -> { }),
        GOOGLE_TRANSLATE_WEB(R.string.translate_external_google_web, 500, () -> true, (act, text) -> startActivityTranslate(act, Locale.getDefault().getDisplayLanguage(), text)),
        GOOGLE_TRANSLATE_APP(R.string.translate_external_google_app, "com.google.android.apps.translate"),
        DEEPL_APP(R.string.translate_external_deepl_app, "com.deepl.mobiletranslator");


        public final int nameId;
        public final int limitWarning;
        public final Supplier<Boolean> isAvailable;
        public final BiConsumer<Activity, String> translateCaller;

        Translator(final int nameId, final int limitWarning, final Supplier<Boolean> isAvailable, final BiConsumer<Activity, String> translateCaller) {
            this.nameId = nameId;
            this.limitWarning = limitWarning;
            this.isAvailable = isAvailable;
            this.translateCaller = translateCaller;
        }

        /** for App translators */
        Translator(final int nameId, final String packageName) {
            this(nameId, 0, () -> appIsAvailable(packageName), (act, text) -> startInAppTranslation(act, packageName, text));
        }

        public String toUserDisplayableString() {
            return LocalizationUtils.getString(nameId) +
                (isAvailable.get() ? "" : " (" + LocalizationUtils.getString(R.string.translate_external_notavailable) + ")");
        }
    }

    //parameters for google web translation
    private static final String TRANSLATION_WEBSITE = "https://translate.google.com/m/translate";
    private static final String TRANSLATION_FORCE_CLASSIC_MODE = "?vi=c";
    private static final String TRANSLATION_AUTO_SELECT = "#auto";
    private static final String TRANSLATION_FIELD_SEPARATOR = "|";
    private static final int TRANSLATION_TEXT_LENGTH_WARN = 500;

    private TranslationUtils() {
        // utility class
    }

    public static boolean isEnabled() {
        return Settings.getTranslatorExternal() != Translator.NONE;
    }

    @NonNull
    public static CharSequence getTranslationLabel() {
        return LocalizationUtils.getString(R.string.translate_external_label, getTranslationName());
    }

    @NonNull
    public static CharSequence getTranslationName() {
        return getEffectiveTranslator().toUserDisplayableString();
    }

    public static void translate(final Activity activity, final String text) {
        final Translator translator = getEffectiveTranslator();
        if (translator.limitWarning > 0 && text.length() > translator.limitWarning) {
            ViewUtils.showToast(activity, R.string.translate_length_warning);
        }
        getEffectiveTranslator().translateCaller.accept(activity, text);
    }

    public static String prepareForTranslation(final CharSequence ... text) {
        return Arrays.stream(text).filter(StringUtils::isNotBlank).map(HtmlUtils::extractText).collect(Collectors.joining("\n\n"));
    }

    public static void registerTranslation(final Activity activity, final Button button, final Supplier<String> textSupplier) {
        registerTranslation(activity, button, null, null, textSupplier);
    }

    public static void registerTranslation(final Activity activity, final Button button, final View box, final TextView label,
           final Supplier<String> textSupplier) {

        final View realBox = box != null ? box : button;
        realBox.setVisibility(isEnabled() ? View.VISIBLE : View.GONE);
        if (label != null) {
            label.setText(getTranslationLabel());
        } else {
            button.setText(getTranslationName());
        }
        button.setOnClickListener(v -> {
            final String text = textSupplier.get();
            translate(activity, text);
        });

    }

    private static Translator getEffectiveTranslator() {
        final Translator trans = Settings.getTranslatorExternal();
        if (trans.isAvailable.get()) {
            return trans;
        }
        return Translator.GOOGLE_TRANSLATE_WEB;
    }


    /**
     * Build a URI for Google Translate.
     *
     * @param toLang The two-letter lowercase ISO language codes as defined by ISO 639-1
     * @param text   The text to be translated
     * @return URI ready to be parsed
     */
    private static String buildTranslationURI(final String toLang, final String text) {
        String content = Network.encode(text);
        // the app works better without the "+", the website works better with "+", therefore assume using the app if installed
        if (Translator.GOOGLE_TRANSLATE_APP.isAvailable.get()) {
            content = StringUtils.replace(content, "+", "%20");
        }
        return TRANSLATION_WEBSITE + TRANSLATION_FORCE_CLASSIC_MODE + TRANSLATION_AUTO_SELECT + TRANSLATION_FIELD_SEPARATOR + toLang + TRANSLATION_FIELD_SEPARATOR + content;
    }

    /**
     * Send Intent for Google Translate. Should only be used if InAppTranslationPopup is not available.
     *
     * @param activity The activity starting the process
     * @param toLang   The two-letter lowercase ISO language codes as defined by ISO 639-1
     * @param text     The text to be translated
     */
    private static void startActivityTranslate(final Activity activity, final String toLang, final String text) {
        if (text.length() > TRANSLATION_TEXT_LENGTH_WARN) {
            ActivityMixin.showToast(activity, R.string.translate_length_warning);
        }
        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(buildTranslationURI(toLang, text))));
    }


    private static boolean appIsAvailable(final String packageName) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ProcessUtils.isLaunchable(packageName);
    }

    private static void startInAppTranslation(final Activity activity, final String packageName, final String text) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        final Intent intent = new Intent();
        intent.setType("text/plain");
        intent.setAction(Intent.ACTION_PROCESS_TEXT);
        intent.putExtra(Intent.EXTRA_PROCESS_TEXT, text);
        intent.putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true);
        intent.setPackage(packageName);
        activity.startActivity(intent);
    }
}
