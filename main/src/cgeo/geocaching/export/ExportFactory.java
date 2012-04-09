package cgeo.geocaching.export;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Factory to create a dialog with all available exporters.
 */
public class ExportFactory {
    /**
     * Contains instances of all available exporters.
     */
    public enum Exporters {
        FIELDNOTES(new FieldnoteExport()),
        GPX(new GpxExport());

        Exporters(Export exporter) {
            this.exporter = exporter;
        }

        public final Export exporter;
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
        builder.setTitle(R.string.export).setIcon(android.R.drawable.ic_menu_share);

        final ArrayAdapter<Exporters> adapter = new ArrayAdapter<Exporters>(activity, android.R.layout.select_dialog_item, Exporters.values()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setText(getItem(position).exporter.getName());
                return textView;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getDropDownView(position, convertView, parent);
                textView.setText(getItem(position).exporter.getName());
                return textView;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.select_dialog_item);

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                Exporters selectedItem = adapter.getItem(item);
                selectedItem.exporter.export(caches, activity);
            }
        });

        builder.create().show();
    }
}
