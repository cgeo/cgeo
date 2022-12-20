package cgeo.geocaching.utils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Instances of this class are meant to create filename.
 *
 * Static method {@link #forName(String, String)} can be used for constant names, while various predefined instances of this
 * class can be used to create unique filenames for certain file types (e.g. {@link #LOGFILE}).
 */
public class FileNameCreator {

    public static final FileNameCreator DEFAULT = new FileNameCreator("cgeo-file", "txt");

    public static final FileNameCreator OFFLINE_MAPS = new FileNameCreator("mapfile", "map");
    public static final FileNameCreator LOGFILE = new FileNameCreator("logcat", "txt");
    public static final FileNameCreator LOGFILE_SELF_WRITTEN = new FileNameCreator("cgeo-log-%s", "txt");
    public static final FileNameCreator MEMORY_DUMP = new FileNameCreator("cgeo_dump", "hprof");
    public static final FileNameCreator GPX_EXPORT = new FileNameCreator("export", "gpx");
    public static final FileNameCreator INDIVIDUAL_ROUTE_NOSUFFIX = new FileNameCreator("route", null);
    public static final FileNameCreator INDIVIDUAL_TRACK_NOSUFFIX = new FileNameCreator("track", null);
    public static final FileNameCreator TRAIL_HISTORY = new FileNameCreator("trail", "gpx");
    public static final FileNameCreator TRACKFILE = new FileNameCreator("track", "gpx");
    public static final FileNameCreator OFFLINE_LOG_IMAGE = new FileNameCreator("cgeo-image-%s", "jpg");

    private final AtomicInteger fileNameCounter = new AtomicInteger(1);

    private final String fixedName;
    private final String prefix;
    private final String suffix;

    private FileNameCreator(final String prefix, final String suffix) {
        this(null, prefix, suffix);
    }

    private FileNameCreator(final String fixedName, final String prefix, final String suffix) {
        this.fixedName = fixedName;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    /**
     * Creates instance of FileNameCreator which returns a constant name always (non-unique!)
     */
    public static FileNameCreator forName(final String name) {
        return new FileNameCreator(name, null, null);
    }

    public String createName(final Object... params) {
        if (fixedName != null) {
            return fixedName;
        }

        //create new unique filename
        return (prefix == null ? "" : String.format(prefix, params) + "-") +
                CalendarUtils.formatDateTime("yyyy-MM-dd-HH-mm-ss") + "-" +
                (fileNameCounter.addAndGet(1))
                + (suffix == null ? "" : "." + suffix);

    }

}
