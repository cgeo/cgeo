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

package cgeo.geocaching.storage

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.DBInspectionActivity
import cgeo.geocaching.Intents
import cgeo.geocaching.R
import cgeo.geocaching.SearchResult
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.capability.ILogin
import cgeo.geocaching.connector.internal.InternalConnector
import cgeo.geocaching.enumerations.CacheSize
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.enumerations.LoadFlags.LoadFlag
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag
import cgeo.geocaching.enumerations.ProjectionType
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.filters.core.ListIdGeocacheFilter
import cgeo.geocaching.list.AbstractList
import cgeo.geocaching.list.PseudoList
import cgeo.geocaching.list.StoredList
import cgeo.geocaching.location.DistanceUnit
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.log.LogType
import cgeo.geocaching.log.LogTypeTrackable
import cgeo.geocaching.log.OfflineLogEntry
import cgeo.geocaching.log.ReportProblemType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.INamedGeoCoordinate
import cgeo.geocaching.models.Image
import cgeo.geocaching.models.Route
import cgeo.geocaching.models.RouteItem
import cgeo.geocaching.models.RouteSegment
import cgeo.geocaching.models.Trackable
import cgeo.geocaching.models.TrailHistoryElement
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.models.bettercacher.Category
import cgeo.geocaching.models.bettercacher.Tier
import cgeo.geocaching.network.HtmlImage
import cgeo.geocaching.search.GeocacheSearchSuggestionCursor
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.sorting.CacheComparator
import cgeo.geocaching.storage.extension.DBDowngradeableVersions
import cgeo.geocaching.storage.extension.Trackfiles
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.CalendarUtils
import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.ContextLogger
import cgeo.geocaching.utils.EmojiUtils
import cgeo.geocaching.utils.EnumValueMapper
import cgeo.geocaching.utils.FileNameCreator
import cgeo.geocaching.utils.FileUtils
import cgeo.geocaching.utils.GeoHeightUtils
import cgeo.geocaching.utils.ImageUtils
import cgeo.geocaching.utils.LifecycleAwareBroadcastReceiver
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.TextUtils
import cgeo.geocaching.utils.Version
import cgeo.geocaching.utils.formulas.VariableList
import cgeo.geocaching.utils.functions.Func1
import cgeo.geocaching.Intents.ACTION_INDIVIDUALROUTE_CHANGED
import cgeo.geocaching.settings.Settings.getMaximumMapTrailLength
import cgeo.geocaching.storage.DataStore.DBExtensionType.DBEXTENSION_INVALID

import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.MatrixCursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CursorFactory
import android.database.sqlite.SQLiteDoneException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteStatement
import android.location.Location
import android.net.Uri

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes
import androidx.core.util.Supplier

import java.io.File
import java.io.FilenameFilter
import java.util.ArrayList
import java.util.Arrays
import java.util.Collection
import java.util.Collections
import java.util.Comparator
import java.util.Date
import java.util.EnumMap
import java.util.EnumSet
import java.util.HashMap
import java.util.HashSet
import java.util.Iterator
import java.util.LinkedList
import java.util.List
import java.util.Locale
import java.util.Map
import java.util.Map.Entry
import java.util.Set
import java.util.TimeZone
import java.util.TreeMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleOnSubscribe
import io.reactivex.rxjava3.schedulers.Schedulers
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.collections4.MapUtils
import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.ObjectUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.ImmutablePair

class DataStore {

    public static val DB_FILE_NAME: String = "data"
    public static val DB_FILE_NAME_BACKUP: String = "cgeo.sqlite"
    public static val DB_FILE_CORRUPTED_EXTENSION: String = ".corrupted"

    // some fields names which are referenced multiple times
    // name scheme is "FIELD_" + table name without prefix + "_" + field name
    private static val FIELD_LISTS_PREVENTASKFORDELETION: String = "preventAskForDeletion"

    enum class class DBRestoreResult {
        RESTORE_SUCCESSFUL(R.string.init_restore_success),
        RESTORE_FAILED_GENERAL(R.string.init_restore_db_failed),
        RESTORE_FAILED_DBRECREATED(R.string.init_restore_failed_dbrecreated)

        public final @StringRes Int res

        DBRestoreResult(final Int res) {
            this.res = res
        }
    }

    enum class class StorageLocation {
        HEAP,
        CACHE,
        DATABASE,
    }

    enum class class DBExtensionType {
        // values for id must not be changed, as there are database entries depending on it
        DBEXTENSION_INVALID(0),
        DBEXTENSION_PENDING_DOWNLOAD(1),
        DBEXTENSION_FOUNDNUM(2),
        DBEXTENSION_DOWNGRADEABLE_DBVERSION(3),
        DBEXTENSION_ONE_TIME_DIALOGS(4),
        DBEXTENSION_EMOJILRU(5),
        DBEXTENSION_POCKETQUERY_HISTORY(6),
        DBEXTENSION_TRACKFILES(7),
        DBEXTENSION_LAST_TRACKABLE_ACTION(8)

        public final Int id
        private static val mapper: EnumValueMapper<Integer, DBExtensionType> = EnumValueMapper<>()

        static {
            for (DBExtensionType type : values()) {
                mapper.add(type, type.id)
            }
        }

        DBExtensionType(final Int id) {
            this.id = id
        }

        public static String getNameFor(final Int id) {
            val type: DBExtensionType = mapper.get(id)
            return type == null ? "unknown:" + id : type.name()
        }
    }

    private static val GET_STRING_0: Func1<Cursor, String> = cursor -> cursor.getString(0)

    private static val GET_INTEGER_0: Func1<Cursor, Integer> = cursor -> cursor.getInt(0)

    // Columns and indices for the cache data
    private static val QUERY_CACHE_DATA: String =
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
                    "cg_caches.alcMode," +       // 46
                    "cg_caches.tier"; // 47

    /**
     * The list of fields needed for mapping.
     */
    private static final String[] WAYPOINT_COLUMNS = {"_id", "geocode", "updated", "type", "prefix", "lookup", "name", "latitude", "longitude", "note", "own", "visited", "user_note", "org_coords_empty", "calc_state", "projection_type", "projection_unit", "projection_formula_1", "projection_formula_2", "preprojected_latitude", "preprojected_longitude", "geofence"}

    /**
     * Number of days (as ms) after temporarily saved caches are deleted
     */
    private static val DAYS_AFTER_CACHE_IS_DELETED: Long = 3 * 24 * 60 * 60 * 1000

    /**
     * holds the column indexes of the cache table to avoid lookups
     */
    private static val cacheCache: CacheCache = CacheCache()
    private static volatile SQLiteDatabase database = null
    private static val databaseLock: ReentrantReadWriteLock = ReentrantReadWriteLock()
    private static val dbVersion: Int = 106
    public static val customListIdOffset: Int = 10

    /**
     * The following constant lists all DBVERSIONS whose changes with the previous version
     * are DOWNWARD-COMPATIBLE. More precisely: if a version x shows up in this list, then this
     * means that c:geo version written for DB version "x-1" can also work with this database.
     * <br>
     * As a rule-of-thumb, a db version is downward compatible if:
     * * it only adds columns which are nullable or provide default values (so previous c:geo-versions don't fail on insert/update)
     * * it only adds tables which don't necessarily need an entry (because previous c:geo-versions will not write anything in there)
     * * migration from "x-1" to x in {@link DbHelper#onUpgrade(SQLiteDatabase, Int, Int)} is programmed such that it can handle it if later
     * db is "upgraded" again from "x-1" to x (this is usually the case if adding tables/columns will not fail if object already exists in db)
     * <br>
     * The following changes usually make a DB change NOT downward compatible
     * * changing name, type or other attributes for a column
     * * adding columns which are not nullable
     * * any change which also requires some sort of data migration
     * * {@link DbHelper#onUpgrade(SQLiteDatabase, Int, Int)} will fail later if db is "upgraded" again from "x-1" to x
     */
    private static val DBVERSIONS_DOWNWARD_COMPATIBLE: Set<Integer> = HashSet<>(Arrays.asList(
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
            99,  // add alcMode to differentiate Linear vs Random
            100, // add column "tier" and table for cache categories. Initially used for bettercacher.org data
            101, // add service_image_id to saved log images
            102, // add projection attributes to waypoints
            103, // add more projection attributes to waypoints
            104,  // add geofence radius for lab stages
            105,  // Migrate UDC geocodes from ZZ1000-based numbers to random ones
            106  // Update lab caches DT rating to zero from minus one
    ))

    private static val dbTableCaches: String = "cg_caches"
        public static val dbFieldCaches_type: String = "type"
        public static val dbFieldCaches_owner_real: String = "owner_real"
        public static val dbFieldCaches_favourite_cnt: String = "favourite_cnt"
        public static val dbFieldCaches_myvote: String = "myvote"
        public static val dbFieldCaches_disabled: String = "disabled"
        public static val dbFieldCaches_archived: String = "archived"
        public static val dbFieldCaches_members: String = "members"
        public static val dbFieldCaches_found: String = "found"
        public static val dbFieldCaches_favourite: String = "favourite"
        public static val dbFieldCaches_inventoryunknown: String = "inventoryunknown"
        public static val dbFieldCaches_onWatchList: String = "onWatchList"
        public static val dbFieldCaches_coordsChanged: String = "coordsChanged"
    public static val dbTableLists: String = "cg_lists"
    public static val dbTableCachesLists: String = "cg_caches_lists"
        public static val dbFieldCachesLists_list_id: String = "list_id"
    public static val dbTableAttributes: String = "cg_attributes"
        public static val dbFieldAttributes_Attribute: String = "attribute"
    public static val dbTableWaypoints: String = "cg_waypoints"
        public static val dbFieldWaypoints_type: String = "type"
        public static val dbFieldWaypoints_own: String = "own"
    private static val dbTableVariables: String = "cg_variables"
    public static val dbTableCategories: String = "cg_categories"
        public static val dbFieldCategories_Category: String = "category"
    private static val dbTableSpoilers: String = "cg_spoilers"
    public static val dbTableLogs: String = "cg_logs"
        public static val dbFieldLogs_Type: String = "type"
        public static val dbFieldLogs_author: String = "author"
        public static val dbFieldLogs_log: String = "log"
    public static val dbTableLogCount: String = "cg_logCount"
        public static val dbFieldLogCount_Type: String = "type"
        public static val dbFieldLogCount_Count: String = "count"
    private static val dbTableLogImages: String = "cg_logImages"
    public static val dbTableLogsOffline: String = "cg_logs_offline"
        public static val dbFieldLogsOffline_log: String = "log"
    private static val dbTableLogsOfflineImages: String = "cg_logs_offline_images"
    private static val dbTableLogsOfflineTrackables: String = "cg_logs_offline_trackables"
    private static val dbTableTrackables: String = "cg_trackables"
    private static val dbTableSearchDestinationHistory: String = "cg_search_destination_history"
    private static val dbTableTrailHistory: String = "cg_trail_history"
    public static val dbTableRoute: String = "cg_route"
        public static val dbFieldRoute_id: String = "id"
    private static val dbTableExtension: String = "cg_extension"
    private static val dbTableFilters: String = "cg_filters"

    private static final String[] dbAll = String[]{
            dbTableCaches, dbTableLists, dbTableCachesLists, dbTableAttributes, dbTableWaypoints,
            dbTableVariables, dbTableCategories, dbTableSpoilers, dbTableLogs, dbTableLogCount,
            dbTableLogImages, dbTableLogsOffline, dbTableLogsOfflineImages,
            dbTableLogsOfflineTrackables, dbTableTrackables,
            dbTableSearchDestinationHistory, dbTableTrailHistory, dbTableRoute,
            dbTableExtension, dbTableFilters
    }

    private static val dbTableSequences: String = "sqlite_sequence"

    // common table field names
    public static val dbField_Geocode: String = "geocode"
    public static val dbField_latitude: String = "latitude"
    public static val dbField_longitude: String = "longitude"

    private static val dbCreateCaches: String = ""
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
            + dbFieldCaches_type + " TEXT, "
            + "name TEXT, "
            + "owner TEXT, "
            + dbFieldCaches_owner_real + " TEXT, "
            + "hidden LONG, "
            + "hint TEXT, "
            + "size TEXT, "
            + "difficulty FLOAT, "
            + "terrain FLOAT, "
            + "location TEXT, "
            + "direction DOUBLE, "
            + "distance DOUBLE, "
            + dbField_latitude + " DOUBLE, "
            + dbField_longitude + " DOUBLE, "
            + "reliable_latlon INTEGER, "           // got unused while v96 - TODO should we remove the column?
            + "personal_note TEXT, "
            + "shortdesc TEXT, "
            + "description TEXT, "
            + dbFieldCaches_favourite_cnt + " INTEGER, "
            + "rating FLOAT, "
            + "votes INTEGER, "
            + dbFieldCaches_myvote + " FLOAT, "
            + dbFieldCaches_disabled + " INTEGER NOT NULL DEFAULT 0, "
            + dbFieldCaches_archived + " INTEGER NOT NULL DEFAULT 0, "
            + dbFieldCaches_members + " INTEGER NOT NULL DEFAULT 0, "
            + dbFieldCaches_found + " INTEGER NOT NULL DEFAULT 0, "
            + dbFieldCaches_favourite + " INTEGER NOT NULL DEFAULT 0, "
            + "inventorycoins INTEGER DEFAULT 0, "
            + "inventorytags INTEGER DEFAULT 0, "
            + dbFieldCaches_inventoryunknown + " INTEGER DEFAULT 0, "
            + dbFieldCaches_onWatchList + " INTEGER DEFAULT 0, "
            + dbFieldCaches_coordsChanged + " INTEGER DEFAULT 0, "
            + "finalDefined INTEGER DEFAULT 0, "
            + "logPasswordRequired INTEGER DEFAULT 0,"
            + "watchlistCount INTEGER DEFAULT -1,"
            + "preventWaypointsFromNote INTEGER DEFAULT 0,"
            + "owner_guid TEXT NOT NULL DEFAULT '',"
            + "emoji INTEGER DEFAULT 0,"
            + "alcMode INTEGER DEFAULT 0,"
            + "tier TEXT"
            + "); "
    private static val dbCreateLists: String = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableLists + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "title TEXT NOT NULL, "
            + "updated LONG NOT NULL,"
            + "marker INTEGER NOT NULL,"            // unused from v93 on - TODO should we remove the column?
            + "emoji INTEGER DEFAULT 0,"
            + FIELD_LISTS_PREVENTASKFORDELETION + " INTEGER DEFAULT 0"
            + "); "
    private static val dbCreateCachesLists: String = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableCachesLists + " ("
            + dbFieldCachesLists_list_id + " INTEGER NOT NULL, "
            + "geocode TEXT NOT NULL, "
            + "PRIMARY KEY (list_id, geocode)"
            + "); "
    private static val dbCreateAttributes: String = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableAttributes + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + dbField_Geocode + " TEXT NOT NULL, "
            + "updated LONG NOT NULL, " // date of save
            + dbFieldAttributes_Attribute + " TEXT "
            + "); "

    private static val dbCreateWaypoints: String = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableWaypoints + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "geocode TEXT NOT NULL, "
            + "updated LONG NOT NULL, " // date of save
            + dbFieldWaypoints_type + " TEXT NOT NULL DEFAULT 'waypoint', "
            + "prefix TEXT, "
            + "lookup TEXT, "
            + "name TEXT, "
            + dbField_latitude + " DOUBLE, "
            + dbField_longitude + " DOUBLE, "
            + "note TEXT, "
            + dbFieldWaypoints_own + " INTEGER DEFAULT 0, "
            + "visited INTEGER DEFAULT 0, "
            + "user_note TEXT, "
            + "org_coords_empty INTEGER DEFAULT 0, "
            + "calc_state TEXT, "
            + "projection_type TEXT, "
            + "projection_unit TEXT, "
            + "projection_formula_2 TEXT, "
            + "projection_formula_1 TEXT, "
            + "preprojected_latitude DOUBLE, "
            + "preprojected_longitude DOUBLE, "
            + "geofence DOUBLE"
            + "); "

    private static val dbCreateVariables: String = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableVariables + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "geocode TEXT NOT NULL, "
            + "varname TEXT, "
            + "varorder INTEGER DEFAULT 0, "
            + "formula TEXT"
            + "); "

    private static val dbCreateCategories: String = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableCategories + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + dbField_Geocode + " TEXT NOT NULL, "
            + dbFieldCategories_Category + " TEXT"
            + "); "

    private static val dbCreateSpoilers: String = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableSpoilers + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "geocode TEXT NOT NULL, "
            + "updated LONG NOT NULL, " // date of save
            + "url TEXT, "
            + "title TEXT, "
            + "description TEXT "
            + "); "
    private static val dbCreateLogs: String = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableLogs + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "geocode TEXT NOT NULL, "
            + "service_log_id TEXT," //added with db version 86
            + "updated LONG NOT NULL, " // date of save
            + dbFieldLogs_Type + " INTEGER NOT NULL DEFAULT 4, "
            + dbFieldLogs_author + " TEXT, "
            + "author_guid TEXT NOT NULL DEFAULT '', "
            + dbFieldLogs_log + " TEXT, "
            + "date LONG, "
            + "found INTEGER NOT NULL DEFAULT 0, "
            + "friend INTEGER "
            + "); "

    private static val dbCreateLogCount: String = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableLogCount + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + dbField_Geocode + " TEXT NOT NULL, "
            + "updated LONG NOT NULL, " // date of save
            + dbFieldLogCount_Type + " INTEGER NOT NULL DEFAULT 4, "
            + dbFieldLogCount_Count + " INTEGER NOT NULL DEFAULT 0 "
            + "); "
    private static val dbCreateLogImages: String = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableLogImages + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "log_id INTEGER NOT NULL, "
            + "title TEXT NOT NULL, "
            + "url TEXT NOT NULL, "
            + "description TEXT, "
            + "service_image_id TEXT"
            + "); "
    private static val dbCreateLogsOffline: String = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableLogsOffline + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "geocode TEXT NOT NULL, "
            + "updated LONG NOT NULL, " // date of save
            + "service_log_id TEXT," //added with db version 86
            + "type INTEGER NOT NULL DEFAULT 4, "
            + dbFieldLogsOffline_log + " TEXT, "
            + "date LONG, "
            + "report_problem TEXT, "
            //for version 85
            + "image_title_prefix TEXT, "
            + "image_scale INTEGER, "
            + "favorite INTEGER, "
            + "rating FLOAT, "
            + "password TEXT, "
            + "tweet INTEGER" // no longer used
            + "); "
    private static val dbCreateLogsOfflineImages: String = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableLogsOfflineImages + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "logoffline_id INTEGER NOT NULL, "
            + "url TEXT NOT NULL, "
            + "title TEXT, "
            + "description TEXT, "
            + "scale INTEGER"
            + "); "
    private static val dbCreateLogsOfflineTrackables: String = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableLogsOfflineTrackables + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "logoffline_id INTEGER NOT NULL, "
            + "tbcode TEXT NOT NULL, "
            + "actioncode INTEGER "
            + "); "

    private static val dbCreateTrackables: String = ""
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
            + "); "

    private static val dbCreateSearchDestinationHistory: String = ""
            + "CREATE TABLE IF NOT EXISTS " + dbTableSearchDestinationHistory + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "date LONG NOT NULL, "
            + dbField_latitude + " DOUBLE, "
            + dbField_longitude + " DOUBLE "
            + "); "

    private static val dbCreateTrailHistory: String = "CREATE TABLE IF NOT EXISTS " + dbTableTrailHistory + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + dbField_latitude + " DOUBLE, "
            + dbField_longitude + " DOUBLE, "
            + "altitude DOUBLE, "
            + "timestamp LONG"
            + "); "

    private static val dbCreateRoute: String = "CREATE TABLE IF NOT EXISTS " + dbTableRoute + " ("
            + "precedence INTEGER, "
            + "type INTEGER, "
            + dbFieldRoute_id + " TEXT, "
            + dbField_latitude + " DOUBLE, "
            + dbField_longitude + " DOUBLE "
            + "); "

    @SuppressWarnings("SyntaxError")
    private static val dbCreateExtension: String = "CREATE TABLE IF NOT EXISTS " + dbTableExtension + " ("
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
            + "); "

    private static val dbCreateFilters: String = "CREATE TABLE IF NOT EXISTS " + dbTableFilters + " ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "name TEXT NOT NULL UNIQUE, "
            + "treeconfig TEXT"
            + "); "

    // reminder to myself: when adding a CREATE TABLE statement:
    // make sure to add it to both onUpgrade() and onCreate()

    public static Int getExpectedDBVersion() {
        return dbVersion
    }

    public static Int getActualDBVersion() {
        return withAccessLock(() -> {
            init()
            return database == null ? -1 : database.getVersion()
        })
    }

    public static Boolean versionsAreCompatible(final SQLiteDatabase databaseToCheck, final Int oldVersion, final Int newVersion) {
        if (newVersion < oldVersion) {
            val downgradeableVersions: Set<Integer> = DBDowngradeableVersions.load(databaseToCheck)
            for (Int version = oldVersion; version > newVersion; version--) {
                if (!downgradeableVersions.contains(version)) {
                    return false
                }
            }
        }
        return true
    }

    public static class DBExtension {

        // reflect actual database schema (+ type param)
        protected Long id
        protected String key
        protected Long long1
        protected Long long2
        protected Long long3
        protected Long long4
        protected String string1
        protected String string2
        protected String string3
        protected String string4

        protected DBExtension() {
            // utility class
        }

        /**
         * internal constructor for database queries
         */
        protected DBExtension(final Long id, final String key, final Long long1, final Long long2, final Long long3, final Long long4, final String string1, final String string2, final String string3, final String string4) {
            this.id = id
            this.key = key
            this.long1 = long1
            this.long2 = long2
            this.long3 = long3
            this.long4 = long4
            this.string1 = string1
            this.string2 = string2
            this.string3 = string3
            this.string4 = string4
        }

        /**
         * public copy constructor
         */
        public DBExtension(final DataStore.DBExtension copyFrom) {
            this.id = copyFrom.getId()
            this.key = copyFrom.getKey()
            this.long1 = copyFrom.getLong1()
            this.long2 = copyFrom.getLong2()
            this.long3 = copyFrom.getLong3()
            this.long4 = copyFrom.getLong4()
            this.string1 = copyFrom.getString1()
            this.string2 = copyFrom.getString2()
            this.string3 = copyFrom.getString3()
            this.string4 = copyFrom.getString4()
        }

        /**
         * get the first entry for this key
         */
        protected static DBExtension load(final DBExtensionType type, final String key) {
            return withAccessLock(() -> {
                if (!init(false)) {
                    return null
                }
                return load(database, type, key)
            })
        }

        protected static DBExtension load(final SQLiteDatabase db, final DBExtensionType type, final String key) {
            return withAccessLock(() -> {
                checkState(type, key, false)
                try (Cursor cursor = db.query(dbTableExtension,
                        String[]{"_id", "_key", "long1", "long2", "long3", "long4", "string1", "string2", "string3", "string4"},
                        "_type = ? AND _key LIKE ?",
                        String[]{String.valueOf(type.id), key},
                        null, null, "_id", "1")) {
                    if (cursor.moveToNext()) {
                        return DBExtension(cursor.getLong(0), cursor.getString(1), cursor.getLong(2), cursor.getLong(3), cursor.getLong(4), cursor.getLong(5), cursor.getString(6), cursor.getString(7), cursor.getString(8), cursor.getString(9))
                    }
                }
                return null
            })
        }

        /**
         * get a list of all entries for this key (if key != null) / for this type (if key is null)
         */
        protected static ArrayList<DBExtension> getAll(final DBExtensionType type, final String key) {
            return withAccessLock(() -> {
                init()
                return getAll(database, type, key)
            })
        }

        /**
         * get a list of all entries for this key (if key != null) / for this type (if key is null)
         */
        protected static ArrayList<DBExtension> getAll(final SQLiteDatabase db, final DBExtensionType type, final String key) {
            return withAccessLock(() -> {
                checkState(type, key, true)
                val result: ArrayList<DBExtension> = ArrayList<>()
                try (Cursor cursor = db.query(dbTableExtension,
                        String[]{"_id", "_key", "long1", "long2", "long3", "long4", "string1", "string2", "string3", "string4"},
                        "_type = ?" + (null == key ? "" : " AND _key LIKE ?"),
                        null == key ? String[]{String.valueOf(type.id)} : String[]{String.valueOf(type.id), key},
                        null, null, "_id", null)) {
                    while (cursor.moveToNext()) {
                        result.add(DBExtension(cursor.getLong(0), cursor.getString(1), cursor.getLong(2), cursor.getLong(3), cursor.getLong(4), cursor.getLong(5), cursor.getString(6), cursor.getString(7), cursor.getString(8), cursor.getString(9)))
                    }
                }
                return result
            })
        }

        /**
         * adds a entry to database
         */
        protected static DBExtension add(final DBExtensionType type, final String key, final Long long1, final Long long2, final Long long3, final Long long4, final String string1, final String string2, final String string3, final String string4) {
            return withAccessLock(() -> {
                if (!init(false)) {
                    return null
                }
                return add(database, type, key, long1, long2, long3, long4, string1, string2, string3, string4)
            })
        }

        protected static DBExtension add(final SQLiteDatabase db, final DBExtensionType type, final String key, final Long long1, final Long long2, final Long long3, final Long long4, final String string1, final String string2, final String string3, final String string4) {
            return withAccessLock(() -> {
                try {
                    val id: Long = db.insert(dbTableExtension, null, toValues(type, key, long1, long2, long3, long4, string1, string2, string3, string4))
                    return DBExtension(id, key, long1, long2, long3, long4, string1, string2, string3, string4)
                } catch (final Exception e) {
                    Log.e("DBExtension.add failed", e)
                }
                return null
            })
        }

        private static ContentValues toValues(final DBExtensionType type, final String key, final Long long1, final Long long2, final Long long3, final Long long4, final String string1, final String string2, final String string3, final String string4) {
            val cv: ContentValues = ContentValues()
            cv.put("_type", String.valueOf(type.id))
            cv.put("_key", key)
            cv.put("long1", long1)
            cv.put("long2", long2)
            cv.put("long3", long3)
            cv.put("long4", long4)
            cv.put("string1", string1)
            cv.put("string2", string2)
            cv.put("string3", string3)
            cv.put("string4", string4)
            return cv
        }

        /**
         * removes all elements with this key from database
         */
        public static Unit removeAll(final DBExtensionType type, final String key) {
            withAccessLock(() -> {
                if (!init(false)) {
                    return
                }
                removeAll(database, type, key)
            })
        }

        public static Unit removeAll(final SQLiteDatabase db, final DBExtensionType type, final String key) {
            withAccessLock(() -> {
                checkState(type, key, false)
                db.delete(dbTableExtension, "_type = ? AND _key LIKE ?", String[]{String.valueOf(type.id), key})
            })
        }

        private static Unit checkState(final DBExtensionType type, final String key, final Boolean nullable) {
            if (type == DBEXTENSION_INVALID) {
                throw IllegalStateException("DBExtension: type must be set to valid type")
            }
            if (!StringUtils.isNotBlank(key) && !(nullable && null == key)) {
                throw IllegalStateException("DBExtension: key value must be set")
            }
        }

        public Long getId() {
            return id
        }

        public String getKey() {
            return key
        }

        public Long getLong1() {
            return long1
        }

        public Long getLong2() {
            return long2
        }

        public Long getLong3() {
            return long3
        }

        public Long getLong4() {
            return long4
        }

        public String getString1() {
            return string1
        }

        public String getString2() {
            return string2
        }

        public String getString3() {
            return string3
        }

        public String getString4() {
            return string4
        }

    }

    public static class DBFilters {

        public static List<GeocacheFilter> getAllStoredFilters() {
            return withAccessLock(() -> queryToColl(dbTableFilters, String[]{"name", "treeconfig"},
                    null, null, null, null, ArrayList<>(),
                    c -> GeocacheFilter.createFromConfig(c.getString(0), c.getString(1))))
        }

        /**
         * Saves using UPSERT on NAME (if filter with same name exists, it deleted before.  otherwise one is created)
         */
        public static Int save(final GeocacheFilter filter) {
            return withAccessLock(() -> {
                delete(filter.getName())
                val values: ContentValues = ContentValues()
                values.put("name", filter.getName())
                values.put("treeconfig", filter.toConfig())
                return (Int) database.insert(dbTableFilters, null, values)
            })
        }

        /**
         * deletes any entry in DB with same filterName as in supplied filter object, if exists
         */
        public static Boolean delete(final String filterName) {
            return withAccessLock(() -> database.delete(dbTableFilters, "name = ?", String[]{filterName}) > 0)
        }

    }

    private DataStore() {
        // utility class
    }

    private static val allCachesCountObservable: Single<Integer> = Single.create((SingleOnSubscribe<Integer>) emitter -> {
        try {
            if (isInitialized()) {
                emitter.onSuccess(getAllCachesCount())
            }
        } catch (RuntimeException re) {
            emitter.onError(re)
        }
    }).timeout(500, TimeUnit.MILLISECONDS).retry(10).subscribeOn(Schedulers.io())

    private static Boolean newlyCreatedDatabase = false
    private static Boolean databaseCleaned = false

    private static Unit init() {
        init(true)
    }

    private static Boolean init(final Boolean force) {
        return initAndCheck(force) == null
    }

    public static String initAndCheck() {
        return withAccessLock(() -> initAndCheck(false))
    }

    /**
     * checks and inits database if not already initialized. Returns null if everything is fine
     * In case of error on init:
     * * when force=true throws exception
     * * when force=false returns error message (guaranteed to be non-null)
     */
    private static String initAndCheck(final Boolean force) {
        if (database != null) {
            return null
        }

        try (ContextLogger ignore = ContextLogger(true, "DataStore.init")) {
            synchronized (DataStore.class) {
                if (database != null) {
                    return null
                }
                val dbHelper: DbHelper = DbHelper(DBContext(CgeoApplication.getInstance()))
                try {
                    database = dbHelper.getWritableDatabase()
                } catch (final Exception e) {
                    Log.e("DataStore.init: unable to open database for R/W", e)
                    val recreateErrorMsg: String = recreateDatabase(dbHelper)
                    if (recreateErrorMsg != null) {
                        //if we land here we could neither open the DB nor could we recreate it and database remains null
                        //=> failfast here by rethrowing original exception. This might give us better error analysis possibilities
                        val msg: String = "DataStore.init: Unrecoverable problem opening database ('" +
                                databasePath().getAbsolutePath() + "')(recreate error: " + recreateErrorMsg + ")"
                        if (force) {
                            Log.e(msg, e)
                            throw RuntimeException(msg, e)
                        }
                        Log.w(msg, e)
                        return msg + ": " + e.getMessage()
                    }
                }
            }
        }
        return null
    }

    /**
     * Attempt to recreate the database if opening has failed
     *
     * @param dbHelper dbHelper to use to reopen the database
     * @return null if everything is ok, error message otherwise
     */
    private static String recreateDatabase(final DbHelper dbHelper) {
        val dbPath: File = databasePath()
        val uri: Uri = ContentStorage.get().writeFileToFolder(PersistableFolder.BACKUP, FileNameCreator.forName(dbPath.getName() + DB_FILE_CORRUPTED_EXTENSION), dbPath, false)
        if (uri != null) {
            Log.i("DataStore.init: renamed " + dbPath + " into " + uri.getPath())
        } else {
            Log.e("DataStore.init: unable to move corrupted database")
        }
        try {
            database = dbHelper.getWritableDatabase()
            return null
        } catch (final Exception f) {
            val msg: String = "DataStore.init: unable to recreate database and open it for R/W"
            Log.w(msg, f)
            return msg + ": " + f.getMessage()
        }
    }

    private static synchronized Unit closeDb() {
        if (!databaseLock.isWriteLockedByCurrentThread()) {
            throw IllegalStateException("Trying to close DB w/o write lock")
        }

        if (database == null) {
            return
        }

        cacheCache.removeAllFromCache()
        PreparedStatement.clearPreparedStatements()
        database.close()
        database = null
    }

    private static  <T> T withAccessLock(final Supplier<T> action) {
        databaseLock.readLock().lock()
        try {
            return action.get()
        } finally {
            databaseLock.readLock().unlock()
        }
    }

    private static  Unit withAccessLock(final Runnable action) {
        databaseLock.readLock().lock()
        try {
            action.run()
        } finally {
            databaseLock.readLock().unlock()
        }
    }

    private static <T> T withChangeLock(final Supplier<T> action) {
        databaseLock.writeLock().lock()
        try {
            return action.get()
        } finally {
            databaseLock.writeLock().unlock()
        }
    }

    public static Uri backupDatabaseInternal(final Folder backupDir) {
        return withChangeLock(() -> {
            closeDb()
            val uri: Uri = ContentStorage.get().copy(Uri.fromFile(databasePath()), backupDir, FileNameCreator.forName(DB_FILE_NAME_BACKUP), false)
            init()

            if (uri == null) {
                Log.e("Database could not be copied to " + backupDir.toUserDisplayableString())
                return null
            }
            Log.i("Database was copied to " + backupDir.toUserDisplayableString())
            return uri
        })
    }

    /**
     * Move the database to/from external cgdata in a thread,
     * showing a progress window
     */
    public static Unit moveDatabase(final Activity fromActivity) {
        val dialog: ProgressDialog = ProgressDialog.show(fromActivity, fromActivity.getString(R.string.init_dbmove_dbmove), fromActivity.getString(R.string.init_dbmove_running), true, false)
        AndroidRxUtils.bindActivity(fromActivity, Observable.defer(() -> {
            if (!LocalStorage.isExternalStorageAvailable()) {
                Log.w("Database was not moved: external memory not available")
                return Observable.just(false)
            }
            return withChangeLock(() -> {

                closeDb()

                val source: File = databasePath()
                val target: File = databaseAlternatePath()
                if (!FileUtils.copy(source, target)) {
                    Log.e("Database could not be moved to " + target)
                    init()
                    return Observable.just(false)
                }
                if (!FileUtils.delete(source)) {
                    Log.e("Original database could not be deleted during move")
                }
                Settings.setDbOnSDCard(!Settings.isDbOnSDCard())
                Log.i("Database was moved to " + target)

                init()
                return Observable.just(true)
            })
        }).subscribeOn(Schedulers.io())).subscribe(success -> {
            dialog.dismiss()
            val message: String = success ? fromActivity.getString(R.string.init_dbmove_success) : fromActivity.getString(R.string.init_dbmove_failed)
            SimpleDialog.of(fromActivity).setTitle(R.string.init_dbmove_dbmove).setMessage(TextParam.text(message)).show()
        })
    }

    private static File databasePath(final Boolean internal) {
        return File(internal ? LocalStorage.getInternalDbDirectory() : LocalStorage.getExternalDbDirectory(), DB_FILE_NAME)
    }

    public static File databasePath() {
        return databasePath(!Settings.isDbOnSDCard())
    }

    private static File databaseAlternatePath() {
        return databasePath(Settings.isDbOnSDCard())
    }

    public static String restoreDatabaseInternal(final Context context, final Uri databaseUri) {
        return withChangeLock(() -> {
            val tmpFile: File = ContentStorage.get().writeUriToTempFile(databaseUri, "backup_db.tmp")
            DBRestoreResult result = DBRestoreResult.RESTORE_FAILED_GENERAL
            try {
                val backup: SQLiteDatabase = SQLiteDatabase.openDatabase(tmpFile.getPath(), null, SQLiteDatabase.OPEN_READONLY)
                val backupDbVersion: Int = backup.getVersion()
                val expectedDbVersion: Int = DataStore.getExpectedDBVersion()
                if (!DataStore.versionsAreCompatible(backup, backupDbVersion, expectedDbVersion)) {
                    return String.format(context.getString(R.string.init_restore_version_error), expectedDbVersion, backupDbVersion)
                }
                closeDb()
                result = FileUtils.copy(tmpFile, databasePath()) ? DBRestoreResult.RESTORE_SUCCESSFUL : DBRestoreResult.RESTORE_FAILED_GENERAL
                init()
                if (newlyCreatedDatabase) {
                    result = DBRestoreResult.RESTORE_FAILED_DBRECREATED
                    Log.e("restored DB seems to be corrupt, needed to recreate database from scratch")
                }
                if (result == DBRestoreResult.RESTORE_SUCCESSFUL) {
                    Log.i("Database successfully restored from " + tmpFile.getPath())
                } else {
                    Log.e("Could not restore database from " + tmpFile.getPath())
                }
            } catch (SQLiteException e) {
                Log.e("error while restoring database: ", e)
            } finally {
                tmpFile.delete()
            }
            return context.getString(result.res)
        })
    }

    private static class DBContext : ContextWrapper() {

        DBContext(final Context base) {
            super(base)
        }

        /**
         * We override the default open/create as it doesn't work on OS 1.6 and
         * causes issues on other devices too.
         */
        override         public SQLiteDatabase openOrCreateDatabase(final String name, final Int mode,
                                                   final CursorFactory factory) {
            val file: File = File(name)
            FileUtils.mkdirs(file.getParentFile())
            return SQLiteDatabase.openOrCreateDatabase(file, factory)
        }

    }

    private static class DbHelper : SQLiteOpenHelper() {

        private static Boolean firstRun = true
        public static val MAX_TRAILHISTORY_LENGTH: Int = getMaximumMapTrailLength()

        DbHelper(final Context context) {
            super(context, databasePath().getPath(), null, dbVersion)
        }

        override         public Unit onCreate(final SQLiteDatabase db) {
            newlyCreatedDatabase = true
            db.execSQL(dbCreateCaches)
            db.execSQL(dbCreateLists)
            db.execSQL(dbCreateCachesLists)
            db.execSQL(dbCreateAttributes)
            db.execSQL(dbCreateWaypoints)
            db.execSQL(dbCreateVariables)
            db.execSQL(dbCreateCategories)
            db.execSQL(dbCreateSpoilers)
            db.execSQL(dbCreateLogs)
            db.execSQL(dbCreateLogCount)
            db.execSQL(dbCreateLogImages)
            db.execSQL(dbCreateLogsOffline)
            db.execSQL(dbCreateLogsOfflineImages)
            db.execSQL(dbCreateLogsOfflineTrackables)
            db.execSQL(dbCreateTrackables)
            db.execSQL(dbCreateSearchDestinationHistory)
            db.execSQL(dbCreateTrailHistory)
            db.execSQL(dbCreateRoute)
            db.execSQL(dbCreateExtension)
            db.execSQL(dbCreateFilters)

            createIndices(db, dbVersion)

            //at the very end of onCreate: write downgradeable versions in database
            try {
                DBDowngradeableVersions.save(db, DBVERSIONS_DOWNWARD_COMPATIBLE)
            } catch (final Exception e) {
                Log.e("Failed to write downgradeable versions to " + DBVERSIONS_DOWNWARD_COMPATIBLE, e)
            }
        }

        private static Unit createIndices(final SQLiteDatabase db, final Int currentVersion) {
            db.execSQL("CREATE INDEX IF NOT EXISTS in_caches_geo ON " + dbTableCaches + " (geocode)")
            db.execSQL("CREATE INDEX IF NOT EXISTS in_caches_guid ON " + dbTableCaches + " (guid)")
            db.execSQL("CREATE INDEX IF NOT EXISTS in_caches_lat ON " + dbTableCaches + " (latitude)")
            db.execSQL("CREATE INDEX IF NOT EXISTS in_caches_lon ON " + dbTableCaches + " (longitude)")
            db.execSQL("CREATE INDEX IF NOT EXISTS in_caches_reason ON " + dbTableCaches + " (reason)")
            db.execSQL("CREATE INDEX IF NOT EXISTS in_caches_detailed ON " + dbTableCaches + " (detailed)")
            db.execSQL("CREATE INDEX IF NOT EXISTS in_caches_type ON " + dbTableCaches + " (type)")
            db.execSQL("CREATE INDEX IF NOT EXISTS in_caches_visit_detail ON " + dbTableCaches + " (visiteddate, detailedupdate)")
            db.execSQL("CREATE INDEX IF NOT EXISTS in_attr_geo ON " + dbTableAttributes + " (geocode)")
            db.execSQL("CREATE INDEX IF NOT EXISTS in_wpts_geo ON " + dbTableWaypoints + " (geocode)")
            db.execSQL("CREATE INDEX IF NOT EXISTS in_wpts_geo_type ON " + dbTableWaypoints + " (geocode, type)")
            db.execSQL("CREATE INDEX IF NOT EXISTS in_spoil_geo ON " + dbTableSpoilers + " (geocode)")
            db.execSQL("CREATE INDEX IF NOT EXISTS in_logs_geo ON " + dbTableLogs + " (geocode, date desc)")
            if (currentVersion >= 54) {
                db.execSQL("CREATE INDEX IF NOT EXISTS in_logimagess_logid ON " + dbTableLogImages + " (log_id)")
            }
            db.execSQL("CREATE INDEX IF NOT EXISTS in_logcount_geo ON " + dbTableLogCount + " (geocode)")
            db.execSQL("CREATE INDEX IF NOT EXISTS in_logsoff_geo ON " + dbTableLogsOffline + " (geocode)")
            if (currentVersion >= 85) {
                db.execSQL("CREATE INDEX IF NOT EXISTS in_logsoffimages_geo ON " + dbTableLogsOfflineImages + " (logoffline_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS in_logsofftrackables_geo ON " + dbTableLogsOfflineTrackables + " (logoffline_id)")
            }
            db.execSQL("CREATE INDEX IF NOT EXISTS in_trck_geo ON " + dbTableTrackables + " (geocode)")
            db.execSQL("CREATE INDEX IF NOT EXISTS in_lists_geo ON " + dbTableCachesLists + " (geocode)")
            if (currentVersion >= 82) {
                db.execSQL("CREATE INDEX IF NOT EXISTS in_extension_key ON " + dbTableExtension + " (_key)")
            }
            if (currentVersion >= 98) {
                db.execSQL("CREATE INDEX IF NOT EXISTS in_vars_geo ON " + dbTableVariables + " (geocode)")
            }
            if (currentVersion >= 100) {
                db.execSQL("CREATE INDEX IF NOT EXISTS in_cats_geo ON " + dbTableCategories + " (geocode)")
            }
        }

        override         public Unit onDowngrade(final SQLiteDatabase db, final Int oldVersion, final Int newVersion) {
            Log.iForce("[DB] Request to downgrade database from ver. " + oldVersion + " to ver. " + newVersion + ": start")

            //ask the database for a list of downgradeable DB versions AT THE TIME THIS DB WAS LAST UPGRADED
            //(which might be later than the current code version was written)
            val downgradeableVersions: Set<Integer> = DBDowngradeableVersions.load(db)

            //allow downgrade if, and only if, all versions between oldVersion and newVersion are marked as "downward-compatible"
            for (Int version = oldVersion; version > newVersion; version--) {
                if (!downgradeableVersions.contains(version)) {
                    throw SQLiteException("Can't downgrade database from version " + oldVersion + " to " + newVersion +
                            ": " + version + " is not downward compatible")
                }
            }
            Log.iForce("[DB] Downgrade database from ver. " + oldVersion + " to ver. " + newVersion + ": allowed")
        }

        override         public Unit onUpgrade(final SQLiteDatabase db, final Int oldVersion, final Int newVersion) {
            Log.iForce("[DB] Upgrade database from ver. " + oldVersion + " to ver. " + newVersion + ": start")

            try {
                if (db.isReadOnly()) {
                    return
                }

                db.beginTransaction()

                if (oldVersion <= 0) { // table
                    dropDatabase(db)
                    onCreate(db)

                    Log.i("Database structure created.")
                }

                if (oldVersion > 0) {
                    if (oldVersion < 52) { // upgrade to 52
                        try {
                            db.execSQL(dbCreateSearchDestinationHistory)

                            Log.i("Added table " + dbTableSearchDestinationHistory + ".")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 52)
                        }
                    }

                    if (oldVersion < 53) { // upgrade to 53
                        try {
                            db.execSQL("ALTER TABLE " + dbTableCaches + " ADD COLUMN onWatchlist INTEGER")

                            Log.i("Column onWatchlist added to " + dbTableCaches + ".")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 53)
                        }
                    }

                    if (oldVersion < 54) { // update to 54
                        try {
                            db.execSQL(dbCreateLogImages)
                        } catch (final SQLException e) {
                            onUpgradeError(e, 54)
                        }
                    }

                    if (oldVersion < 55) { // update to 55
                        try {
                            db.execSQL("ALTER TABLE " + dbTableCaches + " ADD COLUMN personal_note TEXT")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 55)
                        }
                    }

                    // make all internal attribute names lowercase
                    // @see issue #299
                    if (oldVersion < 56) { // update to 56
                        try {
                            db.execSQL("UPDATE " + dbTableAttributes + " SET attribute = " +
                                    "LOWER(attribute) WHERE attribute LIKE \"%_yes\" " +
                                    "OR  attribute LIKE \"%_no\"")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 56)
                        }
                    }

                    // Create missing indices. See issue #435
                    if (oldVersion < 57) { // update to 57
                        try {
                            db.execSQL("DROP INDEX in_a")
                            db.execSQL("DROP INDEX in_b")
                            db.execSQL("DROP INDEX in_c")
                            db.execSQL("DROP INDEX in_d")
                            db.execSQL("DROP INDEX in_e")
                            db.execSQL("DROP INDEX in_f")
                            createIndices(db, 57)
                        } catch (final SQLException e) {
                            onUpgradeError(e, 57)
                        }
                    }

                    if (oldVersion < 58) { // upgrade to 58
                        try {
                            db.beginTransaction()

                            val dbTableCachesTemp: String = dbTableCaches + "_temp"
                            val dbCreateCachesTemp: String = ""
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
                                    + "); "

                            db.execSQL(dbCreateCachesTemp)
                            db.execSQL("INSERT INTO " + dbTableCachesTemp + " SELECT _id,updated,detailed,detailedupdate,visiteddate,geocode,reason,cacheid,guid,type,name,own,owner,owner_real," +
                                    "hidden,hint,size,difficulty,terrain,location,direction,distance,latitude,longitude, 0," +
                                    "personal_note,shortdesc,description,favourite_cnt,rating,votes,myvote,disabled,archived,members,found,favourite,inventorycoins," +
                                    "inventorytags,inventoryunknown,onWatchlist FROM " + dbTableCaches)
                            db.execSQL("DROP TABLE " + dbTableCaches)
                            db.execSQL("ALTER TABLE " + dbTableCachesTemp + " RENAME TO " + dbTableCaches)

                            val dbTableWaypointsTemp: String = dbTableWaypoints + "_temp"
                            val dbCreateWaypointsTemp: String = ""
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
                                    + "); "
                            db.execSQL(dbCreateWaypointsTemp)
                            db.execSQL("INSERT INTO " + dbTableWaypointsTemp + " SELECT _id, geocode, updated, type, prefix, lookup, name, latitude, longitude, note FROM " + dbTableWaypoints)
                            db.execSQL("DROP TABLE " + dbTableWaypoints)
                            db.execSQL("ALTER TABLE " + dbTableWaypointsTemp + " RENAME TO " + dbTableWaypoints)

                            createIndices(db, 58)

                            db.setTransactionSuccessful()

                            Log.i("Removed latitude_string and longitude_string columns")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 58)
                        } finally {
                            db.endTransaction()
                        }
                    }

                    if (oldVersion < 59) {
                        try {
                            // Add indices and remove obsolete cache files
                            createIndices(db, 59)
                            removeObsoleteGeocacheDataDirectories()
                        } catch (final SQLException e) {
                            onUpgradeError(e, 59)
                        }
                    }

                    if (oldVersion < 60) {
                        try {
                            removeSecEmptyDirs()
                        } catch (final SQLException e) {
                            onUpgradeError(e, 60)
                        }
                    }
                    if (oldVersion < 61) {
                        try {
                            db.execSQL("ALTER TABLE " + dbTableLogs + " ADD COLUMN friend INTEGER")
                            db.execSQL("ALTER TABLE " + dbTableCaches + " ADD COLUMN coordsChanged INTEGER DEFAULT 0")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 61)
                        }
                    }
                    // Introduces finalDefined on caches and own on waypoints
                    if (oldVersion < 62) {
                        try {
                            db.execSQL("ALTER TABLE " + dbTableCaches + " ADD COLUMN finalDefined INTEGER DEFAULT 0")
                            db.execSQL("ALTER TABLE " + dbTableWaypoints + " ADD COLUMN own INTEGER DEFAULT 0")
                            db.execSQL("UPDATE " + dbTableWaypoints + " SET own = 1 WHERE type = 'own'")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 62)
                        }
                    }
                    if (oldVersion < 63) {
                        try {
                            removeDoubleUnderscoreMapFiles()
                        } catch (final SQLException e) {
                            onUpgradeError(e, 63)
                        }
                    }

                    if (oldVersion < 64) {
                        try {
                            // No cache should ever be stored into the ALL_CACHES list. Here we use hardcoded list ids
                            // rather than symbolic ones because the fix must be applied with the values at the time
                            // of the problem. The problem was introduced in release 2012.06.01.
                            db.execSQL("UPDATE " + dbTableCaches + " SET reason=1 WHERE reason=2")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 64)
                        }
                    }

                    if (oldVersion < 65) {
                        try {
                            // Set all waypoints where name is Original coordinates to type ORIGINAL
                            db.execSQL("UPDATE " + dbTableWaypoints + " SET type='original', own=0 WHERE name='Original Coordinates'")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 65)
                        }
                    }
                    // Introduces visited feature on waypoints
                    if (oldVersion < 66) {
                        try {
                            db.execSQL("ALTER TABLE " + dbTableWaypoints + " ADD COLUMN visited INTEGER DEFAULT 0")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 66)
                        }
                    }
                    // issue2662 OC: Leichtes Klettern / Easy climbing
                    if (oldVersion < 67) {
                        try {
                            db.execSQL("UPDATE " + dbTableAttributes + " SET attribute = 'easy_climbing_yes' WHERE geocode LIKE 'OC%' AND attribute = 'climbing_yes'")
                            db.execSQL("UPDATE " + dbTableAttributes + " SET attribute = 'easy_climbing_no' WHERE geocode LIKE 'OC%' AND attribute = 'climbing_no'")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 67)
                        }
                    }
                    // Introduces logPasswordRequired on caches
                    if (oldVersion < 68) {
                        try {
                            db.execSQL("ALTER TABLE " + dbTableCaches + " ADD COLUMN logPasswordRequired INTEGER DEFAULT 0")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 68)
                        }
                    }
                    // description for log Images
                    if (oldVersion < 69) {
                        try {
                            db.execSQL("ALTER TABLE " + dbTableLogImages + " ADD COLUMN description TEXT")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 69)
                        }
                    }
                    // Introduces watchListCount
                    if (oldVersion < 70) {
                        try {
                            db.execSQL("ALTER TABLE " + dbTableCaches + " ADD COLUMN watchlistCount INTEGER DEFAULT -1")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 70)
                        }
                    }
                    // Introduces cachesLists
                    if (oldVersion < 71) {
                        try {
                            db.execSQL(dbCreateCachesLists)
                            createIndices(db, 71)
                            db.execSQL("INSERT INTO " + dbTableCachesLists + " SELECT reason, geocode FROM " + dbTableCaches)
                        } catch (final SQLException e) {
                            onUpgradeError(e, 71)
                        }
                    }
                    // User notes in waypoints and local coords changes of WPs without coords on server
                    if (oldVersion < 72) {
                        try {
                            db.execSQL("ALTER TABLE " + dbTableWaypoints + " ADD COLUMN user_note TEXT")
                            db.execSQL("ALTER TABLE " + dbTableWaypoints + " ADD COLUMN org_coords_empty INTEGER DEFAULT 0")
                            db.execSQL("UPDATE " + dbTableWaypoints + " SET user_note = note")
                            db.execSQL("UPDATE " + dbTableWaypoints + " SET note = ''")
                            db.execSQL("UPDATE " + dbTableWaypoints + " SET org_coords_empty = 1 WHERE latitude IS NULL AND longitude IS NULL")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 72)
                        }
                    }
                    // Adds coord calculator state to the waypoint
                    if (oldVersion < 73) {
                        try {
                            db.execSQL("ALTER TABLE " + dbTableWaypoints + " ADD COLUMN calc_state TEXT")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 73)
                        }
                    }

                    // Adds report problem to offline log
                    if (oldVersion < 74) {
                        try {
                            db.execSQL("ALTER TABLE " + dbTableLogsOffline + " ADD COLUMN report_problem TEXT")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 74)
                        }
                    }

                    // Adds log information for trackables
                    if (oldVersion < 75) {
                        try {
                            db.execSQL("ALTER TABLE " + dbTableTrackables + " ADD COLUMN log_date LONG")
                            db.execSQL("ALTER TABLE " + dbTableTrackables + " ADD COLUMN log_type INTEGER")
                            db.execSQL("ALTER TABLE " + dbTableTrackables + " ADD COLUMN log_guid TEXT")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 75)
                        }
                    }

                    // add trail history table
                    if (oldVersion < 76) {
                        try {
                            db.execSQL(dbCreateTrailHistory)

                            Log.i("Added table " + dbTableTrailHistory + ".")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 76)
                        }
                    }

                    // add column for list marker
                    if (oldVersion < 77) {
                        try {
                            db.execSQL("ALTER TABLE " + dbTableLists + " ADD COLUMN marker INTEGER NOT NULL DEFAULT 0")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 77)
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
                                    + ")")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 79)
                        }
                    }

                    if (oldVersion < 80) {
                        try {
                            db.execSQL("ALTER TABLE " + dbTableCaches + " ADD COLUMN preventWaypointsFromNote INTEGER NOT NULL DEFAULT 0")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 80)
                        }
                    }

                    // add route table
                    if (oldVersion < 81) {
                        try {
                            db.execSQL(dbCreateRoute)

                            Log.i("Added table " + dbTableRoute + ".")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 81)
                        }
                    }

                    // add extension table
                    if (oldVersion < 82) {
                        try {
                            db.execSQL(dbCreateExtension)

                            Log.i("Added table " + dbTableExtension + ".")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 82)
                        }
                    }

                    // fix a few creation errors
                    if (oldVersion < 83) {
                        // remove "table" infix for route table by renaming table
                        try {
                            db.execSQL("ALTER TABLE cg_table_route RENAME TO " + dbTableRoute)
                        } catch (final SQLException e) {
                            // ignore, because depending on your upgrade path, the statement above cannot work
                        }
                        // recreate extension table to remove "table" infix and to fix two column types
                        try {
                            db.execSQL("DROP TABLE IF EXISTS cg_table_extension;")
                            db.execSQL("DROP TABLE IF EXISTS cg_extension;")
                            db.execSQL(dbCreateExtension)
                        } catch (final SQLException e) {
                            onUpgradeError(e, 83)
                        }
                    }

                    // redefine & migrate route table
                    if (oldVersion < 84 && oldVersion > 80) {
                        try {
                            db.execSQL("ALTER TABLE " + dbTableRoute + " RENAME TO temp_route")
                            db.execSQL(dbCreateRoute)
                            // migrate existing caches in individual route
                            db.execSQL("INSERT INTO " + dbTableRoute + " (precedence, type, id, latitude, longitude)"
                                    + " SELECT precedence, " + RouteItem.RouteItemType.GEOCACHE.ordinal() + " type, geocode id, latitude, longitude"
                                    + " FROM temp_route LEFT JOIN " + dbTableCaches + " USING (geocode)"
                                    + " WHERE temp_route.type=1")
                            // migrate existing waypoints in individual route
                            db.execSQL("INSERT INTO " + dbTableRoute + " (precedence, type, id, latitude, longitude)"
                                    + " SELECT precedence, " + RouteItem.RouteItemType.WAYPOINT.ordinal() + " type, " + dbTableWaypoints + ".geocode || \"-\" || PREFIX id, latitude, longitude"
                                    + " FROM temp_route INNER JOIN " + dbTableWaypoints + " ON (temp_route.id = " + dbTableWaypoints + "._id)"
                                    + " WHERE temp_route.type=0")
                            // drop temp table
                            db.execSQL("DROP TABLE IF EXISTS temp_route")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 84)
                        }
                    }

                    //enhance offline logging storage
                    if (oldVersion < 85) {
                        try {
                            //add columns
                            createColumnIfNotExists(db, dbTableLogsOffline, "image_title_prefix TEXT")
                            createColumnIfNotExists(db, dbTableLogsOffline, "image_scale INTEGER")
                            createColumnIfNotExists(db, dbTableLogsOffline, "favorite INTEGER")
                            createColumnIfNotExists(db, dbTableLogsOffline, "rating FLOAT")
                            createColumnIfNotExists(db, dbTableLogsOffline, "password TEXT")
                            createColumnIfNotExists(db, dbTableLogsOffline, "tweet INTEGER"); // no longer used
                            //add tables
                            db.execSQL("DROP TABLE IF EXISTS " + dbTableLogsOfflineImages)
                            db.execSQL("DROP TABLE IF EXISTS " + dbTableLogsOfflineTrackables)
                            db.execSQL(dbCreateLogsOfflineImages)
                            db.execSQL(dbCreateLogsOfflineTrackables)
                        } catch (final SQLException e) {
                            onUpgradeError(e, 85)
                        }
                    }

                    //(re)create indices for logging tables
                    if (oldVersion < 86) {
                        db.execSQL("DROP INDEX in_logs_geo")
                        createIndices(db, 86)
                    }

                    //add service log id
                    if (oldVersion < 87) {
                        try {
                            //add columns
                            createColumnIfNotExists(db, dbTableLogsOffline, "service_log_id TEXT")
                            createColumnIfNotExists(db, dbTableLogs, "service_log_id TEXT")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 87)
                        }
                    }

                    // add timestamp to cg_trail_history
                    if (oldVersion < 88) {
                        try {
                            createColumnIfNotExists(db, dbTableTrailHistory, "timestamp INTEGER DEFAULT 0")
                            db.execSQL("UPDATE " + dbTableTrailHistory + " SET timestamp =" + System.currentTimeMillis())
                        } catch (final SQLException e) {
                            onUpgradeError(e, 88)
                        }
                    }

                    // add altitude to cg_trail_history
                    if (oldVersion < 89) {
                        try {
                            createColumnIfNotExists(db, dbTableTrailHistory, "altitude DOUBLE DEFAULT 0.0")
                            db.execSQL("UPDATE " + dbTableTrailHistory + " SET altitude = 0.0")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 89)
                        }
                    }

                    // add user guid to cg_caches and cg_logs
                    if (oldVersion < 90) {
                        try {
                            createColumnIfNotExists(db, dbTableCaches, "owner_guid TEXT NOT NULL DEFAULT ''")
                            createColumnIfNotExists(db, dbTableLogs, "author_guid TEXT NOT NULL DEFAULT ''")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 90)
                        }
                    }

                    // add field to cg_extension
                    if (oldVersion < 91) {
                        try {
                            createColumnIfNotExists(db, dbTableExtension, "long3 LONG NOT NULL DEFAULT 0")
                            createColumnIfNotExists(db, dbTableExtension, "long4 LONG NOT NULL DEFAULT 0")
                            createColumnIfNotExists(db, dbTableExtension, "string3 TEXT NOT NULL DEFAULT ''")
                            createColumnIfNotExists(db, dbTableExtension, "string4 TEXT NOT NULL DEFAULT ''")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 91)
                        }
                    }

                    // add emoji to cg_caches
                    if (oldVersion < 92) {
                        try {
                            createColumnIfNotExists(db, dbTableCaches, "emoji INTEGER DEFAULT 0")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 92)
                        }
                    }

                    // add emoji to cg_lists
                    if (oldVersion < 93) {
                        try {
                            createColumnIfNotExists(db, dbTableLists, "emoji INTEGER DEFAULT 0")
                            // migrate marker values
                            db.execSQL("UPDATE " + dbTableLists + " SET emoji = " + 0xe01e + " WHERE marker=1");    // green
                            db.execSQL("UPDATE " + dbTableLists + " SET emoji = " + 0xe030 + " WHERE marker=2");    // orange
                            db.execSQL("UPDATE " + dbTableLists + " SET emoji = " + 0xe01d + " WHERE marker=3");    // blue
                            db.execSQL("UPDATE " + dbTableLists + " SET emoji = " + 0xe024 + " WHERE marker=4");    // red
                            db.execSQL("UPDATE " + dbTableLists + " SET emoji = " + 0xe020 + " WHERE marker=5");    // turquoise
                            db.execSQL("UPDATE " + dbTableLists + " SET emoji = " + 0xe043 + " WHERE marker=6");    // black
                            // do not remove old field "marker" to keep db structure backward-compatible
                        } catch (final SQLException e) {
                            onUpgradeError(e, 93)
                        }
                    }

                    //add scale to offline log image
                    if (oldVersion < 94) {
                        try {
                            createColumnIfNotExists(db, dbTableLogsOfflineImages, "scale INTEGER")

                        } catch (final SQLException e) {
                            onUpgradeError(e, 94)
                        }
                    }

                    //add table to store custom filters
                    if (oldVersion < 95) {
                        try {
                            db.execSQL(dbCreateFilters)
                        } catch (final SQLException e) {
                            onUpgradeError(e, 95)
                        }
                    }

                    //add preventAskForDeletion to cg_lists
                    if (oldVersion < 96) {
                        try {
                            createColumnIfNotExists(db, dbTableLists, FIELD_LISTS_PREVENTASKFORDELETION + " INTEGER DEFAULT 0")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 96)
                        }
                    }

                    //rename lab adventure caches geocodes prefix from LC to AL
                    if (oldVersion < 97) {
                        try {
                            val sql: String = " SET geocode = \"AL\" || SUBSTR(geocode, 3) WHERE SUBSTR(geocode, 1, 2) = \"LC\" AND LENGTH(geocode) > 10"
                            db.execSQL("UPDATE " + dbTableCaches + sql)
                            db.execSQL("UPDATE " + dbTableAttributes + sql)
                            db.execSQL("UPDATE " + dbTableCachesLists + sql)
                            db.execSQL("UPDATE " + dbTableLogCount + sql)
                            db.execSQL("UPDATE " + dbTableLogs + sql)
                            db.execSQL("UPDATE " + dbTableLogsOffline + sql)
                            db.execSQL("UPDATE " + dbTableSpoilers + sql)
                            db.execSQL("UPDATE " + dbTableTrackables + sql)
                            db.execSQL("UPDATE " + dbTableWaypoints + sql)
                        } catch (final SQLException e) {
                            onUpgradeError(e, 97)
                        }
                    }

                    //create table for variable storage
                    if (oldVersion < 98) {
                        try {
                            db.execSQL(dbCreateVariables)
                            createIndices(db, 98)
                        } catch (final SQLException e) {
                            onUpgradeError(e, 98)
                        }
                    }

                    // add alcMode to cg_caches
                    if (oldVersion < 99) {
                        try {
                            createColumnIfNotExists(db, dbTableCaches, "alcMode INTEGER DEFAULT 0")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 99)
                        }
                    }

                    //create table for category storage
                    if (oldVersion < 100) {
                        try {
                            createColumnIfNotExists(db, dbTableCaches, "tier TEXT")
                            db.execSQL(dbCreateCategories)
                            createIndices(db, 100)
                        } catch (final SQLException e) {
                            onUpgradeError(e, 100)
                        }
                    }

                    //create column
                    if (oldVersion < 101) {
                        try {
                            createColumnIfNotExists(db, dbTableLogImages, "service_image_id TEXT")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 101)
                        }
                    }

                    // Adds projection attributes to waypoints
                    if (oldVersion < 102) {
                        try {
                            createColumnIfNotExists(db, dbTableWaypoints, "projection_type TEXT")
                            createColumnIfNotExists(db, dbTableWaypoints, "projection_unit TEXT")
                            createColumnIfNotExists(db, dbTableWaypoints, "projection_formula_1 TEXT")
                            createColumnIfNotExists(db, dbTableWaypoints, "projection_formula_2 TEXT")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 102)
                        }
                    }

                    // Adds more projection attributes to waypoints
                    if (oldVersion < 103) {
                        try {
                            createColumnIfNotExists(db, dbTableWaypoints, "preprojected_latitude DOUBLE")
                            createColumnIfNotExists(db, dbTableWaypoints, "preprojected_longitude DOUBLE")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 103)
                        }
                    }

                    // Adds radius for lab stages to caches
                    if (oldVersion < 104) {
                        try {
                            createColumnIfNotExists(db, dbTableWaypoints, "geofence DOUBLE")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 104)
                        }
                    }

                    // Migrate UDC geocodes from ZZ1000-based numbers to random ones
                    // 105 has been revoked

                    // Update lab cache DT values
                    if (oldVersion < 106) {
                        try {
                            db.execSQL("UPDATE " + dbTableCaches + " SET difficulty = 0 WHERE difficulty < 0")
                            db.execSQL("UPDATE " + dbTableCaches + " SET terrain = 0 WHERE terrain < 0")
                        } catch (final SQLException e) {
                            onUpgradeError(e, 106)
                        }
                    }

                }

                //at the very end of onUpgrade: rewrite downgradeable versions in database
                try {
                    DBDowngradeableVersions.save(db, DBVERSIONS_DOWNWARD_COMPATIBLE)
                } catch (final Exception e) {
                    Log.e("Failed to rewrite downgradeable versions to " + DBVERSIONS_DOWNWARD_COMPATIBLE, e)
                }

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }

            Log.iForce("[DB] Upgrade database from ver. " + oldVersion + " to ver. " + newVersion + ": completed")
        }

        private Unit onUpgradeError(final SQLException e, final Int version) throws SQLException {
            Log.e("Failed to upgrade to version " + version, e)
            throw e
        }

        override         public Unit onOpen(final SQLiteDatabase db) {

            //get user version
            Log.iForce("[DB] Current Database Version: " + db.getVersion())

            if (firstRun) {
                sanityChecks(db)
                // limit number of records for trailHistory
                try {
                    db.execSQL("DELETE FROM " + dbTableTrailHistory + " WHERE _id < (SELECT MIN(_id) FROM (SELECT _id FROM " + dbTableTrailHistory + " ORDER BY _id DESC LIMIT " + MAX_TRAILHISTORY_LENGTH + "))")
                } catch (final Exception e) {
                    Log.w("Failed to clear trail history", e)
                }

                firstRun = false
            }
        }

        /**
         * Execute sanity checks that should be performed once per application after the database has been
         * opened.
         *
         * @param db the database to perform sanity checks against
         */
        @SuppressWarnings("EmptyMethod")
        private static Unit sanityChecks(final SQLiteDatabase db) {
            // currently unused
        }

        /**
         * Method to remove static map files with Double underscore due to issue#1670
         * introduced with release on 2012-05-24.
         */
        private static Unit removeDoubleUnderscoreMapFiles() {
            final File[] geocodeDirs = LocalStorage.getGeocacheDataDirectory().listFiles()
            if (ArrayUtils.isNotEmpty(geocodeDirs)) {
                val filter: FilenameFilter = (dir, filename) -> filename.startsWith("map_") && filename.contains("__")
                for (final File dir : geocodeDirs) {
                    final File[] wrongFiles = dir.listFiles(filter)
                    if (wrongFiles != null) {
                        for (final File wrongFile : wrongFiles) {
                            FileUtils.deleteIgnoringFailure(wrongFile)
                        }
                    }
                }
            }
        }

        /*
         * Remove empty directories created in the secondary storage area.
         */
        private static Unit removeSecEmptyDirs() {
            final File[] files = LocalStorage.getLegacyExternalCgeoDirectory().listFiles()
            if (ArrayUtils.isNotEmpty(files)) {
                for (final File file : files) {
                    if (file.isDirectory()) {
                        // This will silently fail if the directory is not empty.
                        FileUtils.deleteIgnoringFailure(file)
                    }
                }
            }
        }

        private static Unit dropDatabase(final SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS " + dbTableCaches)
            db.execSQL("DROP TABLE IF EXISTS " + dbTableLists)
            db.execSQL("DROP TABLE IF EXISTS " + dbTableCachesLists)
            db.execSQL("DROP TABLE IF EXISTS " + dbTableAttributes)
            db.execSQL("DROP TABLE IF EXISTS " + dbTableWaypoints)
            db.execSQL("DROP TABLE IF EXISTS " + dbTableVariables)
            db.execSQL("DROP TABLE IF EXISTS " + dbTableCategories)
            db.execSQL("DROP TABLE IF EXISTS " + dbTableSpoilers)
            db.execSQL("DROP TABLE IF EXISTS " + dbTableLogs)
            db.execSQL("DROP TABLE IF EXISTS " + dbTableLogCount)
            db.execSQL("DROP TABLE IF EXISTS " + dbTableLogImages)
            db.execSQL("DROP TABLE IF EXISTS " + dbTableLogsOffline)
            db.execSQL("DROP TABLE IF EXISTS " + dbTableLogsOfflineImages)
            db.execSQL("DROP TABLE IF EXISTS " + dbTableLogsOfflineTrackables)
            db.execSQL("DROP TABLE IF EXISTS " + dbTableTrackables)
            db.execSQL("DROP TABLE IF EXISTS " + dbTableSearchDestinationHistory)
            db.execSQL("DROP TABLE IF EXISTS " + dbTableTrailHistory)
            db.execSQL("DROP TABLE IF EXISTS " + dbTableRoute)
            db.execSQL("DROP TABLE IF EXISTS " + dbTableExtension)
            db.execSQL("DROP TABLE IF EXISTS " + dbTableFilters)
            // also delete tables which have old table names
            db.execSQL("DROP TABLE IF EXISTS cg_table_route")
            db.execSQL("DROP TABLE IF EXISTS cg_table_extension")
        }

        /**
         * Helper for columns creation. This method ignores duplicate column errors
         * and is useful for migration situations
         */
        public Unit createColumnIfNotExists(final SQLiteDatabase db, final String table, final String columnDefinition) {
            try {
                db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + columnDefinition)
                Log.i("[DB] Column '" + table + "'.'" + columnDefinition + "' created")
            } catch (SQLiteException sle) {
                if (!sle.getMessage().contains("duplicate column name")) {
                    throw sle
                }
                Log.iForce("[DB] Column '" + table + "'.'" + columnDefinition + "' was not created because it already exists. " +
                        "It is expected that this can happen and not an error.")
            }
        }
    }

    /**
     * Remove obsolete cache directories in c:geo private storage.
     * Also removes caches marked as "to be deleted" immediately (ignoring 72h grace period!)
     */
    public static Unit removeObsoleteGeocacheDataDirectories() {
        withAccessLock(() -> {

            // force-remove caches marked as "to be deleted", ignoring 72h grace period
            try {
                val geocodes: Set<String> = HashSet<>()
                queryToColl(dbTableCaches,
                        String[]{"geocode"},
                        "geocode NOT IN (SELECT DISTINCT (geocode) FROM " + dbTableCachesLists + ")",
                        null,
                        null,
                        null,
                        geocodes,
                        GET_STRING_0)
                val withoutOfflineLogs: Set<String> = exceptCachesWithOfflineLog(geocodes)
                Log.d("forced database clean: removing " + withoutOfflineLogs.size() + " geocaches ignoring grace period")
                removeCaches(withoutOfflineLogs, LoadFlags.REMOVE_ALL)

                deleteOrphanedRecords()
            } catch (final Exception e) {
                Log.w("DataStore.clean", e)
            }

            // remove orphaned files/folders
            final File[] files = LocalStorage.getGeocacheDataDirectory().listFiles()
            if (ArrayUtils.isNotEmpty(files)) {
                val select: SQLiteStatement = PreparedStatement.CHECK_IF_PRESENT.getStatement()
                val toRemove: List<File> = ArrayList<>(files.length)
                for (final File file : files) {
                    if (file.isDirectory()) {
                        val geocode: String = file.getName()
                        if (!HtmlImage.SHARED == (geocode)) {
                            synchronized (select) {
                                select.bindString(1, geocode)
                                if (select.simpleQueryForLong() == 0) {
                                    toRemove.add(file)
                                }
                            }
                        }
                    }
                }

                // Use a background thread for the real removal to avoid keeping the database locked
                // if we are called from within a transaction.
                Schedulers.io().scheduleDirect(() -> {
                    for (final File dir : toRemove) {
                        Log.i("Removing obsolete cache directory for " + dir.getName())
                        FileUtils.deleteDirectory(dir)
                    }
                })
            }

            reindexDatabase()
        })
    }

    private static Unit reindexDatabase() {
        init()
        try {
            Log.d("Database clean: recreate indices")
            database.execSQL("REINDEX")
        } catch (final Exception e) {
            Log.w("DataStore.clean", e)
        }
    }

    public static Boolean isThere(final String geocode, final String guid, final Boolean checkTime) {
        return withAccessLock(() -> {

            init()

            Long dataDetailedUpdate = 0
            Int dataDetailed = 0

            try {
                final Cursor cursor

                if (StringUtils.isNotBlank(geocode)) {
                    cursor = database.query(
                            dbTableCaches,
                            String[]{"detailed", "detailedupdate", "updated"},
                            "geocode = ?",
                            String[]{geocode},
                            null,
                            null,
                            null,
                            "1")
                } else if (StringUtils.isNotBlank(guid)) {
                    cursor = database.query(
                            dbTableCaches,
                            String[]{"detailed", "detailedupdate", "updated"},
                            "guid = ?",
                            String[]{guid},
                            null,
                            null,
                            null,
                            "1")
                } else {
                    return false
                }

                if (cursor.moveToFirst()) {
                    dataDetailed = cursor.getInt(0)
                    dataDetailedUpdate = cursor.getLong(1)
                }

                cursor.close()
            } catch (final Exception e) {
                Log.e("DataStore.isThere", e)
            }

            if (dataDetailed == 0) {
                // we want details, but these are not stored
                return false
            }

            if (checkTime && dataDetailedUpdate < (System.currentTimeMillis() - DAYS_AFTER_CACHE_IS_DELETED)) {
                // we want to check time for detailed cache, but data are older than 3 days
                return false
            }

            // we have some cache
            return true
        })
    }

    /**
     * is cache stored in one of the lists (not only temporary)
     */
    public static Boolean isOffline(final String geocode, final String guid) {
        if (StringUtils.isBlank(geocode) && StringUtils.isBlank(guid)) {
            return false
        }
        return withAccessLock(() -> {

            init()

            try {
                final SQLiteStatement offlineListCount
                final String value
                if (StringUtils.isNotBlank(geocode)) {
                    offlineListCount = PreparedStatement.GEOCODE_OFFLINE.getStatement()
                    value = geocode
                } else {
                    offlineListCount = PreparedStatement.GUID_OFFLINE.getStatement()
                    value = guid
                }
                synchronized (offlineListCount) {
                    offlineListCount.bindString(1, value)
                    return offlineListCount.simpleQueryForLong() > 0
                }
            } catch (final SQLiteDoneException ignored) {
                // Do nothing, it only means we have no information on the cache
            } catch (final Exception e) {
                Log.e("DataStore.isOffline", e)
            }

            return false
        })
    }

    public static Collection<String> getUnsavedGeocodes(final Collection<String> geocodes) {
        return withAccessLock(() -> {

            val unsavedGeocodes: Set<String> = HashSet<>()

            for (final String geocode : geocodes) {
                if (!isOffline(geocode, null)) {
                    unsavedGeocodes.add(geocode)
                }
            }
            return unsavedGeocodes
        })
    }

    public static String getGeocodeForGuid(final String guid) {
        if (StringUtils.isBlank(guid)) {
            return null
        }
        return withAccessLock(() -> {

            init()

            try {
                val description: SQLiteStatement = PreparedStatement.GEOCODE_OF_GUID.getStatement()
                synchronized (description) {
                    description.bindString(1, guid)
                    return description.simpleQueryForString()
                }
            } catch (final SQLiteDoneException ignored) {
                // Do nothing, it only means we have no information on the cache
            } catch (final Exception e) {
                Log.e("DataStore.getGeocodeForGuid", e)
            }

            return null
        })
    }

    public static String getGeocodeForTitle(final String title) {
        if (StringUtils.isBlank(title)) {
            return null
        }
        return withAccessLock(() -> {

            init()

            try {
                val sqlStatement: SQLiteStatement = PreparedStatement.GEOCODE_FROM_TITLE.getStatement()
                synchronized (sqlStatement) {
                    sqlStatement.bindString(1, title)
                    return sqlStatement.simpleQueryForString()
                }
            } catch (final SQLiteDoneException ignored) {
                // Do nothing, it only means we have no information on the cache
            } catch (final Exception e) {
                Log.e("DataStore.getGeocodeForGuid", e)
            }

            return null
        })
    }

    /**
     * Save the cache for set/reset user modified coordinates
     */
    public static Unit saveUserModifiedCoords(final Geocache cache) {
        withAccessLock(() -> {

            database.beginTransaction()

            val values: ContentValues = ContentValues()
            try {
                saveWaypointsWithoutTransaction(cache)
                putCoords(values, "", cache.getCoords())
                values.put("coordsChanged", cache.hasUserModifiedCoords() ? 1 : 0)

                database.update(dbTableCaches, values, "geocode = ?", String[]{cache.getGeocode()})
                database.setTransactionSuccessful()
            } catch (final Exception e) {
                Log.e("SaveResetCoords", e)
            } finally {
                database.endTransaction()
            }
        })
    }

    /**
     * Save/store a cache to the CacheCache
     *
     * @param cache the Cache to save in the CacheCache/DB
     */
    public static Unit saveCache(final Geocache cache, final Set<LoadFlags.SaveFlag> saveFlags) {
        saveCaches(Collections.singletonList(cache), saveFlags)
    }

    /**
     * Save/store a cache to the CacheCache
     *
     * @param caches the caches to save in the CacheCache/DB
     */
    public static Unit saveCaches(final Collection<Geocache> caches, final Set<LoadFlags.SaveFlag> saveFlags) {
        if (CollectionUtils.isEmpty(caches)) {
            return
        }

        withAccessLock(() -> {

            try (ContextLogger cLog = ContextLogger("DataStore.saveCaches(#%d,flags:%s)", caches.size(), saveFlags)) {

                cLog.add("gc" + cLog.toStringLimited(caches, 10, c -> c == null ? "-" : c.getGeocode()))

                val cachesToLoadFromDatabase: List<String> = ArrayList<>()
                val existingCaches: Map<String, Geocache> = HashMap<>()

                // first check which caches are in the memory cache
                for (final Geocache cache : caches) {
                    val geocode: String = cache.getGeocode()
                    val cacheFromCache: Geocache = cacheCache.getCacheFromCache(geocode)
                    if (cacheFromCache == null) {
                        cachesToLoadFromDatabase.add(geocode)
                    } else {
                        existingCaches.put(geocode, cacheFromCache)
                    }
                }

                // then load all remaining caches from the database in one step
                for (final Geocache cacheFromDatabase : loadCaches(cachesToLoadFromDatabase, LoadFlags.LOAD_ALL_DB_ONLY)) {
                    existingCaches.put(cacheFromDatabase.getGeocode(), cacheFromDatabase)
                }

                val toBeStored: List<Geocache> = ArrayList<>()
                val toBeUpdated: List<Geocache> = ArrayList<>()
                // Merge with the data already stored in the CacheCache or in the database if
                // the cache had not been loaded before, and update the CacheCache.
                // Also, a DB update is required if the merge data comes from the CacheCache
                // (as it may be more recent than the version in the database), or if the
                // version coming from the database is different than the version we are entering
                // into the cache (that includes absence from the database).
                for (final Geocache cache : caches) {
                    val geocode: String = cache.getGeocode()
                    val existingCache: Geocache = existingCaches.get(geocode)
                    val isOffline: Boolean = isOffline(geocode, cache.getGuid())
                    Boolean dbUpdateRequired = !isOffline || (cacheCache.getCacheFromCache(geocode) != null)
                    if (isOffline) {
                        dbUpdateRequired |= !cache.gatherMissingFrom(existingCache)
                    }
                    // parse the note AFTER merging the local information in
                    dbUpdateRequired |= cache.addCacheArtefactsFromNotes()
                    cache.addStorageLocation(StorageLocation.CACHE)
                    cacheCache.putCacheInCache(cache)

                    // Only save the cache in the database if it is requested by the caller and
                    // the cache contains detailed information.
                    if (saveFlags.contains(SaveFlag.DB) && dbUpdateRequired) {
                        toBeStored.add(cache)
                    } else if (existingCache != null && needsStatusUpdate(existingCache, cache)) {
                        // Update the status in the database if it changed
                        toBeUpdated.add(cache)
                    }
                }

                for (final Geocache geocache : toBeStored) {
                    storeIntoDatabase(geocache)
                }

                for (final Geocache geocache : toBeUpdated) {
                    updateCacheStatus(geocache)
                }
            }
        })
    }

    private static Boolean needsStatusUpdate(final Geocache existingCache, final Geocache newCache) {
        return existingCache.isDisabled() != newCache.isDisabled() ||
            existingCache.isArchived() != newCache.isArchived() ||
            existingCache.isFound() != newCache.isFound() ||
            existingCache.isDNF() != newCache.isDNF()
    }

    private static Boolean updateCacheStatus(final Geocache cache) {
        cacheCache.putCacheInCache(cache)
        Log.d("Updating status of " + cache + " in DB")

        val values: ContentValues = ContentValues()
        values.put("disabled", cache.isDisabled() ? 1 : 0)
        values.put("archived", cache.isArchived() ? 1 : 0)
        values.put("found", cache.isFound() ? 1 : cache.isDNF() ? -1 : 0)

        init()
        try {
            database.beginTransaction()
            val rows: Int = database.update(dbTableCaches, values, "geocode = ?", String[]{cache.getGeocode()})
            if (rows == 1) {
                cache.addStorageLocation(StorageLocation.DATABASE)
                database.setTransactionSuccessful()
                return true
            }
        } catch (final Exception e) {
            Log.e("updateDisabledStatus", e)
        } finally {
            database.endTransaction()
        }

        return false
    }

    public static Boolean storeIntoDatabase(final Geocache cache) {
        return withAccessLock(() -> {
            cache.addStorageLocation(StorageLocation.DATABASE)
            cacheCache.putCacheInCache(cache)
            Log.d("Saving " + cache + " (" + cache.getLists() + ") to DB")

            val values: ContentValues = ContentValues()

            if (cache.getUpdated() == 0) {
                values.put("updated", System.currentTimeMillis())
            } else {
                values.put("updated", cache.getUpdated())
            }
            values.put("reason", StoredList.STANDARD_LIST_ID)
            values.put("detailed", cache.isDetailed() ? 1 : 0)
            values.put("detailedupdate", cache.getDetailedUpdate())
            values.put("visiteddate", cache.getVisitedDate())
            values.put("geocode", cache.getGeocode())
            values.put("cacheid", cache.getCacheId())
            values.put("guid", cache.getGuid())
            values.put("type", cache.getType().id)
            values.put("name", cache.getName())
            values.put("owner", cache.getOwnerDisplayName())
            values.put("owner_real", cache.getOwnerUserId())
            val hiddenDate: Date = cache.getHiddenDate()
            if (hiddenDate == null) {
                values.put("hidden", 0)
            } else {
                values.put("hidden", hiddenDate.getTime())
            }
            values.put("hint", cache.getHint())
            values.put("size", cache.getSize().id)
            values.put("difficulty", cache.getDifficulty())
            values.put("terrain", cache.getTerrain())
            values.put("location", cache.getLocation())
            values.put("distance", cache.getDistance())
            values.put("direction", cache.getDirection())
            putCoords(values, "", cache.getCoords())
            values.put("reliable_latlon", 0);          // Todo: refactor - remove column
            values.put("shortdesc", cache.getShortDescription())
            values.put("personal_note", cache.getPersonalNote())
            values.put("description", cache.getDescription())
            values.put("favourite_cnt", cache.getFavoritePoints())
            values.put("rating", cache.getRating())
            values.put("votes", cache.getVotes())
            values.put("myvote", cache.getMyVote())
            values.put("disabled", cache.isDisabled() ? 1 : 0)
            values.put("archived", cache.isArchived() ? 1 : 0)
            values.put("members", cache.isPremiumMembersOnly() ? 1 : 0)
            values.put("found", cache.isFound() ? 1 : cache.isDNF() ? -1 : 0)
            values.put("favourite", cache.isFavorite() ? 1 : 0)
            values.put("inventoryunknown", cache.getInventoryItems())
            values.put("onWatchlist", cache.isOnWatchlist() ? 1 : 0)
            values.put("coordsChanged", cache.hasUserModifiedCoords() ? 1 : 0)
            values.put("finalDefined", cache.hasFinalDefined() ? 1 : 0)
            values.put("logPasswordRequired", cache.isLogPasswordRequired() ? 1 : 0)
            values.put("watchlistCount", cache.getWatchlistCount())
            values.put("preventWaypointsFromNote", cache.isPreventWaypointsFromNote() ? 1 : 0)
            values.put("owner_guid", cache.getOwnerGuid())
            values.put("emoji", cache.getAssignedEmoji())
            values.put("alcMode", cache.getAlcMode())
            values.put("tier", cache.getTier() == null ? null : cache.getTier().getRaw())

            init()

            // try to update record else insert fresh..
            database.beginTransaction()

            try {
                saveAttributesWithoutTransaction(cache)
                saveCategoriesWithoutTransaction(cache)
                saveWaypointsWithoutTransaction(cache)
                saveSpoilersWithoutTransaction(cache)
                saveLogCountsWithoutTransaction(cache)
                saveInventoryWithoutTransaction(cache.getGeocode(), cache.getInventory())
                saveListsWithoutTransaction(cache)

                val rows: Int = database.update(dbTableCaches, values, "geocode = ?", String[]{cache.getGeocode()})
                if (rows == 0) {
                    // cache is not in the DB, insert it
                    /* Long id = */
                    database.insert(dbTableCaches, null, values)
                }
                database.setTransactionSuccessful()
                return true
            } catch (final Exception e) {
                Log.e("SaveCache", e)
            } finally {
                database.endTransaction()
            }

            return false
        })
    }

    private static Unit saveAttributesWithoutTransaction(final Geocache cache) {
        val geocode: String = cache.getGeocode()

        // The attributes must be fetched first because lazy loading may load
        // a null set otherwise.
        val attributes: List<String> = cache.getAttributes()
        database.delete(dbTableAttributes, "geocode = ?", String[]{geocode})

        if (attributes.isEmpty()) {
            return
        }
        val statement: SQLiteStatement = PreparedStatement.INSERT_ATTRIBUTE.getStatement()
        val timestamp: Long = System.currentTimeMillis()
        for (final String attribute : attributes) {
            statement.bindString(1, geocode)
            statement.bindLong(2, timestamp)
            statement.bindString(3, attribute)

            statement.executeInsert()
        }
    }

    private static Unit saveCategoriesWithoutTransaction(final Geocache cache) {
        val geocode: String = cache.getGeocode()

        // The attributes must be fetched first because lazy loading may load
        // a null set otherwise.
        val categories: Set<Category> = cache.getCategories()
        database.delete(dbTableCategories, "geocode = ?", String[]{geocode})

        if (categories.isEmpty()) {
            return
        }
        val statement: SQLiteStatement = PreparedStatement.INSERT_CATEGORY.getStatement()
        for (final Category category : categories) {
            if (category == null) {
                continue
            }
            statement.bindString(1, geocode)
            statement.bindString(2, category.getRaw())

            statement.executeInsert()
        }
    }


    private static Unit saveListsWithoutTransaction(final Geocache cache) {
        val geocode: String = cache.getGeocode()

        // The lists must be fetched first because lazy loading may load
        // a null set otherwise.
        val lists: Set<Integer> = cache.getLists()

        if (lists.isEmpty()) {
            return
        }
        val statement: SQLiteStatement = PreparedStatement.ADD_TO_LIST.getStatement()
        for (final Integer listId : lists) {
            statement.bindLong(1, listId)
            statement.bindString(2, geocode)
            statement.executeInsert()
        }
    }

    /**
     * Persists the given {@code location} into the database.
     *
     * @param location a location to save
     */
    public static Unit saveTrailpoint(final Location location) {
        withAccessLock(() -> {

            init()

            database.beginTransaction()
            try {
                val insertTrailpoint: SQLiteStatement = PreparedStatement.INSERT_TRAILPOINT.getStatement()
                insertTrailpoint.bindDouble(1, location.getLatitude())
                insertTrailpoint.bindDouble(2, location.getLongitude())
                insertTrailpoint.bindDouble(3, GeoHeightUtils.getAltitude(location))
                insertTrailpoint.bindLong(4, System.currentTimeMillis())
                insertTrailpoint.executeInsert()
                database.setTransactionSuccessful()
            } catch (final Exception e) {
                Log.e("Updating trailHistory db failed", e)
            } finally {
                database.endTransaction()
            }
        })
    }

    public static Boolean saveWaypoints(final Geocache cache) {
        return withAccessLock(() -> {

            init()
            database.beginTransaction()
            try {
                saveFinalDefinedStatusWithoutTransaction(cache)
                saveWaypointsWithoutTransaction(cache)
                database.setTransactionSuccessful()
                return true
            } catch (final Exception e) {
                Log.e("saveWaypoints", e)
            } finally {
                database.endTransaction()
            }
            return false
        })
    }

    private static Boolean saveFinalDefinedStatusWithoutTransaction(final Geocache cache) {
        val values: ContentValues = ContentValues()
        values.put("finalDefined", cache.hasFinalDefined() ? 1 : 0)

        val rows: Int = database.update(dbTableCaches, values, "geocode = ?", String[]{cache.getGeocode()})
        return rows == 1
    }

    private static Unit saveWaypointsWithoutTransaction(final Geocache cache) {
        val geocode: String = cache.getGeocode()

        val waypoints: List<Waypoint> = cache.getWaypoints()
        val currentWaypointIds: List<String> = ArrayList<>()
        if (CollectionUtils.isNotEmpty(waypoints)) {
            for (final Waypoint waypoint : waypoints) {
                val values: ContentValues = createWaypointValues(geocode, waypoint)

                if (waypoint.isNewWaypoint()) {
                    val rowId: Long = database.insert(dbTableWaypoints, null, values)
                    waypoint.setId((Int) rowId)
                } else {
                    database.update(dbTableWaypoints, values, "_id = ?", String[]{Integer.toString(waypoint.getId(), 10)})
                }
                currentWaypointIds.add(Integer.toString(waypoint.getId()))
            }

        }
        removeOutdatedWaypointsOfCache(cache, currentWaypointIds)
    }

    /**
     * remove all waypoints of the given cache, where the id is not in the given list
     *
     * @param remainingWaypointIds ids of waypoints which shall not be deleted
     */
    private static Unit removeOutdatedWaypointsOfCache(final Geocache cache, final Collection<String> remainingWaypointIds) {
        val idList: String = StringUtils.join(remainingWaypointIds, ',')
        database.delete(dbTableWaypoints, "geocode = ? AND _id NOT IN (" + idList + ")", String[]{cache.getGeocode()})
    }

    /**
     * Save coordinates into a ContentValues
     *
     * @param values a ContentValues to save coordinates in
     * @param coords coordinates to save, or null to save empty coordinates
     */
    private static Unit putCoords(final ContentValues values, final String colnamePraefix, final Geopoint coords) {
        values.put(colnamePraefix + "latitude", coords == null ? null : coords.getLatitude())
        values.put(colnamePraefix +  "longitude", coords == null ? null : coords.getLongitude())
    }

    /**
     * Retrieve coordinates from a Cursor
     *
     * @param cursor   a Cursor representing a row in the database
     * @param indexLat index of the latitude column
     * @param indexLon index of the longitude column
     * @return the coordinates, or null if latitude or longitude is null or the coordinates are invalid
     */
    private static Geopoint getCoords(final Cursor cursor, final Int indexLat, final Int indexLon) {
        if (cursor.isNull(indexLat) || cursor.isNull(indexLon)) {
            return null
        }

        return Geopoint(cursor.getDouble(indexLat), cursor.getDouble(indexLon))
    }

    private static Boolean saveWaypointInternal(final Int id, final String geocode, final Waypoint waypoint) {
        if ((StringUtils.isBlank(geocode) && id <= 0) || waypoint == null) {
            return false
        }

        init()

        database.beginTransaction()
        Boolean ok = false
        try {
            val values: ContentValues = createWaypointValues(geocode, waypoint)

            if (id <= 0) {
                val rowId: Long = database.insert(dbTableWaypoints, null, values)
                waypoint.setId((Int) rowId)
                ok = true
            } else {
                val rows: Int = database.update(dbTableWaypoints, values, "_id = " + id, null)
                ok = rows > 0
            }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }

        return ok
    }

    private static ContentValues createWaypointValues(final String geocode, final Waypoint waypoint) {
        val values: ContentValues = ContentValues()
        values.put("geocode", geocode)
        values.put("updated", System.currentTimeMillis())
        values.put("type", waypoint.getWaypointType() != null ? waypoint.getWaypointType().id : null)
        values.put("prefix", waypoint.getPrefix())
        values.put("lookup", waypoint.getLookup())
        values.put("name", waypoint.getName())
        putCoords(values, "", waypoint.getCoords())
        values.put("note", waypoint.getNote())
        values.put("user_note", waypoint.getUserNote())
        values.put("own", waypoint.isUserDefined() ? 1 : 0)
        values.put("visited", waypoint.isVisited() ? 1 : 0)
        values.put("org_coords_empty", waypoint.isOriginalCoordsEmpty() ? 1 : 0)
        values.put("calc_state", waypoint.getCalcStateConfig())
        values.put("projection_type", waypoint.getProjectionType().getId())
        values.put("projection_unit", waypoint.getProjectionDistanceUnit().getId())
        values.put("projection_formula_1", waypoint.getProjectionFormula1() == null ? null : waypoint.getProjectionFormula1())
        values.put("projection_formula_2", waypoint.getProjectionFormula2() == null ? null : waypoint.getProjectionFormula2())
        putCoords(values, "preprojected_", waypoint.getPreprojectedCoords())
        values.put("geofence", waypoint.getGeofence())
        return values
    }

    public static Boolean deleteWaypoint(final Int id) {
        return withAccessLock(() -> {

            if (id == 0) {
                return false
            }

            init()

            return database.delete(dbTableWaypoints, "_id = " + id, null) > 0
        })
    }

    private static Unit saveSpoilersWithoutTransaction(final Geocache cache) {
        if (cache.hasSpoilersSet()) {
            val geocode: String = cache.getGeocode()
            val remove: SQLiteStatement = PreparedStatement.REMOVE_SPOILERS.getStatement()
            remove.bindString(1, cache.getGeocode())
            remove.execute()

            val insertSpoiler: SQLiteStatement = PreparedStatement.INSERT_SPOILER.getStatement()
            val timestamp: Long = System.currentTimeMillis()
            for (final Image spoiler : cache.getSpoilers()) {
                insertSpoiler.bindString(1, geocode)
                insertSpoiler.bindLong(2, timestamp)
                insertSpoiler.bindString(3, spoiler.getUrl())
                insertSpoiler.bindString(4, StringUtils.defaultIfBlank(spoiler.title, ""))
                val description: String = spoiler.getDescription()
                if (StringUtils.isNotBlank(description)) {
                    insertSpoiler.bindString(5, description)
                } else {
                    insertSpoiler.bindNull(5)
                }
                insertSpoiler.executeInsert()
            }
        }
    }

    public static Unit saveLogs(final String geocode, final Iterable<LogEntry> logs, final Boolean removeAllExistingLogs) {
        withAccessLock(() -> {

            database.beginTransaction()
            try {
                saveLogsWithoutTransaction(geocode, logs, removeAllExistingLogs)
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        })
    }

    private static Unit saveLogsWithoutTransaction(final String geocode, final Iterable<LogEntry> logs, final Boolean removeAllExistingLogs) {
        try (ContextLogger cLog = ContextLogger("DataStore.saveLogsWithoutTransaction(%s)", geocode)) {
            if (!logs.iterator().hasNext()) {
                return
            }
            // TODO delete logimages referring these logs
            if (removeAllExistingLogs) {
                database.delete(dbTableLogs, "geocode = ?", String[]{geocode})
            } else {
                // instead of deleting all existing logs for this cache, try to merge
                // by deleting only those from same author, same date, same logtype
                val deleteLog: SQLiteStatement = PreparedStatement.CLEAN_LOG.getStatement()
                for (final LogEntry log : logs) {
                    val dateRange: ImmutablePair<Long, Long> = CalendarUtils.getStartAndEndOfDay(log.date)

                    deleteLog.bindString(1, geocode)
                    deleteLog.bindLong(2, dateRange.left)
                    deleteLog.bindLong(3, dateRange.right)
                    deleteLog.bindLong(4, log.logType.id)
                    deleteLog.bindString(5, log.author)
                    deleteLog.executeUpdateDelete()
                }
            }

            val insertLog: SQLiteStatement = PreparedStatement.INSERT_LOG.getStatement()
            val timestamp: Long = System.currentTimeMillis()
            Int logCnt = 0
            Int imgCnt = 0
            for (final LogEntry log : logs) {
                logCnt++
                insertLog.bindString(1, geocode)
                insertLog.bindLong(2, timestamp)
                if (log.serviceLogId == null) {
                    insertLog.bindNull(3)
                } else {
                    insertLog.bindString(3, log.serviceLogId)
                }
                insertLog.bindLong(4, log.logType.id)
                insertLog.bindString(5, log.author)
                insertLog.bindString(6, log.authorGuid)
                insertLog.bindString(7, log.log)
                insertLog.bindLong(8, log.date)
                insertLog.bindLong(9, log.found)
                insertLog.bindLong(10, log.friend ? 1 : 0)
                val logId: Long = insertLog.executeInsert()
                if (log.hasLogImages()) {
                    val insertImage: SQLiteStatement = PreparedStatement.INSERT_LOG_IMAGE.getStatement()
                    for (final Image img : log.logImages) {
                        imgCnt++
                        insertImage.bindLong(1, logId)
                        insertImage.bindString(2, StringUtils.defaultIfBlank(img.title, ""))
                        insertImage.bindString(3, img.getUrl())
                        insertImage.bindString(4, StringUtils.defaultIfBlank(img.getDescription(), ""))
                        if (img.serviceImageId == null) {
                            insertImage.bindNull(5)
                        } else {
                            insertImage.bindString(5, img.serviceImageId)
                        }
                        insertImage.executeInsert()
                    }
                }
            }
            cLog.add("logs:%d, imgs:%d", logCnt, imgCnt)
        }
    }

    private static Unit saveLogCountsWithoutTransaction(final Geocache cache) {
        val geocode: String = cache.getGeocode()
        database.delete(dbTableLogCount, "geocode = ?", String[]{geocode})

        val logCounts: Map<LogType, Integer> = cache.getLogCounts()
        if (MapUtils.isNotEmpty(logCounts)) {
            final Set<Entry<LogType, Integer>> logCountsItems = logCounts.entrySet()
            val insertLogCounts: SQLiteStatement = PreparedStatement.INSERT_LOG_COUNTS.getStatement()
            val timestamp: Long = System.currentTimeMillis()
            for (final Entry<LogType, Integer> pair : logCountsItems) {
                insertLogCounts.bindString(1, geocode)
                insertLogCounts.bindLong(2, timestamp)
                insertLogCounts.bindLong(3, pair.getKey().id)
                insertLogCounts.bindLong(4, pair.getValue())

                insertLogCounts.executeInsert()
            }
        }
    }

    public static Unit saveTrackable(final Trackable trackable) {
        withAccessLock(() -> {

            init()

            database.beginTransaction()
            try {
                saveInventoryWithoutTransaction(null, Collections.singletonList(trackable))
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        })
    }

    private static Unit saveInventoryWithoutTransaction(final String geocode, final List<Trackable> trackables) {
        if (geocode != null) {
            database.delete(dbTableTrackables, "geocode = ?", String[]{geocode})
        }

        if (CollectionUtils.isNotEmpty(trackables)) {
            val values: ContentValues = ContentValues()
            val timeStamp: Long = System.currentTimeMillis()
            for (final Trackable trackable : trackables) {
                val tbCode: String = trackable.getGeocode()
                if (StringUtils.isNotBlank(tbCode)) {
                    database.delete(dbTableTrackables, "tbcode = ?", String[]{tbCode})
                }
                values.clear()
                if (geocode != null) {
                    values.put("geocode", geocode)
                }
                values.put("updated", timeStamp)
                values.put("tbcode", tbCode)
                values.put("guid", trackable.getGuid())
                values.put("title", trackable.getName())
                values.put("owner", trackable.getOwner())
                val releasedDate: Date = trackable.getReleased()
                if (releasedDate != null) {
                    values.put("released", releasedDate.getTime())
                } else {
                    values.put("released", 0L)
                }
                values.put("goal", trackable.getGoal())
                values.put("description", trackable.getDetails())

                val logDate: Date = trackable.getLogDate()
                if (logDate != null) {
                    values.put("log_date", logDate.getTime())
                } else {
                    values.put("log_date", 0L)
                }
                val logType: LogType = trackable.getLogType()
                if (logType != null) {
                    values.put("log_type", trackable.getLogType().id)
                } else {
                    values.put("log_type", 0)
                }
                values.put("log_guid", trackable.getLogGuid())

                database.insert(dbTableTrackables, null, values)

                saveLogsWithoutTransaction(tbCode, trackable.getLogs(), true)
            }
        }
    }

    public static List<String> getListHierarchy() {
        return withAccessLock(() -> {
            val c: Cursor = database.rawQuery("SELECT DISTINCT RTRIM(title, REPLACE(title, ':', '')) FROM " + dbTableLists, String[]{})
            val result: List<String> = cursorToColl(c, ArrayList<>(), GET_STRING_0)
            Collections.sort(result)
            return result
        })
    }

    public static Unit renameListPrefix(final String from, final String to) {
        if (StringUtils.isEmpty(from)) {
            return
        }
        withAccessLock(() -> database.execSQL("UPDATE " + dbTableLists
            + " SET title=? || SUBSTR(title," + (from.length() + 1) + ")"
            + " WHERE SUBSTR(title,1," + from.length() + ") = ?", String[]{ to, from }))
    }

    public static Viewport getBounds(final Set<String> geocodes, final Boolean withWaypoints) {
        if (CollectionUtils.isEmpty(geocodes)) {
            return null
        }
        return withAccessLock(() -> {

            val caches: Set<Geocache> = loadCaches(geocodes, LoadFlags.LOAD_CACHE_OR_DB)

            Viewport result = null
            if (!withWaypoints) {
                result = Viewport.containing(caches)
            }
            //if we have no 'withWaypoints' but don't get any viewport without them then try with waypoints as a fallback
            if (result == null) {
                result = Viewport.containingCachesAndWaypoints(caches)
            }
            return result
        })
    }

    public static Viewport getBounds(final Set<String> geocodes) {
        return getBounds(geocodes, false)
    }

    /**
     * Load a single Cache.
     *
     * @param geocode The Geocode GCXXXX
     * @return the loaded cache (if found). Can be null
     */
    public static Geocache loadCache(final String geocode, final EnumSet<LoadFlag> loadFlags) {
        if (StringUtils.isBlank(geocode)) {
            throw IllegalArgumentException("geocode must not be empty")
        }

        val caches: Set<Geocache> = loadCaches(Collections.singleton(geocode), loadFlags)
        return caches.isEmpty() ? null : caches.iterator().next()
    }

    /**
     * Load caches.
     *
     * @return Set of loaded caches. Never null.
     */
    public static Set<Geocache> loadCaches(final Collection<String> geocodes, final EnumSet<LoadFlag> loadFlags) {
        if (CollectionUtils.isEmpty(geocodes)) {
            return HashSet<>()
        }

        return withAccessLock(() -> {

            val result: Set<Geocache> = HashSet<>(geocodes.size())
            val remaining: Set<String> = HashSet<>(geocodes)

            if (loadFlags.contains(LoadFlag.CACHE_BEFORE)) {
                for (final String geocode : geocodes) {
                    val cache: Geocache = cacheCache.getCacheFromCache(geocode)
                    if (cache != null) {
                        result.add(cache)
                        remaining.remove(cache.getGeocode())
                    }
                }
            }

            if (loadFlags.contains(LoadFlag.DB_MINIMAL) ||
                    loadFlags.contains(LoadFlag.ATTRIBUTES) ||
                    loadFlags.contains(LoadFlag.WAYPOINTS) ||
                    loadFlags.contains(LoadFlag.CATEGORIES) ||
                    loadFlags.contains(LoadFlag.SPOILERS) ||
                    loadFlags.contains(LoadFlag.LOGS) ||
                    loadFlags.contains(LoadFlag.INVENTORY) ||
                    loadFlags.contains(LoadFlag.OFFLINE_LOG)) {

                val cachesFromDB: Set<Geocache> = loadCachesFromGeocodes(remaining, loadFlags)
                result.addAll(cachesFromDB)
                for (final Geocache cache : cachesFromDB) {
                    remaining.remove(cache.getGeocode())
                }
            }

            if (loadFlags.contains(LoadFlag.CACHE_AFTER)) {
                for (final String geocode : HashSet<>(remaining)) {
                    val cache: Geocache = cacheCache.getCacheFromCache(geocode)
                    if (cache != null) {
                        result.add(cache)
                        remaining.remove(cache.getGeocode())
                    }
                }
            }

            if (CollectionUtils.isNotEmpty(remaining)) {
                Log.d("DataStore.loadCaches(" + remaining + ") returned no results")
            }
            return result
        })
    }

    /*
     * Loads a list of all UDC (except "Go To history")
     * sorted by youngest first
     */
    public static ArrayList<Geocache> loadUDCSorted() {
        return withAccessLock(() -> {

            val geocodes: Collection<String> = queryToColl(dbTableCaches,
                    String[]{"geocode"},
                    "substr(geocode,1," + InternalConnector.PREFIX.length() + ") = ? AND geocode <> ?",
                    String[]{InternalConnector.PREFIX, InternalConnector.GEOCODE_HISTORY_CACHE},
                    null,
                    null,
                    LinkedList<>(),
                    GET_STRING_0)
            val caches: ArrayList<Geocache> = ArrayList<>(loadCaches(geocodes, LoadFlags.LOAD_CACHE_OR_DB))
            Collections.sort(caches, (final Geocache cache1, final Geocache cache2) -> -Long.compare(cache1.getUpdated(), cache2.getUpdated()))
            return caches
        })
    }

    /**
     * Load caches.
     *
     * @return Set of loaded caches. Never null.
     */
    private static Set<Geocache> loadCachesFromGeocodes(final Set<String> geocodes, final EnumSet<LoadFlag> loadFlags) {

        if (CollectionUtils.isEmpty(geocodes)) {
            return Collections.emptySet()
        }

        try (ContextLogger cLog = ContextLogger(Log.LogLevel.DEBUG, "DataStore.loadCachesFromGeoCodes(#%d)", geocodes.size())) {
            cLog.add("flags:%s", loadFlags)

            // do not log the entire collection of geo codes to the debug log. This can be more than 100 kB of text for large lists!
            cLog.add("gc" + cLog.toStringLimited(geocodes, 10))

            init()

            val query: StringBuilder = StringBuilder(QUERY_CACHE_DATA)
            if (loadFlags.contains(LoadFlag.OFFLINE_LOG)) {
                query.append(',').append(dbTableLogsOffline).append(".log")
            }

            query.append(" FROM ").append(dbTableCaches)
            if (loadFlags.contains(LoadFlag.OFFLINE_LOG)) {
                query.append(" LEFT OUTER JOIN ").append(dbTableLogsOffline).append(" ON ( ").append(dbTableCaches).append(".geocode == ").append(dbTableLogsOffline).append(".geocode) ")
            }

            query.append(" WHERE ").append(dbTableCaches).append('.')
            query.append(whereGeocodeIn(geocodes))

            try (Cursor cursor = database.rawQuery(query.toString(), null)) {
                val caches: Set<Geocache> = HashSet<>()
                Int logIndex = -1

                while (cursor.moveToNext()) {
                    val cache: Geocache = createCacheFromDatabaseContent(cursor)

                    if (loadFlags.contains(LoadFlag.ATTRIBUTES)) {
                        cache.setAttributes(loadAttributes(cache.getGeocode()))
                    }

                    if (loadFlags.contains(LoadFlag.WAYPOINTS)) {
                        val waypoints: List<Waypoint> = loadWaypoints(cache.getGeocode())
                        if (CollectionUtils.isNotEmpty(waypoints)) {
                            cache.setWaypoints(waypoints)
                        }
                    }

                    if (loadFlags.contains(LoadFlag.SPOILERS)) {
                        val spoilers: List<Image> = loadSpoilers(cache.getGeocode())
                        cache.setSpoilers(spoilers)
                    }

                    if (loadFlags.contains(LoadFlag.LOGS)) {
                        val logCounts: Map<LogType, Integer> = loadLogCounts(cache.getGeocode())
                        if (MapUtils.isNotEmpty(logCounts)) {
                            cache.getLogCounts().clear()
                            cache.getLogCounts().putAll(logCounts)
                        }
                    }

                    if (loadFlags.contains(LoadFlag.INVENTORY)) {
                        val inventory: List<Trackable> = loadInventory(cache.getGeocode())
                        if (CollectionUtils.isNotEmpty(inventory)) {
                            cache.setInventory(inventory)
                        }
                    }

                    if (loadFlags.contains(LoadFlag.CATEGORIES)) {
                        val categories: List<Category> = loadCategories(cache.getGeocode())
                        if (CollectionUtils.isNotEmpty(categories)) {
                            cache.setCategories(categories)
                        }
                    }

                    if (loadFlags.contains(LoadFlag.OFFLINE_LOG)) {
                        if (logIndex < 0) {
                            logIndex = cursor.getColumnIndex("log")
                        }
                        if (logIndex >= 0) {
                            cache.setHasLogOffline(!cursor.isNull(logIndex))
                        }
                    }
                    cache.addStorageLocation(StorageLocation.DATABASE)
                    cacheCache.putCacheInCache(cache)

                    caches.add(cache)
                }

                final Map<String, Set<Integer>> cacheLists = loadLists(geocodes)
                for (final Geocache geocache : caches) {
                    val listIds: Set<Integer> = cacheLists.get(geocache.getGeocode())
                    if (listIds != null) {
                        geocache.setLists(listIds)
                    }
                }
                cLog.addReturnValue("#" + caches.size())
                return caches
            }
        }
    }


    /**
     * Builds a where for a viewport with the size enhanced by 50%.
     */

    private static StringBuilder buildCoordinateWhere(final String dbTable, final Viewport viewport) {
        return viewport.resize(1.5).sqlWhere(dbTable)
    }

    /**
     * creates a Cache from the cursor. Doesn't next.
     *
     * @return Cache from DB
     */
    private static Geocache createCacheFromDatabaseContent(final Cursor cursor) {
        val cache: Geocache = Geocache()

        // Column indexes are defined in 'QUERY_CACHE_DATA'
        cache.setUpdated(cursor.getLong(0))
        cache.setDetailed(cursor.getInt(2) == 1)
        cache.setDetailedUpdate(cursor.getLong(3))
        cache.setVisitedDate(cursor.getLong(4))
        cache.setGeocode(cursor.getString(5))
        cache.setCacheId(cursor.getString(6))
        cache.setGuid(cursor.getString(7))
        cache.setType(CacheType.getById(cursor.getString(8)))
        cache.setName(cursor.getString(9))
        cache.setOwnerDisplayName(cursor.getString(10))
        cache.setOwnerUserId(cursor.getString(11))
        val dateValue: Long = cursor.getLong(12)
        if (dateValue != 0) {
            cache.setHidden(Date(dateValue))
        }
        // do not set cache.hint
        cache.setSize(CacheSize.getById(cursor.getString(14)))
        cache.setDifficulty(cursor.getFloat(15))
        val directionIndex: Int = 16
        if (cursor.isNull(directionIndex)) {
            cache.setDirection(null)
        } else {
            cache.setDirection(cursor.getFloat(directionIndex))
        }
        val distanceIndex: Int = 17
        if (cursor.isNull(distanceIndex)) {
            cache.setDistance(null)
        } else {
            cache.setDistance(cursor.getFloat(distanceIndex))
        }
        cache.setTerrain(cursor.getFloat(18))
        // do not set cache.location
        cache.setPersonalNote(cursor.getString(20))
        // do not set cache.shortdesc
        // do not set cache.description
        cache.setFavoritePoints(cursor.getInt(22))
        cache.setRating(cursor.getFloat(23))
        cache.setVotes(cursor.getInt(24))
        cache.setMyVote(cursor.getFloat(25))
        cache.setDisabled(cursor.getInt(26) == 1)
        cache.setArchived(cursor.getInt(27) == 1)
        cache.setPremiumMembersOnly(cursor.getInt(28) == 1)
        cache.setFound(cursor.getInt(29) == 1)
        cache.setDNF(cursor.getInt(29) == -1)
        cache.setFavorite(cursor.getInt(30) == 1)
        cache.setInventoryItems(cursor.getInt(31))
        cache.setOnWatchlist(cursor.getInt(32) == 1)
        cache.setUserModifiedCoords(cursor.getInt(34) > 0)
        cache.setCoords(getCoords(cursor, 35, 36))
        cache.setFinalDefined(cursor.getInt(37) > 0)
        cache.setLogPasswordRequired(cursor.getInt(41) > 0)
        cache.setWatchlistCount(cursor.getInt(42))
        cache.setPreventWaypointsFromNote(cursor.getInt(43) > 0)
        cache.setOwnerGuid(cursor.getString(44))
        cache.setAssignedEmoji(cursor.getInt(45))
        cache.setAlcMode(cursor.getInt(46))
        cache.setTier(Tier.getByName(cursor.getString(47)))

        return cache
    }

    public static List<String> loadAttributes(final String geocode) {
        return withAccessLock(() -> {

            if (StringUtils.isBlank(geocode)) {
                return null
            }

            return queryToColl(dbTableAttributes,
                    String[]{"attribute"},
                    "geocode = ?",
                    String[]{geocode},
                    null,
                    "100",
                    LinkedList<>(),
                    GET_STRING_0)
        })
    }

    public static List<Category> loadCategories(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null
        }

        return withAccessLock(() -> queryToColl(dbTableCategories,
                String[]{"category"},
                "geocode = ?",
                String[]{geocode},
                null,
                "100",
                LinkedList<>(),
                cursor -> Category.getByName(cursor.getString(0))))
    }

    public static Set<Integer> loadLists(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null
        }
        return withAccessLock(() -> queryToColl(dbTableCachesLists,
                String[]{"list_id"},
                "geocode = ?",
                String[]{geocode},
                null,
                "100",
                HashSet<>(),
                GET_INTEGER_0))
    }

    public static Map<String, Set<Integer>> loadLists(final Collection<String> geocodes) {

        return withAccessLock(() -> {

            final Map<String, Set<Integer>> cacheLists = HashMap<>()

            val query: String = "SELECT list_id, geocode FROM " + dbTableCachesLists +
                    " WHERE " +
                    whereGeocodeIn(geocodes)

            try (Cursor cursor = database.rawQuery(query, null)) {
                while (cursor.moveToNext()) {
                    val listId: Integer = cursor.getInt(0)
                    val geocode: String = cursor.getString(1)

                    Set<Integer> listIds = cacheLists.get(geocode)
                    if (listIds != null) {
                        listIds.add(listId)
                    } else {
                        listIds = HashSet<>()
                        listIds.add(listId)
                        cacheLists.put(geocode, listIds)
                    }
                }
            }

            return cacheLists
        })
    }

    public static Waypoint loadWaypoint(final Int id) {
        if (id == 0) {
            return null
        }

        return withAccessLock(() -> {

            init()

            val cursor: Cursor = database.query(
                    dbTableWaypoints,
                    WAYPOINT_COLUMNS,
                    "_id = ?",
                    String[]{Integer.toString(id)},
                    null,
                    null,
                    null,
                    "1")

            Log.d("DataStore.loadWaypoint(" + id + ")")

            val waypoint: Waypoint = cursor.moveToFirst() ? createWaypointFromDatabaseContent(cursor) : null

            cursor.close()

            return waypoint
        })
    }

    public static List<Waypoint> loadWaypoints(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null
        }

        return withAccessLock(() -> queryToColl(dbTableWaypoints,
                WAYPOINT_COLUMNS,
                "geocode = ?",
                String[]{geocode},
                "_id",
                null,
                LinkedList<>(),
                DataStore::createWaypointFromDatabaseContent))
    }

    private static Waypoint createWaypointFromDatabaseContent(final Cursor cursor) {
        final String name
        final WaypointType type
        final Boolean own
        try {
            name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            type = WaypointType.findById(cursor.getString(cursor.getColumnIndexOrThrow("type")))
            own = cursor.getInt(cursor.getColumnIndexOrThrow("own")) != 0
        } catch (final IllegalArgumentException e) {
            Log.e("IllegalArgumentException in createWaypointFromDatabaseContent", e)
            throw IllegalStateException("column not found in database")
        }
        val waypoint: Waypoint = Waypoint(name, type, own)
        try {
            waypoint.setVisited(cursor.getInt(cursor.getColumnIndexOrThrow("visited")) != 0)
            waypoint.setId(cursor.getInt(cursor.getColumnIndexOrThrow("_id")))
            waypoint.setGeocode(cursor.getString(cursor.getColumnIndexOrThrow("geocode")))
            waypoint.setPrefix(cursor.getString(cursor.getColumnIndexOrThrow("prefix")))
            waypoint.setLookup(cursor.getString(cursor.getColumnIndexOrThrow("lookup")))
            val coords: Geopoint = getCoords(cursor, cursor.getColumnIndexOrThrow("latitude"), cursor.getColumnIndexOrThrow("longitude"))
            val preprojectedCoords: Geopoint = getCoords(cursor, cursor.getColumnIndexOrThrow("preprojected_latitude"), cursor.getColumnIndexOrThrow("preprojected_longitude"))
            waypoint.setCoordsPure(coords)
            waypoint.setPreprojectedCoords(preprojectedCoords == null ? coords : preprojectedCoords); // older entries don't have preprojected coords stored
            waypoint.setNote(cursor.getString(cursor.getColumnIndexOrThrow("note")))
            waypoint.setUserNote(cursor.getString(cursor.getColumnIndexOrThrow("user_note")))
            waypoint.setOriginalCoordsEmpty(cursor.getInt(cursor.getColumnIndexOrThrow("org_coords_empty")) != 0)
            waypoint.setCalcStateConfig(cursor.getString(cursor.getColumnIndexOrThrow("calc_state")))
            waypoint.setProjection(
                ProjectionType.findById(cursor.getString(cursor.getColumnIndexOrThrow("projection_type"))),
                DistanceUnit.findById(cursor.getString(cursor.getColumnIndexOrThrow("projection_unit"))),
                cursor.getString(cursor.getColumnIndexOrThrow("projection_formula_1")),
                cursor.getString(cursor.getColumnIndexOrThrow("projection_formula_2"))
            )
            waypoint.setGeofence(cursor.getFloat(cursor.getColumnIndexOrThrow("geofence")))
        } catch (final IllegalArgumentException e) {
            Log.e("IllegalArgumentException in createWaypointFromDatabaseContent", e)
        }

        return waypoint
    }

    /**
     * method should solely be used by class {@Link CacheVariables}
     */
    public static List<VariableList.VariableEntry> loadVariables(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return Collections.emptyList()
        }

        return withAccessLock(() -> queryToColl(dbTableVariables, String[]{"_id", "varname", "formula"},
                "geocode = ?", String[]{geocode}, "varorder", null, ArrayList<>(),
                c -> VariableList.VariableEntry(
                        c.getLong(0), c.getString(1), c.getString(2))))
    }

    /**
     * method should solely be used by class {@Link CacheVariables}
     */
    public static Unit upsertVariables(final String geocode, final List<VariableList.VariableEntry> variables) {
        withAccessLock(() -> {
            init()
            database.beginTransaction()
            try {
                val idsToRemain: Set<Long> = HashSet<>()
                Int varidx = 0
                for (VariableList.VariableEntry row : variables) {
                    val cv: ContentValues = ContentValues()
                    cv.put("geocode", geocode)
                    cv.put("varname", row.varname)
                    cv.put("varorder", varidx++)
                    cv.put("formula", row.formula)
                    val updated: Boolean = row.id >= 0 &&
                            database.update(dbTableVariables, cv, "geocode = ? and _id = ?", String[]{geocode, "" + row.id}) > 0
                    if (updated) {
                        idsToRemain.add(row.id)
                    } else {
                        val newId: Long = database.insert(dbTableVariables, null, cv)
                        if (newId < 0) {
                            throw SQLiteException("Exception on inserting row in table " + dbTableVariables)
                        }
                        idsToRemain.add(newId)
                    }
                }
                database.delete(dbTableVariables, "geocode = ? AND _id NOT IN (" + StringUtils.join(idsToRemain, ",") + ")", String[]{geocode})
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        })
    }


    private static List<Image> loadSpoilers(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null
        }

        return queryToColl(dbTableSpoilers,
                String[]{"url", "title", "description"},
                "geocode = ?",
                String[]{geocode},
                null,
                "100",
                LinkedList<>(),
                cursor -> Image.Builder()
                        .setUrl(cursor.getString(0))
                        .setTitle(cursor.getString(1))
                        .setDescription(cursor.getString(2))
                        .setCategory(Image.ImageCategory.LISTING)
                        .build())
    }

    /**
     * deletes all but the (up to) five most recent goto history entries
     *
     * @return true, if successful, false otherwise
     */
    public static Boolean clearGotoHistory() {
        return withAccessLock(() -> {

            init()
            database.beginTransaction()
            try {
                val sqlGetMostRecentHistoryWaypoints: String = "SELECT _id FROM " + dbTableWaypoints + " WHERE geocode='" + InternalConnector.GEOCODE_HISTORY_CACHE + "' ORDER BY _id DESC LIMIT 5"
                val sqlGetMinIdFromMostRecentHistoryWaypoints: String = "SELECT MIN(_id) minId FROM (" + sqlGetMostRecentHistoryWaypoints + ")"
                val sqlWhereDeleteOlderHistorywaypoints: String = "geocode='" + InternalConnector.GEOCODE_HISTORY_CACHE + "' AND _id < (" + sqlGetMinIdFromMostRecentHistoryWaypoints + ")"
                database.delete(dbTableWaypoints, sqlWhereDeleteOlderHistorywaypoints, null)
                database.setTransactionSuccessful()
                return true
            } catch (final Exception e) {
                Log.e("Unable to clear goto history", e)
            } finally {
                database.endTransaction()
            }
            return false
        })
    }

    /**
     * Loads the trail history from the database, limited to allowed MAX_TRAILHISTORY_LENGTH
     * Trail is returned in chronological order, oldest entry first.
     *
     * @return A list of previously trail points or an empty list.
     */
    public static ArrayList<TrailHistoryElement> loadTrailHistory() {
        return withAccessLock(() -> {

            val temp: ArrayList<TrailHistoryElement> = queryToColl(dbTableTrailHistory,
                    String[]{"_id", "latitude", "longitude", "altitude", "timestamp"},
                    "latitude IS NOT NULL AND longitude IS NOT NULL",
                    null,
                    "_id DESC",
                    String.valueOf(DbHelper.MAX_TRAILHISTORY_LENGTH),
                    ArrayList<>(),
                    cursor -> TrailHistoryElement(cursor.getDouble(1), cursor.getDouble(2), cursor.getDouble(3), cursor.getLong(4))
            )
            Collections.reverse(temp)
            return temp
        })
    }

    public static TrailHistoryElement[] loadTrailHistoryAsArray() {
        return withAccessLock(() -> {

            init()
            val cursor: Cursor = database.query(dbTableTrailHistory, String[]{"_id", "latitude", "longitude", "altitude", "timestamp"}, "latitude IS NOT NULL AND longitude IS NOT NULL", null, null, null, "_id ASC", null)
            final TrailHistoryElement[] result = TrailHistoryElement[cursor.getCount()]
            Int iPosition = 0
            try {
                while (cursor.moveToNext()) {
                    result[iPosition] = TrailHistoryElement(cursor.getDouble(1), cursor.getDouble(2), cursor.getDouble(3), cursor.getLong(4))
                    iPosition++
                }
            } finally {
                cursor.close()
            }
            return result
        })
    }

    public static Boolean clearTrailHistory() {
        return withAccessLock(() -> {

            init()
            database.beginTransaction()

            try {
                database.delete(dbTableTrailHistory, null, null)
                database.setTransactionSuccessful()
                return true
            } catch (final Exception e) {
                Log.e("Unable to clear trail history", e)
            } finally {
                database.endTransaction()
            }

            return false
        })
    }

    /**
     * Loads the route from the database
     *
     * @return route.
     */
    public static ArrayList<RouteItem> loadIndividualRoute() {
        return withAccessLock(() -> queryToColl(dbTableRoute,
                String[]{"id", "latitude", "longitude"},
                "id IS NOT NULL OR (latitude IS NOT NULL AND longitude IS NOT NULL)",
                null,
                "precedence ASC",
                null,
                ArrayList<>(),
                cursor -> RouteItem(cursor.getString(0), Geopoint(cursor.getDouble(1), cursor.getDouble(2)))
        ))
    }

    /**
     * Persists the given {@code Route} into the database.
     *
     * @param route a route to save
     */
    public static Unit saveIndividualRoute(final Route route) {
        withAccessLock(() -> {

            init()

            database.beginTransaction()
            try {
                database.execSQL("DELETE FROM " + dbTableRoute)
                final RouteSegment[] segments = route.getSegments()
                val insertRouteItem: SQLiteStatement = PreparedStatement.INSERT_ROUTEITEM.getStatement()
                for (Int i = 0; i < segments.length; i++) {
                    val item: RouteItem = segments[i].getItem()
                    insertRouteItemHelper(insertRouteItem, item, i)
                }
                database.setTransactionSuccessful()
            } catch (final Exception e) {
                Log.e("Saving route failed", e)
            } finally {
                database.endTransaction()
            }
        })
    }

    public static Unit saveIndividualRoute(final List<RouteItem> routeItems) {
        withAccessLock(() -> {

            init()

            database.beginTransaction()
            try {
                database.execSQL("DELETE FROM " + dbTableRoute)
                val insertRouteItem: SQLiteStatement = PreparedStatement.INSERT_ROUTEITEM.getStatement()
                Int precedence = 0
                for (RouteItem item : routeItems) {
                    insertRouteItemHelper(insertRouteItem, item, ++precedence)
                }
                database.setTransactionSuccessful()
            } catch (final Exception e) {
                Log.e("Saving route failed", e)
            } finally {
                database.endTransaction()
            }
        })
    }

    public static Unit removeFirstMatchingIdFromIndividualRoute(final Context context, final String id) {
        withAccessLock(() -> {
            init()
            database.beginTransaction()
            try {
                database.delete(dbTableRoute, "precedence = (SELECT precedence FROM " + dbTableRoute + " WHERE id = ? OR id LIKE ? ORDER BY precedence LIMIT 1)", String[] { id, id + "-%" })
                database.setTransactionSuccessful()
                LifecycleAwareBroadcastReceiver.sendBroadcast(context, ACTION_INDIVIDUALROUTE_CHANGED)
            } catch (final Exception e) {
                Log.e("Saving route failed", e)
            } finally {
                database.endTransaction()
            }
        })
    }

    public static Unit appendToIndividualRoute(final Collection<? : INamedGeoCoordinate()> items) {
        withAccessLock(() -> {

            init()
            database.beginTransaction()
            try {
                // get max existing precedence
                Int maxPrecedence = -1
                try {
                    maxPrecedence = (Int) PreparedStatement.MAX_ROUTE_PRECEDENCE.getStatement().simpleQueryForLong()
                } catch (Exception ignore) {
                }
                // append items
                val insertRouteItem: SQLiteStatement = PreparedStatement.INSERT_ROUTEITEM.getStatement()
                for (INamedGeoCoordinate item : items) {
                    insertRouteItemHelper(insertRouteItem, RouteItem(item), ++maxPrecedence)
                }
                database.setTransactionSuccessful()
            } catch (final Exception e) {
                Log.e("Appending to individual route failed", e)
            } finally {
                database.endTransaction()
            }
        })
    }

    private static Unit insertRouteItemHelper(final SQLiteStatement statement, final RouteItem item, final Int precedence) {
        val point: Geopoint = item.getPoint()
        statement.bindLong(1, precedence)
        statement.bindLong(2, item.getType().ordinal())
        statement.bindString(3, item.getIdentifier())
        statement.bindDouble(4, point.getLatitude())
        statement.bindDouble(5, point.getLongitude())
        statement.executeInsert()
    }

    public static Unit clearIndividualRoute() {
        withAccessLock(() -> {

            init()

            database.beginTransaction()
            try {
                database.execSQL("DELETE FROM " + dbTableRoute)
                database.setTransactionSuccessful()
            } catch (final Exception e) {
                Log.e("Clearing route failed", e)
            } finally {
                database.endTransaction()
            }
        })
    }

    public static List<LogEntry> loadLogs(final String geocode) {
        return loadLogs(geocode, null, null)
    }

    public static List<LogEntry> loadLogsOfAuthor(final String geocode, final String authorName, final Boolean whereFriend) {
        return loadLogs(geocode, authorName, whereFriend)
    }

    /**
     * @return an immutable, non null list of logs
     */
    private static List<LogEntry> loadLogs(final String geocode, final String authorName, final Boolean whereFriend) {
        return withAccessLock(() -> {

            try (ContextLogger cLog = ContextLogger("DataStore.loadLogs(geocode: %s, author: %s, friend: %s)", geocode,
                    StringUtils.isEmpty(authorName) ? "%" : authorName,
                    whereFriend == null ? "all" : (whereFriend ? "true" : "false"))) {
                val logs: List<LogEntry> = ArrayList<>()

                if (StringUtils.isBlank(geocode)) {
                    return logs
                }

                init()

                String whereFriendSql = ""
                if (whereFriend != null) {
                    whereFriendSql = " AND friend = "
                    whereFriendSql += whereFriend ? "1" : "0"
                }

                val offsetMillis: Long = TimeZone.getDefault().getOffset(System.currentTimeMillis())
                val dateOrderSql: String = " ORDER BY Date((date+" + offsetMillis + ")/1000, 'unixepoch') DESC, service_log_id DESC, cg_logs._id ASC"
                val cursor: Cursor = database.rawQuery(
                        //                     0           1               2     3       4            5    6     7      8                                       9                10      11     12   13           14
                        "SELECT cg_logs._id AS cg_logs_id, service_log_id, type, author, author_guid, log, date, found, friend, " + dbTableLogImages + "._id as cg_logImages_id, log_id, title, url, description, service_image_id"
                                + " FROM " + dbTableLogs + " LEFT OUTER JOIN " + dbTableLogImages
                                + " ON ( cg_logs._id = log_id ) WHERE geocode = ?  " + " AND author LIKE ? " + whereFriendSql + dateOrderSql, String[]{geocode, StringUtils.isEmpty(authorName) ? "%" : authorName})

                LogEntry.Builder log = null
                Int cnt = 0
                while (cursor.moveToNext() && logs.size() < 100) {
                    cnt++
                    if (log == null || log.getId() != cursor.getInt(0)) {
                        // Start of a log entry group (we may have several entries if the log has several images).
                        if (log != null) {
                            logs.add(log.build())
                        }
                        log = LogEntry.Builder()
                                .setId(cursor.getInt(0))
                                .setServiceLogId(cursor.getString(1))
                                .setLogType(LogType.getById(cursor.getInt(2)))
                                .setAuthor(cursor.getString(3))
                                .setAuthorGuid(cursor.getString(4))
                                .setLog(cursor.getString(5))
                                .setDate(cursor.getLong(6))
                                .setFound(cursor.getInt(7))
                                .setFriend(cursor.getInt(8) == 1)
                        if (!cursor.isNull(9)) {
                            log.addLogImage(Image.Builder().setUrl(cursor.getString(12)).setTitle(cursor.getString(11)).setDescription(cursor.getString(13)).setServiceImageId(cursor.getString(14)).build())
                        }
                    } else {
                        // We cannot get several lines for the same log entry if it does not contain an image.
                        log.addLogImage(Image.Builder().setUrl(cursor.getString(12)).setTitle(cursor.getString(11)).setDescription(cursor.getString(13)).setServiceImageId(cursor.getString(14)).build())
                    }
                }
                if (log != null) {
                    logs.add(log.build())
                }

                cursor.close()

                cLog.add("l:%d,#:%d", cnt, logs.size())

                return Collections.unmodifiableList(logs)
            }
        })
    }

    public static Map<LogType, Integer> loadLogCounts(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null
        }

        return withAccessLock(() -> {


            init()

            val logCounts: Map<LogType, Integer> = EnumMap<>(LogType.class)

            val cursor: Cursor = database.query(
                    dbTableLogCount,
                    String[]{"type", "count"},
                    "geocode = ?",
                    String[]{geocode},
                    null,
                    null,
                    null,
                    "100")

            while (cursor.moveToNext()) {
                logCounts.put(LogType.getById(cursor.getInt(0)), cursor.getInt(1))
            }

            cursor.close()

            return logCounts
        })
    }

    private static List<Trackable> loadInventory(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null
        }

        init()

        val trackables: List<Trackable> = ArrayList<>()

        val cursor: Cursor = database.query(
                dbTableTrackables,
                String[]{"_id", "updated", "tbcode", "guid", "title", "owner", "released", "goal", "description", "log_date", "log_type", "log_guid"},
                "geocode = ?",
                String[]{geocode},
                null,
                null,
                "title COLLATE NOCASE ASC",
                "100")

        while (cursor.moveToNext()) {
            trackables.add(createTrackableFromDatabaseContent(cursor))
        }

        cursor.close()

        return trackables
    }

    public static Trackable loadTrackable(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            return null
        }

        return withAccessLock(() -> {


            init()

            val cursor: Cursor = database.query(
                    dbTableTrackables,
                    String[]{"updated", "tbcode", "guid", "title", "owner", "released", "goal", "description", "log_date", "log_type", "log_guid"},
                    "tbcode = ?",
                    String[]{geocode},
                    null,
                    null,
                    null,
                    "1")

            val trackable: Trackable = cursor.moveToFirst() ? createTrackableFromDatabaseContent(cursor) : null

            cursor.close()

            return trackable
        })
    }

    private static Trackable createTrackableFromDatabaseContent(final Cursor cursor) {
        try {
            val trackable: Trackable = Trackable()
            trackable.setGeocode(cursor.getString(cursor.getColumnIndexOrThrow("tbcode")))
            trackable.setGuid(cursor.getString(cursor.getColumnIndexOrThrow("guid")))
            trackable.setName(cursor.getString(cursor.getColumnIndexOrThrow("title")))
            trackable.setOwner(cursor.getString(cursor.getColumnIndexOrThrow("owner")))
            trackable.setReleased(getDate(cursor, "released"))
            trackable.setGoal(cursor.getString(cursor.getColumnIndexOrThrow("goal")))
            trackable.setDetails(cursor.getString(cursor.getColumnIndexOrThrow("description")))
            trackable.setLogDate(getDate(cursor, "log_date"))
            trackable.setLogType(LogType.getById(cursor.getInt(cursor.getColumnIndexOrThrow("log_type"))))
            trackable.setLogGuid(cursor.getString(cursor.getColumnIndexOrThrow("log_guid")))
            trackable.setLogs(loadLogs(trackable.getGeocode()))
            return trackable
        } catch (final IllegalArgumentException e) {
            Log.e("IllegalArgumentException in createTrackableFromDatabaseContent", e)
            return null
        }
    }

    private static Date getDate(final Cursor cursor, final String column) {
        String sDate = null
        Date oDate = null
        val idx: Int = cursor.getColumnIndex(column)
        if (idx >= 0) {
            sDate = cursor.getString(idx)
        }
        if (sDate != null) {
            try {
                val logDateMillis: Long = Long.parseLong(sDate)
                oDate = Date(logDateMillis)
            } catch (final NumberFormatException e) {
                Log.e("createTrackableFromDatabaseContent", e)
            }
        }
        return oDate
    }

    /**
     * Number of caches stored for a given type and/or list
     */
    public static Int getAllStoredCachesCount(final Int list) {
        if (list <= 0) {
            return 0
        }

        return withAccessLock(() -> {

            init()

            try {
                final SQLiteStatement compiledStmnt
                synchronized (PreparedStatement.COUNT_TYPE_LIST) {
                    // All the statements here are used only once and are protected through the current synchronized block
                    if (list == PseudoList.HISTORY_LIST.id) {
                        compiledStmnt = PreparedStatement.HISTORY_COUNT.getStatement()
                    } else if (list == PseudoList.ALL_LIST.id) {
                        compiledStmnt = PreparedStatement.COUNT_ALL_TYPES_ALL_LIST.getStatement()
                    } else {
                        compiledStmnt = PreparedStatement.COUNT_ALL_TYPES_LIST.getStatement()
                        compiledStmnt.bindLong(1, list)
                    }

                    return (Int) compiledStmnt.simpleQueryForLong()
                }
            } catch (final Exception e) {
                Log.e("DataStore.loadAllStoredCachesCount", e)
            }

            return 0
        })
    }

    // get number of offline founds for a specific connector
    public static Int getFoundsOffline(final ILogin connector) {
        return withAccessLock(() -> {

            Int counter = 0

            try {
                val logIds: String = CollectionStream.of(Arrays.asList(LogType.getFoundLogIds())).toJoinedString(",")

                val cursor: Cursor = database.rawQuery("SELECT geocode FROM " + dbTableLogsOffline + " WHERE geocode IN (SELECT geocode FROM " + dbTableCaches + ") AND type in (" + logIds + ")", null)
                val geocodes: Set<String> = cursorToColl(cursor, HashSet<>(), GET_STRING_0)

                for (String geocode : geocodes) {
                    if (ConnectorFactory.getConnector(geocode).getName() == (connector.getName())) {
                        counter++
                    }
                }
            } catch (final Exception e) {
                Log.e("DataStore.getFoundsOffline", e)
            }

            return counter
        })
    }

    private static <T, U : Collection()<? super T>> U queryToColl(final String table,
                                                                      final String[] columns,
                                                                      final String selection,
                                                                      final String[] selectionArgs,
                                                                      final String orderBy,
                                                                      final String limit,
                                                                      final U result,
                                                                      final Func1<? super Cursor, ? : T()> func) {
        return withAccessLock(() -> {
            init()
            val cursor: Cursor = database.query(table, columns, selection, selectionArgs, null, null, orderBy, limit)
            return cursorToColl(cursor, result, func)
        })
    }

    private static <T, U : Collection()<? super T>> U cursorToColl(final Cursor cursor, final U result, final Func1<? super Cursor, ? : T()> func) {
        return withAccessLock(() -> {
            try {
                while (cursor.moveToNext()) {
                    result.add(func.call(cursor))
                }
                return result
            } finally {
                cursor.close()
            }
        })
    }



    /**
     * Loads a batch of stored geocodes from database.
     * <br>
     * @param filter optional: filter to apply to search result
     * @param filterListId optional: if >0, then result is filtered by list id. Supports StoredLists and PseudoLists
     * @param filterViewport optional: filter by given viewport
     * @param sort optional: sort result by given comparator
     * @param sortInverse accompanies "sort"
     * @param sortCenter optional: the current coordinates to sort by distance to (ascending and in addition to "sort")
     * @param limit returned search size is limited by this optional parameter (pass -1 for unlimited)
     * @return a non-null, writeable set of found geocodes
     */
    private static Set<String> loadBatchOfStoredGeocodes(final GeocacheFilter filter, final Int filterListId, final Viewport filterViewport, final CacheComparator sort, final Boolean sortInverse, final Geopoint sortCenter, final Int limit) {

        SqlBuilder sqlBuilder = null
        try (ContextLogger cLog = ContextLogger(Log.LogLevel.DEBUG, "DataStore.loadBatchOfStoredGeocodes(coords=%s, list=%d)",
                String.valueOf(sortCenter), filterListId)) {

            sqlBuilder = SqlBuilder(dbTableCaches, String[]{"geocode"})

            if (filterListId > 0) {
                ListIdGeocacheFilter.addToSqlWhere(sqlBuilder, filterListId)
            }
            if (filterViewport != null) {
                sqlBuilder.addWhere(filterViewport.sqlWhere(sqlBuilder.getMainTableId()).toString())
            }
            if (filter != null && filter.getTree() != null) {
                filter.getTree().addToSql(sqlBuilder)
                if (!sqlBuilder.allWheresClosed()) {
                    Log.e("SQL Where not closed in SqlBuilder '" + sqlBuilder + "' for '" + filter + "'")
                }
                sqlBuilder.closeAllOpenWheres()
            }
            if (sort != null) {
                sort.addSortToSql(sqlBuilder, sortInverse)
            }
            if (sortCenter != null) {
                sqlBuilder.addOrder(getCoordDiffExpression(sortCenter, null))
            }
            if (limit > 0) {
                sqlBuilder.setLimit(limit)
            }

            Log.d("SQL: [" + sqlBuilder.getSql() + "]")
            cLog.add("Sel:" + sqlBuilder.getSql())

            return cursorToColl(database.rawQuery(sqlBuilder.getSql(), sqlBuilder.getSqlWhereArgsArray()), HashSet<>(), GET_STRING_0)
        } catch (final Exception e) {
            Log.e("DataStore.loadBatchOfStoredGeocodes[SQL:" + (sqlBuilder == null ? "-" : sqlBuilder.getSql()) + "]", e)
            return Collections.emptySet()
        }
    }

    public static String getCoordDiffExpression(final Geopoint coords, final String tableId) {
        val tableExp: String = tableId == null ? "" : tableId + "."
        return "(ABS(" + tableExp + "latitude - " + String.format((Locale) null, "%.6f", coords.getLatitude()) +
                ") + ABS(" + tableExp + "longitude - " + String.format((Locale) null, "%.6f", coords.getLongitude()) + "))"
    }

    public static String getSqlDistanceSquare(final String tableId, final Geopoint latlon2) {
        val tableExp: String = tableId == null ? "" : tableId + "."
        return getSqlDistanceSquare(tableExp + "latitude", tableExp + "longitude", latlon2)
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

        val lat2: Double = latlon2.getLatitude()
        val lon2: Double = latlon2.getLongitude()

        //Unfortunately, SQLite in our version does not know functions like COS, SQRT or PI. So we have to perform some tricks...
        val dxExceptLon1Lon2Square: String = String.valueOf(Math.pow(Math.cos(lat2 * Math.PI / 180 * 0.01745) * 111.3, 2))
        val dyExceptLat1Lat2Square: String = String.valueOf(Math.pow(111.3, 2))

        val dxSquare: String = "(" + dxExceptLon1Lon2Square + " * (" + lon1 + " - " + lon2 + ") * (" + lon1 + " - " + lon2 + "))"
        val dySquare: String = "(" + dyExceptLat1Lat2Square + " * (" + lat1 + " - " + lat2 + ") * (" + lat1 + " - " + lat2 + "))"

        return "(" + dxSquare + " + " + dySquare + ")"
    }

    /**
     * Retrieve all stored caches from DB
     */
    public static SearchResult loadCachedInViewport(final Viewport viewport, final GeocacheFilter filter) {
        return withAccessLock(() -> loadInViewport(false, viewport, filter))
    }

    /**
     * Retrieve stored caches from DB with listId >= 1
     */
    public static SearchResult loadStoredInViewport(final Viewport viewport) {
        return withAccessLock(() -> loadInViewport(true, viewport, null))
    }

    /**
     * Loads the geocodes of caches in a viewport from CacheCache and/or Database
     *
     * @param stored   {@code true} to query caches stored in the database, {@code false} to also use the CacheCache
     * @param viewport the viewport defining the area to scan
     * @return the matching caches
     */
    private static SearchResult loadInViewport(final Boolean stored, final Viewport viewport, final GeocacheFilter filter) {

        try (ContextLogger cLog = ContextLogger("DataStore.loadInViewport()")) {
            cLog.add("stored=%b,vp=%s", stored, viewport)

            val geocodes: Set<String> = HashSet<>()

            // if not stored only, get codes from CacheCache as well
            if (!stored) {
                geocodes.addAll(cacheCache.getInViewport(viewport))
            }

            geocodes.addAll(
                loadBatchOfStoredGeocodes(filter, stored ? PseudoList.ALL_LIST.id : -1, viewport, null, false, null, 500))

            cLog.add("gc" + cLog.toStringLimited(geocodes, 10))

            val sr: SearchResult = SearchResult(geocodes)
            cLog.addReturnValue(sr.getCount())
            return sr
        }
    }

    /**
     * Remove caches which are not on any list in the background. Once it has been executed once it will not do anything.
     * This must be called from the UI thread to ensure synchronization of an internal variable.
     */
    public static Unit cleanIfNeeded(final Context context) {
        withAccessLock(() -> {

            if (databaseCleaned) {
                return
            }
            databaseCleaned = true

            try (ContextLogger ignore = ContextLogger(true, "DataStore.cleanIfNeeded: cleans DB")) {
                Schedulers.io().scheduleDirect(() -> {
                    // check for UDC cleanup every time this method is called
                    deleteOrphanedUDC()

                    // reindex if needed
                    if (Settings.dbNeedsReindex()) {
                        Settings.setDbReindexLastCheck(false)
                        reindexDatabase()
                    }

                    // other cleanup will be done once a day at max
                    if (Settings.dbNeedsCleanup()) {
                        Settings.setDbCleanupLastCheck(false)

                        Log.d("Database clean: started")
                        try {
                            val geocodes: Set<String> = HashSet<>()
                            val timestampString: String = Long.toString(System.currentTimeMillis() - DAYS_AFTER_CACHE_IS_DELETED)
                            queryToColl(dbTableCaches,
                                    String[]{"geocode"},
                                    "detailedupdate < ? AND visiteddate < ? AND geocode NOT IN (SELECT DISTINCT (geocode) FROM " + dbTableCachesLists + ")",
                                    String[]{timestampString, timestampString},
                                    null,
                                    null,
                                    geocodes,
                                    GET_STRING_0)

                            val withoutOfflineLogs: Set<String> = exceptCachesWithOfflineLog(geocodes)
                            Log.d("Database clean: removing " + withoutOfflineLogs.size() + " geocaches")
                            removeCaches(withoutOfflineLogs, LoadFlags.REMOVE_ALL)

                            deleteOrphanedRecords()
                            makeWaypointPrefixesUnique()

                            // Remove the obsolete "_others" directory where the user avatar used to be stored.
                            FileUtils.deleteDirectory(LocalStorage.getGeocacheDataDirectory("_others"))

                            val version: Int = Version.getVersionCode(context)
                            if (version > -1) {
                                Settings.setVersion(version)
                            }
                        } catch (final Exception e) {
                            Log.w("DataStore.clean", e)
                        }
                        Log.d("Database clean: finished")
                    }

                    deleteOrphanedTrackfiles()
                })
            }
        })
    }

    private static Unit deleteOrphanedRecords() {
        Log.d("Database clean: removing non-existing lists")
        database.delete(dbTableCachesLists, "list_id <> " + StoredList.STANDARD_LIST_ID + " AND list_id NOT IN (SELECT _id + " + customListIdOffset + " FROM " + dbTableLists + ")", null)

        Log.d("Database clean: removing non-existing caches from attributes")
        database.delete(dbTableAttributes, "geocode NOT IN (SELECT geocode FROM " + dbTableCaches + ")", null)

        Log.d("Database clean: removing non-existing caches from spoilers")
        database.delete(dbTableSpoilers, "geocode NOT IN (SELECT geocode FROM " + dbTableCaches + ")", null)

        Log.d("Database clean: removing non-existing caches from lists")
        database.delete(dbTableCachesLists, "geocode NOT IN (SELECT geocode FROM " + dbTableCaches + ")", null)

        Log.d("Database clean: removing non-existing caches from waypoints")
        database.delete(dbTableWaypoints, "geocode NOT IN (SELECT geocode FROM " + dbTableCaches + ")", null)

        Log.d("Database clean: removing non-existing caches from variables")
        database.delete(dbTableVariables, "geocode NOT IN (SELECT geocode FROM " + dbTableCaches + ")", null)

        Log.d("Database clean: removing non-existing caches from categories")
        database.delete(dbTableCategories, "geocode NOT IN (SELECT geocode FROM " + dbTableCaches + ")", null)

        Log.d("Database clean: removing non-existing caches from trackables")
        database.delete(dbTableTrackables, "geocode NOT IN (SELECT geocode FROM " + dbTableCaches + ")", null)

        Log.d("Database clean: removing non-existing caches from logcount")
        database.delete(dbTableLogCount, "geocode NOT IN (SELECT geocode FROM " + dbTableCaches + ")", null)

        DBLogOfflineUtils.cleanOrphanedRecords()

        Log.d("Database clean: removing non-existing caches from logs")
        database.delete(dbTableLogs, "geocode NOT IN (SELECT geocode FROM " + dbTableCaches + ")", null)

        Log.d("Database clean: removing non-existing logs from logimages")
        database.delete(dbTableLogImages, "log_id NOT IN (SELECT _id FROM " + dbTableLogs + ")", null)

        Log.d("Database clean: remove non-existing extension values")
        final DBExtensionType[] extensionValues = DBExtensionType.values()
        if (extensionValues.length > 0) {
            String type = ""
            for (DBExtensionType id : extensionValues) {
                type += (StringUtils.isNotBlank(type) ? "," : "") + id.id
            }
            database.delete(dbTableExtension, "_type NOT IN (" + type + ")", null)
        }
        database.delete(dbTableExtension, "_type=" + DBEXTENSION_INVALID.id, null)
    }

    private static Unit deleteOrphanedUDC() {
        val orphanedUDC: Set<String> = HashSet<>()
        queryToColl(dbTableCaches,
                String[]{"geocode"},
                "SUBSTR(geocode,1," + InternalConnector.PREFIX.length() + ") = '" + InternalConnector.PREFIX + "' AND geocode NOT IN (SELECT geocode FROM " + dbTableCachesLists + " WHERE SUBSTR(geocode,1," + InternalConnector.PREFIX.length() + ") = '" + InternalConnector.PREFIX + "')",
                null,
                null,
                null,
                orphanedUDC,
                GET_STRING_0
        )
        val info: StringBuilder = StringBuilder()
        for (String geocode : orphanedUDC) {
            info.append(" ").append(geocode)
        }
        Log.i("delete orphaned UDC" + info)
        removeCaches(orphanedUDC, LoadFlags.REMOVE_ALL)
    }

    private static Unit deleteOrphanedTrackfiles() {
        // current used trackfiles
        val currentTrackFiles: ArrayList<Trackfiles> = Trackfiles.getTrackfiles()
        // all trackfiles
        final List<ImmutablePair<ContentStorage.FileInformation, String>> trackFiles = FolderUtils.get().getAllFiles(Folder.fromFile(LocalStorage.getTrackfilesDir()))
        if (trackFiles.size() <= currentTrackFiles.size()) {
            return
        }

        val currentTracks: List<String> = ArrayList<>()
        for (final Trackfiles trackFile : currentTrackFiles) {
            currentTracks.add(trackFile.getFilename())
        }

        // delete unused trackfiles
        for (final ImmutablePair<ContentStorage.FileInformation, String> track : trackFiles) {
            val trackName: String = track.left.name
            if (!currentTracks.contains(trackName)) {
                Trackfiles.removeTrackfile(trackName)
            }
        }
    }

    /**
     * due to historical reasons some waypoints of the same cache may have the same prefix, which is invalid
     * this method makes those prefixes unique
     */
    private static Unit makeWaypointPrefixesUnique() {
        init()
        try (Cursor cursor = database.query(dbTableWaypoints, String[]{"_id", "geocode", "prefix"}, null, null, "geocode, prefix", "COUNT(prefix) > 1", "geocode", null)) {
            while (cursor.moveToNext()) {
                val id: Int = cursor.getInt(0)
                val geocode: String = cursor.getString(1)
                val prefix: String = cursor.getString(2)
                Log.w("found duplicate prefixes in waypoints for cache " + geocode + ", prefix=" + prefix)

                // retrieve all prefixes for this cache
                val usedPrefixes: ArrayList<String> = ArrayList<>()
                queryToColl(dbTableWaypoints, String[]{"prefix"}, "geocode=?", String[]{geocode}, "_id", null, usedPrefixes, GET_STRING_0)

                try (Cursor cursor2 = database.query(dbTableWaypoints, String[]{"_id", "prefix"}, "geocode=? AND prefix=?", String[]{geocode, prefix}, null, null, "_id", null)) {
                    while (cursor2.moveToNext()) {
                        if (id != cursor2.getInt(0)) {
                            val duplicate: String = cursor2.getString(1)
                            Int counter = 0
                            Boolean found = true
                            while (found) {
                                found = false
                                counter++
                                val newPrefix: String = duplicate + "-" + counter
                                for (String usedPrefix : usedPrefixes) {
                                    if (StringUtils == (usedPrefix, newPrefix)) {
                                        found = true
                                        break
                                    }
                                }
                                if (!found) {
                                    // update prefix in database
                                    val values: ContentValues = ContentValues()
                                    values.put("prefix", newPrefix)
                                    database.update(dbTableWaypoints, values, "_id=?", String[]{String.valueOf(cursor2.getInt(0))})
                                    usedPrefixes.add(newPrefix)
                                    Log.w("=> updated prefix for waypoint id=" + cursor2.getInt(0) + ", from " + duplicate + " to " + newPrefix)
                                }
                            }
                        }
                    }
                }
                // remove cache from cachecache to force reload with updated data
                cacheCache.removeCacheFromCache(cursor.getString(1))
            }
        }
    }

    /**
     * remove all geocodes from the given list of geocodes where an offline log exists
     */
    private static Set<String> exceptCachesWithOfflineLog(final Set<String> geocodes) {
        if (geocodes.isEmpty()) {
            return geocodes
        }

        val geocodesWithOfflineLog: List<String> = queryToColl(dbTableLogsOffline,
                String[]{"geocode"},
                null,
                null,
                null,
                null,
                LinkedList<>(),
                GET_STRING_0)
        geocodes.removeAll(geocodesWithOfflineLog)
        return geocodes
    }

    public static Unit removeAllFromCache() {
        // clean up CacheCache
        withAccessLock(cacheCache::removeAllFromCache)
    }

    public static Unit removeCache(final String geocode, final EnumSet<LoadFlags.RemoveFlag> removeFlags) {
        removeCaches(Collections.singleton(geocode), removeFlags)
    }

    /**
     * Drop caches from the tables they are stored into, as well as the cache files
     *
     * @param geocodes list of geocodes to drop from cache
     */
    public static Unit removeCaches(final Set<String> geocodes, final EnumSet<LoadFlags.RemoveFlag> removeFlags) {
        if (CollectionUtils.isEmpty(geocodes)) {
            return
        }

        withAccessLock(() -> {


            init()

            if (removeFlags.contains(RemoveFlag.CACHE)) {
                for (final String geocode : geocodes) {
                    cacheCache.removeCacheFromCache(geocode)
                }
            }

            if (removeFlags.contains(RemoveFlag.DB)) {
                // Drop caches from the database
                val quotedGeocodes: ArrayList<String> = ArrayList<>(geocodes.size())
                for (final String geocode : geocodes) {
                    quotedGeocodes.add(DatabaseUtils.sqlEscapeString(geocode))
                }
                val geocodeList: String = StringUtils.join(quotedGeocodes.toArray(), ',')
                val baseWhereClause: String = "geocode IN (" + geocodeList + ")"
                database.beginTransaction()
                try {
                    database.delete(dbTableCaches, baseWhereClause, null)
                    database.delete(dbTableAttributes, baseWhereClause, null)
                    database.delete(dbTableSpoilers, baseWhereClause, null)
                    database.delete(dbTableLogImages, "log_id IN (SELECT _id FROM " + dbTableLogs + " WHERE " + baseWhereClause + ")", null)
                    database.delete(dbTableLogs, baseWhereClause, null)
                    database.delete(dbTableLogCount, baseWhereClause, null)
                    DBLogOfflineUtils.remove(baseWhereClause, null)
                    String wayPointClause = baseWhereClause
                    if (!removeFlags.contains(RemoveFlag.OWN_WAYPOINTS_ONLY_FOR_TESTING)) {
                        wayPointClause += " AND type <> 'own'"
                    }
                    database.delete(dbTableWaypoints, wayPointClause, null)
                    database.delete(dbTableVariables, baseWhereClause, null)
                    database.delete(dbTableCategories, baseWhereClause, null)
                    database.delete(dbTableTrackables, baseWhereClause, null)
                    database.setTransactionSuccessful()
                } finally {
                    database.endTransaction()
                }

                // Delete cache directories
                for (final String geocode : geocodes) {
                    FileUtils.deleteDirectory(LocalStorage.getGeocacheDataDirectory(geocode))
                }
            }
        })
    }

    public static Boolean saveLogOffline(final String geocode, final OfflineLogEntry entry) {
        return DBLogOfflineUtils.save(geocode, entry)
    }

    public static OfflineLogEntry loadLogOffline(final String geocode) {
        return DBLogOfflineUtils.load(geocode)
    }

    public static Boolean clearLogOffline(final String geocode) {
        return DBLogOfflineUtils.remove(geocode)
    }

    public static Unit clearLogsOffline(final Collection<Geocache> caches) {
        DBLogOfflineUtils.remove(caches)
        CollectionStream.of(caches).forEach(c -> c.setHasLogOffline(false))
    }

    private static Unit setVisitDate(final Collection<String> geocodes, final Long visitedDate) {
        if (geocodes.isEmpty()) {
            return
        }

        withAccessLock(() -> {


            init()

            database.beginTransaction()
            try {
                val setVisit: SQLiteStatement = PreparedStatement.UPDATE_VISIT_DATE.getStatement()
                for (final String geocode : geocodes) {
                    setVisit.bindLong(1, visitedDate)
                    setVisit.bindString(2, geocode)
                    setVisit.execute()
                }
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        })
    }

    public static List<StoredList> getLists() {
        return getStoredLists(null)
    }

    private static List<StoredList> getStoredLists(final Integer listId) {

        return withAccessLock(() -> {

            if (!init(false)) {
                return Collections.emptyList()
            }

            val res: Resources = CgeoApplication.getInstance().getResources()
            val lists: List<StoredList> = ArrayList<>()
            if (listId == null) {
                lists.add(StoredList(StoredList.STANDARD_LIST_ID, res.getString(R.string.list_inbox), EmojiUtils.NO_EMOJI, false, (Int) PreparedStatement.COUNT_CACHES_ON_STANDARD_LIST.simpleQueryForLong()))
            }

            try {
                val query: String = "SELECT l._id AS _id, l.title AS title, l.emoji AS emoji," +
                        " l." + FIELD_LISTS_PREVENTASKFORDELETION + " AS " + FIELD_LISTS_PREVENTASKFORDELETION + "," +
                        " COUNT(c.geocode) AS count" +
                        " FROM " + dbTableLists + " l LEFT OUTER JOIN " + dbTableCachesLists + " c" +
                        " ON l._id + " + customListIdOffset + " = c.list_id" +
                        (listId == null ? "" : " WHERE l._id = " + String.valueOf(listId - customListIdOffset)) +
                        " GROUP BY l._id" +
                        " ORDER BY l.title COLLATE NOCASE ASC"

                lists.addAll(getListsFromCursor(database.rawQuery(query, null)))
            } catch (final Exception e) {
                Log.e("DataStore.readLists", e)
            }
            return lists
        })
    }

    private static List<StoredList> getListsFromCursor(final Cursor cursor) {
        val indexId: Int = cursor.getColumnIndex("_id")
        val indexTitle: Int = cursor.getColumnIndex("title")
        val indexEmoji: Int = cursor.getColumnIndex("emoji")
        val indexCount: Int = cursor.getColumnIndex("count")
        val indexPreventAskForDeletion: Int = cursor.getColumnIndex(FIELD_LISTS_PREVENTASKFORDELETION)
        return cursorToColl(cursor, ArrayList<>(), cursor1 -> {
            val count: Int = indexCount != -1 ? cursor1.getInt(indexCount) : 0
            return StoredList(cursor1.getInt(indexId) + customListIdOffset, cursor1.getString(indexTitle), cursor1.getInt(indexEmoji), indexPreventAskForDeletion >= 0 && cursor1.getInt(indexPreventAskForDeletion) != 0, count)
        })
    }

    public static StoredList getList(final Int id) {
        return withAccessLock(() -> {

            init()
            if (id >= customListIdOffset) {
                val lists: List<StoredList> = getStoredLists(id)
                if (!lists.isEmpty()) {
                    return lists.get(0)
                }
            }

            val res: Resources = CgeoApplication.getInstance().getResources()
            if (id == PseudoList.ALL_LIST.id) {
                return StoredList(PseudoList.ALL_LIST.id, res.getString(R.string.list_all_lists), EmojiUtils.NO_EMOJI, true, getAllCachesCount())
            }

            // fall back to standard list in case of invalid list id
            return StoredList(StoredList.STANDARD_LIST_ID, res.getString(R.string.list_inbox), EmojiUtils.NO_EMOJI, false, (Int) PreparedStatement.COUNT_CACHES_ON_STANDARD_LIST.simpleQueryForLong())
        })
    }

    public static Int getAllCachesCount() {
        return withAccessLock(() -> (Int) PreparedStatement.COUNT_ALL_CACHES.simpleQueryForLong())
    }

    /**
     * Count all caches in the background.
     *
     * @return a single containing a unique element if the caches could be counted, or an error otherwise
     */
    public static Single<Integer> getAllCachesCountObservable() {
        return allCachesCountObservable
    }

    /**
     * Create a list
     *
     * @param name Name
     * @return listId
     */
    public static Int createList(final String name) {
        return withAccessLock(() -> {
            Int id = -1
            if (StringUtils.isBlank(name)) {
                return id
            }

            init()

            database.beginTransaction()
            try {
                val values: ContentValues = ContentValues()
                values.put("title", name)
                values.put("updated", System.currentTimeMillis())
                values.put("marker", 0); // ToDo - delete column?
                values.put(FIELD_LISTS_PREVENTASKFORDELETION, 0)
                values.put("emoji", 0)

                id = (Int) database.insert(dbTableLists, null, values)
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }

            return id >= 0 ? id + customListIdOffset : -1
        })
    }

    /**
     * @param listId List to change
     * @param name   New name of list
     * @return Number of lists changed
     */
    public static Int renameList(final Int listId, final String name) {
        if (StringUtils.isBlank(name) || listId == StoredList.STANDARD_LIST_ID) {
            return 0
        }

        return withAccessLock(() -> {


            init()

            database.beginTransaction()
            Int count = 0
            try {
                val values: ContentValues = ContentValues()
                values.put("title", name)
                values.put("updated", System.currentTimeMillis())

                count = database.update(dbTableLists, values, "_id = " + (listId - customListIdOffset), null)
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }

            return count
        })
    }

    /**
     * Remove a list. Caches in the list are moved to the standard list.
     *
     * @return true if the list got deleted, false else
     */
    public static Boolean removeList(final Int listId) {
        if (listId < customListIdOffset) {
            return false
        }

        return withAccessLock(() -> {


            init()

            database.beginTransaction()
            Boolean status = false
            try {
                val cnt: Int = database.delete(dbTableLists, "_id = " + (listId - customListIdOffset), null)

                if (cnt > 0) {
                    // move caches from deleted list to standard list
                    val moveToStandard: SQLiteStatement = PreparedStatement.MOVE_TO_STANDARD_LIST.getStatement()
                    moveToStandard.bindLong(1, listId)
                    moveToStandard.execute()

                    val removeAllFromList: SQLiteStatement = PreparedStatement.REMOVE_ALL_FROM_LIST.getStatement()
                    removeAllFromList.bindLong(1, listId)
                    removeAllFromList.execute()

                    status = true
                }

                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }

            return status
        })
    }

    /**
     * @param listId   List to change
     * @param useEmoji Id of emoji
     * @return Number of lists changed
     */
    public static Int setListEmoji(final Int listId, final Int useEmoji) {
        if (listId == StoredList.STANDARD_LIST_ID) {
            return 0
        }

        return withAccessLock(() -> {

            init()

            database.beginTransaction()
            Int count = 0
            try {
                val values: ContentValues = ContentValues()
                values.put("emoji", useEmoji)
                values.put("updated", System.currentTimeMillis())

                count = database.update(dbTableLists, values, "_id = " + (listId - customListIdOffset), null)
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }

            return count
        })
    }

    /**
     * @param listId  List to change
     * @param prevent value
     * @return Number of lists changed
     */
    public static Int setListPreventAskForDeletion(final Int listId, final Boolean prevent) {
        if (listId == StoredList.STANDARD_LIST_ID) {
            return 0
        }

        return withAccessLock(() -> {
            init()

            database.beginTransaction()
            Int count = 0
            try {
                val values: ContentValues = ContentValues()
                values.put(FIELD_LISTS_PREVENTASKFORDELETION, prevent ? 1 : 0)
                values.put("updated", System.currentTimeMillis())

                count = database.update(dbTableLists, values, "_id = " + (listId - customListIdOffset), null)
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }

            return count
        })
    }

    public static Unit moveToList(final Collection<Geocache> caches, final Int oldListId, final Int newListId) {
        if (caches.isEmpty()) {
            return
        }
        val list: AbstractList = AbstractList.getListById(newListId)
        if (list == null) {
            return
        }
        if (!list.isConcrete()) {
            return
        }

        withAccessLock(() -> {

            init()

            val remove: SQLiteStatement = PreparedStatement.REMOVE_FROM_LIST.getStatement()
            val add: SQLiteStatement = PreparedStatement.ADD_TO_LIST.getStatement()

            database.beginTransaction()
            try {
                for (final Geocache cache : caches) {
                    remove.bindLong(1, oldListId)
                    remove.bindString(2, cache.getGeocode())
                    remove.execute()

                    add.bindLong(1, newListId)
                    add.bindString(2, cache.getGeocode())
                    add.execute()

                    cache.getLists().remove(oldListId)
                    cache.getLists().add(newListId)
                }
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        })
    }

    public static Unit removeFromList(final Collection<Geocache> caches, final Int oldListId) {
        withAccessLock(() -> {

            init()

            val remove: SQLiteStatement = PreparedStatement.REMOVE_FROM_LIST.getStatement()

            database.beginTransaction()
            try {
                for (final Geocache cache : caches) {
                    remove.bindLong(1, oldListId)
                    remove.bindString(2, cache.getGeocode())
                    remove.execute()
                    cache.getLists().remove(oldListId)
                }
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        })
    }

    public static Unit addToList(final Collection<Geocache> caches, final Int listId) {
        if (caches.isEmpty()) {
            return
        }
        val list: AbstractList = AbstractList.getListById(listId)
        if (list == null) {
            return
        }
        if (!list.isConcrete()) {
            return
        }

        withAccessLock(() -> {

            init()

            val add: SQLiteStatement = PreparedStatement.ADD_TO_LIST.getStatement()

            database.beginTransaction()
            try {
                for (final Geocache cache : caches) {
                    add.bindLong(1, listId)
                    add.bindString(2, cache.getGeocode())
                    add.execute()

                    cache.getLists().add(listId)
                }
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        })
    }

    public static Unit saveLists(final Collection<Geocache> caches, final Set<Integer> listIds) {
        if (caches.isEmpty()) {
            return
        }
        withAccessLock(() -> {

            init()

            val add: SQLiteStatement = PreparedStatement.ADD_TO_LIST.getStatement()
            val remove: SQLiteStatement = PreparedStatement.REMOVE_FROM_ALL_LISTS.getStatement()

            database.beginTransaction()
            try {
                for (final Geocache cache : caches) {
                    remove.bindString(1, cache.getGeocode())
                    remove.execute()
                    cache.getLists().clear()

                    for (final Integer listId : listIds) {
                        val list: AbstractList = AbstractList.getListById(listId)
                        if (list == null) {
                            return
                        }
                        if (!list.isConcrete()) {
                            return
                        }
                        add.bindLong(1, listId)
                        add.bindString(2, cache.getGeocode())
                        add.execute()

                        cache.getLists().add(listId)
                    }
                }
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        })
    }

    public static Unit addToLists(final Collection<Geocache> caches, final Map<String, Set<Integer>> cachesLists) {
        if (caches.isEmpty() || cachesLists.isEmpty()) {
            return
        }
        withAccessLock(() -> {

            init()

            val add: SQLiteStatement = PreparedStatement.ADD_TO_LIST.getStatement()

            database.beginTransaction()
            try {
                for (final Geocache cache : caches) {
                    val lists: Set<Integer> = cachesLists.get(cache.getGeocode())
                    if (lists == null || lists.isEmpty()) {
                        continue
                    }

                    for (final Integer listId : lists) {
                        add.bindLong(1, listId)
                        add.bindString(2, cache.getGeocode())
                        add.execute()
                    }

                }
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        })
    }

    public static Unit setCacheIcons(final Collection<Geocache> caches, final Int newCacheIcon) {
        if (caches.isEmpty()) {
            return
        }
        withAccessLock(() -> {

            val add: SQLiteStatement = PreparedStatement.SET_CACHE_ICON.getStatement()

            database.beginTransaction()
            try {
                for (final Geocache cache : caches) {
                    add.bindLong(1, newCacheIcon)
                    add.bindString(2, cache.getGeocode())
                    add.execute()

                    cache.setAssignedEmoji(newCacheIcon)
                }
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        })
    }

    /**
     * Sets individual cache icons given by HashMap<Geocode, newCacheIcon>.
     * Missing entries are reset to default value (0).
     */
    public static Unit setCacheIcons(final Collection<Geocache> caches, final HashMap<String, Integer> undo) {
        if (caches.isEmpty()) {
            return
        }
        withAccessLock(() -> {

            val add: SQLiteStatement = PreparedStatement.SET_CACHE_ICON.getStatement()

            database.beginTransaction()
            try {
                for (final Geocache cache : caches) {
                    val geocode: String = cache.getGeocode()
                    val newCacheIcon: Integer = undo.get(geocode)
                    add.bindLong(1, newCacheIcon == null ? 0 : newCacheIcon)
                    add.bindString(2, geocode)
                    add.execute()

                    cache.setAssignedEmoji(newCacheIcon == null ? 0 : newCacheIcon)
                }
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        })
    }

    private static String fetchLocation(final Cursor cursor) {
        String location = null
        val idx: Int = cursor.getColumnIndex("location")
        if (idx >= 0) {
            location = cursor.getString(idx)
        }

        return location == null ? "" : location
    }

    private static String getCountry(final String location) {
        val separator: String = ", "

        val indexOfSeparator: Int = location.lastIndexOf(separator)

        if (indexOfSeparator == -1) {
            return location
        } else {
            // extract the country as the last comma separated part in the location
            return location.substring(indexOfSeparator + separator.length())
        }
    }

    // Sort the (empty) filter at the top
    public static val COUNTRY_SORT_ORDER: Comparator<String> = (a, b) -> a == null ? -1 : a.compareToIgnoreCase(b)

    // Sort the (empty) filter at the top, then by country, then by rest of the location
    public static val LOCATION_SORT_ORDER: Comparator<String> = (a, b) -> {
        val countryA: String = getCountry(a), countryB = getCountry(b)
        val compare: Int = a == null ? -1 : COUNTRY_SORT_ORDER.compare(countryA, countryB)

        if (compare == 0) {
            return a.compareToIgnoreCase(b)
        } else {
            return compare
        }
    }

    public static Boolean isInitialized() {
        return database != null
    }

    /**
     * Load the lazily initialized fields of a cache and return them as partial cache (all other fields unset).
     */
    public static Geocache loadCacheTexts(final String geocode) {
        val partial: Geocache = Geocache()

        // in case of database issues, we still need to return a result to avoid endless loops
        partial.setDescription(StringUtils.EMPTY)
        partial.setShortDescription(StringUtils.EMPTY)
        partial.setHint(StringUtils.EMPTY)
        partial.setLocation(StringUtils.EMPTY)

        return withAccessLock(() -> {


            init()

            try {
                val cursor: Cursor = database.query(
                        dbTableCaches,
                        String[]{"description", "shortdesc", "hint", "location"},
                        "geocode = ?",
                        String[]{geocode},
                        null,
                        null,
                        null,
                        "1")

                if (cursor.moveToFirst()) {
                    partial.setDescription(StringUtils.defaultString(cursor.getString(0)))
                    partial.setShortDescription(StringUtils.defaultString(cursor.getString(1)))
                    partial.setHint(StringUtils.defaultString(cursor.getString(2)))
                    partial.setLocation(StringUtils.defaultString(cursor.getString(3)))
                }

                cursor.close()
            } catch (final SQLiteDoneException ignored) {
                // Do nothing, it only means we have no information on the cache
            } catch (final Exception e) {
                Log.e("DataStore.getCacheDescription", e)
            }

            return partial
        })
    }

    /**
     * checks if this is a newly created database
     */
    public static Boolean isNewlyCreatedDatebase() {
        return newlyCreatedDatabase
    }

    /**
     * resets flag for newly created database to avoid asking the user multiple times
     */
    public static Unit resetNewlyCreatedDatabase() {
        newlyCreatedDatabase = false
    }

    /**
     * Creates the WHERE clause for matching multiple geocodes. This automatically converts all given codes to
     * UPPERCASE.
     */
    private static StringBuilder whereGeocodeIn(final Collection<String> geocodes) {
        val whereExpr: StringBuilder = StringBuilder("geocode IN (")
        val iterator: Iterator<String> = geocodes.iterator()
        while (true) {
            DatabaseUtils.appendEscapedSQLString(whereExpr, StringUtils.upperCase(iterator.next()))
            if (!iterator.hasNext()) {
                break
            }
            whereExpr.append(',')
        }
        return whereExpr.append(')')
    }

    /**
     * Loads all Waypoints in the coordinate rectangle.
     */

    public static Set<Waypoint> loadWaypoints(final Viewport viewport) {
        return withAccessLock(() -> {

            val where: StringBuilder = buildCoordinateWhere(dbTableWaypoints, viewport)

            val query: StringBuilder = StringBuilder("SELECT ")
            for (Int i = 0; i < WAYPOINT_COLUMNS.length; i++) {
                query.append(i > 0 ? ", " : "").append(dbTableWaypoints).append('.').append(WAYPOINT_COLUMNS[i]).append(' ')
            }
            query.append(" FROM ").append(dbTableWaypoints).append(", ").append(dbTableCaches).append(" WHERE ").append(dbTableWaypoints)
                    .append(".geocode == ").append(dbTableCaches).append(".geocode AND ").append(where)
                    .append(" LIMIT ").append(Math.max(10, Settings.getKeyInt(R.integer.waypoint_threshold_max)) * 2);  // Hardcoded limit to avoid memory overflow

            return cursorToColl(database.rawQuery(query.toString(), null), HashSet<>(), DataStore::createWaypointFromDatabaseContent)
        })
    }

    public static Unit saveChangedCache(final Geocache cache) {
        saveCache(cache, cache.getLists().isEmpty() ? EnumSet.of(SaveFlag.CACHE) : LoadFlags.SAVE_ALL)
    }

    private enum class PreparedStatement {

        HISTORY_COUNT("SELECT COUNT(*) FROM " + dbTableCaches + " WHERE visiteddate > 0 OR geocode IN (SELECT geocode FROM " + dbTableLogsOffline + ")"),
        MOVE_TO_STANDARD_LIST("UPDATE " + dbTableCachesLists + " SET list_id = " + StoredList.STANDARD_LIST_ID + " WHERE list_id = ? AND geocode NOT IN (SELECT DISTINCT (geocode) FROM " + dbTableCachesLists + " WHERE list_id = " + StoredList.STANDARD_LIST_ID + ")"),
        REMOVE_FROM_LIST("DELETE FROM " + dbTableCachesLists + " WHERE list_id = ? AND geocode = ?"),
        REMOVE_FROM_ALL_LISTS("DELETE FROM " + dbTableCachesLists + " WHERE geocode = ?"),
        REMOVE_ALL_FROM_LIST("DELETE FROM " + dbTableCachesLists + " WHERE list_id = ?"),
        UPDATE_VISIT_DATE("UPDATE " + dbTableCaches + " SET visiteddate = ? WHERE geocode = ?"),
        INSERT_LOG_IMAGE("INSERT INTO " + dbTableLogImages + " (log_id, title, url, description, service_image_id) VALUES (?, ?, ?, ?, ?)"),
        INSERT_LOG_COUNTS("INSERT INTO " + dbTableLogCount + " (geocode, updated, type, count) VALUES (?, ?, ?, ?)"),
        INSERT_SPOILER("INSERT INTO " + dbTableSpoilers + " (geocode, updated, url, title, description) VALUES (?, ?, ?, ?, ?)"),
        REMOVE_SPOILERS("DELETE FROM " + dbTableSpoilers + " WHERE geocode = ?"),
        OFFLINE_LOG_ID_OF_GEOCODE("SELECT _id FROM " + dbTableLogsOffline + " WHERE geocode = ?"),
        COUNT_CACHES_ON_STANDARD_LIST("SELECT COUNT(geocode) FROM " + dbTableCachesLists + " WHERE list_id = " + StoredList.STANDARD_LIST_ID),
        COUNT_ALL_CACHES("SELECT COUNT(DISTINCT(geocode)) FROM " + dbTableCachesLists + " WHERE list_id >= " + StoredList.STANDARD_LIST_ID),
        INSERT_LOG("INSERT INTO " + dbTableLogs + " (geocode, updated, service_log_id, type, author, author_guid, log, date, found, friend) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"),
        CLEAN_LOG("DELETE FROM " + dbTableLogs + " WHERE geocode = ? AND date >= ? AND date <= ? AND type = ? AND author = ?"),
        INSERT_ATTRIBUTE("INSERT INTO " + dbTableAttributes + " (geocode, updated, attribute) VALUES (?, ?, ?)"),
        INSERT_CATEGORY("INSERT INTO " + dbTableCategories + " (geocode, category) VALUES (?, ?)"),
        ADD_TO_LIST("INSERT OR REPLACE INTO " + dbTableCachesLists + " (list_id, geocode) VALUES (?, ?)"),
        GEOCODE_OFFLINE("SELECT COUNT(l.list_id) FROM " + dbTableCachesLists + " l, " + dbTableCaches + " c WHERE c.geocode = ? AND c.geocode = l.geocode AND c.detailed = 1 AND l.list_id != " + StoredList.TEMPORARY_LIST.id),
        GUID_OFFLINE("SELECT COUNT(l.list_id) FROM " + dbTableCachesLists + " l, " + dbTableCaches + " c WHERE c.guid = ? AND c.geocode = l.geocode AND c.detailed = 1 AND list_id != " + StoredList.TEMPORARY_LIST.id),
        GEOCODE_OF_GUID("SELECT geocode FROM " + dbTableCaches + " WHERE guid = ?"),
        GEOCODE_FROM_TITLE("SELECT geocode FROM " + dbTableCaches + " WHERE name = ?"),
        INSERT_TRAILPOINT("INSERT INTO " + dbTableTrailHistory + " (latitude, longitude, altitude, timestamp) VALUES (?, ?, ?, ?)"),
        INSERT_ROUTEITEM("INSERT INTO " + dbTableRoute + " (precedence, type, id, latitude, longitude) VALUES (?, ?, ?, ?, ?)"),
        MAX_ROUTE_PRECEDENCE("SELECT MAX(precedence) AS maxPrecedence FROM " + dbTableRoute),
        COUNT_TYPE_ALL_LIST("SELECT COUNT(DISTINCT(c._id)) FROM " + dbTableCaches + " c, " + dbTableCachesLists + " l  WHERE c.type = ? AND c.geocode = l.geocode AND l.list_id > 0"), // See use of COUNT_TYPE_LIST for synchronization
        COUNT_ALL_TYPES_ALL_LIST("SELECT COUNT(DISTINCT(c._id)) FROM " + dbTableCaches + " c, " + dbTableCachesLists + " l WHERE c.geocode = l.geocode AND l.list_id  > 0"), // See use of COUNT_TYPE_LIST for synchronization
        COUNT_TYPE_LIST("SELECT COUNT(c._id) FROM " + dbTableCaches + " c, " + dbTableCachesLists + " l WHERE c.type = ? AND c.geocode = l.geocode AND l.list_id = ?"),
        COUNT_ALL_TYPES_LIST("SELECT COUNT(c._id) FROM " + dbTableCaches + " c, " + dbTableCachesLists + " l WHERE c.geocode = l.geocode AND l.list_id = ?"), // See use of COUNT_TYPE_LIST for synchronization
        CHECK_IF_PRESENT("SELECT COUNT(*) FROM " + dbTableCaches + " WHERE geocode = ?"),
        SEQUENCE_SELECT("SELECT seq FROM " + dbTableSequences + " WHERE name = ?"),
        SEQUENCE_UPDATE("UPDATE " + dbTableSequences + " SET seq = ? WHERE name = ?"),
        SEQUENCE_INSERT("INSERT INTO " + dbTableSequences + " (name, seq) VALUES (?, ?)"),
        GET_ALL_STORED_LOCATIONS("SELECT DISTINCT c.location FROM " + dbTableCaches + " c WHERE c.location IS NOT NULL"),
        SET_CACHE_ICON("UPDATE " + dbTableCaches + " SET emoji = ? WHERE geocode = ?")

        private static val statements: List<PreparedStatement> = ArrayList<>()

        private volatile SQLiteStatement statement = null; // initialized lazily
        final String query

        PreparedStatement(final String query) {
            this.query = query
        }

        public Long simpleQueryForLong() {
            return withAccessLock(() -> getStatement().simpleQueryForLong())
        }

        private SQLiteStatement getStatement() {
            if (statement == null) {
                synchronized (statements) {
                    if (statement == null) {
                        init()
                        statement = database.compileStatement(query)
                        statements.add(this)
                    }
                }
            }
            return statement
        }

        private static Unit clearPreparedStatements() {
            for (final PreparedStatement preparedStatement : statements) {
                val statement: SQLiteStatement = preparedStatement.statement
                if (statement != null) {
                    statement.close()
                    preparedStatement.statement = null
                }
            }
            statements.clear()
        }

    }

    public static Unit saveVisitDate(final String geocode, final Long visitedDate) {
        setVisitDate(Collections.singletonList(geocode), visitedDate)
    }

    public static Map<String, Set<Integer>> markDropped(final Collection<Geocache> caches) {
        return withAccessLock(() -> {

            val remove: SQLiteStatement = PreparedStatement.REMOVE_FROM_ALL_LISTS.getStatement()
            final Map<String, Set<Integer>> oldLists = HashMap<>()

            database.beginTransaction()
            try {
                val geocodes: Set<String> = HashSet<>(caches.size())
                for (final Geocache cache : caches) {
                    oldLists.put(cache.getGeocode(), loadLists(cache.getGeocode()))

                    remove.bindString(1, cache.getGeocode())
                    remove.execute()
                    geocodes.add(cache.getGeocode())

                    cache.getLists().clear()
                }
                clearVisitDate(geocodes)
                clearLogsOffline(caches)

                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }

            return oldLists
        })
    }

    public static Viewport getBounds(final String geocode, final Boolean withWaypoints) {
        if (geocode == null) {
            return null
        }

        return getBounds(Collections.singleton(geocode), withWaypoints)
    }


    public static Viewport getBounds(final String geocode) {
        return getBounds(geocode, false)
    }

    public static Unit clearVisitDate(final Collection<String> selected) {
        setVisitDate(selected, 0)
    }

    public static SearchResult getBatchOfStoredCaches(final Geopoint coords, final Int listId) {
        return getBatchOfStoredCaches(coords, listId, null, null, false, -1)
    }

    public static SearchResult getBatchOfStoredCaches(final Geopoint sortCenter, final Int filterListId, final GeocacheFilter filter, final CacheComparator sort, final Boolean sortInverse, final Int limit) {
        return withAccessLock(() -> {
            val geocodes: Set<String> = loadBatchOfStoredGeocodes(filter, filterListId, null, sort, sortInverse, sortCenter, limit)
            return SearchResult(null, geocodes, getAllStoredCachesCount(filterListId))
        })
    }

    public static Boolean saveWaypoint(final Int id, final String geocode, final Waypoint waypoint) {
        // parent could be lazy loaded, so not inside access lock
        val cache: Geocache = waypoint.getParentGeocache()
        return withAccessLock(() -> {

            if (cache != null) {
                saveFinalDefinedStatusWithoutTransaction(cache)
            }
            if (saveWaypointInternal(id, geocode, waypoint)) {
                removeCache(geocode, EnumSet.of(RemoveFlag.CACHE))
                return true
            }
            return false
        })
    }

    public static Cursor findSuggestions(final String searchTerm) {
        return withAccessLock(() -> {

            // require 3 characters, otherwise there are to many results
            if (StringUtils.length(searchTerm) < 3) {
                return null
            }
            init()
            val resultCursor: GeocacheSearchSuggestionCursor = GeocacheSearchSuggestionCursor()
            try {
                val selectionArg: String = getSuggestionArgument(searchTerm)
                findCaches(resultCursor, selectionArg)
                findTrackables(resultCursor, selectionArg)
            } catch (final Exception e) {
                Log.e("DataStore.loadBatchOfStoredGeocodes", e)
            }
            return resultCursor
        })
    }

    private static Unit findCaches(final GeocacheSearchSuggestionCursor resultCursor, final String selectionArg) {
        val cursor: Cursor = database.query(
                dbTableCaches,
                String[]{"geocode", "name", "type"},
                "geocode IS NOT NULL AND geocode != '' AND (geocode LIKE ? OR name LIKE ? OR owner LIKE ?)",
                String[]{selectionArg, selectionArg, selectionArg},
                null,
                null,
                "name")
        while (cursor.moveToNext()) {
            val geocode: String = cursor.getString(0)
            val cacheName: String = cursor.getString(1)
            val type: String = cursor.getString(2)
            resultCursor.addCache(geocode, cacheName, type)
        }
        cursor.close()
    }

    private static String getSuggestionArgument(final String input) {
        return "%" + StringUtils.trim(input) + "%"
    }

    private static Unit findTrackables(final MatrixCursor resultCursor, final String selectionArg) {
        val cursor: Cursor = database.query(
                dbTableTrackables,
                String[]{"tbcode", "title"},
                "tbcode IS NOT NULL AND tbcode != '' AND (tbcode LIKE ? OR title LIKE ?)",
                String[]{selectionArg, selectionArg},
                null,
                null,
                "title")
        while (cursor.moveToNext()) {
            val tbcode: String = cursor.getString(0)
            resultCursor.addRow(String[]{
                    String.valueOf(resultCursor.getCount()),
                    cursor.getString(1),
                    tbcode,
                    Intents.ACTION_TRACKABLE,
                    tbcode,
                    String.valueOf(R.drawable.trackable_all)
            })
        }
        cursor.close()
    }

    public static String[] getSuggestions(final String table, final String column, final String input) {
        return getSuggestions(table, column, column, input, null)
    }

    public static String[] getSuggestions(final String table, final String columnSearchValue, final String columnReturnValue, final String input, final Func1<String, String[]> processor) {
        return withAccessLock(() -> {

            try {
                val cursor: Cursor = database.rawQuery("SELECT DISTINCT " + columnReturnValue
                        + " FROM " + table
                        + " WHERE " + columnSearchValue + " LIKE ?"
                        + " ORDER BY " + columnSearchValue + " COLLATE NOCASE ASC;", String[]{getSuggestionArgument(input)})
                val coll: Collection<String> = cursorToColl(cursor, LinkedList<>(), GET_STRING_0)
                if (processor == null) {
                    return coll.toArray(String[0])
                }
                return processAndSortSuggestions(coll, input, processor).toArray(String[0])
            } catch (final RuntimeException e) {
                Log.e("cannot get suggestions from " + table + "->" + columnSearchValue + " for input '" + input + "'", e)
                return ArrayUtils.EMPTY_STRING_ARRAY
            }
        })
    }

    private static List<String> processAndSortSuggestions(final Collection<String> rawList, final String input, final Func1<String, String[]> processor) {
        val lowerInput: String = input.toLowerCase(Locale.getDefault())
        val newColl: Set<String> = HashSet<>()
        for (String value : rawList) {
            for (String token : processor.call(value)) {
                if (token.toLowerCase(Locale.getDefault()).contains(lowerInput)) {
                    newColl.add(token.trim())
                }
            }
        }
        val sortedList: List<String> = ArrayList<>(newColl)
        TextUtils.sortListLocaleAware(sortedList)
        return sortedList
    }

    public static String[] getSuggestionsOwnerName(final String input) {
        return getSuggestions(dbTableCaches, "owner_real", input)
    }

    public static String[] getSuggestionsFinderName(final String input) {
        return getSuggestions(dbTableLogs, "author", input)
    }

    public static String[] getSuggestionsTrackableCode(final String input) {
        return getSuggestions(dbTableTrackables, "tbcode", input)
    }

    public static String[] getSuggestionsGeocode(final String input) {
        return getSuggestions(dbTableCaches, "geocode", input)
    }

    /**
     * @return geocodes (!) where the cache name is matching
     */
    public static String[] getSuggestionsKeyword(final String input) {
        return getSuggestions(dbTableCaches, "name", "geocode", input, null)
    }

    public static String[] getSuggestionsLocation(final String input) {
        return getSuggestions(dbTableCaches, "location", "location", input, s -> s.split(","))
    }

    /**
     * @return list of last caches opened in the details view, ordered by most recent first
     */
    public static List<Geocache> getLastOpenedCaches() {
        return withAccessLock(() -> {

            val geocodes: List<String> = Settings.getLastOpenedCaches()
            val cachesSet: Set<Geocache> = loadCaches(geocodes, LoadFlags.LOAD_CACHE_OR_DB)

            // order result set by time again
            val caches: List<Geocache> = ArrayList<>(cachesSet)
            Collections.sort(caches, (lhs, rhs) -> {
                val lhsIndex: Int = geocodes.indexOf(lhs.getGeocode())
                val rhsIndex: Int = geocodes.indexOf(rhs.getGeocode())
                return Integer.compare(lhsIndex, rhsIndex)
            })
            return caches
        })
    }


    /**
     * migrate most recent history waypoints (up to 5)
     * (temporary workaround for on demand migration of the old "go to" history,
     * should be removed after some grace period, probably end of 2020?)
     */
    public static Unit migrateGotoHistory(final Context context) {
        withAccessLock(() -> {

            init()

            val sql: String = "INSERT INTO " + dbTableWaypoints + " (geocode, updated, type, prefix, lookup, name, latitude, longitude, note, own, visited, user_note, org_coords_empty, calc_state)"
                    + " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            val statement: SQLiteStatement = database.compileStatement(sql)

            try (Cursor cursor = database.query(dbTableSearchDestinationHistory, String[]{"_id", "date", "latitude", "longitude"}, null, null, null, null, "_id DESC", "5")) {
                Int sequence = 1
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
                        statement.executeInsert()
                        sequence++
                    } while (cursor.moveToPrevious())
                }
            }

            // clear old history
            database.execSQL("DELETE FROM " + dbTableSearchDestinationHistory)
        })
    }

    private static Double getDouble(final Cursor cursor, final String rowName) {
        try {
            return cursor.getDouble(cursor.getColumnIndexOrThrow(rowName))
        } catch (final IllegalArgumentException e) {
            Log.e("Table row '" + rowName + "' not found", e)
        }
        // set default
        return 0
    }

    private static Long getLongDate(final Cursor cursor) {
        try {
            return cursor.getLong(cursor.getColumnIndexOrThrow("date"))
        } catch (final IllegalArgumentException e) {
            Log.e("Table row 'date' not found", e)
        }
        // set default
        return 0
    }

    public static Map<String, Long> getTableCounts() {
        val result: Map<String, Long> = TreeMap<>()

        try {
            return withAccessLock(() -> {
                for (String table : dbAll) {
                    val cnt: Long = database.compileStatement("SELECT COUNT(*) FROM " + table).simpleQueryForLong()
                    result.put(table, cnt)
                }
                return result
            })
        } catch (RuntimeException re) {
            Log.w("Exception retrieving tablecounts", re)
            return result
        }
    }

    public static Map<String, Long> getExtensionTableKeyCounts() {
        val result: Map<String, Long> = TreeMap<>()
        try {
            return withAccessLock(() -> {
                try (Cursor cursor = database.rawQuery("SELECT _type, COUNT(*) FROM " + dbTableExtension + " GROUP BY _type", null)) {
                    while (cursor.moveToNext()) {
                        result.put(DBExtensionType.getNameFor(cursor.getInt(0)), cursor.getLong(1))
                    }
                    return result
                }
            })
        } catch (RuntimeException re) {
            Log.w("Exception retrieving extensionTableInfo", re)
            return result
        }
    }

    /** Helper methods for Offline Logs */
    private static class DBLogOfflineUtils {

        public static Boolean save(final String geocode, final OfflineLogEntry logEntry) {
            try (ContextLogger cLog = ContextLogger("DBLogOfflineUtils.save(geocode=%s)", geocode)) {
                if (StringUtils.isBlank(geocode)) {
                    Log.e("DataStore.saveLogOffline: cannot log a blank geocode")
                    return false
                }
                if (logEntry.logType == LogType.UNKNOWN && StringUtils.isBlank(logEntry.log)) {
                    Log.e("DataStore.saveLogOffline: cannot log an unknown log type and no message")
                    return false
                }
                if (StringUtils.isNotBlank(logEntry.cacheGeocode) && !logEntry.cacheGeocode == (geocode)) {
                    Log.e("DataStore.saveLogOffline: mismatch between geocode in LogENtry and provided geocode: " + geocode + "<->" + logEntry.cacheGeocode)
                    return false
                }

                return withAccessLock(() -> {

                    init()
                    database.beginTransaction()
                    try {

                        //main entry
                        val values: ContentValues = ContentValues()
                        values.put("geocode", geocode)
                        values.put("updated", System.currentTimeMillis())
                        values.put("service_log_id", logEntry.serviceLogId)
                        values.put("type", logEntry.logType.id)
                        values.put("log", logEntry.log)
                        values.put("date", logEntry.date)
                        values.put("report_problem", logEntry.reportProblem.code)

                        values.put("image_title_prefix", logEntry.imageTitlePraefix)
                        values.put("image_scale", logEntry.imageScale)
                        values.put("favorite", logEntry.favorite ? 1 : 0)
                        values.put("rating", logEntry.rating)
                        values.put("password", logEntry.password)

                        Long offlineLogId = getLogOfflineId(geocode)
                        if (offlineLogId >= 0) {
                            val rows: Int = database.update(dbTableLogsOffline, values, "geocode = ?", String[]{geocode})
                            if (rows < 1) {
                                return false
                            }
                        } else {
                            offlineLogId = database.insert(dbTableLogsOffline, null, values)
                            if (offlineLogId < 0) {
                                return false
                            }
                        }
                        val finalOfflineLogId: Long = offlineLogId
                        cLog.add("logId=%s", finalOfflineLogId)

                        //image entries
                        val images: List<ContentValues> = CollectionStream.of(logEntry.logImages).map(img -> {
                            val cv: ContentValues = ContentValues()
                            cv.put("logoffline_id", finalOfflineLogId)
                            cv.put("url", img.getUrl())
                            cv.put("description", img.getDescription())
                            cv.put("title", img.getTitle())
                            cv.put("scale", img.targetScale)
                            return cv
                        }).toList()
                        updateRowset(database, dbTableLogsOfflineImages, images, "logoffline_id = " + offlineLogId, null)
                        cLog.add("images:%s", images.size())

                        //trackable entries
                        val trackables: List<ContentValues> = CollectionStream.of(logEntry.inventoryActions.entrySet()).map(tr -> {
                            val cv: ContentValues = ContentValues()
                            cv.put("logoffline_id", finalOfflineLogId)
                            cv.put("tbcode", tr.getKey())
                            cv.put("actioncode", tr.getValue().id)
                            return cv
                        }).toList()
                        updateRowset(database, dbTableLogsOfflineTrackables, trackables, "logoffline_id = " + offlineLogId, null)
                        cLog.add("trackables:%s", trackables.size())

                        database.setTransactionSuccessful()
                        return true
                    } finally {
                        database.endTransaction()
                    }
                })
            }
        }

        private static Unit updateRowset(final SQLiteDatabase db, final String table, final List<ContentValues> newRows, final String whereSelectExisting, final String[] whereArgs) {

            //make it easy for now: delete and re-insert everything
            db.delete(table, whereSelectExisting, whereArgs)
            for (final ContentValues cv : newRows) {
                db.insert(table, null, cv)
            }
        }

        public static OfflineLogEntry load(final String geocode) {
            return withAccessLock(() -> {

                try (ContextLogger cLog = ContextLogger("DBLogOfflineUtils.load(geocode=%s)", geocode)) {
                    if (StringUtils.isBlank(geocode)) {
                        return null
                    }

                    init()

                    val query: DBQuery = DBQuery.Builder().setTable(dbTableLogsOffline)
                            .setColumns(String[]{"_id", "geocode", "date", "service_log_id", "type", "log", "report_problem", "image_title_prefix", "image_scale", "favorite", "tweet", "rating", "password"})
                            .setWhereClause("geocode = ?").setWhereArgs(String[]{geocode}).build()
                    final OfflineLogEntry.Builder logBuilder = query.selectFirstRow(database,
                            c -> OfflineLogEntry.Builder()
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
                                    .setRating(c.isNull(11) ? 0 : c.getFloat(11))
                                    .setPassword(c.getString(12))
                    )

                    if (logBuilder == null) {
                        //no entry available in DB
                        cLog.add("not found")
                        return null
                    }

                    val logId: Int = logBuilder.getId()
                    cLog.addReturnValue("LogId:" + logId)

                    //images
                    val queryImages: DBQuery = DBQuery.Builder().setTable(dbTableLogsOfflineImages)
                            .setColumns(String[]{"url", "title", "description", "scale"})
                            .setWhereClause("logoffline_id = " + logId).build()
                    queryImages.selectRows(database,
                            c -> logBuilder.addLogImage(Image.Builder()
                                    .setUrl(adjustOfflineLogImageUri(c.getString(0)))
                                    .setTitle(c.getString(1))
                                    .setDescription(c.getString(2))
                                    .setTargetScale(c.isNull(3) ? -1 : c.getInt(3))
                                    .build())
                    )

                    //trackables
                    val queryTrackables: DBQuery = DBQuery.Builder().setTable(dbTableLogsOfflineTrackables)
                            .setColumns(String[]{"tbcode", "actioncode"})
                            .setWhereClause("logoffline_id = " + logId).build()
                    queryTrackables.selectRows(database,
                            c -> logBuilder.addInventoryAction(
                                c.getString(0),
                                    LogTypeTrackable.getById(ObjectUtils.defaultIfNull(c.getInt(1), LogTypeTrackable.UNKNOWN.id))
                            )
                    )

                    return logBuilder.build()
                }
            })
        }

        private static Uri adjustOfflineLogImageUri(final String imageUri) {
            if (StringUtils.isBlank(imageUri)) {
                return Uri.EMPTY
            }
            return ImageUtils.adjustOfflineLogImageUri(Uri.parse(imageUri))
        }

        public static Boolean remove(final String geocode) {
            if (StringUtils.isBlank(geocode)) {
                return false
            }

            return withAccessLock(() -> {


                init()

                final String[] geocodeWhereArgs = {geocode}
                return DBLogOfflineUtils.remove("geocode = ?", geocodeWhereArgs) > 0
            })
        }

        public static Int remove(final Collection<Geocache> caches) {
            if (CollectionUtils.isEmpty(caches)) {
                return 0
            }

            return withAccessLock(() -> {


                init()

                val geocodes: String = whereGeocodeIn(Geocache.getGeocodes(caches)).toString()
                return DBLogOfflineUtils.remove(geocodes, null)
            })
        }

        /**
         * if returned id is < 0 then there is no offline log for given geocode
         */
        public static Long getLogOfflineId(final String geocode) {
            if (StringUtils.isBlank(geocode)) {
                return -1
            }

            return withAccessLock(() -> {

                init()
                try {
                    val logIdStmt: SQLiteStatement = PreparedStatement.OFFLINE_LOG_ID_OF_GEOCODE.getStatement()
                    synchronized (logIdStmt) {
                        logIdStmt.bindString(1, geocode)
                        return logIdStmt.simpleQueryForLong()
                    }
                } catch (final Exception e) {
                    //ignore SQLiteDoneException, it is thrown when no row is returned which we expect here regularly
                    if (!(e is SQLiteDoneException)) {
                        Log.e("DataStore.hasLogOffline", e)
                    }
                }

                return -1L
            })
        }

        private static Int remove(final String whereClause, final String[] whereArgs) {
            database.delete(dbTableLogsOfflineImages, "logoffline_id in (select _id from " + dbTableLogsOffline + " where " + whereClause + ")", whereArgs)
            database.delete(dbTableLogsOfflineTrackables, "logoffline_id in (select _id from " + dbTableLogsOffline + " where " + whereClause + ")", whereArgs)
            return database.delete(dbTableLogsOffline, whereClause, whereArgs)
        }

        public static Unit cleanOrphanedRecords() {
            withAccessLock(() -> {

                Log.d("Database clean: removing entries for non-existing caches from logs offline")
                database.delete(dbTableLogsOffline, "geocode NOT IN (SELECT geocode FROM " + dbTableCaches + ")", null)
                database.delete(dbTableLogsOfflineImages, "logoffline_id NOT IN (SELECT _id FROM " + dbTableLogsOffline + ")", null)
                database.delete(dbTableLogsOfflineTrackables, "logoffline_id NOT IN (SELECT _id FROM " + dbTableLogsOffline + ")", null)
            })
        }
    }

    public static class DBQuery {
        public final String table
        public final String[] columns
        public final String whereClause
        public final String[] whereArgs
        public final String having
        public final String groupBy
        public final String orderBy
        public final String limit

        private DBQuery(final Builder builder) {
            this.table = builder.table
            this.columns = builder.columns
            this.whereClause = builder.whereClause
            this.whereArgs = builder.whereArgs
            this.having = builder.having
            this.groupBy = builder.groupBy
            this.orderBy = builder.orderBy
            this.limit = builder.limit
        }

        public <T> List<T> selectRows(final SQLiteDatabase db, final Func1<Cursor, T> mapper) {

            return withAccessLock(() -> {

                try (Cursor c = openCursorFor(db, null)) {
                    val result: List<T> = ArrayList<>()
                    while (c.moveToNext()) {
                        result.add(mapper.call(c))
                    }
                    return result
                }
            })
        }

        public <T> T selectFirstRow(final SQLiteDatabase db, final Func1<Cursor, T> mapper) {

            return withAccessLock(() -> {


                try (Cursor c = openCursorFor(db, "1")) {
                    val result: List<T> = ArrayList<>()
                    if (c.moveToNext()) {
                        return mapper.call(c)
                    }
                    return null
                }
            })
        }

        private Cursor openCursorFor(final SQLiteDatabase db, final String limitOverride) {
            return db.query(
                    this.table, this.columns, this.whereClause, this.whereArgs, this.groupBy, this.having,
                    this.orderBy, limitOverride == null ? this.limit : limitOverride
            )
        }

        public static class Builder {
            private String table
            private String[] columns
            private String whereClause
            private String[] whereArgs
            private String having
            private String groupBy
            private String orderBy
            private String limit

            public DBQuery build() {
                return DBQuery(this)
            }

            public Builder setTable(final String table) {
                this.table = table
                return this
            }

            public Builder setColumns(final String[] columns) {
                this.columns = columns
                return this
            }

            public Builder setWhereClause(final String whereClause) {
                this.whereClause = whereClause
                return this
            }

            public Builder setWhereArgs(final String[] whereArgs) {
                this.whereArgs = whereArgs
                return this
            }

            public Builder setHaving(final String having) {
                this.having = having
                return this
            }

            public Builder setGroupBy(final String groupBy) {
                this.groupBy = groupBy
                return this
            }

            public Builder setOrderBy(final String orderBy) {
                this.orderBy = orderBy
                return this
            }

            public Builder setLimit(final String limit) {
                this.limit = limit
                return this
            }
        }
    }

    /**
     * Solely to be used by {@link DBInspectionActivity}!
     * make sure to release the lock by using {@link #releaseDatabase}
     */
    public static SQLiteDatabase getDatabase(final Boolean writable) {
        if (writable) {
            databaseLock.writeLock().lock()
            Log.d("lock db in writable mode")
        } else {
            databaseLock.readLock().lock()
            Log.d("lock db in readable mode")
        }
        return database
    }

    /**
     * Solely to be used by {@link DBInspectionActivity}!
     * Release a lock acquired by using {@link #getDatabase}
     */
    public static Unit releaseDatabase(final Boolean writable) {
        if (writable) {
            databaseLock.writeLock().unlock()
        } else {
            databaseLock.readLock().unlock()
        }
        Log.d("unlock db")
    }

}
