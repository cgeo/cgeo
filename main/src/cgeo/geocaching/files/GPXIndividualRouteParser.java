package cgeo.geocaching.files;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.enumerations.CoordinatesType;
import cgeo.geocaching.maps.routing.RouteItem;

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

import org.xml.sax.SAXException;

class GPXIndividualRouteParser {
    protected final String namespace;
    private final String version;
    private final ArrayList<RouteItem> routeItems = new ArrayList<>();

    protected GPXIndividualRouteParser(final String namespaceIn, final String versionIn) {
        namespace = namespaceIn;
        version = versionIn;
    }

    @NonNull
    public ArrayList<RouteItem> parse(@NonNull final InputStream stream) throws IOException, ParserException {
        final RootElement root = new RootElement(namespace, "gpx");
        final Element route = root.getChild(namespace, "rte");
        final Element routePoint = route.getChild(namespace, "rtept");

        routePoint.getChild(namespace, "name").setEndTextElementListener(body -> routeItems.add(new RouteItem(ConnectorFactory.canHandle(body) ? CoordinatesType.CACHE : CoordinatesType.WAYPOINT, body)));

        try {
            final ProgressInputStream progressStream = new ProgressInputStream(stream);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(progressStream, StandardCharsets.UTF_8));
            Xml.parse(new InvalidXMLCharacterFilterReader(reader), root.getContentHandler());
            return routeItems;
        } catch (final SAXException e) {
            throw new ParserException("Cannot parse .gpx file as GPX " + version + ": could not parse XML", e);
        }
    }

}
