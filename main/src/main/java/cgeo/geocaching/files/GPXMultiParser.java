package cgeo.geocaching.files;

import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.utils.Log;

import android.sax.RootElement;
import android.util.Xml;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.xml.sax.SAXException;

public class GPXMultiParser {

    // @todo: return type needs to be changed to a super type of Geocache and Route (the latter being a super type for tracks and routes already)
    private final Collection<Object> result = new ArrayList<>();

    public Collection<Object> doParsing(@NonNull final InputStream stream, final boolean version11) throws IOException, ParserException {
        final String namespace = "http://www.topografix.com/GPX/1/" + (version11 ? "1" : "0");

        final RootElement root = new RootElement(namespace, "gpx");
        root.setStartElementListener(attributes -> Log.e("start GPX"));

        final List<IGPXMultiParser> parser = new ArrayList<>();
        parser.add(new GPXMultiParserCaches(root, namespace, version11, StoredList.STANDARD_LIST_ID, null)); // @todo: listId
        parser.add(new GPXMultiParserRoutes(root, namespace));
        parser.add(new GPXMultiParserTracks(root, namespace));

        try {
            final ProgressInputStream progressStream = new ProgressInputStream(stream);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(progressStream, StandardCharsets.UTF_8));
            Xml.parse(new InvalidXMLCharacterFilterReader(reader), root.getContentHandler());
            for (IGPXMultiParser p : parser) {
                p.onParsingDone(result);
            }
            for (Object o : result) {
                Log.e("result: " + o);
            }
            return result;
        } catch (final SAXException e) {
            throw new ParserException("Cannot parse .gpx file as GPX " + namespace + ": could not parse XML", e);
        }
    }
}
