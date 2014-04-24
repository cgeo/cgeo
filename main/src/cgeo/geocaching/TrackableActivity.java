package cgeo.geocaching;

import butterknife.ButterKnife;
import butterknife.InjectView;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.AbstractViewPagerActivity;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.trackable.TrackableConnector;
import cgeo.geocaching.connector.trackable.TravelBugConnector;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.geopoint.Units;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.ui.AbstractCachingPageViewCreator;
import cgeo.geocaching.ui.AnchorAwareLinkMovementMethod;
import cgeo.geocaching.ui.CacheDetailsCreator;
import cgeo.geocaching.ui.Formatter;
import cgeo.geocaching.ui.UserActionsClickListener;
import cgeo.geocaching.ui.UserNameClickListener;
import cgeo.geocaching.ui.logs.TrackableLogsViewCreator;
import cgeo.geocaching.utils.HtmlUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.UnknownTagsHandler;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import rx.android.observables.AndroidObservable;
import rx.android.observables.ViewObservable;
import rx.functions.Action1;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.text.Html;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TrackableActivity extends AbstractViewPagerActivity<TrackableActivity.Page> {

    public enum Page {
        DETAILS(R.string.detail),
        LOGS(R.string.cache_logs);

        private final int resId;

        Page(final int resId) {
            this.resId = resId;
        }
    }

    private Trackable trackable = null;
    private String geocode = null;
    private String name = null;
    private String guid = null;
    private String id = null;
    private LayoutInflater inflater = null;
    private ProgressDialog waitDialog = null;
    private final Handler loadTrackableHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (trackable == null) {
                if (waitDialog != null) {
                    waitDialog.dismiss();
                }

                if (StringUtils.isNotBlank(geocode)) {
                    showToast(res.getString(R.string.err_tb_find) + " " + geocode + ".");
                } else {
                    showToast(res.getString(R.string.err_tb_find_that));
                }

                finish();
                return;
            }

            try {
                inflater = getLayoutInflater();
                geocode = trackable.getGeocode();

                if (StringUtils.isNotBlank(trackable.getName())) {
                    setTitle(Html.fromHtml(trackable.getName()).toString());
                } else {
                    setTitle(trackable.getName());
                }

                invalidateOptionsMenuCompatible();
                reinitializeViewPager();

            } catch (final Exception e) {
                Log.e("TrackableActivity.loadTrackableHandler: ", e);
            }

            if (waitDialog != null) {
                waitDialog.dismiss();
            }

            // if we have a newer Android device setup Android Beam for easy cache sharing
            initializeAndroidBeam(
                    new ActivitySharingInterface() {
                        @Override
                        public String getUri() {
                            return trackable.getCgeoUrl();
                        }
                    }
            );
        }
    };

    private CharSequence clickedItemText = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.viewpager_activity);

        // set title in code, as the activity needs a hard coded title due to the intent filters
        setTitle(res.getString(R.string.trackable));

        // get parameters
        final Bundle extras = getIntent().getExtras();
        final Uri uri = getIntent().getData();

        // try to get data from extras
        if (extras != null) {
            geocode = extras.getString(Intents.EXTRA_GEOCODE);
            name = extras.getString(Intents.EXTRA_NAME);
            guid = extras.getString(Intents.EXTRA_GUID);
            id = extras.getString(Intents.EXTRA_ID);
        }

        // try to get data from URI
        if (geocode == null && guid == null && id == null && uri != null) {
            geocode = ConnectorFactory.getTrackableFromURL(uri.toString());

            final String uriHost = uri.getHost().toLowerCase(Locale.US);
            if (uriHost.contains("geocaching.com")) {
                geocode = uri.getQueryParameter("tracker");
                guid = uri.getQueryParameter("guid");
                id = uri.getQueryParameter("id");

                if (StringUtils.isNotBlank(geocode)) {
                    geocode = geocode.toUpperCase(Locale.US);
                    guid = null;
                    id = null;
                } else if (StringUtils.isNotBlank(guid)) {
                    geocode = null;
                    guid = guid.toLowerCase(Locale.US);
                    id = null;
                } else if (StringUtils.isNotBlank(id)) {
                    geocode = null;
                    guid = null;
                    id = id.toLowerCase(Locale.US);
                } else {
                    showToast(res.getString(R.string.err_tb_details_open));
                    finish();
                    return;
                }
            } else if (uriHost.contains("coord.info")) {
                final String uriPath = uri.getPath().toLowerCase(Locale.US);
                if (StringUtils.startsWith(uriPath, "/tb")) {
                    geocode = uriPath.substring(1).toUpperCase(Locale.US);
                    guid = null;
                    id = null;
                } else {
                    showToast(res.getString(R.string.err_tb_details_open));
                    finish();
                    return;
                }
            }
        }

        // no given data
        if (geocode == null && guid == null && id == null) {
            showToast(res.getString(R.string.err_tb_display));
            finish();
            return;
        }

        String message;
        if (StringUtils.isNotBlank(name)) {
            message = Html.fromHtml(name).toString();
        } else if (StringUtils.isNotBlank(geocode)) {
            message = geocode;
        } else {
            message = res.getString(R.string.trackable);
        }
        waitDialog = ProgressDialog.show(this, message, res.getString(R.string.trackable_details_loading), true, true);

        createViewPager(0, null);
        final LoadTrackableThread thread = new LoadTrackableThread(loadTrackableHandler, geocode, guid, id);
        thread.start();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info) {
        super.onCreateContextMenu(menu, view, info);
        final int viewId = view.getId();
        assert view instanceof TextView;
        clickedItemText = ((TextView) view).getText();
        switch (viewId) {
            case R.id.value: // name, TB-code, origin, released, distance
                final String itemTitle = (String) ((TextView) ((View) view.getParent()).findViewById(R.id.name)).getText();
                buildDetailsContextMenu(menu, clickedItemText, itemTitle, true);
                break;
            case R.id.goal:
                buildDetailsContextMenu(menu, clickedItemText, res.getString(R.string.trackable_goal), false);
                break;
            case R.id.details:
                buildDetailsContextMenu(menu, clickedItemText, res.getString(R.string.trackable_details), false);
                break;
            case R.id.log:
                buildDetailsContextMenu(menu, clickedItemText, res.getString(R.string.cache_logs), false);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return onClipboardItemSelected(item, clickedItemText) || onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.trackable_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_log_touch:
                LogTrackableActivity.startActivity(this, trackable);
                return true;
            case R.id.menu_browser_trackable:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(trackable.getBrowserUrl())));
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (trackable != null) {
            menu.findItem(R.id.menu_log_touch).setEnabled(StringUtils.isNotBlank(geocode) && trackable.isLoggable());
            menu.findItem(R.id.menu_browser_trackable).setEnabled(StringUtils.isNotBlank(trackable.getBrowserUrl()));
        }
        return super.onPrepareOptionsMenu(menu);
    }

    private class LoadTrackableThread extends Thread {
        final private Handler handler;
        final private String geocode;
        final private String guid;
        final private String id;

        public LoadTrackableThread(Handler handlerIn, String geocodeIn, String guidIn, String idIn) {
            handler = handlerIn;
            geocode = geocodeIn;
            guid = guidIn;
            id = idIn;
        }

        @Override
        public void run() {
            if (StringUtils.isNotEmpty(geocode)) {
                trackable = DataStore.loadTrackable(geocode);

                if (trackable == null || trackable.isLoggable()) {
                    // iterate over the connectors as some codes may be handled by multiple connectors
                    for (final TrackableConnector trackableConnector : ConnectorFactory.getTrackableConnectors()) {
                        if (trackableConnector.canHandleTrackable(geocode)) {
                            trackable = trackableConnector.searchTrackable(geocode, guid, id);
                            if (trackable != null) {
                                break;
                            }
                        }
                    }
                }
            }
            // fall back to GC search by GUID
            if (trackable == null) {
                trackable = TravelBugConnector.getInstance().searchTrackable(geocode, guid, id);
            }
            handler.sendMessage(Message.obtain());
        }
    }

    private class TrackableIconThread extends Thread {
        final private String url;
        final private Handler handler;

        public TrackableIconThread(String urlIn, Handler handlerIn) {
            url = urlIn;
            handler = handlerIn;
        }

        @Override
        public void run() {
            if (url == null || handler == null) {
                return;
            }

            try {
                final HtmlImage imgGetter = new HtmlImage(trackable.getGeocode(), false, 0, false);

                final BitmapDrawable image = imgGetter.getDrawable(url);
                final Message message = handler.obtainMessage(0, image);
                handler.sendMessage(message);
            } catch (final Exception e) {
                Log.e("TrackableActivity.TrackableIconThread.run: ", e);
            }
        }
    }

    private static class TrackableIconHandler extends Handler {
        final private ActionBar view;

        public TrackableIconHandler(ActionBar viewIn) {
            view = viewIn;
        }

        @Override
        public void handleMessage(Message message) {
            final BitmapDrawable image = (BitmapDrawable) message.obj;
            if (image != null && view != null) {
                image.setBounds(0, 0, view.getHeight(), view.getHeight());
                view.setIcon(image);
            }
        }
    }

    public static void startActivity(final AbstractActivity fromContext,
            final String guid, final String geocode, final String name) {
        final Intent trackableIntent = new Intent(fromContext, TrackableActivity.class);
        trackableIntent.putExtra(Intents.EXTRA_GUID, guid);
        trackableIntent.putExtra(Intents.EXTRA_GEOCODE, geocode);
        trackableIntent.putExtra(Intents.EXTRA_NAME, name);
        fromContext.startActivity(trackableIntent);
    }

    @Override
    protected PageViewCreator createViewCreator(Page page) {
        switch (page) {
            case DETAILS:
                return new DetailsViewCreator();
            case LOGS:
                return new TrackableLogsViewCreator(this, trackable);
        }
        throw new IllegalStateException(); // cannot happen as long as switch case is enum complete
    }

    @Override
    protected String getTitle(Page page) {
        return res.getString(page.resId);
    }

    @Override
    protected Pair<List<? extends Page>, Integer> getOrderedPages() {
        final List<Page> pages = new ArrayList<TrackableActivity.Page>();
        pages.add(Page.DETAILS);
        if (!trackable.getLogs().isEmpty()) {
            pages.add(Page.LOGS);
        }
        return new ImmutablePair<List<? extends Page>, Integer>(pages, 0);
    }

    public class DetailsViewCreator extends AbstractCachingPageViewCreator<ScrollView> {

        @InjectView(R.id.goal_box) protected LinearLayout goalBox;
        @InjectView(R.id.goal) protected TextView goalTextView;
        @InjectView(R.id.details_box) protected LinearLayout detailsBox;
        @InjectView(R.id.details) protected TextView detailsTextView;
        @InjectView(R.id.image_box) protected LinearLayout imageBox;
        @InjectView(R.id.details_list) protected LinearLayout detailsList;
        @InjectView(R.id.image) protected LinearLayout imageView;

        @Override
        public ScrollView getDispatchedView() {
            view = (ScrollView) getLayoutInflater().inflate(R.layout.trackable_details_view, null);
            ButterKnife.inject(this, view);

            final CacheDetailsCreator details = new CacheDetailsCreator(TrackableActivity.this, detailsList);

            // action bar icon
            if (StringUtils.isNotBlank(trackable.getIconUrl())) {
                final TrackableIconHandler iconHandler = new TrackableIconHandler(getSupportActionBar());
                final TrackableIconThread iconThread = new TrackableIconThread(trackable.getIconUrl(), iconHandler);
                iconThread.start();
            }

            // trackable name
            registerForContextMenu(details.add(R.string.trackable_name, StringUtils.isNotBlank(trackable.getName()) ? Html.fromHtml(trackable.getName()).toString() : res.getString(R.string.trackable_unknown)));

            // trackable type
            String tbType;
            if (StringUtils.isNotBlank(trackable.getType())) {
                tbType = Html.fromHtml(trackable.getType()).toString();
            } else {
                tbType = res.getString(R.string.trackable_unknown);
            }
            details.add(R.string.trackable_type, tbType);

            // trackable geocode
            registerForContextMenu(details.add(R.string.trackable_code, trackable.getGeocode()));

            // trackable owner
            final TextView owner = details.add(R.string.trackable_owner, res.getString(R.string.trackable_unknown));
            if (StringUtils.isNotBlank(trackable.getOwner())) {
                owner.setText(Html.fromHtml(trackable.getOwner()), TextView.BufferType.SPANNABLE);
                owner.setOnClickListener(new UserActionsClickListener(trackable));
            }

            // trackable spotted
            if (StringUtils.isNotBlank(trackable.getSpottedName()) ||
                    trackable.getSpottedType() == Trackable.SPOTTED_UNKNOWN ||
                    trackable.getSpottedType() == Trackable.SPOTTED_OWNER) {
                boolean showTimeSpan = true;
                StringBuilder text;

                if (trackable.getSpottedType() == Trackable.SPOTTED_CACHE) {
                    text = new StringBuilder(res.getString(R.string.trackable_spotted_in_cache) + ' ' + Html.fromHtml(trackable.getSpottedName()).toString());
                } else if (trackable.getSpottedType() == Trackable.SPOTTED_USER) {
                    text = new StringBuilder(res.getString(R.string.trackable_spotted_at_user) + ' ' + Html.fromHtml(trackable.getSpottedName()).toString());
                } else if (trackable.getSpottedType() == Trackable.SPOTTED_UNKNOWN) {
                    text = new StringBuilder(res.getString(R.string.trackable_spotted_unknown_location));
                } else if (trackable.getSpottedType() == Trackable.SPOTTED_OWNER) {
                    text = new StringBuilder(res.getString(R.string.trackable_spotted_owner));
                } else {
                    text = new StringBuilder("N/A");
                    showTimeSpan = false;
                }

                // days since last spotting
                if (showTimeSpan && trackable.getLogs() != null) {
                    for (final LogEntry log : trackable.getLogs()) {
                        if (log.type == LogType.RETRIEVED_IT || log.type == LogType.GRABBED_IT || log.type == LogType.DISCOVERED_IT || log.type == LogType.PLACED_IT) {
                            final int days = log.daysSinceLog();
                            text.append(" (").append(res.getQuantityString(R.plurals.days_ago, days, days)).append(')');
                            break;
                        }
                    }
                }

                final TextView spotted = details.add(R.string.trackable_spotted, text.toString());
                spotted.setClickable(true);
                if (Trackable.SPOTTED_CACHE == trackable.getSpottedType()) {
                    spotted.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View arg0) {
                            if (StringUtils.isNotBlank(trackable.getSpottedGuid())) {
                                CacheDetailActivity.startActivityGuid(TrackableActivity.this, trackable.getSpottedGuid(), trackable.getSpottedName());
                            }
                            else {
                                // for geokrety we only know the cache geocode
                                final String cacheCode = trackable.getSpottedName();
                                if (ConnectorFactory.canHandle(cacheCode)) {
                                    CacheDetailActivity.startActivity(TrackableActivity.this, cacheCode);
                                }
                            }
                        }
                    });
                } else if (Trackable.SPOTTED_USER == trackable.getSpottedType()) {
                    spotted.setOnClickListener(new UserNameClickListener(trackable, Html.fromHtml(trackable.getSpottedName()).toString()));
                } else if (Trackable.SPOTTED_OWNER == trackable.getSpottedType()) {
                    spotted.setOnClickListener(new UserNameClickListener(trackable, Html.fromHtml(trackable.getOwner()).toString()));
                }
            }

            // trackable origin
            if (StringUtils.isNotBlank(trackable.getOrigin())) {
                final TextView origin = details.add(R.string.trackable_origin, "");
                origin.setText(Html.fromHtml(trackable.getOrigin()), TextView.BufferType.SPANNABLE);
                registerForContextMenu(origin);
            }

            // trackable released
            if (trackable.getReleased() != null) {
                registerForContextMenu(details.add(R.string.trackable_released, Formatter.formatDate(trackable.getReleased().getTime())));
            }

            // trackable distance
            if (trackable.getDistance() >= 0) {
                registerForContextMenu(details.add(R.string.trackable_distance, Units.getDistanceFromKilometers(trackable.getDistance())));
            }

            // trackable goal
            if (StringUtils.isNotBlank(HtmlUtils.extractText(trackable.getGoal()))) {
                goalBox.setVisibility(View.VISIBLE);
                goalTextView.setVisibility(View.VISIBLE);
                goalTextView.setText(Html.fromHtml(trackable.getGoal(), new HtmlImage(geocode, true, 0, false), null), TextView.BufferType.SPANNABLE);
                goalTextView.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
                registerForContextMenu(goalTextView);
            }

            // trackable details
            if (StringUtils.isNotBlank(HtmlUtils.extractText(trackable.getDetails()))) {
                detailsBox.setVisibility(View.VISIBLE);
                detailsTextView.setVisibility(View.VISIBLE);
                detailsTextView.setText(Html.fromHtml(trackable.getDetails(), new HtmlImage(geocode, true, 0, false), new UnknownTagsHandler()), TextView.BufferType.SPANNABLE);
                detailsTextView.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
                registerForContextMenu(detailsTextView);
            }

            // trackable image
            if (StringUtils.isNotBlank(trackable.getImage())) {
                imageBox.setVisibility(View.VISIBLE);
                final ImageView trackableImage = (ImageView) inflater.inflate(R.layout.trackable_image, null);

                trackableImage.setImageResource(R.drawable.image_not_loaded);
                trackableImage.setClickable(true);
                ViewObservable.clicks(trackableImage, false).subscribe(new Action1<View>() {
                    @Override
                    public void call(final View view) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(trackable.getImage())));
                    }
                });

                AndroidObservable.bindActivity(TrackableActivity.this, new HtmlImage(geocode, true, 0, false).fetchDrawable(trackable.getImage())).subscribe(new Action1<BitmapDrawable>() {
                    @Override
                    public void call(final BitmapDrawable bitmapDrawable) {
                        trackableImage.setImageDrawable(bitmapDrawable);
                    }
                });

                imageView.addView(trackableImage);
            }
            return view;
        }

    }

}
