package cgeo.geocaching;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.geopoint.Geopoint;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

public class cgData {

    /** The list of fields needed for mapping. */
    private static final String[] CACHE_COLUMNS = new String[] {
            "_id", "updated", "reason", "detailed", "detailedupdate", "visiteddate", "geocode", "cacheid", "guid", "type", "name", "own", "owner", "owner_real", "hidden", "hint", "size",
            "difficulty", "distance", "direction", "terrain", "latlon", "location", "latitude", "longitude", "elevation", "shortdesc",
            "favourite_cnt", "rating", "votes", "myvote", "disabled", "archived", "members", "found", "favourite", "inventorycoins", "inventorytags",
            "inventoryunknown", "onWatchlist", "personal_note", "reliable_latlon", "coordsChanged"
            // reason is replaced by listId in cgCache
    };
    /**
     * holds the column indexes of the cache table to avoid lookups
     */
    private static int[] cacheColumnIndex;
    private Context context = null;
    private String path = null;
    private cgDbHelper dbHelper = null;
    private SQLiteDatabase databaseRO = null;
    private SQLiteDatabase databaseRW = null;
    private static final int dbVersion = 61;
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
            + "coordsChanged integer default 0"
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
            + "note text "
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

    private boolean initialized = false;
    private SQLiteStatement statementDescription;
    private SQLiteStatement statementLogCount;
    private SQLiteStatement statementStandardList;
    private static boolean newlyCreatedDatabase = false;

    public cgData(Context contextIn) {
        context = contextIn;
    }

    public synchronized void init() {
        if (initialized) {
            return;
        }

        if (databaseRW == null || !databaseRW.isOpen()) {
            try {
                if (dbHelper == null) {
                    dbHelper = new cgDbHelper(context);
                }
                databaseRW = dbHelper.getWritableDatabase();

                if (databaseRW != null && databaseRW.isOpen()) {
                    Log.i(Settings.tag, "Connection to RW database established.");
                } else {
                    Log.e(Settings.tag, "Failed to open connection to RW database.");
                }

                if (databaseRW != null && databaseRW.inTransaction()) {
                    databaseRW.endTransaction();
                }
            } catch (Exception e) {
                Log.e(Settings.tag, "cgData.openDb.RW: " + e.toString());
            }
        }

        if (databaseRO == null || !databaseRO.isOpen()) {
            try {
                if (dbHelper == null) {
                    dbHelper = new cgDbHelper(context);
                }
                databaseRO = dbHelper.getReadableDatabase();

                if (databaseRO.needUpgrade(dbVersion)) {
                    databaseRO = null;
                }

                if (databaseRO != null && databaseRO.isOpen()) {
                    Log.i(Settings.tag, "Connection to RO database established.");
                } else {
                    Log.e(Settings.tag, "Failed to open connection to RO database.");
                }

                if (databaseRO != null && databaseRO.inTransaction()) {
                    databaseRO.endTransaction();
                }
            } catch (Exception e) {
                Log.e(Settings.tag, "cgData.openDb.RO: " + e.toString());
            }
        }

        initialized = true;
    }

    public void closeDb() {
        initialized = false;
        closePreparedStatements();

        if (databaseRO != null) {
            path = databaseRO.getPath();

            if (databaseRO.inTransaction()) {
                databaseRO.endTransaction();
            }

            databaseRO.close();
            databaseRO = null;
            SQLiteDatabase.releaseMemory();

            Log.d(Settings.tag, "Closing RO database");
        }

        if (databaseRW != null) {
            path = databaseRW.getPath();

            if (databaseRW.inTransaction()) {
                databaseRW.endTransaction();
            }

            databaseRW.close();
            databaseRW = null;
            SQLiteDatabase.releaseMemory();

            Log.d(Settings.tag, "Closing RW database");
        }

        if (dbHelper != null) {
            dbHelper.close();
            dbHelper = null;
        }
    }

    private void closePreparedStatements() {
        if (statementDescription != null) {
            statementDescription.close();
            statementDescription = null;
        }
        if (statementLogCount != null) {
            statementLogCount.close();
            statementLogCount = null;
        }
        if (statementStandardList != null) {
            statementStandardList.close();
            statementStandardList = null;
        }
    }

    private static File backupFile() {
        return new File(LocalStorage.getStorage(), "cgeo.sqlite");
    }

    public String backupDatabase() {
        if (!LocalStorage.isExternalStorageAvailable()) {
            Log.w(Settings.tag, "Database wasn't backed up: no external memory");
            return null;
        }

        final File target = backupFile();
        closeDb();
        final boolean backupDone = LocalStorage.copy(new File(path), target);
        init();

        if (backupDone) {
            Log.i(Settings.tag, "Database was copied to " + target);
            return target.getPath();
        } else {
            Log.e(Settings.tag, "Database could not be copied to " + target);
            return null;
        }
    }

    public static File isRestoreFile() {
        final File fileSourceFile = backupFile();
        if (fileSourceFile.exists()) {
            return fileSourceFile;
        } else {
            return null;
        }
    }

    public boolean restoreDatabase() {
        if (!LocalStorage.isExternalStorageAvailable()) {
            Log.w(Settings.tag, "Database wasn't restored: no external memory");
            return false;
        }

        final File sourceFile = backupFile();
        closeDb();
        final boolean restoreDone = LocalStorage.copy(sourceFile, new File(path));
        init();

        if (restoreDone) {
            Log.i(Settings.tag, "Database succesfully restored from " + sourceFile.getPath());
        } else {
            Log.e(Settings.tag, "Could not restore database from " + sourceFile.getPath());
        }

        return restoreDone;
    }

    private static class cgDbHelper extends SQLiteOpenHelper {

        cgDbHelper(Context context) {
            super(context, dbName, null, dbVersion);
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
            Log.i(Settings.tag, "Upgrade database from ver. " + oldVersion + " to ver. " + newVersion + ": start");

            try {
                if (db.isReadOnly()) {
                    return;
                }

                db.beginTransaction();

                if (oldVersion <= 0) { // new table
                    dropDatabase(db);
                    onCreate(db);

                    Log.i(Settings.tag, "Database structure created.");
                }

                if (oldVersion > 0) {
                    db.execSQL("delete from " + dbTableCaches + " where reason = 0");

                    if (oldVersion < 34) { // upgrade to 34
                        try {
                            db.execSQL("create index if not exists in_a on " + dbTableCaches + " (geocode)");
                            db.execSQL("create index if not exists in_b on " + dbTableCaches + " (guid)");
                            db.execSQL("create index if not exists in_c on " + dbTableCaches + " (reason)");
                            db.execSQL("create index if not exists in_d on " + dbTableCaches + " (detailed)");
                            db.execSQL("create index if not exists in_e on " + dbTableCaches + " (type)");
                            db.execSQL("create index if not exists in_a on " + dbTableAttributes + " (geocode)");
                            db.execSQL("create index if not exists in_a on " + dbTableWaypoints + " (geocode)");
                            db.execSQL("create index if not exists in_b on " + dbTableWaypoints + " (geocode, type)");
                            db.execSQL("create index if not exists in_a on " + dbTableSpoilers + " (geocode)");
                            db.execSQL("create index if not exists in_a on " + dbTableLogs + " (geocode)");
                            db.execSQL("create index if not exists in_a on " + dbTableTrackables + " (geocode)");

                            Log.i(Settings.tag, "Indexes added.");
                        } catch (Exception e) {
                            Log.e(Settings.tag, "Failed to upgrade to ver. 34: " + e.toString());
                        }
                    }

                    if (oldVersion < 37) { // upgrade to 37
                        try {
                            db.execSQL("alter table " + dbTableCaches + " add column direction text");
                            db.execSQL("alter table " + dbTableCaches + " add column distance double");

                            Log.i(Settings.tag, "Columns direction and distance added to " + dbTableCaches + ".");
                        } catch (Exception e) {
                            Log.e(Settings.tag, "Failed to upgrade to ver. 37: " + e.toString());
                        }
                    }

                    if (oldVersion < 38) { // upgrade to 38
                        try {
                            db.execSQL("drop table " + dbTableLogs);
                            db.execSQL(dbCreateLogs);

                            Log.i(Settings.tag, "Changed type column in " + dbTableLogs + " to integer.");
                        } catch (Exception e) {
                            Log.e(Settings.tag, "Failed to upgrade to ver. 38: " + e.toString());
                        }
                    }

                    if (oldVersion < 39) { // upgrade to 39
                        try {
                            db.execSQL(dbCreateLists);

                            Log.i(Settings.tag, "Created lists table.");
                        } catch (Exception e) {
                            Log.e(Settings.tag, "Failed to upgrade to ver. 39: " + e.toString());
                        }
                    }

                    if (oldVersion < 40) { // upgrade to 40
                        try {
                            db.execSQL("drop table " + dbTableTrackables);
                            db.execSQL(dbCreateTrackables);

                            Log.i(Settings.tag, "Changed type of geocode column in trackables table.");
                        } catch (Exception e) {
                            Log.e(Settings.tag, "Failed to upgrade to ver. 40: " + e.toString());
                        }
                    }

                    if (oldVersion < 41) { // upgrade to 41
                        try {
                            db.execSQL("alter table " + dbTableCaches + " add column rating float");
                            db.execSQL("alter table " + dbTableCaches + " add column votes integer");
                            db.execSQL("alter table " + dbTableCaches + " add column vote integer");

                            Log.i(Settings.tag, "Added columns for GCvote.");
                        } catch (Exception e) {
                            Log.e(Settings.tag, "Failed to upgrade to ver. 41: " + e.toString());
                        }
                    }

                    if (oldVersion < 42) { // upgrade to 42
                        try {
                            db.execSQL(dbCreateLogsOffline);

                            Log.i(Settings.tag, "Added table for offline logs");
                        } catch (Exception e) {
                            Log.e(Settings.tag, "Failed to upgrade to ver. 42: " + e.toString());
                        }
                    }

                    if (oldVersion < 43) { // upgrade to 43
                        try {
                            final String dbTableCachesTemp = dbTableCaches + "_temp";
                            final String dbCreateCachesTemp = ""
                                    + "create temporary table " + dbTableCachesTemp + " ("
                                    + "_id integer primary key autoincrement, "
                                    + "updated long not null, "
                                    + "detailed integer not null default 0, "
                                    + "detailedupdate long, "
                                    + "geocode text unique not null, "
                                    + "reason integer not null default 0, " // cached, favourite...
                                    + "cacheid text, "
                                    + "guid text, "
                                    + "type text, "
                                    + "name text, "
                                    + "owner text, "
                                    + "hidden long, "
                                    + "hint text, "
                                    + "size text, "
                                    + "difficulty float, "
                                    + "terrain float, "
                                    + "latlon text, "
                                    + "latitude_string text, "
                                    + "longitude_string text, "
                                    + "location text, "
                                    + "distance double, "
                                    + "latitude double, "
                                    + "longitude double, "
                                    + "shortdesc text, "
                                    + "description text, "
                                    + "rating float, "
                                    + "votes integer, "
                                    + "vote integer, "
                                    + "disabled integer not null default 0, "
                                    + "archived integer not null default 0, "
                                    + "members integer not null default 0, "
                                    + "found integer not null default 0, "
                                    + "favourite integer not null default 0, "
                                    + "inventorycoins integer default 0, "
                                    + "inventorytags integer default 0, "
                                    + "inventoryunknown integer default 0 "
                                    + "); ";
                            final String dbCreateCachesNew = ""
                                    + "create table " + dbTableCaches + " ("
                                    + "_id integer primary key autoincrement, "
                                    + "updated long not null, "
                                    + "detailed integer not null default 0, "
                                    + "detailedupdate long, "
                                    + "geocode text unique not null, "
                                    + "reason integer not null default 0, " // cached, favourite...
                                    + "cacheid text, "
                                    + "guid text, "
                                    + "type text, "
                                    + "name text, "
                                    + "owner text, "
                                    + "hidden long, "
                                    + "hint text, "
                                    + "size text, "
                                    + "difficulty float, "
                                    + "terrain float, "
                                    + "latlon text, "
                                    + "latitude_string text, "
                                    + "longitude_string text, "
                                    + "location text, "
                                    + "direction double, "
                                    + "distance double, "
                                    + "latitude double, "
                                    + "longitude double, "
                                    + "shortdesc text, "
                                    + "description text, "
                                    + "rating float, "
                                    + "votes integer, "
                                    + "vote integer, "
                                    + "disabled integer not null default 0, "
                                    + "archived integer not null default 0, "
                                    + "members integer not null default 0, "
                                    + "found integer not null default 0, "
                                    + "favourite integer not null default 0, "
                                    + "inventorycoins integer default 0, "
                                    + "inventorytags integer default 0, "
                                    + "inventoryunknown integer default 0 "
                                    + "); ";

                            db.beginTransaction();
                            db.execSQL(dbCreateCachesTemp);
                            db.execSQL("insert into " + dbTableCachesTemp + " select _id, updated, detailed, detailedupdate, geocode, reason, cacheid, guid, type, name, owner, hidden, hint, size, difficulty, terrain, latlon, latitude_string, longitude_string, location, distance, latitude, longitude, shortdesc, description, rating, votes, vote, disabled, archived, members, found, favourite, inventorycoins, inventorytags, inventoryunknown from " + dbTableCaches);
                            db.execSQL("drop table " + dbTableCaches);
                            db.execSQL(dbCreateCachesNew);
                            db.execSQL("insert into " + dbTableCaches + " select _id, updated, detailed, detailedupdate, geocode, reason, cacheid, guid, type, name, owner, hidden, hint, size, difficulty, terrain, latlon, latitude_string, longitude_string, location, null, distance, latitude, longitude, shortdesc, description, rating, votes, vote, disabled, archived, members, found, favourite, inventorycoins, inventorytags, inventoryunknown from " + dbTableCachesTemp);
                            db.execSQL("drop table " + dbTableCachesTemp);
                            db.setTransactionSuccessful();

                            Log.i(Settings.tag, "Changed direction column");
                        } catch (Exception e) {
                            Log.e(Settings.tag, "Failed to upgrade to ver. 43: " + e.toString());
                        } finally {
                            db.endTransaction();
                        }
                    }

                    if (oldVersion < 44) { // upgrade to 44
                        try {
                            db.execSQL("alter table " + dbTableCaches + " add column favourite_cnt integer");

                            Log.i(Settings.tag, "Column favourite_cnt added to " + dbTableCaches + ".");
                        } catch (Exception e) {
                            Log.e(Settings.tag, "Failed to upgrade to ver. 44: " + e.toString());
                        }
                    }

                    if (oldVersion < 45) { // upgrade to 45
                        try {
                            db.execSQL("alter table " + dbTableCaches + " add column owner_real text");

                            Log.i(Settings.tag, "Column owner_real added to " + dbTableCaches + ".");
                        } catch (Exception e) {
                            Log.e(Settings.tag, "Failed to upgrade to ver. 45: " + e.toString());
                        }
                    }

                    if (oldVersion < 46) { // upgrade to 46
                        try {
                            db.execSQL("alter table " + dbTableCaches + " add column visiteddate long");
                            db.execSQL("create index if not exists in_f on " + dbTableCaches + " (visiteddate, detailedupdate)");

                            Log.i(Settings.tag, "Added column for date of visit.");
                        } catch (Exception e) {
                            Log.e(Settings.tag, "Failed to upgrade to ver. 46: " + e.toString());
                        }
                    }
                    if (oldVersion < 47) { // upgrade to 47
                        try {
                            db.execSQL("alter table " + dbTableCaches + " add column own integer not null default 0");

                            Log.i(Settings.tag, "Added column own.");
                        } catch (Exception e) {
                            Log.e(Settings.tag, "Failed to upgrade to ver. 47: " + e.toString());
                        }
                    }

                    if (oldVersion < 48) { // upgrade to 48
                        try {
                            db.execSQL("alter table " + dbTableCaches + " add column elevation double");

                            Log.i(Settings.tag, "Column elevation added to " + dbTableCaches + ".");
                        } catch (Exception e) {
                            Log.e(Settings.tag, "Failed to upgrade to ver. 48: " + e.toString());
                        }
                    }

                    if (oldVersion < 49) { // upgrade to 49
                        try {
                            db.execSQL(dbCreateLogCount);

                            Log.i(Settings.tag, "Created table " + dbTableLogCount + ".");
                        } catch (Exception e) {
                            Log.e(Settings.tag, "Failed to upgrade to ver. 49: " + e.toString());
                        }
                    }

                    if (oldVersion < 50) { // upgrade to 50
                        try {
                            db.execSQL("alter table " + dbTableCaches + " add column myvote float");

                            Log.i(Settings.tag, "Added float column for votes to " + dbTableCaches + ".");
                        } catch (Exception e) {
                            Log.e(Settings.tag, "Failed to upgrade to ver. 50: " + e.toString());
                        }
                    }

                    if (oldVersion < 51) { // upgrade to 51
                        try {
                            db.execSQL("alter table " + dbTableCaches + " add column reliable_latlon integer");

                            Log.i(Settings.tag, "Column reliable_latlon added to " + dbTableCaches + ".");
                        } catch (Exception e) {
                            Log.e(Settings.tag, "Failed to upgrade to ver. 51: " + e.toString());
                        }
                    }

                    if (oldVersion < 52) { // upgrade to 52
                        try {
                            db.execSQL(dbCreateSearchDestinationHistory);

                            Log.i(Settings.tag, "Added table " + dbTableSearchDestionationHistory + ".");
                        } catch (Exception e) {
                            Log.e(Settings.tag, "Failed to upgrade to ver. 52", e);
                        }
                    }

                    if (oldVersion < 53) { // upgrade to 53
                        try {
                            db.execSQL("alter table " + dbTableCaches + " add column onWatchlist integer");

                            Log.i(Settings.tag, "Column onWatchlist added to " + dbTableCaches + ".");
                        } catch (Exception e) {
                            Log.e(Settings.tag, "Failed to upgrade to ver. 53", e);
                        }
                    }

                    if (oldVersion < 54) { // update to 54
                        try {
                            db.execSQL(dbCreateLogImages);
                        } catch (Exception e) {
                            Log.e(Settings.tag, "Failed to upgrade to ver. 54: " + e.toString());

                        }
                    }

                    if (oldVersion < 55) { // update to 55
                        try {
                            db.execSQL("alter table " + dbTableCaches + " add column personal_note text");
                        } catch (Exception e) {
                            Log.e(Settings.tag, "Failed to upgrade to ver. 55: " + e.toString());
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
                            Log.e(Settings.tag, "Failed to upgrade to ver. 56: " + e.toString());
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
                            Log.e(Settings.tag, "Failed to upgrade to ver. 57: " + e.toString());
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

                            Log.i(Settings.tag, "Removed latitude_string and longitude_string columns");
                        } catch (Exception e) {
                            Log.e(Settings.tag, "Failed to upgrade to ver. 58", e);
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
                            Log.e(Settings.tag, "Failed to upgrade to ver. 59", e);
                        }
                    }

                    if (oldVersion < 60) {
                        try {
                            removeSecEmptyDirs();
                        } catch (Exception e) {
                            Log.e(Settings.tag, "Failed to upgrade to ver. 60", e);
                        }
                    }
                    if (oldVersion < 61) {
                        try {
                            db.execSQL("alter table " + dbTableLogs + " add column friend integer");
                            db.execSQL("alter table " + dbTableCaches + " add column coordsChanged integer default 0");
                        } catch (Exception e) {
                            Log.e(Settings.tag, "Failed to upgrade to ver. 61: " + e.toString());

                        }
                    }

                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            Log.i(Settings.tag, "Upgrade database from ver. " + oldVersion + " to ver. " + newVersion + ": completed");
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
                    Log.i(Settings.tag, "Removing obsolete cache directory for " + dir.getName());
                    cgBase.deleteDirectory(dir);
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
            long timestamp = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000);
            cursor = databaseRO.query(
                    dbTableCaches,
                    new String[] { "geocode" },
                    "(detailed = 1 and detailedupdate > ?) or reason > 0",
                    new String[] { Long.toString(timestamp) },
                    null,
                    null,
                    "detailedupdate desc",
                    "100");

            if (cursor != null) {
                int index = 0;

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
            Log.e(Settings.tag, "cgData.allDetailedThere: " + e.toString());
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
                cursor = databaseRO.query(
                        dbTableCaches,
                        new String[] { "detailed", "detailedupdate", "updated" },
                        "geocode = ?",
                        new String[] { geocode },
                        null,
                        null,
                        null,
                        "1");
            } else if (StringUtils.isNotBlank(guid)) {
                cursor = databaseRO.query(
                        dbTableCaches,
                        new String[] { "detailed", "detailedupdate", "updated" },
                        "guid = ?",
                        new String[] { guid },
                        null,
                        null,
                        null,
                        "1");
            } else {
                return false;
            }

            if (cursor != null) {
                int index = 0;
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
            Log.e(Settings.tag, "cgData.isThere: " + e.toString());
        }

        if (cursor != null) {
            cursor.close();
        }

        if (cnt > 0) {
            if (detailed && dataDetailed == 0) {
                // we want details, but these are not stored
                return false;
            }

            if (checkTime && detailed && dataDetailedUpdate < (System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000))) {
                // we want to check time for detailed cache, but data are older than 3 hours
                return false;
            }

            if (checkTime && !detailed && dataUpdated < (System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000))) {
                // we want to check time for short cache, but data are older than 3 hours
                return false;
            }

            // we have some cache
            return true;
        }

        // we have no such cache stored in cache
        return false;
    }

    public boolean isOffline(String geocode, String guid) {
        init();

        Cursor cursor = null;
        long reason = 0;

        try {
            if (StringUtils.isNotBlank(geocode)) {
                cursor = databaseRO.query(
                        dbTableCaches,
                        new String[] { "reason" },
                        "geocode = ?",
                        new String[] { geocode },
                        null,
                        null,
                        null,
                        "1");
            } else if (StringUtils.isNotBlank(guid)) {
                cursor = databaseRO.query(
                        dbTableCaches,
                        new String[] { "reason" },
                        "guid = ? ",
                        new String[] { guid },
                        null,
                        null,
                        null,
                        "1");
            } else {
                return false;
            }

            if (cursor != null) {
                final int cnt = cursor.getCount();
                int index = 0;

                if (cnt > 0) {
                    cursor.moveToFirst();

                    index = cursor.getColumnIndex("reason");
                    reason = cursor.getLong(index);
                }

                cursor.close();
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "cgData.isOffline: " + e.toString());
        }

        return reason >= 1;
    }

    public String getGeocodeForGuid(String guid) {
        if (StringUtils.isBlank(guid)) {
            return null;
        }

        init();

        Cursor cursor = null;
        String geocode = null;

        try {
            cursor = databaseRO.query(
                    dbTableCaches,
                    new String[] { "geocode" },
                    "guid = ?",
                    new String[] { guid },
                    null,
                    null,
                    null,
                    "1");

            if (cursor != null) {
                int index = 0;

                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();

                    index = cursor.getColumnIndex("geocode");
                    geocode = cursor.getString(index);
                }
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "cgData.getGeocodeForGuid: " + e.toString());
        }

        if (cursor != null) {
            cursor.close();
        }

        return geocode;
    }

    public String getCacheidForGeocode(String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        init();

        Cursor cursor = null;
        String cacheid = null;

        try {
            cursor = databaseRO.query(
                    dbTableCaches,
                    new String[] { "cacheid" },
                    "geocode = ?",
                    new String[] { geocode },
                    null,
                    null,
                    null,
                    "1");

            if (cursor != null) {
                int index = 0;

                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();

                    index = cursor.getColumnIndex("cacheid");
                    cacheid = cursor.getString(index);
                }
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "cgData.getCacheidForGeocode: " + e.toString());
        }

        if (cursor != null) {
            cursor.close();
        }

        return cacheid;
    }

    public boolean saveCache(cgCache cache) {
        //LeeB - writing to the DB is slow
        if (cache == null) {
            return false;
        }

        ContentValues values = new ContentValues();

        if (cache.getUpdated() == 0) {
            values.put("updated", System.currentTimeMillis());
        } else {
            values.put("updated", cache.getUpdated());
        }
        values.put("reason", cache.getListId());
        values.put("detailed", cache.getDetailed() ? 1 : 0);
        values.put("detailedupdate", cache.getDetailedUpdate());
        values.put("visiteddate", cache.getVisitedDate());
        values.put("geocode", cache.getGeocode());
        values.put("cacheid", cache.getCacheId());
        values.put("guid", cache.getGuid());
        values.put("type", cache.getType().id);
        values.put("name", cache.getName());
        values.put("own", cache.isOwn() ? 1 : 0);
        values.put("owner", cache.getOwner());
        values.put("owner_real", cache.getOwnerReal());
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
        values.put("members", cache.isMembers() ? 1 : 0);
        values.put("found", cache.isFound() ? 1 : 0);
        values.put("favourite", cache.isFavourite() ? 1 : 0);
        values.put("inventoryunknown", cache.getInventoryItems());
        values.put("onWatchlist", cache.isOnWatchlist() ? 1 : 0);
        // values.put("coordsChanged", cache.coordsChanged() ? 1 : 0);

        boolean statusOk = true;

        if (cache.getAttributes() != null) {
            if (!saveAttributes(cache.getGeocode(), cache.getAttributes())) {
                statusOk = false;
            }
        }

        if (cache.getWaypoints() != null) {
            if (!saveWaypoints(cache.getGeocode(), cache.getWaypoints(), true)) {
                statusOk = false;
            }
        }

        if (cache.getSpoilers() != null) {
            if (!saveSpoilers(cache.getGeocode(), cache.getSpoilers())) {
                statusOk = false;
            }
        }

        if (cache.getLogs() != null) {
            if (!saveLogs(cache.getGeocode(), cache.getLogs())) {
                statusOk = false;
            }
        }

        if (MapUtils.isNotEmpty(cache.getLogCounts())) {
            if (!saveLogCount(cache.getGeocode(), cache.getLogCounts())) {
                statusOk = false;
            }
        }

        if (cache.getInventory() != null) {
            if (!saveInventory(cache.getGeocode(), cache.getInventory())) {
                statusOk = false;
            }
        }

        if (!statusOk) {
            cache.setDetailed(false);
            cache.setDetailedUpdate(0L);
        }

        init();

        //try to update record else insert fresh..
        try {
            int rows = databaseRW.update(dbTableCaches, values, "geocode = ?", new String[] { cache.getGeocode() });
            if (rows > 0) {
                values = null;
                return true;
            }
        } catch (Exception e) {
            // nothing
        }

        try {
            long id = databaseRW.insert(dbTableCaches, null, values);
            if (id > 0) {
                values = null;
                return true;
            }
        } catch (Exception e) {
            // nothing
        }

        values = null;

        return false;
    }

    public boolean saveAttributes(String geocode, List<String> attributes) {
        init();

        if (StringUtils.isBlank(geocode) || attributes == null) {
            return false;
        }

        databaseRW.beginTransaction();
        try {
            databaseRW.delete(dbTableAttributes, "geocode = ?", new String[] { geocode });

            if (!attributes.isEmpty()) {

                InsertHelper helper = new InsertHelper(databaseRW, dbTableAttributes);
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
            databaseRW.setTransactionSuccessful();
        } finally {
            databaseRW.endTransaction();
        }

        return true;
    }

    /**
     * Persists the given <code>destination</code> into the database.
     *
     * @param destinations
     * @return <code>true</code> if the given destination was successfully
     *         persisted <code>false</code> otherwise.
     */
    public boolean saveSearchedDestination(cgDestination destination) {
        boolean success = true;

        if (destination == null) {
            success = false;
        } else {
            init();

            databaseRW.beginTransaction();

            try {
                ContentValues values = new ContentValues();
                values.put("date", destination.getDate());
                putCoords(values, destination.getCoords());

                long id = databaseRW.insert(dbTableSearchDestionationHistory, null, values);
                destination.setId(id);
                databaseRW.setTransactionSuccessful();
            } catch (Exception e) {
                success = false;
                Log.e(Settings.tag, "Updating searchedDestinations db failed", e);
            } finally {
                databaseRW.endTransaction();
            }
        }

        return success;
    }

    public boolean saveWaypoints(String geocode, List<cgWaypoint> waypoints, boolean drop) {
        init();

        if (StringUtils.isBlank(geocode) || waypoints == null) {
            return false;
        }

        boolean ok = false;
        databaseRW.beginTransaction();
        try {
            if (drop) {
                databaseRW.delete(dbTableWaypoints, "geocode = ? and type <> ?", new String[] { geocode, "own" });
            }

            if (!waypoints.isEmpty()) {
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

                    databaseRW.insert(dbTableWaypoints, null, values);
                }
            }

            databaseRW.setTransactionSuccessful();
            ok = true;
        } finally {
            databaseRW.endTransaction();
        }

        return ok;
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

    public boolean saveOwnWaypoint(int id, String geocode, cgWaypoint waypoint) {
        init();

        if ((StringUtils.isBlank(geocode) && id <= 0) || waypoint == null) {
            return false;
        }

        boolean ok = false;
        databaseRW.beginTransaction();
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

            if (id <= 0) {
                databaseRW.insert(dbTableWaypoints, null, values);
                ok = true;
            } else {
                final int rows = databaseRW.update(dbTableWaypoints, values, "_id = " + id, null);
                if (rows > 0) {
                    ok = true;
                } else {
                    ok = false;
                }
            }
            databaseRW.setTransactionSuccessful();
        } finally {
            databaseRW.endTransaction();
        }

        return ok;
    }

    public boolean deleteWaypoint(int id) {
        init();

        if (id == 0) {
            return false;
        }

        int deleted = databaseRW.delete(dbTableWaypoints, "_id = " + id, null);

        if (deleted > 0) {
            return true;
        }

        return false;
    }

    public boolean saveSpoilers(String geocode, List<cgImage> spoilers) {
        init();

        if (StringUtils.isBlank(geocode) || spoilers == null) {
            return false;
        }

        databaseRW.beginTransaction();
        try {
            databaseRW.delete(dbTableSpoilers, "geocode = ?", new String[] { geocode });

            if (!spoilers.isEmpty()) {
                ContentValues values = new ContentValues();
                long timeStamp = System.currentTimeMillis();
                for (cgImage oneSpoiler : spoilers) {
                    values.clear();
                    values.put("geocode", geocode);
                    values.put("updated", timeStamp);
                    values.put("url", oneSpoiler.getUrl());
                    values.put("title", oneSpoiler.getTitle());
                    values.put("description", oneSpoiler.getDescription());

                    databaseRW.insert(dbTableSpoilers, null, values);
                }
            }
            databaseRW.setTransactionSuccessful();
        } finally {
            databaseRW.endTransaction();
        }

        return true;
    }

    public boolean saveLogs(String geocode, List<cgLog> logs) {
        return saveLogs(geocode, logs, true);
    }

    public boolean saveLogs(String geocode, List<cgLog> logs, boolean drop) {
        init();

        if (StringUtils.isBlank(geocode) || logs == null) {
            return false;
        }

        databaseRW.beginTransaction();
        try {
            if (drop) {
                // TODO delete logimages referring these logs
                databaseRW.delete(dbTableLogs, "geocode = ?", new String[] { geocode });
            }

            if (!logs.isEmpty()) {
                InsertHelper helper = new InsertHelper(databaseRW, dbTableLogs);
                long timeStamp = System.currentTimeMillis();
                for (cgLog log : logs) {
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

                    if (CollectionUtils.isNotEmpty(log.logImages)) {
                        ContentValues values = new ContentValues();
                        for (cgImage img : log.logImages) {
                            values.clear();
                            values.put("log_id", log_id);
                            values.put("title", img.getTitle());
                            values.put("url", img.getUrl());
                            databaseRW.insert(dbTableLogImages, null, values);
                        }
                    }
                }
                helper.close();
            }
            databaseRW.setTransactionSuccessful();
        } finally {
            databaseRW.endTransaction();
        }

        return true;
    }

    public boolean saveLogCount(String geocode, Map<LogType, Integer> logCounts) {
        return saveLogCount(geocode, logCounts, true);
    }

    public boolean saveLogCount(String geocode, Map<LogType, Integer> logCounts, boolean drop) {
        init();

        if (StringUtils.isBlank(geocode) || MapUtils.isEmpty(logCounts)) {
            return false;
        }

        databaseRW.beginTransaction();
        try {
            if (drop) {
                databaseRW.delete(dbTableLogCount, "geocode = ?", new String[] { geocode });
            }

            ContentValues values = new ContentValues();

            Set<Entry<LogType, Integer>> logCountsItems = logCounts.entrySet();
            long timeStamp = System.currentTimeMillis();
            for (Entry<LogType, Integer> pair : logCountsItems) {
                values.clear();
                values.put("geocode", geocode);
                values.put("updated", timeStamp);
                values.put("type", pair.getKey().id);
                values.put("count", pair.getValue());

                databaseRW.insert(dbTableLogCount, null, values);
            }
            databaseRW.setTransactionSuccessful();
        } finally {
            databaseRW.endTransaction();
        }

        return true;
    }

    public boolean saveInventory(String geocode, List<cgTrackable> trackables) {
        init();

        if (trackables == null) {
            return false;
        }

        databaseRW.beginTransaction();
        try {
            if (geocode != null) {
                databaseRW.delete(dbTableTrackables, "geocode = ?", new String[] { geocode });
            }

            if (!trackables.isEmpty()) {
                ContentValues values = new ContentValues();
                long timeStamp = System.currentTimeMillis();
                for (cgTrackable oneTrackable : trackables) {
                    values.clear();
                    if (geocode != null) {
                        values.put("geocode", geocode);
                    }
                    values.put("updated", timeStamp);
                    values.put("tbcode", oneTrackable.getGeocode());
                    values.put("guid", oneTrackable.getGuid());
                    values.put("title", oneTrackable.getName());
                    values.put("owner", oneTrackable.getOwner());
                    if (oneTrackable.getReleased() != null) {
                        values.put("released", oneTrackable.getReleased().getTime());
                    } else {
                        values.put("released", 0L);
                    }
                    values.put("goal", oneTrackable.getGoal());
                    values.put("description", oneTrackable.getDetails());

                    databaseRW.insert(dbTableTrackables, null, values);

                    saveLogs(oneTrackable.getGeocode(), oneTrackable.getLogs());
                }
            }
            databaseRW.setTransactionSuccessful();
        } finally {
            databaseRW.endTransaction();
        }

        return true;
    }

    public List<Number> getBounds(Object[] geocodes) {
        init();

        Cursor cursor = null;

        final List<Number> viewport = new ArrayList<Number>();

        try {
            final StringBuilder where = new StringBuilder();

            if (geocodes != null && geocodes.length > 0) {
                StringBuilder all = new StringBuilder();
                for (Object one : geocodes) {
                    if (all.length() > 0) {
                        all.append(", ");
                    }
                    all.append('"');
                    all.append((String) one);
                    all.append('"');
                }

                if (where.length() > 0) {
                    where.append(" and ");
                }
                where.append("geocode in (");
                where.append(all);
                where.append(')');
            }

            cursor = databaseRO.query(
                    dbTableCaches,
                    new String[] { "count(_id) as cnt", "min(latitude) as latMin", "max(latitude) as latMax", "min(longitude) as lonMin", "max(longitude) as lonMax" },
                    where.toString(),
                    null,
                    null,
                    null,
                    null,
                    null);

            if (cursor != null) {
                int cnt = cursor.getCount();

                if (cnt > 0) {
                    cursor.moveToFirst();

                    viewport.add(cursor.getInt(cursor.getColumnIndex("cnt")));
                    viewport.add(cursor.getDouble(cursor.getColumnIndex("latMin")));
                    viewport.add(cursor.getDouble(cursor.getColumnIndex("latMax")));
                    viewport.add(cursor.getDouble(cursor.getColumnIndex("lonMin")));
                    viewport.add(cursor.getDouble(cursor.getColumnIndex("lonMax")));
                }
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "cgData.getBounds: " + e.toString());
        }

        if (cursor != null) {
            cursor.close();
        }

        return viewport;
    }

    /**
     * Loads a single Cache.
     *
     * @param geocode
     *            The Geocode GCXXXX
     * @param guid
     * @param loadAttributes
     * @param loadWaypoints
     * @param loadSpoilers
     * @param loadLogs
     * @param loadInventory
     * @param loadOfflineLogs
     * @return the loaded cache
     */

    public cgCache loadCache(final String geocode, final int loadFlags) {
        Object[] geocodes = new Object[1];

        if (StringUtils.isNotBlank(geocode)) {
            geocodes[0] = geocode;
        } else {
            geocodes = null;
        }

        List<cgCache> caches = loadCaches(geocodes, null, null, null, null, loadFlags);
        if (CollectionUtils.isNotEmpty(caches)) {
            return caches.get(0);
        }

        return null;
    }

    public List<cgCache> loadCaches(final Object[] geocodes, final Long centerLat, final Long centerLon, final Long spanLat, final Long spanLon, final int loadFlags) {
        init();
        // Using more than one of the parametersets results in overly comlex wheres
        if ((geocodes != null && geocodes.length > 0)
                && centerLat != null
                && centerLon != null
                && spanLat != null
                && spanLon != null) {
            throw new IllegalArgumentException("Please use only one parameter");
        }
        StringBuilder where = new StringBuilder();
        Cursor cursor = null;
        List<cgCache> caches = new ArrayList<cgCache>();

        try {
            if (geocodes != null && geocodes.length > 0) {
                StringBuilder all = new StringBuilder();
                for (Object one : geocodes) {
                    if (all.length() > 0) {
                        all.append(", ");
                    }
                    all.append('"');
                    all.append((String) one);
                    all.append('"');
                }

                if (where.length() > 0) {
                    where.append(" and ");
                }
                where.append("geocode in (");
                where.append(all);
                where.append(')');
            } else {
                return caches;
            }

            // viewport limitation
            if (centerLat != null && centerLon != null && spanLat != null && spanLon != null) {
                double latMin = (centerLat / 1e6) - ((spanLat / 1e6) / 2) - ((spanLat / 1e6) / 4);
                double latMax = (centerLat / 1e6) + ((spanLat / 1e6) / 2) + ((spanLat / 1e6) / 4);
                double lonMin = (centerLon / 1e6) - ((spanLon / 1e6) / 2) - ((spanLon / 1e6) / 4);
                double lonMax = (centerLon / 1e6) + ((spanLon / 1e6) / 2) + ((spanLon / 1e6) / 4);
                double llCache;

                if (latMin > latMax) {
                    llCache = latMax;
                    latMax = latMin;
                    latMin = llCache;
                }
                if (lonMin > lonMax) {
                    llCache = lonMax;
                    lonMax = lonMin;
                    lonMin = llCache;
                }

                if (where.length() > 0) {
                    where.append(" and ");
                }
                where.append("(latitude >= ");
                where.append(String.format((Locale) null, "%.6f", latMin));
                where.append(" and latitude <= ");
                where.append(String.format((Locale) null, "%.6f", latMax));
                where.append(" and longitude >= ");
                where.append(String.format((Locale) null, "%.6f", lonMin));
                where.append(" and longitude <= ");
                where.append(String.format((Locale) null, "%.6f", lonMax));
                where.append(')');
            }
            cursor = databaseRO.query(
                    dbTableCaches,
                    CACHE_COLUMNS,
                    where.toString(),
                    null,
                    null,
                    null,
                    null,
                    null);

            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();

                    do {
                        //Extracted Method
                        cgCache cache = createCacheFromDatabaseContent(cursor);

                        // FIXME: in the following code (and similar blocks below), the
                        // cache.getAttributes() entity probably does not need to be preserved,
                        // and the resolution of the "if" statement could be simply
                        // cache.getAttributes() = attributes
                        if ((loadFlags & cgCache.LOADATTRIBUTES) != 0) {
                            final List<String> attributes = loadAttributes(cache.getGeocode());
                            if (CollectionUtils.isNotEmpty(attributes)) {
                                if (cache.getAttributes() == null) {
                                    cache.setAttributes(new ArrayList<String>());
                                } else {
                                    cache.getAttributes().clear();
                                }
                                cache.getAttributes().addAll(attributes);
                            }
                        }

                        if ((loadFlags & cgCache.LOADWAYPOINTS) != 0) {
                            final List<cgWaypoint> waypoints = loadWaypoints(cache.getGeocode());
                            if (CollectionUtils.isNotEmpty(waypoints)) {
                                if (cache.getWaypoints() == null) {
                                    cache.setWaypoints(new ArrayList<cgWaypoint>());
                                } else {
                                    cache.getWaypoints().clear();
                                }
                                cache.getWaypoints().addAll(waypoints);
                            }
                        }

                        if ((loadFlags & cgCache.LOADSPOILERS) != 0) {
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

                        if ((loadFlags & cgCache.LOADLOGS) != 0) {
                            final List<cgLog> logs = loadLogs(cache.getGeocode());
                            if (CollectionUtils.isNotEmpty(logs)) {
                                if (cache.getLogs() == null) {
                                    cache.setLogs(new ArrayList<cgLog>());
                                } else {
                                    cache.getLogs().clear();
                                }
                                cache.getLogs().addAll(logs);
                            }
                            final Map<LogType, Integer> logCounts = loadLogCounts(cache.getGeocode());
                            if (MapUtils.isNotEmpty(logCounts)) {
                                cache.getLogCounts().clear();
                                cache.getLogCounts().putAll(logCounts);
                            }
                        }

                        if ((loadFlags & cgCache.LOADINVENTORY) != 0) {
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

                        if ((loadFlags & cgCache.LOADOFFLINELOG) != 0) {
                            cache.setLogOffline(hasLogOffline(cache.getGeocode()));
                        }

                        caches.add(cache);
                    } while (cursor.moveToNext());
                } else {
                    cursor.close();
                    return null;
                }
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "cgData.loadCaches: " + e.toString());
        }

        if (cursor != null) {
            cursor.close();
        }

        return caches;
    }

    /**
     * maps a Cache from the cursor. Doesn't next.
     *
     * @param cursor
     * @return
     */

    private static cgCache createCacheFromDatabaseContent(Cursor cursor) {
        int index;
        cgCache cache = new cgCache();

        if (cacheColumnIndex == null) {
            int[] local_cci = new int[40]; // use a local variable to avoid having the not yet fully initialized array be visible to other threads
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
            // local_cci[37] = cursor.getColumnIndex("coordsChanged");
            local_cci[38] = cursor.getColumnIndex("latitude");
            local_cci[39] = cursor.getColumnIndex("longitude");
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
        cache.setOwner(cursor.getString(cacheColumnIndex[11]));
        cache.setOwnerReal(cursor.getString(cacheColumnIndex[12]));
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
        cache.setFavouritePoints(cursor.getInt(cacheColumnIndex[25]));
        cache.setRating(cursor.getFloat(cacheColumnIndex[26]));
        cache.setVotes(cursor.getInt(cacheColumnIndex[27]));
        cache.setMyVote(cursor.getFloat(cacheColumnIndex[28]));
        cache.setDisabled(cursor.getInt(cacheColumnIndex[29]) == 1);
        cache.setArchived(cursor.getInt(cacheColumnIndex[30]) == 1);
        cache.setMembers(cursor.getInt(cacheColumnIndex[31]) == 1);
        cache.setFound(cursor.getInt(cacheColumnIndex[32]) == 1);
        cache.setFavourite(cursor.getInt(cacheColumnIndex[33]) == 1);
        cache.setInventoryItems(cursor.getInt(cacheColumnIndex[34]));
        cache.setOnWatchlist(cursor.getInt(cacheColumnIndex[35]) == 1);
        cache.setReliableLatLon(cursor.getInt(cacheColumnIndex[36]) > 0);
        //cache.setCoordsChanged(cursor.getInt(cacheColumnIndex[37]) > 0);
        return cache;
    }

    public List<String> loadAttributes(String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        init();

        ArrayList<String> attributes = new ArrayList<String>();

        Cursor cursor = databaseRO.query(
                dbTableAttributes,
                new String[] { "attribute" },
                "geocode = ?",
                new String[] { geocode },
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

        Cursor cursor = databaseRO.query(
                dbTableWaypoints,
                new String[] { "_id", "geocode", "updated", "type", "prefix", "lookup", "name", "latlon", "latitude", "longitude", "note" },
                "_id = ?",
                new String[] { Integer.toString(id) },
                null,
                null,
                null,
                "1");

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

        Cursor cursor = databaseRO.query(
                dbTableWaypoints,
                new String[] { "_id", "geocode", "updated", "type", "prefix", "lookup", "name", "latlon", "latitude", "longitude", "note" },
                "geocode = ?",
                new String[] { geocode },
                null,
                null,
                null,
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
        cgWaypoint waypoint = new cgWaypoint();

        waypoint.setId(cursor.getInt(cursor.getColumnIndex("_id")));
        waypoint.setGeocode(cursor.getString(cursor.getColumnIndex("geocode")));
        waypoint.setWaypointType(WaypointType.findById(cursor.getString(cursor.getColumnIndex("type"))));
        waypoint.setPrefix(cursor.getString(cursor.getColumnIndex("prefix")));
        waypoint.setLookup(cursor.getString(cursor.getColumnIndex("lookup")));
        waypoint.setName(cursor.getString(cursor.getColumnIndex("name")));
        waypoint.setLatlon(cursor.getString(cursor.getColumnIndex("latlon")));
        waypoint.setCoords(getCoords(cursor, cursor.getColumnIndex("latitude"), cursor.getColumnIndex("longitude")));
        waypoint.setNote(cursor.getString(cursor.getColumnIndex("note")));

        return waypoint;
    }

    public List<cgImage> loadSpoilers(String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        init();

        List<cgImage> spoilers = new ArrayList<cgImage>();

        Cursor cursor = databaseRO.query(
                dbTableSpoilers,
                new String[] { "url", "title", "description" },
                "geocode = ?",
                new String[] { geocode },
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
    public List<cgDestination> loadHistoryOfSearchedLocations() {
        init();

        Cursor cursor = databaseRO.query(dbTableSearchDestionationHistory,
                new String[] { "_id", "date", "latitude", "longitude" },
                null,
                null,
                null,
                null,
                "date desc",
                "100");

        final List<cgDestination> destinations = new LinkedList<cgDestination>();

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            int indexId = cursor.getColumnIndex("_id");
            int indexDate = cursor.getColumnIndex("date");
            int indexLatitude = cursor.getColumnIndex("latitude");
            int indexLongitude = cursor.getColumnIndex("longitude");

            do {
                final cgDestination dest = new cgDestination();
                dest.setId(cursor.getLong(indexId));
                dest.setDate(cursor.getLong(indexDate));
                dest.setCoords(getCoords(cursor, indexLatitude, indexLongitude));

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
        databaseRW.beginTransaction();

        try {
            databaseRW.delete(dbTableSearchDestionationHistory, null, null);
            databaseRW.setTransactionSuccessful();
        } catch (Exception e) {
            success = false;
            Log.e(Settings.tag, "Unable to clear searched destinations", e);
        } finally {
            databaseRW.endTransaction();
        }

        return success;
    }

    public List<cgLog> loadLogs(String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        init();

        List<cgLog> logs = new ArrayList<cgLog>();

        Cursor cursor = databaseRO.rawQuery(
                "SELECT cg_logs._id as cg_logs_id, type, author, log, date, found, friend, " + dbTableLogImages + "._id as cg_logImages_id, log_id, title, url FROM "
                        + dbTableLogs + " LEFT OUTER JOIN " + dbTableLogImages
                        + " ON ( cg_logs._id = log_id ) WHERE geocode = ?  ORDER BY date desc, cg_logs._id asc", new String[] { geocode });

        if (cursor != null && cursor.getCount() > 0) {
            cgLog log = null;
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
                    log = new cgLog();
                    log.id = cursor.getInt(indexLogsId);
                    log.type = LogType.getById(cursor.getInt(indexType));
                    log.author = cursor.getString(indexAuthor);
                    log.log = cursor.getString(indexLog);
                    log.date = cursor.getLong(indexDate);
                    log.found = cursor.getInt(indexFound);
                    log.friend = cursor.getInt(indexFriend) == 1 ? true : false;
                    logs.add(log);
                }
                if (!cursor.isNull(indexLogImagesId)) {
                    String title = cursor.getString(indexTitle);
                    String url = cursor.getString(indexUrl);
                    if (log.logImages == null) {
                        log.logImages = new ArrayList<cgImage>();
                    }
                    final cgImage log_img = new cgImage(url, title);
                    log.logImages.add(log_img);
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

        Cursor cursor = databaseRO.query(
                dbTableLogCount,
                new String[] { "type", "count" },
                "geocode = ?",
                new String[] { geocode },
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

    public List<cgTrackable> loadInventory(String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        init();

        List<cgTrackable> trackables = new ArrayList<cgTrackable>();

        Cursor cursor = databaseRO.query(
                dbTableTrackables,
                new String[] { "_id", "updated", "tbcode", "guid", "title", "owner", "released", "goal", "description" },
                "geocode = ?",
                new String[] { geocode },
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

        Cursor cursor = databaseRO.query(
                dbTableTrackables,
                new String[] { "updated", "tbcode", "guid", "title", "owner", "released", "goal", "description" },
                "tbcode = ?",
                new String[] { geocode },
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
        String releasedPre = cursor.getString(cursor.getColumnIndex("released"));
        if (releasedPre != null && Long.getLong(releasedPre) != null) {
            trackable.setReleased(new Date(Long.getLong(releasedPre)));
        } else {
            trackable.setReleased(null);
        }
        trackable.setGoal(cursor.getString(cursor.getColumnIndex("goal")));
        trackable.setDetails(cursor.getString(cursor.getColumnIndex("description")));
        trackable.setLogs(loadLogs(trackable.getGeocode()));
        return trackable;
    }

    public int getAllStoredCachesCount(final boolean detailedOnly, final CacheType cacheType, final Integer list) {
        String listSql = null;
        String listSqlW = null;
        if (list == null) {
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
            String sql = "select count(_id) from " + dbTableCaches; // this default is not used, but we like to have variables initialized
            if (!detailedOnly) {
                if (cacheType == CacheType.ALL) {
                    sql = "select count(_id) from " + dbTableCaches + listSql;
                } else {
                    sql = "select count(_id) from " + dbTableCaches + " where type = \"" + cacheType.id + "\"" + listSqlW;
                }
            } else {
                if (cacheType == CacheType.ALL) {
                    sql = "select count(_id) from " + dbTableCaches + " where detailed = 1" + listSqlW;
                } else {
                    sql = "select count(_id) from " + dbTableCaches + " where detailed = 1 and type = \"" + cacheType.id + "\"" + listSqlW;
                }
            }
            SQLiteStatement compiledStmnt = databaseRO.compileStatement(sql);
            count = (int) compiledStmnt.simpleQueryForLong();
            compiledStmnt.close();
        } catch (Exception e) {
            Log.e(Settings.tag, "cgData.loadAllStoredCachesCount: " + e.toString());
        }

        return count;
    }

    public int getAllHistoricCachesCount() {
        init();

        int count = 0;

        try {
            SQLiteStatement sqlCount = databaseRO.compileStatement("select count(_id) from " + dbTableCaches + " where visiteddate > 0");
            count = (int) sqlCount.simpleQueryForLong();
            sqlCount.close();
        } catch (Exception e) {
            Log.e(Settings.tag, "cgData.getAllHistoricCachesCount: " + e.toString());
        }

        return count;
    }

    public List<String> loadBatchOfStoredGeocodes(final boolean detailedOnly, final Geopoint coords, final CacheType cacheType, final int list) {
        init();

        List<String> geocodes = new ArrayList<String>();

        StringBuilder specifySql = new StringBuilder();

        specifySql.append("reason = ");
        specifySql.append(Math.max(list, 1));

        if (detailedOnly) {
            specifySql.append(" and detailed = 1 ");
        }

        if (cacheType != CacheType.ALL) {
            specifySql.append(" and type = \"");
            specifySql.append(cacheType.id);
            specifySql.append('"');
        }

        try {
            Cursor cursor = databaseRO.query(
                    dbTableCaches,
                    new String[] { "geocode", "(abs(latitude-" + String.format((Locale) null, "%.6f", coords.getLatitude()) +
                            ") + abs(longitude-" + String.format((Locale) null, "%.6f", coords.getLongitude()) + ")) as dif" },
                    specifySql.toString(),
                    null,
                    null,
                    null,
                    "dif",
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
            Log.e(Settings.tag, "cgData.loadBatchOfStoredGeocodes: " + e.toString());
        }

        return geocodes;
    }

    public List<String> loadBatchOfHistoricGeocodes(final boolean detailedOnly, final CacheType cacheType) {
        init();

        List<String> geocodes = new ArrayList<String>();

        StringBuilder specifySql = new StringBuilder();
        specifySql.append("visiteddate > 0");

        if (detailedOnly) {
            specifySql.append(" and detailed = 1");
        }
        if (cacheType != CacheType.ALL) {
            specifySql.append(" and type = \"");
            specifySql.append(cacheType.id);
            specifySql.append('"');
        }

        try {
            Cursor cursor = databaseRO.query(
                    dbTableCaches,
                    new String[] { "geocode" },
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
            Log.e(Settings.tag, "cgData.loadBatchOfHistoricGeocodes: " + e.toString());
        }

        return geocodes;
    }

    public List<String> getCachedInViewport(final Long centerLat, final Long centerLon, final Long spanLat, final Long spanLon, final CacheType cacheType) {
        return getInViewport(false, centerLat, centerLon, spanLat, spanLon, cacheType);
    }

    public List<String> getStoredInViewport(final Long centerLat, final Long centerLon, final Long spanLat, final Long spanLon, final CacheType cacheType) {
        return getInViewport(true, centerLat, centerLon, spanLat, spanLon, cacheType);
    }

    public List<String> getInViewport(final boolean stored, final Long centerLat, final Long centerLon, final Long spanLat, final Long spanLon, final CacheType cacheType) {
        if (centerLat == null || centerLon == null || spanLat == null || spanLon == null) {
            return null;
        }

        init();

        List<String> geocodes = new ArrayList<String>();

        // viewport limitation
        double latMin = (centerLat / 1e6) - ((spanLat / 1e6) / 2) - ((spanLat / 1e6) / 4);
        double latMax = (centerLat / 1e6) + ((spanLat / 1e6) / 2) + ((spanLat / 1e6) / 4);
        double lonMin = (centerLon / 1e6) - ((spanLon / 1e6) / 2) - ((spanLon / 1e6) / 4);
        double lonMax = (centerLon / 1e6) + ((spanLon / 1e6) / 2) + ((spanLon / 1e6) / 4);
        double llCache;

        if (latMin > latMax) {
            llCache = latMax;
            latMax = latMin;
            latMin = llCache;
        }
        if (lonMin > lonMax) {
            llCache = lonMax;
            lonMax = lonMin;
            lonMin = llCache;
        }

        StringBuilder where = new StringBuilder();
        where.append("latitude >= ");
        where.append(String.format((Locale) null, "%.6f", latMin));
        where.append(" and latitude <= ");
        where.append(String.format((Locale) null, "%.6f", latMax));
        where.append(" and longitude >= ");
        where.append(String.format((Locale) null, "%.6f", lonMin));
        where.append(" and longitude <= ");
        where.append(String.format((Locale) null, "%.6f", lonMax));

        // cacheType limitation
        if (cacheType != CacheType.ALL) {
            where.append(" and type = \"");
            where.append(cacheType.id);
            where.append('"');
        }

        // offline caches only
        if (stored) {
            where.append(" and reason >= 1");
        }

        try {
            Cursor cursor = databaseRO.query(
                    dbTableCaches,
                    new String[] { "geocode" },
                    where.toString(),
                    null,
                    null,
                    null,
                    null,
                    "500");

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
            Log.e(Settings.tag, "cgData.getOfflineInViewport: " + e.toString());
        }

        return geocodes;
    }

    public List<String> getOfflineAll(CacheType cacheType) {
        init();

        List<String> geocodes = new ArrayList<String>();

        StringBuilder where = new StringBuilder();

        // cacheType limitation
        if (cacheType != CacheType.ALL) {
            where.append(cacheType);
            where.append('"');
        }

        // offline caches only
        if (where.length() > 0) {
            where.append(" and ");
        }
        where.append("reason >= 1");

        try {
            Cursor cursor = databaseRO.query(
                    dbTableCaches,
                    new String[] { "geocode" },
                    where.toString(),
                    null,
                    null,
                    null,
                    null,
                    "5000");

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
            Log.e(Settings.tag, "cgData.getOfflineAll: " + e.toString());
        }

        return geocodes;
    }

    public void markStored(final String geocode, final int listId) {
        if (StringUtils.isBlank(geocode)) {
            return;
        }

        init();

        ContentValues values = new ContentValues();
        values.put("reason", Math.max(listId, 1));
        databaseRW.update(dbTableCaches, values, "geocode = ? and reason < 1", new String[] { geocode });
    }

    public boolean markDropped(String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return false;
        }

        init();

        try {
            ContentValues values = new ContentValues();
            values.put("reason", 0);
            int rows = databaseRW.update(dbTableCaches, values, "geocode = ?", new String[] { geocode });

            if (rows > 0) {
                return true;
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "cgData.markDropped: " + e.toString());
        }

        return false;
    }

    public boolean markFound(String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return false;
        }

        init();

        try {
            ContentValues values = new ContentValues();
            values.put("found", 1);
            int rows = databaseRW.update(dbTableCaches, values, "geocode = ?", new String[] { geocode });

            if (rows > 0) {
                return true;
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "cgData.markFound: " + e.toString());
        }

        return false;
    }

    public void clean() {
        clean(false);
    }

    public void clean(boolean more) {
        init();

        Log.d(Settings.tag, "Database clean: started");

        Cursor cursor = null;
        List<String> geocodes = new ArrayList<String>();

        try {
            if (more) {
                cursor = databaseRO.query(
                        dbTableCaches,
                        new String[] { "geocode" },
                        "reason = 0",
                        null,
                        null,
                        null,
                        null,
                        null);
            } else {
                long timestamp = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000);
                String timestampString = Long.toString(timestamp);
                cursor = databaseRO.query(
                        dbTableCaches,
                        new String[] { "geocode" },
                        "reason = 0 and detailed < ? and detailedupdate < ? and visiteddate < ?",
                        new String[] { timestampString, timestampString, timestampString },
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
                Log.d(Settings.tag, "Database clean: removing " + size + " geocaches");

                dropCaches(geocodes);
            }

            databaseRW.execSQL("delete from " + dbTableCaches + " where geocode = \"\"");

            if (Log.isLoggable(Settings.tag, Log.DEBUG)) {
                final SQLiteStatement countSql = databaseRO.compileStatement("select count(_id) from " + dbTableCaches + " where reason = 0");
                final int count = (int) countSql.simpleQueryForLong();
                countSql.close();
                Log.d(Settings.tag, "Database clean: " + count + " cached geocaches remaining");
            }
        } catch (Exception e) {
            Log.w(Settings.tag, "cgData.clean: " + e.toString());
        }

        Log.d(Settings.tag, "Database clean: finished");
    }

    /**
     * Drop stored list by putting the caches in automatic mode (reason = 0)
     *
     * @param listId
     *            the list id to remove the caches from
     */
    public void dropStored(int listId) {
        init();
        try {
            final ContentValues values = new ContentValues();
            values.put("reason", 0);
            databaseRW.update(dbTableCaches, values, "reason = ?", new String[] { Integer.toString(listId) });
        } catch (Exception e) {
            Log.e(Settings.tag, "cgData.dropStored: error when updating reason", e);
        }
    }

    /**
     * Drop caches from the tables they are stored into, as well as the cache files
     *
     * @param geocodes
     *            list of geocodes to drop from cache
     */
    private void dropCaches(final List<String> geocodes) {
        // Drop caches from the database
        final ArrayList<String> quotedGeocodes = new ArrayList<String>(geocodes.size());
        for (final String geocode : geocodes) {
            quotedGeocodes.add('"' + geocode + '"'); // FIXME: there ought to be a better way of doing this
        }
        final String geocodeList = StringUtils.join(quotedGeocodes.toArray(), ',');
        final String baseWhereClause = "geocode in (" + geocodeList + ")";
        databaseRW.delete(dbTableCaches, baseWhereClause, null);
        databaseRW.delete(dbTableAttributes, baseWhereClause, null);
        databaseRW.delete(dbTableSpoilers, baseWhereClause, null);
        databaseRW.delete(dbTableLogs, baseWhereClause, null);
        databaseRW.delete(dbTableLogCount, baseWhereClause, null);
        databaseRW.delete(dbTableLogsOffline, baseWhereClause, null);
        databaseRW.delete(dbTableWaypoints, baseWhereClause + " and type <> \"own\"", null);
        databaseRW.delete(dbTableTrackables, baseWhereClause, null);

        // Delete cache directories
        for (final String geocode : geocodes) {
            cgBase.deleteDirectory(LocalStorage.getStorageDir(geocode));
        }
    }

    public boolean saveLogOffline(String geocode, Date date, LogType type, String log) {
        if (StringUtils.isBlank(geocode)) {
            return false;
        }
        if (LogType.LOG_UNKNOWN == type && StringUtils.isBlank(log)) {
            return false;
        }

        boolean status = false;

        ContentValues values = new ContentValues();
        values.put("geocode", geocode);
        values.put("updated", System.currentTimeMillis());
        values.put("type", type.id);
        values.put("log", log);
        values.put("date", date.getTime());

        try {
            if (hasLogOffline(geocode)) {
                final int rows = databaseRW.update(dbTableLogsOffline, values, "geocode = ?", new String[] { geocode });

                if (rows > 0) {
                    status = true;
                }
            } else {
                final long id = databaseRW.insert(dbTableLogsOffline, null, values);

                if (id > 0) {
                    status = true;
                }
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "cgData.saveLogOffline: " + e.toString());
        }

        return status;
    }

    public cgLog loadLogOffline(String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        init();

        cgLog log = null;

        Cursor cursor = databaseRO.query(
                dbTableLogsOffline,
                new String[] { "_id", "type", "log", "date" },
                "geocode = ?",
                new String[] { geocode },
                null,
                null,
                "_id desc",
                "1");

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();

            log = new cgLog();
            log.id = cursor.getInt(cursor.getColumnIndex("_id"));
            log.type = LogType.getById(cursor.getInt(cursor.getColumnIndex("type")));
            log.log = cursor.getString(cursor.getColumnIndex("log"));
            log.date = cursor.getLong(cursor.getColumnIndex("date"));
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

        databaseRW.delete(dbTableLogsOffline, "geocode = ?", new String[] { geocode });
    }

    private synchronized SQLiteStatement getStatementLogCount() {
        if (statementLogCount == null) {
            statementLogCount = databaseRO.compileStatement("SELECT count(_id) FROM " + dbTableLogsOffline + " WHERE geocode = ?");
        }
        return statementLogCount;
    }

    private synchronized SQLiteStatement getStatementStandardList() {
        if (statementStandardList == null) {
            statementStandardList = databaseRO.compileStatement("SELECT count(_id) FROM " + dbTableCaches + " WHERE reason = " + StoredList.STANDARD_LIST_ID);
        }
        return statementStandardList;
    }

    public boolean hasLogOffline(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return false;
        }

        init();
        try {
            final SQLiteStatement logCount = getStatementLogCount();
            synchronized (logCount) {
                statementLogCount.bindString(1, geocode);
                return statementLogCount.simpleQueryForLong() > 0;
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "cgData.hasLogOffline", e);
        }

        return false;
    }

    public void saveVisitDate(String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put("visiteddate", System.currentTimeMillis());

        try {
            databaseRW.update(dbTableCaches, values, "geocode = ?", new String[] { geocode });
        } catch (Exception e) {
            Log.e(Settings.tag, "cgData.saveVisitDate: " + e.toString());
        }
    }

    public void clearVisitDate(String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put("visiteddate", 0);

        try {
            databaseRW.update(dbTableCaches, values, "geocode = ?", new String[] { geocode });
        } catch (Exception e) {
            Log.e(Settings.tag, "cgData.clearVisitDate: " + e.toString());
        }
    }

    public List<StoredList> getLists(Resources res) {
        init();

        List<StoredList> lists = new ArrayList<StoredList>();
        lists.add(new StoredList(StoredList.STANDARD_LIST_ID, res.getString(R.string.list_inbox), (int) getStatementStandardList().simpleQueryForLong()));

        try {
            String query = "SELECT l._id as _id, l.title as title, COUNT(c._id) as count" +
                    " FROM " + dbTableLists + " l LEFT OUTER JOIN " + dbTableCaches + " c" +
                    " ON l._id + 10 = c.reason" +
                    " GROUP BY l._id" +
                    " ORDER BY l.title COLLATE NOCASE ASC";

            Cursor cursor = databaseRO.rawQuery(query, null);
            ArrayList<StoredList> storedLists = getListsFromCursor(cursor);
            lists.addAll(storedLists);

        } catch (Exception e) {
            Log.e(Settings.tag, "cgData.readLists: " + e.toString());
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
                    StoredList list = new StoredList(cursor.getInt(indexId) + 10, cursor.getString(indexTitle), count);
                    result.add(list);
                } while (cursor.moveToNext());
            }

            cursor.close();
        }
        return result;
    }

    public StoredList getList(int id, Resources res) {
        if (id == StoredList.STANDARD_LIST_ID) {
            return new StoredList(StoredList.STANDARD_LIST_ID, res.getString(R.string.list_inbox), (int) getStatementStandardList().simpleQueryForLong());
        } else if (id >= 10) {
            init();

            Cursor cursor = databaseRO.query(
                    dbTableLists,
                    new String[] { "_id", "title" },
                    "_id = " + (id - 10),
                    null,
                    null,
                    null,
                    null);
            ArrayList<StoredList> lists = getListsFromCursor(cursor);
            if (!lists.isEmpty()) {
                return lists.get(0);
            }
        }

        return null;
    }

    public int createList(String name) {
        int id = -1;
        if (StringUtils.isBlank(name)) {
            return id;
        }

        init();

        databaseRW.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("title", name);
            values.put("updated", System.currentTimeMillis());

            id = (int) databaseRW.insert(dbTableLists, null, values);
            databaseRW.setTransactionSuccessful();
        } finally {
            databaseRW.endTransaction();
        }

        if (id < 0) {
            return -1;
        } else {
            return (id + 10);
        }
    }

    public int renameList(final int listId, final String name) {
        if (StringUtils.isBlank(name)) {
            return 0;
        }

        init();

        int count = 0;
        databaseRW.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("title", name);
            values.put("updated", System.currentTimeMillis());

            count = databaseRW.update(dbTableLists, values, "_id = " + (listId - 10), null);
            databaseRW.setTransactionSuccessful();
        } finally {
            databaseRW.endTransaction();
        }

        return count;
    }

    public boolean removeList(int id) {
        boolean status = false;
        if (id < 10) {
            return status;
        }

        init();

        databaseRW.beginTransaction();
        try {
            int cnt = databaseRW.delete(dbTableLists, "_id = " + (id - 10), null);

            if (cnt > 0) {
                ContentValues values = new ContentValues();
                values.put("reason", 1);
                databaseRW.update(dbTableCaches, values, "reason = " + id, null);

                status = true;
            }

            databaseRW.setTransactionSuccessful();
        } finally {
            databaseRW.endTransaction();
        }

        return status;
    }

    public void moveToList(String geocode, int listId) {
        if (StringUtils.isBlank(geocode) || listId <= 0) {
            return;
        }

        databaseRW.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("reason", listId);
            databaseRW.update(dbTableCaches, values, "geocode = ?", new String[] { geocode });

            databaseRW.setTransactionSuccessful();
        } finally {
            databaseRW.endTransaction();
        }
    }

    public synchronized boolean status() {
        if (databaseRO == null || databaseRW == null || !initialized) {
            return false;
        }

        return true;
    }

    public boolean removeSearchedDestination(cgDestination destination) {
        boolean success = true;
        if (destination == null) {
            success = false;
        } else {
            init();

            databaseRW.beginTransaction();

            try {
                databaseRW.delete(dbTableSearchDestionationHistory, "_id = " + destination.getId(), null);
                databaseRW.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(Settings.tag, "Unable to remove searched destination", e);
                success = false;
            } finally {
                databaseRW.endTransaction();
            }
        }

        return success;
    }

    private synchronized SQLiteStatement getStatementDescription() {
        if (statementDescription == null) {
            statementDescription = databaseRO.compileStatement("SELECT description FROM " + dbTableCaches + " WHERE geocode = ?");
        }
        return statementDescription;
    }

    public String getCacheDescription(String geocode) {
        if (geocode == null) {
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
            Log.e(Settings.tag, "cgData.getCacheDescription", e);
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
}
