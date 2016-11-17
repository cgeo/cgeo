package cgeo.geocaching.files;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;
import cgeo.geocaching.utils.FileUtils;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import butterknife.BindView;

class GPXListAdapter extends RecyclerView.Adapter<GPXListAdapter.ViewHolder> {
    private final GpxFileListActivity activity;
    @NonNull private final List<File> files;

    protected static final class ViewHolder extends AbstractRecyclerViewHolder {
        @BindView(R.id.filepath) protected TextView filepath;
        @BindView(R.id.filename) protected TextView filename;

        ViewHolder(final View view) {
            super(view);
        }
    }

    GPXListAdapter(final GpxFileListActivity parentIn, final List<File> listIn) {
        files = listIn;
        activity = parentIn;
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int position) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.gpx_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final File file = files.get(position);

        holder.itemView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                (new GPXImporter(activity, activity.getListId(), null)).importGPX(file);
            }
        });

        holder.filepath.setText(file.getParent());
        holder.filename.setText(file.getName());

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(final View v) {
                Dialogs.confirmYesNo(activity, R.string.gpx_import_delete_title, activity.getString(R.string.gpx_import_delete_message, file.getName()), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int id) {
                        final int currentPosition = holder.getAdapterPosition();
                        if (currentPosition != RecyclerView.NO_POSITION) {
                            FileUtils.deleteIgnoringFailure(file);
                            files.remove(currentPosition);
                            notifyItemRemoved(currentPosition);
                        }
                    }
                });
                return true;
            }
        });
    }

}
