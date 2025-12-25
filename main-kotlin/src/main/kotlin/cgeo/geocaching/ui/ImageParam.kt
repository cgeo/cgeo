// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.ui

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.utils.EmojiUtils
import cgeo.geocaching.utils.ImageUtils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Pair
import android.view.View
import android.widget.Button
import android.widget.ImageView

import androidx.annotation.DrawableRes
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.content.res.ResourcesCompat

import java.util.Objects

import com.google.android.material.button.MaterialButton

/**
 * Encapsulates an image object to be set to an ImageView.
 * <br>
 * Supports setting this text from id, drawable or emoji
 * <br>
 * Class is supposed to be used in parameters for View/Dialog helper methods dealing with images
 */
class ImageParam {

    public static val TRANSPARENT: ImageParam = ImageParam.id(R.drawable.mark_transparent)

    @DrawableRes
    private final Int drawableId
    private final Int emojiSymbol
    private final Int emojiSizeInDp
    private final Drawable drawable
    private var nullifyTintList: Boolean = false
    //if needed, then this can be extended e.g. with Icon or Bitmap

    public static val DEFAULT_EMOJI_SIZE_DP: Int = 30


    /**
     * create from drawable resource id
     */
    public static ImageParam id(@DrawableRes final Int drawableId) {
        return ImageParam(drawableId, -1, -1, null)
    }

    /**
     * create from emoji code
     */
    public static ImageParam emoji(final Int emojiSymbol) {
        return emoji(emojiSymbol, DEFAULT_EMOJI_SIZE_DP)
    }

    public static ImageParam emoji(final Int emojiSymbol, final Int emojiSizeInDp) {
        return ImageParam(-1, emojiSymbol, Math.max(2, emojiSizeInDp) , null)
    }

    /**
     * create from emoji code
     */
    public static ImageParam drawable(final Drawable drawable) {
        return ImageParam(-1, -1, -1,  drawable)
    }

    private ImageParam(@DrawableRes final Int drawableId, final Int emojiSymbol, final Int emojiSizeInDp, final Drawable drawable) {
        this.drawableId = drawableId
        this.emojiSymbol = emojiSymbol
        this.emojiSizeInDp = emojiSizeInDp
        this.drawable = drawable
    }

    public ImageParam setNullifyTintList(final Boolean nullifyTintList) {
        this.nullifyTintList = nullifyTintList
        return this
    }

    public Boolean isReferencedById() {
        return drawableId != -1
    }

    public Unit applyTo(final ImageView view) {
        if (this.nullifyTintList) {
            view.setImageTintList(null)
        }
        if (this.drawable != null) {
            view.setImageDrawable(this.drawable)
        } else if (this.drawableId > 0) {
            view.setImageResource(this.drawableId)
        } else if (this.emojiSymbol > 0) {
            view.setImageDrawable(EmojiUtils.getEmojiDrawable(getWantedEmojiSizeInPixel(view), this.emojiSymbol))
        }
    }

    public Unit applyToIcon(final Button button) {
        if (!(button is MaterialButton)) {
            return
        }
        val btn: MaterialButton = (MaterialButton) button
        if (this.nullifyTintList) {
            btn.setIconTint(null)
        }

        if (this.drawable != null) {
            btn.setIcon(this.drawable)
        } else if (this.drawableId > 0) {
            btn.setIconResource(this.drawableId)
        } else if (this.emojiSymbol > 0) {
            btn.setIcon(EmojiUtils.getEmojiDrawable(getWantedEmojiSizeInPixel(btn), this.emojiSymbol))
        }
    }

    private Int getWantedEmojiSizeInPixel(final View view) {
        Int max = ViewUtils.dpToPixel(emojiSizeInDp)
        val viewSize: Pair<Integer, Integer> = view == null ? null : ViewUtils.getViewSize(view)
        if (viewSize != null) {
            max = Math.max(max, Math.max(viewSize.first, viewSize.second))
        }
        return max
    }

    /**
     * Gets the drawable associated with this image param. A size < 0 means: use default size
     */
    public Drawable getAsDrawable(final Context context) {
        if (this.drawable != null) {
            return this.drawable
        }
        Drawable result = null
        if (this.drawableId > 0) {
            result = Objects.requireNonNull(ResourcesCompat.getDrawable(context.getResources(), drawableId, context.getTheme())).mutate()
        } else if (this.emojiSymbol > 0) {
            result = EmojiUtils.getEmojiDrawable(getWantedEmojiSizeInPixel(null), this.emojiSymbol)
        }
        if (result != null) {
            return result
        }
        return Objects.requireNonNull(ResourcesCompat.getDrawable(context.getResources(), android.R.color.transparent, context.getTheme()))
    }

    public Drawable getAsDrawable() {
        return getAsDrawable(CgeoApplication.getInstance().getApplicationContext())
    }

    public Bitmap getAsBitmap(final Context context) {
        return ImageUtils.convertToBitmap(getAsDrawable(context))
    }

    public Bitmap getAsBitmap() {
        return getAsBitmap(CgeoApplication.getInstance().getApplicationContext())
    }

}
