package cgeo.geocaching.connector.gc;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.files.ParserException;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter.Format;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;

public class GCMap {
    private GCMap() {
        // utility class
    }

    public static SearchResult searchByGeocodes(final Set<String> geocodes) {
        final SearchResult result = new SearchResult();

        final Set<String> filteredGeocodes = GCConnector.getInstance().handledGeocodes(geocodes);
        if (filteredGeocodes.isEmpty()) {
            return result;
        }
        final String geocodeList = StringUtils.join(filteredGeocodes.toArray(), "|");

        try {
            final Parameters params = new Parameters("i", geocodeList, "_", String.valueOf(System.currentTimeMillis()));
            params.add("app", "cgeo");
            final String referer = GCConstants.URL_LIVE_MAP_DETAILS;
            final String data = Tile.requestMapInfo(referer, params, referer).blockingGet();

            // Example JSON information
            // {"status":"success",
            //    "data":[{"name":"Mission: Impossible","gc":"GC1234","g":"34c2e609-5246-4f91-9029-d6c02b0f2a82","available":true,"archived":false,"subrOnly":false,"li":false,"fp":"5","difficulty":{"text":3.5,"value":"3_5"},"terrain":{"text":1.0,"value":"1"},"hidden":"7/23/2001","container":{"text":"Regular","value":"regular.gif"},"type":{"text":"Unknown Cache","value":8},"owner":{"text":"Ca$h_Cacher","value":"2db18e69-6877-402a-848d-6362621424f6"}},
            //            {"name":"HP: Hannover - Sahlkamp","gc":"GC2Q97X","g":"a09149ca-00e0-4aa2-b332-db2b4dfb18d2","available":true,"archived":false,"subrOnly":false,"li":false,"fp":"0","difficulty":{"text":1.0,"value":"1"},"terrain":{"text":1.5,"value":"1_5"},"hidden":"5/29/2011","container":{"text":"Small","value":"small.gif"},"type":{"text":"Traditional Cache","value":2},"owner":{"text":"GeoM@n","value":"1deaa69e-6bcc-421d-95a1-7d32b468cb82"}}]
            // }

            final ObjectNode json = (ObjectNode) JsonUtils.reader.readTree(data);
            final String status = json.path("status").asText();
            if (StringUtils.isBlank(status)) {
                throw new ParserException("No status inside JSON");
            }
            if ("success".compareTo(status) != 0) {
                throw new ParserException("Wrong status inside JSON");
            }
            final ArrayNode dataArray = (ArrayNode) json.get("data");
            if (dataArray == null) {
                throw new ParserException("No data inside JSON");
            }

            final List<Geocache> caches = new ArrayList<>();
            for (final JsonNode dataObject: dataArray) {
                final Geocache cache = new Geocache();
                cache.setName(dataObject.path("name").asText());
                cache.setGeocode(dataObject.path("gc").asText());
                cache.setGuid(dataObject.path("g").asText()); // 34c2e609-5246-4f91-9029-d6c02b0f2a82"
                cache.setDisabled(!dataObject.path("available").asBoolean());
                cache.setArchived(dataObject.path("archived").asBoolean());
                cache.setPremiumMembersOnly(dataObject.path("subrOnly").asBoolean());
                // "li" seems to be "false" always
                cache.setFavoritePoints(Integer.parseInt(dataObject.path("fp").asText()));
                cache.setDifficulty(Float.parseFloat(dataObject.path("difficulty").path("text").asText())); // 3.5
                cache.setTerrain(Float.parseFloat(dataObject.path("terrain").path("text").asText())); // 1.5
                cache.setHidden(GCLogin.parseGcCustomDate(dataObject.path("hidden").asText(), "MM/dd/yyyy")); // 7/23/2001
                cache.setSize(CacheSize.getById(dataObject.path("container").path("text").asText())); // Regular
                cache.setType(CacheType.getByPattern(dataObject.path("type").path("text").asText())); // Traditional Cache
                cache.setOwnerDisplayName(dataObject.path("owner").path("text").asText());

                caches.add(cache);
            }
            result.addAndPutInCache(caches);
        } catch (ParserException | ParseException | IOException | NumberFormatException ignored) {
            result.setError(StatusCode.UNKNOWN_ERROR);
        }
        return result;
    }

    /**
     * Searches the view port on the live map with Strategy.AUTO
     *
     * @param viewport
     *            Area to search
     */
    @NonNull
    public static SearchResult searchByViewport(@NonNull final Viewport viewport) {
        final SearchResult result = searchPlayMapByViewport(viewport);

        if (Settings.isDebug()) {
            result.setUrl(result.getUrl());
        }

        Log.d(String.format(Locale.getDefault(), "GCMap: returning %d caches from search", result.getCount()));

        return result;
    }


    /**
     * Searches the view port on the live map for caches.
     *
     * @param viewport
     *            Area to search
     */
    @NonNull
    private static SearchResult searchPlayMapByViewport(@NonNull final Viewport viewport) {
        Log.d("GCMap.searchPlayMapByViewport" + viewport.toString());

        final SearchResult searchResult = new SearchResult();

        if (Settings.isDebug()) {
            searchResult.setUrl(viewport.getCenter().format(Format.LAT_LON_DECMINUTE));
        }

        final GCWebAPI.MapSearchResultSet mapSearchResultSet = GCWebAPI.searchMap(viewport);
        final List<Geocache> foundCaches = new ArrayList<>();

        if (mapSearchResultSet.results != null) {
            for (final GCWebAPI.MapSearchResult r : mapSearchResultSet.results) {
                if (r.postedCoordinates != null) {
                    final Geocache c = new Geocache();
                    c.setDetailed(false);
                    c.setReliableLatLon(true);
                    c.setGeocode(r.code);
                    c.setName(r.name);
                    if (r.userCorrectedCoordinates != null) {
                        c.setCoords(new Geopoint(r.userCorrectedCoordinates.latitude, r.userCorrectedCoordinates.longitude));
                        c.setUserModifiedCoords(true);
                    } else {
                        c.setCoords(new Geopoint(r.postedCoordinates.latitude, r.postedCoordinates.longitude));
                        c.setUserModifiedCoords(false);
                    }
                    c.setType(CacheType.getByWaypointType(Integer.toString(r.geocacheType)));
                    c.setDifficulty(r.difficulty);
                    c.setTerrain(r.terrain);
                    c.setSize(containerTypeToCacheSize(r.containerType));
                    c.setPremiumMembersOnly(r.premiumOnly);

                    //Only set found if the map returns a "found",
                    //the map API will possibly lag behind and break
                    //cache merging if "not found" is set
                    if (r.userFound) {
                        c.setFound(true);
                    }

                    c.setFavoritePoints(r.favoritePoints);
                    c.setDisabled(r.cacheStatus == 1);
                    if (r.owner != null) {
                        c.setOwnerDisplayName(r.owner.username);
                        c.setOwnerUserId(r.owner.username);
                    }
                    foundCaches.add(c);
                }
            }
        }

        searchResult.addAndPutInCache(foundCaches);

        return searchResult;
    }

    private static CacheSize containerTypeToCacheSize(final int containerType) {
        switch (containerType) {
            case 2:
                return CacheSize.MICRO;
            case 3:
                return CacheSize.REGULAR;
            case 4:
                return CacheSize.LARGE;
            case 6:
                return CacheSize.OTHER;
            case 8:
                return CacheSize.SMALL;
            default:
                return CacheSize.UNKNOWN;
        }
    }
}
