package cgeo.geocaching.enumerations;

import cgeo.geocaching.R;
import cgeo.geocaching.models.InfoItem;

import android.app.Activity;

import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.Arrays;

public class CacheListInfoItem extends InfoItem {

    // item id must not be changed, order can be adjusted
    public enum VALUES {
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
        NEWLINE4(104, R.string.newline);

        public final int id;
        @StringRes public final int info;

        VALUES(final int id, final @StringRes int info) {
            this.id = id;
            this.info = info;
        }
    }

    public static final ArrayList<InfoItem> ITEMS = new ArrayList<>(Arrays.asList(
        new CacheListInfoItem(VALUES.GCCODE.id, R.string.cacheListInfoItem_Geocode),
        new CacheListInfoItem(VALUES.DIFFICULTY.id, R.string.cacheListInfoItem_Difficulty),
        new CacheListInfoItem(VALUES.TERRAIN.id, R.string.cacheListInfoItem_Terrain),
        new CacheListInfoItem(VALUES.MEMBERSTATE.id, R.string.cacheListInfoItem_MemberState),
        new CacheListInfoItem(VALUES.SIZE.id, R.string.cacheListInfoItem_Size),
        new CacheListInfoItem(VALUES.HIDDEN_MONTH.id, R.string.cacheListInfoItem_HiddenMonth),
        new CacheListInfoItem((VALUES.EVENTDATE.id), R.string.cacheListInfoItem_EventDate),
        new CacheListInfoItem(VALUES.LISTS.id, R.string.cacheListInfoItem_Lists),
        new CacheListInfoItem(VALUES.RECENT_LOGS.id, R.string.cache_latest_logs),

        // insert additional items before those
        new CacheListInfoItem(VALUES.NEWLINE1.id, R.string.newline),
        new CacheListInfoItem(VALUES.NEWLINE2.id, R.string.newline),
        new CacheListInfoItem(VALUES.NEWLINE3.id, R.string.newline),
        new CacheListInfoItem(VALUES.NEWLINE4.id, R.string.newline)
    ));

    CacheListInfoItem(final int id, final @StringRes int titleResId) {
        super(id, titleResId);
    }

    public static void startActivity(final Activity caller, final @StringRes int title, @StringRes final int prefKey, final int defaultSource) {
        InfoItem.startActivity(caller, CacheListInfoItem.class.getCanonicalName(), "ITEMS", title, prefKey, defaultSource);
    }

}
