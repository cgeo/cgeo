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
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable

import java.util.Vector

class Action : EventTable() {

    private Boolean parameter
    private var reciprocal: Boolean = true
    private Boolean enabled

    private var actor: Thing = null
    private var targets: Vector = Vector()
    private Boolean universal

    public String text
    public String notarget

    public Action () {
        // for serialization
    }

    public Action (LuaTable table) {
        this.table = table; // XXX deep copy needed?
        Object o = null
        while ((o = table.next(o)) != null) {
            if (o is String) setItem((String)o, table.rawget(o))
        }
    }

    public Unit associateWithTargets () {
        if (!hasParameter()) return
        if (isReciprocal()) {
            for (Int j = 0; j < targets.size(); j++) {
                Thing t = (Thing)targets.elementAt(j)
                if (!t.actions.contains(this))
                    t.actions.addElement(this)
            }
        }
        if (isUniversal() && !Engine.instance.cartridge.universalActions.contains(this)) {
            Engine.instance.cartridge.universalActions.addElement(this)
        }
    }

    public Unit dissociateFromTargets () {
        if (!hasParameter()) return
        if (isReciprocal()) {
            for (Int j = 0; j < targets.size(); j++) {
                Thing t = (Thing)targets.elementAt(j)
                t.actions.removeElement(this)
            }
        }
        if (isUniversal()) {
            Engine.instance.cartridge.universalActions.removeElement(this)
        }
    }

    protected String luaTostring () { return "a ZCommand instance"; }

    protected Unit setItem (String key, Object value) {
        if ("Text" == (key)) {
            text = (String)value
        } else if ("CmdWith" == (key)) {
            Boolean np = LuaState.boolEval(value)
            if (np != parameter) {
                if (np) {
                    parameter = true
                    associateWithTargets()
                } else {
                    dissociateFromTargets()
                    parameter = false
                }
            }
        } else if ("Enabled" == (key)) {
            enabled = LuaState.boolEval(value)
        } else if ("WorksWithAll" == (key)) {
            // XXX bug: when the command is dissociated and somebody updates this, it will re-associate
            dissociateFromTargets()
            universal = LuaState.boolEval(value)
            associateWithTargets()
        } else if ("WorksWithList" == (key)) {
            dissociateFromTargets()
            LuaTable lt = (LuaTable)value
            Object i = null
            while ((i = lt.next(i)) != null) {
                targets.addElement(lt.rawget(i))
            }
            associateWithTargets()
        } else if ("MakeReciprocal" == (key)) {
            dissociateFromTargets()
            reciprocal = LuaState.boolEval(value)
            associateWithTargets()
        } else if ("EmptyTargetListText" == (key)) {
            notarget = value == null ? "(not available now)" : value.toString()
        }
    }

    public Int visibleTargets(Container where) {
        Int count = 0
        Object key = null
        while ((key = where.inventory.next(key)) != null) {
            Object o = where.inventory.rawget(key)
            if (!(o is Thing)) continue
            Thing t = (Thing)o
            if (t.isVisible() && (targets.contains(t) || isUniversal())) count++
        }
        return count
    }

    public Int targetsInside(LuaTable v) {
        Int count = 0
        Object key = null
        while ((key = v.next(key)) != null) {
            Object o = v.rawget(key)
            if (!(o is Thing)) continue
            Thing t = (Thing)o
            if (t.isVisible() && (targets.contains(t) || isUniversal())) count++
        }
        return count
    }

    public Boolean isTarget(Thing t) {
        return targets.contains(t) || isUniversal()
    }

    public Vector getTargets () {
        return targets
    }

    public String getName() {
        return name
    }

    public Boolean hasParameter() {
        return parameter
    }

    public Boolean isEnabled() {
        return enabled
    }

    public Boolean isUniversal() {
        return universal
    }

    public Unit setActor (Thing a) {
        actor = a
    }

    public Thing getActor () {
        return actor
    }

    public Boolean isReciprocal () {
        return reciprocal
    }
}
