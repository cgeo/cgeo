package cgeo.geocaching.ui;

import butterknife.InjectView;

import cgeo.geocaching.GpxFileListActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.files.GPXImporter;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.List;

public class GPXListAdapter extends ArrayAdapter<File> {
    private final GpxFileListActivity activity;
    private final LayoutInflater inflater;

    protected static class ViewHolder extends AbstractViewHolder {
        @InjectView(R.id.filepath) protected TextView filepath;
        @InjectView(R.id.filename) protected TextView filename;

        public ViewHolder(View view) {
            super(view);
        }
    }

    public GPXListAdapter(GpxFileListActivity parentIn, List<File> listIn) {
        super(parentIn, 0, listIn);

        activity = parentIn;
        inflater = ((Activity) getContext()).getLayoutInflater();
    }

    @Override
    public View getView(final int position, final View rowView, final ViewGroup parent) {
        if (position > getCount()) {
            Log.w("GPXListAdapter.getView: Attempt to access missing item #" + position);
            return null;
        }

        final File file = getItem(position);

        View view = rowView;

        final ViewHolder holder;
        if (view == null) {
            view = inflater.inflate(R.layout.gpx_item, null);
            holder = new ViewHolder(view);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        view.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                (new GPXImporter(activity, activity.getListId(), null)).importGPX(file);
            }
        });

        holder.filepath.setText(file.getParent());
        holder.filename.setText(file.getName());

        view.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(R.string.gpx_import_delete_title)
                        .setMessage(activity.getString(R.string.gpx_import_delete_message, file.getName()))
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                FileUtils.deleteIgnoringFailure(file);
                                GPXListAdapter.this.remove(file);
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
                return true;
            }
        });

        return view;
    }
}
