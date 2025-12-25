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

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.models.Route
import cgeo.geocaching.models.RouteItem
import cgeo.geocaching.models.RouteSegment

import android.sax.Element
import android.sax.RootElement
import android.util.Xml

import androidx.annotation.NonNull

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.ArrayList

import org.apache.commons.lang3.StringUtils
import org.xml.sax.SAXException

/**
 * parses GPX file with only <wpt> elements as track points
 * (files like this are created by flopp.net and others)
 */
class GPXWptAsTrackParser : AbstractTrackOrRouteParser.RouteParse {

    protected final String namespace
    private final String version
    protected final Route result

    // temporary variables
    protected ArrayList<Geopoint> temp
    protected Element point

    protected GPXWptAsTrackParser(final String namespaceIn, final String versionIn) {
        namespace = namespaceIn
        version = versionIn
        result = Route(false)
    }

    public Route parse(final InputStream stream) throws IOException, ParserException {
        val root: RootElement = RootElement(namespace, "gpx")
        point = root.getChild(namespace, "wpt")

        root.setStartElementListener(attrs -> temp = ArrayList<>())

        root.setEndElementListener(() -> {
            if (!temp.isEmpty()) {
                result.add(RouteSegment(RouteItem(temp.get(temp.size() - 1)), temp, false))
                temp = null
            }
        })

        point.setStartElementListener(attrs -> {
            if (attrs.getIndex("lat") > -1 && attrs.getIndex("lon") > -1) {
                val latitude: String = attrs.getValue("lat")
                val longitude: String = attrs.getValue("lon")
                if (StringUtils.isNotBlank(latitude) && StringUtils.isNotBlank(longitude)) {
                    temp.add(Geopoint(Double.parseDouble(latitude), Double.parseDouble(longitude)))
                }
            }
        })

        try {
            val progressStream: ProgressInputStream = ProgressInputStream(stream)
            val reader: BufferedReader = BufferedReader(InputStreamReader(progressStream, StandardCharsets.UTF_8))
            Xml.parse(InvalidXMLCharacterFilterReader(reader), root.getContentHandler())
            return result
        } catch (final SAXException e) {
            throw ParserException("Cannot parse .gpx file as GPX " + version + ": could not parse XML (" + e.getMessage() + ")", e)
        }
    }

}
