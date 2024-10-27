package cgeo.geocaching.log;

import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.utils.CommonUtils;
import cgeo.geocaching.utils.TextUtils;

import java.util.Comparator;

public enum TrackableComparator {
    TRACKABLE_COMPARATOR_NAME((lhs, rhs) -> TextUtils.COLLATOR.compare(lhs.getName(), rhs.getName())),
    TRACKABLE_COMPARATOR_TRACKCODE((lhs, rhs) -> TextUtils.COLLATOR.compare(lhs.getTrackingcode(), rhs.getTrackingcode()));

    private final Comparator<Trackable> comparator;

    TrackableComparator(final Comparator<Trackable> comparator) {
        this.comparator = CommonUtils.getNullHandlingComparator(comparator, true);
    }

    public Comparator<Trackable> getComparator() {
        return comparator;
    }

    public static TrackableComparator findByName(final String name) {
        for (final TrackableComparator comp : values()) {
            if (comp.name().equals(name)) {
                return comp;
            }
        }
        return TrackableComparator.TRACKABLE_COMPARATOR_NAME;
    }
}
