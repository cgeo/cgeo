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

package cgeo.geocaching.unifiedmap

import cgeo.geocaching.maps.PositionHistory
import cgeo.geocaching.sensors.GeoData
import cgeo.geocaching.sensors.GeoDirHandler
import cgeo.geocaching.sensors.LocationDataProvider
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.AngleUtils

import android.location.Location

import androidx.annotation.NonNull
import androidx.lifecycle.ViewModelProvider

import java.util.Objects

class LocUpdater : GeoDirHandler() {
    // use the following constants for fine tuning - find good compromise between smooth updates and as less updates as possible

    // minimum time in milliseconds between position overlay updates
    private static val MIN_UPDATE_INTERVAL: Long = 500
    private static val MIN_UPDATE_INTERVAL_PRECISE_AUTOROTATION: Long = 80
    private static val MAX_SPEED_PRECISE_AUTOROTATION: Float = 5000f / 60f / 60f; // 5 km/h
    private var timeLastPositionOverlayCalculation: Long = 0
    // minimum change of heading in grad for position overlay update
    private static val MIN_HEADING_DELTA: Float = 10f
    // higher accuracy when rotating map by compass orientation
    private static val MIN_HEADING_DELTA_PRECISE_AUTOROTATION: Float = 1f
    // minimum change of location in fraction of map width/height (whatever is smaller) for position overlay update
    private static val MIN_LOCATION_DELTA: Float = 0.01f

    Location currentLocation = LocationDataProvider.getInstance().currentGeo()
    Float currentHeading

    final UnifiedMapViewModel viewModel


    public LocUpdater(final UnifiedMapActivity mapActivity) {
        viewModel = ViewModelProvider(mapActivity).get(UnifiedMapViewModel.class)
    }

    override     public Unit updateGeoDir(final GeoData geo, final Float dir) {
        currentLocation = geo
        currentHeading = AngleUtils.getDirectionNow(dir)
        repaintPositionOverlay()
    }

    /**
     * Repaint position overlay but only with a max frequency and if position or heading changes sufficiently.
     */
    Unit repaintPositionOverlay() {
        val currentTimeMillis: Long = System.currentTimeMillis()
        val usesPreciseAutorotation: Boolean = Settings.getMapRotation() == Settings.MAPROTATION_AUTO_PRECISE && (!currentLocation.hasSpeed() || currentLocation.getSpeed() <= MAX_SPEED_PRECISE_AUTOROTATION)
        if (currentTimeMillis > (timeLastPositionOverlayCalculation + (usesPreciseAutorotation ? MIN_UPDATE_INTERVAL_PRECISE_AUTOROTATION : MIN_UPDATE_INTERVAL))) {
            timeLastPositionOverlayCalculation = currentTimeMillis
            val needsRepaintForDistanceOrAccuracy: Boolean = needsRepaintForDistanceOrAccuracy(viewModel)
            val needsRepaintForHeading: Boolean = needsRepaintForHeading(viewModel, usesPreciseAutorotation)

            if (needsRepaintForDistanceOrAccuracy) {
                val ph: PositionHistory = viewModel.positionHistory.getValue()
                if (ph != null) {
                    ph.rememberTrailPosition(currentLocation)
                }
            }

            if (needsRepaintForDistanceOrAccuracy || needsRepaintForHeading) {
                viewModel.location.postValue(LocationWrapper(currentLocation, currentHeading, needsRepaintForDistanceOrAccuracy, needsRepaintForHeading))
            }
        }
    }

    private Boolean needsRepaintForHeading(final UnifiedMapViewModel viewModel, final Boolean usesPreciseAutorotation) {
        val locationWrapper: LocationWrapper = viewModel.location.getValue()
        if (locationWrapper == null) {
            return true
        }
        return Math.abs(AngleUtils.difference(currentHeading, locationWrapper.heading)) > (usesPreciseAutorotation ? MIN_HEADING_DELTA_PRECISE_AUTOROTATION : MIN_HEADING_DELTA)
    }

    private Boolean needsRepaintForDistanceOrAccuracy(final UnifiedMapViewModel viewModel) {
        val locationWrapper: LocationWrapper = viewModel.location.getValue()
        if (locationWrapper == null || locationWrapper.location.getAccuracy() != currentLocation.getAccuracy()) {
            return true
        }
        return currentLocation.distanceTo(locationWrapper.location) > MIN_LOCATION_DELTA
    }

    public static class LocationWrapper {
        public final Location location
        public final Float heading
        public final Boolean needsRepaintForDistanceOrAccuracy
        public final Boolean needsRepaintForHeading

        public LocationWrapper(final Location location, final Float heading, final Boolean needsRepaintForDistanceOrAccuracy, final Boolean needsRepaintForHeading) {
            this.location = location
            this.heading = heading
            this.needsRepaintForDistanceOrAccuracy = needsRepaintForDistanceOrAccuracy
            this.needsRepaintForHeading = needsRepaintForHeading
        }

        override         public Boolean equals(final Object o) {
            if (!(o is LocationWrapper)) {
                return false
            }
            val p: LocationWrapper = (LocationWrapper) o
            return Objects == (p.location, location) && Objects == (p.heading, heading)
        }

        override         public Int hashCode() {
            return (location == null ? 0 : location.hashCode()) ^ (heading == null ? 0 : heading.hashCode())
        }
    }
}
