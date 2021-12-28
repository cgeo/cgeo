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
    CAT_TOOLS(10, R.string.cat_tools, R.color.attribute_category_tools, R.color.attribute_category_tools_disabled, R.color.attribute_category_tools_negative),
    CAT_TYPE(20, R.string.cat_type, R.color.attribute_category_type, R.color.attribute_category_type_disabled),
    CAT_LOCATION(30, R.string.cat_location, R.color.attribute_category_location, R.color.attribute_category_location_disabled),
    CAT_DURATION(40, R.string.cat_duration, R.color.attribute_category_duration, R.color.attribute_category_duration_disabled),
    CAT_SURROUNDINGS(50, R.string.cat_surrounding, R.color.attribute_category_surroundings, R.color.attribute_category_surroundings_disabled),
    CAT_PERMISSIONS(60, R.string.cat_permissions, R.color.attribute_category_transport, R.color.attribute_category_transport_disabled);

    public final int categoryId;
    public final int categoryNameResId;
    public final int categoryColor;
    public final int categoryColorDisabled;
    public final int categoryColorNegative;

    CacheAttributeCategory(final int categoryId, final @StringRes int categoryNameResId, final @ColorRes int categoryColor, final @ColorRes int categoryColorDisabled) {
        this.categoryId = categoryId;
        this.categoryNameResId = categoryNameResId;
        this.categoryColor = categoryColor;
        this.categoryColorDisabled = categoryColorDisabled;
        this.categoryColorNegative = categoryColor;
    }

    CacheAttributeCategory(final int categoryId, final @StringRes int categoryNameResId, final @ColorRes int categoryColor, final @ColorRes int categoryColorDisabled, final @ColorRes int categoryColorNegative) {
        this.categoryId = categoryId;
        this.categoryNameResId = categoryNameResId;
        this.categoryColor = categoryColor;
        this.categoryColorDisabled = categoryColorDisabled;
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

    @Nullable
    public String getName(final Context context) {
        return context.getString(categoryNameResId);
    }

    public static List<CacheAttributeCategory> getOrderedCategoryList() {
        return new ArrayList<>(Arrays.asList(CAT_TOOLS, CAT_DURATION, CAT_LOCATION, CAT_TYPE, CAT_SURROUNDINGS, CAT_PERMISSIONS));
    }

    public ColorStateList getCategoryColorStateList(final Boolean state) {
        final int color;
        if (state == null) {
            color = categoryColorDisabled;
        } else if (state) {
            color = categoryColor;
        } else {
            color = categoryColorNegative;
        }
        return ColorStateList.valueOf(ResourcesCompat.getColor(CgeoApplication.getInstance().getResources(), color, null));
    }
}
