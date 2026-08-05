package cgeo.geocaching.ui;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.utils.EmojiUtils;
import cgeo.geocaching.utils.ImageUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import java.util.Objects;

import com.google.android.material.button.MaterialButton;
import org.apache.commons.lang3.StringUtils;

/**
 * Encapsulates an image object to be set to an ImageView.
 * <br>
 * Supports setting this text from id, drawable or emoji
 * <br>
 * Class is supposed to be used in parameters for View/Dialog helper methods dealing with images
 */
public class ImageParam {

    public static final ImageParam TRANSPARENT = ImageParam.id(R.drawable.mark_transparent);

    @DrawableRes
    private final int drawableId;
    @Nullable
    private final String emoji;
    private final int emojiSizeInDp;
    private final Drawable drawable;
    private boolean nullifyTintList = false;
    //if needed, then this can be extended e.g. with Icon or Bitmap

    public static final int DEFAULT_EMOJI_SIZE_DP = 30;


    /**
     * create from drawable resource id
     */
    public static ImageParam id(@DrawableRes final int drawableId) {
        return new ImageParam(drawableId, null, -1, null);
    }

    /**
     * create from emoji string (as returned by the emoji picker / stored as cache or list marker)
     */
    public static ImageParam emoji(@Nullable final String emojiStr) {
        return emoji(emojiStr, DEFAULT_EMOJI_SIZE_DP);
    }

    public static ImageParam emoji(@Nullable final String emojiStr, final int emojiSizeInDp) {
        return new ImageParam(-1, emojiStr, Math.max(2, emojiSizeInDp), null);
    }

    /**
     * create from emoji code
     */
    public static ImageParam drawable(final Drawable drawable) {
        return new ImageParam(-1, null, -1,  drawable);
    }

    private ImageParam(@DrawableRes final int drawableId, @Nullable final String emoji, final int emojiSizeInDp, @Nullable final Drawable drawable) {
        this.drawableId = drawableId;
        this.emoji = emoji;
        this.emojiSizeInDp = emojiSizeInDp;
        this.drawable = drawable;
    }

    public ImageParam setNullifyTintList(final boolean nullifyTintList) {
        this.nullifyTintList = nullifyTintList;
        return this;
    }

    public boolean isReferencedById() {
        return drawableId != -1;
    }

    public void applyTo(final ImageView view) {
        if (this.nullifyTintList) {
            view.setImageTintList(null);
        }
        if (this.drawable != null) {
            view.setImageDrawable(this.drawable);
        } else if (this.drawableId > 0) {
            view.setImageResource(this.drawableId);
        } else if (StringUtils.isNotBlank(this.emoji)) {
            view.setImageDrawable(EmojiUtils.getEmojiDrawable(getWantedEmojiSizeInPixel(view), this.emoji));
        }
    }

    public void applyToIcon(final Button button) {
        if (!(button instanceof MaterialButton)) {
            return;
        }
        final MaterialButton btn = (MaterialButton) button;
        if (this.nullifyTintList) {
            btn.setIconTint(null);
        }

        if (this.drawable != null) {
            btn.setIcon(this.drawable);
        } else if (this.drawableId > 0) {
            btn.setIconResource(this.drawableId);
        } else if (StringUtils.isNotBlank(this.emoji)) {
            btn.setIcon(EmojiUtils.getEmojiDrawable(getWantedEmojiSizeInPixel(btn), this.emoji));
        }
    }

    private int getWantedEmojiSizeInPixel(final View view) {
        int max = ViewUtils.dpToPixel(emojiSizeInDp);
        final Pair<Integer, Integer> viewSize = view == null ? null : ViewUtils.getViewSize(view);
        if (viewSize != null) {
            max = Math.max(max, Math.max(viewSize.first, viewSize.second));
        }
        return max;
    }

    /**
     * Gets the drawable associated with this image param. A size < 0 means: use default size
     */
    @NonNull
    public Drawable getAsDrawable(final Context context) {
        if (this.drawable != null) {
            return this.drawable;
        }
        Drawable result = null;
        if (this.drawableId > 0) {
            result = Objects.requireNonNull(ResourcesCompat.getDrawable(context.getResources(), drawableId, context.getTheme())).mutate();
        } else if (StringUtils.isNotBlank(this.emoji)) {
            result = EmojiUtils.getEmojiDrawable(getWantedEmojiSizeInPixel(null), this.emoji);
        }
        if (result != null) {
            return result;
        }
        return Objects.requireNonNull(ResourcesCompat.getDrawable(context.getResources(), android.R.color.transparent, context.getTheme()));
    }

    @NonNull
    public Drawable getAsDrawable() {
        return getAsDrawable(CgeoApplication.getInstance().getApplicationContext());
    }

    @NonNull
    public Bitmap getAsBitmap(final Context context) {
        return ImageUtils.convertToBitmap(getAsDrawable(context));
    }

    @NonNull
    public Bitmap getAsBitmap() {
        return getAsBitmap(CgeoApplication.getInstance().getApplicationContext());
    }

}
