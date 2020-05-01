package cgeo.geocaching.network;

import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.ImageUtils.LineHeightContainerDrawable;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.widget.TextView;

import io.reactivex.rxjava3.core.Observable;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Specialized image class for fetching and displaying smileys in the log book.
 */
public class SmileyImage extends HtmlImage {

    public SmileyImage(final String geocode, final TextView view) {
        super(geocode, false, false, view, false);
    }

    @Override
    protected ImmutablePair<BitmapDrawable, Boolean> scaleImage(final ImmutablePair<Bitmap, Boolean> loadResult) {
        final Bitmap bitmap = loadResult.left;
        if (bitmap == null) {
            return ImmutablePair.of(null, loadResult.right);
        }
        final TextView view = viewRef.get();
        final BitmapDrawable drawable = new BitmapDrawable(view.getResources(), bitmap);
        drawable.setBounds(ImageUtils.scaleImageToLineHeight(drawable, view));
        return ImmutablePair.of(drawable, loadResult.right);
    }

    @Override
    protected BitmapDrawable getContainerDrawable(final TextView view, final Observable<BitmapDrawable> drawable) {
        return new LineHeightContainerDrawable(view, drawable);
    }

}
