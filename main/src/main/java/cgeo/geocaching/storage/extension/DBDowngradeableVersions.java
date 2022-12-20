package cgeo.geocaching.storage.extension;

import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.Log;

import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class DBDowngradeableVersions extends DataStore.DBExtension {

    private static final DataStore.DBExtensionType type = DataStore.DBExtensionType.DBEXTENSION_DOWNGRADEABLE_DBVERSION;
    private static final String KEY = "ALL";

    private final Set<Integer> downgradeableVersions = new HashSet<>();

    private DBDowngradeableVersions(final DataStore.DBExtension copyFrom) {
        try {
            for (String version : copyFrom.getString1().split(",")) {
                try {
                    final Integer vInt = Integer.parseInt(version);
                    downgradeableVersions.add(vInt);
                } catch (NumberFormatException nfe) {
                    //can happen when string is completey empty in db, thus ignore
                }
            }
        } catch (Exception e) {
            Log.w("Problems reading downgradeable versions from db ('" + copyFrom.getString1() + "'), aborting", e);
        }
    }

    @NonNull
    public static Set<Integer> load(final SQLiteDatabase db) {
        final DataStore.DBExtension temp = load(db, type, KEY);
        return null == temp ? Collections.emptySet() : new DBDowngradeableVersions(temp).downgradeableVersions;
    }

    public static void save(final SQLiteDatabase db, final Set<Integer> downgradeableVersions) {
        final String dbVersionsAsString = CollectionStream.of(downgradeableVersions).toJoinedString(",");
        removeAll(db, type, KEY);
        add(db, type, KEY, 0, 0, 0, 0, dbVersionsAsString, "", "", "");
    }
}
