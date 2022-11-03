package cgeo.geocaching.log;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.HtmlUtils;
import cgeo.geocaching.utils.MatcherWrapper;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Entry in a log book.
 *
 * {@link LogEntry} Objects are immutable. They should be manipulated by {@link LogEntry.Builder}. Use {@link LogEntry#buildUpon()}
 * to create a {@link LogEntry.Builder} object capable of creating a new {@link LogEntry}.
 *
 * Class utilizes the Builder pattern as explained <a href="https://en.wikipedia.org/wiki/Builder_pattern">here</a>.
 * To support inheritance, it utilizes the <a href="https://en.wikipedia.org/wiki/Curiously_recurring_template_pattern">Curiously Recursive Generic Pattern</a>
 * (usage for java builders is explained e.g. <a href="https://stackoverflow.com/questions/17164375/subclassing-a-java-builder-class">here</a>)
 *
 * This object should not be referenced directly from a Geocache object to reduce the memory usage
 * of the Geocache objects.
 */
public class LogEntry implements Parcelable {

    private static final Pattern PATTERN_REMOVE_COLORS = Pattern.compile("</?font.*?>", Pattern.CASE_INSENSITIVE);

    /**
     * Log id
     */
    public final int id;
    /**
     * service-specific log id (only filled if log was loaded from a service)
     */
    @Nullable public final String serviceLogId;
    /**
     * The {@link LogType}
     */
    @NonNull public final LogType logType;
    /**
     * The author
     */
    @NonNull public final String author;
    /**
     * The author guid
     */
    @NonNull public final String authorGuid;
    /**
     * The log message
     */
    @NonNull public final String log;
    /**
     * The log date
     */
    public final long date;
    /**
     * Is a found log
     */
    public final int found;
    /**
     * Own's or Friend's log entry indicator.
     * Such logs will be visible in separated tab "Friends/Own Logs" in addition to main Logbook
     */
    public final boolean friend;
    /**
     * Report problem
     */
    public final ReportProblemType reportProblem;
    /**
     * log {@link Image} List
     */
    @NonNull public final List<Image> logImages;
    /**
     * Spotted cache name
     */
    @NonNull public final String cacheName; // used for trackables
    /**
     * Spotted cache guid
     */
    @NonNull public final String cacheGuid; // used for trackables
    /**
     * Spotted cache geocode
     */
    @NonNull public final String cacheGeocode; // used for trackables

    // Parcelable START

    @Override
    public int describeContents() {
        return 0;
    }

    protected LogEntry(final Parcel in) {
        id = in.readInt();
        serviceLogId = in.readString();
        logType = (LogType) in.readSerializable();
        author = in.readString();
        authorGuid = in.readString();
        log = in.readString();
        date = in.readLong();
        found = in.readInt();
        friend = in.readInt() == 1;
        reportProblem = (ReportProblemType) in.readSerializable();
        final List<Image> localLogImages = new ArrayList<>();
        in.readList(localLogImages, Image.class.getClassLoader());
        logImages = processLogImages(localLogImages);
        cacheName = in.readString();
        cacheGuid = in.readString();
        cacheGeocode = in.readString();
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeInt(id);
        dest.writeString(serviceLogId);
        dest.writeSerializable(logType);
        dest.writeString(author);
        dest.writeString(authorGuid);
        dest.writeString(log);
        dest.writeLong(date);
        dest.writeInt(found);
        dest.writeInt(friend ? 1 : 0);
        dest.writeSerializable(reportProblem);
        dest.writeList(logImages);
        dest.writeString(cacheName);
        dest.writeString(cacheGuid);
        dest.writeString(cacheGeocode);
    }

    public static final Parcelable.Creator<LogEntry> CREATOR = new Parcelable.Creator<LogEntry>() {
        @Override
        public LogEntry createFromParcel(final Parcel in) {
            return new LogEntry(in);
        }

        @Override
        public LogEntry[] newArray(final int size) {
            return new LogEntry[size];
        }
    };

    // Parcelable END


    private List<Image> processLogImages(final List<Image> logImages) {
        final List<Image> result = new ArrayList<>();
        for (Image img : logImages) {
            result.add(img.buildUpon()
                    .setContextInformation(author + " - " + Formatter.formatShortDateVerbally(this.date) + " - " + logType.getL10n())
                    .build());
        }
        return result;
    }

    /**
     * Helper class for building or manipulating {@link LogEntry} references.
     *
     * Use {@link #buildUpon()} to obtain a builder representing an existing {@link LogEntry}.
     */
    public static class Builder<T extends Builder<T>> {
        // see {@link LogEntry} for explanation of these properties
        protected int id = 0;
        protected String serviceLogId = null;
        @NonNull
        protected LogType logType = LogType.UNKNOWN;
        @NonNull protected String author = "";
        @NonNull protected String authorGuid = "";
        @NonNull protected String log = "";
        protected long date = 0;
        protected int found = -1;
        protected boolean friend = false;
        @NonNull protected ReportProblemType reportProblem = ReportProblemType.NO_PROBLEM;
        @NonNull protected List<Image> logImages = Collections.emptyList();
        @NonNull protected String cacheName = ""; // used for trackables
        @NonNull protected String cacheGuid = ""; // used for trackables
        @NonNull protected String cacheGeocode = ""; // used for trackables


        /**
         * Build an immutable {@link LogEntry} Object.
         */
        @NonNull
        public LogEntry build() {
            return new LogEntry(this);
        }

        /**
         * Set {@link LogEntry} id.
         *
         * @param id The log id
         */
        @NonNull
        public T setId(final int id) {
            this.id = id;
            return (T) this;
        }

        public T setServiceLogId(final String serviceLogId) {
            this.serviceLogId = serviceLogId;
            return (T) this;
        }

        /**
         * Get {@link LogEntry} id. Throws an exception if {@link #setId(int)} has not be called previously.
         *
         * @return The log id
         */
        public int getId() {
            if (id == 0) {
                throw new IllegalStateException("setId must be called before getId");
            }
            return id;
        }

        /**
         * Set {@link LogEntry} {@link LogType}.
         *
         * @param logType The {@link LogType}
         */
        @NonNull
        public T setLogType(@NonNull final LogType logType) {
            this.logType = logType;
            return (T) this;
        }

        /**
         * Set {@link LogEntry} author.
         *
         * @param author The author
         */
        @NonNull
        public T setAuthor(@NonNull final String author) {
            this.author = author;
            return (T) this;
        }

        /**
         * Set {@link LogEntry} author guid.
         *
         * @param authorGuid The author guid
         */
        @NonNull
        public T setAuthorGuid(@NonNull final String authorGuid) {
            this.authorGuid = authorGuid;
            return (T) this;
        }

        /**
         * Set {@link LogEntry} log message.
         *
         * @param message The log message
         */
        @NonNull
        public T setLog(@NonNull final String message) {
            this.log = HtmlUtils.removeExtraTags(message);
            return (T) this;
        }

        /**
         * Set {@link LogEntry} date.
         *
         * @param date The log date
         */
        @NonNull
        public T setDate(final long date) {
            this.date = date;
            return (T) this;
        }

        /**
         * Set {@link LogEntry} found.
         *
         * @param found {@code true} if this is a found {@link LogType}
         */
        @NonNull
        public T setFound(final int found) {
            this.found = found;
            return (T) this;
        }

        /**
         * Set {@link LogEntry} friend.
         *
         * @param friend {@code true} if this is a log from current user himself or his friend
         */
        @NonNull
        public T setFriend(final boolean friend) {
            this.friend = friend;
            return (T) this;
        }

        /**
         * Set {@link LogEntry} spotted cache name.
         *
         * @param cacheName The cache name
         */
        @NonNull
        public T setCacheName(@NonNull final String cacheName) {
            this.cacheName = cacheName;
            return (T) this;
        }

        /**
         * Set {@link LogEntry} spotted cache Guid.
         *
         * @param cacheGuid The cache guid
         */
        @NonNull
        public T setCacheGuid(@NonNull final String cacheGuid) {
            this.cacheGuid = cacheGuid;
            return (T) this;
        }

        /**
         * Set {@link LogEntry} spotted cache Geocode.
         *
         * @param cacheGeocode The cache geocode
         */
        @NonNull
        public T setCacheGeocode(@NonNull final String cacheGeocode) {
            this.cacheGeocode = cacheGeocode;
            return (T) this;
        }

        /**
         * Set {@link LogEntry} images.
         *
         * @param logImages The {@code Image}s List
         */
        @NonNull
        public T setLogImages(@NonNull final List<Image> logImages) {
            this.logImages = logImages;
            return (T) this;
        }

        /**
         * Add a new {@link Image} to the {@link LogEntry}.
         *
         * @param image to be added to the {@link LogEntry}
         */
        public T addLogImage(final Image image) {
            if (image.equals(Image.NONE)) {
                return (T) this;
            }

            if (logImages.isEmpty()) {
                logImages = new ArrayList<>();
            }
            logImages.add(image.buildUpon().setCategory(Image.ImageCategory.LOG).build());
            return (T) this;
        }

        public T setReportProblem(@NonNull final ReportProblemType reportProblem) {
            this.reportProblem = reportProblem;
            return (T) this;
        }
    }

    /**
     * LogEntry main constructor.
     *
     * @param builder builder to contruct from
     */
    protected LogEntry(final Builder builder) {
        this.id = builder.id;
        this.serviceLogId = builder.serviceLogId;
        this.logType = builder.logType;
        this.author = StringUtils.defaultIfBlank(builder.author, Settings.getUserName());
        this.authorGuid = builder.authorGuid;
        this.log = builder.log;
        this.date = builder.date;
        this.found = builder.found;
        this.friend = builder.friend;
        this.logImages = Collections.unmodifiableList(processLogImages(builder.logImages));
        this.cacheName = builder.cacheName;
        this.cacheGuid = builder.cacheGuid;
        this.cacheGeocode = builder.cacheGeocode;
        this.reportProblem = builder.reportProblem;
    }

    /**
     * Constructs a new {@link LogEntry.Builder}, copying the attributes from this LogEntry.
     *
     * @return A new {@link LogEntry.Builder}
     */
    public Builder buildUpon() {
        return new Builder()
                .setId(id)
                .setServiceLogId(serviceLogId)
                .setLogType(logType)
                .setAuthor(author)
                .setAuthorGuid(authorGuid)
                .setLog(log)
                .setDate(date)
                .setFound(found)
                .setFriend(friend)
                .setLogImages(new ArrayList<>(logImages))
                .setCacheName(cacheName)
                .setCacheGuid(cacheGuid)
                .setCacheGeocode(cacheGeocode);
    }

    /**
     * Get the hashCode of a LogEntry Object.
     *
     * @return The object's hash code
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(id).append(logType).append(author).append(log).append(date).append(found)
                .append(friend).append(logImages).append(cacheName).append(cacheGuid).append(cacheGeocode)
                .build();
    }

    /**
     * {@link LogEntry} {@link Comparator} by descending date
     */
    public static final Comparator<LogEntry> DESCENDING_DATE_COMPARATOR = (logEntry1, logEntry2) -> (int) (logEntry2.date - logEntry1.date);

    /**
     * Return {@code true} if passed {@link LogType} Object is equal to the current {@link LogType} Object.
     * Object are also detected as equal if date, {@link LogType}, author and log are the same.
     *
     * @return {@code true} if objects are identical
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LogEntry)) {
            return false;
        }
        final LogEntry otherLog = (LogEntry) obj;
        return date == otherLog.date &&
                logType == otherLog.logType &&
                author.compareTo(otherLog.author) == 0 &&
                log.compareTo(otherLog.log) == 0;
    }

    /**
     * Check if current LogType has Images.
     * Check if current {@link LogType} has {@link Image}.
     *
     * @return {@code true} if {@link LogType} has images
     */
    public boolean hasLogImages() {
        return CollectionUtils.isNotEmpty(logImages);
    }

    /**
     * Get the images titles separated by commas.
     * If no titles are present, display a 'default title'
     *
     * @return {@link Image} titles separated by commas or 'default title'
     */
    public CharSequence getImageTitles() {
        final List<String> titles = new ArrayList<>(5);
        for (final Image image : logImages) {
            if (StringUtils.isNotBlank(image.getTitle())) {
                titles.add(HtmlUtils.extractText(image.getTitle()));
            }
        }
        if (titles.isEmpty()) {
            titles.add(CgeoApplication.getInstance().getString(R.string.cache_log_image_default_title));
        }
        return "• " + StringUtils.join(titles, "\n• ");
    }

    /**
     * Get the log message to be displayed. Depending on the settings, color tags might be removed.
     *
     * @return Log message
     */
    public String getDisplayText() {
        if (Settings.getPlainLogs()) {
            final MatcherWrapper matcher = new MatcherWrapper(PATTERN_REMOVE_COLORS, log);
            return matcher.replaceAll(StringUtils.EMPTY);
        }
        return log;
    }

    /**
     * Check if the LogEntry is owned by the current configured user on geocaching.com connector.
     *
     * @return {@code true} if LogEntry is from current user
     */
    public boolean isOwn() {
        return author.equalsIgnoreCase(Settings.getUserName());
    }
}
