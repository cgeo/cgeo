package cgeo.geocaching.utils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Instances of this class are meant to create filename.
 *
 * Static method {@link #forName(String, String)} can be used for constant names, while various predefined instances of this
 * class can be used to create unique filenames for certain file types (e.g. {@link #LOGFILE}).
 * */
public class FileNameCreator {

    public static final FileNameCreator DEFAULT = new FileNameCreator("file", "dat");
    public static final FileNameCreator DEFAULT_TEXT = new FileNameCreator("file", "txt");
    public static final FileNameCreator DEFAULT_BINARY = new FileNameCreator("file", "bin");

    public static final FileNameCreator OFFLINE_MAPS = new FileNameCreator("mapfile", "map");
    public static final FileNameCreator LOGFILE = new FileNameCreator("logcat", "txt");
    public static final FileNameCreator MEMORY_DUMP = new FileNameCreator("cgeo_dump", "hprof");

    private final AtomicInteger fileNameCounter = new AtomicInteger(1);

    private final String fixedName;
    private final String praefix;
    private final String suffix;

    private FileNameCreator(final String praefix, final String suffix) {
        this(null, praefix, suffix);
    }

    private FileNameCreator(final String fixedName, final String praefix, final String suffix) {
        this.fixedName = fixedName;
        this.praefix = praefix;
        this.suffix = suffix;
    }

    /** Creates instance of FileNameCreator which returns a constant name always (non-unique!) */
    public static FileNameCreator forName(final String name) {
        return new FileNameCreator(name, null, null);
    }

    public String createName() {
        if (fixedName != null) {
            return fixedName;
        }

        //create new unique filename
        return (praefix == null ? "file" : praefix) + "_" +
            CalendarUtils.formatDateTime("yyyy-MM-dd_HH-mm-ss") + "-" +
            (fileNameCounter.addAndGet(1))
            + "." + (suffix == null ? "dat" : suffix);

    }

}
