/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig
 */
package cgeo.geocaching.wherigo.openwig;

import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;

import java.util.ArrayList;
import java.util.List;

public class Action extends EventTable {

    private boolean parameter;
    private boolean reciprocal = true;
    private boolean enabled;

    private Thing actor = null;
    private final List<Thing> targets = new ArrayList<>();
    private boolean universal;

    public String text;
    public String notarget;

    public Action () {
        // for serialization
    }

    public Action (final LuaTable table) {
        this.table = table; // XXX deep copy needed?
        Object o = null;
        while ((o = table.next(o)) != null) {
            if (o instanceof String) setItem((String)o, table.rawget(o));
        }
    }

    public void associateWithTargets () {
        if (!hasParameter()) return;
        if (isReciprocal()) {
            for (final Thing t : targets) {
                if (!t.actions.contains(this))
                    t.actions.add(this);
            }
        }
        if (isUniversal() && !Engine.instance.cartridge.universalActions.contains(this)) {
            Engine.instance.cartridge.universalActions.add(this);
        }
    }

    public void dissociateFromTargets () {
        if (!hasParameter()) return;
        if (isReciprocal()) {
            for (final Thing t : targets) {
                t.actions.remove(this);
            }
        }
        if (isUniversal()) {
            Engine.instance.cartridge.universalActions.remove(this);
        }
    }

    protected String luaTostring () { return "a ZCommand instance"; }

    protected void setItem (final String key, final Object value) {
        if ("Text".equals(key)) {
            text = (String)value;
        } else if ("CmdWith".equals(key)) {
            final boolean np = LuaState.boolEval(value);
            if (np != parameter) {
                if (np) {
                    parameter = true;
                    associateWithTargets();
                } else {
                    dissociateFromTargets();
                    parameter = false;
                }
            }
        } else if ("Enabled".equals(key)) {
            enabled = LuaState.boolEval(value);
        } else if ("WorksWithAll".equals(key)) {
            // XXX bug: when the command is dissociated and somebody updates this, it will re-associate
            dissociateFromTargets();
            universal = LuaState.boolEval(value);
            associateWithTargets();
        } else if ("WorksWithList".equals(key)) {
            dissociateFromTargets();
            final LuaTable lt = (LuaTable)value;
            Object i = null;
            while ((i = lt.next(i)) != null) {
                targets.add((Thing)lt.rawget(i));
            }
            associateWithTargets();
        } else if ("MakeReciprocal".equals(key)) {
            dissociateFromTargets();
            reciprocal = LuaState.boolEval(value);
            associateWithTargets();
        } else if ("EmptyTargetListText".equals(key)) {
            notarget = value == null ? "(not available now)" : value.toString();
        }
    }

    public int visibleTargets(final Container where) {
        int count = 0;
        Object key = null;
        while ((key = where.inventory.next(key)) != null) {
            final Object o = where.inventory.rawget(key);
            if (!(o instanceof Thing)) continue;
            final Thing t = (Thing)o;
            if (t.isVisible() && (targets.contains(t) || isUniversal())) count++;
        }
        return count;
    }

    public int targetsInside(final LuaTable v) {
        int count = 0;
        Object key = null;
        while ((key = v.next(key)) != null) {
            final Object o = v.rawget(key);
            if (!(o instanceof Thing)) continue;
            final Thing t = (Thing)o;
            if (t.isVisible() && (targets.contains(t) || isUniversal())) count++;
        }
        return count;
    }

    public boolean isTarget(final Thing t) {
        return targets.contains(t) || isUniversal();
    }

    public List<Thing> getTargets () {
        return targets;
    }

    public String getName() {
        return name;
    }

    public boolean hasParameter() {
        return parameter;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isUniversal() {
        return universal;
    }

    public void setActor (final Thing a) {
        actor = a;
    }

    public Thing getActor () {
        return actor;
    }

    public boolean isReciprocal () {
        return reciprocal;
    }
}
