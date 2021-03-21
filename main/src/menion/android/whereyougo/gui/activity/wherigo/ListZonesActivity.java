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

import java.util.ArrayList;
import java.util.Vector;

import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.Zone;
import menion.android.whereyougo.geo.location.ILocationEventListener;
import menion.android.whereyougo.geo.location.Location;
import menion.android.whereyougo.geo.location.LocationState;
import menion.android.whereyougo.geo.location.SatellitePosition;
import menion.android.whereyougo.gui.activity.WhereYouGoActivity;
import menion.android.whereyougo.openwig.WUI;

public class ListZonesActivity extends ListVariousActivity implements ILocationEventListener {

    private static final String TAG = "ListZones";

    @Override
    protected void callStuff(Object what) {
        WhereYouGoActivity.wui.showScreen(WUI.DETAILSCREEN, (Zone) what);
        ListZonesActivity.this.finish();
    }

    @Override
    public String getName() {
        return TAG;
    }

    public int getPriority() {
        return ILocationEventListener.PRIORITY_MEDIUM;
    }

    @Override
    protected String getStuffName(Object what) {
        return ((Zone) what).name;
    }

    @Override
    protected Vector<Object> getValidStuff() {
        Vector<Object> ret = new Vector<>();
        @SuppressWarnings("unchecked")
        Vector<Zone> v = Engine.instance.cartridge.zones;
        for (int i = 0; i < v.size(); i++) {
            if (v.get(i).isVisible())
                ret.add(v.get(i));
        }
        return ret;
    }

    @Override
    public boolean isRequired() {
        return false;
    }


    public void onGpsStatusChanged(int event, ArrayList<SatellitePosition> sats) {
    }

    public void onLocationChanged(Location location) {
        refresh();
    }

    public void onStart() {
        super.onStart();
        LocationState.addLocationChangeListener(this);
    }

    public void onStatusChanged(String provider, int state, Bundle extras) {
    }

    public void onStop() {
        super.onStop();
        LocationState.removeLocationChangeListener(this);
    }

    @Override
    protected boolean stillValid() {
        return true;
    }
}
