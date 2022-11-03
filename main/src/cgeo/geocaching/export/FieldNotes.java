package cgeo.geocaching.export;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.Folder;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.SynchronizedDateFormat;

import android.net.Uri;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
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
                .append(StringUtils.capitalize(log.logType.type))
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

    public Uri writeToFolder(final Folder folder, final String filename) {

        final Charset encoding = StandardCharsets.UTF_16LE;

        final String content = getContent();

        final Uri uri = ContentStorage.get().create(folder, filename);
        if (uri == null) {
            return null;
        }

        Writer writer = null;
        try {
            final OutputStream os = ContentStorage.get().openForWrite(uri);
            if (os == null) {
                return null;
            }

            writer = new OutputStreamWriter(os, encoding);
            IOUtils.write(content, writer);
            writer.flush();
        } catch (final IOException e) {
            Log.e("writing field notes failed", e);
            // delete partial file on error
            ContentStorage.get().delete(uri);

            return null;
        } finally {
            IOUtils.closeQuietly(writer);
        }

        return uri;
    }

    public int size() {
        return size;
    }

}
