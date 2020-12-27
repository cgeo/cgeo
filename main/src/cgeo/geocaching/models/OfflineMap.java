package cgeo.geocaching.models;

import android.annotation.SuppressLint;
import android.net.Uri;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

public class OfflineMap {

    private static final String datePattern = "yyyy-MM-dd";

    private final String name;
    private final Uri uri;
    private final boolean isDir;
    private final long dateInfo;
    private final String sizeInfo;

    public OfflineMap(final String name, final Uri uri, final boolean isDir, final String dateISO, final String sizeInfo) {
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
        this.sizeInfo = sizeInfo;

        // parse date info - dateISO has format yyyy-MM-dd
        long result;
        try {
            @SuppressLint("SimpleDateFormat") final SimpleDateFormat pattern = new SimpleDateFormat(datePattern);
            final Date date = pattern.parse(dateISO);
            result = date != null ? date.getTime() : 0;
        } catch (ParseException | NullPointerException e) {
            result = 0;
        }
        this.dateInfo = result;
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
        @SuppressLint("SimpleDateFormat") final SimpleDateFormat pattern = new SimpleDateFormat(datePattern);
        return pattern.format(dateInfo);
    }

    public long getDateInfo() {
        return dateInfo;
    }

    public String getSizeInfo() {
        return sizeInfo;
    }

}
