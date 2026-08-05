package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;

import androidx.core.content.res.ResourcesCompat;

import java.util.Objects;

import javax.annotation.Nullable;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rotation;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.overlay.Marker;

public class CoordsMarkerLayer extends Layer {

    Marker coordsMarker;

    public void setCoordsMarker(@Nullable final Geopoint coords) {
        if (coordsMarker != null) {
            coordsMarker = null;
        }
        if (coords != null && coords.isValid()) {
            coordsMarker = new Marker(new LatLong(coords.getLatitude(), coords.getLongitude()), AndroidGraphicFactory.convertToBitmap(Objects.requireNonNull(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.coords_indicator, null))), 0, 0);
        }
    }

    @Override
    protected void onAdd() {
        if (coordsMarker != null) {
            coordsMarker.setDisplayModel(this.displayModel);
        }
    }

    @Override
    public void onDestroy() {
        if (coordsMarker != null) {
            coordsMarker.onDestroy();
        }
    }

    @Override
    public void draw(final BoundingBox boundingBox, final byte zoomLevel, final Canvas canvas, final Point topLeftPoint, final Rotation rotation) {
        if (coordsMarker != null) {
            coordsMarker.draw(boundingBox, zoomLevel, canvas, topLeftPoint, rotation);
        }
    }
}
