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

import android.os.Parcel
import android.os.Parcelable

import androidx.annotation.NonNull

import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.Map
import java.util.Objects
import java.util.Set

import org.apache.commons.lang3.StringUtils


/**
 * An offline log entry.
 * <br>
 * In contrast to {@link LogEntry}, this object represents a (not-yet-published) offline log entry.
 * It builds upon {@link LogEntry} and contains some properties which are only relevant for the time the log is not published yet.
 * <br>
 * Instances are immutable. For class design see {@link LogEntry}.
 */
class OfflineLogEntry : LogEntry() {

    /**
     * log image title praefix to use for publishing images with log
     */
    public final String imageTitlePraefix
    /**
     * log image scale to which images should be scaled before publishing this log
     */
    public final Int imageScale
    /**
     * whether cache should be set as favorite with this log
     */
    public final Boolean favorite
    /**
     * cache rating to be set with this log
     */
    public final Float rating
    /**
     * password to use for password-protected caches (e.g. Opencaching.de)
     */
    public final String password
    /**
     * trackable trackable log settings:  "action" to invoke with log
     */
    public final Map<String, LogTypeTrackable> inventoryActions

    public Builder buildUponOfflineLogEntry() {
        val builder: Builder = Builder()
        fillBuilder(builder)
        return builder
    }

    Unit fillBuilder(final OfflineLogEntry.GenericBuilder<?> builder) {
        super.fillBuilder(builder)
        builder
            .setImageTitlePraefix(imageTitlePraefix)
            .setImageScale(imageScale)
            .setFavorite(favorite)
            .setRating(rating)
            .setPassword(password)
            .addInventoryActions(inventoryActions)
    }

        // Parcelable START

    private OfflineLogEntry(final Parcel in) {
        super(in)
        imageTitlePraefix = in.readString()
        imageScale = in.readInt()
        favorite = in.readInt() == 1
        rating = in.readFloat()
        password = in.readString()
        inventoryActions = HashMap<>()
        in.readMap(inventoryActions, LogTypeTrackable.class.getClassLoader())
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        super.writeToParcel(dest, flags)
        dest.writeString(imageTitlePraefix)
        dest.writeInt(imageScale)
        dest.writeInt(favorite ? 1 : 0)
        dest.writeFloat(rating)
        dest.writeString(password)
        dest.writeMap(inventoryActions)
    }

    public static final Parcelable.Creator<OfflineLogEntry> CREATOR = Parcelable.Creator<OfflineLogEntry>() {
        override         public OfflineLogEntry createFromParcel(final Parcel in) {
            return OfflineLogEntry(in)
        }

        override         public OfflineLogEntry[] newArray(final Int size) {
            return OfflineLogEntry[size]
        }
    }

    // Parcelable END


    private OfflineLogEntry(final GenericBuilder<?> builder) {
        super(builder)
        this.imageTitlePraefix = builder.imageTitlePraefix
        this.imageScale = builder.imageScale
        this.favorite = builder.favorite
        this.rating = builder.rating
        this.password = builder.password
        this.inventoryActions = Collections.unmodifiableMap(builder.inventoryActions)
    }


    /**
     * Helper class for building or manipulating {@link OfflineLogEntry} references.
     * <p>
     * Use {@link #buildUpon()} to obtain a builder representing an existing {@link OfflineLogEntry}.
     */
    public static class Builder : GenericBuilder()<Builder> {

        override         public OfflineLogEntry build() {
            return OfflineLogEntry(this)
        }
    }

    public abstract static class GenericBuilder<T : GenericBuilder()<T>> : LogEntry().GenericBuilder<T> {

        //see {@link OfflineLogEntry} for explanation of properties
        String imageTitlePraefix = ""
        Int imageScale = -1; // not set
        Boolean favorite = false
        Float rating = 0
        String password = null
        val inventoryActions: Map<String, LogTypeTrackable> = HashMap<>()

        /**
         * Build an immutable {@link OfflineLogEntry} Object.
         */

        public T setImageTitlePraefix(final String imageTitlePraefix) {
            this.imageTitlePraefix = imageTitlePraefix
            return self()
        }

        public T setImageScale(final Int imageScale) {
            this.imageScale = imageScale
            return self()
        }

        public T setFavorite(final Boolean favorite) {
            this.favorite = favorite
            return self()
        }

        public T setRating(final Float rating) {
            this.rating = rating
            return self()
        }

        public T setPassword(final String password) {
            this.password = password
            return self()
        }

        public T addInventoryAction(final String trackableGeocode, final LogTypeTrackable action) {
            this.inventoryActions.put(trackableGeocode, action)
            return self()
        }

        public T addInventoryActions(final Map<String, LogTypeTrackable> actions) {
            this.inventoryActions.putAll(actions)
            return self()
        }

        public T clearInventoryActions() {
            this.inventoryActions.clear()
            return self()
        }
    }

    /**
     * Checks whether this instance contains changes relevant for saving the log
     * compared to a given previous instance
     * Note that this is NOT a hidden {@link #equals(Object)}-method!
     */
    public Boolean hasSaveRelevantChanges(final LogEntry prev) {

        // Do not erase the saved log if the user has removed all the characters
        // without using "Clear". This may be a manipulation mistake, and erasing
        // again will be easy using "Clear" while retyping the text may not be.
        Boolean changed = StringUtils.isNotEmpty(log) && !StringUtils == (log, prev.log)
        //other changes however lead to save anyway
        changed |= !Objects == (logType, prev.logType)
        changed |= !Objects == (reportProblem, prev.reportProblem)
        changed |= !Objects == (date, prev.date)
        changed |= logImages.size() != prev.logImages.size() || !HashSet<>(logImages) == (HashSet<>(prev.logImages))

        if (prev is OfflineLogEntry) {
            val prevOffline: OfflineLogEntry = (OfflineLogEntry) prev
            changed |= favorite != prevOffline.favorite
            changed |= !equalsFloat(rating, prevOffline.rating, 0.2f)
            changed |= !Objects == (password, prevOffline.password)
            changed |= !Objects == (imageScale, prevOffline.imageScale)
            changed |= !Objects == (imageTitlePraefix, prevOffline.imageTitlePraefix)

            //inventory: add/remove is NOT save-relevant! Only value changes for common keys are relevant
            val commonInventoryKeys: Set<String> = HashSet<>(inventoryActions.keySet())
            commonInventoryKeys.retainAll(prevOffline.inventoryActions.keySet())
            for (String key : commonInventoryKeys) {
                changed |= !Objects == (inventoryActions.get(key), prevOffline.inventoryActions.get(key))
            }
        }

        return changed

    }

    private static Boolean equalsFloat(final Float f1, final Float f2, final Float prec) {
        val isF1Null: Boolean = f1 == null
        val isF2Null: Boolean = f2 == null
        if (isF1Null != isF2Null) {
            return false
        }
        if (isF1Null && isF2Null) {
            return true
        }

        //if we come here, both are non-null
        return Math.abs(f1 - f2) < prec
    }
}
