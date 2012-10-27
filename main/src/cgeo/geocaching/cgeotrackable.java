package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.connector.gc.GCParser;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.geopoint.Units;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.ui.CacheDetailsCreator;
import cgeo.geocaching.ui.Formatter;
import cgeo.geocaching.utils.BaseUtils;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;

public class cgeotrackable extends AbstractActivity {
    private static final int MENU_LOG_TOUCH = 1;
    private static final int MENU_BROWSER_TRACKABLE = 2;
    private cgTrackable trackable = null;
    private String geocode = null;
    private String name = null;
    private String guid = null;
    private String id = null;
    private String contextMenuUser = null;
    private LayoutInflater inflater = null;
    private ProgressDialog waitDialog = null;
    private Handler loadTrackableHandler = new Handler() {

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
                geocode = trackable.getGeocode().toUpperCase();

                if (StringUtils.isNotBlank(trackable.getName())) {
                    setTitle(Html.fromHtml(trackable.getName()).toString());
                } else {
                    setTitle(trackable.getName().toUpperCase());
                }

                findViewById(R.id.details_list_box).setVisibility(View.VISIBLE);
                final CacheDetailsCreator details = new CacheDetailsCreator(cgeotrackable.this, (LinearLayout) findViewById(R.id.details_list));

                // action bar icon
                if (StringUtils.isNotBlank(trackable.getIconUrl())) {
                    final TrackableIconHandler iconHandler = new TrackableIconHandler(((TextView) findViewById(R.id.actionbar_title)));
                    final TrackableIconThread iconThread = new TrackableIconThread(trackable.getIconUrl(), iconHandler);
                    iconThread.start();
                }

                // trackable name
                details.add(R.string.trackable_name, StringUtils.isNotBlank(trackable.getName()) ? Html.fromHtml(trackable.getName()).toString() : res.getString(R.string.trackable_unknown));

                // trackable type
                String tbType;
                if (StringUtils.isNotBlank(trackable.getType())) {
                    tbType = Html.fromHtml(trackable.getType()).toString();
                } else {
                    tbType = res.getString(R.string.trackable_unknown);
                }
                details.add(R.string.trackable_type, tbType);

                // trackable geocode
                details.add(R.string.trackable_code, trackable.getGeocode().toUpperCase());

                // trackable owner
                TextView owner = details.add(R.string.trackable_owner, res.getString(R.string.trackable_unknown));
                if (StringUtils.isNotBlank(trackable.getOwner())) {
                    owner.setText(Html.fromHtml(trackable.getOwner()), TextView.BufferType.SPANNABLE);
                    owner.setOnClickListener(new UserActionsListener());
                }

                // trackable spotted
                if (StringUtils.isNotBlank(trackable.getSpottedName()) ||
                        trackable.getSpottedType() == cgTrackable.SPOTTED_UNKNOWN ||
                        trackable.getSpottedType() == cgTrackable.SPOTTED_OWNER
                ) {
                    boolean showTimeSpan = true;
                    StringBuilder text;

                    if (trackable.getSpottedType() == cgTrackable.SPOTTED_CACHE) {
                        text = new StringBuilder(res.getString(R.string.trackable_spotted_in_cache) + ' ' + Html.fromHtml(trackable.getSpottedName()).toString());
                    } else if (trackable.getSpottedType() == cgTrackable.SPOTTED_USER) {
                        text = new StringBuilder(res.getString(R.string.trackable_spotted_at_user) + ' ' + Html.fromHtml(trackable.getSpottedName()).toString());
                    } else if (trackable.getSpottedType() == cgTrackable.SPOTTED_UNKNOWN) {
                        text = new StringBuilder(res.getString(R.string.trackable_spotted_unknown_location));
                    } else if (trackable.getSpottedType() == cgTrackable.SPOTTED_OWNER) {
                        text = new StringBuilder(res.getString(R.string.trackable_spotted_owner));
                    } else {
                        text = new StringBuilder("N/A");
                        showTimeSpan = false;
                    }

                    // days since last spotting
                    if (showTimeSpan && trackable.getLogs() != null) {
                        for (LogEntry log : trackable.getLogs()) {
                            if (log.type == LogType.RETRIEVED_IT || log.type == LogType.GRABBED_IT || log.type == LogType.DISCOVERED_IT || log.type == LogType.PLACED_IT) {
                                final int days = log.daysSinceLog();
                                text.append(" (").append(res.getQuantityString(R.plurals.days_ago, days, days)).append(')');
                                break;
                            }
                        }
                    }

                    final TextView spotted = details.add(R.string.trackable_spotted, text.toString());
                    spotted.setClickable(true);
                    if (cgTrackable.SPOTTED_CACHE == trackable.getSpottedType()) {
                        spotted.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View arg0) {
                                CacheDetailActivity.startActivityGuid(cgeotrackable.this, trackable.getSpottedGuid(), trackable.getSpottedName());
                            }
                        });
                    } else if (cgTrackable.SPOTTED_USER == trackable.getSpottedType()) {
                        spotted.setOnClickListener(new UserActionsListener());
                    }
                }

                // trackable origin
                if (StringUtils.isNotBlank(trackable.getOrigin())) {
                    TextView origin = details.add(R.string.trackable_origin, "");
                    origin.setText(Html.fromHtml(trackable.getOrigin()), TextView.BufferType.SPANNABLE);
                }

                // trackable released
                if (trackable.getReleased() != null) {
                    details.add(R.string.trackable_released, Formatter.formatDate(trackable.getReleased().getTime()));
                }

                // trackable distance
                if (trackable.getDistance() >= 0) {
                    details.add(R.string.trackable_distance, Units.getDistanceFromKilometers(trackable.getDistance()));
                }

                // trackable goal
                if (StringUtils.isNotBlank(trackable.getGoal())) {
                    findViewById(R.id.goal_box).setVisibility(View.VISIBLE);
                    TextView descView = (TextView) findViewById(R.id.goal);
                    descView.setVisibility(View.VISIBLE);
                    descView.setText(Html.fromHtml(trackable.getGoal(), new HtmlImage(geocode, true, 0, false), null), TextView.BufferType.SPANNABLE);
                    descView.setMovementMethod(LinkMovementMethod.getInstance());
                }

                // trackable details
                if (StringUtils.isNotBlank(trackable.getDetails())) {
                    findViewById(R.id.details_box).setVisibility(View.VISIBLE);
                    TextView descView = (TextView) findViewById(R.id.details);
                    descView.setVisibility(View.VISIBLE);
                    descView.setText(Html.fromHtml(trackable.getDetails(), new HtmlImage(geocode, true, 0, false), null), TextView.BufferType.SPANNABLE);
                    descView.setMovementMethod(LinkMovementMethod.getInstance());
                }

                // trackable image
                if (StringUtils.isNotBlank(trackable.getImage())) {
                    findViewById(R.id.image_box).setVisibility(View.VISIBLE);
                    LinearLayout imgView = (LinearLayout) findViewById(R.id.image);

                    final ImageView trackableImage = (ImageView) inflater.inflate(R.layout.trackable_image, null);

                    trackableImage.setImageResource(R.drawable.image_not_loaded);
                    trackableImage.setClickable(true);
                    trackableImage.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View arg0) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(trackable.getImage())));
                        }
                    });

                    // try to load image
                    final Handler handler = new Handler() {

                        @Override
                        public void handleMessage(Message message) {
                            BitmapDrawable image = (BitmapDrawable) message.obj;
                            if (image != null) {
                                trackableImage.setImageDrawable((BitmapDrawable) message.obj);
                            }
                        }
                    };

                    new Thread() {

                        @Override
                        public void run() {
                            BitmapDrawable image;
                            try {
                                HtmlImage imgGetter = new HtmlImage(geocode, true, 0, false);

                                image = imgGetter.getDrawable(trackable.getImage());
                                Message message = handler.obtainMessage(0, image);
                                handler.sendMessage(message);
                            } catch (Exception e) {
                                Log.e("cgeospoilers.onCreate.onClick.run: " + e.toString());
                            }
                        }
                    }.start();

                    imgView.addView(trackableImage);
                }
            } catch (Exception e) {
                Log.e("cgeotrackable.loadTrackableHandler: " + e.toString() + Arrays.toString(e.getStackTrace()));
            }

            displayLogs();

            if (waitDialog != null) {
                waitDialog.dismiss();
            }
        }
    };

    public cgeotrackable() {
        super("c:geo-trackable-details");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme();
        setContentView(R.layout.trackable_detail);
        setTitle(res.getString(R.string.trackable));

        // get parameters
        Bundle extras = getIntent().getExtras();
        Uri uri = getIntent().getData();

        // try to get data from extras
        if (extras != null) {
            geocode = extras.getString("geocode");
            name = extras.getString("name");
            guid = extras.getString("guid");
            id = extras.getString("id");
        }

        // try to get data from URI
        if (geocode == null && guid == null && id == null && uri != null) {
            String uriHost = uri.getHost().toLowerCase();
            if (uriHost.contains("geocaching.com")) {
                geocode = uri.getQueryParameter("tracker");
                guid = uri.getQueryParameter("guid");
                id = uri.getQueryParameter("id");

                if (StringUtils.isNotBlank(geocode)) {
                    geocode = geocode.toUpperCase();
                    guid = null;
                    id = null;
                } else if (StringUtils.isNotBlank(guid)) {
                    geocode = null;
                    guid = guid.toLowerCase();
                    id = null;
                } else if (StringUtils.isNotBlank(id)) {
                    geocode = null;
                    guid = null;
                    id = id.toLowerCase();
                } else {
                    showToast(res.getString(R.string.err_tb_details_open));
                    finish();
                    return;
                }
            } else if (uriHost.contains("coord.info")) {
                String uriPath = uri.getPath().toLowerCase();
                if (uriPath != null && uriPath.startsWith("/tb")) {
                    geocode = uriPath.substring(1).toUpperCase();
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
            message = geocode.toUpperCase();
        } else {
            message = res.getString(R.string.trackable);
        }
        waitDialog = ProgressDialog.show(this, message, res.getString(R.string.trackable_details_loading), true, true);

        LoadTrackableThread thread = new LoadTrackableThread(loadTrackableHandler, geocode, guid, id);
        thread.start();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info) {
        super.onCreateContextMenu(menu, view, info);
        final int viewId = view.getId();

        if (viewId == R.id.author) { // Log item author
            contextMenuUser = ((TextView) view).getText().toString();
        } else { // Trackable owner, and user holding trackable now
            RelativeLayout itemLayout = (RelativeLayout) view.getParent();
            TextView itemName = (TextView) itemLayout.findViewById(R.id.name);

            String selectedName = itemName.getText().toString();
            if (selectedName.equals(res.getString(R.string.trackable_owner))) {
                contextMenuUser = trackable.getOwner();
            } else if (selectedName.equals(res.getString(R.string.trackable_spotted))) {
                contextMenuUser = trackable.getSpottedName();
            }
        }

        menu.setHeaderTitle(res.getString(R.string.user_menu_title) + " " + contextMenuUser);
        menu.add(viewId, 1, 0, res.getString(R.string.user_menu_view_hidden));
        menu.add(viewId, 2, 0, res.getString(R.string.user_menu_view_found));
        menu.add(viewId, 3, 0, res.getString(R.string.user_menu_open_browser));
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                cgeocaches.startActivityOwner(this, contextMenuUser);
                return true;
            case 2:
                cgeocaches.startActivityUserName(this, contextMenuUser);
                return true;
            case 3:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.geocaching.com/profile/?u=" + URLEncoder.encode(contextMenuUser))));
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_LOG_TOUCH, 0, res.getString(R.string.trackable_log_touch)).setIcon(R.drawable.ic_menu_agenda); // log touch
        menu.add(0, MENU_BROWSER_TRACKABLE, 0, res.getString(R.string.trackable_browser_open)).setIcon(R.drawable.ic_menu_info_details); // browser
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_LOG_TOUCH:
                LogTrackableActivity.startActivity(this, trackable);
                return true;
            case MENU_BROWSER_TRACKABLE:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(trackable.getUrl())));
                return true;
        }
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(MENU_LOG_TOUCH).setEnabled(StringUtils.isNotBlank(geocode) && trackable.isLoggable());
        menu.findItem(MENU_BROWSER_TRACKABLE).setEnabled(StringUtils.isNotBlank(trackable.getUrl()));
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
            // for non TB trackables, we should just use what we have in the database
            trackable = cgeoapplication.getInstance().getTrackableByGeocode(geocode);

            if ((trackable == null || trackable.isLoggable()) && !StringUtils.startsWithIgnoreCase(geocode, "GK")) {
                trackable = GCParser.searchTrackable(geocode, guid, id);
            }
            handler.sendMessage(Message.obtain());
        }
    }

    private void displayLogs() {
        // trackable logs
        LinearLayout listView = (LinearLayout) findViewById(R.id.log_list);
        listView.removeAllViews();

        RelativeLayout rowView;

        if (trackable != null && trackable.getLogs() != null) {
            for (LogEntry log : trackable.getLogs()) {
                rowView = (RelativeLayout) inflater.inflate(R.layout.trackable_logs_item, null);

                if (log.date > 0) {
                    ((TextView) rowView.findViewById(R.id.added)).setText(Formatter.formatShortDate(log.date));
                }

                ((TextView) rowView.findViewById(R.id.type)).setText(log.type.getL10n());
                ((TextView) rowView.findViewById(R.id.author)).setText(Html.fromHtml(log.author), TextView.BufferType.SPANNABLE);

                if (StringUtils.isBlank(log.cacheName)) {
                    rowView.findViewById(R.id.location).setVisibility(View.GONE);
                } else {
                    ((TextView) rowView.findViewById(R.id.location)).setText(Html.fromHtml(log.cacheName));
                    final String cacheGuid = log.cacheGuid;
                    final String cacheName = log.cacheName;
                    rowView.findViewById(R.id.location).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View arg0) {
                            CacheDetailActivity.startActivityGuid(cgeotrackable.this, cacheGuid, Html.fromHtml(cacheName).toString());
                        }
                    });
                }

                TextView logView = (TextView) rowView.findViewById(R.id.log);
                logView.setMovementMethod(LinkMovementMethod.getInstance());

                String logText = log.log;
                if (BaseUtils.containsHtml(logText)) {
                    logText = log.getDisplayText();
                    logView.setText(Html.fromHtml(logText, new HtmlImage(null, false, StoredList.TEMPORARY_LIST_ID, false), null), TextView.BufferType.SPANNABLE);
                }
                else {
                    logView.setText(logText);
                }

                // add LogImages
                LinearLayout logLayout = (LinearLayout) rowView.findViewById(R.id.log_layout);

                if (log.hasLogImages()) {

                    final ArrayList<cgImage> logImages = new ArrayList<cgImage>(log.getLogImages());

                    final View.OnClickListener listener = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ImagesActivity.startActivityLogImages(cgeotrackable.this, trackable.getGeocode(), logImages);
                        }
                    };

                    LinearLayout log_imgView = (LinearLayout) getLayoutInflater().inflate(R.layout.trackable_logs_img, null);
                    TextView log_img_title = (TextView) log_imgView.findViewById(R.id.title);
                    log_img_title.setText(log.getImageTitles());
                    log_img_title.setOnClickListener(listener);
                    logLayout.addView(log_imgView);
                }

                rowView.findViewById(R.id.author).setOnClickListener(new UserActionsListener());
                listView.addView(rowView);
            }

            if (trackable.getLogs().size() > 0) {
                findViewById(R.id.log_box).setVisibility(View.VISIBLE);
            }
        }
    }

    private class UserActionsListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            if (view == null) {
                return;
            }

            try {
                registerForContextMenu(view);
                openContextMenu(view);
            } catch (Exception e) {
                Log.e("cgeotrackable.UserActionsListener.onClick ", e);
            }
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

            BitmapDrawable image;
            try {
                HtmlImage imgGetter = new HtmlImage(trackable.getGeocode(), false, 0, false);

                image = imgGetter.getDrawable(url);
                Message message = handler.obtainMessage(0, image);
                handler.sendMessage(message);
            } catch (Exception e) {
                Log.e("cgeotrackable.TrackableIconThread.run: " + e.toString());
            }
        }
    }

    private static class TrackableIconHandler extends Handler {
        final private TextView view;

        public TrackableIconHandler(TextView viewIn) {
            view = viewIn;
        }

        @Override
        public void handleMessage(Message message) {
            final BitmapDrawable image = (BitmapDrawable) message.obj;
            if (image != null && view != null) {
                image.setBounds(0, 0, view.getHeight(), view.getHeight());
                view.setCompoundDrawables(image, null, null, null);
            }
        }
    }

    public static void startActivity(final AbstractActivity fromContext,
            final String guid, final String geocode, final String name) {
        final Intent trackableIntent = new Intent(fromContext, cgeotrackable.class);
        trackableIntent.putExtra("guid", guid);
        trackableIntent.putExtra("geocode", geocode);
        trackableIntent.putExtra("name", name);
        fromContext.startActivity(trackableIntent);
    }
}
