package cgeo.geocaching.enumerations;

import cgeo.geocaching.R;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum CacheAttributeCategory {
    CAT_TOOLS(10, R.string.cat_tools),
    CAT_TYPE(20, R.string.cat_type),
    CAT_LOCATION(30, R.string.cat_location),
    CAT_DURATION(40, R.string.cat_duration),
    CAT_SURROUNDINGS(50, R.string.cat_surrounding),
    CAT_TRANSPORT(60, R.string.cat_transport);

    public final int categoryId;
    public final int categoryNameResId;

    CacheAttributeCategory(final int categoryId, final @StringRes int categoryNameResId) {
        this.categoryId = categoryId;
        this.categoryNameResId = categoryNameResId;
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
}
