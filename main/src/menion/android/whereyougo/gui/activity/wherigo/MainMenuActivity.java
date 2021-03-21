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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.Vector;

import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.Player;
import cz.matejcik.openwig.Task;
import cz.matejcik.openwig.Thing;
import cz.matejcik.openwig.Zone;
import cgeo.geocaching.R;
import menion.android.whereyougo.gui.IRefreshable;
import menion.android.whereyougo.gui.activity.WhereYouGoActivity;
import menion.android.whereyougo.gui.activity.SatelliteActivity;
import menion.android.whereyougo.gui.extension.DataInfo;
import menion.android.whereyougo.gui.extension.IconedListAdapter;
import menion.android.whereyougo.gui.extension.activity.CustomActivity;
import menion.android.whereyougo.gui.extension.dialog.CustomDialog;
import menion.android.whereyougo.gui.utils.UtilsGUI;
import menion.android.whereyougo.openwig.WUI;
import menion.android.whereyougo.preferences.Preferences;
import menion.android.whereyougo.utils.A;
import menion.android.whereyougo.utils.FileSystem;
import menion.android.whereyougo.utils.Logger;
import menion.android.whereyougo.utils.ManagerNotify;
import menion.android.whereyougo.utils.Utils;
import menion.android.whereyougo.utils.UtilsFormat;

public class MainMenuActivity extends CustomActivity implements IRefreshable {

    private static final String TAG = "CartridgeMainMenu";
    private static final int DOUBLE_PRESS_HK_BACK_PERIOD = 666;
    private AdapterView.OnItemClickListener listClick;
    private long lastPressedTime = 0;
    private MenuItem saveGameMainMenuItem;

    private String getVisibleCartridgeThingsDescription() {
        String description = null;
        @SuppressWarnings("unchecked")
        Vector<Zone> zones = Engine.instance.cartridge.zones;
        for (int i = 0; i < zones.size(); i++) {
            Zone z = zones.elementAt(i);
            String des = getVisibleThingsDescription(z);
            if (des != null) {
                if (description == null)
                    description = "";
                else
                    description += ", ";

                description += des;
            }
        }
        return description;
    }

    private String getVisiblePlayerThingsDescription() {
        Player p = Engine.instance.player;
        String description = null;
        Object key = null;
        while ((key = p.inventory.next(key)) != null) {
            Object o = p.inventory.rawget(key);
            if (o instanceof Thing && ((Thing) o).isVisible()) {
                if (description == null)
                    description = "";
                else
                    description += ", ";

                description += ((Thing) o).name;
            }
        }
        return description;
    }

    private int getVisibleTasksCount() {
        int count = 0;
        for (int i = 0; i < Engine.instance.cartridge.tasks.size(); i++) {
            Task a = (Task) Engine.instance.cartridge.tasks.elementAt(i);
            if (a.isVisible())
                count++;
        }
        return count;
    }

    private String getVisibleTasksDescription() {
        String description = null;
        for (int i = 0; i < Engine.instance.cartridge.tasks.size(); i++) {
            Task a = (Task) Engine.instance.cartridge.tasks.elementAt(i);
            if (a.isVisible()) {
                if (description == null)
                    description = "";
                else
                    description += ", ";
                description += a.name;
            }
        }
        return description;
    }


    /* SPECIAL ITEMS FUNCTIONS */

    private String getVisibleThingsDescription(Zone z) {
        String description = null;
        if (!z.showThings())
            return null;
        Object key = null;
        while ((key = z.inventory.next(key)) != null) {
            Object o = z.inventory.rawget(key);
            if (o instanceof Player)
                continue;
            if (!(o instanceof Thing))
                continue;
            if (((Thing) o).isVisible()) {
                if (description == null)
                    description = "";
                else
                    description += ", ";

                description += ((Thing) o).name;
            }
        }
        return description;
    }

    /***********************************/

    // private Vector<Zone> getVisibleZones() {
    // Vector<Zone> zones = Engine.instance.cartridge.zones;
    // Vector<Zone> visible = new Vector<Zone>();
    // for (int i = 0; i < zones.size(); i++) {
    // Zone z = (Zone) zones.get(i);
    // if (z.isVisible())
    // visible.add(z);
    // }
    // return visible;
    // }
    private String getVisibleZonesDescription() {
        String description = null;
        @SuppressWarnings("unchecked")
        Vector<Zone> zones = Engine.instance.cartridge.zones;
        for (int i = 0; i < zones.size(); i++) {
            Zone z = zones.get(i);
            if (z.isVisible()) {
                if (description == null)
                    description = "";
                else
                    description += ", ";

                description += z.name;
                if (z.contains(Engine.instance.player))
                    description += String.format(" (%s)", getString(R.string.zone_state_inside));
            }
        }
        return description;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (A.getMain() == null || Engine.instance == null) {
            finish();
            return;
        }
        setContentView(R.layout.custom_dialog);

        listClick = (parent, view, position, id) -> {
            Logger.d(TAG, "onItemClick:" + position);
            switch (position) {
                case 0:
                    if (Engine.instance.cartridge.visibleZones() >= 1) {
                        WhereYouGoActivity.wui.showScreen(WUI.LOCATIONSCREEN, null);
                    }
                    break;
                case 1:
                    if (Engine.instance.cartridge.visibleThings() >= 1) {
                        WhereYouGoActivity.wui.showScreen(WUI.ITEMSCREEN, null);
                    }
                    break;
                case 2:
                    if (Engine.instance.player.visibleThings() >= 1) {
                        WhereYouGoActivity.wui.showScreen(WUI.INVENTORYSCREEN, null);
                    }
                    break;
                case 3:
                    if (getVisibleTasksCount() > 0) {
                        WhereYouGoActivity.wui.showScreen(WUI.TASKSCREEN, null);
                    }
                    break;
            }
        };

        CustomDialog.setTitle(this, Engine.instance.cartridge.name, null, CustomDialog.NO_IMAGE, null);
        String saveGameText;
        CustomDialog.OnClickListener saveGameListener;
        if (Preferences.GLOBAL_SAVEGAME_SLOTS > 0) {
            saveGameText = getString(R.string.save_game_slots);
            saveGameListener = (dialog, v, btn) -> {
                openOptionsMenu();
                return true;
            };
        } else {
            saveGameText = getString(R.string.save_game);
            saveGameListener = (dialog, v, btn) -> {
                WhereYouGoActivity.wui.setOnSavingFinished(() -> {
                    ManagerNotify.toastShortMessage(MainMenuActivity.this, getString(R.string.save_game_ok));
                    WhereYouGoActivity.wui.setOnSavingFinished(null);
                });
                new SaveGame().execute();
                return true;
            };
        }
        CustomDialog.setBottom(this, getString(R.string.gps), (dialog, v, btn) -> {
            Intent intent = new Intent(MainMenuActivity.this, SatelliteActivity.class);
            startActivity(intent);
            return true;
        }, getString(R.string.map), (dialog, v, btn) -> {
//            MapDataProvider mdp = MapHelper.getMapDataProvider();
//            mdp.clear();
//            mdp.addAll();
            WhereYouGoActivity.wui.showScreen(WUI.SCREEN_MAP, null);
            return true;
        }, saveGameText, saveGameListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        try {
            File saveFile = WhereYouGoActivity.getSaveFile();
            {
                // main save
                String text;
                if (saveFile.exists()) {
                    text = String.format("%s: %s",
                            getString(R.string.save_file_main),
                            UtilsFormat.formatDatetime(saveFile.lastModified())
                    );
                } else {
                    text = String.format("%s",
                            getString(R.string.save_file_main)
                    );
                }
                saveGameMainMenuItem = menu.add(0, 0, Preferences.GLOBAL_SAVEGAME_SLOTS, text);
            }
            for (int slot = 1; slot <= Preferences.GLOBAL_SAVEGAME_SLOTS; ++slot) {
                File file = new File(saveFile.getAbsolutePath() + "." + slot);
                String text;
                if (file.exists()) {
                    text = String.format("%s %d: %s",
                            getString(R.string.save_game_slot),
                            slot,
                            UtilsFormat.formatDatetime(file.lastModified())
                    );
                } else {
                    text = String.format("%s %d",
                            getString(R.string.save_game_slot),
                            slot
                    );
                }
                menu.add(0, slot, Preferences.GLOBAL_SAVEGAME_SLOTS - slot, text);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getGroupId() != 0)
            return false;
        if (item.getItemId() == 0) {
            WhereYouGoActivity.wui.setOnSavingFinished(new Runnable() {
                @Override
                public void run() {
                    try {
                        File saveFile = WhereYouGoActivity.getSaveFile();
                        String title = String.format("%s: %s",
                                getString(R.string.save_file_main),
                                UtilsFormat.formatDatetime(saveFile.lastModified())
                        );
                        item.setTitle(title);
                        ManagerNotify.toastShortMessage(MainMenuActivity.this, getString(R.string.save_game_ok));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    WhereYouGoActivity.wui.setOnSavingFinished(null);
                }
            });
        } else {
            WhereYouGoActivity.wui.setOnSavingFinished(new Runnable() {
                @Override
                public void run() {
                    try {
                        File saveFile = WhereYouGoActivity.getSaveFile();
                        File slotFile = new File(saveFile.getAbsolutePath() + "." + item.getItemId());
                        FileSystem.copyFile(saveFile, slotFile);
                        String mainTitle = String.format("%s: %s",
                                getString(R.string.save_file_main),
                                UtilsFormat.formatDatetime(saveFile.lastModified())
                        );
                        saveGameMainMenuItem.setTitle(mainTitle);
                        String title = String.format("%s %d: %s",
                                getString(R.string.save_game_slot),
                                item.getItemId(),
                                UtilsFormat.formatDatetime(slotFile.lastModified())
                        );
                        item.setTitle(title);
                        String message = String.format("%s %d\n%s",
                                getString(R.string.save_game_slot),
                                item.getItemId(),
                                getText(R.string.save_game_ok)
                        );
                        ManagerNotify.toastShortMessage(MainMenuActivity.this, message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    WhereYouGoActivity.wui.setOnSavingFinished(null);
                }
            });
        }
        new SaveGame().execute();
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Logger.d(TAG, "onKeyDown(" + keyCode + ", " + event + ")");
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (!Preferences.GLOBAL_DOUBLE_CLICK || event.getDownTime() - lastPressedTime < DOUBLE_PRESS_HK_BACK_PERIOD) {
        /* exit game */
                UtilsGUI.showDialogQuestion(this, R.string.save_game_before_exit,
                        (dialog, which) -> {
                            new SaveGameOnExit().execute();
                            WhereYouGoActivity.selectedFile = null;
                            DetailsActivity.et = null;
                        }, (dialog, which) -> {
                            Engine.kill();
                            WhereYouGoActivity.selectedFile = null;
                            DetailsActivity.et = null;
                            MainMenuActivity.this.finish();
                        }, null);

                return true;
            } else {
        /* back is tapped once */
                lastPressedTime = event.getDownTime();
                ManagerNotify.toastShortMessage(R.string.msg_exit_game);
                return true;
            }
        } else
            return event.getKeyCode() == KeyEvent.KEYCODE_SEARCH || super.onKeyDown(keyCode, event);
    }

    public void onResume() {
        super.onResume();
        refresh();
    }

    public void refresh() {
        runOnUiThread(() -> {
            if (A.getMain() == null || Engine.instance == null || Engine.instance.cartridge == null) {
                return;
            }

            ArrayList<DataInfo> data = new ArrayList<>();
            DataInfo diLocations =
                    new DataInfo(getString(R.string.locations) + " ("
                            + Engine.instance.cartridge.visibleZones() + ")", getVisibleZonesDescription(),
                            R.drawable.icon_locations);
            data.add(diLocations);

            DataInfo diYouSee =
                    new DataInfo(getString(R.string.you_see) + " ("
                            + Engine.instance.cartridge.visibleThings() + ")",
                            getVisibleCartridgeThingsDescription(), R.drawable.icon_search);
            data.add(diYouSee);

            DataInfo diInventory =
                    new DataInfo(getString(R.string.inventory) + " ("
                            + Engine.instance.player.visibleThings() + ")",
                            getVisiblePlayerThingsDescription(), R.drawable.icon_inventory);
            data.add(diInventory);

            DataInfo diTasks =
                    new DataInfo(getString(R.string.tasks) + " ("
                            + Engine.instance.cartridge.visibleTasks() + ")", getVisibleTasksDescription(),
                            R.drawable.icon_tasks);
            data.add(diTasks);

            ListView lv = new ListView(MainMenuActivity.this);
            IconedListAdapter adapter = new IconedListAdapter(MainMenuActivity.this, data, lv);
            adapter.setMinHeight((int) Utils.getDpPixels(70));
            adapter.setTextView02Visible(View.VISIBLE, true);
            lv.setAdapter(adapter);
            lv.setOnItemClickListener(listClick);
            CustomDialog.setContent(MainMenuActivity.this, lv, 0, true, false);
        });
    }

    private class SaveGame extends menion.android.whereyougo.gui.SaveGame {
        public SaveGame() {
            super(MainMenuActivity.this);
        }
    }

    private class SaveGameOnExit extends SaveGame {

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            Engine.kill();
            MainMenuActivity.this.finish();
        }
    }
}
