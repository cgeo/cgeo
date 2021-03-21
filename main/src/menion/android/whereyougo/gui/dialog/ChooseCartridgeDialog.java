package menion.android.whereyougo.gui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import cz.matejcik.openwig.formats.CartridgeFile;
import cgeo.geocaching.R;
import menion.android.whereyougo.geo.location.Location;
import menion.android.whereyougo.geo.location.LocationState;
import menion.android.whereyougo.gui.activity.WhereYouGoActivity;
import menion.android.whereyougo.gui.extension.DataInfo;
import menion.android.whereyougo.gui.extension.dialog.CustomDialogFragment;
import menion.android.whereyougo.gui.utils.UtilsGUI;
import menion.android.whereyougo.utils.A;
import menion.android.whereyougo.utils.FileSystem;
import menion.android.whereyougo.utils.Images;
import menion.android.whereyougo.utils.Logger;

public class ChooseCartridgeDialog extends CustomDialogFragment {

    private static final String TAG = "ChooseCartridgeDialog";
    private ArrayList<DataInfo> data;
    private BaseAdapter adapter;
    private Vector<CartridgeFile> cartridgeFiles;

    public ChooseCartridgeDialog() {
        super();
    }

    @Override
    public Dialog createDialog(Bundle savedInstanceState) {
        if (A.getMain() == null || cartridgeFiles == null) {
            return null;
        }
        try {
            // sort cartridges
            final Location actLoc = LocationState.getLocation();
            final Location loc1 = new Location(TAG);
            final Location loc2 = new Location(TAG);
            Collections.sort(cartridgeFiles, (object1, object2) -> {
                loc1.setLatitude(object1.latitude);
                loc1.setLongitude(object1.longitude);
                loc2.setLatitude(object2.latitude);
                loc2.setLongitude(object2.longitude);
                return (int) (actLoc.distanceTo(loc1) - actLoc.distanceTo(loc2));
            });

            // prepare list
            data = new ArrayList<>();
            for (int i = 0; i < cartridgeFiles.size(); i++) {
                CartridgeFile file = cartridgeFiles.get(i);
                byte[] iconData = file.getFile(file.iconId);
                Bitmap icon;
                try {
                    icon = BitmapFactory.decodeByteArray(iconData, 0, iconData.length);
                } catch (Exception e) {
                    icon = Images.getImageB(R.drawable.icon_gc_wherigo);
                }

                DataInfo di =
                        new DataInfo(file.name, file.type + ", " + file.author + ", " + file.version, icon);
                di.value01 = file.latitude;
                di.value02 = file.longitude;
                di.setDistAzi(actLoc);
                data.add(di);
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
                    .setTitle(R.string.choose_cartridge)
                    .setIcon(R.drawable.ic_title_logo)
                    .setView(listView)
                    .setNeutralButton(R.string.close, null)
                    .create();
        } catch (Exception e) {
            Logger.e(TAG, "createDialog()", e);
        }
        return null;
    }

    private void itemClicked(int position) {
        try {
            WhereYouGoActivity.openCartridge(cartridgeFiles.get(position));
        } catch (Exception e) {
            Logger.e(TAG, "onCreate()", e);
        }
        dismiss();
    }

    private void itemLongClicked(final int position) {
        try {
            final CartridgeFile cartridgeFile = cartridgeFiles.get(position);
            final String filename = cartridgeFile.filename.substring(0,
                    cartridgeFile.filename.length() - 3);

            UtilsGUI.showDialogQuestion(getActivity(), R.string.delete_cartridge,
                    (dialog, btn) -> {
                        File[] files = FileSystem.getFiles2(
                                new File(cartridgeFile.filename).getParent(),
                                pathname -> pathname.getAbsolutePath().startsWith(filename)
                        );
                        for (File file : files) {
                            file.delete();
                        }
                        WhereYouGoActivity.refreshCartridges();
                        cartridgeFiles.remove(position);
                        data.remove(position);
                        if (adapter != null)
                            adapter.notifyDataSetChanged();
                    }, null);
        } catch (Exception e) {
            Logger.e(TAG, "onCreate()", e);
        }
    }

    public void setParams(Vector<CartridgeFile> cartridgeFiles) {
        this.cartridgeFiles = cartridgeFiles;
    }
}
