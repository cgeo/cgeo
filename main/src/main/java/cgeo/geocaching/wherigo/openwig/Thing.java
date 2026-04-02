/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig
 */
package cgeo.geocaching.wherigo.openwig;

import cgeo.geocaching.wherigo.kahlua.stdlib.BaseLib;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Thing extends Container {

    private boolean character = false;

    protected String luaTostring () { return character ? "a ZCharacter instance" : "a ZItem instance"; }

    public List<Action> actions = new ArrayList<>();

    public Thing () {
        // for serialization
    }

    public void serialize (DataOutputStream out) throws IOException {
        out.writeBoolean(character);
        super.serialize(out);
    }

    public void deserialize (DataInputStream in) throws IOException {
        character = in.readBoolean();
        super.deserialize(in);
    }

    public Thing(boolean character) {
        this.character = character;
        table.rawset("Commands", new LuaTableImpl());
    }

    protected void setItem (String key, Object value) {
        if ("Commands".equals(key)) {
            // clear out existing actions
            for (final Action a : actions) {
                a.dissociateFromTargets();
            }
            actions.clear();

            // add new actions
            LuaTable lt = (LuaTable)value;
            Object i = null;
            while ((i = lt.next(i)) != null) {
                Action a = (Action)lt.rawget(i);
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
