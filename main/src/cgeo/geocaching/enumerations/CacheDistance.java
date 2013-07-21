package cgeo.geocaching.enumerations;

import cgeo.geocaching.R;
import cgeo.geocaching.cgeoapplication;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum CacheDistance {
    NEAR("Near", 0, 3, R.string.cache_distance_near),
    REGULAR("Regular", 3, 5, R.string.cache_distance_regular),
    FAR("Far", 5, 10, R.string.cache_distance_far);

	public final String id;
    public final int minDistance;
    public final int maxDistance;
    private final int stringId;

    CacheDistance(String id, int minDistance, int maxDistance, int stringId) {
        this.id = id;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.stringId = stringId;
    }

    final private static Map<String, CacheDistance> FIND_BY_ID;
    static {
        final HashMap<String, CacheDistance> mapping = new HashMap<String, CacheDistance>();
        for (CacheDistance cd : values()) {
            mapping.put(cd.id.toLowerCase(Locale.US), cd);
        }
        FIND_BY_ID = Collections.unmodifiableMap(mapping);
    }

    public static CacheDistance getById(final String id) {
        final CacheDistance result = CacheDistance.FIND_BY_ID.get(id);
        if (result != null) {
            return result;
        }
        final CacheDistance resultNormalized = CacheDistance.FIND_BY_ID.get(id.toLowerCase(Locale.US).trim());
        if (resultNormalized != null) {
            return resultNormalized;
        }
        return FAR; //put default if needed
    }

    public final String getL10n() {
        return cgeoapplication.getInstance().getBaseContext().getResources().getString(stringId);
    }
}
