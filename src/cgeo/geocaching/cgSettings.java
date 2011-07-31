package cgeo.geocaching;

import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

import cgeo.geocaching.googlemaps.googleMapFactory;
import cgeo.geocaching.mapinterfaces.MapFactory;
import cgeo.geocaching.mapsforge.mfMapFactory;
import android.os.Environment;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.Log;

public class cgSettings {
	
	public enum mapSourceEnum {
		googleMap,
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
	}
	
	// constants
	public final static int unitsMetric = 1;
	public final static int unitsImperial = 2;
	public final static int mapSatellite = 1;
	public final static int mapClassic = 2;
	public final static String cache = ".cgeo";
	public final static String analytics = "UA-1103507-15";

	// twitter api keys
	public final static String keyConsumerPublic = "RFafPiNi3xRhcS1TPE3wTw";
	public final static String keyConsumerSecret = "7iDJprNPI9hzRwWhpzycSr9SPZMFrdVdsxD2OauI9k";

	// version
	public int version = 0;

	// skin
	public int skin = 0;
	public int buttonActive = R.drawable.action_button_dark;
	public int buttonInactive = R.drawable.action_button_dark_off;
	public int buttonPressed = R.drawable.action_button_dark_pressed;
	
	// settings
	public boolean loaded = false;
	public boolean hideMySearch = false;
	public int helper = 0;
	public int initialized = 0;
	public String languages = null;
	public int cachesFound = 0;
	public int autoLoadDesc = 0;
	public int units = unitsMetric;
	public int livelist = 1;
	public int maptype = mapSatellite;
	public int mapzoom = 14;
	public int maplive = 1;
	public int maptrail = 1;
	public boolean useEnglish = false;
	public boolean showCaptcha = false;
	public int excludeMine = 0;
	public int excludeDisabled = 0;
	public int storeOfflineMaps = 0;
	public int asBrowser = 1;
	public int useCompass = 1;
	public int useGNavigation = 1;
	public int showAddress = 1;
	public int publicLoc = 0;
	public int twitter = 0;
	public int altCorrection = 0;
	public String signature = null;
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
	private String passVote = null;
	
	// maps
	public static final int MAP_GOOGLE = 0;
	public static final int MAP_MF = 1;
	public MapFactory mapFactory = null;
	public mapSourceEnum mapProviderUsed = mapSourceEnum.googleMap;
	public mapSourceEnum mapProvider = mapSourceEnum.googleMap;
	public String mapFile = null;

	public cgSettings(Context contextIn, SharedPreferences prefsIn) {
		context = contextIn;
		prefs = prefsIn;

		load();
	}

	public void load() {
		version = prefs.getInt("version", 0);

		initialized = prefs.getInt("initialized", 0);
		helper = prefs.getInt("helper", 0);
		
		skin = prefs.getInt("skin", 0);
		setSkinDefaults();

		languages = prefs.getString("languages", null);
		cachesFound = prefs.getInt("found", 0);
		autoLoadDesc = prefs.getInt("autoloaddesc", 0);
		units = prefs.getInt("units", 1);
		livelist = prefs.getInt("livelist", 1);
		maptype = prefs.getInt("maptype", 1);
		maplive = prefs.getInt("maplive", 1);
		mapzoom = prefs.getInt("mapzoom", 14);
		maptrail = prefs.getInt("maptrail", 1);
		useEnglish = prefs.getBoolean("useenglish", false);
		showCaptcha = prefs.getBoolean("showcaptcha", false);
		excludeMine = prefs.getInt("excludemine", 0);
		excludeDisabled = prefs.getInt("excludedisabled", 0);
		storeOfflineMaps = prefs.getInt("offlinemaps", 1);
		asBrowser = prefs.getInt("asbrowser", 1);
		useCompass = prefs.getInt("usecompass", 1);
		useGNavigation = prefs.getInt("usegnav", 1);
		showAddress = prefs.getInt("showaddress", 1);
		publicLoc = prefs.getInt("publicloc", 0);
		twitter = prefs.getInt("twitter", 0);
		altCorrection = prefs.getInt("altcorrection", 0);
		signature = prefs.getString("signature", null);
		cacheType = prefs.getString("cachetype", null);
		tokenPublic = prefs.getString("tokenpublic", null);
		tokenSecret = prefs.getString("tokensecret", null);
		mapFile = prefs.getString("mfmapfile", null);
		mapProvider = mapSourceEnum.fromInt(prefs.getInt("mapsource", 0));
		webDeviceName = prefs.getString("webDeviceName", null);
		webDeviceCode = prefs.getString("webDeviceCode", null);
		trackableAutovisit = prefs.getBoolean("trackautovisit", false);
		signatureAutoinsert = prefs.getBoolean("sigautoinsert", false);
		
		setLanguage(useEnglish);
	}
	
	private void setSkinDefaults() {
		if (skin == 1) {
			buttonActive = R.drawable.action_button_light;
			buttonInactive = R.drawable.action_button_light_off;
			buttonPressed = R.drawable.action_button_light_pressed;
		} else {
			skin = 0;
			buttonActive = R.drawable.action_button_dark;
			buttonInactive = R.drawable.action_button_dark_off;
			buttonPressed = R.drawable.action_button_dark_pressed;
		}
	}

	public void setSkin(int skinIn) {
		if (skin == 1) {
			skin = 1;
			setSkinDefaults();
		} else {
			skin = 0;
			setSkinDefaults();
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
		tokenPublic = prefs.getString("tokenpublic", null);
		tokenSecret = prefs.getString("tokensecret", null);
	}

	public void reloadCacheType() {
		cacheType = prefs.getString("cachetype", null);
	}

	public String getStorage() {
		return getStorageSpecific(null)[0];
	}

	public String getStorageSec() {
		return getStorageSpecific(null)[1];
	}

	public String[] getStorageSpecific(Boolean hidden) {
		String[] storage = new String[2];

		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			storage[0] = Environment.getExternalStorageDirectory() + "/" + cache + "/";
			storage[1] = Environment.getDataDirectory() + "/data/cgeo.geocaching/" + cache + "/";
		} else {
			storage[0] = Environment.getDataDirectory() + "/data/cgeo.geocaching/" + cache + "/";
			storage[1] = Environment.getExternalStorageDirectory() + "/" + cache + "/";
		}

		return storage;
	}

	public boolean isLogin() {
		final String preUsername = prefs.getString("username", null);
		final String prePassword = prefs.getString("password", null);

		if (preUsername == null || prePassword == null || preUsername.length() == 0 || prePassword.length() == 0) {
			return false;
		} else {
			return true;
		}
	}

	public HashMap<String, String> getLogin() {
		final HashMap<String, String> login = new HashMap<String, String>();

		if (username == null || password == null) {
			final String preUsername = prefs.getString("username", null);
			final String prePassword = prefs.getString("password", null);

			if (initialized == 0 && (preUsername == null || prePassword == null)) {
				Intent initIntent = new Intent(context, cgeoinit.class);
				context.startActivity(initIntent);

				final SharedPreferences.Editor prefsEdit = prefs.edit();
				prefsEdit.putInt("initialized", 1);
				prefsEdit.commit();

				initialized = 1;

				return null;
			} else if (initialized == 1 && (preUsername == null || prePassword == null)) {
				return null;
			}

			login.put("username", preUsername);
			login.put("password", prePassword);

			username = preUsername;
			password = prePassword;
		} else {
			login.put("username", username);
			login.put("password", password);
		}

		return login;
	}

	public String getUsername() {
		String user = null;

		if (username == null) {
			user = prefs.getString("username", null);
		} else {
			user = username;
		}

		return user;
	}

	public boolean setLogin(String username, String password) {
		final SharedPreferences.Editor prefsEdit = prefs.edit();

		if (username == null || username.length() == 0 || password == null || password.length() == 0) {
			// erase username and password
			prefsEdit.remove("username");
			prefsEdit.remove("password");
		} else {
			// save username and password
			prefsEdit.putString("username", username);
			prefsEdit.putString("password", password);
		}

		this.username = username;
		this.password = password;
		
		return prefsEdit.commit();
	}

	public boolean isGCvoteLogin() {
		final String preUsername = prefs.getString("username", null);
		final String prePassword = prefs.getString("pass-vote", null);

		if (preUsername == null || prePassword == null || preUsername.length() == 0 || prePassword.length() == 0) {
			return false;
		} else {
			return true;
		}
	}

	public boolean setGCvoteLogin(String password) {
		final SharedPreferences.Editor prefsEdit = prefs.edit();

		if (password == null || password.length() == 0) {
			// erase password
			prefsEdit.remove("pass-vote");
		} else {
			// save password
			prefsEdit.putString("pass-vote", password);
		}

		passVote = password;

		return prefsEdit.commit();
	}

	public HashMap<String, String> getGCvoteLogin() {
		final HashMap<String, String> login = new HashMap<String, String>();

		if (username == null || password == null) {
			final String preUsername = prefs.getString("username", null);
			final String prePassword = prefs.getString("pass-vote", null);

			if (preUsername == null || prePassword == null) {
				return null;
			}

			login.put("username", preUsername);
			login.put("password", prePassword);

			username = preUsername;
			passVote = prePassword;
		} else {
			login.put("username", username);
			login.put("password", password);
		}

		return login;
	}

	public boolean setSignature(String signature) {
		final SharedPreferences.Editor prefsEdit = prefs.edit();

		if (signature == null || signature.length() == 0) {
			// erase signature
			prefsEdit.remove("signature");
		} else {
			// save signature
			prefsEdit.putString("signature", signature);
		}

		this.signature = signature;

		return prefsEdit.commit();
	}

	public String getSignature() {
		return prefs.getString("signature", null);
	}

	public boolean setLanguages(String languages) {
		final SharedPreferences.Editor prefsEdit = prefs.edit();

		if (languages == null || languages.length() == 0) {
			// erase languages
			prefsEdit.remove("languages");
		} else {
			// save langauges
			languages = languages.toLowerCase();
			languages = languages.replaceAll("([^a-z]+)", " ");
			languages = languages.replaceAll("([ ]+)", " ");

			prefsEdit.putString("languages", languages);
		}

		this.languages = languages;

		return prefsEdit.commit();
	}

	public boolean setAltCorrection(int altitude) {
		final SharedPreferences.Editor prefsEdit = prefs.edit();

		prefsEdit.putInt("altcorrection", altitude);

		altCorrection = altitude;

		return prefsEdit.commit();
	}

	public String getLanguages() {
		return prefs.getString("languages", null);
	}

	public void deleteCookies() {
		SharedPreferences.Editor prefsEdit = prefs.edit();

		// delete cookies
		Map<String, ?> prefsValues = prefs.getAll();

		if (prefsValues != null && prefsValues.size() > 0) {
			Log.i(cgSettings.tag, "Removing cookies");

			Object[] keys = prefsValues.keySet().toArray();

			for (int i = 0; i < keys.length; i++) {
				if (keys[i].toString().length() > 7 && keys[i].toString().substring(0, 7).equals("cookie_") == true) {
					prefsEdit.remove(keys[i].toString());
				}
			}
		}

		prefsEdit.commit();
	}

	public String setCacheType(String cacheTypeIn) {
		final SharedPreferences.Editor edit = prefs.edit();
		edit.putString("cachetype", cacheTypeIn);
		edit.commit();

		cacheType = cacheTypeIn;

		return cacheType;
	}

	public void liveMapEnable() {
		final SharedPreferences.Editor edit = prefs.edit();
		edit.putInt("maplive", 1);

		if (edit.commit() == true) {
			maplive = 1;
		}
	}

	public void liveMapDisable() {
		final SharedPreferences.Editor edit = prefs.edit();
		edit.putInt("maplive", 0);

		if (edit.commit() == true) {
			maplive = 0;
		}
	}
	
	public int getLastList() {
		int listId = prefs.getInt("lastlist", -1);
		
		return listId;
	}
	
	public void saveLastList(int listId) {
		final SharedPreferences.Editor edit = prefs.edit();
		
		edit.putInt("lastlist", listId);
		edit.commit();
	}

	public void setWebNameCode(String name, String code) {
	final SharedPreferences.Editor edit = prefs.edit();
                
		this.webDeviceCode=code;
		this.webDeviceName=name;
		
		edit.putString("webDeviceName", name);
		edit.putString("webDeviceCode", code);
		edit.commit();
	}
	
	public MapFactory getMapFactory() {
		if (mapProvider == mapSourceEnum.googleMap) {
			if (mapProviderUsed != mapSourceEnum.googleMap || mapFactory == null) {
				mapFactory = new googleMapFactory();
				mapProviderUsed = mapProvider;
			}
		} else if (mapProvider != mapSourceEnum.googleMap) {
			if (mapProviderUsed == mapSourceEnum.googleMap || mapFactory == null) {
				mapFactory = new mfMapFactory();
				mapProviderUsed = mapProvider;
			}
		}
		
		return mapFactory;
	}
	
	public String getMapFile() {
		return mapFile;
	}
	
	public boolean setMapFile(String mapFileIn) {
		final SharedPreferences.Editor prefsEdit = prefs.edit();

		prefsEdit.putString("mfmapfile", mapFileIn);

		mapFile = mapFileIn;

		return prefsEdit.commit();		
	}
	
	public Context getContext() {
		return context;
	}
}
