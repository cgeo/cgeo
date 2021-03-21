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
import menion.android.whereyougo.gui.activity.WhereYouGoActivity;
import menion.android.whereyougo.openwig.WUI;

public class ListActionsActivity extends ListVariousActivity {

    private static Thing thing;

    public static void callAction(Action z) {
        String eventName = "On" + z.getName();

        if (z.hasParameter()) {
            if (z.getActor() == thing) {
                ListTargetsActivity.reset(thing.name + ": " + z.text, z, thing);
                WhereYouGoActivity.wui.showScreen(WUI.SCREEN_TARGETS, null);
            } else {
                // TODO necessary?
                // MainActivity.wui.showScreen(WUI.DETAILSCREEN, DetailsActivity.et);
                Engine.callEvent(z.getActor(), eventName, thing);
            }
        } else {
            // TODO necessary?
            // MainActivity.wui.showScreen(WUI.DETAILSCREEN, DetailsActivity.et);
            Engine.callEvent(thing, eventName, null);
        }
    }

    public static Vector<Object> getValidActions(Thing thing) {
        Vector<Object> newActions = new Vector<>();
        for (int i = 0; i < thing.actions.size(); i++)
            newActions.add(thing.actions.get(i));

        for (int i = 0; i < newActions.size(); i++) {
            Action a = (Action) newActions.elementAt(i);
            if (!a.isEnabled() || !a.getActor().visibleToPlayer()) {
                newActions.removeElementAt(i--);
            }
        }
        return newActions;
    }

    public static void reset(Thing what) {
        ListActionsActivity.thing = what;
    }

    @Override
    protected void callStuff(Object what) {
        Action z = (Action) what;
        callAction(z);
        ListActionsActivity.this.finish();
    }

    @Override
    protected String getStuffName(Object what) {
        Action a = (Action) what;
        if (a.getActor() == thing)
            return a.text;
        else
            return (String.format("%s: %s", a.getActor().name, a.text));
    }

    @Override
    protected Vector<Object> getValidStuff() {
        return getValidActions(thing);
    }

    @Override
    protected boolean stillValid() {
        return thing.visibleToPlayer() && thing.visibleActions() > 0;
    }

}
