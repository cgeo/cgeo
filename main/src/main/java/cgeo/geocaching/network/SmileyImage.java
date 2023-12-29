package cgeo.geocaching.network;

import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.ImageUtils.LineHeightContainerDrawable;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.widget.TextView;

import io.reactivex.rxjava3.core.Observable;

/**
 * Specialized image class for fetching and displaying smileys in the log book.
 */
public class SmileyImage extends HtmlImage {

    public SmileyImage(final String geocode, final TextView view) {
        super(geocode, false, false, view, false);
    }

    @Override
    protected BitmapDrawable scaleImage(final Bitmap bitmap) {
        final TextView view = viewRef.get();
        if (bitmap == null || view == null) {
            return null;
        }
        final BitmapDrawable drawable = new BitmapDrawable(view.getResources(), bitmap);
        drawable.setBounds(ImageUtils.scaleImageToLineHeight(drawable, view));
        return drawable;
    }

    @Override
    protected BitmapDrawable getContainerDrawable(final TextView view, final Observable<BitmapDrawable> drawable) {
        return new LineHeightContainerDrawable(view, drawable);
    }

}
