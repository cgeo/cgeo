package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.maps.PositionHistory;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.AngleUtils;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import java.util.Objects;

public class LocUpdater extends GeoDirHandler {
    // use the following constants for fine tuning - find good compromise between smooth updates and as less updates as possible

    // minimum time in milliseconds between position overlay updates
    private static final long MIN_UPDATE_INTERVAL = 500;
    private static final long MIN_UPDATE_INTERVAL_AUTOROTATION = 100;
    private long timeLastPositionOverlayCalculation = 0;
    // minimum change of heading in grad for position overlay update
    private static final float MIN_HEADING_DELTA = 15f;
    // higher accuracy when rotating map by compass orientation
    private static final float MIN_HEADING_DELTA_AUTOROTATION = 1f;
    // minimum change of location in fraction of map width/height (whatever is smaller) for position overlay update
    private static final float MIN_LOCATION_DELTA = 0.01f;

    @NonNull
    Location currentLocation = LocationDataProvider.getInstance().currentGeo();
    float currentHeading;

    final UnifiedMapViewModel viewModel;


    public LocUpdater(@NonNull final UnifiedMapActivity mapActivity) {
        viewModel = new ViewModelProvider(mapActivity).get(UnifiedMapViewModel.class);
    }

    @Override
    public void updateGeoDir(@NonNull final GeoData geo, final float dir) {
        currentLocation = geo;
        currentHeading = AngleUtils.getDirectionNow(dir);
        repaintPositionOverlay();
    }

    /**
     * Repaint position overlay but only with a max frequency and if position or heading changes sufficiently.
     */
    void repaintPositionOverlay() {
        final long currentTimeMillis = System.currentTimeMillis();
        final boolean usesAutorotation = Settings.getMapRotation() == Settings.MAPROTATION_AUTO;
        if (currentTimeMillis > (timeLastPositionOverlayCalculation + (usesAutorotation ? MIN_UPDATE_INTERVAL_AUTOROTATION : MIN_UPDATE_INTERVAL))) {
            timeLastPositionOverlayCalculation = currentTimeMillis;
            final boolean needsRepaintForDistanceOrAccuracy = needsRepaintForDistanceOrAccuracy(viewModel);
            final boolean needsRepaintForHeading = needsRepaintForHeading(viewModel, usesAutorotation);

            if (needsRepaintForDistanceOrAccuracy) {
                final PositionHistory ph = viewModel.positionHistory.getValue();
                if (ph != null) {
                    ph.rememberTrailPosition(currentLocation);
                }
            }

            if (needsRepaintForDistanceOrAccuracy || needsRepaintForHeading) {
                viewModel.location.postValue(new LocationWrapper(currentLocation, currentHeading, needsRepaintForDistanceOrAccuracy, needsRepaintForHeading));
            }
        }
    }

    private boolean needsRepaintForHeading(final UnifiedMapViewModel viewModel, final boolean usesAutorotation) {
        final LocationWrapper locationWrapper = viewModel.location.getValue();
        if (locationWrapper == null) {
            return true;
        }
        return Math.abs(AngleUtils.difference(currentHeading, locationWrapper.heading)) > (usesAutorotation ? MIN_HEADING_DELTA_AUTOROTATION : MIN_HEADING_DELTA);
    }

    private boolean needsRepaintForDistanceOrAccuracy(final UnifiedMapViewModel viewModel) {
        final LocationWrapper locationWrapper = viewModel.location.getValue();
        if (locationWrapper == null || locationWrapper.location.getAccuracy() != currentLocation.getAccuracy()) {
            return true;
        }
        return currentLocation.distanceTo(locationWrapper.location) > MIN_LOCATION_DELTA;
    }

    public static class LocationWrapper {
        public final Location location;
        public final Float heading;
        public final boolean needsRepaintForDistanceOrAccuracy;
        public final boolean needsRepaintForHeading;

        public LocationWrapper(final Location location, final Float heading, final boolean needsRepaintForDistanceOrAccuracy, final boolean needsRepaintForHeading) {
            this.location = location;
            this.heading = heading;
            this.needsRepaintForDistanceOrAccuracy = needsRepaintForDistanceOrAccuracy;
            this.needsRepaintForHeading = needsRepaintForHeading;
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof LocationWrapper)) {
                return false;
            }
            final LocationWrapper p = (LocationWrapper) o;
            return Objects.equals(p.location, location) && Objects.equals(p.heading, heading);
        }

        @Override
        public int hashCode() {
            return (location == null ? 0 : location.hashCode()) ^ (heading == null ? 0 : heading.hashCode());
        }
    }
}
