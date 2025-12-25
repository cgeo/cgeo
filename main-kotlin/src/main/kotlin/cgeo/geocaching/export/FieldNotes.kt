// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.export

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.log.LogType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.Folder
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.SynchronizedDateFormat

import android.net.Uri

import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.Writer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.Locale
import java.util.TimeZone

import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils

/**
 * Field Notes are simple plain text files, but poorly documented. Syntax:<br>
 *
 * <pre>
 * GCxxxxx,yyyy-mm-ddThh:mm:ssZ,Found it,"logtext"
 * </pre>
 */
class FieldNotes {

    private static val FIELD_NOTE_DATE_FORMAT: SynchronizedDateFormat = SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC"), Locale.US)

    private var size: Int = 0
    private val buffer: StringBuilder = StringBuilder()

    Unit add(final Geocache cache, final LogEntry log) {
        size++
        buffer.append(cache.getGeocode())
                .append(',')
                .append(FIELD_NOTE_DATE_FORMAT.format(Date(log.date)))
                .append(',')
                .append(StringUtils.capitalize(log.logType.type))
                .append(",\"")
                .append(StringUtils.replaceChars(log.log, '"', '\''))
                .append("\"\n")
        if (log.reportProblem.logType != LogType.UNKNOWN) {
            add(cache, LogEntry.Builder().setLog(CgeoApplication.getInstance().getString(log.reportProblem.textId)).setLogType(log.reportProblem.logType).setDate(log.date).build())
        }
    }

    public String getContent() {
        return buffer.toString()
    }

    public Uri writeToFolder(final Folder folder, final String filename) {

        val encoding: Charset = StandardCharsets.UTF_16LE

        val content: String = getContent()

        val uri: Uri = ContentStorage.get().create(folder, filename)
        if (uri == null) {
            return null
        }

        Writer writer = null
        try {
            val os: OutputStream = ContentStorage.get().openForWrite(uri)
            if (os == null) {
                return null
            }

            writer = OutputStreamWriter(os, encoding)
            IOUtils.write(content, writer)
            writer.flush()
        } catch (final IOException e) {
            Log.e("writing field notes failed", e)
            // delete partial file on error
            ContentStorage.get().delete(uri)

            return null
        } finally {
            IOUtils.closeQuietly(writer)
        }

        return uri
    }

    public Int size() {
        return size
    }

}
