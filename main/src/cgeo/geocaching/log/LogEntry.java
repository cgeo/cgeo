package cgeo.geocaching.log;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.HtmlUtils;
import cgeo.geocaching.utils.MatcherWrapper;

import androidx.annotation.NonNull;

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
 * This object should not be referenced directly from a Geocache object to reduce the memory usage
 * of the Geocache objects.
 *
 */
public final class LogEntry {

    private static final Pattern PATTERN_REMOVE_COLORS = Pattern.compile("</?font.*?>", Pattern.CASE_INSENSITIVE);

    /** Log id */
    public final int id;
    /** The {@link LogType} */
    @NonNull private final LogType logType;
    /** The author */
    @NonNull public final String author;
    /** The log message */
    @NonNull public final String log;
    /** The log date */
    public final long date;
    /** Is a found log */
    public final int found;
    /** Own's or Friend's log entry indicator.
     * Such logs will be visible in separated tab "Friends/Own Logs" in addition to main Logbook
     * */
    public final boolean friend;
    /** Report problem */
    public final ReportProblemType reportProblem;
    /** log {@link Image} List */
    @NonNull private final List<Image> logImages;
    /** Spotted cache name */
    @NonNull public final String cacheName; // used for trackables
    /** Spotted cache guid */
    @NonNull public final String cacheGuid; // used for trackables
    /** Spotted cache geocode */
    @NonNull public final String cacheGeocode; // used for trackables

    /**
     * Helper class for building or manipulating {@link LogEntry} references.
     *
     * Use {@link #buildUpon()} to obtain a builder representing an existing {@link LogEntry}.
     */
    public static class Builder {
        /** Log id */
        private int id;
        /** The LogType */
        @NonNull
        private LogType logType;
        /** The author */
        @NonNull private String author;
        /** The log message */
        @NonNull private String message;
        /** The log date */
        private long date;
        /** Is a found log */
        private int found;
        /** Friend's log entry */
        private boolean friend;
        /** report problem */
        @NonNull private ReportProblemType reportProblem;
        /** log {@link Image} List */
        @NonNull private List<Image> logImages;
        /** Spotted cache name */
        @NonNull private String cacheName; // used for trackables
        /** Spotted cache guid */
        @NonNull private String cacheGuid; // used for trackables
        /** Spotted cache geocode */
        @NonNull private String cacheGeocode; // used for trackables


        /**
         * Create a new LogEntry.Builder.
         *
         */
        public Builder() {
            id = 0;
            logType = LogType.UNKNOWN;
            author = "";
            message = "";
            date = 0;
            found = -1;
            friend = false;
            logImages = Collections.emptyList();
            cacheName = "";
            cacheGuid = "";
            cacheGeocode = "";
            reportProblem = ReportProblemType.NO_PROBLEM;
        }

        /**
         * Build an immutable {@link LogEntry} Object.
         *
         */
        @NonNull
        public LogEntry build() {
            return new LogEntry(id, logType, StringUtils.defaultIfBlank(author, Settings.getUserName()),
                    message, date, found, friend, logImages, cacheName, cacheGuid, cacheGeocode, reportProblem);
        }

        /**
         * Set {@link LogEntry} id.
         *
         * @param id
         *          The log id
         */
        @NonNull
        public Builder setId(final int id) {
            this.id = id;
            return this;
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
         * @param logType
         *          The {@link LogType}
         */
        @NonNull
        public Builder setLogType(@NonNull final LogType logType) {
            this.logType = logType;
            return this;
        }

        /**
         * Set {@link LogEntry} author.
         *
         * @param author
         *          The author
         */
        @NonNull
        public Builder setAuthor(@NonNull final String author) {
            this.author = author;
            return this;
        }

        /**
         * Set {@link LogEntry} log message.
         *
         * @param message
         *          The log message
         */
        @NonNull
        public Builder setLog(@NonNull final String message) {
            this.message = HtmlUtils.removeExtraTags(message);
            return this;
        }

        /**
         * Set {@link LogEntry} date.
         *
         * @param date
         *      The log date
         */
        @NonNull
        public Builder setDate(final long date) {
            this.date = date;
            return this;
        }

        /**
         * Set {@link LogEntry} found.
         *
         * @param found
         *      {@code true} if this is a found {@link LogType}
         */
        @NonNull
        public Builder setFound(final int found) {
            this.found = found;
            return this;
        }

        /**
         * Set {@link LogEntry} friend.
         *
         * @param friend
         *          {@code true} if this is a log from current user himself or his friend
         */
        @NonNull
        public Builder setFriend(final boolean friend) {
            this.friend = friend;
            return this;
        }

        /**
         * Set {@link LogEntry} spotted cache name.
         *
         * @param cacheName
         *          The cache name
         */
        @NonNull
        public Builder setCacheName(@NonNull final String cacheName) {
            this.cacheName = cacheName;
            return this;
        }

        /**
         * Set {@link LogEntry} spotted cache Guid.
         *
         * @param cacheGuid
         *          The cache guid
         */
        @NonNull
        public Builder setCacheGuid(@NonNull final String cacheGuid) {
            this.cacheGuid = cacheGuid;
            return this;
        }

        /**
         * Set {@link LogEntry} spotted cache Geocode.
         *
         * @param cacheGeocode
         *          The cache geocode
         */
        @NonNull
        public Builder setCacheGeocode(@NonNull final String cacheGeocode) {
            this.cacheGeocode = cacheGeocode;
            return this;
        }

        /**
         * Set {@link LogEntry} images.
         *
         * @param logImages
         *          The {@code Image}s List
         */
        @NonNull
        public Builder setLogImages(@NonNull final List<Image> logImages) {
            this.logImages = logImages;
            return this;
        }

        /**
         * Add a new {@link Image} to the {@link LogEntry}.
         *
         * @param image
         *          to be added to the {@link LogEntry}
         */
        public Builder addLogImage(final Image image) {
            if (image.equals(Image.NONE)) {
                return this;
            }

            if (logImages.isEmpty()) {
                logImages = new ArrayList<>();
            }
            logImages.add(image);
            return this;
        }

        public Builder setReportProblem(@NonNull final String reportProblemCode) {
            this.reportProblem = ReportProblemType.findByCode(reportProblemCode);
            return this;
        }
    }

    /**
     * LogEntry main constructor.
     *
     * @param id log id
     * @param logType log {@link LogType}
     * @param author log author
     * @param log log message
     * @param date log date
     * @param found is a found log
     * @param friend is a friend log
     * @param logImages log images
     * @param cacheName spotted cache name
     * @param cacheGuid spotted cache guid
     * @param cacheGeocode spotted cache geocode
     */
    private LogEntry(final int id, @NonNull final LogType logType, @NonNull final String author, @NonNull final String log,
                     final long date, final int found, final boolean friend,
                     @NonNull final List<Image> logImages,
                     @NonNull final String cacheName, @NonNull final String cacheGuid, @NonNull final String cacheGeocode, @NonNull final ReportProblemType reportProblem) {
        this.id = id;
        this.logType = logType;
        this.author = author;
        this.log = log;
        this.date = date;
        this.found = found;
        this.friend = friend;
        this.logImages = logImages;
        this.cacheName = cacheName;
        this.cacheGuid = cacheGuid;
        this.cacheGeocode = cacheGeocode;
        this.reportProblem = reportProblem;
    }

    /**
     * Constructs a new {@link LogEntry.Builder}, copying the attributes from this LogEntry.
     *
     * @return
     *          A new {@link LogEntry.Builder}
     */
    public Builder buildUpon() {
        return new Builder()
                .setId(id)
                .setLogType(logType)
                .setAuthor(author)
                .setLog(log)
                .setDate(date)
                .setFound(found)
                .setFriend(friend)
                .setLogImages(logImages)
                .setCacheName(cacheName)
                .setCacheGuid(cacheGuid)
                .setCacheGeocode(cacheGeocode);

    }

    /**
     * Get the {@link LogType}
     *
     * @return
     *          The {@link LogType}
     */
    public LogType getType() {
        return logType;
    }

    /**
     * Get the {@link Image} for log
     *
     * @return
     *          The {@link Image}s List
     */
    @NonNull
    public List<Image> getLogImages() {
        return logImages;
    }

    /**
     * Get the hashCode of a LogEntry Object.
     *
     * @return
     *          The object's hash code
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
    public static final Comparator<LogEntry> DESCENDING_DATE_COMPARATOR = new Comparator<LogEntry>() {
        @Override
        public int compare(final LogEntry logEntry1, final LogEntry logEntry2) {
            return (int) (logEntry2.date - logEntry1.date);
        }
    };

    /**
     * Return {@code true} if passed {@link LogType} Object is equal to the current {@link LogType} Object.
     * Object are also detected as equal if date, {@link LogType}, author and log are the same.
     *
     * @return
     *          {@code true} if objects are identical
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
     * @return
     *          {@code true} if {@link LogType} has images
     */
    public boolean hasLogImages() {
        return CollectionUtils.isNotEmpty(logImages);
    }

    /**
     * Get the images titles separated by commas.
     * If no titles are present, display a 'default title'
     *
     * @return
     *          {@link Image} titles separated by commas or 'default title'
     */
    public CharSequence getImageTitles() {
        final List<String> titles = new ArrayList<>(5);
        assert logImages != null; // make compiler happy
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
     * @return
     *          Log message
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
     * @return
     *          {@code true} if LogEntry is from current user
     */
    public boolean isOwn() {
        return author.equalsIgnoreCase(Settings.getUserName());
    }
}
