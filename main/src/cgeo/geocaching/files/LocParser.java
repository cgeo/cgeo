package cgeo.geocaching.files;

import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MatcherWrapper;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

public final class LocParser extends FileParser {

    private static final String NAME_OWNER_SEPARATOR = " by ";
    private static final Pattern patternGeocode = Pattern
            .compile("name id=\"([^\"]+)\"");
    private static final Pattern patternLat = Pattern
            .compile("lat=\"([^\"]+)\"");
    private static final Pattern patternLon = Pattern
            .compile("lon=\"([^\"]+)\"");
    private static final Pattern patternName = Pattern.compile("CDATA\\[([^\\]]+)\\]");

    private static final CacheSize[] SIZES = {
            CacheSize.NOT_CHOSEN, // 1
            CacheSize.MICRO, // 2
            CacheSize.REGULAR, // 3
            CacheSize.LARGE, // 4
            CacheSize.VIRTUAL, // 5
            CacheSize.OTHER, // 6
            CacheSize.UNKNOWN, // 7
            CacheSize.SMALL, // 8
    };

    private int listId;

    public static void parseLoc(final SearchResult searchResult, final String fileContent) {
        final Map<String, Geocache> cidCoords = parseCoordinates(fileContent);

        // save found cache coordinates
        final HashSet<String> contained = new HashSet<String>();
        for (String geocode : searchResult.getGeocodes()) {
            if (cidCoords.containsKey(geocode)) {
                contained.add(geocode);
            }
        }
        Set<Geocache> caches = DataStore.loadCaches(contained, LoadFlags.LOAD_CACHE_OR_DB);
        for (Geocache cache : caches) {
            Geocache coord = cidCoords.get(cache.getGeocode());
            copyCoordToCache(coord, cache);
        }
    }

    private static void copyCoordToCache(final Geocache coord, final Geocache cache) {
        cache.setCoords(coord.getCoords());
        cache.setDifficulty(coord.getDifficulty());
        cache.setTerrain(coord.getTerrain());
        cache.setSize(coord.getSize());
        cache.setGeocode(coord.getGeocode());
        cache.setReliableLatLon(true);
        if (StringUtils.isBlank(cache.getName())) {
            cache.setName(coord.getName());
        }
        cache.setOwnerUserId(coord.getOwnerUserId());
    }

    static Map<String, Geocache> parseCoordinates(final String fileContent) {
        final Map<String, Geocache> coords = new HashMap<String, Geocache>();
        if (StringUtils.isBlank(fileContent)) {
            return coords;
        }
        // >> premium only

        final String[] points = fileContent.split("<waypoint>");

        // parse coordinates
        for (String pointString : points) {
            final Geocache pointCoord = parseCache(pointString);
            if (StringUtils.isNotBlank(pointCoord.getGeocode())) {
                coords.put(pointCoord.getGeocode(), pointCoord);
            }
        }

        Log.i("Coordinates found in .loc file: " + coords.size());
        return coords;
    }

    public static Geopoint parsePoint(final String latitude, final String longitude) {
        // the loc file contains the coordinates as plain floating point values, therefore avoid using the GeopointParser
        try {
            return new Geopoint(Double.valueOf(latitude), Double.valueOf(longitude));
        } catch (NumberFormatException e) {
            Log.e("LOC format has changed", e);
        }
        // fall back to parser, just in case the format changes
        return new Geopoint(latitude, longitude);
    }

    public LocParser(int listId) {
        this.listId = listId;
    }

    @Override
    public Collection<Geocache> parse(InputStream stream, CancellableHandler progressHandler) throws IOException, ParserException {
        final String streamContent = readStream(stream, null).toString();
        final int maxSize = streamContent.length();
        final Map<String, Geocache> coords = parseCoordinates(streamContent);
        final List<Geocache> caches = new ArrayList<Geocache>();
        for (Entry<String, Geocache> entry : coords.entrySet()) {
            Geocache coord = entry.getValue();
            if (StringUtils.isBlank(coord.getGeocode()) || StringUtils.isBlank(coord.getName())) {
                continue;
            }
            Geocache cache = new Geocache();
            cache.setReliableLatLon(true);
            copyCoordToCache(coord, cache);
            caches.add(cache);

            fixCache(cache);
            cache.setType(CacheType.UNKNOWN); // type is not given in the LOC file
            cache.setListId(listId);
            cache.setDetailed(true);
            cache.store(null);
            progressHandler.sendMessage(progressHandler.obtainMessage(0, maxSize * caches.size() / coords.size(), 0));
        }
        Log.i("Caches found in .loc file: " + caches.size());
        return caches;
    }

    public static Geocache parseCache(final String pointString) {
        final Geocache cache = new Geocache();
        final MatcherWrapper matcherGeocode = new MatcherWrapper(patternGeocode, pointString);
        if (matcherGeocode.find()) {
            cache.setGeocode(matcherGeocode.group(1).trim());
        }

        final MatcherWrapper matcherName = new MatcherWrapper(patternName, pointString);
        if (matcherName.find()) {
            final String name = matcherName.group(1).trim();
            String ownerName = StringUtils.trim(StringUtils.substringAfterLast(name, NAME_OWNER_SEPARATOR));
            if (StringUtils.isEmpty(cache.getOwnerUserId()) && StringUtils.isNotEmpty(ownerName)) {
                cache.setOwnerUserId(ownerName);
            }
            cache.setName(StringUtils.substringBeforeLast(name, NAME_OWNER_SEPARATOR).trim());
        } else {
            cache.setName(cache.getGeocode());
        }

        final MatcherWrapper matcherLat = new MatcherWrapper(patternLat, pointString);
        final MatcherWrapper matcherLon = new MatcherWrapper(patternLon, pointString);
        if (matcherLat.find() && matcherLon.find()) {
            cache.setCoords(parsePoint(matcherLat.group(1).trim(), matcherLon.group(1).trim()));
        }

        final String difficulty = StringUtils.substringBetween(pointString, "<difficulty>", "</difficulty>");
        final String terrain = StringUtils.substringBetween(pointString, "<terrain>", "</terrain>");
        final String container = StringUtils.substringBetween(pointString, "<container>", "</container");
        try {
            if (StringUtils.isNotBlank(difficulty)) {
                cache.setDifficulty(Float.parseFloat(difficulty.trim()));
            }

            if (StringUtils.isNotBlank(terrain)) {
                cache.setTerrain(Float.parseFloat(terrain.trim()));
            }

            if (StringUtils.isNotBlank(container)) {
                final int size = Integer.parseInt(container.trim());
                if (size >= 1 && size <= 8) {
                    cache.setSize(SIZES[size - 1]);
                }
            }
        } catch (NumberFormatException e) {
            Log.e("LocParser.parseCache", e);
        }

        return cache;
    }
}
