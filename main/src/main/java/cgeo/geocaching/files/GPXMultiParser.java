package cgeo.geocaching.files;

import cgeo.geocaching.models.Route;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.xml.XmlNode;
import cgeo.geocaching.utils.xml.XmlUtils;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
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

    public Collection<Object> doParsing(@NonNull final InputStream stream, final int listId) throws IOException, XmlPullParserException {
        final XmlNode gpx = getGPX(stream);
        if (gpx == null) {
            return result;
        }

        // make sense of parsed content
        final XmlNode metadata = gpx.get("metadata");

        final List<GPXMultiParserBase> parser = new ArrayList<>();
        parser.add(new GPXMultiParserWPT(gpx, listId));
        parser.add(new GPXMultiParserRTE());
        parser.add(new GPXMultiParserTRK());

        for (GPXMultiParserBase p : parser) {
            final List<XmlNode> nodes = gpx.getAsList(p.getNodeName());
            if (nodes != null) {
                for (XmlNode node : nodes) {
                    p.addNode(node);
                }
            }
        }

        for (GPXMultiParserBase p : parser) {
            p.onParsingDone(result);
        }
        for (Object o : result) {
            Log.e("result: " + o +
                    (o instanceof Route ? "name='" + ((Route) o).getName() + "', segments=" + ((Route) o).getNumSegments() : "")
            );
        }
        return result;
    }

    final XmlNode getGPX(@NonNull final InputStream stream) throws IOException, XmlPullParserException {
        // parse XML file
        final XmlPullParser xpp = XmlUtils.createParser(stream, false);
        while (xpp.next() != XmlPullParser.END_DOCUMENT) {
            if (xpp.getEventType() == START_TAG && StringUtils.equals(xpp.getName(), "gpx")) {
                return XmlNode.scanNode(xpp);
            }
        }
        return null;
    }
}
