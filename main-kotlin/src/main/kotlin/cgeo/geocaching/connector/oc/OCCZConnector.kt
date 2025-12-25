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

package cgeo.geocaching.connector.oc

import cgeo.geocaching.utils.Log

import android.net.Uri

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import org.apache.commons.lang3.StringUtils

class OCCZConnector : OCBaseConnector() {

    private static val GEOCODE_PREFIX: String = "OZ"

    public OCCZConnector() {
        super("OpenCaching.CZ", "www.opencaching.cz", true, GEOCODE_PREFIX, "OC.CZ")
    }

    override     public String getGeocodeFromUrl(final String url) {
        val uri: Uri = Uri.parse(url)
        if (!StringUtils.containsIgnoreCase(uri.getHost(), getShortHost())) {
            return null
        }

        // host.tld?cacheid=cacheid
        val id: String = uri.getQueryParameter("cacheid")
        if (StringUtils.isNotBlank(id)) {
            try {
                val geocode: String = GEOCODE_PREFIX + StringUtils.leftPad(Integer.toHexString(Integer.parseInt(id)), 4, '0')
                if (canHandle(geocode)) {
                    return geocode
                }
            } catch (final NumberFormatException e) {
                Log.e("Unexpected URL for opencaching.cz " + url)
            }
        }
        return super.getGeocodeFromUrl(url)
    }

}
