package cgeo.geocaching.files;

import cgeo.geocaching.R;
import cgeo.geocaching.cgeogpxes;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.ArrayAdapter;

import java.util.ArrayList;

public abstract class GpxImportChooser {
    /**
     * Contains instances of all available exporter classes.
     */
    private static final GpxScanChoice[] importChoices = { GpxScanChoice.FORCE_SCAN, GpxScanChoice.DEFAULT_DIR };

    /**
     * Creates a dialog so that the user can choose to scan all directories or use the default dir only.
     *
     * @param activity
     *            The {@link Activity} in whose context the dialog should be shown
     * @param listId
     *            The listId chosen for import
     */
    public static void showImportMenu(final Activity activity, final int listId) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.gpx_import_scan_choice).setIcon(R.drawable.ic_menu_share);

        final ArrayList<GpxScanChoice> menuChoices = new ArrayList<GpxScanChoice>();
        for (GpxScanChoice importChoice : importChoices) {
            try {
                menuChoices.add(importChoice);
            } catch (Exception ex) {
                Log.e("showImportMenu", ex);
            }
        }

        final ArrayAdapter<GpxScanChoice> adapter = new ArrayAdapter<GpxScanChoice>(activity, android.R.layout.select_dialog_item, menuChoices);

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                final GpxScanChoice selectedImport = adapter.getItem(item);
                cgeogpxes.startSubActivity(activity, listId, selectedImport);
            }
        });

        builder.create().show();
    }

}
