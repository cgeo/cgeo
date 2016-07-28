package cgeo.geocaching.staticmaps;

import cgeo.geocaching.R;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

class StaticMapsAdapter extends ArrayAdapter<Bitmap> {

    private final LayoutInflater inflater;

    StaticMapsAdapter(final Activity context) {
        super(context, 0);
        inflater = context.getLayoutInflater();
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final Bitmap bitmap = getItem(position);

        ImageView view = (ImageView) convertView;

        // holder pattern implementation
        if (view == null) {
            view = (ImageView) inflater.inflate(R.layout.staticmaps_activity_item, parent, false);
        }

        view.setImageBitmap(bitmap);

        return view;
    }

}
