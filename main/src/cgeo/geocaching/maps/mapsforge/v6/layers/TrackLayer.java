package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.maps.Tracks;
import cgeo.geocaching.utils.MapLineUtils;

public class TrackLayer extends AbstractRouteLayer implements Tracks.UpdateTrack {

    public TrackLayer() {
        width = MapLineUtils.getTrackLineWidth(false);
        resetColor();
    }

    public void resetColor() {
        lineColor = MapLineUtils.getTrackColor();
        super.resetColor();
    }

}
