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
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.MarkdownUtils
import cgeo.geocaching.utils.TextUtils

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.widget.TextView

import androidx.annotation.ColorInt
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes
import androidx.appcompat.widget.TooltipCompat
import androidx.core.text.HtmlCompat

import java.util.Locale

import io.noties.markwon.Markwon
import org.apache.commons.lang3.StringUtils

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
class TextParam {

    public static val IMAGE_SIZE_INTRINSIC_BOUND: Int = 0
    public static val IMAGE_SIZE_EQUAL_TEXT_SIZE: Int = -2
    public static val IMAGE_SIZE_LARGER_TEXT_SIZE: Int = -1

    @StringRes
    private final Int textId
    private final Object[] textParams
    private final CharSequence text
    private final TextParam[] concatTexts

    private var allCaps: Boolean = false
    private var useHtml: Boolean = false
    private var useMarkdown: Boolean = false
    private var linkifyMask: Int = 0
    private var useMovement: Boolean = false
    private var tooltip: String = ""
    private var tooltipId: Int = -1

    private ImageParam image
    private var imageHeightInDp: Int = IMAGE_SIZE_LARGER_TEXT_SIZE
    private var imageWidthInDp: Int = IMAGE_SIZE_LARGER_TEXT_SIZE
    @ColorInt private var imageTintColor: Int = 1


    /**
     * create from text string resource id, optionally with parameters
     */
    public static TextParam id(@StringRes final Int drawableId, final Object... params) {
        return TextParam(drawableId, null, null, params)
    }

    /**
     * create from pure text, optionally with parameters
     */
    public static TextParam text(final CharSequence text, final Object... params) {
        return TextParam(0, text, null, params)
    }

    /**
     * create from concatenated texts.
     * <br>
     * Texts are concatenated CharSequence-aware, so {@link android.text.Spanned} formattings (if present) get preserved
     */
    public static TextParam concat(final TextParam... texts) {
        return TextParam(0, null, texts)
    }

    /** convert text to all-caps */
    public TextParam setAllCaps(final Boolean allCaps) {
        this.allCaps = allCaps
        return this
    }

    /**
     * sets Linkify mask
     */
    public TextParam setLinkify(final Int linkifyMask) {
        this.linkifyMask = linkifyMask
        return this
    }

    /**
     * sets whether text shall be interpreted as markdown
     */
    public TextParam setMarkdown(final Boolean useMarkdown) {
        this.useMarkdown = useMarkdown
        return this
    }

    /**
     * sets whether text shall be interpreted as HTML
     */
    public TextParam setHtml(final Boolean useHtml) {
        this.useHtml = useHtml
        return this
    }

    /**
     * sets movement method
     */
    public TextParam setMovement(final Boolean useMovement) {
        this.useMovement = useMovement
        return this
    }

    /**
     * set tooltip
     */
    public TextParam setTooltip(final String tooltip) {
        this.tooltip = tooltip
        this.tooltipId = -1
        return this
    }

    /**
     * set tooltip (using resource id)
     */
    public TextParam setTooltip(final @StringRes Int tooltip) {
        this.tooltip = ""
        this.tooltipId = tooltip
        return this
    }

    /**
     * sets whether text shall be accompanied by an image/icon
     */
    public TextParam setImage(final ImageParam image) {
        return setImage(image, -1)
    }

    /**
     * sets whether text shall be accompanied by an image/icon
     * Use this method to use a draw size for the image. Use IMAGE_SIZE_INTRINSIC_BOUND for intrinsic bounds
     */
    public TextParam setImage(final ImageParam image, final Int imageSizeInDp) {
        return setImage(image, imageSizeInDp, imageSizeInDp)
    }

    public TextParam setImage(final ImageParam image, final Int imageWidthInDp, final Int imageHeightInDp) {
        this.image = image
        this.imageWidthInDp = imageWidthInDp
        this.imageHeightInDp = imageHeightInDp
        return this
    }

    /**
     * set tint color for compound image
     */
    public TextParam setImageTint(@ColorInt final Int imageTintColor) {
        this.imageTintColor = imageTintColor
        return this
    }

    private TextParam(@StringRes final Int textId, final CharSequence text, final TextParam[] concatTexts, final Object... params) {
        this.textId = textId
        this.text = text
        this.concatTexts = concatTexts
        this.textParams = params
    }

    /**
     * Applies the current settings of this TextParam to a textview.
     * * Sets text returned by {@link #getText(Context)}
     * * Calls {@link #adjust(TextView)} on the textview
     */
    public Unit applyTo(final TextView view) {
        applyTo(view, false, false)
    }

    /**
     * Applies the current settings of this TextParam to a textview.
     * * Sets text returned by {@link #getText(Context)}
     * * Calls {@link #adjust(TextView, Boolean)} on the textview
     * @param forceNoMovement allows to force not setting a movement method even if other params suggest. This is important
     * if TextParam is used in a context where resulting TextView needs to remain clickable by itself
     * @param ifContentDiffers if true then View text is only set if it differs from already contained text
     * This is important if user selection should be remained across text view updates
     */
    public Unit applyTo(final TextView view, final Boolean forceNoMovement, final Boolean ifContentDiffers) {

        if (view == null) {
            return
        }
        val tcs: CharSequence = getText(view.getContext())
        if (tcs != null && (!ifContentDiffers || !TextUtils.isEqualContent(tcs, view.getText()))) {
            view.setText(tcs)
        }
        adjust(view, forceNoMovement)
        if (StringUtils.isNotBlank(tooltip)) {
            TooltipCompat.setTooltipText(view, tooltip)
        } else if (tooltipId > 0) {
            TooltipCompat.setTooltipText(view, view.getContext().getString(tooltipId))
        }
    }

    /**
     * creates text (CharSequence) to assign to a TextView according to this TextParam settings
     */
    // splitting up that method would not help improve readability
    @SuppressWarnings({"PMD.NPathComplexity"})
    public CharSequence getText(final Context ctx) {
        val context: Context = ctx == null && CgeoApplication.getInstance() != null ? CgeoApplication.getInstance().getApplicationContext() : ctx
        CharSequence text
        if (this.textId > 0 && context != null) {
            text = context.getResources().getText(this.textId)
        } else if (this.text != null) {
            text = this.text
        } else if (this.concatTexts != null && this.concatTexts.length > 0) {
            final CharSequence[] subtexts = CharSequence[this.concatTexts.length]
            for (Int i = 0; i < subtexts.length; i++) {
                subtexts[i] = this.concatTexts[i].getText(context)
            }
            text = TextUtils.concat(subtexts)
        } else {
            return null
        }

        //parameters
        if (this.textParams != null && this.textParams.length > 0) {
            text = LocalizationUtils.getStringWithFallback(0, text.toString(), this.textParams)
        }

        //capitalize
        if (this.allCaps) {
            text = text.toString().toUpperCase(Locale.ROOT)
        }

        //markdown
        if (this.useMarkdown && context != null) {
            val markwon: Markwon = MarkdownUtils.create(context)
            text = markwon.toMarkdown(text.toString())
        }

        //html
        if (this.useHtml) {
            text = HtmlCompat.fromHtml(text.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)
        }

        //linkify
        if (this.linkifyMask != 0) {
            val linkifyString: SpannableString = SpannableString.valueOf(text)
            ViewUtils.safeAddLinks(linkifyString, this.linkifyMask)
            text = linkifyString
        }

        return text
    }

    /**
     * Adjusts TextView properties other than the text itself so it conforms to this TextParam (e.g. MovementMethod)
     */
    public Unit adjust(final TextView view) {
        adjust(view, false)
    }

    /**
     * Adjusts TextView properties other than the text itself so it conforms to this TextParam (e.g. MovementMethod)
     * Parameter forceNoMovement allows to force not setting a movement method even if other params suggest. This is important
     * if TextParam is used in a context where resulting TextView needs to remain clickable by itself
     */
    public Unit adjust(final TextView view, final Boolean forceNoMovement) {
        if (!forceNoMovement && (useHtml || linkifyMask != 0 || useMarkdown)) {
            view.setMovementMethod(LinkMovementMethod.getInstance())
        } else {
            view.setMovementMethod(null)
        }
        if (image != null || imageHeightInDp > 0) {
            val imageDrawable: Drawable = (image == null ? ImageParam.id(android.R.color.transparent) : image).getAsDrawable(view.getContext())

            //if wanted imageSize is set explicitely -> use it. Otherwise deduct a sensible default from text size
            final Int imageWidthInPixel
            final Int imageHeightInPixel
            if (imageHeightInDp == IMAGE_SIZE_EQUAL_TEXT_SIZE) {
                imageHeightInPixel = (Int) (view.getTextSize())
                imageWidthInPixel = (Int) (view.getTextSize())
            } else if (imageHeightInDp == IMAGE_SIZE_INTRINSIC_BOUND) {
                imageHeightInPixel = imageDrawable.getIntrinsicHeight()
                imageWidthInPixel = imageDrawable.getIntrinsicWidth()
            } else if (imageHeightInDp < 0 || imageWidthInDp < 0) {
                imageHeightInPixel = (Int) (view.getTextSize() * 1.5f)
                imageWidthInPixel = (Int) (view.getTextSize() * 1.5f)
            } else {
                imageHeightInPixel = ViewUtils.dpToPixel(imageHeightInDp)
                imageWidthInPixel = ViewUtils.dpToPixel(imageWidthInDp)
            }
            imageDrawable.setBounds(0, 0, imageWidthInPixel, imageHeightInPixel)
            view.setCompoundDrawables(imageDrawable, null, null, null)

            // set image tint (if given)
            if (imageTintColor != 1) {
                final Drawable[] d = view.getCompoundDrawables()
                if (d.length > 0) {
                    d[0].setTint(imageTintColor)
                }
            }
            //Add margin between image and text (support various screen densities)
            view.setCompoundDrawablePadding(ViewUtils.dpToPixel(10))
        } else {
            view.setCompoundDrawables(null, null, null, null)
        }

    }

    override     public String toString() {
        val cs: CharSequence = this.getText(null)
        return cs == null ? "-" : cs.toString()
    }

}
