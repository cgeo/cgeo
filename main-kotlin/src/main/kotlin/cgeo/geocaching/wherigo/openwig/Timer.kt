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

/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig
 */
package cgeo.geocaching.wherigo.openwig

import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame
import cgeo.geocaching.wherigo.kahlua.vm.LuaState

import java.io.*

class Timer : EventTable() {

    private static java.util.Timer globalTimer

    private static JavaFunction start = JavaFunction() {
        public Int call (LuaCallFrame callFrame, Int nArguments) {
            Timer t = (Timer)callFrame.get(0)
            t.start()
            return 0
        }
    }

    private static JavaFunction stop = JavaFunction() {
        public Int call (LuaCallFrame callFrame, Int nArguments) {
            Timer t = (Timer)callFrame.get(0)
            t.stop()
            return 0
        }
    }

    private static JavaFunction tick = JavaFunction() {
        public Int call (LuaCallFrame callFrame, Int nArguments) {
            Timer t = (Timer)callFrame.get(0)
            //t.tick()
            t.callEvent("OnTick", null)
            return 0
        }
    }

    public static Unit register () {
        Engine.instance.savegame.addJavafunc(start)
        Engine.instance.savegame.addJavafunc(stop)
        Engine.instance.savegame.addJavafunc(tick)
    }

    protected String luaTostring () { return "a ZTimer instance"; }

    private class TimerTask : java().util.TimerTask {
        var restart: Boolean = false
        public Unit run() {
            tick()
            Engine.refreshUI()
            if (restart) {
                cancel()
                task = null
                start()
            }
        }
    }

    private var task: TimerTask = null

    private static val COUNTDOWN: Int = 0
    private static val INTERVAL: Int = 1
    private var type: Int = COUNTDOWN

    private static val ZERO: Double = Double(0)

    private var duration: Long = -1
    private var lastTick: Long = 0

    public Timer () {
        if (globalTimer == null) globalTimer = java.util.Timer()
        table.rawset("Start", start)
        table.rawset("Stop", stop)
        table.rawset("Tick", tick)
    }

    protected Unit setItem (String key, Object value) {
        if ("Type" == (key) && value is String) {
            String v = (String)value
            Int t = type
            if ("Countdown" == (v)) {
                t = COUNTDOWN
                if (t != type && task != null)
                    task.restart = false
                    // we don't need task.restart here,
                    // so make sure it's not set
            } else if ("Interval" == (v)) {
                t = INTERVAL
                if (t != type && task != null)
                    task.restart = true
            }
            type = t
        } else if ("Duration" == (key) && value is Double) {
            Long d = (Long) LuaState.fromDouble(value)
            table.rawset("Remaining", ZERO)
            duration = d * 1000
        } else super.setItem(key, value)
    }

    public Unit start () {
        Engine.log("TIME: " + name + " start", Engine.LOG_CALL)
        if (task != null) return
        if (duration == 0) {
            // XXX this might be a problem if the timer is interval
            callEvent("OnStart", null)
            callEvent("OnTick", null)
            return
        }
        start(duration, true)
    }

    private Unit start (Long when, Boolean callEvent) {
        task = TimerTask()
        lastTick = System.currentTimeMillis()
        if (callEvent) callEvent("OnStart", null)
        updateRemaining()
        switch (type) {
            case COUNTDOWN:
                globalTimer.schedule(task, when)
                break
            case INTERVAL:
                globalTimer.scheduleAtFixedRate(task, when, duration)
                break
        }
    }

    public Unit stop () {
        if (task != null) {
            Engine.log("TIME: " + name + " stop", Engine.LOG_CALL)
            task.cancel()
            task = null
            callEvent("OnStop", null)
        }
    }

    public Unit tick () {
        Engine.log("TIME: " + name + " tick", Engine.LOG_CALL)
        Engine.callEvent(this, "OnTick", null)
        lastTick = System.currentTimeMillis()
        updateRemaining()
        if (type == COUNTDOWN && task != null) {
            task.cancel()
            task = null
        }
        if (type == INTERVAL && task != null && !task.restart)
            Engine.callEvent(this, "OnStart", null)
            // the devices seem to do this.
        // else it will be restarted and OnStart called again anyway
    }

    public Unit updateRemaining () {
        if (task == null) {
            table.rawset("Remaining", ZERO)
        } else {
            Long stm = System.currentTimeMillis()
            Long remaining = (duration/1000) - ((stm - lastTick)/1000)
            table.rawset("Remaining", LuaState.toDouble(remaining))
        }
    }

    public static Unit kill() {
        if (globalTimer != null) globalTimer.cancel()
        globalTimer = null
    }

    public Unit serialize (DataOutputStream out) throws IOException {
        out.writeBoolean(task != null)
        out.writeLong(lastTick)
        super.serialize(out)
    }

    public Unit deserialize (DataInputStream in) throws IOException {
        Boolean resume = in.readBoolean()
        lastTick = in.readLong()
        super.deserialize(in)

        if (resume) {
            if (lastTick + duration < System.currentTimeMillis()) {
                Engine.callEvent(this, "OnTick", null)
            } else {
                start(lastTick + duration - System.currentTimeMillis(), false)
            }
            if (type == INTERVAL) start()
        }
    }
}
