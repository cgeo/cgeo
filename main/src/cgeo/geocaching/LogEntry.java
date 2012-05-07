package cgeo.geocaching;

import cgeo.geocaching.enumerations.LogType;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public final class LogEntry {

    public int id = 0;
    public LogType type = LogType.NOTE; // note
    public String author = "";
    public String log = "";
    public long date = 0;
    public int found = -1;
    /** Friend's log entry */
    public boolean friend = false;
    private List<cgImage> logImages = null;
    public String cacheName = ""; // used for trackables
    public String cacheGuid = ""; // used for trackables

    public LogEntry(final Calendar date, final LogType type, final String text) {
        this(Settings.getUsername(), date.getTimeInMillis(), type, text);
    }

    public LogEntry(final long dateInMilliSeconds, final LogType type, final String text) {
        this(Settings.getUsername(), dateInMilliSeconds, type, text);
    }

    public LogEntry(final String author, long dateInMilliSeconds, final LogType type, final String text) {
        this.author = author;
        this.date = dateInMilliSeconds;
        this.type = type;
        this.log = text;
    }

    @Override
    public int hashCode() {
        return (int) date * type.hashCode() * author.hashCode() * log.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
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

    public void addLogImage(final cgImage image) {
        if (logImages == null) {
            logImages = new ArrayList<cgImage>();
        }
        logImages.add(image);
    }

    /**
     * @return the log images or an empty list, never <code>null</code>
     */
    public List<cgImage> getLogImages() {
        if (logImages == null) {
            return Collections.emptyList();
        }
        return logImages;
    }

    public boolean hasLogImages() {
        return CollectionUtils.isNotEmpty(logImages);
    }

    public CharSequence getImageTitles() {
        final List<String> titles = new ArrayList<String>(5);
        for (cgImage image : getLogImages()) {
            if (StringUtils.isNotBlank(image.getTitle())) {
                titles.add(image.getTitle());
            }
        }
        if (titles.isEmpty()) {
            titles.add(cgeoapplication.getInstance().getString(R.string.cache_log_image_default_title));
        }
        return StringUtils.join(titles, ", ");
    }

    public int daysSinceLog() {
        final Calendar logDate = Calendar.getInstance();
        logDate.setTimeInMillis(date);
        logDate.set(Calendar.SECOND, 0);
        logDate.set(Calendar.MINUTE, 0);
        logDate.set(Calendar.HOUR, 0);
        final Calendar today = Calendar.getInstance();
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.HOUR, 0);
        return (int) Math.round((today.getTimeInMillis() - logDate.getTimeInMillis()) / 86400000d);
    }
}
