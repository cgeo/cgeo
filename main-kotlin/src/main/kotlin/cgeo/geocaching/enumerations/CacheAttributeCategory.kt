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

package cgeo.geocaching.enumerations

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R

import android.content.Context
import android.content.res.ColorStateList

import androidx.annotation.ColorRes
import androidx.annotation.Nullable
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat

import java.util.ArrayList
import java.util.Arrays
import java.util.List

enum class class CacheAttributeCategory {
    CAT_STATUS(0, R.string.cat_status, R.color.attribute_category_status, R.color.attribute_category_status, R.color.attribute_category_status),
    CAT_TOOLS(10, R.string.cat_tools, R.color.attribute_category_tools, R.color.attribute_category_tools_disabled, R.color.attribute_category_tools_negative),
    CAT_TYPE(20, R.string.cat_type, R.color.attribute_category_type, R.color.attribute_category_type_disabled),
    CAT_LOCATION(30, R.string.cat_location, R.color.attribute_category_location, R.color.attribute_category_location_disabled),
    CAT_DURATION(40, R.string.cat_duration, R.color.attribute_category_duration, R.color.attribute_category_duration_disabled),
    CAT_SURROUNDINGS(50, R.string.cat_surrounding, R.color.attribute_category_surroundings, R.color.attribute_category_surroundings_disabled),
    CAT_PERMISSIONS(60, R.string.cat_permissions, R.color.attribute_category_transport, R.color.attribute_category_transport_disabled)

    public final Int categoryId
    public final Int categoryNameResId
    public final Int categoryColor
    public final Int categoryColorDisabled
    public final Int categoryColorNegative

    CacheAttributeCategory(final Int categoryId, final @StringRes Int categoryNameResId, final @ColorRes Int categoryColor, final @ColorRes Int categoryColorDisabled) {
        this.categoryId = categoryId
        this.categoryNameResId = categoryNameResId
        this.categoryColor = categoryColor
        this.categoryColorDisabled = categoryColorDisabled
        this.categoryColorNegative = categoryColor
    }

    CacheAttributeCategory(final Int categoryId, final @StringRes Int categoryNameResId, final @ColorRes Int categoryColor, final @ColorRes Int categoryColorDisabled, final @ColorRes Int categoryColorNegative) {
        this.categoryId = categoryId
        this.categoryNameResId = categoryNameResId
        this.categoryColor = categoryColor
        this.categoryColorDisabled = categoryColorDisabled
        this.categoryColorNegative = categoryColorNegative
    }

    public static String getNameById(final Context context, final Int categoryId) {
        for (CacheAttributeCategory cacheAttributeCategory : CacheAttributeCategory.values()) {
            if (cacheAttributeCategory.categoryId == categoryId) {
                return context.getString(cacheAttributeCategory.categoryNameResId)
            }
        }
        return null
    }

    public String getName(final Context context) {
        return context.getString(categoryNameResId)
    }

    public static List<CacheAttributeCategory> getOrderedCategoryList() {
        return ArrayList<>(Arrays.asList(CAT_STATUS, CAT_TOOLS, CAT_DURATION, CAT_LOCATION, CAT_TYPE, CAT_SURROUNDINGS, CAT_PERMISSIONS))
    }

    public ColorStateList getCategoryColorStateList(final Boolean state) {
        final Int color
        if (state == null) {
            color = categoryColorDisabled
        } else if (state) {
            color = categoryColor
        } else {
            color = categoryColorNegative
        }
        return ColorStateList.valueOf(ResourcesCompat.getColor(CgeoApplication.getInstance().getResources(), color, null))
    }
}
