package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.maps.Tracks;
import cgeo.geocaching.models.geoitem.GeoGroup;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;
import cgeo.geocaching.utils.MapLineUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class TracksLayer {

    final UnifiedMapViewModel viewModel;

    public TracksLayer(final AppCompatActivity activity, final GeoItemLayer<String> layer) {
        viewModel = new ViewModelProvider(activity).get(UnifiedMapViewModel.class);

        viewModel.trackUpdater.observe(activity, event -> event.ifNotHandled((key -> {
            final Tracks.Track track = viewModel.getTracks().getTrack(key);
            if (track == null || track.getRoute().isHidden()) {
                layer.remove(key);
            } else {
                final GeoStyle lineStyle = GeoStyle.builder()
                        .setStrokeColor(track.getTrackfile().getColor())
                        .setStrokeWidth(MapLineUtils.getWidthFromRaw(track.getTrackfile().getWidth(), true))
                        .build();

                final GeoGroup.Builder geoGroup = GeoGroup.builder();
                for (GeoPrimitive segment : track.getRoute().getGeoData()) {
                    geoGroup.addItems(GeoPrimitive.createPolyline(segment.getPoints(), lineStyle).buildUpon().setZLevel(LayerHelper.ZINDEX_TRACK_ROUTE).build());
                }
                layer.put(key, geoGroup.build());
            }
        })));

    }

}
