package cgeo.geocaching.network;

import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.ImageUtils.LineHeightContainerDrawable;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import rx.Observable;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.widget.TextView;

/**
 * Specialized image class for fetching and displaying smileys in the log book.
 */
public class SmileyImage extends HtmlImage {

    public SmileyImage(final String geocode, final TextView view) {
        super(geocode, false, StoredList.STANDARD_LIST_ID, false, view);
    }

    @Override
    protected Pair<BitmapDrawable, Boolean> scaleImage(final Pair<Bitmap, Boolean> loadResult) {
        final Bitmap bitmap = loadResult.getLeft();
        BitmapDrawable drawable;
        if (bitmap != null) {
            drawable = new BitmapDrawable(view.getResources(), bitmap);
            drawable.setBounds(ImageUtils.scaleImageToLineHeight(drawable, view));
        }
        else {
            drawable = null;
        }
        return new ImmutablePair<>(drawable, loadResult.getRight());
    }

    @Override
    protected BitmapDrawable getContainerDrawable(final Observable<BitmapDrawable> drawable) {
        return new LineHeightContainerDrawable(view, drawable);
    }

}
