package cgeo.geocaching.connector.gc;

import cgeo.geocaching.GCConstants;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.utils.BaseUtils;
import cgeo.geocaching.utils.LeastRecentlyUsedCache;

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

    private static final LeastRecentlyUsedCache<String, cgCache> liveMapCache = new LeastRecentlyUsedCache<String, cgCache>(2000); // JSON id, cache

    /**
     * @param viewport
     * @param zoomlevel
     *            initial zoomlevel
     * @param autoAdjust
     *            Auto-adjust zoomlevel
     * @param sessionToken
     * @return
     */
    @SuppressWarnings("null")
    public static SearchResult searchByViewport(final Viewport viewport, final String[] tokens) {

        String referer = GCConstants.URL_LIVE_MAP;

        final SearchResult searchResult = new SearchResult();
        searchResult.setUrl(referer + "?ll=" + viewport.getCenter().getLatitude() + "," + viewport.getCenter().getLongitude());

        List<Tile> tiles = getTilesForViewport(viewport);

        for (Tile tile : tiles) {
            String url =
                    "?x=" + tile.getX() + // x tile
                    "&y=" + tile.getY() + // y tile
                    "&z=" + tile.getZoomlevel(); // zoom level
            /*
             * if (tokens != null) {
             * url += "&k=" + tokens[0]; // user session
             * url += "&st=" + tokens[1]; // session token
             * }
             * url += "&ep=1";
             * /*
             * if (true || Settings.isExcludeMyCaches()) {
             * url += "&hf=1"; // hide found
             * url += "&hh=1"; // hide hidden
             * }
             *
             * if (Settings.getCacheType() == CacheType.TRADITIONAL) {
             * url += "&ect=9,5,3,6,453,13,1304,137,11,4,8,1858"; // 2 = tradi 3 = multi 8 = mystery
             * }
             * if (Settings.getCacheType() == CacheType.MULTI) {
             * url += "&ect=9,5,2,6,453,13,1304,137,11,4,8,1858";
             * }
             * if (Settings.getCacheType() == CacheType.MYSTERY) {
             * url += "&ect=9,5,3,6,453,13,1304,137,11,4,2,1858";
             * }
             */
            if (tile.getZoomlevel() != 14) {
                url += "&_=" + String.valueOf(System.currentTimeMillis());
            }

            // The PNG must be request before ! Else the following request would return with 204 - No Content
            cgBase.requestMapTile(GCConstants.URL_MAP_TILE + url, referer);

            String data = cgBase.requestMapInfo(GCConstants.URL_MAP_INFO + url, referer);
            if (StringUtils.isEmpty(data)) {
                Log.e(Settings.tag, "GCBase.searchByViewport: No data from server for tile (" + tile.getX() + "/" + tile.getY() + ")");
            } else {
                final SearchResult search = parseMapJSON(data, tile);
                if (search == null || CollectionUtils.isEmpty(search.getGeocodes())) {
                    Log.e(Settings.tag, "GCBase.searchByViewport: No cache parsed for viewport " + viewport);
                }
                searchResult.addGeocodes(search.getGeocodes());
            }
        }

        return searchResult;
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

            /*
             * Optimization: the grid can get ignored. The keys are the grid position in the format x_y
             * It's not used at the moment due to optimizations
             * But maybe we need it some day...
             * 
             * // attach all keys with the cache positions in the tile
             * Map<String, UTFGridPosition> keyPositions = new HashMap<String, UTFGridPosition>(); // JSON key, (x/y) in
             * grid
             * for (int y = 0; y < grid.length(); y++) {
             * String rowUTF8 = grid.getString(y);
             * if (rowUTF8.length() != (UTFGrid.GRID_MAXX + 1)) {
             * throw new JSONException("Grid has wrong size");
             * }
             * 
             * for (int x = 0; x < UTFGrid.GRID_MAXX; x++) {
             * char c = rowUTF8.charAt(x);
             * if (c != ' ') {
             * short id = UTFGrid.getUTFGridId(c);
             * keyPositions.put(keys.getString(id), new UTFGridPosition(x, y));
             * }
             * }
             * }
             */

            // iterate over the data and construct all caches in this tile
            Map<String, List<UTFGridPosition>> positions = new HashMap<String, List<UTFGridPosition>>(); // JSON id as key
            for (int i = 1; i < keys.length(); i++) { // index 0 is empty
                String key = keys.getString(i);
                if (StringUtils.isNotBlank(key)) {
                    int[] xy = splitJSONKey(key);
                    JSONArray dataForKey = dataObject.getJSONArray(key);
                    for (int j = 0; j < dataForKey.length(); j++) {
                        JSONObject cacheInfo = dataForKey.getJSONObject(j);
                        String id = cacheInfo.getString("i");
                        cgCache cache = liveMapCache.get(id);
                        if (cache == null) {
                            cache = new cgCache();
                            cache.setDetailed(false);
                            cache.setReliableLatLon(false);
                            cache.setGeocode(newidToGeocode(id));
                            cache.setName(cacheInfo.getString("n"));
                            cache.setType(CacheType.GC_LIVE_MAP);
                            cache.setZoomlevel(tile.getZoomlevel());

                            liveMapCache.put(id, cache);
                        }

                        List<UTFGridPosition> listOfPositions = positions.get(id);
                        if (listOfPositions == null) {
                            listOfPositions = new ArrayList<UTFGridPosition>();
                        }
                        /*
                         * Optimization
                         * UTFGridPosition pos = keyPositions.get(key);
                         */
                        UTFGridPosition pos = new UTFGridPosition(xy[0], xy[1]);
                        listOfPositions.add(pos);
                        positions.put(id, listOfPositions);
                    }
                }
            }

            for (String id : positions.keySet()) {
                List<UTFGridPosition> pos = positions.get(id);
                cgCache cache = liveMapCache.get(id);
                if (cache != null) {
                    // if we have "better" coords from a previous search -> reuse them
                    if (cache.getZoomlevel() < tile.getZoomlevel() ||
                        cache.getCoords() == null) {
                        cache.setCoords(getCoordsForUTFGrid(tile, pos));

                        // Log.d(Settings.tag, "id=" + id + " geocode=" + cache.getGeocode() + " coords=" + cache.getCoords().toString());
                    }
                    searchResult.addCache(cache);
                }
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
    protected static List<Tile> getTilesForViewport(final Viewport viewport) {
        List<Tile> tiles = new ArrayList<Tile>();
        tiles.add(new Tile(viewport.getCenter(), 14)); // precise coords for caches nearby
        tiles.add(new Tile(viewport.getCenter(), 12)); // other caches around
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

    /** Get user session & session token from the Live Map. Needed for following requests */
    public static String[] getTokens() {
        final HttpResponse response = cgBase.request(GCConstants.URL_LIVE_MAP, null, false);
        final String data = cgBase.getResponseData(response);
        String userSession = BaseUtils.getMatch(data, GCConstants.PATTERN_USERSESSION, "");
        String sessionToken = BaseUtils.getMatch(data, GCConstants.PATTERN_SESSIONTOKEN, "");
        return new String[] { userSession, sessionToken };
    }

    private static int[] splitJSONKey(final String key) {
        // two possible positions for the underscore
        int underscore = key.charAt(1) == '_' ? 1 : 2;
        int x = Integer.parseInt(key.substring(0, underscore));
        int y = Integer.parseInt(key.substring(underscore + 1));
        return new int[] { x, y };
    }

}
