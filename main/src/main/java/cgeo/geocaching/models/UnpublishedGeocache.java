package cgeo.geocaching.models;

import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
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
        codes.add(isAwaitingPublication() ? StatusCode.AWAITING_PUBLICATION : StatusCode.UNPUBLISHED);
        return codes;
    }

    /**
     * geocaching.com distinguishes two states for a cache that isn't live yet: still being edited by the owner
     * ("Unpublished"), or submitted and waiting on a reviewer ("Awaiting Publication"). In the simple case, that's
     * a "submit for review" log; a reviewer can send it back to editing with a subsequent "disable" log, in which
     * case it counts as unpublished again despite the earlier submission.
     */
    private boolean isAwaitingPublication() {
        long lastSubmitDate = -1;
        for (final LogEntry log : getLogs()) {
            if (log.logType == LogType.SUBMIT_FOR_REVIEW && log.date > lastSubmitDate) {
                lastSubmitDate = log.date;
            }
        }
        if (lastSubmitDate < 0) {
            return false;
        }
        for (final LogEntry log : getLogs()) {
            if (log.logType == LogType.TEMP_DISABLE_LISTING && log.date > lastSubmitDate) {
                return false;
            }
        }
        return true;
    }
}
