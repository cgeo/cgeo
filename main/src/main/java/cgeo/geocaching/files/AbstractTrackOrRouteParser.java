package cgeo.geocaching.files;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Route;

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

abstract class AbstractTrackOrRouteParser {

    protected final String namespace;
    private final String version;
    protected final Route result;

    // temporary variables
    protected ArrayList<Geopoint> temp;
    protected ArrayList<Float> tempElevation;
    protected Element points;
    protected Element point;

    // when using this constructor, you should call result.setRoutable() manually
    protected AbstractTrackOrRouteParser(final String namespaceIn, final String versionIn) {
        namespace = namespaceIn;
        version = versionIn;
        result = new Route();
    }

    protected AbstractTrackOrRouteParser(final String namespaceIn, final String versionIn, final boolean routeable) {
        namespace = namespaceIn;
        version = versionIn;
        result = new Route(routeable);
    }

    interface RouteParse {
        Route parse(@NonNull InputStream stream) throws IOException, ParserException;
    }

    @NonNull
    public Route parse(@NonNull final InputStream stream, final RootElement root) throws IOException, ParserException {
        // if you do not call this method, you need to call the following steps individually or implement a replacement
        points.setStartElementListener(attrs -> resetTempData());
        setNameAndLatLonParsers();
        return doParsing(stream, root);
    }

    protected void resetTempData() {
        temp = new ArrayList<>();
        tempElevation = new ArrayList<>();
    }

    protected void setNameAndLatLonParsers() {
        points.getChild(namespace, "name").setEndTextElementListener(result::setName);

        point.setStartElementListener(attrs -> {
            if (attrs.getIndex("lat") > -1 && attrs.getIndex("lon") > -1) {
                final String latitude = attrs.getValue("lat");
                final String longitude = attrs.getValue("lon");
                if (StringUtils.isNotBlank(latitude) && StringUtils.isNotBlank(longitude)) {
                    temp.add(new Geopoint(Double.parseDouble(latitude), Double.parseDouble(longitude)));
                }
            }
        });
        point.getChild(namespace, "ele").setEndTextElementListener(el -> tempElevation.add(Float.parseFloat(el)));
    }

    protected Route doParsing(@NonNull final InputStream stream, final RootElement root) throws IOException, ParserException {
        try {
            final ProgressInputStream progressStream = new ProgressInputStream(stream);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(progressStream, StandardCharsets.UTF_8));
            Xml.parse(new InvalidXMLCharacterFilterReader(reader), root.getContentHandler());
            return result;
        } catch (final SAXException e) {
            throw new ParserException("Cannot parse .gpx file as GPX " + version + ": could not parse XML", e);
        }
    }

}
