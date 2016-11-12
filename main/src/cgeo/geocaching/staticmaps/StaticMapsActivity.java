package cgeo.geocaching.staticmaps;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractListActivity;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.Log;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;

import java.util.concurrent.Callable;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.OptionsItem;

@EActivity
public class StaticMapsActivity extends AbstractListActivity {

    @Extra(Intents.EXTRA_DOWNLOAD)
    boolean download = false;
    @Extra(Intents.EXTRA_WAYPOINT_ID)
    Integer waypointId = null;
    @Extra(Intents.EXTRA_GEOCODE)
    String geocode = null;

    private Geocache cache;
    private ProgressDialog waitDialog = null;
    private StaticMapsAdapter adapter;
    private MenuItem menuRefresh;
    private CompositeDisposable resumeDisposables = new CompositeDisposable();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.staticmaps_activity);
    }

    @Override
    public void onStart() {
        super.onStart();

        cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);

        if (cache == null) {
            Log.e("StaticMapsActivity.onCreate: cannot find the cache " + geocode);
            finish();
            return;
        }

        setCacheTitleBar(cache);

        adapter = new StaticMapsAdapter(this);
        setListAdapter(adapter);

    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.static_maps_activity_options, menu);
        menuRefresh = menu.findItem(R.id.menu_refresh);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.clear();

        final Disposable load = loadAndDisplay();
        resumeDisposables.add(load);
        waitDialog = ProgressDialog.show(this, null, res.getString(R.string.map_static_loading), true);
        waitDialog.setCancelable(true);
        waitDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(final DialogInterface dialog) {
                load.dispose();
            }
        });
    }

    @Override
    public void onPause() {
        resumeDisposables.clear();
        super.onPause();
    }

    @NonNull
    private Disposable loadAndDisplay() {
        return loadMaps().observeOn(AndroidSchedulers.mainThread()).map(new Function<Bitmap, Bitmap>() {
            @Override
            public Bitmap apply(final Bitmap bitmap) throws Exception {
                adapter.add(bitmap);
                return bitmap;
            }
        }).ignoreElements().subscribe(new Action() {
            @Override
            public void run() {
                Dialogs.dismiss(waitDialog);
                if (adapter.isEmpty()) {
                    if (download) {
                        resumeDisposables.add(downloadStaticMaps().subscribe(new Consumer<Boolean>() {
                            @Override
                            public void accept(final Boolean succeeded) throws Exception {
                                if (succeeded) {
                                    // Loading from disk will succeed this time
                                    AndroidSchedulers.mainThread().scheduleDirect(new Runnable() {
                                        @Override
                                        public void run() {
                                            adapter.clear();
                                            resumeDisposables.add(loadAndDisplay());
                                        }
                                    });
                                } else {
                                    showToast(res.getString(R.string.err_detail_google_maps_limit_reached));
                                }
                            }
                        }));
                    } else {
                        showToast(res.getString(R.string.err_detail_not_load_map_static));
                        finish();
                    }
                } else {
                    if (menuRefresh != null) {
                        menuRefresh.setEnabled(true);
                    }
                }
            }
        });
    }

    private Observable<Bitmap> loadMaps() {
        return Observable.range(1, StaticMapsProvider.MAPS_LEVEL_MAX).concatMap(new Function<Integer, Observable<Bitmap>>() {
            @Override
            public Observable<Bitmap> apply(final Integer zoomLevel) throws Exception {
                return Maybe.fromCallable(new Callable<Bitmap>() {
                    @Override
                    public Bitmap call() throws Exception {
                        return waypointId != null ?
                                StaticMapsProvider.getWaypointMap(geocode, cache.getWaypointById(waypointId), zoomLevel) :
                                StaticMapsProvider.getCacheMap(geocode, zoomLevel);
                    }
                }).toObservable().subscribeOn(Schedulers.io());
            }
        });
    }

    @OptionsItem(R.id.menu_refresh)
    void refreshMaps() {
        menuRefresh.setEnabled(false);
        downloadStaticMaps().toCompletable().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action() {
            @Override
            public void run() throws Exception {
                menuRefresh.setEnabled(true);
                loadMaps();
            }
        });
    }

    private Single<Boolean> downloadStaticMaps() {
        if (waypointId == null) {
            showToast(res.getString(R.string.info_storing_static_maps));
            return StaticMapsProvider.storeCacheStaticMap(cache).andThen(Single.fromCallable(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return cache.hasStaticMap();
                }
            }));
        }
        final Waypoint waypoint = cache.getWaypointById(waypointId);
        if (waypoint != null) {
            showToast(res.getString(R.string.info_storing_static_maps));
            // refresh always removes old waypoint files
            StaticMapsProvider.removeWpStaticMaps(waypoint, geocode);
            return StaticMapsProvider.storeWaypointStaticMap(cache, waypoint).andThen(Single.fromCallable(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return StaticMapsProvider.hasStaticMapForWaypoint(geocode, waypoint);
                }
            }));
        }
        showToast(res.getString(R.string.err_detail_not_load_map_static));
        return Single.just(false);
    }

    @Override
    public void finish() {
        Dialogs.dismiss(waitDialog);
        super.finish();
    }
}
