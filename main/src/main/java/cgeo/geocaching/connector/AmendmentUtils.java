package cgeo.geocaching.connector;

import cgeo.geocaching.connector.bettercacher.BetterCacherConnector;
import cgeo.geocaching.connector.capability.ICacheAmendment;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.CollectionStream;

import java.util.Collection;

public class AmendmentUtils {

    private static final ICacheAmendment[] AMENDERS = new ICacheAmendment[] {
            BetterCacherConnector.INSTANCE
    };

    private AmendmentUtils() {
        //no instance
    }

    public static void amendCaches(final Collection<Geocache> caches) {
        if (caches == null) {
            return;
        }

        for (ICacheAmendment amender : AMENDERS) {
            if (amender.isActive()) {
                amender.amendCaches(
                        CollectionStream.of(caches).filter(c -> amender.canHandle(c.getGeocode())).toList());
            }
        }
    }
}
