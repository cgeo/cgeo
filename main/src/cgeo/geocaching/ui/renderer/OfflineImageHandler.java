package cgeo.geocaching.ui.renderer;

import cgeo.geocaching.network.HtmlImage;

import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;

import net.nightwhistler.htmlspanner.SpanStack;
import net.nightwhistler.htmlspanner.TagNodeHandler;
import org.htmlcleaner.TagNode;

public class OfflineImageHandler extends TagNodeHandler {
    private final HtmlImage imageGetter;

    OfflineImageHandler(final String geocode) {
        imageGetter = new HtmlImage(geocode, true, false, null, false);
    }

    @Override
    public void handleTagNode(final TagNode node, final SpannableStringBuilder builder, final int start, final int end, final SpanStack spanStack) {
        final String src = node.getAttributeByName("src");

        builder.append("\uFFFC"); // gets spanned with the image

        final Drawable drawable = imageGetter.getDrawable(src);
        if (drawable != null) {
            builder.setSpan(new ImageSpan(drawable), start, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}
