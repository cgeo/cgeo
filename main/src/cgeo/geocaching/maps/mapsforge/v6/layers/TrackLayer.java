package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.models.Route;
import cgeo.geocaching.utils.MapLineUtils;

public class TrackLayer extends AbstractRouteLayer implements Route.UpdateRoute {

    public TrackLayer(final boolean isHidden) {
        this.isHidden = isHidden;
        width = MapLineUtils.getTrackLineWidth();
        resetColor();
    }

    public void resetColor() {
        lineColor = MapLineUtils.getTrackColor();
        super.resetColor();
    }

}
