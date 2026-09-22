package cgeo.geocaching.models.geoitem;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable value class for GeoItem Style info. Includes some helpers to deal with these objects.
 */
public class GeoStyle implements Parcelable {

    public static final GeoStyle SYSTEM_DEFAULT = fixed(Color.BLACK, Color.TRANSPARENT, 2f);

    //dynamics
    private final List<GeoStyleRule> styleRules; //if null then this is a fixed style, otherwise it is dynamic

    //cached or fixed values
    @ColorInt private int strokeColor;
    @ColorInt private int fillColor;
    private float strokeWidth;

    private GeoStyle(final int strokeColor, final int fillColor, final float strokeWidth, final List<GeoStyleRule> styleRules) {
        this.styleRules = styleRules == null ? null : Collections.unmodifiableList(styleRules);
        this.strokeColor = strokeColor;
        this.fillColor = fillColor;
        this.strokeWidth = strokeWidth;
    }

    public int getFillColor() {
        return fillColor;
    }

    public int getStrokeColor() {
        return strokeColor;
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public boolean isDynamic() {
        return styleRules != null;
    }

    public void recalculateDynamic(@Nullable final GeoItem item, @Nullable final GeoStyle parentStyle, @Nullable final GeoStyle mainStyle) {
        //don't recalculate for fixed values
        if (!isDynamic()) {
            return;
        }

        final List<GeoStyleRule> rules = styleRules;
        final GeoStyle main = mainStyle != null ? mainStyle : SYSTEM_DEFAULT;
        final GeoStyle parent = parentStyle != null ? parentStyle : main;
        Integer strokeColor = null;
        Integer fillColor = null;
        Float strokeWidth = null;

        if (item != null && rules != null && !rules.isEmpty()) {
            for (GeoStyleRule rule : rules) {
                switch (rule.type()) {
                    case GeoStyleRule.STYLE_FILL_COLOR -> {
                        if (fillColor == null && GeoItemUtils.matchesCondition(item, rule.condition())) {
                            fillColor = rule.intValue() == GeoStyleRule.DEFAULT_FILL_COLOR_MARKER ? main.getFillColor() : rule.intValue();
                        }
                    }
                    case GeoStyleRule.STYLE_STROKE_COLOR -> {
                        if (strokeColor == null && GeoItemUtils.matchesCondition(item, rule.condition())) {
                            strokeColor = rule.intValue() == GeoStyleRule.DEFAULT_STROKE_COLOR_MARKER ? main.getStrokeColor() : rule.intValue();
                        }
                    }
                    case GeoStyleRule.STYLE_STROKE_WIDTH -> {
                        if (strokeWidth == null && GeoItemUtils.matchesCondition(item, rule.condition())) {
                            strokeWidth = rule.floatValue() == GeoStyleRule.DEFAULT_STROKE_WIDTH_MARKER ? main.getStrokeWidth() : rule.floatValue();
                        }
                    }
                }
            }
        }

        this.strokeColor = strokeColor != null ? strokeColor : parent.getStrokeColor();
        this.fillColor = fillColor != null ? fillColor : parent.getFillColor();
        this.strokeWidth = strokeWidth != null ? strokeWidth : parent.getStrokeWidth();
    }

    //creators

    public static GeoStyle dynamic(final List<GeoStyleRule> styleRules) {
        return dynamic(SYSTEM_DEFAULT.strokeColor, SYSTEM_DEFAULT.fillColor, SYSTEM_DEFAULT.strokeWidth, styleRules);
    }

    public static GeoStyle dynamic(final int strokeColor, final int fillColor, final float strokeWidth, final List<GeoStyleRule> styleRules) {
        return new GeoStyle(strokeColor, fillColor, strokeWidth, styleRules == null ? Collections.emptyList() : new ArrayList<>(styleRules));
    }

    public static GeoStyle fixed(final int strokeColor, final int fillColor, final float strokeWidth) {
        return new GeoStyle(strokeColor, fillColor, strokeWidth, null);
    }

    public static GeoStyle solid(final int color, final float width) {
        return fixed(color, color, width);
    }

    public static GeoStyle solid(final int color) {
        return fixed(color, color, SYSTEM_DEFAULT.getStrokeWidth());
    }

    /** creates a style where fill color is a transparent version of stroke color */
    public static GeoStyle transparentFill(final int strokeColor, final int fillTransparency, final float width) {
        final int fillColor = Color.argb(fillTransparency, Color.red(strokeColor), Color.green(strokeColor), Color.blue(strokeColor));
        return fixed(strokeColor, fillColor, width);
    }

    // equals / hashcode

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final GeoStyle geoStyle = (GeoStyle) o;
        final boolean styleRulesMatch;
        if (null != styleRules) {
            styleRulesMatch = styleRules.equals(geoStyle.styleRules);
        } else {
            styleRulesMatch = (null == geoStyle.styleRules);
        }
        return strokeColor == geoStyle.strokeColor && fillColor == geoStyle.fillColor && Float.compare(geoStyle.strokeWidth, strokeWidth) == 0 && styleRulesMatch;
    }

    @Override
    public int hashCode() {
        int result = strokeColor;
        result = 31 * result + fillColor;
        result = 31 * result + Float.hashCode(strokeWidth);
        result = 31 * result + (null == styleRules ? 1 : styleRules.hashCode());
        return result;
    }

    // parcelable stuff

    private GeoStyle(final Parcel in) {
        this(in.readInt(), in.readInt(), in.readFloat(), in.createTypedArrayList(GeoStyleRule.CREATOR));
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeInt(strokeColor);
        dest.writeInt(fillColor);
        dest.writeFloat(strokeWidth);
        dest.writeTypedList(styleRules);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<GeoStyle> CREATOR = new Creator<>() {
        @Override
        public GeoStyle createFromParcel(final Parcel in) {
            return new GeoStyle(in);
        }

        @Override
        public GeoStyle[] newArray(final int size) {
            return new GeoStyle[size];
        }
    };
}
