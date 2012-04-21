package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.cgSelectMapfile;
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

public class MapfileListAdapter extends ArrayAdapter<File> {

    private cgSelectMapfile parentView;
    private LayoutInflater inflater;

    public MapfileListAdapter(cgSelectMapfile parentIn, List<File> listIn) {
        super(parentIn, 0, listIn);

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
        MapfileView holder;

        if (v == null) {
            v = inflater.inflate(R.layout.mapfile_item, null);

            holder = new MapfileView();
            holder.filepath = (TextView) v.findViewById(R.id.mapfilepath);
            holder.filename = (TextView) v.findViewById(R.id.mapfilename);

            v.setTag(holder);
        } else {
            holder = (MapfileView) v.getTag();
        }

        File current = new File(parentView.getCurrentMapfile());

        if (file.equals(current)) {
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
        public void onClick(View view) {
            parentView.setMapfile(file.toString());
            parentView.close();
        }
    }

    private static class MapfileView {
        public TextView filepath;
        public TextView filename;
    }
}
