package cgeo.geocaching.export;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Factory to create a dialog with all available exporters.
 */
public abstract class ExportFactory {

    /**
     * Contains instances of all available exporter classes.
     */
    private static final List<Class<? extends Export>> exporterClasses;

    static {
        final ArrayList<Class<? extends Export>> temp = new ArrayList<Class<? extends Export>>();
        temp.add(FieldnoteExport.class);
        temp.add(GpxExport.class);
        exporterClasses = Collections.unmodifiableList(temp);
    }

    /**
     * Creates a dialog so that the user can select an exporter.
     *
     * @param caches
     *            The {@link List} of {@link cgCache} to be exported
     * @param activity
     *            The {@link Activity} in whose context the dialog should be shown
     */
    public static void showExportMenu(final List<cgCache> caches, final Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.export).setIcon(R.drawable.ic_menu_share);

        final ArrayList<Export> export = new ArrayList<Export>();
        for (Class<? extends Export> exporterClass : exporterClasses) {
            try {
                export.add(exporterClass.newInstance());
            } catch (Exception ex) {
                Log.e("showExportMenu", ex);
            }
        }

        final ArrayAdapter<Export> adapter = new ArrayAdapter<Export>(activity, android.R.layout.select_dialog_item, export);

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                final Export selectedExport = adapter.getItem(item);
                selectedExport.export(caches, activity);
            }
        });

        builder.create().show();
    }
}