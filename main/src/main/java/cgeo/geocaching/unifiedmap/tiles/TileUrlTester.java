package cgeo.geocaching.unifiedmap.tiles;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import okhttp3.Response;
import org.mapsforge.core.util.MercatorProjection;

/**
 * Checks that a tile url template actually serves tiles.
 *
 * <p>Syntax validation alone cannot catch a wrong path, which simply yields 404s and a layer
 * that silently draws nothing. This fetches one real tile before the overlay is stored.</p>
 */
public final class TileUrlTester {

    /** zoom to test at: low enough to exist in sparse overlays, high enough to be a real request */
    private static final byte TEST_ZOOM = 12;

    private static final int TILE_SIZE = 256;

    /**
     * Geocaching HQ (GCK25B, N 47 38.938 W 122 20.887).
     *
     * <p>A fallback is needed because the test has to pick a real place to request a tile for, and
     * on a fresh installation no map position has been stored yet - that preference defaults to
     * 0/0, which is open ocean in the Gulf of Guinea, where nearly every sparse tile source legitimately has
     * nothing to serve. The check would then fail for perfectly good urls. Any populated place
     * would do; this one is at least on topic.</p>
     */
    private static final Geopoint DEFAULT_POSITION = new Geopoint(47.6489667, -122.3481167);

    private TileUrlTester() {
        //no instance
    }

    /**
     * Fetches a single tile, from around the last map position so that the test hits an area the
     * user actually looks at rather than a low zoom level many overlays do not serve at all.
     *
     * <p>Blocking - must not be called on the UI thread.</p>
     *
     * @return {@code null} if a tile was served, otherwise a short description of what went wrong
     */
    @Nullable
    public static String test(@NonNull final String urlTemplate) {
        final String url = buildTileUrl(urlTemplate);
        if (url == null) {
            return "invalid url";
        }

        try {
            final Response response = Network.getRequest(url).blockingGet();
            try {
                if (!response.isSuccessful()) {
                    return "HTTP " + response.code();
                }
                // Anything the server actually answers with is accepted: a tile that looks empty
                // may just mean there is no data here, and some servers serve images as
                // application/octet-stream. Only a textual response is rejected, which is how an
                // error page or a captive portal answering 200 gives itself away.
                final String contentType = response.header("Content-Type");
                return contentType != null && contentType.startsWith("text/") ? contentType : null;
            } finally {
                response.close();
            }
        } catch (final RuntimeException e) {
            // blockingGet wraps any IOException from the request into a RuntimeException
            Log.d("Tile url test failed for '" + url + "': " + e);
            return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        }
    }

    /**
     * Where to fetch the test tile from: the last map position, which is the area the user
     * actually looks at, or {@link #DEFAULT_POSITION} when there is none yet.
     */
    @NonNull
    private static Geopoint testPosition() {
        final Geopoint mapCenter = Settings.getMapCenter();
        return mapCenter.getLatitudeE6() == 0 && mapCenter.getLongitudeE6() == 0 ? DEFAULT_POSITION : mapCenter;
    }

    /** resolves the template for a tile at the last known map position */
    @Nullable
    private static String buildTileUrl(@NonNull final String urlTemplate) {
        if (!TileUrlUtils.isValid(urlTemplate)) {
            return null;
        }
        final Geopoint center = testPosition();
        final int tileX = MercatorProjection.pixelXToTileX(
                MercatorProjection.longitudeToPixelX(center.getLongitude(), TEST_ZOOM, TILE_SIZE), TEST_ZOOM, TILE_SIZE);
        final int tileY = MercatorProjection.pixelYToTileY(
                MercatorProjection.latitudeToPixelY(center.getLatitude(), TEST_ZOOM, TILE_SIZE), TEST_ZOOM, TILE_SIZE);
        return TileUrlUtils.forTile(urlTemplate, TEST_ZOOM, tileX, tileY);
    }
}
