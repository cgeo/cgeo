package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.utils.MapLineUtils;
import cgeo.geocaching.utils.TrackUtils;

public class TrackLayer extends AbstractLineLayer implements TrackUtils.TrackUpdaterSingle {

    public TrackLayer(final boolean isHidden) {
        this.isHidden = isHidden;
        lineColor = MapLineUtils.getTrackColor();
        width = MapLineUtils.getTrackLineWidth();
    }

    public void updateTrack(final TrackUtils.Track track) {
        super.updateTrack(track.getTrack());
    };

}
