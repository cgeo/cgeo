package cgeo.geocaching;

import cgeo.geocaching.utils.Log;

import android.os.Looper;

/** Helper class, gaining and logging statistics about a looper */
public class LooperLogger {

    private long totalTime = 0;
    private long currStartTime = 0;
    private String currMsg = null;
    private long totalMsgCount = 0;

    public static void startLogging(final Looper looper) {
        final LooperLogger ll = new LooperLogger();
        looper.setMessageLogging(ll::process);
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
        } else {
            final long duration = System.currentTimeMillis() - currStartTime;
            totalTime += duration;

            if (duration > 500) {
                Log.w("LooperLogger: long process time for " + currMsg + " (" + duration + "ms, " + getStats() + ")");
            } else if (Log.isDebug() && totalMsgCount % 1000 == 0) {
                Log.d("LooperLogger: " + getStats());
            }
        }
    }

    private String getStats() {
        return "total:#" + totalMsgCount + "/" + totalTime + "ms/avg " + (totalTime / totalMsgCount) + "ms";
    }

}
