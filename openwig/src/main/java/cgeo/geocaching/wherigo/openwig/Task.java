/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig
 */
package cgeo.geocaching.wherigo.openwig;

import cgeo.geocaching.wherigo.kahlua.vm.LuaState;

/**
 * Represents a task or objective in a Wherigo game.
 * <p>
 * Task extends EventTable to provide quest/objective tracking. Tasks can be
 * active or inactive, complete or incomplete, and have a correctness state
 * (done or failed).
 * <p>
 * Key features:
 * <ul>
 * <li>Tracks active state - whether the task is currently active</li>
 * <li>Tracks completion state - whether the task has been completed</li>
 * <li>Tracks correctness - whether completed correctly (DONE) or incorrectly (FAILED)</li>
 * <li>Triggers events on state changes (OnSetActive, OnSetComplete, OnSetCorrectState)</li>
 * <li>Only visible when both visible flag is true and task is active</li>
 * </ul>
 */
public class Task extends EventTable {

    private boolean active;
    private boolean complete;

    public static final int PENDING = 0;
    public static final int DONE = 1;
    public static final int FAILED = 2;
    private int state = DONE;

    public boolean isVisible () { return visible && active; }
    public boolean isComplete () { return complete; }
    public int state () {
        if (!complete) return PENDING;
        else return state;
    }

    protected String luaTostring () { return "a ZTask instance"; }

    protected void setItem (String key, Object value) {
        if ("Active".equals(key)) {
            boolean a = LuaState.boolEval(value);
            if (a != active) {
                active = a;
                callEvent("OnSetActive", null);
            }
        } else if ("Complete".equals(key)) {
            boolean c = LuaState.boolEval(value);
            if (c != complete) {
                complete = c;
                callEvent("OnSetComplete", null);
            }
        } else if ("CorrectState".equals(key) && value instanceof String) {
            String v = (String)value;
            int s = DONE;
            if ("Incorrect".equalsIgnoreCase(v) || "NotCorrect".equalsIgnoreCase(v)) {
                s = FAILED;
            }
            if (s != state) {
                state = s;
                callEvent("OnSetCorrectState", null);
            }
        } else super.setItem(key, value);
    }
}
