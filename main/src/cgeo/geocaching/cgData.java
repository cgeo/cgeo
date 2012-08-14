package cgeo.geocaching;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.LoadFlag;
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.utils.Log;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

public class cgData {

    public enum StorageLocation {
        HEAP,
        CACHE,
        DATABASE,
    }

    /** The list of fields needed for mapping. */
    private static final String[] CACHE_COLUMNS = new String[] {
            "_id", "updated", "reason", "detailed", "detailedupdate", "visiteddate", "geocode", "cacheid", "guid", "type", "name", "own", "owner", "owner_real", "hidden", "hint", "size",
            "difficulty", "distance", "direction", "terrain", "latlon", "location", "latitude", "longitude", "elevation", "shortdesc",
            "favourite_cnt", "rating", "votes", "myvote", "disabled", "archived", "members", "found", "favourite", "inventorycoins", "inventorytags",
            "inventoryunknown", "onWatchlist", "personal_note", "reliable_latlon", "coordsChanged", "finalDefined"
            // reason is replaced by listId in cgCache
    };
    /** The list of fields needed for mapping. */
    private static final String[] WAYPOINT_COLUMNS = new String[] { "_id", "geocode", "updated", "type", "prefix", "lookup", "name", "latlon", "latitude", "longitude", "note", "own" };

    /** Number of days (as ms) after temporarily saved caches are deleted */
    private final static long DAYS_AFTER_CACHE_IS_DELETED = 3 * 24 * 60 * 60 * 1000;

    /**
     * holds the column indexes of the cache table to avoid lookups
     */
    private static int[] cacheColumnIndex;
    private CacheCache cacheCache = new CacheCache();
    private SQLiteDatabase database = null;
    private static final int dbVersion = 65;
    public static final int customListIdOffset = 10;
    private static final String dbName = "data";
    private static final String dbTableCaches = "cg_caches";
    private static final String dbTableLists = "cg_lists";
    private static final String dbTableAttributes = "cg_attributes";
    private static final String dbTableWaypoints = "cg_waypoints";
    private static final String dbTableSpoilers = "cg_spoilers";
    private static final String dbTableLogs = "cg_logs";
    private static final String dbTableLogCount = "cg_logCount";
    private static final String dbTableLogImages = "cg_logImages";
    private static final String dbTableLogsOffline = "cg_logs_offline";
    private static final String dbTableTrackables = "cg_trackables";
    private static final String dbTableSearchDestionationHistory = "cg_search_destination_history";
    private static final String dbCreateCaches = ""
            + "create table " + dbTableCaches + " ("
            + "_id integer primary key autoincrement, "
            + "updated long not null, "
            + "detailed integer not null default 0, "
            + "detailedupdate long, "
            + "visiteddate long, "
            + "geocode text unique not null, "
            + "reason integer not null default 0, " // cached, favourite...
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
            + "latlon text, "
            + "location text, "
            + "direction double, "
            + "distance double, "
            + "latitude double, "
            + "longitude double, "
            + "reliable_latlon integer, "
            + "elevation double, "
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
            + "finalDefined integer default 0"
            + "); ";
    private static final String dbCreateLists = ""
            + "create table " + dbTableLists + " ("
            + "_id integer primary key autoincrement, "
            + "title text not null, "
            + "updated long not null, "
            + "latitude double, "
            + "longitude double "
            + "); ";
    private static final String dbCreateAttributes = ""
            + "create table " + dbTableAttributes + " ("
            + "_id integer primary key autoincrement, "
            + "geocode text not null, "
            + "updated long not null, " // date of save
            + "attribute text "
            + "); ";
    private final static int ATTRIBUTES_GEOCODE = 2;
    private final static int ATTRIBUTES_UPDATED = 3;
    private final static int ATTRIBUTES_ATTRIBUTE = 4;

    private static final String dbCreateWaypoints = ""
            + "create table " + dbTableWaypoints + " ("
            + "_id integer primary key autoincrement, "
            + "geocode text not null, "
            + "updated long not null, " // date of save
            + "type text not null default 'waypoint', "
            + "prefix text, "
            + "lookup text, "
            + "name text, "
            + "latlon text, "
            + "latitude double, "
            + "longitude double, "
            + "note text, "
            + "own integer default 0"
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
    private final static int LOGS_GEOCODE = 2;
    private final static int LOGS_UPDATED = 3;
    private final static int LOGS_TYPE = 4;
    private final static int LOGS_AUTHOR = 5;
    private final static int LOGS_LOG = 6;
    private final static int LOGS_DATE = 7;
    private final static int LOGS_FOUND = 8;
    private final static int LOGS_FRIEND = 9;

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
            + "create table " + dbTableSearchDestionationHistory + " ("
            + "_id integer primary key autoincrement, "
            + "date long not null, "
            + "latitude double, "
            + "longitude double "
            + "); ";

    private HashMap<String, SQLiteStatement> statements = new HashMap<String, SQLiteStatement>();
    private static boolean newlyCreatedDatabase = false;

    public synchronized void init() {
        if (database != null) {
            return;
        }

        try {
            final DbHelper dbHelper = new DbHelper(new DBContext(cgeoapplication.getInstance()));
            database = dbHelper.getWritableDatabase();
        } catch (Exception e) {
            Log.e("cgData.init: unable to open database for R/W", e);
        }
    }

    public void closeDb() {
        if (database == null) {
            return;
        }

        cacheCache.removeAllFromCache();
        clearPreparedStatements();
        database.close();
        database = null;
    }

    private void clearPreparedStatements() {
        for (SQLiteStatement statement : statements.values()) {
            statement.close();
        }
        statements.clear();
    }

    private static File backupFile() {
        return new File(LocalStorage.getStorage(), "cgeo.sqlite");
    }

    public String backupDatabase() {
        if (!LocalStorage.isExternalStorageAvailable()) {
            Log.w("Database wasn't backed up: no external memory");
            return null;
        }

        final File target = backupFile();
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

    public boolean moveDatabase() {
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

        source.delete();
        Settings.setDbOnSDCard(!Settings.isDbOnSDCard());
        Log.i("Database was moved to " + target);
        init();
        return true;
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

    public static File isRestoreFile() {
        final File fileSourceFile = backupFile();
        return fileSourceFile.exists() ? fileSourceFile : null;
    }

    public boolean restoreDatabase() {
        if (!LocalStorage.isExternalStorageAvailable()) {
            Log.w("Database wasn't restored: no external memory");
            return false;
        }

        final File sourceFile = backupFile();
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

        public DBContext(Context base) {
            super(base);
        }

        /**
         * We override the default open/create as it doesn't work on OS 1.6 and
         * causes issues on other devices too.
         */
        @Override
        public SQLiteDatabase openOrCreateDatabase(String name, int mode,
                CursorFactory factory) {
            final File file = new File(name);
            file.getParentFile().mkdirs();
            return SQLiteDatabase.openOrCreateDatabase(file, factory);
        }

    }

    private static class DbHelper extends SQLiteOpenHelper {

        DbHelper(Context context) {
            super(context, databasePath().getPath(), null, dbVersion);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
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
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
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

                            Log.i("Added table " + dbTableSearchDestionationHistory + ".");
                        } catch (Exception e) {
                            Log.e("Failed to upgrade to ver. 52", e);
                        }
                    }

                    if (oldVersion < 53) { // upgrade to 53
                        try {
                            db.execSQL("alter table " + dbTableCaches + " add column onWatchlist integer");

                            Log.i("Column onWatchlist added to " + dbTableCaches + ".");
                        } catch (Exception e) {
                            Log.e("Failed to upgrade to ver. 53", e);
                        }
                    }

                    if (oldVersion < 54) { // update to 54
                        try {
                            db.execSQL(dbCreateLogImages);
                        } catch (Exception e) {
                            Log.e("Failed to upgrade to ver. 54: " + e.toString());

                        }
                    }

                    if (oldVersion < 55) { // update to 55
                        try {
                            db.execSQL("alter table " + dbTableCaches + " add column personal_note text");
                        } catch (Exception e) {
                            Log.e("Failed to upgrade to ver. 55: " + e.toString());
                        }
                    }

                    // make all internal attribute names lowercase
                    // @see issue #299
                    if (oldVersion < 56) { // update to 56
                        try {
                            db.execSQL("update " + dbTableAttributes + " set attribute = " +
                                    "lower(attribute) where attribute like \"%_yes\" " +
                                    "or attribute like \"%_no\"");
                        } catch (Exception e) {
                            Log.e("Failed to upgrade to ver. 56: " + e.toString());
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
                        } catch (Exception e) {
                            Log.e("Failed to upgrade to ver. 57: " + e.toString());
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
                                    + "latlon text, "
                                    + "location text, "
                                    + "direction double, "
                                    + "distance double, "
                                    + "latitude double, "
                                    + "longitude double, "
                                    + "reliable_latlon integer, "
                                    + "elevation double, "
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
                                    "hidden,hint,size,difficulty,terrain,latlon,location,direction,distance,latitude,longitude,reliable_latlon,elevation," +
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
                                    + "latlon text, "
                                    + "latitude double, "
                                    + "longitude double, "
                                    + "note text "
                                    + "); ";
                            db.execSQL(dbCreateWaypointsTemp);
                            db.execSQL("insert into " + dbTableWaypointsTemp + " select _id, geocode, updated, type, prefix, lookup, name, latlon, latitude, longitude, note from " + dbTableWaypoints);
                            db.execSQL("drop table " + dbTableWaypoints);
                            db.execSQL("alter table " + dbTableWaypointsTemp + " rename to " + dbTableWaypoints);

                            createIndices(db);

                            db.setTransactionSuccessful();

                            Log.i("Removed latitude_string and longitude_string columns");
                        } catch (Exception e) {
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
                        } catch (Exception e) {
                            Log.e("Failed to upgrade to ver. 59", e);
                        }
                    }

                    if (oldVersion < 60) {
                        try {
                            removeSecEmptyDirs();
                        } catch (Exception e) {
                            Log.e("Failed to upgrade to ver. 60", e);
                        }
                    }
                    if (oldVersion < 61) {
                        try {
                            db.execSQL("alter table " + dbTableLogs + " add column friend integer");
                            db.execSQL("alter table " + dbTableCaches + " add column coordsChanged integer default 0");
                        } catch (Exception e) {
                            Log.e("Failed to upgrade to ver. 61: " + e.toString());

                        }
                    }
                    // Introduces finalDefined on caches and own on waypoints
                    if (oldVersion < 62) {
                        try {
                            db.execSQL("alter table " + dbTableCaches + " add column finalDefined integer default 0");
                            db.execSQL("alter table " + dbTableWaypoints + " add column own integer default 0");
                            db.execSQL("update " + dbTableWaypoints + " set own = 1 where type = 'own'");
                        } catch (Exception e) {
                            Log.e("Failed to upgrade to ver. 62: " + e.toString());

                        }
                    }
                    if (oldVersion < 63) {
                        try {
                            removeDoubleUnderscoreMapFiles();
                        } catch (Exception e) {
                            Log.e("Failed to upgrade to ver. 63: " + e.toString());

                        }
                    }

                    if (oldVersion < 64) {
                        try {
                            // No cache should ever be stored into the ALL_CACHES list. Here we use hardcoded list ids
                            // rather than symbolic ones because the fix must be applied with the values at the time
                            // of the problem. The problem was introduced in release 2012.06.01.
                            db.execSQL("update " + dbTableCaches + " set reason=1 where reason=2");
                        } catch (Exception e) {
                            Log.e("Failed to upgrade to ver. 64", e);
                        }
                    }

                    if (oldVersion < 65) {
                        try {
                            // Set all waypoints where name is Original coordinates to type ORIGINAL
                            db.execSQL("update " + dbTableWaypoints + " set type='original', own=0 where name='Original Coordinates'");
                        } catch (Exception e) {
                            Log.e("Failed to upgrade to ver. 65:" + e.toString());
                        }
                    }
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            Log.i("Upgrade database from ver. " + oldVersion + " to ver. " + newVersion + ": completed");
        }

        /**
         * Method to remove static map files with double underscore due to issue#1670
         * introduced with release on 2012-05-24.
         */
        private static void removeDoubleUnderscoreMapFiles() {
            File[] geocodeDirs = LocalStorage.getStorage().listFiles();
            final FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return (filename.startsWith("map_") && filename.contains("__"));
                }
            };
            for (File dir : geocodeDirs) {
                File[] wrongFiles = dir.listFiles(filter);
                for (File wrongFile : wrongFiles) {
                    wrongFile.delete();
                }
            }
        }
    }

    /**
     * Remove obsolete cache directories in c:geo private storage.
     *
     * @param db
     *            the read-write database to use
     */
    private static void removeObsoleteCacheDirectories(final SQLiteDatabase db) {
        final Pattern oldFilePattern = Pattern.compile("^[GC|TB|O][A-Z0-9]{4,7}$");
        final SQLiteStatement select = db.compileStatement("select count(*) from " + dbTableCaches + " where geocode = ?");
        final File[] files = LocalStorage.getStorage().listFiles();
        final ArrayList<File> toRemove = new ArrayList<File>(files.length);
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (final File dir : toRemove) {
                    Log.i("Removing obsolete cache directory for " + dir.getName());
                    LocalStorage.deleteDirectory(dir);
                }
            }
        }).start();
    }

    /*
     * Remove empty directories created in the secondary storage area.
     */
    private static void removeSecEmptyDirs() {
        for (final File file : LocalStorage.getStorageSec().listFiles()) {
            if (file.isDirectory()) {
                // This will silently fail if the directory is not empty.
                file.delete();
            }
        }
    }

    private static void dropDatabase(SQLiteDatabase db) {
        db.execSQL("drop table if exists " + dbTableCaches);
        db.execSQL("drop table if exists " + dbTableAttributes);
        db.execSQL("drop table if exists " + dbTableWaypoints);
        db.execSQL("drop table if exists " + dbTableSpoilers);
        db.execSQL("drop table if exists " + dbTableLogs);
        db.execSQL("drop table if exists " + dbTableLogCount);
        db.execSQL("drop table if exists " + dbTableLogsOffline);
        db.execSQL("drop table if exists " + dbTableTrackables);
    }

    public String[] allDetailedThere() {
        init();

        Cursor cursor = null;
        List<String> list = new ArrayList<String>();

        try {
            long timestamp = System.currentTimeMillis() - DAYS_AFTER_CACHE_IS_DELETED;
            cursor = database.query(
                    dbTableCaches,
                    new String[]{"geocode"},
                    "(detailed = 1 and detailedupdate > ?) or reason > 0",
                    new String[]{Long.toString(timestamp)},
                    null,
                    null,
                    "detailedupdate desc",
                    "100");

            if (cursor != null) {
                int index;

                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    index = cursor.getColumnIndex("geocode");

                    do {
                        list.add(cursor.getString(index));
                    } while (cursor.moveToNext());
                } else {
                    cursor.close();
                    return null;
                }
            }
        } catch (Exception e) {
            Log.e("cgData.allDetailedThere: " + e.toString());
        }

        if (cursor != null) {
            cursor.close();
        }

        return list.toArray(new String[list.size()]);
    }

    public boolean isThere(String geocode, String guid, boolean detailed, boolean checkTime) {
        init();

        Cursor cursor = null;

        int cnt = 0;
        long dataUpdated = 0;
        long dataDetailedUpdate = 0;
        int dataDetailed = 0;

        try {
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

            if (cursor != null) {
                int index;
                cnt = cursor.getCount();

                if (cnt > 0) {
                    cursor.moveToFirst();

                    index = cursor.getColumnIndex("updated");
                    dataUpdated = cursor.getLong(index);
                    index = cursor.getColumnIndex("detailedupdate");
                    dataDetailedUpdate = cursor.getLong(index);
                    index = cursor.getColumnIndex("detailed");
                    dataDetailed = cursor.getInt(index);
                }
            }
        } catch (Exception e) {
            Log.e("cgData.isThere: " + e.toString());
        }

        if (cursor != null) {
            cursor.close();
        }

        if (cnt > 0) {
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

        // we have no such cache stored in cache
        return false;
    }

    /** is cache stored in one of the lists (not only temporary) */
    public boolean isOffline(String geocode, String guid) {
        if (StringUtils.isBlank(geocode) && StringUtils.isBlank(guid)) {
            return false;
        }
        init();

        try {
            final SQLiteStatement listId;
            final String value;
            if (StringUtils.isNotBlank(geocode)) {
                listId = getStatementListIdFromGeocode();
                value = geocode;
            }
            else {
                listId = getStatementListIdFromGuid();
                value = guid;
            }
            synchronized (listId) {
                listId.bindString(1, value);
                return listId.simpleQueryForLong() != StoredList.TEMPORARY_LIST_ID;
            }
        } catch (SQLiteDoneException e) {
            // Do nothing, it only means we have no information on the cache
        } catch (Exception e) {
            Log.e("cgData.isOffline", e);
        }

        return false;
    }

    public String getGeocodeForGuid(String guid) {
        if (StringUtils.isBlank(guid)) {
            return null;
        }
        init();

        try {
            final SQLiteStatement description = getStatementGeocode();
            synchronized (description) {
                description.bindString(1, guid);
                return description.simpleQueryForString();
            }
        } catch (SQLiteDoneException e) {
            // Do nothing, it only means we have no information on the cache
        } catch (Exception e) {
            Log.e("cgData.getGeocodeForGuid", e);
        }

        return null;
    }

    public String getCacheidForGeocode(String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }
        init();

        try {
            final SQLiteStatement description = getStatementCacheId();
            synchronized (description) {
                description.bindString(1, geocode);
                return description.simpleQueryForString();
            }
        } catch (SQLiteDoneException e) {
            // Do nothing, it only means we have no information on the cache
        } catch (Exception e) {
            Log.e("cgData.getCacheidForGeocode", e);
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
     * @return true = cache saved successfully to the CacheCache/DB
     */
    public boolean saveCache(cgCache cache, EnumSet<LoadFlags.SaveFlag> saveFlags) {
        if (cache == null) {
            throw new IllegalArgumentException("cache must not be null");
        }

        // merge always with data already stored in the CacheCache or DB
        if (saveFlags.contains(SaveFlag.SAVE_CACHE)) {
            cache.gatherMissingFrom(cacheCache.getCacheFromCache(cache.getGeocode()));
            cacheCache.putCacheInCache(cache);
        }

        if (!saveFlags.contains(SaveFlag.SAVE_DB)) {
            return true;
        }
        boolean updateRequired = !cache.gatherMissingFrom(loadCache(cache.getGeocode(), LoadFlags.LOAD_ALL_DB_ONLY));

        // only save a cache to the database if
        // - the cache is detailed
        // - there are changes
        // - the cache is only stored in the CacheCache so far
        if ((!updateRequired || !cache.isDetailed()) && cache.getStorageLocation().contains(StorageLocation.DATABASE)) {
            return false;
        }

        cache.addStorageLocation(StorageLocation.DATABASE);
        cacheCache.putCacheInCache(cache);
        Log.d("Saving " + cache.toString() + " (" + cache.getListId() + ") to DB");

        ContentValues values = new ContentValues();

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
        values.put("own", cache.isOwn() ? 1 : 0);
        values.put("owner", cache.getOwnerDisplayName());
        values.put("owner_real", cache.getOwnerUserId());
        if (cache.getHiddenDate() == null) {
            values.put("hidden", 0);
        } else {
            values.put("hidden", cache.getHiddenDate().getTime());
        }
        values.put("hint", cache.getHint());
        values.put("size", cache.getSize() == null ? "" : cache.getSize().id);
        values.put("difficulty", cache.getDifficulty());
        values.put("terrain", cache.getTerrain());
        values.put("latlon", cache.getLatlon());
        values.put("location", cache.getLocation());
        values.put("distance", cache.getDistance());
        values.put("direction", cache.getDirection());
        putCoords(values, cache.getCoords());
        values.put("reliable_latlon", cache.isReliableLatLon() ? 1 : 0);
        values.put("elevation", cache.getElevation());
        values.put("shortdesc", cache.getShortdesc());
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

        boolean result = false;
        init();

        //try to update record else insert fresh..
        database.beginTransaction();

        try {
            saveAttributesWithoutTransaction(cache);
            saveOriginalWaypointsWithoutTransaction(cache);
            saveSpoilersWithoutTransaction(cache);
            saveLogsWithoutTransaction(cache.getGeocode(), cache.getLogs());
            saveLogCountsWithoutTransaction(cache);
            saveInventoryWithoutTransaction(cache.getGeocode(), cache.getInventory());

            int rows = database.update(dbTableCaches, values, "geocode = ?", new String[] { cache.getGeocode() });
            if (rows == 0) {
                // cache is not in the DB, insert it
                /* long id = */
                database.insert(dbTableCaches, null, values);
            }
            database.setTransactionSuccessful();
            result = true;
        } catch (Exception e) {
            // nothing
        } finally {
            database.endTransaction();
        }

        return result;
    }

    private void saveAttributesWithoutTransaction(final cgCache cache) {
        String geocode = cache.getGeocode();
        database.delete(dbTableAttributes, "geocode = ?", new String[]{geocode});

        final List<String> attributes = cache.getAttributes();
        if (CollectionUtils.isNotEmpty(attributes)) {

            InsertHelper helper = new InsertHelper(database, dbTableAttributes);
            long timeStamp = System.currentTimeMillis();

            for (String attribute : attributes) {
                helper.prepareForInsert();

                helper.bind(ATTRIBUTES_GEOCODE, geocode);
                helper.bind(ATTRIBUTES_UPDATED, timeStamp);
                helper.bind(ATTRIBUTES_ATTRIBUTE, attribute);

                helper.execute();
            }
            helper.close();
        }
    }

    /**
     * Persists the given <code>destination</code> into the database.
     *
     * @param destination
     *            a destination to save
     */
    public void saveSearchedDestination(final Destination destination) {
        init();

        database.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put("date", destination.getDate());
            putCoords(values, destination.getCoords());
            database.insert(dbTableSearchDestionationHistory, null, values);
            database.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("Updating searchedDestinations db failed", e);
        } finally {
            database.endTransaction();
        }
    }

    public boolean saveWaypoints(final cgCache cache) {
        boolean result = false;
        init();
        database.beginTransaction();

        try {
            saveOriginalWaypointsWithoutTransaction(cache);
            database.setTransactionSuccessful();
            result = true;
        } catch (Exception e) {
            Log.e("saveWaypoints", e);
        } finally {
            database.endTransaction();
        }
        return result;
    }

    private void saveOriginalWaypointsWithoutTransaction(final cgCache cache) {
        String geocode = cache.getGeocode();
        database.delete(dbTableWaypoints, "geocode = ? and type <> ? and own = 0", new String[]{geocode, "own"});

        List<cgWaypoint> waypoints = cache.getWaypoints();
        if (CollectionUtils.isNotEmpty(waypoints)) {
            ContentValues values = new ContentValues();
            long timeStamp = System.currentTimeMillis();
            for (cgWaypoint oneWaypoint : waypoints) {
                if (oneWaypoint.isUserDefined()) {
                    continue;
                }

                values.clear();
                values.put("geocode", geocode);
                values.put("updated", timeStamp);
                values.put("type", oneWaypoint.getWaypointType() != null ? oneWaypoint.getWaypointType().id : null);
                values.put("prefix", oneWaypoint.getPrefix());
                values.put("lookup", oneWaypoint.getLookup());
                values.put("name", oneWaypoint.getName());
                values.put("latlon", oneWaypoint.getLatlon());
                putCoords(values, oneWaypoint.getCoords());
                values.put("note", oneWaypoint.getNote());
                values.put("own", oneWaypoint.isUserDefined() ? 1 : 0);

                final long rowId = database.insert(dbTableWaypoints, null, values);
                oneWaypoint.setId((int) rowId);
            }
        }
    }

    /**
     * Save coordinates into a ContentValues
     *
     * @param values
     *            a ContentValues to save coordinates in
     * @param oneWaypoint
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

    public boolean saveWaypoint(int id, String geocode, cgWaypoint waypoint) {
        if ((StringUtils.isBlank(geocode) && id <= 0) || waypoint == null) {
            return false;
        }

        init();

        boolean ok = false;
        database.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("geocode", geocode);
            values.put("updated", System.currentTimeMillis());
            values.put("type", waypoint.getWaypointType() != null ? waypoint.getWaypointType().id : null);
            values.put("prefix", waypoint.getPrefix());
            values.put("lookup", waypoint.getLookup());
            values.put("name", waypoint.getName());
            values.put("latlon", waypoint.getLatlon());
            putCoords(values, waypoint.getCoords());
            values.put("note", waypoint.getNote());
            values.put("own", waypoint.isUserDefined() ? 1 : 0);

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

    public boolean deleteWaypoint(int id) {
        if (id == 0) {
            return false;
        }

        init();

        return database.delete(dbTableWaypoints, "_id = " + id, null) > 0;
    }

    private void saveSpoilersWithoutTransaction(final cgCache cache) {
        String geocode = cache.getGeocode();
        database.delete(dbTableSpoilers, "geocode = ?", new String[]{geocode});

        List<cgImage> spoilers = cache.getSpoilers();
        if (CollectionUtils.isNotEmpty(spoilers)) {
            ContentValues values = new ContentValues();
            long timeStamp = System.currentTimeMillis();
            for (cgImage spoiler : spoilers) {
                values.clear();
                values.put("geocode", geocode);
                values.put("updated", timeStamp);
                values.put("url", spoiler.getUrl());
                values.put("title", spoiler.getTitle());
                values.put("description", spoiler.getDescription());

                database.insert(dbTableSpoilers, null, values);
            }
        }
    }

    private void saveLogsWithoutTransaction(final String geocode, final List<LogEntry> logs) {
        // TODO delete logimages referring these logs
        database.delete(dbTableLogs, "geocode = ?", new String[]{geocode});

        if (CollectionUtils.isNotEmpty(logs)) {
            InsertHelper helper = new InsertHelper(database, dbTableLogs);
            long timeStamp = System.currentTimeMillis();
            for (LogEntry log : logs) {
                helper.prepareForInsert();

                helper.bind(LOGS_GEOCODE, geocode);
                helper.bind(LOGS_UPDATED, timeStamp);
                helper.bind(LOGS_TYPE, log.type.id);
                helper.bind(LOGS_AUTHOR, log.author);
                helper.bind(LOGS_LOG, log.log);
                helper.bind(LOGS_DATE, log.date);
                helper.bind(LOGS_FOUND, log.found);
                helper.bind(LOGS_FRIEND, log.friend);

                long log_id = helper.execute();

                if (log.hasLogImages()) {
                    ContentValues values = new ContentValues();
                    for (cgImage img : log.getLogImages()) {
                        values.clear();
                        values.put("log_id", log_id);
                        values.put("title", img.getTitle());
                        values.put("url", img.getUrl());
                        database.insert(dbTableLogImages, null, values);
                    }
                }
            }
            helper.close();
        }
    }

    private void saveLogCountsWithoutTransaction(final cgCache cache) {
        String geocode = cache.getGeocode();
        database.delete(dbTableLogCount, "geocode = ?", new String[]{geocode});

        Map<LogType, Integer> logCounts = cache.getLogCounts();
        if (MapUtils.isNotEmpty(logCounts)) {
            ContentValues values = new ContentValues();

            Set<Entry<LogType, Integer>> logCountsItems = logCounts.entrySet();
            long timeStamp = System.currentTimeMillis();
            for (Entry<LogType, Integer> pair : logCountsItems) {
                values.clear();
                values.put("geocode", geocode);
                values.put("updated", timeStamp);
                values.put("type", pair.getKey().id);
                values.put("count", pair.getValue());

                database.insert(dbTableLogCount, null, values);
            }
        }
    }

    public boolean saveTrackable(final cgTrackable trackable) {
        init();

        database.beginTransaction();
        try {
            saveInventoryWithoutTransaction(null, Collections.singletonList(trackable));
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

        return true;
    }

    private void saveInventoryWithoutTransaction(final String geocode, final List<cgTrackable> trackables) {
        if (geocode != null) {
            database.delete(dbTableTrackables, "geocode = ?", new String[]{geocode});
        }

        if (CollectionUtils.isNotEmpty(trackables)) {
            ContentValues values = new ContentValues();
            long timeStamp = System.currentTimeMillis();
            for (cgTrackable trackable : trackables) {
                values.clear();
                if (geocode != null) {
                    values.put("geocode", geocode);
                }
                values.put("updated", timeStamp);
                values.put("tbcode", trackable.getGeocode());
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

                saveLogsWithoutTransaction(trackable.getGeocode(), trackable.getLogs());
            }
        }
    }

    public Viewport getBounds(final Set<String> geocodes) {
        if (CollectionUtils.isEmpty(geocodes)) {
            return null;
        }

        final Set<cgCache> caches = loadCaches(geocodes, LoadFlags.LOAD_CACHE_OR_DB);
        return Viewport.containing(caches);
    }

    /**
     * Load a single Cache.
     *
     * @param geocode
     *            The Geocode GCXXXX
     * @return the loaded cache (if found). Can be null
     */
    public cgCache loadCache(final String geocode, final EnumSet<LoadFlag> loadFlags) {
        if (StringUtils.isBlank(geocode)) {
            throw new IllegalArgumentException("geocode must not be empty");
        }

        final Set<cgCache> caches = loadCaches(Collections.singleton(geocode), loadFlags);
        return caches.isEmpty() ? null : caches.iterator().next();
    }

    /**
     * Load caches.
     *
     * @param geocodes
     * @return Set of loaded caches. Never null.
     */
    public Set<cgCache> loadCaches(final Set<String> geocodes, final EnumSet<LoadFlag> loadFlags) {
        if (CollectionUtils.isEmpty(geocodes)) {
            return new HashSet<cgCache>();
        }

        Set<cgCache> result = new HashSet<cgCache>();
        Set<String> remaining = new HashSet<String>(geocodes);

        if (loadFlags.contains(LoadFlag.LOAD_CACHE_BEFORE)) {
            for (String geocode : new HashSet<String>(remaining)) {
                cgCache cache = cacheCache.getCacheFromCache(geocode);
                if (cache != null) {
                    result.add(cache);
                    remaining.remove(cache.getGeocode());
                }
            }
        }

        if (loadFlags.contains(LoadFlag.LOAD_DB_MINIMAL) ||
                loadFlags.contains(LoadFlag.LOAD_ATTRIBUTES) ||
                loadFlags.contains(LoadFlag.LOAD_WAYPOINTS) ||
                loadFlags.contains(LoadFlag.LOAD_SPOILERS) ||
                loadFlags.contains(LoadFlag.LOAD_LOGS) ||
                loadFlags.contains(LoadFlag.LOAD_INVENTORY) ||
                loadFlags.contains(LoadFlag.LOAD_OFFLINE_LOG)) {

            final Set<cgCache> cachesFromDB = loadCachesFromGeocodes(remaining, loadFlags);
            result.addAll(cachesFromDB);
            for (final cgCache cache : cachesFromDB) {
                remaining.remove(cache.getGeocode());
            }
        }

        if (loadFlags.contains(LoadFlag.LOAD_CACHE_AFTER)) {
            for (String geocode : new HashSet<String>(remaining)) {
                cgCache cache = cacheCache.getCacheFromCache(geocode);
                if (cache != null) {
                    result.add(cache);
                    remaining.remove(cache.getGeocode());
                }
            }
        }

        if (remaining.size() >= 1) {
            Log.e("cgData.loadCaches(" + remaining.toString() + ") failed");
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
    private Set<cgCache> loadCachesFromGeocodes(final Set<String> geocodes, final EnumSet<LoadFlag> loadFlags) {
        if (CollectionUtils.isEmpty(geocodes)) {
            return Collections.emptySet();
        }


        Log.d("cgData.loadCachesFromGeocodes(" + geocodes.toString() + ") from DB");

        init();

        final StringBuilder query = new StringBuilder("SELECT ");
        for (int i = 0; i < CACHE_COLUMNS.length; i++) {
            query.append(i > 0 ? ", " : "").append(dbTableCaches).append('.').append(CACHE_COLUMNS[i]).append(' ');
        }
        if (loadFlags.contains(LoadFlag.LOAD_OFFLINE_LOG)) {
            query.append(',').append(dbTableLogsOffline).append(".log");
        }

        query.append(" FROM ").append(dbTableCaches);
        if (loadFlags.contains(LoadFlag.LOAD_OFFLINE_LOG)) {
            query.append(" LEFT OUTER JOIN ").append(dbTableLogsOffline).append(" ON ( ").append(dbTableCaches).append(".geocode == ").append(dbTableLogsOffline).append(".geocode) ");
        }

        query.append(" WHERE ").append(dbTableCaches).append('.');
        query.append(cgData.whereGeocodeIn(geocodes));

        Cursor cursor = database.rawQuery(query.toString(), null);
        try {
            if (!cursor.moveToFirst()) {
                return Collections.emptySet();
            }

            final Set<cgCache> caches = new HashSet<cgCache>();
            int logIndex = -1;
            do {
                //Extracted Method = LOADDBMINIMAL
                cgCache cache = cgData.createCacheFromDatabaseContent(cursor);

                if (loadFlags.contains(LoadFlag.LOAD_ATTRIBUTES)) {
                    cache.setAttributes(loadAttributes(cache.getGeocode()));
                }

                if (loadFlags.contains(LoadFlag.LOAD_WAYPOINTS)) {
                    final List<cgWaypoint> waypoints = loadWaypoints(cache.getGeocode());
                    if (CollectionUtils.isNotEmpty(waypoints)) {
                        cache.setWaypoints(waypoints, false);
                    }
                }

                if (loadFlags.contains(LoadFlag.LOAD_SPOILERS)) {
                    final List<cgImage> spoilers = loadSpoilers(cache.getGeocode());
                    if (CollectionUtils.isNotEmpty(spoilers)) {
                        if (cache.getSpoilers() == null) {
                            cache.setSpoilers(new ArrayList<cgImage>());
                        } else {
                            cache.getSpoilers().clear();
                        }
                        cache.getSpoilers().addAll(spoilers);
                    }
                }

                if (loadFlags.contains(LoadFlag.LOAD_LOGS)) {
                    cache.setLogs(loadLogs(cache.getGeocode()));
                    final Map<LogType, Integer> logCounts = loadLogCounts(cache.getGeocode());
                    if (MapUtils.isNotEmpty(logCounts)) {
                        cache.getLogCounts().clear();
                        cache.getLogCounts().putAll(logCounts);
                    }
                }

                if (loadFlags.contains(LoadFlag.LOAD_INVENTORY)) {
                    final List<cgTrackable> inventory = loadInventory(cache.getGeocode());
                    if (CollectionUtils.isNotEmpty(inventory)) {
                        if (cache.getInventory() == null) {
                            cache.setInventory(new ArrayList<cgTrackable>());
                        } else {
                            cache.getInventory().clear();
                        }
                        cache.getInventory().addAll(inventory);
                    }
                }

                if (loadFlags.contains(LoadFlag.LOAD_OFFLINE_LOG)) {
                    if (logIndex < 0) {
                        logIndex = cursor.getColumnIndex("log");
                    }
                    cache.setLogOffline(!cursor.isNull(logIndex));
                }
                cache.addStorageLocation(StorageLocation.DATABASE);
                cacheCache.putCacheInCache(cache);

                caches.add(cache);
            } while (cursor.moveToNext());
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

    private static String buildCoordinateWhere(final String dbTable, final Viewport viewport) {
        return viewport.resize(1.5).sqlWhere(dbTable);
    }

    /**
     * creates a Cache from the cursor. Doesn't next.
     *
     * @param cursor
     * @return Cache from DB
     */
    private static cgCache createCacheFromDatabaseContent(Cursor cursor) {
        int index;
        cgCache cache = new cgCache();

        if (cacheColumnIndex == null) {
            int[] local_cci = new int[41]; // use a local variable to avoid having the not yet fully initialized array be visible to other threads
            local_cci[0] = cursor.getColumnIndex("updated");
            local_cci[1] = cursor.getColumnIndex("reason");
            local_cci[2] = cursor.getColumnIndex("detailed");
            local_cci[3] = cursor.getColumnIndex("detailedupdate");
            local_cci[4] = cursor.getColumnIndex("visiteddate");
            local_cci[5] = cursor.getColumnIndex("geocode");
            local_cci[6] = cursor.getColumnIndex("cacheid");
            local_cci[7] = cursor.getColumnIndex("guid");
            local_cci[8] = cursor.getColumnIndex("type");
            local_cci[9] = cursor.getColumnIndex("name");
            local_cci[10] = cursor.getColumnIndex("own");
            local_cci[11] = cursor.getColumnIndex("owner");
            local_cci[12] = cursor.getColumnIndex("owner_real");
            local_cci[13] = cursor.getColumnIndex("hidden");
            local_cci[14] = cursor.getColumnIndex("hint");
            local_cci[15] = cursor.getColumnIndex("size");
            local_cci[16] = cursor.getColumnIndex("difficulty");
            local_cci[17] = cursor.getColumnIndex("direction");
            local_cci[18] = cursor.getColumnIndex("distance");
            local_cci[19] = cursor.getColumnIndex("terrain");
            local_cci[20] = cursor.getColumnIndex("latlon");
            local_cci[21] = cursor.getColumnIndex("location");
            local_cci[22] = cursor.getColumnIndex("elevation");
            local_cci[23] = cursor.getColumnIndex("personal_note");
            local_cci[24] = cursor.getColumnIndex("shortdesc");
            local_cci[25] = cursor.getColumnIndex("favourite_cnt");
            local_cci[26] = cursor.getColumnIndex("rating");
            local_cci[27] = cursor.getColumnIndex("votes");
            local_cci[28] = cursor.getColumnIndex("myvote");
            local_cci[29] = cursor.getColumnIndex("disabled");
            local_cci[30] = cursor.getColumnIndex("archived");
            local_cci[31] = cursor.getColumnIndex("members");
            local_cci[32] = cursor.getColumnIndex("found");
            local_cci[33] = cursor.getColumnIndex("favourite");
            local_cci[34] = cursor.getColumnIndex("inventoryunknown");
            local_cci[35] = cursor.getColumnIndex("onWatchlist");
            local_cci[36] = cursor.getColumnIndex("reliable_latlon");
            local_cci[37] = cursor.getColumnIndex("coordsChanged");
            local_cci[38] = cursor.getColumnIndex("latitude");
            local_cci[39] = cursor.getColumnIndex("longitude");
            local_cci[40] = cursor.getColumnIndex("finalDefined");
            cacheColumnIndex = local_cci;
        }

        cache.setUpdated(cursor.getLong(cacheColumnIndex[0]));
        cache.setListId(cursor.getInt(cacheColumnIndex[1]));
        cache.setDetailed(cursor.getInt(cacheColumnIndex[2]) == 1);
        cache.setDetailedUpdate(cursor.getLong(cacheColumnIndex[3]));
        cache.setVisitedDate(cursor.getLong(cacheColumnIndex[4]));
        cache.setGeocode(cursor.getString(cacheColumnIndex[5]));
        cache.setCacheId(cursor.getString(cacheColumnIndex[6]));
        cache.setGuid(cursor.getString(cacheColumnIndex[7]));
        cache.setType(CacheType.getById(cursor.getString(cacheColumnIndex[8])));
        cache.setName(cursor.getString(cacheColumnIndex[9]));
        cache.setOwn(cursor.getInt(cacheColumnIndex[10]) == 1);
        cache.setOwnerDisplayName(cursor.getString(cacheColumnIndex[11]));
        cache.setOwnerUserId(cursor.getString(cacheColumnIndex[12]));
        long dateValue = cursor.getLong(cacheColumnIndex[13]);
        if (dateValue != 0) {
            cache.setHidden(new Date(dateValue));
        }
        cache.setHint(cursor.getString(cacheColumnIndex[14]));
        cache.setSize(CacheSize.getById(cursor.getString(cacheColumnIndex[15])));
        cache.setDifficulty(cursor.getFloat(cacheColumnIndex[16]));
        index = cacheColumnIndex[17];
        if (cursor.isNull(index)) {
            cache.setDirection(null);
        } else {
            cache.setDirection(cursor.getFloat(index));
        }
        index = cacheColumnIndex[18];
        if (cursor.isNull(index)) {
            cache.setDistance(null);
        } else {
            cache.setDistance(cursor.getFloat(index));
        }
        cache.setTerrain(cursor.getFloat(cacheColumnIndex[19]));
        cache.setLatlon(cursor.getString(cacheColumnIndex[20]));
        cache.setLocation(cursor.getString(cacheColumnIndex[21]));
        cache.setCoords(getCoords(cursor, cacheColumnIndex[38], cacheColumnIndex[39]));
        index = cacheColumnIndex[22];
        if (cursor.isNull(index)) {
            cache.setElevation(null);
        } else {
            cache.setElevation(cursor.getDouble(index));
        }
        cache.setPersonalNote(cursor.getString(cacheColumnIndex[23]));
        cache.setShortdesc(cursor.getString(cacheColumnIndex[24]));
        // do not set cache.description !
        cache.setFavoritePoints(cursor.getInt(cacheColumnIndex[25]));
        cache.setRating(cursor.getFloat(cacheColumnIndex[26]));
        cache.setVotes(cursor.getInt(cacheColumnIndex[27]));
        cache.setMyVote(cursor.getFloat(cacheColumnIndex[28]));
        cache.setDisabled(cursor.getInt(cacheColumnIndex[29]) == 1);
        cache.setArchived(cursor.getInt(cacheColumnIndex[30]) == 1);
        cache.setPremiumMembersOnly(cursor.getInt(cacheColumnIndex[31]) == 1);
        cache.setFound(cursor.getInt(cacheColumnIndex[32]) == 1);
        cache.setFavorite(cursor.getInt(cacheColumnIndex[33]) == 1);
        cache.setInventoryItems(cursor.getInt(cacheColumnIndex[34]));
        cache.setOnWatchlist(cursor.getInt(cacheColumnIndex[35]) == 1);
        cache.setReliableLatLon(cursor.getInt(cacheColumnIndex[36]) > 0);
        cache.setUserModifiedCoords(cursor.getInt(cacheColumnIndex[37]) > 0);
        cache.setFinalDefined(cursor.getInt(cacheColumnIndex[40]) > 0);

        Log.d("Loading " + cache.toString() + " (" + cache.getListId() + ") from DB");

        return cache;
    }

    public List<String> loadAttributes(String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        init();

        ArrayList<String> attributes = new ArrayList<String>();

        Cursor cursor = database.query(
                dbTableAttributes,
                new String[]{"attribute"},
                "geocode = ?",
                new String[]{geocode},
                null,
                null,
                null,
                "100");

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("attribute");

            do {
                attributes.add(cursor.getString(index));
            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }

        return attributes;
    }

    public cgWaypoint loadWaypoint(int id) {
        if (id == 0) {
            return null;
        }

        init();

        cgWaypoint waypoint = null;

        Cursor cursor = database.query(
                dbTableWaypoints,
                WAYPOINT_COLUMNS,
                "_id = ?",
                new String[]{Integer.toString(id)},
                null,
                null,
                null,
                "1");

        Log.d("cgData.loadWaypoint(" + id + ")");

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();

            waypoint = createWaypointFromDatabaseContent(cursor);
        }

        if (cursor != null) {
            cursor.close();
        }

        return waypoint;
    }

    public List<cgWaypoint> loadWaypoints(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        init();

        List<cgWaypoint> waypoints = new ArrayList<cgWaypoint>();

        Cursor cursor = database.query(
                dbTableWaypoints,
                WAYPOINT_COLUMNS,
                "geocode = ?",
                new String[]{geocode},
                null,
                null,
                "_id",
                "100");

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();

            do {

                cgWaypoint waypoint = createWaypointFromDatabaseContent(cursor);

                waypoints.add(waypoint);
            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }

        return waypoints;
    }

    private static cgWaypoint createWaypointFromDatabaseContent(Cursor cursor) {
        String name = cursor.getString(cursor.getColumnIndex("name"));
        WaypointType type = WaypointType.findById(cursor.getString(cursor.getColumnIndex("type")));
        boolean own = cursor.getInt(cursor.getColumnIndex("own")) != 0;
        cgWaypoint waypoint = new cgWaypoint(name, type, own);

        waypoint.setId(cursor.getInt(cursor.getColumnIndex("_id")));
        waypoint.setGeocode(cursor.getString(cursor.getColumnIndex("geocode")));
        waypoint.setPrefix(cursor.getString(cursor.getColumnIndex("prefix")));
        waypoint.setLookup(cursor.getString(cursor.getColumnIndex("lookup")));
        waypoint.setLatlon(cursor.getString(cursor.getColumnIndex("latlon")));
        waypoint.setCoords(getCoords(cursor, cursor.getColumnIndex("latitude"), cursor.getColumnIndex("longitude")));
        waypoint.setNote(cursor.getString(cursor.getColumnIndex("note")));

        return waypoint;
    }

    private List<cgImage> loadSpoilers(String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        init();

        List<cgImage> spoilers = new ArrayList<cgImage>();

        Cursor cursor = database.query(
                dbTableSpoilers,
                new String[]{"url", "title", "description"},
                "geocode = ?",
                new String[]{geocode},
                null,
                null,
                null,
                "100");

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            int indexUrl = cursor.getColumnIndex("url");
            int indexTitle = cursor.getColumnIndex("title");
            int indexDescription = cursor.getColumnIndex("description");

            do {
                cgImage spoiler = new cgImage(cursor.getString(indexUrl), cursor.getString(indexTitle), cursor.getString(indexDescription));

                spoilers.add(spoiler);
            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }

        return spoilers;
    }

    /**
     * Loads the history of previously entered destinations from
     * the database. If no destinations exist, an {@link Collections#emptyList()} will be returned.
     *
     * @return A list of previously entered destinations or an empty list.
     */
    public List<Destination> loadHistoryOfSearchedLocations() {
        init();

        Cursor cursor = database.query(dbTableSearchDestionationHistory,
                new String[]{"_id", "date", "latitude", "longitude"},
                null,
                null,
                null,
                null,
                "date desc",
                "100");

        final List<Destination> destinations = new LinkedList<Destination>();

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            int indexId = cursor.getColumnIndex("_id");
            int indexDate = cursor.getColumnIndex("date");
            int indexLatitude = cursor.getColumnIndex("latitude");
            int indexLongitude = cursor.getColumnIndex("longitude");

            do {
                final Destination dest = new Destination(cursor.getLong(indexId), cursor.getLong(indexDate), getCoords(cursor, indexLatitude, indexLongitude));

                // If coordinates are non-existent or invalid, do not consider
                // this point.
                if (dest.getCoords() != null) {
                    destinations.add(dest);
                }
            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }

        return destinations;
    }

    public boolean clearSearchedDestinations() {
        boolean success = true;
        init();
        database.beginTransaction();

        try {
            database.delete(dbTableSearchDestionationHistory, null, null);
            database.setTransactionSuccessful();
        } catch (Exception e) {
            success = false;
            Log.e("Unable to clear searched destinations", e);
        } finally {
            database.endTransaction();
        }

        return success;
    }

    public List<LogEntry> loadLogs(String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        init();

        List<LogEntry> logs = new ArrayList<LogEntry>();

        Cursor cursor = database.rawQuery(
                "SELECT cg_logs._id as cg_logs_id, type, author, log, date, found, friend, " + dbTableLogImages + "._id as cg_logImages_id, log_id, title, url FROM "
                        + dbTableLogs + " LEFT OUTER JOIN " + dbTableLogImages
                        + " ON ( cg_logs._id = log_id ) WHERE geocode = ?  ORDER BY date desc, cg_logs._id asc", new String[]{geocode});

        if (cursor != null && cursor.getCount() > 0) {
            LogEntry log = null;
            int indexLogsId = cursor.getColumnIndex("cg_logs_id");
            int indexType = cursor.getColumnIndex("type");
            int indexAuthor = cursor.getColumnIndex("author");
            int indexLog = cursor.getColumnIndex("log");
            int indexDate = cursor.getColumnIndex("date");
            int indexFound = cursor.getColumnIndex("found");
            int indexFriend = cursor.getColumnIndex("friend");
            int indexLogImagesId = cursor.getColumnIndex("cg_logImages_id");
            int indexTitle = cursor.getColumnIndex("title");
            int indexUrl = cursor.getColumnIndex("url");
            while (cursor.moveToNext() && logs.size() < 100) {
                if (log == null || log.id != cursor.getInt(indexLogsId)) {
                    log = new LogEntry(
                            cursor.getString(indexAuthor),
                            cursor.getLong(indexDate),
                            LogType.getById(cursor.getInt(indexType)),
                            cursor.getString(indexLog));
                    log.id = cursor.getInt(indexLogsId);
                    log.found = cursor.getInt(indexFound);
                    log.friend = cursor.getInt(indexFriend) == 1;
                    logs.add(log);
                }
                if (!cursor.isNull(indexLogImagesId)) {
                    String title = cursor.getString(indexTitle);
                    String url = cursor.getString(indexUrl);
                    log.addLogImage(new cgImage(url, title));
                }
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        return logs;
    }

    public Map<LogType, Integer> loadLogCounts(String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        init();

        Map<LogType, Integer> logCounts = new HashMap<LogType, Integer>();

        Cursor cursor = database.query(
                dbTableLogCount,
                new String[]{"type", "count"},
                "geocode = ?",
                new String[]{geocode},
                null,
                null,
                null,
                "100");

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            int indexType = cursor.getColumnIndex("type");
            int indexCount = cursor.getColumnIndex("count");

            do {
                LogType type = LogType.getById(cursor.getInt(indexType));
                Integer count = cursor.getInt(indexCount);

                logCounts.put(type, count);
            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }

        return logCounts;
    }

    private List<cgTrackable> loadInventory(String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        init();

        List<cgTrackable> trackables = new ArrayList<cgTrackable>();

        Cursor cursor = database.query(
                dbTableTrackables,
                new String[]{"_id", "updated", "tbcode", "guid", "title", "owner", "released", "goal", "description"},
                "geocode = ?",
                new String[]{geocode},
                null,
                null,
                "title COLLATE NOCASE ASC",
                "100");

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();

            do {
                cgTrackable trackable = createTrackableFromDatabaseContent(cursor);

                trackables.add(trackable);
            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }

        return trackables;
    }

    public cgTrackable loadTrackable(String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        init();

        cgTrackable trackable = new cgTrackable();

        Cursor cursor = database.query(
                dbTableTrackables,
                new String[]{"updated", "tbcode", "guid", "title", "owner", "released", "goal", "description"},
                "tbcode = ?",
                new String[]{geocode},
                null,
                null,
                null,
                "1");

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            trackable = createTrackableFromDatabaseContent(cursor);
        }

        if (cursor != null) {
            cursor.close();
        }

        return trackable;
    }

    private cgTrackable createTrackableFromDatabaseContent(Cursor cursor) {
        cgTrackable trackable = new cgTrackable();
        trackable.setGeocode(cursor.getString(cursor.getColumnIndex("tbcode")));
        trackable.setGuid(cursor.getString(cursor.getColumnIndex("guid")));
        trackable.setName(cursor.getString(cursor.getColumnIndex("title")));
        trackable.setOwner(cursor.getString(cursor.getColumnIndex("owner")));
        String released = cursor.getString(cursor.getColumnIndex("released"));
        if (released != null) {
            try {
                long releaseMilliSeconds = Long.parseLong(released);
                trackable.setReleased(new Date(releaseMilliSeconds));
            } catch (NumberFormatException e) {
                Log.e("createTrackableFromDatabaseContent", e);
            }
        }
        trackable.setGoal(cursor.getString(cursor.getColumnIndex("goal")));
        trackable.setDetails(cursor.getString(cursor.getColumnIndex("description")));
        trackable.setLogs(loadLogs(trackable.getGeocode()));
        return trackable;
    }

    /**
     * Number of caches stored. The number is shown on the starting activity of c:geo
     *
     * @param detailedOnly
     * @param cacheType
     * @param list
     * @return
     */
    public int getAllStoredCachesCount(final boolean detailedOnly, final CacheType cacheType, final int list) {
        if (cacheType == null) {
            throw new IllegalArgumentException("cacheType must not be null");
        }
        init();

        String listSql;
        String listSqlW;
        if (list == 0) {
            listSql = " where reason >= 1";
            listSqlW = " and reason >= 1";
        } else if (list >= 1) {
            listSql = " where reason = " + list;
            listSqlW = " and reason = " + list;
        } else {
            return 0;
        }

        int count = 0;
        try {
            String sql;
            if (!detailedOnly) {
                if (cacheType == CacheType.ALL) {
                    sql = "select count(_id) from " + dbTableCaches + listSql;
                } else {
                    sql = "select count(_id) from " + dbTableCaches + " where type = " + DatabaseUtils.sqlEscapeString(cacheType.id) + listSqlW;
                }
            } else {
                if (cacheType == CacheType.ALL) {
                    sql = "select count(_id) from " + dbTableCaches + " where detailed = 1" + listSqlW;
                } else {
                    sql = "select count(_id) from " + dbTableCaches + " where detailed = 1 and type = " + DatabaseUtils.sqlEscapeString(cacheType.id) + listSqlW;
                }
            }
            SQLiteStatement compiledStmnt = database.compileStatement(sql);
            count = (int) compiledStmnt.simpleQueryForLong();
            compiledStmnt.close();
        } catch (Exception e) {
            Log.e("cgData.loadAllStoredCachesCount: " + e.toString());
        }

        return count;
    }

    public int getAllHistoricCachesCount() {
        init();

        int count = 0;

        try {
            SQLiteStatement sqlCount = database.compileStatement("select count(_id) from " + dbTableCaches + " where visiteddate > 0");
            count = (int) sqlCount.simpleQueryForLong();
            sqlCount.close();
        } catch (Exception e) {
            Log.e("cgData.getAllHistoricCachesCount: " + e.toString());
        }

        return count;
    }

    /**
     * Return a batch of stored geocodes.
     *
     * @param detailedOnly
     * @param coords
     *            the current coordinates to sort by distance, or null to sort by geocode
     * @param cacheType
     * @param listId
     * @return
     */
    public Set<String> loadBatchOfStoredGeocodes(final boolean detailedOnly, final Geopoint coords, final CacheType cacheType, final int listId) {
        if (cacheType == null) {
            throw new IllegalArgumentException("cacheType must not be null");
        }
        init();

        Set<String> geocodes = new HashSet<String>();

        StringBuilder specifySql = new StringBuilder();

        specifySql.append("reason ");
        specifySql.append(listId != StoredList.ALL_LIST_ID ? "=" + Math.max(listId, 1) : ">= " + StoredList.STANDARD_LIST_ID);

        if (detailedOnly) {
            specifySql.append(" and detailed = 1 ");
        }

        if (cacheType != CacheType.ALL) {
            specifySql.append(" and type = ");
            specifySql.append(DatabaseUtils.sqlEscapeString(cacheType.id));
        }

        try {
            Cursor cursor;
            if (coords != null) {
                cursor = database.query(
                        dbTableCaches,
                        new String[]{"geocode", "(abs(latitude-" + String.format((Locale) null, "%.6f", coords.getLatitude()) +
                                ") + abs(longitude-" + String.format((Locale) null, "%.6f", coords.getLongitude()) + ")) as dif"},
                        specifySql.toString(),
                        null,
                        null,
                        null,
                        "dif",
                        null);
            } else {
                cursor = database.query(
                        dbTableCaches,
                        new String[]{"geocode"},
                        specifySql.toString(),
                        null,
                        null,
                        null,
                        "geocode");
            }

            if (cursor.moveToFirst()) {
                final int index = cursor.getColumnIndex("geocode");

                do {
                    geocodes.add(cursor.getString(index));
                } while (cursor.moveToNext());
            }

            cursor.close();

        } catch (Exception e) {
            Log.e("cgData.loadBatchOfStoredGeocodes: " + e.toString());
        }

        return geocodes;
    }

    public Set<String> loadBatchOfHistoricGeocodes(final boolean detailedOnly, final CacheType cacheType) {
        init();

        Set<String> geocodes = new HashSet<String>();

        StringBuilder specifySql = new StringBuilder();
        specifySql.append("visiteddate > 0");

        if (detailedOnly) {
            specifySql.append(" and detailed = 1");
        }
        if (cacheType != CacheType.ALL) {
            specifySql.append(" and type = ");
            specifySql.append(DatabaseUtils.sqlEscapeString(cacheType.id));
        }

        try {
            Cursor cursor = database.query(
                    dbTableCaches,
                    new String[]{"geocode"},
                    specifySql.toString(),
                    null,
                    null,
                    null,
                    "visiteddate",
                    null);

            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    int index = cursor.getColumnIndex("geocode");

                    do {
                        geocodes.add(cursor.getString(index));
                    } while (cursor.moveToNext());
                } else {
                    cursor.close();
                    return null;
                }

                cursor.close();
            }
        } catch (Exception e) {
            Log.e("cgData.loadBatchOfHistoricGeocodes: " + e.toString());
        }

        return geocodes;
    }

    /** Retrieve all stored caches from DB */
    public Set<String> loadCachedInViewport(final Viewport viewport, final CacheType cacheType) {
        return loadInViewport(false, viewport, cacheType);
    }

    /** Retrieve stored caches from DB with listId >= 1 */
    public Set<String> loadStoredInViewport(final Viewport viewport, final CacheType cacheType) {
        return loadInViewport(true, viewport, cacheType);
    }

    /**
     * Loads the geocodes of caches in a viewport from CacheCache and/or Database
     *
     * @param stored
     *            True - query only stored caches, False - query cached ones as well
     * @param centerLat
     * @param centerLon
     * @param spanLat
     * @param spanLon
     * @param cacheType
     * @return Set with geocodes
     */
    private Set<String> loadInViewport(final boolean stored, final Viewport viewport, final CacheType cacheType) {
        init();

        final Set<String> geocodes = new HashSet<String>();

        // if not stored only, get codes from CacheCache as well
        if (!stored) {
            geocodes.addAll(cacheCache.getInViewport(viewport, cacheType));
        }

        // viewport limitation
        final StringBuilder where = new StringBuilder(buildCoordinateWhere(dbTableCaches, viewport));

        // cacheType limitation
        if (cacheType != CacheType.ALL) {
            where.append(" and type = ");
            where.append(DatabaseUtils.sqlEscapeString(cacheType.id));
        }

        // offline caches only
        if (stored) {
            where.append(" and reason >= " + StoredList.STANDARD_LIST_ID);
        }

        try {
            final Cursor cursor = database.query(
                    dbTableCaches,
                    new String[]{"geocode"},
                    where.toString(),
                    null,
                    null,
                    null,
                    null,
                    "500");

            if (cursor.moveToFirst()) {
                final int index = cursor.getColumnIndex("geocode");

                do {
                    geocodes.add(cursor.getString(index));
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            Log.e("cgData.loadInViewport: " + e.toString());
        }

        return geocodes;
    }

    /** delete caches from the DB store 3 days or more before */
    public void clean() {
        clean(false);
    }

    /**
     * Remove caches with listId = 0
     *
     * @param more
     *            true = all caches false = caches stored 3 days or more before
     */
    public void clean(boolean more) {
        init();

        Log.d("Database clean: started");

        Cursor cursor;
        Set<String> geocodes = new HashSet<String>();

        try {
            if (more) {
                cursor = database.query(
                        dbTableCaches,
                        new String[]{"geocode"},
                        "reason = 0",
                        null,
                        null,
                        null,
                        null,
                        null);
            } else {
                long timestamp = System.currentTimeMillis() - DAYS_AFTER_CACHE_IS_DELETED;
                String timestampString = Long.toString(timestamp);
                cursor = database.query(
                        dbTableCaches,
                        new String[]{"geocode"},
                        "reason = 0 and detailed < ? and detailedupdate < ? and visiteddate < ?",
                        new String[]{timestampString, timestampString, timestampString},
                        null,
                        null,
                        null,
                        null);
            }

            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    final int index = cursor.getColumnIndex("geocode");

                    do {
                        geocodes.add(cursor.getString(index));
                    } while (cursor.moveToNext());
                }

                cursor.close();
            }

            final int size = geocodes.size();
            if (size > 0) {
                Log.d("Database clean: removing " + size + " geocaches from listId=0");

                removeCaches(geocodes, LoadFlags.REMOVE_ALL);
            }

            final SQLiteStatement countSql = database.compileStatement("select count(_id) from " + dbTableCaches + " where reason = 0");
            final int count = (int) countSql.simpleQueryForLong();
            countSql.close();
            Log.d("Database clean: " + count + " geocaches remaining for listId=0");

        } catch (Exception e) {
            Log.w("cgData.clean: " + e.toString());
        }

        Log.d("Database clean: finished");
    }

    public void removeAllFromCache() {
        // clean up CacheCache
        cacheCache.removeAllFromCache();
    }

    public void removeCache(final String geocode, EnumSet<LoadFlags.RemoveFlag> removeFlags) {
        Set<String> geocodes = new HashSet<String>();
        geocodes.add(geocode);
        removeCaches(geocodes, removeFlags);
    }

    /**
     * Drop caches from the tables they are stored into, as well as the cache files
     *
     * @param geocodes
     *            list of geocodes to drop from cache
     */
    public void removeCaches(final Set<String> geocodes, EnumSet<LoadFlags.RemoveFlag> removeFlags) {
        if (CollectionUtils.isEmpty(geocodes)) {
            return;
        }

        init();

        if (removeFlags.contains(RemoveFlag.REMOVE_CACHE)) {
            for (final String geocode : geocodes) {
                cacheCache.removeCacheFromCache(geocode);
            }
        }

        if (removeFlags.contains(RemoveFlag.REMOVE_DB)) {
            // Drop caches from the database
            final ArrayList<String> quotedGeocodes = new ArrayList<String>(geocodes.size());
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
                database.delete(dbTableLogs, baseWhereClause, null);
                database.delete(dbTableLogCount, baseWhereClause, null);
                database.delete(dbTableLogsOffline, baseWhereClause, null);
                database.delete(dbTableWaypoints, baseWhereClause + " and type <> 'own'", null);
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

    public boolean saveLogOffline(String geocode, Date date, LogType type, String log) {
        if (StringUtils.isBlank(geocode)) {
            Log.e("cgData.saveLogOffline: cannot log a blank geocode");
            return false;
        }
        if (LogType.UNKNOWN == type && StringUtils.isBlank(log)) {
            Log.e("cgData.saveLogOffline: cannot log an unknown log type and no message");
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

    public LogEntry loadLogOffline(String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        init();

        LogEntry log = null;

        Cursor cursor = database.query(
                dbTableLogsOffline,
                new String[]{"_id", "type", "log", "date"},
                "geocode = ?",
                new String[]{geocode},
                null,
                null,
                "_id desc",
                "1");

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();

            log = new LogEntry(
                    cursor.getLong(cursor.getColumnIndex("date")),
                    LogType.getById(cursor.getInt(cursor.getColumnIndex("type"))),
                    cursor.getString(cursor.getColumnIndex("log")));
            log.id = cursor.getInt(cursor.getColumnIndex("_id"));
        }

        if (cursor != null) {
            cursor.close();
        }

        return log;
    }

    public void clearLogOffline(String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return;
        }

        init();

        database.delete(dbTableLogsOffline, "geocode = ?", new String[]{geocode});
    }

    private SQLiteStatement getStatementLogCount() {
        return getStatement("LogCountFromGeocode", "SELECT count(_id) FROM " + dbTableLogsOffline + " WHERE geocode = ?");
    }

    private synchronized SQLiteStatement getStatement(final String key, final String query) {
        SQLiteStatement statement = statements.get(key);
        if (statement == null) {
            statement = database.compileStatement(query);
            statements.put(key, statement);
        }
        return statement;
    }

    private SQLiteStatement getStatementCountStandardList() {
        return getStatement("CountStandardList", "SELECT count(_id) FROM " + dbTableCaches + " WHERE reason = " + StoredList.STANDARD_LIST_ID);
    }

    private SQLiteStatement getStatementCountAllLists() {
        return getStatement("CountAllLists", "SELECT count(_id) FROM " + dbTableCaches + " WHERE reason >= " + StoredList.STANDARD_LIST_ID);
    }

    public boolean hasLogOffline(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return false;
        }

        init();
        try {
            final SQLiteStatement logCount = getStatementLogCount();
            synchronized (logCount) {
                logCount.bindString(1, geocode);
                return logCount.simpleQueryForLong() > 0;
            }
        } catch (Exception e) {
            Log.e("cgData.hasLogOffline", e);
        }

        return false;
    }

    public void setVisitDate(List<String> geocodes, long visitedDate) {
        if (geocodes.isEmpty()) {
            return;
        }

        init();

        database.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("visiteddate", visitedDate);

            for (String geocode : geocodes) {
                database.update(dbTableCaches, values, "geocode = ?", new String[]{geocode});
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public List<StoredList> getLists(Resources res) {
        init();

        List<StoredList> lists = new ArrayList<StoredList>();
        lists.add(new StoredList(StoredList.STANDARD_LIST_ID, res.getString(R.string.list_inbox), (int) getStatementCountStandardList().simpleQueryForLong()));

        try {
            String query = "SELECT l._id as _id, l.title as title, COUNT(c._id) as count" +
                    " FROM " + dbTableLists + " l LEFT OUTER JOIN " + dbTableCaches + " c" +
                    " ON l._id + " + customListIdOffset + " = c.reason" +
                    " GROUP BY l._id" +
                    " ORDER BY l.title COLLATE NOCASE ASC";

            Cursor cursor = database.rawQuery(query, null);
            ArrayList<StoredList> storedLists = getListsFromCursor(cursor);
            lists.addAll(storedLists);

        } catch (Exception e) {
            Log.e("cgData.readLists: " + e.toString());
        }
        return lists;
    }

    private static ArrayList<StoredList> getListsFromCursor(Cursor cursor) {
        ArrayList<StoredList> result = new ArrayList<StoredList>();
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                int indexId = cursor.getColumnIndex("_id");
                int indexTitle = cursor.getColumnIndex("title");
                int indexCount = cursor.getColumnIndex("count");
                do {
                    int count = 0;
                    if (indexCount >= 0) {
                        count = cursor.getInt(indexCount);
                    }
                    StoredList list = new StoredList(cursor.getInt(indexId) + customListIdOffset, cursor.getString(indexTitle), count);
                    result.add(list);
                } while (cursor.moveToNext());
            }

            cursor.close();
        }
        return result;
    }

    public StoredList getList(int id, Resources res) {
        init();
        if (id >= customListIdOffset) {
            Cursor cursor = database.query(
                    dbTableLists,
                    new String[]{"_id", "title"},
                    "_id = " + (id - customListIdOffset),
                    null,
                    null,
                    null,
                    null);
            ArrayList<StoredList> lists = getListsFromCursor(cursor);
            if (!lists.isEmpty()) {
                return lists.get(0);
            }
        }

        if (id == StoredList.ALL_LIST_ID) {
            return new StoredList(StoredList.ALL_LIST_ID, res.getString(R.string.list_all_lists), (int) getStatementCountAllLists().simpleQueryForLong());
        }

        // fall back to standard list in case of invalid list id
        if (id == StoredList.STANDARD_LIST_ID || id >= customListIdOffset) {
            return new StoredList(StoredList.STANDARD_LIST_ID, res.getString(R.string.list_inbox), (int) getStatementCountStandardList().simpleQueryForLong());
        }

        return null;
    }

    /**
     * Create a new list
     *
     * @param name
     *            Name
     * @return new listId
     */
    public int createList(String name) {
        int id = -1;
        if (StringUtils.isBlank(name)) {
            return id;
        }

        init();

        database.beginTransaction();
        try {
            ContentValues values = new ContentValues();
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
    public int renameList(final int listId, final String name) {
        if (StringUtils.isBlank(name) || StoredList.STANDARD_LIST_ID == listId) {
            return 0;
        }

        init();

        int count = 0;
        database.beginTransaction();
        try {
            ContentValues values = new ContentValues();
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
    public boolean removeList(int listId) {
        boolean status = false;
        if (listId < customListIdOffset) {
            return status;
        }

        init();

        database.beginTransaction();
        try {
            int cnt = database.delete(dbTableLists, "_id = " + (listId - customListIdOffset), null);

            if (cnt > 0) {
                // move caches from deleted list to standard list
                ContentValues values = new ContentValues();
                values.put("reason", StoredList.STANDARD_LIST_ID);
                database.update(dbTableCaches, values, "reason = " + listId, null);

                status = true;
            }

            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

        return status;
    }

    public void moveToList(final List<cgCache> caches, final int listId) {
        if (listId == StoredList.ALL_LIST_ID) {
            return;
        }
        if (caches.isEmpty()) {
            return;
        }
        init();

        final ContentValues values = new ContentValues();
        values.put("reason", listId);

        database.beginTransaction();
        try {
            for (cgCache cache : caches) {
                database.update(dbTableCaches, values, "geocode = ?", new String[]{cache.getGeocode()});
                cache.setListId(listId);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public synchronized boolean status() {
        return database != null;

    }

    public boolean removeSearchedDestination(Destination destination) {
        boolean success = true;
        if (destination == null) {
            success = false;
        } else {
            init();

            database.beginTransaction();
            try {
                database.delete(dbTableSearchDestionationHistory, "_id = " + destination.getId(), null);
                database.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e("Unable to remove searched destination", e);
                success = false;
            } finally {
                database.endTransaction();
            }
        }

        return success;
    }

    private SQLiteStatement getStatementDescription() {
        return getStatement("descriptionFromGeocode", "SELECT description FROM " + dbTableCaches + " WHERE geocode = ?");
    }

    private SQLiteStatement getStatementListIdFromGeocode() {
        return getStatement("listFromGeocode", "SELECT reason FROM " + dbTableCaches + " WHERE geocode = ?");
    }

    private SQLiteStatement getStatementListIdFromGuid() {
        return getStatement("listFromGeocode", "SELECT reason FROM " + dbTableCaches + " WHERE guid = ?");
    }

    private SQLiteStatement getStatementCacheId() {
        return getStatement("cacheIdFromGeocode", "SELECT cacheid FROM " + dbTableCaches + " WHERE geocode = ?");
    }

    private SQLiteStatement getStatementGeocode() {
        return getStatement("geocodeFromGuid", "SELECT geocode FROM " + dbTableCaches + " WHERE guid = ?");
    }

    public String getCacheDescription(String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }
        init();

        try {
            final SQLiteStatement description = getStatementDescription();
            synchronized (description) {
                description.bindString(1, geocode);
                return description.simpleQueryForString();
            }
        } catch (SQLiteDoneException e) {
            // Do nothing, it only means we have no information on the cache
        } catch (Exception e) {
            Log.e("cgData.getCacheDescription", e);
        }

        return null;
    }

    /**
     * checks if this is a newly created database
     *
     * @return
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

    private static String whereGeocodeIn(Set<String> geocodes) {
        final StringBuilder where = new StringBuilder();

        if (geocodes != null && geocodes.size() > 0) {
            StringBuilder all = new StringBuilder();
            for (String geocode : geocodes) {
                if (all.length() > 0) {
                    all.append(", ");
                }
                all.append(DatabaseUtils.sqlEscapeString(geocode));
            }

            where.append("geocode in (").append(all).append(')');
        }

        return where.toString();
    }

    /**
     * Loads all Waypoints in the coordinate rectangle.
     *
     * @param excludeDisabled
     * @param excludeMine
     * @param type
     * @return
     */

    public Set<cgWaypoint> loadWaypoints(final Viewport viewport, boolean excludeMine, boolean excludeDisabled, CacheType type) {
        final StringBuilder where = new StringBuilder(buildCoordinateWhere(dbTableWaypoints, viewport));
        if (excludeMine) {
            where.append(" and ").append(dbTableCaches).append(".own == 0 and ").append(dbTableCaches).append(".found == 0");
        }
        if (excludeDisabled) {
            where.append(" and ").append(dbTableCaches).append(".disabled == 0");
        }
        if (type != CacheType.ALL) {
            where.append(" and ").append(dbTableCaches).append(".type == '").append(type.id).append("'");
        }
        init();

        final StringBuilder query = new StringBuilder("SELECT ");
        for (int i = 0; i < WAYPOINT_COLUMNS.length; i++) {
            query.append(i > 0 ? ", " : "").append(dbTableWaypoints).append('.').append(WAYPOINT_COLUMNS[i]).append(' ');
        }
        query.append(" FROM ").append(dbTableWaypoints).append(", ").append(dbTableCaches).append(" WHERE ").append(dbTableWaypoints).append(".geocode == ").append(dbTableCaches).append(".geocode and ").append(where);

        final Cursor cursor = database.rawQuery(query.toString(), null);
        try {
            if (!cursor.moveToFirst()) {
                return Collections.emptySet();
            }

            final Set<cgWaypoint> waypoints = new HashSet<cgWaypoint>();
            do {
                waypoints.add(createWaypointFromDatabaseContent(cursor));
            } while (cursor.moveToNext());
            return waypoints;
        } finally {
            cursor.close();
        }
    }

}
