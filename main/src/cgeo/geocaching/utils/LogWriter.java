package cgeo.geocaching.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class LogWriter {
    private PrintWriter logWriter = null;
    private final String prefix;

    public LogWriter(final String prefix) {
        this.prefix = prefix;
        checkLogfile();
    }

    public void log(final String info) {
        checkLogfile();
        if (null != logWriter) {
            logWriter.println("--------------------------------------------------------------------");
            logWriter.println(CalendarUtils.formatDateTime("yyyy-MM-dd HH:mm:ss.SSS"));
            logWriter.println(info);
        }
    }

    public void d(final String info) {
        Log.d(info);
        log(info);
    }

    public void e(final String info) {
        Log.e(info);
        log(info);
    }

    public void i(final String info) {
        Log.i(info);
        log(info);
    }

    public void v(final String info) {
        Log.v(info);
        log(info);
    }

    public void w(final String info) {
        Log.w(info);
        log(info);
    }

    public void close() {
        if (null != logWriter) {
            log("end of logging");
            logWriter.println("--------------------------------------------------------------------");
            logWriter.close();
            logWriter = null;
        }
    }

    private void checkLogfile() {
        if (null == logWriter && Log.isDebug()) {
            final File logfile = FileUtils.getUniqueNamedLogfile(prefix, "txt");
            try {
                logWriter = new PrintWriter(logfile);
                log("begin logging to file " + logfile.getPath());
            } catch (FileNotFoundException e) {
                // ignore error
            }
        }
    }

}
