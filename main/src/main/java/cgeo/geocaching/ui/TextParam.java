package cgeo.geocaching.ui;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.utils.LocalizationUtils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.text.HtmlCompat;

import java.util.Locale;

import io.noties.markwon.Markwon;
import org.apache.commons.lang3.StringUtils;

/**
 * Encapsulates a text object to be set to a TextView.
 * <br>
 * Supports setting this text from id or raw texts (including i18n parametrization) as well as various text formattings:
 * * markdown
 * * HTML
 * * Linkify
 * * accompanying icon/image
 * <br>
 * Class is supposed to be used in parameters for View/Dialog helper methods dealing with text
 */
public class TextParam {

    public static final int IMAGE_SIZE_INTRINSIC_BOUND = 0;
    public static final int IMAGE_SIZE_EQUAL_TEXT_SIZE = -2;
    public static final int IMAGE_SIZE_LARGER_TEXT_SIZE = -1;

    @StringRes
    private final int textId;
    private final Object[] textParams;
    private final CharSequence text;
    private final TextParam[] concatTexts;

    private boolean allCaps = false;
    private boolean useHtml = false;
    private boolean useMarkdown = false;
    private int linkifyMask = 0;
    private boolean useMovement = false;
    private String tooltip = "";
    private int tooltipId = -1;

    private ImageParam image;
    private int imageHeightInDp = IMAGE_SIZE_LARGER_TEXT_SIZE;
    private int imageWidthInDp = IMAGE_SIZE_LARGER_TEXT_SIZE;
    @ColorInt private int imageTintColor = 1;


    /**
     * create from text string resource id, optionally with parameters
     */
    public static TextParam id(@StringRes final int drawableId, final Object... params) {
        return new TextParam(drawableId, null, null, params);
    }

    /**
     * create from pure text, optionally with parameters
     */
    public static TextParam text(final CharSequence text, final Object... params) {
        return new TextParam(0, text, null, params);
    }

    /**
     * create from concatenated texts.
     * <br>
     * Texts are concatenated CharSequence-aware, so {@link android.text.Spanned} formattings (if present) get preserved
     */
    public static TextParam concat(final TextParam... texts) {
        return new TextParam(0, null, texts);
    }

    /** convert text to all-caps */
    public TextParam setAllCaps(final boolean allCaps) {
        this.allCaps = allCaps;
        return this;
    }

    /**
     * sets Linkify mask
     */
    public TextParam setLinkify(final int linkifyMask) {
        this.linkifyMask = linkifyMask;
        return this;
    }

    /**
     * sets whether text shall be interpreted as markdown
     */
    public TextParam setMarkdown(final boolean useMarkdown) {
        this.useMarkdown = useMarkdown;
        return this;
    }

    /**
     * sets whether text shall be interpreted as HTML
     */
    public TextParam setHtml(final boolean useHtml) {
        this.useHtml = useHtml;
        return this;
    }

    /**
     * sets movement method
     */
    public TextParam setMovement(final boolean useMovement) {
        this.useMovement = useMovement;
        return this;
    }

    /**
     * set tooltip
     */
    public TextParam setTooltip(final String tooltip) {
        this.tooltip = tooltip;
        this.tooltipId = -1;
        return this;
    }

    /**
     * set tooltip (using resource id)
     */
    public TextParam setTooltip(final @StringRes int tooltip) {
        this.tooltip = "";
        this.tooltipId = tooltip;
        return this;
    }

    /**
     * sets whether text shall be accompanied by an image/icon
     */
    public TextParam setImage(final ImageParam image) {
        return setImage(image, -1);
    }

    /**
     * sets whether text shall be accompanied by an image/icon
     * Use this method to use a draw size for the image. Use IMAGE_SIZE_INTRINSIC_BOUND for intrinsic bounds
     */
    public TextParam setImage(final ImageParam image, final int imageSizeInDp) {
        return setImage(image, imageSizeInDp, imageSizeInDp);
    }

    public TextParam setImage(final ImageParam image, final int imageWidthInDp, final int imageHeightInDp) {
        this.image = image;
        this.imageWidthInDp = imageWidthInDp;
        this.imageHeightInDp = imageHeightInDp;
        return this;
    }

    /**
     * set tint color for compound image
     */
    public TextParam setImageTint(@ColorInt final int imageTintColor) {
        this.imageTintColor = imageTintColor;
        return this;
    }

    private TextParam(@StringRes final int textId, final CharSequence text, final TextParam[] concatTexts, final Object... params) {
        this.textId = textId;
        this.text = text;
        this.concatTexts = concatTexts;
        this.textParams = params;
    }

    /**
     * Applies the current settings of this TextParam to a textview.
     * * Sets text returned by {@link #getText(Context)}
     * * Calls {@link #adjust(TextView)} on the textview
     */
    public void applyTo(@Nullable final TextView view) {
        applyTo(view, false);
    }

    /**
     * Applies the current settings of this TextParam to a textview.
     * Parameter forceNoMovement allows to force not setting a movement method even if other params suggest. This is important
     * if TextParam is used in a context where resulting TextView needs to remain clickable by itself
     * * Sets text returned by {@link #getText(Context)}
     * * Calls {@link #adjust(TextView, boolean)} on the textview
     */
    public void applyTo(@Nullable final TextView view, final boolean forceNoMovement) {

        if (view == null) {
            return;
        }
        final CharSequence tcs = getText(view.getContext());
        if (tcs != null) {
            view.setText(tcs);
        }
        adjust(view, forceNoMovement);
        if (StringUtils.isNotBlank(tooltip)) {
            TooltipCompat.setTooltipText(view, tooltip);
        } else if (tooltipId > 0) {
            TooltipCompat.setTooltipText(view, view.getContext().getString(tooltipId));
        }
    }

    /**
     * creates text (CharSequence) to assign to a TextView according to this TextParam settings
     */
    // splitting up that method would not help improve readability
    @SuppressWarnings({"PMD.NPathComplexity"})
    public CharSequence getText(@Nullable final Context ctx) {
        final Context context = ctx == null && CgeoApplication.getInstance() != null ? CgeoApplication.getInstance().getApplicationContext() : ctx;
        CharSequence text;
        if (this.textId > 0 && context != null) {
            text = context.getResources().getText(this.textId);
        } else if (this.text != null) {
            text = this.text;
        } else if (this.concatTexts != null && this.concatTexts.length > 0) {
            final CharSequence[] subtexts = new CharSequence[this.concatTexts.length];
            for (int i = 0; i < subtexts.length; i++) {
                subtexts[i] = this.concatTexts[i].getText(context);
            }
            text = TextUtils.concat(subtexts);
        } else {
            return null;
        }

        //parameters
        if (this.textParams != null && this.textParams.length > 0) {
            text = LocalizationUtils.getStringWithFallback(0, text.toString(), this.textParams);
        }

        //capitalize
        if (this.allCaps) {
            text = text.toString().toUpperCase(Locale.ROOT);
        }

        //markdown
        if (this.useMarkdown && context != null) {
            final Markwon markwon = Markwon.create(context);
            text = markwon.toMarkdown(text.toString());
        }

        //html
        if (this.useHtml) {
            text = HtmlCompat.fromHtml(text.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY);
        }

        //linkify
        if (this.linkifyMask != 0) {
            final SpannableString linkifyString = SpannableString.valueOf(text);
            ViewUtils.safeAddLinks(linkifyString, this.linkifyMask);
            text = linkifyString;
        }

        return text;
    }

    /**
     * Adjusts TextView properties other than the text itself so it conforms to this TextParam (e.g. MovementMethod)
     */
    public void adjust(final TextView view) {
        adjust(view, false);
    }

    /**
     * Adjusts TextView properties other than the text itself so it conforms to this TextParam (e.g. MovementMethod)
     * Parameter forceNoMovement allows to force not setting a movement method even if other params suggest. This is important
     * if TextParam is used in a context where resulting TextView needs to remain clickable by itself
     */
    public void adjust(final TextView view, final boolean forceNoMovement) {
        if (!forceNoMovement && (useHtml || linkifyMask != 0 || useMarkdown)) {
            view.setMovementMethod(LinkMovementMethod.getInstance());
        }
        if (image != null || imageHeightInDp > 0) {
            final Drawable imageDrawable = (image == null ? ImageParam.id(android.R.color.transparent) : image).getAsDrawable(view.getContext());

            //if wanted imageSize is set explicitely -> use it. Otherwise deduct a sensible default from text size
            final int imageWidthInPixel;
            final int imageHeightInPixel;
            if (imageHeightInDp == IMAGE_SIZE_EQUAL_TEXT_SIZE) {
                imageHeightInPixel = (int) (view.getTextSize());
                imageWidthInPixel = (int) (view.getTextSize());
            } else if (imageHeightInDp == IMAGE_SIZE_INTRINSIC_BOUND) {
                imageHeightInPixel = imageDrawable.getIntrinsicHeight();
                imageWidthInPixel = imageDrawable.getIntrinsicWidth();
            } else if (imageHeightInDp < 0 || imageWidthInDp < 0) {
                imageHeightInPixel = (int) (view.getTextSize() * 1.5f);
                imageWidthInPixel = (int) (view.getTextSize() * 1.5f);
            } else {
                imageHeightInPixel = ViewUtils.dpToPixel(imageHeightInDp);
                imageWidthInPixel = ViewUtils.dpToPixel(imageWidthInDp);
            }
            imageDrawable.setBounds(0, 0, imageWidthInPixel, imageHeightInPixel);
            view.setCompoundDrawables(imageDrawable, null, null, null);

            // set image tint (if given)
            if (imageTintColor != 1) {
                final Drawable[] d = view.getCompoundDrawables();
                if (d.length > 0) {
                    d[0].setTint(imageTintColor);
                }
            }
            //Add margin between image and text (support various screen densities)
            view.setCompoundDrawablePadding(ViewUtils.dpToPixel(10));
        }

    }

    @Override
    @NonNull
    public String toString() {
        final CharSequence cs = this.getText(null);
        return cs == null ? "-" : cs.toString();
    }

}
