package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgeogpxes;
import cgeo.geocaching.R.id;
import cgeo.geocaching.R.layout;
import cgeo.geocaching.files.GPXImporter;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.List;

public class GPXListAdapter extends ArrayAdapter<File> {
    private GPXView holder = null;
    private cgeogpxes parent = null;
    private LayoutInflater inflater = null;

    public GPXListAdapter(cgeogpxes parentIn, List<File> listIn) {
        super(parentIn, 0, listIn);

        parent = parentIn;
    }

    @Override
    public View getView(final int position, final View rowView, final ViewGroup parent) {
        if (inflater == null) {
            inflater = ((Activity) getContext()).getLayoutInflater();
        }

        if (position > getCount()) {
            Log.w(Settings.tag, "cgGPXListAdapter.getView: Attempt to access missing item #" + position);
            return null;
        }

        File file = getItem(position);

        View v = rowView;

        if (v == null) {
            v = inflater.inflate(R.layout.gpx_item, null);

            holder = new GPXView();
            holder.filepath = (TextView) v.findViewById(R.id.filepath);
            holder.filename = (TextView) v.findViewById(R.id.filename);

            v.setTag(holder);
        } else {
            holder = (GPXView) v.getTag();
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
            (new GPXImporter(parent, parent.getListId(), null)).importGPX(file);
        }
    }
}
