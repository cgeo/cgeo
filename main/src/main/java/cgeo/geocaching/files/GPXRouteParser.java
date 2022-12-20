package cgeo.geocaching.files;

import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.RouteSegment;

import android.sax.RootElement;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class GPXRouteParser extends AbstractTrackOrRouteParser implements AbstractTrackOrRouteParser.RouteParse {

    protected GPXRouteParser(final String namespaceIn, final String versionIn) {
        super(namespaceIn, versionIn, true);
    }

    @NonNull
    public Route parse(@NonNull final InputStream stream) throws IOException, ParserException {
        final RootElement root = new RootElement(namespace, "gpx");
        points = root.getChild(namespace, "rte");
        point = points.getChild(namespace, "rtept");

        point.setEndElementListener(() -> {
            if (temp.size() > 0) {
                result.add(new RouteSegment(new RouteItem(temp.get(temp.size() - 1)), temp, false));
                temp = new ArrayList<>();
            }
        });

        return super.parse(stream, root);
    }
}
