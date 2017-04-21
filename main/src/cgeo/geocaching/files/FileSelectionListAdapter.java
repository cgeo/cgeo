package cgeo.geocaching.files;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewAdapter;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;

import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import butterknife.BindView;

public class FileSelectionListAdapter extends AbstractRecyclerViewAdapter<FileSelectionListAdapter.ViewHolder> {

    private final IFileSelectionView parentView;
    @NonNull private final List<File> files;

    public FileSelectionListAdapter(@NonNull final IFileSelectionView parentIn, @NonNull final List<File> listIn) {
        files = listIn;
        parentView = parentIn;
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int position) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.mapfile_item, parent, false);
        final ViewHolder viewHolder = new ViewHolder(view);
        viewHolder.itemView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View view) {
                final File file = files.get(viewHolder.getItemPosition());
                parentView.setCurrentFile(file.toString());
                parentView.close();
            }
        });

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        super.onBindViewHolder(holder, position);
        final File file = files.get(position);

        final String currentFile = parentView.getCurrentFile();
        if (currentFile != null && file.equals(new File(currentFile))) {
            holder.filename.setTypeface(holder.filename.getTypeface(), Typeface.BOLD);
        } else {
            holder.filename.setTypeface(holder.filename.getTypeface(), Typeface.NORMAL);
        }

        holder.filepath.setText(file.getParent());
        holder.filename.setText(file.getName());
    }

    protected static final class ViewHolder extends AbstractRecyclerViewHolder {
        @BindView(R.id.mapfilepath) TextView filepath;
        @BindView(R.id.mapfilename) TextView filename;

        ViewHolder(final View view) {
            super(view);
        }
    }
}
