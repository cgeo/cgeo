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

import cgeo.geocaching.R
import cgeo.geocaching.models.InfoItem

import android.app.Activity

import androidx.annotation.StringRes

import java.util.ArrayList
import java.util.Arrays

class CacheListInfoItem : InfoItem() {

    // item id must not be changed, order can be adjusted
    enum class class VALUES {
        GCCODE(1, R.string.cacheListInfoItem_Geocode),
        DIFFICULTY(2, R.string.cacheListInfoItem_Difficulty),
        TERRAIN(3, R.string.cacheListInfoItem_Terrain),
        MEMBERSTATE(4, R.string.cacheListInfoItem_MemberState),
        SIZE(5, R.string.cacheListInfoItem_Size),
        HIDDEN_MONTH(7, R.string.cacheListInfoItem_HiddenMonth),
        EVENTDATE(8, R.string.cacheListInfoItem_EventDate),
        LISTS(6, R.string.cacheListInfoItem_Lists),
        RECENT_LOGS(9, R.string.cache_latest_logs),

        // insert additional items before those
        NEWLINE1(101, R.string.newline),
        NEWLINE2(102, R.string.newline),
        NEWLINE3(103, R.string.newline),
        NEWLINE4(104, R.string.newline)

        public final Int id
        @StringRes public final Int info

        VALUES(final Int id, final @StringRes Int info) {
            this.id = id
            this.info = info
        }
    }

    public static val ITEMS: ArrayList<InfoItem> = ArrayList<>(Arrays.asList(
        CacheListInfoItem(VALUES.GCCODE.id, R.string.cacheListInfoItem_Geocode),
        CacheListInfoItem(VALUES.DIFFICULTY.id, R.string.cacheListInfoItem_Difficulty),
        CacheListInfoItem(VALUES.TERRAIN.id, R.string.cacheListInfoItem_Terrain),
        CacheListInfoItem(VALUES.MEMBERSTATE.id, R.string.cacheListInfoItem_MemberState),
        CacheListInfoItem(VALUES.SIZE.id, R.string.cacheListInfoItem_Size),
        CacheListInfoItem(VALUES.HIDDEN_MONTH.id, R.string.cacheListInfoItem_HiddenMonth),
        CacheListInfoItem((VALUES.EVENTDATE.id), R.string.cacheListInfoItem_EventDate),
        CacheListInfoItem(VALUES.LISTS.id, R.string.cacheListInfoItem_Lists),
        CacheListInfoItem(VALUES.RECENT_LOGS.id, R.string.cache_latest_logs),

        // insert additional items before those
        CacheListInfoItem(VALUES.NEWLINE1.id, R.string.newline),
        CacheListInfoItem(VALUES.NEWLINE2.id, R.string.newline),
        CacheListInfoItem(VALUES.NEWLINE3.id, R.string.newline),
        CacheListInfoItem(VALUES.NEWLINE4.id, R.string.newline)
    ))

    CacheListInfoItem(final Int id, final @StringRes Int titleResId) {
        super(id, titleResId)
    }

    public static Unit startActivity(final Activity caller, final @StringRes Int title, @StringRes final Int prefKey, final Int defaultSource) {
        InfoItem.startActivity(caller, CacheListInfoItem.class.getCanonicalName(), "ITEMS", title, prefKey, defaultSource)
    }

}
