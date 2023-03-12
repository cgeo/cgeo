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
        GCCODE(1, R.string.cacheListInfoItem_GCCode),
        DIFFICULTY(2, R.string.cacheListInfoItem_Difficulty),
        TERRAIN(3, R.string.cacheListInfoItem_Terrain),
        MEMBERSTATE(4, R.string.cacheListInfoItem_MemberState),
        SIZE(5, R.string.cacheListInfoItem_Size),
        JASMER(7, R.string.cacheListInfoItem_Jasmer),
        EVENTDATE(8, R.string.cacheListInfoItem_EventDate),
        LISTS(6, R.string.cacheListInfoItem_Lists);

        public final int id;
        @StringRes public final int info;

        VALUES(final int id, final @StringRes int info) {
            this.id = id;
            this.info = info;
        }
    }

    public static final ArrayList<InfoItem> ITEMS = new ArrayList<>(Arrays.asList(
        new CacheListInfoItem(VALUES.GCCODE.id, R.string.cacheListInfoItem_GCCode),
        new CacheListInfoItem(VALUES.DIFFICULTY.id, R.string.cacheListInfoItem_Difficulty),
        new CacheListInfoItem(VALUES.TERRAIN.id, R.string.cacheListInfoItem_Terrain),
        new CacheListInfoItem(VALUES.MEMBERSTATE.id, R.string.cacheListInfoItem_MemberState),
        new CacheListInfoItem(VALUES.SIZE.id, R.string.cacheListInfoItem_Size),
        new CacheListInfoItem(VALUES.JASMER.id, R.string.cacheListInfoItem_Jasmer),
        new CacheListInfoItem((VALUES.EVENTDATE.id), R.string.cacheListInfoItem_EventDate),
        new CacheListInfoItem(VALUES.LISTS.id, R.string.cacheListInfoItem_Lists)
    ));

    CacheListInfoItem(final int id, final @StringRes int titleResId) {
        super(id, titleResId);
    }

    public static void startActivity(final Activity caller, final @StringRes int title, @StringRes final int prefKey, final int defaultSource) {
        InfoItem.startActivity(caller, CacheListInfoItem.class.getCanonicalName(), "ITEMS", title, prefKey, defaultSource);
    }

}
