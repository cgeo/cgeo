package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.maps.Tracks;
import cgeo.geocaching.utils.MapLineUtils;

import org.mapsforge.core.graphics.Paint;

public class TrackLayer extends AbstractRouteLayer implements Tracks.UpdateTrack {

    public TrackLayer() {
        width = MapLineUtils.getTrackLineWidth(false);
        resetColor(MapLineUtils.getTrackColor());
    }

    @Override
    public Paint resetColor(final int lineColor) {
        return super.resetColor(lineColor);
    }

}
