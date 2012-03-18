package cgeo.geocaching;

import cgeo.geocaching.enumerations.LogType;

import java.util.List;

public class cgLog {
    public int id = 0;
    public LogType type = LogType.LOG_NOTE; // note
    public String author = "";
    public String log = "";
    public long date = 0;
    public int found = -1;
    /** Friend's logentry */
    public boolean friend = false;
    public List<cgImage> logImages = null;
    public String cacheName = ""; // used for trackables
    public String cacheGuid = ""; // used for trackables

    @Override
    public int hashCode() {
        return (int) date * type.hashCode() * author.hashCode() * log.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof cgLog)) {
            return false;
        }
        final cgLog otherLog = (cgLog) obj;
        return date == otherLog.date &&
                type == otherLog.type &&
                author.compareTo(otherLog.author) == 0 &&
                log.compareTo(otherLog.log) == 0 ? true : false;
    }
}
