package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.AbstractViewPagerActivity;
import cgeo.geocaching.connector.gc.GCParser;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.geopoint.Units;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.ui.AbstractCachingPageViewCreator;
import cgeo.geocaching.ui.CacheDetailsCreator;
import cgeo.geocaching.ui.Formatter;
import cgeo.geocaching.utils.BaseUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.UnknownTagsHandler;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

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
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TrackableActivity extends AbstractViewPagerActivity<TrackableActivity.Page> {

    public enum Page {
        DETAILS(R.string.detail),
        LOGS(R.string.cache_logs);

        protected final int resId;

        private Page(final int resId) {
            this.resId = resId;
        }
    }
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
                geocode = trackable.getGeocode();

                if (StringUtils.isNotBlank(trackable.getName())) {
                    setTitle(Html.fromHtml(trackable.getName()).toString());
                } else {
                    setTitle(trackable.getName());
                }

                invalidateOptionsMenuCompatible();
                reinitializeViewPager();

            } catch (Exception e) {
                Log.e("TrackableActivity.loadTrackableHandler: ", e);
            }

            if (waitDialog != null) {
                waitDialog.dismiss();
            }
        }
    };

    public TrackableActivity() {
        super("c:geo-trackable-details");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme();
        setContentView(R.layout.trackable_activity);
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
            String uriHost = uri.getHost().toLowerCase(Locale.US);
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
                String uriPath = uri.getPath().toLowerCase(Locale.US);
                if (uriPath != null && uriPath.startsWith("/tb")) {
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
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.geocaching.com/profile/?u=" + Network.encode(contextMenuUser))));
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
            default:
                return false;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (trackable != null) {
            menu.findItem(MENU_LOG_TOUCH).setEnabled(StringUtils.isNotBlank(geocode) && trackable.isLoggable());
            menu.findItem(MENU_BROWSER_TRACKABLE).setEnabled(StringUtils.isNotBlank(trackable.getUrl()));
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
            trackable = cgData.loadTrackable(geocode);

            if ((trackable == null || trackable.isLoggable()) && !StringUtils.startsWithIgnoreCase(geocode, "GK")) {
                trackable = GCParser.searchTrackable(geocode, guid, id);
            }
            handler.sendMessage(Message.obtain());
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
                Log.e("TrackableActivity.UserActionsListener.onClick ", e);
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

            try {
                HtmlImage imgGetter = new HtmlImage(trackable.getGeocode(), false, 0, false);

                BitmapDrawable image = imgGetter.getDrawable(url);
                Message message = handler.obtainMessage(0, image);
                handler.sendMessage(message);
            } catch (Exception e) {
                Log.e("TrackableActivity.TrackableIconThread.run: ", e);
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
        final Intent trackableIntent = new Intent(fromContext, TrackableActivity.class);
        trackableIntent.putExtra("guid", guid);
        trackableIntent.putExtra("geocode", geocode);
        trackableIntent.putExtra("name", name);
        fromContext.startActivity(trackableIntent);
    }

    @Override
    protected PageViewCreator createViewCreator(Page page) {
        switch (page) {
            case DETAILS:
                return new DetailsViewCreator();
            case LOGS:
                return new LogsViewCreator();
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    protected String getTitle(Page page) {
        return res.getString(page.resId);
    }

    @Override
    protected Pair<List<? extends Page>, Integer> getOrderedPages() {
        List<Page> pages = new ArrayList<TrackableActivity.Page>();
        pages.add(Page.DETAILS);
        if (!trackable.getLogs().isEmpty()) {
            pages.add(Page.LOGS);
        }
        return new ImmutablePair<List<? extends Page>, Integer>(pages, 0);
    }

    public class LogsViewCreator extends AbstractCachingPageViewCreator<ListView> {

        private class LogViewHolder {

            private final TextView added;
            private final TextView type;
            private final TextView author;
            private final TextView location;
            private final TextView log;
            private final ImageView marker;
            private final LinearLayout logImages;

            public LogViewHolder(View rowView) {
                added = ((TextView) rowView.findViewById(R.id.added));
                type = ((TextView) rowView.findViewById(R.id.type));
                author = ((TextView) rowView.findViewById(R.id.author));
                location = ((TextView) rowView.findViewById(R.id.location));
                log = (TextView) rowView.findViewById(R.id.log);
                marker = (ImageView) rowView.findViewById(R.id.log_mark);
                logImages = (LinearLayout) rowView.findViewById(R.id.log_layout);
            }
        }

        @Override
        public ListView getDispatchedView() {
            view = (ListView) getLayoutInflater().inflate(R.layout.trackable_logs_view, null);

            if (trackable != null && trackable.getLogs() != null) {
                view.setAdapter(new ArrayAdapter<LogEntry>(TrackableActivity.this, R.layout.trackable_logs_item, trackable.getLogs()) {
                    @Override
                    public View getView(int position, View convertView, android.view.ViewGroup parent) {
                        View rowView = convertView;
                        if (null == rowView) {
                            rowView = getLayoutInflater().inflate(R.layout.trackable_logs_item, null);
                        }
                        LogViewHolder holder = (LogViewHolder) rowView.getTag();
                        if (null == holder) {
                            holder = new LogViewHolder(rowView);
                            rowView.setTag(holder);
                        }

                        final LogEntry log = getItem(position);
                        fillViewHolder(holder, log);
                        return rowView;
                    }
                });
            }
            return view;
        }

        protected void fillViewHolder(LogViewHolder holder, LogEntry log) {
            if (log.date > 0) {
                holder.added.setText(Formatter.formatShortDate(log.date));
            }

            holder.type.setText(log.type.getL10n());
            holder.author.setText(Html.fromHtml(log.author), TextView.BufferType.SPANNABLE);

            if (StringUtils.isBlank(log.cacheName)) {
                holder.location.setVisibility(View.GONE);
            } else {
                holder.location.setText(Html.fromHtml(log.cacheName));
                final String cacheGuid = log.cacheGuid;
                final String cacheName = log.cacheName;
                holder.location.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        CacheDetailActivity.startActivityGuid(TrackableActivity.this, cacheGuid, Html.fromHtml(cacheName).toString());
                    }
                });
            }

            TextView logView = holder.log;
            logView.setMovementMethod(LinkMovementMethod.getInstance());

            String logText = log.log;
            if (BaseUtils.containsHtml(logText)) {
                logText = log.getDisplayText();
                logView.setText(Html.fromHtml(logText, new HtmlImage(null, false, StoredList.TEMPORARY_LIST_ID, false), null), TextView.BufferType.SPANNABLE);
            }
            else {
                logView.setText(logText);
            }

            ImageView statusMarker = holder.marker;
            // colored marker
            int marker = log.type.markerId;
            if (marker != 0) {
                statusMarker.setVisibility(View.VISIBLE);
                statusMarker.setImageResource(marker);
            }
            else {
                statusMarker.setVisibility(View.GONE);
            }

            // add LogImages
            LinearLayout logLayout = holder.logImages;

            if (log.hasLogImages()) {

                final ArrayList<cgImage> logImages = new ArrayList<cgImage>(log.getLogImages());

                final View.OnClickListener listener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ImagesActivity.startActivityLogImages(TrackableActivity.this, trackable.getGeocode(), logImages);
                    }
                };

                LinearLayout log_imgView = (LinearLayout) getLayoutInflater().inflate(R.layout.trackable_logs_img, null);
                TextView log_img_title = (TextView) log_imgView.findViewById(R.id.title);
                log_img_title.setText(log.getImageTitles());
                log_img_title.setOnClickListener(listener);
                logLayout.addView(log_imgView);
            }

            holder.author.setOnClickListener(new UserActionsListener());
        }

    }

    public class DetailsViewCreator extends AbstractCachingPageViewCreator<ScrollView> {

        @Override
        public ScrollView getDispatchedView() {
            view = (ScrollView) getLayoutInflater().inflate(R.layout.trackable_details_view, null);
            final CacheDetailsCreator details = new CacheDetailsCreator(TrackableActivity.this, (LinearLayout) view.findViewById(R.id.details_list));

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
            details.add(R.string.trackable_code, trackable.getGeocode());

            // trackable owner
            TextView owner = details.add(R.string.trackable_owner, res.getString(R.string.trackable_unknown));
            if (StringUtils.isNotBlank(trackable.getOwner())) {
                owner.setText(Html.fromHtml(trackable.getOwner()), TextView.BufferType.SPANNABLE);
                owner.setOnClickListener(new UserActionsListener());
            }

            // trackable spotted
            if (StringUtils.isNotBlank(trackable.getSpottedName()) ||
                    trackable.getSpottedType() == cgTrackable.SPOTTED_UNKNOWN ||
                    trackable.getSpottedType() == cgTrackable.SPOTTED_OWNER) {
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
                            CacheDetailActivity.startActivityGuid(TrackableActivity.this, trackable.getSpottedGuid(), trackable.getSpottedName());
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
                view.findViewById(R.id.goal_box).setVisibility(View.VISIBLE);
                TextView descView = (TextView) view.findViewById(R.id.goal);
                descView.setVisibility(View.VISIBLE);
                descView.setText(Html.fromHtml(trackable.getGoal(), new HtmlImage(geocode, true, 0, false), null), TextView.BufferType.SPANNABLE);
                descView.setMovementMethod(LinkMovementMethod.getInstance());
            }

            // trackable details
            if (StringUtils.isNotBlank(trackable.getDetails())) {
                view.findViewById(R.id.details_box).setVisibility(View.VISIBLE);
                TextView descView = (TextView) view.findViewById(R.id.details);
                descView.setVisibility(View.VISIBLE);
                descView.setText(Html.fromHtml(trackable.getDetails(), new HtmlImage(geocode, true, 0, false), new UnknownTagsHandler()), TextView.BufferType.SPANNABLE);
                descView.setMovementMethod(LinkMovementMethod.getInstance());
            }

            // trackable image
            if (StringUtils.isNotBlank(trackable.getImage())) {
                view.findViewById(R.id.image_box).setVisibility(View.VISIBLE);
                LinearLayout imgView = (LinearLayout) view.findViewById(R.id.image);

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
                        try {
                            HtmlImage imgGetter = new HtmlImage(geocode, true, 0, false);

                            BitmapDrawable image = imgGetter.getDrawable(trackable.getImage());
                            Message message = handler.obtainMessage(0, image);
                            handler.sendMessage(message);
                        } catch (Exception e) {
                            Log.e("cgeospoilers.onCreate.onClick.run: ", e);
                        }
                    }
                }.start();

                imgView.addView(trackableImage);
            }
            return view;
        }

    }

}
