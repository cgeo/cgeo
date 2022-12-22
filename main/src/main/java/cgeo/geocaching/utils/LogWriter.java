package cgeo.geocaching.utils;

import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;

import android.net.Uri;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class LogWriter {
    private PrintWriter logWriter = null;

    public LogWriter(final String prefix) {
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
            try {
                final Uri logWriterFile = ContentStorage.get().create(PersistableFolder.LOGFILES, FileNameCreator.LOGFILE, false);
                if (logWriterFile == null) {
                    Log.w("Could not create LogWriter-File");
                    return;
                }
                logWriter = new PrintWriter(new OutputStreamWriter(ContentStorage.get().openForWrite(logWriterFile)));
                log("begin logging to file " + logWriterFile);
            } catch (Exception e) {
                Log.w("Problem while creating LogWrilter", e);
                // ignore any error
            }
        }
    }

}
