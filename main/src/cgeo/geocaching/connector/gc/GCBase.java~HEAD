package cgeo.geocaching.connector.gc;

import cgeo.geocaching.GCConstants;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Settings;
import cgeo.geocaching.StoredList;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.utils.BaseUtils;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GC.com/Groundspeak (GS) specific stuff
 *
 * @author blafoo
 *
 */
public class GCBase {

    protected final static String SEQUENCE_GCID = "0123456789ABCDEFGHJKMNPQRTVWXYZ";
    protected final static String SEQUENCE_NEWID = "tHpXJR8gyfzCrdV5G0Kb3Y92N47lTBPAhWnvLZkaexmSwq6sojDcEQMFO";
    protected final static long GC_BASE57 = 57;
    protected final static long GC_BASE31 = 31;
    protected final static long GC_BASE16 = 16;

    // TODO Valentine remove before merge
    /** go online or use mocked data ? */
    public static final boolean IS_ONLINE = true;


    // TODO Valentine move to connector before merge
    /**
     * @param viewport
     * @param zoomlevel
     *            initial zoomlevel
     * @autoAdjust Auto-adjust zoomlevel
     * @param sessionToken
     * @return
     */
    @SuppressWarnings("null")
    public static SearchResult searchByViewport(final Viewport viewport, final int zoomlevel, final boolean autoAdjust, final String sessionToken) {

        assert zoomlevel >= Tile.ZOOMLEVEL_MIN && zoomlevel <= Tile.ZOOMLEVEL_MAX : "zoomlevel out of bounds.";

        Geopoint centerOfViewport = new Geopoint((viewport.getLatitudeMin() + viewport.getLatitudeMax()) / 2, (viewport.getLongitudeMin() + viewport.getLongitudeMax()) / 2);
        final String referer = GCConstants.URL_LIVE_MAP +
                "?ll=" + centerOfViewport.getLatitude() +
                "," + centerOfViewport.getLongitude();

        final SearchResult searchResult = new SearchResult();
        searchResult.setUrl(referer);

        List<Tile> tiles = getTilesForViewport(viewport, zoomlevel, autoAdjust);

        for (Tile tile : tiles) {
            /*
             * http://www.geocaching.com/map/ --- map-url
             * map.info? --- request for JSON
             * x=8634 --- x-tile
             * y=5381 --- y-tile
             * z=14 --- zoom
             * _=1329484185663 --- token/filter, not required
             */
            String url = GCConstants.URL_MAP_INFO +
                    "?x=" + tile.getX() +
                    "&y=" + tile.getY() +
                    "&z=" + tile.getZoomlevel();
            if (StringUtils.isNotEmpty(sessionToken)) {
                url += "&st=" + sessionToken;
            }

            String data = "";
            if (IS_ONLINE) {
                data = cgBase.requestJSON(url, referer);
            } else {
                data = "{\"grid\":[\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"               04$                                              \",\"               /5'                                              \",\"               .6&                                              \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                       %:(      \",\"                                                       #;,      \",\"                                                       !<)      \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"  8-1                                                           \",\"  9+2                                                           \",\"  7*3                                                           \",\"                                                                \"],\"keys\":[\"\",\"55_55\",\"55_54\",\"17_25\",\"55_53\",\"17_27\",\"17_26\",\"57_53\",\"57_55\",\"3_62\",\"3_61\",\"57_54\",\"3_60\",\"15_27\",\"15_26\",\"15_25\",\"4_60\",\"4_61\",\"4_62\",\"16_25\",\"16_26\",\"16_27\",\"2_62\",\"2_60\",\"2_61\",\"56_53\",\"56_54\",\"56_55\"],\"data\":{\"55_55\":[{\"i\":\"gEaR\",\"n\":\"Spiel & Sport\"}],\"" +
                        "55_54\":[{\"i\":\"gEaR\",\"n\":\"Spiel & Sport\"}],\"17_25\":[{\"i\":\"Rkzt\",\"n\":\"EDSSW:  Rathaus \"}],\"55_53\":[{\"i\":\"gEaR\",\"n\":\"Spiel & Sport\"}],\"17_27\":[{\"i\":\"Rkzt\",\"n\":\"EDSSW:  Rathaus \"}],\"17_26\":[{\"i\":\"Rkzt\",\"n\":\"EDSSW:  Rathaus \"}],\"57_53\":[{\"i\":\"gEaR\",\"n\":\"Spiel & Sport\"}],\"57_55\":[{\"i\":\"gEaR\",\"n\":\"Spiel & Sport\"}],\"3_62\":[{\"i\":\"gOWz\",\"n\":\"Baumarktserie - Wer Wo Was -\"}],\"3_61\":[{\"i\":\"gOWz\",\"n\":\"Baumarktserie - Wer Wo Was -\"}],\"57_54\":[{\"i\":\"gEaR\",\"n\":\"Spiel & Sport\"}],\"3_60\":[{\"i\":\"gOWz\",\"n\":\"Baumarktserie - Wer Wo Was -\"}],\"15_27\":[{\"i\":\"Rkzt\",\"n\":\"EDSSW:  Rathaus \"}],\"15_26\":[{\"i\":\"Rkzt\",\"n\":\"EDSSW:  Rathaus \"}],\"15_25\":[{\"i\":\"Rkzt\",\"n\":\"EDSSW:  Rathaus \"}],\"4_60\":[{\"i\":\"gOWz\",\"n\":\"Baumarktserie - Wer Wo Was -\"}],\"4_61\":[{\"i\":\"gOWz\",\"n\":\"Baumarktserie - Wer Wo Was -\"}],\"4_62\":[{\"i\":\"gOWz\",\"n\":\"Baumarktserie - Wer Wo Was -\"}],\"16_25\":[{\"i\":\"Rkzt\",\"n\":\"EDSSW:  Rathaus \"}],\"16_26\":[{\"i\":\"Rkzt\",\"n\":\"EDSSW:  Rathaus \"}],\"16_27\":[{\"i\":\"Rkzt\",\"n\":\"EDSSW:  Rathaus \"}],\"2_62\":[{\"i\":\"gOWz\",\"n\":\"Baumarktserie - Wer Wo Was -\"}],\"2_60\":[{\"i\":\"gOWz\",\"n\":\"Baumarktserie - Wer Wo Was -\"}],\"2_61\":[{\"i\":\"gOWz\",\"n\":\"Baumarktserie - Wer Wo Was -\"}],\"56_53\":[{\"i\":\"gEaR\",\"n\":\"Spiel & Sport\"}],\"56_54\":[{\"i\":\"gEaR\",\"n\":\"Spiel & Sport\"}],\"56_55\":[{\"i\":\"gEaR\",\"n\":\"Spiel & Sport\"}]}}";
            }
            if (StringUtils.isEmpty(data)) {
                Log.e(Settings.tag, "GCBase.searchByViewport: No data from server for tile (" + tile.getX() + "/" + tile.getY() + ")");
            }
            final SearchResult search = parseMapJSON(data, tile);
            if (search == null || CollectionUtils.isEmpty(search.getGeocodes())) {
                Log.e(Settings.tag, "GCBase.searchByViewport: No cache parsed for viewport " + viewport);
            }
            searchResult.addGeocodes(search.getGeocodes());
        }

        final SearchResult search = searchResult.filterSearchResults(Settings.isExcludeDisabledCaches(), Settings.isExcludeMyCaches(), Settings.getCacheType(), StoredList.TEMPORARY_LIST_ID);
        return search;
    }

    /**
     * @param url
     *            URL used to retrieve data.
     * @param data
     *            Retrieved data.
     * @return SearchResult. Never null.
     */
    public static SearchResult parseMapJSON(final String data, Tile tile) {

        final SearchResult searchResult = new SearchResult();

        try {

            if (StringUtils.isEmpty(data)) {
                throw new JSONException("No page given");
            }

            // Example JSON information
            // {"grid":[....],
            //  "keys":["","55_55","55_54","17_25","55_53","17_27","17_26","57_53","57_55","3_62","3_61","57_54","3_60","15_27","15_26","15_25","4_60","4_61","4_62","16_25","16_26","16_27","2_62","2_60","2_61","56_53","56_54","56_55"],
            //  "data":{"55_55":[{"i":"gEaR","n":"Spiel & Sport"}],"55_54":[{"i":"gEaR","n":"Spiel & Sport"}],"17_25":[{"i":"Rkzt","n":"EDSSW:  Rathaus "}],"55_53":[{"i":"gEaR","n":"Spiel & Sport"}],"17_27":[{"i":"Rkzt","n":"EDSSW:  Rathaus "}],"17_26":[{"i":"Rkzt","n":"EDSSW:  Rathaus "}],"57_53":[{"i":"gEaR","n":"Spiel & Sport"}],"57_55":[{"i":"gEaR","n":"Spiel & Sport"}],"3_62":[{"i":"gOWz","n":"Baumarktserie - Wer Wo Was -"}],"3_61":[{"i":"gOWz","n":"Baumarktserie - Wer Wo Was -"}],"57_54":[{"i":"gEaR","n":"Spiel & Sport"}],"3_60":[{"i":"gOWz","n":"Baumarktserie - Wer Wo Was -"}],"15_27":[{"i":"Rkzt","n":"EDSSW:  Rathaus "}],"15_26":[{"i":"Rkzt","n":"EDSSW:  Rathaus "}],"15_25":[{"i":"Rkzt","n":"EDSSW:  Rathaus "}],"4_60":[{"i":"gOWz","n":"Baumarktserie - Wer Wo Was -"}],"4_61":[{"i":"gOWz","n":"Baumarktserie - Wer Wo Was -"}],"4_62":[{"i":"gOWz","n":"Baumarktserie - Wer Wo Was -"}],"16_25":[{"i":"Rkzt","n":"EDSSW:  Rathaus "}],"16_26":[{"i":"Rkzt","n":"EDSSW:  Rathaus "}],"16_27":[{"i":"Rkzt","n":"EDSSW:  Rathaus "}],"2_62":[{"i":"gOWz","n":"Baumarktserie - Wer Wo Was -"}],"2_60":[{"i":"gOWz","n":"Baumarktserie - Wer Wo Was -"}],"2_61":[{"i":"gOWz","n":"Baumarktserie - Wer Wo Was -"}],"56_53":[{"i":"gEaR","n":"Spiel & Sport"}],"56_54":[{"i":"gEaR","n":"Spiel & Sport"}],"56_55":[{"i":"gEaR","n":"Spiel & Sport"}]}
            //  }

            final JSONObject json = new JSONObject(data);

            final JSONArray grid = json.getJSONArray("grid");
            if (grid == null || grid.length() != (UTFGrid.GRID_MAXY + 1)) {
                throw new JSONException("No grid inside JSON");
            }
            final JSONArray keys = json.getJSONArray("keys");
            if (keys == null) {
                throw new JSONException("No keys inside JSON");
            }
            final JSONObject dataObject = json.getJSONObject("data");
            if (dataObject == null) {
                throw new JSONException("No data inside JSON");
            }

            // attach all keys with the cache positions in the tile
            Map<String, UTFGridPosition> keyPositions = new HashMap<String, UTFGridPosition>(); // JSON key, (x/y) in grid
            for (int y = 0; y < grid.length(); y++) {
                String rowUTF8 = grid.getString(y);
                if (rowUTF8.length() != (UTFGrid.GRID_MAXX + 1)) {
                    throw new JSONException("Grid has wrong size");
                }

                for (int x = 0; x < UTFGrid.GRID_MAXX; x++) {
                    char c = rowUTF8.charAt(x);
                    if (c != ' ') {
                        short id = UTFGrid.getUTFGridId(c);
                        keyPositions.put(keys.getString(id), new UTFGridPosition(x, y));
                    }
                }
            }

            // Optimization:
            // the grid can get ignored. The keys are the grid position in the format x_y

            // iterate over the data and construct all caches in this tile
            Map<String, cgCache> caches = new HashMap<String, cgCache>(); // JSON id, cache
            Map<String, List<UTFGridPosition>> positions = new HashMap<String, List<UTFGridPosition>>(); // JSON id as key
            for (int i = 1; i < keys.length(); i++) { // index 0 is empty
                String key = keys.getString(i);
                if (StringUtils.isNotBlank(key)) {
                    JSONArray dataForKey = dataObject.getJSONArray(key);
                    for (int j = 0; j < dataForKey.length(); j++) {
                        JSONObject cacheInfo = dataForKey.getJSONObject(j);
                        String id = cacheInfo.getString("i");
                        cgCache cache = caches.get(id);
                        if (cache == null) {
                            cache = new cgCache();
                            cache.setDetailed(false);
                            cache.setReliableLatLon(false);
                            cache.setGeocode(newidToGeocode(id));
                            cache.setName(cacheInfo.getString("n"));
                            cache.setType(CacheType.GC_LIVE_MAP);

                            caches.put(id, cache);
                        }

                        List<UTFGridPosition> listOfPositions = positions.get(id);
                        if (listOfPositions == null) {
                            listOfPositions = new ArrayList<UTFGridPosition>();
                        }
                        UTFGridPosition pos = keyPositions.get(key);
                        if (pos == null) {
                            Log.e(Settings.tag, "key " + key + " not found in keyPositions");
                        } else {
                            listOfPositions.add(pos);
                        }
                        positions.put(id, listOfPositions);
                    }
                }
            }

            for (String id : positions.keySet()) {
                List<UTFGridPosition> pos = positions.get(id);
                cgCache cache = caches.get(id);
                cache.setCoords(getCoordsForUTFGrid(tile, pos));

                Log.d(Settings.tag, "id=" + id + " geocode=" + cache.getGeocode() + " coords=" + cache.getCoords().toString());

                searchResult.addCache(cache);
            }
            Log.d(Settings.tag, "Retrieved " + searchResult.getCount() + " caches for tile " + tile.toString());

        } catch (Exception e) {
            Log.e(Settings.tag, "GCBase.parseMapJSON", e);
        }

        return searchResult;
    }

    /**
     * Calculate needed tiles for the given viewport
     *
     * @param viewport
     * @return
     */
    protected static List<Tile> getTilesForViewport(final Viewport viewport, final int zoomlevel, final boolean autoAdjust) {
        Tile tileBottomLeft = new Tile(viewport.bottomLeft, zoomlevel);
        Tile tileTopRight = new Tile(viewport.topRight, zoomlevel);

        int minX = Math.min(tileBottomLeft.getX(), tileTopRight.getX());
        int maxX = Math.max(tileBottomLeft.getX(), tileTopRight.getX());
        int minY = Math.min(tileBottomLeft.getY(), tileTopRight.getY());
        int maxY = Math.max(tileBottomLeft.getY(), tileTopRight.getY());

        // The recursion is a compromise between number of requests and precision.
        // The smaller the zoomlevel the smaller the number of requests the more inaccurate the coords are
        // The bigger the zoomlevel the bigger the number of requests the more accurate the coords are
        // For really usable coords a zoomlevel >= 13 is required
        if (autoAdjust && zoomlevel >= Tile.ZOOMLEVEL_MIN && ((maxX - minX + 1) * (maxY - minY + 1) > 4)) {
            return getTilesForViewport(viewport, zoomlevel - 1, autoAdjust);
        }

        List<Tile> tiles = new ArrayList<Tile>();

        if (tileBottomLeft.getX() == tileTopRight.getX() &&
                tileBottomLeft.getY() == tileTopRight.getY()) {
            tiles.add(tileBottomLeft);
            return tiles;
        }

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                tiles.add(new Tile(x, y, zoomlevel));
            }
        }
        Log.d(Settings.tag, "# tiles=" + tiles.size() + " " + minX + "/" + minY + " " + maxX + "/" + maxY);
        return tiles;
    }

    /** Calculate from a list of positions (x/y) the coords */
    protected static Geopoint getCoordsForUTFGrid(Tile tile, List<UTFGridPosition> positions) {
        int minX = UTFGrid.GRID_MAXX;
        int maxX = 0;
        int minY = UTFGrid.GRID_MAXY;
        int maxY = 0;
        for (UTFGridPosition pos : positions) {
            minX = Math.min(minX, pos.x);
            maxX = Math.max(maxX, pos.x);
            minY = Math.min(minY, pos.y);
            maxY = Math.max(maxY, pos.y);
        }
        return tile.getCoord(new UTFGridPosition((minX + maxX) / 2, (minY + maxY) / 2));
    }

    /**
     * Convert GCCode (geocode) to (old) GCIds
     *
     * Based on http://www.geoclub.de/viewtopic.php?f=111&t=54859&start=40
     * see http://support.groundspeak.com/index.php?pg=kb.printer.friendly&id=1#p221
     */
    public static long gccodeToGCId(final String gccode) {
        long gcid = 0;
        long base = GC_BASE31;
        String geocodeWO = gccode.substring(2).toUpperCase();

        if ((geocodeWO.length() < 4) || (geocodeWO.length() == 4 && SEQUENCE_GCID.indexOf(geocodeWO.charAt(0)) < 16)) {
            base = GC_BASE16;
        }

        for (int p = 0; p < geocodeWO.length(); p++) {
            gcid = base * gcid + SEQUENCE_GCID.indexOf(geocodeWO.charAt(p));
        }

        if (base == GC_BASE31) {
            gcid += Math.pow(16, 4) - 16 * Math.pow(31, 3);
        }
        return gcid;
    }

    private static String modulo(final long value, final long base, final String sequence) {
        String result = "";
        long rest = 0;
        long divResult = value;
        do
        {
            rest = divResult % base;
            divResult = (int) Math.floor(divResult / base);
            result = sequence.charAt((int) rest) + result;
        } while (divResult != 0);
        return result;
    }

    /**
     * Convert (old) GCIds to GCCode (geocode)
     *
     * Based on http://www.geoclub.de/viewtopic.php?f=111&t=54859&start=40
     */
    public static String gcidToGCCode(final long gcid) {
        String gccode = modulo(gcid + 411120, GC_BASE31, SEQUENCE_GCID);
        if ((gccode.length() < 4) || (gccode.length() == 4 && SEQUENCE_GCID.indexOf(gccode.charAt(0)) < 16)) {
            gccode = modulo(gcid, GC_BASE16, SEQUENCE_GCID);
        }
        return "GC" + gccode;
    }

    /**
     * Convert ids from the live map to (old) GCIds
     *
     * Based on http://www.geoclub.de/viewtopic.php?f=111&t=54859&start=40
     */
    public static long newidToGCId(final String newid) {
        long gcid = 0;
        for (int p = 0; p < newid.length(); p++) {
            gcid = GC_BASE57 * gcid + SEQUENCE_NEWID.indexOf(newid.charAt(p));
        }
        return gcid;
    }

    /**
     * Convert (old) GCIds to ids used in the live map
     *
     * Based on http://www.geoclub.de/viewtopic.php?f=111&t=54859&start=40
     */
    public static String gcidToNewId(final long gcid) {
        return modulo(gcid, GC_BASE57, SEQUENCE_NEWID);
    }

    /**
     * Convert ids from the live map into GCCode (geocode)
     */
    public static String newidToGeocode(final String newid) {
        long gcid = GCBase.newidToGCId(newid);
        return GCBase.gcidToGCCode(gcid);
    }

    // TODO Valentine
    /** Request further details in the live mapa for a given id */
    public void requestDetailsFromMap(@SuppressWarnings("unused") String id) {
        /**
         * URL http://www.geocaching.com/map/map.details?i=gEaR
         * Response: {"status":"success","data":[{"name":"Spiel & Sport","gc":"GC211WG","g":
         * "872d7eda-7cb9-40d5-890d-5b344bce7302"
         * ,"disabled":false,"subrOnly":false,"li":false,"fp":"0","difficulty":{"text"
         * :3.0,"value":"3"},"terrain":{"text"
         * :2.0,"value":"2"},"hidden":"11/15/2009","container":{"text":"Regular","value"
         * :"regular.gif"},"type":{"text":"Multi-cache"
         * ,"value":3},"owner":{"text":"kai2707","value":"5c4b0915-5cec-4fa1-8afd-4b3ca67e004e"}}]}
         */
    }

    /** Get session token from the Live Map. Needed for following requests */
    public static String getSessionToken() {
        final HttpResponse response = cgBase.request(GCConstants.URL_LIVE_MAP, null, false);
        final String data = cgBase.getResponseData(response);
        return BaseUtils.getMatch(data, GCConstants.PATTERN_SESSIONTOKEN, "");
    }

}
