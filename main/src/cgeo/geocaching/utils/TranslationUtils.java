package cgeo.geocaching.utils;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.network.Network;

import android.content.Intent;
import android.net.Uri;

/**
 * Utilities used for translating
 */
public final class TranslationUtils {

    private static final String translationWebsite = "http://translate.google.com/";
    private static final String translationForceClassicMode = "?vi=c";
    private static final String translationAutoSelect = "#auto";
    private static final String translationFieldSeparator = "|";

    public static final int translationTextLengthToWarn = 500;
    private static final String TRANSLATION_APP = "com.google.android.apps.translate";

    /**
     * Build a URI for Google Translate
     *
     * @param toLang
     *            The two-letter lowercase ISO language codes as defined by ISO 639-1
     * @param text
     *            The text to be translated
     * @return URI ready to be parsed
     */
    private static String buildTranslationURI(final String toLang, final String text) {
        String content = Network.encode(text);
        // the app works better without the "+", the website works better with "+", therefore assume using the app if installed
        if (ProcessUtils.isInstalled(TRANSLATION_APP)) {
            content = content.replace("+", "%20");
        }
        return translationWebsite + translationForceClassicMode + translationAutoSelect + translationFieldSeparator + toLang + translationFieldSeparator + content;
    }

    /**
     * Send Intent for Google Translate. Can be caught by Google Translate App or browser
     *
     * @param toLang
     *            The two-letter lowercase ISO language codes as defined by ISO 639-1
     * @param text
     *            The text to be translated
     */
    public static void startActivityTranslate(final AbstractActivity context, final String toLang, final String text) {
        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(buildTranslationURI(toLang, text))));
    }
}
