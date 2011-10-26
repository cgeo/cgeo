package cgeo.geocaching.files;

import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgCacheWrap;
import cgeo.geocaching.cgCoord;
import cgeo.geocaching.cgSearch;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.geopoint.GeopointParser;

import org.apache.commons.lang3.StringUtils;

import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.util.HashMap;
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
                pointCoord.setDifficulty(new Float(matcherDifficulty.group(1).trim()));
            }
            final Matcher matcherTerrain = patternTerrain.matcher(pointString);
            if (matcherTerrain.find()) {
                pointCoord.setTerrain(new Float(matcherTerrain.group(1).trim()));
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

    public static cgSearch parseLoc(File file, int listId,
            Handler handler) {
        final cgSearch search = new cgSearch();

        try {
            final Map<String, cgCoord> coords = parseCoordinates(readFile(file).toString());
            final cgCacheWrap caches = new cgCacheWrap();
            for (Entry<String, cgCoord> entry : coords.entrySet()) {
                cgCoord coord = entry.getValue();
                if (StringUtils.isBlank(coord.getGeocode()) || StringUtils.isBlank(coord.getName())) {
                    continue;
                }
                cgCache cache = new cgCache();
                copyCoordToCache(coord, cache);
                caches.cacheList.add(cache);

                fixCache(cache);
                cache.setCacheType(CacheType.UNKNOWN); // type is not given in the LOC file
                cache.setReason(listId);
                cache.setDetailed(true);

                cgeoapplication.getInstance().addCacheToSearch(search, cache);
            }
            caches.totalCnt = caches.cacheList.size();
            showCountMessage(handler, R.string.gpx_import_loading_stored, search.getCount());
            Log.i(Settings.tag, "Caches found in .loc file: " + caches.totalCnt);
        } catch (Exception e) {
            Log.e(Settings.tag, "LocParser.parseLoc: " + e.toString());
        }

        return search;
    }
}
