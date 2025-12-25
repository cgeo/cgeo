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

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.models.Image
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.CommonUtils
import cgeo.geocaching.utils.Formatter
import cgeo.geocaching.utils.MatcherWrapper
import cgeo.geocaching.utils.TextUtils
import cgeo.geocaching.utils.html.HtmlUtils

import android.os.Parcel
import android.os.Parcelable

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.Calendar
import java.util.Collections
import java.util.Comparator
import java.util.Date
import java.util.List
import java.util.regex.Pattern

import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.apache.commons.lang3.time.DateUtils

/**
 * Entry in a log book.
 * <br>
 * {@link LogEntry} Objects are immutable. They should be manipulated by {@link LogEntry.Builder}. Use {@link LogEntry#buildUpon()}
 * to create a {@link LogEntry.Builder} object capable of creating a {@link LogEntry}.
 * <br>
 * Class utilizes the Builder pattern as explained <a href="https://en.wikipedia.org/wiki/Builder_pattern">here</a>.
 * To support inheritance, it utilizes the <a href="https://en.wikipedia.org/wiki/Curiously_recurring_template_pattern">Curiously Recursive Generic Pattern</a>
 * (usage for java builders is explained e.g. <a href="https://stackoverflow.com/questions/17164375/subclassing-a-java-builder-class">here</a>)
 * <br>
 * This object should not be referenced directly from a Geocache object to reduce the memory usage
 * of the Geocache objects.
 */
class LogEntry : Parcelable {

    private static val PATTERN_REMOVE_COLORS: Pattern = Pattern.compile("</?font.*?>", Pattern.CASE_INSENSITIVE)

    /** Log id */
    public final Int id
    /** service-specific log id (only filled if log was loaded from a service) */
    public final String serviceLogId
    /** The {@link LogType} */
    public final LogType logType
    /** The author */
    public final String author
    /** The author guid */
    public final String authorGuid
    /** The log message */
    public final String log
    /** The log date */
    public final Long date
    /** Is a found log */
    public final Int found
    /** Own's or Friend's log entry indicator. */
    public final Boolean friend
    /** Report problem */
    public final ReportProblemType reportProblem
    /** log {@link Image} List */
    public final List<Image> logImages
    /** Spotted cache name */
    public final String cacheName; // used for trackables
    /** Spotted cache guid */
    public final String cacheGuid; // used for trackables
    /** Spotted cache geocode */
    public final String cacheGeocode; // used for trackables

    public Date getDate() {
        return Date(date)
    }

    public Calendar getCalendar() {
        val cal: Calendar = Calendar.getInstance()
        cal.setTime(getDate())
        return cal
    }

    // Parcelable START

    override     public Int describeContents() {
        return 0
    }

    protected LogEntry(final Parcel in) {
        id = in.readInt()
        serviceLogId = in.readString()
        logType = CommonUtils.intToEnum(LogType.class, in.readInt(), LogType.UNKNOWN)
        author = StringUtils.defaultString(in.readString())
        authorGuid = StringUtils.defaultString(in.readString())
        log = StringUtils.defaultString(in.readString())
        date = in.readLong()
        found = in.readInt()
        friend = in.readInt() == 1
        reportProblem = CommonUtils.intToEnum(ReportProblemType.class, in.readInt(), ReportProblemType.NO_PROBLEM)
        val localLogImages: List<Image> = ArrayList<>()
        in.readList(localLogImages, Image.class.getClassLoader())
        logImages = processLogImages(localLogImages)
        cacheName = StringUtils.defaultString(in.readString())
        cacheGuid = StringUtils.defaultString(in.readString())
        cacheGeocode = StringUtils.defaultString(in.readString())
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        dest.writeInt(id)
        dest.writeString(serviceLogId)
        dest.writeInt(CommonUtils.enumToInt(logType))
        dest.writeString(author)
        dest.writeString(authorGuid)
        dest.writeString(log)
        dest.writeLong(date)
        dest.writeInt(found)
        dest.writeInt(friend ? 1 : 0)
        dest.writeInt(CommonUtils.enumToInt(reportProblem))
        dest.writeList(logImages)
        dest.writeString(cacheName)
        dest.writeString(cacheGuid)
        dest.writeString(cacheGeocode)
    }

    public static final Parcelable.Creator<LogEntry> CREATOR = Parcelable.Creator<LogEntry>() {
        override         public LogEntry createFromParcel(final Parcel in) {
            return LogEntry(in)
        }

        override         public LogEntry[] newArray(final Int size) {
            return LogEntry[size]
        }
    }

    // Parcelable END


    private List<Image> processLogImages(final List<Image> logImages) {
        val result: List<Image> = ArrayList<>()
        for (Image img : logImages) {
            result.add(img.buildUpon()
                .setContextInformation(author + " - " + Formatter.formatShortDateVerbally(this.date) + " - " + logType.getL10n())
                .build())
        }
        return result
    }

    /**
     * Helper class for building or manipulating {@link LogEntry} references.
     * Use {@link #buildUpon()} to obtain a builder representing an existing {@link LogEntry}.
     */
    public static class Builder : GenericBuilder()<Builder> {

        override         public LogEntry build() {
            return LogEntry(this)
        }
    }

    /** base class for builders also for inherited classes. See e.g. <a href="https://stackoverflow.com/questions/21086417/builder-pattern-and-inheritance">...</a> */
    public abstract static class GenericBuilder<T : GenericBuilder()<T>> {
        // see {@link LogEntry} for explanation of these properties
        Int id = 0
        String serviceLogId = null
        LogType logType = LogType.UNKNOWN
        String author = ""
        String authorGuid = ""
        String log = ""
        Long date = 0
        Int found = -1
        Boolean friend = false
        ReportProblemType reportProblem = ReportProblemType.NO_PROBLEM
        val logImages: List<Image> = ArrayList<>()
        String cacheName = ""; // used for trackables
        String cacheGuid = ""; // used for trackables
        String cacheGeocode = ""; // used for trackables


        /** Build an immutable {@link LogEntry} Object. */
        public abstract LogEntry build()

        @SuppressWarnings("unchecked")
        final T self() {
            return (T) this
        }

        /** Set {@link LogEntry} id. */
        public T setId(final Int id) {
            this.id = id
            return self()
        }

        public T setServiceLogId(final String serviceLogId) {
            this.serviceLogId = serviceLogId
            return self()
        }

        /**
         * Get {@link LogEntry} id. Throws an exception if {@link #setId(Int)} has not be called previously.
         *
         * @return The log id
         */
        public Int getId() {
            if (id == 0) {
                throw IllegalStateException("setId must be called before getId")
            }
            return id
        }

        /**
         * Set {@link LogEntry} {@link LogType}.
         *
         * @param logType The {@link LogType}
         */
        public T setLogType(final LogType logType) {
            this.logType = logType
            return self()
        }

        /**
         * Set {@link LogEntry} author.
         *
         * @param author The author
         */
        public T setAuthor(final String author) {
            this.author = author
            return self()
        }

        /**
         * Set {@link LogEntry} author guid.
         *
         * @param authorGuid The author guid
         */
        public T setAuthorGuid(final String authorGuid) {
            this.authorGuid = authorGuid
            return self()
        }

        /**
         * Set {@link LogEntry} log message.
         *
         * @param message The log message
         */
        public T setLog(final String message) {
            this.log = HtmlUtils.removeExtraTags(message)
            return self()
        }

        /**
         * Set {@link LogEntry} date.
         *
         * @param date The log date
         */
        public T setDate(final Long date) {
            this.date = date
            return self()
        }

        /**
         * Set {@link LogEntry} found.
         *
         * @param found {@code true} if this is a found {@link LogType}
         */
        public T setFound(final Int found) {
            this.found = found
            return self()
        }

        /**
         * Set {@link LogEntry} friend.
         *
         * @param friend {@code true} if this is a log from current user himself or his friend
         */
        public T setFriend(final Boolean friend) {
            this.friend = friend
            return self()
        }

        /**
         * Set {@link LogEntry} spotted cache name.
         *
         * @param cacheName The cache name
         */
        public T setCacheName(final String cacheName) {
            this.cacheName = cacheName
            return self()
        }

        /**
         * Set {@link LogEntry} spotted cache Guid.
         *
         * @param cacheGuid The cache guid
         */
        public T setCacheGuid(final String cacheGuid) {
            this.cacheGuid = cacheGuid
            return self()
        }

        /**
         * Set {@link LogEntry} spotted cache Geocode.
         *
         * @param cacheGeocode The cache geocode
         */
        public T setCacheGeocode(final String cacheGeocode) {
            this.cacheGeocode = cacheGeocode
            return self()
        }

        /**
         * Set {@link LogEntry} images.
         *
         * @param logImages The {@code Image}s List
         */
        public T setLogImages(final List<Image> logImages) {
            this.logImages.clear()
            if (logImages != null) {
                this.logImages.addAll(logImages)
            }
            return self()
        }

        /**
         * Add a {@link Image} to the {@link LogEntry}.
         *
         * @param image to be added to the {@link LogEntry}
         */
        public T addLogImage(final Image image) {
            if (image == (Image.NONE)) {
                return self()
            }

            logImages.add(image.buildUpon().setCategory(Image.ImageCategory.LOG).build())
            return self()
        }

        public T setReportProblem(final ReportProblemType reportProblem) {
            this.reportProblem = reportProblem
            return self()
        }
    }

    /**
     * LogEntry main constructor.
     *
     * @param builder builder to contruct from
     */
    protected LogEntry(final GenericBuilder<?> builder) {
        this.id = builder.id
        this.serviceLogId = builder.serviceLogId
        this.logType = builder.logType
        this.author = StringUtils.defaultIfBlank(builder.author, Settings.getUserName())
        this.authorGuid = builder.authorGuid
        this.log = builder.log
        this.date = builder.date
        this.found = builder.found
        this.friend = builder.friend
        this.logImages = Collections.unmodifiableList(processLogImages(builder.logImages))
        this.cacheName = builder.cacheName
        this.cacheGuid = builder.cacheGuid
        this.cacheGeocode = builder.cacheGeocode
        this.reportProblem = builder.reportProblem == null ? ReportProblemType.NO_PROBLEM : builder.reportProblem
    }

    /**
     * Constructs a {@link LogEntry.Builder}, copying the attributes from this LogEntry.
     *
     * @return A {@link LogEntry.Builder}
     */
    public Builder buildUpon() {
        val builder: Builder = Builder()
        fillBuilder(builder)
        return builder
    }

    Unit fillBuilder(final GenericBuilder<?> builder) {
        builder
            .setId(id)
            .setServiceLogId(serviceLogId)
            .setLogType(logType)
            .setAuthor(author)
            .setAuthorGuid(authorGuid)
            .setLog(log)
            .setDate(date)
            .setFound(found)
            .setFriend(friend)
            .setLogImages(logImages)
            .setCacheName(cacheName)
            .setCacheGuid(cacheGuid)
            .setCacheGeocode(cacheGeocode)
    }




    /**
     * Get the hashCode of a LogEntry Object.
     *
     * @return The object's hash code
     */
    override     public Int hashCode() {
        return HashCodeBuilder()
                .append(id).append(logType).append(author).append(log).append(date).append(found)
                .append(friend).append(logImages).append(cacheName).append(cacheGuid).append(cacheGeocode)
                .build()
    }

    /**
     * {@link LogEntry} {@link Comparator} by descending date
     */
    public static val DESCENDING_DATE_COMPARATOR: Comparator<LogEntry> = (logEntry1, logEntry2) -> (Int) (logEntry2.date - logEntry1.date)

    /**
     * Return {@code true} if passed {@link LogType} Object is equal to the current {@link LogType} Object.
     * Object are also detected as equal if date, {@link LogType}, author and log are the same.
     *
     * @return {@code true} if objects are identical
     */
    override     public Boolean equals(final Object obj) {
        if (this == obj) {
            return true
        }
        if (!(obj is LogEntry)) {
            return false
        }
        val otherLog: LogEntry = (LogEntry) obj
        return date == otherLog.date &&
                logType == otherLog.logType &&
                author.compareTo(otherLog.author) == 0 &&
                log.compareTo(otherLog.log) == 0
    }

    override     public String toString() {
        return "id:" + id + "/serviceId:" + serviceLogId + "/date:" + Date(date) + "/type:" + logType +
            "/author:" + author + "/log:'" + log + "'/images:" + logImages
    }

    /**
     * Check if current LogType has Images.
     * Check if current {@link LogType} has {@link Image}.
     *
     * @return {@code true} if {@link LogType} has images
     */
    public Boolean hasLogImages() {
        return CollectionUtils.isNotEmpty(logImages)
    }

    /**
     * Get the images titles separated by commas.
     * If no titles are present, display a 'default title'
     *
     * @return {@link Image} titles separated by commas or 'default title'
     */
    public CharSequence getImageTitles() {
        val titles: List<String> = ArrayList<>(5)
        for (final Image image : logImages) {
            if (StringUtils.isNotBlank(image.getTitle())) {
                titles.add(HtmlUtils.extractText(image.getTitle()))
            }
        }
        if (titles.isEmpty()) {
            titles.add(CgeoApplication.getInstance().getString(R.string.cache_log_image_default_title))
        }
        return "• " + StringUtils.join(titles, "\n• ")
    }

    /**
     * Get the log message to be displayed. Depending on the settings, color tags might be removed.
     *
     * @return Log message
     */
    public String getDisplayText() {
        if (Settings.getPlainLogs()) {
            val matcher: MatcherWrapper = MatcherWrapper(PATTERN_REMOVE_COLORS, log)
            return matcher.replaceAll(StringUtils.EMPTY)
        }
        return log
    }

    /**
     * Check if the LogEntry is owned by the current configured user on geocaching.com connector.
     *
     * @return {@code true} if LogEntry is from current user
     */
    public Boolean isOwn() {
        return author.equalsIgnoreCase(Settings.getUserName())
    }

    /**
     * Checks, if the logs are representing the same log-entry.
     * Log-text can be empty (to deal with field-notes)
     */
    public Boolean isMatchingLog(final LogEntry log) {
        return this.logType == log.logType &&
                this.author.compareTo(log.author) == 0 &&
                DateUtils.isSameDay(Date(this.date), Date(log.date)) &&
                (this.log.isEmpty() || log.log.isEmpty() || TextUtils.isEqualStripHtmlIgnoreSpaces(this.log, log.log))
    }
}
