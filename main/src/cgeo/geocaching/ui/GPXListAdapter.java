package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.cgeogpxes;
import cgeo.geocaching.files.GPXImporter;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.List;

public class GPXListAdapter extends ArrayAdapter<File> {
    private cgeogpxes activity = null;
    private LayoutInflater inflater = null;

    public GPXListAdapter(cgeogpxes parentIn, List<File> listIn) {
        super(parentIn, 0, listIn);

        activity = parentIn;
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

        final File file = getItem(position);

        View v = rowView;
        GPXListView holder;

        if (v == null) {
            v = inflater.inflate(R.layout.gpx_item, null);

            holder = new GPXListView();
            holder.filepath = (TextView) v.findViewById(R.id.filepath);
            holder.filename = (TextView) v.findViewById(R.id.filename);

            v.setTag(holder);
        } else {
            holder = (GPXListView) v.getTag();
        }

        v.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                (new GPXImporter(activity, activity.getListId(), null)).importGPX(file);
            }
        });

        holder.filepath.setText(file.getParent());
        holder.filename.setText(file.getName());

        return v;
    }
}
