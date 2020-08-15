package cgeo.geocaching.files;

import cgeo.geocaching.models.Route;

import android.sax.RootElement;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;

public class GPXRouteParser extends AbstractTrackOrRouteParser implements AbstractTrackOrRouteParser.RouteParse {

    protected GPXRouteParser(final String namespaceIn, final String versionIn) {
        super(namespaceIn, versionIn);
    }

    @NonNull
    public Route parse(@NonNull final InputStream stream) throws IOException, ParserException {
        final RootElement root = new RootElement(namespace, "gpx");
        points = root.getChild(namespace, "rte");
        point = points.getChild(namespace, "rtept");

        return super.parse(stream, root);
    }
}
