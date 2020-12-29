package cgeo.geocaching.utils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Instances of this class are meant to create filename.
 *
 * Static method {@link #forName(String, String)} can be used for constant names, while various predefined instances of this
 * class can be used to create unique filenames for certain file types (e.g. {@link #LOGFILE}).
 * */
public class FileNameCreator {

    public static final String MIME_TYPE_TEXT = "text/plain";
    public static final String MIME_TYPE_BINARY = "application/octet-stream"; //or maybe application/x-binary?

    public static final FileNameCreator DEFAULT = new FileNameCreator(null, "file", "dat");
    public static final FileNameCreator DEFAULT_TEXT = new FileNameCreator(MIME_TYPE_TEXT, "file", "txt");
    public static final FileNameCreator DEFAULT_BINARY = new FileNameCreator(MIME_TYPE_BINARY, "file", "bin");

    public static final FileNameCreator OFFLINE_MAPS = new FileNameCreator(null, "mapfile", "map");
    public static final FileNameCreator LOGFILE = new FileNameCreator(MIME_TYPE_TEXT, "logcat", "txt");
    public static final FileNameCreator MEMORY_DUMP = new FileNameCreator(null, "cgeo_dump", "hprof");

    private final AtomicInteger fileNameCounter = new AtomicInteger(1);

    private final String fixedName;
    private final String mimeType;
    private final String praefix;
    private final String suffix;

    private FileNameCreator(final String mimeType, final String praefix, final String suffix) {
        this(null, mimeType, praefix, suffix);
    }

    private FileNameCreator(final String fixedName, final String mimeType, final String praefix, final String suffix) {
        this.fixedName = fixedName;
        this.mimeType = mimeType;
        this.praefix = praefix;
        this.suffix = suffix;
    }

    /** Creates instance of FileNameCreator which returns a constant name always (non-unique!) */
    public static FileNameCreator forName(final String name) {
        return new FileNameCreator(name, null, null, null);
    }

    /** Creates instance of FileNameCreator which returns a constant name always (non-unique!) */
    public static FileNameCreator forName(final String name, final String mimeType) {
        return new FileNameCreator(name, mimeType, null, null);
    }

    public String getMimeType() {
        return mimeType;
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
