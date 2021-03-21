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

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Vector;

import cz.matejcik.openwig.Action;
import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.EventTable;
import cz.matejcik.openwig.Media;
import cz.matejcik.openwig.Task;
import cz.matejcik.openwig.Thing;
import cz.matejcik.openwig.Zone;
import cgeo.geocaching.R;
import menion.android.whereyougo.geo.location.ILocationEventListener;
import menion.android.whereyougo.geo.location.Location;
import menion.android.whereyougo.geo.location.LocationState;
import menion.android.whereyougo.geo.location.SatellitePosition;
import menion.android.whereyougo.gui.IRefreshable;
import menion.android.whereyougo.gui.activity.GuidingActivity;
import menion.android.whereyougo.gui.activity.WhereYouGoActivity;
import menion.android.whereyougo.gui.extension.activity.MediaActivity;
import menion.android.whereyougo.gui.extension.dialog.CustomDialog;
import menion.android.whereyougo.gui.utils.UtilsGUI;
import menion.android.whereyougo.gui.utils.UtilsWherigo;
import menion.android.whereyougo.guide.Guide;
import menion.android.whereyougo.openwig.WUI;
import menion.android.whereyougo.preferences.Locale;
import menion.android.whereyougo.utils.A;
import menion.android.whereyougo.utils.Logger;
import menion.android.whereyougo.utils.UtilsFormat;

// ADD locationListener to update UpdateNavi
public class DetailsActivity extends MediaActivity implements IRefreshable, ILocationEventListener {

    private static final String TAG = "Details";
    private static final String[] taskStates = {
            Locale.getString(R.string.pending),
            Locale.getString(R.string.finished),
            Locale.getString(R.string.failed)
    };
    public static EventTable et;
    private TextView tvName;
    private TextView tvDescription;
    private TextView tvDistance;
    private TextView tvState;

    private void enableGuideOnEventTable() {
        Location loc = UtilsWherigo.extractLocation(et);
        if (loc != null) {
            A.getGuidingContent().guideStart(new Guide(et.name, loc));
        } else {
            Logger.d(TAG, "enableGuideOnEventTable(), waypoint 'null'");
        }
    }

    @Override
    public String getName() {
        return TAG;
    }

    public int getPriority() {
        return ILocationEventListener.PRIORITY_MEDIUM;
    }

    @Override
    public boolean isRequired() {
        return false;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (A.getMain() == null || Engine.instance == null) {
            finish();
            return;
        }
        setContentView(R.layout.layout_details);
    }

    public void onGpsStatusChanged(int event, ArrayList<SatellitePosition> sats) {
    }

    public void onLocationChanged(Location location) {
        refresh();
    }

    public void onResume() {
        super.onResume();
        Logger.d(TAG, "onResume(), et:" + et);
        if (et != null) {
            setTitle(et.name);

            tvName = (TextView) findViewById(R.id.layoutDetailsTextViewName);
            tvState = (TextView) findViewById(R.id.layoutDetailsTextViewState);
            tvDescription = (TextView) findViewById(R.id.layoutDetailsTextViewDescription);
            tvDistance = (TextView) findViewById(R.id.layoutDetailsTextViewDistance);
        } else {
            Logger.i(TAG, "onCreate(), et == null, end!");
            DetailsActivity.this.finish();
        }

        refresh();
    }

    public void onStart() {
        super.onStart();
        if (et instanceof Zone)
            LocationState.addLocationChangeListener(this);
    }

    public void onStatusChanged(String provider, int state, Bundle extras) {
    }

    public void onStop() {
        super.onStop();
        LocationState.removeLocationChangeListener(this);
    }

    @Override
    public void refresh() {
        runOnUiThread(() -> {
            if (!stillValid()) {
                Logger.d(TAG, "refresh(), not valid anymore");
                DetailsActivity.this.finish();
                return;
            }

            tvName.setText(et.name);
            tvDescription.setText(UtilsGUI.simpleHtml(et.description));

            Media media = (Media) et.table.rawget("Media");
            setMedia(media);

            updateNavi();
            setBottomMenu();
        });
    }

    private void setBottomMenu() {
        String btn01 = null, btn02 = null, btn03 = null;
        CustomDialog.OnClickListener btn01Click = null, btn02Click = null, btn03Click = null;

        // get count of items
        boolean location = et.isLocated();

        int actions = 0;
        Vector<Object> validActions = null;

        if (et instanceof Thing) {
            Thing t = (Thing) et;
            actions = t.visibleActions() + Engine.instance.cartridge.visibleUniversalActions();
            Logger.d(TAG, "actions:" + actions);
            validActions = ListActionsActivity.getValidActions(t);
            actions = validActions.size();
            Logger.d(TAG, "validActions:" + actions);
        }

        Logger.d(TAG, "setBottomMenu(), loc:" + et.isLocated() + ", et:" + et + ", act:" + actions);

        // set location on first two buttons
        if (location) {
            btn01 = getString(R.string.navigate);
            btn01Click = (dialog, v, btn) -> {
                try {
                    enableGuideOnEventTable();
                    Intent intent = new Intent(DetailsActivity.this, GuidingActivity.class);
                    startActivity(intent);
                    // this was causing closing of another DetailsActivity, that was called in action
                    //DetailsActivity.this.finish();
                } catch (Exception e) {
                    Logger.w(TAG, "btn01.click() - unknown problem");
                }
                return true;
            };

            btn02 = getString(R.string.map);
            btn02Click = (dialog, v, btn) -> {
//                MapDataProvider mdp = MapHelper.getMapDataProvider();
//                mdp.clear();
//                mdp.addAll();
                WhereYouGoActivity.wui.showScreen(WUI.SCREEN_MAP, et);
                return true;
            };
        }

        // set actions
        if (actions > 0) {
            if (location) {
                // only one empty button, set actions on it
                btn03 = getString(R.string.actions_more, actions);
                btn03Click = (dialog, v, btn) -> {
                    ListActionsActivity.reset((Thing) et);
                    WhereYouGoActivity.wui.showScreen(WUI.SCREEN_ACTIONS, et);
                    // this was causing closing of another DetailsActivity, that was called in action
                    //DetailsActivity.this.finish();
                    return true;
                };
            } else {
                // all three buttons free
                if (actions <= 3) {
                    if (actions > 0) {
                        final Action action = (Action) validActions.get(0);
                        btn01 = action.text;
                        btn01Click = (dialog, v, btn) -> {
                            ListActionsActivity.reset((Thing) et);
                            ListActionsActivity.callAction(action);
                            // this was causing closing of another DetailsActivity, that was called in action
                            //DetailsActivity.this.finish();
                            return true;
                        };
                    }
                    if (actions > 1) {
                        final Action action = (Action) validActions.get(1);
                        btn02 = action.text;
                        btn02Click = (dialog, v, btn) -> {
                            ListActionsActivity.reset((Thing) et);
                            ListActionsActivity.callAction(action);
                            // this was causing closing of another DetailsActivity, that was called in action
                            //DetailsActivity.this.finish();
                            return true;
                        };
                    }
                    if (actions > 2) {
                        final Action action = (Action) validActions.get(2);
                        btn03 = action.text;
                        btn03Click = (dialog, v, btn) -> {
                            ListActionsActivity.reset((Thing) et);
                            ListActionsActivity.callAction(action);
                            // this was causing closing of another DetailsActivity, that was called in action
                            //DetailsActivity.this.finish();
                            return true;
                        };
                    }
                } else {
                    btn03 = getString(R.string.actions_more, actions);
                    btn03Click = (dialog, v, btn) -> {
                        ListActionsActivity.reset((Thing) et);
                        WhereYouGoActivity.wui.showScreen(WUI.SCREEN_ACTIONS, et);
                        // this was causing closing of another DetailsActivity, that was called in action
                        //DetailsActivity.this.finish();
                        return true;
                    };
                }
            }
        }

        // show bottom menu
        CustomDialog.setBottom(this, btn01, btn01Click, btn02, btn02Click, btn03, btn03Click);

        // set title text
        if (et instanceof Task) {
            Task t = (Task) et;
            tvState.setText(taskStates[t.state()]);
        }
    }

    public boolean stillValid() {
        if (et != null) {
            if (et instanceof Thing) {
                return ((Thing) et).visibleToPlayer();
            }
            return et.isVisible();
        } else
            return false;
    }

    private void updateNavi() {
        if (!(et instanceof Zone)) {
            return;
        }

        Zone z = (Zone) et;
        String ss = getString(R.string.zone_state_unknown);
        switch (z.contain) {
            case Zone.DISTANT:
                ss = getString(R.string.zone_state_distant);
                break;
            case Zone.PROXIMITY:
                ss = getString(R.string.zone_state_near);
                break;
            case Zone.INSIDE:
                ss = getString(R.string.zone_state_inside);
                break;
        }
        tvState.setText(getString(R.string.zone_state, ss));

        if (z.contain == Zone.INSIDE) {
            tvDistance.setText(getString(R.string.zone_distance, getString(R.string.zone_state_inside)));
        } else {
            tvDistance.setText(getString(R.string.zone_distance, UtilsFormat.formatDistance(z.distance, false)));
        }
    }
}
