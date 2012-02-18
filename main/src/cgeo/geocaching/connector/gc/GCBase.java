package cgeo.geocaching.connector.gc;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Settings;
import cgeo.geocaching.StoredList;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
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
    public static final boolean IS_ONLINE = false;


    // TODO Valentine move to connector before merge
    @SuppressWarnings("null")
    public static SearchResult searchByViewport(final Viewport viewport) {

        final SearchResult searchResult = new SearchResult();

        List<ImmutablePair<Integer, Integer>> tiles = getTilesForViewport(viewport);
        // TODO Valentine Use the coords from the viewport for the referer
        final String referer = "http://www.geocaching.com/map/default.aspx?ll=52.4162,9.59412";
        for (ImmutablePair<Integer, Integer> tile : tiles) {
            /*
             * http://www.geocaching.com/map/ --- map-url
             * map.info? --- request for JSON
             * x=8634 --- x-tile
             * y=5381 --- y-tile
             * z=14 --- zoom
             * _=1329484185663 --- token/filter, not required
             */
            final String url = "http://www.geocaching.com/map/map.info?x=" + tile.left + "&y=" + tile.right + "&z=14";
            String page = "";
            if (IS_ONLINE) {
                page = cgBase.requestJSON(url, referer);
            } else {
                page = "{\"grid\":[\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"               04$                                              \",\"               /5'                                              \",\"               .6&                                              \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                       %:(      \",\"                                                       #;,      \",\"                                                       !<)      \",\"                                                                \",\"                                                                \",\"                                                                \",\"                                                                \",\"  8-1                                                           \",\"  9+2                                                           \",\"  7*3                                                           \",\"                                                                \"],\"keys\":[\"\",\"55_55\",\"55_54\",\"17_25\",\"55_53\",\"17_27\",\"17_26\",\"57_53\",\"57_55\",\"3_62\",\"3_61\",\"57_54\",\"3_60\",\"15_27\",\"15_26\",\"15_25\",\"4_60\",\"4_61\",\"4_62\",\"16_25\",\"16_26\",\"16_27\",\"2_62\",\"2_60\",\"2_61\",\"56_53\",\"56_54\",\"56_55\"],\"data\":{\"55_55\":[{\"i\":\"gEaR\",\"n\":\"Spiel & Sport\"}],\"" +
                        "55_54\":[{\"i\":\"gEaR\",\"n\":\"Spiel & Sport\"}],\"17_25\":[{\"i\":\"Rkzt\",\"n\":\"EDSSW:  Rathaus \"}],\"55_53\":[{\"i\":\"gEaR\",\"n\":\"Spiel & Sport\"}],\"17_27\":[{\"i\":\"Rkzt\",\"n\":\"EDSSW:  Rathaus \"}],\"17_26\":[{\"i\":\"Rkzt\",\"n\":\"EDSSW:  Rathaus \"}],\"57_53\":[{\"i\":\"gEaR\",\"n\":\"Spiel & Sport\"}],\"57_55\":[{\"i\":\"gEaR\",\"n\":\"Spiel & Sport\"}],\"3_62\":[{\"i\":\"gOWz\",\"n\":\"Baumarktserie - Wer Wo Was -\"}],\"3_61\":[{\"i\":\"gOWz\",\"n\":\"Baumarktserie - Wer Wo Was -\"}],\"57_54\":[{\"i\":\"gEaR\",\"n\":\"Spiel & Sport\"}],\"3_60\":[{\"i\":\"gOWz\",\"n\":\"Baumarktserie - Wer Wo Was -\"}],\"15_27\":[{\"i\":\"Rkzt\",\"n\":\"EDSSW:  Rathaus \"}],\"15_26\":[{\"i\":\"Rkzt\",\"n\":\"EDSSW:  Rathaus \"}],\"15_25\":[{\"i\":\"Rkzt\",\"n\":\"EDSSW:  Rathaus \"}],\"4_60\":[{\"i\":\"gOWz\",\"n\":\"Baumarktserie - Wer Wo Was -\"}],\"4_61\":[{\"i\":\"gOWz\",\"n\":\"Baumarktserie - Wer Wo Was -\"}],\"4_62\":[{\"i\":\"gOWz\",\"n\":\"Baumarktserie - Wer Wo Was -\"}],\"16_25\":[{\"i\":\"Rkzt\",\"n\":\"EDSSW:  Rathaus \"}],\"16_26\":[{\"i\":\"Rkzt\",\"n\":\"EDSSW:  Rathaus \"}],\"16_27\":[{\"i\":\"Rkzt\",\"n\":\"EDSSW:  Rathaus \"}],\"2_62\":[{\"i\":\"gOWz\",\"n\":\"Baumarktserie - Wer Wo Was -\"}],\"2_60\":[{\"i\":\"gOWz\",\"n\":\"Baumarktserie - Wer Wo Was -\"}],\"2_61\":[{\"i\":\"gOWz\",\"n\":\"Baumarktserie - Wer Wo Was -\"}],\"56_53\":[{\"i\":\"gEaR\",\"n\":\"Spiel & Sport\"}],\"56_54\":[{\"i\":\"gEaR\",\"n\":\"Spiel & Sport\"}],\"56_55\":[{\"i\":\"gEaR\",\"n\":\"Spiel & Sport\"}]}}";
            }
            if (StringUtils.isBlank(page)) {
                Log.e(Settings.tag, "GCBase.searchByViewport: No data from server for tile (" + tile.left + "/" + tile.right + ")");
            }
            final SearchResult search = parseMapJSON(url, page);
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
     * @param page
     *            Retrieved data.
     * @return SearchResult. Never null.
     */
    public static SearchResult parseMapJSON(final String url, final String page) {

        final SearchResult searchResult = new SearchResult();
        searchResult.setUrl(url);

        try {

            if (StringUtils.isEmpty(page)) {
                throw new JSONException("No page given");
            }

            // Example JSON information
            // {"grid":[....],
            //  "keys":["","55_55","55_54","17_25","55_53","17_27","17_26","57_53","57_55","3_62","3_61","57_54","3_60","15_27","15_26","15_25","4_60","4_61","4_62","16_25","16_26","16_27","2_62","2_60","2_61","56_53","56_54","56_55"],
            //  "data":{"55_55":[{"i":"gEaR","n":"Spiel & Sport"}],"55_54":[{"i":"gEaR","n":"Spiel & Sport"}],"17_25":[{"i":"Rkzt","n":"EDSSW:  Rathaus "}],"55_53":[{"i":"gEaR","n":"Spiel & Sport"}],"17_27":[{"i":"Rkzt","n":"EDSSW:  Rathaus "}],"17_26":[{"i":"Rkzt","n":"EDSSW:  Rathaus "}],"57_53":[{"i":"gEaR","n":"Spiel & Sport"}],"57_55":[{"i":"gEaR","n":"Spiel & Sport"}],"3_62":[{"i":"gOWz","n":"Baumarktserie - Wer Wo Was -"}],"3_61":[{"i":"gOWz","n":"Baumarktserie - Wer Wo Was -"}],"57_54":[{"i":"gEaR","n":"Spiel & Sport"}],"3_60":[{"i":"gOWz","n":"Baumarktserie - Wer Wo Was -"}],"15_27":[{"i":"Rkzt","n":"EDSSW:  Rathaus "}],"15_26":[{"i":"Rkzt","n":"EDSSW:  Rathaus "}],"15_25":[{"i":"Rkzt","n":"EDSSW:  Rathaus "}],"4_60":[{"i":"gOWz","n":"Baumarktserie - Wer Wo Was -"}],"4_61":[{"i":"gOWz","n":"Baumarktserie - Wer Wo Was -"}],"4_62":[{"i":"gOWz","n":"Baumarktserie - Wer Wo Was -"}],"16_25":[{"i":"Rkzt","n":"EDSSW:  Rathaus "}],"16_26":[{"i":"Rkzt","n":"EDSSW:  Rathaus "}],"16_27":[{"i":"Rkzt","n":"EDSSW:  Rathaus "}],"2_62":[{"i":"gOWz","n":"Baumarktserie - Wer Wo Was -"}],"2_60":[{"i":"gOWz","n":"Baumarktserie - Wer Wo Was -"}],"2_61":[{"i":"gOWz","n":"Baumarktserie - Wer Wo Was -"}],"56_53":[{"i":"gEaR","n":"Spiel & Sport"}],"56_54":[{"i":"gEaR","n":"Spiel & Sport"}],"56_55":[{"i":"gEaR","n":"Spiel & Sport"}]}
            //  }

            final JSONObject json = new JSONObject(page);

            final JSONArray grid = json.getJSONArray("grid");
            if (grid == null || grid.length() != 64) {
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
            Map<String, ImmutablePair<Integer, Integer>> keyPositions = new HashMap<String, ImmutablePair<Integer, Integer>>(); // JSON key, (x/y) in grid
            for (int y = 0; y < grid.length(); y++) {
                byte[] row = grid.getString(y).getBytes();
                for (int x = 0; x < row.length; x++) {
                    if (row[x] != 32) {
                        byte id = UTFGrid.getUTFGridId(row[x]);
                        keyPositions.put(keys.getString(id), new ImmutablePair<Integer, Integer>(x, y));
                    }
                }
            }

            // iterate over the data and construct all caches in this tile
            Map<String, cgCache> caches = new HashMap<String, cgCache>(); // JSON id, cache
            Map<String, List<ImmutablePair<Integer, Integer>>> positions = new HashMap<String, List<ImmutablePair<Integer, Integer>>>(); // JSON id as key
            for (int i = 0; i < keys.length(); i++) {
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

                        List<ImmutablePair<Integer, Integer>> pos = positions.get(id);
                        if (pos == null) {
                            pos = new ArrayList<ImmutablePair<Integer, Integer>>();
                        }
                        pos.add(keyPositions.get(key));
                        positions.put(id, pos);
                    }
                }
            }

            for (String id : positions.keySet()) {
                List<ImmutablePair<Integer, Integer>> pos = positions.get(id);
                cgCache cache = caches.get(id);
                cache.setCoords(getCoordsForUTFGrid(pos));

                Log.d(Settings.tag, "id= " + id + " geocode= " + cache.getGeocode());
                for (ImmutablePair<Integer, Integer> ImmutablePair : pos) {
                    Log.d(Settings.tag, "(" + ImmutablePair.left + "," + ImmutablePair.right + ")");
                }

                searchResult.addCache(cache);
            }

        } catch (Exception e) {
            Log.e(Settings.tag, "GCBase.parseMapJSON", e);
        }

        return searchResult;
    }

    /**
     * Calculate tiles for the given viewport
     *
     * @param viewport
     * @return
     */
    protected static List<ImmutablePair<Integer, Integer>> getTilesForViewport(Viewport viewport) {
        // TODO Valentine Calculate tile number
        ImmutablePair<Integer, Integer> tile = new ImmutablePair<Integer, Integer>(8633, 5381); // = N 52° 24,516 E 009° 42,592
        List<ImmutablePair<Integer, Integer>> tiles = new ArrayList<ImmutablePair<Integer, Integer>>();
        tiles.add(tile);
        return tiles;

    }

    /** Calculate from a list of positions (x/y) the coords */
    protected static Geopoint getCoordsForUTFGrid(List<ImmutablePair<Integer, Integer>> positions) {
        // TODO Valentine Calculate coords
        return new Geopoint("N 52° 24,516 E 009° 42,592");
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

}
