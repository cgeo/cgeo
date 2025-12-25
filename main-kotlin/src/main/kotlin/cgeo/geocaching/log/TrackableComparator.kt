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

package cgeo.geocaching.log

import cgeo.geocaching.models.Trackable
import cgeo.geocaching.utils.CommonUtils
import cgeo.geocaching.utils.TextUtils

import java.util.Comparator

enum class class TrackableComparator {
    TRACKABLE_COMPARATOR_NAME((lhs, rhs) -> TextUtils.COLLATOR.compare(lhs.getName(), rhs.getName())),
    TRACKABLE_COMPARATOR_TRACKCODE((lhs, rhs) -> TextUtils.COLLATOR.compare(lhs.getTrackingcode(), rhs.getTrackingcode()))

    private final Comparator<Trackable> comparator

    TrackableComparator(final Comparator<Trackable> comparator) {
        this.comparator = CommonUtils.getNullHandlingComparator(comparator, true)
    }

    public Comparator<Trackable> getComparator() {
        return comparator
    }

    public static TrackableComparator findByName(final String name) {
        for (final TrackableComparator comp : values()) {
            if (comp.name() == (name)) {
                return comp
            }
        }
        return TrackableComparator.TRACKABLE_COMPARATOR_NAME
    }
}
