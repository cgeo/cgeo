package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.PositionHistory;
import cgeo.geocaching.models.TrailHistoryElement;
import cgeo.geocaching.models.geoitem.GeoGroup;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;
import cgeo.geocaching.unifiedmap.geoitemlayer.ILayer;
import cgeo.geocaching.unifiedmap.geoitemlayer.IProviderGeoItemLayer;
import cgeo.geocaching.utils.MapLineUtils;

import android.location.Location;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;

public class PositionHistoryLayer implements ILayer {

    private final GeoItemLayer<String> geoItemLayer = new GeoItemLayer<>("history");

    /**
     * maximum distance (in meters) up to which two points in the trail get connected by a drawn line
     */
    private static final float LINE_MAXIMUM_DISTANCE_METERS = 10000;

    Location lastPos = null;

    final UnifiedMapViewModel viewModel;


    private final GeoStyle lineStyle = GeoStyle.builder()
            .setStrokeColor(MapLineUtils.getTrailColor())
            .setStrokeWidth(MapLineUtils.getHistoryLineWidth(true))
            .build();

    public PositionHistoryLayer(final AppCompatActivity activity) {
        viewModel = new ViewModelProvider(activity).get(UnifiedMapViewModel.class);

        viewModel.getPositionHistory().observe(activity, positionHistory -> drawHistory());

        viewModel.getPositionAndHeading().observe(activity, positionAndHeading -> {
            if (!positionAndHeading.first.equals(lastPos)) {
                lastPos = positionAndHeading.first;
                drawHistory();
            }
        });
    }

    private void drawHistory() {
        PositionHistory history = viewModel.getPositionHistory().getValue();

        // only draw if position history is currently enabled. Remove possible old history line if not.
        if (history == null || history.getHistory().isEmpty()) {
            geoItemLayer.remove("historyLine");
            return;
        }

        final ArrayList<TrailHistoryElement> historyElements = history.getHistory();
        final GeoGroup.Builder geoGroup = GeoGroup.builder();

        ArrayList<Geopoint> segmentPoints = new ArrayList<>();

        segmentPoints.add(new Geopoint(historyElements.get(0).getLocation()));
        for (int i = 1; i < historyElements.size(); i++) {
            if (historyElements.get(i).getLocation().distanceTo(historyElements.get(i - 1).getLocation()) > LINE_MAXIMUM_DISTANCE_METERS) {
                geoGroup.addItems(GeoPrimitive.createPolyline(segmentPoints, lineStyle));
                segmentPoints = new ArrayList<>();
            }
            segmentPoints.add(new Geopoint(historyElements.get(i).getLocation()));
        }
        // always add current position
        if (lastPos != null) {
            segmentPoints.add(new Geopoint(lastPos));
        }
        geoGroup.addItems(GeoPrimitive.createPolyline(segmentPoints, lineStyle));

        geoItemLayer.put("historyLine", geoGroup.build());
    }

    @Override
    public void init(final IProviderGeoItemLayer<?> provider) {
        geoItemLayer.setProvider(provider, LayerHelper.ZINDEX_HISTORY);
    }

    @Override
    public void destroy() {
        geoItemLayer.destroy();
    }


}
