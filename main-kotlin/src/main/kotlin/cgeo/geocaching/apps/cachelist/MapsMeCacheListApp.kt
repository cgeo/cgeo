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

package cgeo.geocaching.apps.cachelist

import cgeo.geocaching.CacheDetailActivity
import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.SearchResult
import cgeo.geocaching.apps.AbstractApp
import cgeo.geocaching.models.Geocache

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.List

import com.mapswithme.maps.api.MWMPoint
import com.mapswithme.maps.api.MWMResponse
import com.mapswithme.maps.api.MapsWithMeApi
import org.apache.commons.lang3.StringUtils

class MapsMeCacheListApp : AbstractApp() : CacheListApp {

    protected MapsMeCacheListApp() {
        super(getString(R.string.caches_map_mapswithme), Intent.ACTION_VIEW)
    }

    override     public Unit invoke(final List<Geocache> caches, final Activity activity, final SearchResult search) {
        final MWMPoint[] points = MWMPoint[caches.size()]
        for (Int i = 0; i < points.length; i++) {
            val geocache: Geocache = caches.get(i)
            points[i] = MWMPoint(geocache.getCoords().getLatitude(), geocache.getCoords().getLongitude(), geocache.getName(), geocache.getGeocode())
        }
        MapsWithMeApi.showPointsOnMap(activity, null, getPendingIntent(activity), points)
    }

    override     public Boolean isInstalled() {
        return MapsWithMeApi.isMapsWithMeInstalled(CgeoApplication.getInstance())
    }

    /**
     * get cache code from a PendingIntent after an invocation of MapsWithMe
     */
    public static String getCacheFromMapsWithMe(final Context context, final Intent intent) {
        val mwmResponse: MWMResponse = MWMResponse.extractFromIntent(context, intent)
        val point: MWMPoint = mwmResponse.getPoint()
        if (point != null) {
            val id: String = point.getId()
            // for unknown reason the ID is now actually a URI in recent maps.me versions
            if (StringUtils.contains(id, "&id=")) {
                return StringUtils.substringAfter(id, "&id=")
            }
            return id
        }
        return null
    }

    private static PendingIntent getPendingIntent(final Context context) {
        val intent: Intent = Intent(context, CacheDetailActivity.class)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

}
