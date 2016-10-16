package cgeo.geocaching.files;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;

import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import butterknife.BindView;

public class FileSelectionListAdapter extends RecyclerView.Adapter<FileSelectionListAdapter.ViewHolder> {

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
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final File file = files.get(position);

        final String currentFile = parentView.getCurrentFile();
        if (currentFile != null && file.equals(new File(currentFile))) {
            holder.filename.setTypeface(holder.filename.getTypeface(), Typeface.BOLD);
        } else {
            holder.filename.setTypeface(holder.filename.getTypeface(), Typeface.NORMAL);
        }

        final TouchListener touchLst = new TouchListener(file);
        holder.itemView.setOnClickListener(touchLst);

        holder.filepath.setText(file.getParent());
        holder.filename.setText(file.getName());
    }

    private class TouchListener implements View.OnClickListener {
        private File file = null;

        TouchListener(final File fileIn) {
            file = fileIn;
        }

        // tap on item
        @Override
        public void onClick(final View view) {
            parentView.setCurrentFile(file.toString());
            parentView.close();
        }
    }

    protected static final class ViewHolder extends AbstractRecyclerViewHolder {
        @BindView(R.id.mapfilepath) TextView filepath;
        @BindView(R.id.mapfilename) TextView filename;

        ViewHolder(final View view) {
            super(view);
        }
    }
}
