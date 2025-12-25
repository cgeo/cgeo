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

import cgeo.geocaching.wherigo.kahlua.vm.LuaState

class Task : EventTable() {

    private Boolean active
    private Boolean complete

    public static val PENDING: Int = 0
    public static val DONE: Int = 1
    public static val FAILED: Int = 2
    private var state: Int = DONE

    public Boolean isVisible () { return visible && active; }
    public Boolean isComplete () { return complete; }
    public Int state () {
        if (!complete) return PENDING
        else return state
    }

    protected String luaTostring () { return "a ZTask instance"; }

    protected Unit setItem (String key, Object value) {
        if ("Active" == (key)) {
            Boolean a = LuaState.boolEval(value)
            if (a != active) {
                active = a
                callEvent("OnSetActive", null)
            }
        } else if ("Complete" == (key)) {
            Boolean c = LuaState.boolEval(value)
            if (c != complete) {
                complete = c
                callEvent("OnSetComplete", null)
            }
        } else if ("CorrectState" == (key) && value is String) {
            String v = (String)value
            Int s = DONE
            if ("Incorrect".equalsIgnoreCase(v) || "NotCorrect".equalsIgnoreCase(v)) {
                s = FAILED
            }
            if (s != state) {
                state = s
                callEvent("OnSetCorrectState", null)
            }
        } else super.setItem(key, value)
    }
}
