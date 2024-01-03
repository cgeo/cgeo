package cgeo.geocaching.log;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;


/**
 * An offline log entry.
 * <p>
 * In contrast to {@link LogEntry}, this object represents a (not-yet-published) offline log entry.
 * It builds upon {@link LogEntry} and contains some properties which are only relevant for the time the log is not published yet.
 * <p>
 * Instances are immutable. For class design see {@link LogEntry}.
 *
 * Implementation Note: this class is not yet used, will be as part of Offline Logging extension (as of 2.8.20) // TODO remove when done
 */
public final class OfflineLogEntry extends LogEntry {

    /**
     * log image title praefix to use for publishing images with log
     */
    public final String imageTitlePraefix;
    /**
     * log image scale to which images should be scaled before publishing this log
     */
    public final int imageScale;
    /**
     * whether cache should be set as favorite with this log
     */
    public final boolean favorite;
    /**
     * cache rating to be set with this log
     */
    public final float rating;
    /**
     * password to use for password-protected caches (e.g. Opencaching.de)
     */
    public final String password;
    /**
     * trackable trackable log settings:  "action" to invoke with log
     */
    public final Map<String, LogTypeTrackable> inventoryActions;

    // Parcelable START

    private OfflineLogEntry(final Parcel in) {
        super(in);
        imageTitlePraefix = in.readString();
        imageScale = in.readInt();
        favorite = in.readInt() == 1;
        rating = in.readFloat();
        password = in.readString();
        inventoryActions = new HashMap<>();
        in.readMap(inventoryActions, LogTypeTrackable.class.getClassLoader());
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(imageTitlePraefix);
        dest.writeInt(imageScale);
        dest.writeInt(favorite ? 1 : 0);
        dest.writeFloat(rating);
        dest.writeString(password);
        dest.writeMap(inventoryActions);
    }

    public static final Parcelable.Creator<OfflineLogEntry> CREATOR = new Parcelable.Creator<OfflineLogEntry>() {
        @Override
        public OfflineLogEntry createFromParcel(final Parcel in) {
            return new OfflineLogEntry(in);
        }

        @Override
        public OfflineLogEntry[] newArray(final int size) {
            return new OfflineLogEntry[size];
        }
    };

    // Parcelable END


    private OfflineLogEntry(final Builder builder) {
        super(builder);
        this.imageTitlePraefix = builder.imageTitlePraefix;
        this.imageScale = builder.imageScale;
        this.favorite = builder.favorite;
        this.rating = builder.rating;
        this.password = builder.password;
        this.inventoryActions = Collections.<String, TrackableLogEntry>unmodifiableMap(builder.inventoryActions);
    }


    /**
     * Helper class for building or manipulating {@link OfflineLogEntry} references.
     * <p>
     * Use {@link #buildUpon()} to obtain a builder representing an existing {@link OfflineLogEntry}.
     */
    public static class Builder<T extends Builder<T>> extends LogEntry.Builder<T> {

        //see {@link OfflineLogEntry} for explanation of properties
        private String imageTitlePraefix = "";
        private int imageScale = -1; // not set
        private boolean favorite = false;
        private float rating = 0;
        private String password = null;
        private final Map<String, LogTypeTrackable> inventoryActions = new HashMap<>();

        /**
         * Build an immutable {@link OfflineLogEntry} Object.
         */
        @NonNull
        public OfflineLogEntry build() {
            return new OfflineLogEntry(this);
        }

        public T setImageTitlePraefix(final String imageTitlePraefix) {
            this.imageTitlePraefix = imageTitlePraefix;
            return (T) this;
        }

        public T setImageScale(final int imageScale) {
            this.imageScale = imageScale;
            return (T) this;
        }

        public T setFavorite(final boolean favorite) {
            this.favorite = favorite;
            return (T) this;
        }

        public T setRating(final float rating) {
            this.rating = rating;
            return (T) this;
        }

        public T setPassword(final String password) {
            this.password = password;
            return (T) this;
        }

        public T addInventoryAction(final String trackableGeocode, final LogTypeTrackable action) {
            this.inventoryActions.put(trackableGeocode, action);
            return (T) this;
        }

        public T addInventoryActions(final Map<String, LogTypeTrackable> actions) {
            this.inventoryActions.putAll(actions);
            return (T) this;
        }

        public T clearInventoryActions() {
            this.inventoryActions.clear();
            return (T) this;
        }
    }

    /**
     * Checks whether this instance contains changes relevant for saving the log
     * compared to a given previous instance
     * Note that this is NOT a hidden {@link #equals(Object)}-method!
     */
    public boolean hasSaveRelevantChanges(final OfflineLogEntry prev) {

        // Do not erase the saved log if the user has removed all the characters
        // without using "Clear". This may be a manipulation mistake, and erasing
        // again will be easy using "Clear" while retyping the text may not be.
        boolean changed = StringUtils.isNotEmpty(log) && !StringUtils.equals(log, prev.log);
        //other changes however lead to save anyway
        changed |= !Objects.equals(logType, prev.logType);
        changed |= !Objects.equals(reportProblem, prev.reportProblem);
        changed |= !Objects.equals(date, prev.date);
        changed |= favorite != prev.favorite;
        changed |= !equalsFloat(rating, prev.rating, 0.2f);
        changed |= !Objects.equals(password, prev.password);
        changed |= !Objects.equals(imageScale, prev.imageScale);
        changed |= !Objects.equals(imageTitlePraefix, prev.imageTitlePraefix);

        changed |= logImages.size() != prev.logImages.size() || !new HashSet<>(logImages).equals(new HashSet<>(prev.logImages));
        changed |= !Objects.equals(inventoryActions, prev.inventoryActions);

        return changed;

    }

    private static boolean equalsFloat(final Float f1, final Float f2, final float prec) {
        final boolean isF1Null = f1 == null;
        final boolean isF2Null = f2 == null;
        if (isF1Null != isF2Null) {
            return false;
        }
        if (isF1Null && isF2Null) {
            return true;
        }

        //if we come here, both are non-null
        return Math.abs(f1 - f2) < prec;
    }
}
