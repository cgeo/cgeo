package cgeo.geocaching.utils.html;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.ui.ViewUtils;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.LeadingMarginSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * One HTML list item: draws a bullet or a number in front of its first line and indents all of its lines
 * (hanging indent). Nested lists indent further because Android adds up overlapping leading margins.
 */
public class ListItemSpan implements LeadingMarginSpan {

    private static final float INDENT_BULLET_SP = 18f;
    private static final float PADDING_NUMBER_SP = 3f;
    private static final float GAP_SP = 6f;
    private static final float BULLET_RADIUS_SP = 2.5f;

    @Nullable
    private final String number;
    private final int indent;

    /**
     * @param number marker text of a numbered item (e.g. "1."), or null for a bullet
     * @param widestNumber widest marker of the same list (e.g. "120."), determines the indent of numbered items
     */
    public ListItemSpan(@Nullable final String number, @Nullable final String widestNumber) {
        this.number = number;
        this.indent = number == null || widestNumber == null ? ViewUtils.spToPixel(INDENT_BULLET_SP)
                : Math.round(ViewUtils.spToPixelFloat(PADDING_NUMBER_SP + GAP_SP) + measureMarker(widestNumber));
    }

    /** width of the marker in the text size used for listings and logs (getLeadingMargin() has no Paint to measure with) */
    private static float measureMarker(@NonNull final String marker) {
        final Resources res = CgeoApplication.getInstance() == null ? null : CgeoApplication.getInstance().getResources();
        final TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(res == null ? ViewUtils.spToPixelFloat(16f) : res.getDimension(R.dimen.textSize_detailsPrimary));
        return paint.measureText(marker);
    }

    @Nullable
    public String getNumber() {
        return number;
    }

    @Override
    public int getLeadingMargin(final boolean first) {
        return indent;
    }

    @Override
    public void drawLeadingMargin(@NonNull final Canvas canvas, @NonNull final Paint paint, final int x, final int dir,
                                  final int top, final int baseline, final int bottom,
                                  @NonNull final CharSequence text, final int start, final int end,
                                  final boolean first, @Nullable final Layout layout) {
        // only the line the item starts on gets the marker, all following lines (e.g. after <br>) are just indented
        if (!(text instanceof Spanned) || ((Spanned) text).getSpanStart(this) != start) {
            return;
        }
        final float gap = ViewUtils.spToPixelFloat(GAP_SP);
        if (number == null) {
            final float radius = ViewUtils.spToPixelFloat(BULLET_RADIUS_SP);
            final Paint.Style oldStyle = paint.getStyle();
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x + dir * (indent - gap - radius), (top + bottom) / 2f, radius, paint);
            paint.setStyle(oldStyle);
        } else {
            final float width = paint.measureText(number);
            canvas.drawText(number, dir > 0 ? x + indent - gap - width : x - indent + gap, baseline, paint);
        }
    }
}
