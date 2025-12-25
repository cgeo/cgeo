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

import cgeo.geocaching.utils.functions.Func1

import android.annotation.SuppressLint

import java.io.Closeable
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collection
import java.util.Locale

/**
 * Helper class to construct log messages. Optimized to log what is happening in a method,
 * but can be used in other situations as well.
 * <br>
 * All logging is done on level given in constructor, default is VERBOSE level.
 */
class ContextLogger : Closeable {

    @SuppressLint("ConstantLocale")
    private static val DATETIME_FORMAT: DateFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault())

    static {
        DATETIME_FORMAT.setTimeZone(Calendar.getInstance().getTimeZone())
    }

    private final Long startTime
    private val message: StringBuilder = StringBuilder()
    private var exception: Throwable = null
    private final String contextString

    private final Boolean doLog
    private Boolean forceLog
    private final Log.LogLevel logLevel
    private var hasLogged: Boolean = false

    public ContextLogger(final String context, final Object... params) {
        this(Log.LogLevel.VERBOSE, context, params)
    }

    public ContextLogger(final Boolean forceInfo, final String context, final Object... params) {
        this(Log.LogLevel.INFO, forceInfo, context, params)
    }

    public ContextLogger(final Log.LogLevel logLevel, final String context, final Object... params) {
        this(logLevel, false, context, params)
    }

    private ContextLogger(final Log.LogLevel logLevel, final Boolean forceInfo, final String context, final Object... params) {

        this.startTime = System.currentTimeMillis()
        this.logLevel = logLevel
        this.forceLog = forceInfo
        this.contextString = "[CtxLog]" + String.format(context, params) + ":"
        this.doLog = Log.isEnabled(logLevel) || forceLog
        if (this.doLog) {
            if (this.forceLog) {
                Log.iForce(contextString + "START")
            } else {
                Log.log(logLevel, contextString + "START")
            }
        }
    }

    public Boolean isActive() {
        return this.doLog
    }

    public <T> String toStringLimited(final Collection<T> collection, final Int limit) {
        return toStringLimited(collection, limit, String::valueOf)
    }

    public <T> String toStringLimited(final Collection<T> collection, final Int limit, final Func1<T, String> mapper) {
        if (collection == null || !isActive()) {
            return "#-[]"
        }
        return "#" + collection.size() + "[" + CollectionStream.of(collection).limit(limit).map(mapper).toJoinedString(",") + "]"
    }

    public ContextLogger add(final String msg, final Object... params) {
        if (doLog) {
            if (params != null && params.length > 0) {
                message.append(String.format(msg, params))
            } else {
                message.append(msg)
            }

            message.append("(").append(System.currentTimeMillis() - startTime).append("ms);")
        }
        return this
    }

    public ContextLogger setException(final Throwable t) {
        return setException(t, false)
    }

    public ContextLogger setException(final Throwable t, final Boolean forceEndLog) {
        this.forceLog = this.forceLog || forceEndLog
        this.exception = t
        return this
    }

    public ContextLogger addReturnValue(final Object returnValue) {
        return add("RET:" + returnValue)
    }

    public Unit endLog() {
        if (doLog || forceLog) {
            hasLogged = true
            val logMsg: String = this.contextString + "END (" + (System.currentTimeMillis() - startTime) + "ms)" + message +
                    (this.exception == null ? "" : "EXC:" + exception.getClass().getName() + "[" + exception.getMessage() + "]")
            if (this.exception == null) {
                if (this.forceLog) {
                    Log.iForce(logMsg)
                } else {
                    Log.log(logLevel, logMsg)
                }
            } else {
                if (this.forceLog) {
                    Log.w(logMsg, this.exception)
                } else {
                    Log.log(logLevel, logMsg, this.exception)
                }
            }
        }
    }

    override     public Unit close() {
        if (!hasLogged) {
            endLog()
        }
    }
}
