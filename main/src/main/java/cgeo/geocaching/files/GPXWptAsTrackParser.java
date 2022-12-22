package cgeo.geocaching.files;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.RouteSegment;

import android.sax.Element;
import android.sax.RootElement;
import android.util.Xml;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

/**
 * parses GPX file with only <wpt> elements as track points
 * (files like this are created by flopp.net and others)
 */
public class GPXWptAsTrackParser implements AbstractTrackOrRouteParser.RouteParse {

    protected final String namespace;
    private final String version;
    protected final Route result;

    // temporary variables
    protected ArrayList<Geopoint> temp;
    protected Element point;

    protected GPXWptAsTrackParser(final String namespaceIn, final String versionIn) {
        namespace = namespaceIn;
        version = versionIn;
        result = new Route(false);
    }

    @NonNull
    public Route parse(@NonNull final InputStream stream) throws IOException, ParserException {
        final RootElement root = new RootElement(namespace, "gpx");
        point = root.getChild(namespace, "wpt");

        root.setStartElementListener(attrs -> temp = new ArrayList<>());

        root.setEndElementListener(() -> {
            if (temp.size() > 0) {
                result.add(new RouteSegment(new RouteItem(temp.get(temp.size() - 1)), temp, false));
                temp = null;
            }
        });

        point.setStartElementListener(attrs -> {
            if (attrs.getIndex("lat") > -1 && attrs.getIndex("lon") > -1) {
                final String latitude = attrs.getValue("lat");
                final String longitude = attrs.getValue("lon");
                if (StringUtils.isNotBlank(latitude) && StringUtils.isNotBlank(longitude)) {
                    temp.add(new Geopoint(Double.parseDouble(latitude), Double.parseDouble(longitude)));
                }
            }
        });

        try {
            final ProgressInputStream progressStream = new ProgressInputStream(stream);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(progressStream, StandardCharsets.UTF_8));
            Xml.parse(new InvalidXMLCharacterFilterReader(reader), root.getContentHandler());
            return result;
        } catch (final SAXException e) {
            throw new ParserException("Cannot parse .gpx file as GPX " + version + ": could not parse XML (" + e.getMessage() + ")", e);
        }
    }

}
