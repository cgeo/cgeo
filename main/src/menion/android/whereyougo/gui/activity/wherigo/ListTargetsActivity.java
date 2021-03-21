/*
 * This file is part of WhereYouGo.
 *
 * WhereYouGo is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * WhereYouGo is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with WhereYouGo. If not,
 * see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2012 Menion <whereyougo@asamm.cz>
 */

package menion.android.whereyougo.gui.activity.wherigo;

import java.util.Vector;

import cz.matejcik.openwig.Action;
import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.Thing;
import cgeo.geocaching.R;
import menion.android.whereyougo.gui.activity.WhereYouGoActivity;
import menion.android.whereyougo.gui.utils.UtilsGUI;
import menion.android.whereyougo.openwig.WUI;
import se.krka.kahlua.vm.LuaTable;

public class ListTargetsActivity extends ListVariousActivity {

    // private static String title;
    private static Action action;
    private static Thing thing;

    private static Vector<Object> validStuff;

    private static void makeValidStuff() {
        LuaTable current = Engine.instance.cartridge.currentThings();
        // int size = current.len() + Engine.instance.player.inventory.len();
        validStuff = new Vector<>();
        Object key = null;
        while ((key = current.next(key)) != null)
            validStuff.addElement(current.rawget(key));
        while ((key = Engine.instance.player.inventory.next(key)) != null)
            validStuff.addElement(Engine.instance.player.inventory.rawget(key));

        for (int i = 0; i < validStuff.size(); i++) {
            Thing t = (Thing) validStuff.elementAt(i);
            if (!t.isVisible() || !action.isTarget(t)) {
                validStuff.removeElementAt(i--);
            }
        }
    }

    public static void reset(String title, Action what, Thing actor) {
        // ListTargets.title = title;
        ListTargetsActivity.action = what;
        ListTargetsActivity.thing = actor;
        makeValidStuff();
    }

    @Override
    protected void callStuff(Object what) {
        WhereYouGoActivity.wui.showScreen(WUI.DETAILSCREEN, DetailsActivity.et);
        String eventName = "On" + action.getName();
        Engine.callEvent(action.getActor(), eventName, what);
        ListTargetsActivity.this.finish();
    }

    @Override
    protected String getStuffName(Object what) {
        return ((Thing) what).name;
    }

    @Override
    protected Vector<Object> getValidStuff() {
        return validStuff;
    }

    public void refresh() {
        if (validStuff.isEmpty()) {
            UtilsGUI.showDialogInfo(this, R.string.no_target, (dialog, which) -> ListTargetsActivity.this.finish());
        } else {
            super.refresh();
        }
    }

    @Override
    protected boolean stillValid() {
        return thing.visibleToPlayer();
    }

}
