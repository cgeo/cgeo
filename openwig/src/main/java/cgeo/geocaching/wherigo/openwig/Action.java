/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig
 */
package cgeo.geocaching.wherigo.openwig;

import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;

import java.util.Vector;

/**
 * Represents a command or action that can be executed in a Wherigo game.
 * <p>
 * Action extends EventTable to provide interactive commands (ZCommands) that
 * players can execute on game objects. Actions can require parameters (target
 * objects) and can be enabled or disabled based on game state.
 * <p>
 * Key features:
 * <ul>
 * <li>Can be associated with Things (actor) and require target Things (parameter)</li>
 * <li>Supports reciprocal actions - shown on both actor and target objects</li>
 * <li>Supports universal actions - available from anywhere in the game</li>
 * <li>Can be enabled/disabled dynamically based on game state</li>
 * <li>Executes OnClick event when activated by player</li>
 * <li>Provides text descriptions and "no target" messages</li>
 * </ul>
 * <p>
 * Actions are used for object interactions like "Open box", "Talk to character",
 * "Use key on door", etc. They form the primary interaction mechanism in Wherigo.
 */
public class Action extends EventTable {

    private boolean parameter;
    private boolean reciprocal = true;
    private boolean enabled;

    private Thing actor = null;
    private Vector<Thing> targets = new Vector<>();
    private boolean universal;

    public String text;
    public String notarget;

    public Action () {
        // for serialization
    }

    public Action (final LuaTable table) {
        // Copy table contents to this EventTable
        Object o = null;
        while ((o = table.next(o)) != null) {
            if (o instanceof String s) {
                setItem(s, table.rawget(o));
            }
        }
    }

    public void associateWithTargets () {
        if (!hasParameter()) return;
        if (isReciprocal()) {
            for (int j = 0; j < targets.size(); j++) {
                Thing t = targets.elementAt(j);
                if (!t.actions.contains(this))
                    t.actions.addElement(this);
            }
        }
        Engine currentEngine = Engine.getCurrentInstance();
        if (isUniversal() && currentEngine != null && !currentEngine.cartridge.universalActions.contains(this)) {
            currentEngine.cartridge.universalActions.addElement(this);
        }
    }

    public void dissociateFromTargets () {
        if (!hasParameter()) return;
        if (isReciprocal()) {
            for (int j = 0; j < targets.size(); j++) {
                Thing t = targets.elementAt(j);
                t.actions.removeElement(this);
            }
        }
        if (isUniversal()) {
            Engine currentEngine = Engine.getCurrentInstance();
            if (currentEngine != null) {
                currentEngine.cartridge.universalActions.removeElement(this);
            }
        }
    }

    protected String luaTostring () { return "a ZCommand instance"; }

    protected void setItem (String key, Object value) {
        if ("Text".equals(key)) {
            text = (String)value;
        } else if ("CmdWith".equals(key)) {
            boolean np = LuaState.boolEval(value);
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
            LuaTable lt = (LuaTable)value;
            Object i = null;
            while ((i = lt.next(i)) != null) {
                targets.addElement(lt.rawget(i));
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

    public int visibleTargets(Container where) {
        int count = 0;
        Object key = null;
        while ((key = where.inventory.next(key)) != null) {
            Object o = where.inventory.rawget(key);
            if (!(o instanceof Thing t)) continue;
            if (t.isVisible() && (targets.contains(t) || isUniversal())) count++;
        }
        return count;
    }

    public int targetsInside(LuaTable v) {
        int count = 0;
        Object key = null;
        while ((key = v.next(key)) != null) {
            Object o = v.rawget(key);
            if (!(o instanceof Thing t)) continue;
            if (t.isVisible() && (targets.contains(t) || isUniversal())) count++;
        }
        return count;
    }

    public boolean isTarget(Thing t) {
        return targets.contains(t) || isUniversal();
    }

    public Vector<Thing> getTargets () {
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

    public void setActor (Thing a) {
        actor = a;
    }

    public Thing getActor () {
        return actor;
    }

    public boolean isReciprocal () {
        return reciprocal;
    }
}
