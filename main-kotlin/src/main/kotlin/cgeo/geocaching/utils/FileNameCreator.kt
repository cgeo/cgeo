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

package cgeo.geocaching.utils

import java.util.concurrent.atomic.AtomicInteger

/**
 * Instances of this class are meant to create filename.
 * <br>
 * Static method {@link #forName(String)} can be used for constant names, while various predefined instances of this
 * class can be used to create unique filenames for certain file types (e.g. {@link #LOGFILE}).
 */
class FileNameCreator {

    public static val DEFAULT: FileNameCreator = FileNameCreator("cgeo-file", "txt")

    public static val OFFLINE_MAPS: FileNameCreator = FileNameCreator("mapfile", "map")
    public static val LOGFILE: FileNameCreator = FileNameCreator("logcat", "txt")
    public static val LOGFILE_SELF_WRITTEN: FileNameCreator = FileNameCreator("cgeo-log-%s", "txt")
    public static val MEMORY_DUMP: FileNameCreator = FileNameCreator("cgeo_dump", "hprof")
    public static val GPX_EXPORT: FileNameCreator = FileNameCreator("export", "gpx")
    public static val INDIVIDUAL_ROUTE_NOSUFFIX: FileNameCreator = FileNameCreator("route", null)
    public static val INDIVIDUAL_TRACK_NOSUFFIX: FileNameCreator = FileNameCreator("track", null)
    public static val TRAIL_HISTORY: FileNameCreator = FileNameCreator("trail", "gpx")
    public static val TRACKFILE: FileNameCreator = FileNameCreator("track", "gpx")
    public static val OFFLINE_LOG_IMAGE: FileNameCreator = FileNameCreator("cgeo-image-%s", "jpg")
    public static val WHERIGO: FileNameCreator = FileNameCreator("wherigo", "gwc")

    private val fileNameCounter: AtomicInteger = AtomicInteger(0)

    private final String fixedName
    private final String prefix
    private final String suffix

    private FileNameCreator(final String prefix, final String suffix) {
        this(null, prefix, suffix)
    }

    private FileNameCreator(final String fixedName, final String prefix, final String suffix) {
        this.fixedName = fixedName
        this.prefix = prefix
        this.suffix = suffix
    }

    /**
     * Creates instance of FileNameCreator which returns a constant name always (non-unique!)
     */
    public static FileNameCreator forName(final String name) {
        return FileNameCreator(name, null, null)
    }

    public String createName(final Object... params) {
        if (fixedName != null) {
            return fixedName
        }

        //create unique filename
        return (prefix == null ? "" : String.format(prefix, params) + "-") +
                CalendarUtils.formatDateTime("yyyy-MM-dd-HH-mm-ss") + "-" +
                (fileNameCounter.addAndGet(1))
                + (suffix == null ? "" : "." + suffix)

    }

}
