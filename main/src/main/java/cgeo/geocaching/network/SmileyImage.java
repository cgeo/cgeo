package cgeo.geocaching.network;

import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.ImageUtils.LineHeightContainerDrawable;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.widget.TextView;

import com.drew.metadata.Metadata;
import io.reactivex.rxjava3.core.Observable;
import org.apache.commons.lang3.tuple.ImmutableTriple;

/**
 * Specialized image class for fetching and displaying smileys in the log book.
 */
public class SmileyImage extends HtmlImage {

    public SmileyImage(final String geocode, final TextView view) {
        super(geocode, false, false, view, false);
    }

    @Override
    protected ImmutableTriple<BitmapDrawable, Metadata, Boolean> scaleImage(final ImmutableTriple<Bitmap, Metadata, Boolean> loadResult) {
        final Bitmap bitmap = loadResult.left;
        final TextView view = viewRef.get();
        if (bitmap == null || view == null) {
            return ImmutableTriple.of(null, null, loadResult.right);
        }
        final BitmapDrawable drawable = new BitmapDrawable(view.getResources(), bitmap);
        drawable.setBounds(ImageUtils.scaleImageToLineHeight(drawable, view));
        return ImmutableTriple.of(drawable, null, loadResult.right);
    }

    @Override
    protected BitmapDrawable getContainerDrawable(final TextView view, final Observable<BitmapDrawable> drawable) {
        return new LineHeightContainerDrawable(view, drawable);
    }

}
