package cgeo.geocaching.ui;

import butterknife.InjectView;

import cgeo.geocaching.R;
import cgeo.geocaching.files.IFileSelectionView;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.List;

public class FileSelectionListAdapter extends ArrayAdapter<File> {

    private final IFileSelectionView parentView;
    private final LayoutInflater inflater;

    public FileSelectionListAdapter(final IFileSelectionView parentIn, final List<File> listIn) {
        super(parentIn.getContext(), 0, listIn);

        parentView = parentIn;
        inflater = ((Activity) getContext()).getLayoutInflater();
    }

    @Override
    public View getView(final int position, final View rowView, final ViewGroup parent) {
        if (position > getCount()) {
            Log.w("FileSelectionListAdapter.getView: Attempt to access missing item #" + position);
            return null;
        }

        final File file = getItem(position);

        View v = rowView;

        ViewHolder holder;
        if (v == null) {
            v = inflater.inflate(R.layout.mapfile_item, parent, false);
            holder = new ViewHolder(v);
        } else {
            holder = (ViewHolder) v.getTag();
        }

        final String currentFile = parentView.getCurrentFile();
        if (currentFile != null && file.equals(new File(currentFile))) {
            holder.filename.setTypeface(holder.filename.getTypeface(), Typeface.BOLD);
        } else {
            holder.filename.setTypeface(holder.filename.getTypeface(), Typeface.NORMAL);
        }

        final TouchListener touchLst = new TouchListener(file);
        v.setOnClickListener(touchLst);

        holder.filepath.setText(file.getParent());
        holder.filename.setText(file.getName());

        return v;
    }

    private class TouchListener implements View.OnClickListener {
        private File file = null;

        public TouchListener(final File fileIn) {
            file = fileIn;
        }

        // tap on item
        @Override
        public void onClick(final View view) {
            parentView.setCurrentFile(file.toString());
            parentView.close();
        }
    }

    protected static final class ViewHolder extends AbstractViewHolder {
        @InjectView(R.id.mapfilepath) protected TextView filepath;
        @InjectView(R.id.mapfilename) protected TextView filename;

        public ViewHolder(final View view) {
            super(view);
        }
    }
}
