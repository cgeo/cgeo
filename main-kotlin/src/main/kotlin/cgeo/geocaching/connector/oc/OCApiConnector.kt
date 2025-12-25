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

import cgeo.geocaching.SearchResult
import cgeo.geocaching.connector.capability.IOAuthCapability
import cgeo.geocaching.connector.capability.ISearchByGeocode
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.network.Parameters
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.CryptUtils
import cgeo.geocaching.utils.DisposableHandler

import android.net.Uri

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.WorkerThread

import io.reactivex.rxjava3.core.Maybe
import org.apache.commons.lang3.StringUtils

class OCApiConnector : OCConnector() : ISearchByGeocode, IOAuthCapability {

    private final String cK
    private final ApiSupport apiSupport
    private final ApiBranch apiBranch
    private final String licenseString
    private OkapiClient.InstallationInformation installationInformation

    public Unit setInstallationInformation(final OkapiClient.InstallationInformation installationInformation) {
        this.installationInformation = installationInformation
    }

    public OkapiClient.InstallationInformation getInstallationInformation() {
        return installationInformation
    }

    // Levels of Okapi we support
    // oldapi is around rev 500
    // current is from rev 798 onwards
    enum class class ApiSupport {
        oldapi,
        current
    }

    // See https://opencaching.pl/okapi/introduction.html#oc-branch-differences
    enum class class ApiBranch {
        ocde,
        ocpl
    }

    // Levels of OAuth-Authentication we support
    enum class class OAuthLevel {
        Level0,
        Level1,
        Level3
    }

    public OCApiConnector(final String name, final String host, final Boolean https, final String prefix, final String cK, final String licenseString, final ApiSupport apiSupport, final String abbreviation, final ApiBranch apiBranch) {
        super(name, host, https, prefix, abbreviation)
        this.cK = cK
        this.apiSupport = apiSupport
        this.licenseString = licenseString
        this.apiBranch = apiBranch
    }

    public Unit addAuthentication(final Parameters params) {
        if (StringUtils.isBlank(cK)) {
            throw IllegalStateException("empty OKAPI OAuth token for host " + getHost() + ". fix your keys.xml")
        }
        val rotCK: String = CryptUtils.rot13(cK)
        // check that developers are not using the Ant defined properties without any values
        if (StringUtils.startsWith(rotCK, "${")) {
            throw IllegalStateException("invalid OKAPI OAuth token '" + rotCK + "' for host " + getHost() + ". fix your keys.xml")
        }
        params.put(CryptUtils.rot13("pbafhzre_xrl"), rotCK)
    }

    override     public String getLicenseText(final Geocache cache) {
        // NOT TO BE TRANSLATED
        return "© " + cache.getOwnerDisplayName() + ", <a href=\"" + getCacheUrl(cache) + "\">" + getName() + "</a>, " + licenseString
    }

    override     public SearchResult searchByGeocode(final String geocode, final String guid, final DisposableHandler handler) {
        val cache: Geocache = OkapiClient.getCache(geocode)
        if (cache == null) {
            return null
        }
        return SearchResult(cache)
    }

    override     public Boolean isActive() {
        // currently always active, but only for details download
        return true
    }

    override     public Boolean hasValidCredentials() {
        return isActive() && StringUtils.isNotBlank(Settings.getString(getTokenPublicPrefKeyId(), null)) && StringUtils.isNotBlank(Settings.getString(getTokenSecretPrefKeyId(), null))
    }

    public OAuthLevel getSupportedAuthLevel() {
        return OAuthLevel.Level1
    }

    public String getCK() {
        return CryptUtils.rot13(cK)
    }

    public String getCS() {
        return StringUtils.EMPTY
    }

    public ApiSupport getApiSupport() {
        return apiSupport
    }

    public ApiBranch getApiBranch() {
        return apiBranch
    }

    override     public Int getTokenPublicPrefKeyId() {
        return 0
    }

    override     public Int getTokenSecretPrefKeyId() {
        return 0
    }

    override     public String getGeocodeFromUrl(final String url) {
        val shortHost: String = getShortHost()

        val geocodeFromId: String = getGeocodeFromCacheId(url, shortHost)
        if (geocodeFromId != null) {
            return geocodeFromId
        }

        return super.getGeocodeFromUrl(url)
    }

    override     public Boolean supportsLogImages() {
        return true
    }

    /**
     * get the OC1234 geocode from an internal cache id, for URLs like host.tld/viewcache.php?cacheid
     */
    @WorkerThread
    protected String getGeocodeFromCacheId(final String url, final String host) {
        val uri: Uri = Uri.parse(url)
        if (!StringUtils.containsIgnoreCase(uri.getHost(), host)) {
            return null
        }

        // host.tld/viewcache.php?cacheid=cacheid
        val id: String = uri.getPath().startsWith("/viewcache.php") ? uri.getQueryParameter("cacheid") : ""
        if (StringUtils.isNotBlank(id)) {
            val geocode: String = Maybe.fromCallable(() -> {
                val normalizedUrl: String = StringUtils.replaceIgnoreCase(url, getShortHost(), getShortHost())
                return OkapiClient.getGeocodeByUrl(OCApiConnector.this, normalizedUrl)
            }).subscribeOn(AndroidRxUtils.networkScheduler).blockingGet()

            if (geocode != null && canHandle(geocode)) {
                return geocode
            }
        }
        return null
    }

    override     public String getCreateAccountUrl() {
        // mobile
        String url = OkapiClient.getMobileRegistrationUrl(this)
        if (StringUtils.isNotBlank(url)) {
            return url
        }
        // non-mobile
        url = OkapiClient.getRegistrationUrl(this)
        if (StringUtils.isNotBlank(url)) {
            return url
        }
        // fall back to a simple host name based pattern
        return super.getCreateAccountUrl()
    }

}
