package cgeo.geocaching.models;

import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.OfflineMapUtils;

import android.net.Uri;

public class OfflineMap {

    private final String name;
    private final Uri uri;
    private final boolean isDir;
    private final long dateInfo;
    private final String sizeInfo;
    private String addInfo;

    public OfflineMap(final String name, final Uri uri, final boolean isDir, final String dateISO, final String sizeInfo) {
        this.name = OfflineMapUtils.getDisplayName(name);
        this.uri = uri;
        this.isDir = isDir;
        this.sizeInfo = sizeInfo;
        this.addInfo = "";
        this.dateInfo = CalendarUtils.parseYearMonthDay(dateISO);
    }

    public String getName() {
        return name;
    }

    public Uri getUri() {
        return uri;
    }

    public boolean getIsDir() {
        return isDir;
    }

    public String getDateInfoAsString() {
        return CalendarUtils.yearMonthDay(dateInfo);
    }

    public long getDateInfo() {
        return dateInfo;
    }

    public String getSizeInfo() {
        return sizeInfo;
    }

    public void setAddInfo(final String addInfo) {
        this.addInfo = addInfo;
    }

    public String getAddInfo() {
        return addInfo;
    }
}
