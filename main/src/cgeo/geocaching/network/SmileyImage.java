package cgeo.geocaching.network;

import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.ImageUtils.LineHeightContainerDrawable;

import org.apache.commons.lang3.tuple.ImmutablePair;

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
    protected ImmutablePair<BitmapDrawable, Boolean> scaleImage(final ImmutablePair<Bitmap, Boolean> loadResult) {
        final Bitmap bitmap = loadResult.left;
        if (bitmap == null) {
            return ImmutablePair.of((BitmapDrawable) null, loadResult.right);
        }
        final BitmapDrawable drawable = new BitmapDrawable(view.getResources(), bitmap);
        drawable.setBounds(ImageUtils.scaleImageToLineHeight(drawable, view));
        return ImmutablePair.of(drawable, loadResult.right);
    }

    @Override
    protected BitmapDrawable getContainerDrawable(final Observable<BitmapDrawable> drawable) {
        return new LineHeightContainerDrawable(view, drawable);
    }

}
