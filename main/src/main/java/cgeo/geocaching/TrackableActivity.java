package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.TabbedViewPagerActivity;
import cgeo.geocaching.activity.TabbedViewPagerFragment;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.connector.trackable.TrackableTrackingCode;
import cgeo.geocaching.databinding.CachedetailImagegalleryPageBinding;
import cgeo.geocaching.databinding.CachedetailImagesPageBinding;
import cgeo.geocaching.databinding.TrackableDetailsViewBinding;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogTrackableActivity;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.TrackableLogsViewCreator;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.network.AndroidBeam;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.AnchorAwareLinkMovementMethod;
import cgeo.geocaching.ui.CacheDetailsCreator;
import cgeo.geocaching.ui.ImageGalleryView;
import cgeo.geocaching.ui.ImagesList;
import cgeo.geocaching.ui.UserClickListener;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.HtmlUtils;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.UnknownTagsHandler;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.view.ActionMode;
import androidx.core.text.HtmlCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;

public class TrackableActivity extends TabbedViewPagerActivity implements AndroidBeam.ActivitySharingInterface {

    public enum Page {
        DETAILS(R.string.detail),
        LOGS(R.string.cache_logs),
        IMAGES(R.string.cache_images),
        IMAGEGALLERY(R.string.cache_images);

        @StringRes
        private final int resId;
        public final long id;

        Page(@StringRes final int resId) {
            this.resId = resId;
            this.id = ordinal();
        }

        static Page find(final long pageId) {
            for (Page page : Page.values()) {
                if (page.id == pageId) {
                    return page;
                }
            }
            return null;
        }
    }

    private Trackable trackable = null;
    private String geocode = null;
    private String name = null;
    private String guid = null;
    private String id = null;
    private String geocache = null;
    private String trackingCode = null;
    private TrackableBrand brand = null;
    private ProgressDialog waitDialog = null;
    private CharSequence clickedItemText = null;
    private ImagesList imagesList = null;
    private ImageGalleryView imageGallery = null;
    private String fallbackKeywordSearch = null;
    private final CompositeDisposable createDisposables = new CompositeDisposable();
    private final CompositeDisposable geoDataDisposable = new CompositeDisposable();
    private static final GeoDirHandler locationUpdater = new GeoDirHandler() {
        @SuppressWarnings("EmptyMethod")
        @Override
        public void updateGeoData(final GeoData geoData) {
            // Do not do anything, as we just want to maintain the GPS on
        }
    };

    /**
     * Action mode of the current contextual action bar (e.g. for copy and share actions).
     */
    private ActionMode currentActionMode;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeAndContentView(R.layout.tabbed_viewpager_activity_refreshable);

        // set title in code, as the activity needs a hard coded title due to the intent filters
        setTitle(res.getString(R.string.trackable));

        // get parameters
        final Bundle extras = getIntent().getExtras();

        final Uri uri = AndroidBeam.getUri(getIntent());
        if (extras != null) {
            // try to get data from extras
            geocode = extras.getString(Intents.EXTRA_GEOCODE);
            name = extras.getString(Intents.EXTRA_NAME);
            guid = extras.getString(Intents.EXTRA_GUID);
            id = extras.getString(Intents.EXTRA_ID);
            geocache = extras.getString(Intents.EXTRA_GEOCACHE);
            brand = TrackableBrand.getById(extras.getInt(Intents.EXTRA_BRAND));
            trackingCode = extras.getString(Intents.EXTRA_TRACKING_CODE);
            fallbackKeywordSearch = extras.getString(Intents.EXTRA_KEYWORD);
        }

        // try to get data from URI
        if (geocode == null && guid == null && id == null && uri != null) {
            // check if port part needs to be removed
            String address = uri.toString();
            if (uri.getPort() > 0) {
                address = StringUtils.remove(address, ":" + uri.getPort());
            }
            geocode = ConnectorFactory.getTrackableFromURL(address);
            final TrackableTrackingCode tbTrackingCode = ConnectorFactory.getTrackableTrackingCodeFromURL(address);

            final String uriHost = uri.getHost().toLowerCase(Locale.US);
            if (uriHost.endsWith("geocaching.com")) {
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
            } else if (uriHost.endsWith("geokrety.org")) {
                brand = TrackableBrand.GEOKRETY;

                // If geocode isn't found, try to find by Tracking Code
                if (geocode == null && !tbTrackingCode.isEmpty()) {
                    trackingCode = tbTrackingCode.trackingCode;
                    geocode = tbTrackingCode.trackingCode;
                }
            }
        }

        // no given data
        if (geocode == null && guid == null && id == null) {
            showToast(res.getString(R.string.err_tb_display));
            finish();
            return;
        }

        final String message;
        if (StringUtils.isNotBlank(name)) {
            message = TextUtils.stripHtml(name);
        } else if (StringUtils.isNotBlank(geocode)) {
            message = geocode;
        } else {
            message = res.getString(R.string.trackable);
        }

        // If we have a newer Android device setup Android Beam for easy cache sharing
        AndroidBeam.enable(this, this);

        createViewPager(Page.DETAILS.id, getOrderedPages(), null, true);

        refreshTrackable(message);
    }

    @Override
    public void onResume() {
        super.onResume();

        // resume location access
        geoDataDisposable.add(locationUpdater.start(GeoDirHandler.UPDATE_GEODATA));
    }

    @Override
    public void onPause() {
        geoDataDisposable.clear();
        super.onPause();
    }

    private void act(final Trackable newTrackable) {
        trackable = newTrackable;
        displayTrackable();
        // reset imagelist // @todo mb: more to do?
        imagesList = null;
        imageGallery = null;
    }

    private void refreshTrackable(final String message) {
        waitDialog = ProgressDialog.show(this, message, res.getString(R.string.trackable_details_loading), true, true);
        createDisposables.add(AndroidRxUtils.bindActivity(this, ConnectorFactory.loadTrackable(geocode, guid, id, brand)).subscribe(
                newTrackable -> {
                    if (trackingCode != null) {
                        newTrackable.setTrackingcode(trackingCode);
                    }
                    act(newTrackable);
                }, throwable -> {
                    Log.w("unable to retrieve trackable information", throwable);
                    showToast(res.getString(R.string.err_tb_find_that));
                    finish();
                }, () -> act(null)));
    }

    @Nullable
    @Override
    public String getAndroidBeamUri() {
        return trackable != null ? trackable.getUrl() : null;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.trackable_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.menu_log_touch) {
            startActivityForResult(LogTrackableActivity.getIntent(this, trackable, geocache), LogTrackableActivity.LOG_TRACKABLE);
        } else if (itemId == R.id.menu_browser_trackable) {
            ShareUtils.openUrl(this, trackable.getUrl(), true);
        } else if (itemId == R.id.menu_refresh_trackable) {
            refreshTrackable(StringUtils.defaultIfBlank(trackable.getName(), trackable.getGeocode()));
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        if (trackable != null) {
            menu.findItem(R.id.menu_log_touch).setVisible(StringUtils.isNotBlank(geocode) && trackable.isLoggable());
            menu.findItem(R.id.menu_browser_trackable).setVisible(trackable.hasUrl());
            menu.findItem(R.id.menu_refresh_trackable).setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void pullToRefreshActionTrigger() {
        refreshTrackable(StringUtils.defaultIfBlank(trackable.getName(), trackable.getGeocode()));
    }

    public void displayTrackable() {
        if (trackable == null) {
            Dialogs.dismiss(waitDialog);

            if (fallbackKeywordSearch != null) {
                CacheListActivity.startActivityKeyword(this, fallbackKeywordSearch);
            } else {
                if (StringUtils.isNotBlank(geocode)) {
                    showToast(res.getString(R.string.err_tb_not_found, geocode));
                } else {
                    showToast(res.getString(R.string.err_tb_find_that));
                }
            }

            finish();
            return;
        }

        try {
            geocode = trackable.getGeocode();

            if (StringUtils.isNotBlank(trackable.getName())) {
                setTitle(TextUtils.stripHtml(trackable.getName()));
            } else {
                setTitle(trackable.getName());
            }

            invalidateOptionsMenuCompatible();
            setOrderedPages(getOrderedPages());
            reinitializeViewPager();

        } catch (final Exception e) {
            Log.e("TrackableActivity.loadTrackableHandler: ", e);
        }
        Dialogs.dismiss(waitDialog);
    }

    private static void setupIcon(final TrackableActivity activity, final ActionBar actionBar, final String url) {
        final HtmlImage imgGetter = new HtmlImage(HtmlImage.SHARED, false, false, false);
        AndroidRxUtils.bindActivity(activity, imgGetter.fetchDrawable(url)).subscribe(image -> {
            if (actionBar != null) {
                final int height = actionBar.getHeight();
                //noinspection SuspiciousNameCombination
                image.setBounds(0, 0, height, height);
                actionBar.setIcon(image);
            }
        });
    }

    private static void setupIcon(final ActionBar actionBar, @DrawableRes final int resId) {
        if (actionBar != null) {
            actionBar.setIcon(resId);
        }
    }

    public static void startActivity(final AbstractActivity fromContext, final String guid, final String geocode, final String name, final String geocache, final int brandId) {
        final Intent trackableIntent = new Intent(fromContext, TrackableActivity.class);
        trackableIntent.putExtra(Intents.EXTRA_GUID, guid);
        trackableIntent.putExtra(Intents.EXTRA_GEOCODE, geocode);
        trackableIntent.putExtra(Intents.EXTRA_NAME, name);
        trackableIntent.putExtra(Intents.EXTRA_GEOCACHE, geocache);
        trackableIntent.putExtra(Intents.EXTRA_BRAND, brandId);
        fromContext.startActivity(trackableIntent);
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected TabbedViewPagerFragment createNewFragment(final long pageId) {
        if (pageId == Page.DETAILS.id) {
            return new DetailsViewCreator();
        } else if (pageId == Page.LOGS.id) {
            return new TrackableLogsViewCreator();
        } else if (pageId == Page.IMAGES.id) {
            return new ImagesViewCreator();
        } else if (pageId == Page.IMAGEGALLERY.id) {
            return new ImageGalleryViewCreator();
        }
        throw new IllegalStateException(); // cannot happen as long as switch case is enum complete
    }

    public static class ImagesViewCreator extends TabbedViewPagerFragment<CachedetailImagesPageBinding> {

        @Override
        public CachedetailImagesPageBinding createView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
            return CachedetailImagesPageBinding.inflate(inflater, container, false);
        }

        @Override
        public long getPageId() {
            return Page.IMAGES.id;
        }

        @Override
        public void setContent() {
            final TrackableActivity activity = (TrackableActivity) getActivity();
            if (activity == null) {
                return;
            }
            final Trackable trackable = activity.getTrackable();
            if (trackable == null) {
                return;
            }
            binding.getRoot().setVisibility(View.VISIBLE);

            if (activity.imagesList == null) {
                activity.imagesList = new ImagesList(activity, trackable.getGeocode(), null);
                activity.createDisposables.add(activity.imagesList.loadImages(binding.getRoot(), trackable.getImages()));
            }
        }

    }

    public static class ImageGalleryViewCreator extends TabbedViewPagerFragment<CachedetailImagegalleryPageBinding> {

        @Override
        public CachedetailImagegalleryPageBinding createView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
            return CachedetailImagegalleryPageBinding.inflate(inflater, container, false);
        }

        @Override
        public long getPageId() {
            return Page.IMAGEGALLERY.id;
        }

        @Override
        public void setContent() {
            final TrackableActivity activity = (TrackableActivity) getActivity();
            if (activity == null) {
                return;
            }
            final Trackable trackable = activity.getTrackable();
            if (trackable == null) {
                return;
            }
            binding.getRoot().setVisibility(View.VISIBLE);

            if (activity.imageGallery == null) {
                ImageUtils.initializeImageGallery(binding.imageGallery, trackable.getGeocode(), trackable.getImages(), true);
                activity.imageGallery = binding.imageGallery;
                reinitializeTitle();
                activity.imageGallery.setImageCountChangeCallback((ig, c) -> reinitializeTitle());
            }
        }

    }

    @Override
    protected String getTitle(final long pageId) {
        if (pageId == Page.IMAGEGALLERY.id) {
            String title = this.getString(Page.find(pageId).resId);
            if (this.imageGallery != null) {
                title += " (" + this.imageGallery.getImageCount() + ")";
            }
            return title;
        }
        return this.getString(Page.find(pageId).resId);
    }

    protected long[] getOrderedPages() {
        final List<Long> pages = new ArrayList<>();
        pages.add(Page.DETAILS.id);
        if (trackable != null) {
            if (CollectionUtils.isNotEmpty(trackable.getLogs())) {
                pages.add(Page.LOGS.id);
            }
            if (!Settings.enableFeatureNewImageGallery() && CollectionUtils.isNotEmpty(trackable.getImages())) {
                pages.add(Page.IMAGES.id);
            }
            if (Settings.enableFeatureNewImageGallery()) {
                pages.add(Page.IMAGEGALLERY.id);
            }
        }
        final long[] result = new long[pages.size()];
        for (int i = 0; i < pages.size(); i++) {
            result[i] = pages.get(i);
        }
        return result;
    }

    public static class DetailsViewCreator extends TabbedViewPagerFragment<TrackableDetailsViewBinding> {

        @Override
        public TrackableDetailsViewBinding createView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
            return TrackableDetailsViewBinding.inflate(getLayoutInflater(), container, false);
        }

        @Override
        public long getPageId() {
            return Page.DETAILS.id;
        }

        @Override
        // splitting up that method would not help improve readability
        @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
        public void setContent() {
            final TrackableActivity activity = (TrackableActivity) getActivity();
            if (activity == null) {
                return;
            }
            final Trackable trackable = activity.getTrackable();
            if (trackable == null) {
                return;
            }
            binding.getRoot().setVisibility(View.VISIBLE);

            final CacheDetailsCreator details = new CacheDetailsCreator(activity, binding.detailsList);

            // action bar icon
            if (StringUtils.isNotBlank(trackable.getIconUrl())) {
                setupIcon(activity, activity.getSupportActionBar(), trackable.getIconUrl());
            } else {
                setupIcon(activity.getSupportActionBar(), trackable.getIconBrand());
            }

            // trackable name
            final TextView nameTxtView = details.add(R.string.trackable_name, StringUtils.isNotBlank(trackable.getName()) ? TextUtils.stripHtml(trackable.getName()) : activity.res.getString(R.string.trackable_unknown)).valueView;
            activity.addContextMenu(nameTxtView);

            // missing status
            if (trackable.isMissing()) {
                nameTxtView.setPaintFlags(nameTxtView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }

            // trackable type
            final String tbType;
            if (StringUtils.isNotBlank(trackable.getType())) {
                tbType = TextUtils.stripHtml(trackable.getType());
            } else {
                tbType = activity.res.getString(R.string.trackable_unknown);
            }
            details.add(R.string.trackable_brand, trackable.getBrand().getLabel());
            details.add(R.string.trackable_type, tbType);

            // trackable geocode
            activity.addContextMenu(details.add(R.string.trackable_code, trackable.getGeocode()).valueView);

            // retrieved status
            final Date logDate = trackable.getLogDate();
            final LogType logType = trackable.getLogType();
            if (logDate != null && logType != null) {
                final Uri uri = new Uri.Builder().scheme("https").authority("www.geocaching.com").path("/track/log.aspx").encodedQuery("LUID=" + trackable.getLogGuid()).build();
                final TextView logView = details.add(R.string.trackable_status, activity.res.getString(R.string.trackable_found, logType.getL10n(), Formatter.formatDate(logDate.getTime()))).valueView;
                logView.setOnClickListener(v -> ShareUtils.openUrl(activity, uri.toString()));
            }

            // trackable owner
            final TextView owner = details.add(R.string.trackable_owner, activity.res.getString(R.string.trackable_unknown)).valueView;
            if (StringUtils.isNotBlank(trackable.getOwner())) {
                owner.setText(HtmlCompat.fromHtml(trackable.getOwner(), HtmlCompat.FROM_HTML_MODE_LEGACY), TextView.BufferType.SPANNABLE);
                owner.setOnClickListener(UserClickListener.forOwnerOf(trackable));
            }

            // trackable spotted
            if (StringUtils.isNotBlank(trackable.getSpottedName()) ||
                    trackable.getSpottedType() == Trackable.SPOTTED_UNKNOWN ||
                    trackable.getSpottedType() == Trackable.SPOTTED_OWNER ||
                    trackable.getSpottedType() == Trackable.SPOTTED_ARCHIVED) {

                final StringBuilder text;
                boolean showTimeSpan = true;
                switch (trackable.getSpottedType()) {
                    case Trackable.SPOTTED_CACHE:
                        // TODO: the whole sentence fragment should not be constructed, but taken from the resources
                        text = new StringBuilder(activity.res.getString(R.string.trackable_spotted_in_cache)).append(' ').append(HtmlCompat.fromHtml(trackable.getSpottedName(), HtmlCompat.FROM_HTML_MODE_LEGACY));
                        break;
                    case Trackable.SPOTTED_USER:
                        // TODO: the whole sentence fragment should not be constructed, but taken from the resources
                        text = new StringBuilder(activity.res.getString(R.string.trackable_spotted_at_user)).append(' ').append(HtmlCompat.fromHtml(trackable.getSpottedName(), HtmlCompat.FROM_HTML_MODE_LEGACY));
                        break;
                    case Trackable.SPOTTED_UNKNOWN:
                        text = new StringBuilder(activity.res.getString(R.string.trackable_spotted_unknown_location));
                        break;
                    case Trackable.SPOTTED_OWNER:
                        text = new StringBuilder(activity.res.getString(R.string.trackable_spotted_owner));
                        break;
                    case Trackable.SPOTTED_ARCHIVED:
                        text = new StringBuilder(activity.res.getString(R.string.trackable_spotted_archived));
                        break;
                    default:
                        text = new StringBuilder("N/A");
                        showTimeSpan = false;
                        break;
                }

                // days since last spotting
                if (showTimeSpan) {
                    for (final LogEntry log : trackable.getLogs()) {
                        if (log.logType == LogType.RETRIEVED_IT || log.logType == LogType.GRABBED_IT || log.logType == LogType.DISCOVERED_IT || log.logType == LogType.PLACED_IT) {
                            text.append(" (").append(Formatter.formatDaysAgo(log.date)).append(')');
                            break;
                        }
                    }
                }

                final TextView spotted = details.add(R.string.trackable_spotted, text.toString()).valueView;
                spotted.setClickable(true);
                if (trackable.getSpottedType() == Trackable.SPOTTED_CACHE) {
                    spotted.setOnClickListener(arg0 -> {
                        if (StringUtils.isNotBlank(trackable.getSpottedGuid())) {
                            CacheDetailActivity.startActivityGuid(activity, trackable.getSpottedGuid(), trackable.getSpottedName());
                        } else {
                            // for GeoKrety we only know the cache geocode
                            final String cacheCode = trackable.getSpottedName();
                            if (ConnectorFactory.canHandle(cacheCode)) {
                                CacheDetailActivity.startActivity(activity, cacheCode);
                            }
                        }
                    });
                } else if (trackable.getSpottedType() == Trackable.SPOTTED_USER) {
                    spotted.setOnClickListener(UserClickListener.forUser(trackable, TextUtils.stripHtml(trackable.getSpottedName()), trackable.getSpottedGuid()));
                } else if (trackable.getSpottedType() == Trackable.SPOTTED_OWNER) {
                    spotted.setOnClickListener(UserClickListener.forUser(trackable, TextUtils.stripHtml(trackable.getOwner()), trackable.getOwnerGuid()));
                }
            }

            // trackable origin
            if (StringUtils.isNotBlank(trackable.getOrigin())) {
                final TextView origin = details.add(R.string.trackable_origin, "").valueView;
                origin.setText(HtmlCompat.fromHtml(trackable.getOrigin(), HtmlCompat.FROM_HTML_MODE_LEGACY), TextView.BufferType.SPANNABLE);
                activity.addContextMenu(origin);
            }

            // trackable released
            final Date releasedDate = trackable.getReleased();
            if (releasedDate != null) {
                activity.addContextMenu(details.add(R.string.trackable_released, Formatter.formatDate(releasedDate.getTime())).valueView);
            }

            // trackable distance
            if (trackable.getDistance() >= 0) {
                activity.addContextMenu(details.add(R.string.trackable_distance, Units.getDistanceFromKilometers(trackable.getDistance())).valueView);
            }

            // trackable goal
            if (StringUtils.isNotBlank(HtmlUtils.extractText(trackable.getGoal()))) {
                binding.goalBox.setVisibility(View.VISIBLE);
                binding.goal.setVisibility(View.VISIBLE);
                binding.goal.setText(HtmlCompat.fromHtml(trackable.getGoal(), HtmlCompat.FROM_HTML_MODE_LEGACY, new HtmlImage(activity.geocode, true, false, binding.goal, false), null), TextView.BufferType.SPANNABLE);
                binding.goal.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
                activity.addContextMenu(binding.goal);
            }

            // trackable details
            if (StringUtils.isNotBlank(HtmlUtils.extractText(trackable.getDetails()))) {
                binding.detailsBox.setVisibility(View.VISIBLE);
                binding.details.setVisibility(View.VISIBLE);
                binding.details.setText(HtmlCompat.fromHtml(trackable.getDetails(), HtmlCompat.FROM_HTML_MODE_LEGACY, new HtmlImage(activity.geocode, true, false, binding.details, false), new UnknownTagsHandler()), TextView.BufferType.SPANNABLE);
                binding.details.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
                activity.addContextMenu(binding.details);
            }

            // trackable image
            if (StringUtils.isNotBlank(trackable.getImage())) {
                binding.imageBox.setVisibility(View.VISIBLE);
                final ImageView trackableImage = (ImageView) activity.getLayoutInflater().inflate(R.layout.trackable_image, binding.image, false);

                trackableImage.setImageResource(R.drawable.image_not_loaded);
                trackableImage.setClickable(true);

                if (Settings.enableFeatureNewImageGallery()) {
                    trackableImage.setOnClickListener(view -> ImageViewActivity.openImageView(activity, trackable.getGeocode(), Collections.singletonList(IterableUtils.find(trackable.getImages(), i -> trackable.getImage().equals(i.getUrl()))), 0, p -> view));
                } else {
                    trackableImage.setOnClickListener(view -> ShareUtils.openUrl(activity, trackable.getImage()));
                }


                AndroidRxUtils.bindActivity(activity, new HtmlImage(activity.geocode, true, false, false).fetchDrawable(trackable.getImage())).subscribe(trackableImage::setImageDrawable);

                binding.image.removeAllViews();
                binding.image.addView(trackableImage);
            }
        }

    }

    @Override
    public void addContextMenu(final View view) {
        view.setOnLongClickListener(v -> startContextualActionBar(view));

        view.setOnClickListener(v -> startContextualActionBar(view));
    }

    private boolean startContextualActionBar(final View view) {
        if (currentActionMode != null) {
            return false;
        }
        currentActionMode = startSupportActionMode(new ActionMode.Callback() {

            @Override
            public boolean onPrepareActionMode(final ActionMode actionMode, final Menu menu) {
                return prepareClipboardActionMode(view, actionMode, menu);
            }

            private boolean prepareClipboardActionMode(final View view, final ActionMode actionMode, final Menu menu) {
                clickedItemText = ((TextView) view).getText();
                final int viewId = view.getId();
                if (viewId == R.id.value) { // name, TB-code, origin, released, distance
                    final TextView textView = ((View) view.getParent().getParent()).findViewById(R.id.name);
                    final CharSequence itemTitle = textView.getText();
                    buildDetailsContextMenu(actionMode, menu, itemTitle, true);
                } else if (viewId == R.id.goal) {
                    buildDetailsContextMenu(actionMode, menu, res.getString(R.string.trackable_goal), false);
                } else if (viewId == R.id.details) {
                    buildDetailsContextMenu(actionMode, menu, res.getString(R.string.trackable_details), false);
                } else if (viewId == R.id.log) {
                    buildDetailsContextMenu(actionMode, menu, res.getString(R.string.cache_logs), false);
                } else {
                    return false;
                }
                return true;
            }

            @Override
            public void onDestroyActionMode(final ActionMode actionMode) {
                currentActionMode = null;
            }

            @Override
            public boolean onCreateActionMode(final ActionMode actionMode, final Menu menu) {
                actionMode.getMenuInflater().inflate(R.menu.details_context, menu);
                prepareClipboardActionMode(view, actionMode, menu);
                return true;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode actionMode, final MenuItem menuItem) {
                return onClipboardItemSelected(actionMode, menuItem, clickedItemText, null);
            }
        });
        return false;
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);  // call super to make lint happy
        // Refresh the logs view after coming back from logging a trackable
        if (requestCode == LogTrackableActivity.LOG_TRACKABLE && resultCode == RESULT_OK) {
            refreshTrackable(StringUtils.defaultIfBlank(trackable.getName(), trackable.getGeocode()));
        } else if (imageGallery != null) {
            imageGallery.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onDestroy() {
        createDisposables.clear();
        if (imageGallery != null) {
            imageGallery.clear();
        }
        super.onDestroy();
    }

    public Trackable getTrackable() {
        return trackable;
    }

    @Override
    public void finish() {
        Dialogs.dismiss(waitDialog);
        super.finish();
    }
}
