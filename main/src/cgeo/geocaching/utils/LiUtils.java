package cgeo.geocaching.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.text.style.LeadingMarginSpan;
import android.util.TypedValue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.XMLReader;

/**
 * Extends the standard spannable by recognizing and formatting <li>...</li> tags
 * even on systems < API 24 + including fixes for higher API levels
 */
public class LiUtils {

    private LiUtils() {
        // utility class
    }

    public static SpannableStringBuilder formatHTML(final String html) {
        // replace our standard bullet points ... \n by HTML <li> ... </li>
        final Pattern p = Pattern.compile("([·])([^·\n\r]+)([\\n]?)([\n\r]+)");
        final Matcher m = p.matcher(html);
        final String input = m.find() ? m.replaceAll("<li>$2</li>") : html;

        // now apply HTML formatting (including <li> tags)
        final Spanned spanned = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? Html.fromHtml(input, Html.FROM_HTML_MODE_LEGACY) : Html.fromHtml(input, null, new LiTagHandler());
        final SpannableStringBuilder builder = new SpannableStringBuilder(spanned);
        final BulletSpan[] bulletSpans = builder.getSpans(0, builder.length(), BulletSpan.class);
        for (BulletSpan bulletSpan :bulletSpans) {
            final int start = builder.getSpanStart(bulletSpan);
            final int end = builder.getSpanEnd(bulletSpan);
            builder.removeSpan(bulletSpan);
            builder.setSpan(new LiAwareBulletSpan(), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        return builder;
    }
}

class LiAwareBulletSpan implements LeadingMarginSpan {

    private static final int BULLET_RADIUS = dip(3);

    @Override
    public int getLeadingMargin(final boolean first) {
        return dip(8) + 2 * BULLET_RADIUS;
    }

    @Override
    public void drawLeadingMargin(final Canvas canvas, final Paint paint, final int x, final int dir, final int top, final int baseline,
                                  final int bottom, final CharSequence text, final int start, final int end, final boolean first, final Layout layout) {
        Path bullet = null;

        if (((Spanned) text).getSpanStart(this) == start) {
            final Paint.Style style = paint.getStyle();
            paint.setStyle(Paint.Style.FILL);

            final float yPosition = layout != null ? layout.getLineBaseline(layout.getLineForOffset(start)) - BULLET_RADIUS * 2f : (top + bottom) / 2f;
            final float xPosition = x + dir * BULLET_RADIUS;

            if (canvas.isHardwareAccelerated()) {
                if (null == bullet) {
                    bullet = new Path();
                    bullet.addCircle(0.0f, 0.0f, BULLET_RADIUS, Path.Direction.CW);
                }

                canvas.save();
                canvas.translate(xPosition, yPosition);
                canvas.drawPath(bullet, paint);
                canvas.restore();
            } else {
                canvas.drawCircle(xPosition, yPosition, BULLET_RADIUS, paint);
            }

            paint.setStyle(style);
        }
    }

    private static int dip(final int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, DisplayUtils.getDisplayMetrics());
    }
}

class LiTagHandler implements Html.TagHandler {

    @Override
    public void handleTag(final boolean opening, final String tag, final Editable output, final XMLReader xmlReader) {
        if (tag.equals("li")) {
            if (opening) {
                output.setSpan(new Bullet(), output.length(), output.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            } else {
                output.append("\n\n");
                final Bullet[] bullets = output.getSpans(0, output.length(), Bullet.class);
                if (bullets.length > 0) {
                    final int start = output.getSpanStart(bullets[bullets.length - 1]);
                    output.removeSpan(bullets[bullets.length - 1]);
                    if (start != output.length()) {
                        output.setSpan(new BulletSpan(), start, output.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    }
                }
            }
        }
    }

    public static final class Bullet {
    }

}
