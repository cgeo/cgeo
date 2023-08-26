package cgeo.geocaching.connector.bettercacher;

import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.capability.ICacheAmendment;
import cgeo.geocaching.models.Category;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Tier;
import cgeo.geocaching.network.HttpRequest;
import cgeo.geocaching.network.HttpResponse;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.CommonUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BetterCacherConnector extends AbstractConnector implements ICacheAmendment {

    //API format:
    // - request example: https://api.bettercacher.org/cgeo/data?gccodes=XYZ123,ABC987,GC5HP25
    // - reply example:
    //   [
    //    {
    //        "gccode": "XYZ123",
    //        "error": true,
    //        "reason": "No data found for the provided gccode."
    //    },
    //    {
    //        "gccode": "ABC987",
    //        "error": true,
    //        "reason": "No data found for the provided gccode."
    //    },
    //    {
    //        "gccode": "GC5HP25",
    //        "tier": "GOLD",
    //        "attributes": [
    //            "mystery",
    //            "location",
    //            "recommendation"
    //        ]
    //    }
    //]

    public static final BetterCacherConnector INSTANCE = new BetterCacherConnector();

    private static final String API_URL_BASE = "https://api.bettercacher.org/cgeo/data?gccodes=";

    //Json parser classes

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class BetterCacherResponse extends HttpResponse {
        @JsonProperty("geocaches")
        BetterCacherCache[] caches;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class BetterCacherCache {
        @JsonProperty("gccode")
        String gccode;

        @JsonProperty("error")
        boolean error;
        @JsonProperty("reason")
        String reason;

        @JsonProperty("tier")
        String tier;

        @JsonProperty("attributes")
        String[] attributes;

        public List<Category> getCategories() {
            final List<Category> cats = new ArrayList<>();
            if (attributes != null) {
                for (String att : attributes) {
                    final Category cat = new Category("bc-" + att);
                    if (cat.getType() != Category.Type.UNKNOWN) {
                        cats.add(cat);
                    }
                }
            }
            return cats;
        }

        public Tier getTier() {
            return Tier.of(tier);
        }

    }

    private BetterCacherConnector() {
        //singleton
    }

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return true;
    }

    @Override
    public boolean isActive() {
        return Settings.isBetterCacherConnectorActive();
    }

    @NonNull
    @Override
    protected String getCacheUrlPrefix() {
        return getHostUrl() + "/geocache/";
    }

    @NonNull
    @Override
    public String getName() {
        return "bettercacher.org";
    }

    @Nullable
    @Override
    public String getCacheUrl(@NonNull final Geocache cache) {
        return getCacheUrlPrefix() + cache.getGeocode();
    }

    @NonNull
    @Override
    public String getHost() {
        return "bettercacher.org";
    }

    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
        return false;
    }

    @NonNull
    @Override
    public String getNameAbbreviated() {
        return "bc.org";
    }

    @Override
    public void amendCaches(final Collection<Geocache> allCaches) {
        //main method
        CommonUtils.executeOnPartitions(allCaches, 10, sublist -> {

            final String geocodes = CollectionStream.of(sublist).map(Geocache::getGeocode).toJoinedString(",");
            final BetterCacherCache[] caches = new HttpRequest().uri(API_URL_BASE + geocodes + "&source=cgeo")
                    .requestJson(BetterCacherCache[].class, e -> null).blockingGet();
            if (caches != null) {
                final Map<String, BetterCacherCache> bcCaches = new HashMap<>();
                for (BetterCacherCache bcCache : caches) {
                    if (bcCache.gccode != null) {
                        bcCaches.put(bcCache.gccode, bcCache);
                    }
                }
                for (Geocache cache : sublist) {
                    final BetterCacherCache bcCache = bcCaches.get(cache.getGeocode());
                    if (bcCache != null) {
                        if (bcCache.tier != null) {
                            cache.setTier(Tier.of("bc-" + bcCache.tier));
                        }
                        cache.setCategories(bcCache.getCategories());
                    }
                }
            }
        });

    }
}
