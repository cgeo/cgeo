package cgeo.geocaching.export;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.SynchronizedDateFormat;

import java.io.File;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;

/**
 * Field Notes are simple plain text files, but poorly documented. Syntax:<br>
 *
 * <pre>
 * GCxxxxx,yyyy-mm-ddThh:mm:ssZ,Found it,"logtext"
 * </pre>
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
                .append(StringUtils.capitalize(log.getType().type))
                .append(",\"")
                .append(StringUtils.replaceChars(log.log, '"', '\''))
                .append("\"\n");
        if (log.reportProblem.logType != LogType.UNKNOWN) {
            add(cache, new LogEntry.Builder().setLog(CgeoApplication.getInstance().getString(log.reportProblem.textId)).setLogType(log.reportProblem.logType).setDate(log.date).build());
        }
    }

    public String getContent() {
        return buffer.toString();
    }

    File writeToDirectory(final File exportLocation, final String fileName) {
        FileUtils.mkdirs(exportLocation);

        final File exportFile = new File(exportLocation.toString() + '/' + fileName);

        if (!FileUtils.writeFileUTF16(exportFile, getContent())) {
            return null;
        }

        return exportFile;
    }

    public int size() {
        return size;
    }

}
