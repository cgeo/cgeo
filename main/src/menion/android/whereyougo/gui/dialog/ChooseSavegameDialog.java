/*
 * Copyright 2016 biylda <biylda@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package menion.android.whereyougo.gui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import cgeo.geocaching.R;
import menion.android.whereyougo.gui.activity.WhereYouGoActivity;
import menion.android.whereyougo.gui.extension.DataInfo;
import menion.android.whereyougo.gui.extension.dialog.CustomDialogFragment;
import menion.android.whereyougo.gui.utils.UtilsGUI;
import menion.android.whereyougo.openwig.WUI;
import menion.android.whereyougo.preferences.Preferences;
import menion.android.whereyougo.utils.A;
import menion.android.whereyougo.utils.FileSystem;
import menion.android.whereyougo.utils.UtilsFormat;

public class ChooseSavegameDialog extends CustomDialogFragment {

    private static final String SAVE_FILE = "SAVE_FILE";
    private static final String TAG = "ChooseSavegameDialog";
    private File saveFile;
    private ArrayList<DataInfo> data;
    private BaseAdapter adapter;

    public ChooseSavegameDialog() {
        super();
    }

    public static ChooseSavegameDialog newInstance(File saveFile) {
        ChooseSavegameDialog savegameDialog = new ChooseSavegameDialog();
        Bundle bundle = new Bundle();
        bundle.putString(SAVE_FILE, saveFile.getAbsolutePath());
        savegameDialog.setArguments(bundle);
        return savegameDialog;
    }

    private boolean restoreInstance(Bundle bundle) {
        if (bundle == null) {
            return false;
        }
        String saveFileName = bundle.getString(SAVE_FILE);
        if (saveFileName == null) {
            return false;
        }
        saveFile = new File(saveFileName);
        return true;
    }

    @Override
    public Dialog createDialog(Bundle savedInstanceState) {
        if (A.getMain() == null) {
            return null;
        }
        if (!(restoreInstance(savedInstanceState) || restoreInstance(getArguments()))) {
            return null;
        }

        // prepare list
        data = new ArrayList<>();
        data.add(new DataInfo(getString(R.string.save_file_new), ""));
        File file;
        if ((file = saveFile).exists()) {
            data.add(new DataInfo(getString(R.string.save_file_main), UtilsFormat.formatDatetime(file.lastModified()), file));
        }
        if ((file = new File(saveFile.getAbsolutePath() + ".bak")).exists()) {
            data.add(new DataInfo(getString(R.string.save_file_backup), UtilsFormat.formatDatetime(file.lastModified()), file));
        }
        for (int slot = 1; slot <= Preferences.GLOBAL_SAVEGAME_SLOTS; ++slot) {
            if ((file = new File(saveFile.getAbsolutePath() + "." + slot)).exists()) {
                data.add(new DataInfo(String.format("%s %d", getString(R.string.save_game_slot), slot), UtilsFormat.formatDatetime(file.lastModified()), file));
            }
        }

        // create listView
        ListView listView = UtilsGUI.createListView(getActivity(), false, data);
        // set click listener
        listView.setOnItemClickListener((parent, view, position, id) -> itemClicked(position));
        // set on long click listener for file deletion
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            itemLongClicked(position);
            return true;
        });
        adapter = (BaseAdapter) listView.getAdapter();
        // construct dialog
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.load_game)
                .setIcon(R.drawable.ic_title_logo)
                .setView(listView)
                .setNeutralButton(R.string.close, null)
                .create();
    }

    private void itemClicked(int position) {
        if (position == 0) {
            WhereYouGoActivity.wui.showScreen(WUI.SCREEN_CART_DETAIL, null);
            dismiss();
            return;
        }
        try {
            FileSystem.copyFile((File) data.remove(position).addData01, saveFile);
            WhereYouGoActivity.startSelectedCartridge(true);
            dismiss();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void itemLongClicked(final int position) {
        if (position == 0)
            return;
        UtilsGUI.showDialogQuestion(getActivity(), R.string.delete_save_game,
                (dialog, btn) -> {
                    ((File) data.remove(position).addData01).delete();
                    if (adapter != null)
                        adapter.notifyDataSetChanged();
                }, null);
    }

    public void setParams(File saveFile) {
        this.saveFile = saveFile;
    }
}
