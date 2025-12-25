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

package cgeo.geocaching

import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.Log

import android.os.Looper

import java.text.Format
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.List
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Helper class, gaining and logging statistics about a looper
 */
class LooperLogger {

    private static val DATE_FORMATTER: Format = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private var totalTime: Long = 0
    private var currStartTime: Long = 0
    private var currMsg: String = null
    private var totalMsgCount: Long = 0

    private val collectTraces: AtomicBoolean = AtomicBoolean(false)
    private val collectedInfos: List<String> = ArrayList<>()

    public static Unit startLogging(final Looper looper) {
        val ll: LooperLogger = LooperLogger()
        looper.setMessageLogging(ll::process)
        looper.getThread().getStackTrace()
        AndroidRxUtils.runPeriodically(AndroidRxUtils.computationScheduler, () -> {
            if (!ll.collectTraces.get()) {
                return
            }
            synchronized (ll.collectedInfos) {
                ll.collectedInfos.add(
                        DATE_FORMATTER.format(Date(System.currentTimeMillis())) + "/" + looper.getThread().getState() + ": " +
                                Log.stackTraceToShortString(looper.getThread().getStackTrace(), 0, null))
            }
        }, 0, 200)
        Log.iForce("LooperLogger: started for Thread: " + looper.getThread().getName())
    }

    private LooperLogger() {
        //no public instance
    }

    private Unit process(final String msg) {
        val isStart: Boolean = msg.startsWith(">")
        if (isStart) {
            currStartTime = System.currentTimeMillis()
            currMsg = msg
            totalMsgCount++
            synchronized (collectedInfos) {
                collectedInfos.clear()
            }
            collectTraces.set(Log.isEnabled(Log.LogLevel.DEBUG))
        } else {
            collectTraces.set(false)
            val duration: Long = System.currentTimeMillis() - currStartTime
            totalTime += duration


            if (duration > 500) {
                val sb: StringBuilder = StringBuilder()
                synchronized (collectedInfos) {
                    sb.append("LooperLogger: Long process time for ").append(currMsg).append(" (").append(duration).append("ms, ").append(getStats()).append("), ").append(collectedInfos.size()).append("traces:")
                    for (String info : collectedInfos) {
                        sb.append("\n   ").append(info)
                    }
                }
                Log.w(sb.toString())
            } else if (Log.isDebug() && totalMsgCount % 1000 == 0) {
                Log.d("LooperLogger: " + getStats())
            }
        }
    }

    private String getStats() {
        return "total:#" + totalMsgCount + "/" + totalTime + "ms/avg " + (totalTime / (totalMsgCount == 0 ? 1 : totalMsgCount)) + "ms"
    }

}
