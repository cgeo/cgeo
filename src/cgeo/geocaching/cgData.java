package cgeo.geocaching;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Environment;
import android.util.Log;

public class cgData {

	/**The list of fields needed for mapping.*/
	private static final String[] CACHE_COLUMNS = new String[]{
		"_id", "updated", "reason", "detailed", "detailedupdate", "visiteddate", "geocode", "cacheid", "guid", "type", "name", "own", "owner", "owner_real", "hidden", "hint", "size",
		"difficulty", "distance", "direction", "terrain", "latlon", "latitude_string", "longitude_string", "location", "latitude", "longitude", "elevation", "shortdesc",
		"description", "favourite_cnt", "rating", "votes", "myvote", "disabled", "archived", "members", "found", "favourite", "inventorycoins", "inventorytags",
		"inventoryunknown", "onWatchlist", "personal_note", "reliable_latlon"
	};
	public cgCacheWrap caches;
	private Context context = null;
	private String path = null;
	private cgDbHelper dbHelper = null;
	// Used when Profiling and loggin SELECTS
//	private SQLiteDatabasePerformanceLoggingWrapper databaseRO = null;
//	private SQLiteDatabasePerformanceLoggingWrapper databaseRW = null;
	private SQLiteDatabase databaseRW;
	private SQLiteDatabase databaseRO;

	private static final int dbVersion = 55;
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
			+ "latitude_string text, "
			+ "longitude_string text, "
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
			+ "latitude_string text, "
			+ "longitude_string text, "
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

	public cgData(Context contextIn) {
		context = contextIn;
	}

	public synchronized void init() {
		if (databaseRW == null || databaseRW.isOpen() == false) {
			try {
				if (dbHelper == null) {
					dbHelper = new cgDbHelper(context);
				}
//				databaseRW = new SQLiteDatabasePerformanceLoggingWrapper(dbHelper.getWritableDatabase());
				databaseRW = dbHelper.getWritableDatabase();

				if (databaseRW != null && databaseRW.isOpen()) {
					Log.i(cgSettings.tag, "Connection to RW database established.");
				} else {
					Log.e(cgSettings.tag, "Failed to open connection to RW database.");
				}

				if (databaseRW != null && databaseRW.inTransaction()) {
					databaseRW.endTransaction();
				}
			} catch (Exception e) {
				Log.e(cgSettings.tag, "cgData.openDb.RW: " + e.toString());
			}
		}

		if (databaseRO == null || !databaseRO.isOpen()) {
			try {
				if (dbHelper == null) {
					dbHelper = new cgDbHelper(context);
				}
//				databaseRO = new SQLiteDatabasePerformanceLoggingWrapper(dbHelper.getReadableDatabase());
				databaseRO = dbHelper.getReadableDatabase();

				if (databaseRO.needUpgrade(dbVersion)) {
					databaseRO = null;
				}

				if (databaseRO != null && databaseRO.isOpen()) {
					Log.i(cgSettings.tag, "Connection to RO database established.");
				} else {
					Log.e(cgSettings.tag, "Failed to open connection to RO database.");
				}

				if (databaseRO != null && databaseRO.inTransaction()) {
					databaseRO.endTransaction();
				}
			} catch (Exception e) {
				Log.e(cgSettings.tag, "cgData.openDb.RO: " + e.toString());
			}
		}

		initialized = true;
	}

	public void closeDb() {
		if (databaseRO != null) {
			path = databaseRO.getPath();

			if (databaseRO.inTransaction()) {
				databaseRO.endTransaction();
			}

			databaseRO.close();
			databaseRO = null;
			SQLiteDatabase.releaseMemory();

			Log.d(cgSettings.tag, "Closing RO database");
		}

		if (databaseRW != null) {
			path = databaseRW.getPath();

			if (databaseRW.inTransaction()) {
				databaseRW.endTransaction();
			}

			databaseRW.close();
			databaseRW = null;
			SQLiteDatabase.releaseMemory();

			Log.d(cgSettings.tag, "Closing RW database");
		}

		if (dbHelper != null) {
			dbHelper.close();
			dbHelper = null;
		}
	}

	public String backupDatabase() {
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) == false) {
			Log.w(cgSettings.tag, "Database wasn't backed up: no external memory");

			return null;
		}

		closeDb();

		boolean backupDone = false;
		final String directoryImg = cgSettings.cache;
		final String directoryTarget = Environment.getExternalStorageDirectory() + "/" + directoryImg + "/";
		final String fileTarget = directoryTarget + "cgeo.sqlite";
		final String fileSource = path;

		File directoryTargetFile = new File(directoryTarget);
		if (directoryTargetFile.exists() == false) {
			directoryTargetFile.mkdir();
		}

		InputStream  input  = null;
		OutputStream output = null;
		try {
			input  = new FileInputStream(fileSource);
			output = new FileOutputStream(fileTarget);
		} catch (FileNotFoundException e) {
			Log.e(cgSettings.tag, "Database wasn't backed up, could not open file: " + e.toString());
		}

		byte[] buffer = new byte[1024];
		int length;
		if ((input != null) && (output != null)) {
			try {
				while ((length = input.read(buffer)) > 0) {
					output.write(buffer, 0, length);
				}
				output.flush();
				backupDone = true;
			} catch (IOException e) {
				Log.e(cgSettings.tag, "Database wasn't backed up, could not read/write file: " + e.toString());
			}
		}

		try {
			if (output != null)
				output.close();
			if (input != null)
				input.close();
		} catch (IOException e) {
			Log.e(cgSettings.tag, "Database wasn't backed up, could not close file: " + e.toString());
		}

		if (backupDone)
			Log.i(cgSettings.tag, "Database was copied to " + fileTarget);

		init();

		return backupDone ? fileTarget : null;
	}

	public static File isRestoreFile() {
		final String directoryImg = cgSettings.cache;
		final String fileSource = Environment.getExternalStorageDirectory() + "/" + directoryImg + "/cgeo.sqlite";

		File fileSourceFile = new File(fileSource);
		if (fileSourceFile.exists()) {
			return fileSourceFile;
		} else {
			return null;
		}
	}

	public boolean restoreDatabase() {
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) == false) {
			Log.w(cgSettings.tag, "Database wasn't restored: no external memory");

			return false;
		}

		closeDb();

		boolean restoreDone = false;

		final String directoryImg = cgSettings.cache;
		final String fileSource = Environment.getExternalStorageDirectory() + "/" + directoryImg + "/cgeo.sqlite";
		final String fileTarget = path;

		File fileSourceFile = new File(fileSource);
		if (fileSourceFile.exists() == false) {
			Log.w(cgSettings.tag, "Database backup was not found");

			init();

			return restoreDone;
		}

		InputStream  input  = null;
		OutputStream output = null;
		try {
			input  = new FileInputStream(fileSource);
			output = new FileOutputStream(fileTarget);
		} catch (FileNotFoundException e) {
			Log.e(cgSettings.tag, "Database wasn't restored, could not open file: " + e.toString());
		}

		byte[] buffer = new byte[1024];
		int length;
		if ((input != null) && (output != null)) {
			try {
				while ((length = input.read(buffer)) > 0) {
					output.write(buffer, 0, length);
				}
				output.flush();
				restoreDone = true;
			} catch (IOException e) {
				Log.e(cgSettings.tag, "Database wasn't restored, could not read/write file: " + e.toString());
			}
		}

		try {
			if (output != null)
				output.close();
			if (input != null)
				input.close();
		} catch (IOException e) {
			Log.e(cgSettings.tag, "Database wasn't restored, could not close file: " + e.toString());
		}

		if (restoreDone)
			Log.i(cgSettings.tag, "Database was restored");

		init();

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

			db.execSQL("create index if not exists in_a on " + dbTableCaches + " (geocode)");
			db.execSQL("create index if not exists in_b on " + dbTableCaches + " (guid)");
			db.execSQL("create index if not exists in_c on " + dbTableCaches + " (reason)");
			db.execSQL("create index if not exists in_d on " + dbTableCaches + " (detailed)");
			db.execSQL("create index if not exists in_e on " + dbTableCaches + " (type)");
			db.execSQL("create index if not exists in_f on " + dbTableCaches + " (visiteddate, detailedupdate)");
			db.execSQL("create index if not exists in_a on " + dbTableAttributes + " (geocode)");
			db.execSQL("create index if not exists in_a on " + dbTableWaypoints + " (geocode)");
			db.execSQL("create index if not exists in_b on " + dbTableWaypoints + " (geocode, type)");
			db.execSQL("create index if not exists in_a on " + dbTableSpoilers + " (geocode)");
			db.execSQL("create index if not exists in_a on " + dbTableLogs + " (geocode)");
			db.execSQL("create index if not exists in_a on " + dbTableLogCount + " (geocode)");
			db.execSQL("create index if not exists in_a on " + dbTableLogsOffline + " (geocode)");
			db.execSQL("create index if not exists in_a on " + dbTableTrackables + " (geocode)");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.i(cgSettings.tag, "Upgrade database from ver. " + oldVersion + " to ver. " + newVersion + ": start");

			try {
				if (db.isReadOnly()) {
					return;
				}

				db.beginTransaction();

				if (oldVersion <= 0) { // new table
					dropDatabase(db);
					onCreate(db);

					Log.i(cgSettings.tag, "Database structure created.");
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

							Log.i(cgSettings.tag, "Indexes added.");
						} catch (Exception e) {
							Log.e(cgSettings.tag, "Failed to upgrade to ver. 34: " + e.toString());
						}
					}

					if (oldVersion < 37) { // upgrade to 37
						try {
							db.execSQL("alter table " + dbTableCaches + " add column direction text");
							db.execSQL("alter table " + dbTableCaches + " add column distance double");

							Log.i(cgSettings.tag, "Columns direction and distance added to " + dbTableCaches + ".");
						} catch (Exception e) {
							Log.e(cgSettings.tag, "Failed to upgrade to ver. 37: " + e.toString());
						}
					}

					if (oldVersion < 38) { // upgrade to 38
						try {
							db.execSQL("drop table " + dbTableLogs);
							db.execSQL(dbCreateLogs);

							Log.i(cgSettings.tag, "Changed type column in " + dbTableLogs + " to integer.");
						} catch (Exception e) {
							Log.e(cgSettings.tag, "Failed to upgrade to ver. 38: " + e.toString());
						}
					}

					if (oldVersion < 39) { // upgrade to 39
						try {
							db.execSQL(dbCreateLists);

							Log.i(cgSettings.tag, "Created lists table.");
						} catch (Exception e) {
							Log.e(cgSettings.tag, "Failed to upgrade to ver. 39: " + e.toString());
						}
					}

					if (oldVersion < 40) { // upgrade to 40
						try {
							db.execSQL("drop table " + dbTableTrackables);
							db.execSQL(dbCreateTrackables);

							Log.i(cgSettings.tag, "Changed type of geocode column in trackables table.");
						} catch (Exception e) {
							Log.e(cgSettings.tag, "Failed to upgrade to ver. 40: " + e.toString());
						}
					}

					if (oldVersion < 41) { // upgrade to 41
						try {
							db.execSQL("alter table " + dbTableCaches + " add column rating float");
							db.execSQL("alter table " + dbTableCaches + " add column votes integer");
							db.execSQL("alter table " + dbTableCaches + " add column vote integer");

							Log.i(cgSettings.tag, "Added columns for GCvote.");
						} catch (Exception e) {
							Log.e(cgSettings.tag, "Failed to upgrade to ver. 41: " + e.toString());
						}
					}

					if (oldVersion < 42) { // upgrade to 42
						try {
							db.execSQL(dbCreateLogsOffline);

							Log.i(cgSettings.tag, "Added table for offline logs");
						} catch (Exception e) {
							Log.e(cgSettings.tag, "Failed to upgrade to ver. 42: " + e.toString());
						}
					}

					if (oldVersion < 43) { // upgrade to 43
						try {
							final String dbCreateCachesTemp = ""
									+ "create temporary table " + dbTableCaches + "_temp ("
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
							db.execSQL("insert into " + dbTableCaches + "_temp select _id, updated, detailed, detailedupdate, geocode, reason, cacheid, guid, type, name, owner, hidden, hint, size, difficulty, terrain, latlon, latitude_string, longitude_string, location, distance, latitude, longitude, shortdesc, description, rating, votes, vote, disabled, archived, members, found, favourite, inventorycoins, inventorytags, inventoryunknown from " + dbTableCaches);
							db.execSQL("drop table " + dbTableCaches);
							db.execSQL(dbCreateCachesNew);
							db.execSQL("insert into " + dbTableCaches + " select _id, updated, detailed, detailedupdate, geocode, reason, cacheid, guid, type, name, owner, hidden, hint, size, difficulty, terrain, latlon, latitude_string, longitude_string, location, null, distance, latitude, longitude, shortdesc, description, rating, votes, vote, disabled, archived, members, found, favourite, inventorycoins, inventorytags, inventoryunknown from " + dbTableCaches + "_temp");
							db.execSQL("drop table " + dbTableCaches + "_temp");
							db.setTransactionSuccessful();

							Log.i(cgSettings.tag, "Changed direction column");
						} catch (Exception e) {
							Log.e(cgSettings.tag, "Failed to upgrade to ver. 43: " + e.toString());
						} finally {
							db.endTransaction();
						}
					}

					if (oldVersion < 44) { // upgrade to 44
						try {
							db.execSQL("alter table " + dbTableCaches + " add column favourite_cnt integer");

							Log.i(cgSettings.tag, "Column favourite_cnt added to " + dbTableCaches + ".");
						} catch (Exception e) {
							Log.e(cgSettings.tag, "Failed to upgrade to ver. 44: " + e.toString());
						}
					}

					if (oldVersion < 45) { // upgrade to 45
						try {
							db.execSQL("alter table " + dbTableCaches + " add column owner_real text");

							Log.i(cgSettings.tag, "Column owner_real added to " + dbTableCaches + ".");
						} catch (Exception e) {
							Log.e(cgSettings.tag, "Failed to upgrade to ver. 45: " + e.toString());
						}
					}

					if (oldVersion < 46) { // upgrade to 46
						try {
							db.execSQL("alter table " + dbTableCaches + " add column visiteddate long");
							db.execSQL("create index if not exists in_f on " + dbTableCaches + " (visiteddate, detailedupdate)");

							Log.i(cgSettings.tag, "Added column for date of visit.");
						} catch (Exception e) {
							Log.e(cgSettings.tag, "Failed to upgrade to ver. 46: " + e.toString());
						}
					}
					if (oldVersion < 47) { // upgrade to 47
						try {
							db.execSQL("alter table " + dbTableCaches + " add column own integer not null default 0");

							Log.i(cgSettings.tag, "Added column own.");
						} catch (Exception e) {
							Log.e(cgSettings.tag, "Failed to upgrade to ver. 47: " + e.toString());
						}
					}

					if (oldVersion < 48) { // upgrade to 48
						try {
							db.execSQL("alter table " + dbTableCaches + " add column elevation double");

							Log.i(cgSettings.tag, "Column elevation added to " + dbTableCaches + ".");
						} catch (Exception e) {
							Log.e(cgSettings.tag, "Failed to upgrade to ver. 48: " + e.toString());
						}
					}

					if (oldVersion < 49) { // upgrade to 49
						try {
							db.execSQL(dbCreateLogCount);

							Log.i(cgSettings.tag, "Created table " + dbTableLogCount + ".");
						} catch (Exception e) {
							Log.e(cgSettings.tag, "Failed to upgrade to ver. 49: " + e.toString());
						}
					}

					if (oldVersion < 50) { // upgrade to 50
						try {
							db.execSQL("alter table " + dbTableCaches + " add column myvote float");

							Log.i(cgSettings.tag, "Added float column for votes to " + dbTableCaches + ".");
						} catch (Exception e) {
							Log.e(cgSettings.tag, "Failed to upgrade to ver. 50: " + e.toString());
						}
					}

					if (oldVersion < 51) { // upgrade to 51
						try {
							db.execSQL("alter table " + dbTableCaches + " add column reliable_latlon integer");

							Log.i(cgSettings.tag, "Column reliable_latlon added to " + dbTableCaches + ".");
						} catch (Exception e) {
							Log.e(cgSettings.tag, "Failed to upgrade to ver. 51: " + e.toString());
						}
					}

					if (oldVersion < 52) { // upgrade to 52
						try {
							db.execSQL(dbCreateSearchDestinationHistory);

							Log.i(cgSettings.tag, "Added table " +dbTableSearchDestionationHistory +".");
						} catch (Exception e) {
							Log.e(cgSettings.tag, "Failed to upgrade to ver. 52", e);
						}
					}

					if (oldVersion < 53) { // upgrade to 53
						try {
							db.execSQL("alter table " + dbTableCaches + " add column onWatchlist integer");

							Log.i(cgSettings.tag, "Column onWatchlist added to " + dbTableCaches +".");
						} catch (Exception e) {
							Log.e(cgSettings.tag, "Failed to upgrade to ver. 53", e);
						}
					}

					if (oldVersion < 54) { // update to 54
						try {
							db.execSQL(dbCreateLogImages);
						} catch (Exception e) {
							Log.e(cgSettings.tag, "Failed to upgrade to ver. 54: " + e.toString());

						}
					}

					if (oldVersion < 55) { // update to 55
						try {
							db.execSQL("alter table " + dbTableCaches + " add column personal_note text");
						} catch (Exception e) {
							Log.e(cgSettings.tag, "Failed to upgrade to ver. 55: " + e.toString());

						}
					}
				}

				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}

			Log.i(cgSettings.tag, "Upgrade database from ver. " + oldVersion + " to ver. " + newVersion + ": completed");
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
		ArrayList<String> thereA = new ArrayList<String>();

		try {
			cursor = databaseRO.query(
					dbTableCaches,
					new String[]{"_id", "geocode"},
					"(detailed = 1 and detailedupdate > " + (System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000)) + ") or reason > 0",
					null,
					null,
					null,
					"detailedupdate desc",
					"100");

			if (cursor != null) {
				int index = 0;
				String geocode = null;

				if (cursor.getCount() > 0) {
					cursor.moveToFirst();

					do {
						index = cursor.getColumnIndex("geocode");
						geocode = (String) cursor.getString(index);

						thereA.add(geocode);
					} while (cursor.moveToNext());
				} else {
					if (cursor != null) {
						cursor.close();
					}

					return null;
				}
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.allDetailedThere: " + e.toString());
		}

		if (cursor != null) {
			cursor.close();
		}

		return thereA.toArray(new String[thereA.size()]);
	}

	public boolean isThere(String geocode, String guid, boolean detailed, boolean checkTime) {
		init();

		Cursor cursor = null;

		int cnt = 0;
		long dataUpdated = 0;
		long dataDetailedUpdate = 0;
		int dataDetailed = 0;

		try {
			if (geocode != null && geocode.length() > 0) {
				cursor = databaseRO.query(
						dbTableCaches,
						new String[]{"_id", "detailed", "detailedupdate", "updated"},
						"geocode = \"" + geocode + "\"",
						null,
						null,
						null,
						null,
						"1");
			} else if (guid != null && guid.length() > 0) {
				cursor = databaseRO.query(
						dbTableCaches,
						new String[]{"_id", "detailed", "detailedupdate", "updated"},
						"guid = \"" + guid + "\"",
						null,
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
					dataUpdated = (long) cursor.getLong(index);
					index = cursor.getColumnIndex("detailedupdate");
					dataDetailedUpdate = (long) cursor.getLong(index);
					index = cursor.getColumnIndex("detailed");
					dataDetailed = (int) cursor.getInt(index);
				}
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.isThere: " + e.toString());
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

			if (checkTime && detailed == false && dataUpdated < (System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000))) {
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
			if (geocode != null && geocode.length() > 0) {
				cursor = databaseRO.query(
						dbTableCaches,
						new String[]{"reason"},
						"geocode = \"" + geocode + "\"",
						null,
						null,
						null,
						null,
						"1");
			} else if (guid != null && guid.length() > 0) {
				cursor = databaseRO.query(
						dbTableCaches,
						new String[]{"reason"},
						"guid = \"" + guid + "\"",
						null,
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
					reason = (long) cursor.getLong(index);
				}

				cursor.close();
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.isOffline: " + e.toString());
		}

		if (reason >= 1) {
			return true;
		} else {
			return false;
		}
	}
	
	
    @Deprecated
	public boolean isReliableLatLon(String geocode, String guid) {
		init();

		Cursor cursor = null;
		int rel = 0;

		try {
			if (geocode != null && geocode.length() > 0) {
				cursor = databaseRO.query(
						dbTableCaches,
						new String[]{"reliable_latlon"},
						"geocode = \"" + geocode + "\"",
						null,
						null,
						null,
						null,
						"1");
			} else if (guid != null && guid.length() > 0) {
				cursor = databaseRO.query(
						dbTableCaches,
						new String[]{"reliable_latlon"},
						"guid = \"" + guid + "\"",
						null,
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

					index = cursor.getColumnIndex("reliable_latlon");
					rel = (int) cursor.getInt(index);
				}

				cursor.close();
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.isOffline: " + e.toString());
		}

		if (rel >= 1) {
			return true;
		} else {
			return false;
		}
	}

	public String getGeocodeForGuid(String guid) {
		if (guid == null || guid.length() == 0) {
			return null;
		}

		init();

		Cursor cursor = null;
		String geocode = null;

		try {
			cursor = databaseRO.query(
					dbTableCaches,
					new String[]{"_id", "geocode"},
					"guid = \"" + guid + "\"",
					null,
					null,
					null,
					null,
					"1");

			if (cursor != null) {
				int index = 0;

				if (cursor.getCount() > 0) {
					cursor.moveToFirst();

					index = cursor.getColumnIndex("geocode");
					geocode = (String) cursor.getString(index);
				}
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.getGeocodeForGuid: " + e.toString());
		}

		if (cursor != null) {
			cursor.close();
		}

		return geocode;
	}

	public String getCacheidForGeocode(String geocode) {
		if (geocode == null || geocode.length() == 0) {
			return null;
		}

		init();

		Cursor cursor = null;
		String cacheid = null;

		try {
			cursor = databaseRO.query(
					dbTableCaches,
					new String[]{"_id", "cacheid"},
					"geocode = \"" + geocode + "\"",
					null,
					null,
					null,
					null,
					"1");

			if (cursor != null) {
				int index = 0;

				if (cursor.getCount() > 0) {
					cursor.moveToFirst();

					index = cursor.getColumnIndex("cacheid");
					cacheid = (String) cursor.getString(index);
				}
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.getCacheidForGeocode: " + e.toString());
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
		values.put("cacheid", cache.cacheid);
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
		values.put("size", cache.size);
		values.put("difficulty", cache.difficulty);
		values.put("terrain", cache.terrain);
		values.put("latlon", cache.latlon);
		values.put("latitude_string", cache.latitudeString);
		values.put("longitude_string", cache.longitudeString);
		values.put("location", cache.location);
		values.put("distance", cache.distance);
		values.put("direction", cache.direction);
		// save coordinates

		final boolean rel = cache.reliableLatLon;
		if (cache.reliableLatLon) { // new cache has reliable coordinates, store
			values.put("latitude", cache.latitude);
			values.put("longitude", cache.longitude);
			values.put("reliable_latlon", 1);
		} else if (!rel) { // new cache neither stored cache is not reliable, just update
			values.put("latitude", cache.latitude);
			values.put("longitude", cache.longitude);
			values.put("reliable_latlon", 0);
		}
		values.put("elevation", cache.elevation);
		values.put("shortdesc", cache.shortdesc);
		values.put("personal_note", cache.personalNote);
		values.put("description", cache.description);
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

		if (cache.logCounts != null && cache.logCounts.isEmpty() == false) {
			if (!saveLogCount(cache.geocode, cache.logCounts)) {
				statusOk = false;
			}
		}

		if (cache.inventory != null) {
			if (!saveInventory(cache.geocode, cache.inventory)) {
				statusOk = false;
			}
		}

		if (statusOk == false) {
			cache.detailed = false;
			cache.detailedUpdate = 0l;
		}

		init();

		//try to update record else insert fresh..
		try {
			int rows = databaseRW.update(dbTableCaches, values, "geocode = ?", new String[] {cache.geocode});
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

	public boolean saveAttributes(String geocode, ArrayList<String> attributes) {
		init();

		if (geocode == null || geocode.length() == 0 || attributes == null) {
			return false;
		}

		databaseRW.beginTransaction();
		try {
			databaseRW.delete(dbTableAttributes, "geocode = ?", new String[] {geocode});

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
	 * persisted <code>false</code> otherwise.
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
				values.put("latitude", destination.getLatitude());
				values.put("longitude", destination.getLongitude());

				long id = databaseRW.insert(dbTableSearchDestionationHistory, null, values);
				destination.setId(id);
				databaseRW.setTransactionSuccessful();
			} catch (Exception e) {
				success = false;
				Log.e(cgSettings.tag, "Updating searchedDestinations db failed", e);
			} finally {
				databaseRW.endTransaction();
			}
		}

		return success;
	}

	public boolean saveWaypoints(String geocode, ArrayList<cgWaypoint> waypoints, boolean drop) {
		init();

		if (geocode == null || geocode.length() == 0 || waypoints == null) {
			return false;
		}

		boolean ok = false;
		databaseRW.beginTransaction();
		try {
			if (drop) {
				databaseRW.delete(dbTableWaypoints, "geocode = ? and type <> ?", new String[] {geocode, "own"});
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
					values.put("type", oneWaypoint.type);
					values.put("prefix", oneWaypoint.prefix);
					values.put("lookup", oneWaypoint.lookup);
					values.put("name", oneWaypoint.name);
					values.put("latlon", oneWaypoint.latlon);
					values.put("latitude_string", oneWaypoint.latitudeString);
					values.put("longitude_string", oneWaypoint.longitudeString);
					values.put("latitude", oneWaypoint.latitude);
					values.put("longitude", oneWaypoint.longitude);
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

	public boolean saveOwnWaypoint(int id, String geocode, cgWaypoint waypoint) {
		init();

		if (((geocode == null || geocode.length() == 0) && id <= 0) || waypoint == null) {
			return false;
		}

		boolean ok = false;
		databaseRW.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put("geocode", geocode);
			values.put("updated", System.currentTimeMillis());
			values.put("type", waypoint.type);
			values.put("prefix", waypoint.prefix);
			values.put("lookup", waypoint.lookup);
			values.put("name", waypoint.name);
			values.put("latlon", waypoint.latlon);
			values.put("latitude_string", waypoint.latitudeString);
			values.put("longitude_string", waypoint.longitudeString);
			values.put("latitude", waypoint.latitude);
			values.put("longitude", waypoint.longitude);
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

	public boolean saveSpoilers(String geocode, ArrayList<cgImage> spoilers) {
		init();

		if (geocode == null || geocode.length() == 0 || spoilers == null) {
			return false;
		}

		databaseRW.beginTransaction();
		try {
			databaseRW.delete(dbTableSpoilers, "geocode = ?", new String[] {geocode});

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

	public boolean saveLogs(String geocode, ArrayList<cgLog> logs) {
		return saveLogs(geocode, logs, true);
	}

	public boolean saveLogs(String geocode, ArrayList<cgLog> logs, boolean drop) {
		init();

		if (geocode == null || geocode.length() == 0 || logs == null) {
			return false;
		}

		databaseRW.beginTransaction();
		try {
			if (drop) {
				// TODO delete logimages referring these logs
				databaseRW.delete(dbTableLogs, "geocode = ?", new String[] {geocode});
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

					if ((log.logImages != null) && (log.logImages.size() > 0)) {
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

	public boolean saveLogCount(String geocode, HashMap<Integer, Integer> logCounts) {
		return saveLogCount(geocode, logCounts, true);
	}

	public boolean saveLogCount(String geocode, HashMap<Integer, Integer> logCounts, boolean drop) {
		init();

		if (geocode == null || geocode.length() == 0 || logCounts == null || logCounts.isEmpty()) {
			return false;
		}

		databaseRW.beginTransaction();
		try {
			if (drop) {
				databaseRW.delete(dbTableLogCount, "geocode = ?", new String[] {geocode});
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

	public boolean saveInventory(String geocode, ArrayList<cgTrackable> trackables) {
		init();

		if (trackables == null) {
			return false;
		}

		databaseRW.beginTransaction();
		try {
			if (geocode != null) {
				databaseRW.delete(dbTableTrackables, "geocode = ?", new String[] {geocode});
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
					values.put("tbcode", oneTrackable.geocode);
					values.put("guid", oneTrackable.guid);
					values.put("title", oneTrackable.name);
					values.put("owner", oneTrackable.owner);
					if (oneTrackable.released != null) {
						values.put("released", oneTrackable.released.getTime());
					} else {
						values.put("released", 0l);
					}
					values.put("goal", oneTrackable.goal);
					values.put("description", oneTrackable.details);

					databaseRW.insert(dbTableTrackables, null, values);

					saveLogs(oneTrackable.geocode, oneTrackable.logs);
				}
			}
			databaseRW.setTransactionSuccessful();
		} finally {
			databaseRW.endTransaction();
		}

		return true;
	}

	public ArrayList<Object> getBounds(Object[] geocodes) {
		init();

		Cursor cursor = null;

		final ArrayList<Object> viewport = new ArrayList<Object>();

		try {
			final StringBuilder where = new StringBuilder();

			if (geocodes != null && geocodes.length > 0) {
				StringBuilder all = new StringBuilder();
				for (Object one : geocodes) {
					if (all.length() > 0) {
						all.append(", ");
					}
					all.append("\"");
					all.append((String) one);
					all.append("\"");
				}

				if (where.length() > 0) {
					where.append(" and ");
				}
				where.append("geocode in (");
				where.append(all);
				where.append(")");
			}

			cursor = databaseRO.query(
					dbTableCaches,
					new String[]{"count(_id) as cnt", "min(latitude) as latMin", "max(latitude) as latMax", "min(longitude) as lonMin", "max(longitude) as lonMax"},
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

					viewport.add((Integer) cursor.getInt(cursor.getColumnIndex("cnt")));
					viewport.add((Double) cursor.getDouble(cursor.getColumnIndex("latMin")));
					viewport.add((Double) cursor.getDouble(cursor.getColumnIndex("latMax")));
					viewport.add((Double) cursor.getDouble(cursor.getColumnIndex("lonMin")));
					viewport.add((Double) cursor.getDouble(cursor.getColumnIndex("lonMax")));
				}
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.getBounds: " + e.toString());
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
	 * @param geocode The Geocode GCXXXX
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

		if (geocode != null && geocode.length() > 0) {
			geocodes[0] = geocode;
		} else {
			geocodes = null;
		}

		if (guid != null && guid.length() > 0) {
			guids[0] = guid;
		} else {
			guids = null;
		}

		ArrayList<cgCache> caches = loadCaches(geocodes, null, null, null, null, null, loadA, loadW, loadS, loadL, loadI, loadO);
		if (caches != null && caches.isEmpty() == false) {
			return caches.get(0);
		}

		return null;
	}

	public ArrayList<cgCache> loadCaches(Object[] geocodes, Object[] guids) {
		return loadCaches(geocodes, guids, null, null, null, null, false, true, false, false, false, false);
	}

	public ArrayList<cgCache> loadCaches(Object[] geocodes, Object[] guids, boolean lite) {
		if (lite) {
			return loadCaches(geocodes, guids, null, null, null, null, false, true, false, false, false, false);
		} else {
			return loadCaches(geocodes, guids, null, null, null, null, true, true, true, true, true, true);
		}
	}

	public ArrayList<cgCache> loadCaches(Object[] geocodes, Object[] guids, Long centerLat, Long centerLon, Long spanLat, Long spanLon, boolean loadA, boolean loadW, boolean loadS, boolean loadL, boolean loadI, boolean loadO) {
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
		ArrayList<cgCache> caches = new ArrayList<cgCache>();

		try {
			if (geocodes != null) {
				if (geocodes.length > 0) {
					StringBuilder all = new StringBuilder();
					for (Object one : geocodes) {
						if (all.length() > 0) {
							all.append(", ");
						}
						all.append("\"");
						all.append((String) one);
						all.append("\"");
					}

					if (where.length() > 0) {
						where.append(" and ");
					}
					where.append("geocode in (");
					where.append(all);
					where.append(")");
				}
			} else if (guids != null && guids.length > 0) {
				StringBuilder all = new StringBuilder();
				for (Object one : guids) {
					if (all.length() > 0) {
						all.append(", ");
					}
					all.append("\"");
					all.append((String) one);
					all.append("\"");
				}

				if (where.length() > 0) {
					where.append(" and ");
				}
				where.append("guid in (");
				where.append(all);
				where.append(")");
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
				where.append("(");
				where.append("latitude >= ");
				where.append(String.format((Locale) null, "%.6f", latMin));
				where.append(" and latitude <= ");
				where.append(String.format((Locale) null, "%.6f", latMax));
				where.append(" and longitude >= ");
				where.append(String.format((Locale) null, "%.6f", lonMin));
				where.append(" and longitude <= ");
				where.append(String.format((Locale) null, "%.6f", lonMax));
				where.append(")");
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

						if (loadA) {
							ArrayList<String> attributes = loadAttributes(cache.geocode);
							if (attributes != null && attributes.isEmpty() == false) {
								if (cache.attributes == null)
									cache.attributes = new ArrayList<String>();
								else
									cache.attributes.clear();
								cache.attributes.addAll(attributes);
							}
						}

						if (loadW) {
							ArrayList<cgWaypoint> waypoints = loadWaypoints(cache.geocode);
							if (waypoints != null && waypoints.isEmpty() == false) {
								if (cache.waypoints == null)
									cache.waypoints = new ArrayList<cgWaypoint>();
								else
									cache.waypoints.clear();
								cache.waypoints.addAll(waypoints);
							}
						}

						if (loadS) {
							ArrayList<cgImage> spoilers = loadSpoilers(cache.geocode);
							if (spoilers != null && spoilers.isEmpty() == false) {
								if (cache.spoilers == null)
									cache.spoilers = new ArrayList<cgImage>();
								else
									cache.spoilers.clear();
								cache.spoilers.addAll(spoilers);
							}
						}

						if (loadL) {
							ArrayList<cgLog> logs = loadLogs(cache.geocode);
							if (logs != null && logs.isEmpty() == false) {
								if (cache.logs == null)
									cache.logs = new ArrayList<cgLog>();
								else
									cache.logs.clear();
								cache.logs.addAll(logs);
							}
							HashMap<Integer, Integer> logCounts = loadLogCounts(cache.geocode);
							if (logCounts != null && logCounts.isEmpty() == false) {
								cache.logCounts.clear();
								cache.logCounts.putAll(logCounts);
							}
						}

						if (loadI) {
							ArrayList<cgTrackable> inventory = loadInventory(cache.geocode);
							if (inventory != null && inventory.isEmpty() == false) {
								if (cache.inventory == null)
									cache.inventory = new ArrayList<cgTrackable>();
								else
									cache.inventory.clear();
								cache.inventory.addAll(inventory);
							}
						}

						if (loadO) {
							cache.logOffline = hasLogOffline(cache.geocode);
						}

						caches.add(cache);
					} while (cursor.moveToNext());
				} else {
					if (cursor != null) {
						cursor.close();
					}

					return null;
				}
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.loadCaches: " + e.toString());
		}

		if (cursor != null) {
			cursor.close();
		}

		return caches;
	}
	
	/**
	 * maps a Cache from the cursor. Doesn't next.
	 * @param cursor
	 * @return
	 */

	private cgCache createCacheFromDatabaseContent(Cursor cursor) {
		int index;
		cgCache cache = new cgCache();

		cache.updated = (long) cursor.getLong(cursor.getColumnIndex("updated"));
		cache.reason = (int) cursor.getInt(cursor.getColumnIndex("reason"));
		cache.detailed = cursor.getInt(cursor.getColumnIndex("detailed")) == 1;
		cache.detailedUpdate = (Long) cursor.getLong(cursor.getColumnIndex("detailedupdate"));
		cache.visitedDate = (Long) cursor.getLong(cursor.getColumnIndex("visiteddate"));
		cache.geocode = (String) cursor.getString(cursor.getColumnIndex("geocode"));
		cache.cacheid = (String) cursor.getString(cursor.getColumnIndex("cacheid"));
		cache.guid = (String) cursor.getString(cursor.getColumnIndex("guid"));
		cache.type = (String) cursor.getString(cursor.getColumnIndex("type"));
		cache.name = (String) cursor.getString(cursor.getColumnIndex("name"));
		cache.own = cursor.getInt(cursor.getColumnIndex("own")) == 1;
		cache.owner = (String) cursor.getString(cursor.getColumnIndex("owner"));
		cache.ownerReal = (String) cursor.getString(cursor.getColumnIndex("owner_real"));
		cache.hidden = new Date((long) cursor.getLong(cursor.getColumnIndex("hidden")));
		cache.hint = (String) cursor.getString(cursor.getColumnIndex("hint"));
		cache.size = (String) cursor.getString(cursor.getColumnIndex("size"));
		cache.difficulty = (Float) cursor.getFloat(cursor.getColumnIndex("difficulty"));
		index = cursor.getColumnIndex("direction");
		if (cursor.isNull(index)) {
			cache.direction = null;
		} else {
			cache.direction = (Double) cursor.getDouble(index);
		}
		index = cursor.getColumnIndex("distance");
		if (cursor.isNull(index)) {
			cache.distance = null;
		} else {
			cache.distance = (Double) cursor.getDouble(index);
		}
		cache.terrain = (Float) cursor.getFloat(cursor.getColumnIndex("terrain"));
		cache.latlon = (String) cursor.getString(cursor.getColumnIndex("latlon"));
		cache.latitudeString = (String) cursor.getString(cursor.getColumnIndex("latitude_string"));
		cache.longitudeString = (String) cursor.getString(cursor.getColumnIndex("longitude_string"));
		cache.location = (String) cursor.getString(cursor.getColumnIndex("location"));
		index = cursor.getColumnIndex("latitude");
		if (cursor.isNull(index)) {
			cache.latitude = null;
		} else {
			cache.latitude = (Double) cursor.getDouble(index);
		}
		index = cursor.getColumnIndex("longitude");
		if (cursor.isNull(index)) {
			cache.longitude = null;
		} else {
			cache.longitude = (Double) cursor.getDouble(index);
		}
		index = cursor.getColumnIndex("elevation");
		if (cursor.isNull(index)) {
			cache.elevation = null;
		} else {
			cache.elevation = (Double) cursor.getDouble(index);
		}
		cache.personalNote = (String) cursor.getString(cursor.getColumnIndex("personal_note"));
		cache.shortdesc = (String) cursor.getString(cursor.getColumnIndex("shortdesc"));
		cache.description = (String) cursor.getString(cursor.getColumnIndex("description"));
		cache.favouriteCnt = (Integer) cursor.getInt(cursor.getColumnIndex("favourite_cnt"));
		cache.rating = (Float) cursor.getFloat(cursor.getColumnIndex("rating"));
		cache.votes = (Integer) cursor.getInt(cursor.getColumnIndex("votes"));
		cache.myVote = (Float) cursor.getFloat(cursor.getColumnIndex("myvote"));
		cache.disabled = cursor.getLong(cursor.getColumnIndex("disabled")) == 1l;
		cache.archived = cursor.getLong(cursor.getColumnIndex("archived")) == 1l;
		cache.members = cursor.getLong(cursor.getColumnIndex("members")) == 1l;
		cache.found = cursor.getLong(cursor.getColumnIndex("found")) == 1l;
		cache.favourite = cursor.getLong(cursor.getColumnIndex("favourite")) == 1l;
		cache.inventoryItems = (Integer) cursor.getInt(cursor.getColumnIndex("inventoryunknown"));
		cache.onWatchlist = cursor.getLong(cursor.getColumnIndex("onWatchlist")) == 1l;
		cache.reliableLatLon = cursor.getInt(cursor.getColumnIndex("reliable_latlon"))>0;
		return cache;
	}

	public ArrayList<String> loadAttributes(String geocode) {
		if (geocode == null || geocode.length() == 0) {
			return null;
		}

		init();

		ArrayList<String> attributes = new ArrayList<String>();

		Cursor cursor = databaseRO.query(
				dbTableAttributes,
				new String[]{"_id", "attribute"},
				"geocode = \"" + geocode + "\"",
				null,
				null,
				null,
				null,
				"100");

		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();

			do {
				attributes.add((String) cursor.getString(cursor.getColumnIndex("attribute")));
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
				new String[]{"_id", "geocode", "updated", "type", "prefix", "lookup", "name", "latlon", "latitude_string", "longitude_string", "latitude", "longitude", "note"},
				"_id = " + id,
				null,
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

	public ArrayList<cgWaypoint> loadWaypoints(String geocode) {
		if (geocode == null || geocode.length() == 0) {
			return null;
		}

		init();

		ArrayList<cgWaypoint> waypoints = new ArrayList<cgWaypoint>();

		Cursor cursor = databaseRO.query(
				dbTableWaypoints,
				new String[]{"_id", "geocode", "updated", "type", "prefix", "lookup", "name", "latlon", "latitude_string", "longitude_string", "latitude", "longitude", "note"},
				"geocode = \"" + geocode + "\"",
				null,
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
		waypoint.id = (int) cursor.getInt(cursor.getColumnIndex("_id"));
		waypoint.geocode = (String) cursor.getString(cursor.getColumnIndex("geocode"));
		waypoint.type = (String) cursor.getString(cursor.getColumnIndex("type"));
		waypoint.prefix = (String) cursor.getString(cursor.getColumnIndex("prefix"));
		waypoint.lookup = (String) cursor.getString(cursor.getColumnIndex("lookup"));
		waypoint.name = (String) cursor.getString(cursor.getColumnIndex("name"));
		waypoint.latlon = (String) cursor.getString(cursor.getColumnIndex("latlon"));
		waypoint.latitudeString = (String) cursor.getString(cursor.getColumnIndex("latitude_string"));
		waypoint.longitudeString = (String) cursor.getString(cursor.getColumnIndex("longitude_string"));
		int index = cursor.getColumnIndex("latitude");
		if (cursor.isNull(index)) {
			waypoint.latitude = null;
		} else {
			waypoint.latitude = (Double) cursor.getDouble(index);
		}
		index = cursor.getColumnIndex("longitude");
		if (cursor.isNull(index)) {
			waypoint.longitude = null;
		} else {
			waypoint.longitude = (Double) cursor.getDouble(index);
		}
		waypoint.note = (String) cursor.getString(cursor.getColumnIndex("note"));

		return waypoint;
	}

	public ArrayList<cgImage> loadSpoilers(String geocode) {
		if (geocode == null || geocode.length() == 0) {
			return null;
		}

		init();

		ArrayList<cgImage> spoilers = new ArrayList<cgImage>();

		Cursor cursor = databaseRO.query(
				dbTableSpoilers,
				new String[]{"_id", "url", "title", "description"},
				"geocode = \"" + geocode + "\"",
				null,
				null,
				null,
				null,
				"100");

		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();

			do {
				cgImage spoiler = new cgImage();
				spoiler.url = (String) cursor.getString(cursor.getColumnIndex("url"));
				spoiler.title = (String) cursor.getString(cursor.getColumnIndex("title"));
				spoiler.description = (String) cursor.getString(cursor.getColumnIndex("description"));

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
	 * the database. If no destinations exist, an {@link Collections#emptyList()}
	 * will be returned.
	 * @return A list of previously entered destinations or an empty list.
	 */
	public List<cgDestination> loadHistoryOfSearchedLocations() {
		init();

		Cursor cursor = databaseRO.query(dbTableSearchDestionationHistory,
				new String[] { "_id", "date", "latitude", "longitude" }, null,
				null, null, null, "date desc", "100");

		final List<cgDestination> destinations = new LinkedList<cgDestination>();

		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();

			do {
				cgDestination dest = new cgDestination();

				dest.setId((long) cursor.getLong(cursor.getColumnIndex("_id")));
				dest.setDate((long) cursor.getLong(cursor.getColumnIndex("date")));
				dest.setLatitude((double) cursor.getDouble(cursor.getColumnIndex("latitude")));
				dest.setLongitude((double) cursor.getDouble(cursor.getColumnIndex("longitude")));

				destinations.add(dest);
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
			Log.e(cgSettings.tag, "Unable to clear searched destinations", e);
		}
		finally{
			databaseRW.endTransaction();
		}

		return success;
	}

	public ArrayList<cgLog> loadLogs(String geocode) {
		if (geocode == null || geocode.length() == 0) {
			return null;
		}

		init();

		ArrayList<cgLog> logs = new ArrayList<cgLog>();

		Cursor cursor = databaseRO.rawQuery(
				"SELECT cg_logs._id as cg_logs_id, type, author, log, date, found, " + dbTableLogImages + "._id as cg_logImages_id, log_id, title, url FROM "
						+ dbTableLogs + " LEFT OUTER JOIN " + dbTableLogImages
						+ " ON ( cg_logs._id = log_id ) WHERE geocode = ?  ORDER BY date desc, cg_logs._id asc", new String[]{ geocode});

		if (cursor != null && cursor.getCount() > 0) {
			cgLog log = null;
			while (cursor.moveToNext() && logs.size() < 100) {
				if (log == null || log.id != cursor.getInt(cursor.getColumnIndex("cg_logs_id"))) {
					log = new cgLog();
					log.id = (int) cursor.getInt(cursor.getColumnIndex("cg_logs_id"));
					log.type = (int) cursor.getInt(cursor.getColumnIndex("type"));
					log.author = (String) cursor.getString(cursor.getColumnIndex("author"));
					log.log = (String) cursor.getString(cursor.getColumnIndex("log"));
					log.date = (long) cursor.getLong(cursor.getColumnIndex("date"));
					log.found = (int) cursor.getInt(cursor.getColumnIndex("found"));
					logs.add(log);
				}
				if (!cursor.isNull(cursor.getColumnIndex("cg_logImages_id"))) {
					final cgImage log_img = new cgImage();
					log_img.title = (String) cursor.getString(cursor.getColumnIndex("title"));
					if (log_img.title == null) {
						log_img.title = "";
					}
					log_img.url = (String) cursor.getString(cursor.getColumnIndex("url"));
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

	public HashMap<Integer, Integer> loadLogCounts(String geocode) {
		if (geocode == null || geocode.length() == 0) {
			return null;
		}

		init();

		HashMap<Integer, Integer> logCounts = new HashMap<Integer, Integer>();

		Cursor cursor = databaseRO.query(
				dbTableLogCount,
				new String[]{"_id", "type", "count"},
				"geocode = \"" + geocode + "\"",
				null,
				null,
				null,
				null,
				"100");

		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();

			do {
				Integer type = cursor.getInt(cursor.getColumnIndex("type"));
				Integer count = cursor.getInt(cursor.getColumnIndex("count"));

				logCounts.put(type, count);
			} while (cursor.moveToNext());
		}

		if (cursor != null) {
			cursor.close();
		}

		return logCounts;
	}


	public ArrayList<cgTrackable> loadInventory(String geocode) {
		if (geocode == null || geocode.length() == 0) {
			return null;
		}

		init();

		ArrayList<cgTrackable> trackables = new ArrayList<cgTrackable>();

		Cursor cursor = databaseRO.query(
				dbTableTrackables,
				new String[]{"_id", "updated", "tbcode", "guid", "title", "owner", "released", "goal", "description"},
				"geocode = \"" + geocode + "\"",
				null,
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
		if (geocode == null || geocode.length() == 0) {
			return null;
		}

		init();

		cgTrackable trackable = new cgTrackable();

		Cursor cursor = databaseRO.query(
				dbTableTrackables,
				new String[]{"_id", "updated", "tbcode", "guid", "title", "owner", "released", "goal", "description"},
				"tbcode = \"" + geocode + "\"",
				null,
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
		trackable.geocode = (String) cursor.getString(cursor.getColumnIndex("tbcode"));
		trackable.guid = (String) cursor.getString(cursor.getColumnIndex("guid"));
		trackable.name = (String) cursor.getString(cursor.getColumnIndex("title"));
		trackable.owner = (String) cursor.getString(cursor.getColumnIndex("owner"));
		String releasedPre = cursor.getString(cursor.getColumnIndex("released"));
		if (releasedPre != null && Long.getLong(releasedPre) != null) {
			trackable.released = new Date(Long.getLong(releasedPre));
		} else {
			trackable.released = null;
		}
		trackable.goal = (String) cursor.getString(cursor.getColumnIndex("goal"));
		trackable.details = (String) cursor.getString(cursor.getColumnIndex("description"));
		trackable.logs = loadLogs(trackable.geocode);
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
			if (detailedOnly == false) {
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
			Log.e(cgSettings.tag, "cgData.loadAllStoredCachesCount: " + e.toString());
		}

		return count;
	}

	public int getAllHistoricCachesCount(boolean detailedOnly, String cachetype) {
		init();

		int count = 0;

		try {
			SQLiteStatement sqlCount = databaseRO.compileStatement("select count(_id) from " + dbTableCaches + " where visiteddate > 0");
			count = (int) sqlCount.simpleQueryForLong();
			sqlCount.close();
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.getAllHistoricCachesCount: " + e.toString());
		}

		return count;
	}

	public ArrayList<String> loadBatchOfStoredGeocodes(boolean detailedOnly, Double latitude, Double longitude, String cachetype, int list) {
		init();

		if (list < 1) {
			list = 1;
		}

		ArrayList<String> geocodes = new ArrayList<String>();

		StringBuilder specifySql = new StringBuilder();

		specifySql.append("reason = ");
		specifySql.append(list);

		if (detailedOnly)
			specifySql.append(" and detailed = 1 ");

		if (cachetype != null) {
			specifySql.append(" and type = \"");
			specifySql.append(cachetype);
			specifySql.append("\"");
		}

		try {
			Cursor cursor = databaseRO.query(
					dbTableCaches,
					new String[]{"_id", "geocode", "(abs(latitude-" + String.format((Locale) null, "%.6f", latitude) + ") + abs(longitude-" + String.format((Locale) null, "%.6f", longitude) + ")) as dif"},
					specifySql.toString(),
					null,
					null,
					null,
					"dif",
					null);

			if (cursor != null) {
				if (cursor.getCount() > 0) {
					cursor.moveToFirst();

					do {
						geocodes.add((String) cursor.getString(cursor.getColumnIndex("geocode")));
					} while (cursor.moveToNext());
				} else {
					cursor.close();
					return null;
				}

				cursor.close();
			}

		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.loadBatchOfStoredGeocodes: " + e.toString());
		}

		return geocodes;
	}

	public ArrayList<String> loadBatchOfHistoricGeocodes(boolean detailedOnly, String cachetype) {
		init();

		ArrayList<String> geocodes = new ArrayList<String>();

		StringBuilder specifySql = new StringBuilder();
		specifySql.append("visiteddate > 0");

		if (detailedOnly) {
			specifySql.append(" and detailed = 1");
		}
		if (cachetype != null) {
			specifySql.append(" and type = \"");
			specifySql.append(cachetype);
			specifySql.append("\"");
		}

		try {
			Cursor cursor = databaseRO.query(
					dbTableCaches,
					new String[]{"_id", "geocode"},
					specifySql.toString(),
					null,
					null,
					null,
					"visiteddate",
					null);

			if (cursor != null) {
				if (cursor.getCount() > 0) {
					cursor.moveToFirst();

					do {
						geocodes.add((String) cursor.getString(cursor.getColumnIndex("geocode")));
					} while (cursor.moveToNext());
				} else {
					cursor.close();
					return null;
				}

				cursor.close();
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.loadBatchOfHistoricGeocodes: " + e.toString());
		}

		return geocodes;
	}

	public ArrayList<String> getCachedInViewport(Long centerLat, Long centerLon, Long spanLat, Long spanLon, String cachetype) {
		return getInViewport(false, centerLat, centerLon, spanLat, spanLon, cachetype);
	}

	public ArrayList<String> getStoredInViewport(Long centerLat, Long centerLon, Long spanLat, Long spanLon, String cachetype) {
		return getInViewport(true, centerLat, centerLon, spanLat, spanLon, cachetype);
	}

	public ArrayList<String> getInViewport(boolean stored, Long centerLat, Long centerLon, Long spanLat, Long spanLon, String cachetype) {
		if (centerLat == null || centerLon == null || spanLat == null || spanLon == null) {
			return null;
		}

		init();

		ArrayList<String> geocodes = new ArrayList<String>();

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
			where.append("\"");
		}

		// offline caches only
		if (stored) {
			where.append(" and reason >= 1");
		}

		try {
			Cursor cursor = databaseRO.query(
					dbTableCaches,
					new String[]{"_id", "geocode"},
					where.toString(),
					null,
					null,
					null,
					null,
					"500");

			if (cursor != null) {
				if (cursor.getCount() > 0) {
					cursor.moveToFirst();

					do {
						geocodes.add((String) cursor.getString(cursor.getColumnIndex("geocode")));
					} while (cursor.moveToNext());
				} else {
					cursor.close();
					return null;
				}

				cursor.close();
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.getOfflineInViewport: " + e.toString());
		}

		return geocodes;
	}

	public ArrayList<String> getOfflineAll(String cachetype) {
		init();

		ArrayList<String> geocodes = new ArrayList<String>();

		StringBuilder where = new StringBuilder();

		// cachetype limitation
		if (cachetype != null) {
			where.append(cachetype);
			where.append("\"");
		}

		// offline caches only
		if (where.length() > 0) {
			where.append(" and ");
		}
		where.append("reason >= 1");

		try {
			Cursor cursor = databaseRO.query(
					dbTableCaches,
					new String[]{"_id", "geocode"},
					where.toString(),
					null,
					null,
					null,
					null,
					"5000");

			if (cursor != null) {
				if (cursor.getCount() > 0) {
					cursor.moveToFirst();

					do {
						geocodes.add((String) cursor.getString(cursor.getColumnIndex("geocode")));
					} while (cursor.moveToNext());
				} else {
					cursor.close();
					return null;
				}

				cursor.close();
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.getOfflineAll: " + e.toString());
		}

		return geocodes;
	}

	public void markStored(String geocode, int listId) {
		if (geocode == null || geocode.length() == 0) {
			return;
		}

		init();

		if (listId <= 0) {
			listId = 1;
		}

		ContentValues values = new ContentValues();
		values.put("reason", listId);
		databaseRW.update(dbTableCaches, values, "geocode = ? and reason < 1", new String[] {geocode});
	}

	public boolean markDropped(String geocode) {
		if (geocode == null || geocode.length() == 0) {
			return false;
		}

		init();

		try {
			ContentValues values = new ContentValues();
			values.put("reason", 0);
			int rows = databaseRW.update(dbTableCaches, values, "geocode = ?", new String[] {geocode});

			if (rows > 0) {
				return true;
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.markDropped: " + e.toString());
		}

		return false;
	}

	public boolean markFound(String geocode) {
		if (geocode == null || geocode.length() == 0) {
			return false;
		}

		init();

		try {
			ContentValues values = new ContentValues();
			values.put("found", 1);
			int rows = databaseRW.update(dbTableCaches, values, "geocode = ?", new String[] {geocode});

			if (rows > 0) {
				return true;
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.markFound: " + e.toString());
		}

		return false;
	}

	public void clean() {
		clean(false);
	}

	public void clean(boolean more) {
		init();

		Log.d(cgSettings.tag, "Database clean: started");

		Cursor cursor = null;
		ArrayList<String> geocodes = new ArrayList<String>();

		try {
			if (more) {
				cursor = databaseRO.query(
						dbTableCaches,
						new String[]{"_id", "geocode"},
						"reason = 0",
						null,
						null,
						null,
						null,
						null);
			} else {
				cursor = databaseRO.query(
						dbTableCaches,
						new String[]{"_id", "geocode"},
						"reason = 0 and detailed < " + (System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000)) + " and detailedupdate < " + (System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000)) + " and visiteddate < " + (System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000)),
						null,
						null,
						null,
						null,
						null);
			}

			if (cursor != null) {
				if (cursor.getCount() > 0) {
					cursor.moveToFirst();

					do {
						geocodes.add("\"" + (String) cursor.getString(cursor.getColumnIndex("geocode")) + "\"");
					} while (cursor.moveToNext());
				}

				cursor.close();
			}

			final int size = geocodes.size();
			if (size > 0) {
				Log.d(cgSettings.tag, "Database clean: removing " + size + " geocaches");

				String geocodeList = cgBase.implode(", ", geocodes.toArray());
				databaseRW.execSQL("delete from " + dbTableCaches + " where geocode in (" + geocodeList + ")");
				databaseRW.execSQL("delete from " + dbTableAttributes + " where geocode in (" + geocodeList + ")");
				databaseRW.execSQL("delete from " + dbTableSpoilers + " where geocode in (" + geocodeList + ")");
				databaseRW.execSQL("delete from " + dbTableLogs + " where geocode in (" + geocodeList + ")");
				databaseRW.execSQL("delete from " + dbTableLogCount + " where geocode in (" + geocodeList + ")");
				databaseRW.execSQL("delete from " + dbTableLogsOffline + " where geocode in (" + geocodeList + ")");
				databaseRW.execSQL("delete from " + dbTableWaypoints + " where geocode in (" + geocodeList + ") and type <> \"own\"");
				databaseRW.execSQL("delete from " + dbTableTrackables + " where geocode in (" + geocodeList + ")");

				geocodes.clear();
			}

			databaseRW.execSQL("delete from " + dbTableCaches + " where geocode = \"\"");

			if (Log.isLoggable(cgSettings.tag, Log.DEBUG)) {
				final SQLiteStatement countSql = databaseRO.compileStatement("select count(_id) from " + dbTableCaches + " where reason = 0");
				final int count = (int) countSql.simpleQueryForLong();
				countSql.close();
				Log.d(cgSettings.tag, "Database clean: " + count + " cached geocaches remaining");
			}
		} catch (Exception e) {
			Log.w(cgSettings.tag, "cgData.clean: " + e.toString());
		}

		Log.d(cgSettings.tag, "Database clean: finished");
	}

	public void dropStored(int listId) {
		init();

		ArrayList<String> geocodes = new ArrayList<String>();

		try {
			Cursor cursor = databaseRO.query(
					dbTableCaches,
					new String[]{"_id", "geocode"},
					"reason = " + listId,
					null,
					null,
					null,
					null,
					null);

			if (cursor != null) {
				if (cursor.getCount() > 0) {
					cursor.moveToFirst();

					do {
						geocodes.add("\"" + (String) cursor.getString(cursor.getColumnIndex("geocode")) + "\"");
					} while (cursor.moveToNext());
				} else {
					cursor.close();
					return;
				}

				cursor.close();
			}

			if (geocodes.size() > 0) {
				String geocodeList = cgBase.implode(", ", geocodes.toArray());
				databaseRW.execSQL("delete from " + dbTableCaches + " where geocode in (" + geocodeList + ")");
				databaseRW.execSQL("delete from " + dbTableAttributes + " where geocode in (" + geocodeList + ")");
				databaseRW.execSQL("delete from " + dbTableSpoilers + " where geocode in (" + geocodeList + ")");
				databaseRW.execSQL("delete from " + dbTableLogs + " where geocode in (" + geocodeList + ")");
				databaseRW.execSQL("delete from " + dbTableLogCount + " where geocode in (" + geocodeList + ")");
				databaseRW.execSQL("delete from " + dbTableLogsOffline + " where geocode in (" + geocodeList + ")");
				databaseRW.execSQL("delete from " + dbTableWaypoints + " where geocode in (" + geocodeList + ") and type <> \"own\"");
				databaseRW.execSQL("delete from " + dbTableTrackables + " where geocode in (" + geocodeList + ")");

				geocodes.clear();
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.dropStored: " + e.toString());
		}
	}

	public boolean saveLogOffline(String geocode, Date date, int type, String log) {
		if (geocode == null || geocode.length() == 0) {
			return false;
		}
		if (type <= 0 && (log == null || log.length() == 0)) {
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
				final int rows = databaseRW.update(dbTableLogsOffline, values, "geocode = ?", new String[] {geocode});

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
			Log.e(cgSettings.tag, "cgData.saveLogOffline: " + e.toString());
		}

		return status;
	}

	public cgLog loadLogOffline(String geocode) {
		if (geocode == null || geocode.length() == 0) {
			return null;
		}

		init();

		cgLog log = null;

		Cursor cursor = databaseRO.query(
				dbTableLogsOffline,
				new String[]{"_id", "type", "log", "date"},
				"geocode = \"" + geocode + "\"",
				null,
				null,
				null,
				"_id desc",
				"1");

		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();

			log = new cgLog();
			log.id = (int) cursor.getInt(cursor.getColumnIndex("_id"));
			log.type = (int) cursor.getInt(cursor.getColumnIndex("type"));
			log.log = (String) cursor.getString(cursor.getColumnIndex("log"));
			log.date = (long) cursor.getLong(cursor.getColumnIndex("date"));
		}

		if (cursor != null) {
			cursor.close();
		}

		return log;
	}

	public void clearLogOffline(String geocode) {
		if (geocode == null || geocode.length() == 0) {
			return;
		}

		init();

		databaseRW.delete(dbTableLogsOffline, "geocode = ?", new String[] {geocode});
	}

	public boolean hasLogOffline(String geocode) {
		if (geocode == null || geocode.length() == 0) {
			return false;
		}

		int count = 0;
		init();
		try {
			final SQLiteStatement countSql = databaseRO.compileStatement("select count(_id) from " + dbTableLogsOffline + " where geocode = \"" + geocode.toUpperCase() + "\"");
			count = (int) countSql.simpleQueryForLong();

			countSql.close();
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.hasLogOffline: " + e.toString());
		}

		return count > 0;
	}

	public void saveVisitDate(String geocode) {
		if (geocode == null || geocode.length() == 0) {
			return;
		}

		ContentValues values = new ContentValues();
		values.put("visiteddate", System.currentTimeMillis());

		try {
			databaseRW.update(dbTableCaches, values, "geocode = ?", new String[] {geocode});
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.saveVisitDate: " + e.toString());
		}
	}

	public void clearVisitDate(String geocode) {
        if (geocode == null || geocode.length() == 0) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put("visiteddate", 0);

        try {
            databaseRW.update(dbTableCaches, values, "geocode = ?", new String[] {geocode});
        } catch (Exception e) {
            Log.e(cgSettings.tag, "cgData.clearVisitDate: " + e.toString());
        }
    }

	public ArrayList<cgList> getLists(Resources res) {
		init();

		ArrayList<cgList> lists = new ArrayList<cgList>();

		lists.add(new cgList(true, 1, res.getString(R.string.list_inbox)));
		// lists.add(new cgList(true, 2, res.getString(R.string.list_wpt)));

		try {
			Cursor cursor = databaseRO.query(
					dbTableLists,
					new String[]{"_id", "title", "updated", "latitude", "longitude"},
					null,
					null,
					null,
					null,
					"title COLLATE NOCASE ASC",
					null);

			if (cursor != null) {
				if (cursor.getCount() > 0) {
					cursor.moveToFirst();

					do {
						cgList list = new cgList(false);

						list.id = ((int) cursor.getInt(cursor.getColumnIndex("_id"))) + 10;
						list.title = (String) cursor.getString(cursor.getColumnIndex("title"));
						list.updated = (Long) cursor.getLong(cursor.getColumnIndex("updated"));
						list.latitude = (Double) cursor.getDouble(cursor.getColumnIndex("latitude"));
						list.longitude = (Double) cursor.getDouble(cursor.getColumnIndex("longitude"));

						lists.add(list);
					} while (cursor.moveToNext());
				}

				cursor.close();
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.getLists: " + e.toString());
		}

		return lists;
	}

	public cgList getList(int id, Resources res) {
		cgList list = null;

		if (id == 1) {
			list = new cgList(true, 1, res.getString(R.string.list_inbox));
		} else if (id == 2) {
			list = new cgList(true, 2, res.getString(R.string.list_wpt));
		} else if (id >= 10) {
			init();

			try {
				Cursor cursor = databaseRO.query(
						dbTableLists,
						new String[]{"_id", "title", "updated", "latitude", "longitude"},
						"_id = " + (id - 10),
						null,
						null,
						null,
						null,
						null);

				if (cursor != null) {
					if (cursor.getCount() > 0) {
						cursor.moveToFirst();

						do {
							list = new cgList(false);

							list.id = ((int) cursor.getInt(cursor.getColumnIndex("_id"))) + 10;
							list.title = (String) cursor.getString(cursor.getColumnIndex("title"));
							list.updated = (Long) cursor.getLong(cursor.getColumnIndex("updated"));
							list.latitude = (Double) cursor.getDouble(cursor.getColumnIndex("latitude"));
							list.longitude = (Double) cursor.getDouble(cursor.getColumnIndex("longitude"));
						} while (cursor.moveToNext());
					}

					cursor.close();
				}
			} catch (Exception e) {
				Log.e(cgSettings.tag, "cgData.getList: " + e.toString());
			}
		}

		return list;
	}

	public int createList(String name) {
		int id = -1;
		if (name == null || name.length() == 0) {
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
		if (geocode == null || geocode.length() == 0 || listId <= 0) {
			return;
		}

		databaseRW.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put("reason", listId);
			databaseRW.update(dbTableCaches, values, "geocode = ?", new String[] {geocode});

			databaseRW.setTransactionSuccessful();
		} finally {
			databaseRW.endTransaction();
		}
	}

	public synchronized boolean status() {
		if (databaseRO == null || databaseRW == null || initialized == false) {
			return false;
		}

		return true;
	}

	public boolean removeSearchedDestination(cgDestination destination) {
		boolean success = true;
		if(destination == null){
			success = false;
		} else{
			init();

			databaseRW.beginTransaction();

			try {
				databaseRW.delete(dbTableSearchDestionationHistory, "_id = " +destination.getId(), null);
				databaseRW.setTransactionSuccessful();
			} catch (Exception e) {
				Log.e(cgSettings.tag, "Unable to remove searched destination" ,e);
				success = false;
			} finally{
				databaseRW.endTransaction();
			}
		}

		return success;
	}
}
