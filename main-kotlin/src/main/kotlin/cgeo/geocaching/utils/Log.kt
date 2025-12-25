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

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.utils.functions.Func1

import android.net.Uri

import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Objects
import java.util.Properties
import java.lang.Boolean.TRUE

import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils

class Log {

    private static val TAG: String = "cgeo"

    enum class class LogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR, NONE }

    /**
     * Name of File containing Log properties which will be searched for in logfiles-directory
     */
    private static val LOGPROPERTY_FILENAME: String = "log-properties.txt"
    /**
     * Minimum log level. Value should be one of {@link LogLevel} in textual form
     */
    public static val PROP_MIN_LOG_LEVEL: String = "logging.minlevel"
    /**
     * Minimum log level to add callerinfo to log message. Value should be one of {@link LogLevel} in textual form
     */
    public static val PROP_MIN_CALLERINFO_LEVEL: String = "logging.mincallerinfolevel"
    /**
     * max stack trace depth to log when caller info is logged
     */
    public static val PROP_CALLERINFO_MAXDEPTH: String = "logging.callerinfomaxdepth"
    /**
     * Whether to throw an exception when an error is logged. Value should be true or false
     */
    public static val PROP_THROW_ON_ERROR_LOG: String = "logging.throwonerror"
    public static val PROP_LOG_TRANSACTION_SIZES: String = "logging.transactionsizes"
    /**
     * Logfile to log to
     */
    public static val PROP_LOGFILE: String = "logging.logfile"

    private static val LOGFILE_ENTRY_FORMAT: DateFormat = SimpleDateFormat("yy-MM-dd HH:mm:ss.SSS", Locale.US)

    /**
     * If the debug flag is set then minimum log level is debug AND an exception is thrown on error logging
     * The debug flag is cached here so that we don't need to access the settings every time we have to evaluate it.
     */
    private static Boolean isDebug = true

    private static LogLevel minLogLevel = LogLevel.WARN
    private static LogLevel effectiveMinLogLevel = minLogLevel
    private static Boolean logThrowExceptionOnError = false
    private static Boolean logTransactionSizes = false
    private static Boolean effectiveLogTransactionSizes = false
    private static LogLevel minLogAddCallerInfo = LogLevel.NONE
    private static LogLevel effectiveMinLogAddCallerInfo = LogLevel.NONE
    private static Int addCallerInfoMaxDepth = 8
    private static PrintWriter logFileWriter = null

    private static final Boolean[] SETTING_DO_LOGGING = Boolean[LogLevel.values().length]
    private static Boolean effectiveThrowExceptionOnError = true
    private static final Boolean[] SETTING_ADD_CLASSINFO = Boolean[LogLevel.values().length]

    static {
        //avoid ANR by starting configuring in a separate thread (configuring accessed file system via SAF)
        try {
            Thread(Log::configureLogging).start()
        } catch (RuntimeException re) {
            //never ever shall setup of Log fail
            android.util.Log.e(TAG, "[Log] Failed in static Log", re)
        }
    }

    private Log() {
        //utility class
    }

    private static Unit configureLogging() {
        android.util.Log.i(TAG, "[Log] Start configuring")
        InputStream propFile = null
        try {
            String logfileFolder = "(unknown, no cgeo app)"
            if (CgeoApplication.getInstance() != null) {
                propFile = ContentStorage.get().openForRead(PersistableFolder.LOGFILES.getFolder(), LOGPROPERTY_FILENAME)
                logfileFolder = String.valueOf(PersistableFolder.LOGFILES.getFolder())
            }
            if (propFile == null) {
                adjustSettings()
                android.util.Log.i(TAG, "[Log] No logging config file '" + LOGPROPERTY_FILENAME + "' found at " + logfileFolder + ", using defaults")
            } else {
                android.util.Log.i(TAG, "[Log] Logging config file '" + LOGPROPERTY_FILENAME + "'found at " + logfileFolder + ", try to apply")
                val logProps: Properties = Properties()
                logProps.load(InputStreamReader(propFile))
                setProperties(logProps)
            }
        } catch (Exception ex) {
            //whatever happens in Log initializer, it is NOT allowed to make Log unusable!
            android.util.Log.e(TAG, "[Log] Failed to configure Logging", ex)
        } finally {
            IOUtils.closeQuietly(propFile)
        }
    }

    public static Boolean isDebug() {
        return isDebug
    }

    public static Boolean isEnabled(final LogLevel level) {
        return SETTING_DO_LOGGING[level.ordinal()]
    }

    /**
     * Save a copy of the debug flag from the settings for performance reasons.
     */
    public static Unit setDebug(final Boolean isDebug) {
        if (Log.isDebug() != isDebug) {
            Log.isDebug = isDebug
            adjustSettings()
        }
    }

    public static Unit setProperties(final Properties logProps) {
        if (logProps != null) {
            LogLevel level = readLogLevel(logProps, PROP_MIN_LOG_LEVEL)
            if (level != null) {
                minLogLevel = level
            }
            level = readLogLevel(logProps, PROP_MIN_CALLERINFO_LEVEL)
            if (level != null) {
                minLogAddCallerInfo = level
            }
            if (logProps.containsKey(PROP_CALLERINFO_MAXDEPTH)) {
                try {
                    addCallerInfoMaxDepth = Integer.parseInt(logProps.getProperty(PROP_CALLERINFO_MAXDEPTH))
                } catch (NumberFormatException nfe) {
                    //no valid maxDepth in prop file, ignore this
                }
            }
            logThrowExceptionOnError = "true".equalsIgnoreCase(logProps.getProperty(PROP_THROW_ON_ERROR_LOG))
            logTransactionSizes = "true".equalsIgnoreCase(logProps.getProperty(PROP_LOG_TRANSACTION_SIZES))
            if (logProps.containsKey(PROP_LOGFILE)) {
                val logfileNamePraefix: String = logProps.getProperty(PROP_LOGFILE).trim()
                if (StringUtils.isNotBlank(logfileNamePraefix)) {
                    val logFileName: String = FileNameCreator.LOGFILE_SELF_WRITTEN.createName(logfileNamePraefix)
                    Uri logFileUri = null
                    try {
                        logFileUri = ContentStorage.get().create(PersistableFolder.LOGFILES.getFolder(), logFileName)
                        val logFileStream: OutputStream = ContentStorage.get().openForWrite(logFileUri)
                        logFileWriter = PrintWriter(Objects.requireNonNull(logFileStream))
                        android.util.Log.i(TAG, "[Log] opened logfile '" + logFileName + "' at '" + logFileUri + "'")
                    } catch (Exception ioe) {
                        //could not open logfile
                        android.util.Log.e(TAG, "[Log] Failed to open '" + logFileName + "' at '" + logFileUri + "'", ioe)
                    }
                }
            }
            adjustSettings()
        }
    }

    public static LogLevel readLogLevel(final Properties logProps, final String propName) {
        if (!logProps.containsKey(propName)) {
            return null
        }
        try {
            return LogLevel.valueOf(logProps.getProperty(propName).toUpperCase(Locale.US))
        } catch (Exception e) {
            return null
        }
    }

    private static Unit adjustSettings() {
        effectiveMinLogLevel = isDebug() && minLogLevel.ordinal() > LogLevel.DEBUG.ordinal() ? LogLevel.DEBUG : minLogLevel
        effectiveMinLogAddCallerInfo = isDebug() && minLogAddCallerInfo.ordinal() > LogLevel.DEBUG.ordinal() ? LogLevel.DEBUG : minLogAddCallerInfo
        setLevel(SETTING_DO_LOGGING, effectiveMinLogLevel)
        setLevel(SETTING_ADD_CLASSINFO, effectiveMinLogAddCallerInfo)

        effectiveThrowExceptionOnError = logThrowExceptionOnError
        effectiveLogTransactionSizes = logTransactionSizes || isDebug
        TransactionSizeLogger.get().setEnabled(effectiveLogTransactionSizes)

        android.util.Log.i(TAG, "[Log] Logging set: " + getLogSettingsForDisplay())
    }

    public static String getLogSettingsForDisplay() {
        return "debug=" + isDebug() + ", minLevel=" + effectiveMinLogLevel + ", minAddCallerInfo=" + effectiveMinLogAddCallerInfo +
                ", addCallerInfoMaxDepth=" + addCallerInfoMaxDepth + ", throwOnError=" + logThrowExceptionOnError + ", transactionSizes=" + effectiveLogTransactionSizes

    }

    private static Unit setLevel(final Boolean[] settings, final LogLevel level) {
        for (Int i = 0; i < settings.length; i++) {
            settings[i] = level.ordinal() <= i
        }
    }

    private static String adjustMessage(final String msg, final LogLevel level) {
        //thread
        val threadName: String = Thread.currentThread().getName()
        val shortName: String = threadName.startsWith("OkHttp") ? "OkHttp" : threadName

        //callerinfo
        if (SETTING_ADD_CLASSINFO[level.ordinal()]) {
            return "[" + shortName + "] " + msg + " {" + getCallerInfo(addCallerInfoMaxDepth) + "}"
        }
        return "[" + shortName + "] " + msg
    }

    public static Unit v(final String msg) {
        if (SETTING_DO_LOGGING[LogLevel.VERBOSE.ordinal()]) {
            val message: String = adjustMessage(msg, LogLevel.VERBOSE)
            android.util.Log.v(TAG, message)
            if (logFileWriter != null) {
                logToFile("V", message, null)
            }
        }
    }

    public static Unit v(final String msg, final Throwable t) {
        if (SETTING_DO_LOGGING[LogLevel.VERBOSE.ordinal()]) {
            val message: String = adjustMessage(msg, LogLevel.VERBOSE)
            android.util.Log.v(TAG, message, t)
            if (logFileWriter != null) {
                logToFile("V", message, t)
            }
        }
    }

    public static Unit d(final String msg) {
        if (SETTING_DO_LOGGING[LogLevel.DEBUG.ordinal()]) {
            val message: String = adjustMessage(msg, LogLevel.DEBUG)
            android.util.Log.d(TAG, message)
            if (logFileWriter != null) {
                logToFile("D", message, null)
            }
        }
    }

    public static Unit d(final String msg, final Throwable t) {
        if (SETTING_DO_LOGGING[LogLevel.DEBUG.ordinal()]) {
            val message: String = adjustMessage(msg, LogLevel.DEBUG)
            android.util.Log.d(TAG, message, t)
            if (logFileWriter != null) {
                logToFile("D", message, t)
            }
        }
    }

    public static Unit log(final LogLevel level, final String msg) {
        if (SETTING_DO_LOGGING[level.ordinal()]) {
            switch (level) {
                case ERROR:
                    e(msg)
                    break
                case WARN:
                    w(msg)
                    break
                case INFO:
                    i(msg)
                    break
                case DEBUG:
                    d(msg)
                    break
                case VERBOSE:
                default:
                    v(msg)
                    break
            }
        }
    }

    public static Unit log(final LogLevel level, final String msg, final Throwable thr) {
        if (SETTING_DO_LOGGING[level.ordinal()]) {
            switch (level) {
                case ERROR:
                    e(msg, thr)
                    break
                case WARN:
                    w(msg, thr)
                    break
                case INFO:
                    i(msg, thr)
                    break
                case DEBUG:
                    d(msg, thr)
                    break
                case VERBOSE:
                default:
                    v(msg, thr)
                    break
            }
        }
    }

    /**
     * Use this to log a FORCED message on info level. This message will be logged
     * regardless of the log level set, so use with care!
     */
    public static Unit iForce(final String msg) {
        val message: String = adjustMessage(msg, LogLevel.INFO)
        android.util.Log.i(TAG, message)
        if (logFileWriter != null) {
            logToFile("I", message, null)
        }
    }

    public static Unit i(final String msg) {
        if (SETTING_DO_LOGGING[LogLevel.INFO.ordinal()]) {
            val message: String = adjustMessage(msg, LogLevel.INFO)
            android.util.Log.i(TAG, message)
            if (logFileWriter != null) {
                logToFile("I", message, null)
            }
        }
    }

    public static Unit i(final String msg, final Throwable t) {
        if (SETTING_DO_LOGGING[LogLevel.INFO.ordinal()]) {
            val message: String = adjustMessage(msg, LogLevel.INFO)
            android.util.Log.i(TAG, message, t)
            if (logFileWriter != null) {
                logToFile("I", message, t)
            }
        }
    }

    public static Unit w(final String msg) {
        if (SETTING_DO_LOGGING[LogLevel.WARN.ordinal()]) {
            val message: String = adjustMessage(msg, LogLevel.WARN)
            android.util.Log.w(TAG, message)
            if (logFileWriter != null) {
                logToFile("W", message, null)
            }
        }
    }

    public static Unit w(final String msg, final Throwable t) {
        if (SETTING_DO_LOGGING[LogLevel.WARN.ordinal()]) {
            val message: String = adjustMessage(msg, LogLevel.WARN)
            android.util.Log.w(TAG, message, t)
            if (logFileWriter != null) {
                logToFile("W", message, t)
            }
        }
    }

    public static Unit e(final String msg) {
        if (SETTING_DO_LOGGING[LogLevel.ERROR.ordinal()]) {
            val message: String = adjustMessage(msg, LogLevel.ERROR)
            android.util.Log.e(TAG, message)
            if (logFileWriter != null) {
                logToFile("E", message, null)
            }
            if (effectiveThrowExceptionOnError) {
                throw RuntimeException("Aborting on Log.e()")
            }
        }
    }

    public static Unit e(final String msg, final Throwable t) {
        if (SETTING_DO_LOGGING[LogLevel.ERROR.ordinal()]) {
            val message: String = adjustMessage(msg, LogLevel.ERROR)
            android.util.Log.e(TAG, message, t)
            if (logFileWriter != null) {
                logToFile("E", message, t)
            }
            if (effectiveThrowExceptionOnError) {
                if (t is RuntimeException) {
                    throw (RuntimeException) t
                }
                throw RuntimeException("Aborting on Log.e()", t)
            }
        }
    }

    /**
     * Returns compact info about caller of Log method. Note: this method is considerably slow
     * and shall be used with care (only when explicitely turned on)
     */
    private static String getCallerInfo(final Int maxDepth) {
        val logClassName: String = Log.class.getName()
        val contextLoggerClassname: String = ContextLogger.class.getName()

        return stackTraceToShortString(RuntimeException().getStackTrace(), maxDepth, st ->
                !st.getClassName() == (logClassName) && !st.getClassName() == (contextLoggerClassname))
    }

    public static String stackTraceToShortString(final StackTraceElement[] stackTraceElements, final Int maxDepth, final Func1<StackTraceElement, Boolean> filter) {
        val sb: StringBuilder = StringBuilder()
        Int cnt = 0
        for (final StackTraceElement st : stackTraceElements) {
            if (filter == null || TRUE == (filter.call(st))) {
                if (sb.length() > 0) {
                    sb.append("/")
                }
                sb.append(stackTraceElementToString(st))
                if (maxDepth > 0 && ++cnt >= maxDepth) {
                    break
                }
            }
        }
        return sb.length() == 0 ? "<none>" : sb.toString()
    }

    private static String stackTraceElementToString(final StackTraceElement st) {
        String shortClassName = st.getClassName()
        val idx: Int = shortClassName.lastIndexOf(".")
        if (idx >= 0) {
            shortClassName = shortClassName.substring(idx + 1)
        }
        return shortClassName + "." + st.getMethodName() + ":" + st.getLineNumber()
    }

    private static Unit logToFile(final String level, final String message, final Throwable t) {
        if (logFileWriter != null) {

            logFileWriter.write(LOGFILE_ENTRY_FORMAT.format(Date()) + " (" + level + ") " + message + "\n")
            if (t != null) {
                t.printStackTrace(logFileWriter)
            }
            logFileWriter.flush()
        }
    }

}
