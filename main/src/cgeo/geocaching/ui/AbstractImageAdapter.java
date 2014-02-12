package cgeo.geocaching.ui;

import cgeo.geocaching.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.List;

public abstract class AbstractImageAdapter extends BaseAdapter {
    private final Context context;
    // references to our images
    private final List<Integer> imageIds;
    private final int imageWidth;

    public AbstractImageAdapter(final Context context, final GridView gridView, final List<Integer> imageIds) {
        this.context = context;
        this.imageIds = imageIds;

        final Drawable drawable = context.getResources().getDrawable(imageIds.get(0));
        imageWidth = drawable.getIntrinsicWidth();

        // fix the column width, now that we know the images
        gridView.setColumnWidth(getImageWidth());
    }

    @Override
    public int getCount() {
        return imageIds.size();
    }

    @Override
    public Object getItem(final int position) {
        return null;
    }

    @Override
    public long getItemId(final int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) { // if it's not recycled, initialize some attributes
            imageView = (ImageView) LayoutInflater.from(context).inflate(R.layout.grid_image, null);
        } else {
            imageView = (ImageView) convertView;
        }

        imageView.setImageResource(imageIds.get(position));
        return imageView;
    }

    int getImageWidth() {
        return imageWidth;
    }

}