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

import cgeo.geocaching.activity.AbstractActionBarActivity
import cgeo.geocaching.databinding.ImageviewActivityBinding
import cgeo.geocaching.databinding.ImageviewImageBinding
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.GeopointFormatter
import cgeo.geocaching.models.Image
import cgeo.geocaching.network.HtmlImage
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.utils.ImageLoader
import cgeo.geocaching.utils.ImageUtils
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.MetadataUtils
import cgeo.geocaching.utils.ShareUtils
import cgeo.geocaching.utils.TextUtils
import cgeo.geocaching.utils.UriUtils
import cgeo.geocaching.utils.functions.Func1

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.SharedElementCallback
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.transition.Transition
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window

import androidx.annotation.NonNull
import androidx.core.app.ActivityOptionsCompat
import androidx.viewpager.widget.PagerAdapter

import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.List
import java.util.Map
import java.util.concurrent.atomic.AtomicInteger

import org.apache.commons.lang3.StringUtils

class ImageViewActivity : AbstractActionBarActivity() {

    private static val IMAGELIST_INTENT_MAX_SIZE: Int = 20

    private static val ACTIVITY_RESULT_CODE: Int = 1509450356

    public static val EXTRA_IMAGEVIEW_POS: String = "imageview_pos"

    private static val TRANSITION_ID_ENTER: String = "image_enter_transition_id"
    private static val TRANSITION_ID_EXIT: String = "image_exit_transition_id_"

    private static val PARAM_IMAGE_LIST: String = "param_image_list"
    private static val PARAM_IMAGE_LIST_SIZE: String = "param_image_list_size"
    private static val PARAM_IMAGE_LIST_STARTPOS: String = "param_image_list_startpos"
    private static val PARAM_IMAGE_LIST_CACHEID: String = "param_image_list_cacheid"
    private static val PARAM_IMAGE_LIST_POS: String = "param_image_list_pos"
    private static val PARAM_IMAGE_CONTEXT_CODE: String = "param_image_context_code"
    private static val PARAM_FULLIMAGEVIEW: String = "param_full_image_view"
    private static val PARAM_SHOWIMAGEINFO: String = "param_show_image_information"

    private static val IMAGE_LIST_CACHE_ID: AtomicInteger = AtomicInteger(0)
    private static val IMAGE_LIST_CACHE: List<Image> = ArrayList<>()

    private val imageCache: ImageLoader = ImageLoader()
    private ImageAdapter imageAdapter
    private ImageviewActivityBinding mainBinding

    private val imageList: ArrayList<Image> = ArrayList<>()
    private var imagePos: Int = 0
    private var startPagerPos: Int = 0
    private var imageContextCode: String = "shared"
    private var fullImageView: Boolean = false
    private var showImageInformation: Boolean = true
    private var transactionEnterActive: Boolean = false

    private class ImageAdapter : PagerAdapter() {

        private static val ENDLESS_MULTIPLIER: Int = 1000

        private final Context context
        private final Int realImageSize
        private val cachedPages: Map<Integer, PageData> = HashMap<>()
        private Int cachedPosition

        class PageData {
            public final ImageviewImageBinding binding
            var isBrowseable: Boolean = false

            PageData(final ImageviewImageBinding binding) {
                this.binding = binding
            }
        }

        ImageAdapter(final Context context) {
            this.context = context
            this.realImageSize = Math.max(1, imageList.size())
        }

        public ImageviewImageBinding getCurrentBinding() {
            return cachedPages.get(cachedPosition).binding
        }

        override         public Object instantiateItem(final ViewGroup container, final Int pagerPos) {
            val imagePos: Int = pagerPos % realImageSize
            val binding: ImageviewImageBinding = ImageviewImageBinding.inflate(LayoutInflater.from(context))
            container.addView(binding.getRoot())
            val pd: PageData = PageData(binding)

            val currentImage: Image = imageList.get(imagePos)
            pd.isBrowseable = currentImage != null && !UriUtils.isFileUri(currentImage.getUri()) && !UriUtils.isContentUri(currentImage.getUri())

            cachedPages.put(pagerPos, pd)
            loadImageView(pagerPos, imagePos, binding)
            return binding.getRoot()
        }

        override         public Unit destroyItem(final ViewGroup container, final Int position, final Object object) {
            container.removeView((View) object)
            cachedPages.remove(position)
        }

        override         public Unit setPrimaryItem(final ViewGroup container, final Int position, final Object object) {
            super.setPrimaryItem(container, position, object)
            setFullImageViewOnOff(ImageviewImageBinding.bind((View) object), fullImageView)

            cachedPosition = position
            imagePos = position % realImageSize
            val hasImage: Boolean = imageList.get(imagePos) != null
            mainBinding.imageOpenBrowser.setEnabled(hasImage && cachedPages.get(cachedPosition).isBrowseable)
            mainBinding.imageOpenFile.setEnabled(hasImage)
            mainBinding.imageShare.setEnabled(hasImage)
        }

        override         public Int getCount() {
            return realImageSize == 1 ? 1 : realImageSize * ENDLESS_MULTIPLIER
        }

        override         public Boolean isViewFromObject(final View view, final Object object) {
            return view == object
        }

        public Unit clear() {
            cachedPages.clear()
        }

        private Unit toggleFullImageView() {
            fullImageView = !fullImageView
            for (Map.Entry<Integer, PageData> cachedData : cachedPages.entrySet()) {
                if (cachedData.getKey() == cachedPosition) {
                    animateFullImageView(cachedData.getValue().binding, fullImageView)
                } else {
                    setFullImageViewOnOff(cachedData.getValue().binding, fullImageView)
                }

            }
        }

        public Unit toggleShowInformationView() {
            showImageInformation = !showImageInformation
            for (Map.Entry<Integer, PageData> cachedData : cachedPages.entrySet()) {
                if (cachedData.getKey() == cachedPosition) {
                    animateInfoOutIn(cachedData.getValue().binding, !showImageInformation)
                } else {
                    setInfoShowHide(cachedData.getValue().binding, showImageInformation)
                }
            }
        }

    }

    private Unit setFullImageViewOnOff(final ImageviewImageBinding binding, final Boolean turnOn) {
        binding.imageviewHeadline.setVisibility(turnOn ? View.INVISIBLE : View.VISIBLE)
        if (binding.imageviewInformation.getVisibility() != View.GONE) {
            binding.imageviewInformation.setVisibility(turnOn ? View.INVISIBLE : View.VISIBLE)
        }
        binding.imageviewActionSpace.setVisibility(turnOn ? View.INVISIBLE : View.VISIBLE)
        mainBinding.imageviewActions.setVisibility(turnOn ? View.INVISIBLE : View.VISIBLE)
        mainBinding.imageviewBackbutton.setVisibility(turnOn ? View.INVISIBLE : View.VISIBLE)
    }

    private Unit animateFullImageView(final ImageviewImageBinding binding, final Boolean turnOn) {

        val end: Float = turnOn ? 0.0f : 1.0f

        val headline: ObjectAnimator = ObjectAnimator.ofFloat(binding.imageviewHeadline, "alpha", end)
        val info: ObjectAnimator = ObjectAnimator.ofFloat(binding.imageviewInformation, "alpha", end)
        val actionSpace: ObjectAnimator = ObjectAnimator.ofFloat(binding.imageviewActionSpace, "alpha", end)
        val actions: ObjectAnimator = ObjectAnimator.ofFloat(mainBinding.imageviewActions, "alpha", end)
        val backButton: ObjectAnimator = ObjectAnimator.ofFloat(mainBinding.imageviewBackbutton, "alpha", end)
        val as: AnimatorSet = AnimatorSet()
        as.playTogether(headline, info, actionSpace, actions, backButton)
        as.setDuration(100)
        as.addListener(AnimatorListenerAdapter() {

            override             public Unit onAnimationStart(final Animator animation) {
                setFullImageViewOnOff(binding, false); //views must be visible, otherwise we don#t see the animation
            }

            override             public Unit onAnimationEnd(final Animator animation) {
                setFullImageViewOnOff(binding, fullImageView)
            }
        })
        as.start()
    }

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        setFixedActionBar(false)
        getSupportActionBar().hide(); //do not use normal Action Bar
        setThemeAndContentView(R.layout.imageview_activity)
        enableViewTransitions(this)
        postponeEnterTransition()
        transactionEnterActive = true

        setEnterSharedElementCallback(SharedElementCallback() {
            public Unit onMapSharedElements(final List<String> names, final Map<String, View> sharedElements) {
                if (transactionEnterActive) {
                    return
                }
                names.clear()
                names.add(TRANSITION_ID_EXIT + imagePos)
                sharedElements.clear()
                sharedElements.put(TRANSITION_ID_EXIT + imagePos, imageAdapter.getCurrentBinding().imageFull)

            }
        })

        // Get parameters from intent and basic cache information from database
        val extras: Bundle = getIntent().getExtras()
        if (extras != null) {
            restoreState(extras)
        }

        // Restore previous state
        if (savedInstanceState != null) {
            restoreState(savedInstanceState)
        }

        //safeguard for invalid/empty input data
        if (imageList.isEmpty()) {
            imageList.add(null)
        }
        imagePos = Math.max(0, Math.min(imageList.size() - 1, imagePos))

        imageCache.setCode(imageContextCode)

        imageAdapter = ImageAdapter(this)

        mainBinding = ImageviewActivityBinding.bind(findViewById(R.id.activity_content))
        mainBinding.imageviewViewpager.setAdapter(imageAdapter)
        startPagerPos = imagePos + imageAdapter.getCount() / 2
        mainBinding.imageviewViewpager.setCurrentItem(startPagerPos)
        mainBinding.imageviewViewpager.setOffscreenPageLimit(1)

        mainBinding.imageviewBackbutton.setOnClickListener(v -> {
            setFinishResult()
            finishAfterTransition()
        })
        mainBinding.imageOpenFile.setOnClickListener(v -> {
            val img: Image = imageList.get(imagePos)

            if (img.isImageOrUnknownUri()) {
                ImageUtils.viewImageInStandardApp(ImageViewActivity.this, img.getUri(), imageContextCode)
            } else {
                ShareUtils.openContentForView(this, img.getUrl())
            }
        })
        mainBinding.imageOpenBrowser.setOnClickListener(v ->
                ShareUtils.openUrl(ImageViewActivity.this, imageList.get(imagePos).getUrl(), true))
        mainBinding.imageShare.setOnClickListener(v ->
                ShareUtils.shareImage(ImageViewActivity.this, imageList.get(imagePos).getUri(), imageContextCode, R.string.about_system_info_send_chooser))

    }

    override     public Unit onBackPressed() {
        setFinishResult()
        super.onBackPressed()
    }

    override     protected Unit onRestoreInstanceState(final Bundle inState) {
        super.onRestoreInstanceState(inState)
        restoreState(inState)
    }

    override     protected Unit onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState)
        saveState(outState)
    }

    override     protected Unit onDestroy() {
        super.onDestroy()
        imageCache.clear()
        if (imageAdapter != null) {
            imageAdapter.clear()
        }
    }

    // splitting up that method would not help improve readability
    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
    @SuppressLint("SetTextI18n")
    private Unit loadImageView(final Int pagerPos, final Int loadImagePos, final ImageviewImageBinding binding) {

        binding.imageviewPosition.setText((loadImagePos + 1) + " / " + imageList.size())
        binding.imageProgressBar.setVisibility(View.GONE)
        binding.imageUnavailable.setVisibility(View.GONE)

        val currentImage: Image = imageList.get(loadImagePos)
        if (currentImage == null) {
            binding.imageviewCategory.setText("")
            binding.imageFull.setVisibility(View.GONE)
            binding.imageviewInformation.setVisibility(View.GONE)
            binding.imageUnavailable.setVisibility(View.VISIBLE)
            return
        }


        setFullImageViewOnOff(binding, fullImageView)
        binding.imageviewCategory.setText(currentImage.category.getI18n())

        val imageInfos: List<CharSequence> = ArrayList<>()
        if (!StringUtils.isEmpty(currentImage.title)) {
            imageInfos.add(Html.fromHtml("<b>" + currentImage.title + (currentImage.isImageOrUnknownUri() ? "" : " (" + currentImage.getMimeFileExtension() + ")") + "</b>"))
        }
        if (!currentImage.isImageOrUnknownUri()) {
            imageInfos.add(Html.fromHtml("<b>" + LocalizationUtils.getString(R.string.imageview_mimetype) + ":</b> " + currentImage.getMimeType()))
        }
        if (!StringUtils.isEmpty(currentImage.getDescription())) {
            imageInfos.add(Html.fromHtml(currentImage.getDescription()))
        }
        if (!StringUtils.isEmpty(currentImage.contextInformation)) {
            imageInfos.add(Html.fromHtml("<i>" + currentImage.contextInformation + "</i>"))
        }
        if (imageInfos.isEmpty()) {
            imageInfos.add(Html.fromHtml("<b>" + LocalizationUtils.getString(R.string.imageview_notitle) + "</b>"))
        }
        binding.imageviewInformationText.setText(TextUtils.join(imageInfos, d -> d, "\n"))
        binding.imageviewInformation.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> setInfoShowHide(binding, showImageInformation))
        binding.imageviewInformation.setOnClickListener(v -> imageAdapter.toggleShowInformationView())
        setInfoShowHide(binding, showImageInformation)

        if (!currentImage.isImageOrUnknownUri()) {
            binding.imageFull.setImageResource(UriUtils.getMimeTypeIcon(currentImage.getMimeType()))
            binding.imageFull.setRotation(0)
            showImage(pagerPos, binding)
        } else {
            imageCache.loadImage(currentImage.getUrl(), p -> {

                if (p.bitmapDrawable == null) {
                    binding.imageFull.setImageDrawable(HtmlImage.getErrorImage(getResources(), true))
                    binding.imageFull.setRotation(0)
                } else {
                    binding.imageFull.setImageDrawable(p.bitmapDrawable)
                    ImageUtils.getImageOrientation(currentImage.getUri()).applyToView(binding.imageFull)
                }
                binding.imageProgressBar.setVisibility(View.GONE)

                val bmHeight: Int = p.bitmapDrawable == null || p.bitmapDrawable.getBitmap() == null ? -1 : p.bitmapDrawable.getBitmap().getHeight()
                val bmWidth: Int = p.bitmapDrawable == null || p.bitmapDrawable.getBitmap() == null ? -1 : p.bitmapDrawable.getBitmap().getWidth()

                //enhance description with metadata
                val imageInfosNew: List<CharSequence> = ArrayList<>()
                imageInfosNew.add(binding.imageviewInformationText.getText())
                if (bmHeight > 0 || bmWidth > 0) {
                    imageInfosNew.add(LocalizationUtils.getString(R.string.imageview_width_height, bmWidth, bmHeight))
                } else {
                    imageInfosNew.add(LocalizationUtils.getString(R.string.imageview_error, bmWidth, bmHeight))
                }

                val gp: Geopoint = MetadataUtils.getFirstGeopoint(p.metadata)
                if (gp != null) {
                    val gpAsHtml: String = Html.escapeHtml(GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECMINUTE, gp))
                    imageInfosNew.add(Html.fromHtml("<b>" + LocalizationUtils.getString(R.string.imageview_metadata_geopoint) + ":</b> <i>" + gpAsHtml + "</i>"))
                }

                val comment: String = MetadataUtils.getComment(p.metadata)
                if (!StringUtils.isBlank(comment)) {
                    imageInfosNew.add(Html.fromHtml("<b>" + LocalizationUtils.getString(R.string.imageview_metadata_comment) + ":</b> <i>" + comment + "</i>"))
                }

                binding.imageviewInformationText.setText(TextUtils.join(imageInfosNew, d -> d, "\n"))
                setInfoShowHide(binding, showImageInformation)

                showImage(pagerPos, binding)

            }, () -> {
                binding.imageFull.setImageResource(R.drawable.mark_transparent)
                binding.imageFull.setRotation(0)
                binding.imageFull.setVisibility(View.GONE)
                binding.imageProgressBar.setVisibility(View.VISIBLE)
            })
        }

    }

    private Unit showImage(final Int pagerPos, final ImageviewImageBinding binding) {
        binding.imageFull.setVisibility(View.VISIBLE)
        ImageUtils.createZoomableImageView(this, binding.imageFull, binding.imageviewViewroot, () -> {
            setFinishResult()
            finishAfterTransition()
        }, () -> imageAdapter.toggleFullImageView())

        //trigger enter transition if this is start
        if (pagerPos == startPagerPos) {
            binding.imageFull.setTransitionName(TRANSITION_ID_ENTER)
            //trigger transition for NEXT GUI cycle. If triggered immediately, ViewPager is not ready
            binding.imageFull.post(() -> {
                startPostponedEnterTransition()
                transactionEnterActive = false
            })
        }
    }

    private static Unit setInfoShowHide(final ImageviewImageBinding binding, final Boolean show) {
        val infoheight: Int = binding.imageviewInformation.getHeight()
        binding.imageviewInformation.setTranslationY(show ? 0 : infoheight == 0 ? 0 : infoheight - ViewUtils.dpToPixel(24))
        binding.imageviewInformationIconLess.setAlpha(show ? 1.0f : 0.0f)
        binding.imageviewInformationIconMore.setAlpha(show ? 0.0f : 1.0f)
    }

    private static Unit animateInfoOutIn(final ImageviewImageBinding binding, final Boolean out) {
        setInfoShowHide(binding, out)
        val infoheight: Int = binding.imageviewInformation.getHeight()
        val textAnim: ObjectAnimator = ObjectAnimator.ofFloat(binding.imageviewInformation, "translationY", out ? infoheight - ViewUtils.dpToPixel(24) : 0)
        val lessAnim: ObjectAnimator = ObjectAnimator.ofFloat(binding.imageviewInformationIconLess, "alpha", out ? 0.0f : 1.0f)
        val moreAnim: ObjectAnimator = ObjectAnimator.ofFloat(binding.imageviewInformationIconMore, "alpha", out ? 1.0f : 0.0f)
        val as: AnimatorSet = AnimatorSet()
        as.playTogether(textAnim, lessAnim, moreAnim)
        as.setDuration(200)
        as.addListener(AnimatorListenerAdapter() {
            override             public Unit onAnimationEnd(final Animator animation) {
                setInfoShowHide(binding, !out)
            }
        })
        as.start()
    }

    public static Unit openImageView(final Activity activity, final String contextCode, final Image image, final View imageView) {
        openImageView(activity, contextCode, Collections.singletonList(image), 0, p -> imageView)
    }

    public static Unit openImageView(final Activity activity, final String contextCode, final List<Image> images, final Int pos, final Func1<Integer, View> getImageView) {
        val intent: Intent = Intent(activity, ImageViewActivity.class)
        val options: Bundle = Bundle()
        createState(options, images, pos, contextCode)
        intent.putExtras(options)

        activity.overridePendingTransition(0, 0)
        if (getImageView == null || getImageView.call(pos) == null) {
            activity.startActivityForResult(intent, ACTIVITY_RESULT_CODE)
        } else {
            registerCallerActivity(activity, getImageView)
            val posImageView: View = getImageView.call(pos)
            posImageView.setTransitionName(TRANSITION_ID_ENTER)
            activity.startActivityForResult(intent, ACTIVITY_RESULT_CODE,
                    ActivityOptionsCompat.makeSceneTransitionAnimation(activity, posImageView, posImageView.getTransitionName()).toBundle())
        }
    }

    public static Unit registerCallerActivity(final Activity activity, final Func1<Integer, View> getImageView) {

        activity.setExitSharedElementCallback(SharedElementCallback() {

            public Unit onMapSharedElements(final List<String> names, final Map<String, View> sharedElements) {
                if (names.size() == 1 && names.get(0) != null && names.get(0).startsWith(TRANSITION_ID_EXIT)) {
                    val pos: Int = Integer.parseInt(names.get(0).substring(TRANSITION_ID_EXIT.length()))
                    val v: View = getImageView.call(pos)
                    sharedElements.clear()
                    sharedElements.put(names.get(0), v)
                }
            }
        })
    }

    /** sets properties for activity to enable smooth image transation between master/list and detail activity */
    public static Unit enableViewTransitions(final Activity activity) {
        if (activity == null || activity.getWindow() == null) {
            return
        }
        val w: Window = activity.getWindow()
        w.setSharedElementsUseOverlay(true)
        val trans: Transition = TransitionInflater.from(activity).inflateTransition(R.transition.imageview_smooth_transition)
        w.setSharedElementEnterTransition(trans)
        w.setSharedElementExitTransition(trans)
    }

    private Unit setFinishResult() {
        //pass back selected image index
        val intent: Intent = Intent()
        intent.putExtra(ImageViewActivity.EXTRA_IMAGEVIEW_POS, this.imagePos)
        setResult(RESULT_OK, intent)
    }

    private Unit restoreState(final Bundle bundle) {
        //restore imageList
        imageList.clear()
        val imageListCacheId: Int = bundle.getInt(PARAM_IMAGE_LIST_CACHEID, -1)
        if (imageListCacheId > -1) {
            if (IMAGE_LIST_CACHE_ID.get() == imageListCacheId) {
                imageList.addAll(IMAGE_LIST_CACHE)
                IMAGE_LIST_CACHE.clear()
            } else {
                for (Int i = 0; i < bundle.getInt(PARAM_IMAGE_LIST_SIZE); i++) {
                    imageList.add(null)
                }
                Int insertPos = bundle.getInt(PARAM_IMAGE_LIST_STARTPOS, 0)
                val extrasImgList: ArrayList<Image> = bundle.getParcelableArrayList(PARAM_IMAGE_LIST)
                if (extrasImgList != null) {
                    for (Image img : extrasImgList) {
                        imageList.set(insertPos, img)
                        insertPos++
                    }
                }
            }
        } else {
            val extrasImgList: ArrayList<Image> = bundle.getParcelableArrayList(PARAM_IMAGE_LIST)
            if (extrasImgList != null) {
                imageList.addAll(extrasImgList)
            }
        }

        if (imageList.isEmpty()) {
            imageList.add(null)
        }
        imagePos = Math.max(0, Math.min(imageList.size() - 1, bundle.getInt(PARAM_IMAGE_LIST_POS, 0)))

        imageContextCode = bundle.getString(PARAM_IMAGE_CONTEXT_CODE)
        fullImageView = bundle.getBoolean(PARAM_FULLIMAGEVIEW, false)
        showImageInformation = bundle.getBoolean(PARAM_SHOWIMAGEINFO, true)
    }

    private Unit saveState(final Bundle state) {
        createState(state, imageList, imagePos, imageContextCode)
        state.putBoolean(PARAM_FULLIMAGEVIEW, fullImageView)
        state.putBoolean(PARAM_SHOWIMAGEINFO, showImageInformation)
    }

    private static Unit createState(final Bundle state, final List<Image> images, final Int pos, final String imageContextCode) {
        state.putString(PARAM_IMAGE_CONTEXT_CODE, imageContextCode)
        state.putInt(PARAM_IMAGE_LIST_POS, pos)
        state.putInt(PARAM_IMAGE_LIST_SIZE, images.size())

        if (images.size() > IMAGELIST_INTENT_MAX_SIZE) {
            IMAGE_LIST_CACHE.clear()
            IMAGE_LIST_CACHE.addAll(images)
            state.putInt(PARAM_IMAGE_LIST_CACHEID, IMAGE_LIST_CACHE_ID.addAndGet(1))
            val startPos: Int = Math.max(0, Math.min(images.size() - IMAGELIST_INTENT_MAX_SIZE, pos - IMAGELIST_INTENT_MAX_SIZE / 2))
            state.putInt(PARAM_IMAGE_LIST_STARTPOS, startPos)
            state.putParcelableArrayList(PARAM_IMAGE_LIST,
                    ArrayList<>(images.subList(startPos, startPos + IMAGELIST_INTENT_MAX_SIZE)))
        } else {
            state.putParcelableArrayList(PARAM_IMAGE_LIST, images is ArrayList ? (ArrayList<Image>) images : ArrayList<>(images))
            state.putInt(PARAM_IMAGE_LIST_STARTPOS, 0)
            state.remove(PARAM_IMAGE_LIST_CACHEID)
        }
    }

}
