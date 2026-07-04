/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig
 */
package cgeo.geocaching.wherigo.openwig;

import java.io.*;

import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;

import java.util.ArrayList;
import java.util.List;
import cgeo.geocaching.wherigo.kahlua.stdlib.BaseLib;

public class Thing extends Container {

    private boolean character = false;

    protected String luaTostring () { return character ? "a ZCharacter instance" : "a ZItem instance"; }

    public List<Action> actions = new ArrayList<>();

    public Thing () {
        // for serialization
    }

    public void serialize (final DataOutputStream out) throws IOException {
        out.writeBoolean(character);
        super.serialize(out);
    }

    public void deserialize (final DataInputStream in) throws IOException {
        character = in.readBoolean();
        super.deserialize(in);
    }

    public Thing(final boolean character) {
        this.character = character;
        table.rawset("Commands", new LuaTableImpl());
    }

    protected void setItem (final String key, final Object value) {
        if ("Commands".equals(key)) {
            // clear out existing actions
            for (final Action a : actions) {
                a.dissociateFromTargets();
            }
            actions.clear();

            // add new actions
            final LuaTable lt = (LuaTable)value;
            Object i = null;
            while ((i = lt.next(i)) != null) {
                final Action a = (Action)lt.rawget(i);
                //a.name = (String)i;
                if (i instanceof Double) a.name = BaseLib.numberToString((Double)i);
                else a.name = i.toString();
                a.setActor(this);
                actions.add(a);
                a.associateWithTargets();
            }
        } else super.setItem(key, value);
    }

    public int visibleActions() {
        int count = 0;
        for (final Action c : actions) {
            if (!c.isEnabled()) continue;
            if (c.getActor() == this || c.getActor().visibleToPlayer()) count++;
        }
        return count;
    }

    public boolean isItem() {
        return !character;
    }

    public boolean isCharacter() {
        return character;
    }
}
