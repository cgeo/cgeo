package cgeo.geocaching.ui;

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

    private IFileSelectionView parentView;
    private LayoutInflater inflater;

    public FileSelectionListAdapter(IFileSelectionView parentIn, List<File> listIn) {
        super(parentIn.getContext(), 0, listIn);

        parentView = parentIn;
    }

    @Override
    public View getView(final int position, final View rowView, final ViewGroup parent) {
        if (inflater == null) {
            inflater = ((Activity) getContext()).getLayoutInflater();
        }

        if (position > getCount()) {
            Log.w("cgGPXListAdapter.getView: Attempt to access missing item #" + position);
            return null;
        }

        File file = getItem(position);

        View v = rowView;

        ViewHolder holder;
        if (v == null) {
            v = inflater.inflate(R.layout.mapfile_item, null);

            holder = new ViewHolder();
            holder.filepath = (TextView) v.findViewById(R.id.mapfilepath);
            holder.filename = (TextView) v.findViewById(R.id.mapfilename);

            v.setTag(holder);
        } else {
            holder = (ViewHolder) v.getTag();
        }

        String currentFile = parentView.getCurrentFile();
        if (currentFile != null && file.equals(new File(currentFile))) {
            holder.filename.setTypeface(holder.filename.getTypeface(), Typeface.BOLD);
        } else {
            holder.filename.setTypeface(holder.filename.getTypeface(), Typeface.NORMAL);
        }

        final touchListener touchLst = new touchListener(file);
        v.setOnClickListener(touchLst);

        holder.filepath.setText(file.getParent());
        holder.filename.setText(file.getName());

        return v;
    }

    private class touchListener implements View.OnClickListener {
        private File file = null;

        public touchListener(File fileIn) {
            file = fileIn;
        }

        // tap on item
        @Override
        public void onClick(View view) {
            parentView.setCurrentFile(file.toString());
            parentView.close();
        }
    }

    private static final class ViewHolder {
        TextView filepath;
        TextView filename;
    }
}
