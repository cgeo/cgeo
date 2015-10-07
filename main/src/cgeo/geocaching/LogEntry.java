package cgeo.geocaching;

import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.HtmlUtils;
import cgeo.geocaching.utils.MatcherWrapper;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Entry in a log book. This object should not be referenced directly from a Geocache object to reduce the memory usage
 * of the Geocache objects.
 * 
 */
public final class LogEntry {

    private static final Pattern PATTERN_REMOVE_COLORS = Pattern.compile("</?font.*?>", Pattern.CASE_INSENSITIVE);

    public int id = 0;
    public LogType type = LogType.UNKNOWN;
    public String author = "";
    public String log = "";
    public long date = 0;
    public int found = -1;
    /** Friend's log entry */
    public boolean friend = false;
    private List<Image> logImages = null;
    public String cacheName = ""; // used for trackables
    public String cacheGuid = ""; // used for trackables

    /**
     * Construct a LogEntry Object usable for logging Geocaches.
     * Username field is automatically taken from the current configured user on geocaching.com connector.
     *
     * @param dateInMilliSeconds of log
     * @param type of log
     * @param text message of log
     */
    public LogEntry(final long dateInMilliSeconds, final LogType type, final String text) {
        this(Settings.getUsername(), dateInMilliSeconds, type, text);
    }

    /**
     * Construct a LogEntry Object usable for logging Geocaches.
     *
     * @param author of log
     * @param dateInMilliSeconds of log
     * @param type of log
     * @param text message of log
     */
    public LogEntry(final String author, final long dateInMilliSeconds, final LogType type, final String text) {
        this.author = author;
        this.date = dateInMilliSeconds;
        this.type = type;
        this.log = text;
    }

    /**
     * Get the hashCode of a LogDate Object.
     *
     * @return the object's hash code
     */
    @Override
    public int hashCode() {
        return (int) date * type.hashCode() * author.hashCode() * log.hashCode();
    }

    /**
     * LogEntry Comparator by descending date
     */
    public static final Comparator<LogEntry> DESCENDING_DATE_COMPARATOR = new Comparator<LogEntry>() {
        @Override
        public int compare(final LogEntry logEntry1, final LogEntry logEntry2) {
            return (int) (logEntry2.date - logEntry1.date);
        }
    };

    /**
     * Return True if passed LogDate Object is equal to the current LogDate Object.
     * Object are also detected as equal if date, LogType, author and log are the same.
     *
     * @return true if objects are identical
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
                type == otherLog.type &&
                author.compareTo(otherLog.author) == 0 &&
                log.compareTo(otherLog.log) == 0;
    }

    /**
     * Add a new Image to the logEntry.
     *
     * @param image to be added to the LogEntry
     */
    public void addLogImage(final Image image) {
        if (logImages == null) {
            logImages = new ArrayList<>();
        }
        logImages.add(image);
    }

    /**
     * @return the log images or an empty list, never <code>null</code>
     */
    public List<Image> getLogImages() {
        if (logImages == null) {
            return Collections.emptyList();
        }
        return logImages;
    }

    /**
     * Check if current LogType has Images.
     *
     * @return True if LogType has images
     */
    public boolean hasLogImages() {
        return CollectionUtils.isNotEmpty(logImages);
    }

    /**
     * Get the Images Titles separated by commas.
     * If no titles are present, display a 'default title'
     *
     * @return Images Titles separated by commas or 'default title'
     */
    public CharSequence getImageTitles() {
        final List<String> titles = new ArrayList<>(5);
        for (final Image image : getLogImages()) {
            if (StringUtils.isNotBlank(image.getTitle())) {
                titles.add(HtmlUtils.extractText(image.getTitle()));
            }
        }
        if (titles.isEmpty()) {
            titles.add(CgeoApplication.getInstance().getString(R.string.cache_log_image_default_title));
        }
        return StringUtils.join(titles, ", ");
    }

    /**
     * Get the log text to be displayed. Depending on the settings, color tags might be removed.
     *
     * @return log text
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
     * @return True if LogEntry is from current user
     */
    public boolean isOwn() {
        return author.equalsIgnoreCase(Settings.getUsername());
    }
}
