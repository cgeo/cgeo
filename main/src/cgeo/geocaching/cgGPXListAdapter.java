package cgeo.geocaching;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.List;

public class cgGPXListAdapter extends ArrayAdapter<File> {
    private cgGPXView holder = null;
    private cgeogpxes parent = null;
    private LayoutInflater inflater = null;

    public cgGPXListAdapter(cgeogpxes parentIn, List<File> listIn) {
        super(parentIn, 0, listIn);

        parent = parentIn;
    }

    @Override
    public View getView(final int position, final View rowView, final ViewGroup parent) {
        if (inflater == null)
            inflater = ((Activity) getContext()).getLayoutInflater();

        if (position > getCount()) {
            Log.w(Settings.tag, "cgGPXListAdapter.getView: Attempt to access missing item #" + position);
            return null;
        }

        File file = getItem(position);

        View v = rowView;

        if (v == null) {
            v = inflater.inflate(R.layout.gpx_item, null);

            holder = new cgGPXView();
            holder.filepath = (TextView) v.findViewById(R.id.filepath);
            holder.filename = (TextView) v.findViewById(R.id.filename);

            v.setTag(holder);
        } else {
            holder = (cgGPXView) v.getTag();
        }

        final touchListener touchLst = new touchListener(file);
        v.setOnClickListener(touchLst);

        holder.filepath.setText(file.getParent());
        holder.filename.setText(file.getName());

        return v;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    private class touchListener implements View.OnClickListener {
        private File file = null;

        public touchListener(File fileIn) {
            file = fileIn;
        }

        // tap on item
        @Override
        public void onClick(View view) {
            parent.loadGPX(file);
        }
    }
}
