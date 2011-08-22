package cgeo.geocaching;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.mapsforge.android.maps.MapDatabase;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Environment;
import android.util.Log;
import cgeo.geocaching.googlemaps.googleMapFactory;
import cgeo.geocaching.mapinterfaces.MapFactory;
import cgeo.geocaching.mapsforge.mfMapFactory;

public class cgSettings {

	private static final String KEY_WEB_DEVICE_CODE = "webDeviceCode";
	private static final String KEY_WEBDEVICE_NAME = "webDeviceName";
	private static final String KEY_MAP_LIVE = "maplive";
	private static final String KEY_MAP_SOURCE = "mapsource";
	private static final String KEY_USE_TWITTER = "twitter";
	private static final String KEY_SHOW_ADDRESS = "showaddress";
	private static final String KEY_SHOW_CAPTCHA = "showcaptcha";
	private static final String KEY_MAP_TRAIL = "maptrail";
	private static final String KEY_LAST_MAP_ZOOM = "mapzoom";
	private static final String KEY_LIVE_LIST = "livelist";
	private static final String KEY_METRIC_UNITS = "units";
	private static final String KEY_SKIN = "skin";
	private static final String KEY_LAST_USED_LIST = "lastlist";
	private static final String KEY_CACHE_TYPE = "cachetype";
	private static final String KEY_INITIALIZED = "initialized";
	private static final String KEY_TWITTER_TOKEN_SECRET = "tokensecret";
	private static final String KEY_TWITTER_TOKEN_PUBLIC = "tokenpublic";
	private static final String KEY_VERSION = "version";
	private static final String KEY_LOAD_DESCRIPTION = "autoloaddesc";
	private static final String KEY_USE_ENGLISH = "useenglish";
	private static final String KEY_AS_BROWSER = "asbrowser";
	private static final String KEY_USE_COMPASS = "usecompass";
	private static final String KEY_AUTO_VISIT_TRACKABLES = "trackautovisit";
	private static final String KEY_AUTO_INSERT_SIGNATURE = "sigautoinsert";
	private static final String KEY_ALTITUDE_CORRECTION = "altcorrection";
	private static final String KEY_USE_GOOGLE_NAVIGATION = "usegnav";
	private static final String KEY_STORE_LOG_IMAGES = "logimages";
	private static final String KEY_EXCLUDE_DISABLED = "excludedisabled";
	private static final String KEY_EXCLUDE_OWN = "excludemine";
	private static final String KEY_MAPFILE = "mfmapfile";
	private static final String KEY_SIGNATURE = "signature";
	private static final String KEY_GCVOTE_PASSWORD = "pass-vote";
	private static final String KEY_PASSWORD = "password";
	private static final String KEY_USERNAME = "username";
	private static final String KEY_COORD_INPUT_FORMAT = "coordinputformat";
	private static final String KEY_LOG_OFFLINE = "log_offline";
	private static final String KEY_LOAD_DIRECTION_IMG = "loaddirectionimg";

	private interface PrefRunnable {
		void edit(final Editor edit);
	}

	public enum mapSourceEnum {
		googleMap,
		googleSat,
		mapsforgeMapnik,
		mapsforgeOsmarender,
		mapsforgeCycle,
		mapsforgeOffline;

		static mapSourceEnum fromInt(int id) {
			mapSourceEnum[] values = mapSourceEnum.values();
			if (id >= 0 && id < values.length) {
				return values[id];
			} else {
				return googleMap;
			}
		}

		public boolean isGoogleMapSource() {
			if (googleMap == this || googleSat == this) {
				return true;
			}

			return false;
		}
	}

	public enum coordInputFormatEnum {
		Plain,
		Deg,
		Min,
		Sec;

		static coordInputFormatEnum fromInt(int id) {
			coordInputFormatEnum[] values = coordInputFormatEnum.values();
			if (id >= 0 && id < values.length) {
				return values[id];
			} else {
				return Min;
			}
		}
	}

	// constants
	public final static int unitsMetric = 1;
	public final static int unitsImperial = 2;
	public final static String cache = ".cgeo";

	// twitter api keys
	public final static String keyConsumerPublic = "RFafPiNi3xRhcS1TPE3wTw";
	public final static String keyConsumerSecret = "7iDJprNPI9hzRwWhpzycSr9SPZMFrdVdsxD2OauI9k";

	// version
	public int version = 0;

	// skin
	public int skin = 0;

	// settings
	public int helper = 0;
	public int initialized = 0;
	public int autoLoadDesc = 0;
	public int units = unitsMetric;
	public int livelist = 1;
	public int mapzoom = 14;
	public int maplive = 1;
	public int maptrail = 1;
	public boolean useEnglish = false;
	public boolean showCaptcha = false;
	public int excludeMine = 0;
	public int excludeDisabled = 0;
	public int storeOfflineMaps = 0;
	public boolean storelogimages = false;
	public int asBrowser = 1;
	public int useCompass = 1;
	public int useGNavigation = 1;
	public int showAddress = 1;
	public int publicLoc = 0;
	public int twitter = 0;
	public int altCorrection = 0;
	public String cacheType = null;
	public String tokenPublic = null;
	public String tokenSecret = null;
    public String webDeviceName = null;
    public String webDeviceCode = null;
    public boolean trackableAutovisit = false;
    public boolean signatureAutoinsert = false;

	// usable values
	public static final String tag = "c:geo";

	// preferences file
	public static final String preferences = "cgeo.pref";

	// private variables
	private Context context = null;
	private SharedPreferences prefs = null;
	private String username = null;
	private String password = null;
	private coordInputFormatEnum coordInput = coordInputFormatEnum.Plain;

	// maps
	public MapFactory mapFactory = null;
	public mapSourceEnum mapSourceUsed = mapSourceEnum.googleMap;
	public mapSourceEnum mapSource = mapSourceEnum.googleMap;
	private String mapFile = null;
	private boolean mapFileValid = false;

	public cgSettings(Context contextIn, SharedPreferences prefsIn) {
		context = contextIn;
		prefs = prefsIn;

		load();
	}

	public void load() {
		version = prefs.getInt(KEY_VERSION, 0);

		initialized = prefs.getInt(KEY_INITIALIZED, 0);
		helper = prefs.getInt("helper", 0);

		skin = prefs.getInt(KEY_SKIN, 0);

		autoLoadDesc = prefs.getInt(KEY_LOAD_DESCRIPTION, 0);
		units = prefs.getInt(KEY_METRIC_UNITS, 1);
		livelist = prefs.getInt(KEY_LIVE_LIST, 1);
		maplive = prefs.getInt(KEY_MAP_LIVE, 1);
		mapzoom = prefs.getInt(KEY_LAST_MAP_ZOOM, 14);
		maptrail = prefs.getInt(KEY_MAP_TRAIL, 1);
		useEnglish = prefs.getBoolean(KEY_USE_ENGLISH, false);
		showCaptcha = prefs.getBoolean(KEY_SHOW_CAPTCHA, false);
		excludeMine = prefs.getInt(KEY_EXCLUDE_OWN, 0);
		excludeDisabled = prefs.getInt(KEY_EXCLUDE_DISABLED, 0);
		storeOfflineMaps = prefs.getInt("offlinemaps", 1);
		storelogimages = prefs.getBoolean(KEY_STORE_LOG_IMAGES, false);
		asBrowser = prefs.getInt(KEY_AS_BROWSER, 1);
		useCompass = prefs.getInt(KEY_USE_COMPASS, 1);
		useGNavigation = prefs.getInt(KEY_USE_GOOGLE_NAVIGATION, 1);
		showAddress = prefs.getInt(KEY_SHOW_ADDRESS, 1);
		publicLoc = prefs.getInt("publicloc", 0);
		twitter = prefs.getInt(KEY_USE_TWITTER, 0);
		altCorrection = prefs.getInt(KEY_ALTITUDE_CORRECTION, 0);
		cacheType = prefs.getString(KEY_CACHE_TYPE, null);
		tokenPublic = prefs.getString(KEY_TWITTER_TOKEN_PUBLIC, null);
		tokenSecret = prefs.getString(KEY_TWITTER_TOKEN_SECRET, null);
		mapFile = prefs.getString(KEY_MAPFILE, null);
		mapFileValid = checkMapfile(mapFile);
		mapSource = mapSourceEnum.fromInt(prefs.getInt(KEY_MAP_SOURCE, 0));
		webDeviceName = prefs.getString(KEY_WEBDEVICE_NAME, null);
		webDeviceCode = prefs.getString(KEY_WEB_DEVICE_CODE, null);
		trackableAutovisit = prefs.getBoolean(KEY_AUTO_VISIT_TRACKABLES, false);
		signatureAutoinsert = prefs.getBoolean(KEY_AUTO_INSERT_SIGNATURE, false);
		coordInput = coordInputFormatEnum.fromInt(prefs.getInt(KEY_COORD_INPUT_FORMAT, 0));

		setLanguage(useEnglish);
	}

	public void setSkin(int skinIn) {
		if (skin == 1) {
			skin = 1;
		} else {
			skin = 0;
		}
	}

	public void setLanguage(boolean useEnglish) {
		Locale locale = Locale.getDefault();
		if (useEnglish) {
			locale = new Locale("en");
		}
	    Configuration config = new Configuration();
		config.locale = locale;
		context.getResources().updateConfiguration(config,
		context.getResources().getDisplayMetrics());
	}

	public void reloadTwitterTokens() {
		tokenPublic = prefs.getString(KEY_TWITTER_TOKEN_PUBLIC, null);
		tokenSecret = prefs.getString(KEY_TWITTER_TOKEN_SECRET, null);
	}

	public void reloadCacheType() {
		cacheType = prefs.getString(KEY_CACHE_TYPE, null);
	}

	public static String getStorage() {
		return getStorageSpecific(null)[0];
	}

	public static String getStorageSec() {
		return getStorageSpecific(null)[1];
	}

	public static String[] getStorageSpecific(Boolean hidden) {
		String external = Environment.getExternalStorageDirectory() + "/" + cache + "/";
		String data = Environment.getDataDirectory() + "/data/cgeo.geocaching/" + cache + "/";

		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			return new String[] {external, data};
		} else {
			return new String[] {data, external};
		}
	}

	public boolean isLogin() {
		final String preUsername = prefs.getString(KEY_USERNAME, null);
		final String prePassword = prefs.getString(KEY_PASSWORD, null);

		if (preUsername == null || prePassword == null || preUsername.length() == 0 || prePassword.length() == 0) {
			return false;
		} else {
			return true;
		}
	}

	public HashMap<String, String> getLogin() {
		final HashMap<String, String> login = new HashMap<String, String>();

		if (username == null || password == null) {
			final String preUsername = prefs.getString(KEY_USERNAME, null);
			final String prePassword = prefs.getString(KEY_PASSWORD, null);

			if (initialized == 0 && (preUsername == null || prePassword == null)) {
				Intent initIntent = new Intent(context, cgeoinit.class);
				context.startActivity(initIntent);

				final SharedPreferences.Editor prefsEdit = prefs.edit();
				prefsEdit.putInt(KEY_INITIALIZED, 1);
				prefsEdit.commit();

				initialized = 1;

				return null;
			} else if (initialized == 1 && (preUsername == null || prePassword == null)) {
				return null;
			}

			login.put(KEY_USERNAME, preUsername);
			login.put(KEY_PASSWORD, prePassword);

			username = preUsername;
			password = prePassword;
		} else {
			login.put(KEY_USERNAME, username);
			login.put(KEY_PASSWORD, password);
		}

		return login;
	}

	public String getUsername() {
		if (username == null) {
			return prefs.getString(KEY_USERNAME, null);
		} else {
			return username;
		}
	}

	public boolean setLogin(final String username, final String password) {
		this.username = username;
		this.password = password;
		return editSettings(new PrefRunnable() {

			@Override
			public void edit(Editor edit) {
				if (username == null || username.length() == 0 || password == null || password.length() == 0) {
					// erase username and password
					edit.remove(KEY_USERNAME);
					edit.remove(KEY_PASSWORD);
				} else {
					// save username and password
					edit.putString(KEY_USERNAME, username);
					edit.putString(KEY_PASSWORD, password);
				}
			}
		});
	}

	public boolean isGCvoteLogin() {
		final String preUsername = prefs.getString(KEY_USERNAME, null);
		final String prePassword = prefs.getString(KEY_GCVOTE_PASSWORD, null);

		if (preUsername == null || prePassword == null || preUsername.length() == 0 || prePassword.length() == 0) {
			return false;
		} else {
			return true;
		}
	}

	public boolean setGCvoteLogin(final String password) {
		return editSettings(new PrefRunnable() {

			@Override
			public void edit(Editor edit) {
				if (password == null || password.length() == 0) {
					// erase password
					edit.remove(KEY_GCVOTE_PASSWORD);
				} else {
					// save password
					edit.putString(KEY_GCVOTE_PASSWORD, password);
				}
			}
		});
	}

	public HashMap<String, String> getGCvoteLogin() {
		final HashMap<String, String> login = new HashMap<String, String>();

		if (username == null || password == null) {
			final String preUsername = prefs.getString(KEY_USERNAME, null);
			final String prePassword = prefs.getString(KEY_GCVOTE_PASSWORD, null);

			if (preUsername == null || prePassword == null) {
				return null;
			}

			login.put(KEY_USERNAME, preUsername);
			login.put(KEY_PASSWORD, prePassword);

			username = preUsername;
		} else {
			login.put(KEY_USERNAME, username);
			login.put(KEY_PASSWORD, password);
		}

		return login;
	}

	public boolean setSignature(final String signature) {
		return editSettings(new PrefRunnable() {

			@Override
			public void edit(Editor edit) {
				if (signature == null || signature.length() == 0) {
					// erase signature
					edit.remove(KEY_SIGNATURE);
				} else {
					// save signature
					edit.putString(KEY_SIGNATURE, signature);
				}
			}
		});
	}

	public String getSignature() {
		return prefs.getString(KEY_SIGNATURE, null);
	}

	public boolean setAltCorrection(final int altitude) {
		altCorrection = altitude;
		return editSettings(new PrefRunnable() {

			@Override
			public void edit(Editor edit) {
				edit.putInt(KEY_ALTITUDE_CORRECTION, altitude);
			}
		});
	}

	public void deleteCookies() {
		editSettings(new PrefRunnable() {

			@Override
			public void edit(Editor edit) {
				// delete cookies
				Map<String, ?> prefsValues = prefs.getAll();

				if (prefsValues != null && prefsValues.size() > 0) {
					Log.d(cgSettings.tag, "Removing cookies");

					for (String key : prefsValues.keySet()) {
						if (key.length() > 7 && key.substring(0, 7).equals("cookie_")) {
							edit.remove(key);
						}
					}
				}
			}
		});
	}

	public String setCacheType(final String cacheTypeIn) {
		editSettings(new PrefRunnable() {
			@Override
			public void edit(Editor edit) {
				edit.putString(KEY_CACHE_TYPE, cacheTypeIn);
			}
		});

		cacheType = cacheTypeIn;

		return cacheType;
	}

	public void liveMapEnable() {
		if (editSettings(new PrefRunnable() {

			@Override
			public void edit(Editor edit) {
				edit.putInt(KEY_MAP_LIVE, 1);
			}
		})) {
			maplive = 1;
		}
	}

	public void liveMapDisable() {
		if (editSettings(new PrefRunnable() {

			@Override
			public void edit(Editor edit) {
				edit.putInt(KEY_MAP_LIVE, 0);
			}
		})) {
			maplive = 0;
		}
	}

	public int getLastList() {
		int listId = prefs.getInt(KEY_LAST_USED_LIST, -1);

		return listId;
	}

	public void saveLastList(final int listId) {
		editSettings(new PrefRunnable() {

			@Override
			public void edit(Editor edit) {
				edit.putInt(KEY_LAST_USED_LIST, listId);
			}
		});
	}

	public void setWebNameCode(final String name, final String code) {
		if (editSettings(new PrefRunnable() {

			@Override
			public void edit(Editor edit) {

				edit.putString(KEY_WEBDEVICE_NAME, name);
				edit.putString(KEY_WEB_DEVICE_CODE, code);
			}
		})) {
			webDeviceCode=code;
			webDeviceName=name;
		}
	}

	public MapFactory getMapFactory() {
		if (mapSource.isGoogleMapSource()) {
			if (!mapSourceUsed.isGoogleMapSource() || mapFactory == null) {
				mapFactory = new googleMapFactory();
				mapSourceUsed = mapSource;
			}
		} else if (!mapSource.isGoogleMapSource()) {
			if (mapSourceUsed.isGoogleMapSource() || mapFactory == null) {
				mapFactory = new mfMapFactory();
				mapSourceUsed = mapSource;
			}
		}

		return mapFactory;
	}

	public String getMapFile() {
		return mapFile;
	}

	public boolean setMapFile(final String mapFileIn) {
		boolean commitResult = editSettings(new PrefRunnable() {

			@Override
			public void edit(Editor edit) {
				edit.putString(KEY_MAPFILE, mapFileIn);
			}
		});

		mapFile = mapFileIn;
		mapFileValid = checkMapfile(mapFile);

		return commitResult;
	}

	public boolean hasValidMapFile() {
		return mapFileValid;
	}

	private static boolean checkMapfile(String mapFileIn) {
		if (null == mapFileIn) {
			return false;
		}
		return MapDatabase.isValidMapFile(mapFileIn);
	}

	public coordInputFormatEnum getCoordInputFormat() {
		return coordInput;
	}

	public boolean setCoordInputFormat (coordInputFormatEnum format) {
		coordInput = format;
		boolean commitResult = editSettings(new PrefRunnable() {

			@Override
			public void edit(Editor edit) {
				edit.putInt(KEY_COORD_INPUT_FORMAT, coordInput.ordinal());
			}
		});
		return commitResult;
	}

	/**
	 * edit some settings without knowing how to get the settings editor or how to commit
	 * @param runnable
	 * @return commit result
	 */
	private boolean editSettings(final PrefRunnable runnable) {
		final SharedPreferences.Editor prefsEdit = prefs.edit();
		runnable.edit(prefsEdit);
		return prefsEdit.commit();
	}
	
	void setLogOffline(final boolean offline) {
		editSettings(new PrefRunnable() {

			@Override
			public void edit(Editor edit) {
				edit.putBoolean(KEY_LOG_OFFLINE, offline);
			}
		});
	}
	
	public boolean getLogOffline() {
		return prefs.getBoolean(KEY_LOG_OFFLINE, false);
	}

	void setLoadDirImg(final boolean value) {
        editSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putBoolean(KEY_LOAD_DIRECTION_IMG, value);
            }
        });
    }
    
    public boolean getLoadDirImg() {
        return prefs.getBoolean(KEY_LOAD_DIRECTION_IMG, true);
    }
}
