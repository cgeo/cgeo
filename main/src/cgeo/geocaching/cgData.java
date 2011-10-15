package cgeo.geocaching;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Geopoint.MalformedCoordinateException;

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
            "inventoryunknown", "onWatchlist", "personal_note", "reliable_latlon"
    };
    private Context context = null;
    private String path = null;
    private cgDbHelper dbHelper = null;
    private SQLiteDatabase databaseRO = null;
    private SQLiteDatabase databaseRW = null;
    private static final int dbVersion = 59;
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
            + "onWatchlist integer default 0 "
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
            + "found integer not null default 0 "
            + "); ";
    private final static int LOGS_GEOCODE = 2;
    private final static int LOGS_UPDATED = 3;
    private final static int LOGS_TYPE = 4;
    private final static int LOGS_AUTHOR = 5;
    private final static int LOGS_LOG = 6;
    private final static int LOGS_DATE = 7;
    private final static int LOGS_FOUND = 8;

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

    public boolean initialized = false;
    private SQLiteStatement statementDescription;
    private SQLiteStatement statementLogCount;

    public cgData(Context contextIn) {
        context = contextIn;
    }

    public synchronized void init() {
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
        }
        if (statementLogCount != null) {
            statementLogCount.close();
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

        if (cache.updated == null) {
            values.put("updated", System.currentTimeMillis());
        } else {
            values.put("updated", cache.updated);
        }
        values.put("reason", cache.reason);
        values.put("detailed", cache.detailed ? 1 : 0);
        values.put("detailedupdate", cache.detailedUpdate);
        values.put("visiteddate", cache.visitedDate);
        values.put("geocode", cache.geocode);
        values.put("cacheid", cache.cacheId);
        values.put("guid", cache.guid);
        values.put("type", cache.type);
        values.put("name", cache.name);
        values.put("own", cache.own ? 1 : 0);
        values.put("owner", cache.owner);
        values.put("owner_real", cache.ownerReal);
        if (cache.hidden == null) {
            values.put("hidden", 0);
        } else {
            values.put("hidden", cache.hidden.getTime());
        }
        values.put("hint", cache.hint);
        values.put("size", cache.size == null ? "" : cache.size.id);
        values.put("difficulty", cache.difficulty);
        values.put("terrain", cache.terrain);
        values.put("latlon", cache.latlon);
        values.put("location", cache.location);
        values.put("distance", cache.distance);
        values.put("direction", cache.direction);
        putCoords(values, cache.coords);
        values.put("reliable_latlon", cache.reliableLatLon ? 1 : 0);
        values.put("elevation", cache.elevation);
        values.put("shortdesc", cache.shortdesc);
        values.put("personal_note", cache.personalNote);
        values.put("description", cache.getDescription());
        values.put("favourite_cnt", cache.favouriteCnt);
        values.put("rating", cache.rating);
        values.put("votes", cache.votes);
        values.put("myvote", cache.myVote);
        values.put("disabled", cache.disabled ? 1 : 0);
        values.put("archived", cache.archived ? 1 : 0);
        values.put("members", cache.members ? 1 : 0);
        values.put("found", cache.found ? 1 : 0);
        values.put("favourite", cache.favourite ? 1 : 0);
        values.put("inventoryunknown", cache.inventoryItems);
        values.put("onWatchlist", cache.onWatchlist ? 1 : 0);

        boolean statusOk = true;

        if (cache.attributes != null) {
            if (!saveAttributes(cache.geocode, cache.attributes)) {
                statusOk = false;
            }
        }

        if (cache.waypoints != null) {
            if (!saveWaypoints(cache.geocode, cache.waypoints, true)) {
                statusOk = false;
            }
        }

        if (cache.spoilers != null) {
            if (!saveSpoilers(cache.geocode, cache.spoilers)) {
                statusOk = false;
            }
        }

        if (cache.logs != null) {
            if (!saveLogs(cache.geocode, cache.logs)) {
                statusOk = false;
            }
        }

        if (MapUtils.isNotEmpty(cache.logCounts)) {
            if (!saveLogCount(cache.geocode, cache.logCounts)) {
                statusOk = false;
            }
        }

        if (cache.inventory != null) {
            if (!saveInventory(cache.geocode, cache.inventory)) {
                statusOk = false;
            }
        }

        if (!statusOk) {
            cache.detailed = false;
            cache.detailedUpdate = 0L;
        }

        init();

        //try to update record else insert fresh..
        try {
            int rows = databaseRW.update(dbTableCaches, values, "geocode = ?", new String[] { cache.geocode });
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
                    values.put("type", oneWaypoint.type != null ? oneWaypoint.type.id : null);
                    values.put("prefix", oneWaypoint.getPrefix());
                    values.put("lookup", oneWaypoint.lookup);
                    values.put("name", oneWaypoint.name);
                    values.put("latlon", oneWaypoint.latlon);
                    putCoords(values, oneWaypoint.coords);
                    values.put("note", oneWaypoint.note);

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

        try {
            return new Geopoint(cursor.getDouble(indexLat), cursor.getDouble(indexLon));
        } catch (MalformedCoordinateException e) {
            // TODO: check whether the exception should be returned to the caller instead,
            // as it might want to remove an invalid geopoint from the database.
            Log.e(Settings.tag, "cannot parse geopoint from database: " + e.getMessage());
            return null;
        }
    }

    /**
     * Retrieve coordinates from a Cursor
     *
     * @param cursor
     *            a Cursor representing a row in the database
     * @return the coordinates, or null if latitude or longitude is null or the coordinates are invalid
     */
    private static Geopoint getCoords(final Cursor cursor) {
        final int indexLat = cursor.getColumnIndex("latitude");
        final int indexLon = cursor.getColumnIndex("longitude");
        return getCoords(cursor, indexLat, indexLon);
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
            values.put("type", waypoint.type != null ? waypoint.type.id : null);
            values.put("prefix", waypoint.getPrefix());
            values.put("lookup", waypoint.lookup);
            values.put("name", waypoint.name);
            values.put("latlon", waypoint.latlon);
            putCoords(values, waypoint.coords);
            values.put("note", waypoint.note);

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
                    values.put("url", oneSpoiler.url);
                    values.put("title", oneSpoiler.title);
                    values.put("description", oneSpoiler.description);

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
                    helper.bind(LOGS_TYPE, log.type);
                    helper.bind(LOGS_AUTHOR, log.author);
                    helper.bind(LOGS_LOG, log.log);
                    helper.bind(LOGS_DATE, log.date);
                    helper.bind(LOGS_FOUND, log.found);

                    long log_id = helper.execute();

                    if (CollectionUtils.isNotEmpty(log.logImages)) {
                        ContentValues values = new ContentValues();
                        for (cgImage img : log.logImages) {
                            values.clear();
                            values.put("log_id", log_id);
                            values.put("title", img.title);
                            values.put("url", img.url);
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

    public boolean saveLogCount(String geocode, Map<Integer, Integer> logCounts) {
        return saveLogCount(geocode, logCounts, true);
    }

    public boolean saveLogCount(String geocode, Map<Integer, Integer> logCounts, boolean drop) {
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

            Set<Entry<Integer, Integer>> logCountsItems = logCounts.entrySet();
            long timeStamp = System.currentTimeMillis();
            for (Entry<Integer, Integer> pair : logCountsItems) {
                values.clear();
                values.put("geocode", geocode);
                values.put("updated", timeStamp);
                values.put("type", pair.getKey().intValue());
                values.put("count", pair.getValue().intValue());

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

    public List<Object> getBounds(Object[] geocodes) {
        init();

        Cursor cursor = null;

        final List<Object> viewport = new ArrayList<Object>();

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

    public cgCache loadCache(String geocode, String guid) {
        return loadCache(geocode, guid, false, true, false, false, false, false);
    }

    /**
     * Loads a single Cache.
     *
     * @param geocode
     *            The Geocode GCXXXX
     * @param guid
     * @param loadA
     * @param loadW
     * @param loadS
     * @param loadL
     * @param loadI
     * @param loadO
     * @return the loaded cache
     */

    public cgCache loadCache(String geocode, String guid, boolean loadA, boolean loadW, boolean loadS, boolean loadL, boolean loadI, boolean loadO) {
        Object[] geocodes = new Object[1];
        Object[] guids = new Object[1];

        if (StringUtils.isNotBlank(geocode)) {
            geocodes[0] = geocode;
        } else {
            geocodes = null;
        }

        if (StringUtils.isNotBlank(guid)) {
            guids[0] = guid;
        } else {
            guids = null;
        }

        List<cgCache> caches = loadCaches(geocodes, null, null, null, null, null, loadA, loadW, loadS, loadL, loadI, loadO);
        if (CollectionUtils.isNotEmpty(caches)) {
            return caches.get(0);
        }

        return null;
    }

    public List<cgCache> loadCaches(Object[] geocodes, Object[] guids) {
        return loadCaches(geocodes, guids, null, null, null, null, false, true, false, false, false, false);
    }

    public List<cgCache> loadCaches(Object[] geocodes, Object[] guids, boolean lite) {
        if (lite) {
            return loadCaches(geocodes, guids, null, null, null, null, false, true, false, false, false, false);
        } else {
            return loadCaches(geocodes, guids, null, null, null, null, true, true, true, true, true, true);
        }
    }

    public List<cgCache> loadCaches(Object[] geocodes, Object[] guids, Long centerLat, Long centerLon, Long spanLat, Long spanLon, boolean loadA, boolean loadW, boolean loadS, boolean loadL, boolean loadI, boolean loadO) {
        init();
        // Using more than one of the parametersets results in overly comlex wheres
        if (((geocodes != null && geocodes.length > 0) && (guids != null && guids.length > 0))) {
            throw new IllegalArgumentException("Please use only one parameter");
        }
        if (((geocodes != null && geocodes.length > 0) || (guids != null && guids.length > 0))
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
            } else if (guids != null && guids.length > 0) {
                StringBuilder all = new StringBuilder();
                for (Object one : guids) {
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
                where.append("guid in (");
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
                        // cache.attributes entity probably does not need to be preserved,
                        // and the resolution of the "if" statement could be simply
                        // cache.attributes = attributes
                        if (loadA) {
                            final List<String> attributes = loadAttributes(cache.geocode);
                            if (CollectionUtils.isNotEmpty(attributes)) {
                                if (cache.attributes == null) {
                                    cache.attributes = new ArrayList<String>();
                                } else {
                                    cache.attributes.clear();
                                }
                                cache.attributes.addAll(attributes);
                            }
                        }

                        if (loadW) {
                            final List<cgWaypoint> waypoints = loadWaypoints(cache.geocode);
                            if (CollectionUtils.isNotEmpty(waypoints)) {
                                if (cache.waypoints == null) {
                                    cache.waypoints = new ArrayList<cgWaypoint>();
                                } else {
                                    cache.waypoints.clear();
                                }
                                cache.waypoints.addAll(waypoints);
                            }
                        }

                        if (loadS) {
                            final List<cgImage> spoilers = loadSpoilers(cache.geocode);
                            if (CollectionUtils.isNotEmpty(spoilers)) {
                                if (cache.spoilers == null) {
                                    cache.spoilers = new ArrayList<cgImage>();
                                } else {
                                    cache.spoilers.clear();
                                }
                                cache.spoilers.addAll(spoilers);
                            }
                        }

                        if (loadL) {
                            final List<cgLog> logs = loadLogs(cache.geocode);
                            if (CollectionUtils.isNotEmpty(logs)) {
                                if (cache.logs == null) {
                                    cache.logs = new ArrayList<cgLog>();
                                } else {
                                    cache.logs.clear();
                                }
                                cache.logs.addAll(logs);
                            }
                            final Map<Integer, Integer> logCounts = loadLogCounts(cache.geocode);
                            if (MapUtils.isNotEmpty(logCounts)) {
                                cache.logCounts.clear();
                                cache.logCounts.putAll(logCounts);
                            }
                        }

                        if (loadI) {
                            final List<cgTrackable> inventory = loadInventory(cache.geocode);
                            if (CollectionUtils.isNotEmpty(inventory)) {
                                if (cache.inventory == null) {
                                    cache.inventory = new ArrayList<cgTrackable>();
                                } else {
                                    cache.inventory.clear();
                                }
                                cache.inventory.addAll(inventory);
                            }
                        }

                        if (loadO) {
                            cache.logOffline = hasLogOffline(cache.geocode);
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

        cache.updated = cursor.getLong(cursor.getColumnIndex("updated"));
        cache.reason = cursor.getInt(cursor.getColumnIndex("reason"));
        cache.detailed = cursor.getInt(cursor.getColumnIndex("detailed")) == 1;
        cache.detailedUpdate = cursor.getLong(cursor.getColumnIndex("detailedupdate"));
        cache.visitedDate = cursor.getLong(cursor.getColumnIndex("visiteddate"));
        cache.geocode = cursor.getString(cursor.getColumnIndex("geocode"));
        cache.cacheId = cursor.getString(cursor.getColumnIndex("cacheid"));
        cache.guid = cursor.getString(cursor.getColumnIndex("guid"));
        cache.type = cursor.getString(cursor.getColumnIndex("type"));
        cache.name = cursor.getString(cursor.getColumnIndex("name"));
        cache.own = cursor.getInt(cursor.getColumnIndex("own")) == 1;
        cache.owner = cursor.getString(cursor.getColumnIndex("owner"));
        cache.ownerReal = cursor.getString(cursor.getColumnIndex("owner_real"));
        cache.hidden = new Date(cursor.getLong(cursor.getColumnIndex("hidden")));
        cache.hint = cursor.getString(cursor.getColumnIndex("hint"));
        cache.size = CacheSize.FIND_BY_ID.get(cursor.getString(cursor.getColumnIndex("size")));
        cache.difficulty = cursor.getFloat(cursor.getColumnIndex("difficulty"));
        index = cursor.getColumnIndex("direction");
        if (cursor.isNull(index)) {
            cache.direction = null;
        } else {
            cache.direction = cursor.getFloat(index);
        }
        index = cursor.getColumnIndex("distance");
        if (cursor.isNull(index)) {
            cache.distance = null;
        } else {
            cache.distance = cursor.getFloat(index);
        }
        cache.terrain = cursor.getFloat(cursor.getColumnIndex("terrain"));
        cache.latlon = cursor.getString(cursor.getColumnIndex("latlon"));
        cache.location = cursor.getString(cursor.getColumnIndex("location"));
        cache.coords = getCoords(cursor);
        index = cursor.getColumnIndex("elevation");
        if (cursor.isNull(index)) {
            cache.elevation = null;
        } else {
            cache.elevation = cursor.getDouble(index);
        }
        cache.personalNote = cursor.getString(cursor.getColumnIndex("personal_note"));
        cache.shortdesc = cursor.getString(cursor.getColumnIndex("shortdesc"));
        // do not set cache.description !
        cache.favouriteCnt = cursor.getInt(cursor.getColumnIndex("favourite_cnt"));
        cache.rating = cursor.getFloat(cursor.getColumnIndex("rating"));
        cache.votes = cursor.getInt(cursor.getColumnIndex("votes"));
        cache.myVote = cursor.getFloat(cursor.getColumnIndex("myvote"));
        cache.disabled = cursor.getLong(cursor.getColumnIndex("disabled")) == 1L;
        cache.archived = cursor.getLong(cursor.getColumnIndex("archived")) == 1L;
        cache.members = cursor.getLong(cursor.getColumnIndex("members")) == 1L;
        cache.found = cursor.getLong(cursor.getColumnIndex("found")) == 1L;
        cache.favourite = cursor.getLong(cursor.getColumnIndex("favourite")) == 1L;
        cache.inventoryItems = cursor.getInt(cursor.getColumnIndex("inventoryunknown"));
        cache.onWatchlist = cursor.getLong(cursor.getColumnIndex("onWatchlist")) == 1L;
        cache.reliableLatLon = cursor.getInt(cursor.getColumnIndex("reliable_latlon")) > 0;
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

    public cgWaypoint loadWaypoint(Integer id) {
        if (id == null || id == 0) {
            return null;
        }

        init();

        cgWaypoint waypoint = new cgWaypoint();

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

        waypoint.id = cursor.getInt(cursor.getColumnIndex("_id"));
        waypoint.geocode = cursor.getString(cursor.getColumnIndex("geocode"));
        waypoint.type = WaypointType.FIND_BY_ID.get(cursor.getString(cursor.getColumnIndex("type")));
        waypoint.setPrefix(cursor.getString(cursor.getColumnIndex("prefix")));
        waypoint.lookup = cursor.getString(cursor.getColumnIndex("lookup"));
        waypoint.name = cursor.getString(cursor.getColumnIndex("name"));
        waypoint.latlon = cursor.getString(cursor.getColumnIndex("latlon"));
        waypoint.coords = getCoords(cursor);
        waypoint.note = cursor.getString(cursor.getColumnIndex("note"));

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
                cgImage spoiler = new cgImage();
                spoiler.url = cursor.getString(indexUrl);
                spoiler.title = cursor.getString(indexTitle);
                spoiler.description = cursor.getString(indexDescription);

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
                "SELECT cg_logs._id as cg_logs_id, type, author, log, date, found, " + dbTableLogImages + "._id as cg_logImages_id, log_id, title, url FROM "
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
            int indexLogImagesId = cursor.getColumnIndex("cg_logImages_id");
            int indexTitle = cursor.getColumnIndex("title");
            int indexUrl = cursor.getColumnIndex("url");
            while (cursor.moveToNext() && logs.size() < 100) {
                if (log == null || log.id != cursor.getInt(indexLogsId)) {
                    log = new cgLog();
                    log.id = cursor.getInt(indexLogsId);
                    log.type = cursor.getInt(indexType);
                    log.author = cursor.getString(indexAuthor);
                    log.log = cursor.getString(indexLog);
                    log.date = cursor.getLong(indexDate);
                    log.found = cursor.getInt(indexFound);
                    logs.add(log);
                }
                if (!cursor.isNull(indexLogImagesId)) {
                    final cgImage log_img = new cgImage();
                    log_img.title = cursor.getString(indexTitle);
                    if (log_img.title == null) {
                        log_img.title = "";
                    }
                    log_img.url = cursor.getString(indexUrl);
                    if (log_img.url == null) {
                        log_img.url = "";
                    }
                    if (log.logImages == null) {
                        log.logImages = new ArrayList<cgImage>();
                    }
                    log.logImages.add(log_img);
                }
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        return logs;
    }

    public Map<Integer, Integer> loadLogCounts(String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        init();

        Map<Integer, Integer> logCounts = new HashMap<Integer, Integer>();

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
                Integer type = cursor.getInt(indexType);
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

    public int getAllStoredCachesCount(boolean detailedOnly, String cachetype, Integer list) {
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
                if (cachetype == null) {
                    sql = "select count(_id) from " + dbTableCaches + listSql;
                } else {
                    sql = "select count(_id) from " + dbTableCaches + " where type = \"" + cachetype + "\"" + listSqlW;
                }
            } else {
                if (cachetype == null) {
                    sql = "select count(_id) from " + dbTableCaches + " where detailed = 1" + listSqlW;
                } else {
                    sql = "select count(_id) from " + dbTableCaches + " where detailed = 1 and type = \"" + cachetype + "\"" + listSqlW;
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

    public List<String> loadBatchOfStoredGeocodes(final boolean detailedOnly, final Geopoint coords, final String cachetype, final int list) {
        init();

        List<String> geocodes = new ArrayList<String>();

        StringBuilder specifySql = new StringBuilder();

        specifySql.append("reason = ");
        specifySql.append(Math.max(list, 1));

        if (detailedOnly) {
            specifySql.append(" and detailed = 1 ");
        }

        if (cachetype != null) {
            specifySql.append(" and type = \"");
            specifySql.append(cachetype);
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

    public List<String> loadBatchOfHistoricGeocodes(boolean detailedOnly, String cachetype) {
        init();

        List<String> geocodes = new ArrayList<String>();

        StringBuilder specifySql = new StringBuilder();
        specifySql.append("visiteddate > 0");

        if (detailedOnly) {
            specifySql.append(" and detailed = 1");
        }
        if (cachetype != null) {
            specifySql.append(" and type = \"");
            specifySql.append(cachetype);
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

    public List<String> getCachedInViewport(Long centerLat, Long centerLon, Long spanLat, Long spanLon, String cachetype) {
        return getInViewport(false, centerLat, centerLon, spanLat, spanLon, cachetype);
    }

    public List<String> getStoredInViewport(Long centerLat, Long centerLon, Long spanLat, Long spanLon, String cachetype) {
        return getInViewport(true, centerLat, centerLon, spanLat, spanLon, cachetype);
    }

    public List<String> getInViewport(boolean stored, Long centerLat, Long centerLon, Long spanLat, Long spanLon, String cachetype) {
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

        // cachetype limitation
        if (cachetype != null) {
            where.append(" and type = \"");
            where.append(cachetype);
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

    public List<String> getOfflineAll(String cachetype) {
        init();

        List<String> geocodes = new ArrayList<String>();

        StringBuilder where = new StringBuilder();

        // cachetype limitation
        if (cachetype != null) {
            where.append(cachetype);
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

    public boolean saveLogOffline(String geocode, Date date, int type, String log) {
        if (StringUtils.isBlank(geocode)) {
            return false;
        }
        if (type <= 0 && StringUtils.isBlank(log)) {
            return false;
        }

        boolean status = false;

        ContentValues values = new ContentValues();
        values.put("geocode", geocode);
        values.put("updated", System.currentTimeMillis());
        values.put("type", type);
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
            log.type = cursor.getInt(cursor.getColumnIndex("type"));
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

    private SQLiteStatement getStatementLogCount() {
        if (statementLogCount == null) {
            synchronized (this) {
                if (statementLogCount == null) {
                    statementLogCount = databaseRO.compileStatement("SELECT count(_id) FROM " + dbTableLogsOffline + " WHERE geocode = ?");
                }
            }
        }
        return statementLogCount;
    }

    public boolean hasLogOffline(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return false;
        }

        init();
        try {
            final SQLiteStatement logCount = getStatementLogCount();
            synchronized (logCount) {
                statementLogCount.bindString(1, geocode.toUpperCase());
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

    public List<cgList> getLists(Resources res) {
        init();

        List<cgList> lists = new ArrayList<cgList>();

        lists.add(new cgList(true, 1, res.getString(R.string.list_inbox)));
        // lists.add(new cgList(true, 2, res.getString(R.string.list_wpt)));

        ArrayList<cgList> storedLists = readLists(null, "title COLLATE NOCASE ASC");
        lists.addAll(storedLists);

        return lists;
    }

    private ArrayList<cgList> readLists(String selection, String sorting) {
        ArrayList<cgList> result = new ArrayList<cgList>();
        try {
            Cursor cursor = databaseRO.query(
                    dbTableLists,
                    new String[] { "_id", "title", "updated", "latitude", "longitude" },
                    selection,
                    null,
                    null,
                    null,
                    sorting);

            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    int indexId = cursor.getColumnIndex("_id");
                    int indexTitle = cursor.getColumnIndex("title");
                    int indexUpdated = cursor.getColumnIndex("updated");
                    int indexLatitude = cursor.getColumnIndex("latitude");
                    int indexLongitude = cursor.getColumnIndex("longitude");

                    do {
                        cgList list = new cgList(false);

                        list.id = (cursor.getInt(indexId)) + 10;
                        list.title = cursor.getString(indexTitle);
                        list.updated = cursor.getLong(indexUpdated);
                        list.coords = getCoords(cursor, indexLatitude, indexLongitude);

                        result.add(list);
                    } while (cursor.moveToNext());
                }

                cursor.close();
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "cgData.readLists: " + e.toString());
        }
        return result;
    }

    public cgList getList(int id, Resources res) {
        if (id == 1) {
            return new cgList(true, 1, res.getString(R.string.list_inbox));
        } else if (id == 2) {
            return new cgList(true, 2, res.getString(R.string.list_wpt));
        } else if (id >= 10) {
            init();

            ArrayList<cgList> lists = readLists("_id = " + (id - 10), null);
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

    private SQLiteStatement getStatementDescription() {
        if (statementDescription == null) {
            synchronized (this) {
                if (statementDescription == null) {
                    statementDescription = databaseRO.compileStatement("SELECT description FROM " + dbTableCaches + " WHERE geocode = ?");
                }
            }
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
}
