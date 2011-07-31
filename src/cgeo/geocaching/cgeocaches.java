package cgeo.geocaching;

import gnu.android.app.appmanualclient.AppManualReaderClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.apps.cachelist.CacheListAppFactory;
import cgeo.geocaching.filter.cgFilter;
import cgeo.geocaching.filter.cgFilterBySize;
import cgeo.geocaching.filter.cgFilterByTrackables;
import cgeo.geocaching.filter.cgFilterByType;
import cgeo.geocaching.sorting.CacheComparator;
import cgeo.geocaching.sorting.DifficultyComparator;
import cgeo.geocaching.sorting.GeocodeComparator;
import cgeo.geocaching.sorting.InventoryComparator;
import cgeo.geocaching.sorting.NameComparator;
import cgeo.geocaching.sorting.PopularityComparator;
import cgeo.geocaching.sorting.RatingComparator;
import cgeo.geocaching.sorting.SizeComparator;
import cgeo.geocaching.sorting.TerrainComparator;
import cgeo.geocaching.sorting.VoteComparator;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class cgeocaches extends ListActivity {

	private static final int MENU_SWITCH_SELECT_MODE = 0;
	private static final int MENU_REFRESH_STORED = 1;
	private static final int MENU_DROP_CACHES = 5;
	private static final int MENU_IMPORT_GPX = 6;
	private static final int MENU_CREATE_LIST = 7;
	private static final int MENU_DROP_LIST = 8;
	private static final int MENU_INVERT_SELECTION = 9;
	private static final int MENU_SORT_DISTANCE = 10;
	private static final int MENU_SORT_DIFFICULTY = 11;
	private static final int MENU_SORT_TERRAIN = 12;
	private static final int MENU_SORT_SIZE = 13;
	private static final int MENU_SORT_FAVORITES = 14;
	private static final int MENU_SORT_NAME = 15;
	private static final int MENU_SORT_GEOCODE = 16;
	private static final int MENU_SWITCH_LIST = 17;
	private static final int MENU_SORT_RATING = 18;
	private static final int MENU_SORT_VOTE = 19;
	private static final int MENU_SORT_INVENTORY = 20;

	private static final int MENU_IMPORT_WEB = 25;
	private static final int MENU_EXPORT_NOTES = 26;

	private static final int MENU_DROP_CACHE = 30;
	private static final int MENU_MOVE_TO_LIST = 31;

	private static final int SUBMENU_MOVE_TO_LIST = 100;


	private GoogleAnalyticsTracker tracker = null;
	private String action = null;
	private String type = null;
	private Double latitude = null;
	private Double longitude = null;
	private String cachetype = null;
	private String keyword = null;
	private String address = null;
	private String username = null;
	private Long searchId = null;
	private ArrayList<cgCache> cacheList = new ArrayList<cgCache>();
	private cgeoapplication app = null;
	private Resources res = null;
	private static Activity activity = null;
	private cgCacheListAdapter adapter = null;
	private LayoutInflater inflater = null;
	private View listFooter = null;
	private TextView listFooterText = null;
	private cgSettings settings = null;
	private cgBase base = null;
	private cgWarning warning = null;
	private ProgressDialog waitDialog = null;
	private Double northHeading = new Double(0);
	private cgGeo geo = null;
	private cgDirection dir = null;
	private cgUpdateLoc geoUpdate = new update();
	private cgUpdateDir dirUpdate = new UpdateDirection();
	private String title = "";
	private int detailTotal = 0;
	private int detailProgress = 0;
	private long detailProgressTime = 0l;
	private geocachesLoadDetails threadD = null;
	private geocachesLoadFromWeb threadW = null;
	private geocachesDropDetails threadR = null;
	private geocachesExportFieldNotes threadF = null;
	private int listId = 0;
	private ArrayList<cgList> lists = null;
	private String selectedFilter = null;
	private GeocodeComparator gcComparator = new GeocodeComparator();
	private Handler loadCachesHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			try {
				if (searchId != null && searchId > 0) {
					base.setTitle(activity, title + " [" + app.getCount(searchId) + "]");
					cacheList.clear();

					final ArrayList<cgCache> cacheListTmp = app.getCaches(searchId);
					if (cacheListTmp != null && cacheListTmp.isEmpty() == false) {
						cacheList.addAll(cacheListTmp);
						cacheListTmp.clear();

						Collections.sort((List<cgCache>)cacheList, gcComparator);
					}
				} else {
					base.setTitle(activity, title);
				}

				setAdapter();

				if (cacheList == null) {
					warning.showToast(res.getString(R.string.err_list_load_fail));
					setMoreCaches(false);
				} else {
					final Integer count = app.getTotal(searchId);

					if (count != null && count > 0) {
						if (cacheList.size() < app.getTotal(searchId) && cacheList.size() < 1000) {
							setMoreCaches(true);
						} else {
							setMoreCaches(false);
						}
					} else {
						setMoreCaches(false);
					}
				}

				if (cacheList != null && app.getError(searchId) != null && app.getError(searchId).equalsIgnoreCase(cgBase.errorRetrieve.get(-7)) == true) {
					AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
					dialog.setTitle(res.getString(R.string.license));
					dialog.setMessage(res.getString(R.string.err_license));
					dialog.setCancelable(true);
					dialog.setNegativeButton(res.getString(R.string.license_dismiss), new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int id) {
							settings.deleteCookies();
							dialog.cancel();
						}
					});
					dialog.setPositiveButton(res.getString(R.string.license_show), new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int id) {
							settings.deleteCookies();
							activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.geocaching.com/software/agreement.aspx?ID=0")));
						}
					});

					AlertDialog alert = dialog.create();
					alert.show();
				} else if (app != null && app.getError(searchId) != null && app.getError(searchId).length() > 0) {
					warning.showToast(res.getString(R.string.err_download_fail) + app.getError(searchId) + ".");

					hideLoading();
					base.showProgress(activity, false);

					finish();
					return;
				}

				if (geo != null && geo.latitudeNow != null && geo.longitudeNow != null) {
					adapter.setActualCoordinates(geo.latitudeNow, geo.longitudeNow);
					adapter.setActualHeading(northHeading);
				}
			} catch (Exception e) {
				warning.showToast(res.getString(R.string.err_detail_cache_find_any));
				Log.e(cgSettings.tag, "cgeocaches.loadCachesHandler: " + e.toString());

				hideLoading();
				base.showProgress(activity, false);

				finish();
				return;
			}

			try {
				hideLoading();
				base.showProgress(activity, false);
			} catch (Exception e2) {
				Log.e(cgSettings.tag, "cgeocaches.loadCachesHandler.2: " + e2.toString());
			}

			if (adapter != null) {
				adapter.setSelectMode(false, true);
			}
		}
	};
	private Handler loadNextPageHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			try {
				if (searchId != null && searchId > 0) {
					base.setTitle(activity, title + " [" + app.getCount(searchId) + "]");
					cacheList.clear();

					final ArrayList<cgCache> cacheListTmp = app.getCaches(searchId);
					if (cacheListTmp != null && cacheListTmp.isEmpty() == false) {
						cacheList.addAll(cacheListTmp);
						cacheListTmp.clear();
						Collections.sort((List<cgCache>)cacheList, gcComparator);
					}
					if(adapter != null){
						adapter.reFilter();
					}
				} else {
					base.setTitle(activity, title);
				}

				setAdapter();

				if (cacheList == null) {
					warning.showToast(res.getString(R.string.err_list_load_fail));
					setMoreCaches(false);
				} else {
					final Integer count = app.getTotal(searchId);
					if (count != null && count > 0) {
						if (cacheList.size() < app.getTotal(searchId) && cacheList.size() < 1000) {
							setMoreCaches(true);
						} else {
							setMoreCaches(false);
						}
					} else {
						setMoreCaches(false);
					}
				}

				if (app.getError(searchId) != null && app.getError(searchId).length() > 0) {
					warning.showToast(res.getString(R.string.err_download_fail) + app.getError(searchId) + ".");

					listFooter.setOnClickListener(new moreCachesListener());
					hideLoading();
					base.showProgress(activity, false);

					finish();
					return;
				}

				if (geo != null && geo.latitudeNow != null && geo.longitudeNow != null) {
					adapter.setActualCoordinates(geo.latitudeNow, geo.longitudeNow);
					adapter.setActualHeading(northHeading);
				}
			} catch (Exception e) {
				warning.showToast(res.getString(R.string.err_detail_cache_find_next));
				Log.e(cgSettings.tag, "cgeocaches.loadNextPageHandler: " + e.toString());
			}

			listFooter.setOnClickListener(new moreCachesListener());

			hideLoading();
			base.showProgress(activity, false);

			if (adapter != null) {
				adapter.setSelectMode(false, true);
			}
		}
	};
	private Handler loadDetailsHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			setAdapter();

			if (msg.what > -1) {
				if (waitDialog != null) {
					cacheList.get(msg.what).statusChecked = false;

					if (adapter != null) {
						adapter.notifyDataSetChanged();
					}

					int secondsElapsed = (int)((System.currentTimeMillis() - detailProgressTime) / 1000);
					int minutesRemaining = (int) ((detailTotal - detailProgress) * secondsElapsed / detailProgress / 60);

					waitDialog.setProgress(detailProgress);
					if (minutesRemaining < 1) {
						waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + res.getString(R.string.caches_eta_ltm));
					} else if (minutesRemaining == 1) {
						waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + minutesRemaining + " " + res.getString(R.string.caches_eta_min));
					} else {
						waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + minutesRemaining + " " + res.getString(R.string.caches_eta_mins));
					}
				}
			} else {
				if (cacheList != null && searchId != null) {
					final ArrayList<cgCache> cacheListTmp = app.getCaches(searchId);
					if (cacheListTmp != null && cacheListTmp.isEmpty() == false) {
						cacheList.clear();
						cacheList.addAll(cacheListTmp);
						cacheListTmp.clear();
						Collections.sort((List<cgCache>)cacheList, gcComparator);
					}
				}

				if (geo != null && geo.latitudeNow != null && geo.longitudeNow != null) {
					adapter.setActualCoordinates(geo.latitudeNow, geo.longitudeNow);
					adapter.setActualHeading(northHeading);
				}

				base.showProgress(activity, false);
				if (waitDialog != null) {
					waitDialog.dismiss();
					waitDialog.setOnCancelListener(null);
				}

				if (geo == null) {
					geo = app.startGeo(activity, geoUpdate, base, settings, warning, 0, 0);
				}
				if (settings.livelist == 1 && settings.useCompass == 1 && dir == null) {
					dir = app.startDir(activity, dirUpdate, warning);
				}
			}
		}
	};
	private Handler downloadFromWebHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			setAdapter();

			if (adapter != null) {
				adapter.notifyDataSetChanged();
			}

			if (msg.what == 0) { //no caches
				waitDialog.setMessage(res.getString(R.string.web_import_waiting));
			} else if (msg.what == 1) { //cache downloading
				waitDialog.setMessage(res.getString(R.string.web_downloading)+" "+(String)msg.obj+"...");
			} else if (msg.what == 2) { //Cache downloaded
				waitDialog.setMessage(res.getString(R.string.web_downloaded)+" "+(String)msg.obj+".");
				//Once a cache is downloaded I used switchList to refresh it.
				switchList(listId, -1);
                        } else if (msg.what == -2) {
				if (waitDialog != null) {
					waitDialog.dismiss();
					waitDialog.setOnCancelListener(null);
				}
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                warning.showToast(res.getString(R.string.gpx_import_no_files));
				finish();
				return;
			} else {
				if (adapter != null) {
					adapter.setSelectMode(false, true);
				}

				cacheList.clear();

				final ArrayList<cgCache> cacheListTmp = app.getCaches(searchId);
				if (cacheListTmp != null && cacheListTmp.isEmpty() == false) {
					cacheList.addAll(cacheListTmp);
					cacheListTmp.clear();

					Collections.sort((List<cgCache>)cacheList, gcComparator);
				}

				if (waitDialog != null) {
					waitDialog.dismiss();
					waitDialog.setOnCancelListener(null);
				}
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			}
		}
	};
	private Handler dropDetailsHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			setAdapter();

			if (msg.what > -1) {
				cacheList.get(msg.what).statusChecked = false;
			} else {
				if (adapter != null) {
					adapter.setSelectMode(false, true);
				}

				cacheList.clear();

				final ArrayList<cgCache> cacheListTmp = app.getCaches(searchId);
				if (cacheListTmp != null && cacheListTmp.isEmpty() == false) {
					cacheList.addAll(cacheListTmp);
					cacheListTmp.clear();

					Collections.sort((List<cgCache>)cacheList, gcComparator);
				}

				if (waitDialog != null) {
					waitDialog.dismiss();
					waitDialog.setOnCancelListener(null);
				}
			}
		}
	};
	private Handler exportFieldNotesHandler = new Handler() {

        @Override
        public void handleMessage(Message msg)
        {
            setAdapter();

            if (msg.what > -1)
            {
                cacheList.get(msg.what).statusChecked = false;
                waitDialog.setProgress(detailProgress);
            }
            if (-2 == msg.what)
            {
                warning.showToast(res.getString(R.string.info_fieldnotes_exported_to) + ": " + msg.obj.toString());
            }
            if (-3 == msg.what)
            {
                warning.showToast(res.getString(R.string.err_fieldnotes_export_failed));
            }
            else
            {
                if (adapter != null)
                {
                    adapter.setSelectMode(false, true);
                }

                if (waitDialog != null)
                {
                    waitDialog.dismiss();
                    waitDialog.setOnCancelListener(null);
                }
            }
        }
    };
	private ContextMenuInfo lastMenuInfo;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// init
		activity = this;
		res = this.getResources();
		app = (cgeoapplication) this.getApplication();
		app.setAction(action);
		settings = new cgSettings(this, getSharedPreferences(cgSettings.preferences, 0));
		base = new cgBase(app, settings, getSharedPreferences(cgSettings.preferences, 0));
		warning = new cgWarning(this);

		// set layout
		if (settings.skin == 1) {
			setTheme(R.style.light);
		} else {
			setTheme(R.style.dark);
		}
		setContentView(R.layout.caches);
		base.setTitle(activity, "caches");

		// google analytics
		tracker = GoogleAnalyticsTracker.getInstance();
		tracker.start(cgSettings.analytics, this);
		tracker.dispatch();
		base.sendAnal(activity, tracker, "/cache/list");

		// get parameters
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			type = extras.getString("type");
			latitude = extras.getDouble("latitude");
			longitude = extras.getDouble("longitude");
			cachetype = extras.getString("cachetype");
			keyword = extras.getString("keyword");
			address = extras.getString("address");
			username = extras.getString("username");
		}

		init();

		Thread threadPure;
		cgSearchThread thread;

		if (type.equals("offline") == true) {
			listId = settings.getLastList();
			if (listId <= 0) {
				listId = 1;
				title = res.getString(R.string.caches_stored);
			} else {
				final cgList list = app.getList(listId);
				title = list.title;
			}

			base.setTitle(activity, title);
			base.showProgress(activity, true);
			setLoadingCaches();

			threadPure = new geocachesLoadByOffline(loadCachesHandler, latitude, longitude, listId);
			threadPure.start();
		} else if (type.equals("history") == true) {
			if (adapter != null) {
				adapter.setHistoric(true);
			}

			title = res.getString(R.string.caches_history);
			base.setTitle(activity, title);
			base.showProgress(activity, true);
			setLoadingCaches();

			threadPure = new geocachesLoadByHistory(loadCachesHandler);
			threadPure.start();
		} else if (type.equals("nearest") == true) {
			action = "pending";
			title = res.getString(R.string.caches_nearby);
			base.setTitle(activity, title);
			base.showProgress(activity, true);
			setLoadingCaches();

			thread = new geocachesLoadByCoords(loadCachesHandler, latitude, longitude, cachetype);
			thread.setRecaptchaHandler(new cgSearchHandler(activity, res, thread));
			thread.start();
		} else if (type.equals("coordinate") == true) {
			action = "planning";
			title = base.formatCoordinate(latitude, res.getString(R.string.search_lat), true) + " | " + base.formatCoordinate(longitude, res.getString(R.string.search_lon), true);
			base.setTitle(activity, title);
			base.showProgress(activity, true);
			setLoadingCaches();

			thread = new geocachesLoadByCoords(loadCachesHandler, latitude, longitude, cachetype);
			thread.setRecaptchaHandler(new cgSearchHandler(activity, res, thread));
			thread.start();
		} else if (type.equals("keyword") == true) {
			title = keyword;
			base.setTitle(activity, title);
			base.showProgress(activity, true);
			setLoadingCaches();

			thread = new geocachesLoadByKeyword(loadCachesHandler, keyword, cachetype);
			thread.setRecaptchaHandler(new cgSearchHandler(activity, res, thread));
			thread.start();
		} else if (type.equals("address") == true) {
			action = "planning";
			if (address != null && address.length() > 0) {
				title = address;
				base.setTitle(activity, title);
				base.showProgress(activity, true);
				setLoadingCaches();
			} else {
				title = base.formatCoordinate(latitude, res.getString(R.string.search_lat), true) + " | " + base.formatCoordinate(longitude, res.getString(R.string.search_lon), true);
				base.setTitle(activity, title);
				base.showProgress(activity, true);
				setLoadingCaches();
			}

			thread = new geocachesLoadByCoords(loadCachesHandler, latitude, longitude, cachetype);
			thread.setRecaptchaHandler(new cgSearchHandler(activity, res, thread));
			thread.start();
		} else if (type.equals("username") == true) {
			title = username;
			base.setTitle(activity, title);
			base.showProgress(activity, true);
			setLoadingCaches();

			thread = new geocachesLoadByUserName(loadCachesHandler, username, cachetype);
			thread.setRecaptchaHandler(new cgSearchHandler(activity, res, thread));
			thread.start();
		} else if (type.equals("owner") == true) {
			title = username;
			base.setTitle(activity, title);
			base.showProgress(activity, true);
			setLoadingCaches();

			thread = new geocachesLoadByOwner(loadCachesHandler, username, cachetype);
			thread.setRecaptchaHandler(new cgSearchHandler(activity, res, thread));
			thread.start();
		} else {
			title = "caches";
			base.setTitle(activity, title);
			Log.e(cgSettings.tag, "cgeocaches.onCreate: No action or unknown action specified");
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		init();
	}

	@Override
	public void onResume() {
		super.onResume();

		settings.load();
		init();

		if (adapter != null && geo != null && geo.latitudeNow != null && geo.longitudeNow != null) {
			adapter.setActualCoordinates(geo.latitudeNow, geo.longitudeNow);
			adapter.setActualHeading(northHeading);
		}

		if (adapter != null) {
			adapter.setSelectMode(false, true);
			if (geo != null && geo.latitudeNow != null && geo.longitudeNow != null) {
				adapter.forceSort(geo.latitudeNow, geo.longitudeNow);
			}
		}

		if (loadCachesHandler != null && searchId != null) {
			loadCachesHandler.sendEmptyMessage(0);
		}
	}

	@Override
	public void onDestroy() {
		if (adapter != null) {
			adapter = null;
		}

		if (dir != null) {
			dir = app.removeDir();
		}
		if (geo != null) {
			geo = app.removeGeo();
		}
		if (tracker != null) {
			tracker.stop();
		}

		super.onDestroy();
	}

	@Override
	public void onStop() {
		if (dir != null) {
			dir = app.removeDir();
		}
		if (geo != null) {
			geo = app.removeGeo();
		}

		super.onStop();
	}

	@Override
	public void onPause() {
		if (dir != null) {
			dir = app.removeDir();
		}
		if (geo != null) {
			geo = app.removeGeo();
		}

		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		SubMenu subMenuFilter = menu.addSubMenu(0, 105, 0, res.getString(R.string.caches_filter)).setIcon(android.R.drawable.ic_menu_search);
		subMenuFilter.setHeaderTitle(res.getString(R.string.caches_filter_title));
		subMenuFilter.add(0, 21, 0, res.getString(R.string.caches_filter_type));
		subMenuFilter.add(0, 22, 0, res.getString(R.string.caches_filter_size));
		subMenuFilter.add(0, 23, 0, res.getString(R.string.caches_filter_track));
		subMenuFilter.add(0, 24, 0, res.getString(R.string.caches_filter_clear));

		SubMenu subMenuSort = menu.addSubMenu(0, 104, 0, res.getString(R.string.caches_sort)).setIcon(android.R.drawable.ic_menu_sort_alphabetically);
		subMenuSort.setHeaderTitle(res.getString(R.string.caches_sort_title));

		// sort the context menu labels alphabetically for easier reading
		HashMap<String, Integer> comparators = new HashMap<String, Integer>();
		comparators.put(res.getString(R.string.caches_sort_distance), MENU_SORT_DISTANCE);
		comparators.put(res.getString(R.string.caches_sort_difficulty), MENU_SORT_DIFFICULTY);
		comparators.put(res.getString(R.string.caches_sort_terrain), MENU_SORT_TERRAIN);
		comparators.put(res.getString(R.string.caches_sort_size), MENU_SORT_SIZE);
		comparators.put(res.getString(R.string.caches_sort_favorites), MENU_SORT_FAVORITES);
		comparators.put(res.getString(R.string.caches_sort_name), MENU_SORT_NAME);
		comparators.put(res.getString(R.string.caches_sort_gccode), MENU_SORT_GEOCODE);
		comparators.put(res.getString(R.string.caches_sort_rating), MENU_SORT_RATING);
		comparators.put(res.getString(R.string.caches_sort_vote), MENU_SORT_VOTE);
		comparators.put(res.getString(R.string.caches_sort_inventory), MENU_SORT_INVENTORY);

		ArrayList<String> sortedLabels = new ArrayList<String>(comparators.keySet());
		Collections.sort(sortedLabels);
		for (String label : sortedLabels) {
			Integer id = comparators.get(label);
			subMenuSort.add(1, id, 0, label).setCheckable(true).setChecked(id == MENU_SORT_DISTANCE);
		}

		subMenuSort.setGroupCheckable(1, true, true);

		menu.add(0, 0, 0, res.getString(R.string.caches_select_mode)).setIcon(android.R.drawable.ic_menu_agenda);
		menu.add(0, MENU_INVERT_SELECTION, 0, res.getString(R.string.caches_select_invert)).setIcon(android.R.drawable.ic_menu_agenda);
		if (type.equals("offline") == true) {
			SubMenu subMenu = menu.addSubMenu(0, 103, 0, res.getString(R.string.caches_manage)).setIcon(android.R.drawable.ic_menu_save);
			subMenu.add(0, MENU_DROP_CACHES, 0, res.getString(R.string.caches_drop_all)); // delete saved caches
			subMenu.add(0, MENU_REFRESH_STORED, 0, res.getString(R.string.cache_offline_refresh)); // download details for all caches
			subMenu.add(0, MENU_EXPORT_NOTES, 0, res.getString(R.string.cache_export_fieldnote)); // export field notes
			if (settings.webDeviceCode == null)
			{
				menu.add(0, MENU_IMPORT_GPX, 0, res.getString(R.string.gpx_import_title)).setIcon(android.R.drawable.ic_menu_upload); // import gpx file
			} else {
				SubMenu subMenuImport = menu.addSubMenu(0, 105, 0, res.getString(R.string.import_title)).setIcon(android.R.drawable.ic_menu_upload); // import
				subMenuImport.add(1, MENU_IMPORT_GPX, 0, res.getString(R.string.gpx_import_title)).setCheckable(false).setChecked(false);
				subMenuImport.add(1, MENU_IMPORT_WEB, 0, res.getString(R.string.web_import_title)).setCheckable(false).setChecked(false);
			}
		} else {
		    if (type.equals("history"))
		    {
		        menu.add(0, MENU_EXPORT_NOTES, 0, res.getString(R.string.cache_export_fieldnote)).setIcon(android.R.drawable.ic_menu_save); // export field notes
		    }
			menu.add(0, MENU_REFRESH_STORED, 0, res.getString(R.string.caches_store_offline)).setIcon(android.R.drawable.ic_menu_set_as); // download details for all caches
		}

		CacheListAppFactory.addMenuItems(menu, activity, res);

		if (type.equals("offline") == true) {
			SubMenu subMenu = menu.addSubMenu(0, 102, 0, res.getString(R.string.list_menu)).setIcon(android.R.drawable.ic_menu_more);
			subMenu.add(0, MENU_CREATE_LIST, 0, res.getString(R.string.list_menu_create));
			subMenu.add(0, MENU_DROP_LIST, 0, res.getString(R.string.list_menu_drop));
			subMenu.add(0, MENU_SWITCH_LIST, 0, res.getString(R.string.list_menu_change));
		}

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		try {
			if (adapter != null && adapter.getSelectMode() == true) {
				menu.findItem(MENU_SWITCH_SELECT_MODE).setTitle(res.getString(R.string.caches_select_mode_exit));
				menu.findItem(MENU_INVERT_SELECTION).setVisible(true);
			} else {
				menu.findItem(MENU_SWITCH_SELECT_MODE).setTitle(res.getString(R.string.caches_select_mode));
				menu.findItem(MENU_INVERT_SELECTION).setVisible(false);
			}

			if (type != null && type.equals("offline") == true) {
				if (adapter != null && adapter.getChecked() > 0) {
					menu.findItem(MENU_DROP_CACHES).setTitle(res.getString(R.string.caches_drop_selected) + " (" + adapter.getChecked() + ")");
				} else {
					menu.findItem(MENU_DROP_CACHES).setTitle(res.getString(R.string.caches_drop_all));
				}

				if (adapter != null && adapter.getChecked() > 0) {
					menu.findItem(MENU_REFRESH_STORED).setTitle(res.getString(R.string.caches_refresh_selected) + " (" + adapter.getChecked() + ")");
				} else {
					menu.findItem(MENU_REFRESH_STORED).setTitle(res.getString(R.string.caches_refresh_all));
				}

				if (adapter != null && adapter.getChecked() > 0) {
                    menu.findItem(MENU_EXPORT_NOTES).setTitle(res.getString(R.string.cache_export_fieldnote) + " (" + adapter.getChecked() + ")");
                } else {
                    menu.findItem(MENU_EXPORT_NOTES).setTitle(res.getString(R.string.cache_export_fieldnote));
                }
			} else {
				if (adapter == null) {
					Log.i(cgSettings.tag, "No adapter");
				} else {
					Log.i(cgSettings.tag, "Checked: " + adapter.getChecked());
				}
				if (adapter != null && adapter.getChecked() > 0) {
					menu.findItem(MENU_REFRESH_STORED).setTitle(res.getString(R.string.caches_store_selected) + " (" + adapter.getChecked() + ")");
				} else {
					menu.findItem(MENU_REFRESH_STORED).setTitle(res.getString(R.string.caches_store_offline));
				}
			}

			if (type != null && type.equals("offline") == false && (cacheList != null && app != null && cacheList.size() >= app.getTotal(searchId))) { // there are no more caches
				menu.findItem(MENU_SWITCH_SELECT_MODE).setEnabled(false);
			} else {
				menu.findItem(MENU_SWITCH_SELECT_MODE).setEnabled(true);
			}

			MenuItem item = menu.findItem(MENU_DROP_LIST);
			if (item != null) {
				item.setVisible(listId != 1);
			}
			item = menu.findItem(MENU_SWITCH_LIST);
			if (item != null) {
				item.setVisible(app.getLists().size() >= 2);
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgeocaches.onPrepareOptionsMenu: " + e.toString());
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case MENU_SWITCH_SELECT_MODE:
				if (adapter != null) {
					adapter.switchSelectMode();
				}
				return true;
			case MENU_REFRESH_STORED:
				refreshStored();
				return true;
			case MENU_DROP_CACHES:
				dropStored();
				return false;
			case MENU_IMPORT_GPX:
				importGpx();
				return false;
			case MENU_CREATE_LIST:
				createList();
				return false;
			case MENU_DROP_LIST:
				removeList();
				return false;
			case MENU_INVERT_SELECTION:
				if (adapter != null) {
					adapter.invertSelection();
				}
				return false;
			case MENU_SORT_DISTANCE:
				setComparator(item, null);
				return false;
			case MENU_SORT_DIFFICULTY:
				setComparator(item, new DifficultyComparator());
				return false;
			case MENU_SORT_TERRAIN:
				setComparator(item, new TerrainComparator());
				return false;
			case MENU_SORT_SIZE:
				setComparator(item, new SizeComparator());
				return false;
			case MENU_SORT_FAVORITES:
				setComparator(item, new PopularityComparator());
				return false;
			case MENU_SORT_NAME:
				setComparator(item, new NameComparator());
				return false;
			case MENU_SORT_GEOCODE:
				setComparator(item, new GeocodeComparator());
				return false;
			case MENU_SWITCH_LIST:
				selectList(null);
				return false;
			case MENU_SORT_RATING:
				setComparator(item, new RatingComparator());
				return false;
			case MENU_SORT_VOTE:
				setComparator(item, new VoteComparator());
				return false;
			case MENU_SORT_INVENTORY:
				setComparator(item, new InventoryComparator());
				return false;
			case 21:
				selectedFilter = res.getString(R.string.caches_filter_type);
				openContextMenu(getListView());
				return false;
			case 22:
				selectedFilter = res.getString(R.string.caches_filter_size);
				openContextMenu(getListView());
				return false;
			case 23:
				adapter.setFilter(new cgFilterByTrackables());
				return false;
			case 24:
				if (adapter != null) {
					adapter.setFilter(null);
				}
				return false;
			case MENU_IMPORT_WEB:
				importWeb();
				return false;
			case MENU_EXPORT_NOTES:
                exportFieldNotes();
                return false;
		}

		return CacheListAppFactory.onMenuItemSelected(item, geo, cacheList, activity, res, tracker, null);
	}

	private void setComparator(MenuItem item,
			CacheComparator comparator) {
		if (adapter != null) {
			adapter.setComparator(comparator);
		}
		item.setChecked(true);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info) {
		super.onCreateContextMenu(menu, view, info);

		if (adapter == null) {
			return;
		}

		AdapterContextMenuInfo adapterInfo = null;
		try {
			adapterInfo = (AdapterContextMenuInfo) info;
		} catch (Exception e) {
			Log.w(cgSettings.tag, "cgeocaches.onCreateContextMenu: " + e.toString());
		}

		if ((adapterInfo == null || adapterInfo.position < 0) && selectedFilter != null){
			// Context menu opened by selecting an option on the filter submenu

			if (selectedFilter.equals(res.getString(R.string.caches_filter_size))) {
				menu.setHeaderTitle(res.getString(R.string.caches_filter_size_title));
				menu.add(0,  8, 0, res.getString(R.string.caches_filter_size_micro));
				menu.add(0,  9, 0, res.getString(R.string.caches_filter_size_small));
				menu.add(0, 10, 0, res.getString(R.string.caches_filter_size_regular));
				menu.add(0, 11, 0, res.getString(R.string.caches_filter_size_large));
				menu.add(0, 12, 0, res.getString(R.string.caches_filter_size_other));
				menu.add(0, 13, 0, res.getString(R.string.caches_filter_size_virtual));
				menu.add(0, 14, 0, res.getString(R.string.caches_filter_size_notchosen));
			} else if (selectedFilter.equals(res.getString(R.string.caches_filter_type))) {
				menu.setHeaderTitle(res.getString(R.string.caches_filter_type_title));
				menu.add(0,  15, 0, res.getString(R.string.caches_filter_type_traditional));
				menu.add(0,  16, 0, res.getString(R.string.caches_filter_type_multi));
				menu.add(0,  17, 0, res.getString(R.string.caches_filter_type_mystery));
				menu.add(0,  18, 0, res.getString(R.string.caches_filter_type_letterbox));
				menu.add(0,  19, 0, res.getString(R.string.caches_filter_type_event));
				menu.add(0,  20, 0, res.getString(R.string.caches_filter_type_mega));
				menu.add(0,  21, 0, res.getString(R.string.caches_filter_type_earth));
				menu.add(0,  22, 0, res.getString(R.string.caches_filter_type_cito));
				menu.add(0,  23, 0, res.getString(R.string.caches_filter_type_webcam));
				menu.add(0,  24, 0, res.getString(R.string.caches_filter_type_virtual));
				menu.add(0,  25, 0, res.getString(R.string.caches_filter_type_wherigo));
				menu.add(0,  26, 0, res.getString(R.string.caches_filter_type_lostfound));
				menu.add(0,  27, 0, res.getString(R.string.caches_filter_type_ape));
				menu.add(0,  28, 0, res.getString(R.string.caches_filter_type_gchq));
				menu.add(0,  29, 0, res.getString(R.string.caches_filter_type_gps));
			}
		} else{
			final cgCache cache = adapter.getItem(adapterInfo.position);

			if (cache.name != null && cache.name.length() > 0) {
				menu.setHeaderTitle(cache.name);
			} else {
				menu.setHeaderTitle(cache.geocode);
			}

			if (cache.latitude != null && cache.longitude != null) {
				menu.add(0, 1, 0, res.getString(R.string.cache_menu_compass));
				SubMenu subMenu = menu.addSubMenu(1, 0, 0, res.getString(R.string.cache_menu_navigate)).setIcon(android.R.drawable.ic_menu_more);
				NavigationAppFactory.addMenuItems(subMenu, activity, res);
				menu.add(0, 6, 0, res.getString(R.string.cache_menu_visit));
				menu.add(0, 7, 0, res.getString(R.string.cache_menu_details));
			}
			if (cache.reason >= 1) {
				menu.add(0, MENU_DROP_CACHE, 0, res.getString(R.string.cache_offline_drop));
				ArrayList<cgList> cacheLists = app.getLists();
				int listCount = cacheLists.size();
				if (listCount > 1) {
					SubMenu submenu = menu.addSubMenu(0, MENU_MOVE_TO_LIST, 0, res.getString(R.string.cache_menu_move_list));
					for (int i = 0; i < listCount; i++) {
						cgList list = cacheLists.get(i);
						submenu.add(Menu.NONE, SUBMENU_MOVE_TO_LIST + list.id, Menu.NONE, list.title);
					}
				}
			}
		}
	}



	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final int id = item.getItemId();
		ContextMenu.ContextMenuInfo info = item.getMenuInfo();

		// restore menu info for sub menu items, see
		// https://code.google.com/p/android/issues/detail?id=7139
		if (info == null) {
			info = lastMenuInfo;
			lastMenuInfo = null;
		}

		AdapterContextMenuInfo adapterInfo = null;
		try {
			adapterInfo = (AdapterContextMenuInfo) info;
		} catch (Exception e) {
			Log.w(cgSettings.tag, "cgeocaches.onContextItemSelected: " + e.toString());
		}

		// the context menu may be invoked for the cache or for the filter list
		final int touchedPos;
		final cgCache cache;
		if (adapterInfo != null) {
			touchedPos = adapterInfo.position;
			cache = adapter.getItem(touchedPos);
		}
		else {
			touchedPos = -1;
			cache = null;
		}

		if (id == 1) { // compass
			Intent navigateIntent = new Intent(activity, cgeonavigate.class);
			navigateIntent.putExtra("latitude", cache.latitude);
			navigateIntent.putExtra("longitude", cache.longitude);
			navigateIntent.putExtra("geocode", cache.geocode.toUpperCase());
			navigateIntent.putExtra("name", cache.name);

			activity.startActivity(navigateIntent);

			return true;
		} else if (id == 6) { // log visit
			if (cache.cacheid == null || cache.cacheid.length() == 0) {
				warning.showToast(res.getString(R.string.err_cannot_log_visit));
				return true;
			}

			Intent logVisitIntent = new Intent(activity, cgeovisit.class);
			logVisitIntent.putExtra("id", cache.cacheid);
			logVisitIntent.putExtra("geocode", cache.geocode.toUpperCase());
			logVisitIntent.putExtra("type", cache.type.toLowerCase());

			activity.startActivity(logVisitIntent);

			return true;
		} else if (id == 7) { // cache details
			Intent cachesIntent = new Intent(activity, cgeodetail.class);
			cachesIntent.putExtra("geocode", cache.geocode.toUpperCase());
			cachesIntent.putExtra("name", cache.name);
			activity.startActivity(cachesIntent);

			return true;
		}
		else if (id == 8) {
			return setFilter(new cgFilterBySize(
					res.getString(R.string.caches_filter_size_micro)));
		} else if (id == 9) {
			return setFilter(new cgFilterBySize(
					res.getString(R.string.caches_filter_size_small)));
		} else if (id == 10) {
			return setFilter(new cgFilterBySize(
					res.getString(R.string.caches_filter_size_regular)));
		} else if (id == 11) {
			return setFilter(new cgFilterBySize(
					res.getString(R.string.caches_filter_size_large)));
		} else if (id == 12) {
			return setFilter(new cgFilterBySize(
					res.getString(R.string.caches_filter_size_other)));
		} else if (id == 13) {
			return setFilter(new cgFilterBySize(
					res.getString(R.string.caches_filter_size_virtual)));
		} else if (id == 14) {
			return setFilter(new cgFilterBySize(
					res.getString(R.string.caches_filter_size_notchosen)));
		} else if (id == 15) {
			return setFilter(new cgFilterByType("traditional"));
		} else if (id == 16) {
			return setFilter(new cgFilterByType("multi"));
		} else if (id == 17) {
			return setFilter(new cgFilterByType("mystery"));
		} else if (id == 18) {
			return setFilter(new cgFilterByType("letterbox"));
		} else if (id == 19) {
			return setFilter(new cgFilterByType("event"));
		} else if (id == 20) {
			return setFilter(new cgFilterByType("mega"));
		} else if (id == 21) {
			return setFilter(new cgFilterByType("earth"));
		} else if (id == 22) {
			return setFilter(new cgFilterByType("cito"));
		} else if (id == 23) {
			return setFilter(new cgFilterByType("webcam"));
		} else if (id == 24) {
			return setFilter(new cgFilterByType("virtual"));
		} else if (id == 25) {
			return setFilter(new cgFilterByType("wherigo"));
		} else if (id == 26) {
			return setFilter(new cgFilterByType("lostfound"));
		} else if (id == 27) {
			return setFilter(new cgFilterByType("ape"));
		} else if (id == 28) {
			return setFilter(new cgFilterByType("gchq"));
		} else if (id == 29) {
			return setFilter(new cgFilterByType("gps"));
		} else if (id == MENU_DROP_CACHE) {
			base.dropCache(app, activity, cache, new Handler() {
				@Override
				public void handleMessage(Message msg) {
					switchList(listId, -1); // refresh
				}
			});
			return true;
		} else if (id >= SUBMENU_MOVE_TO_LIST && id < SUBMENU_MOVE_TO_LIST + 100) {
			int newListId = id - SUBMENU_MOVE_TO_LIST;
			app.moveToList(cache.geocode, newListId);
			// refresh list by switching to the current list
			switchListById(listId);
			return true;
		}
		// we must remember the menu info for the sub menu, there is a bug
		// in Android:
		// https://code.google.com/p/android/issues/detail?id=7139
		lastMenuInfo = info;

		return NavigationAppFactory.onMenuItemSelected(item, geo, activity,
				res, warning, tracker, cache, null, null, null);
	}

	private boolean setFilter(cgFilter filter) {
		if (adapter != null) {
			adapter.setFilter(filter);
			return true;
		}
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (adapter != null) {
				if (adapter.resetChecks() == true) {
					return true;
				} else if (adapter.getSelectMode() == true) {
					adapter.setSelectMode(false, true);
					return true;
				}
			}
		}
		return super.onKeyDown(keyCode, event);
	}


	private void setAdapter() {
		if (listFooter == null) {
			if (inflater == null) {
				inflater = activity.getLayoutInflater();
			}
			listFooter = inflater.inflate(R.layout.caches_footer, null);

			listFooter.setClickable(true);
			listFooter.setOnClickListener(new moreCachesListener());
		}
		if (listFooterText == null) {
			listFooterText = (TextView) listFooter.findViewById(R.id.more_caches);
		}

		if (adapter == null) {
			final ListView list = getListView();

			registerForContextMenu(list);
			list.setLongClickable(true);
			list.addFooterView(listFooter);

			adapter = new cgCacheListAdapter(activity, settings, cacheList, base);
			setListAdapter(adapter);
		} else {
			adapter.notifyDataSetChanged();
		}

		if (adapter != null && geo != null) {
			adapter.setActualCoordinates(geo.latitudeNow, geo.longitudeNow);
		}
		if (adapter != null && dir != null) {
			adapter.setActualHeading(dir.directionNow);
		}
	}

	private void setLoadingCaches() {
		if (listFooter == null) {
			return;
		}
		if (listFooterText == null) {
			return;
		}

		listFooterText.setText(res.getString(R.string.caches_more_caches_loading));
		listFooter.setClickable(false);
		listFooter.setOnClickListener(null);
	}

	private void setMoreCaches(boolean more) {
		if (listFooter == null) {
			return;
		}
		if (listFooterText == null) {
			return;
		}

		if (more == false) {
			if (cacheList == null || cacheList.isEmpty()) {
				listFooterText.setText(res.getString(R.string.caches_no_cache));
			} else {
				listFooterText.setText(res.getString(R.string.caches_more_caches_no));
			}
			listFooter.setClickable(false);
			listFooter.setOnClickListener(null);
		} else {
			listFooterText.setText(res.getString(R.string.caches_more_caches));
			listFooter.setClickable(true);
			listFooter.setOnClickListener(new moreCachesListener());
		}
	}

	private void init() {
		// sensor & geolocation manager
		if (geo == null) {
			geo = app.startGeo(activity, geoUpdate, base, settings, warning, 0, 0);
		}
		if (settings.livelist == 1 && settings.useCompass == 1 && dir == null) {
			dir = app.startDir(activity, dirUpdate, warning);
		}

		if (cacheList != null) {
			base.setTitle(activity, title);
		}

		if (cacheList != null && cacheList.isEmpty() == false) {
			final Integer count = app.getTotal(searchId);
			if (count != null && count > 0) {
				base.setTitle(activity, title);
				if (cacheList.size() < app.getTotal(searchId) && cacheList.size() < 1000) {
					setMoreCaches(true);
				} else {
					setMoreCaches(false);
				}
			} else {
				base.setTitle(activity, title);
				setMoreCaches(false);
			}
		} else {
			base.setTitle(activity, title);
		}

		setAdapter();

		if (geo != null) {
			geoUpdate.updateLoc(geo);
		}
		if (dir != null) {
			dirUpdate.updateDir(dir);
		}
	}

	private void importGpx() {
		final Intent intent = new Intent(activity, cgeogpxes.class);
		intent.putExtra("list", listId);
		activity.startActivity(intent);

		finish();
	}

	public void refreshStored() {
		if (adapter != null && adapter.getChecked() > 0) {
			// there are some checked caches
			detailTotal = adapter.getChecked();
		} else {
			// no checked caches, download everything (when already stored - refresh them)
			detailTotal = cacheList.size();
		}
		detailProgress = 0;

		base.showProgress(activity, false);
		waitDialog = new ProgressDialog(this);
		waitDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

			public void onCancel(DialogInterface arg0) {
				try {
					if (threadD != null) {
						threadD.kill();
					}

					if (geo == null) {
						geo = app.startGeo(activity, geoUpdate, base, settings, warning, 0, 0);
					}
					if (settings.livelist == 1 && settings.useCompass == 1 && dir == null) {
						dir = app.startDir(activity, dirUpdate, warning);
					}
				} catch (Exception e) {
					Log.e(cgSettings.tag, "cgeocaches.onOptionsItemSelected.onCancel: " + e.toString());
				}
			}
		});

		waitDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		int etaTime = (int) ((detailTotal * 25) / 60);
		if (etaTime < 1) {
			waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + res.getString(R.string.caches_eta_ltm));
		} else if (etaTime == 1) {
			waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + etaTime + " " + res.getString(R.string.caches_eta_min));
		} else {
			waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + etaTime + " " + res.getString(R.string.caches_eta_mins));
		}
		waitDialog.setCancelable(true);
		waitDialog.setMax(detailTotal);
		waitDialog.show();

		detailProgressTime = System.currentTimeMillis();

		threadD = new geocachesLoadDetails(loadDetailsHandler, listId);
		threadD.start();
	}

	public void exportFieldNotes()
	{
        if (adapter != null && adapter.getChecked() > 0)
        {
            // there are some checked caches
            detailTotal = adapter.getChecked();
        }
        else
        {
            // no checked caches, export all
            detailTotal = cacheList.size();
        }
        detailProgress = 0;

        base.showProgress(activity, false);
        waitDialog = new ProgressDialog(this);
        waitDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

            public void onCancel(DialogInterface arg0)
            {
                try
                {
                    if (threadD != null)
                    {
                        threadD.kill();
                    }
                } catch (Exception e)
                {
                    Log.e(cgSettings.tag, "cgeocaches.exportFieldNotes.onCancel: " + e.toString());
                }
            }
        });

        waitDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        waitDialog.setMessage(res.getString(R.string.caches_exporting_fieldnote));

        waitDialog.setCancelable(true);
        waitDialog.setMax(detailTotal);
        waitDialog.show();

        threadF = new geocachesExportFieldNotes(exportFieldNotesHandler);
        threadF.start();
    }

	public void importWeb() {
		detailProgress = 0;

                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		base.showProgress(activity, false);
		waitDialog = new ProgressDialog(this);
		waitDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

			public void onCancel(DialogInterface arg0) {
				try {
					if (threadW != null) {
						threadW.kill();
					}

					if (geo == null) {
						geo = app.startGeo(activity, geoUpdate, base, settings, warning, 0, 0);
					}
					if (settings.livelist == 1 && settings.useCompass == 1 && dir == null) {
						dir = app.startDir(activity, dirUpdate, warning);
					}
				} catch (Exception e) {
					Log.e(cgSettings.tag, "cgeocaches.importWeb.onCancel: " + e.toString());
				}
			}
		});

		waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		waitDialog.setMessage(res.getString(R.string.web_import_waiting));
		waitDialog.setCancelable(true);
		waitDialog.show();

		threadW = new geocachesLoadFromWeb(downloadFromWebHandler, listId);
		threadW.start();
	}

	public void dropStored() {
		AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
		dialog.setCancelable(true);
		dialog.setTitle(res.getString(R.string.caches_drop_stored));

		if (adapter != null && adapter.getChecked() > 0) {
			dialog.setMessage(res.getString(R.string.caches_drop_selected_ask));
			dialog.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int id) {
					dropSelected();
					dialog.cancel();
				}
			});
		} else {
			dialog.setMessage(res.getString(R.string.caches_drop_all_ask));
			dialog.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int id) {
					dropSelected();
					dialog.cancel();
				}
			});
		}
		dialog.setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});

		AlertDialog alert = dialog.create();
		alert.show();
	}

	public void dropSelected() {
		waitDialog = new ProgressDialog(this);
		waitDialog.setMessage(res.getString(R.string.caches_drop_progress));
		waitDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

			public void onCancel(DialogInterface arg0) {
				try {
					if (threadR != null) {
						threadR.kill();
					}
				} catch (Exception e) {
					Log.e(cgSettings.tag, "cgeocaches.onOptionsItemSelected.onCancel: " + e.toString());
				}
			}
		});

		waitDialog.setCancelable(true);
		waitDialog.show();

		threadR = new geocachesDropDetails(dropDetailsHandler);
		threadR.start();
	}

	private class update extends cgUpdateLoc {

		@Override
		public void updateLoc(cgGeo geo) {
			if (geo == null) {
				return;
			}
			if (adapter == null) {
				return;
			}

			try {
				if (cacheList != null && geo.latitudeNow != null && geo.longitudeNow != null) {
					adapter.setActualCoordinates(geo.latitudeNow, geo.longitudeNow);
				}

				if (settings.useCompass == 0 || (geo.speedNow != null && geo.speedNow > 5)) { // use GPS when speed is higher than 18 km/h
					if (settings.useCompass == 0) {
						if (geo.bearingNow != null) {
							adapter.setActualHeading(geo.bearingNow);
						} else {
							adapter.setActualHeading(new Double(0));
						}
					}
					if (northHeading != null) {
						adapter.setActualHeading(northHeading);
					}
				}
			} catch (Exception e) {
				Log.w(cgSettings.tag, "Failed to update location.");
			}
		}
	}

	private class UpdateDirection extends cgUpdateDir {

		@Override
		public void updateDir(cgDirection dir) {
			if (settings.livelist == 0) {
				return;
			}
			if (dir == null || dir.directionNow == null) {
				return;
			}

			northHeading = dir.directionNow;
			if (northHeading != null && adapter != null && (geo == null || geo.speedNow == null || geo.speedNow <= 5)) { // use compass when speed is lower than 18 km/h) {
				adapter.setActualHeading(northHeading);
			}
		}
	}

	private class geocachesLoadByOffline extends Thread {

		private Handler handler = null;
		private Double latitude = null;
		private Double longitude = null;
		private int listId = 1;

		public geocachesLoadByOffline(Handler handlerIn, Double latitudeIn, Double longitudeIn, int listIdIn) {
			handler = handlerIn;
			latitude = latitudeIn;
			longitude = longitudeIn;
			listId = listIdIn;
		}

		@Override
		public void run() {
			HashMap<String, Object> params = new HashMap<String, Object>();
			if (latitude != null && longitude != null) {
				params.put("latitude", latitude);
				params.put("longitude", longitude);
				params.put("cachetype", settings.cacheType);
				params.put("list", listId);
			}

			searchId = base.searchByOffline(params);

			handler.sendMessage(new Message());
		}
	}

	private class geocachesLoadByHistory extends Thread {

		private Handler handler = null;

		public geocachesLoadByHistory(Handler handlerIn) {
			handler = handlerIn;
		}

		@Override
		public void run() {
			HashMap<String, Object> params = new HashMap<String, Object>();
			if (latitude != null && longitude != null) {
				params.put("cachetype", settings.cacheType);
			}

			searchId = base.searchByHistory(params);

			handler.sendMessage(new Message());
		}
	}

	private class geocachesLoadNextPage extends cgSearchThread {

		private Handler handler = null;

		public geocachesLoadNextPage(Handler handlerIn) {
			handler = handlerIn;
		}

		@Override
		public void run() {
			searchId = base.searchByNextPage(this, searchId, 0, settings.showCaptcha);

			handler.sendMessage(new Message());
		}
	}

	private class geocachesLoadByCoords extends cgSearchThread {

		private Handler handler = null;
		private Double latitude = null;
		private Double longitude = null;
		private String cachetype = null;

		public geocachesLoadByCoords(Handler handlerIn, Double latitudeIn, Double longitudeIn, String cachetypeIn) {
			setPriority(Thread.MIN_PRIORITY);

			handler = handlerIn;
			latitude = latitudeIn;
			longitude = longitudeIn;
			cachetype = cachetypeIn;

			if (latitude == null || longitude == null) {
				warning.showToast(res.getString(R.string.warn_no_coordinates));

				finish();
				return;
			}
		}

		@Override
		public void run() {
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("latitude", String.format((Locale) null, "%.6f", latitude));
			params.put("longitude", String.format((Locale) null, "%.6f", longitude));
			params.put("cachetype", cachetype);

			searchId = base.searchByCoords(this, params, 0, settings.showCaptcha);

			handler.sendMessage(new Message());
		}
	}

	private class geocachesLoadByKeyword extends cgSearchThread {

		private Handler handler = null;
		private String keyword = null;
		private String cachetype = null;

		public geocachesLoadByKeyword(Handler handlerIn, String keywordIn, String cachetypeIn) {
			setPriority(Thread.MIN_PRIORITY);

			handler = handlerIn;
			keyword = keywordIn;
			cachetype = cachetypeIn;

			if (keyword == null) {
				warning.showToast(res.getString(R.string.warn_no_keyword));

				finish();
				return;
			}
		}

		@Override
		public void run() {
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("keyword", keyword);
			params.put("cachetype", cachetype);

			searchId = base.searchByKeyword(this, params, 0, settings.showCaptcha);

			handler.sendMessage(new Message());
		}
	}

	private class geocachesLoadByUserName extends cgSearchThread {

		private Handler handler = null;
		private String username = null;
		private String cachetype = null;

		public geocachesLoadByUserName(Handler handlerIn, String usernameIn, String cachetypeIn) {
			setPriority(Thread.MIN_PRIORITY);

			handler = handlerIn;
			username = usernameIn;
			cachetype = cachetypeIn;

			if (username == null || username.length() == 0) {
				warning.showToast(res.getString(R.string.warn_no_username));

				finish();
				return;
			}
		}

		@Override
		public void run() {
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("username", username);
			params.put("cachetype", cachetype);

			searchId = base.searchByUsername(this, params, 0, settings.showCaptcha);

			handler.sendMessage(new Message());
		}
	}

	private class geocachesLoadByOwner extends cgSearchThread {

		private Handler handler = null;
		private String username = null;
		private String cachetype = null;

		public geocachesLoadByOwner(Handler handlerIn, String usernameIn, String cachetypeIn) {
			setPriority(Thread.MIN_PRIORITY);

			handler = handlerIn;
			username = usernameIn;
			cachetype = cachetypeIn;

			if (username == null || username.length() == 0) {
				warning.showToast(res.getString(R.string.warn_no_username));

				finish();
				return;
			}
		}

		@Override
		public void run() {
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("username", username);
			params.put("cachetype", cachetype);

			searchId = base.searchByOwner(this, params, 0, settings.showCaptcha);

			handler.sendMessage(new Message());
		}
	}

	private class geocachesLoadDetails extends Thread {

		private Handler handler = null;
		private int reason = 1;
		private volatile boolean needToStop = false;
		private int checked = 0;
		private long last = 0l;

		public geocachesLoadDetails(Handler handlerIn, int reasonIn) {
			setPriority(Thread.MIN_PRIORITY);

			handler = handlerIn;
			reason = reasonIn;

			if (adapter != null) {
				checked = adapter.getChecked();
			}
		}

		public void kill() {
			needToStop = true;
		}

		@Override
		public void run() {
			if (dir != null) {
				dir = app.removeDir();
			}
			if (geo != null) {
				geo = app.removeGeo();
			}

			final ArrayList<cgCache> cacheListTemp = (ArrayList<cgCache>) cacheList.clone();
			for (cgCache cache : cacheListTemp) {
				if (checked > 0 && cache.statusChecked == false) {
					handler.sendEmptyMessage(0);

					yield();
					continue;
				}

				try {
					if (needToStop == true) {
						Log.i(cgSettings.tag, "Stopped storing process.");
						break;
					}

					if ((System.currentTimeMillis() - last) < 1500) {
						try {
							int delay = 1000 + ((Double) (Math.random() * 1000)).intValue() - (int) (System.currentTimeMillis() - last);
							if (delay < 0) {
								delay = 500;
							}

							Log.i(cgSettings.tag, "Waiting for next cache " + delay + " ms");
							sleep(delay);
						} catch (Exception e) {
							Log.e(cgSettings.tag, "cgeocaches.geocachesLoadDetails.sleep: " + e.toString());
						}
					}

					if (needToStop == true) {
						Log.i(cgSettings.tag, "Stopped storing process.");
						break;
					}

					detailProgress++;
					base.storeCache(app, activity, cache, null, reason, handler);

					handler.sendEmptyMessage(cacheList.indexOf(cache));

					yield();
				} catch (Exception e) {
					Log.e(cgSettings.tag, "cgeocaches.geocachesLoadDetails: " + e.toString());
				}

				last = System.currentTimeMillis();
			}
			cacheListTemp.clear();

			handler.sendEmptyMessage(-1);
		}
	}

	private class geocachesLoadFromWeb extends Thread {

		private Handler handler = null;
		private int reason = 1;
		private volatile boolean needToStop = false;

		public geocachesLoadFromWeb(Handler handlerIn, int reasonIn) {
			setPriority(Thread.MIN_PRIORITY);

			handler = handlerIn;
			reason = reasonIn;
		}

		public void kill() {
			needToStop = true;
		}

		@Override
		public void run() {
			if (dir != null) {
				dir = app.removeDir();
			}
			if (geo != null) {
				geo = app.removeGeo();
			}

			int delay=-1;
			int times=0;

			while (times < 3 * 60 / 5) // maximum: 3 minutes, every 5 seconds
			{
				if (needToStop)
				{
					handler.sendEmptyMessage(-1);
					break;
				}

				//download new code
				String deviceCode = settings.webDeviceCode;
				if (deviceCode == null) {
					deviceCode = "";
				}
				cgResponse responseFromWeb = base.request(false, "send2cgeo.carnero.cc", "/readCode.php", "GET", "d=" + cgBase.urlencode_rfc3986(deviceCode), 0, true);

				if ((responseFromWeb.getStatusCode() == 200)
						&& (responseFromWeb.getData().length() > 2)) {

					String GCcode = responseFromWeb.getData();

					delay = 1;
					Message mes = new Message();
					mes.what = 1;
					mes.obj = GCcode;
					handler.sendMessage(mes);
					yield();

					base.storeCache(app, activity, null, GCcode, reason, null);

					Message mes1 = new Message();
					mes1.what = 2;
					mes1.obj = GCcode;
					handler.sendMessage(mes1);
					yield();
				} else {
					delay = 0;
					handler.sendEmptyMessage(0);
					yield();
				}
				if (responseFromWeb.getStatusCode() != 200) {
					needToStop = true;
					settings.setWebNameCode(null, null);
					handler.sendEmptyMessage(-2);
					return;
				}

				try {
					yield();
					if (delay==0)
					{
						sleep(5000); //No caches 5s
						times++;
					} else {
						sleep(500); //Cache was loaded 0.5s
						times=0;
					}
				} catch (InterruptedException e) {
					Log.e(cgSettings.tag, "cgeocaches.geocachesLoadFromWeb.sleep: " + e.toString());
				}
			}
			handler.sendEmptyMessage(-1);
		}
	}

	private class geocachesDropDetails extends Thread {

		private Handler handler = null;
		private volatile boolean needToStop = false;
		private int checked = 0;

		public geocachesDropDetails(Handler handlerIn) {
			setPriority(Thread.MIN_PRIORITY);

			handler = handlerIn;

			if (adapter != null) {
				checked = adapter.getChecked();
			}
		}

		public void kill() {
			needToStop = true;
		}

		@Override
		public void run() {
			if (dir != null) {
				dir = app.removeDir();
			}
			if (geo != null) {
				geo = app.removeGeo();
			}

			final ArrayList<cgCache> cacheListTemp = (ArrayList<cgCache>) cacheList.clone();
			for (cgCache cache : cacheListTemp) {
				if (checked > 0 && cache.statusChecked == false) {
					handler.sendEmptyMessage(0);

					yield();
					continue;
				}

				try {
					if (needToStop == true) {
						Log.i(cgSettings.tag, "Stopped dropping process.");
						break;
					}

					app.markDropped(cache.geocode);

					handler.sendEmptyMessage(cacheList.indexOf(cache));

					yield();
				} catch (Exception e) {
					Log.e(cgSettings.tag, "cgeocaches.geocachesDropDetails: " + e.toString());
				}
			}
			cacheListTemp.clear();

			handler.sendEmptyMessage(-1);
		}
	}

	private class geocachesExportFieldNotes extends Thread
	{
        private Handler handler = null;
        private volatile boolean needToStop = false;
        private int checked = 0;

        public geocachesExportFieldNotes(Handler handlerIn)
        {
            setPriority(Thread.MIN_PRIORITY);

            handler = handlerIn;

            if (adapter != null)
            {
                checked = adapter.getChecked();
            }
        }

        public void kill()
        {
            needToStop = true;
        }

        @Override
        public void run()
        {
            SimpleDateFormat fieldNoteDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            StringBuffer fieldNoteBuffer = new StringBuffer(500);

            // We need our own HashMap because cgBase.LogTypes1 will give us localized and maybe
            // different strings than gc.com expects in the field note
            // We only need such logtypes that are possible to log via c:geo
            HashMap<Integer, String> logTypes = new HashMap<Integer, String>();
            logTypes.put(cgBase.LOG_FOUND_IT, "Found it");
            logTypes.put(cgBase.LOG_DIDNT_FIND_IT, "Didn't find it");
            logTypes.put(cgBase.LOG_NOTE, "Write Note");
            logTypes.put(cgBase.LOG_NEEDS_ARCHIVE, "Needs archived");
            //logTypes.put(cgBase.LOG_NEEDS_MAINTENANCE, "Needs Maintenance"); // TODO: Strange problems, gc.com aborts with a parse-error

            for (cgCache cache : cacheList) {
                if (checked > 0 && cache.statusChecked == false) {
                    handler.sendEmptyMessage(0);

                    yield();
                    continue;
                }

                try {
                    if (needToStop == true)
                    {
                        Log.i(cgSettings.tag, "Stopped exporting process.");
                        break;
                    }

                    if (cache.logOffline)
                    {
                        cgLog log = app.loadLogOffline(cache.geocode);

                        if (null != logTypes.get(log.type))
                        {
                            fieldNoteBuffer.append(cache.geocode)
                                           .append(",")
                                           .append(fieldNoteDateFormat.format(new Date(log.date)))
                                           .append(",")
                                           .append(logTypes.get(log.type))
                                           .append(",\"")
                                           .append(log.log.replaceAll("\"", "'"))
                                           .append("\"\n");
                        }
                    }

                    detailProgress++;

                    handler.sendEmptyMessage(cacheList.indexOf(cache));

                    yield();
                } catch (Exception e) {
                    Log.e(cgSettings.tag, "cgeocaches.geocachesExportFieldNotes: " + e.toString());
                }
            }

            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            {
                File exportLocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/field-notes");
                exportLocation.mkdirs();

                SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                File exportFile = new File(exportLocation + "/" + fileNameDateFormat.format(new Date()) + ".txt");

                OutputStream os = null;
                Writer       fw = null;
                try
                {
                    os = new FileOutputStream(exportFile);
                    fw = new OutputStreamWriter(os, "ISO-8859-1"); // TODO: gc.com doesn't support UTF-8
                    fw.write(fieldNoteBuffer.toString());

                    Message.obtain(handler, -2, exportFile).sendToTarget();
                }
                catch (IOException e) {
                    Log.e(cgSettings.tag, "cgeocaches.geocachesExportFieldNotes: " + e.toString());
                    handler.sendEmptyMessage(-3);
                }
                finally
                {
                    if ( fw != null )
                    {
                        try { fw.close(); } catch (IOException e) { Log.e(cgSettings.tag, "cgeocaches.geocachesExportFieldNotes: " + e.toString()); }
                    }
                }
            }

            handler.sendEmptyMessage(-1);
        }
    }

	private class moreCachesListener implements View.OnClickListener {

		@Override
		public void onClick(View arg0) {
			base.showProgress(activity, true);
			setLoadingCaches();
			listFooter.setOnClickListener(null);

			geocachesLoadNextPage thread;
			thread = new geocachesLoadNextPage(loadNextPageHandler);
			thread.setRecaptchaHandler(new cgSearchHandler(activity, res, thread));
			thread.start();
		}
	}

	private void hideLoading() {
		final ListView list = getListView();
		final RelativeLayout loading = (RelativeLayout) findViewById(R.id.loading);

		if (list.getVisibility() == View.GONE) {
			list.setVisibility(View.VISIBLE);
			loading.setVisibility(View.GONE);
		}
	}

	public void selectList(View view) {
		if (type.equals("offline") == false) {
			return;
		}

		lists = app.getLists();

		if (lists == null) {
			return;
		}

		final ArrayList<CharSequence> listsTitle = new ArrayList<CharSequence>();
		for (cgList list : lists) {
			listsTitle.add(list.title);
		}

		final CharSequence[] items = new CharSequence[listsTitle.size()];

		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(res.getString(R.string.list_title));
		builder.setItems(listsTitle.toArray(items), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialogInterface, int item) {
				switchListByOrder(item);

				return;
			}
		});
		builder.create().show();
	}

	public void switchListByOrder(int order) {
		switchList(-1, order);
	}

	public void switchListById(int id) {
		switchList(id, -1);
	}

	public void switchList(int id, int order) {
		cgList list = null;

		if (id >= 0) {
			list = app.getList(id);
		} else if (order >= 0) {
			lists = app.getLists();
			list = lists.get(order);
		} else {
			return;
		}

		if (list == null) {
			return;
		}

		listId = list.id;
		title = list.title;

		settings.saveLastList(listId);

		base.showProgress(activity, true);
		setLoadingCaches();

		Handler handlerMove = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				Thread threadPure = new geocachesLoadByOffline(loadCachesHandler, latitude, longitude, msg.what);
				threadPure.start();
			}
		};

		(new moveCachesToList(listId, handlerMove)).start();
	}

	private class moveCachesToList extends Thread {
		int listId = -1;
		Handler handler = null;

		public moveCachesToList(int listIdIn, Handler handlerIn) {
			listId = listIdIn;
			handler = handlerIn;
		}

		@Override
		public void run() {
			int checked = adapter.getChecked();
			if (checked > 0) {
				final ArrayList<cgCache> cacheListTemp = (ArrayList<cgCache>) cacheList.clone();
				for (cgCache cache : cacheListTemp) {
					if (cache.statusChecked != false) {
						app.moveToList(cache.geocode, listId);
					}
				}
			}

			handler.sendEmptyMessage(listId);
		}
	}

	private void createList() {
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		final View view = inflater.inflate(R.layout.list_create_dialog, null);
		final EditText input = (EditText) view.findViewById(R.id.text);

		alert.setTitle(R.string.list_dialog_create_title);
		alert.setView(view);
		alert.setPositiveButton(R.string.list_dialog_create, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				// remove whitespaces added by autocompletion of Android keyboard
				if (value != null) {
					value = value.trim();
				}

				if (value != null && value.length() > 0) {
					int newId = app.createList(value);

					if (newId >= 10) {
						warning.showToast(res.getString(R.string.list_dialog_create_ok));
					} else {
						warning.showToast(res.getString(R.string.list_dialog_create_err));
					}
				}
			}
		});
		alert.setNegativeButton(res.getString(R.string.list_dialog_cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
			}
		});

		alert.show();
	}

	private void removeList() {
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle(R.string.list_dialog_remove_title);
		alert.setMessage(R.string.list_dialog_remove_description);
		alert.setPositiveButton(R.string.list_dialog_remove, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				boolean status = app.removeList(listId);

				if (status) {
					warning.showToast(res.getString(R.string.list_dialog_remove_ok));
					switchListById(1);
				} else {
					warning.showToast(res.getString(R.string.list_dialog_remove_err));
				}
			}
		});
		alert.setNegativeButton(res.getString(R.string.list_dialog_cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
			}
		});

		alert.show();
	}

	public void goMap(View view) {
		if (searchId == null || searchId == 0 || cacheList == null || cacheList.isEmpty() == true) {
			warning.showToast(res.getString(R.string.warn_no_cache_coord));

			return;
		}

		Intent mapIntent = new Intent(activity, settings.getMapFactory().getMapClass());
		mapIntent.putExtra("detail", false);
		mapIntent.putExtra("searchid", searchId);

		activity.startActivity(mapIntent);
	}

	public void goHome(View view) {
		base.goHome(activity);
	}

	public void goManual(View view) {
		try {
			if (type != null && type.equals("offline") == true) {
				AppManualReaderClient.openManual(
						"c-geo",
						"c:geo-stored",
						activity,
						"http://cgeo.carnero.cc/manual/");
			} else if (type != null && type.equals("history") == true) {
				AppManualReaderClient.openManual(
						"c-geo",
						"c:geo-history",
						activity,
						"http://cgeo.carnero.cc/manual/");
			} else {
				AppManualReaderClient.openManual(
						"c-geo",
						"c:geo-nearby",
						activity,
						"http://cgeo.carnero.cc/manual/");
			}
		} catch (Exception e) {
			// nothing
		}
	}
}
