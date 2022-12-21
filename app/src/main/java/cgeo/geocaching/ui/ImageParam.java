package cgeo.geocaching.ui;

import cgeo.geocaching.utils.EmojiUtils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import java.util.Objects;

/**
 * Encapsulates an image object to be set to an ImageView.
 *
 * Supports setting this text from id, drawable or emoji
 *
 * Class is supposed to be used in parameters for View/Dialog helper methods dealing with images
 */
public class ImageParam {

    @DrawableRes
    private final int drawableId;
    private final int emojiSymbol;
    private final Drawable drawable;
    //if needed, then this can be extended e.g. with Icon or Bitmap


    /**
     * create from drawable resource id
     */
    public static ImageParam id(@DrawableRes final int drawableId) {
        return new ImageParam(drawableId, -1, null);
    }

    /**
     * create from emoji code
     */
    public static ImageParam emoji(final int emojiSymbol) {
        return new ImageParam(-1, emojiSymbol, null);
    }

    /**
     * create from emoji code
     */
    public static ImageParam drawable(final Drawable drawable) {
        return new ImageParam(-1, -1, drawable);
    }

    private ImageParam(@DrawableRes final int drawableId, final int emojiSymbol, @Nullable final Drawable drawable) {
        this.drawableId = drawableId;
        this.emojiSymbol = emojiSymbol;
        this.drawable = drawable;
    }

    public void apply(final ImageView view) {
        if (this.drawable != null) {
            view.setImageDrawable(this.drawable);
        } else if (this.drawableId > 0) {
            view.setImageResource(this.drawableId);
        } else if (this.emojiSymbol > 0) {
            final Pair<Integer, Integer> viewSize = ViewUtils.getViewSize(view);
            final int wantedSize = viewSize == null ? ViewUtils.dpToPixel(100) : Math.max(viewSize.first, viewSize.second);
            view.setImageDrawable(EmojiUtils.getEmojiDrawable(wantedSize, this.emojiSymbol));
        }
    }

    @NonNull
    public Drawable getAsDrawable(final Context context, final int sizeInDp) {
        if (this.drawable != null) {
            return this.drawable;
        }
        Drawable result = null;
        if (this.drawableId > 0) {
            result = ResourcesCompat.getDrawable(context.getResources(), drawableId, context.getTheme());

        } else if (this.emojiSymbol > 0) {
            result = EmojiUtils.getEmojiDrawable(ViewUtils.dpToPixel(sizeInDp), this.emojiSymbol);
        }
        if (result != null) {
            return result;
        }
        return Objects.requireNonNull(ResourcesCompat.getDrawable(context.getResources(), android.R.color.transparent, context.getTheme()));
    }

}
