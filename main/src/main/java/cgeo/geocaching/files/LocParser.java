package cgeo.geocaching.files;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public final class LocParser extends FileParser {

    @NonNull
    private static final String NAME_OWNER_SEPARATOR = " by ";

    @NonNull
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

    // Used so that the initial value of the geocache is not null. Never filled.
    @NonNull
    private static final Geocache DUMMY_GEOCACHE = new Geocache();

    private final int listId;

    public static void parseLoc(final String fileContent, final Set<Geocache> caches) {
        final Map<String, Geocache> cidCoords = parseLoc(fileContent);

        for (final Geocache cache : caches) {
            final Geocache coord = cidCoords.get(cache.getGeocode());
            // Archived caches will not have any coordinates
            if (coord != null) {
                copyCoordToCache(coord, cache);
            }
        }
    }

    @NonNull
    private static Map<String, Geocache> parseLoc(final String content) {
        return parseLoc(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    @NonNull
    private static Map<String, Geocache> parseLoc(final InputStream content) {
        try {
            final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            final XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(content, StandardCharsets.UTF_8.name());
            final Map<String, Geocache> caches = new HashMap<>();
            int eventType = xpp.getEventType();
            Geocache currentCache = DUMMY_GEOCACHE;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    switch (xpp.getName()) {
                        case "waypoint":
                            currentCache = new Geocache();
                            currentCache.setType(CacheType.UNKNOWN);  // Type not present in .loc file
                            break;
                        case "name":
                            currentCache.setGeocode(xpp.getAttributeValue(null, "id"));
                            if (xpp.next() == XmlPullParser.TEXT) {
                                final String nameOwner = xpp.getText();
                                currentCache.setName(StringUtils.trim(StringUtils.substringBeforeLast(nameOwner, NAME_OWNER_SEPARATOR)));
                                currentCache.setOwnerUserId(StringUtils.trim(StringUtils.substringAfterLast(nameOwner, NAME_OWNER_SEPARATOR)));
                            }
                            break;
                        case "coord":
                            currentCache.setCoords(new Geopoint(Double.parseDouble(xpp.getAttributeValue(null, "lat")),
                                    Double.parseDouble(xpp.getAttributeValue(null, "lon"))));
                            break;
                        case "container":
                            if (xpp.next() == XmlPullParser.TEXT) {
                                currentCache.setSize(SIZES[Integer.parseInt(xpp.getText()) - 1]);
                            }
                            break;
                        case "difficulty":
                            if (xpp.next() == XmlPullParser.TEXT) {
                                currentCache.setDifficulty(Float.parseFloat(xpp.getText()));
                            }
                            break;
                        case "terrain":
                            if (xpp.next() == XmlPullParser.TEXT) {
                                currentCache.setTerrain(Float.parseFloat(xpp.getText()));
                            }
                            break;
                        default:
                            // Ignore
                    }
                } else if (eventType == XmlPullParser.END_TAG && xpp.getName().equals("waypoint") && StringUtils.isNotBlank(currentCache.getGeocode())) {
                    caches.put(currentCache.getGeocode(), currentCache);
                }
                eventType = xpp.next();
            }
            Log.d("Coordinates found in .loc content: " + caches.size());
            return caches;
        } catch (XmlPullParserException | IOException e) {
            Log.e("unable to parse .loc content", e);
            return Collections.emptyMap();
        }
    }

    private static void copyCoordToCache(final Geocache coord, final Geocache cache) {
        cache.setCoords(coord.getCoords());
        cache.setDifficulty(coord.getDifficulty());
        cache.setTerrain(coord.getTerrain());
        cache.setSize(coord.getSize());
        cache.setGeocode(coord.getGeocode());
        if (StringUtils.isBlank(cache.getName())) {
            cache.setName(coord.getName());
        }
        cache.setOwnerUserId(coord.getOwnerUserId());
    }

    @NonNull
    public static Geopoint parsePoint(final String latitude, final String longitude) {
        // the loc file contains the coordinates as plain floating point values, therefore avoid using the GeopointParser
        try {
            return new Geopoint(Double.parseDouble(latitude), Double.parseDouble(longitude));
        } catch (final NumberFormatException e) {
            Log.e("LOC format has changed", e);
        }
        // fall back to parser, just in case the format changes
        return new Geopoint(latitude, longitude);
    }

    public LocParser(final int listId) {
        this.listId = listId;
    }

    @Override
    @NonNull
    public Collection<Geocache> parse(@NonNull final InputStream stream, @Nullable final DisposableHandler progressHandler) throws IOException, ParserException {
        final int maxSize = stream.available();
        final Map<String, Geocache> coords = parseLoc(stream);
        final List<Geocache> caches = new ArrayList<>();
        for (final Entry<String, Geocache> entry : coords.entrySet()) {
            final Geocache cache = entry.getValue();
            if (StringUtils.isBlank(cache.getGeocode()) || StringUtils.isBlank(cache.getName())) {
                continue;
            }
            caches.add(cache);

            fixCache(cache);
            cache.setType(CacheType.UNKNOWN); // type is not given in the LOC file
            cache.getLists().add(listId);
            cache.setDetailed(true);
            cache.store();
            if (progressHandler != null) {
                progressHandler.sendMessage(progressHandler.obtainMessage(0, maxSize * caches.size() / coords.size(), 0));
            }
        }
        Log.i("Caches found in .loc file: " + caches.size());
        return caches;
    }

}
