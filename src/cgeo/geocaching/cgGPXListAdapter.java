package cgeo.geocaching;

import java.io.File;
import java.util.List;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class cgGPXListAdapter extends ArrayAdapter<File> {
	private cgGPXView holder = null;
	private cgeogpxes parent = null;
	private LayoutInflater inflater = null;

	public cgGPXListAdapter(cgeogpxes parentIn, cgSettings settingsIn, List<File> listIn) {
		super(parentIn, 0, listIn);

		parent = parentIn;
	}

    @Override
    public View getView(int position, View rowView, ViewGroup parent) {
		if (inflater == null) inflater = ((Activity)getContext()).getLayoutInflater();

		if (position > getCount()) {
			Log.d(cgSettings.tag, "cgGPXListAdapter.getView: Attempt to access missing item #" + position);
			return null;
		}

		File file = getItem(position);

		if (rowView == null) {
			rowView = (View)inflater.inflate(R.layout.gpx_item, null);

			holder = new cgGPXView();
			holder.filepath = (TextView)rowView.findViewById(R.id.filepath);
			holder.filename = (TextView)rowView.findViewById(R.id.filename);

			rowView.setTag(holder);
		} else {
			holder = (cgGPXView)rowView.getTag();
		}

		final touchListener touchLst = new touchListener(file);
		rowView.setOnClickListener(touchLst);

		holder.filepath.setText(file.getParent());
		holder.filename.setText(file.getName());

		return rowView;
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
		public void onClick(View view) {
			parent.loadGPX(file);
		}
	}
}
