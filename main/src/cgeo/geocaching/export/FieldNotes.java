package cgeo.geocaching.export;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogEntry;
import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.SynchronizedDateFormat;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Field Notes are simple plain text files, but poorly documented. Syntax:<br>
 * <code>GCxxxxx,yyyy-mm-ddThh:mm:ssZ,Found it,"logtext"</code>
 */
class FieldNotes {

    private static final SynchronizedDateFormat FIELD_NOTE_DATE_FORMAT = new SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC"), Locale.US);

    private int size = 0;
    private final StringBuilder buffer = new StringBuilder();

    void add(final Geocache cache, final LogEntry log) {
        size++;
        buffer.append(cache.getGeocode())
                .append(',')
                .append(FIELD_NOTE_DATE_FORMAT.format(new Date(log.date)))
                .append(',')
                .append(StringUtils.capitalize(log.type.type))
                .append(",\"")
                .append(StringUtils.replaceChars(log.log, '"', '\''))
                .append("\"\n");
    }

    public String getContent() {
        return buffer.toString();
    }

    File writeToDirectory(final File exportLocation) {
        if (!LocalStorage.isExternalStorageAvailable()) {
            return null;
        }

        FileUtils.mkdirs(exportLocation);

        final SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        final File exportFile = new File(exportLocation.toString() + '/' + fileNameDateFormat.format(new Date()) + ".txt");

        if (!FileUtils.writeFileUTF16(exportFile, getContent())) {
            return null;
        }

        return exportFile;
    }

    public int size() {
        return size;
    }

}
