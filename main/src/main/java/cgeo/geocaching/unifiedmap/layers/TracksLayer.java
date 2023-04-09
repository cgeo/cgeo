package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.models.geoitem.GeoGroup;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;
import cgeo.geocaching.unifiedmap.geoitemlayer.ILayer;
import cgeo.geocaching.unifiedmap.geoitemlayer.IProviderGeoItemLayer;
import cgeo.geocaching.utils.MapLineUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class TracksLayer implements ILayer {

    private final GeoItemLayer<String> geoItemLayer = new GeoItemLayer<>("tracks");

    final UnifiedMapViewModel viewModel;

    public TracksLayer(final AppCompatActivity activity) {
        viewModel = new ViewModelProvider(activity).get(UnifiedMapViewModel.class);

        viewModel.getTrackUpdater().observe(activity, event -> event.ifNotHandled((track -> {
            if (track.getRoute().isHidden()) {
                geoItemLayer.remove(track.getTrackfile().getKey());
            } else {
                final GeoStyle lineStyle = GeoStyle.builder()
                        .setStrokeColor(track.getTrackfile().getColor())
                        .setStrokeWidth(MapLineUtils.getWidthFromRaw(track.getTrackfile().getWidth(), true))
                        .build();

                final GeoGroup.Builder geoGroup = GeoGroup.builder();
                for (GeoPrimitive segment : track.getRoute().getGeoData()) {
                    geoGroup.addItems(GeoPrimitive.createPolyline(segment.getPoints(), lineStyle));
                }
                geoItemLayer.put(track.getTrackfile().getKey(), geoGroup.build());
            }
        })));

    }

    @Override
    public void init(final IProviderGeoItemLayer<?> provider) {
        geoItemLayer.setProvider(provider, LayerHelper.ZINDEX_TRACK_ROUTE);
    }

    @Override
    public void destroy() {
        geoItemLayer.destroy();
    }


}
