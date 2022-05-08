package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.network.Network;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import org.apache.commons.lang3.StringUtils;

/**
 * Utilities used for translating
 */
public final class TranslationUtils {

    private static final String TRANSLATION_WEBSITE = "https://translate.google.com/m/translate";
    private static final String TRANSLATION_FORCE_CLASSIC_MODE = "?vi=c";
    private static final String TRANSLATION_AUTO_SELECT = "#auto";
    private static final String TRANSLATION_FIELD_SEPARATOR = "|";

    private static final int TRANSLATION_TEXT_LENGTH_WARN = 500;
    private static final String TRANSLATION_APP = "com.google.android.apps.translate";

    private TranslationUtils() {
        // utility class
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
        if (ProcessUtils.isLaunchable(TRANSLATION_APP)) {
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
    public static void startActivityTranslate(final Activity activity, final String toLang, final String text) {
        if (text.length() > TRANSLATION_TEXT_LENGTH_WARN) {
            ActivityMixin.showToast(activity, R.string.translate_length_warning);
        }
        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(buildTranslationURI(toLang, text))));
    }

    public static boolean supportsInAppTranslationPopup() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ProcessUtils.isLaunchable(TRANSLATION_APP);
    }

    public static void startInAppTranslationPopup(final Activity activity, final String text) {
        if (!supportsInAppTranslationPopup()) {
            return;
        }

        final Intent intent = new Intent();
        intent.setType("text/plain");
        intent.setAction(Intent.ACTION_PROCESS_TEXT);
        intent.putExtra(Intent.EXTRA_PROCESS_TEXT, text);
        intent.putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true);
        intent.setPackage(TRANSLATION_APP);
        activity.startActivity(intent);
    }
}
