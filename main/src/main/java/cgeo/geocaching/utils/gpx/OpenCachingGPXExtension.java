package cgeo.geocaching.utils.gpx;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.xml.XmlNode;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class OpenCachingGPXExtension implements IGPXExtension {

    //Namespaces of extensions
    private static final Set<String> OPENCACHING_NS = Set.of(
            "https://github.com/opencaching/gpx-extension-v1"
    );

    @Override
    public void enrichGeocache(final XmlNode wptNode, final Geocache cache) {
        final XmlNode ocCache = GPXUtils.gpxNodeChild(wptNode, "cache", OPENCACHING_NS);
        if (ocCache == null) {
            return;
        }
        final String requiresPassword = GPXUtils.gpxNodeChildText(ocCache, "requires_password", OPENCACHING_NS);
        if (requiresPassword != null) {
            cache.setLogPasswordRequired(Boolean.parseBoolean(requiresPassword.trim()));
        }
        final String otherCode = GPXUtils.gpxNodeChildText(ocCache, "other_code", OPENCACHING_NS);
        if (StringUtils.isNotBlank(otherCode)) {
            cache.setDescription(Geocache.getAlternativeListingText(otherCode.trim()) + cache.getDescription());
        }
        final String size = GPXUtils.gpxNodeChildText(ocCache, "size", OPENCACHING_NS);
        if (StringUtils.isNotBlank(size)) {
            final CacheSize cacheSize = CacheSize.getById(size);
            if (cacheSize != CacheSize.UNKNOWN) {
                cache.setSize(cacheSize);
            }
        }
    }


}
