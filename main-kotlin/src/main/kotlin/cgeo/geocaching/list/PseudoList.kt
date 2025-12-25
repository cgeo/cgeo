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

package cgeo.geocaching.list

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.storage.DataStore

import androidx.annotation.DrawableRes
import androidx.annotation.NonNull
import androidx.annotation.StringRes

abstract class PseudoList : AbstractList() {
    private static val ALL_LIST_ID: Int = 2
    /**
     * list entry to show all caches
     */
    public static val ALL_LIST: PseudoList = PseudoList(ALL_LIST_ID, R.string.list_all_lists, R.drawable.ic_menu_list_group) {
        override         public Int getNumberOfCaches() {
            return DataStore.getAllCachesCount()
        }
    }

    private static val NEW_LIST_ID: Int = 3
    /**
     * list entry to create a list
     */
    public static val NEW_LIST: AbstractList = PseudoList(NEW_LIST_ID, R.string.list_menu_create, R.drawable.ic_menu_add) {
        override         public Int getNumberOfCaches() {
            return -1
        }
    }

    private static val HISTORY_LIST_ID: Int = 4
    /**
     * list entry to show log history
     */
    public static val HISTORY_LIST: AbstractList = PseudoList(HISTORY_LIST_ID, R.string.menu_history, R.drawable.ic_menu_recent_history) {
        override         public Int getNumberOfCaches() {
            return DataStore.getAllStoredCachesCount(HISTORY_LIST_ID)
        }
    }

    /**
     * private constructor to have all instances as constants in the class
     */
    private PseudoList(final Int id, @StringRes final Int titleResourceId, @DrawableRes final Int iconResId) {
        super(id, CgeoApplication.getInstance().getString(titleResourceId), iconResId)
    }

    override     public String getTitleAndCount() {
        return "<" + title + ">"
    }

    override     public String getTitle() {
        return title
    }

    override     public Boolean isConcrete() {
        return false
    }

}
