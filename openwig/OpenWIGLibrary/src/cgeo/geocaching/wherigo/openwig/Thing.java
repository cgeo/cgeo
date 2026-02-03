package cgeo.geocaching.wherigo.openwig;

import java.io.*;
import java.util.Vector;

import cgeo.geocaching.wherigo.openwig.kahlua.stdlib.BaseLib;
import cgeo.geocaching.wherigo.openwig.kahlua.vm.*;

public class Thing extends Container {
    
    private boolean character = false;

    protected String luaTostring () { return character ? "a ZCharacter instance" : "a ZItem instance"; }
    
    public Vector actions = new Vector();

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
            for (int i = 0; i < actions.size(); i++) {
                Action a = (Action) actions.elementAt(i);
                a.dissociateFromTargets();
            }
            actions.removeAllElements();

            // add new actions
            LuaTable lt = (LuaTable) value;
            Object i = null;
            while ((i = lt.next(i)) != null) {
                Action a = (Action) lt.rawget(i);
                //a.name = (String) i;
                if (i instanceof Double) a.name = BaseLib.numberToString((Double) i);
                else a.name = i.toString();
                a.setActor(this);
                actions.addElement(a);
                a.associateWithTargets();
            }
        } else super.setItem(key, value);
    }
    
    public int visibleActions() {
        int count = 0;
        for (int i = 0; i < actions.size(); i++) {
            Action c = (Action) actions.elementAt(i);
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
