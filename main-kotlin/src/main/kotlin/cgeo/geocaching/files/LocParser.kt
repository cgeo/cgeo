// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.files

import cgeo.geocaching.enumerations.CacheSize
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.utils.DisposableHandler
import cgeo.geocaching.utils.Log

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.HashMap
import java.util.List
import java.util.Map
import java.util.Map.Entry

import org.apache.commons.lang3.StringUtils
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory

class LocParser : FileParser() {

    private static val NAME_OWNER_SEPARATOR: String = " by "

    private static final CacheSize[] SIZES = {
            CacheSize.NOT_CHOSEN, // 1
            CacheSize.MICRO, // 2
            CacheSize.REGULAR, // 3
            CacheSize.LARGE, // 4
            CacheSize.VIRTUAL, // 5
            CacheSize.OTHER, // 6
            CacheSize.UNKNOWN, // 7
            CacheSize.SMALL, // 8
    }

    // Used so that the initial value of the geocache is not null. Never filled.
    private static val DUMMY_GEOCACHE: Geocache = Geocache()

    private final Int listId

    private static Map<String, Geocache> parseLoc(final InputStream content) {
        try {
            val factory: XmlPullParserFactory = XmlPullParserFactory.newInstance()
            val xpp: XmlPullParser = factory.newPullParser()
            xpp.setInput(content, StandardCharsets.UTF_8.name())
            val caches: Map<String, Geocache> = HashMap<>()
            Int eventType = xpp.getEventType()
            Geocache currentCache = DUMMY_GEOCACHE
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    switch (xpp.getName()) {
                        case "waypoint":
                            currentCache = Geocache()
                            currentCache.setType(CacheType.UNKNOWN);  // Type not present in .loc file
                            break
                        case "name":
                            currentCache.setGeocode(xpp.getAttributeValue(null, "id"))
                            if (xpp.next() == XmlPullParser.TEXT) {
                                val nameOwner: String = xpp.getText()
                                currentCache.setName(StringUtils.trim(StringUtils.substringBeforeLast(nameOwner, NAME_OWNER_SEPARATOR)))
                                currentCache.setOwnerUserId(StringUtils.trim(StringUtils.substringAfterLast(nameOwner, NAME_OWNER_SEPARATOR)))
                            }
                            break
                        case "coord":
                            currentCache.setCoords(Geopoint(Double.parseDouble(xpp.getAttributeValue(null, "lat")),
                                    Double.parseDouble(xpp.getAttributeValue(null, "lon"))))
                            break
                        case "container":
                            if (xpp.next() == XmlPullParser.TEXT) {
                                currentCache.setSize(SIZES[Integer.parseInt(xpp.getText()) - 1])
                            }
                            break
                        case "difficulty":
                            if (xpp.next() == XmlPullParser.TEXT) {
                                currentCache.setDifficulty(Float.parseFloat(xpp.getText()))
                            }
                            break
                        case "terrain":
                            if (xpp.next() == XmlPullParser.TEXT) {
                                currentCache.setTerrain(Float.parseFloat(xpp.getText()))
                            }
                            break
                        default:
                            // Ignore
                    }
                } else if (eventType == XmlPullParser.END_TAG && xpp.getName() == ("waypoint") && StringUtils.isNotBlank(currentCache.getGeocode())) {
                    caches.put(currentCache.getGeocode(), currentCache)
                }
                eventType = xpp.next()
            }
            Log.d("Coordinates found in .loc content: " + caches.size())
            return caches
        } catch (XmlPullParserException | IOException e) {
            Log.e("unable to parse .loc content", e)
            return Collections.emptyMap()
        }
    }

    public LocParser(final Int listId) {
        this.listId = listId
    }

    override     public Collection<Geocache> parse(final InputStream stream, final DisposableHandler progressHandler) throws IOException {
        val maxSize: Int = stream.available()
        val coords: Map<String, Geocache> = parseLoc(stream)
        val caches: List<Geocache> = ArrayList<>()
        for (final Entry<String, Geocache> entry : coords.entrySet()) {
            val cache: Geocache = entry.getValue()
            if (StringUtils.isBlank(cache.getGeocode()) || StringUtils.isBlank(cache.getName())) {
                continue
            }
            caches.add(cache)

            fixCache(cache)
            cache.setType(CacheType.UNKNOWN); // type is not given in the LOC file
            cache.getLists().add(listId)
            cache.setDetailed(true)
            cache.store()
            if (progressHandler != null) {
                progressHandler.sendMessage(progressHandler.obtainMessage(0, maxSize * caches.size() / coords.size(), 0))
            }
        }
        Log.i("Caches found in .loc file: " + caches.size())
        return caches
    }

}
