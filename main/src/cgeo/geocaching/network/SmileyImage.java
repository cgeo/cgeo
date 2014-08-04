package cgeo.geocaching.network;

import cgeo.geocaching.list.StoredList;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.widget.TextView;

public class SmileyImage extends HtmlImage {

    public SmileyImage(final String geocode, final TextView view) {
        super(geocode, false, StoredList.STANDARD_LIST_ID, false, view);
    }

    @Override
    protected Pair<BitmapDrawable, Boolean> scaleImage(final Pair<Bitmap, Boolean> loadResult) {
        final Bitmap bitmap = loadResult.getLeft();
        BitmapDrawable drawable;
        if (bitmap != null) {
            final int lineHeight = (int) (view.getLineHeight() * 0.8);
            drawable = new BitmapDrawable(view.getResources(), bitmap);
            final int width = drawable.getIntrinsicWidth() * lineHeight / drawable.getIntrinsicHeight();
            drawable.setBounds(0, 0, width, lineHeight);
        }
        else {
            drawable = null;
        }
        return new ImmutablePair<>(drawable, loadResult.getRight());
    }

}
