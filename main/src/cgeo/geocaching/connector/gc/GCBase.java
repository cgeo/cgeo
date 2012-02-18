package cgeo.geocaching.connector;

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
import java.util.List;

/**
 *
 * @author blafoo
 *
 */
public class GCConnectorImpl {

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
                Log.e(Settings.tag, "GCConnectImpl.searchByViewport: No data from server for tile (" + tile.left + "/" + tile.right + ")");
            }
            final SearchResult search = parseMapJSON(url, page);
            if (search == null || CollectionUtils.isEmpty(search.getGeocodes())) {
                Log.e(Settings.tag, "GCConnectImpl.searchByViewport: No cache parsed for viewport " + viewport);
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

            for (int y = 0; y < grid.length(); y++) {
                byte[] row = grid.getString(y).getBytes();
                for (int x = 0; x < row.length; x++) {
                    byte id = getUTFGridId(row[x]);
                    if (id > 0) {
                        Log.d(Settings.tag, "(" + x + "/" + y + ") =" + String.valueOf(id));
                    }
                }
            }

            //Map<String, Integer> keyIds = new HashMap<String, Integer>(); // key (55_55), index
            //Map<String, cgCache> caches = new HashMap<String, cgCache>(); // name, cache
            //Map<String, List<String>> positions = new HashMap<String, List<String>>(); // name, keys

            for (int i = 0; i < keys.length(); i++) {
                String key = keys.getString(i);
                Log.d(Settings.tag, "Key #" + i + "=" + key);
                if (StringUtils.isNotBlank(key)) {
                    JSONArray dataForKey = dataObject.getJSONArray(key);
                    for (int j = 0; j < dataForKey.length(); j++) {
                        JSONObject cacheInfo = dataForKey.getJSONObject(j);
                        // TODO Valentine How to convert to a geocode ? Or is an extra request needed ?
                        String id = cacheInfo.getString("i");
                        String name = cacheInfo.getString("n");

                        final cgCache cacheToAdd = new cgCache();
                        cacheToAdd.setDetailed(false);
                        cacheToAdd.setReliableLatLon(false);
                        cacheToAdd.setGeocode(id);
                        cacheToAdd.setCoords(getCoordsForUTFGrid(key));
                        cacheToAdd.setName(name);
                        cacheToAdd.setType(CacheType.GC_LIVE_MAP);

                        Log.d(Settings.tag, "key=" + key + " id=" + id + " name=" + name);

                        //caches.put(id, cacheToAdd);
                        searchResult.addCache(cacheToAdd);
                    }
                }
            }

        } catch (Exception e) {
            Log.e(Settings.tag, "cgBase.parseMapJSON", e);
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

    protected static Geopoint getCoordsForUTFGrid(String key) {
        // TODO Valentine Calculate coords
        return new Geopoint("N 52° 24,516 E 009° 42,592");
    }

    /** @see https://github.com/mapbox/mbtiles-spec/blob/master/1.1/utfgrid.md */
    protected static byte getUTFGridId(final byte value) {
        byte result = value;
        if (result >= 93) {
            result--;
        }
        if (result >= 35) {
            result--;
        }
        return (byte) (result - 32);
    }

}
