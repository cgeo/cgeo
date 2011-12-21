package cgeo.geocaching.files;

import cgeo.geocaching.Settings;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgCacheWrap;
import cgeo.geocaching.cgCoord;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.geopoint.GeopointParser;
import cgeo.geocaching.utils.CancellableHandler;

import org.apache.commons.lang3.StringUtils;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

    private int listId;

    public static void parseLoc(final cgCacheWrap caches,
            final String fileContent) {
        final Map<String, cgCoord> cidCoords = parseCoordinates(fileContent);

        // save found cache coordinates
        for (cgCache cache : caches.cacheList) {
            if (cidCoords.containsKey(cache.getGeocode())) {
                cgCoord coord = cidCoords.get(cache.getGeocode());

                copyCoordToCache(coord, cache);
            }
        }
    }

    private static void copyCoordToCache(final cgCoord coord, final cgCache cache) {
        cache.setCoords(coord.getCoords());
        cache.setDifficulty(coord.getDifficulty());
        cache.setTerrain(coord.getTerrain());
        cache.setSize(coord.getSize());
        cache.setGeocode(coord.getGeocode().toUpperCase());
        if (StringUtils.isBlank(cache.getName())) {
            cache.setName(coord.getName());
        }
    }

    static Map<String, cgCoord> parseCoordinates(
            final String fileContent) {
        final Map<String, cgCoord> coords = new HashMap<String, cgCoord>();
        if (StringUtils.isBlank(fileContent)) {
            return coords;
        }
        // >> premium only

        final String[] points = fileContent.split("<waypoint>");

        // parse coordinates
        for (String pointString : points) {
            final cgCoord pointCoord = new cgCoord();

            final Matcher matcherGeocode = patternGeocode.matcher(pointString);
            if (matcherGeocode.find()) {
                String geocode = matcherGeocode.group(1).trim().toUpperCase();
                pointCoord.setName(geocode);
                pointCoord.setGeocode(geocode);
            }
            final Matcher matcherName = patternName.matcher(pointString);
            if (matcherName.find()) {
                String name = matcherName.group(1).trim();
                pointCoord.setName(StringUtils.substringBeforeLast(name, " by ").trim());
                // owner = StringUtils.substringAfterLast(" by ").trim();
            }
            final Matcher matcherLat = patternLat.matcher(pointString);
            final Matcher matcherLon = patternLon.matcher(pointString);
            if (matcherLat.find() && matcherLon.find()) {
                pointCoord.setCoords(GeopointParser.parse(matcherLat.group(1).trim(), matcherLon.group(1).trim()));
            }
            final Matcher matcherDifficulty = patternDifficulty.matcher(pointString);
            if (matcherDifficulty.find()) {
                pointCoord.setDifficulty(Float.parseFloat(matcherDifficulty.group(1).trim()));
            }
            final Matcher matcherTerrain = patternTerrain.matcher(pointString);
            if (matcherTerrain.find()) {
                pointCoord.setTerrain(Float.parseFloat(matcherTerrain.group(1).trim()));
            }
            final Matcher matcherContainer = patternContainer.matcher(pointString);
            if (matcherContainer.find()) {
                final int size = Integer.parseInt(matcherContainer.group(1)
                        .trim());

                if (size == 1) {
                    pointCoord.setSize(CacheSize.NOT_CHOSEN);
                } else if (size == 2) {
                    pointCoord.setSize(CacheSize.MICRO);
                } else if (size == 3) {
                    pointCoord.setSize(CacheSize.REGULAR);
                } else if (size == 4) {
                    pointCoord.setSize(CacheSize.LARGE);
                } else if (size == 5) {
                    pointCoord.setSize(CacheSize.VIRTUAL);
                } else if (size == 6) {
                    pointCoord.setSize(CacheSize.OTHER);
                } else if (size == 8) {
                    pointCoord.setSize(CacheSize.SMALL);
                } else {
                    pointCoord.setSize(null);
                }
            }

            if (StringUtils.isNotBlank(pointCoord.getGeocode())) {
                coords.put(pointCoord.getGeocode(), pointCoord);
            }
        }

        Log.i(Settings.tag,
                "Coordinates found in .loc file: " + coords.size());
        return coords;
    }

    public LocParser(int listId) {
        this.listId = listId;
    }

    @Override
    public Collection<cgCache> parse(InputStream stream, CancellableHandler progressHandler) throws IOException, ParserException {
        // TODO: progress reporting happens during reading stream only, not during parsing
        String streamContent = readStream(stream, progressHandler).toString();
        final Map<String, cgCoord> coords = parseCoordinates(streamContent);
        final List<cgCache> caches = new ArrayList<cgCache>();
        for (Entry<String, cgCoord> entry : coords.entrySet()) {
            cgCoord coord = entry.getValue();
            if (StringUtils.isBlank(coord.getGeocode()) || StringUtils.isBlank(coord.getName())) {
                continue;
            }
            cgCache cache = new cgCache();
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
}
