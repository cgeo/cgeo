package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.unifiedmap.UnifiedMapActivity;
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;
import cgeo.geocaching.utils.MapLineUtils;

import android.location.Location;

import androidx.lifecycle.ViewModelProvider;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

public class NavigationTargetLayer {

    private static final String KEY_TARGET_PATH = "TARGETPATH";

    private final GeoStyle lineStyle = GeoStyle.builder()
            .setStrokeColor(MapLineUtils.getDirectionColor())
            .setStrokeWidth(MapLineUtils.getDirectionLineWidth(true))
            .build();

    final UnifiedMapViewModel viewModel;
    private final UnifiedTargetAndDistancesHandler mapDistanceDrawer;
    final GeoItemLayer<String> layer;

    private final boolean showBothDistances = Settings.isBrouterShowBothDistances();

    private Location currentLocation = null;

    public NavigationTargetLayer(final UnifiedMapActivity activity, final GeoItemLayer<String> layer) {
        mapDistanceDrawer = new UnifiedTargetAndDistancesHandler(activity.findViewById(R.id.distanceinfo));
        viewModel = new ViewModelProvider(activity).get(UnifiedMapViewModel.class);
        this.layer = layer;

        viewModel.target.observe(activity, target -> {
            mapDistanceDrawer.setLastNavTarget(target.geopoint);

            if (StringUtils.isNotBlank(target.geocode)) {
                mapDistanceDrawer.setTargetGeocode(target.geocode);
                final Geocache targetCache = activity.getCurrentTargetCache();
                mapDistanceDrawer.setTarget(targetCache != null ? targetCache.getName() : StringUtils.EMPTY);
                if (target.geopoint == null && targetCache != null) {
                    mapDistanceDrawer.setLastNavTarget(targetCache.getCoords());
                }
            } else {
                mapDistanceDrawer.setTargetGeocode(null);
                mapDistanceDrawer.setTarget(null);
                mapDistanceDrawer.drawDistance(showBothDistances, 0, 0);
            }

            repaintHelper(target);
        });

        viewModel.positionAndHeading.observe(activity, locationFloatPair -> {
            if (currentLocation == null || !currentLocation.equals(locationFloatPair.first)) {
                currentLocation = locationFloatPair.first;
                repaintHelper(viewModel.target.getValue());
            }
        });

        viewModel.individualRoute.observe(activity, individualRoute -> mapDistanceDrawer.drawRouteDistance(individualRoute.getDistance()));
    }


    private void repaintHelper(final UnifiedMapViewModel.Target target) {

        if (currentLocation != null && target.geopoint != null) {
            final Geopoint currentGp = new Geopoint(currentLocation.getLatitude(), currentLocation.getLongitude());
            final Geopoint[] routingPoints = Routing.getTrack(currentGp, target.geopoint);

            float routedDistance = 0.0f;
            if (routingPoints.length > 2 || Settings.isMapDirection()) {
                for (int i = 1; i < routingPoints.length; i++) {
                    routedDistance += routingPoints[i - 1].distanceTo(routingPoints[i]);
                }
            }

            layer.put(KEY_TARGET_PATH, GeoPrimitive.createPolyline(Arrays.asList(routingPoints), lineStyle).buildUpon()
                    .setZLevel(LayerHelper.ZINDEX_DIRECTION_LINE).build());

            mapDistanceDrawer.drawDistance(showBothDistances, currentGp.distanceTo(target.geopoint), routedDistance);

        }

        if (target.geopoint == null) {
            layer.remove(KEY_TARGET_PATH);
        }
    }
}
