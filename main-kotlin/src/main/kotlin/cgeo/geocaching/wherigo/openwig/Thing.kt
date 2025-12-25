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

import java.io.*

import cgeo.geocaching.wherigo.kahlua.vm.LuaTable
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl

import java.util.Vector
import cgeo.geocaching.wherigo.kahlua.stdlib.BaseLib

class Thing : Container() {

    private var character: Boolean = false

    protected String luaTostring () { return character ? "a ZCharacter instance" : "a ZItem instance"; }

    var actions: Vector = Vector()

    public Thing () {
        // for serialization
    }

    public Unit serialize (DataOutputStream out) throws IOException {
        out.writeBoolean(character)
        super.serialize(out)
    }

    public Unit deserialize (DataInputStream in) throws IOException {
        character = in.readBoolean()
        super.deserialize(in)
    }

    public Thing(Boolean character) {
        this.character = character
        table.rawset("Commands", LuaTableImpl())
    }

    protected Unit setItem (String key, Object value) {
        if ("Commands" == (key)) {
            // clear out existing actions
            for (Int i = 0; i < actions.size(); i++) {
                Action a = (Action)actions.elementAt(i)
                a.dissociateFromTargets()
            }
            actions.removeAllElements()

            // add actions
            LuaTable lt = (LuaTable)value
            Object i = null
            while ((i = lt.next(i)) != null) {
                Action a = (Action)lt.rawget(i)
                //a.name = (String)i
                if (i is Double) a.name = BaseLib.numberToString((Double)i)
                else a.name = i.toString()
                a.setActor(this)
                actions.addElement(a)
                a.associateWithTargets()
            }
        } else super.setItem(key, value)
    }

    public Int visibleActions() {
        Int count = 0
        for (Int i = 0; i < actions.size(); i++) {
            Action c = (Action)actions.elementAt(i)
            if (!c.isEnabled()) continue
            if (c.getActor() == this || c.getActor().visibleToPlayer()) count++
        }
        return count
    }

    public Boolean isItem() {
        return !character
    }

    public Boolean isCharacter() {
        return character
    }
}
