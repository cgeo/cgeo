package cgeo.geocaching.storage;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.internal.InternalConnector;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.LoadFlag;
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.list.AbstractList;
import cgeo.geocaching.list.PseudoList;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.LogTypeTrackable;
import cgeo.geocaching.log.OfflineLogEntry;
import cgeo.geocaching.log.ReportProblemType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.RouteSegment;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.models.TrailHistoryElement;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.search.GeocacheSearchSuggestionCursor;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.sorting.CacheComparator;
import cgeo.geocaching.storage.extension.DBDowngradeableVersions;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.EmojiUtils;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.Version;
import cgeo.geocaching.utils.formulas.VariableList;
import cgeo.geocaching.utils.functions.Func1;
import static cgeo.geocaching.settings.Settings.getMaximumMapTrailLength;
import static cgeo.geocaching.storage.DataStore.DBExtensionType.DBEXTENSION_INVALID;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.location.Location;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

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
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleOnSubscribe;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class DataStore {

    public static final String DB_FILE_NAME = "data";
    public static final String DB_FILE_NAME_BACKUP = "cgeo.sqlite";
    public static final String DB_FILE_CORRUPTED_EXTENSION = ".corrupted";

    // some fields names which are referenced multiple times
    // name scheme is "FIELD_" + table name without prefix + "_" + field name
    private static final String FIELD_LISTS_PREVENTASKFORDELETION = "preventAskForDeletion";

    public enum DBRestoreResult {
        RESTORE_SUCCESSFUL(R.string.init_restore_success),
        RESTORE_FAILED_GENERAL(R.string.init_restore_db_failed),
        RESTORE_FAILED_DBRECREATED(R.string.init_restore_failed_dbrecreated);

        public final @StringRes int res;

        DBRestoreResult(final int res) {
            this.res = res;
        }
    }

    public enum StorageLocation {
        HEAP,
        CACHE,
        DATABASE,
    }

    public enum DBExtensionType {
        // values for id must not be changed, as there are database entries depending on it
        DBEXTENSION_INVALID(0),
        DBEXTENSION_PENDING_DOWNLOAD(1),
        DBEXTENSION_FOUNDNUM(2),
        DBEXTENSION_DOWNGRADEABLE_DBVERSION(3),
        DBEXTENSION_ONE_TIME_DIALOGS(4),
        DBEXTENSION_EMOJILRU(5),
        DBEXTENSION_POCKETQUERY_HISTORY(6),
        DBEXTENSION_TRACKFILES(7),
        DBEXTENSION_LAST_TRACKABLE_ACTION(8);

        public final int id;

        DBExtensionType(final int id) {
            this.id = id;
        }
    }

    private static final Func1<Cursor, String> GET_STRING_0 = cursor -> cursor.getString(0);

    private static final Func1<Cursor, Integer> GET_INTEGER_0 = cursor -> cursor.getInt(0);

    // Columns and indices for the cache data
    private static final String QUERY_CACHE_DATA =
            "SELECT " +
                    "cg_caches.updated," +  //  0
                    "cg_caches.reason," +  //  1 - unused column
                    "cg_caches.detailed," +  //  2
                    "cg_caches.detailedupdate," +  //  3
                    "cg_caches.visiteddate," +  //  4
                    "cg_caches.geocode," +  //  5
                    "cg_caches.cacheid," +  //  6
                    "cg_caches.guid," +  //  7
                    "cg_caches.type," +  //  8
                    "cg_caches.name," +  //  9
                    "cg_caches.owner," +  // 10
                    "cg_caches.owner_real," +  // 11
                    "cg_caches.hidden," +  // 12
                    "cg_caches.hint," +  // 13 - unused in this query -> lazyload
                    "cg_caches.size," +  // 14
                    "cg_caches.difficulty," +  // 15
                    "cg_caches.direction," +  // 16
                    "cg_caches.distance," +  // 17
                    "cg_caches.terrain," +  // 18
                    "cg_caches.location," +  // 19 - unused in this query -> lazyload
                    "cg_caches.personal_note," +  // 20
                    "cg_caches.shortdesc," +  // 21 - unused in this query -> lazyload
                    "cg_caches.favourite_cnt," +  // 22
                    "cg_caches.rating," +  // 23
                    "cg_caches.votes," +  // 24
                    "cg_caches.myvote," +  // 25
                    "cg_caches.disabled," +  // 26
                    "cg_caches.archived," +  // 27
                    "cg_caches.members," +  // 28
                    "cg_caches.found," +  // 29
                    "cg_caches.favourite," +  // 30
                    "cg_caches.inventoryunknown," +  // 31
                    "cg_caches.onWatchlist," +  // 32
                    "cg_caches.reliable_latlon," +  // 33 - unused column
                    "cg_caches.coordsChanged," +  // 34
                    "cg_caches.latitude," +  // 35
                    "cg_caches.longitude," +  // 36
                    "cg_caches.finalDefined," +  // 37
                    "cg_caches._id," +  // 38 - unused in this query
                    "cg_caches.inventorycoins," +  // 39 - unused column
                    "cg_caches.inventorytags," +  // 40 - unused column
                    "cg_caches.logPasswordRequired," +  // 41
                    "cg_caches.watchlistCount," +  // 42
                    "cg_caches.preventWaypointsFromNote," +  // 43
                    "cg_caches.owner_guid," +  // 44
                    "cg_caches.emoji," +       // 45
                    "cg_caches.alcMode";       // 46

    /**
     * The list of fields needed for mapping.
     */
    private static final String[] WAYPOINT_COLUMNS = {"_id", "geocode", "updated", "type", "prefix", "lookup", "name", "latitude", "longitude", "note", "own", "visited", "user_note", "org_coords_empty", "calc_state"};

    /**
     * Number of days (as ms) after temporarily saved caches are deleted
     */
    private static final long DAYS_AFTER_CACHE_IS_DELETED = 3 * 24 * 60 * 60 * 1000;

    /**
     * holds the column indexes of the cache table to avoid lookups
     */
    private static final CacheCache cacheCache = new CacheCache();
    private static volatile SQLiteDatabase database = null;
    private static final int dbVersion = 99;
    public static final int customListIdOffset = 10;

    /**
     * The following constant lists all DBVERSIONS whose changes with the previous version
     * are DOWNWARD-COMPATIBLE. More precisely: if a version x shows up in this list, then this
     * means that c:geo version written for DB version "x-1" can also work with this database.
     *
     * As a rule-of-thumb, a db version is downward compatible if:
     * * it only adds columns which are nullable or provide default values (so previous c:geo-versions don't fail on insert/update)
     * * it only adds tables which don't necessarily need an entry (because previous c:geo-versions will not write anything in there)
     * * migration from "x-1" to x in {@link DbHelper#onUpgrade(SQLiteDatabase, int, int)} is programmed such that it can handle it if later
     * db is "upgraded" again from "x-1" to x (this is usually the case if adding tables/columns will not fail if object already exists in db)
     *
     * The following changes usually make a DB change NOT downward compatible
     * * changing name, type or other attributes for a column
     * * adding columns which are not nullable
     * * any change which also requires some sort of data migration
     * * {@link DbHelper#onUpgrade(SQLiteDatabase, int, int)} will fail later if db is "upgraded" again from "x-1" to x
     */
    private static final Set<Integer> DBVERSIONS_DOWNWARD_COMPATIBLE = new HashSet<>(Arrays.asList(
            85, // adds offline logging columns/tables
            86, // (re)create indices on c_logs and c_logImages
            87, // adds service log id to logging tables
            88, // add timestamp to trail history
            89, // add altitude to trail history
            90, // add user guid to cg_caches and cg_logs
            91, // add fields to cg_extension
            92, // add emoji id to cg_caches
            93, // add emoji id to cg_lists
            94, // add scale to offline log images
            95, // add table to store custom filters
            96, // add preventAskForDeletion to cg_lists
            97, // rename ALC caches' geocodes from "LC" prefix to "AL" prefix
            98, // add table cg_variables to store cache variables
            99  // add alcMode to differentiate Linear vs Random
    ));

    @NonNull private static final String dbTableCaches = "cg_caches";
    @NonNull private static final String dbTableLists = "cg_lists";
    @NonNull private static final String dbTableCachesLists = "cg_caches_lists";
    @NonNull private static final String dbTableAttributes = "cg_attributes";
    @NonNull private static final String dbTableWaypoints = "cg_waypoints";
    @NonNull private static final String dbTableVariables = "cg_variables";
    @NonNull private static final String dbTableSpoilers = "cg_spoilers";
    @NonNull private static final String dbTableLogs = "cg_logs";
    @NonNull private static final String dbTableLogCount = "cg_logCount";
    @NonNull private static final String dbTableLogImages = "cg_logImages";
    @NonNull private static final String dbTableLogsOffline = "cg_logs_offline";
    @NonNull private static final String dbTableLogsOfflineImages = "cg_logs_offline_images";
    @NonNull private static final String dbTableLogsOfflineTrackables = "cg_logs_offline_trackables";
    @NonNull private static final String dbTableTrackables = "cg_trackables";
    @NonNull private static final String dbTableSearchDestinationHistory = "cg_search_destination_history";
    @NonNull private static final String dbTableTrailHistory = "cg_trail_history";
    @NonNull private static final String dbTableRoute = "cg_route";
    @NonNull private static final String dbTableExtension = "cg_extension";
    @NonNull private static final String dbTableFilters = "cg_filters";
    @NonNull private static final String dbTableSequences = "sqlite_sequence";
    @NonNull private static final String dbCreateCaches = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableCaches + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "updated LONG NOT NULL, "
            + "detailed INTEGER NOT NULL DEFAULT 0, "
            + "detailedupdate LONG, "
            + "visiteddate LONG, "
            + "geocode TEXT UNIQUE NOT NULL, "
            + "reason INTEGER NOT NULL DEFAULT 0, " // cached, favorite...
            + "cacheid TEXT, "
            + "guid TEXT, "
            + "type TEXT, "
            + "name TEXT, "
            + "owner TEXT, "
            + "owner_real TEXT, "
            + "hidden LONG, "
            + "hint TEXT, "
            + "size TEXT, "
            + "difficulty FLOAT, "
            + "terrain FLOAT, "
            + "location TEXT, "
            + "direction DOUBLE, "
            + "distance DOUBLE, "
            + "latitude DOUBLE, "
            + "longitude DOUBLE, "
            + "reliable_latlon INTEGER, "           // got unused while v96 - TODO should we remove the column?
            + "personal_note TEXT, "
            + "shortdesc TEXT, "
            + "description TEXT, "
            + "favourite_cnt INTEGER, "
            + "rating FLOAT, "
            + "votes INTEGER, "
            + "myvote FLOAT, "
            + "disabled INTEGER NOT NULL DEFAULT 0, "
            + "archived INTEGER NOT NULL DEFAULT 0, "
            + "members INTEGER NOT NULL DEFAULT 0, "
            + "found INTEGER NOT NULL DEFAULT 0, "
            + "favourite INTEGER NOT NULL DEFAULT 0, "
            + "inventorycoins INTEGER DEFAULT 0, "
            + "inventorytags INTEGER DEFAULT 0, "
            + "inventoryunknown INTEGER DEFAULT 0, "
            + "onWatchlist INTEGER DEFAULT 0, "
            + "coordsChanged INTEGER DEFAULT 0, "
            + "finalDefined INTEGER DEFAULT 0, "
            + "logPasswordRequired INTEGER DEFAULT 0,"
            + "watchlistCount INTEGER DEFAULT -1,"
            + "preventWaypointsFromNote INTEGER DEFAULT 0,"
            + "owner_guid TEXT NOT NULL DEFAULT '',"
            + "emoji INTEGER DEFAULT 0,"
            + "alcMode INTEGER DEFAULT 0"
            + "); ";
    private static final String dbCreateLists = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableLists + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "title TEXT NOT NULL, "
            + "updated LONG NOT NULL,"
            + "marker INTEGER NOT NULL,"            // unused from v93 on - TODO should we remove the column?
            + "emoji INTEGER DEFAULT 0,"
            + FIELD_LISTS_PREVENTASKFORDELETION + " INTEGER DEFAULT 0"
            + "); ";
    private static final String dbCreateCachesLists = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableCachesLists + " ("
            + "list_id INTEGER NOT NULL, "
            + "geocode TEXT NOT NULL, "
            + "PRIMARY KEY (list_id, geocode)"
            + "); ";
    private static final String dbCreateAttributes = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableAttributes + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "geocode TEXT NOT NULL, "
            + "updated LONG NOT NULL, " // date of save
            + "attribute TEXT "
            + "); ";

    private static final String dbCreateWaypoints = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableWaypoints + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "geocode TEXT NOT NULL, "
            + "updated LONG NOT NULL, " // date of save
            + "type TEXT NOT NULL DEFAULT 'waypoint', "
            + "prefix TEXT, "
            + "lookup TEXT, "
            + "name TEXT, "
            + "latitude DOUBLE, "
            + "longitude DOUBLE, "
            + "note TEXT, "
            + "own INTEGER DEFAULT 0, "
            + "visited INTEGER DEFAULT 0, "
            + "user_note TEXT, "
            + "org_coords_empty INTEGER DEFAULT 0, "
            + "calc_state TEXT"
            + "); ";

    private static final String dbCreateVariables = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableVariables + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "geocode TEXT NOT NULL, "
            + "varname TEXT, "
            + "varorder INTEGER DEFAULT 0, "
            + "formula TEXT"
            + "); ";

    private static final String dbCreateSpoilers = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableSpoilers + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "geocode TEXT NOT NULL, "
            + "updated LONG NOT NULL, " // date of save
            + "url TEXT, "
            + "title TEXT, "
            + "description TEXT "
            + "); ";
    private static final String dbCreateLogs = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableLogs + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "geocode TEXT NOT NULL, "
            + "service_log_id TEXT," //added with db version 86
            + "updated LONG NOT NULL, " // date of save
            + "type INTEGER NOT NULL DEFAULT 4, "
            + "author TEXT, "
            + "author_guid TEXT NOT NULL DEFAULT '', "
            + "log TEXT, "
            + "date LONG, "
            + "found INTEGER NOT NULL DEFAULT 0, "
            + "friend INTEGER "
            + "); ";

    private static final String dbCreateLogCount = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableLogCount + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "geocode TEXT NOT NULL, "
            + "updated LONG NOT NULL, " // date of save
            + "type INTEGER NOT NULL DEFAULT 4, "
            + "count INTEGER NOT NULL DEFAULT 0 "
            + "); ";
    private static final String dbCreateLogImages = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableLogImages + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "log_id INTEGER NOT NULL, "
            + "title TEXT NOT NULL, "
            + "url TEXT NOT NULL, "
            + "description TEXT "
            + "); ";
    private static final String dbCreateLogsOffline = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableLogsOffline + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "geocode TEXT NOT NULL, "
            + "updated LONG NOT NULL, " // date of save
            + "service_log_id TEXT," //added with db version 86
            + "type INTEGER NOT NULL DEFAULT 4, "
            + "log TEXT, "
            + "date LONG, "
            + "report_problem TEXT, "
            //new for version 85
            + "image_title_prefix TEXT, "
            + "image_scale INTEGER, "
            + "favorite INTEGER, "
            + "rating FLOAT, "
            + "password TEXT, "
            + "tweet INTEGER"
            + "); ";
    private static final String dbCreateLogsOfflineImages = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableLogsOfflineImages + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "logoffline_id INTEGER NOT NULL, "
            + "url TEXT NOT NULL, "
            + "title TEXT, "
            + "description TEXT, "
            + "scale INTEGER"
            + "); ";
    private static final String dbCreateLogsOfflineTrackables = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableLogsOfflineTrackables + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "logoffline_id INTEGER NOT NULL, "
            + "tbcode TEXT NOT NULL, "
            + "actioncode INTEGER "
            + "); ";

    private static final String dbCreateTrackables = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableTrackables + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "updated LONG NOT NULL, " // date of save
            + "tbcode TEXT NOT NULL, "
            + "guid TEXT, "
            + "title TEXT, "
            + "owner TEXT, "
            + "released LONG, "
            + "goal TEXT, "
            + "description TEXT, "
            + "geocode TEXT, "
            + "log_date LONG, "
            + "log_type INTEGER, "
            + "log_guid TEXT "
            + "); ";

    private static final String dbCreateSearchDestinationHistory = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableSearchDestinationHistory + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "date LONG NOT NULL, "
            + "latitude DOUBLE, "
            + "longitude DOUBLE "
            + "); ";

    private static final String dbCreateTrailHistory
            = "CREATE TABLE IF NOT EXISTS " + dbTableTrailHistory + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "latitude DOUBLE, "
            + "longitude DOUBLE, "
            + "altitude DOUBLE, "
            + "timestamp LONG"
            + "); ";

    private static final String dbCreateRoute
            = "CREATE TABLE IF NOT EXISTS " + dbTableRoute + " ("
            + "precedence INTEGER, "
            + "type INTEGER, "
            + "id TEXT, "
            + "latitude DOUBLE, "
            + "longitude DOUBLE "
            + "); ";

    @SuppressWarnings("SyntaxError")
    private static final String dbCreateExtension
            = "CREATE TABLE IF NOT EXISTS " + dbTableExtension + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "_type INTEGER DEFAULT " + DBEXTENSION_INVALID.id + ", "
            + "_key VARCHAR(50), "
            + "long1 INTEGER DEFAULT 0, "
            + "long2 INTEGER DEFAULT 0, "
            + "long3 INTEGER DEFAULT 0, "
            + "long4 INTEGER DEFAULT 0, "
            + "string1 TEXT, "
            + "string2 TEXT,"
            + "string3 TEXT,"
            + "string4 TEXT"
            + "); ";

    private static final String dbCreateFilters
            = "CREATE TABLE IF NOT EXISTS " + dbTableFilters + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "name TEXT NOT NULL UNIQUE, "
            + "treeconfig TEXT"
            + "); ";

    // reminder to myself: when adding a new CREATE TABLE statement:
    // make sure to add it to both onUpgrade() and onCreate()

    public static int getExpectedDBVersion() {
        return dbVersion;
    }

    public static int getActualDBVersion() {
        init();
        return database == null ? -1 : database.getVersion();

    }

    public static boolean versionsAreCompatible(final SQLiteDatabase databaseToCheck, final int oldVersion, final int newVersion) {
        if (newVersion < oldVersion) {
            final Set<Integer> downgradeableVersions = DBDowngradeableVersions.load(databaseToCheck);
            for (int version = oldVersion; version > newVersion; version--) {
                if (!downgradeableVersions.contains(version)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static class DBExtension {

        // reflect actual database schema (+ type param)
        protected long id;
        protected String key;
        protected long long1;
        protected long long2;
        protected long long3;
        protected long long4;
        protected String string1;
        protected String string2;
        protected String string3;
        protected String string4;

        protected DBExtension() {
            // utility class
        }

        /**
         * internal constructor for database queries
         */
        protected DBExtension(final long id, final String key, final long long1, final long long2, final long long3, final long long4, final String string1, final String string2, final String string3, final String string4) {
            this.id = id;
            this.key = key;
            this.long1 = long1;
            this.long2 = long2;
            this.long3 = long3;
            this.long4 = long4;
            this.string1 = string1;
            this.string2 = string2;
            this.string3 = string3;
            this.string4 = string4;
        }

        /**
         * public copy constructor
         */
        public DBExtension(final DataStore.DBExtension copyFrom) {
            this.id = copyFrom.getId();
            this.key = copyFrom.getKey();
            this.long1 = copyFrom.getLong1();
            this.long2 = copyFrom.getLong2();
            this.long3 = copyFrom.getLong3();
            this.long4 = copyFrom.getLong4();
            this.string1 = copyFrom.getString1();
            this.string2 = copyFrom.getString2();
            this.string3 = copyFrom.getString3();
            this.string4 = copyFrom.getString4();
        }

        /**
         * get the first entry for this key
         */
        @Nullable
        protected static DBExtension load(final DBExtensionType type, @NonNull final String key) {
            if (!init(false)) {
                return null;
            }
            return load(database, type, key);
        }

        @Nullable
        protected static DBExtension load(final SQLiteDatabase db, final DBExtensionType type, @NonNull final String key) {
            checkState(type, key, false);
            try (Cursor cursor = db.query(dbTableExtension,
                    new String[]{"_id", "_key", "long1", "long2", "long3", "long4", "string1", "string2", "string3", "string4"},
                    "_type = ? AND _key LIKE ?",
                    new String[]{String.valueOf(type.id), key},
                    null, null, "_id", "1")) {
                if (cursor.moveToNext()) {
                    return new DBExtension(cursor.getLong(0), cursor.getString(1), cursor.getLong(2), cursor.getLong(3), cursor.getLong(4), cursor.getLong(5), cursor.getString(6), cursor.getString(7), cursor.getString(8), cursor.getString(9));
                }
            }
            return null;
        }

        /**
         * get a list of all entries for this key (if key != null) / for this type (if key is null)
         */
        protected static ArrayList<DBExtension> getAll(final DBExtensionType type, @Nullable final String key) {
            init();
            return getAll(database, type, key);
        }

        /**
         * get a list of all entries for this key (if key != null) / for this type (if key is null)
         */
        protected static ArrayList<DBExtension> getAll(final SQLiteDatabase db, final DBExtensionType type, @Nullable final String key) {
            checkState(type, key, true);
            final ArrayList<DBExtension> result = new ArrayList<>();
            try (Cursor cursor = db.query(dbTableExtension,
                    new String[]{"_id", "_key", "long1", "long2", "long3", "long4", "string1", "string2", "string3", "string4"},
                    "_type = ?" + (null == key ? "" : " AND _key LIKE ?"),
                    null == key ? new String[]{String.valueOf(type.id)} : new String[]{String.valueOf(type.id), key},
                    null, null, "_id", null)) {
                while (cursor.moveToNext()) {
                    result.add(new DBExtension(cursor.getLong(0), cursor.getString(1), cursor.getLong(2), cursor.getLong(3), cursor.getLong(4), cursor.getLong(5), cursor.getString(6), cursor.getString(7), cursor.getString(8), cursor.getString(9)));
                }
            }
            return result;
        }

        /**
         * adds a new entry to database
         */
        protected static DBExtension add(final DBExtensionType type, final String key, final long long1, final long long2, final long long3, final long long4, final String string1, final String string2, final String string3, final String string4) {
            if (!init(false)) {
                return null;
            }
            return add(database, type, key, long1, long2, long3, long4, string1, string2, string3, string4);
        }

        protected static DBExtension add(final SQLiteDatabase db, final DBExtensionType type, final String key, final long long1, final long long2, final long long3, final long long4, final String string1, final String string2, final String string3, final String string4) {
            try {
                final long id = db.insert(dbTableExtension, null, toValues(type, key, long1, long2, long3, long4, string1, string2, string3, string4));
                return new DBExtension(id, key, long1, long2, long3, long4, string1, string2, string3, string4);
            } catch (final Exception e) {
                Log.e("DBExtension.add failed", e);
            }
            return null;
        }

        private static ContentValues toValues(final DBExtensionType type, final String key, final long long1, final long long2, final long long3, final long long4, final String string1, final String string2, final String string3, final String string4) {
            final ContentValues cv = new ContentValues();
            cv.put("_type", String.valueOf(type.id));
            cv.put("_key", key);
            cv.put("long1", long1);
            cv.put("long2", long2);
            cv.put("long3", long3);
            cv.put("long4", long4);
            cv.put("string1", string1);
            cv.put("string2", string2);
            cv.put("string3", string3);
            cv.put("string4", string4);
            return cv;
        }

        /**
         * removes all elements with this key from database
         */
        public static void removeAll(final DBExtensionType type, final String key) {
            if (!init(false)) {
                return;
            }
            removeAll(database, type, key);
        }

        public static void removeAll(final SQLiteDatabase db, final DBExtensionType type, final String key) {
            checkState(type, key, false);
            db.delete(dbTableExtension, "_type = ? AND _key LIKE ?", new String[]{String.valueOf(type.id), key});
        }

        private static void checkState(final DBExtensionType type, @Nullable final String key, final boolean nullable) {
            if (type == DBEXTENSION_INVALID) {
                throw new IllegalStateException("DBExtension: type must be set to valid type");
            }
            if (!StringUtils.isNotBlank(key) && !(nullable && null == key)) {
                throw new IllegalStateException("DBExtension: key value must be set");
            }
        }

        public long getId() {
            return id;
        }

        public String getKey() {
            return key;
        }

        public long getLong1() {
            return long1;
        }

        public long getLong2() {
            return long2;
        }

        public long getLong3() {
            return long3;
        }

        public long getLong4() {
            return long4;
        }

        public String getString1() {
            return string1;
        }

        public String getString2() {
            return string2;
        }

        public String getString3() {
            return string3;
        }

        public String getString4() {
            return string4;
        }

    }

    public static class DBFilters {

        public static List<GeocacheFilter> getAllStoredFilters() {
            return queryToColl(dbTableFilters, new String[]{"name", "treeconfig"},
                    null, null, null, null, new ArrayList<>(),
                    c -> GeocacheFilter.createFromConfig(c.getString(0), c.getString(1)));
        }

        /**
         * Saves using UPSERT on NAME (if filter with same name exists, it deleted before.  otherwise new one is created)
         */
        public static int save(final GeocacheFilter filter) {
            delete(filter.getName());
            final ContentValues values = new ContentValues();
            values.put("name", filter.getName());
            values.put("treeconfig", filter.toConfig());
            return (int) database.insert(dbTableFilters, null, values);
        }

        /**
         * deletes any entry in DB with same filterName as in supplied filter object, if exists
         */
        public static boolean delete(final String filterName) {
            return database.delete(dbTableFilters, "name = ?", new String[]{filterName}) > 0;
        }

    }

    private DataStore() {
        // utility class
    }

    private static final Single<Integer> allCachesCountObservable = Single.create((SingleOnSubscribe<Integer>) emitter -> {
        try {
            if (isInitialized()) {
                emitter.onSuccess(getAllCachesCount());
            }
        } catch (RuntimeException re) {
            emitter.onError(re);
        }
    }).timeout(500, TimeUnit.MILLISECONDS).retry(10).subscribeOn(Schedulers.io());

    private static boolean newlyCreatedDatabase = false;
    private static boolean databaseCleaned = false;

    public static void init() {
        init(true);
    }

    public static boolean init(final boolean force) {
        return initAndCheck(force) == null;
    }

    /**
     * checks and inits database if not already initialized. Returns null if everything is fine
     * In case of error on init:
     * * when force=true throws exception
     * * when force=false returns error message (guaranteed to be non-null)
     */
    @Nullable
    public static String initAndCheck(final boolean force) {
        if (database != null) {
            return null;
        }

        try (ContextLogger ignore = new ContextLogger(true, "DataStore.init")) {
            synchronized (DataStore.class) {
                if (database != null) {
                    return null;
                }
                final DbHelper dbHelper = new DbHelper(new DBContext(CgeoApplication.getInstance()));
                try {
                    database = dbHelper.getWritableDatabase();
                } catch (final Exception e) {
                    Log.e("DataStore.init: unable to open database for R/W", e);
                    final String recreateErrorMsg = recreateDatabase(dbHelper);
                    if (recreateErrorMsg != null) {
                        //if we land here we could neither open the DB nor could we recreate it and database remains null
                        //=> failfast here by rethrowing original exception. This might give us better error analysis possibilities
                        final String msg = "DataStore.init: Unrecoverable problem opening database ('" +
                                databasePath().getAbsolutePath() + "')(recreate error: " + recreateErrorMsg + ")";
                        if (force) {
                            Log.e(msg, e);
                            throw new RuntimeException(msg, e);
                        }
                        Log.w(msg, e);
                        return msg + ": " + e.getMessage();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Attempt to recreate the database if opening has failed
     *
     * @param dbHelper dbHelper to use to reopen the database
     * @return null if everything is ok, error message otherwise
     */
    private static String recreateDatabase(final DbHelper dbHelper) {
        final File dbPath = databasePath();
        final Uri uri = ContentStorage.get().writeFileToFolder(PersistableFolder.BACKUP, FileNameCreator.forName(dbPath.getName() + DB_FILE_CORRUPTED_EXTENSION), dbPath, false);
        if (uri != null) {
            Log.i("DataStore.init: renamed " + dbPath + " into " + uri.getPath());
        } else {
            Log.e("DataStore.init: unable to move corrupted database");
        }
        try {
            database = dbHelper.getWritableDatabase();
            return null;
        } catch (final Exception f) {
            final String msg = "DataStore.init: unable to recreate database and open it for R/W";
            Log.w(msg, f);
            return msg + ": " + f.getMessage();
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

    public static Uri backupDatabaseInternal(final Folder backupDir) {

        closeDb();
        final Uri uri = ContentStorage.get().copy(Uri.fromFile(databasePath()), backupDir, FileNameCreator.forName(DB_FILE_NAME_BACKUP), false);
        init();

        if (uri == null) {
            Log.e("Database could not be copied to " + backupDir.toUserDisplayableString());
            return null;
        }
        Log.i("Database was copied to " + backupDir.toUserDisplayableString());
        return uri;
    }

    /**
     * Move the database to/from external cgdata in a new thread,
     * showing a progress window
     */
    public static void moveDatabase(final Activity fromActivity) {
        final ProgressDialog dialog = ProgressDialog.show(fromActivity, fromActivity.getString(R.string.init_dbmove_dbmove), fromActivity.getString(R.string.init_dbmove_running), true, false);
        AndroidRxUtils.bindActivity(fromActivity, Observable.defer(() -> {
            if (!LocalStorage.isExternalStorageAvailable()) {
                Log.w("Database was not moved: external memory not available");
                return Observable.just(false);
            }
            closeDb();

            final File source = databasePath();
            final File target = databaseAlternatePath();
            if (!FileUtils.copy(source, target)) {
                Log.e("Database could not be moved to " + target);
                init();
                return Observable.just(false);
            }
            if (!FileUtils.delete(source)) {
                Log.e("Original database could not be deleted during move");
            }
            Settings.setDbOnSDCard(!Settings.isDbOnSDCard());
            Log.i("Database was moved to " + target);

            init();
            return Observable.just(true);
        }).subscribeOn(Schedulers.io())).subscribe(success -> {
            dialog.dismiss();
            final String message = success ? fromActivity.getString(R.string.init_dbmove_success) : fromActivity.getString(R.string.init_dbmove_failed);
            SimpleDialog.of(fromActivity).setTitle(R.string.init_dbmove_dbmove).setMessage(TextParam.text(message)).show();
        });
    }

    @NonNull
    private static File databasePath(final boolean internal) {
        return new File(internal ? LocalStorage.getInternalDbDirectory() : LocalStorage.getExternalDbDirectory(), DB_FILE_NAME);
    }

    @NonNull
    public static File databasePath() {
        return databasePath(!Settings.isDbOnSDCard());
    }

    @NonNull
    private static File databaseAlternatePath() {
        return databasePath(Settings.isDbOnSDCard());
    }

    public static String restoreDatabaseInternal(final Context context, final Uri databaseUri) {

        final File tmpFile = ContentStorage.get().writeUriToTempFile(databaseUri, "backup_db.tmp");
        DBRestoreResult result = DBRestoreResult.RESTORE_FAILED_GENERAL;
        try {
            final SQLiteDatabase backup = SQLiteDatabase.openDatabase(tmpFile.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            final int backupDbVersion = backup.getVersion();
            final int expectedDbVersion = DataStore.getExpectedDBVersion();
            if (!DataStore.versionsAreCompatible(backup, backupDbVersion, expectedDbVersion)) {
                return String.format(context.getString(R.string.init_restore_version_error), expectedDbVersion, backupDbVersion);
            }
            closeDb();
            result = FileUtils.copy(tmpFile, databasePath()) ? DBRestoreResult.RESTORE_SUCCESSFUL : DBRestoreResult.RESTORE_FAILED_GENERAL;
            init();
            if (newlyCreatedDatabase) {
                result = DBRestoreResult.RESTORE_FAILED_DBRECREATED;
                Log.e("restored DB seems to be corrupt, needed to recreate database from scratch");
            }
            if (result == DBRestoreResult.RESTORE_SUCCESSFUL) {
                Log.i("Database successfully restored from " + tmpFile.getPath());
            } else {
                Log.e("Could not restore database from " + tmpFile.getPath());
            }
        } catch (SQLiteException e) {
            Log.e("error while restoring database: ", e);
        } finally {
            tmpFile.delete();
        }
        return context.getString(result.res);
    }

    private static class DBContext extends ContextWrapper {

        DBContext(final Context base) {
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
        public static final int MAX_TRAILHISTORY_LENGTH = getMaximumMapTrailLength();

        DbHelper(final Context context) {
            super(context, databasePath().getPath(), null, dbVersion);
        }

        @Override
        public void onCreate(final SQLiteDatabase db) {
            newlyCreatedDatabase = true;
            db.execSQL(dbCreateCaches);
            db.execSQL(dbCreateLists);
            db.execSQL(dbCreateCachesLists);
            db.execSQL(dbCreateAttributes);
            db.execSQL(dbCreateWaypoints);
            db.execSQL(dbCreateVariables);
            db.execSQL(dbCreateSpoilers);
            db.execSQL(dbCreateLogs);
            db.execSQL(dbCreateLogCount);
            db.execSQL(dbCreateLogImages);
            db.execSQL(dbCreateLogsOffline);
            db.execSQL(dbCreateLogsOfflineImages);
            db.execSQL(dbCreateLogsOfflineTrackables);
            db.execSQL(dbCreateTrackables);
            db.execSQL(dbCreateSearchDestinationHistory);
            db.execSQL(dbCreateTrailHistory);
            db.execSQL(dbCreateRoute);
            db.execSQL(dbCreateExtension);
            db.execSQL(dbCreateFilters);

            createIndices(db, dbVersion);
        }

        private static void createIndices(final SQLiteDatabase db, final int currentVersion) {
            db.execSQL("CREATE INDEX IF NOT EXISTS in_caches_geo ON " + dbTableCaches + " (geocode)");
            db.execSQL("CREATE INDEX IF NOT EXISTS in_caches_guid ON " + dbTableCaches + " (guid)");
            db.execSQL("CREATE INDEX IF NOT EXISTS in_caches_lat ON " + dbTableCaches + " (latitude)");
            db.execSQL("CREATE INDEX IF NOT EXISTS in_caches_lon ON " + dbTableCaches + " (longitude)");
            db.execSQL("CREATE INDEX IF NOT EXISTS in_caches_reason ON " + dbTableCaches + " (reason)");
            db.execSQL("CREATE INDEX IF NOT EXISTS in_caches_detailed ON " + dbTableCaches + " (detailed)");
            db.execSQL("CREATE INDEX IF NOT EXISTS in_caches_type ON " + dbTableCaches + " (type)");
            db.execSQL("CREATE INDEX IF NOT EXISTS in_caches_visit_detail ON " + dbTableCaches + " (visiteddate, detailedupdate)");
            db.execSQL("CREATE INDEX IF NOT EXISTS in_attr_geo ON " + dbTableAttributes + " (geocode)");
            db.execSQL("CREATE INDEX IF NOT EXISTS in_wpts_geo ON " + dbTableWaypoints + " (geocode)");
            db.execSQL("CREATE INDEX IF NOT EXISTS in_wpts_geo_type ON " + dbTableWaypoints + " (geocode, type)");
            db.execSQL("CREATE INDEX IF NOT EXISTS in_spoil_geo ON " + dbTableSpoilers + " (geocode)");
            db.execSQL("CREATE INDEX IF NOT EXISTS in_logs_geo ON " + dbTableLogs + " (geocode, date desc)");
            if (currentVersion >= 54) {
                db.execSQL("CREATE INDEX IF NOT EXISTS in_logimagess_logid ON " + dbTableLogImages + " (log_id)");
            }
            db.execSQL("CREATE INDEX IF NOT EXISTS in_logcount_geo ON " + dbTableLogCount + " (geocode)");
            db.execSQL("CREATE INDEX IF NOT EXISTS in_logsoff_geo ON " + dbTableLogsOffline + " (geocode)");
            if (currentVersion >= 85) {
                db.execSQL("CREATE INDEX IF NOT EXISTS in_logsoffimages_geo ON " + dbTableLogsOfflineImages + " (logoffline_id)");
                db.execSQL("CREATE INDEX IF NOT EXISTS in_logsofftrackables_geo ON " + dbTableLogsOfflineTrackables + " (logoffline_id)");
            }
            db.execSQL("CREATE INDEX IF NOT EXISTS in_trck_geo ON " + dbTableTrackables + " (geocode)");
            db.execSQL("CREATE INDEX IF NOT EXISTS in_lists_geo ON " + dbTableCachesLists + " (geocode)");
            if (currentVersion >= 82) {
                db.execSQL("CREATE INDEX IF NOT EXISTS in_extension_key ON " + dbTableExtension + " (_key)");
            }
            if (currentVersion >= 98) {
                db.execSQL("CREATE INDEX IF NOT EXISTS in_vars_geo ON " + dbTableVariables + " (geocode)");
            }
        }

        @Override
        public void onDowngrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
            Log.iForce("[DB] Request to downgrade database from ver. " + oldVersion + " to ver. " + newVersion + ": start");

            //ask the database for a list of downgradeable DB versions AT THE TIME THIS DB WAS LAST UPGRADED
            //(which might be later than the current code version was written)
            final Set<Integer> downgradeableVersions = DBDowngradeableVersions.load(db);

            //allow downgrade if, and only if, all versions between oldVersion and newVersion are marked as "downward-compatible"
            for (int version = oldVersion; version > newVersion; version--) {
                if (!downgradeableVersions.contains(version)) {
                    throw new SQLiteException("Can't downgrade database from version " + oldVersion + " to " + newVersion +
                            ": " + version + " is not downward compatible");
                }
            }
            Log.iForce("[DB] Downgrade database from ver. " + oldVersion + " to ver. " + newVersion + ": allowed");
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
            Log.iForce("[DB] Upgrade database from ver. " + oldVersion + " to ver. " + newVersion + ": start");

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
                    if (oldVersion < 52) { // upgrade to 52
                        try {
                            db.execSQL(dbCreateSearchDestinationHistory);

                            Log.i("Added table " + dbTableSearchDestinationHistory + ".");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 52);
                        }
                    }

                    if (oldVersion < 53) { // upgrade to 53
                        try {
                            db.execSQL("ALTER TABLE " + dbTableCaches + " ADD COLUMN onWatchlist INTEGER");

                            Log.i("Column onWatchlist added to " + dbTableCaches + ".");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 53);
                        }
                    }

                    if (oldVersion < 54) { // update to 54
                        try {
                            db.execSQL(dbCreateLogImages);
                        } catch (final SQLException e) {
                            onUpgradeError(e, 54);
                        }
                    }

                    if (oldVersion < 55) { // update to 55
                        try {
                            db.execSQL("ALTER TABLE " + dbTableCaches + " ADD COLUMN personal_note TEXT");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 55);
                        }
                    }

                    // make all internal attribute names lowercase
                    // @see issue #299
                    if (oldVersion < 56) { // update to 56
                        try {
                            db.execSQL("UPDATE " + dbTableAttributes + " SET attribute = " +
                                    "LOWER(attribute) WHERE attribute LIKE \"%_yes\" " +
                                    "OR  attribute LIKE \"%_no\"");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 56);
                        }
                    }

                    // Create missing indices. See issue #435
                    if (oldVersion < 57) { // update to 57
                        try {
                            db.execSQL("DROP INDEX in_a");
                            db.execSQL("DROP INDEX in_b");
                            db.execSQL("DROP INDEX in_c");
                            db.execSQL("DROP INDEX in_d");
                            db.execSQL("DROP INDEX in_e");
                            db.execSQL("DROP INDEX in_f");
                            createIndices(db, 57);
                        } catch (final SQLException e) {
                            onUpgradeError(e, 57);
                        }
                    }

                    if (oldVersion < 58) { // upgrade to 58
                        try {
                            db.beginTransaction();

                            final String dbTableCachesTemp = dbTableCaches + "_temp";
                            final String dbCreateCachesTemp = ""
                                    + "CREATE TABLE " + dbTableCachesTemp + " ("
                                    + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                                    + "updated LONG NOT NULL, "
                                    + "detailed INTEGER NOT NULL DEFAULT 0, "
                                    + "detailedupdate LONG, "
                                    + "visiteddate LONG, "
                                    + "geocode TEXT UNIQUE NOT NULL, "
                                    + "reason INTEGER NOT NULL DEFAULT 0, "
                                    + "cacheid TEXT, "
                                    + "guid TEXT, "
                                    + "type TEXT, "
                                    + "name TEXT, "
                                    + "own INTEGER NOT NULL DEFAULT 0, "
                                    + "owner TEXT, "
                                    + "owner_real TEXT, "
                                    + "hidden LONG, "
                                    + "hint TEXT, "
                                    + "size TEXT, "
                                    + "difficulty FLOAT, "
                                    + "terrain FLOAT, "
                                    + "location TEXT, "
                                    + "direction DOUBLE, "
                                    + "distance DOUBLE, "
                                    + "latitude DOUBLE, "
                                    + "longitude DOUBLE, "
                                    + "reliable_latlon INTEGER, "
                                    + "personal_note TEXT, "
                                    + "shortdesc TEXT, "
                                    + "description TEXT, "
                                    + "favourite_cnt INTEGER, "
                                    + "rating FLOAT, "
                                    + "votes INTEGER, "
                                    + "myvote FLOAT, "
                                    + "disabled INTEGER NOT NULL DEFAULT 0, "
                                    + "archived INTEGER NOT NULL DEFAULT 0, "
                                    + "members INTEGER NOT NULL DEFAULT 0, "
                                    + "found INTEGER NOT NULL DEFAULT 0, "
                                    + "favourite INTEGER NOT NULL DEFAULT 0, "
                                    + "inventorycoins INTEGER DEFAULT 0, "
                                    + "inventorytags INTEGER DEFAULT 0, "
                                    + "inventoryunknown INTEGER DEFAULT 0, "
                                    + "onWatchlist INTEGER DEFAULT 0 "
                                    + "); ";

                            db.execSQL(dbCreateCachesTemp);
                            db.execSQL("INSERT INTO " + dbTableCachesTemp + " SELECT _id,updated,detailed,detailedupdate,visiteddate,geocode,reason,cacheid,guid,type,name,own,owner,owner_real," +
                                    "hidden,hint,size,difficulty,terrain,location,direction,distance,latitude,longitude, 0," +
                                    "personal_note,shortdesc,description,favourite_cnt,rating,votes,myvote,disabled,archived,members,found,favourite,inventorycoins," +
                                    "inventorytags,inventoryunknown,onWatchlist FROM " + dbTableCaches);
                            db.execSQL("DROP TABLE " + dbTableCaches);
                            db.execSQL("ALTER TABLE " + dbTableCachesTemp + " RENAME TO " + dbTableCaches);

                            final String dbTableWaypointsTemp = dbTableWaypoints + "_temp";
                            final String dbCreateWaypointsTemp = ""
                                    + "CREATE TABLE " + dbTableWaypointsTemp + " ("
                                    + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                                    + "geocode TEXT NOT NULL, "
                                    + "updated LONG NOT NULL, " // date of save
                                    + "type TEXT NOT NULL DEFAULT 'waypoint', "
                                    + "prefix TEXT, "
                                    + "lookup TEXT, "
                                    + "name TEXT, "
                                    + "latitude DOUBLE, "
                                    + "longitude DOUBLE, "
                                    + "note TEXT "
                                    + "); ";
                            db.execSQL(dbCreateWaypointsTemp);
                            db.execSQL("INSERT INTO " + dbTableWaypointsTemp + " SELECT _id, geocode, updated, type, prefix, lookup, name, latitude, longitude, note FROM " + dbTableWaypoints);
                            db.execSQL("DROP TABLE " + dbTableWaypoints);
                            db.execSQL("ALTER TABLE " + dbTableWaypointsTemp + " RENAME TO " + dbTableWaypoints);

                            createIndices(db, 58);

                            db.setTransactionSuccessful();

                            Log.i("Removed latitude_string and longitude_string columns");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 58);
                        } finally {
                            db.endTransaction();
                        }
                    }

                    if (oldVersion < 59) {
                        try {
                            // Add new indices and remove obsolete cache files
                            createIndices(db, 59);
                            removeObsoleteGeocacheDataDirectories();
                        } catch (final SQLException e) {
                            onUpgradeError(e, 59);
                        }
                    }

                    if (oldVersion < 60) {
                        try {
                            removeSecEmptyDirs();
                        } catch (final SQLException e) {
                            onUpgradeError(e, 60);
                        }
                    }
                    if (oldVersion < 61) {
                        try {
                            db.execSQL("ALTER TABLE " + dbTableLogs + " ADD COLUMN friend INTEGER");
                            db.execSQL("ALTER TABLE " + dbTableCaches + " ADD COLUMN coordsChanged INTEGER DEFAULT 0");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 61);
                        }
                    }
                    // Introduces finalDefined on caches and own on waypoints
                    if (oldVersion < 62) {
                        try {
                            db.execSQL("ALTER TABLE " + dbTableCaches + " ADD COLUMN finalDefined INTEGER DEFAULT 0");
                            db.execSQL("ALTER TABLE " + dbTableWaypoints + " ADD COLUMN own INTEGER DEFAULT 0");
                            db.execSQL("UPDATE " + dbTableWaypoints + " SET own = 1 WHERE type = 'own'");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 62);
                        }
                    }
                    if (oldVersion < 63) {
                        try {
                            removeDoubleUnderscoreMapFiles();
                        } catch (final SQLException e) {
                            onUpgradeError(e, 63);
                        }
                    }

                    if (oldVersion < 64) {
                        try {
                            // No cache should ever be stored into the ALL_CACHES list. Here we use hardcoded list ids
                            // rather than symbolic ones because the fix must be applied with the values at the time
                            // of the problem. The problem was introduced in release 2012.06.01.
                            db.execSQL("UPDATE " + dbTableCaches + " SET reason=1 WHERE reason=2");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 64);
                        }
                    }

                    if (oldVersion < 65) {
                        try {
                            // Set all waypoints where name is Original coordinates to type ORIGINAL
                            db.execSQL("UPDATE " + dbTableWaypoints + " SET type='original', own=0 WHERE name='Original Coordinates'");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 65);
                        }
                    }
                    // Introduces visited feature on waypoints
                    if (oldVersion < 66) {
                        try {
                            db.execSQL("ALTER TABLE " + dbTableWaypoints + " ADD COLUMN visited INTEGER DEFAULT 0");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 66);
                        }
                    }
                    // issue2662 OC: Leichtes Klettern / Easy climbing
                    if (oldVersion < 67) {
                        try {
                            db.execSQL("UPDATE " + dbTableAttributes + " SET attribute = 'easy_climbing_yes' WHERE geocode LIKE 'OC%' AND attribute = 'climbing_yes'");
                            db.execSQL("UPDATE " + dbTableAttributes + " SET attribute = 'easy_climbing_no' WHERE geocode LIKE 'OC%' AND attribute = 'climbing_no'");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 67);
                        }
                    }
                    // Introduces logPasswordRequired on caches
                    if (oldVersion < 68) {
                        try {
                            db.execSQL("ALTER TABLE " + dbTableCaches + " ADD COLUMN logPasswordRequired INTEGER DEFAULT 0");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 68);
                        }
                    }
                    // description for log Images
                    if (oldVersion < 69) {
                        try {
                            db.execSQL("ALTER TABLE " + dbTableLogImages + " ADD COLUMN description TEXT");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 69);
                        }
                    }
                    // Introduces watchListCount
                    if (oldVersion < 70) {
                        try {
                            db.execSQL("ALTER TABLE " + dbTableCaches + " ADD COLUMN watchlistCount INTEGER DEFAULT -1");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 70);
                        }
                    }
                    // Introduces cachesLists
                    if (oldVersion < 71) {
                        try {
                            db.execSQL(dbCreateCachesLists);
                            createIndices(db, 71);
                            db.execSQL("INSERT INTO " + dbTableCachesLists + " SELECT reason, geocode FROM " + dbTableCaches);
                        } catch (final SQLException e) {
                            onUpgradeError(e, 71);
                        }
                    }
                    // User notes in waypoints and local coords changes of WPs without coords on server
                    if (oldVersion < 72) {
                        try {
                            db.execSQL("ALTER TABLE " + dbTableWaypoints + " ADD COLUMN user_note TEXT");
                            db.execSQL("ALTER TABLE " + dbTableWaypoints + " ADD COLUMN org_coords_empty INTEGER DEFAULT 0");
                            db.execSQL("UPDATE " + dbTableWaypoints + " SET user_note = note");
                            db.execSQL("UPDATE " + dbTableWaypoints + " SET note = ''");
                            db.execSQL("UPDATE " + dbTableWaypoints + " SET org_coords_empty = 1 WHERE latitude IS NULL AND longitude IS NULL");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 72);
                        }
                    }
                    // Adds coord calculator state to the waypoint
                    if (oldVersion < 73) {
                        try {
                            db.execSQL("ALTER TABLE " + dbTableWaypoints + " ADD COLUMN calc_state TEXT");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 73);
                        }
                    }

                    // Adds report problem to offline log
                    if (oldVersion < 74) {
                        try {
                            db.execSQL("ALTER TABLE " + dbTableLogsOffline + " ADD COLUMN report_problem TEXT");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 74);
                        }
                    }

                    // Adds log information for trackables
                    if (oldVersion < 75) {
                        try {
                            db.execSQL("ALTER TABLE " + dbTableTrackables + " ADD COLUMN log_date LONG");
                            db.execSQL("ALTER TABLE " + dbTableTrackables + " ADD COLUMN log_type INTEGER");
                            db.execSQL("ALTER TABLE " + dbTableTrackables + " ADD COLUMN log_guid TEXT");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 75);
                        }
                    }

                    // add trail history table
                    if (oldVersion < 76) {
                        try {
                            db.execSQL(dbCreateTrailHistory);

                            Log.i("Added table " + dbTableTrailHistory + ".");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 76);
                        }
                    }

                    // add column for list marker
                    if (oldVersion < 77) {
                        try {
                            db.execSQL("ALTER TABLE " + dbTableLists + " ADD COLUMN marker INTEGER NOT NULL DEFAULT 0");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 77);
                        }
                    }

                    // 78 has been revoked

                    // next one is needed only for users with version 78, therefore using "==" instead of the regular "<"
                    if (oldVersion == 78) {
                        try {
                            db.execSQL("CREATE TABLE IF NOT EXISTS " + dbTableSearchDestinationHistory + " ("
                                    + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                                    + "date LONG NOT NULL, "
                                    + "latitude DOUBLE, "
                                    + "longitude DOUBLE "
                                    + ")");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 79);
                        }
                    }

                    if (oldVersion < 80) {
                        try {
                            db.execSQL("ALTER TABLE " + dbTableCaches + " ADD COLUMN preventWaypointsFromNote INTEGER NOT NULL DEFAULT 0");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 80);
                        }
                    }

                    // add route table
                    if (oldVersion < 81) {
                        try {
                            db.execSQL(dbCreateRoute);

                            Log.i("Added table " + dbTableRoute + ".");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 81);
                        }
                    }

                    // add extension table
                    if (oldVersion < 82) {
                        try {
                            db.execSQL(dbCreateExtension);

                            Log.i("Added table " + dbTableExtension + ".");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 82);
                        }
                    }

                    // fix a few creation errors
                    if (oldVersion < 83) {
                        // remove "table" infix for route table by renaming table
                        try {
                            db.execSQL("ALTER TABLE cg_table_route RENAME TO " + dbTableRoute);
                        } catch (final SQLException e) {
                            // ignore, because depending on your upgrade path, the statement above cannot work
                        }
                        // recreate extension table to remove "table" infix and to fix two column types
                        try {
                            db.execSQL("DROP TABLE IF EXISTS cg_table_extension;");
                            db.execSQL("DROP TABLE IF EXISTS cg_extension;");
                            db.execSQL(dbCreateExtension);
                        } catch (final SQLException e) {
                            onUpgradeError(e, 83);
                        }
                    }

                    // redefine & migrate route table
                    if (oldVersion < 84 && oldVersion > 80) {
                        try {
                            db.execSQL("ALTER TABLE " + dbTableRoute + " RENAME TO temp_route");
                            db.execSQL(dbCreateRoute);
                            // migrate existing caches in individual route
                            db.execSQL("INSERT INTO " + dbTableRoute + " (precedence, type, id, latitude, longitude)"
                                    + " SELECT precedence, " + RouteItem.RouteItemType.GEOCACHE.ordinal() + " type, geocode id, latitude, longitude"
                                    + " FROM temp_route LEFT JOIN " + dbTableCaches + " USING (geocode)"
                                    + " WHERE temp_route.type=1");
                            // migrate existing waypoints in individual route
                            db.execSQL("INSERT INTO " + dbTableRoute + " (precedence, type, id, latitude, longitude)"
                                    + " SELECT precedence, " + RouteItem.RouteItemType.WAYPOINT.ordinal() + " type, " + dbTableWaypoints + ".geocode || \"-\" || PREFIX id, latitude, longitude"
                                    + " FROM temp_route INNER JOIN " + dbTableWaypoints + " ON (temp_route.id = " + dbTableWaypoints + "._id)"
                                    + " WHERE temp_route.type=0");
                            // drop temp table
                            db.execSQL("DROP TABLE IF EXISTS temp_route");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 84);
                        }
                    }

                    //enhance offline logging storage
                    if (oldVersion < 85) {
                        try {
                            //add new columns
                            createColumnIfNotExists(db, dbTableLogsOffline, "image_title_prefix TEXT");
                            createColumnIfNotExists(db, dbTableLogsOffline, "image_scale INTEGER");
                            createColumnIfNotExists(db, dbTableLogsOffline, "favorite INTEGER");
                            createColumnIfNotExists(db, dbTableLogsOffline, "rating FLOAT");
                            createColumnIfNotExists(db, dbTableLogsOffline, "password TEXT");
                            createColumnIfNotExists(db, dbTableLogsOffline, "tweet INTEGER");
                            //add new tables
                            db.execSQL("DROP TABLE IF EXISTS " + dbTableLogsOfflineImages);
                            db.execSQL("DROP TABLE IF EXISTS " + dbTableLogsOfflineTrackables);
                            db.execSQL(dbCreateLogsOfflineImages);
                            db.execSQL(dbCreateLogsOfflineTrackables);
                        } catch (final SQLException e) {
                            onUpgradeError(e, 85);
                        }
                    }

                    //(re)create indices for logging tables
                    if (oldVersion < 86) {
                        db.execSQL("DROP INDEX in_logs_geo");
                        createIndices(db, 86);
                    }

                    //add service log id
                    if (oldVersion < 87) {
                        try {
                            //add new columns
                            createColumnIfNotExists(db, dbTableLogsOffline, "service_log_id TEXT");
                            createColumnIfNotExists(db, dbTableLogs, "service_log_id TEXT");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 87);
                        }
                    }

                    // add timestamp to cg_trail_history
                    if (oldVersion < 88) {
                        try {
                            createColumnIfNotExists(db, dbTableTrailHistory, "timestamp INTEGER DEFAULT 0");
                            db.execSQL("UPDATE " + dbTableTrailHistory + " SET timestamp =" + System.currentTimeMillis());
                        } catch (final SQLException e) {
                            onUpgradeError(e, 88);
                        }
                    }

                    // add altitude to cg_trail_history
                    if (oldVersion < 89) {
                        try {
                            createColumnIfNotExists(db, dbTableTrailHistory, "altitude DOUBLE DEFAULT 0.0");
                            db.execSQL("UPDATE " + dbTableTrailHistory + " SET altitude = 0.0");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 89);
                        }
                    }

                    // add user guid to cg_caches and cg_logs
                    if (oldVersion < 90) {
                        try {
                            createColumnIfNotExists(db, dbTableCaches, "owner_guid TEXT NOT NULL DEFAULT ''");
                            createColumnIfNotExists(db, dbTableLogs, "author_guid TEXT NOT NULL DEFAULT ''");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 90);
                        }
                    }

                    // add field to cg_extension
                    if (oldVersion < 91) {
                        try {
                            createColumnIfNotExists(db, dbTableExtension, "long3 LONG NOT NULL DEFAULT 0");
                            createColumnIfNotExists(db, dbTableExtension, "long4 LONG NOT NULL DEFAULT 0");
                            createColumnIfNotExists(db, dbTableExtension, "string3 TEXT NOT NULL DEFAULT ''");
                            createColumnIfNotExists(db, dbTableExtension, "string4 TEXT NOT NULL DEFAULT ''");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 91);
                        }
                    }

                    // add emoji to cg_caches
                    if (oldVersion < 92) {
                        try {
                            createColumnIfNotExists(db, dbTableCaches, "emoji INTEGER DEFAULT 0");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 92);
                        }
                    }

                    // add emoji to cg_lists
                    if (oldVersion < 93) {
                        try {
                            createColumnIfNotExists(db, dbTableLists, "emoji INTEGER DEFAULT 0");
                            // migrate marker values
                            db.execSQL("UPDATE " + dbTableLists + " SET emoji = " + 0xe01e + " WHERE marker=1");    // green
                            db.execSQL("UPDATE " + dbTableLists + " SET emoji = " + 0xe030 + " WHERE marker=2");    // orange
                            db.execSQL("UPDATE " + dbTableLists + " SET emoji = " + 0xe01d + " WHERE marker=3");    // blue
                            db.execSQL("UPDATE " + dbTableLists + " SET emoji = " + 0xe024 + " WHERE marker=4");    // red
                            db.execSQL("UPDATE " + dbTableLists + " SET emoji = " + 0xe020 + " WHERE marker=5");    // turquoise
                            db.execSQL("UPDATE " + dbTableLists + " SET emoji = " + 0xe043 + " WHERE marker=6");    // black
                            // do not remove old field "marker" to keep db structure backward-compatible
                        } catch (final SQLException e) {
                            onUpgradeError(e, 93);
                        }
                    }

                    //add scale to offline log image
                    if (oldVersion < 94) {
                        try {
                            createColumnIfNotExists(db, dbTableLogsOfflineImages, "scale INTEGER");

                        } catch (final SQLException e) {
                            onUpgradeError(e, 94);
                        }
                    }

                    //add table to store custom filters
                    if (oldVersion < 95) {
                        try {
                            db.execSQL(dbCreateFilters);
                        } catch (final SQLException e) {
                            onUpgradeError(e, 95);
                        }
                    }

                    //add preventAskForDeletion to cg_lists
                    if (oldVersion < 96) {
                        try {
                            createColumnIfNotExists(db, dbTableLists, FIELD_LISTS_PREVENTASKFORDELETION + " INTEGER DEFAULT 0");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 96);
                        }
                    }

                    //rename lab adventure caches geocodes prefix from LC to AL
                    if (oldVersion < 97) {
                        try {
                            final String sql = " SET geocode = \"AL\" || SUBSTR(geocode, 3) WHERE SUBSTR(geocode, 1, 2) = \"LC\" AND LENGTH(geocode) > 10";
                            db.execSQL("UPDATE " + dbTableCaches + sql);
                            db.execSQL("UPDATE " + dbTableAttributes + sql);
                            db.execSQL("UPDATE " + dbTableCachesLists + sql);
                            db.execSQL("UPDATE " + dbTableLogCount + sql);
                            db.execSQL("UPDATE " + dbTableLogs + sql);
                            db.execSQL("UPDATE " + dbTableLogsOffline + sql);
                            db.execSQL("UPDATE " + dbTableSpoilers + sql);
                            db.execSQL("UPDATE " + dbTableTrackables + sql);
                            db.execSQL("UPDATE " + dbTableWaypoints + sql);
                        } catch (final SQLException e) {
                            onUpgradeError(e, 97);
                        }
                    }

                    //create table for variable storage
                    if (oldVersion < 98) {
                        try {
                            db.execSQL(dbCreateVariables);
                            createIndices(db, 98);
                        } catch (final SQLException e) {
                            onUpgradeError(e, 98);
                        }
                    }

                    // add alcMode to cg_caches
                    if (oldVersion < 99) {
                        try {
                            createColumnIfNotExists(db, dbTableCaches, "alcMode INTEGER DEFAULT 0");
                        } catch (final SQLException e) {
                            onUpgradeError(e, 99);
                        }
                    }

                }

                //at the very end of onUpgrade: rewrite downgradeable versions in database
                try {
                    DBDowngradeableVersions.save(db, DBVERSIONS_DOWNWARD_COMPATIBLE);
                } catch (final Exception e) {
                    Log.e("Failed to rewrite downgradeable versions to " + DBVERSIONS_DOWNWARD_COMPATIBLE, e);
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            Log.iForce("[DB] Upgrade database from ver. " + oldVersion + " to ver. " + newVersion + ": completed");
        }

        private void onUpgradeError(final SQLException e, final int version) throws SQLException {
            Log.e("Failed to upgrade to version " + version, e);
            throw e;
        }

        @Override
        public void onOpen(final SQLiteDatabase db) {

            //get user version
            Log.iForce("[DB] Current Database Version: " + db.getVersion());

            if (firstRun) {
                sanityChecks(db);
                // limit number of records for trailHistory
                try {
                    db.execSQL("DELETE FROM " + dbTableTrailHistory + " WHERE _id < (SELECT MIN(_id) FROM (SELECT _id FROM " + dbTableTrailHistory + " ORDER BY _id DESC LIMIT " + MAX_TRAILHISTORY_LENGTH + "))");
                } catch (final Exception e) {
                    Log.w("Failed to clear trail history", e);
                }

                firstRun = false;
            }
        }

        /**
         * Execute sanity checks that should be performed once per application after the database has been
         * opened.
         *
         * @param db the database to perform sanity checks against
         */
        @SuppressWarnings("EmptyMethod")
        private static void sanityChecks(final SQLiteDatabase db) {
            // currently unused
        }

        /**
         * Method to remove static map files with double underscore due to issue#1670
         * introduced with release on 2012-05-24.
         */
        private static void removeDoubleUnderscoreMapFiles() {
            final File[] geocodeDirs = LocalStorage.getGeocacheDataDirectory().listFiles();
            if (ArrayUtils.isNotEmpty(geocodeDirs)) {
                final FilenameFilter filter = (dir, filename) -> filename.startsWith("map_") && filename.contains("__");
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

        /*
         * Remove empty directories created in the secondary storage area.
         */
        private static void removeSecEmptyDirs() {
            final File[] files = LocalStorage.getLegacyExternalCgeoDirectory().listFiles();
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
            db.execSQL("DROP TABLE IF EXISTS " + dbTableCaches);
            db.execSQL("DROP TABLE IF EXISTS " + dbTableLists);
            db.execSQL("DROP TABLE IF EXISTS " + dbTableCachesLists);
            db.execSQL("DROP TABLE IF EXISTS " + dbTableAttributes);
            db.execSQL("DROP TABLE IF EXISTS " + dbTableWaypoints);
            db.execSQL("DROP TABLE IF EXISTS " + dbTableVariables);
            db.execSQL("DROP TABLE IF EXISTS " + dbTableSpoilers);
            db.execSQL("DROP TABLE IF EXISTS " + dbTableLogs);
            db.execSQL("DROP TABLE IF EXISTS " + dbTableLogCount);
            db.execSQL("DROP TABLE IF EXISTS " + dbTableLogImages);
            db.execSQL("DROP TABLE IF EXISTS " + dbTableLogsOffline);
            db.execSQL("DROP TABLE IF EXISTS " + dbTableLogsOfflineImages);
            db.execSQL("DROP TABLE IF EXISTS " + dbTableLogsOfflineTrackables);
            db.execSQL("DROP TABLE IF EXISTS " + dbTableTrackables);
            db.execSQL("DROP TABLE IF EXISTS " + dbTableSearchDestinationHistory);
            db.execSQL("DROP TABLE IF EXISTS " + dbTableTrailHistory);
            db.execSQL("DROP TABLE IF EXISTS " + dbTableRoute);
            db.execSQL("DROP TABLE IF EXISTS " + dbTableExtension);
            db.execSQL("DROP TABLE IF EXISTS " + dbTableFilters);
            // also delete tables which have old table names
            db.execSQL("DROP TABLE IF EXISTS cg_table_route");
            db.execSQL("DROP TABLE IF EXISTS cg_table_extension");
        }

        /**
         * Helper for columns creation. This method ignores duplicate column errors
         * and is useful for migration situations
         */
        public void createColumnIfNotExists(final SQLiteDatabase db, final String table, final String columnDefinition) {
            try {
                db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + columnDefinition);
                Log.i("[DB] Column '" + table + "'.'" + columnDefinition + "' created");
            } catch (SQLiteException sle) {
                if (!sle.getMessage().contains("duplicate column name")) {
                    throw sle;
                }
                Log.iForce("[DB] Column '" + table + "'.'" + columnDefinition + "' was not created because it already exists. " +
                        "It is expected that this can happen and not an error.");
            }
        }
    }

    /**
     * management of sequences for program internal use
     * synchronisation in respect of sequence name must be done by calling method
     *
     * @param sequence immutable name of sequence
     * @return id           next free sequence number reserved for caller
     */
    private static long incSequence(final String sequence, final long minValue) {
        init();

        database.beginTransaction();
        final SQLiteStatement sequenceSelect = PreparedStatement.SEQUENCE_SELECT.getStatement();
        sequenceSelect.bindString(1, sequence);
        try {
            final long newId = sequenceSelect.simpleQueryForLong() + 1;
            // sequence identifier already exists in DB => increment
            try {
                final SQLiteStatement sequenceUpdate = PreparedStatement.SEQUENCE_UPDATE.getStatement();
                sequenceUpdate.bindLong(1, newId);
                sequenceUpdate.bindString(2, sequence);
                final int test2 = sequenceUpdate.executeUpdateDelete();
                final long test = sequenceSelect.simpleQueryForLong();
                database.setTransactionSuccessful();
                return newId;
            } catch (SQLiteException e1) {
                Log.e("could not increment sequence " + sequence);
                throw new IllegalStateException();
            }
        } catch (android.database.sqlite.SQLiteDoneException e) {
            // sequence identifier not yet in DB => create & set to the minValue given
            try {
                final SQLiteStatement sequenceInsert = PreparedStatement.SEQUENCE_INSERT.getStatement();
                sequenceInsert.bindString(1, sequence);
                sequenceInsert.bindLong(2, minValue);
                sequenceInsert.executeInsert();
                database.setTransactionSuccessful();
                return minValue;
            } catch (SQLiteException e2) {
                Log.e("could not create sequence " + sequence);
                throw new IllegalStateException();
            }
        } finally {
            database.endTransaction();
        }
    }

    public static synchronized long getNextAvailableInternalCacheId() {
        final int minimum = 1000;

        init();
        final Cursor c = database.rawQuery("SELECT MAX(CAST(SUBSTR(geocode," + (1 + InternalConnector.PREFIX.length()) + ") AS INTEGER)) FROM " + dbTableCaches + " WHERE substr(geocode,1," + InternalConnector.PREFIX.length() + ") = \"" + InternalConnector.PREFIX + "\"", new String[]{});
        final Set<Integer> nextId = cursorToColl(c, new HashSet<>(), GET_INTEGER_0);
        for (Integer i : nextId) {
            return Math.max(i + 1, minimum);
        }
        return minimum;
    }

    /**
     * Remove obsolete cache directories in c:geo private storage.
     * Also removes caches marked as "to be deleted" immediately (ignoring 72h grace period!)
     */
    public static void removeObsoleteGeocacheDataDirectories() {
        // force-remove caches marked as "to be deleted", ignoring 72h grace period
        try {
            final Set<String> geocodes = new HashSet<>();
            queryToColl(dbTableCaches,
                    new String[]{"geocode"},
                    "geocode NOT IN (SELECT DISTINCT (geocode) FROM " + dbTableCachesLists + ")",
                    null,
                    null,
                    null,
                    geocodes,
                    GET_STRING_0);
            final Set<String> withoutOfflineLogs = exceptCachesWithOfflineLog(geocodes);
            Log.d("forced database clean: removing " + withoutOfflineLogs.size() + " geocaches ignoring grace period");
            removeCaches(withoutOfflineLogs, LoadFlags.REMOVE_ALL);

            deleteOrphanedRecords();
        } catch (final Exception e) {
            Log.w("DataStore.clean", e);
        }

        // remove orphaned files/folders
        final File[] files = LocalStorage.getGeocacheDataDirectory().listFiles();
        if (ArrayUtils.isNotEmpty(files)) {
            final SQLiteStatement select = PreparedStatement.CHECK_IF_PRESENT.getStatement();
            final List<File> toRemove = new ArrayList<>(files.length);
            for (final File file : files) {
                if (file.isDirectory()) {
                    final String geocode = file.getName();
                    if (!HtmlImage.SHARED.equals(geocode)) {
                        synchronized (select) {
                            select.bindString(1, geocode);
                            if (select.simpleQueryForLong() == 0) {
                                toRemove.add(file);
                            }
                        }
                    }
                }
            }

            // Use a background thread for the real removal to avoid keeping the database locked
            // if we are called from within a transaction.
            Schedulers.io().scheduleDirect(() -> {
                for (final File dir : toRemove) {
                    Log.i("Removing obsolete cache directory for " + dir.getName());
                    FileUtils.deleteDirectory(dir);
                }
            });
        }

        reindexDatabase();
    }

    private static void reindexDatabase() {
        init();
        try {
            Log.d("Database clean: recreate indices");
            database.execSQL("REINDEX");
        } catch (final Exception e) {
            Log.w("DataStore.clean", e);
        }
    }

    public static boolean isThere(final String geocode, final String guid, final boolean checkTime) {
        init();

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
            }

            cursor.close();
        } catch (final Exception e) {
            Log.e("DataStore.isThere", e);
        }

        if (dataDetailed == 0) {
            // we want details, but these are not stored
            return false;
        }

        if (checkTime && dataDetailedUpdate < (System.currentTimeMillis() - DAYS_AFTER_CACHE_IS_DELETED)) {
            // we want to check time for detailed cache, but data are older than 3 days
            return false;
        }

        // we have some cache
        return true;
    }

    /**
     * is cache stored in one of the lists (not only temporary)
     */
    public static boolean isOffline(final String geocode, final String guid) {
        if (StringUtils.isBlank(geocode) && StringUtils.isBlank(guid)) {
            return false;
        }
        init();

        try {
            final SQLiteStatement offlineListCount;
            final String value;
            if (StringUtils.isNotBlank(geocode)) {
                offlineListCount = PreparedStatement.GEOCODE_OFFLINE.getStatement();
                value = geocode;
            } else {
                offlineListCount = PreparedStatement.GUID_OFFLINE.getStatement();
                value = guid;
            }
            synchronized (offlineListCount) {
                offlineListCount.bindString(1, value);
                return offlineListCount.simpleQueryForLong() > 0;
            }
        } catch (final SQLiteDoneException ignored) {
            // Do nothing, it only means we have no information on the cache
        } catch (final Exception e) {
            Log.e("DataStore.isOffline", e);
        }

        return false;
    }

    public static Set<String> getUnsavedGeocodes(@NonNull final Set<String> geocodes) {
        final Set<String> unsavedGeocodes = new HashSet<>();

        for (final String geocode : geocodes) {
            if (!isOffline(geocode, null)) {
                unsavedGeocodes.add(geocode);
            }
        }
        return unsavedGeocodes;
    }

    @Nullable
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

    @Nullable
    public static String getGeocodeForTitle(@NonNull final String title) {
        if (StringUtils.isBlank(title)) {
            return null;
        }
        init();

        try {
            final SQLiteStatement sqlStatement = PreparedStatement.GEOCODE_FROM_TITLE.getStatement();
            synchronized (sqlStatement) {
                sqlStatement.bindString(1, title);
                return sqlStatement.simpleQueryForString();
            }
        } catch (final SQLiteDoneException ignored) {
            // Do nothing, it only means we have no information on the cache
        } catch (final Exception e) {
            Log.e("DataStore.getGeocodeForGuid", e);
        }

        return null;
    }

    /**
     * Save the cache for set/reset user modified coordinates
     */
    public static void saveUserModifiedCoords(final Geocache cache) {
        database.beginTransaction();

        final ContentValues values = new ContentValues();
        try {
            saveWaypointsWithoutTransaction(cache);
            putCoords(values, cache.getCoords());
            values.put("coordsChanged", cache.hasUserModifiedCoords() ? 1 : 0);

            database.update(dbTableCaches, values, "geocode = ?", new String[]{cache.getGeocode()});
            database.setTransactionSuccessful();
        } catch (final Exception e) {
            Log.e("SaveResetCoords", e);
        } finally {
            database.endTransaction();
        }
    }

    /**
     * Save/store a cache to the CacheCache
     *
     * @param cache the Cache to save in the CacheCache/DB
     */
    public static void saveCache(final Geocache cache, final Set<LoadFlags.SaveFlag> saveFlags) {
        saveCaches(Collections.singletonList(cache), saveFlags);
    }

    /**
     * Save/store a cache to the CacheCache
     *
     * @param caches the caches to save in the CacheCache/DB
     */
    public static void saveCaches(final Collection<Geocache> caches, final Set<LoadFlags.SaveFlag> saveFlags) {
        if (CollectionUtils.isEmpty(caches)) {
            return;
        }

        try (ContextLogger cLog = new ContextLogger("DataStore.saveCaches(#%d,flags:%s)", caches.size(), saveFlags)) {

            cLog.add("gc" + cLog.toStringLimited(caches, 10, c -> c == null ? "-" : c.getGeocode()));

            final List<String> cachesFromDatabase = new ArrayList<>();
            final Map<String, Geocache> existingCaches = new HashMap<>();

            // first check which caches are in the memory cache
            for (final Geocache cache : caches) {
                final String geocode = cache.getGeocode();
                final Geocache cacheFromCache = cacheCache.getCacheFromCache(geocode);
                if (cacheFromCache == null) {
                    cachesFromDatabase.add(geocode);
                } else {
                    existingCaches.put(geocode, cacheFromCache);
                }
            }

            // then load all remaining caches from the database in one step
            for (final Geocache cacheFromDatabase : loadCaches(cachesFromDatabase, LoadFlags.LOAD_ALL_DB_ONLY)) {
                existingCaches.put(cacheFromDatabase.getGeocode(), cacheFromDatabase);
            }

            final List<Geocache> toBeStored = new ArrayList<>();
            final List<Geocache> toBeUpdated = new ArrayList<>();
            // Merge with the data already stored in the CacheCache or in the database if
            // the cache had not been loaded before, and update the CacheCache.
            // Also, a DB update is required if the merge data comes from the CacheCache
            // (as it may be more recent than the version in the database), or if the
            // version coming from the database is different than the version we are entering
            // into the cache (that includes absence from the database).
            for (final Geocache cache : caches) {
                final String geocode = cache.getGeocode();
                final Geocache existingCache = existingCaches.get(geocode);
                boolean dbUpdateRequired = !cache.gatherMissingFrom(existingCache) || cacheCache.getCacheFromCache(geocode) != null;
                // parse the note AFTER merging the local information in
                dbUpdateRequired |= cache.addCacheArtefactsFromNotes();
                cache.addStorageLocation(StorageLocation.CACHE);
                cacheCache.putCacheInCache(cache);

                // Only save the cache in the database if it is requested by the caller and
                // the cache contains detailed information.
                if (saveFlags.contains(SaveFlag.DB) && dbUpdateRequired) {
                    toBeStored.add(cache);
                } else if (existingCache != null && existingCache.isDisabled() != cache.isDisabled()) {
                    // Update the disabled status in the database if it changed
                    toBeUpdated.add(cache);
                }
            }

            for (final Geocache geocache : toBeStored) {
                storeIntoDatabase(geocache);
            }

            for (final Geocache geocache : toBeUpdated) {
                updateDisabledStatus(geocache);
            }
        }

    }

    private static boolean updateDisabledStatus(final Geocache cache) {
        cache.addStorageLocation(StorageLocation.DATABASE);
        cacheCache.putCacheInCache(cache);
        Log.d("Updating disabled status of " + cache + " in DB");

        final ContentValues values = new ContentValues();
        values.put("disabled", cache.isDisabled() ? 1 : 0);

        init();
        try {
            database.beginTransaction();
            final int rows = database.update(dbTableCaches, values, "geocode = ?", new String[]{cache.getGeocode()});
            if (rows == 1) {
                database.setTransactionSuccessful();
                return true;
            }
        } catch (final Exception e) {
            Log.e("updateDisabledStatus", e);
        } finally {
            database.endTransaction();
        }

        return false;
    }

    public static boolean storeIntoDatabase(final Geocache cache) {
        cache.addStorageLocation(StorageLocation.DATABASE);
        cacheCache.putCacheInCache(cache);
        Log.d("Saving " + cache + " (" + cache.getLists() + ") to DB");

        final ContentValues values = new ContentValues();

        if (cache.getUpdated() == 0) {
            values.put("updated", System.currentTimeMillis());
        } else {
            values.put("updated", cache.getUpdated());
        }
        values.put("reason", StoredList.STANDARD_LIST_ID);
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
        values.put("size", cache.getSize().id);
        values.put("difficulty", cache.getDifficulty());
        values.put("terrain", cache.getTerrain());
        values.put("location", cache.getLocation());
        values.put("distance", cache.getDistance());
        values.put("direction", cache.getDirection());
        putCoords(values, cache.getCoords());
        values.put("reliable_latlon", 0);          // Todo: refactor - remove column
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
        values.put("found", cache.isFound() ? 1 : cache.isDNF() ? -1 : 0);
        values.put("favourite", cache.isFavorite() ? 1 : 0);
        values.put("inventoryunknown", cache.getInventoryItems());
        values.put("onWatchlist", cache.isOnWatchlist() ? 1 : 0);
        values.put("coordsChanged", cache.hasUserModifiedCoords() ? 1 : 0);
        values.put("finalDefined", cache.hasFinalDefined() ? 1 : 0);
        values.put("logPasswordRequired", cache.isLogPasswordRequired() ? 1 : 0);
        values.put("watchlistCount", cache.getWatchlistCount());
        values.put("preventWaypointsFromNote", cache.isPreventWaypointsFromNote() ? 1 : 0);
        values.put("owner_guid", cache.getOwnerGuid());
        values.put("emoji", cache.getAssignedEmoji());
        values.put("alcMode", cache.getAlcMode());

        init();

        // try to update record else insert fresh..
        database.beginTransaction();

        try {
            saveAttributesWithoutTransaction(cache);
            saveWaypointsWithoutTransaction(cache);
            saveSpoilersWithoutTransaction(cache);
            saveLogCountsWithoutTransaction(cache);
            saveInventoryWithoutTransaction(cache.getGeocode(), cache.getInventory());
            saveListsWithoutTransaction(cache);

            final int rows = database.update(dbTableCaches, values, "geocode = ?", new String[]{cache.getGeocode()});
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

    private static void saveListsWithoutTransaction(final Geocache cache) {
        final String geocode = cache.getGeocode();

        // The lists must be fetched first because lazy loading may load
        // a null set otherwise.
        final Set<Integer> lists = cache.getLists();

        if (lists.isEmpty()) {
            return;
        }
        final SQLiteStatement statement = PreparedStatement.ADD_TO_LIST.getStatement();
        for (final Integer listId : lists) {
            statement.bindLong(1, listId);
            statement.bindString(2, geocode);
            statement.executeInsert();
        }
    }

    /**
     * Persists the given {@code location} into the database.
     *
     * @param location a location to save
     */
    public static void saveTrailpoint(final Location location) {
        init();

        database.beginTransaction();
        try {
            final SQLiteStatement insertTrailpoint = PreparedStatement.INSERT_TRAILPOINT.getStatement();
            insertTrailpoint.bindDouble(1, location.getLatitude());
            insertTrailpoint.bindDouble(2, location.getLongitude());
            insertTrailpoint.bindDouble(3, location.getAltitude());
            insertTrailpoint.bindLong(4, System.currentTimeMillis());
            insertTrailpoint.executeInsert();
            database.setTransactionSuccessful();
        } catch (final Exception e) {
            Log.e("Updating trailHistory db failed", e);
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
            final List<String> currentWaypointIds = new ArrayList<>();
            for (final Waypoint waypoint : waypoints) {
                final ContentValues values = createWaypointValues(geocode, waypoint);

                if (waypoint.getId() < 0) {
                    final long rowId = database.insert(dbTableWaypoints, null, values);
                    waypoint.setId((int) rowId);
                } else {
                    database.update(dbTableWaypoints, values, "_id = ?", new String[]{Integer.toString(waypoint.getId(), 10)});
                }
                currentWaypointIds.add(Integer.toString(waypoint.getId()));
            }

            removeOutdatedWaypointsOfCache(cache, currentWaypointIds);
        }
    }

    /**
     * remove all waypoints of the given cache, where the id is not in the given list
     *
     * @param remainingWaypointIds ids of waypoints which shall not be deleted
     */
    private static void removeOutdatedWaypointsOfCache(@NonNull final Geocache cache, @NonNull final Collection<String> remainingWaypointIds) {
        final String idList = StringUtils.join(remainingWaypointIds, ',');
        database.delete(dbTableWaypoints, "geocode = ? AND _id NOT IN (" + idList + ")", new String[]{cache.getGeocode()});
    }

    /**
     * Save coordinates into a ContentValues
     *
     * @param values a ContentValues to save coordinates in
     * @param coords coordinates to save, or null to save empty coordinates
     */
    private static void putCoords(final ContentValues values, final Geopoint coords) {
        values.put("latitude", coords == null ? null : coords.getLatitude());
        values.put("longitude", coords == null ? null : coords.getLongitude());
    }

    /**
     * Retrieve coordinates from a Cursor
     *
     * @param cursor   a Cursor representing a row in the database
     * @param indexLat index of the latitude column
     * @param indexLon index of the longitude column
     * @return the coordinates, or null if latitude or longitude is null or the coordinates are invalid
     */
    @Nullable
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
            final ContentValues values = createWaypointValues(geocode, waypoint);

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

    @NonNull
    private static ContentValues createWaypointValues(final String geocode, final Waypoint waypoint) {
        final ContentValues values = new ContentValues();
        values.put("geocode", geocode);
        values.put("updated", System.currentTimeMillis());
        values.put("type", waypoint.getWaypointType() != null ? waypoint.getWaypointType().id : null);
        values.put("prefix", waypoint.getPrefix());
        values.put("lookup", waypoint.getLookup());
        values.put("name", waypoint.getName());
        putCoords(values, waypoint.getCoords());
        values.put("note", waypoint.getNote());
        values.put("user_note", waypoint.getUserNote());
        values.put("own", waypoint.isUserDefined() ? 1 : 0);
        values.put("visited", waypoint.isVisited() ? 1 : 0);
        values.put("org_coords_empty", waypoint.isOriginalCoordsEmpty() ? 1 : 0);
        values.put("calc_state", waypoint.getCalcStateConfig());
        return values;
    }

    public static boolean deleteWaypoint(final int id) {
        if (id == 0) {
            return false;
        }

        init();

        return database.delete(dbTableWaypoints, "_id = " + id, null) > 0;
    }

    private static void saveSpoilersWithoutTransaction(final Geocache cache) {
        if (cache.hasSpoilersSet()) {
            final String geocode = cache.getGeocode();
            final SQLiteStatement remove = PreparedStatement.REMOVE_SPOILERS.getStatement();
            remove.bindString(1, cache.getGeocode());
            remove.execute();

            final SQLiteStatement insertSpoiler = PreparedStatement.INSERT_SPOILER.getStatement();
            final long timestamp = System.currentTimeMillis();
            for (final Image spoiler : cache.getSpoilers()) {
                insertSpoiler.bindString(1, geocode);
                insertSpoiler.bindLong(2, timestamp);
                insertSpoiler.bindString(3, spoiler.getUrl());
                insertSpoiler.bindString(4, StringUtils.defaultIfBlank(spoiler.title, ""));
                final String description = spoiler.getDescription();
                if (StringUtils.isNotBlank(description)) {
                    insertSpoiler.bindString(5, description);
                } else {
                    insertSpoiler.bindNull(5);
                }
                insertSpoiler.executeInsert();
            }
        }
    }

    public static void saveLogs(final String geocode, final Iterable<LogEntry> logs, final boolean removeAllExistingLogs) {
        database.beginTransaction();
        try {
            saveLogsWithoutTransaction(geocode, logs, removeAllExistingLogs);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private static void saveLogsWithoutTransaction(final String geocode, final Iterable<LogEntry> logs, final boolean removeAllExistingLogs) {
        try (ContextLogger cLog = new ContextLogger("DataStore.saveLogsWithoutTransaction(%s)", geocode)) {
            if (!logs.iterator().hasNext()) {
                return;
            }
            // TODO delete logimages referring these logs
            if (removeAllExistingLogs) {
                database.delete(dbTableLogs, "geocode = ?", new String[]{geocode});
            } else {
                // instead of deleting all existing logs for this cache, try to merge
                // by deleting only those from same author, same date, same logtype
                final SQLiteStatement deleteLog = PreparedStatement.CLEAN_LOG.getStatement();
                for (final LogEntry log : logs) {
                    final ImmutablePair<Long, Long> dateRange = CalendarUtils.getStartAndEndOfDay(log.date);

                    deleteLog.bindString(1, geocode);
                    deleteLog.bindLong(2, dateRange.left);
                    deleteLog.bindLong(3, dateRange.right);
                    deleteLog.bindLong(4, log.logType.id);
                    deleteLog.bindString(5, log.author);
                    deleteLog.executeUpdateDelete();
                }
            }

            final SQLiteStatement insertLog = PreparedStatement.INSERT_LOG.getStatement();
            final long timestamp = System.currentTimeMillis();
            int logCnt = 0;
            int imgCnt = 0;
            for (final LogEntry log : logs) {
                logCnt++;
                insertLog.bindString(1, geocode);
                insertLog.bindLong(2, timestamp);
                if (log.serviceLogId == null) {
                    insertLog.bindNull(3);
                } else {
                    insertLog.bindString(3, log.serviceLogId);
                }
                insertLog.bindLong(4, log.logType.id);
                insertLog.bindString(5, log.author);
                insertLog.bindString(6, log.authorGuid);
                insertLog.bindString(7, log.log);
                insertLog.bindLong(8, log.date);
                insertLog.bindLong(9, log.found);
                insertLog.bindLong(10, log.friend ? 1 : 0);
                final long logId = insertLog.executeInsert();
                if (log.hasLogImages()) {
                    final SQLiteStatement insertImage = PreparedStatement.INSERT_LOG_IMAGE.getStatement();
                    for (final Image img : log.logImages) {
                        imgCnt++;
                        insertImage.bindLong(1, logId);
                        insertImage.bindString(2, StringUtils.defaultIfBlank(img.title, ""));
                        insertImage.bindString(3, img.getUrl());
                        insertImage.bindString(4, StringUtils.defaultIfBlank(img.getDescription(), ""));
                        insertImage.executeInsert();
                    }
                }
            }
            cLog.add("logs:%d, imgs:%d", logCnt, imgCnt);
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
                    database.delete(dbTableTrackables, "tbcode = ?", new String[]{tbCode});
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
                final Date releasedDate = trackable.getReleased();
                if (releasedDate != null) {
                    values.put("released", releasedDate.getTime());
                } else {
                    values.put("released", 0L);
                }
                values.put("goal", trackable.getGoal());
                values.put("description", trackable.getDetails());

                final Date logDate = trackable.getLogDate();
                if (logDate != null) {
                    values.put("log_date", logDate.getTime());
                } else {
                    values.put("log_date", 0L);
                }
                final LogType logType = trackable.getLogType();
                if (logType != null) {
                    values.put("log_type", trackable.getLogType().id);
                } else {
                    values.put("log_type", 0);
                }
                values.put("log_guid", trackable.getLogGuid());

                database.insert(dbTableTrackables, null, values);

                saveLogsWithoutTransaction(tbCode, trackable.getLogs(), true);
            }
        }
    }

    @Nullable
    public static Viewport getBounds(final Set<String> geocodes, final boolean withWaypoints) {
        if (CollectionUtils.isEmpty(geocodes)) {
            return null;
        }

        final Set<Geocache> caches = loadCaches(geocodes, LoadFlags.LOAD_CACHE_OR_DB);

        Viewport result = null;
        if (!withWaypoints) {
            result = Viewport.containing(caches);
        }
        //if we have no 'withWaypoints' but don't get any viewport without them then try with waypoints as a fallback
        if (result == null) {
            result = Viewport.containingCachesAndWaypoints(caches);
        }
        return result;
    }

    @Nullable
    public static Viewport getBounds(final Set<String> geocodes) {
        return getBounds(geocodes, false);
    }

    /**
     * Load a single Cache.
     *
     * @param geocode The Geocode GCXXXX
     * @return the loaded cache (if found). Can be null
     */
    @Nullable
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
     * @return Set of loaded caches. Never null.
     */
    @NonNull
    public static Set<Geocache> loadCaches(final Collection<String> geocodes, final EnumSet<LoadFlag> loadFlags) {
        if (CollectionUtils.isEmpty(geocodes)) {
            return new HashSet<>();
        }

        final Set<Geocache> result = new HashSet<>(geocodes.size());
        final Set<String> remaining = new HashSet<>(geocodes);

        if (loadFlags.contains(LoadFlag.CACHE_BEFORE)) {
            for (final String geocode : geocodes) {
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

        if (CollectionUtils.isNotEmpty(remaining)) {
            Log.d("DataStore.loadCaches(" + remaining + ") returned no results");
        }
        return result;
    }

    /*
     * Loads a list of all UDC (except "Go To history")
     * sorted by youngest first
     */
    @NonNull
    public static ArrayList<Geocache> loadUDCSorted() {
        final Collection<String> geocodes = queryToColl(dbTableCaches,
                new String[]{"geocode"},
                "substr(geocode,1," + InternalConnector.PREFIX.length() + ") = ? AND geocode <> ?",
                new String[]{InternalConnector.PREFIX, InternalConnector.GEOCODE_HISTORY_CACHE},
                null,
                null,
                new LinkedList<>(),
                GET_STRING_0);
        final ArrayList<Geocache> caches = new ArrayList<>(loadCaches(geocodes, LoadFlags.LOAD_CACHE_OR_DB));
        Collections.sort(caches, (final Geocache cache1, final Geocache cache2) -> -Long.compare(cache1.getUpdated(), cache2.getUpdated()));
        return caches;
    }

    /**
     * Load caches.
     *
     * @return Set of loaded caches. Never null.
     */
    @NonNull
    private static Set<Geocache> loadCachesFromGeocodes(final Set<String> geocodes, final EnumSet<LoadFlag> loadFlags) {

        if (CollectionUtils.isEmpty(geocodes)) {
            return Collections.emptySet();
        }

        try (ContextLogger cLog = new ContextLogger(Log.LogLevel.DEBUG, "DataStore.loadCachesFromGeoCodes(#%d)", geocodes.size())) {
            cLog.add("flags:%s", loadFlags);

            // do not log the entire collection of geo codes to the debug log. This can be more than 100 kB of text for large lists!
            cLog.add("gc" + cLog.toStringLimited(geocodes, 10));

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
            query.append(whereGeocodeIn(geocodes));

            try (Cursor cursor = database.rawQuery(query.toString(), null)) {
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
                            cache.setInventory(inventory);
                        }
                    }

                    if (loadFlags.contains(LoadFlag.OFFLINE_LOG)) {
                        if (logIndex < 0) {
                            logIndex = cursor.getColumnIndex("log");
                        }
                        if (logIndex >= 0) {
                            cache.setHasLogOffline(!cursor.isNull(logIndex));
                        }
                    }
                    cache.addStorageLocation(StorageLocation.DATABASE);
                    cacheCache.putCacheInCache(cache);

                    caches.add(cache);
                }

                final Map<String, Set<Integer>> cacheLists = loadLists(geocodes);
                for (final Geocache geocache : caches) {
                    final Set<Integer> listIds = cacheLists.get(geocache.getGeocode());
                    if (listIds != null) {
                        geocache.setLists(listIds);
                    }
                }
                cLog.addReturnValue("#" + caches.size());
                return caches;
            }
        }
    }


    /**
     * Builds a where for a viewport with the size enhanced by 50%.
     */

    @NonNull
    private static StringBuilder buildCoordinateWhere(final String dbTable, final Viewport viewport) {
        return viewport.resize(1.5).sqlWhere(dbTable);
    }

    /**
     * creates a Cache from the cursor. Doesn't next.
     *
     * @return Cache from DB
     */
    @NonNull
    private static Geocache createCacheFromDatabaseContent(final Cursor cursor) {
        final Geocache cache = new Geocache();

        // Column indexes are defined in 'QUERY_CACHE_DATA'
        cache.setUpdated(cursor.getLong(0));
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
        final int directionIndex = 16;
        if (cursor.isNull(directionIndex)) {
            cache.setDirection(null);
        } else {
            cache.setDirection(cursor.getFloat(directionIndex));
        }
        final int distanceIndex = 17;
        if (cursor.isNull(distanceIndex)) {
            cache.setDistance(null);
        } else {
            cache.setDistance(cursor.getFloat(distanceIndex));
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
        cache.setDNF(cursor.getInt(29) == -1);
        cache.setFavorite(cursor.getInt(30) == 1);
        cache.setInventoryItems(cursor.getInt(31));
        cache.setOnWatchlist(cursor.getInt(32) == 1);
        cache.setUserModifiedCoords(cursor.getInt(34) > 0);
        cache.setCoords(getCoords(cursor, 35, 36));
        cache.setFinalDefined(cursor.getInt(37) > 0);
        cache.setLogPasswordRequired(cursor.getInt(41) > 0);
        cache.setWatchlistCount(cursor.getInt(42));
        cache.setPreventWaypointsFromNote(cursor.getInt(43) > 0);
        cache.setOwnerGuid(cursor.getString(44));
        cache.setAssignedEmoji(cursor.getInt(45));
        cache.setAlcMode(cursor.getInt(46));

        return cache;
    }

    @Nullable
    public static List<String> loadAttributes(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        return queryToColl(dbTableAttributes,
                new String[]{"attribute"},
                "geocode = ?",
                new String[]{geocode},
                null,
                "100",
                new LinkedList<>(),
                GET_STRING_0);
    }

    @Nullable
    public static Set<Integer> loadLists(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        return queryToColl(dbTableCachesLists,
                new String[]{"list_id"},
                "geocode = ?",
                new String[]{geocode},
                null,
                "100",
                new HashSet<>(),
                GET_INTEGER_0);
    }

    @NonNull
    public static Map<String, Set<Integer>> loadLists(final Collection<String> geocodes) {
        final Map<String, Set<Integer>> cacheLists = new HashMap<>();

        final String query = "SELECT list_id, geocode FROM " + dbTableCachesLists +
                " WHERE " +
                whereGeocodeIn(geocodes);

        try (Cursor cursor = database.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                final Integer listId = cursor.getInt(0);
                final String geocode = cursor.getString(1);

                Set<Integer> listIds = cacheLists.get(geocode);
                if (listIds != null) {
                    listIds.add(listId);
                } else {
                    listIds = new HashSet<>();
                    listIds.add(listId);
                    cacheLists.put(geocode, listIds);
                }
            }
        }

        return cacheLists;
    }

    @Nullable
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

    @Nullable
    public static List<Waypoint> loadWaypoints(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        return queryToColl(dbTableWaypoints,
                WAYPOINT_COLUMNS,
                "geocode = ?",
                new String[]{geocode},
                "_id",
                null,
                new LinkedList<>(),
                DataStore::createWaypointFromDatabaseContent);
    }

    @NonNull
    private static Waypoint createWaypointFromDatabaseContent(final Cursor cursor) {
        final String name;
        final WaypointType type;
        final boolean own;
        try {
            name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            type = WaypointType.findById(cursor.getString(cursor.getColumnIndexOrThrow("type")));
            own = cursor.getInt(cursor.getColumnIndexOrThrow("own")) != 0;
        } catch (final IllegalArgumentException e) {
            Log.e("IllegalArgumentException in createWaypointFromDatabaseContent", e);
            throw new IllegalStateException("column not found in database");
        }
        final Waypoint waypoint = new Waypoint(name, type, own);
        try {
            waypoint.setVisited(cursor.getInt(cursor.getColumnIndexOrThrow("visited")) != 0);
            waypoint.setId(cursor.getInt(cursor.getColumnIndexOrThrow("_id")));
            waypoint.setGeocode(cursor.getString(cursor.getColumnIndexOrThrow("geocode")));
            waypoint.setPrefix(cursor.getString(cursor.getColumnIndexOrThrow("prefix")));
            waypoint.setLookup(cursor.getString(cursor.getColumnIndexOrThrow("lookup")));
            waypoint.setCoords(getCoords(cursor, cursor.getColumnIndexOrThrow("latitude"), cursor.getColumnIndexOrThrow("longitude")));
            waypoint.setNote(cursor.getString(cursor.getColumnIndexOrThrow("note")));
            waypoint.setUserNote(cursor.getString(cursor.getColumnIndexOrThrow("user_note")));
            waypoint.setOriginalCoordsEmpty(cursor.getInt(cursor.getColumnIndexOrThrow("org_coords_empty")) != 0);
            waypoint.setCalcStateConfig(cursor.getString(cursor.getColumnIndexOrThrow("calc_state")));
        } catch (final IllegalArgumentException e) {
            Log.e("IllegalArgumentException in createWaypointFromDatabaseContent", e);
        }

        return waypoint;
    }

    /**
     * method should solely be used by class {@Link CacheVariables}
     */
    @NonNull
    public static List<VariableList.VariableEntry> loadVariables(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return Collections.emptyList();
        }

        return queryToColl(dbTableVariables, new String[]{"_id", "varname", "formula"},
                "geocode = ?", new String[]{geocode}, "varorder", null, new ArrayList<>(),
                c -> new VariableList.VariableEntry(
                        c.getLong(0), c.getString(1), c.getString(2)));
    }

    /**
     * method should solely be used by class {@Link CacheVariables}
     */
    public static void upsertVariables(final String geocode, final List<VariableList.VariableEntry> variables) {
        init();
        database.beginTransaction();
        try {
            final Set<Long> idsToRemain = new HashSet<>();
            int varidx = 0;
            for (VariableList.VariableEntry row : variables) {
                final ContentValues cv = new ContentValues();
                cv.put("geocode", geocode);
                cv.put("varname", row.varname);
                cv.put("varorder", varidx++);
                cv.put("formula", row.formula);
                final boolean updated = row.id >= 0 &&
                        database.update(dbTableVariables, cv, "geocode = ? and _id = ?", new String[]{geocode, "" + row.id}) > 0;
                if (updated) {
                    idsToRemain.add(row.id);
                } else {
                    final long newId = database.insert(dbTableVariables, null, cv);
                    if (newId < 0) {
                        throw new SQLiteException("Exception on inserting row in table " + dbTableVariables);
                    }
                    idsToRemain.add(newId);
                }
            }
            database.delete(dbTableVariables, "geocode = ? AND _id NOT IN (" + StringUtils.join(idsToRemain, ",") + ")", new String[]{geocode});
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }


    @Nullable
    private static List<Image> loadSpoilers(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        return queryToColl(dbTableSpoilers,
                new String[]{"url", "title", "description"},
                "geocode = ?",
                new String[]{geocode},
                null,
                "100",
                new LinkedList<>(),
                cursor -> new Image.Builder()
                        .setUrl(cursor.getString(0))
                        .setTitle(cursor.getString(1))
                        .setDescription(cursor.getString(2))
                        .setCategory(Image.ImageCategory.LISTING)
                        .build());
    }

    /**
     * deletes all but the (up to) five most recent goto history entries
     *
     * @return true, if successful, false otherwise
     */
    public static boolean clearGotoHistory() {
        init();
        database.beginTransaction();
        try {
            final String sqlGetMostRecentHistoryWaypoints = "SELECT _id FROM " + dbTableWaypoints + " WHERE geocode='" + InternalConnector.GEOCODE_HISTORY_CACHE + "' ORDER BY _id DESC LIMIT 5";
            final String sqlGetMinIdFromMostRecentHistoryWaypoints = "SELECT MIN(_id) minId FROM (" + sqlGetMostRecentHistoryWaypoints + ")";
            final String sqlWhereDeleteOlderHistorywaypoints = "geocode='" + InternalConnector.GEOCODE_HISTORY_CACHE + "' AND _id < (" + sqlGetMinIdFromMostRecentHistoryWaypoints + ")";
            database.delete(dbTableWaypoints, sqlWhereDeleteOlderHistorywaypoints, null);
            database.setTransactionSuccessful();
            return true;
        } catch (final Exception e) {
            Log.e("Unable to clear goto history", e);
        } finally {
            database.endTransaction();
        }
        return false;
    }

    /**
     * Loads the trail history from the database, limited to allowed MAX_TRAILHISTORY_LENGTH
     * Trail is returned in chronological order, oldest entry first.
     *
     * @return A list of previously trail points or an empty list.
     */
    @NonNull
    public static ArrayList<TrailHistoryElement> loadTrailHistory() {
        final ArrayList<TrailHistoryElement> temp = queryToColl(dbTableTrailHistory,
                new String[]{"_id", "latitude", "longitude", "altitude", "timestamp"},
                "latitude IS NOT NULL AND longitude IS NOT NULL",
                null,
                "_id DESC",
                String.valueOf(DbHelper.MAX_TRAILHISTORY_LENGTH),
                new ArrayList<>(),
                cursor -> new TrailHistoryElement(cursor.getDouble(1), cursor.getDouble(2), cursor.getDouble(3), cursor.getLong(4))
        );
        Collections.reverse(temp);
        return temp;
    }

    public static TrailHistoryElement[] loadTrailHistoryAsArray() {
        init();
        final Cursor cursor = database.query(dbTableTrailHistory, new String[]{"_id", "latitude", "longitude", "altitude", "timestamp"}, "latitude IS NOT NULL AND longitude IS NOT NULL", null, null, null, "_id ASC", null);
        final TrailHistoryElement[] result = new TrailHistoryElement[cursor.getCount()];
        int iPosition = 0;
        try {
            while (cursor.moveToNext()) {
                result[iPosition] = new TrailHistoryElement(cursor.getDouble(1), cursor.getDouble(2), cursor.getDouble(3), cursor.getLong(4));
                iPosition++;
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    public static boolean clearTrailHistory() {
        init();
        database.beginTransaction();

        try {
            database.delete(dbTableTrailHistory, null, null);
            database.setTransactionSuccessful();
            return true;
        } catch (final Exception e) {
            Log.e("Unable to clear trail history", e);
        } finally {
            database.endTransaction();
        }

        return false;
    }

    /**
     * Loads the route from the database
     *
     * @return route.
     */
    @NonNull
    public static ArrayList<RouteItem> loadIndividualRoute() {
        return queryToColl(dbTableRoute,
                new String[]{"id", "latitude", "longitude"},
                "id IS NOT NULL OR (latitude IS NOT NULL AND longitude IS NOT NULL)",
                null,
                "precedence ASC",
                null,
                new ArrayList<>(),
                cursor -> new RouteItem(cursor.getString(0), new Geopoint(cursor.getDouble(1), cursor.getDouble(2)))
        );
    }

    /**
     * Persists the given {@code Route} into the database.
     *
     * @param route a route to save
     */
    public static void saveIndividualRoute(final Route route) {
        init();

        database.beginTransaction();
        try {
            database.execSQL("DELETE FROM " + dbTableRoute);
            final RouteSegment[] segments = route.getSegments();
            final SQLiteStatement insertRouteItem = PreparedStatement.INSERT_ROUTEITEM.getStatement();
            for (int i = 0; i < segments.length; i++) {
                final RouteItem item = segments[i].getItem();
                insertRouteItemHelper(insertRouteItem, item, i);
            }
            database.setTransactionSuccessful();
        } catch (final Exception e) {
            Log.e("Saving route failed", e);
        } finally {
            database.endTransaction();
        }
    }

    public static void saveIndividualRoute(final List<RouteItem> routeItems) {
        init();

        database.beginTransaction();
        try {
            database.execSQL("DELETE FROM " + dbTableRoute);
            final SQLiteStatement insertRouteItem = PreparedStatement.INSERT_ROUTEITEM.getStatement();
            int precedence = 0;
            for (RouteItem item : routeItems) {
                insertRouteItemHelper(insertRouteItem, item, ++precedence);
            }
            database.setTransactionSuccessful();
        } catch (final Exception e) {
            Log.e("Saving route failed", e);
        } finally {
            database.endTransaction();
        }
    }

    private static void insertRouteItemHelper(final SQLiteStatement statement, final RouteItem item, final int precedence) throws Exception {
        final Geopoint point = item.getPoint();
        statement.bindLong(1, precedence);
        statement.bindLong(2, item.getType().ordinal());
        statement.bindString(3, item.getIdentifier());
        statement.bindDouble(4, point.getLatitude());
        statement.bindDouble(5, point.getLongitude());
        statement.executeInsert();
    }

    public static void clearIndividualRoute() {
        init();

        database.beginTransaction();
        try {
            database.execSQL("DELETE FROM " + dbTableRoute);
            database.setTransactionSuccessful();
        } catch (final Exception e) {
            Log.e("Clearing route failed", e);
        } finally {
            database.endTransaction();
        }
    }


    /**
     * @return an immutable, non null list of logs
     */
    @NonNull
    public static List<LogEntry> loadLogs(final String geocode) {
        try (ContextLogger cLog = new ContextLogger("DataStore.loadLogs(%s)", geocode)) {
            final List<LogEntry> logs = new ArrayList<>();

            if (StringUtils.isBlank(geocode)) {
                return logs;
            }

            init();

            final Cursor cursor = database.rawQuery(
                    //                           0          1               2     3       4            5    6     7      8                                       9                10      11     12   13
                    "SELECT cg_logs._id AS cg_logs_id, service_log_id, type, author, author_guid, log, date, found, friend, " + dbTableLogImages + "._id as cg_logImages_id, log_id, title, url, description"
                            + " FROM " + dbTableLogs + " LEFT OUTER JOIN " + dbTableLogImages
                            + " ON ( cg_logs._id = log_id ) WHERE geocode = ?  ORDER BY date DESC, cg_logs._id ASC", new String[]{geocode});

            LogEntry.Builder log = null;
            int cnt = 0;
            while (cursor.moveToNext() && logs.size() < 100) {
                cnt++;
                if (log == null || log.getId() != cursor.getInt(0)) {
                    // Start of a new log entry group (we may have several entries if the log has several images).
                    if (log != null) {
                        logs.add(log.build());
                    }
                    log = new LogEntry.Builder()
                            .setId(cursor.getInt(0))
                            .setServiceLogId(cursor.getString(1))
                            .setLogType(LogType.getById(cursor.getInt(2)))
                            .setAuthor(cursor.getString(3))
                            .setAuthorGuid(cursor.getString(4))
                            .setLog(cursor.getString(5))
                            .setDate(cursor.getLong(6))
                            .setFound(cursor.getInt(7))
                            .setFriend(cursor.getInt(8) == 1);
                    if (!cursor.isNull(9)) {
                        log.addLogImage(new Image.Builder().setUrl(cursor.getString(12)).setTitle(cursor.getString(11)).setDescription(cursor.getString(13)).build());
                    }
                } else {
                    // We cannot get several lines for the same log entry if it does not contain an image.
                    log.addLogImage(new Image.Builder().setUrl(cursor.getString(12)).setTitle(cursor.getString(11)).setDescription(cursor.getString(13)).build());
                }
            }
            if (log != null) {
                logs.add(log.build());
            }

            cursor.close();

            cLog.add("l:%d,#:%d", cnt, logs.size());

            return Collections.unmodifiableList(logs);
        }
    }

    @Nullable
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

    @Nullable
    private static List<Trackable> loadInventory(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null;
        }

        init();

        final List<Trackable> trackables = new ArrayList<>();

        final Cursor cursor = database.query(
                dbTableTrackables,
                new String[]{"_id", "updated", "tbcode", "guid", "title", "owner", "released", "goal", "description", "log_date", "log_type", "log_guid"},
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

    @Nullable
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

    @NonNull
    private static Trackable createTrackableFromDatabaseContent(final Cursor cursor) {
        final Trackable trackable = new Trackable();
        try {
            trackable.setGeocode(cursor.getString(cursor.getColumnIndexOrThrow("tbcode")));
            trackable.setGuid(cursor.getString(cursor.getColumnIndexOrThrow("guid")));
            trackable.setName(cursor.getString(cursor.getColumnIndexOrThrow("title")));
            trackable.setOwner(cursor.getString(cursor.getColumnIndexOrThrow("owner")));
            trackable.setReleased(getDate(cursor, "released"));
            trackable.setGoal(cursor.getString(cursor.getColumnIndexOrThrow("goal")));
            trackable.setDetails(cursor.getString(cursor.getColumnIndexOrThrow("description")));
            trackable.setLogDate(getDate(cursor, "log_date"));
            trackable.setLogType(LogType.getById(cursor.getInt(cursor.getColumnIndexOrThrow("log_type"))));
            trackable.setLogGuid(cursor.getString(cursor.getColumnIndexOrThrow("log_guid")));
            trackable.setLogs(loadLogs(trackable.getGeocode()));
        } catch (final IllegalArgumentException e) {
            Log.e("IllegalArgumentException in createTrackableFromDatabaseContent", e);
        }
        return trackable;
    }

    @Nullable
    private static Date getDate(final Cursor cursor, final String column) {
        String sDate = null;
        Date oDate = null;
        final int idx = cursor.getColumnIndex(column);
        if (idx >= 0) {
            sDate = cursor.getString(idx);
        }
        if (sDate != null) {
            try {
                final long logDateMillis = Long.parseLong(sDate);
                oDate = new Date(logDateMillis);
            } catch (final NumberFormatException e) {
                Log.e("createTrackableFromDatabaseContent", e);
            }
        }
        return oDate;
    }

    /**
     * Number of caches stored for a given type and/or list
     */
    public static int getAllStoredCachesCount(final int list) {
        if (list <= 0) {
            return 0;
        }
        init();

        try {
            final SQLiteStatement compiledStmnt;
            synchronized (PreparedStatement.COUNT_TYPE_LIST) {
                // All the statements here are used only once and are protected through the current synchronized block
                if (list == PseudoList.HISTORY_LIST.id) {
                    compiledStmnt = PreparedStatement.HISTORY_COUNT.getStatement();
                } else if (list == PseudoList.ALL_LIST.id) {
                    compiledStmnt = PreparedStatement.COUNT_ALL_TYPES_ALL_LIST.getStatement();
                } else {
                    compiledStmnt = PreparedStatement.COUNT_ALL_TYPES_LIST.getStatement();
                    compiledStmnt.bindLong(1, list);
                }

                return (int) compiledStmnt.simpleQueryForLong();
            }
        } catch (final Exception e) {
            Log.e("DataStore.loadAllStoredCachesCount", e);
        }

        return 0;
    }

    // get number of offline founds for a specific connector
    public static int getFoundsOffline(final ILogin connector) {
        int counter = 0;

        try {
            final String logIds = CollectionStream.of(Arrays.asList(LogType.getFoundLogIds())).toJoinedString(",");

            final Cursor cursor = database.rawQuery("SELECT geocode FROM " + dbTableLogsOffline + " WHERE geocode IN (SELECT geocode FROM " + dbTableCaches + ") AND type in (" + logIds + ")", null);
            final Set<String> geocodes = cursorToColl(cursor, new HashSet<>(), GET_STRING_0);

            for (String geocode : geocodes) {
                if (ConnectorFactory.getConnector(geocode).getName().equals(connector.getName())) {
                    counter++;
                }
            }
        } catch (final Exception e) {
            Log.e("DataStore.getFoundsOffline", e);
        }

        return counter;
    }

    @NonNull
    private static <T, U extends Collection<? super T>> U queryToColl(@NonNull final String table,
                                                                      final String[] columns,
                                                                      final String selection,
                                                                      final String[] selectionArgs,
                                                                      final String orderBy,
                                                                      final String limit,
                                                                      final U result,
                                                                      final Func1<? super Cursor, ? extends T> func) {
        init();
        final Cursor cursor = database.query(table, columns, selection, selectionArgs, null, null, orderBy, limit);
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
     * @param coords the current coordinates to sort by distance, or null to sort by geocode
     * @return a non-null set of geocodes
     */
    @NonNull
    private static Set<String> loadBatchOfStoredGeocodes(final Geopoint coords, final int listId, final GeocacheFilter filter, final CacheComparator sort, final boolean sortInverse, final int limit) {

        try (ContextLogger cLog = new ContextLogger(Log.LogLevel.DEBUG, "DataStore.loadBatchOfStoredGeocodes(coords=%s, list=%d)",
                String.valueOf(coords), listId)) {

            final SqlBuilder sqlBuilder = new SqlBuilder(dbTableCaches, new String[]{"geocode"});

            if (listId == PseudoList.HISTORY_LIST.id) {
                sqlBuilder.addWhere(" ( visiteddate > 0 OR geocode IN (SELECT geocode FROM " + dbTableLogsOffline + ") )");
            } else if (listId > 0) {
                final String clId = sqlBuilder.getNewTableId();
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".geocode IN (SELECT " + clId + ".geocode FROM " + dbTableCachesLists + " " + clId + " WHERE list_id " +
                        (listId != PseudoList.ALL_LIST.id ? "=" + Math.max(listId, 1) : ">= " + StoredList.STANDARD_LIST_ID) + ")");
            }
            if (filter != null && filter.getTree() != null) {
                filter.getTree().addToSql(sqlBuilder);
                if (!sqlBuilder.allWheresClosed()) {
                    Log.e("SQL Where not closed in SqlBuilder '" + sqlBuilder + "' for '" + filter + "'");
                }
                sqlBuilder.closeAllOpenWheres();
            }
            if (sort != null) {
                sort.addSortToSql(sqlBuilder, sortInverse);
            }
            if (coords != null) {
                sqlBuilder.addOrder(getCoordDiffExpression(coords, null));
            }
            if (limit > 0) {
                sqlBuilder.setLimit(limit);
            }

            Log.d("SQL: [" + sqlBuilder.getSql() + "]");
            cLog.add("Sel:" + sqlBuilder.getSql());

            return cursorToColl(database.rawQuery(sqlBuilder.getSql(), sqlBuilder.getSqlWhereArgsArray()), new HashSet<>(), GET_STRING_0);
        } catch (final Exception e) {
            Log.e("DataStore.loadBatchOfStoredGeocodes", e);
            return Collections.emptySet();
        }
    }

    public static String getCoordDiffExpression(@NonNull final Geopoint coords, @Nullable final String tableId) {
        final String tableExp = tableId == null ? "" : tableId + ".";
        return "(ABS(" + tableExp + "latitude - " + String.format((Locale) null, "%.6f", coords.getLatitude()) +
                ") + ABS(" + tableExp + "longitude - " + String.format((Locale) null, "%.6f", coords.getLongitude()) + "))";
    }

    public static String getSqlDistanceSquare(@Nullable final String tableId, final Geopoint latlon2) {
        final String tableExp = tableId == null ? "" : tableId + ".";
        return getSqlDistanceSquare(tableExp + "latitude", tableExp + "longitude", latlon2);
    }

    /**
     * Returns an SQL expression calculation the SQUARE (!) distance between two coordinates in meters.
     * Note that given (String) values for lat1/lon1 can be either numbers or e.g. SQL column names/expressions.
     * lat/lon2, however, must be numbers for our calculation tricks to work.... (hey, this is SQL we're talking about!)
     */
    public static String getSqlDistanceSquare(final String lat1, final String lon1, final Geopoint latlon2) {
        //This is SQL after all! So we have to use a simplified distance calculation here, according to: https://www.mkompf.com/gps/distcalc.html
        //distance = sqrt(dx * dx + dy * dy)
        //with distance: Distance in km
        //dx = 111.3 * cos(lat) * (lon1 - lon2)
        //lat = (lat1 + lat2) / 2 * 0.01745
        //dy = 111.3 * (lat1 - lat2)
        //lat1, lat2, lon1, lon2: Latitude, Longitude in degrees (not radians!)

        final double lat2 = latlon2.getLatitude();
        final double lon2 = latlon2.getLongitude();

        //Unfortunately, SQLite in our version does not know functions like COS, SQRT or PI. So we have to perform some tricks...
        final String dxExceptLon1Lon2Square = String.valueOf(Math.pow(Math.cos(lat2 * Math.PI / 180 * 0.01745) * 111.3, 2));
        final String dyExceptLat1Lat2Square = String.valueOf(Math.pow(111.3, 2));

        final String dxSquare = "(" + dxExceptLon1Lon2Square + " * (" + lon1 + " - " + lon2 + ") * (" + lon1 + " - " + lon2 + "))";
        final String dySquare = "(" + dyExceptLat1Lat2Square + " * (" + lat1 + " - " + lat2 + ") * (" + lat1 + " - " + lat2 + "))";

        final String dist = "(" + dxSquare + " + " + dySquare + ")";
        return dist;
    }

    /**
     * Retrieve all stored caches from DB
     */
    @NonNull
    public static SearchResult loadCachedInViewport(final Viewport viewport) {
        return loadInViewport(false, viewport);
    }

    /**
     * Retrieve stored caches from DB with listId >= 1
     */
    @NonNull
    public static SearchResult loadStoredInViewport(final Viewport viewport) {
        return loadInViewport(true, viewport);
    }

    /**
     * Loads the geocodes of caches in a viewport from CacheCache and/or Database
     *
     * @param stored   {@code true} to query caches stored in the database, {@code false} to also use the CacheCache
     * @param viewport the viewport defining the area to scan
     * @return the matching caches
     */
    @NonNull
    private static SearchResult loadInViewport(final boolean stored, final Viewport viewport) {
        try (ContextLogger cLog = new ContextLogger("DataStore.loadInViewport()")) {
            cLog.add("stored=%b,vp=%s", stored, viewport);

            final Set<String> geocodes = new HashSet<>();

            // if not stored only, get codes from CacheCache as well
            if (!stored) {
                geocodes.addAll(cacheCache.getInViewport(viewport));
            }

            // viewport limitation
            final StringBuilder selection = buildCoordinateWhere(dbTableCaches, viewport);

            // offline caches only
            if (stored) {
                selection.append(" AND geocode IN (SELECT geocode FROM " + dbTableCachesLists + " WHERE list_id >= " + StoredList.STANDARD_LIST_ID + ")");
            }

            cLog.add("gc" + cLog.toStringLimited(geocodes, 10));

            try {
                final SearchResult sr = new SearchResult(queryToColl(dbTableCaches,
                        new String[]{"geocode"},
                        selection.toString(),
                        null,
                        null,
                        "500",
                        geocodes,
                        GET_STRING_0));
                cLog.addReturnValue(sr.getCount());
                return sr;

            } catch (final Exception e) {
                Log.e("DataStore.loadInViewport", e);
            }

            return new SearchResult();
        }
    }

    /**
     * Remove caches which are not on any list in the background. Once it has been executed once it will not do anything.
     * This must be called from the UI thread to ensure synchronization of an internal variable.
     */
    public static void cleanIfNeeded(final Context context) {
        if (databaseCleaned) {
            return;
        }
        databaseCleaned = true;

        try (ContextLogger ignore = new ContextLogger(true, "DataStore.cleanIfNeeded: cleans DB")) {
            Schedulers.io().scheduleDirect(() -> {
                // check for UDC cleanup every time this method is called
                deleteOrphanedUDC();

                // reindex if needed
                if (Settings.dbNeedsReindex()) {
                    Settings.setDbReindexLastCheck(false);
                    reindexDatabase();
                }

                // other cleanup will be done once a day at max
                if (Settings.dbNeedsCleanup()) {
                    Settings.setDbCleanupLastCheck(false);

                    Log.d("Database clean: started");
                    try {
                        final Set<String> geocodes = new HashSet<>();
                        final String timestampString = Long.toString(System.currentTimeMillis() - DAYS_AFTER_CACHE_IS_DELETED);
                        queryToColl(dbTableCaches,
                                new String[]{"geocode"},
                                "detailedupdate < ? AND visiteddate < ? AND geocode NOT IN (SELECT DISTINCT (geocode) FROM " + dbTableCachesLists + ")",
                                new String[]{timestampString, timestampString},
                                null,
                                null,
                                geocodes,
                                GET_STRING_0);

                        final Set<String> withoutOfflineLogs = exceptCachesWithOfflineLog(geocodes);
                        Log.d("Database clean: removing " + withoutOfflineLogs.size() + " geocaches");
                        removeCaches(withoutOfflineLogs, LoadFlags.REMOVE_ALL);

                        deleteOrphanedRecords();
                        makeWaypointPrefixesUnique();

                        // Remove the obsolete "_others" directory where the user avatar used to be stored.
                        FileUtils.deleteDirectory(LocalStorage.getGeocacheDataDirectory("_others"));

                        final int version = Version.getVersionCode(context);
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
    }

    private static void deleteOrphanedRecords() {
        Log.d("Database clean: removing non-existing lists");
        database.delete(dbTableCachesLists, "list_id <> " + StoredList.STANDARD_LIST_ID + " AND list_id NOT IN (SELECT _id + " + customListIdOffset + " FROM " + dbTableLists + ")", null);

        Log.d("Database clean: removing non-existing caches from attributes");
        database.delete(dbTableAttributes, "geocode NOT IN (SELECT geocode FROM " + dbTableCaches + ")", null);

        Log.d("Database clean: removing non-existing caches from spoilers");
        database.delete(dbTableSpoilers, "geocode NOT IN (SELECT geocode FROM " + dbTableCaches + ")", null);

        Log.d("Database clean: removing non-existing caches from lists");
        database.delete(dbTableCachesLists, "geocode NOT IN (SELECT geocode FROM " + dbTableCaches + ")", null);

        Log.d("Database clean: removing non-existing caches from waypoints");
        database.delete(dbTableWaypoints, "geocode NOT IN (SELECT geocode FROM " + dbTableCaches + ")", null);

        Log.d("Database clean: removing non-existing caches from variables");
        database.delete(dbTableVariables, "geocode NOT IN (SELECT geocode FROM " + dbTableCaches + ")", null);

        Log.d("Database clean: removing non-existing caches from trackables");
        database.delete(dbTableTrackables, "geocode NOT IN (SELECT geocode FROM " + dbTableCaches + ")", null);

        Log.d("Database clean: removing non-existing caches from logcount");
        database.delete(dbTableLogCount, "geocode NOT IN (SELECT geocode FROM " + dbTableCaches + ")", null);

        DBLogOfflineUtils.cleanOrphanedRecords(database);

        Log.d("Database clean: removing non-existing caches from logs");
        database.delete(dbTableLogs, "geocode NOT IN (SELECT geocode FROM " + dbTableCaches + ")", null);

        Log.d("Database clean: removing non-existing logs from logimages");
        database.delete(dbTableLogImages, "log_id NOT IN (SELECT _id FROM " + dbTableLogs + ")", null);

        Log.d("Database clean: remove non-existing extension values");
        final DBExtensionType[] extensionValues = DBExtensionType.values();
        if (extensionValues.length > 0) {
            String type = "";
            for (DBExtensionType id : extensionValues) {
                type += (StringUtils.isNotBlank(type) ? "," : "") + id.id;
            }
            database.delete(dbTableExtension, "_type NOT IN (" + type + ")", null);
        }
        database.delete(dbTableExtension, "_type=" + DBEXTENSION_INVALID.id, null);
    }

    private static void deleteOrphanedUDC() {
        final Set<String> orphanedUDC = new HashSet<>();
        queryToColl(dbTableCaches,
                new String[]{"geocode"},
                "SUBSTR(geocode,1," + InternalConnector.PREFIX.length() + ") = '" + InternalConnector.PREFIX + "' AND geocode NOT IN (SELECT geocode FROM " + dbTableCachesLists + " WHERE SUBSTR(geocode,1," + InternalConnector.PREFIX.length() + ") = '" + InternalConnector.PREFIX + "')",
                null,
                null,
                null,
                orphanedUDC,
                GET_STRING_0
        );
        final StringBuilder info = new StringBuilder();
        for (String geocode : orphanedUDC) {
            info.append(" ").append(geocode);
        }
        Log.i("delete orphaned UDC" + info);
        removeCaches(orphanedUDC, LoadFlags.REMOVE_ALL);
    }

    /**
     * due to historical reasons some waypoints of the same cache may have the same prefix, which is invalid
     * this method makes those prefixes unique
     */
    private static void makeWaypointPrefixesUnique() {
        init();
        try (Cursor cursor = database.query(dbTableWaypoints, new String[]{"_id", "geocode", "prefix"}, null, null, "geocode, prefix", "COUNT(prefix) > 1", "geocode", null)) {
            while (cursor.moveToNext()) {
                final int id = cursor.getInt(0);
                final String geocode = cursor.getString(1);
                final String prefix = cursor.getString(2);
                Log.w("found duplicate prefixes in waypoints for cache " + geocode + ", prefix=" + prefix);

                // retrieve all prefixes for this cache
                final ArrayList<String> usedPrefixes = new ArrayList<>();
                queryToColl(dbTableWaypoints, new String[]{"prefix"}, "geocode=?", new String[]{geocode}, "_id", null, usedPrefixes, GET_STRING_0);

                try (Cursor cursor2 = database.query(dbTableWaypoints, new String[]{"_id", "prefix"}, "geocode=? AND prefix=?", new String[]{geocode, prefix}, null, null, "_id", null)) {
                    while (cursor2.moveToNext()) {
                        if (id != cursor2.getInt(0)) {
                            final String duplicate = cursor2.getString(1);
                            int counter = 0;
                            boolean found = true;
                            while (found) {
                                found = false;
                                counter++;
                                final String newPrefix = duplicate + "-" + counter;
                                for (String usedPrefix : usedPrefixes) {
                                    if (StringUtils.equals(usedPrefix, newPrefix)) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    // update prefix in database
                                    final ContentValues values = new ContentValues();
                                    values.put("prefix", newPrefix);
                                    database.update(dbTableWaypoints, values, "_id=?", new String[]{String.valueOf(cursor2.getInt(0))});
                                    usedPrefixes.add(newPrefix);
                                    Log.w("=> updated prefix for waypoint id=" + cursor2.getInt(0) + ", from " + duplicate + " to " + newPrefix);
                                }
                            }
                        }
                    }
                }
                // remove cache from cachecache to force reload with updated data
                cacheCache.removeCacheFromCache(cursor.getString(1));
            }
        }
    }

    /**
     * remove all geocodes from the given list of geocodes where an offline log exists
     */
    @NonNull
    private static Set<String> exceptCachesWithOfflineLog(@NonNull final Set<String> geocodes) {
        if (geocodes.isEmpty()) {
            return geocodes;
        }

        final List<String> geocodesWithOfflineLog = queryToColl(dbTableLogsOffline,
                new String[]{"geocode"},
                null,
                null,
                null,
                null,
                new LinkedList<>(),
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
     * @param geocodes list of geocodes to drop from cache
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
            final String baseWhereClause = "geocode IN (" + geocodeList + ")";
            database.beginTransaction();
            try {
                database.delete(dbTableCaches, baseWhereClause, null);
                database.delete(dbTableAttributes, baseWhereClause, null);
                database.delete(dbTableSpoilers, baseWhereClause, null);
                database.delete(dbTableLogImages, "log_id IN (SELECT _id FROM " + dbTableLogs + " WHERE " + baseWhereClause + ")", null);
                database.delete(dbTableLogs, baseWhereClause, null);
                database.delete(dbTableLogCount, baseWhereClause, null);
                DBLogOfflineUtils.remove(database, baseWhereClause, null);
                String wayPointClause = baseWhereClause;
                if (!removeFlags.contains(RemoveFlag.OWN_WAYPOINTS_ONLY_FOR_TESTING)) {
                    wayPointClause += " AND type <> 'own'";
                }
                database.delete(dbTableWaypoints, wayPointClause, null);
                database.delete(dbTableVariables, baseWhereClause, null);
                database.delete(dbTableTrackables, baseWhereClause, null);
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }

            // Delete cache directories
            for (final String geocode : geocodes) {
                FileUtils.deleteDirectory(LocalStorage.getGeocacheDataDirectory(geocode));
            }
        }
    }

    public static boolean saveLogOffline(final String geocode, final OfflineLogEntry entry) {
        return DBLogOfflineUtils.save(geocode, entry);
    }

    @Nullable
    public static OfflineLogEntry loadLogOffline(final String geocode) {
        return DBLogOfflineUtils.load(geocode);
    }

    public static boolean clearLogOffline(final String geocode) {
        return DBLogOfflineUtils.remove(geocode);
    }

    public static void clearLogsOffline(final Collection<Geocache> caches) {
        DBLogOfflineUtils.remove(caches);
        CollectionStream.of(caches).forEach(c -> c.setHasLogOffline(false));
    }

    private static void setVisitDate(final Collection<String> geocodes, final long visitedDate) {
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
        if (!init(false)) {
            return Collections.emptyList();
        }

        final Resources res = CgeoApplication.getInstance().getResources();
        final List<StoredList> lists = new ArrayList<>();
        lists.add(new StoredList(StoredList.STANDARD_LIST_ID, res.getString(R.string.list_inbox), EmojiUtils.NO_EMOJI, false, (int) PreparedStatement.COUNT_CACHES_ON_STANDARD_LIST.simpleQueryForLong()));

        try {
            final String query = "SELECT l._id AS _id, l.title AS title, l.emoji AS emoji, COUNT(c.geocode) AS count" +
                    " FROM " + dbTableLists + " l LEFT OUTER JOIN " + dbTableCachesLists + " c" +
                    " ON l._id + " + customListIdOffset + " = c.list_id" +
                    " GROUP BY l._id" +
                    " ORDER BY l.title COLLATE NOCASE ASC";

            lists.addAll(getListsFromCursor(database.rawQuery(query, null)));
        } catch (final Exception e) {
            Log.e("DataStore.readLists", e);
        }
        return lists;
    }

    @NonNull
    private static List<StoredList> getListsFromCursor(final Cursor cursor) {
        final int indexId = cursor.getColumnIndex("_id");
        final int indexTitle = cursor.getColumnIndex("title");
        final int indexEmoji = cursor.getColumnIndex("emoji");
        final int indexCount = cursor.getColumnIndex("count");
        final int indexPreventAskForDeletion = cursor.getColumnIndex(FIELD_LISTS_PREVENTASKFORDELETION);
        return cursorToColl(cursor, new ArrayList<>(), cursor1 -> {
            final int count = indexCount != -1 ? cursor1.getInt(indexCount) : 0;
            return new StoredList(cursor1.getInt(indexId) + customListIdOffset, cursor1.getString(indexTitle), cursor1.getInt(indexEmoji), indexPreventAskForDeletion >= 0 && cursor1.getInt(indexPreventAskForDeletion) != 0, count);
        });
    }

    @NonNull
    public static StoredList getList(final int id) {
        init();
        if (id >= customListIdOffset) {
            final Cursor cursor = database.query(
                    dbTableLists,
                    new String[]{"_id", "title", "emoji", FIELD_LISTS_PREVENTASKFORDELETION},
                    "_id = ? ",
                    new String[]{String.valueOf(id - customListIdOffset)},
                    null,
                    null,
                    null);
            final List<StoredList> lists = getListsFromCursor(cursor);
            if (!lists.isEmpty()) {
                return lists.get(0);
            }
        }

        final Resources res = CgeoApplication.getInstance().getResources();
        if (id == PseudoList.ALL_LIST.id) {
            return new StoredList(PseudoList.ALL_LIST.id, res.getString(R.string.list_all_lists), EmojiUtils.NO_EMOJI, true, getAllCachesCount());
        }

        // fall back to standard list in case of invalid list id
        return new StoredList(StoredList.STANDARD_LIST_ID, res.getString(R.string.list_inbox), EmojiUtils.NO_EMOJI, false, (int) PreparedStatement.COUNT_CACHES_ON_STANDARD_LIST.simpleQueryForLong());
    }

    public static int getAllCachesCount() {
        return (int) PreparedStatement.COUNT_ALL_CACHES.simpleQueryForLong();
    }

    /**
     * Count all caches in the background.
     *
     * @return a single containing a unique element if the caches could be counted, or an error otherwise
     */
    public static Single<Integer> getAllCachesCountObservable() {
        return allCachesCountObservable;
    }

    /**
     * Create a new list
     *
     * @param name Name
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
            values.put("marker", 0); // ToDo - delete column?
            values.put(FIELD_LISTS_PREVENTASKFORDELETION, 0);
            values.put("emoji", 0);

            id = (int) database.insert(dbTableLists, null, values);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

        return id >= 0 ? id + customListIdOffset : -1;
    }

    /**
     * @param listId List to change
     * @param name   New name of list
     * @return Number of lists changed
     */
    public static int renameList(final int listId, final String name) {
        if (StringUtils.isBlank(name) || listId == StoredList.STANDARD_LIST_ID) {
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

                final SQLiteStatement removeAllFromList = PreparedStatement.REMOVE_ALL_FROM_LIST.getStatement();
                removeAllFromList.bindLong(1, listId);
                removeAllFromList.execute();

                status = true;
            }

            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

        return status;
    }

    /**
     * @param listId   List to change
     * @param useEmoji Id of new emoji
     * @return Number of lists changed
     */
    public static int setListEmoji(final int listId, final int useEmoji) {
        if (listId == StoredList.STANDARD_LIST_ID) {
            return 0;
        }

        init();

        database.beginTransaction();
        int count = 0;
        try {
            final ContentValues values = new ContentValues();
            values.put("emoji", useEmoji);
            values.put("updated", System.currentTimeMillis());

            count = database.update(dbTableLists, values, "_id = " + (listId - customListIdOffset), null);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

        return count;
    }

    /**
     * @param listId  List to change
     * @param prevent value
     * @return Number of lists changed
     */
    public static int setListPreventAskForDeletion(final int listId, final boolean prevent) {
        if (listId == StoredList.STANDARD_LIST_ID) {
            return 0;
        }

        init();

        database.beginTransaction();
        int count = 0;
        try {
            final ContentValues values = new ContentValues();
            values.put(FIELD_LISTS_PREVENTASKFORDELETION, prevent ? 1 : 0);
            values.put("updated", System.currentTimeMillis());

            count = database.update(dbTableLists, values, "_id = " + (listId - customListIdOffset), null);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

        return count;
    }

    public static void moveToList(final Collection<Geocache> caches, final int oldListId, final int newListId) {
        if (caches.isEmpty()) {
            return;
        }
        final AbstractList list = AbstractList.getListById(newListId);
        if (list == null) {
            return;
        }
        if (!list.isConcrete()) {
            return;
        }
        init();

        final SQLiteStatement remove = PreparedStatement.REMOVE_FROM_LIST.getStatement();
        final SQLiteStatement add = PreparedStatement.ADD_TO_LIST.getStatement();

        database.beginTransaction();
        try {
            for (final Geocache cache : caches) {
                remove.bindLong(1, oldListId);
                remove.bindString(2, cache.getGeocode());
                remove.execute();

                add.bindLong(1, newListId);
                add.bindString(2, cache.getGeocode());
                add.execute();

                cache.getLists().remove(oldListId);
                cache.getLists().add(newListId);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public static void removeFromList(final Collection<Geocache> caches, final int oldListId) {
        init();

        final SQLiteStatement remove = PreparedStatement.REMOVE_FROM_LIST.getStatement();

        database.beginTransaction();
        try {
            for (final Geocache cache : caches) {
                remove.bindLong(1, oldListId);
                remove.bindString(2, cache.getGeocode());
                remove.execute();
                cache.getLists().remove(oldListId);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public static void addToList(final Collection<Geocache> caches, final int listId) {
        if (caches.isEmpty()) {
            return;
        }
        final AbstractList list = AbstractList.getListById(listId);
        if (list == null) {
            return;
        }
        if (!list.isConcrete()) {
            return;
        }
        init();

        final SQLiteStatement add = PreparedStatement.ADD_TO_LIST.getStatement();

        database.beginTransaction();
        try {
            for (final Geocache cache : caches) {
                add.bindLong(1, listId);
                add.bindString(2, cache.getGeocode());
                add.execute();

                cache.getLists().add(listId);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public static void saveLists(final Collection<Geocache> caches, final Set<Integer> listIds) {
        if (caches.isEmpty()) {
            return;
        }
        init();

        final SQLiteStatement add = PreparedStatement.ADD_TO_LIST.getStatement();
        final SQLiteStatement remove = PreparedStatement.REMOVE_FROM_ALL_LISTS.getStatement();

        database.beginTransaction();
        try {
            for (final Geocache cache : caches) {
                remove.bindString(1, cache.getGeocode());
                remove.execute();
                cache.getLists().clear();

                for (final Integer listId : listIds) {
                    final AbstractList list = AbstractList.getListById(listId);
                    if (list == null) {
                        return;
                    }
                    if (!list.isConcrete()) {
                        return;
                    }
                    add.bindLong(1, listId);
                    add.bindString(2, cache.getGeocode());
                    add.execute();

                    cache.getLists().add(listId);
                }
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public static void addToLists(final Collection<Geocache> caches, final Map<String, Set<Integer>> cachesLists) {
        if (caches.isEmpty() || cachesLists.isEmpty()) {
            return;
        }
        init();

        final SQLiteStatement add = PreparedStatement.ADD_TO_LIST.getStatement();

        database.beginTransaction();
        try {
            for (final Geocache cache : caches) {
                final Set<Integer> lists = cachesLists.get(cache.getGeocode());
                if (lists.isEmpty()) {
                    continue;
                }

                for (final Integer listId : lists) {
                    add.bindLong(1, listId);
                    add.bindString(2, cache.getGeocode());
                    add.execute();
                }

            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public static void setCacheIcons(final Collection<Geocache> caches, final int newCacheIcon) {
        if (caches.isEmpty()) {
            return;
        }
        final SQLiteStatement add = PreparedStatement.SET_CACHE_ICON.getStatement();

        database.beginTransaction();
        try {
            for (final Geocache cache : caches) {
                add.bindLong(1, newCacheIcon);
                add.bindString(2, cache.getGeocode());
                add.execute();

                cache.setAssignedEmoji(newCacheIcon);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    /**
     * Sets individual cache icons given by HashMap<Geocode, newCacheIcon>.
     * Missing entries are reset to default value (0).
     */
    public static void setCacheIcons(final Collection<Geocache> caches, final HashMap<String, Integer> undo) {
        if (caches.isEmpty()) {
            return;
        }
        final SQLiteStatement add = PreparedStatement.SET_CACHE_ICON.getStatement();

        database.beginTransaction();
        try {
            for (final Geocache cache : caches) {
                final String geocode = cache.getGeocode();
                final Integer newCacheIcon = undo.get(geocode);
                add.bindLong(1, newCacheIcon == null ? 0 : newCacheIcon);
                add.bindString(2, geocode);
                add.execute();

                cache.setAssignedEmoji(newCacheIcon == null ? 0 : newCacheIcon);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private static @NonNull
    String fetchLocation(final Cursor cursor) {
        String location = null;
        final int idx = cursor.getColumnIndex("location");
        if (idx >= 0) {
            location = cursor.getString(idx);
        }

        return location == null ? "" : location;
    }

    private static @NonNull
    String getCountry(final String location) {
        final String separator = ", ";

        final int indexOfSeparator = location.lastIndexOf(separator);

        if (indexOfSeparator == -1) {
            return location;
        } else {
            // extract the country as the last comma separated part in the location
            return location.substring(indexOfSeparator + separator.length());
        }
    }

    private static @NonNull
    String fetchCountry(final Cursor cursor) {
        return getCountry(fetchLocation(cursor));
    }

    // Sort the (empty) filter at the top
    public static final Comparator<String> COUNTRY_SORT_ORDER = (a, b) -> a == null ? -1 : a.compareToIgnoreCase(b);

    // Sort the (empty) filter at the top, then by country, then by rest of the location
    public static final Comparator<String> LOCATION_SORT_ORDER = (a, b) -> {
        final String countryA = getCountry(a), countryB = getCountry(b);
        final int compare = a == null ? -1 : COUNTRY_SORT_ORDER.compare(countryA, countryB);

        if (compare == 0) {
            return a.compareToIgnoreCase(b);
        } else {
            return compare;
        }
    };

    public static SortedSet<String> getAllStoredLocations() {
        init();

        final Cursor cursor = database.rawQuery(PreparedStatement.GET_ALL_STORED_LOCATIONS.query, new String[0]);

        return cursorToColl(cursor, new TreeSet<>(LOCATION_SORT_ORDER), DataStore::fetchLocation);
    }

    public static SortedSet<String> getAllStoredCountries() {
        init();

        final Cursor cursor = database.rawQuery(PreparedStatement.GET_ALL_STORED_LOCATIONS.query, new String[0]);

        return cursorToColl(cursor, new TreeSet<>(COUNTRY_SORT_ORDER), DataStore::fetchCountry);
    }

    public static boolean isInitialized() {
        return database != null;
    }

    /**
     * Load the lazily initialized fields of a cache and return them as partial cache (all other fields unset).
     */
    @NonNull
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
                    new String[]{"description", "shortdesc", "hint", "location"},
                    "geocode = ?",
                    new String[]{geocode},
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
    @NonNull
    private static StringBuilder whereGeocodeIn(final Collection<String> geocodes) {
        final StringBuilder whereExpr = new StringBuilder("geocode IN (");
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
     */

    @NonNull
    public static Set<Waypoint> loadWaypoints(final Viewport viewport) {
        final StringBuilder where = buildCoordinateWhere(dbTableWaypoints, viewport);

        final StringBuilder query = new StringBuilder("SELECT ");
        for (int i = 0; i < WAYPOINT_COLUMNS.length; i++) {
            query.append(i > 0 ? ", " : "").append(dbTableWaypoints).append('.').append(WAYPOINT_COLUMNS[i]).append(' ');
        }
        query.append(" FROM ").append(dbTableWaypoints).append(", ").append(dbTableCaches).append(" WHERE ").append(dbTableWaypoints)
                .append(".geocode == ").append(dbTableCaches).append(".geocode AND ").append(where)
                .append(" LIMIT ").append(Math.max(10, Settings.getKeyInt(R.integer.waypoint_threshold_max)) * 2);  // Hardcoded limit to avoid memory overflow

        return cursorToColl(database.rawQuery(query.toString(), null), new HashSet<>(), DataStore::createWaypointFromDatabaseContent);
    }

    public static void saveChangedCache(final Geocache cache) {
        saveCache(cache, cache.inDatabase() ? LoadFlags.SAVE_ALL : EnumSet.of(SaveFlag.CACHE));
    }

    private enum PreparedStatement {

        HISTORY_COUNT("SELECT COUNT(*) FROM " + dbTableCaches + " WHERE visiteddate > 0 OR geocode IN (SELECT geocode FROM " + dbTableLogsOffline + ")"),
        MOVE_TO_STANDARD_LIST("UPDATE " + dbTableCachesLists + " SET list_id = " + StoredList.STANDARD_LIST_ID + " WHERE list_id = ? AND geocode NOT IN (SELECT DISTINCT (geocode) FROM " + dbTableCachesLists + " WHERE list_id = " + StoredList.STANDARD_LIST_ID + ")"),
        REMOVE_FROM_LIST("DELETE FROM " + dbTableCachesLists + " WHERE list_id = ? AND geocode = ?"),
        REMOVE_FROM_ALL_LISTS("DELETE FROM " + dbTableCachesLists + " WHERE geocode = ?"),
        REMOVE_ALL_FROM_LIST("DELETE FROM " + dbTableCachesLists + " WHERE list_id = ?"),
        UPDATE_VISIT_DATE("UPDATE " + dbTableCaches + " SET visiteddate = ? WHERE geocode = ?"),
        INSERT_LOG_IMAGE("INSERT INTO " + dbTableLogImages + " (log_id, title, url, description) VALUES (?, ?, ?, ?)"),
        INSERT_LOG_COUNTS("INSERT INTO " + dbTableLogCount + " (geocode, updated, type, count) VALUES (?, ?, ?, ?)"),
        INSERT_SPOILER("INSERT INTO " + dbTableSpoilers + " (geocode, updated, url, title, description) VALUES (?, ?, ?, ?, ?)"),
        REMOVE_SPOILERS("DELETE FROM " + dbTableSpoilers + " WHERE geocode = ?"),
        OFFLINE_LOG_ID_OF_GEOCODE("SELECT _id FROM " + dbTableLogsOffline + " WHERE geocode = ?"),
        COUNT_CACHES_ON_STANDARD_LIST("SELECT COUNT(geocode) FROM " + dbTableCachesLists + " WHERE list_id = " + StoredList.STANDARD_LIST_ID),
        COUNT_ALL_CACHES("SELECT COUNT(DISTINCT(geocode)) FROM " + dbTableCachesLists + " WHERE list_id >= " + StoredList.STANDARD_LIST_ID),
        INSERT_LOG("INSERT INTO " + dbTableLogs + " (geocode, updated, service_log_id, type, author, author_guid, log, date, found, friend) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"),
        CLEAN_LOG("DELETE FROM " + dbTableLogs + " WHERE geocode = ? AND date >= ? AND date <= ? AND type = ? AND author = ?"),
        INSERT_ATTRIBUTE("INSERT INTO " + dbTableAttributes + " (geocode, updated, attribute) VALUES (?, ?, ?)"),
        ADD_TO_LIST("INSERT OR REPLACE INTO " + dbTableCachesLists + " (list_id, geocode) VALUES (?, ?)"),
        GEOCODE_OFFLINE("SELECT COUNT(l.list_id) FROM " + dbTableCachesLists + " l, " + dbTableCaches + " c WHERE c.geocode = ? AND c.geocode = l.geocode AND c.detailed = 1 AND l.list_id != " + StoredList.TEMPORARY_LIST.id),
        GUID_OFFLINE("SELECT COUNT(l.list_id) FROM " + dbTableCachesLists + " l, " + dbTableCaches + " c WHERE c.guid = ? AND c.geocode = l.geocode AND c.detailed = 1 AND list_id != " + StoredList.TEMPORARY_LIST.id),
        GEOCODE_OF_GUID("SELECT geocode FROM " + dbTableCaches + " WHERE guid = ?"),
        GEOCODE_FROM_TITLE("SELECT geocode FROM " + dbTableCaches + " WHERE name = ?"),
        INSERT_TRAILPOINT("INSERT INTO " + dbTableTrailHistory + " (latitude, longitude, altitude, timestamp) VALUES (?, ?, ?, ?)"),
        INSERT_ROUTEITEM("INSERT INTO " + dbTableRoute + " (precedence, type, id, latitude, longitude) VALUES (?, ?, ?, ?, ?)"),
        COUNT_TYPE_ALL_LIST("SELECT COUNT(DISTINCT(c._id)) FROM " + dbTableCaches + " c, " + dbTableCachesLists + " l  WHERE c.type = ? AND c.geocode = l.geocode AND l.list_id > 0"), // See use of COUNT_TYPE_LIST for synchronization
        COUNT_ALL_TYPES_ALL_LIST("SELECT COUNT(DISTINCT(c._id)) FROM " + dbTableCaches + " c, " + dbTableCachesLists + " l WHERE c.geocode = l.geocode AND l.list_id  > 0"), // See use of COUNT_TYPE_LIST for synchronization
        COUNT_TYPE_LIST("SELECT COUNT(c._id) FROM " + dbTableCaches + " c, " + dbTableCachesLists + " l WHERE c.type = ? AND c.geocode = l.geocode AND l.list_id = ?"),
        COUNT_ALL_TYPES_LIST("SELECT COUNT(c._id) FROM " + dbTableCaches + " c, " + dbTableCachesLists + " l WHERE c.geocode = l.geocode AND l.list_id = ?"), // See use of COUNT_TYPE_LIST for synchronization
        CHECK_IF_PRESENT("SELECT COUNT(*) FROM " + dbTableCaches + " WHERE geocode = ?"),
        SEQUENCE_SELECT("SELECT seq FROM " + dbTableSequences + " WHERE name = ?"),
        SEQUENCE_UPDATE("UPDATE " + dbTableSequences + " SET seq = ? WHERE name = ?"),
        SEQUENCE_INSERT("INSERT INTO " + dbTableSequences + " (name, seq) VALUES (?, ?)"),
        GET_ALL_STORED_LOCATIONS("SELECT DISTINCT c.location FROM " + dbTableCaches + " c WHERE c.location IS NOT NULL"),
        SET_CACHE_ICON("UPDATE " + dbTableCaches + " SET emoji = ? WHERE geocode = ?");

        private static final List<PreparedStatement> statements = new ArrayList<>();

        @Nullable
        private volatile SQLiteStatement statement = null; // initialized lazily
        final String query;

        PreparedStatement(final String query) {
            this.query = query;
        }

        public long simpleQueryForLong() {
            return getStatement().simpleQueryForLong();
        }

        private SQLiteStatement getStatement() {
            if (statement == null) {
                synchronized (statements) {
                    if (statement == null) {
                        init();
                        statement = database.compileStatement(query);
                        statements.add(this);
                    }
                }
            }
            return statement;
        }

        private static void clearPreparedStatements() {
            for (final PreparedStatement preparedStatement : statements) {
                final SQLiteStatement statement = preparedStatement.statement;
                if (statement != null) {
                    statement.close();
                    preparedStatement.statement = null;
                }
            }
            statements.clear();
        }

    }

    public static void saveVisitDate(final String geocode, final long visitedDate) {
        setVisitDate(Collections.singletonList(geocode), visitedDate);
    }

    public static Map<String, Set<Integer>> markDropped(final Collection<Geocache> caches) {
        final SQLiteStatement remove = PreparedStatement.REMOVE_FROM_ALL_LISTS.getStatement();
        final Map<String, Set<Integer>> oldLists = new HashMap<>();

        database.beginTransaction();
        try {
            final Set<String> geocodes = new HashSet<>(caches.size());
            for (final Geocache cache : caches) {
                oldLists.put(cache.getGeocode(), loadLists(cache.getGeocode()));

                remove.bindString(1, cache.getGeocode());
                remove.execute();
                geocodes.add(cache.getGeocode());

                cache.getLists().clear();
            }
            clearVisitDate(geocodes);
            clearLogsOffline(caches);

            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

        return oldLists;
    }

    @Nullable
    public static Viewport getBounds(final String geocode, final boolean withWaypoints) {
        if (geocode == null) {
            return null;
        }

        return getBounds(Collections.singleton(geocode), withWaypoints);
    }

    @Nullable
    public static Viewport getBounds(final String geocode) {
        return getBounds(geocode, false);
    }

    public static void clearVisitDate(final Collection<String> selected) {
        setVisitDate(selected, 0);
    }

    @NonNull
    public static SearchResult getBatchOfStoredCaches(final Geopoint coords, final int listId) {
        return getBatchOfStoredCaches(coords, listId, null, null, false, -1);
    }

    @NonNull
    public static SearchResult getBatchOfStoredCaches(final Geopoint coords, final int listId, final GeocacheFilter filter, final CacheComparator sort, final boolean sortInverse, final int limit) {
        final Set<String> geocodes = loadBatchOfStoredGeocodes(coords, listId, filter, sort, sortInverse, limit);
        return new SearchResult(null, geocodes, getAllStoredCachesCount(listId));
    }

    public static boolean saveWaypoint(final int id, final String geocode, final Waypoint waypoint) {
        if (saveWaypointInternal(id, geocode, waypoint)) {
            removeCache(geocode, EnumSet.of(RemoveFlag.CACHE));
            return true;
        }
        return false;
    }

    @Nullable
    public static Cursor findSuggestions(final String searchTerm) {
        // require 3 characters, otherwise there are to many results
        if (StringUtils.length(searchTerm) < 3) {
            return null;
        }
        init();
        final GeocacheSearchSuggestionCursor resultCursor = new GeocacheSearchSuggestionCursor();
        try {
            final String selectionArg = getSuggestionArgument(searchTerm);
            findCaches(resultCursor, selectionArg);
            findTrackables(resultCursor, selectionArg);
        } catch (final Exception e) {
            Log.e("DataStore.loadBatchOfStoredGeocodes", e);
        }
        return resultCursor;
    }

    private static void findCaches(final GeocacheSearchSuggestionCursor resultCursor, final String selectionArg) {
        final Cursor cursor = database.query(
                dbTableCaches,
                new String[]{"geocode", "name", "type"},
                "geocode IS NOT NULL AND geocode != '' AND (geocode LIKE ? OR name LIKE ? OR owner LIKE ?)",
                new String[]{selectionArg, selectionArg, selectionArg},
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

    @NonNull
    private static String getSuggestionArgument(final String input) {
        return "%" + StringUtils.trim(input) + "%";
    }

    private static void findTrackables(final MatrixCursor resultCursor, final String selectionArg) {
        final Cursor cursor = database.query(
                dbTableTrackables,
                new String[]{"tbcode", "title"},
                "tbcode IS NOT NULL AND tbcode != '' AND (tbcode LIKE ? OR title LIKE ?)",
                new String[]{selectionArg, selectionArg},
                null,
                null,
                "title");
        while (cursor.moveToNext()) {
            final String tbcode = cursor.getString(0);
            resultCursor.addRow(new String[]{
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

    @NonNull
    public static String[] getSuggestions(final String table, final String column, final String input) {
        return getSuggestions(table, column, column, input, null);
    }

    @NonNull
    public static String[] getSuggestions(final String table, final String columnSearchValue, final String columnReturnValue, final String input, final Func1<String, String[]> processor) {

        try {
            final Cursor cursor = database.rawQuery("SELECT DISTINCT " + columnReturnValue
                    + " FROM " + table
                    + " WHERE " + columnSearchValue + " LIKE ?"
                    + " ORDER BY " + columnSearchValue + " COLLATE NOCASE ASC;", new String[]{getSuggestionArgument(input)});
            final Collection<String> coll = cursorToColl(cursor, new LinkedList<>(), GET_STRING_0);
            if (processor == null) {
                return coll.toArray(new String[0]);
            }
            return processAndSortSuggestions(coll, input, processor).toArray(new String[0]);
        } catch (final RuntimeException e) {
            Log.e("cannot get suggestions from " + table + "->" + columnSearchValue + " for input '" + input + "'", e);
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
    }

    private static List<String> processAndSortSuggestions(final Collection<String> rawList, final String input, final Func1<String, String[]> processor) {
        final String lowerInput = input.toLowerCase(Locale.getDefault());
        final Set<String> newColl = new HashSet<>();
        for (String value : rawList) {
            for (String token : processor.call(value)) {
                if (token.toLowerCase(Locale.getDefault()).contains(lowerInput)) {
                    newColl.add(token.trim());
                }
            }
        }
        final List<String> sortedList = new ArrayList<>(newColl);
        TextUtils.sortListLocaleAware(sortedList);
        return sortedList;
    }

    @NonNull
    public static String[] getSuggestionsOwnerName(final String input) {
        return getSuggestions(dbTableCaches, "owner_real", input);
    }

    @NonNull
    public static String[] getSuggestionsFinderName(final String input) {
        return getSuggestions(dbTableLogs, "author", input);
    }

    @NonNull
    public static String[] getSuggestionsTrackableCode(final String input) {
        return getSuggestions(dbTableTrackables, "tbcode", input);
    }

    @NonNull
    public static String[] getSuggestionsGeocode(final String input) {
        return getSuggestions(dbTableCaches, "geocode", input);
    }

    /**
     * @return geocodes (!) where the cache name is matching
     */
    @NonNull
    public static String[] getSuggestionsKeyword(final String input) {
        return getSuggestions(dbTableCaches, "name", "geocode", input, null);
    }

    @NonNull
    public static String[] getSuggestionsLocation(final String input) {
        return getSuggestions(dbTableCaches, "location", "location", input, s -> s.split(","));
    }

    /**
     * @return list of last caches opened in the details view, ordered by most recent first
     */
    @NonNull
    public static List<Geocache> getLastOpenedCaches() {
        final List<String> geocodes = Settings.getLastOpenedCaches();
        final Set<Geocache> cachesSet = loadCaches(geocodes, LoadFlags.LOAD_CACHE_OR_DB);

        // order result set by time again
        final List<Geocache> caches = new ArrayList<>(cachesSet);
        Collections.sort(caches, (lhs, rhs) -> {
            final int lhsIndex = geocodes.indexOf(lhs.getGeocode());
            final int rhsIndex = geocodes.indexOf(rhs.getGeocode());
            return Integer.compare(lhsIndex, rhsIndex);
        });
        return caches;
    }

    /**
     * migrate most recent history waypoints (up to 5)
     * (temporary workaround for on demand migration of the old "go to" history,
     * should be removed after some grace period, probably end of 2020?)
     */
    public static void migrateGotoHistory(final Context context) {
        init();

        final String sql = "INSERT INTO " + dbTableWaypoints + " (geocode, updated, type, prefix, lookup, name, latitude, longitude, note, own, visited, user_note, org_coords_empty, calc_state)"
                + " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        final SQLiteStatement statement = database.compileStatement(sql);

        try (Cursor cursor = database.query(dbTableSearchDestinationHistory, new String[]{"_id", "date", "latitude", "longitude"}, null, null, null, null, "_id DESC", "5")) {
            int sequence = 1;
            if (cursor.moveToLast()) {
                do {
                    statement.bindString(1, InternalConnector.GEOCODE_HISTORY_CACHE);  // geocode
                    statement.bindLong(2, getLongDate(cursor));                      // updated
                    statement.bindString(3, "waypoint");                         // type
                    statement.bindString(4, "00");                               // prefix
                    statement.bindString(5, "---");                              // lookup
                    statement.bindString(6, context.getString(R.string.wp_waypoint) + " " + sequence);      // name
                    statement.bindDouble(7, getDouble(cursor, "latitude"));      // latitude
                    statement.bindDouble(8, getDouble(cursor, "longitude"));     // longitude
                    statement.bindString(9, "");                                 // note
                    statement.bindLong(10, 1);                                 // own
                    statement.bindLong(11, 0);                                 // visited
                    statement.bindString(12, "");                                // user note
                    statement.bindLong(13, 0);                                 // org_coords_empty
                    statement.bindNull(14);                                    // calc_state
                    statement.executeInsert();
                    sequence++;
                } while (cursor.moveToPrevious());
            }
        }

        // clear old history
        database.execSQL("DELETE FROM " + dbTableSearchDestinationHistory);
    }

    private static double getDouble(final Cursor cursor, final String rowName) {
        try {
            return cursor.getDouble(cursor.getColumnIndexOrThrow(rowName));
        } catch (final IllegalArgumentException e) {
            Log.e("Table row '" + rowName + "' not found", e);
        }
        // set default
        return 0;
    }

    private static long getLongDate(final Cursor cursor) {
        try {
            return cursor.getLong(cursor.getColumnIndexOrThrow("date"));
        } catch (final IllegalArgumentException e) {
            Log.e("Table row 'date' not found", e);
        }
        // set default
        return 0;
    }

    /**
     * Helper methods for Offline Logs
     */
    private static class DBLogOfflineUtils {

        public static boolean save(final String geocode, final OfflineLogEntry logEntry) {
            try (ContextLogger cLog = new ContextLogger("DBLogOfflineUtils.save(geocode=%s)", geocode)) {
                if (StringUtils.isBlank(geocode)) {
                    Log.e("DataStore.saveLogOffline: cannot log a blank geocode");
                    return false;
                }
                if (logEntry.logType == LogType.UNKNOWN && StringUtils.isBlank(logEntry.log)) {
                    Log.e("DataStore.saveLogOffline: cannot log an unknown log type and no message");
                    return false;
                }
                if (StringUtils.isNotBlank(logEntry.cacheGeocode) && !logEntry.cacheGeocode.equals(geocode)) {
                    Log.e("DataStore.saveLogOffline: mismatch between geocode in LogENtry and provided geocode: " + geocode + "<->" + logEntry.cacheGeocode);
                    return false;
                }


                init();
                database.beginTransaction();
                try {

                    //main entry
                    final ContentValues values = new ContentValues();
                    values.put("geocode", geocode);
                    values.put("updated", System.currentTimeMillis());
                    values.put("service_log_id", logEntry.serviceLogId);
                    values.put("type", logEntry.logType.id);
                    values.put("log", logEntry.log);
                    values.put("date", logEntry.date);
                    values.put("report_problem", logEntry.reportProblem.code);

                    values.put("image_title_prefix", logEntry.imageTitlePraefix);
                    values.put("image_scale", logEntry.imageScale);
                    values.put("tweet", logEntry.tweet ? 1 : 0);
                    values.put("favorite", logEntry.favorite ? 1 : 0);
                    values.put("rating", logEntry.rating);
                    values.put("password", logEntry.password);

                    long offlineLogId = getLogOfflineId(geocode);
                    if (offlineLogId >= 0) {
                        final int rows = database.update(dbTableLogsOffline, values, "geocode = ?", new String[]{geocode});
                        if (rows < 1) {
                            return false;
                        }
                    } else {
                        offlineLogId = database.insert(dbTableLogsOffline, null, values);
                        if (offlineLogId < 0) {
                            return false;
                        }
                    }
                    final long finalOfflineLogId = offlineLogId;
                    cLog.add("logId=%s", finalOfflineLogId);

                    //image entries
                    final List<ContentValues> images = CollectionStream.of(logEntry.logImages).map(img -> {
                        final ContentValues cv = new ContentValues();
                        cv.put("logoffline_id", finalOfflineLogId);
                        cv.put("url", img.getUrl());
                        cv.put("description", img.getDescription());
                        cv.put("title", img.getTitle());
                        cv.put("scale", img.targetScale);
                        return cv;
                    }).toList();
                    updateRowset(database, dbTableLogsOfflineImages, images, "logoffline_id = " + offlineLogId, null);
                    cLog.add("images:%s", images.size());

                    //trackable entries
                    final List<ContentValues> trackables = CollectionStream.of(logEntry.trackableActions.entrySet()).map(tr -> {
                        final ContentValues cv = new ContentValues();
                        cv.put("logoffline_id", finalOfflineLogId);
                        cv.put("tbcode", tr.getKey());
                        cv.put("actioncode", tr.getValue().id);
                        return cv;
                    }).toList();
                    updateRowset(database, dbTableLogsOfflineTrackables, trackables, "logoffline_id = " + offlineLogId, null);
                    cLog.add("trackables:%s", trackables.size());

                    database.setTransactionSuccessful();
                    return true;
                } finally {
                    database.endTransaction();
                }
            }
        }

        private static void updateRowset(final SQLiteDatabase db, final String table, final List<ContentValues> newRows, final String whereSelectExisting, final String[] whereArgs) {

            //make it easy for now: delete and re-insert everything
            db.delete(table, whereSelectExisting, whereArgs);
            for (final ContentValues cv : newRows) {
                db.insert(table, null, cv);
            }
        }


        //TODO
        @Nullable
        public static OfflineLogEntry load(final String geocode) {

            try (ContextLogger cLog = new ContextLogger("DBLogOfflineUtils.load(geocode=%s)", geocode)) {
                if (StringUtils.isBlank(geocode)) {
                    return null;
                }

                init();

                final DBQuery query = new DBQuery.Builder().setTable(dbTableLogsOffline)
                        .setColumns(new String[]{"_id", "geocode", "date", "service_log_id", "type", "log", "report_problem", "image_title_prefix", "image_scale", "favorite", "tweet", "rating", "password"})
                        .setWhereClause("geocode = ?").setWhereArgs(new String[]{geocode}).build();
                final OfflineLogEntry.Builder<?> logBuilder = query.selectFirstRow(database,
                        c -> new OfflineLogEntry.Builder<>()
                                .setId(c.getInt(0))
                                .setCacheGeocode(c.getString(1))
                                .setDate(c.getLong(2))
                                .setServiceLogId(c.getString(3))
                                .setLogType(LogType.getById(c.getInt(4)))
                                .setLog(c.getString(5))
                                .setReportProblem(ReportProblemType.findByCode(c.getString(6)))
                                .setImageTitlePraefix(c.getString(7))
                                .setImageScale(c.getInt(8))
                                .setFavorite(c.getInt(9) > 0)
                                .setTweet(c.getInt(10) > 0)
                                .setRating(c.isNull(11) ? null : c.getFloat(11))
                                .setPassword(c.getString(12))
                );

                if (logBuilder == null) {
                    //no entry available in DB
                    cLog.add("not found");
                    return null;
                }

                final int logId = logBuilder.getId();
                cLog.addReturnValue("LogId:" + logId);

                //images
                final DBQuery queryImages = new DBQuery.Builder().setTable(dbTableLogsOfflineImages)
                        .setColumns(new String[]{"url", "title", "description", "scale"})
                        .setWhereClause("logoffline_id = " + logId).build();
                queryImages.selectRows(database,
                        c -> logBuilder.addLogImage(new Image.Builder()
                                .setUrl(adjustOfflineLogImageUri(c.getString(0)))
                                .setTitle(c.getString(1))
                                .setDescription(c.getString(2))
                                .setTargetScale(c.isNull(3) ? -1 : c.getInt(3))
                                .build())
                );

                //trackables
                final DBQuery queryTrackables = new DBQuery.Builder().setTable(dbTableLogsOfflineTrackables)
                        .setColumns(new String[]{"tbcode", "actioncode"})
                        .setWhereClause("logoffline_id = " + logId).build();
                queryTrackables.selectRows(database,
                        c -> logBuilder.addTrackableAction(
                                c.getString(0),
                                LogTypeTrackable.getById(ObjectUtils.defaultIfNull(c.getInt(1), LogTypeTrackable.UNKNOWN.id))
                        )
                );

                return logBuilder.build();
            }
        }

        private static Uri adjustOfflineLogImageUri(final String imageUri) {
            if (StringUtils.isBlank(imageUri)) {
                return Uri.EMPTY;
            }
            return ImageUtils.adjustOfflineLogImageUri(Uri.parse(imageUri));
        }

        public static boolean remove(final String geocode) {
            if (StringUtils.isBlank(geocode)) {
                return false;
            }

            init();

            final String[] geocodeWhereArgs = {geocode};
            return DBLogOfflineUtils.remove(database, "geocode = ?", geocodeWhereArgs) > 0;
        }

        public static int remove(final Collection<Geocache> caches) {
            if (CollectionUtils.isEmpty(caches)) {
                return 0;
            }

            init();

            final String geocodes = whereGeocodeIn(Geocache.getGeocodes(caches)).toString();
            return DBLogOfflineUtils.remove(database, geocodes, null);
        }

        /**
         * if returned id is < 0 then there is no offline log for given geocode
         */
        public static long getLogOfflineId(final String geocode) {
            if (StringUtils.isBlank(geocode)) {
                return -1;
            }

            init();
            try {
                final SQLiteStatement logIdStmt = PreparedStatement.OFFLINE_LOG_ID_OF_GEOCODE.getStatement();
                synchronized (logIdStmt) {
                    logIdStmt.bindString(1, geocode);
                    return logIdStmt.simpleQueryForLong();
                }
            } catch (final Exception e) {
                //ignore SQLiteDoneException, it is thrown when no row is returned which we expect here regularly
                if (!(e instanceof SQLiteDoneException)) {
                    Log.e("DataStore.hasLogOffline", e);
                }
            }

            return -1;
        }

        private static int remove(final SQLiteDatabase db, final String whereClause, final String[] whereArgs) {
            database.delete(dbTableLogsOfflineImages, "logoffline_id in (select _id from " + dbTableLogsOffline + " where " + whereClause + ")", whereArgs);
            database.delete(dbTableLogsOfflineTrackables, "logoffline_id in (select _id from " + dbTableLogsOffline + " where " + whereClause + ")", whereArgs);
            return database.delete(dbTableLogsOffline, whereClause, whereArgs);
        }

        public static void cleanOrphanedRecords(final SQLiteDatabase db) {
            Log.d("Database clean: removing entries for non-existing caches from logs offline");
            database.delete(dbTableLogsOffline, "geocode NOT IN (SELECT geocode FROM " + dbTableCaches + ")", null);
            database.delete(dbTableLogsOfflineImages, "logoffline_id NOT IN (SELECT _id FROM " + dbTableLogsOffline + ")", null);
            database.delete(dbTableLogsOfflineTrackables, "logoffline_id NOT IN (SELECT _id FROM " + dbTableLogsOffline + ")", null);
        }
    }

    public static class DBQuery {
        public final String table;
        public final String[] columns;
        public final String whereClause;
        public final String[] whereArgs;
        public final String having;
        public final String groupBy;
        public final String orderBy;
        public final String limit;

        private DBQuery(final Builder builder) {
            this.table = builder.table;
            this.columns = builder.columns;
            this.whereClause = builder.whereClause;
            this.whereArgs = builder.whereArgs;
            this.having = builder.having;
            this.groupBy = builder.groupBy;
            this.orderBy = builder.orderBy;
            this.limit = builder.limit;
        }

        public <T> List<T> selectRows(final SQLiteDatabase db, final Func1<Cursor, T> mapper) {

            try (Cursor c = openCursorFor(db, null)) {
                final List<T> result = new ArrayList<>();
                while (c.moveToNext()) {
                    result.add(mapper.call(c));
                }
                return result;
            }
        }

        public <T> T selectFirstRow(final SQLiteDatabase db, final Func1<Cursor, T> mapper) {

            try (Cursor c = openCursorFor(db, "1")) {
                final List<T> result = new ArrayList<>();
                if (c.moveToNext()) {
                    return mapper.call(c);
                }
                return null;
            }
        }

        public Cursor openCursorFor(final SQLiteDatabase db, final String limitOverride) {
            return db.query(
                    this.table, this.columns, this.whereClause, this.whereArgs, this.groupBy, this.having,
                    this.orderBy, limitOverride == null ? this.limit : limitOverride
            );
        }

        public static class Builder {
            private String table;
            private String[] columns;
            private String whereClause;
            private String[] whereArgs;
            private String having;
            private String groupBy;
            private String orderBy;
            private String limit;

            public DBQuery build() {
                return new DBQuery(this);
            }

            public Builder setTable(final String table) {
                this.table = table;
                return this;
            }

            public Builder setColumns(final String[] columns) {
                this.columns = columns;
                return this;
            }

            public Builder setWhereClause(final String whereClause) {
                this.whereClause = whereClause;
                return this;
            }

            public Builder setWhereArgs(final String[] whereArgs) {
                this.whereArgs = whereArgs;
                return this;
            }

            public Builder setHaving(final String having) {
                this.having = having;
                return this;
            }

            public Builder setGroupBy(final String groupBy) {
                this.groupBy = groupBy;
                return this;
            }

            public Builder setOrderBy(final String orderBy) {
                this.orderBy = orderBy;
                return this;
            }

            public Builder setLimit(final String limit) {
                this.limit = limit;
                return this;
            }
        }
    }

}
