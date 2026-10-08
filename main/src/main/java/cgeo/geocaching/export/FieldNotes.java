package cgeo.geocaching.export;

import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.LogUtils;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.Folder;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.SynchronizedDateFormat;

import android.net.Uri;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Field Notes are simple plain text files, but poorly documented. Syntax:<br>
 *
 * <pre>
 * GCxxxxx,yyyy-mm-ddThh:mm:ssZ,Found it,"logtext"
 * </pre>
 *
 * There is exactly ONE field notes object per export, and it is hybrid: it holds the offline logs
 * of all platforms (GC, OC, ...) at once. Never split it into separate objects or files per platform.
 * <ul>
 * <li>The export file is written once from this object, in the field notes format (geocaching.com
 * log type names) with the records of all platforms. It is shared with other apps and imported on
 * websites, and those consumers handle records of several platforms; a per-platform split would
 * break them.</li>
 * <li>Every upload target receives this same object and takes what concerns it: a connector which
 * uploads the file sends it as it is (the site ignores foreign records), a connector which uploads
 * through an API picks its own records via {@link #getContent(Predicate, Function)}.</li>
 * </ul>
 */
public class FieldNotes {

    private static final SynchronizedDateFormat FIELD_NOTE_DATE_FORMAT = new SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC"), Locale.US);

    private static final class Entry {
        final String geocode;
        final long date;
        final LogType logType;
        final String text;

        Entry(final String geocode, final long date, final LogType logType, final String text) {
            this.geocode = geocode;
            this.date = date;
            this.logType = logType;
            this.text = text;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    void add(final Geocache cache, final LogEntry log) {
        entries.add(new Entry(cache.getGeocode(), log.date, log.logType, StringUtils.replaceChars(LogUtils.trimForPublishing(log.log), '"', '\'')));
        if (log.reportProblem.logType != LogType.UNKNOWN) {
            add(cache, new LogEntry.Builder().setLog(LocalizationUtils.getString(log.reportProblem.textId)).setLogType(log.reportProblem.logType).setDate(log.date).build());
        }
    }

    /**
     * Content of the export file: all notes, with the geocaching.com log type names.
     */
    public String getContent() {
        return buildContent(geocode -> true, logType -> StringUtils.capitalize(logType.type), false);
    }

    /**
     * Content for one platform: only the notes whose geocode matches the filter, with the
     * platform's name for each log type. Notes whose log type has no name on the platform are left out.
     */
    public String getContent(final Predicate<String> geocodeFilter, final Function<LogType, String> logTypeName) {
        return buildContent(geocodeFilter, logTypeName, true);
    }

    private String buildContent(final Predicate<String> geocodeFilter, final Function<LogType, String> logTypeName, final boolean skipUnnamedTypes) {
        final StringBuilder buffer = new StringBuilder();
        for (final Entry entry : entries) {
            final String typeName = logTypeName.apply(entry.logType);
            if (!geocodeFilter.test(entry.geocode) || (skipUnnamedTypes && StringUtils.isBlank(typeName))) {
                continue;
            }
            buffer.append(entry.geocode)
                    .append(',')
                    .append(FIELD_NOTE_DATE_FORMAT.format(new Date(entry.date)))
                    .append(',')
                    .append(typeName)
                    .append(",\"")
                    .append(entry.text)
                    .append("\"\n");
        }
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
        return entries.size();
    }

}
