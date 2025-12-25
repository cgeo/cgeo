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

import cgeo.geocaching.R

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import org.apache.commons.lang3.StringUtils

class OCDEConnector : OCApiLiveConnector() {

    /**
     * Alternate Opencaching domains which are managed by Opencaching.DE.
     * current: no mapped domain; opencaching.es is a direct forward to opencaching.de/?locale=ES
     */
    private static final String[] MAPPED_DOMAINS = String[]{null}

    public OCDEConnector() {
        super("opencaching.de", "www.opencaching.de", true, "OC", "CC BY-NC-ND, alle Logeinträge © jeweiliger Autor",
                R.string.oc_de_okapi_consumer_key, R.string.oc_de_okapi_consumer_secret,
                R.string.pref_connectorOCActive, R.string.pref_ocde_tokenpublic, R.string.pref_ocde_tokensecret, ApiSupport.current, "OC.DE", ApiBranch.ocde, R.string.preference_screen_ocde)
    }

    override     public String getGeocodeFromUrl(final String url) {
        for (final String mappedDomain : MAPPED_DOMAINS) {
            //noinspection ConstantConditions
            if (StringUtils.containsIgnoreCase(url, mappedDomain)) {
                // replace the country specific URL to not confuse the OKAPI interface val deUrl: String = StringUtils.replaceIgnoreCase(url, mappedDomain, "opencaching.de")
                val geocodeFromId: String = getGeocodeFromCacheId(deUrl, getShortHost())
                if (geocodeFromId != null) {
                    return geocodeFromId
                }
            }
        }

        return super.getGeocodeFromUrl(url)
    }
}
