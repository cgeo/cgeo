/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig
 */
package cgeo.geocaching.wherigo.openwig;

import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;

import java.io.*;

/**
 * Represents a timer in a Wherigo game.
 * <p>
 * Timer extends EventTable to provide time-based event triggering. Timers can
 * operate in two modes: countdown (fires once after a duration) or interval
 * (fires repeatedly at regular intervals).
 * <p>
 * Key features:
 * <ul>
 * <li>Supports countdown mode - fires OnTick event once after specified duration</li>
 * <li>Supports interval mode - fires OnTick event repeatedly at regular intervals</li>
 * <li>Can be started and stopped via Start() and Stop() methods</li>
 * <li>Duration can be set dynamically</li>
 * <li>Remaining time is accessible to scripts</li>
 * <li>Uses a shared global Timer for efficiency</li>
 * </ul>
 * <p>
 * Timers are commonly used for time-based challenges, delayed events, and
 * periodic game state updates.
 */
public class Timer extends EventTable {

    private static java.util.Timer globalTimer;

    private final JavaFunction start = (callFrame, nArguments) -> {
        Timer t = (Timer)callFrame.get(0);
        t.start();
        return 0;
    };

    private final JavaFunction stop = (callFrame, nArguments) -> {
        Timer t = (Timer)callFrame.get(0);
        t.stop();
        return 0;
    };

    private final JavaFunction tick = (callFrame, nArguments) -> {
        Timer t = (Timer)callFrame.get(0);
        //t.tick();
        t.callEvent("OnTick", null);
        return 0;
    };

    public static void register () {
        Engine currentEngine = Engine.getCurrentInstance();
        if (currentEngine == null) return;
        
        // Create static versions for savegame compatibility
        JavaFunction startFunc = (callFrame, nArguments) -> {
            Timer t = (Timer)callFrame.get(0);
            t.start();
            return 0;
        };
        JavaFunction stopFunc = (callFrame, nArguments) -> {
            Timer t = (Timer)callFrame.get(0);
            t.stop();
            return 0;
        };
        JavaFunction tickFunc = (callFrame, nArguments) -> {
            Timer t = (Timer)callFrame.get(0);
            t.callEvent("OnTick", null);
            return 0;
        };
        
        currentEngine.savegame.addJavafunc(startFunc);
        currentEngine.savegame.addJavafunc(stopFunc);
        currentEngine.savegame.addJavafunc(tickFunc);
    }

    protected String luaTostring () { return "a ZTimer instance"; }

    private class TimerTask extends java.util.TimerTask {
        public boolean restart = false;
        public void run() {
            tick();
            Engine.refreshUI();
            if (restart) {
                cancel();
                task = null;
                start();
            }
        }
    }

    private TimerTask task = null;

    private static final int COUNTDOWN = 0;
    private static final int INTERVAL = 1;
    private int type = COUNTDOWN;

    private static final Double ZERO = (double) 0;

    private long duration = -1;
    private long lastTick = 0;

    public Timer () {
        if (globalTimer == null) globalTimer = new java.util.Timer();
        rawset("Start", start);
        rawset("Stop", stop);
        rawset("Tick", tick);
    }

    protected void setItem (String key, Object value) {
        if ("Type".equals(key) && value instanceof String v) {
            int t = type;
            if ("Countdown".equals(v)) {
                t = COUNTDOWN;
                if (t != type && task != null)
                    task.restart = false;
                    // we don't need task.restart here,
                    // so make sure it's not set
            } else if ("Interval".equals(v)) {
                t = INTERVAL;
                if (t != type && task != null)
                    task.restart = true;
            }
            type = t;
        } else if ("Duration".equals(key) && value instanceof Double d) {
            long duration_ms = (long) LuaState.fromDouble(d);
            rawset("Remaining", ZERO);
            duration = duration_ms * 1000;
        } else super.setItem(key, value);
    }

    public void start () {
        Engine.log("TIME: " + name + " start", Engine.LOG_CALL);
        if (task != null) return;
        if (duration == 0) {
            // XXX this might be a problem if the timer is interval
            callEvent("OnStart", null);
            callEvent("OnTick", null);
            return;
        }
        start(duration, true);
    }

    private void start (long when, boolean callEvent) {
        task = new TimerTask();
        lastTick = System.currentTimeMillis();
        if (callEvent) callEvent("OnStart", null);
        updateRemaining();
        switch (type) {
            case COUNTDOWN:
                globalTimer.schedule(task, when);
                break;
            case INTERVAL:
                globalTimer.scheduleAtFixedRate(task, when, duration);
                break;
        }
    }

    public void stop () {
        if (task != null) {
            Engine.log("TIME: " + name + " stop", Engine.LOG_CALL);
            task.cancel();
            task = null;
            callEvent("OnStop", null);
        }
    }

    public void tick () {
        Engine.log("TIME: " + name + " tick", Engine.LOG_CALL);
        Engine.callEvent(this, "OnTick", null);
        lastTick = System.currentTimeMillis();
        updateRemaining();
        if (type == COUNTDOWN && task != null) {
            task.cancel();
            task = null;
        }
        if (type == INTERVAL && task != null && !task.restart)
            Engine.callEvent(this, "OnStart", null);
            // the devices seem to do this.
        // else it will be restarted and OnStart called again anyway
    }

    public void updateRemaining () {
        if (task == null) {
            rawset("Remaining", ZERO);
        } else {
            long stm = System.currentTimeMillis();
            long remaining = (duration/1000) - ((stm - lastTick)/1000);
            rawset("Remaining", LuaState.toDouble(remaining));
        }
    }

    public static void kill() {
        if (globalTimer != null) globalTimer.cancel();
        globalTimer = null;
    }

    @Override
    public void serialize (DataOutputStream out) throws IOException {
        out.writeBoolean(task != null);
        out.writeLong(lastTick);
        super.serialize(out);
    }

    @Override
    public void deserialize (DataInputStream in) throws IOException {
        boolean resume = in.readBoolean();
        lastTick = in.readLong();
        super.deserialize(in);

        if (resume) {
            if (lastTick + duration < System.currentTimeMillis()) {
                Engine.callEvent(this, "OnTick", null);
            } else {
                start(lastTick + duration - System.currentTimeMillis(), false);
            }
            if (type == INTERVAL) start();
        }
    }
}
