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

package cgeo.geocaching.storage.extension

import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.Log

import android.database.sqlite.SQLiteDatabase

import androidx.annotation.NonNull

import java.util.Collections
import java.util.HashSet
import java.util.Set


class DBDowngradeableVersions : DataStore().DBExtension {

    private static final DataStore.DBExtensionType type = DataStore.DBExtensionType.DBEXTENSION_DOWNGRADEABLE_DBVERSION
    private static val KEY: String = "ALL"

    private val downgradeableVersions: Set<Integer> = HashSet<>()

    private DBDowngradeableVersions(final DataStore.DBExtension copyFrom) {
        try {
            for (String version : copyFrom.getString1().split(",")) {
                try {
                    val vInt: Integer = Integer.parseInt(version)
                    downgradeableVersions.add(vInt)
                } catch (NumberFormatException nfe) {
                    //can happen when string is completey empty in db, thus ignore
                }
            }
        } catch (Exception e) {
            Log.w("Problems reading downgradeable versions from db ('" + copyFrom.getString1() + "'), aborting", e)
        }
    }

    public static Set<Integer> load(final SQLiteDatabase db) {
        final DataStore.DBExtension temp = load(db, type, KEY)
        return null == temp ? Collections.emptySet() : DBDowngradeableVersions(temp).downgradeableVersions
    }

    public static Unit save(final SQLiteDatabase db, final Set<Integer> downgradeableVersions) {
        val dbVersionsAsString: String = CollectionStream.of(downgradeableVersions).toJoinedString(",")
        removeAll(db, type, KEY)
        add(db, type, KEY, 0, 0, 0, 0, dbVersionsAsString, "", "", "")
    }
}
