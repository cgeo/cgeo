package cgeo.geocaching.files;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.GeopointParser;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.Log;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocParser extends FileParser {

    private static final Pattern patternGeocode = Pattern
            .compile("name id=\"([^\"]+)\"");
    private static final Pattern patternLat = Pattern
            .compile("lat=\"([^\"]+)\"");
    private static final Pattern patternLon = Pattern
            .compile("lon=\"([^\"]+)\"");
    // premium only >>
    private static final Pattern patternDifficulty = Pattern
            .compile("<difficulty>([^<]+)</difficulty>");
    private static final Pattern patternTerrain = Pattern
            .compile("<terrain>([^<]+)</terrain>");
    private static final Pattern patternContainer = Pattern
            .compile("<container>([^<]+)</container>");
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
        final Map<String, cgCache> cidCoords = parseCoordinates(fileContent);

        // save found cache coordinates
        final HashSet<String> contained = new HashSet<String>();
        for (String geocode : searchResult.getGeocodes()) {
            if (cidCoords.containsKey(geocode)) {
                contained.add(geocode);
            }
        }
        Set<cgCache> caches = cgeoapplication.getInstance().loadCaches(contained, LoadFlags.LOAD_CACHE_OR_DB);
        for (cgCache cache : caches) {
            cgCache coord = cidCoords.get(cache.getGeocode());
            copyCoordToCache(coord, cache);
        }
    }

    private static void copyCoordToCache(final cgCache coord, final cgCache cache) {
        cache.setCoords(coord.getCoords());
        cache.setDifficulty(coord.getDifficulty());
        cache.setTerrain(coord.getTerrain());
        cache.setSize(coord.getSize());
        cache.setGeocode(coord.getGeocode().toUpperCase());
        cache.setReliableLatLon(true);
        if (StringUtils.isBlank(cache.getName())) {
            cache.setName(coord.getName());
        }
    }

    static Map<String, cgCache> parseCoordinates(final String fileContent) {
        final Map<String, cgCache> coords = new HashMap<String, cgCache>();
        if (StringUtils.isBlank(fileContent)) {
            return coords;
        }
        // >> premium only

        final String[] points = fileContent.split("<waypoint>");

        // parse coordinates
        for (String pointString : points) {
            final cgCache pointCoord = parseCache(pointString);
            if (StringUtils.isNotBlank(pointCoord.getGeocode())) {
                coords.put(pointCoord.getGeocode(), pointCoord);
            }
        }

        Log.i(Settings.tag,
                "Coordinates found in .loc file: " + coords.size());
        return coords;
    }

    public static Geopoint parsePoint(final String latitude, final String longitude) {
        // the loc file contains the coordinates as plain floating point values, therefore avoid using the GeopointParser
        try {
            return new Geopoint(Double.valueOf(latitude), Double.valueOf(longitude));
        } catch (NumberFormatException e) {
            Log.e(Settings.tag, "LOC format has changed");
        }
        // fall back to parser, just in case the format changes
        return GeopointParser.parse(latitude, longitude);
    }

    public LocParser(int listId) {
        this.listId = listId;
    }

    @Override
    public Collection<cgCache> parse(InputStream stream, CancellableHandler progressHandler) throws IOException, ParserException {
        // TODO: progress reporting happens during reading stream only, not during parsing
        String streamContent = readStream(stream, progressHandler).toString();
        final Map<String, cgCache> coords = parseCoordinates(streamContent);
        final List<cgCache> caches = new ArrayList<cgCache>();
        for (Entry<String, cgCache> entry : coords.entrySet()) {
            cgCache coord = entry.getValue();
            if (StringUtils.isBlank(coord.getGeocode()) || StringUtils.isBlank(coord.getName())) {
                continue;
            }
            cgCache cache = new cgCache();
            cache.setReliableLatLon(true);
            copyCoordToCache(coord, cache);
            caches.add(cache);

            fixCache(cache);
            cache.setType(CacheType.UNKNOWN); // type is not given in the LOC file
            cache.setListId(listId);
            cache.setDetailed(true);
        }
        Log.i(Settings.tag, "Caches found in .loc file: " + caches.size());
        return caches;
    }

    public static cgCache parseCache(final String pointString) {
        final cgCache cache = new cgCache();
        final Matcher matcherGeocode = patternGeocode.matcher(pointString);
        if (matcherGeocode.find()) {
            final String geocode = matcherGeocode.group(1).trim().toUpperCase();
            cache.setGeocode(geocode.toUpperCase());
        }

        final Matcher matcherName = patternName.matcher(pointString);
        if (matcherName.find()) {
            final String name = matcherName.group(1).trim();
            cache.setName(StringUtils.substringBeforeLast(name, " by ").trim());
        } else {
            cache.setName(cache.getGeocode());
        }

        final Matcher matcherLat = patternLat.matcher(pointString);
        final Matcher matcherLon = patternLon.matcher(pointString);
        if (matcherLat.find() && matcherLon.find()) {
            cache.setCoords(parsePoint(matcherLat.group(1).trim(), matcherLon.group(1).trim()));
        }

        final Matcher matcherDifficulty = patternDifficulty.matcher(pointString);
        if (matcherDifficulty.find()) {
            cache.setDifficulty(Float.parseFloat(matcherDifficulty.group(1).trim()));
        }

        final Matcher matcherTerrain = patternTerrain.matcher(pointString);
        if (matcherTerrain.find()) {
            cache.setTerrain(Float.parseFloat(matcherTerrain.group(1).trim()));
        }

        final Matcher matcherContainer = patternContainer.matcher(pointString);
        if (matcherContainer.find()) {
            final int size = Integer.parseInt(matcherContainer.group(1).trim());
            if (size >= 1 && size <= 8) {
                cache.setSize(SIZES[size - 1]);
            }
        }

        return cache;
    }
}
