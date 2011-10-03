package cgeo.geocaching.files;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgCacheWrap;
import cgeo.geocaching.cgCoord;
import cgeo.geocaching.cgSearch;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.geopoint.GeopointParser;

import org.apache.commons.lang3.StringUtils;

import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
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

    public static void parseLoc(final cgCacheWrap caches,
            final String fileContent) {
        final Map<String, cgCoord> cidCoords = parseCoordinates(fileContent);

        // save found cache coordinates
        for (cgCache cache : caches.cacheList) {
            if (cidCoords.containsKey(cache.geocode)) {
                cgCoord coord = cidCoords.get(cache.geocode);

                copyCoordToCache(coord, cache);
            }
        }
    }

    private static void copyCoordToCache(final cgCoord coord, final cgCache cache) {
        cache.coords = coord.coords;
        cache.difficulty = coord.difficulty;
        cache.terrain = coord.terrain;
        cache.size = coord.size;
        cache.geocode = coord.geocode.toUpperCase();
        if (StringUtils.isBlank(cache.name)) {
            cache.name = coord.name;
        }
    }

    public static Map<String, cgCoord> parseCoordinates(
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
                pointCoord.name = geocode;
                pointCoord.geocode = geocode;
            }
            final Matcher matcherName = patternName.matcher(pointString);
            if (matcherName.find()) {
                String name = matcherName.group(1).trim();
                pointCoord.name = StringUtils.substringBeforeLast(name, " by ").trim();
                // owner = StringUtils.substringAfterLast(" by ").trim();
            }
            final Matcher matcherLat = patternLat.matcher(pointString);
            final Matcher matcherLon = patternLon.matcher(pointString);
            if (matcherLat.find() && matcherLon.find()) {
                pointCoord.coords =
                        GeopointParser.parse(matcherLat.group(1).trim(), matcherLon.group(1).trim());
            }
            final Matcher matcherDifficulty = patternDifficulty.matcher(pointString);
            if (matcherDifficulty.find()) {
                pointCoord.difficulty = new Float(matcherDifficulty.group(1)
                        .trim());
            }
            final Matcher matcherTerrain = patternTerrain.matcher(pointString);
            if (matcherTerrain.find()) {
                pointCoord.terrain = new Float(matcherTerrain.group(1).trim());
            }
            final Matcher matcherContainer = patternContainer.matcher(pointString);
            if (matcherContainer.find()) {
                final int size = Integer.parseInt(matcherContainer.group(1)
                        .trim());

                if (size == 1) {
                    pointCoord.size = CacheSize.NOT_CHOSEN;
                } else if (size == 2) {
                    pointCoord.size = CacheSize.MICRO;
                } else if (size == 3) {
                    pointCoord.size = CacheSize.REGULAR;
                } else if (size == 4) {
                    pointCoord.size = CacheSize.LARGE;
                } else if (size == 5) {
                    pointCoord.size = CacheSize.VIRTUAL;
                } else if (size == 6) {
                    pointCoord.size = CacheSize.OTHER;
                } else if (size == 8) {
                    pointCoord.size = CacheSize.SMALL;
                } else {
                    pointCoord.size = null;
                }
            }

            if (StringUtils.isNotBlank(pointCoord.geocode)) {
                coords.put(pointCoord.geocode, pointCoord);
            }
        }

        Log.i(cgSettings.tag,
                "Coordinates found in .loc file: " + coords.size());
        return coords;
    }

    public static UUID parseLoc(File file, int listId,
            Handler handler) {
        final cgSearch search = new cgSearch();

        try {
            final Map<String, cgCoord> coords = parseCoordinates(readFile(file).toString());
            final cgCacheWrap caches = new cgCacheWrap();
            for (Entry<String, cgCoord> entry : coords.entrySet()) {
                cgCoord coord = entry.getValue();
                if (StringUtils.isBlank(coord.geocode) || StringUtils.isBlank(coord.name)) {
                    continue;
                }
                cgCache cache = new cgCache();
                copyCoordToCache(coord, cache);
                caches.cacheList.add(cache);

                fixCache(cache);
                cache.type = "traditional"; // type is not given in the LOC file
                cache.reason = listId;
                cache.detailed = true;

                cgeoapplication.getInstance().addCacheToSearch(search, cache);
            }
            caches.totalCnt = caches.cacheList.size();
            showCountMessage(handler, R.string.gpx_import_loading_stored, search.getCount());
            Log.i(cgSettings.tag, "Caches found in .gpx file: " + caches.totalCnt);
        } catch (Exception e) {
            Log.e(cgSettings.tag, "cgBase.parseGPX: " + e.toString());
        }

        return search.getCurrentId();
    }
}
