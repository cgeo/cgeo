package cgeo.geocaching.utils.xml;

import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

public final class XmlUtils {

    private static final XmlPullParserFactory XPP_FACTORY = safeCreateFactory();

    private XmlUtils() {
        // Do not instantiate
    }

    public static boolean isValidXmlChar(final char c) {
        return c == 0x9 || c == 0xA || c == 0xD
                || (c >= 0x20 && c <= 0xD7FF)
                || (c >= 0xE000 && c <= 0xFFFD);
    }

    /**
     * Write text content to an XML serializer, stripping XML-1.0-illegal characters and
     * converting surrogate pairs (e.g. emoji) to numeric character references (e.g. {@code &#129414;})
     * so that any {@link XmlSerializer} implementation can handle the output without throwing.
     *
     * @param serializer an XML serializer that is currently in text-content position
     * @param text       the text to write, or {@code null} to write nothing
     */
    public static void writeText(final XmlSerializer serializer, final String text) throws IOException {
        try {
            writeContent(serializer, text);
        } catch (final IOException e) {
            Log.e("XmlUtils.writeText: cannot write text", e);
            writeContent(serializer, " [end of text omitted due to an invalid character]");
        }
    }

    /**
     * Insert an attribute-less tag with enclosed text in a XML serializer output.
     *
     * @param serializer an XML serializer
     * @param prefix     an XML prefix, see {@link XmlSerializer#startTag(String, String)}
     * @param tag        an XML tag
     * @param text       some text to insert, or <tt>null</tt> to omit completely this tag
     */
    public static void simpleText(final XmlSerializer serializer, final String prefix, final String tag, final String text) throws IOException {
        if (text != null) {
            serializer.startTag(prefix, tag);
            writeContent(serializer, text);
            serializer.endTag(prefix, tag);
        }
    }

    /**
     * Insert pairs of attribute-less tags and enclosed texts in a XML serializer output
     *
     * @param serializer an XML serializer
     * @param prefix     an XML prefix, see {@link XmlSerializer#startTag(String, String)} shared by all tags
     * @param tagAndText an XML tag, the corresponding text, another XML tag, the corresponding text. <tt>null</tt> texts
     *                   will be omitted along with their respective tag.
     */
    public static void multipleTexts(final XmlSerializer serializer, final String prefix, final String... tagAndText) throws IOException {
        for (int i = 0; i < tagAndText.length; i += 2) {
            simpleText(serializer, prefix, tagAndText[i], tagAndText[i + 1]);
        }
    }

    public static XmlPullParser createParser(@NonNull final InputStream input, final boolean namespaceAware) throws XmlPullParserException {
        return createParser(input, namespaceAware, "UTF-8");
    }

    public static XmlPullParser createParser(@NonNull final InputStream input, final boolean namespaceAware, final String inputEncoding) throws XmlPullParserException {
        if (XPP_FACTORY == null) {
            throw new XmlPullParserException("XmlUtils: can't create XML Parser, no factory available");
        }

        synchronized (XPP_FACTORY) {
            final XmlPullParser parser = XPP_FACTORY.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, namespaceAware);
            parser.setInput(input, inputEncoding);
            return parser;
        }
    }

    private static XmlPullParserFactory safeCreateFactory() {
        try {
            return XmlPullParserFactory.newInstance();
        } catch (XmlPullParserException e) {
            Log.e("XmlUtils: could not create a XmlPullParserFactory, e");
        }
        return null;
    }

    /**
     * Write text to the serializer chunk by chunk, converting surrogate pairs to numeric
     * character references (&#CODEPOINT;) via {@link XmlSerializer#entityRef(String)} and
     * stripping lone surrogates and other XML-1.0-illegal characters.
     * This avoids passing raw surrogate chars to {@link XmlSerializer#text(String)}, which
     * would throw on implementations like kxml2's KXmlSerializer.
     */
    private static void writeContent(final XmlSerializer serializer, final String text) throws IOException {
        if (text == null) {
            return;
        }
        final StringBuilder chunk = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (i + 1 < text.length() && Character.isLowSurrogate(text.charAt(i + 1))) {
                    // Valid surrogate pair: flush accumulated chunk, then emit numeric char ref
                    if (chunk.length() > 0) {
                        serializer.text(chunk.toString());
                        chunk.setLength(0);
                    }
                    final int codePoint = Character.toCodePoint(c, text.charAt(i + 1));
                    serializer.entityRef("#" + codePoint);
                    i++; // skip the low surrogate
                }
                // else: lone high surrogate — skip
            } else if (Character.isLowSurrogate(c)) {
                // Lone low surrogate — skip
            } else if (isValidXmlChar(c)) {
                chunk.append(c);
            } else {
                reportInvalidCharacter(c);
            }
            // else: invalid XML 1.0 character — skip
        }
        if (chunk.length() > 0) {
            serializer.text(chunk.toString());
        }
    }

    private static void reportInvalidCharacter(char ch) {
        Log.w("Skipping illegal character (" + Integer.toHexString((int) ch) + ")");
    }

}