package cgeo.geocaching;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.util.Log;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Environment;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

public class cgData {

	public cgCacheWrap caches;
	private Context context = null;
	private String path = null;
	private cgDbHelper dbHelper = null;
	private SQLiteDatabase databaseRO = null;
	private SQLiteDatabase databaseRW = null;
	private static final int dbVersion = 51;
	private static final String dbName = "data";
	private static final String dbTableCaches = "cg_caches";
	private static final String dbTableLists = "cg_lists";
	private static final String dbTableAttributes = "cg_attributes";
	private static final String dbTableWaypoints = "cg_waypoints";
	private static final String dbTableSpoilers = "cg_spoilers";
	private static final String dbTableLogs = "cg_logs";
	private static final String dbTableLogCount = "cg_logCount";
	private static final String dbTableLogsOffline = "cg_logs_offline";
	private static final String dbTableTrackables = "cg_trackables";
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
			+ "inventoryunknown integer default 0 "
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
	private static final String dbCreateLogCount = ""
			+ "create table " + dbTableLogCount + " ("
			+ "_id integer primary key autoincrement, "
			+ "geocode text not null, "
			+ "updated long not null, " // date of save
			+ "type integer not null default 4, "
			+ "count integer not null default 0 "
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
	public boolean initialized = false;

	public cgData(Context contextIn) {
		context = contextIn;
	}

	public void init() {
		if (databaseRW == null || databaseRW.isOpen() == false) {
			try {
				if (dbHelper == null) {
					dbHelper = new cgDbHelper(context);
				}
				databaseRW = dbHelper.getWritableDatabase();

				if (databaseRW != null && databaseRW.isOpen()) {
					Log.i(cgSettings.tag, "Connection to RW database established.");
				} else {
					Log.e(cgSettings.tag, "Failed to open connection to RW database.");
				}

				if (databaseRW.inTransaction() == true) {
					databaseRW.endTransaction();
				}
			} catch (Exception e) {
				Log.e(cgSettings.tag, "cgData.openDb.RW: " + e.toString());
			}
		}

		if (databaseRO == null || databaseRO.isOpen() == false) {
			try {
				if (dbHelper == null) {
					dbHelper = new cgDbHelper(context);
				}
				databaseRO = dbHelper.getReadableDatabase();

				if (databaseRO.needUpgrade(dbVersion) == true) {
					databaseRO = null;
				}

				if (databaseRO != null && databaseRO.isOpen()) {
					Log.i(cgSettings.tag, "Connection to RO database established.");
				} else {
					Log.e(cgSettings.tag, "Failed to open connection to RO database.");
				}

				if (databaseRO.inTransaction() == true) {
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

			if (databaseRO.inTransaction() == true) {
				databaseRO.endTransaction();
			}

			databaseRO.close();
			databaseRO = null;
			SQLiteDatabase.releaseMemory();

			Log.d(cgSettings.tag, "Closing RO database");
		}

		if (databaseRW != null) {
			path = databaseRW.getPath();

			if (databaseRW.inTransaction() == true) {
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

		try {
			final String directoryImg = cgSettings.cache;
			final String directoryTarget = Environment.getExternalStorageDirectory() + "/" + directoryImg + "/";
			final String fileTarget = directoryTarget + "cgeo.sqlite";
			final String fileSource = path;

			File directoryTargetFile = new File(directoryTarget);
			if (directoryTargetFile.exists() == false) {
				directoryTargetFile.mkdir();
			}

			InputStream input = new FileInputStream(fileSource);
			OutputStream output = new FileOutputStream(fileTarget);

			byte[] buffer = new byte[1024];
			int length;
			while ((length = input.read(buffer)) > 0) {
				output.write(buffer, 0, length);
			}

			output.flush();
			output.close();
			input.close();

			Log.i(cgSettings.tag, "Database was copied to " + fileTarget);

			init();

			return fileTarget;
		} catch (Exception e) {
			Log.w(cgSettings.tag, "Database wasn't backed up: " + e.toString());
		}

		init();

		return null;
	}

	public File isRestoreFile() {
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

		try {
			final String directoryImg = cgSettings.cache;
			final String fileSource = Environment.getExternalStorageDirectory() + "/" + directoryImg + "/cgeo.sqlite";
			final String fileTarget = path;

			File fileSourceFile = new File(fileSource);
			if (fileSourceFile.exists() == false) {
				Log.w(cgSettings.tag, "Database backup was not found");

				init();

				return false;
			}

			InputStream input = new FileInputStream(fileSource);
			OutputStream output = new FileOutputStream(fileTarget);

			byte[] buffer = new byte[1024];
			int length;
			while ((length = input.read(buffer)) > 0) {
				output.write(buffer, 0, length);
			}

			output.flush();
			output.close();
			input.close();

			Log.i(cgSettings.tag, "Database was restored");

			init();

			return true;
		} catch (Exception e) {
			Log.w(cgSettings.tag, "Database wasn't restored: " + e.toString());
		}

		init();

		return false;
	}

	private class cgDbHelper extends SQLiteOpenHelper {

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
			db.execSQL(dbCreateLogsOffline);
			db.execSQL(dbCreateTrackables);

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
				if (db.isReadOnly() == true) {
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
			if (detailed == true && dataDetailed == 0) {
				// we want details, but these are not stored
				return false;
			}

			if (checkTime == true && detailed == true && dataDetailedUpdate < (System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000))) {
				// we want to check time for detailed cache, but data are older than 3 hours
				return false;
			}

			if (checkTime == true && detailed == false && dataUpdated < (System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000))) {
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
		init();

		if (guid == null || guid.length() == 0) {
			return null;
		}

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
		init();

		if (geocode == null || geocode.length() == 0) {
			return null;
		}

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
		if (cache.detailed == true) {
			values.put("detailed", 1);
		} else {
			values.put("detailed", 0);
		}
		values.put("detailedupdate", cache.detailedUpdate);
		values.put("visiteddate", cache.visitedDate);
		values.put("geocode", cache.geocode);
		values.put("cacheid", cache.cacheid);
		values.put("guid", cache.guid);
		values.put("type", cache.type);
		values.put("name", cache.name);
		if (cache.own == true) {
			values.put("own", 1);
		} else {
			values.put("own", 0);
		}
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
		final boolean rel = isReliableLatLon(cache.geocode, cache.guid);
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
		values.put("description", cache.description);
		values.put("favourite_cnt", cache.favouriteCnt);
		values.put("rating", cache.rating);
		values.put("votes", cache.votes);
		values.put("myvote", cache.myVote);
		if (cache.disabled == true) {
			values.put("disabled", 1);
		} else {
			values.put("disabled", 0);
		}
		if (cache.archived == true) {
			values.put("archived", 1);
		} else {
			values.put("archived", 0);
		}
		if (cache.members == true) {
			values.put("members", 1);
		} else {
			values.put("members", 0);
		}
		if (cache.found == true) {
			values.put("found", 1);
		} else {
			values.put("found", 0);
		}
		if (cache.favourite == true) {
			values.put("favourite", 1);
		} else {
			values.put("favourite", 0);
		}
		values.put("inventoryunknown", cache.inventoryItems);

		boolean status = false;
		boolean statusOk = true;

		if (cache.attributes != null) {
			status = saveAttributes(cache.geocode, cache.attributes);
			if (status == false) {
				statusOk = false;
			}
		}

		if (cache.waypoints != null) {
			status = saveWaypoints(cache.geocode, cache.waypoints, true);
			if (status == false) {
				statusOk = false;
			}
		}

		if (cache.spoilers != null) {
			status = saveSpoilers(cache.geocode, cache.spoilers);
			if (status == false) {
				statusOk = false;
			}
		}

		if (cache.logs != null) {
			status = saveLogs(cache.geocode, cache.logs);
			if (status == false) {
				statusOk = false;
			}
		}

		if (cache.logCounts != null && cache.logCounts.isEmpty() == false) {
			status = saveLogCount(cache.geocode, cache.logCounts);
			if (status == false) {
				statusOk = false;
			}
		}

		if (cache.inventory != null) {
			status = saveInventory(cache.geocode, cache.inventory);
			if (status == false) {
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
			int rows = databaseRW.update(dbTableCaches, values, "geocode = \"" + cache.geocode + "\"", null);
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
			databaseRW.delete(dbTableAttributes, "geocode = \"" + geocode + "\"", null);

			if (!attributes.isEmpty()) {
				ContentValues values = new ContentValues();
				for (String oneAttribute : attributes) {
					values.clear();
					values.put("geocode", geocode);
					values.put("updated", System.currentTimeMillis());
					values.put("attribute", oneAttribute);

					databaseRW.insert(dbTableAttributes, null, values);
				}
			}
			databaseRW.setTransactionSuccessful();
		} finally {
			databaseRW.endTransaction();
		}

		return true;
	}

	public boolean saveWaypoints(String geocode, ArrayList<cgWaypoint> waypoints, boolean drop) {
		init();

		if (geocode == null || geocode.length() == 0 || waypoints == null) {
			return false;
		}

		boolean ok = false;
		databaseRW.beginTransaction();
		try {
			if (drop == true) {
				databaseRW.delete(dbTableWaypoints, "geocode = \"" + geocode + "\" and type <> \"own\"", null);
			}

			if (!waypoints.isEmpty()) {
				ContentValues values = new ContentValues();
				for (cgWaypoint oneWaypoint : waypoints) {
					if (oneWaypoint.type.equalsIgnoreCase("own") == true) {
						continue;
					}

					values.clear();
					values.put("geocode", geocode);
					values.put("updated", System.currentTimeMillis());
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

	public boolean saveSpoilers(String geocode, ArrayList<cgSpoiler> spoilers) {
		init();

		if (geocode == null || geocode.length() == 0 || spoilers == null) {
			return false;
		}

		databaseRW.beginTransaction();
		try {
			databaseRW.delete(dbTableSpoilers, "geocode = \"" + geocode + "\"", null);

			if (!spoilers.isEmpty()) {
				ContentValues values = new ContentValues();
				for (cgSpoiler oneSpoiler : spoilers) {
					values.clear();
					values.put("geocode", geocode);
					values.put("updated", System.currentTimeMillis());
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
			if (drop == true) {
				databaseRW.delete(dbTableLogs, "geocode = \"" + geocode + "\"", null);
			}

			if (!logs.isEmpty()) {
				ContentValues values = new ContentValues();
				for (cgLog oneLog : logs) {
					values.clear();
					values.put("geocode", geocode);
					values.put("updated", System.currentTimeMillis());
					values.put("type", oneLog.type);
					values.put("author", oneLog.author);
					values.put("log", oneLog.log);
					values.put("date", oneLog.date);
					values.put("found", oneLog.found);

					databaseRW.insert(dbTableLogs, null, values);
				}
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
			if (drop == true) {
				databaseRW.delete(dbTableLogCount, "geocode = \"" + geocode + "\"", null);
			}

			ContentValues values = new ContentValues();

			Set<Entry<Integer, Integer>> logCountsItems = logCounts.entrySet();
			for (Entry<Integer, Integer> pair : logCountsItems) {
				values.clear();
				values.put("geocode", geocode);
				values.put("updated", System.currentTimeMillis());
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
				databaseRW.delete(dbTableTrackables, "geocode = \"" + geocode + "\"", null);
			}

			if (!trackables.isEmpty()) {
				ContentValues values = new ContentValues();
				for (cgTrackable oneTrackable : trackables) {
					values.clear();
					if (geocode != null) {
						values.put("geocode", geocode);
					}
					values.put("updated", System.currentTimeMillis());
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

		ArrayList<cgCache> caches = loadCaches(geocodes, guids, null, null, null, null, loadA, loadW, loadS, loadL, loadI, loadO);
		if (caches != null && caches.isEmpty() == false) {
			return caches.get(0);
		}

		return null;
	}

	public ArrayList<cgCache> loadCaches(Object[] geocodes, Object[] guids) {
		return loadCaches(geocodes, guids, null, null, null, null, false, true, false, false, false, false);
	}

	public ArrayList<cgCache> loadCaches(Object[] geocodes, Object[] guids, boolean lite) {
		if (lite == true) {
			return loadCaches(geocodes, guids, null, null, null, null, false, true, false, false, false, false);
		} else {
			return loadCaches(geocodes, guids, null, null, null, null, true, true, true, true, true, true);
		}
	}

	public ArrayList<cgCache> loadCaches(Object[] geocodes, Object[] guids, Long centerLat, Long centerLon, Long spanLat, Long spanLon, boolean loadA, boolean loadW, boolean loadS, boolean loadL, boolean loadI, boolean loadO) {
		init();

		StringBuilder where = new StringBuilder();
		Cursor cursor = null;
		ArrayList<cgCache> caches = new ArrayList<cgCache>();

		try {
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
					new String[]{
						"_id", "updated", "reason", "detailed", "detailedupdate", "visiteddate", "geocode", "cacheid", "guid", "type", "name", "own", "owner", "owner_real", "hidden", "hint", "size",
						"difficulty", "distance", "direction", "terrain", "latlon", "latitude_string", "longitude_string", "location", "latitude", "longitude", "elevation", "shortdesc",
						"description", "favourite_cnt", "rating", "votes", "myvote", "disabled", "archived", "members", "found", "favourite", "inventorycoins", "inventorytags",
						"inventoryunknown"
					},
					where.toString(),
					null,
					null,
					null,
					null,
					null);

			if (cursor != null) {
				int index = 0;

				if (cursor.getCount() > 0) {
					cursor.moveToFirst();

					do {
						cgCache cache = new cgCache();

						cache.updated = (long) cursor.getLong(cursor.getColumnIndex("updated"));
						cache.reason = (int) cursor.getInt(cursor.getColumnIndex("reason"));
						index = cursor.getColumnIndex("detailed");
						if ((int) cursor.getInt(index) == 1) {
							cache.detailed = true;
						} else {
							cache.detailed = false;
						}
						cache.detailedUpdate = (Long) cursor.getLong(cursor.getColumnIndex("detailedupdate"));
						cache.visitedDate = (Long) cursor.getLong(cursor.getColumnIndex("visiteddate"));
						cache.geocode = (String) cursor.getString(cursor.getColumnIndex("geocode"));
						cache.cacheid = (String) cursor.getString(cursor.getColumnIndex("cacheid"));
						cache.guid = (String) cursor.getString(cursor.getColumnIndex("guid"));
						cache.type = (String) cursor.getString(cursor.getColumnIndex("type"));
						cache.name = (String) cursor.getString(cursor.getColumnIndex("name"));
						index = cursor.getColumnIndex("own");
						if ((int) cursor.getInt(index) == 1) {
							cache.own = true;
						} else {
							cache.own = false;
						}
						cache.owner = (String) cursor.getString(cursor.getColumnIndex("owner"));
						cache.ownerReal = (String) cursor.getString(cursor.getColumnIndex("owner_real"));
						cache.hidden = new Date((long) cursor.getLong(cursor.getColumnIndex("hidden")));
						cache.hint = (String) cursor.getString(cursor.getColumnIndex("hint"));
						cache.size = (String) cursor.getString(cursor.getColumnIndex("size"));
						cache.difficulty = (Float) cursor.getFloat(cursor.getColumnIndex("difficulty"));
						index = cursor.getColumnIndex("direction");
						if (cursor.isNull(index) == true) {
							cache.direction = null;
						} else {
							cache.direction = (Double) cursor.getDouble(index);
						}
						index = cursor.getColumnIndex("distance");
						if (cursor.isNull(index) == true) {
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
						if (cursor.isNull(index) == true) {
							cache.latitude = null;
						} else {
							cache.latitude = (Double) cursor.getDouble(index);
						}
						index = cursor.getColumnIndex("longitude");
						if (cursor.isNull(index) == true) {
							cache.longitude = null;
						} else {
							cache.longitude = (Double) cursor.getDouble(index);
						}
						index = cursor.getColumnIndex("elevation");
						if (cursor.isNull(index) == true) {
							cache.elevation = null;
						} else {
							cache.elevation = (Double) cursor.getDouble(index);
						}
						cache.shortdesc = (String) cursor.getString(cursor.getColumnIndex("shortdesc"));
						cache.description = (String) cursor.getString(cursor.getColumnIndex("description"));
						cache.favouriteCnt = (Integer) cursor.getInt(cursor.getColumnIndex("favourite_cnt"));
						cache.rating = (Float) cursor.getFloat(cursor.getColumnIndex("rating"));
						cache.votes = (Integer) cursor.getInt(cursor.getColumnIndex("votes"));
						cache.myVote = (Float) cursor.getFloat(cursor.getColumnIndex("myvote"));
						index = cursor.getColumnIndex("disabled");
						if ((int) cursor.getLong(index) == 1) {
							cache.disabled = true;
						} else {
							cache.disabled = false;
						}
						index = cursor.getColumnIndex("archived");
						if ((int) cursor.getLong(index) == 1) {
							cache.archived = true;
						} else {
							cache.archived = false;
						}
						index = cursor.getColumnIndex("members");
						if ((int) cursor.getLong(index) == 1) {
							cache.members = true;
						} else {
							cache.members = false;
						}
						index = cursor.getColumnIndex("found");
						if ((int) cursor.getLong(index) == 1) {
							cache.found = true;
						} else {
							cache.found = false;
						}
						index = cursor.getColumnIndex("favourite");
						if ((int) cursor.getLong(index) == 1) {
							cache.favourite = true;
						} else {
							cache.favourite = false;
						}
						cache.inventoryItems = (Integer) cursor.getInt(cursor.getColumnIndex("inventoryunknown"));

						if (loadA == true) {
							ArrayList<String> attributes = loadAttributes(cache.geocode);
							if (attributes != null && attributes.isEmpty() == false) {
								if (cache.attributes == null)
									cache.attributes = new ArrayList<String>();
								else
									cache.attributes.clear();
								cache.attributes.addAll(attributes);
							}
						}

						if (loadW == true) {
							ArrayList<cgWaypoint> waypoints = loadWaypoints(cache.geocode);
							if (waypoints != null && waypoints.isEmpty() == false) {
								if (cache.waypoints == null)
									cache.waypoints = new ArrayList<cgWaypoint>();
								else
									cache.waypoints.clear();
								cache.waypoints.addAll(waypoints);
							}
						}

						if (loadS == true) {
							ArrayList<cgSpoiler> spoilers = loadSpoilers(cache.geocode);
							if (spoilers != null && spoilers.isEmpty() == false) {
								if (cache.spoilers == null)
									cache.spoilers = new ArrayList<cgSpoiler>();
								else
									cache.spoilers.clear();
								cache.spoilers.addAll(spoilers);
							}
						}

						if (loadL == true) {
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

						if (loadI == true) {
							ArrayList<cgTrackable> inventory = loadInventory(cache.geocode);
							if (inventory != null && inventory.isEmpty() == false) {
								if (cache.inventory == null)
									cache.inventory = new ArrayList<cgTrackable>();
								else
									cache.inventory.clear();
								cache.inventory.addAll(inventory);
							}
						}

						if (loadO == true) {
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

	public ArrayList<String> loadAttributes(String geocode) {
		init();

		if (geocode == null || geocode.length() == 0) {
			return null;
		}

		Cursor cursor = null;
		ArrayList<String> attributes = new ArrayList<String>();

		cursor = databaseRO.query(
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
		init();

		if (id == null || id == 0) {
			return null;
		}

		Cursor cursor = null;
		cgWaypoint waypoint = new cgWaypoint();

		cursor = databaseRO.query(
				dbTableWaypoints,
				new String[]{"_id", "geocode", "updated", "type", "prefix", "lookup", "name", "latlon", "latitude_string", "longitude_string", "latitude", "longitude", "note"},
				"_id = " + id,
				null,
				null,
				null,
				null,
				"100");

		if (cursor != null && cursor.getCount() > 0) {
			int index;
			cursor.moveToFirst();

			waypoint.id = (int) cursor.getInt(cursor.getColumnIndex("_id"));
			waypoint.geocode = (String) cursor.getString(cursor.getColumnIndex("geocode"));
			waypoint.type = (String) cursor.getString(cursor.getColumnIndex("type"));
			waypoint.prefix = (String) cursor.getString(cursor.getColumnIndex("prefix"));
			waypoint.lookup = (String) cursor.getString(cursor.getColumnIndex("lookup"));
			waypoint.name = (String) cursor.getString(cursor.getColumnIndex("name"));
			waypoint.latlon = (String) cursor.getString(cursor.getColumnIndex("latlon"));
			waypoint.latitudeString = (String) cursor.getString(cursor.getColumnIndex("latitude_string"));
			waypoint.longitudeString = (String) cursor.getString(cursor.getColumnIndex("longitude_string"));
			index = cursor.getColumnIndex("latitude");
			if (cursor.isNull(index) == true) {
				waypoint.latitude = null;
			} else {
				waypoint.latitude = (Double) cursor.getDouble(index);
			}
			index = cursor.getColumnIndex("longitude");
			if (cursor.isNull(index) == true) {
				waypoint.longitude = null;
			} else {
				waypoint.longitude = (Double) cursor.getDouble(index);
			}
			waypoint.note = (String) cursor.getString(cursor.getColumnIndex("note"));
		}

		if (cursor != null) {
			cursor.close();
		}

		return waypoint;
	}

	public ArrayList<cgWaypoint> loadWaypoints(String geocode) {
		init();

		if (geocode == null || geocode.length() == 0) {
			return null;
		}

		Cursor cursor = null;
		ArrayList<cgWaypoint> waypoints = new ArrayList<cgWaypoint>();

		cursor = databaseRO.query(
				dbTableWaypoints,
				new String[]{"_id", "geocode", "updated", "type", "prefix", "lookup", "name", "latlon", "latitude_string", "longitude_string", "latitude", "longitude", "note"},
				"geocode = \"" + geocode + "\"",
				null,
				null,
				null,
				null,
				"100");

		if (cursor != null && cursor.getCount() > 0) {
			int index;
			cursor.moveToFirst();

			do {
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
				index = cursor.getColumnIndex("latitude");
				if (cursor.isNull(index) == true) {
					waypoint.latitude = null;
				} else {
					waypoint.latitude = (Double) cursor.getDouble(index);
				}
				index = cursor.getColumnIndex("longitude");
				if (cursor.isNull(index) == true) {
					waypoint.longitude = null;
				} else {
					waypoint.longitude = (Double) cursor.getDouble(index);
				}
				waypoint.note = (String) cursor.getString(cursor.getColumnIndex("note"));

				waypoints.add(waypoint);
			} while (cursor.moveToNext());
		}

		if (cursor != null) {
			cursor.close();
		}

		return waypoints;
	}

	public ArrayList<cgSpoiler> loadSpoilers(String geocode) {
		init();

		if (geocode == null || geocode.length() == 0) {
			return null;
		}

		Cursor cursor = null;
		ArrayList<cgSpoiler> spoilers = new ArrayList<cgSpoiler>();

		cursor = databaseRO.query(
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
				cgSpoiler spoiler = new cgSpoiler();
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

	public ArrayList<cgLog> loadLogs(String geocode) {
		init();

		if (geocode == null || geocode.length() == 0) {
			return null;
		}

		Cursor cursor = null;
		ArrayList<cgLog> logs = new ArrayList<cgLog>();

		cursor = databaseRO.query(
				dbTableLogs,
				new String[]{"_id", "type", "author", "log", "date", "found"},
				"geocode = \"" + geocode + "\"",
				null,
				null,
				null,
				"date desc, _id asc",
				"100");

		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();

			do {
				cgLog log = new cgLog();
				log.id = (int) cursor.getInt(cursor.getColumnIndex("_id"));
				log.type = (int) cursor.getInt(cursor.getColumnIndex("type"));
				log.author = (String) cursor.getString(cursor.getColumnIndex("author"));
				log.log = (String) cursor.getString(cursor.getColumnIndex("log"));
				log.date = (long) cursor.getLong(cursor.getColumnIndex("date"));
				log.found = (int) cursor.getInt(cursor.getColumnIndex("found"));

				logs.add(log);
			} while (cursor.moveToNext());
		}

		if (cursor != null) {
			cursor.close();
		}

		return logs;
	}

	public HashMap<Integer, Integer> loadLogCounts(String geocode) {
		init();

		if (geocode == null || geocode.length() == 0) {
			return null;
		}

		Cursor cursor = null;
		HashMap<Integer, Integer> logCounts = new HashMap<Integer, Integer>();

		cursor = databaseRO.query(
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
				Integer type = new Integer(cursor.getInt(cursor.getColumnIndex("type")));
				Integer count = new Integer(cursor.getInt(cursor.getColumnIndex("count")));

				logCounts.put(type, count);
			} while (cursor.moveToNext());
		}

		if (cursor != null) {
			cursor.close();
		}

		return logCounts;
	}

	public ArrayList<cgTrackable> loadInventory(String geocode) {
		init();

		if (geocode == null || geocode.length() == 0) {
			return null;
		}

		Cursor cursor = null;
		ArrayList<cgTrackable> trackables = new ArrayList<cgTrackable>();

		cursor = databaseRO.query(
				dbTableTrackables,
				new String[]{"_id", "updated", "tbcode", "guid", "title", "owner", "released", "goal", "description"},
				"geocode = \"" + geocode + "\"",
				null,
				null,
				null,
				null,
				"100");

		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();

			do {
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

				trackables.add(trackable);
			} while (cursor.moveToNext());
		}

		if (cursor != null) {
			cursor.close();
		}

		return trackables;
	}

	public cgTrackable loadTrackable(String geocode) {
		init();

		if (geocode == null || geocode.length() == 0) {
			return null;
		}

		Cursor cursor = null;
		cgTrackable trackable = new cgTrackable();

		cursor = databaseRO.query(
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
		}

		if (cursor != null) {
			cursor.close();
		}

		return trackable;
	}

	public int getAllStoredCachesCount(boolean detailedOnly, String cachetype, Integer list) {
		int count = 0;

		String listSql = null;
		String listSqlW = null;
		if (list == null) {
			listSql = " where reason >= 1";
			listSqlW = " and reason >= 1";
		} else if (list >= 1) {
			listSql = " where reason = " + list;
			listSqlW = " and reason = " + list;
		} else {
			return count;
		}

		try {
			if (detailedOnly == false) {
				if (cachetype == null) {
					SQLiteStatement sqlCount = databaseRO.compileStatement("select count(_id) from " + dbTableCaches + listSql);
					count = (int) sqlCount.simpleQueryForLong();
				} else {
					SQLiteStatement sqlCount = databaseRO.compileStatement("select count(_id) from " + dbTableCaches + " where type = \"" + cachetype + "\"" + listSqlW);
					count = (int) sqlCount.simpleQueryForLong();
				}
			} else {
				if (cachetype == null) {
					SQLiteStatement sqlCount = databaseRO.compileStatement("select count(_id) from " + dbTableCaches + " where detailed = 1" + listSqlW);
					count = (int) sqlCount.simpleQueryForLong();
				} else {
					SQLiteStatement sqlCount = databaseRO.compileStatement("select count(_id) from " + dbTableCaches + " where detailed = 1 and type = \"" + cachetype + "\"" + listSqlW);
					count = (int) sqlCount.simpleQueryForLong();
				}
			}
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

		Cursor cursor = null;
		ArrayList<String> geocodes = new ArrayList<String>();

		StringBuilder specifySql = new StringBuilder();

		specifySql.append("reason = ");
		specifySql.append(list);

		if (detailedOnly == true) {
			if (specifySql.length() > 0) {
				specifySql.append(" and ");
			}

			specifySql.append("detailed = 1");
		}

		if (cachetype != null) {
			if (specifySql.length() > 0) {
				specifySql.append(" and ");
			}

			specifySql.append("type = \"");
			specifySql.append(cachetype);
			specifySql.append("\"");
		}

		try {
			cursor = databaseRO.query(
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

		Cursor cursor = null;
		ArrayList<String> geocodes = new ArrayList<String>();

		StringBuilder specifySql = new StringBuilder();
		if (detailedOnly == true) {
			specifySql.append(" and detailed = 1");
		}
		if (cachetype != null) {
			specifySql.append(" and type = \"");
			specifySql.append(cachetype);
			specifySql.append("\"");
		}

		try {
			cursor = databaseRO.query(
					dbTableCaches,
					new String[]{"_id", "geocode"},
					"visiteddate > 0" + specifySql.toString(),
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

		Cursor cursor = null;
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
			cursor = databaseRO.query(
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

		Cursor cursor = null;
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
			cursor = databaseRO.query(
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
		init();

		if (geocode == null || geocode.length() == 0) {
			return;
		}

		if (listId <= 0) {
			listId = 1;
		}

		ContentValues values = new ContentValues();
		values.put("reason", listId);
		databaseRW.update(dbTableCaches, values, "geocode = \"" + geocode + "\" and reason < 1", null);
	}

	public boolean markDropped(String geocode) {
		init();

		if (geocode == null || geocode.length() == 0) {
			return false;
		}

		try {
			ContentValues values = new ContentValues();
			values.put("reason", 0);
			int rows = databaseRW.update(dbTableCaches, values, "geocode = \"" + geocode + "\"", null);

			if (rows > 0) {
				return true;
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.markDropped: " + e.toString());
		}

		return false;
	}

	public boolean markFound(String geocode) {
		init();

		if (geocode == null || geocode.length() == 0) {
			return false;
		}

		try {
			ContentValues values = new ContentValues();
			values.put("found", 1);
			int rows = databaseRW.update(dbTableCaches, values, "geocode = \"" + geocode + "\"", null);

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

				databaseRW.execSQL("delete from " + dbTableCaches + " where geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ")");
				databaseRW.execSQL("delete from " + dbTableAttributes + " where geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ")");
				databaseRW.execSQL("delete from " + dbTableSpoilers + " where geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ")");
				databaseRW.execSQL("delete from " + dbTableLogs + " where geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ")");
				databaseRW.execSQL("delete from " + dbTableLogCount + " where geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ")");
				databaseRW.execSQL("delete from " + dbTableLogsOffline + " where geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ")");
				databaseRW.execSQL("delete from " + dbTableWaypoints + " where geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ") and type <> \"own\"");
				databaseRW.execSQL("delete from " + dbTableTrackables + " where geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ")");

				geocodes.clear();
			}

			databaseRW.execSQL("delete from " + dbTableCaches + " where geocode = \"\"");

			final SQLiteStatement countSql = databaseRO.compileStatement("select count(_id) from " + dbTableCaches + " where reason = 0");
			final int count = (int) countSql.simpleQueryForLong();
			Log.d(cgSettings.tag, "Database clean: " + count + " cached geocaches remaining");
		} catch (Exception e) {
			Log.w(cgSettings.tag, "cgData.clean: " + e.toString());
		}

		Log.d(cgSettings.tag, "Database clean: finished");
	}

	public void dropStored(int listId) {
		init();

		Cursor cursor = null;
		ArrayList<String> geocodes = new ArrayList<String>();

		try {
			cursor = databaseRO.query(
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
				databaseRW.execSQL("delete from " + dbTableCaches + " where geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ")");
				databaseRW.execSQL("delete from " + dbTableAttributes + " where geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ")");
				databaseRW.execSQL("delete from " + dbTableSpoilers + " where geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ")");
				databaseRW.execSQL("delete from " + dbTableLogs + " where geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ")");
				databaseRW.execSQL("delete from " + dbTableLogCount + " where geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ")");
				databaseRW.execSQL("delete from " + dbTableLogsOffline + " where geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ")");
				databaseRW.execSQL("delete from " + dbTableWaypoints + " where geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ") and type <> \"own\"");
				databaseRW.execSQL("delete from " + dbTableTrackables + " where geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ")");

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
			if (hasLogOffline(geocode) == true) {
				final int rows = databaseRW.update(dbTableLogsOffline, values, "geocode = \"" + geocode + "\"", null);

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
		init();

		if (geocode == null || geocode.length() == 0) {
			return null;
		}

		Cursor cursor = null;
		cgLog log = null;

		cursor = databaseRO.query(
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
		init();

		if (geocode == null || geocode.length() == 0) {
			return;
		}

		databaseRW.delete(dbTableLogsOffline, "geocode = \"" + geocode + "\"", null);
	}

	public boolean hasLogOffline(String geocode) {
		if (geocode == null || geocode.length() == 0) {
			return false;
		}

		init();

		try {
			final SQLiteStatement countSql = databaseRO.compileStatement("select count(_id) from " + dbTableLogsOffline + " where geocode = \"" + geocode.toUpperCase() + "\"");
			final int count = (int) countSql.simpleQueryForLong();

			if (count > 0) {
				return true;
			}

			countSql.close();
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.hasLogOffline: " + e.toString());
		}

		return false;
	}

	public void saveVisitDate(String geocode) {
		if (geocode == null || geocode.length() == 0) {
			return;
		}

		ContentValues values = new ContentValues();
		values.put("visiteddate", System.currentTimeMillis());

		try {
			databaseRW.update(dbTableCaches, values, "geocode = \"" + geocode + "\"", null);
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.saveVisitDate: " + e.toString());
		}
	}

	public ArrayList<cgList> getLists(Resources res) {
		init();

		Cursor cursor = null;
		ArrayList<cgList> lists = new ArrayList<cgList>();

		lists.add(new cgList(true, 1, res.getString(R.string.list_inbox)));
		// lists.add(new cgList(true, 2, res.getString(R.string.list_wpt)));

		try {
			cursor = databaseRO.query(
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

			Cursor cursor = null;

			try {
				cursor = databaseRO.query(
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
		init();

		int id = -1;
		if (name == null || name.length() == 0) {
			return id;
		}

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
		init();

		boolean status = false;
		if (id < 10) {
			return status;
		}

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
			databaseRW.update(dbTableCaches, values, "geocode = \"" + geocode + "\"", null);

			databaseRW.setTransactionSuccessful();
		} finally {
			databaseRW.endTransaction();
		}
	}

	public boolean status() {
		if (databaseRO == null || databaseRW == null || initialized == false) {
			return false;
		}

		return true;
	}
}
