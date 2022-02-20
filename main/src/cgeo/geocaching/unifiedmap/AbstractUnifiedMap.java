package cgeo.geocaching.unifiedmap;

import androidx.appcompat.app.AppCompatActivity;

import org.oscim.core.BoundingBox;

import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;

public abstract class AbstractUnifiedMap {

    public abstract void init(AppCompatActivity activity);

    public abstract void setTileSource(AbstractTileProvider newSource);

    public void applyTheme() {
    };

    public abstract void zoomToBounds(BoundingBox bounds);

    // Lifecycle methods

    protected void onResume() {
    }

    protected void onPause() {
    }

    protected void onDestroy() {
    }
}
