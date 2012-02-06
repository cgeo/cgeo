package cgeo.geocaching.utils;

import java.net.URLEncoder;

/**
 * Utilities used for translating
 */
public final class TranslationUtils {

    private static final String translationWebsite = "http://translate.google.com/";
    private static final String translationForceClassicMode = "?vi=c";
    private static final String translationAutoSelect = "#auto";
    private static final String translationFieldSeparator = "|";

    /**
     * Build a URI for Google Translate
     *
     * @param toLang
     *            The two-letter lowercase ISO language codes as defined by ISO 639-1
     * @param text
     *            The text to be translated
     * @return URI ready to be parsed
     */
    public static String buildTranslationURI(final String toLang, final String text) {
        return translationWebsite + translationForceClassicMode + translationAutoSelect + translationFieldSeparator + toLang + translationFieldSeparator + URLEncoder.encode(text).replace("+", "%20");
    }

}
