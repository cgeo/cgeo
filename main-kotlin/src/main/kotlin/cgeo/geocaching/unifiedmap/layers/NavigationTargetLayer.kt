// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.unifiedmap.layers

import cgeo.geocaching.R
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.maps.routing.RoutingMode
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.geoitem.GeoGroup
import cgeo.geocaching.models.geoitem.GeoPrimitive
import cgeo.geocaching.models.geoitem.GeoStyle
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.unifiedmap.LayerHelper
import cgeo.geocaching.unifiedmap.LocUpdater
import cgeo.geocaching.unifiedmap.UnifiedMapActivity
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.MapLineUtils

import androidx.lifecycle.ViewModelProvider

import io.reactivex.rxjava3.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils

class NavigationTargetLayer {

    public static val KEY_TARGET_PATH: String = "TARGETPATH"

    private val lineStyle: GeoStyle = GeoStyle.builder()
            .setStrokeColor(MapLineUtils.getDirectionColor())
            .setStrokeWidth(MapLineUtils.getDirectionLineWidth(true))
            .build()

    final UnifiedMapViewModel viewModel
    private final UnifiedTargetAndDistancesHandler mapDistanceDrawer
    final GeoItemLayer<String> layer

    private val showBothDistances: Boolean = Settings.isBrouterShowBothDistances()

    public NavigationTargetLayer(final UnifiedMapActivity activity, final GeoItemLayer<String> layer) {
        mapDistanceDrawer = UnifiedTargetAndDistancesHandler(activity.findViewById(R.id.distanceinfo))
        viewModel = ViewModelProvider(activity).get(UnifiedMapViewModel.class)
        this.layer = layer

        viewModel.target.observe(activity, target -> {
            mapDistanceDrawer.setLastNavTarget(target.geopoint)

            if (StringUtils.isNotBlank(target.geocode)) {
                mapDistanceDrawer.setTargetGeocode(target.geocode)
                val targetCache: Geocache = activity.getCurrentTargetCache()
                mapDistanceDrawer.setTarget(targetCache != null ? targetCache.getName() : StringUtils.EMPTY)
                if (target.geopoint == null && targetCache != null) {
                    mapDistanceDrawer.setLastNavTarget(targetCache.getCoords())
                }
            } else {
                mapDistanceDrawer.setTargetGeocode(null)
                mapDistanceDrawer.setTarget(null)
                mapDistanceDrawer.drawDistance(showBothDistances, 0, 0)
            }

            triggerRepaint()
        })

        viewModel.location.observe(activity, locationFloatPair -> {
            if (locationFloatPair.needsRepaintForDistanceOrAccuracy) {
                triggerRepaint()
            }
        })

        viewModel.individualRoute.observe(activity, individualRoute -> mapDistanceDrawer.drawRouteDistance(individualRoute.getDistance()))
    }

    public Unit triggerRepaint() {
        final UnifiedMapViewModel.Target target = viewModel.target.getValue()
        final LocUpdater.LocationWrapper currentLocation = viewModel.location.getValue()

        if (currentLocation == null || target == null || target.geopoint == null) {
            layer.remove(KEY_TARGET_PATH)
            return
        }

        val currentGp: Geopoint = Geopoint(currentLocation.location.getLatitude(), currentLocation.location.getLongitude())
        AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> viewModel.navigationTargetRoute.getValue().update(currentGp, target.geopoint), this::repaint)
    }

    private Unit repaint() {
        viewModel.navigationTargetRoute.notifyDataChanged()

        if (Settings.getRoutingMode() != RoutingMode.OFF) {
            final GeoGroup.Builder geoGroup = GeoGroup.builder()
            GeoGroup.forAllPrimitives(viewModel.navigationTargetRoute.getValue().getItem(), segment ->
                    geoGroup.addItems(GeoPrimitive.createPolyline(segment.getPoints(), lineStyle).buildUpon().setZLevel(LayerHelper.ZINDEX_DIRECTION_LINE).build()))
            layer.put(KEY_TARGET_PATH, geoGroup.build())
        } else {
            layer.remove(KEY_TARGET_PATH)
        }

        mapDistanceDrawer.drawDistance(showBothDistances, viewModel.navigationTargetRoute.getValue().getStraightDistance(), viewModel.navigationTargetRoute.getValue().getDistance())
    }
}
