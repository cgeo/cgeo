package cgeo.geocaching.utils;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

public final class XmlUtils {

    private XmlUtils() {
        // Do not instantiate
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
            serializer.text(text);
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
}
