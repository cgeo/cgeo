package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.utils.MapLineUtils;
import cgeo.geocaching.utils.TrackUtils;

public class TrackLayer extends AbstractLineLayer implements TrackUtils.TrackUpdaterSingle {

    public TrackLayer(final boolean isHidden) {
        this.isHidden = isHidden;
        width = MapLineUtils.getTrackLineWidth();
        resetColor();
    }

    public void resetColor() {
        lineColor = MapLineUtils.getTrackColor();
        super.resetColors();
    }

    public void updateTrack(final TrackUtils.Track track) {
        super.updateTrack(null != track ? track.getTrack() : null);
    }

}
