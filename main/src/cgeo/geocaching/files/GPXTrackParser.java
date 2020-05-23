package cgeo.geocaching.files;

import cgeo.geocaching.location.Geopoint;

import android.sax.Element;
import android.sax.RootElement;
import android.util.Xml;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

public class GPXTrackParser {

    protected final String namespace;
    private final String version;
    private final LinkedList<Track> result = new LinkedList<>();

    // temporary variables
    private Track temp;

    public static class Track {
        public String trackName;
        public LinkedList<Geopoint> track;

        public Track() {
            trackName = "";
            track = new LinkedList<>();
        }
    }

    protected GPXTrackParser(final String namespaceIn, final String versionIn) {
        namespace = namespaceIn;
        version = versionIn;
    }

    @NonNull
    public LinkedList<Track> parse(@NonNull final InputStream stream) throws IOException, ParserException {
        final RootElement root = new RootElement(namespace, "gpx");
        final Element track = root.getChild(namespace, "trk");
        final Element trackSegment = track.getChild(namespace, "trkseg");
        final Element trackPoint = trackSegment.getChild(namespace, "trkpt");

        track.setStartElementListener(attrs -> startNewTrack());
        track.getChild(namespace, "name").setEndTextElementListener(body -> temp.trackName = body);
        track.setEndElementListener(() -> {
            result.add(temp);
            temp = null;
        });

        trackPoint.setStartElementListener(attrs -> {
            if (attrs.getIndex("lat") > -1 && attrs.getIndex("lon") > -1) {
                final String latitude = attrs.getValue("lat");
                final String longitude = attrs.getValue("lon");
                if (StringUtils.isNotBlank(latitude) && StringUtils.isNotBlank(longitude)) {
                    temp.track.add(new Geopoint(Double.parseDouble(latitude), Double.parseDouble(longitude)));
                }
            }
        });

        try {
            final ProgressInputStream progressStream = new ProgressInputStream(stream);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(progressStream, CharEncoding.UTF_8));
            Xml.parse(new InvalidXMLCharacterFilterReader(reader), root.getContentHandler());
            return result;
        } catch (final SAXException e) {
            throw new ParserException("Cannot parse .gpx file as GPX " + version + ": could not parse XML", e);
        }
    }

    private void startNewTrack() {
        temp = new Track();
    }

}

