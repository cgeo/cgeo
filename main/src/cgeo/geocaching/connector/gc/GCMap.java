package cgeo.geocaching.connector.gc;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.files.ParserException;
import cgeo.geocaching.filters.core.AttributesGeocacheFilter;
import cgeo.geocaching.filters.core.BaseGeocacheFilter;
import cgeo.geocaching.filters.core.DifficultyGeocacheFilter;
import cgeo.geocaching.filters.core.DistanceGeocacheFilter;
import cgeo.geocaching.filters.core.FavoritesGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.HiddenGeocacheFilter;
import cgeo.geocaching.filters.core.LogEntryGeocacheFilter;
import cgeo.geocaching.filters.core.NameGeocacheFilter;
import cgeo.geocaching.filters.core.OriginGeocacheFilter;
import cgeo.geocaching.filters.core.OwnerGeocacheFilter;
import cgeo.geocaching.filters.core.SizeGeocacheFilter;
import cgeo.geocaching.filters.core.StatusGeocacheFilter;
import cgeo.geocaching.filters.core.TerrainGeocacheFilter;
import cgeo.geocaching.filters.core.TypeGeocacheFilter;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter.Format;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        try (ContextLogger cLog = new ContextLogger(Log.LogLevel.DEBUG, "GCMap.searchByViewport")) {
            cLog.add("vp:" + viewport);

            final SearchResult searchResult = GCWebAPI.searchMap(viewport);

            if (Settings.isDebug()) {
                    searchResult.setUrl(viewport.getCenter().format(Format.LAT_LON_DECMINUTE));
            }
            cLog.add("returning " + searchResult.getCount() + " caches");
            return searchResult;
        }
    }

    @NonNull
    public static SearchResult searchByFilter(@NonNull final GeocacheFilter filter, final IConnector connector) {
        final GCWebAPI.WebApiSearch search = new GCWebAPI.WebApiSearch();
        search.setOrigin(Sensors.getInstance().currentGeo().getCoords());
        search.setPage(200, 0);

        for (BaseGeocacheFilter baseFilter: filter.getAndChainIfPossible()) {
            if (baseFilter instanceof OriginGeocacheFilter && !((OriginGeocacheFilter) baseFilter).allowsCachesOf(connector)) {
                return new SearchResult(); //no need to search if connector is filtered out itself
            }
            fillForBasicFilter(baseFilter, search);
        }

        return GCWebAPI.searchCaches(search);
    }

    private static boolean fillForBasicFilter(@NonNull final BaseGeocacheFilter basicFilter, final GCWebAPI.WebApiSearch search) {
        switch (basicFilter.getType()) {
            case TYPE:
                search.addCacheTypes(((TypeGeocacheFilter) basicFilter).getRawValues());
                break;
            case NAME:
                search.setKeywords(((NameGeocacheFilter) basicFilter).getStringFilter().getTextValue());
                break;
            case ATTRIBUTES: //TODO: does not work for v1!
                search.addCacheAttributes(
                    CollectionStream.of(((AttributesGeocacheFilter) basicFilter).getAttributes().entrySet())
                    .filter(e -> Boolean.TRUE.equals(e.getValue())).map(Map.Entry::getKey).toArray(CacheAttribute.class));
                break;
            case SIZE:
                search.addCacheSizes(((SizeGeocacheFilter) basicFilter).getValues());
                break;
            case DISTANCE:
                final DistanceGeocacheFilter distanceFilter = (DistanceGeocacheFilter) basicFilter;
                final Geopoint coord = distanceFilter.getEffectiveCoordinate();
                if (distanceFilter.getMaxRangeValue() != null) {
                    search.setBox(new Viewport(coord, distanceFilter.getMaxRangeValue()));
                } else {
                    search.setOrigin(coord);
                }
                break;
            case DIFFICULTY:
                search.setDifficulty(((DifficultyGeocacheFilter) basicFilter).getMinRangeValue(), ((DifficultyGeocacheFilter) basicFilter).getMaxRangeValue());
                break;
            case TERRAIN:
                search.setTerrain(((TerrainGeocacheFilter) basicFilter).getMinRangeValue(), ((TerrainGeocacheFilter) basicFilter).getMaxRangeValue());
                break;
            case OWNER:
                search.setHiddenBy(((OwnerGeocacheFilter) basicFilter).getStringFilter().getTextValue());
                break;
            case FAVORITES:
                final FavoritesGeocacheFilter favFilter = (FavoritesGeocacheFilter) basicFilter;
                if (!favFilter.isPercentage()) {
                    search.setMinFavoritepoints(Math.round(favFilter.getMinRangeValue()));
                }
                break;
            case STATUS:
                final StatusGeocacheFilter statusFilter = (StatusGeocacheFilter) basicFilter;
                search.setStatusFound(statusFilter.getStatusFound());
                search.setStatusOwn(statusFilter.getStatusOwned());
                search.setStatusEnabled(statusFilter.isExcludeDisabled() ? Boolean.TRUE : (statusFilter.isExcludeActive() ? Boolean.FALSE : null));
                break;
            case HIDDEN:
                final HiddenGeocacheFilter hiddenFilter = (HiddenGeocacheFilter) basicFilter;
                search.setPlacementDate(hiddenFilter.getMinDate(), hiddenFilter.getMaxDate());
                break;
            case LOG_ENTRY:
                final LogEntryGeocacheFilter foundByFilter = (LogEntryGeocacheFilter) basicFilter;
                if (foundByFilter.isInverse()) {
                    search.setNotFoundBy(foundByFilter.getFoundByUser());
                }
                break;
            default:
                break;
        }
        return true;
    }
 }
