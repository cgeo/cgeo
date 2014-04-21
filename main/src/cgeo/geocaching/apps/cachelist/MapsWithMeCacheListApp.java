package cgeo.geocaching.apps.cachelist;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.apps.AbstractApp;

import com.mapswithme.maps.api.MWMPoint;
import com.mapswithme.maps.api.MWMResponse;
import com.mapswithme.maps.api.MapsWithMeApi;

import org.eclipse.jdt.annotation.Nullable;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.List;

public class MapsWithMeCacheListApp extends AbstractApp implements CacheListApp {

    protected MapsWithMeCacheListApp() {
        super(getString(R.string.caches_map_mapswithme), R.id.cache_app_mapswithme, Intent.ACTION_VIEW);
    }

    @Override
    public boolean invoke(List<Geocache> caches, Activity activity, SearchResult search) {
        final MWMPoint[] points = new MWMPoint[caches.size()];
        for (int i = 0; i < points.length; i++) {
            Geocache geocache = caches.get(i);
            points[i] = new MWMPoint(geocache.getCoords().getLatitude(), geocache.getCoords().getLongitude(), geocache.getName(), geocache.getGeocode());
        }
        MapsWithMeApi.showPointsOnMap(activity, null, getPendingIntent(activity), points);
        return true;
    }

    @Override
    public boolean isInstalled() {
        // API can handle installing on the fly
        return true;
    }

    /**
     * get cache code from an invocation of MapsWithMe
     *
     * @return
     */
    @Nullable
    public static String getCacheFromMapsWithMe(final Context context, final Intent intent) {
        final MWMResponse mwmResponse = MWMResponse.extractFromIntent(context, intent);
        if (mwmResponse != null) {
            final MWMPoint point = mwmResponse.getPoint();
            return point.getId();
        }
        return null;
    }

    private static PendingIntent getPendingIntent(Context context) {
        final Intent intent = new Intent(context, CacheDetailActivity.class);
        return PendingIntent.getActivity(context, 0, intent, 0);
    }

}
