package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.unifiedmap.AbstractMapFragment;
import cgeo.geocaching.unifiedmap.googlemaps.GoogleMapsFragment;
import cgeo.geocaching.utils.Log;

import androidx.annotation.StringRes;
import androidx.core.util.Pair;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapsSdkInitializedCallback;

public class AbstractGoogleTileProvider extends AbstractTileProvider implements OnMapsSdkInitializedCallback {

    final int mapType;

    AbstractGoogleTileProvider(final int mapType, final @StringRes int nameRes) {
        super(2, 21, new Pair<>("", false));
        this.mapType = mapType;
        this.tileProviderName = CgeoApplication.getInstance().getString(nameRes);
        this.supportsThemeOptions = true;
    }

    public void setMapType(final GoogleMap googleMap) {
        if (googleMap != null) {
            googleMap.setMapType(mapType);
        }
    }

    public int getMapType() {
        return mapType;
    }

    @Override
    public AbstractMapFragment createMapFragment() {
        MapsInitializer.initialize(CgeoApplication.getInstance(), MapsInitializer.Renderer.LATEST, this);
        return new GoogleMapsFragment();
    }

    @Override
    public void onMapsSdkInitialized(final MapsInitializer.Renderer renderer) {
        switch (renderer) {
            case LATEST:
                Log.d("GMv2: The latest version of the renderer is used.");
                break;
            case LEGACY:
                Log.d("GMv2: The legacy version of the renderer is used.");
                break;
            default:
                // to make Codacy happy...
                Log.w("GMv2: Unknown renderer version used, neither LATEST nor LEGACY.");
                break;
        }
    }
}
