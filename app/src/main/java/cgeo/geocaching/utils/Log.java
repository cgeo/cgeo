package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.functions.Func1;

import android.net.Uri;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import static java.lang.Boolean.TRUE;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public final class Log {

    private static final String TAG = "cgeo";

    public enum LogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR, NONE }

    /**
     * Name of File containing Log properties which will be searched for in logfiles-directory
     */
    private static final String LOGPROPERTY_FILENAME = "log-properties.txt";
    /**
     * Minimum log level. Value should be one of {@link LogLevel} in textual form
     */
    public static final String PROP_MIN_LOG_LEVEL = "logging.minlevel";
    /**
     * Minimum log level to add callerinfo to log message. Value should be one of {@link LogLevel} in textual form
     */
    public static final String PROP_MIN_CALLERINFO_LEVEL = "logging.mincallerinfolevel";
    /**
     * max stack trace depth to log when caller info is logged
     */
    public static final String PROP_CALLERINFO_MAXDEPTH = "logging.callerinfomaxdepth";
    /**
     * Whether to throw an exception when an error is logged. Value should be true or false
     */
    public static final String PROP_THROW_ON_ERROR_LOG = "logging.throwonerror";
    /**
     * Logfile to log to
     */
    public static final String PROP_LOGFILE = "logging.logfile";

    private static final DateFormat LOGFILE_ENTRY_FORMAT = new SimpleDateFormat("yy-MM-dd HH:mm:ss.SSS", Locale.US);

    /**
     * If the debug flag is set then minimum log level is debug AND an exception is thrown on error logging
     * The debug flag is cached here so that we don't need to access the settings every time we have to evaluate it.
     */
    private static boolean isDebug = true;

    private static LogLevel minLogLevel = LogLevel.WARN;
    private static boolean logThrowExceptionOnError = false;
    private static LogLevel minLogAddCallerInfo = LogLevel.NONE;
    private static int addCallerInfoMaxDepth = 8;
    private static PrintWriter logFileWriter = null;

    private static final boolean[] SETTING_DO_LOGGING = new boolean[LogLevel.values().length];
    private static boolean settingThrowExceptionOnError = true;
    private static final boolean[] SETTING_ADD_CLASSINFO = new boolean[LogLevel.values().length];

    static {
        InputStream propFile = null;
        try {
            String logfileFolder = "(unknown, no cgeo app)";
            if (CgeoApplication.getInstance() != null) {
                propFile = ContentStorage.get().openForRead(PersistableFolder.LOGFILES.getFolder(), LOGPROPERTY_FILENAME);
                logfileFolder = String.valueOf(PersistableFolder.LOGFILES.getFolder());
            }
            if (propFile == null) {
                adjustSettings();
                android.util.Log.i(TAG, "[Log] No logging config file '" + LOGPROPERTY_FILENAME + "' found at " + logfileFolder + ", using defaults");
            } else {
                android.util.Log.i(TAG, "[Log] Logging config file '" + LOGPROPERTY_FILENAME + "'found at " + logfileFolder + ", try to apply");
                final Properties logProps = new Properties();
                logProps.load(new InputStreamReader(propFile));
                setProperties(logProps);
            }
        } catch (Exception ex) {
            //whatever happens in Log initializer, it is NOT allowed to make Log unusable!
            android.util.Log.e(TAG, "[Log] Failed to set up Logging", ex);
        } finally {
            IOUtils.closeQuietly(propFile);
        }
    }

    private Log() {
        //utility class
    }

    public static boolean isDebug() {
        return isDebug;
    }

    public static boolean isEnabled(final LogLevel level) {
        return SETTING_DO_LOGGING[level.ordinal()];
    }

    /**
     * Save a copy of the debug flag from the settings for performance reasons.
     */
    public static void setDebug(final boolean isDebug) {
        if (Log.isDebug() != isDebug) {
            Log.isDebug = isDebug;
            adjustSettings();
        }
    }

    public static void setProperties(final Properties logProps) {
        if (logProps != null) {
            LogLevel level = readLogLevel(logProps, PROP_MIN_LOG_LEVEL);
            if (level != null) {
                minLogLevel = level;
            }
            level = readLogLevel(logProps, PROP_MIN_CALLERINFO_LEVEL);
            if (level != null) {
                minLogAddCallerInfo = level;
            }
            if (logProps.containsKey(PROP_CALLERINFO_MAXDEPTH)) {
                try {
                    addCallerInfoMaxDepth = Integer.parseInt(logProps.getProperty(PROP_CALLERINFO_MAXDEPTH));
                } catch (NumberFormatException nfe) {
                    //no valid maxDepth in prop file, ignore this
                }
            }
            logThrowExceptionOnError = "true".equalsIgnoreCase(logProps.getProperty(PROP_THROW_ON_ERROR_LOG));
            if (logProps.containsKey(PROP_LOGFILE)) {
                final String logfileNamePraefix = logProps.getProperty(PROP_LOGFILE).trim();
                if (StringUtils.isNotBlank(logfileNamePraefix)) {
                    final String logFileName = FileNameCreator.LOGFILE_SELF_WRITTEN.createName(logfileNamePraefix);
                    Uri logFileUri = null;
                    try {
                        logFileUri = ContentStorage.get().create(PersistableFolder.LOGFILES.getFolder(), logFileName);
                        final OutputStream logFileStream = ContentStorage.get().openForWrite(logFileUri);
                        logFileWriter = new PrintWriter(Objects.requireNonNull(logFileStream));
                        android.util.Log.i(TAG, "[Log] opened logfile '" + logFileName + "' at '" + logFileUri + "'");
                    } catch (Exception ioe) {
                        //could not open logfile
                        android.util.Log.e(TAG, "[Log] Failed to open '" + logFileName + "' at '" + logFileUri + "'", ioe);
                    }
                }
            }
            adjustSettings();
        }
    }

    public static LogLevel readLogLevel(final Properties logProps, final String propName) {
        if (!logProps.containsKey(propName)) {
            return null;
        }
        try {
            return LogLevel.valueOf(logProps.getProperty(propName).toUpperCase(Locale.US));
        } catch (Exception e) {
            return null;
        }
    }

    private static void adjustSettings() {
        final LogLevel minDoLogging = isDebug() && minLogLevel.ordinal() > LogLevel.DEBUG.ordinal() ? LogLevel.DEBUG : minLogLevel;
        final LogLevel minAddCallerInfo = isDebug() && minLogAddCallerInfo.ordinal() > LogLevel.DEBUG.ordinal() ? LogLevel.DEBUG : minLogAddCallerInfo;
        setLevel(SETTING_DO_LOGGING, minDoLogging);
        setLevel(SETTING_ADD_CLASSINFO, minAddCallerInfo);
        settingThrowExceptionOnError = logThrowExceptionOnError || isDebug;
        android.util.Log.i(TAG, "[Log] Logging set: minLevel=" + minDoLogging + ", minAddCallerInfo=" + minAddCallerInfo +
                ", addCallerInfoMaxDepth=" + addCallerInfoMaxDepth + ", throwOnError=" + logThrowExceptionOnError);
    }

    private static void setLevel(final boolean[] settings, final LogLevel level) {
        for (int i = 0; i < settings.length; i++) {
            settings[i] = level.ordinal() <= i;
        }
    }

    private static String adjustMessage(final String msg, final LogLevel level) {
        //thread
        final String threadName = Thread.currentThread().getName();
        final String shortName = threadName.startsWith("OkHttp") ? "OkHttp" : threadName;

        //callerinfo
        if (SETTING_ADD_CLASSINFO[level.ordinal()]) {
            return "[" + shortName + "] " + msg + " {" + getCallerInfo(addCallerInfoMaxDepth) + "}";
        }
        return "[" + shortName + "] " + msg;
    }

    public static void v(final String msg) {
        if (SETTING_DO_LOGGING[LogLevel.VERBOSE.ordinal()]) {
            final String message = adjustMessage(msg, LogLevel.VERBOSE);
            android.util.Log.v(TAG, message);
            if (logFileWriter != null) {
                logToFile("V", message, null);
            }
        }
    }

    public static void v(final String msg, final Throwable t) {
        if (SETTING_DO_LOGGING[LogLevel.VERBOSE.ordinal()]) {
            final String message = adjustMessage(msg, LogLevel.VERBOSE);
            android.util.Log.v(TAG, message, t);
            if (logFileWriter != null) {
                logToFile("V", message, t);
            }
        }
    }

    public static void d(final String msg) {
        if (SETTING_DO_LOGGING[LogLevel.DEBUG.ordinal()]) {
            final String message = adjustMessage(msg, LogLevel.DEBUG);
            android.util.Log.d(TAG, message);
            if (logFileWriter != null) {
                logToFile("D", message, null);
            }
        }
    }

    public static void d(final String msg, final Throwable t) {
        if (SETTING_DO_LOGGING[LogLevel.DEBUG.ordinal()]) {
            final String message = adjustMessage(msg, LogLevel.DEBUG);
            android.util.Log.d(TAG, message, t);
            if (logFileWriter != null) {
                logToFile("D", message, t);
            }
        }
    }

    public static void log(final LogLevel level, final String msg) {
        if (SETTING_DO_LOGGING[level.ordinal()]) {
            switch (level) {
                case ERROR:
                    e(msg);
                    break;
                case WARN:
                    w(msg);
                    break;
                case INFO:
                    i(msg);
                    break;
                case DEBUG:
                    d(msg);
                    break;
                case VERBOSE:
                default:
                    v(msg);
                    break;
            }
        }
    }

    public static void log(final LogLevel level, final String msg, final Throwable thr) {
        if (SETTING_DO_LOGGING[level.ordinal()]) {
            switch (level) {
                case ERROR:
                    e(msg, thr);
                    break;
                case WARN:
                    w(msg, thr);
                    break;
                case INFO:
                    i(msg, thr);
                    break;
                case DEBUG:
                    d(msg, thr);
                    break;
                case VERBOSE:
                default:
                    v(msg, thr);
                    break;
            }
        }
    }

    /**
     * Use this to log a FORCED message on info level. This message will be logged
     * regardless of the log level set, so use with care!
     */
    public static void iForce(final String msg) {
        final String message = adjustMessage(msg, LogLevel.INFO);
        android.util.Log.i(TAG, message);
        if (logFileWriter != null) {
            logToFile("I", message, null);
        }
    }

    public static void i(final String msg) {
        if (SETTING_DO_LOGGING[LogLevel.INFO.ordinal()]) {
            final String message = adjustMessage(msg, LogLevel.INFO);
            android.util.Log.i(TAG, message);
            if (logFileWriter != null) {
                logToFile("I", message, null);
            }
        }
    }

    public static void i(final String msg, final Throwable t) {
        if (SETTING_DO_LOGGING[LogLevel.INFO.ordinal()]) {
            final String message = adjustMessage(msg, LogLevel.INFO);
            android.util.Log.i(TAG, message, t);
            if (logFileWriter != null) {
                logToFile("I", message, t);
            }
        }
    }

    public static void w(final String msg) {
        if (SETTING_DO_LOGGING[LogLevel.WARN.ordinal()]) {
            final String message = adjustMessage(msg, LogLevel.WARN);
            android.util.Log.w(TAG, message);
            if (logFileWriter != null) {
                logToFile("W", message, null);
            }
        }
    }

    public static void w(final String msg, final Throwable t) {
        if (SETTING_DO_LOGGING[LogLevel.WARN.ordinal()]) {
            final String message = adjustMessage(msg, LogLevel.WARN);
            android.util.Log.w(TAG, message, t);
            if (logFileWriter != null) {
                logToFile("W", message, t);
            }
        }
    }

    public static void e(final String msg) {
        if (SETTING_DO_LOGGING[LogLevel.ERROR.ordinal()]) {
            final String message = adjustMessage(msg, LogLevel.ERROR);
            android.util.Log.e(TAG, message);
            if (logFileWriter != null) {
                logToFile("E", message, null);
            }
            if (settingThrowExceptionOnError) {
                throw new RuntimeException("Aborting on Log.e()");
            }
        }
    }

    public static void e(final String msg, final Throwable t) {
        if (SETTING_DO_LOGGING[LogLevel.ERROR.ordinal()]) {
            final String message = adjustMessage(msg, LogLevel.ERROR);
            android.util.Log.e(TAG, message, t);
            if (logFileWriter != null) {
                logToFile("E", message, t);
            }
            if (settingThrowExceptionOnError) {
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
                throw new RuntimeException("Aborting on Log.e()", t);
            }
        }
    }

    /**
     * Returns compact info about caller of Log method. Note: this method is considerably slow
     * and shall be used with care (only when explicitely turned on)
     */
    private static String getCallerInfo(final int maxDepth) {
        final String logClassName = Log.class.getName();
        final String contextLoggerClassname = ContextLogger.class.getName();

        return stackTraceToShortString(new RuntimeException().getStackTrace(), maxDepth, st ->
                !st.getClassName().equals(logClassName) && !st.getClassName().equals(contextLoggerClassname));
    }

    public static String stackTraceToShortString(final StackTraceElement[] stackTraceElements, final int maxDepth, final Func1<StackTraceElement, Boolean> filter) {
        final StringBuilder sb = new StringBuilder();
        int cnt = 0;
        for (final StackTraceElement st : stackTraceElements) {
            if (filter == null || TRUE.equals(filter.call(st))) {
                if (sb.length() > 0) {
                    sb.append("/");
                }
                sb.append(stackTraceElementToString(st));
                if (maxDepth > 0 && ++cnt >= maxDepth) {
                    break;
                }
            }
        }
        return sb.length() == 0 ? "<none>" : sb.toString();
    }

    private static String stackTraceElementToString(final StackTraceElement st) {
        String shortClassName = st.getClassName();
        final int idx = shortClassName.lastIndexOf(".");
        if (idx >= 0) {
            shortClassName = shortClassName.substring(idx + 1);
        }
        return shortClassName + "." + st.getMethodName() + ":" + st.getLineNumber();
    }

    private static void logToFile(final String level, final String message, final Throwable t) {
        if (logFileWriter != null) {

            logFileWriter.write(LOGFILE_ENTRY_FORMAT.format(new Date()) + " (" + level + ") " + message + "\n");
            if (t != null) {
                t.printStackTrace(logFileWriter);
            }
            logFileWriter.flush();
        }
    }

}
