package cgeo.geocaching.files;

import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.xml.XmlNode;
import cgeo.geocaching.utils.xml.XmlUtils;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

public class GPXMultiParser {

    // @todo: return type needs to be changed to a super type of Geocache and Route (the latter being a super type for tracks and routes already)
    private final Collection<Object> result = new ArrayList<>();

    public Collection<Object> doParsing(@NonNull final InputStream stream, final boolean version11) throws IOException, XmlPullParserException {
        final String namespace = "http://www.topografix.com/GPX/1/" + (version11 ? "1" : "0");

        final List<GPXMultiParserBase> parser = new ArrayList<>();
        // parser.add(new GPXMultiParserCaches(root, namespace, version11, StoredList.STANDARD_LIST_ID, null)); // @todo: listId
        parser.add(new GPXMultiParserRoutes());
        // parser.add(new GPXMultiParserTracks(root, namespace));

        final XmlPullParser xpp = XmlUtils.createParser(new InputStreamReader(stream, StandardCharsets.UTF_8), true);
        while (xpp.next() != XmlPullParser.END_DOCUMENT) {
            if (xpp.getEventType() == START_TAG) {
                if (!StringUtils.equals(xpp.getName(), "gpx")) {
                    boolean tagHandled = false;
                    for (GPXMultiParserBase p : parser) {
                        if (p.handlesNode(xpp.getName())) {
                            p.addNode(XmlNode.scanNode(xpp));
                            tagHandled = true;
                        }
                    }
                    if (!tagHandled) {
                        // skip data
                        XmlNode.scanNode(xpp);
                    }
                }
            }
        }
        for (GPXMultiParserBase p : parser) {
            p.onParsingDone(result);
        }
        for (Object o : result) {
            Log.e("result: " + o);
        }
        return result;
    }
}
