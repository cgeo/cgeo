package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.PositionHistory;
import cgeo.geocaching.models.TrailHistoryElement;
import cgeo.geocaching.models.geoitem.GeoGroup;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;
import cgeo.geocaching.utils.MapLineUtils;

import android.location.Location;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;

import javax.annotation.Nullable;

public class PositionHistoryLayer {

    private final GeoItemLayer<String> layer;
    private boolean pathStored = true;

    /**
     * maximum distance (in meters) up to which two points in the trail get connected by a drawn line
     */
    private static final float LINE_MAXIMUM_DISTANCE_METERS = 10000;

    private static final String KEY_HISTORY_LINE = "historyLine";

    final UnifiedMapViewModel viewModel;


    private final GeoStyle lineStyle = GeoStyle.builder()
            .setStrokeColor(MapLineUtils.getTrailColor())
            .setStrokeWidth(MapLineUtils.getHistoryLineWidth(true))
            .build();

    public PositionHistoryLayer(final AppCompatActivity activity, final GeoItemLayer<String> layer) {
        this.layer = layer;

        viewModel = new ViewModelProvider(activity).get(UnifiedMapViewModel.class);

        viewModel.positionHistory.observe(activity, positionHistory -> drawHistory(null));

        viewModel.location.observe(activity, locationWrapper -> {
            if (locationWrapper.needsRepaintForDistanceOrAccuracy) {
                drawHistory(locationWrapper.location);
            }
        });
    }

    private void drawHistory(@Nullable final Location currentLoc) {
        // only draw if position history is currently enabled. Remove possible old history line if not.
        if (!Settings.isMapTrail()) {
            removePath();
            return;
        }

        final PositionHistory history = viewModel.positionHistory.getValue();

        // only draw if position history is non-empty. Remove possible old history line if not.
        if (history == null || history.getHistory().isEmpty()) {
            removePath();
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
        if (currentLoc != null) {
            segmentPoints.add(new Geopoint(currentLoc));
        }
        geoGroup.addItems(GeoPrimitive.createPolyline(segmentPoints, lineStyle).buildUpon().setZLevel(LayerHelper.ZINDEX_HISTORY).build());

        layer.put(KEY_HISTORY_LINE, geoGroup.build());
        pathStored = true;
    }

    private void removePath() {
        if (pathStored) {
            layer.remove(KEY_HISTORY_LINE);
            pathStored = false;
        }
    }

}
