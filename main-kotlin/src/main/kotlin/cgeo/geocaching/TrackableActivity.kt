// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching

import cgeo.geocaching.activity.AbstractActivity
import cgeo.geocaching.activity.TabbedViewPagerActivity
import cgeo.geocaching.activity.TabbedViewPagerFragment
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.trackable.TrackableBrand
import cgeo.geocaching.connector.trackable.TrackableTrackingCode
import cgeo.geocaching.databinding.CachedetailImagegalleryPageBinding
import cgeo.geocaching.databinding.TrackableDetailsViewBinding
import cgeo.geocaching.location.Units
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.log.LogTrackableActivity
import cgeo.geocaching.log.LogType
import cgeo.geocaching.log.TrackableLogsViewCreator
import cgeo.geocaching.models.Trackable
import cgeo.geocaching.network.HtmlImage
import cgeo.geocaching.sensors.GeoData
import cgeo.geocaching.sensors.GeoDirHandler
import cgeo.geocaching.ui.AnchorAwareLinkMovementMethod
import cgeo.geocaching.ui.CacheDetailsCreator
import cgeo.geocaching.ui.ImageGalleryView
import cgeo.geocaching.ui.UserClickListener
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.Formatter
import cgeo.geocaching.utils.ImageUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.MenuUtils
import cgeo.geocaching.utils.OfflineTranslateUtils
import cgeo.geocaching.utils.ShareUtils
import cgeo.geocaching.utils.TextUtils
import cgeo.geocaching.utils.TranslationUtils
import cgeo.geocaching.utils.html.HtmlUtils
import cgeo.geocaching.utils.html.UnknownTagsHandler

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import androidx.annotation.DrawableRes
import androidx.annotation.NonNull
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBar
import androidx.core.text.HtmlCompat

import java.util.ArrayList
import java.util.Collections
import java.util.Date
import java.util.List
import java.util.Locale

import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.collections4.IterableUtils
import org.apache.commons.lang3.StringUtils

class TrackableActivity : TabbedViewPagerActivity() {

    public static val STATE_TRANSLATION_LANGUAGE_SOURCE: String = "cgeo.geocaching.translation.languageSource"

    enum class class Page {
        DETAILS(R.string.detail),
        LOGS(R.string.cache_logs),
        IMAGEGALLERY(R.string.cache_images)

        @StringRes
        private final Int resId
        public final Long id

        Page(@StringRes final Int resId) {
            this.resId = resId
            this.id = ordinal()
        }

        static Page find(final Long pageId) {
            for (Page page : Page.values()) {
                if (page.id == pageId) {
                    return page
                }
            }
            return null
        }
    }

    private var trackable: Trackable = null
    private var geocode: String = null
    private var name: String = null
    private var guid: String = null
    private var id: String = null
    private var geocache: String = null
    private var trackingCode: String = null
    private var brand: TrackableBrand = null
    private var waitDialog: ProgressDialog = null
    private var imageGallery: ImageGalleryView = null
    private var fallbackKeywordSearch: String = null
    private val createDisposables: CompositeDisposable = CompositeDisposable()
    private val geoDataDisposable: CompositeDisposable = CompositeDisposable()
    private static val locationUpdater: GeoDirHandler = GeoDirHandler() {
        @SuppressWarnings("EmptyMethod")
        override         public Unit updateGeoData(final GeoData geoData) {
            // Do not do anything, as we just want to maintain the GPS on
        }
    }

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        setThemeAndContentView(R.layout.tabbed_viewpager_activity_refreshable)

        // set title in code, as the activity needs a hard coded title due to the intent filters
        setTitle(res.getString(R.string.trackable))

        // get parameters
        val extras: Bundle = getIntent().getExtras()

        if (extras != null) {
            // try to get data from extras
            geocode = extras.getString(Intents.EXTRA_GEOCODE)
            name = extras.getString(Intents.EXTRA_NAME)
            guid = extras.getString(Intents.EXTRA_GUID)
            id = extras.getString(Intents.EXTRA_ID)
            geocache = extras.getString(Intents.EXTRA_GEOCACHE)
            brand = TrackableBrand.getById(extras.getInt(Intents.EXTRA_BRAND))
            trackingCode = extras.getString(Intents.EXTRA_TRACKING_CODE)
            fallbackKeywordSearch = extras.getString(Intents.EXTRA_KEYWORD)
        }

        // try to get data from URI
        val uri: Uri = getIntent().getData()
        if (geocode == null && guid == null && id == null && uri != null) {
            // check if port part needs to be removed
            String address = uri.toString()
            if (uri.getPort() > 0) {
                address = StringUtils.remove(address, ":" + uri.getPort())
            }
            geocode = ConnectorFactory.getTrackableFromURL(address)
            val tbTrackingCode: TrackableTrackingCode = ConnectorFactory.getTrackableTrackingCodeFromURL(address)

            val uriHost: String = uri.getHost().toLowerCase(Locale.US)
            if (uriHost.endsWith("geocaching.com")) {
                geocode = uri.getQueryParameter("tracker")
                if (StringUtils.isBlank(geocode)) {
                    geocode = uri.getQueryParameter("TB")
                }
                guid = uri.getQueryParameter("guid")
                id = uri.getQueryParameter("id")

                if (StringUtils.isNotBlank(geocode)) {
                    geocode = geocode.toUpperCase(Locale.US)
                    guid = null
                    id = null
                } else if (StringUtils.isNotBlank(guid)) {
                    geocode = null
                    guid = guid.toLowerCase(Locale.US)
                    id = null
                } else if (StringUtils.isNotBlank(id)) {
                    geocode = null
                    guid = null
                    id = id.toLowerCase(Locale.US)
                } else {
                    showToast(res.getString(R.string.err_tb_details_open))
                    finish()
                    return
                }
            } else if (uriHost.endsWith("geokrety.org")) {
                brand = TrackableBrand.GEOKRETY

                // If geocode isn't found, try to find by Tracking Code
                if (geocode == null && !tbTrackingCode.isEmpty()) {
                    trackingCode = tbTrackingCode.trackingCode
                    geocode = tbTrackingCode.trackingCode
                }
            }
        }

        // no given data
        if (geocode == null && guid == null && id == null) {
            showToast(res.getString(R.string.err_tb_display))
            finish()
            return
        }

        final String message
        if (StringUtils.isNotBlank(name)) {
            message = TextUtils.stripHtml(name)
        } else if (StringUtils.isNotBlank(geocode)) {
            message = geocode
        } else {
            message = res.getString(R.string.trackable)
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(STATE_TRANSLATION_LANGUAGE_SOURCE)) {
                final OfflineTranslateUtils.Language newLanguage = OfflineTranslateUtils.Language(savedInstanceState.getString(STATE_TRANSLATION_LANGUAGE_SOURCE))
                if (newLanguage.isValid()) {
                    translationStatus.setSourceLanguage(newLanguage)
                    translationStatus.setNeedsRetranslation()
                }
            }
        }

        createViewPager(Page.DETAILS.id, getOrderedPages(), null, true)

        refreshTrackable(message)
    }

    override     public Unit onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState)

        outState.putString(STATE_TRANSLATION_LANGUAGE_SOURCE, this.translationStatus.isTranslated() ? this.translationStatus.getSourceLanguage().getCode() : "")
    }

    override     public Unit onResume() {
        super.onResume()

        // resume location access
        geoDataDisposable.add(locationUpdater.start(GeoDirHandler.UPDATE_GEODATA))
    }

    override     public Unit onPause() {
        geoDataDisposable.clear()
        super.onPause()
    }

    private Unit act(final Trackable newTrackable) {
        trackable = newTrackable
        displayTrackable()
        // reset imagelist // @todo mb: more to do?
        imageGallery = null
    }

    private Unit refreshTrackable(final String message) {
        waitDialog = ProgressDialog.show(this, message, res.getString(R.string.trackable_details_loading), true, true)
        createDisposables.add(AndroidRxUtils.bindActivity(this, ConnectorFactory.loadTrackable(geocode, guid, id, brand)).subscribe(
                newTrackable -> {
                    if (trackingCode != null) {
                        newTrackable.setTrackingcode(trackingCode)
                    }
                    act(newTrackable)
                }, throwable -> {
                    Log.w("unable to retrieve trackable information", throwable)
                    showToast(res.getString(R.string.err_tb_find_that))
                    finish()
                }, () -> act(null)))
    }

    override     public Boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.trackable_activity, menu)
        return true
    }

    override     public Boolean onOptionsItemSelected(final MenuItem item) {
        val itemId: Int = item.getItemId()
        if (itemId == R.id.menu_log_touch) {
            startActivityForResult(LogTrackableActivity.getIntent(this, trackable, geocache), LogTrackableActivity.LOG_TRACKABLE)
        } else if (itemId == R.id.menu_browser_trackable) {
            ShareUtils.openUrl(this, trackable.getUrl(), true)
        } else if (itemId == R.id.menu_refresh_trackable) {
            refreshTrackable(StringUtils.defaultIfBlank(trackable.getName(), trackable.getGeocode()))
        } else if (itemId == R.id.menu_translate) {
            TranslationUtils.translate(this, getTranslationText(trackable))
            refreshTrackable(StringUtils.defaultIfBlank(trackable.getName(), trackable.getGeocode()))
        } else {
            return super.onOptionsItemSelected(item)
        }
        return true
    }

    override     public Boolean onPrepareOptionsMenu(final Menu menu) {
        if (trackable != null) {
            menu.findItem(R.id.menu_log_touch).setVisible(StringUtils.isNotBlank(geocode) && trackable.isLoggable())
            menu.findItem(R.id.menu_browser_trackable).setVisible(trackable.hasUrl())
            menu.findItem(R.id.menu_refresh_trackable).setVisible(true)
        }
        MenuUtils.setVisible(menu.findItem(R.id.menu_translate), trackable != null && TranslationUtils.isEnabled())
        menu.findItem(R.id.menu_translate).setTitle(TranslationUtils.getTranslationLabel())

        return super.onPrepareOptionsMenu(menu)
    }

    override     public Unit pullToRefreshActionTrigger() {
        refreshTrackable(StringUtils.defaultIfBlank(trackable.getName(), trackable.getGeocode()))
    }

    public Unit displayTrackable() {
        if (trackable == null) {
            Dialogs.dismiss(waitDialog)

            if (fallbackKeywordSearch != null) {
                CacheListActivity.startActivityKeyword(this, fallbackKeywordSearch)
            } else {
                if (StringUtils.isNotBlank(geocode)) {
                    showToast(res.getString(R.string.err_tb_not_found, geocode))
                } else {
                    showToast(res.getString(R.string.err_tb_find_that))
                }
            }

            finish()
            return
        }

        try {
            geocode = trackable.getGeocode()

            if (StringUtils.isNotBlank(trackable.getName())) {
                setTitle(TextUtils.stripHtml(trackable.getName()))
            } else {
                setTitle(trackable.getName())
            }

            invalidateOptionsMenuCompatible()
            setOrderedPages(getOrderedPages())
            reinitializeViewPager()

        } catch (final Exception e) {
            Log.e("TrackableActivity.loadTrackableHandler: ", e)
        }
        Dialogs.dismiss(waitDialog)
    }

    private static Unit setupIcon(final TrackableActivity activity, final ActionBar actionBar, final String url) {
        val imgGetter: HtmlImage = HtmlImage(HtmlImage.SHARED, false, false, false)
        AndroidRxUtils.bindActivity(activity, imgGetter.fetchDrawable(url)).subscribe(image -> {
            if (actionBar != null) {
                val height: Int = actionBar.getHeight()
                //noinspection SuspiciousNameCombination
                image.setBounds(0, 0, height, height)
                actionBar.setIcon(image)
            }
        })
    }

    private static Unit setupIcon(final ActionBar actionBar, @DrawableRes final Int resId) {
        if (actionBar != null) {
            actionBar.setIcon(resId)
        }
    }

    public static Unit startActivity(final AbstractActivity fromContext, final String guid, final String geocode, final String name, final String geocache, final Int brandId) {
        val trackableIntent: Intent = Intent(fromContext, TrackableActivity.class)
        trackableIntent.putExtra(Intents.EXTRA_GUID, guid)
        trackableIntent.putExtra(Intents.EXTRA_GEOCODE, geocode)
        trackableIntent.putExtra(Intents.EXTRA_NAME, name)
        trackableIntent.putExtra(Intents.EXTRA_GEOCACHE, geocache)
        trackableIntent.putExtra(Intents.EXTRA_BRAND, brandId)
        fromContext.startActivity(trackableIntent)
    }

    override     @SuppressWarnings("rawtypes")
    protected TabbedViewPagerFragment createNewFragment(final Long pageId) {
        if (pageId == Page.DETAILS.id) {
            return DetailsViewCreator()
        } else if (pageId == Page.LOGS.id) {
            return TrackableLogsViewCreator()
        } else if (pageId == Page.IMAGEGALLERY.id) {
            return ImageGalleryViewCreator()
        }
        throw IllegalStateException(); // cannot happen as Long as switch case is enum class complete
    }

    public static class ImageGalleryViewCreator : TabbedViewPagerFragment()<CachedetailImagegalleryPageBinding> {

        override         public CachedetailImagegalleryPageBinding createView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
            return CachedetailImagegalleryPageBinding.inflate(inflater, container, false)
        }

        override         public Long getPageId() {
            return Page.IMAGEGALLERY.id
        }

        override         public Unit setContent() {
            val activity: TrackableActivity = (TrackableActivity) getActivity()
            if (activity == null) {
                return
            }
            val trackable: Trackable = activity.getTrackable()
            if (trackable == null) {
                return
            }
            binding.getRoot().setVisibility(View.VISIBLE)

            if (activity.imageGallery == null) {
                ImageUtils.initializeImageGallery(binding.imageGallery, trackable.getGeocode(), trackable.getImages(), true)
                activity.imageGallery = binding.imageGallery
                reinitializeTitle()
                activity.imageGallery.setImageCountChangeCallback((ig, c) -> reinitializeTitle())
            }
        }

    }

    override     protected String getTitle(final Long pageId) {
        if (pageId == Page.IMAGEGALLERY.id) {
            String title = this.getString(Page.find(pageId).resId)
            if (this.imageGallery != null) {
                title += " (" + this.imageGallery.getImageCount() + ")"
            }
            return title
        }
        return this.getString(Page.find(pageId).resId)
    }

    public static String getTranslationText(final Trackable trackable) {
        if (trackable == null) {
            return ""
        }
        return TranslationUtils.prepareForTranslation(trackable.getGoal(), trackable.getDetails())
    }

    protected Long[] getOrderedPages() {
        val pages: List<Long> = ArrayList<>()
        pages.add(Page.DETAILS.id)
        if (trackable != null) {
            if (CollectionUtils.isNotEmpty(trackable.getLogs())) {
                pages.add(Page.LOGS.id)
            }
            pages.add(Page.IMAGEGALLERY.id)
        }
        final Long[] result = Long[pages.size()]
        for (Int i = 0; i < pages.size(); i++) {
            result[i] = pages.get(i)
        }
        return result
    }

    public static class DetailsViewCreator : TabbedViewPagerFragment()<TrackableDetailsViewBinding> {
        private var descriptionTranslated: Boolean = false

        override         public TrackableDetailsViewBinding createView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
            return TrackableDetailsViewBinding.inflate(getLayoutInflater(), container, false)
        }

        override         public Long getPageId() {
            return Page.DETAILS.id
        }

        override         // splitting up that method would not help improve readability
        @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
        public Unit setContent() {
            val activity: TrackableActivity = (TrackableActivity) getActivity()
            if (activity == null) {
                return
            }
            val trackable: Trackable = activity.getTrackable()
            if (trackable == null) {
                return
            }
            binding.getRoot().setVisibility(View.VISIBLE)

            val details: CacheDetailsCreator = CacheDetailsCreator(activity, binding.detailsList)

            // action bar icon
            if (StringUtils.isNotBlank(trackable.getIconUrl())) {
                setupIcon(activity, activity.getSupportActionBar(), trackable.getIconUrl())
            } else {
                setupIcon(activity.getSupportActionBar(), trackable.getIconBrand())
            }

            // trackable name
            val nameTxtView: TextView = details.add(R.string.trackable_name, StringUtils.isNotBlank(trackable.getName()) ? TextUtils.stripHtml(trackable.getName()) : activity.res.getString(R.string.trackable_unknown)).valueView
            activity.addShareAction(nameTxtView)

            // missing status
            if (trackable.isMissing()) {
                nameTxtView.setPaintFlags(nameTxtView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG)
            }

            // trackable type
            final String tbType
            if (StringUtils.isNotBlank(trackable.getType())) {
                tbType = TextUtils.stripHtml(trackable.getType())
            } else {
                tbType = activity.res.getString(R.string.trackable_unknown)
            }
            details.add(R.string.trackable_brand, trackable.getBrand().getLabel())
            details.add(R.string.trackable_type, tbType)

            // trackable geocode
            activity.addShareAction(details.add(R.string.trackable_code, trackable.getGeocode()).valueView)

            // retrieved status
            val logDate: Date = trackable.getLogDate()
            val logType: LogType = trackable.getLogType()
            if (logDate != null && logType != null) {
                val uri: Uri = Uri.Builder().scheme("https").authority("www.geocaching.com").path("/track/log.aspx").encodedQuery("LUID=" + trackable.getLogGuid()).build()
                val logView: TextView = details.add(R.string.trackable_status, activity.res.getString(R.string.trackable_found, logType.getL10n(), Formatter.formatDate(logDate.getTime()))).valueView
                logView.setOnClickListener(v -> ShareUtils.openUrl(activity, uri.toString()))
            }

            // trackable owner
            val owner: TextView = details.add(R.string.trackable_owner, activity.res.getString(R.string.trackable_unknown)).valueView
            if (StringUtils.isNotBlank(trackable.getOwner())) {
                owner.setText(HtmlCompat.fromHtml(trackable.getOwner(), HtmlCompat.FROM_HTML_MODE_LEGACY), TextView.BufferType.SPANNABLE)
                owner.setOnClickListener(UserClickListener.forOwnerOf(trackable))
            }

            // trackable spotted
            if (StringUtils.isNotBlank(trackable.getSpottedName()) ||
                    trackable.getSpottedType() == Trackable.SPOTTED_UNKNOWN ||
                    trackable.getSpottedType() == Trackable.SPOTTED_OWNER ||
                    trackable.getSpottedType() == Trackable.SPOTTED_ARCHIVED) {

                final StringBuilder text
                Boolean showTimeSpan = true
                switch (trackable.getSpottedType()) {
                    case Trackable.SPOTTED_CACHE:
                        // TODO: the whole sentence fragment should not be constructed, but taken from the resources
                        text = StringBuilder(activity.res.getString(R.string.trackable_spotted_in_cache)).append(' ').append(HtmlCompat.fromHtml(trackable.getSpottedName(), HtmlCompat.FROM_HTML_MODE_LEGACY))
                        break
                    case Trackable.SPOTTED_USER:
                        // TODO: the whole sentence fragment should not be constructed, but taken from the resources
                        text = StringBuilder(activity.res.getString(R.string.trackable_spotted_at_user)).append(' ').append(HtmlCompat.fromHtml(trackable.getSpottedName(), HtmlCompat.FROM_HTML_MODE_LEGACY))
                        break
                    case Trackable.SPOTTED_UNKNOWN:
                        text = StringBuilder(activity.res.getString(R.string.trackable_spotted_unknown_location))
                        break
                    case Trackable.SPOTTED_OWNER:
                        text = StringBuilder(activity.res.getString(R.string.trackable_spotted_owner))
                        break
                    case Trackable.SPOTTED_ARCHIVED:
                        text = StringBuilder(activity.res.getString(R.string.trackable_spotted_archived))
                        break
                    default:
                        text = StringBuilder("N/A")
                        showTimeSpan = false
                        break
                }

                // days since last spotting
                if (showTimeSpan) {
                    for (final LogEntry log : trackable.getLogs()) {
                        if (log.logType == LogType.RETRIEVED_IT || log.logType == LogType.GRABBED_IT || log.logType == LogType.DISCOVERED_IT || log.logType == LogType.PLACED_IT) {
                            text.append(" (").append(Formatter.formatDaysAgo(log.date)).append(')')
                            break
                        }
                    }
                }

                val spotted: TextView = details.add(R.string.trackable_spotted, text.toString()).valueView
                spotted.setClickable(true)
                if (trackable.getSpottedType() == Trackable.SPOTTED_CACHE) {
                    spotted.setOnClickListener(arg0 -> {
                        if (StringUtils.isNotBlank(trackable.getSpottedCacheGeocode())) {
                            CacheDetailActivity.startActivity(activity, trackable.getSpottedCacheGeocode(), trackable.getSpottedName())
                        } else if (StringUtils.isNotBlank(trackable.getSpottedGuid())) {
                            CacheDetailActivity.startActivityGuid(activity, trackable.getSpottedGuid(), trackable.getSpottedName())
                        } else {
                            // for GeoKrety we only know the cache geocode
                            val cacheCode: String = trackable.getSpottedName()
                            if (ConnectorFactory.canHandle(cacheCode)) {
                                CacheDetailActivity.startActivity(activity, cacheCode)
                            }
                        }
                    })
                } else if (trackable.getSpottedType() == Trackable.SPOTTED_USER) {
                    spotted.setOnClickListener(UserClickListener.forUser(trackable, TextUtils.stripHtml(trackable.getSpottedName()), trackable.getSpottedGuid()))
                } else if (trackable.getSpottedType() == Trackable.SPOTTED_OWNER) {
                    spotted.setOnClickListener(UserClickListener.forUser(trackable, TextUtils.stripHtml(trackable.getOwner()), trackable.getOwnerGuid()))
                }
            }

            // trackable origin
            if (StringUtils.isNotBlank(trackable.getOrigin())) {
                val origin: TextView = details.add(R.string.trackable_origin, "").valueView
                origin.setText(HtmlCompat.fromHtml(trackable.getOrigin(), HtmlCompat.FROM_HTML_MODE_LEGACY), TextView.BufferType.SPANNABLE)
                activity.addShareAction(origin)
            }

            // trackable released
            val releasedDate: Date = trackable.getReleased()
            if (releasedDate != null) {
                activity.addShareAction(details.add(R.string.trackable_released, Formatter.formatDate(releasedDate.getTime())).valueView)
            }

            // trackable distance
            if (trackable.getDistance() >= 0) {
                activity.addShareAction(details.add(R.string.trackable_distance, Units.getDistanceFromKilometers(trackable.getDistance())).valueView)
            }

            // trackable goal
            if (StringUtils.isNotBlank(HtmlUtils.extractText(trackable.getGoal()))) {
                binding.goalBox.setVisibility(View.VISIBLE)
                binding.goal.setVisibility(View.VISIBLE)
                binding.goal.setText(HtmlCompat.fromHtml(trackable.getGoal(), HtmlCompat.FROM_HTML_MODE_LEGACY, HtmlImage(activity.geocode, true, false, binding.goal, false), null), TextView.BufferType.SPANNABLE)
                binding.goal.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance())
            }

            // trackable details
            if (StringUtils.isNotBlank(HtmlUtils.extractText(trackable.getDetails()))) {
                binding.detailsBox.setVisibility(View.VISIBLE)
                binding.details.setVisibility(View.VISIBLE)
                binding.details.setText(HtmlCompat.fromHtml(trackable.getDetails(), HtmlCompat.FROM_HTML_MODE_LEGACY, HtmlImage(activity.geocode, true, false, binding.details, false), UnknownTagsHandler()), TextView.BufferType.SPANNABLE)
                binding.details.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance())
            }

            // trackable image
            if (StringUtils.isNotBlank(trackable.getImage())) {
                binding.imageBox.setVisibility(View.VISIBLE)
                val trackableImage: ImageView = (ImageView) activity.getLayoutInflater().inflate(R.layout.trackable_image, binding.image, false)

                trackableImage.setImageResource(R.drawable.image_not_loaded)
                trackableImage.setClickable(true)

                trackableImage.setOnClickListener(view -> ImageViewActivity.openImageView(activity, trackable.getGeocode(), Collections.singletonList(IterableUtils.find(trackable.getImages(), i -> trackable.getImage() == (i.getUrl()))), 0, p -> view))

                AndroidRxUtils.bindActivity(activity, HtmlImage(activity.geocode, true, false, false).fetchDrawable(trackable.getImage())).subscribe(trackableImage::setImageDrawable)

                binding.image.removeAllViews()
                binding.image.addView(trackableImage)
            }

            //external translation
            TranslationUtils.registerTranslation(
                getActivity(),
                binding.descriptionTranslateExternalButton,
                binding.descriptionTranslateExternal,
                binding.descriptionTranslateExternalNote,
                () -> getTranslationText(trackable))

            OfflineTranslateUtils.initializeListingTranslatorInTabbedViewPagerActivity((TrackableActivity) getActivity(), binding.descriptionTranslate, binding.goal.getText().toString() + binding.details.getText().toString(), this::translateListing)

            final OfflineTranslateUtils.Status currentTranslationStatus = activity.translationStatus
            if (currentTranslationStatus.checkRetranslation()) {
                currentTranslationStatus.setNotTranslated()
                translateListing()
            }
        }

        private Unit translateListing() {
            val cda: TrackableActivity = (TrackableActivity) getActivity()

            if (cda.translationStatus.isTranslated()) {
                cda.translationStatus.setNotTranslated()
                setContent()
                return
            }

            final OfflineTranslateUtils.Language sourceLng = cda.translationStatus.getSourceLanguage()
            cda.translationStatus.startTranslation(2, cda, cda.findViewById(R.id.description_translate_button))

            OfflineTranslateUtils.getTranslator(cda, cda.translationStatus, sourceLng,
                    unsupportedLng -> {
                        cda.translationStatus.abortTranslation()
                        binding.descriptionTranslateNote.setText(getResources().getString(R.string.translator_language_unsupported, unsupportedLng))
                    }, modelDownloading -> binding.descriptionTranslateNote.setText(R.string.translator_model_download_notification),
        translator -> {
                        if (null == translator) {
                            binding.descriptionTranslateNote.setText(R.string.translator_translation_initerror)
                            return
                        }

                        for (TextView tv : TextView[] { binding.details, binding.goal }) {
                            OfflineTranslateUtils.translateParagraph(translator, cda.translationStatus, tv.getText().toString(), tv::setText, error -> {
                                binding.descriptionTranslateNote.setText(getResources().getText(R.string.translator_translation_error, error.getMessage()))
                                binding.descriptionTranslateButton.setEnabled(false)
                            })
                        }
                    })
        }

    }

    public Unit addShareAction(final TextView view) {
        view.setOnLongClickListener(v -> {
            ShareUtils.sharePlainText(this, view.getText().toString())
            return true
        })
    }

    override     protected Unit onActivityResult(final Int requestCode, final Int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);  // call super to make lint happy
        // Refresh the logs view after coming back from logging a trackable
        if (requestCode == LogTrackableActivity.LOG_TRACKABLE && resultCode == RESULT_OK) {
            refreshTrackable(StringUtils.defaultIfBlank(trackable.getName(), trackable.getGeocode()))
        } else if (imageGallery != null) {
            imageGallery.onActivityResult(requestCode, resultCode, data)
        }
    }

    override     protected Unit onDestroy() {
        createDisposables.clear()
        if (imageGallery != null) {
            imageGallery.clear()
        }
        super.onDestroy()
    }

    public Trackable getTrackable() {
        return trackable
    }

    override     public Unit finish() {
        Dialogs.dismiss(waitDialog)
        super.finish()
    }
}
