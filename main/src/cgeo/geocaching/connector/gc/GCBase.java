package cgeo.geocaching.connector.gc;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LiveMapStrategy.Strategy;
import cgeo.geocaching.enumerations.LiveMapStrategy.StrategyFlag;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.IConversion;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.network.Login;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.ui.Formatter;
import cgeo.geocaching.utils.BaseUtils;
import cgeo.geocaching.utils.LeastRecentlyUsedMap;
import cgeo.geocaching.utils.Log;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

    private final static LeastRecentlyUsedMap<Integer, Tile> tileCache = new LeastRecentlyUsedMap.LruCache<Integer, Tile>(64);
    private static Viewport lastSearchViewport = null;

    /**
     * Searches the view port on the live map with Strategy.AUTO
     *
     * @param viewport
     *            Area to search
     * @param tokens
     *            Live map tokens
     * @return
     */
    public static SearchResult searchByViewport(final Viewport viewport, final String[] tokens) {
        Strategy strategy = Settings.getLiveMapStrategy();
        if (strategy == Strategy.AUTO) {
            float speedNow = cgeoapplication.getInstance().getSpeedFromGeo();
            strategy = speedNow >= 8 ? Strategy.FAST : Strategy.DETAILED; // 8 m/s = 30 km/h
        }
        // return searchByViewport(viewport, tokens, strategy);

        // testing purpose
        {
            SearchResult result = searchByViewport(viewport, tokens, strategy);
            String text = Formatter.SEPARATOR + strategy.getL10n() + Formatter.SEPARATOR;
            int speed = (int) cgeoapplication.getInstance().getSpeedFromGeo();
            if (Settings.isUseMetricUnits()) {
                text += speed + " km/h";
            } else {
                text += speed / IConversion.MILES_TO_KILOMETER + " mph";
            }
            result.setUrl(result.getUrl() + text);
            return result;
        }
    }

    public static void removeFromTileCache(Geopoint coords) {
        if (coords != null) {
            Collection<Tile> tiles = new ArrayList<Tile>(tileCache.values());
            for (Tile tile : tiles) {
                if (tile.containsPoint(coords)) {
                    tileCache.remove(tile.hashCode());
                }
            }
        }
    }

    /**
     * Searches the view port on the live map for caches.
     * The strategy dictates if only live map information is used or if an additional
     * searchByCoordinates query is issued.
     *
     * @param viewport
     *            Area to search
     * @param tokens
     *            Live map tokens
     * @param strategy
     *            Strategy for data retrieval and parsing, @see Strategy
     * @return
     */
    private static SearchResult searchByViewport(final Viewport viewport, final String[] tokens, Strategy strategy) {
        Log.d(Settings.tag, "GCBase.searchByViewport" + viewport.toString());

        String referer = GCConstants.URL_LIVE_MAP;

        final SearchResult searchResult = new SearchResult();
        searchResult.setUrl(referer + "?ll=" + viewport.getCenter().getLatitude() + "," + viewport.getCenter().getLongitude());

        if (strategy.flags.contains(StrategyFlag.LOAD_TILES)) {
            final Set<Tile> tiles = getTilesForViewport(viewport);

            for (Tile tile : tiles) {

                if (!tileCache.containsKey(tile.hashCode())) {

                    StringBuilder url = new StringBuilder();
                    url.append("?x=").append(tile.getX()) // x tile
                    .append("&y=").append(tile.getY()) // y tile
                    .append("&z=").append(tile.getZoomlevel()); // zoom level
                    if (tokens != null) {
                        url.append("&k=").append(tokens[0]); // user session
                        url.append("&st=").append(tokens[1]); // session token
                    }
                    url.append("&ep=1");
                    if (Settings.isExcludeMyCaches()) {
                        url.append("&hf=1").append("&hh=1"); // hide found, hide hidden
                    }
                    if (Settings.getCacheType() == CacheType.TRADITIONAL) {
                        url.append("&ect=9,5,3,6,453,13,1304,137,11,4,8,1858"); // 2 = tradi 3 = multi 8 = mystery
                    }
                    if (Settings.getCacheType() == CacheType.MULTI) {
                        url.append("&ect=9,5,2,6,453,13,1304,137,11,4,8,1858");
                    }
                    if (Settings.getCacheType() == CacheType.MYSTERY) {
                        url.append("&ect=9,5,3,6,453,13,1304,137,11,4,2,1858");
                    }
                    if (tile.getZoomlevel() != 14) {
                        url.append("&_=").append(String.valueOf(System.currentTimeMillis()));
                    }
                    // other types t.b.d
                    final String urlString = url.toString();

                    // The PNG must be requested first, otherwise the following request would always return with 204 - No Content
                    Bitmap bitmap = Tile.requestMapTile(GCConstants.URL_MAP_TILE + urlString, referer);

                    // Check bitmap size
                    if (bitmap.getWidth() != Tile.TILE_SIZE ||
                            bitmap.getHeight() != Tile.TILE_SIZE) {
                        bitmap.recycle();
                        bitmap = null;
                    }

                    String data = Tile.requestMapInfo(GCConstants.URL_MAP_INFO + urlString, referer);
                    if (StringUtils.isEmpty(data)) {
                        Log.e(Settings.tag, "GCBase.searchByViewport: No data from server for tile (" + tile.getX() + "/" + tile.getY() + ")");
                    } else {
                        final SearchResult search = parseMapJSON(data, tile, bitmap, strategy);
                        if (search == null || CollectionUtils.isEmpty(search.getGeocodes())) {
                            Log.e(Settings.tag, "GCBase.searchByViewport: No cache parsed for viewport " + viewport);
                        }
                        else {
                            searchResult.addGeocodes(search.getGeocodes());
                        }
                        tileCache.put(tile.hashCode(), tile);
                    }

                    // release native bitmap memory
                    if (bitmap != null) {
                        bitmap.recycle();
                    }

                }
            }
        }

        if (strategy.flags.contains(StrategyFlag.SEARCH_NEARBY)) {
            Geopoint center = viewport.getCenter();
            if ((lastSearchViewport == null) || !lastSearchViewport.isInViewport(center)) {
                SearchResult search = cgBase.searchByCoords(null, center, Settings.getCacheType(), false);
                if (search != null && !search.isEmpty()) {

                    List<Number> bounds = cgeoapplication.getInstance().getBounds(search.getGeocodes());
                    lastSearchViewport = new Viewport(bounds.get(1).doubleValue(), bounds.get(2).doubleValue(), bounds.get(4).doubleValue(), bounds.get(3).doubleValue());
                    searchResult.addGeocodes(search.getGeocodes());
                }
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
    public static SearchResult parseMapJSON(final String data, Tile tile, Bitmap bitmap, final Strategy strategy) {
        final SearchResult searchResult = new SearchResult();

        try {

            final LeastRecentlyUsedMap<String, String> nameCache = new LeastRecentlyUsedMap.LruCache<String, String>(2000); // JSON id, cache name

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
                    UTFGridPosition pos = UTFGridPosition.fromString(key);
                    JSONArray dataForKey = dataObject.getJSONArray(key);
                    for (int j = 0; j < dataForKey.length(); j++) {
                        JSONObject cacheInfo = dataForKey.getJSONObject(j);
                        String id = cacheInfo.getString("i");
                        nameCache.put(id, cacheInfo.getString("n"));

                        List<UTFGridPosition> listOfPositions = positions.get(id);
                        if (listOfPositions == null) {
                            listOfPositions = new ArrayList<UTFGridPosition>();
                            positions.put(id, listOfPositions);
                        }

                        listOfPositions.add(pos);
                    }
                }
            }

            for (Entry<String, List<UTFGridPosition>> entry : positions.entrySet()) {
                String id = entry.getKey();
                List<UTFGridPosition> pos = entry.getValue();
                UTFGridPosition xy = UTFGrid.getPositionInGrid(pos);
                cgCache cache = new cgCache();
                cache.setDetailed(false);
                cache.setReliableLatLon(false);
                cache.setGeocode(id);
                cache.setName(nameCache.get(id));
                cache.setZoomlevel(tile.getZoomlevel());
                cache.setCoords(tile.getCoord(xy));
                if (strategy.flags.contains(StrategyFlag.PARSE_TILES) && positions.size() < 64 && bitmap != null) {
                    // don't parse if there are too many caches. The decoding would return too much wrong results
                    IconDecoder.parseMapPNG(cache, bitmap, xy, tile.getZoomlevel());
                } else {
                    cache.setType(CacheType.UNKNOWN);
                }
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
    protected static Set<Tile> getTilesForViewport(final Viewport viewport) {
        Set<Tile> tiles = new HashSet<Tile>();
        int zoom = Math.min(Tile.calcZoomLon(viewport.bottomLeft, viewport.topRight),
                Tile.calcZoomLat(viewport.bottomLeft, viewport.topRight));
        tiles.add(new Tile(viewport.bottomLeft, zoom));
        tiles.add(new Tile(new Geopoint(viewport.getLatitudeMin(), viewport.getLongitudeMax()), zoom));
        tiles.add(new Tile(new Geopoint(viewport.getLatitudeMax(), viewport.getLongitudeMin()), zoom));
        tiles.add(new Tile(viewport.topRight, zoom));
        return tiles;
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

    /** Get user session & session token from the Live Map. Needed for following requests */
    public static String[] getTokens() {
        final HttpResponse response = Network.request(GCConstants.URL_LIVE_MAP, null, false);
        final String data = Network.getResponseData(response);
        String userSession = BaseUtils.getMatch(data, GCConstants.PATTERN_USERSESSION, "");
        String sessionToken = BaseUtils.getMatch(data, GCConstants.PATTERN_SESSIONTOKEN, "");
        return new String[] { userSession, sessionToken };
    }

    public static SearchResult searchByGeocodes(final Set<String> geocodes) {

        SearchResult result = new SearchResult();

        final String geocodeList = StringUtils.join(geocodes.toArray(), "|");

        String referer = GCConstants.URL_LIVE_MAP_DETAILS;

        StringBuilder url = new StringBuilder();
        url.append("?i=").append(geocodeList).append("&_=").append(String.valueOf(System.currentTimeMillis()));
        final String urlString = url.toString();

        try {
            String data = Tile.requestMapInfo(referer + urlString, referer);

            // Example JSON information
            // {"status":"success",
            //    "data":[{"name":"Mission: Impossible","gc":"GC1234","g":"34c2e609-5246-4f91-9029-d6c02b0f2a82","available":true,"archived":false,"subrOnly":false,"li":false,"fp":"5","difficulty":{"text":3.5,"value":"3_5"},"terrain":{"text":1.0,"value":"1"},"hidden":"7/23/2001","container":{"text":"Regular","value":"regular.gif"},"type":{"text":"Unknown Cache","value":8},"owner":{"text":"Ca$h_Cacher","value":"2db18e69-6877-402a-848d-6362621424f6"}},
            //            {"name":"HP: Hannover - Sahlkamp","gc":"GC2Q97X","g":"a09149ca-00e0-4aa2-b332-db2b4dfb18d2","available":true,"archived":false,"subrOnly":false,"li":false,"fp":"0","difficulty":{"text":1.0,"value":"1"},"terrain":{"text":1.5,"value":"1_5"},"hidden":"5/29/2011","container":{"text":"Small","value":"small.gif"},"type":{"text":"Traditional Cache","value":2},"owner":{"text":"GeoM@n","value":"1deaa69e-6bcc-421d-95a1-7d32b468cb82"}}]
            // }

            final JSONObject json = new JSONObject(data);
            final String status = json.getString("status");
            if (StringUtils.isBlank(status)) {

                throw new JSONException("No status inside JSON");
            }
            if ("success".compareTo(status) != 0) {
                throw new JSONException("Wrong status inside JSON");
            }
            final JSONArray dataArray = json.getJSONArray("data");
            if (dataArray == null) {
                throw new JSONException("No data inside JSON");
            }

            for (int j = 0; j < dataArray.length(); j++) {

                cgCache cache = new cgCache();

                JSONObject dataObject = dataArray.getJSONObject(j);
                cache.setName(dataObject.getString("name"));
                cache.setGeocode(dataObject.getString("gc"));
                cache.setGuid(dataObject.getString("g")); // 34c2e609-5246-4f91-9029-d6c02b0f2a82"
                cache.setDisabled(!dataObject.getBoolean("available"));
                cache.setArchived(dataObject.getBoolean("archived"));
                cache.setPremiumMembersOnly(dataObject.getBoolean("subrOnly"));
                // "li" seems to be "false" always
                cache.setFavoritePoints(Integer.parseInt(dataObject.getString("fp")));
                JSONObject difficultyObj = dataObject.getJSONObject("difficulty");
                cache.setDifficulty(Float.parseFloat(difficultyObj.getString("text"))); // 3.5
                JSONObject terrainObj = dataObject.getJSONObject("terrain");
                cache.setTerrain(Float.parseFloat(terrainObj.getString("text"))); // 1.5
                cache.setHidden(Login.parseGcCustomDate(dataObject.getString("hidden"), "MM/dd/yyyy")); // 7/23/2001
                JSONObject containerObj = dataObject.getJSONObject("container");
                cache.setSize(CacheSize.getById(containerObj.getString("text"))); // Regular
                JSONObject typeObj = dataObject.getJSONObject("type");
                cache.setType(CacheType.getByPattern(typeObj.getString("text"))); // Traditional Cache
                JSONObject ownerObj = dataObject.getJSONObject("owner");
                cache.setOwner(ownerObj.getString("text"));

                result.addCache(cache);

            }
        } catch (JSONException e) {
            result.setError(StatusCode.UNKNOWN_ERROR);
        } catch (ParseException e) {
            result.setError(StatusCode.UNKNOWN_ERROR);
        } catch (NumberFormatException e) {
            result.setError(StatusCode.UNKNOWN_ERROR);
        }
        return result;
    }

}
