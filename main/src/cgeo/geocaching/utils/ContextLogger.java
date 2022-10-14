package cgeo.geocaching.utils;

import cgeo.geocaching.utils.functions.Func1;

import android.annotation.SuppressLint;

import java.io.Closeable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Locale;

/**
 * Helper class to construct log messages. Optimized to log what is happening in a method,
 * but can be used in other situations as well.
 *
 * All logging is done on level given in constructor, default is VERBOSE level.
 */
public class ContextLogger implements Closeable {

    @SuppressLint("ConstantLocale")
    private static final DateFormat DATETIME_FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault());

    static {
        DATETIME_FORMAT.setTimeZone(Calendar.getInstance().getTimeZone());
    }

    private final long startTime;
    private final StringBuilder message = new StringBuilder();
    private Throwable exception = null;
    private final String contextString;

    private final boolean doLog;
    private final boolean forceLog;
    private final Log.LogLevel logLevel;
    private boolean hasLogged = false;

    public ContextLogger(final String context, final Object... params) {
        this(Log.LogLevel.VERBOSE, context, params);
    }

    public ContextLogger(final boolean forceInfo, final String context, final Object... params) {
        this(Log.LogLevel.INFO, forceInfo, context, params);
    }

    public ContextLogger(final Log.LogLevel logLevel, final String context, final Object... params) {
        this(logLevel, false, context, params);
    }

    private ContextLogger(final Log.LogLevel logLevel, final boolean forceInfo, final String context, final Object... params) {

        this.startTime = System.currentTimeMillis();
        this.logLevel = logLevel;
        this.forceLog = forceInfo;
        this.doLog = Log.isEnabled(logLevel) || forceLog;
        if (this.doLog) {
            this.contextString = "[CtxLog]" + String.format(context, params) + ":";
            if (this.forceLog) {
                Log.iForce(contextString + "START");
            } else {
                Log.log(logLevel, contextString + "START");
            }
        } else {
            this.contextString = null;
        }
    }

    public boolean isActive() {
        return this.doLog;
    }

    public <T> String toStringLimited(final Collection<T> collection, final int limit) {
        return toStringLimited(collection, limit, String::valueOf);
    }

    public <T> String toStringLimited(final Collection<T> collection, final int limit, final Func1<T, String> mapper) {
        if (collection == null || !isActive()) {
            return "#-[]";
        }
        return "#" + collection.size() + "[" + CollectionStream.of(collection).limit(limit).map(mapper).toJoinedString(",") + "]";
    }

    public ContextLogger add(final String msg, final Object... params) {
        if (doLog) {
            if (params != null && params.length > 0) {
                message.append(String.format(msg, params));
            } else {
                message.append(msg);
            }

            message.append("(").append(System.currentTimeMillis() - startTime).append("ms);");
        }
        return this;
    }

    public ContextLogger setException(final Throwable t) {
        this.exception = t;
        return this;
    }

    public ContextLogger addReturnValue(final Object returnValue) {
        add("RET:" + returnValue);
        return this;
    }

    public void endLog() {
        if (doLog) {
            hasLogged = true;
            final String logMsg = this.contextString + "END (" + (System.currentTimeMillis() - startTime) + "ms)" + message +
                    (this.exception == null ? "" : "EXC:" + exception.getClass().getName() + "[" + exception.getMessage() + "]");
            if (this.exception == null) {
                if (this.forceLog) {
                    Log.iForce(logMsg);
                } else {
                    Log.log(logLevel, logMsg);
                }
            } else {
                if (this.forceLog) {
                    Log.w(logMsg, this.exception);
                } else {
                    Log.log(logLevel, logMsg, this.exception);
                }
            }
        }
    }

    @Override
    public void close() {
        if (!hasLogged) {
            endLog();
        }
    }
}
