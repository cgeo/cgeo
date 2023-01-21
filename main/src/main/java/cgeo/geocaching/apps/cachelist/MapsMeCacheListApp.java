package cgeo.geocaching.apps.cachelist;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.apps.AbstractApp;
import cgeo.geocaching.models.Geocache;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import com.mapswithme.maps.api.MWMPoint;
import com.mapswithme.maps.api.MWMResponse;
import com.mapswithme.maps.api.MapsWithMeApi;
import org.apache.commons.lang3.StringUtils;

public class MapsMeCacheListApp extends AbstractApp implements CacheListApp {

    protected MapsMeCacheListApp() {
        super(getString(R.string.caches_map_mapswithme), Intent.ACTION_VIEW);
    }

    @Override
    public void invoke(@NonNull final List<Geocache> caches, @NonNull final Activity activity, @NonNull final SearchResult search) {
        final MWMPoint[] points = new MWMPoint[caches.size()];
        for (int i = 0; i < points.length; i++) {
            final Geocache geocache = caches.get(i);
            points[i] = new MWMPoint(geocache.getCoords().getLatitude(), geocache.getCoords().getLongitude(), geocache.getName(), geocache.getGeocode());
        }
        MapsWithMeApi.showPointsOnMap(activity, null, getPendingIntent(activity), points);
    }

    @Override
    public boolean isInstalled() {
        // API can handle installing on the fly
        return true;
    }

    /**
     * get cache code from a PendingIntent after an invocation of MapsWithMe
     */
    @Nullable
    public static String getCacheFromMapsWithMe(final Context context, final Intent intent) {
        final MWMResponse mwmResponse = MWMResponse.extractFromIntent(context, intent);
        final MWMPoint point = mwmResponse.getPoint();
        if (point != null) {
            final String id = point.getId();
            // for unknown reason the ID is now actually a URI in recent maps.me versions
            if (StringUtils.contains(id, "&id=")) {
                return StringUtils.substringAfter(id, "&id=");
            }
            return id;
        }
        return null;
    }

    private static PendingIntent getPendingIntent(final Context context) {
        final Intent intent = new Intent(context, CacheDetailActivity.class);
        return PendingIntent.getActivity(context, 0, intent, Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
    }

}
