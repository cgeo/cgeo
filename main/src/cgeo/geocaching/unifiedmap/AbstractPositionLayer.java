package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import static cgeo.geocaching.settings.Settings.MAPROTATION_MANUAL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;

import java.util.Objects;

/**
 * layer for positioning the following elements on the map
 * - position and heading arrow
 * - position history
 * - direction line
 * - target and distance infos (target cache, distance to target, length of individual route)
 * - individual route
 * - track(s)
 */
public abstract class AbstractPositionLayer {

    protected Location currentLocation = null;
    protected float currentHeading = 0.0f;
    protected Bitmap positionAndHeadingArrow = BitmapFactory.decodeResource(CgeoApplication.getInstance().getResources(), R.drawable.my_location_chevron);
    protected int arrowWidth = positionAndHeadingArrow.getWidth();
    protected int arrowHeight = positionAndHeadingArrow.getHeight();

    // settings for map auto rotation
    private Location lastBearingCoordinates = null;
    private int mapRotation = MAPROTATION_MANUAL;

    public void setCurrentPositionAndHeading(final Location location, final float heading) {
        final boolean coordChanged = !Objects.equals(location, currentLocation);
        final boolean headingChanged = heading != currentHeading;
        currentLocation = location;
        currentHeading = heading;

        if (coordChanged || headingChanged) {
            repaintArrow();
        }

        if (coordChanged) {
            // history.rememberTrailPosition(coordinates);

            /*
            if (mapRotation == MAPROTATION_AUTO) {
                if (null != lastBearingCoordinates) {
                    final GoogleMap map = mapRef.get();
                    if (null != map) {
                        final CameraPosition currentCameraPosition = map.getCameraPosition();
                        final float bearing = AngleUtils.normalize(lastBearingCoordinates.bearingTo(coordinates));
                        final float bearingDiff = Math.abs(AngleUtils.difference(bearing, currentCameraPosition.bearing));
                        if (bearingDiff > 15.0f) {
                            lastBearingCoordinates = coordinates;
                            // adjust bearing of map, keep position and zoom level
                            final CameraPosition cameraPosition = new CameraPosition.Builder()
                                    .target(currentCameraPosition.target)
                                    .zoom(currentCameraPosition.zoom)
                                    .bearing(bearing)
                                    .tilt(0)
                                    .build();
                            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                        }
                    } else {
                        lastBearingCoordinates = null;
                    }
                } else {
                    lastBearingCoordinates = coordinates;
                }
            }

             */

        }
    }

    public void repaintRequired() {
        repaintArrow();
        repaintPosition();
        repaintHistory();
        repaintRouteAndTracks();
    }

    protected abstract void repaintArrow();
    protected abstract void repaintPosition();
    protected abstract void repaintHistory();
    protected abstract void repaintRouteAndTracks();

    public float getCurrentHeading() {
        return currentHeading;
    }
}
