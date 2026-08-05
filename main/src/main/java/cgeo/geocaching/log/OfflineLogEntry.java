package cgeo.geocaching.log;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;


/**
 * An offline log entry.
 * <br>
 * In contrast to {@link LogEntry}, this object represents a (not-yet-published) offline log entry.
 * It builds upon {@link LogEntry} and contains some properties which are only relevant for the time the log is not published yet.
 * <br>
 * Instances are immutable. For class design see {@link LogEntry}.
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

    public Builder buildUponOfflineLogEntry() {
        final Builder builder = new Builder();
        fillBuilder(builder);
        return builder;
    }

    void fillBuilder(final OfflineLogEntry.GenericBuilder<?> builder) {
        super.fillBuilder(builder);
        builder
            .setImageTitlePraefix(imageTitlePraefix)
            .setImageScale(imageScale)
            .setFavorite(favorite)
            .setRating(rating)
            .setPassword(password)
            .addInventoryActions(inventoryActions);
    }

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


    private OfflineLogEntry(final GenericBuilder<?> builder) {
        super(builder);
        this.imageTitlePraefix = builder.imageTitlePraefix;
        this.imageScale = builder.imageScale;
        this.favorite = builder.favorite;
        this.rating = builder.rating;
        this.password = builder.password;
        this.inventoryActions = Collections.unmodifiableMap(builder.inventoryActions);
    }


    /**
     * Helper class for building or manipulating {@link OfflineLogEntry} references.
     * <p>
     * Use {@link #buildUpon()} to obtain a builder representing an existing {@link OfflineLogEntry}.
     */
    public static final class Builder extends GenericBuilder<Builder> {

        @NonNull
        @Override
        public OfflineLogEntry build() {
            return new OfflineLogEntry(this);
        }
    }

    public abstract static class GenericBuilder<T extends GenericBuilder<T>> extends LogEntry.GenericBuilder<T> {

        //see {@link OfflineLogEntry} for explanation of properties
        String imageTitlePraefix = "";
        int imageScale = -1; // not set
        boolean favorite = false;
        float rating = 0;
        String password = null;
        @NonNull final Map<String, LogTypeTrackable> inventoryActions = new HashMap<>();

        /**
         * Build an immutable {@link OfflineLogEntry} Object.
         */

        public T setImageTitlePraefix(final String imageTitlePraefix) {
            this.imageTitlePraefix = imageTitlePraefix;
            return self();
        }

        public T setImageScale(final int imageScale) {
            this.imageScale = imageScale;
            return self();
        }

        public T setFavorite(final boolean favorite) {
            this.favorite = favorite;
            return self();
        }

        public T setRating(final float rating) {
            this.rating = rating;
            return self();
        }

        public T setPassword(final String password) {
            this.password = password;
            return self();
        }

        public T addInventoryAction(final String trackableGeocode, final LogTypeTrackable action) {
            this.inventoryActions.put(trackableGeocode, action);
            return self();
        }

        public T addInventoryActions(final Map<String, LogTypeTrackable> actions) {
            this.inventoryActions.putAll(actions);
            return self();
        }

        public T clearInventoryActions() {
            this.inventoryActions.clear();
            return self();
        }
    }

    /**
     * Checks whether this instance contains changes relevant for saving the log
     * compared to a given previous instance
     * Note that this is NOT a hidden {@link #equals(Object)}-method!
     */
    public boolean hasSaveRelevantChanges(final LogEntry prev) {

        // Do not erase the saved log if the user has removed all the characters
        // without using "Clear". This may be a manipulation mistake, and erasing
        // again will be easy using "Clear" while retyping the text may not be.
        boolean changed = StringUtils.isNotEmpty(log) && !StringUtils.equals(log, prev.log);
        //other changes however lead to save anyway
        changed |= !Objects.equals(logType, prev.logType);
        changed |= !Objects.equals(reportProblem, prev.reportProblem);
        changed |= !Objects.equals(date, prev.date);
        changed |= logImages.size() != prev.logImages.size() || !new HashSet<>(logImages).equals(new HashSet<>(prev.logImages));

        if (prev instanceof OfflineLogEntry) {
            final OfflineLogEntry prevOffline = (OfflineLogEntry) prev;
            changed |= favorite != prevOffline.favorite;
            changed |= !equalsFloat(rating, prevOffline.rating, 0.2f);
            changed |= !Objects.equals(password, prevOffline.password);
            changed |= !Objects.equals(imageScale, prevOffline.imageScale);
            changed |= !Objects.equals(imageTitlePraefix, prevOffline.imageTitlePraefix);

            //inventory: add/remove is NOT save-relevant! Only value changes for common keys are relevant
            final Set<String> commonInventoryKeys = new HashSet<>(inventoryActions.keySet());
            commonInventoryKeys.retainAll(prevOffline.inventoryActions.keySet());
            for (String key : commonInventoryKeys) {
                changed |= !Objects.equals(inventoryActions.get(key), prevOffline.inventoryActions.get(key));
            }
        }

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
