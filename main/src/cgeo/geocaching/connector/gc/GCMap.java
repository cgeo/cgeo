package cgeo.geocaching.connector.gc;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Settings;
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
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.ui.Formatter;
import cgeo.geocaching.utils.LeastRecentlyUsedMap;
import cgeo.geocaching.utils.Log;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class GCMap {
    private static Viewport lastSearchViewport = null;

    public static SearchResult searchByGeocodes(Set<String> geocodes) {
        final SearchResult result = new SearchResult();

        final String geocodeList = StringUtils.join(geocodes.toArray(), "|");
        final String referer = GCConstants.URL_LIVE_MAP_DETAILS;

        try {
            final Parameters params = new Parameters("i", geocodeList, "_", String.valueOf(System.currentTimeMillis()));
            final String data = StringUtils.defaultString(Tile.requestMapInfo(referer, params, referer));

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
                final cgCache cache = new cgCache();

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

    /**
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
                boolean exclude = false;
                if (Settings.isExcludeMyCaches() && (cache.isFound() || cache.isOwn())) { // workaround for BM
                    exclude = true;
                }
                if (Settings.isExcludeDisabledCaches() && cache.isDisabled()) {
                    exclude = true;
                }
                if (!exclude) {
                    searchResult.addCache(cache);
                }
            }
            Log.d("Retrieved " + searchResult.getCount() + " caches for tile " + tile.toString());

        } catch (Exception e) {
            Log.e("GCBase.parseMapJSON", e);
        }

        return searchResult;
    }

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
            float speedNow = cgeoapplication.getInstance().currentGeo().getSpeed();
            strategy = speedNow >= 8 ? Strategy.FAST : Strategy.DETAILED; // 8 m/s = 30 km/h
        }
        // return searchByViewport(viewport, tokens, strategy);

        // testing purpose
        {
            SearchResult result = searchByViewport(viewport, tokens, strategy);
            String text = Formatter.SEPARATOR + strategy.getL10n() + Formatter.SEPARATOR;
            int speed = (int) cgeoapplication.getInstance().currentGeo().getSpeed();
            if (Settings.isUseMetricUnits()) {
                text += speed + " km/h";
            } else {
                text += speed / IConversion.MILES_TO_KILOMETER + " mph";
            }
            result.setUrl(result.getUrl() + text);
            return result;
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
        Log.d("GCBase.searchByViewport" + viewport.toString());

        final SearchResult searchResult = new SearchResult();
        searchResult.setUrl(GCConstants.URL_LIVE_MAP + "?ll=" + viewport.getCenter().getLatitude() + "," + viewport.getCenter().getLongitude());

        if (strategy.flags.contains(StrategyFlag.LOAD_TILES)) {
            final Set<Tile> tiles = Tile.getTilesForViewport(viewport);

            for (Tile tile : tiles) {

                if (!Tile.Cache.contains(tile)) {
                    final Parameters params = new Parameters(
                            "x", String.valueOf(tile.getX()),
                            "y", String.valueOf(tile.getY()),
                            "z", String.valueOf(tile.getZoomlevel()),
                            "ep", "1");
                    if (tokens != null) {
                        params.put("k", tokens[0], "st", tokens[1]);
                    }
                    if (Settings.isExcludeMyCaches()) { // works only for PM
                        params.put("hf", "1", "hh", "1"); // hide found, hide hidden
                    }
                    if (Settings.getCacheType() == CacheType.TRADITIONAL) {
                        params.put("ect", "9,5,3,6,453,13,1304,137,11,4,8,1858"); // 2 = tradi 3 = multi 8 = mystery
                    } else if (Settings.getCacheType() == CacheType.MULTI) {
                        params.put("ect", "9,5,2,6,453,13,1304,137,11,4,8,1858");
                    } else if (Settings.getCacheType() == CacheType.MYSTERY) {
                        params.put("ect", "9,5,3,6,453,13,1304,137,11,4,2,1858");
                    }
                    if (tile.getZoomlevel() != 14) {
                        params.put("_", String.valueOf(System.currentTimeMillis()));
                    }
                    // TODO: other types t.b.d

                    // The PNG must be requested first, otherwise the following request would always return with 204 - No Content
                    Bitmap bitmap = Tile.requestMapTile(params);

                    // Check bitmap size
                    if (bitmap != null && (bitmap.getWidth() != Tile.TILE_SIZE ||
                            bitmap.getHeight() != Tile.TILE_SIZE)) {
                        bitmap.recycle();
                        bitmap = null;
                    }

                    String data = Tile.requestMapInfo(GCConstants.URL_MAP_INFO, params, GCConstants.URL_LIVE_MAP);
                    if (StringUtils.isEmpty(data)) {
                        Log.e("GCBase.searchByViewport: No data from server for tile (" + tile.getX() + "/" + tile.getY() + ")");
                    } else {
                        final SearchResult search = GCMap.parseMapJSON(data, tile, bitmap, strategy);
                        if (search == null || CollectionUtils.isEmpty(search.getGeocodes())) {
                            Log.e("GCBase.searchByViewport: No cache parsed for viewport " + viewport);
                        }
                        else {
                            searchResult.addGeocodes(search.getGeocodes());
                        }
                        Tile.Cache.add(tile);
                    }

                    // release native bitmap memory
                    if (bitmap != null) {
                        bitmap.recycle();
                    }

                }
            }
        }

        if (strategy.flags.contains(StrategyFlag.SEARCH_NEARBY)) {
            final Geopoint center = viewport.getCenter();
            if ((lastSearchViewport == null) || !lastSearchViewport.contains(center)) {
                SearchResult search = GCParser.searchByCoords(center, Settings.getCacheType(), false);
                if (search != null && !search.isEmpty()) {
                    final Set<String> geocodes = search.getGeocodes();
                    if (Settings.isPremiumMember()) {
                        lastSearchViewport = cgeoapplication.getInstance().getBounds(geocodes);
                    } else {
                        lastSearchViewport = new Viewport(center, center);
                    }
                    searchResult.addGeocodes(geocodes);
                }
            }
        }

        return searchResult;
    }
}
