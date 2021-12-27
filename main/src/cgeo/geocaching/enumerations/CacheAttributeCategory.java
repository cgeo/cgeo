package cgeo.geocaching.enumerations;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import android.content.Context;
import android.content.res.ColorStateList;

import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum CacheAttributeCategory {
    CAT_TOOLS(10, R.string.cat_tools, R.color.attribute_category_tools, R.color.attribute_category_tools_negative),
    CAT_TYPE(20, R.string.cat_type, R.color.attribute_category_type),
    CAT_LOCATION(30, R.string.cat_location, R.color.attribute_category_location),
    CAT_DURATION(40, R.string.cat_duration, R.color.attribute_category_duration),
    CAT_SURROUNDINGS(50, R.string.cat_surrounding, R.color.attribute_category_surroundings),
    CAT_TRANSPORT(60, R.string.cat_transport, R.color.attribute_category_transport);

    public final int categoryId;
    public final int categoryNameResId;
    public final int categoryColor;
    public final int categoryColorNegative;

    CacheAttributeCategory(final int categoryId, final @StringRes int categoryNameResId, final @ColorRes int categoryColor) {
        this.categoryId = categoryId;
        this.categoryNameResId = categoryNameResId;
        this.categoryColor = categoryColor;
        this.categoryColorNegative = categoryColor;
    }

    CacheAttributeCategory(final int categoryId, final @StringRes int categoryNameResId, final @ColorRes int categoryColor, final @ColorRes int categoryColorNegative) {
        this.categoryId = categoryId;
        this.categoryNameResId = categoryNameResId;
        this.categoryColor = categoryColor;
        this.categoryColorNegative = categoryColorNegative;
    }

    @Nullable
    public static String getNameById(final Context context, final int categoryId) {
        for (CacheAttributeCategory cacheAttributeCategory : CacheAttributeCategory.values()) {
            if (cacheAttributeCategory.categoryId == categoryId) {
                return context.getString(cacheAttributeCategory.categoryNameResId);
            }
        }
        return null;
    }

    public static List<Integer> getOrderedCategoryIdList() {
        return new ArrayList<>(Arrays.asList(CAT_TOOLS.categoryId, CAT_DURATION.categoryId, CAT_LOCATION.categoryId, CAT_TYPE.categoryId, CAT_SURROUNDINGS.categoryId, CAT_TRANSPORT.categoryId));
    }

    public ColorStateList getCategoryColorList() {
        return ColorStateList.valueOf(ResourcesCompat.getColor(CgeoApplication.getInstance().getResources(), categoryColor, null));
    }

    public ColorStateList getCategoryColorNegativeList() {
        return ColorStateList.valueOf(ResourcesCompat.getColor(CgeoApplication.getInstance().getResources(), categoryColorNegative, null));
    }
}
