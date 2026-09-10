package cgeo.geocaching.utils.xml;

import cgeo.geocaching.files.InvalidXMLCharacterFilterReader;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

public final class XmlUtils {

    private static final XmlPullParserFactory XPP_FACTORY = safeCreateFactory();
    private static final Pattern PATTERN_MILLISECONDS = Pattern.compile("\\.\\d+");

    private XmlUtils() {
        // Do not instantiate
    }

    public static String getLocalName(final String name) {
        if (name == null) {
            return null;
        }
        final int lastColon = name.lastIndexOf(':');
        return lastColon < 0 ? name : name.substring(lastColon + 1);
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

    public static XmlPullParser createParser(@NonNull final InputStream input, final boolean namespaceAware) throws XmlPullParserException {
        return createParser(input, null, namespaceAware, false);
    }

    public static XmlPullParser createParser(@NonNull final InputStream input, final String inputEncoding, final boolean namespaceAware, final boolean relaxed) throws XmlPullParserException {
        final XmlPullParser parser = createParser(namespaceAware, relaxed);
        setInput(parser, input, inputEncoding == null ? StandardCharsets.UTF_8.name() : inputEncoding);
        return parser;
    }

    /** Creates a parser directly on a reader, e.g. to filter XML characters before parsing. */
    public static XmlPullParser createParser(@NonNull final Reader input, final boolean namespaceAware) throws XmlPullParserException {
        final XmlPullParser parser = createParser(namespaceAware, false);
        setInput(parser, input);
        return parser;
    }

    public static void setInput(final XmlPullParser parser, final InputStream input, final String inputEncoding) throws XmlPullParserException {
        try {
            final BOMInputStream bomIn = BOMInputStream.builder().setInputStream(input).get();
            setInput(parser, new InputStreamReader(bomIn, inputEncoding));
        } catch (IOException e) {
            throw new XmlPullParserException("Error setting input for parser", parser, e);
        }
    }

    public static void setInput(final XmlPullParser parser, final Reader input) throws XmlPullParserException {
        final Reader reader = new InvalidXMLCharacterFilterReader(input);
        parser.setInput(reader);
    }

    public static XmlPullParser createParser(final boolean namespaceAware, final boolean relaxed) throws XmlPullParserException {
        if (XPP_FACTORY == null) {
            throw new XmlPullParserException("XmlUtils: can't create XML Parser, no factory available");
        }

        synchronized (XPP_FACTORY) {
            final XmlPullParser parser = XPP_FACTORY.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, namespaceAware);
            parser.setFeature("http://xmlpull.org/v1/doc/features.html#relaxed", relaxed);
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

    @Nullable
    public static Date parseDate(final String input) {
        if (StringUtils.isBlank(input)) {
            return null;
        }
        String body = input.trim();
        body = PATTERN_MILLISECONDS.matcher(body).replaceFirst("");
        final String[] patterns = {
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ssXX",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd",
        };
        for (final String pattern : patterns) {
            final SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
            format.setLenient(false);
            format.setTimeZone(TimeZone.getDefault());
            final ParsePosition position = new ParsePosition(0);
            final Date parsed = format.parse(body, position);
            if (parsed != null && position.getIndex() == body.length()) {
                return parsed;
            }
        }
        return null;
    }

    @Nullable
    public static Float parseFloat(final String input) {
        if (StringUtils.isBlank(input)) {
            return null;
        }
        try {
            return Float.parseFloat(input.trim());
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    public static Geopoint parseGeopoint(final String lat, final String lon, final boolean zeroFill) {
        final Double latitude = parseCoordinate(lat, 90);
        final Double longitude = parseCoordinate(lon, 180);
        if (!zeroFill && (latitude == null || longitude == null || (latitude == 0 && longitude == 0))) {
            return null;
        }
        return new Geopoint(latitude == null ? 0 : latitude, longitude == null ? 0 : longitude);
    }

    @Nullable
    private static Double parseCoordinate(final String value, final int limit) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            final double coordinate = Double.parseDouble(value.trim());
            return Double.isFinite(coordinate) && Math.abs(coordinate) <= limit ? coordinate : null;
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    /** Reads the text content of the element the parser is currently positioned at (a {@code START_TAG}). */
    public static String parseText(final XmlPullParser parser) throws IOException, XmlPullParserException {
        final StringBuilder sb = new StringBuilder();
        performAction(parser, (p, event) -> {
            if (event == XmlPullParser.TEXT) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(p.getText());
            }
        });
        return sb.length() == 0 ? null : sb.toString();
    }

    /**
     * Skips the subtree of the element the parser is currently positioned at (a {@code START_TAG}), leaving the
     * parser positioned at the corresponding {@code END_TAG}.
     */
    public static void skipSubtree(final XmlPullParser parser) throws IOException, XmlPullParserException {
        performAction(parser, null);
    }

    /** performs an action for every subevent of a START tag (recursive) until END tag is reached */
    public static void performAction(final XmlPullParser parser, final BiConsumer<XmlPullParser, Integer> action) throws IOException, XmlPullParserException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("Parser not positioned at START_TAG");
        }
        int depth = 1;
        while (depth > 0) {
            final int event = parser.next();
            if (event == XmlPullParser.START_TAG) {
                depth++;
            } else if (event == XmlPullParser.END_TAG) {
                depth--;
            } else if (event == XmlPullParser.END_DOCUMENT) {
                throw new XmlPullParserException("Unexpected end of document while skipping subtree");
            } else if (action != null) {
                action.accept(parser, event);
            }
        }
    }

    public static String validate(final String input) {
        if ("nil".equalsIgnoreCase(input)) {
            return "";
        }
        return input.trim();
    }
}
