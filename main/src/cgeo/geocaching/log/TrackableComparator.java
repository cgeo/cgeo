package cgeo.geocaching.log;

import cgeo.geocaching.utils.TextUtils;

import java.util.Comparator;

public enum TrackableComparator {
    TRACKABLE_COMPARATOR_NAME(new Comparator<TrackableLog>() {
        @Override
        public int compare(final TrackableLog lhs, final TrackableLog rhs) {
            return TextUtils.COLLATOR.compare(lhs.name, rhs.name);
        }
    }),
    TRACKABLE_COMPARATOR_TRACKCODE(new Comparator<TrackableLog>() {
        @Override
        public int compare(final TrackableLog lhs, final TrackableLog rhs) {
            return TextUtils.COLLATOR.compare(lhs.trackCode, rhs.trackCode);
        }
    });

    private final Comparator<TrackableLog> comparator;

    TrackableComparator(final Comparator<TrackableLog> comparator) {
        this.comparator = comparator;
    }

    public Comparator<TrackableLog> getComparator() {
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
