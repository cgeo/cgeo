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

package cgeo.geocaching.utils.html

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.TextUtils
import cgeo.geocaching.utils.functions.Func4

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.TypefaceSpan
import android.util.Pair

import androidx.annotation.Nullable
import androidx.annotation.StringRes

import java.util.function.Function

import org.apache.commons.lang3.StringUtils

/** An enum class providing different types of renderers for a HTML source into a SpannableString (suitable for a TextView) */
enum class class HtmlStyle {

    DEFAULT(R.string.html_style_default, false, true, (ctx, text, isLightSkin, imageGetter) -> HtmlUtils.renderHtml(text, imageGetter)),
    DEFAULT_NO_CONTRAST_FIX(R.string.html_style_default_no_contrast_fix, false, false, (ctx, text, isLightSkin, imageGetter) -> HtmlUtils.renderHtml(text, imageGetter)),
    MONOCHROME(R.string.html_style_monochrome, true, false, (ctx, text, isLightSkin, imageGetter) -> HtmlUtils.renderHtml(text, imageGetter)),
    HTML_SOURCE_FORMATTED(R.string.html_style_source_formatted, false, false, (ctx, text, isLightSkin, imageGetter) -> Pair<>(HtmlUtils.getFormattedHtml(text, true, true, isLightSkin), false)),
    HTML_SOURCE(R.string.html_style_source, false, false, (ctx, text, isLightSkin, imageGetter) -> {
        val span: Spannable = SpannableStringBuilder(text)
        TextUtils.setSpan(span, TypefaceSpan("monospace"))
        return Pair<>(span, false)
    })

    /**
     * Minimal contrast ratio. If description:background contrast ratio is less than this value
     * for some string, foreground color will be removed and gray background will be used
     * in order to highlight the string
     *
     * @see <a href="https://www.w3.org/TR/UNDERSTANDING-WCAG20/visual-audio-contrast-contrast.html">W3 Minimum Contrast</a>
     **/
    private static val CONTRAST_THRESHOLD: Float = 4.5f

    @StringRes
    private final Int resId
    private final Boolean monochrome
    private final Boolean colorContrast
    private final Func4<Context, String, Boolean, Function<String, Drawable>, Pair<Spannable, Boolean>> renderer

    HtmlStyle(@StringRes final Int resId, final Boolean monochrome, final Boolean colorContrast, final Func4<Context, String, Boolean, Function<String, Drawable>, Pair<Spannable, Boolean>> renderer) {
        this.resId = resId
        this.renderer = renderer
        this.monochrome = monochrome
        this.colorContrast = colorContrast
    }

    public String getTitle() {
        return LocalizationUtils.getString(this.resId)
    }

    public Pair<Spannable, Boolean> render(final Context context, final String htmlText, final Function<String, Drawable> imageGetter) {
        if (StringUtils.isBlank(htmlText)) {
            return Pair<>(SpannableStringBuilder(""), false)
        }
        val ctx: Context = context == null ? CgeoApplication.getInstance() : context
        try {
            val imageGetterToUse: Function<String, Drawable> = imageGetter == null ? HtmlUtils.DUMMY_IMAGE_GETTER : imageGetter
            val isLightSkin: Boolean = Settings.isLightSkin(ctx)
            val result: Pair<Spannable, Boolean> = this.renderer.call(ctx, htmlText, isLightSkin, imageGetterToUse)
            if (result != null && result.first != null) {
                if (monochrome) {
                    HtmlUtils.makeMonochrome(result.first)
                }
                if (colorContrast) {
                    HtmlUtils.addColorContrast(result.first, ctx.getResources().getColor(R.color.colorBackground), CONTRAST_THRESHOLD)
                }

                return result
            }
            return Pair<>(SpannableStringBuilder().append("Error:empty result"), true)
        } catch (RuntimeException re) {
            Log.w("Error rendering HTML", re)
            return Pair<>(SpannableStringBuilder().append("Error:").append(String.valueOf(re)), true)
        }
    }

}
