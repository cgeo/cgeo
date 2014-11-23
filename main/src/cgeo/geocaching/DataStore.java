package cgeo.geocaching;

import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.gc.Tile;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.LoadFlag;
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.list.AbstractList;
import cgeo.geocaching.list.PseudoList;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.search.SearchSuggestionCursor;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.Version;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.android.observables.AndroidObservable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.util.async.Async;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class DataStore {

    private DataStore() {
        // utility class
    }

    public enum StorageLocation {
        HEAP,
        CACHE,
        DATABASE,
    }

    private static final Func1<Cursor,String> GET_STRING_0 = new Func1<Cursor, String>() {
        @Override
        public String call(final Cursor cursor) {
            return cursor.getString(0);
        }
    };

    // Columns and indices for the cache data
    private static final String QUERY_CACHE_DATA =
            "SELECT " +
                    "cg_caches.updated,"            +    // 0
                    "cg_caches.reason,"             +    // 1
                    "cg_caches.detailed,"           +    // 2
                    "cg_caches.detailedupdate,"     +    // 3
                    "cg_caches.visiteddate,"        +    // 4
                    "cg_caches.geocode,"            +    // 5
                    "cg_caches.cacheid,"            +    // 6
                    "cg_caches.guid,"               +    // 7
                    "cg_caches.type,"               +    // 8
                    "cg_caches.name,"               +    // 9
                    "cg_caches.owner,"              +    // 10
                    "cg_caches.owner_real,"         +    // 11
                    "cg_caches.hidden,"             +    // 12
                    "cg_caches.hint,"               +    // 13
                    "cg_caches.size,"               +    // 14
                    "cg_caches.difficulty,"         +    // 15
                    "cg_caches.direction,"          +    // 16
                    "cg_caches.distance,"           +    // 17
                    "cg_caches.terrain,"            +    // 18
                    "cg_caches.location,"           +    // 19
                    "cg_caches.personal_note,"      +    // 20
                    "cg_caches.shortdesc,"          +    // 21
                    "cg_caches.favourite_cnt,"      +    // 22
                    "cg_caches.rating,"             +    // 23
                    "cg_caches.votes,"              +    // 24
                    "cg_caches.myvote,"             +    // 25
                    "cg_caches.disabled,"           +    // 26
                    "cg_caches.archived,"           +    // 27
                    "cg_caches.members,"            +    // 28
                    "cg_caches.found,"              +    // 29
                    "cg_caches.favourite,"          +    // 30
                    "cg_caches.inventoryunknown,"   +    // 31
                    "cg_caches.onWatchlist,"        +    // 32
                    "cg_caches.reliable_latlon,"    +    // 33
                    "cg_caches.coordsChanged,"      +    // 34
                    "cg_caches.latitude,"           +    // 35
                    "cg_caches.longitude,"          +    // 36
                    "cg_caches.finalDefined,"       +    // 37
                    "cg_caches._id,"                +    // 38
                    "cg_caches.inventorycoins,"     +    // 39
                    "cg_caches.inventorytags,"      +    // 40
                    "cg_caches.logPasswordRequired";     // 41

    /** The list of fields needed for mapping. */
    private static final String[] WAYPOINT_COLUMNS = new String[] { "_id", "geocode", "updated", "type", "prefix", "lookup", "name", "latitude", "longitude", "note", "own", "visited" };

    /** Number of days (as ms) after temporarily saved caches are deleted */
    private final static long DAYS_AFTER_CACHE_IS_DELETED = 3 * 24 * 60 * 60 * 1000;

    /**
     * holds the column indexes of the cache table to avoid lookups
     */
    private static final CacheCache cacheCache = new CacheCache();
    private static SQLiteDatabase database = null;
    private static final int dbVersion = 68;
    public static final int customListIdOffset = 10;
    private static final @NonNull String dbName = "data";
    private static final @NonNull String dbTableCaches = "cg_caches";
    private static final @NonNull String dbTableLists = "cg_lists";
    private static final @NonNull String dbTableAttributes = "cg_attributes";
    private static final @NonNull String dbTableWaypoints = "cg_waypoints";
    private static final @NonNull String dbTableSpoilers = "cg_spoilers";
    private static final @NonNull String dbTableLogs = "cg_logs";
    private static final @NonNull String dbTableLogCount = "cg_logCount";
    private static final @NonNull String dbTableLogImages = "cg_logImages";
    private static final @NonNull String dbTableLogsOffline = "cg_logs_offline";
    private static final @NonNull String dbTableTrackables = "cg_trackables";
    private static final @NonNull String dbTableSearchDestinationHistory = "cg_search_destination_history";
    private static final @NonNull String dbCreateCaches = ""
            + "create table " + dbTableCaches + " ("
            + "_id integer primary key autoincrement, "
            + "updated long not null, "
            + "detailed integer not null default 0, "
            + "detailedupdate long, "
            + "visiteddate long, "
            + "geocode text unique not null, "
            + "reason integer not null default 0, " // cached, favorite...
            + "cacheid text, "
            + "guid text, "
            + "type text, "
            + "name text, "
            + "owner text, "
            + "owner_real text, "
            + "hidden long, "
            + "hint text, "
            + "size text, "
            + "difficulty float, "
            + "terrain float, "
            + "location text, "
            + "direction double, "
            + "distance double, "
            + "latitude double, "
            + "longitude double, "
            + "reliable_latlon integer, "
            + "personal_note text, "
            + "shortdesc text, "
            + "description text, "
            + "favourite_cnt integer, "
            + "rating float, "
            + "votes integer, "
            + "myvote float, "
            + "disabled integer not null default 0, "
            + "archived integer not null default 0, "
            + "members integer not null default 0, "
            + "found integer not null default 0, "
            + "favourite integer not null default 0, "
            + "inventorycoins integer default 0, "
            + "inventorytags integer default 0, "
            + "inventoryunknown integer default 0, "
            + "onWatchlist integer default 0, "
            + "coordsChanged integer default 0, "
            + "finalDefined integer default 0, "
            + "logPasswordRequired integer default 0"
            + "); ";
    private static final String dbCreateLists = ""
            + "create table " + dbTableLists + " ("
            + "_id integer primary key autoincrement, "
            + "title text not null, "
            + "updated long not null"
            + "); ";
    private static final String dbCreateAttributes = ""
            + "create table " + dbTableAttributes + " ("
            + "_id integer primary key autoincrement, "
            + "geocode text not null, "
            + "updated long not null, " // date of save
            + "attribute text "
            + "); ";

    private static final String dbCreateWaypoints = ""
            + "create table " + dbTableWaypoints + " ("
            + "_id integer primary key autoincrement, "
            + "geocode text not null, "
            + "updated long not null, " // date of save
            + "type text not null default 'waypoint', "
            + "prefix text, "
            + "lookup text, "
            + "name text, "
            + "latitude double, "
            + "longitude double, "
            + "note text, "
            + "own integer default 0, "
            + "visited integer default 0"
            + "); ";
    private static final String dbCreateSpoilers = ""
            + "create table " + dbTableSpoilers + " ("
            + "_id integer primary key autoincrement, "
            + "geocode text not null, "
            + "updated long not null, " // date of save
            + "url text, "
            + "title text, "
            + "description text "
            + "); ";
    private static final String dbCreateLogs = ""
            + "create table " + dbTableLogs + " ("
            + "_id integer primary key autoincrement, "
            + "geocode text not null, "
            + "updated long not null, " // date of save
            + "type integer not null default 4, "
            + "author text, "
            + "log text, "
            + "date long, "
            + "found integer not null default 0, "
            + "friend integer "
            + "); ";

    private static final String dbCreateLogCount = ""
            + "create table " + dbTableLogCount + " ("
            + "_id integer primary key autoincrement, "
            + "geocode text not null, "
            + "updated long not null, " // date of save
            + "type integer not null default 4, "
            + "count integer not null default 0 "
            + "); ";
    private static final String dbCreateLogImages = ""
            + "create table " + dbTableLogImages + " ("
            + "_id integer primary key autoincrement, "
            + "log_id integer not null, "
            + "title text not null, "
            + "url text not null"
            + "); ";
    private static final String dbCreateLogsOffline = ""
            + "create table " + dbTableLogsOffline + " ("
            + "_id integer primary key autoincrement, "
            + "geocode text not null, "
            + "updated long not null, " // date of save
            + "type integer not null default 4, "
            + "log text, "
            + "date long "
            + "); ";
    private static final String dbCreateTrackables = ""
            + "create table " + dbTableTrackables + " ("
            + "_id integer primary key autoincrement, "
            + "updated long not null, " // date of save
            + "tbcode text not null, "
            + "guid text, "
            + "title text, "
            + "owner text, "
            + "released long, "
            + "goal text, "
            + "description text, "
            + "geocode text "
            + "); ";

    private static final String dbCreateSearchDestinationHistory = ""
            + "create table " + dbTableSearchDestinationHistory + " ("
            + "_id integer primary key autoincrement, "
            + "date long not null, "
            + "latitude double, "
            + "longitude double "
            + "); ";

    private static final Observable<Integer> allCachesCountObservable = Observable.create(new OnSubscribe<Integer>() {
        @Override
        public void call(final Subscriber<? super Integer> subscriber) {
            if (isInitialized()) {
                subscriber.onNext(getAllCachesCount());
                subscriber.onCompleted();
            }
        }
    }).timeout(500, TimeUnit.MILLISECONDS).retry(10).subscribeOn(Schedulers.io());

    private static boolean newlyCreatedDatabase = false;
    private static boolean databaseCleaned = false;

    public static void init() {
        if (database != null) {
            return;
        }

        synchronized(DataStore.class) {
            if (database != null) {
                return;
            }
            final DbHelper dbHelper = new DbHelper(new DBContext(CgeoApplication.getInstance()));
            try {
                database = dbHelper.getWritableDatabase();
            } catch (final Exception e) {
                Log.e("DataStore.init: unable to open database for R/W", e);
                recreateDatabase(dbHelper);
            }
        }
    }

    /**
     * Attempt to recreate the database if opening has failed
     *
     * @param dbHelper dbHelper to use to reopen the database
     */
    private static void recreateDatabase(final DbHelper dbHelper) {
        final File dbPath = databasePath();
        final File corruptedPath = new File(LocalStorage.getStorage(), dbPath.getName() + ".corrupted");
        if (LocalStorage.copy(dbPath, corruptedPath)) {
            Log.i("DataStore.init: renamed " + dbPath + " into " + corruptedPath);
        } else {
            Log.e("DataStore.init: unable to rename corrupted database");
        }
        try {
            database = dbHelper.getWritableDatabase();
        } catch (final Exception f) {
            Log.e("DataStore.init: unable to recreate database and open it for R/W", f);
        }
    }

    public static synchronized void closeDb() {
        if (database == null) {
            return;
        }

        cacheCache.removeAllFromCache();
        PreparedStatement.clearPreparedStatements();
        database.close();
        database = null;
    }

    public static File getBackupFileInternal() {
        return new File(LocalStorage.getStorage(), "cgeo.sqlite");
    }

    public static String backupDatabaseInternal() {
        if (!LocalStorage.isExternalStorageAvailable()) {
            Log.w("Database wasn't backed up: no external memory");
            return null;
        }

        final File target = getBackupFileInternal();
        closeDb();
        final boolean backupDone = LocalStorage.copy(databasePath(), target);
        init();

        if (!backupDone) {
            Log.e("Database could not be copied to " + target);
            return null;
        }

        Log.i("Database was copied to " + target);
        return target.getPath();
    }

    /**
     * Move the database to/from external cgdata in a new thread,
     * showing a progress window
     *
     * @param fromActivity
     */
    public static void moveDatabase(final Activity fromActivity) {
        final ProgressDialog dialog = ProgressDialog.show(fromActivity, fromActivity.getString(R.string.init_dbmove_dbmove), fromActivity.getString(R.string.init_dbmove_running), true, false);
        AndroidObservable.bindActivity(fromActivity, Async.fromCallable(new Func0<Boolean>() {
            @Override
            public Boolean call() {
                if (!LocalStorage.isExternalStorageAvailable()) {
                    Log.w("Database was not moved: external memory not available");
                    return false;
                }
                closeDb();

                final File source = databasePath();
                final File target = databaseAlternatePath();
                if (!LocalStorage.copy(source, target)) {
                    Log.e("Database could not be moved to " + target);
                    init();
                    return false;
                }
                if (!FileUtils.delete(source)) {
                    Log.e("Original database could not be deleted during move");
                }
                Settings.setDbOnSDCard(!Settings.isDbOnSDCard());
                Log.i("Database was moved to " + target);

                init();
                return true;
            }
        })).subscribeOn(Schedulers.io()).subscribe(new Action1<Boolean>() {
            @Override
            public void call(final Boolean success) {
                dialog.dismiss();
                final String message = success ? fromActivity.getString(R.string.init_dbmove_success) : fromActivity.getString(R.string.init_dbmove_failed);
                Dialogs.message(fromActivity, R.string.init_dbmove_dbmove, message);
            }
        });
    }

    private static File databasePath(final boolean internal) {
        return new File(internal ? LocalStorage.getInternalDbDirectory() : LocalStorage.getExternalDbDirectory(), dbName);
    }

    private static File databasePath() {
        return databasePath(!Settings.isDbOnSDCard());
    }

    private static File databaseAlternatePath() {
        return databasePath(Settings.isDbOnSDCard());
    }

    public static boolean restoreDatabaseInternal() {
        if (!LocalStorage.isExternalStorageAvailable()) {
            Log.w("Database wasn't restored: no external memory");
            return false;
        }

        final File sourceFile = getBackupFileInternal();
        closeDb();
        final boolean restoreDone = LocalStorage.copy(sourceFile, databasePath());
        init();

        if (restoreDone) {
            Log.i("Database succesfully restored from " + sourceFile.getPath());
        } else {
            Log.e("Could not restore database from " + sourceFile.getPath());
        }

        return restoreDone;
    }

    private static class DBContext extends ContextWrapper {

        public DBContext(final Context base) {
            super(base);
        }

        /**
         * We override the default open/create as it doesn't work on OS 1.6 and
         * causes issues on other devices too.
         */
        @Override
        public SQLiteDatabase openOrCreateDatabase(final String name, final int mode,
                final CursorFactory factory) {
            final File file = new File(name);
            FileUtils.mkdirs(file.getParentFile());
            return SQLiteDatabase.openOrCreateDatabase(file, factory);
        }

    }

    private static class DbHelper extends SQLiteOpenHelper {

        private static boolean firstRun = true;

        DbHelper(final Context context) {
            super(context, databasePath().getPath(), null, dbVersion);
        }

        @Override
        public void onCreate(final SQLiteDatabase db) {
            newlyCreatedDatabase = true;
            db.execSQL(dbCreateCaches);
            db.execSQL(dbCreateLists);
            db.execSQL(dbCreateAttributes);
            db.execSQL(dbCreateWaypoints);
            db.execSQL(dbCreateSpoilers);
            db.execSQL(dbCreateLogs);
            db.execSQL(dbCreateLogCount);
            db.execSQL(dbCreateLogImages);
            db.execSQL(dbCreateLogsOffline);
            db.execSQL(dbCreateTrackables);
            db.execSQL(dbCreateSearchDestinationHistory);

            createIndices(db);
        }

        static private void createIndices(final SQLiteDatabase db) {
            db.execSQL("create index if not exists in_caches_geo on " + dbTableCaches + " (geocode)");
            db.execSQL("create index if not exists in_caches_guid on " + dbTableCaches + " (guid)");
            db.execSQL("create index if not exists in_caches_lat on " + dbTableCaches + " (latitude)");
            db.execSQL("create index if not exists in_caches_lon on " + dbTableCaches + " (longitude)");
            db.execSQL("create index if not exists in_caches_reason on " + dbTableCaches + " (reason)");
            db.execSQL("create index if not exists in_caches_detailed on " + dbTableCaches + " (detailed)");
            db.execSQL("create index if not exists in_caches_type on " + dbTableCaches + " (type)");
            db.execSQL("create index if not exists in_caches_visit_detail on " + dbTableCaches + " (visiteddate, detailedupdate)");
            db.execSQL("create index if not exists in_attr_geo on " + dbTableAttributes + " (geocode)");
            db.execSQL("create index if not exists in_wpts_geo on " + dbTableWaypoints + " (geocode)");
            db.execSQL("create index if not exists in_wpts_geo_type on " + dbTableWaypoints + " (geocode, type)");
            db.execSQL("create index if not exists in_spoil_geo on " + dbTableSpoilers + " (geocode)");
            db.execSQL("create index if not exists in_logs_geo on " + dbTableLogs + " (geocode)");
            db.execSQL("create index if not exists in_logcount_geo on " + dbTableLogCount + " (geocode)");
            db.execSQL("create index if not exists in_logsoff_geo on " + dbTableLogsOffline + " (geocode)");
            db.execSQL("create index if not exists in_trck_geo on " + dbTableTrackables + " (geocode)");
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
            Log.i("Upgrade database from ver. " + oldVersion + " to ver. " + newVersion + ": start");

            try {
                if (db.isReadOnly()) {
                    return;
                }

                db.beginTransaction();

                if (oldVersion <= 0) { // new table
                    dropDatabase(db);
                    onCreate(db);

                    Log.i("Database structure created.");
                }

                if (oldVersion > 0) {
                    db.execSQL("delete from " + dbTableCaches + " where reason = 0");

                    if (oldVersion < 52) { // upgrade to 52
                        try {
                            db.execSQL(dbCreateSearchDestinationHistory);

                            Log.i("Added table " + dbTableSearchDestinationHistory + ".");
                        } catch (final Exception e) {
                            Log.e("Failed to upgrade to ver. 52", e);
                        }
                    }

                    if (oldVersion < 53) { // upgrade to 53
                        try {
                            db.execSQL("alter table " + dbTableCaches + " add column onWatchlist integer");

                            Log.i("Column onWatchlist added to " + dbTableCaches + ".");
                        } catch (final Exception e) {
                            Log.e("Failed to upgrade to ver. 53", e);
                        }
                    }

                    if (oldVersion < 54) { // update to 54
                        try {
                            db.execSQL(dbCreateLogImages);
                        } catch (final Exception e) {
                            Log.e("Failed to upgrade to ver. 54", e);

                        }
                    }

                    if (oldVersion < 55) { // update to 55
                        try {
                            db.execSQL("alter table " + dbTableCaches + " add column personal_note text");
                        } catch (final Exception e) {
                            Log.e("Failed to upgrade to ver. 55", e);
                        }
                    }

                    // make all internal attribute names lowercase
                    // @see issue #299
                    if (oldVersion < 56) { // update to 56
                        try {
                            db.execSQL("update " + dbTableAttributes + " set attribute = " +
                                    "lower(attribute) where attribute like \"%_yes\" " +
                                    "or attribute like \"%_no\"");
                        } catch (final Exception e) {
                            Log.e("Failed to upgrade to ver. 56", e);
                        }
                    }

                    // Create missing indices. See issue #435
                    if (oldVersion < 57) { // update to 57
                        try {
                            db.execSQL("drop index in_a");
                            db.execSQL("drop index in_b");
                            db.execSQL("drop index in_c");
                            db.execSQL("drop index in_d");
                            db.execSQL("drop index in_e");
                            db.execSQL("drop index in_f");
                            createIndices(db);
                        } catch (final Exception e) {
                            Log.e("Failed to upgrade to ver. 57", e);
                        }
                    }

                    if (oldVersion < 58) { // upgrade to 58
                        try {
                            db.beginTransaction();

                            final String dbTableCachesTemp = dbTableCaches + "_temp";
                            final String dbCreateCachesTemp = ""
                                    + "create table " + dbTableCachesTemp + " ("
                                    + "_id integer primary key autoincrement, "
                                    + "updated long not null, "
                                    + "detailed integer not null default 0, "
                                    + "detailedupdate long, "
                                    + "visiteddate long, "
                                    + "geocode text unique not null, "
                                    + "reason integer not null default 0, "
                                    + "cacheid text, "
                                    + "guid text, "
                                    + "type text, "
                                    + "name text, "
                                    + "own integer not null default 0, "
                                    + "owner text, "
                                    + "owner_real text, "
                                    + "hidden long, "
                                    + "hint text, "
                                    + "size text, "
                                    + "difficulty float, "
                                    + "terrain float, "
                                    + "location text, "
                                    + "direction double, "
                                    + "distance double, "
                                    + "latitude double, "
                                    + "longitude double, "
                                    + "reliable_latlon integer, "
                                    + "personal_note text, "
                                    + "shortdesc text, "
                                    + "description text, "
                                    + "favourite_cnt integer, "
                                    + "rating float, "
                                    + "votes integer, "
                                    + "myvote float, "
                                    + "disabled integer not null default 0, "
                                    + "archived integer not null default 0, "
                                    + "members integer not null default 0, "
                                    + "found integer not null default 0, "
                                    + "favourite integer not null default 0, "
                                    + "inventorycoins integer default 0, "
                                    + "inventorytags integer default 0, "
                                    + "inventoryunknown integer default 0, "
                                    + "onWatchlist integer default 0 "
                                    + "); ";

                            db.execSQL(dbCreateCachesTemp);
                            db.execSQL("insert into " + dbTableCachesTemp + " select _id,updated,detailed,detailedupdate,visiteddate,geocode,reason,cacheid,guid,type,name,own,owner,owner_real," +
                                    "hidden,hint,size,difficulty,terrain,location,direction,distance,latitude,longitude, 0," +
                                    "personal_note,shortdesc,description,favourite_cnt,rating,votes,myvote,disabled,archived,members,found,favourite,inventorycoins," +
                                    "inventorytags,inventoryunknown,onWatchlist from " + dbTableCaches);
                            db.execSQL("drop table " + dbTableCaches);
                            db.execSQL("alter table " + dbTableCachesTemp + " rename to " + dbTableCaches);

                            final String dbTableWaypointsTemp = dbTableWaypoints + "_temp";
                            final String dbCreateWaypointsTemp = ""
                                    + "create table " + dbTableWaypointsTemp + " ("
                                    + "_id integer primary key autoincrement, "
                                    + "geocode text not null, "
                                    + "updated long not null, " // date of save
                                    + "type text not null default 'waypoint', "
                                    + "prefix text, "
                                    + "lookup text, "
                                    + "name text, "
                                    + "latitude double, "
                                    + "longitude double, "
                                    + "note text "
                                    + "); ";
                            db.execSQL(dbCreateWaypointsTemp);
                            db.execSQL("insert into " + dbTableWaypointsTemp + " select _id, geocode, updated, type, prefix, lookup, name, latitude, longitude, note from " + dbTableWaypoints);
                            db.execSQL("drop table " + dbTableWaypoints);
                            db.execSQL("alter table " + dbTableWaypointsTemp + " rename to " + dbTableWaypoints);

                            createIndices(db);

                            db.setTransactionSuccessful();

                            Log.i("Removed latitude_string and longitude_string columns");
                        } catch (final Exception e) {
                            Log.e("Failed to upgrade to ver. 58", e);
                        } finally {
                            db.endTransaction();
                        }
                    }

                    if (oldVersion < 59) {
                        try {
                            // Add new indices and remove obsolete cache files
                            createIndices(db);
                            removeObsoleteCacheDirectories(db);
                        } catch (final Exception e) {
                            Log.e("Failed to upgrade to ver. 59", e);
                        }
                    }

                    if (oldVersion < 60) {
                        try {
                            removeSecEmptyDirs();
                        } catch (final Exception e) {
                            Log.e("Failed to upgrade to ver. 60", e);
                        }
                    }
                    if (oldVersion < 61) {
                        try {
                            db.execSQL("alter table " + dbTableLogs + " add column friend integer");
                            db.execSQL("alter table " + dbTableCaches + " add column coordsChanged integer default 0");
                        } catch (final Exception e) {
                            Log.e("Failed to upgrade to ver. 61", e);

                        }
                    }
                    // Introduces finalDefined on caches and own on waypoints
                    if (oldVersion < 62) {
                        try {
                            db.execSQL("alter table " + dbTableCaches + " add column finalDefined integer default 0");
                            db.execSQL("alter table " + dbTableWaypoints + " add column own integer default 0");
                            db.execSQL("update " + dbTableWaypoints + " set own = 1 where type = 'own'");
                        } catch (final Exception e) {
                            Log.e("Failed to upgrade to ver. 62", e);

                        }
                    }
                    if (oldVersion < 63) {
                        try {
                            removeDoubleUnderscoreMapFiles();
                        } catch (final Exception e) {
                            Log.e("Failed to upgrade to ver. 63", e);

                        }
                    }

                    if (oldVersion < 64) {
                        try {
                            // No cache should ever be stored into the ALL_CACHES list. Here we use hardcoded list ids
                            // rather than symbolic ones because the fix must be applied with the values at the time
                            // of the problem. The problem was introduced in release 2012.06.01.
                            db.execSQL("update " + dbTableCaches + " set reason=1 where reason=2");
                        } catch (final Exception e) {
                            Log.e("Failed to upgrade to ver. 64", e);
                        }
                    }

                    if (oldVersion < 65) {
                        try {
                            // Set all waypoints where name is Original coordinates to type ORIGINAL
                            db.execSQL("update " + dbTableWaypoints + " set type='original', own=0 where name='Original Coordinates'");
                        } catch (final Exception e) {
                            Log.e("Failed to upgrade to ver. 65:", e);
                        }
                    }
                    // Introduces visited feature on waypoints
                    if (oldVersion < 66) {
                        try {
                            db.execSQL("alter table " + dbTableWaypoints + " add column visited integer default 0");
                        } catch (final Exception e) {
                            Log.e("Failed to upgrade to ver. 66", e);

                        }
                    }
                    // issue2662 OC: Leichtes Klettern / Easy climbing
                    if (oldVersion < 67) {
                        try {
                            db.execSQL("update " + dbTableAttributes + " set attribute = 'easy_climbing_yes' where geocode like 'OC%' and attribute = 'climbing_yes'");
                            db.execSQL("update " + dbTableAttributes + " set attribute = 'easy_climbing_no' where geocode like 'OC%' and attribute = 'climbing_no'");
                        } catch (final Exception e) {
                            Log.e("Failed to upgrade to ver. 67", e);

                        }
                    }
                    // Introduces logPasswordRequired on caches
                    if (oldVersion < 68) {
                        try {
                            db.execSQL("alter table " + dbTableCaches + " add column logPasswordRequired integer default 0");
                        } catch (final Exception e) {
                            Log.e("Failed to upgrade to ver. 68", e);

                        }
                    }
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            Log.i("Upgrade database from ver. " + oldVersion + " to ver. " + newVersion + ": completed");
        }

        @Override
        public void onOpen(final SQLiteDatabase db) {
            if (firstRun) {
                sanityChecks(db);
                firstRun = false;
            }
        }

        /**
         * Execute sanity checks that should be performed once per application after the database has been
         * opened.
         *
         * @param db the database to perform sanity checks against
         */
        private static void sanityChecks(final SQLiteDatabase db) {
            // Check that the history of searches is well formed as some dates seem to be missing according
            // to NPE traces.
            final int staleHistorySearches = db.delete(dbTableSearchDestinationHistory, "date is null", null);
            if (staleHistorySearches > 0) {
                Log.w(String.format(Locale.getDefault(), "DataStore.dbHelper.onOpen: removed %d bad search history entries", staleHistorySearches));
            }
        }

        /**
         * Method to remove static map files with double underscore due to issue#1670
         * introduced with release on 2012-05-24.
         */
        private static void removeDoubleUnderscoreMapFiles() {
            final File[] geocodeDirs = LocalStorage.getStorage().listFiles();
            if (ArrayUtils.isNotEmpty(geocodeDirs)) {
                final FilenameFilter filter = new FilenameFilter() {
                    @Override
                    public boolean accept(final File dir, final String filename) {
                        return filename.startsWith("map_") && filename.contains("__");
                    }
                };
                for (final File dir : geocodeDirs) {
                    final File[] wrongFiles = dir.listFiles(filter);
                    if (wrongFiles != null) {
                        for (final File wrongFile : wrongFiles) {
                            FileUtils.deleteIgnoringFailure(wrongFile);
                        }
                    }
                }
            }
        }
    }

    /**
     * Remove obsolete cache directories in c:geo private storage.
     */
    public static void removeObsoleteCacheDirectories() {
        removeObsoleteCacheDirectories(database);
    }

    /**
     * Remove obsolete cache directories in c:geo private storage.
     *
     * @param db
     *            the read-write database to use
     */
    private static void removeObsoleteCacheDirectories(final SQLiteDatabase db) {
        final File[] files = LocalStorage.getStorage().listFiles();
        if (ArrayUtils.isNotEmpty(files)) {
            final Pattern oldFilePattern = Pattern.compile("^[GC|TB|EC|GK|O][A-Z0-9]{4,7}$");
            final SQLiteStatement select = db.compileStatement("select count(*) from " + dbTableCaches + " where geocode = ?");
            final ArrayList<File> toRemove = new ArrayList<>(files.length);
            for (final File file : files) {
                if (file.isDirectory()) {
                    final String geocode = file.getName();
                    if (oldFilePattern.matcher(geocode).find()) {
                        select.bindString(1, geocode);
                        if (select.simpleQueryForLong() == 0) {
                            toRemove.add(file);
                        }
                    }
                }
            }

            // Use a background thread for the real removal to avoid keeping the database locked
            // if we are called from within a transaction.
            Schedulers.io().createWorker().schedule(new Action0() {
                @Override
                public void call() {
                    for (final File dir : toRemove) {
                        Log.i("Removing obsolete cache directory for " + dir.getName());
                        LocalStorage.deleteDirectory(dir);
                    }
                }
            });
        }
    }

    /*
     * Remove empty directories created in the secondary storage area.
     */
    private static void removeSecEmptyDirs() {
        final File[] files = LocalStorage.getStorageSec().listFiles();
        if (ArrayUtils.isNotEmpty(files)) {
            for (final File file : files) {
                if (file.isDirectory()) {
                    // This will silently fail if the directory is not empty.
                    FileUtils.deleteIgnoringFailure(file);
                }
            }
        }
    }

    private static void dropDatabase(final SQLiteDatabase db) {
        db.execSQL("drop table if exists " + dbTableCaches);
        db.execSQL("drop table if exists " + dbTableAttributes);
        db.execSQL("drop table if exists " + dbTableWaypoints);
        db.execSQL("drop table if exists " + dbTableSpoilers);
        db.execSQL("drop table if exists " + dbTableLogs);
        db.execSQL("drop table if exists " + dbTableLogCount);
        db.execSQL("drop table if exists " + dbTableLogsOffline);
        db.execSQL("drop table if exists " + dbTableTrackables);
    }

    public static boolean isThere(final String geocode, final String guid, final boolean detailed, final boolean checkTime) {
        init();

        long dataUpdated = 0;
        long dataDetailedUpdate = 0;
        int dataDetailed = 0;

        try {
            final Cursor cursor;

            if (StringUtils.isNotBlank(geocode)) {
                cursor = database.query(
                        dbTableCaches,
                        new String[]{"detailed", "detailedupdate", "updated"},
                        "geocode = ?",
                        new String[]{geocode},
                        null,
                        null,
                        null,
                        "1");
            } else if (StringUtils.isNotBlank(guid)) {
                cursor = database.query(
                        dbTableCaches,
                        new String[]{"detailed", "detailedupdate", "updated"},
                        "guid = ?",
                        new String[]{guid},
                        null,
                        null,
                        null,
                        "1");
            } else {
                return false;
            }

            if (cursor.moveToFirst()) {
                dataDetailed = cursor.getInt(0);
                dataDetailedUpdate = cursor.getLong(1);
                dataUpdated = cursor.getLong(2);
            }

            cursor.close();
        } catch (final Exception e) {
            Log.e("DataStore.isThere", e);
        }

        if (detailed && dataDetailed == 0) {
            // we want details, but these are not stored
            return false;
        }

        if (checkTime && detailed && dataDetailedUpdate < (System.currentTimeMillis() - DAYS_AFTER_CACHE_IS_DELETED)) {
            // we want to check time for detailed cache, but data are older than 3 hours
            return false;
        }

        if (checkTime && !detailed && dataUpdated < (System.currentTimeMillis() - DAYS_AFTER_CACHE_IS_DELETED)) {
            // we want to check time for short cache, but data are older than 3 hours
            return false;
        }

        // we have some cache
        return true;
    }

    /** is cache stored in one of the lists (not only temporary) */
    public static boolean isOffline(final String geocode, final String guid) {
        if (StringUtils.isBlank(geocode) && StringUtils.isBlank(guid)) {
            return false;
        }
        init();

        try {
            final SQLiteStatement listId;
            final String value;
            if (StringUtils.isNotBlank(geocode)) {
                listId = PreparedStatement.LIST_ID_OF_GEOCODE.getStatement();
                value = geocode;
            }
            else {
                listId = PreparedStatement.LIST_ID_OF_GUID.getStatement();
                value = guid;
            }
            synchronized (listId) {
                listId.bindString(1, value);
                return listId.simpleQueryForLong() != StoredList.TEMPORARY_LIST.id;
            }
        } catch (final SQLiteDoneException ignored) {
            // Do nothing, it only means we have no information on the cache
        } catch (final Exception e) {
            Log.e("DataStore.isOffline", e);
        }

        return false;
    }

    public static String getGeocodeForGuid(final String guid) {
        if (StringUtils.isBlank(guid)) {
            return null;
        }
        init();

        try {
            final SQLiteStatement description = PreparedStatement.GEOCODE_OF_GUID.getStatement();
            synchronized (description) {
                description.bindString(1, guid);
                return description.simpleQueryForString();
            }
        } catch (final SQLiteDoneException ignored) {
            // Do nothing, it only means we have no information on the cache
        } catch (final Exception e) {
            Log.e("DataStore.getGeocodeForGuid", e);
        }

        return null;
    }

    /**
     * Save/store a cache to the CacheCache
     *
     * @param cache
     *            the Cache to save in the CacheCache/DB
     * @param saveFlags
     *
     */
    public static void saveCache(final Geocache cache, final Set<LoadFlags.SaveFlag> saveFlags) {
        saveCaches(Collections.singletonList(cache), saveFlags);
    }

    /**
     * Save/store a cache to the CacheCache
     *
     * @param caches
     *            the caches to save in the CacheCache/DB
     * @param saveFlags
     *
     */
    public static void saveCaches(final Collection<Geocache> caches, final Set<LoadFlags.SaveFlag> saveFlags) {
        if (CollectionUtils.isEmpty(caches)) {
            return;
        }
        final ArrayList<String> cachesFromDatabase = new ArrayList<>();
        final HashMap<String, Geocache> existingCaches = new HashMap<>();

        // first check which caches are in the memory cache
        for (final Geocache cache : caches) {
            final String geocode = cache.getGeocode();
            final Geocache cacheFromCache = cacheCache.getCacheFromCache(geocode);
            if (cacheFromCache == null) {
                cachesFromDatabase.add(geocode);
            }
            else {
                existingCaches.put(geocode, cacheFromCache);
            }
        }

        // then load all remaining caches from the database in one step
        for (final Geocache cacheFromDatabase : loadCaches(cachesFromDatabase, LoadFlags.LOAD_ALL_DB_ONLY)) {
            existingCaches.put(cacheFromDatabase.getGeocode(), cacheFromDatabase);
        }

        final ArrayList<Geocache> toBeStored = new ArrayList<>();
        // Merge with the data already stored in the CacheCache or in the database if
        // the cache had not been loaded before, and update the CacheCache.
        // Also, a DB update is required if the merge data comes from the CacheCache
        // (as it may be more recent than the version in the database), or if the
        // version coming from the database is different than the version we are entering
        // into the cache (that includes absence from the database).
        for (final Geocache cache : caches) {
            final String geocode = cache.getGeocode();
            final Geocache existingCache = existingCaches.get(geocode);
            final boolean dbUpdateRequired = !cache.gatherMissingFrom(existingCache) || cacheCache.getCacheFromCache(geocode) != null;
            cache.addStorageLocation(StorageLocation.CACHE);
            cacheCache.putCacheInCache(cache);

            // Only save the cache in the database if it is requested by the caller and
            // the cache contains detailed information.
            if (saveFlags.contains(SaveFlag.DB) && cache.isDetailed() && dbUpdateRequired) {
                toBeStored.add(cache);
            }
        }

        for (final Geocache geocache : toBeStored) {
            storeIntoDatabase(geocache);
        }
    }

    private static boolean storeIntoDatabase(final Geocache cache) {
        cache.addStorageLocation(StorageLocation.DATABASE);
        cacheCache.putCacheInCache(cache);
        Log.d("Saving " + cache.toString() + " (" + cache.getListId() + ") to DB");

        final ContentValues values = new ContentValues();

        if (cache.getUpdated() == 0) {
            values.put("updated", System.currentTimeMillis());
        } else {
            values.put("updated", cache.getUpdated());
        }
        values.put("reason", cache.getListId());
        values.put("detailed", cache.isDetailed() ? 1 : 0);
        values.put("detailedupdate", cache.getDetailedUpdate());
        values.put("visiteddate", cache.getVisitedDate());
        values.put("geocode", cache.getGeocode());
        values.put("cacheid", cache.getCacheId());
        values.put("guid", cache.getGuid());
        values.put("type", cache.getType().id);
        values.put("name", cache.getName());
        values.put("owner", cache.getOwnerDisplayName());
        values.put("owner_real", cache.getOwnerUserId());
        final Date hiddenDate = cache.getHiddenDate();
        if (hiddenDate == null) {
            values.put("hidden", 0);
        } else {
            values.put("hidden", hiddenDate.getTime());
        }
        values.put("hint", cache.getHint());
        values.put("size", cache.getSize() == null ? "" : cache.getSize().id);
        values.put("difficulty", cache.getDifficulty());
        values.put("terrain", cache.getTerrain());
        values.put("location", cache.getLocation());
        values.put("distance", cache.getDistance());
        values.put("direction", cache.getDirection());
        putCoords(values, cache.getCoords());
        values.put("reliable_latlon", cache.isReliableLatLon() ? 1 : 0);
        values.put("shortdesc", cache.getShortDescription());
        values.put("personal_note", cache.getPersonalNote());
        values.put("description", cache.getDescription());
        values.put("favourite_cnt", cache.getFavoritePoints());
        values.put("rating", cache.getRating());
        values.put("votes", cache.getVotes());
        values.put("myvote", cache.getMyVote());
        values.put("disabled", cache.isDisabled() ? 1 : 0);
        values.put("archived", cache.isArchived() ? 1 : 0);
        values.put("members", cache.isPremiumMembersOnly() ? 1 : 0);
        values.put("found", cache.isFound() ? 1 : 0);
        values.put("favourite", cache.isFavorite() ? 1 : 0);
        values.put("inventoryunknown", cache.getInventoryItems());
        values.put("onWatchlist", cache.isOnWatchlist() ? 1 : 0);
        values.put("coordsChanged", cache.hasUserModifiedCoords() ? 1 : 0);
        values.put("finalDefined", cache.hasFinalDefined() ? 1 : 0);
        values.put("logPasswordRequired", cache.isLogPasswordRequired() ? 1 : 0);

        init();

        //try to update record else insert fresh..
        database.beginTransaction();

        try {
            saveAttributesWithoutTransaction(cache);
            saveWaypointsWithoutTransaction(cache);
            saveSpoilersWithoutTransaction(cache);
            saveLogCountsWithoutTransaction(cache);
            saveInventoryWithoutTransaction(cache.getGeocode(), cache.getInventory());

            final int rows = database.update(dbTableCaches, values, "geocode = ?", new String[] { cache.getGeocode() });
            if (rows == 0) {
                // cache is not in the DB, insert it
                /* long id = */
                database.insert(dbTableCaches, null, values);
            }
            database.setTransactionSuccessful();
            return true;
        } catch (final Exception e) {
            Log.e("SaveCache", e);
        } finally {
            database.endTransaction();
        }

        return false;
    }

    private static void saveAttributesWithoutTransaction(final Geocache cache) {
        final String geocode = cache.getGeocode();

        // The attributes must be fetched first because lazy loading may load
        // a null set otherwise.
        final List<String> attributes = cache.getAttributes();
        database.delete(dbTableAttributes, "geocode = ?", new String[]{geocode});

        if (attributes.isEmpty()) {
            return;
        }
        final SQLiteStatement statement = PreparedStatement.INSERT_ATTRIBUTE.getStatement();
        final long timestamp = System.currentTimeMillis();
        for (final String attribute : attributes) {
            statement.bindString(1, geocode);
            statement.bindLong(2, timestamp);
            statement.bindString(3, attribute);

            statement.executeInsert();
        }
    }

    /**
     * Persists the given <code>destination</code> into the database.
     *
     * @param destination
     *            a destination to save
     */
    public static void saveSearchedDestination(final Destination destination) {
        init();

        database.beginTransaction();

        try {
            final SQLiteStatement insertDestination = PreparedStatement.INSERT_SEARCH_DESTINATION.getStatement();
            insertDestination.bindLong(1, destination.getDate());
            final Geopoint coords = destination.getCoords();
            insertDestination.bindDouble(2, coords.getLatitude());
            insertDestination.bindDouble(3, coords.getLongitude());

            insertDestination.executeInsert();
            database.setTransactionSuccessful();
        } catch (final Exception e) {
            Log.e("Updating searchedDestinations db failed", e);
        } finally {
            database.endTransaction();
        }
    }

    public static boolean saveWaypoints(final Geocache cache) {
        init();
        database.beginTransaction();

        try {
            saveWaypointsWithoutTransaction(cache);
            database.setTransactionSuccessful();
            return true;
        } catch (final Exception e) {
            Log.e("saveWaypoints", e);
        } finally {
            database.endTransaction();
        }
        return false;
    }

    private static void saveWaypointsWithoutTransaction(final Geocache cache) {
        final String geocode = cache.getGeocode();

        final List<Waypoint> waypoints = cache.getWaypoints();
        if (CollectionUtils.isNotEmpty(waypoints)) {
            final ArrayList<String> currentWaypointIds = new ArrayList<>();
            final ContentValues values = new ContentValues();
            final long timeStamp = System.currentTimeMillis();
            for (final Waypoint oneWaypoint : waypoints) {

                values.clear();
                values.put("geocode", geocode);
                values.put("updated", timeStamp);
                values.put("type", oneWaypoint.getWaypointType() != null ? oneWaypoint.getWaypointType().id : null);
                values.put("prefix", oneWaypoint.getPrefix());
                values.put("lookup", oneWaypoint.getLookup());
                values.put("name", oneWaypoint.getName());
                putCoords(values, oneWaypoint.getCoords());
                values.put("note", oneWaypoint.getNote());
                values.put("own", oneWaypoint.isUserDefined() ? 1 : 0);
                values.put("visited", oneWaypoint.isVisited() ? 1 : 0);
                if (oneWaypoint.getId() < 0) {
                    final long rowId = database.insert(dbTableWaypoints, null, values);
                    oneWaypoint.setId((int) rowId);
                } else {
                    database.update(dbTableWaypoints, values, "_id = ?", new String[] { Integer.toString(oneWaypoint.getId(), 10) });
                }
                currentWaypointIds.add(Integer.toString(oneWaypoint.getId()));
            }

            removeOutdatedWaypointsOfCache(cache, currentWaypointIds);
        }
    }

    /**
     * remove all waypoints of the given cache, where the id is not in the given list
     *
     * @param cache
     * @param remainingWaypointIds
     *            ids of waypoints which shall not be deleted
     */
    private static void removeOutdatedWaypointsOfCache(final @NonNull Geocache cache, final @NonNull Collection<String> remainingWaypointIds) {
        final String idList = StringUtils.join(remainingWaypointIds, ',');
        database.delete(dbTableWaypoints, "geocode = ? AND _id NOT in (" + idList + ")", new String[] { cache.getGeocode() });
    }

    /**
     * Save coordinates into a ContentValues
     *
     * @param values
     *            a ContentValues to save coordinates in
     * @param coords
     *            coordinates to save, or null to save empty coordinates
     */
    private static void putCoords(final ContentValues values, final Geopoint coords) {
        values.put("latitude", coords == null ? null : coords.getLatitude());
        values.put("longitude", coords == null ? null : coords.getLongitude());
    }

    /**
     * Retrieve coordinates from a Cursor
     *
     * @param cursor
     *            a Cursor representing a row in the database
     * @param indexLat
     *            index of the latitude column
     * @param indexLon
     *            index of the longitude column
     * @return the coordinates, or null if latitude or longitude is null or the coordinates are invalid
     */
    private static Geopoint getCoords(final Cursor cursor, final int indexLat, final int indexLon) {
        if (cursor.isNull(indexLat) || cursor.isNull(indexLon)) {
            return null;
        }

        return new Geopoint(cursor.getDouble(indexLat), cursor.getDouble(indexLon));
    }

    private static boolean saveWaypointInternal(final int id, final String geocode, final Waypoint waypoint) {
        if ((StringUtils.isBlank(geocode) && id <= 0) || waypoint == null) {
            return false;
        }

        init();

        database.beginTransaction();
        boolean ok = false;
        try {
            final ContentValues values = new ContentValues();
            values.put("geocode", geocode);
            values.put("updated", System.currentTimeMillis());
            values.put("type", waypoint.getWaypointType() != null ? waypoint.getWaypointType().id : null);
            values.put("prefix", waypoint.getPrefix());
            values.put("lookup", waypoint.getLookup());
            values.put("name", waypoint.getName());
            putCoords(values, waypoint.getCoords());
            values.put("note", waypoint.getNote());
            values.put("own", waypoint.isUserDefined() ? 1 : 0);
            values.put("visited", waypoint.isVisited() ? 1 : 0);
            if (id <= 0) {
                final long rowId = database.insert(dbTableWaypoints, null, values);
                waypoint.setId((int) rowId);
                ok = true;
            } else {
                final int rows = database.update(dbTableWaypoints, values, "_id = " + id, null);
                ok = rows > 0;
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

        return ok;
    }

    public static boolean deleteWaypoint(final int id) {
        if (id == 0) {
            return false;
        }

        init();

        return database.delete(dbTableWaypoints, "_id = " + id, null) > 0;
    }

    private static void saveSpoilersWithoutTransaction(final Geocache cache) {
        final String geocode = cache.getGeocode();
        database.delete(dbTableSpoilers, "geocode = ?", new String[]{geocode});

        final List<Image> spoilers = cache.getSpoilers();
        if (CollectionUtils.isNotEmpty(spoilers)) {
            final SQLiteStatement insertSpoiler = PreparedStatement.INSERT_SPOILER.getStatement();
            final long timestamp = System.currentTimeMillis();
            for (final Image spoiler : spoilers) {
                insertSpoiler.bindString(1, geocode);
                insertSpoiler.bindLong(2, timestamp);
                insertSpoiler.bindString(3, spoiler.getUrl());
                insertSpoiler.bindString(4, spoiler.getTitle());
                final String description = spoiler.getDescription();
                if (description != null) {
                    insertSpoiler.bindString(5, description);
                }
                else {
                    insertSpoiler.bindNull(5);
                }
                insertSpoiler.executeInsert();
            }
        }
    }

    public static void saveLogsWithoutTransaction(final String geocode, final Iterable<LogEntry> logs) {
        // TODO delete logimages referring these logs
        database.delete(dbTableLogs, "geocode = ?", new String[]{geocode});

        final SQLiteStatement insertLog = PreparedStatement.INSERT_LOG.getStatement();
        final long timestamp = System.currentTimeMillis();
        for (final LogEntry log : logs) {
            insertLog.bindString(1, geocode);
            insertLog.bindLong(2, timestamp);
            insertLog.bindLong(3, log.type.id);
            insertLog.bindString(4, log.author);
            insertLog.bindString(5, log.log);
            insertLog.bindLong(6, log.date);
            insertLog.bindLong(7, log.found);
            insertLog.bindLong(8, log.friend ? 1 : 0);
            final long logId = insertLog.executeInsert();
            if (log.hasLogImages()) {
                final SQLiteStatement insertImage = PreparedStatement.INSERT_LOG_IMAGE.getStatement();
                for (final Image img : log.getLogImages()) {
                    insertImage.bindLong(1, logId);
                    insertImage.bindString(2, img.getTitle());
                    insertImage.bindString(3, img.getUrl());
                    insertImage.executeInsert();
                }
            }
        }
    }

    private static void saveLogCountsWithoutTransaction(final Geocache cache) {
        final String geocode = cache.getGeocode();
        database.delete(dbTableLogCount, "geocode = ?", new String[]{geocode});

        final Map<LogType, Integer> logCounts = cache.getLogCounts();
        if (MapUtils.isNotEmpty(logCounts)) {
            final Set<Entry<LogType, Integer>> logCountsItems = logCounts.entrySet();
            final SQLiteStatement insertLogCounts = PreparedStatement.INSERT_LOG_COUNTS.getStatement();
            final long timestamp = System.currentTimeMillis();
            for (final Entry<LogType, Integer> pair : logCountsItems) {
                insertLogCounts.bindString(1, geocode);
                insertLogCounts.bindLong(2, timestamp);
                insertLogCounts.bindLong(3, pair.getKey().id);
                insertLogCounts.bindLong(4, pair.getValue());

                insertLogCounts.executeInsert();
            }
        }
    }

    public static void saveTrackable(final Trackable trackable) {
        init();

        database.beginTransaction();
        try {
            saveInventoryWithoutTransaction(null, Collections.singletonList(trackable));
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private static void saveInventoryWithoutTransaction(final String geocode, final List<Trackable> trackables) {
        if (geocode != null) {
            database.delete(dbTableTrackables, "geocode = ?", new String[]{geocode});
        }

        if (CollectionUtils.isNotEmpty(trackables)) {
            final ContentValues values = new ContentValues();
            final long timeStamp = System.currentTimeMillis();
            for (final Trackable trackable : trackables) {
                final String tbCode = trackable.getGeocode();
                if (StringUtils.isNotBlank(tbCode)) {
                    database.delete(dbTableTrackables, "tbcode = ?", new String[] { tbCode });
                }
                values.clear();
                if (geocode != null) {
                    values.put("geocode", geocode);
                }
                values.put("updated", timeStamp);
                values.put("tbcode", tbCode);
                values.put("guid", trackable.getGuid());
                values.put("title", trackable.getName());
                values.put("owner", trackable.getOwner());
                if (trackable.getReleased() != null) {
                    values.put("released", trackable.getReleased().getTime());
                } else {
                    values.put("released", 0L);
                }
                values.put("goal", trackable.getGoal());
                values.put("description", trackable.getDetails());

                database.insert(dbTableTrackables, null, values);

                saveLogsWithoutTransaction(tbCode, trackable.getLogs());
            }
        }
    }

    public static Viewport getBounds(final Set<String> geocodes) {
        if (CollectionUtils.isEmpty(geocodes)) {
            return null;
        }

        final Set<Geocache> caches = loadCaches(geocodes, LoadFlags.LOAD_CACHE_OR_DB);
        return Viewport.containing(caches);
    }

    /**
     * Load a single Cache.
     *
     * @param geocode
     *            The Geocode GCXXXX
     * @return the loaded cache (if found). Can be null
     */
    public static Geocache loadCache(final String geocode, final EnumSet<LoadFlag> loadFlags) {
        if (StringUtils.isBlank(geocode)) {
            throw new IllegalArgumentException("geocode must not be empty");
        }

        final Set<Geocache> caches = loadCaches(Collections.singleton(geocode), loadFlags);
        return caches.isEmpty() ? null : caches.iterator().next();
    }

    /**
     * Load caches.
     *
     * @param geocodes
     * @return Set of loaded caches. Never null.
     */
    public static Set<Geocache> loadCaches(final Collection<String> geocodes, final EnumSet<LoadFlag> loadFlags) {
        if (CollectionUtils.isEmpty(geocodes)) {
            return new HashSet<>();
        }

        final Set<Geocache> result = new HashSet<>(geocodes.size());
        final Set<String> remaining = new HashSet<>(geocodes);

        if (loadFlags.contains(LoadFlag.CACHE_BEFORE)) {
            for (final String geocode : new HashSet<>(remaining)) {
                final Geocache cache = cacheCache.getCacheFromCache(geocode);
                if (cache != null) {
                    result.add(cache);
                    remaining.remove(cache.getGeocode());
                }
            }
        }

        if (loadFlags.contains(LoadFlag.DB_MINIMAL) ||
                loadFlags.contains(LoadFlag.ATTRIBUTES) ||
                loadFlags.contains(LoadFlag.WAYPOINTS) ||
                loadFlags.contains(LoadFlag.SPOILERS) ||
                loadFlags.contains(LoadFlag.LOGS) ||
                loadFlags.contains(LoadFlag.INVENTORY) ||
                loadFlags.contains(LoadFlag.OFFLINE_LOG)) {

            final Set<Geocache> cachesFromDB = loadCachesFromGeocodes(remaining, loadFlags);
            result.addAll(cachesFromDB);
            for (final Geocache cache : cachesFromDB) {
                remaining.remove(cache.getGeocode());
            }
        }

        if (loadFlags.contains(LoadFlag.CACHE_AFTER)) {
            for (final String geocode : new HashSet<>(remaining)) {
                final Geocache cache = cacheCache.getCacheFromCache(geocode);
                if (cache != null) {
                    result.add(cache);
                    remaining.remove(cache.getGeocode());
                }
            }
        }

        if (remaining.size() >= 1) {
            Log.d("DataStore.loadCaches(" + remaining.toString() + ") returned no results");
        }
        return result;
    }

    /**
     * Load caches.
     *
     * @param geocodes
     * @param loadFlags
     * @return Set of loaded caches. Never null.
     */
    private static Set<Geocache> loadCachesFromGeocodes(final Set<String> geocodes, final EnumSet<LoadFlag> loadFlags) {
        if (CollectionUtils.isEmpty(geocodes)) {
            return Collections.emptySet();
        }

        // do not log the entire collection of geo codes to the debug log. This can be more than 100 KB of text for large lists!
        init();

        final StringBuilder query = new StringBuilder(QUERY_CACHE_DATA);
        if (loadFlags.contains(LoadFlag.OFFLINE_LOG)) {
            query.append(',').append(dbTableLogsOffline).append(".log");
        }

        query.append(" FROM ").append(dbTableCaches);
        if (loadFlags.contains(LoadFlag.OFFLINE_LOG)) {
            query.append(" LEFT OUTER JOIN ").append(dbTableLogsOffline).append(" ON ( ").append(dbTableCaches).append(".geocode == ").append(dbTableLogsOffline).append(".geocode) ");
        }

        query.append(" WHERE ").append(dbTableCaches).append('.');
        query.append(DataStore.whereGeocodeIn(geocodes));

        final Cursor cursor = database.rawQuery(query.toString(), null);
        try {
            final Set<Geocache> caches = new HashSet<>();
            int logIndex = -1;

            while (cursor.moveToNext()) {
                final Geocache cache = createCacheFromDatabaseContent(cursor);

                if (loadFlags.contains(LoadFlag.ATTRIBUTES)) {
                    cache.setAttributes(loadAttributes(cache.getGeocode()));
                }

                if (loadFlags.contains(LoadFlag.WAYPOINTS)) {
                    final List<Waypoint> waypoints = loadWaypoints(cache.getGeocode());
                    if (CollectionUtils.isNotEmpty(waypoints)) {
                        cache.setWaypoints(waypoints, false);
                    }
                }

                if (loadFlags.contains(LoadFlag.SPOILERS)) {
                    final List<Image> spoilers = loadSpoilers(cache.getGeocode());
                    cache.setSpoilers(spoilers);
                }

                if (loadFlags.contains(LoadFlag.LOGS)) {
                    final Map<LogType, Integer> logCounts = loadLogCounts(cache.getGeocode());
                    if (MapUtils.isNotEmpty(logCounts)) {
                        cache.getLogCounts().clear();
                        cache.getLogCounts().putAll(logCounts);
                    }
                }

                if (loadFlags.contains(LoadFlag.INVENTORY)) {
                    final List<Trackable> inventory = loadInventory(cache.getGeocode());
                    if (CollectionUtils.isNotEmpty(inventory)) {
                        if (cache.getInventory() == null) {
                            cache.setInventory(new ArrayList<Trackable>());
                        } else {
                            cache.getInventory().clear();
                        }
                        cache.getInventory().addAll(inventory);
                    }
                }

                if (loadFlags.contains(LoadFlag.OFFLINE_LOG)) {
                    if (logIndex < 0) {
                        logIndex = cursor.getColumnIndex("log");
                    }
                    cache.setLogOffline(!cursor.isNull(logIndex));
                }
                cache.addStorageLocation(StorageLocation.DATABASE);
                cacheCache.putCacheInCache(cache);

                caches.add(cache);
            }
            return caches;
        } finally {
            cursor.close();
        }
    }


    /**
     * Builds a where for a viewport with the size enhanced by 50%.
     *
     * @param dbTable
     * @param viewport
     * @return
     */

    private static StringBuilder buildCoordinateWhere(final String dbTable, final Viewport viewport) {
        return viewport.resize(1.5).sqlWhere(dbTable);
    }

    /**
     * creates a Cache from the cursor. Doesn't next.
     *
     * @param cursor
     * @return Cache from DB
     */
    private static Geocache createCacheFromDatabaseContent(final Cursor cursor) {
        final Geocache cache = new Geocache();

        cache.setUpdated(cursor.getLong(0));
        cache.setListId(cursor.getInt(1));
        cache.setDetailed(cursor.getInt(2) == 1);
        cache.setDetailedUpdate(cursor.getLong(3));
        cache.setVisitedDate(cursor.getLong(4));
        cache.setGeocode(cursor.getString(5));
        cache.setCacheId(cursor.getString(6));
        cache.setGuid(cursor.getString(7));
        cache.setType(CacheType.getById(cursor.getString(8)));
        cache.setName(cursor.getString(9));
        cache.setOwnerDisplayName(cursor.getString(10));
        cache.setOwnerUserId(cursor.getString(11));
        final long dateValue = cursor.getLong(12);
        if (dateValue != 0) {
            cache.setHidden(new Date(dateValue));
        }
        // do not set cache.hint
        cache.setSize(CacheSize.getById(cursor.getString(14)));
        cache.setDifficulty(cursor.getFloat(15));
        int index = 16;
        if (cursor.isNull(index)) {
            cache.setDirection(null);
        } else {
            cache.setDirection(cursor.getFloat(index));
        }
        index = 17;
        if (cursor.isNull(index)) {
            cache.setDistance(null);
        } else {
            cache.setDistance(cursor.getFloat(index));
        }
        cache.setTerrain(cursor.getFloat(18));
        // do not set cache.location
        cache.setPersonalNote(cursor.getString(20));
        // do not set cache.shortdesc
        // do not set cache.description
        cache.setFavoritePoints(cursor.getInt(22));
        cache.setRating(cursor.getFloat(23));
        cache.setVotes(cursor.getInt(24));
        cache.setMyVote(cursor.getFloat(25));
        cache.setDisabled(cursor.getInt(26) == 1);
        cache.setArchived(cursor.getInt(27) == 1);
        cache.setPremiumMembersOnly(cursor.getInt(28) == 1);
        cache.setFound(cursor.getInt(29) == 1);
        cache.setFavorite(cursor.getInt(30) == 1);
        cache.setInventoryItems(cursor.getInt(31));
        cache.setOnWatchlist(cursor.getInt(32) == 1);
        cache.setReliableLatLon(cursor.getInt(33) > 0);
        cache.setUserModifiedCoords(cursor.getInt(34) > 0);
        cache.setCoords(getCoords(cursor, 35, 36));
        cache.setFinalDefined(cursor.getInt(37) > 0);
        cache.setLogPasswordRequired(cursor.getInt(41) > 0);

        Log.d("Loading " + cache.toString() + " (" + cache.getListId() + ") from DB");

        return cache;
    }

    public static List<String> loadAttributes(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        return queryToColl(dbTableAttributes,
                new String[]{"attribute"},
                "geocode = ?",
                new String[]{geocode},
                null,
                null,
                null,
                "100",
                new LinkedList<String>(),
                GET_STRING_0);
    }

    public static Waypoint loadWaypoint(final int id) {
        if (id == 0) {
            return null;
        }

        init();

        final Cursor cursor = database.query(
                dbTableWaypoints,
                WAYPOINT_COLUMNS,
                "_id = ?",
                new String[]{Integer.toString(id)},
                null,
                null,
                null,
                "1");

        Log.d("DataStore.loadWaypoint(" + id + ")");

        final Waypoint waypoint = cursor.moveToFirst() ? createWaypointFromDatabaseContent(cursor) : null;

        cursor.close();

        return waypoint;
    }

    public static List<Waypoint> loadWaypoints(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        return queryToColl(dbTableWaypoints,
                WAYPOINT_COLUMNS,
                "geocode = ?",
                new String[]{geocode},
                null,
                null,
                "_id",
                "100",
                new LinkedList<Waypoint>(),
                new Func1<Cursor, Waypoint>() {
                    @Override
                    public Waypoint call(final Cursor cursor) {
                        return createWaypointFromDatabaseContent(cursor);
                    }
                });
    }

    private static Waypoint createWaypointFromDatabaseContent(final Cursor cursor) {
        final String name = cursor.getString(cursor.getColumnIndex("name"));
        final WaypointType type = WaypointType.findById(cursor.getString(cursor.getColumnIndex("type")));
        final boolean own = cursor.getInt(cursor.getColumnIndex("own")) != 0;
        final Waypoint waypoint = new Waypoint(name, type, own);
        waypoint.setVisited(cursor.getInt(cursor.getColumnIndex("visited")) != 0);
        waypoint.setId(cursor.getInt(cursor.getColumnIndex("_id")));
        waypoint.setGeocode(cursor.getString(cursor.getColumnIndex("geocode")));
        waypoint.setPrefix(cursor.getString(cursor.getColumnIndex("prefix")));
        waypoint.setLookup(cursor.getString(cursor.getColumnIndex("lookup")));
        waypoint.setCoords(getCoords(cursor, cursor.getColumnIndex("latitude"), cursor.getColumnIndex("longitude")));
        waypoint.setNote(cursor.getString(cursor.getColumnIndex("note")));

        return waypoint;
    }

    private static List<Image> loadSpoilers(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        return queryToColl(dbTableSpoilers,
                new String[]{"url", "title", "description"},
                "geocode = ?",
                new String[]{geocode},
                null,
                null,
                null,
                "100",
                new LinkedList<Image>(),
                new Func1<Cursor, Image>() {
                    @Override
                    public Image call(final Cursor cursor) {
                        return new Image(cursor.getString(0), cursor.getString(1), cursor.getString(2));
                    }
                });
    }

    /**
     * Loads the history of previously entered destinations from
     * the database. If no destinations exist, an {@link Collections#emptyList()} will be returned.
     *
     * @return A list of previously entered destinations or an empty list.
     */
    public static List<Destination> loadHistoryOfSearchedLocations() {
        return queryToColl(dbTableSearchDestinationHistory,
                new String[]{"_id", "date", "latitude", "longitude"},
                "latitude IS NOT NULL AND longitude IS NOT NULL",
                null,
                null,
                null,
                "date desc",
                "100",
                new LinkedList<Destination>(),
                new Func1<Cursor, Destination>() {
                    @Override
                    public Destination call(final Cursor cursor) {
                        return new Destination(cursor.getLong(0), cursor.getLong(1), getCoords(cursor, 2, 3));
                    }
                });
    }

    public static boolean clearSearchedDestinations() {
        init();
        database.beginTransaction();

        try {
            database.delete(dbTableSearchDestinationHistory, null, null);
            database.setTransactionSuccessful();
            return true;
        } catch (final Exception e) {
            Log.e("Unable to clear searched destinations", e);
        } finally {
            database.endTransaction();
        }

        return false;
    }

    /**
     * @param geocode
     * @return an immutable, non null list of logs
     */
    @NonNull
    public static List<LogEntry> loadLogs(final String geocode) {
        final List<LogEntry> logs = new ArrayList<>();

        if (StringUtils.isBlank(geocode)) {
            return logs;
        }

        init();

        final Cursor cursor = database.rawQuery(
                /*                           0       1      2      3    4      5      6                                                7       8      9     10 */
                "SELECT cg_logs._id as cg_logs_id, type, author, log, date, found, friend, " + dbTableLogImages + "._id as cg_logImages_id, log_id, title, url"
                        + " FROM " + dbTableLogs + " LEFT OUTER JOIN " + dbTableLogImages
                        + " ON ( cg_logs._id = log_id ) WHERE geocode = ?  ORDER BY date desc, cg_logs._id asc", new String[]{geocode});

        LogEntry log = null;
        while (cursor.moveToNext() && logs.size() < 100) {
            if (log == null || log.id != cursor.getInt(0)) {
                log = new LogEntry(
                        cursor.getString(2),
                        cursor.getLong(4),
                        LogType.getById(cursor.getInt(1)),
                        cursor.getString(3));
                log.id = cursor.getInt(0);
                log.found = cursor.getInt(5);
                log.friend = cursor.getInt(6) == 1;
                logs.add(log);
            }
            if (!cursor.isNull(7)) {
                log.addLogImage(new Image(cursor.getString(10), cursor.getString(9)));
            }
        }

        cursor.close();

        return Collections.unmodifiableList(logs);
    }

    public static Map<LogType, Integer> loadLogCounts(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        init();

        final Map<LogType, Integer> logCounts = new EnumMap<>(LogType.class);

        final Cursor cursor = database.query(
                dbTableLogCount,
                new String[]{"type", "count"},
                "geocode = ?",
                new String[]{geocode},
                null,
                null,
                null,
                "100");

        while (cursor.moveToNext()) {
            logCounts.put(LogType.getById(cursor.getInt(0)), cursor.getInt(1));
        }

        cursor.close();

        return logCounts;
    }

    private static List<Trackable> loadInventory(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        init();

        final List<Trackable> trackables = new ArrayList<>();

        final Cursor cursor = database.query(
                dbTableTrackables,
                new String[]{"_id", "updated", "tbcode", "guid", "title", "owner", "released", "goal", "description"},
                "geocode = ?",
                new String[]{geocode},
                null,
                null,
                "title COLLATE NOCASE ASC",
                "100");

        while (cursor.moveToNext()) {
            trackables.add(createTrackableFromDatabaseContent(cursor));
        }

        cursor.close();

        return trackables;
    }

    public static Trackable loadTrackable(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        init();

        final Cursor cursor = database.query(
                dbTableTrackables,
                new String[]{"updated", "tbcode", "guid", "title", "owner", "released", "goal", "description"},
                "tbcode = ?",
                new String[]{geocode},
                null,
                null,
                null,
                "1");

        final Trackable trackable = cursor.moveToFirst() ? createTrackableFromDatabaseContent(cursor) : null;

        cursor.close();

        return trackable;
    }

    private static Trackable createTrackableFromDatabaseContent(final Cursor cursor) {
        final Trackable trackable = new Trackable();
        trackable.setGeocode(cursor.getString(cursor.getColumnIndex("tbcode")));
        trackable.setGuid(cursor.getString(cursor.getColumnIndex("guid")));
        trackable.setName(cursor.getString(cursor.getColumnIndex("title")));
        trackable.setOwner(cursor.getString(cursor.getColumnIndex("owner")));
        final String released = cursor.getString(cursor.getColumnIndex("released"));
        if (released != null) {
            try {
                final long releaseMilliSeconds = Long.parseLong(released);
                trackable.setReleased(new Date(releaseMilliSeconds));
            } catch (final NumberFormatException e) {
                Log.e("createTrackableFromDatabaseContent", e);
            }
        }
        trackable.setGoal(cursor.getString(cursor.getColumnIndex("goal")));
        trackable.setDetails(cursor.getString(cursor.getColumnIndex("description")));
        trackable.setLogs(loadLogs(trackable.getGeocode()));
        return trackable;
    }

    /**
     * Number of caches stored for a given type and/or list
     *
     * @param cacheType
     * @param list
     * @return
     */
    public static int getAllStoredCachesCount(final CacheType cacheType, final int list) {
        if (cacheType == null) {
            throw new IllegalArgumentException("cacheType must not be null");
        }
        if (list <= 0) {
            throw new IllegalArgumentException("list must be > 0");
        }
        init();

        try {
            final SQLiteStatement compiledStmnt;
            if (list == PseudoList.ALL_LIST.id) {
                if (cacheType == CacheType.ALL) {
                    compiledStmnt = PreparedStatement.COUNT_ALL_TYPES_ALL_LIST.getStatement();
                }
                else {
                    compiledStmnt = PreparedStatement.COUNT_TYPE_ALL_LIST.getStatement();
                    compiledStmnt.bindString(1, cacheType.id);
                }
            } else {
                if (cacheType == CacheType.ALL) {
                    compiledStmnt = PreparedStatement.COUNT_ALL_TYPES_LIST.getStatement();
                    compiledStmnt.bindLong(1, list);
                }
                else {
                    compiledStmnt = PreparedStatement.COUNT_TYPE_LIST.getStatement();
                    compiledStmnt.bindString(1, cacheType.id);
                    compiledStmnt.bindLong(1, list);
                }
            }

            return (int) compiledStmnt.simpleQueryForLong();
        } catch (final Exception e) {
            Log.e("DataStore.loadAllStoredCachesCount", e);
        }

        return 0;
    }

    public static int getAllHistoryCachesCount() {
        init();

        try {
            return (int) PreparedStatement.HISTORY_COUNT.simpleQueryForLong();
        } catch (final Exception e) {
            Log.e("DataStore.getAllHistoricCachesCount", e);
        }

        return 0;
    }

    private static<T, U extends Collection<? super T>> U queryToColl(@NonNull final String table,
                                                                     final String[] columns,
                                                                     final String selection,
                                                                     final String[] selectionArgs,
                                                                     final String groupBy,
                                                                     final String having,
                                                                     final String orderBy,
                                                                     final String limit,
                                                                     final U result,
                                                                     final Func1<? super Cursor, ? extends T> func) {
        init();
        final Cursor cursor = database.query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
        return cursorToColl(cursor, result, func);
    }

    private static <T, U extends Collection<? super T>> U cursorToColl(final Cursor cursor, final U result, final Func1<? super Cursor, ? extends T> func) {
        try {
            while (cursor.moveToNext()) {
                result.add(func.call(cursor));
            }
            return result;
        } finally {
            cursor.close();
        }
    }

    /**
     * Return a batch of stored geocodes.
     *
     * @param coords
     *            the current coordinates to sort by distance, or null to sort by geocode
     * @param cacheType
     * @param listId
     * @return a non-null set of geocodes
     */
    private static Set<String> loadBatchOfStoredGeocodes(final Geopoint coords, final CacheType cacheType, final int listId) {
        if (cacheType == null) {
            throw new IllegalArgumentException("cacheType must not be null");
        }
        final StringBuilder selection = new StringBuilder();

        selection.append("reason ");
        selection.append(listId != PseudoList.ALL_LIST.id ? "=" + Math.max(listId, 1) : ">= " + StoredList.STANDARD_LIST_ID);
        selection.append(" and detailed = 1 ");

        String[] selectionArgs = null;
        if (cacheType != CacheType.ALL) {
            selection.append(" and type = ?");
            selectionArgs = new String[] { String.valueOf(cacheType.id) };
        }

        try {
            if (coords != null) {
                return queryToColl(dbTableCaches,
                        new String[]{"geocode", "(abs(latitude-" + String.format((Locale) null, "%.6f", coords.getLatitude()) +
                                ") + abs(longitude-" + String.format((Locale) null, "%.6f", coords.getLongitude()) + ")) as dif"},
                        selection.toString(),
                        selectionArgs,
                        null,
                        null,
                        "dif",
                        null,
                        new HashSet<String>(),
                        GET_STRING_0);
            }
            return queryToColl(dbTableCaches,
                    new String[] { "geocode" },
                    selection.toString(),
                    selectionArgs,
                    null,
                    null,
                    "geocode",
                    null,
                    new HashSet<String>(),
                    GET_STRING_0);
        } catch (final Exception e) {
            Log.e("DataStore.loadBatchOfStoredGeocodes", e);
            return Collections.emptySet();
        }
    }

    private static Set<String> loadBatchOfHistoricGeocodes(final boolean detailedOnly, final CacheType cacheType) {
        final StringBuilder selection = new StringBuilder("visiteddate > 0");

        if (detailedOnly) {
            selection.append(" and detailed = 1");
        }
        String[] selectionArgs = null;
        if (cacheType != CacheType.ALL) {
            selection.append(" and type = ?");
            selectionArgs = new String[] { String.valueOf(cacheType.id) };
        }

        try {
            return queryToColl(dbTableCaches,
                    new String[]{"geocode"},
                    selection.toString(),
                    selectionArgs,
                    null,
                    null,
                    "visiteddate",
                    null,
                    new HashSet<String>(),
                    GET_STRING_0);
        } catch (final Exception e) {
            Log.e("DataStore.loadBatchOfHistoricGeocodes", e);
        }

        return Collections.emptySet();
    }

    /** Retrieve all stored caches from DB */
    public static SearchResult loadCachedInViewport(final Viewport viewport, final CacheType cacheType) {
        return loadInViewport(false, viewport, cacheType);
    }

    /** Retrieve stored caches from DB with listId >= 1 */
    public static SearchResult loadStoredInViewport(final Viewport viewport, final CacheType cacheType) {
        return loadInViewport(true, viewport, cacheType);
    }

    /**
     * Loads the geocodes of caches in a viewport from CacheCache and/or Database
     *
     * @param stored {@code true} to query caches stored in the database, {@code false} to also use the CacheCache
     * @param viewport the viewport defining the area to scan
     * @param cacheType the cache type
     * @return the matching caches
     */
    private static SearchResult loadInViewport(final boolean stored, final Viewport viewport, final CacheType cacheType) {
        final Set<String> geocodes = new HashSet<>();

        // if not stored only, get codes from CacheCache as well
        if (!stored) {
            geocodes.addAll(cacheCache.getInViewport(viewport, cacheType));
        }

        // viewport limitation
        final StringBuilder selection = buildCoordinateWhere(dbTableCaches, viewport);

        // cacheType limitation
        String[] selectionArgs = null;
        if (cacheType != CacheType.ALL) {
            selection.append(" and type = ?");
            selectionArgs = new String[] { String.valueOf(cacheType.id) };
        }

        // offline caches only
        if (stored) {
            selection.append(" and reason >= " + StoredList.STANDARD_LIST_ID);
        }

        try {
            return new SearchResult(queryToColl(dbTableCaches,
                    new String[]{"geocode"},
                    selection.toString(),
                    selectionArgs,
                    null,
                    null,
                    null,
                    "500",
                    geocodes,
                    GET_STRING_0));
        } catch (final Exception e) {
            Log.e("DataStore.loadInViewport", e);
        }

        return new SearchResult();
    }

    /**
     * Remove caches with listId = 0 in the background. Once it has been executed once it will not do anything.
     * This must be called from the UI thread to ensure synchronization of an internal variable.
     */
    public static void cleanIfNeeded(final Context context) {
        if (databaseCleaned) {
            return;
        }
        databaseCleaned = true;

        Schedulers.io().createWorker().schedule(new Action0() {
            @Override
            public void call() {
                Log.d("Database clean: started");
                try {
                    final int version = Version.getVersionCode(context);
                    final Set<String> geocodes = new HashSet<>();
                    if (version != Settings.getVersion()) {
                        queryToColl(dbTableCaches,
                                new String[]{"geocode"},
                                "reason = 0",
                                null,
                                null,
                                null,
                                null,
                                null,
                                geocodes,
                                GET_STRING_0);
                    } else {
                        final long timestamp = System.currentTimeMillis() - DAYS_AFTER_CACHE_IS_DELETED;
                        final String timestampString = Long.toString(timestamp);
                        queryToColl(dbTableCaches,
                                new String[]{"geocode"},
                                "reason = 0 and detailed < ? and detailedupdate < ? and visiteddate < ?",
                                new String[]{timestampString, timestampString, timestampString},
                                null,
                                null,
                                null,
                                null,
                                geocodes,
                                GET_STRING_0);
                    }

                    final Set<String> withoutOfflineLogs = exceptCachesWithOfflineLog(geocodes);
                    Log.d("Database clean: removing " + withoutOfflineLogs.size() + " geocaches from listId=0");
                    removeCaches(withoutOfflineLogs, LoadFlags.REMOVE_ALL);

                    // This cleanup needs to be kept in place for about one year so that older log images records are
                    // cleaned. TO BE REMOVED AFTER 2015-03-24.
                    Log.d("Database clean: removing obsolete log images records");
                    database.delete(dbTableLogImages, "log_id NOT IN (SELECT _id FROM " + dbTableLogs + ")", null);

                    if (version > -1) {
                        Settings.setVersion(version);
                    }
                } catch (final Exception e) {
                    Log.w("DataStore.clean", e);
                }

                Log.d("Database clean: finished");
            }
        });
    }

    /**
     * remove all geocodes from the given list of geocodes where an offline log exists
     *
     * @param geocodes
     * @return
     */
    private static Set<String> exceptCachesWithOfflineLog(final Set<String> geocodes) {
        if (geocodes.isEmpty()) {
            return geocodes;
        }

        final List<String> geocodesWithOfflineLog = queryToColl(dbTableLogsOffline,
                new String[] { "geocode" },
                null,
                null,
                null,
                null,
                null,
                null,
                new LinkedList<String>(),
                GET_STRING_0);
        geocodes.removeAll(geocodesWithOfflineLog);
        return geocodes;
    }

    public static void removeAllFromCache() {
        // clean up CacheCache
        cacheCache.removeAllFromCache();
    }

    public static void removeCache(final String geocode, final EnumSet<LoadFlags.RemoveFlag> removeFlags) {
        removeCaches(Collections.singleton(geocode), removeFlags);
    }

    /**
     * Drop caches from the tables they are stored into, as well as the cache files
     *
     * @param geocodes
     *            list of geocodes to drop from cache
     */
    public static void removeCaches(final Set<String> geocodes, final EnumSet<LoadFlags.RemoveFlag> removeFlags) {
        if (CollectionUtils.isEmpty(geocodes)) {
            return;
        }

        init();

        if (removeFlags.contains(RemoveFlag.CACHE)) {
            for (final String geocode : geocodes) {
                cacheCache.removeCacheFromCache(geocode);
            }
        }

        if (removeFlags.contains(RemoveFlag.DB)) {
            // Drop caches from the database
            final ArrayList<String> quotedGeocodes = new ArrayList<>(geocodes.size());
            for (final String geocode : geocodes) {
                quotedGeocodes.add(DatabaseUtils.sqlEscapeString(geocode));
            }
            final String geocodeList = StringUtils.join(quotedGeocodes.toArray(), ',');
            final String baseWhereClause = "geocode in (" + geocodeList + ")";
            database.beginTransaction();
            try {
                database.delete(dbTableCaches, baseWhereClause, null);
                database.delete(dbTableAttributes, baseWhereClause, null);
                database.delete(dbTableSpoilers, baseWhereClause, null);
                database.delete(dbTableLogImages, "log_id IN (SELECT _id FROM " + dbTableLogs + " WHERE " + baseWhereClause + ")", null);
                database.delete(dbTableLogs, baseWhereClause, null);
                database.delete(dbTableLogCount, baseWhereClause, null);
                database.delete(dbTableLogsOffline, baseWhereClause, null);
                String wayPointClause = baseWhereClause;
                if (!removeFlags.contains(RemoveFlag.OWN_WAYPOINTS_ONLY_FOR_TESTING)) {
                    wayPointClause += " and type <> 'own'";
                }
                database.delete(dbTableWaypoints, wayPointClause, null);
                database.delete(dbTableTrackables, baseWhereClause, null);
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }

            // Delete cache directories
            for (final String geocode : geocodes) {
                LocalStorage.deleteDirectory(LocalStorage.getStorageDir(geocode));
            }
        }
    }

    public static boolean saveLogOffline(final String geocode, final Date date, final LogType type, final String log) {
        if (StringUtils.isBlank(geocode)) {
            Log.e("DataStore.saveLogOffline: cannot log a blank geocode");
            return false;
        }
        if (LogType.UNKNOWN == type && StringUtils.isBlank(log)) {
            Log.e("DataStore.saveLogOffline: cannot log an unknown log type and no message");
            return false;
        }

        init();

        final ContentValues values = new ContentValues();
        values.put("geocode", geocode);
        values.put("updated", System.currentTimeMillis());
        values.put("type", type.id);
        values.put("log", log);
        values.put("date", date.getTime());

        if (hasLogOffline(geocode)) {
            final int rows = database.update(dbTableLogsOffline, values, "geocode = ?", new String[] { geocode });
            return rows > 0;
        }
        final long id = database.insert(dbTableLogsOffline, null, values);
        return id != -1;
    }

    public static LogEntry loadLogOffline(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        init();


        final Cursor cursor = database.query(
                dbTableLogsOffline,
                new String[]{"_id", "type", "log", "date"},
                "geocode = ?",
                new String[]{geocode},
                null,
                null,
                "_id desc",
                "1");

        LogEntry log = null;
        if (cursor.moveToFirst()) {
            log = new LogEntry(cursor.getLong(3),
                    LogType.getById(cursor.getInt(1)),
                    cursor.getString(2));
            log.id = cursor.getInt(0);
        }

        cursor.close();

        return log;
    }

    public static void clearLogOffline(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return;
        }

        init();

        database.delete(dbTableLogsOffline, "geocode = ?", new String[]{geocode});
    }

    public static void clearLogsOffline(final List<Geocache> caches) {
        if (CollectionUtils.isEmpty(caches)) {
            return;
        }

        init();

        for (final Geocache cache : caches) {
            cache.setLogOffline(false);
        }

        database.execSQL(String.format("DELETE FROM %s where %s", dbTableLogsOffline, whereGeocodeIn(Geocache.getGeocodes(caches))));
    }

    public static boolean hasLogOffline(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return false;
        }

        init();
        try {
            final SQLiteStatement logCount = PreparedStatement.LOG_COUNT_OF_GEOCODE.getStatement();
            synchronized (logCount) {
                logCount.bindString(1, geocode);
                return logCount.simpleQueryForLong() > 0;
            }
        } catch (final Exception e) {
            Log.e("DataStore.hasLogOffline", e);
        }

        return false;
    }

    private static void setVisitDate(final List<String> geocodes, final long visitedDate) {
        if (geocodes.isEmpty()) {
            return;
        }

        init();

        database.beginTransaction();
        try {
            final SQLiteStatement setVisit = PreparedStatement.UPDATE_VISIT_DATE.getStatement();

            for (final String geocode : geocodes) {
                setVisit.bindLong(1, visitedDate);
                setVisit.bindString(2, geocode);
                setVisit.execute();
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    @NonNull
    public static List<StoredList> getLists() {
        init();

        final Resources res = CgeoApplication.getInstance().getResources();
        final List<StoredList> lists = new ArrayList<>();
        lists.add(new StoredList(StoredList.STANDARD_LIST_ID, res.getString(R.string.list_inbox), (int) PreparedStatement.COUNT_CACHES_ON_STANDARD_LIST.simpleQueryForLong()));

        try {
            final String query = "SELECT l._id as _id, l.title as title, COUNT(c._id) as count" +
                    " FROM " + dbTableLists + " l LEFT OUTER JOIN " + dbTableCaches + " c" +
                    " ON l._id + " + customListIdOffset + " = c.reason" +
                    " GROUP BY l._id" +
                    " ORDER BY l.title COLLATE NOCASE ASC";

            lists.addAll(getListsFromCursor(database.rawQuery(query, null)));
        } catch (final Exception e) {
            Log.e("DataStore.readLists", e);
        }
        return lists;
    }

    private static ArrayList<StoredList> getListsFromCursor(final Cursor cursor) {
        final int indexId = cursor.getColumnIndex("_id");
        final int indexTitle = cursor.getColumnIndex("title");
        final int indexCount = cursor.getColumnIndex("count");
        return cursorToColl(cursor, new ArrayList<StoredList>(), new Func1<Cursor, StoredList>() {
            @Override
            public StoredList call(final Cursor cursor) {
                final int count = indexCount != -1 ? cursor.getInt(indexCount) : 0;
                return new StoredList(cursor.getInt(indexId) + customListIdOffset, cursor.getString(indexTitle), count);
            }
        });
    }

    public static StoredList getList(final int id) {
        init();
        if (id >= customListIdOffset) {
            final Cursor cursor = database.query(
                    dbTableLists,
                    new String[]{"_id", "title"},
                    "_id = ? ",
                    new String[] { String.valueOf(id - customListIdOffset) },
                    null,
                    null,
                    null);
            final ArrayList<StoredList> lists = getListsFromCursor(cursor);
            if (!lists.isEmpty()) {
                return lists.get(0);
            }
        }

        final Resources res = CgeoApplication.getInstance().getResources();
        if (id == PseudoList.ALL_LIST.id) {
            return new StoredList(PseudoList.ALL_LIST.id, res.getString(R.string.list_all_lists), getAllCachesCount());
        }

        // fall back to standard list in case of invalid list id
        if (id == StoredList.STANDARD_LIST_ID || id >= customListIdOffset) {
            return new StoredList(StoredList.STANDARD_LIST_ID, res.getString(R.string.list_inbox), (int) PreparedStatement.COUNT_CACHES_ON_STANDARD_LIST.simpleQueryForLong());
        }

        return null;
    }

    public static int getAllCachesCount() {
        return (int) PreparedStatement.COUNT_ALL_CACHES.simpleQueryForLong();
    }

    /**
     * Count all caches in the background.
     *
     * @return an observable containing a unique element if the caches could be counted, or an error otherwise
     */
    public static Observable<Integer> getAllCachesCountObservable() {
        return allCachesCountObservable;
    }

    /**
     * Create a new list
     *
     * @param name
     *            Name
     * @return new listId
     */
    public static int createList(final String name) {
        int id = -1;
        if (StringUtils.isBlank(name)) {
            return id;
        }

        init();

        database.beginTransaction();
        try {
            final ContentValues values = new ContentValues();
            values.put("title", name);
            values.put("updated", System.currentTimeMillis());

            id = (int) database.insert(dbTableLists, null, values);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

        return id >= 0 ? id + customListIdOffset : -1;
    }

    /**
     * @param listId
     *            List to change
     * @param name
     *            New name of list
     * @return Number of lists changed
     */
    public static int renameList(final int listId, final String name) {
        if (StringUtils.isBlank(name) || StoredList.STANDARD_LIST_ID == listId) {
            return 0;
        }

        init();

        database.beginTransaction();
        int count = 0;
        try {
            final ContentValues values = new ContentValues();
            values.put("title", name);
            values.put("updated", System.currentTimeMillis());

            count = database.update(dbTableLists, values, "_id = " + (listId - customListIdOffset), null);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

        return count;
    }

    /**
     * Remove a list. Caches in the list are moved to the standard list.
     *
     * @param listId
     * @return true if the list got deleted, false else
     */
    public static boolean removeList(final int listId) {
        if (listId < customListIdOffset) {
            return false;
        }

        init();

        database.beginTransaction();
        boolean status = false;
        try {
            final int cnt = database.delete(dbTableLists, "_id = " + (listId - customListIdOffset), null);

            if (cnt > 0) {
                // move caches from deleted list to standard list
                final SQLiteStatement moveToStandard = PreparedStatement.MOVE_TO_STANDARD_LIST.getStatement();
                moveToStandard.bindLong(1, listId);
                moveToStandard.execute();

                status = true;
            }

            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

        return status;
    }

    public static void moveToList(final Geocache cache, final int listId) {
        moveToList(Collections.singletonList(cache), listId);
    }

    public static void moveToList(final List<Geocache> caches, final int listId) {
        final AbstractList list = AbstractList.getListById(listId);
        if (list == null) {
            return;
        }
        if (!list.isConcrete()) {
            return;
        }
        if (caches.isEmpty()) {
            return;
        }
        init();

        final SQLiteStatement move = PreparedStatement.MOVE_TO_LIST.getStatement();

        database.beginTransaction();
        try {
            for (final Geocache cache : caches) {
                move.bindLong(1, listId);
                move.bindString(2, cache.getGeocode());
                move.execute();
                cache.setListId(listId);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public static boolean isInitialized() {
        return database != null;
    }

    public static boolean removeSearchedDestination(final Destination destination) {
        if (destination == null) {
            return false;
        }
        init();

        database.beginTransaction();
        try {
            database.delete(dbTableSearchDestinationHistory, "_id = " + destination.getId(), null);
            database.setTransactionSuccessful();
            return true;
        } catch (final Exception e) {
            Log.e("Unable to remove searched destination", e);
        } finally {
            database.endTransaction();
        }

        return false;
    }

    /**
     * Load the lazily initialized fields of a cache and return them as partial cache (all other fields unset).
     *
     * @param geocode
     * @return
     */
    public static Geocache loadCacheTexts(final String geocode) {
        final Geocache partial = new Geocache();

        // in case of database issues, we still need to return a result to avoid endless loops
        partial.setDescription(StringUtils.EMPTY);
        partial.setShortDescription(StringUtils.EMPTY);
        partial.setHint(StringUtils.EMPTY);
        partial.setLocation(StringUtils.EMPTY);

        init();

        try {
            final Cursor cursor = database.query(
                    dbTableCaches,
                    new String[] { "description", "shortdesc", "hint", "location" },
                    "geocode = ?",
                    new String[] { geocode },
                    null,
                    null,
                    null,
                    "1");

            if (cursor.moveToFirst()) {
                partial.setDescription(StringUtils.defaultString(cursor.getString(0)));
                partial.setShortDescription(StringUtils.defaultString(cursor.getString(1)));
                partial.setHint(StringUtils.defaultString(cursor.getString(2)));
                partial.setLocation(StringUtils.defaultString(cursor.getString(3)));
            }

            cursor.close();
        } catch (final SQLiteDoneException ignored) {
            // Do nothing, it only means we have no information on the cache
        } catch (final Exception e) {
            Log.e("DataStore.getCacheDescription", e);
        }

        return partial;
    }

    /**
     * checks if this is a newly created database
     */
    public static boolean isNewlyCreatedDatebase() {
        return newlyCreatedDatabase;
    }

    /**
     * resets flag for newly created database to avoid asking the user multiple times
     */
    public static void resetNewlyCreatedDatabase() {
        newlyCreatedDatabase = false;
    }

    /**
     * Creates the WHERE clause for matching multiple geocodes. This automatically converts all given codes to
     * UPPERCASE.
     */
    private static StringBuilder whereGeocodeIn(final Collection<String> geocodes) {
        final StringBuilder whereExpr = new StringBuilder("geocode in (");
        final Iterator<String> iterator = geocodes.iterator();
        while (true) {
            DatabaseUtils.appendEscapedSQLString(whereExpr, StringUtils.upperCase(iterator.next()));
            if (!iterator.hasNext()) {
                break;
            }
            whereExpr.append(',');
        }
        return whereExpr.append(')');
    }

    /**
     * Loads all Waypoints in the coordinate rectangle.
     *
     * @param excludeDisabled
     * @param excludeMine
     * @param type
     * @return
     */

    public static Set<Waypoint> loadWaypoints(final Viewport viewport, final boolean excludeMine, final boolean excludeDisabled, final CacheType type) {
        final StringBuilder where = buildCoordinateWhere(dbTableWaypoints, viewport);
        if (excludeMine) {
            where.append(" and ").append(dbTableCaches).append(".found == 0");
        }
        if (excludeDisabled) {
            where.append(" and ").append(dbTableCaches).append(".disabled == 0");
            where.append(" and ").append(dbTableCaches).append(".archived == 0");
        }
        if (type != CacheType.ALL) {
            where.append(" and ").append(dbTableCaches).append(".type == '").append(type.id).append('\'');
        }

        final StringBuilder query = new StringBuilder("SELECT ");
        for (int i = 0; i < WAYPOINT_COLUMNS.length; i++) {
            query.append(i > 0 ? ", " : "").append(dbTableWaypoints).append('.').append(WAYPOINT_COLUMNS[i]).append(' ');
        }
        query.append(" FROM ").append(dbTableWaypoints).append(", ").append(dbTableCaches).append(" WHERE ").append(dbTableWaypoints)
                .append(".geocode == ").append(dbTableCaches).append(".geocode and ").append(where)
                .append(" LIMIT " + (Settings.SHOW_WP_THRESHOLD_MAX * 2));  // Hardcoded limit to avoid memory overflow

        return cursorToColl(database.rawQuery(query.toString(), null), new HashSet<Waypoint>(), new Func1<Cursor, Waypoint>() {
            @Override
            public Waypoint call(final Cursor cursor) {
                return createWaypointFromDatabaseContent(cursor);
            }
        });
    }

    public static void saveChangedCache(final Geocache cache) {
        DataStore.saveCache(cache, cache.getStorageLocation().contains(StorageLocation.DATABASE) ? LoadFlags.SAVE_ALL : EnumSet.of(SaveFlag.CACHE));
    }

    private static class PreparedStatement {

        private final static PreparedStatement HISTORY_COUNT = new PreparedStatement("select count(_id) from " + dbTableCaches + " where visiteddate > 0");
        private final static PreparedStatement MOVE_TO_STANDARD_LIST = new PreparedStatement("UPDATE " + dbTableCaches + " SET reason = " + StoredList.STANDARD_LIST_ID + " WHERE reason = ?");
        private final static PreparedStatement MOVE_TO_LIST = new PreparedStatement("UPDATE " + dbTableCaches + " SET reason = ? WHERE geocode = ?");
        private final static PreparedStatement UPDATE_VISIT_DATE = new PreparedStatement("UPDATE " + dbTableCaches + " SET visiteddate = ? WHERE geocode = ?");
        private final static PreparedStatement INSERT_LOG_IMAGE = new PreparedStatement("INSERT INTO " + dbTableLogImages + " (log_id, title, url) VALUES (?, ?, ?)");
        private final static PreparedStatement INSERT_LOG_COUNTS = new PreparedStatement("INSERT INTO " + dbTableLogCount + " (geocode, updated, type, count) VALUES (?, ?, ?, ?)");
        private final static PreparedStatement INSERT_SPOILER = new PreparedStatement("INSERT INTO " + dbTableSpoilers + " (geocode, updated, url, title, description) VALUES (?, ?, ?, ?, ?)");
        private final static PreparedStatement LOG_COUNT_OF_GEOCODE = new PreparedStatement("SELECT count(_id) FROM " + DataStore.dbTableLogsOffline + " WHERE geocode = ?");
        private final static PreparedStatement COUNT_CACHES_ON_STANDARD_LIST = new PreparedStatement("SELECT count(_id) FROM " + dbTableCaches + " WHERE reason = " + StoredList.STANDARD_LIST_ID);
        private final static PreparedStatement COUNT_ALL_CACHES = new PreparedStatement("SELECT count(_id) FROM " + dbTableCaches + " WHERE reason >= " + StoredList.STANDARD_LIST_ID);
        private final static PreparedStatement INSERT_LOG = new PreparedStatement("INSERT INTO " + dbTableLogs + " (geocode, updated, type, author, log, date, found, friend) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        private final static PreparedStatement INSERT_ATTRIBUTE = new PreparedStatement("INSERT INTO " + dbTableAttributes + " (geocode, updated, attribute) VALUES (?, ?, ?)");
        private final static PreparedStatement LIST_ID_OF_GEOCODE = new PreparedStatement("SELECT reason FROM " + dbTableCaches + " WHERE geocode = ?");
        private final static PreparedStatement LIST_ID_OF_GUID = new PreparedStatement("SELECT reason FROM " + dbTableCaches + " WHERE guid = ?");
        private final static PreparedStatement GEOCODE_OF_GUID = new PreparedStatement("SELECT geocode FROM " + dbTableCaches + " WHERE guid = ?");
        private final static PreparedStatement INSERT_SEARCH_DESTINATION = new PreparedStatement("INSERT INTO " + dbTableSearchDestinationHistory + " (date, latitude, longitude) VALUES (?, ?, ?)");
        private final static PreparedStatement COUNT_TYPE_ALL_LIST = new PreparedStatement("select count(_id) from " + dbTableCaches + " where detailed = 1 and type = ? and reason > 0");
        private final static PreparedStatement COUNT_ALL_TYPES_ALL_LIST = new PreparedStatement("select count(_id) from " + dbTableCaches + " where detailed = 1 and reason > 0");
        private final static PreparedStatement COUNT_TYPE_LIST = new PreparedStatement("select count(_id) from " + dbTableCaches + " where detailed = 1 and type = ? and reason = ?");
        private final static PreparedStatement COUNT_ALL_TYPES_LIST = new PreparedStatement("select count(_id) from " + dbTableCaches + " where detailed = 1 and reason = ?");

        private static final List<PreparedStatement> statements = new ArrayList<>();

        private SQLiteStatement statement = null; // initialized lazily
        final String query;

        public PreparedStatement(final String query) {
            this.query = query;
        }

        public long simpleQueryForLong() {
            return getStatement().simpleQueryForLong();
        }

        private SQLiteStatement getStatement() {
            if (statement == null) {
                init();
                statement = database.compileStatement(query);
                statements.add(this);
            }
            return statement;
        }

        private static void clearPreparedStatements() {
            for (final PreparedStatement preparedStatement : statements) {
                preparedStatement.statement.close();
                preparedStatement.statement = null;
            }
            statements.clear();
        }

    }

    public static void saveVisitDate(final String geocode) {
        setVisitDate(Collections.singletonList(geocode), System.currentTimeMillis());
    }

    public static void markDropped(final List<Geocache> caches) {
        moveToList(caches, StoredList.TEMPORARY_LIST.id);
    }

    public static Viewport getBounds(final String geocode) {
        if (geocode == null) {
            return null;
        }

        return DataStore.getBounds(Collections.singleton(geocode));
    }

    public static void clearVisitDate(final String[] selected) {
        setVisitDate(Arrays.asList(selected), 0);
    }

    public static SearchResult getBatchOfStoredCaches(final Geopoint coords, final CacheType cacheType, final int listId) {
        final Set<String> geocodes = DataStore.loadBatchOfStoredGeocodes(coords, cacheType, listId);
        return new SearchResult(geocodes, DataStore.getAllStoredCachesCount(cacheType, listId));
    }

    public static SearchResult getHistoryOfCaches(final boolean detailedOnly, final CacheType cacheType) {
        final Set<String> geocodes = DataStore.loadBatchOfHistoricGeocodes(detailedOnly, cacheType);
        return new SearchResult(geocodes, DataStore.getAllHistoryCachesCount());
    }

    public static boolean saveWaypoint(final int id, final String geocode, final Waypoint waypoint) {
        if (DataStore.saveWaypointInternal(id, geocode, waypoint)) {
            DataStore.removeCache(geocode, EnumSet.of(RemoveFlag.CACHE));
            return true;
        }
        return false;
    }

    public static Set<String> getCachedMissingFromSearch(final SearchResult searchResult, final Set<Tile> tiles, final IConnector connector, final int maxZoom) {

        // get cached CacheListActivity
        final Set<String> cachedGeocodes = new HashSet<>();
        for (final Tile tile : tiles) {
            cachedGeocodes.addAll(cacheCache.getInViewport(tile.getViewport(), CacheType.ALL));
        }
        // remove found in search result
        cachedGeocodes.removeAll(searchResult.getGeocodes());

        // check remaining against viewports
        final Set<String> missingFromSearch = new HashSet<>();
        for (final String geocode : cachedGeocodes) {
            if (connector.canHandle(geocode)) {
                final Geocache geocache = cacheCache.getCacheFromCache(geocode);
                // TODO: parallel searches seem to have the potential to make some caches be expunged from the CacheCache (see issue #3716).
                if (geocache != null && geocache.getCoordZoomLevel() <= maxZoom) {
                    for (final Tile tile : tiles) {
                        if (tile.containsPoint(geocache)) {
                            missingFromSearch.add(geocode);
                            break;
                        }
                    }
                }
            }
        }

        return missingFromSearch;
    }

    public static Cursor findSuggestions(final String searchTerm) {
        // require 3 characters, otherwise there are to many results
        if (StringUtils.length(searchTerm) < 3) {
            return null;
        }
        init();
        final SearchSuggestionCursor resultCursor = new SearchSuggestionCursor();
        try {
            final String selectionArg = getSuggestionArgument(searchTerm);
            findCaches(resultCursor, selectionArg);
            findTrackables(resultCursor, selectionArg);
        } catch (final Exception e) {
            Log.e("DataStore.loadBatchOfStoredGeocodes", e);
        }
        return resultCursor;
    }

    private static void findCaches(final SearchSuggestionCursor resultCursor, final String selectionArg) {
        final Cursor cursor = database.query(
                dbTableCaches,
                new String[] { "geocode", "name", "type" },
                "geocode IS NOT NULL AND geocode != '' AND (geocode LIKE ? OR name LIKE ? OR owner LIKE ?)",
                new String[] { selectionArg, selectionArg, selectionArg },
                null,
                null,
                "name");
        while (cursor.moveToNext()) {
            final String geocode = cursor.getString(0);
            final String cacheName = cursor.getString(1);
            final String type = cursor.getString(2);
            resultCursor.addCache(geocode, cacheName, type);
        }
        cursor.close();
    }

    private static String getSuggestionArgument(final String input) {
        return "%" + StringUtils.trim(input) + "%";
    }

    private static void findTrackables(final MatrixCursor resultCursor, final String selectionArg) {
        final Cursor cursor = database.query(
                dbTableTrackables,
                new String[] { "tbcode", "title" },
                "tbcode IS NOT NULL AND tbcode != '' AND (tbcode LIKE ? OR title LIKE ?)",
                new String[] { selectionArg, selectionArg },
                null,
                null,
                "title");
        while (cursor.moveToNext()) {
            final String tbcode = cursor.getString(0);
            resultCursor.addRow(new String[] {
                    String.valueOf(resultCursor.getCount()),
                    cursor.getString(1),
                    tbcode,
                    Intents.ACTION_TRACKABLE,
                    tbcode,
                    String.valueOf(R.drawable.trackable_all)
            });
        }
        cursor.close();
    }

    public static String[] getSuggestions(final String table, final String column, final String input) {
        try {
            final Cursor cursor = database.rawQuery("SELECT DISTINCT " + column
                    + " FROM " + table
                    + " WHERE " + column + " LIKE ?"
                    + " ORDER BY " + column + " COLLATE NOCASE ASC;", new String[] { getSuggestionArgument(input) });
            return cursorToColl(cursor, new LinkedList<String>(), GET_STRING_0).toArray(new String[cursor.getCount()]);
        } catch (final RuntimeException e) {
            Log.e("cannot get suggestions from " + table + "->" + column + " for input '" + input + "'", e);
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
    }

    public static String[] getSuggestionsOwnerName(final String input) {
        return getSuggestions(dbTableCaches, "owner_real", input);
    }

    public static String[] getSuggestionsTrackableCode(final String input) {
        return getSuggestions(dbTableTrackables, "tbcode", input);
    }

    public static String[] getSuggestionsFinderName(final String input) {
        return getSuggestions(dbTableLogs, "author", input);
    }

    public static String[] getSuggestionsGeocode(final String input) {
        return getSuggestions(dbTableCaches, "geocode", input);
    }

    public static String[] getSuggestionsKeyword(final String input) {
        return getSuggestions(dbTableCaches, "name", input);
    }

    /**
     *
     * @return list of last caches opened in the details view, ordered by most recent first
     */
    public static ArrayList<Geocache> getLastOpenedCaches() {
        final List<String> geocodes = Settings.getLastOpenedCaches();
        final Set<Geocache> cachesSet = DataStore.loadCaches(geocodes, LoadFlags.LOAD_CACHE_OR_DB);

        // order result set by time again
        final ArrayList<Geocache> caches = new ArrayList<>(cachesSet);
        Collections.sort(caches, new Comparator<Geocache>() {

            @Override
            public int compare(final Geocache lhs, final Geocache rhs) {
                final int lhsIndex = geocodes.indexOf(lhs.getGeocode());
                final int rhsIndex = geocodes.indexOf(rhs.getGeocode());
                return lhsIndex < rhsIndex ? -1 : (lhsIndex == rhsIndex ? 0 : 1);
            }
        });
        return caches;
    }

}
