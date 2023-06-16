package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.PositionHistory;
import cgeo.geocaching.models.TrailHistoryElement;
import cgeo.geocaching.models.geoitem.GeoGroup;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;
import cgeo.geocaching.utils.MapLineUtils;

import android.location.Location;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;

public class PositionHistoryLayer {

    private final GeoItemLayer<String> layer;

    /**
     * maximum distance (in meters) up to which two points in the trail get connected by a drawn line
     */
    private static final float LINE_MAXIMUM_DISTANCE_METERS = 10000;

    private static final String KEY_HISTORY_LINE = "historyLine";

    Location lastPos = null;

    final UnifiedMapViewModel viewModel;


    private final GeoStyle lineStyle = GeoStyle.builder()
            .setStrokeColor(MapLineUtils.getTrailColor())
            .setStrokeWidth(MapLineUtils.getHistoryLineWidth(true))
            .build();

    public PositionHistoryLayer(final AppCompatActivity activity, final GeoItemLayer<String> layer) {
        this.layer = layer;

        viewModel = new ViewModelProvider(activity).get(UnifiedMapViewModel.class);

        viewModel.positionHistory.observe(activity, positionHistory -> drawHistory());

        viewModel.positionAndHeading.observe(activity, positionAndHeading -> {
            if (!positionAndHeading.first.equals(lastPos)) {
                lastPos = positionAndHeading.first;
                drawHistory();
            }
        });
    }

    private void drawHistory() {
        final PositionHistory history = viewModel.positionHistory.getValue();

        // only draw if position history is currently enabled. Remove possible old history line if not.
        if (history == null || history.getHistory().isEmpty()) {
            layer.remove(KEY_HISTORY_LINE);
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
        geoGroup.addItems(GeoPrimitive.createPolyline(segmentPoints, lineStyle).buildUpon().setZLevel(LayerHelper.ZINDEX_HISTORY).build());

        layer.put(KEY_HISTORY_LINE, geoGroup.build());
    }

}
