package cgeo.geocaching;

import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import android.os.Looper;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Helper class, gaining and logging statistics about a looper
 */
public class LooperLogger {

    private static final Format DATE_FORMATTER = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    private long totalTime = 0;
    private long currStartTime = 0;
    private String currMsg = null;
    private long totalMsgCount = 0;

    private final AtomicBoolean collectTraces = new AtomicBoolean(false);
    private final List<String> collectedInfos = new ArrayList<>();

    public static void startLogging(final Looper looper) {
        final LooperLogger ll = new LooperLogger();
        looper.setMessageLogging(ll::process);
        looper.getThread().getStackTrace();
        AndroidRxUtils.runPeriodically(AndroidRxUtils.computationScheduler, () -> {
            if (!ll.collectTraces.get()) {
                return;
            }
            synchronized (ll.collectedInfos) {
                ll.collectedInfos.add(
                        DATE_FORMATTER.format(new Date(System.currentTimeMillis())) + "/" + looper.getThread().getState() + ": " +
                                Log.stackTraceToShortString(looper.getThread().getStackTrace(), 0, null));
            }
        }, 0, 200);
        Log.iForce("LooperLogger: started for Thread: " + looper.getThread().getName());
    }

    private LooperLogger() {
        //no public instance
    }

    private void process(final String msg) {
        final boolean isStart = msg.startsWith(">");
        if (isStart) {
            currStartTime = System.currentTimeMillis();
            currMsg = msg;
            totalMsgCount++;
            synchronized (collectedInfos) {
                collectedInfos.clear();
            }
            collectTraces.set(Log.isEnabled(Log.LogLevel.DEBUG));
        } else {
            collectTraces.set(false);
            final long duration = System.currentTimeMillis() - currStartTime;
            totalTime += duration;


            if (duration > 500) {
                final StringBuilder sb = new StringBuilder();
                synchronized (collectedInfos) {
                    sb.append("LooperLogger: long process time for " + currMsg + " (" + duration + "ms, " + getStats() + "), " + collectedInfos.size() + "traces:");
                    for (String info : collectedInfos) {
                        sb.append("\n   ").append(info);
                    }
                }
                Log.w(sb.toString());
            } else if (Log.isDebug() && totalMsgCount % 1000 == 0) {
                Log.d("LooperLogger: " + getStats());
            }
        }
    }

    private String getStats() {
        return "total:#" + totalMsgCount + "/" + totalTime + "ms/avg " + (totalTime / (totalMsgCount == 0 ? 1 : totalMsgCount)) + "ms";
    }

}
