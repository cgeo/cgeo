package cgeo.geocaching.utils.html;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.util.Pair;

import androidx.annotation.StringRes;

import java.util.function.Function;

import kotlin.jvm.functions.Function3;

/** An enum providing different types of renderers for a HTML source into a SpannableString (suitable for a TextView) */
public enum HtmlStyle {

    DEFAULT(R.string.html_style_default, (ctx, text, imageGetter) -> HtmlUtils.renderHtml(text, imageGetter, false)),
    MONOCHROME(R.string.html_style_monochrome, (ctx, text, imageGetter) -> HtmlUtils.renderHtml(text, imageGetter, true)),
    HTML_SOURCE(R.string.html_style_source, (ctx, text, imageGetter) -> new Pair<>(new SpannableStringBuilder(text), false));

    @StringRes
    private final int resId;
    private final Function3<Context, String, Function<String, Drawable>, Pair<Spannable, Boolean>> renderer;

    HtmlStyle(@StringRes final int resId, final Function3<Context, String, Function<String, Drawable>, Pair<Spannable, Boolean>> renderer) {
        this.resId = resId;
        this.renderer = renderer;
    }

    public String getTitle() {
        return LocalizationUtils.getString(this.resId);
    }

    public Pair<Spannable, Boolean> render(final Context ctx, final String htmlText, final Function<String, Drawable> imageGetter) {
        try {
            final Pair<Spannable, Boolean> result = this.renderer.invoke(ctx, htmlText, imageGetter);
            if (result != null && result.first != null) {
                return result;
            }
            return new Pair<>(new SpannableStringBuilder().append("Error:empty result"), true);
        } catch (RuntimeException re) {
            Log.w("Error rendering HTML", re);
            return new Pair<>(new SpannableStringBuilder().append("Error:").append(String.valueOf(re)), true);
        }
    }

}
