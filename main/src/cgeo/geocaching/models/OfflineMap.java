package cgeo.geocaching.models;

import android.net.Uri;

import org.apache.commons.lang3.StringUtils;

public class OfflineMap {

    private final String name;
    private final Uri uri;
    private final boolean isDir;
    private final String dateInfo;
    private final String sizeInfo;

    public OfflineMap(final String name, final Uri uri, final boolean isDir, final String dateInfo, final String sizeInfo) {
        // capitalize first letter + every first after a "-"
        String tempName = StringUtils.upperCase(name.substring(0, 1)) + name.substring(1);
        int pos = name.indexOf("-");
        while (pos > 0) {
            tempName = tempName.substring(0, pos + 1) + StringUtils.upperCase(tempName.substring(pos + 1, pos + 2)) + tempName.substring(pos + 2);
            pos = name.indexOf("-", pos + 1);
        }

        this.name = tempName;
        this.uri = uri;
        this.isDir = isDir;
        this.dateInfo = dateInfo;
        this.sizeInfo = sizeInfo;
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

    public String getDateInfo() {
        return dateInfo;
    }

    public String getSizeInfo() {
        return sizeInfo;
    }
}
