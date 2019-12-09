package cgeo.geocaching.connector.oc;

import cgeo.geocaching.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

public final class OCDEConnector extends OCApiLiveConnector {

    /**
     * Alternate Opencaching domains which are managed by Opencaching.DE.
     */
    private static final String[] MAPPED_DOMAINS = new String[] { "opencachingspain.es", "opencaching.it", "opencaching.fr" };

    public OCDEConnector() {
        super("opencaching.de", "www.opencaching.de", true, "OC", "CC BY-NC-ND, alle Logeinträge © jeweiliger Autor",
                R.string.oc_de_okapi_consumer_key, R.string.oc_de_okapi_consumer_secret,
                R.string.pref_connectorOCActive, R.string.pref_ocde_tokenpublic, R.string.pref_ocde_tokensecret, ApiSupport.current, "OC.DE");
    }

    @Override
    @Nullable
    public String getGeocodeFromUrl(@NonNull final String url) {
        for (final String mappedDomain : MAPPED_DOMAINS) {
            if (StringUtils.containsIgnoreCase(url, mappedDomain)) {
                // replace the country specific URL to not confuse the OKAPI interface
                final String deUrl = StringUtils.replaceIgnoreCase(url, mappedDomain, "opencaching.de");
                final String geocodeFromId = getGeocodeFromCacheId(deUrl, getShortHost());
                if (geocodeFromId != null) {
                    return geocodeFromId;
                }
            }
        }

        return super.getGeocodeFromUrl(url);
    }
}
