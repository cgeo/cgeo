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

import android.os.Bundle;

import java.util.Vector;

import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.Thing;
import menion.android.whereyougo.gui.activity.WhereYouGoActivity;
import menion.android.whereyougo.openwig.WUI;
import se.krka.kahlua.vm.LuaTable;

public class ListThingsActivity extends ListVariousActivity {

    public static final int INVENTORY = 0;
    public static final int SURROUNDINGS = 1;
    private int mode;

    @Override
    protected void callStuff(Object what) {
        Thing t = (Thing) what;
        if (t.hasEvent("OnClick")) {
            Engine.callEvent(t, "OnClick", null);
        } else {
            WhereYouGoActivity.wui.showScreen(WUI.DETAILSCREEN, t);
        }
        ListThingsActivity.this.finish();
    }

    @Override
    protected String getStuffName(Object what) {
        return ((Thing) what).name;
    }

    @Override
    protected Vector<Object> getValidStuff() {
        LuaTable container;
        if (mode == INVENTORY)
            container = Engine.instance.player.inventory;
        else
            container = Engine.instance.cartridge.currentThings();

        Vector<Object> newthings = new Vector<>();
        Object key = null;
        while ((key = container.next(key)) != null) {
            Thing t = (Thing) container.rawget(key);
            if (t.isVisible())
                newthings.add(t);
        }
        return newthings;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mode = getIntent().getIntExtra("mode", INVENTORY);
    }

    @Override
    protected boolean stillValid() {
        return true;
    }

    // TODO in TAB version
    // public boolean onKeyDown(int keyCode, KeyEvent event) {
    // if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
    // return getParent().onKeyDown(keyCode, event);
    // } else {
    // return super.onKeyDown(keyCode, event);
    // }
    // }
}
