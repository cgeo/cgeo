package cgeo.geocaching.models;

import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Geocache} that is known to be one of the owner's own caches which is not (yet, or no longer)
 * published on geocaching.com. Used for caches loaded via GCParser.searchOwnUnpublishedGeocaches, so the
 * rest of the app can distinguish them (e.g. via {@code instanceof}) from regular, live caches.
 */
public class UnpublishedGeocache extends Geocache {

    /**
     * Builds an UnpublishedGeocache carrying the same data as an already fully-populated {@link Geocache}
     * (e.g. the result of a normal detail-page fetch). Copies every field Geocache currently declares via
     * reflection, since Geocache has no copy constructor and dozens of fields to keep in sync by hand.
     */
    public UnpublishedGeocache(final Geocache source) {
        super();
        for (final Field field : Geocache.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                field.setAccessible(true);
                field.set(this, field.get(source));
            } catch (final IllegalAccessException e) {
                Log.e("UnpublishedGeocache: could not copy field '" + field.getName() + "' from source cache", e);
            }
        }
    }

    @Override
    @NonNull
    public List<StatusCode> getStatusCodes() {
        final List<StatusCode> codes = new ArrayList<>(super.getStatusCodes());
        codes.add(StatusCode.UNPUBLISHED_CACHE);
        return codes;
    }
}
