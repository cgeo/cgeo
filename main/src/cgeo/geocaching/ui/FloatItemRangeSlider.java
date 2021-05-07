package cgeo.geocaching.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FloatItemRangeSlider extends ItemRangeSlider<Float> {

    public FloatItemRangeSlider(final Context context) {
        super(context);
    }

    public FloatItemRangeSlider(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
    }

    public FloatItemRangeSlider(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FloatItemRangeSlider(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private static List<Float> constructFloatItems(final float minValue, final float maxValue, final float stepWide) {
        final List<Float> items = new ArrayList<>();
        float f;
        for (f = minValue; f < maxValue; f += stepWide) {
            items.add(f);
        }
        items.add(maxValue);

        return items;
    }

    private static String formatFloat(final float f, final int precision) {
        return String.format("%." + precision + "f", f);
    }

    public void setScale(final float minValue, final float maxValue, final float stepWide, final int labelSpace, final int precision) {
        setScale(
            constructFloatItems(minValue, maxValue, stepWide),
            (i, f) -> formatFloat(f, precision),
            (i, f) -> i % labelSpace == 0 || i == getItems().size() - 1 ? formatFloat(f, precision) : null);
    }
}
