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

package cgeo.geocaching.ui

import cgeo.geocaching.ImageViewActivity
import cgeo.geocaching.R
import cgeo.geocaching.apps.navi.NavigationAppFactory
import cgeo.geocaching.databinding.ImageGalleryCategoryBinding
import cgeo.geocaching.databinding.ImageGalleryImageBinding
import cgeo.geocaching.databinding.ImagegalleryViewBinding
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Image
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.service.GeocacheChangedBroadcastReceiver
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.ui.dialog.ContextMenuDialog
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter
import cgeo.geocaching.utils.CategorizedListHelper
import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.ImageLoader
import cgeo.geocaching.utils.ImageUtils
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.MetadataUtils
import cgeo.geocaching.utils.ShareUtils
import cgeo.geocaching.utils.UriUtils
import cgeo.geocaching.utils.functions.Action2
import cgeo.geocaching.utils.functions.Func1

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.text.TextUtils
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

import java.util.ArrayList
import java.util.Collection
import java.util.HashMap
import java.util.List
import java.util.Map

import org.apache.commons.lang3.StringUtils

class ImageGalleryView : LinearLayout() {

    private static val IMAGE_TARGET_WIDTH_DP: Int = 160
    private static val IMAGE_TARGET_HEIGHT_DP: Int = 200; // without title textview

    private Context context
    private Activity activity
    private ImagegalleryViewBinding binding
    private ImageGalleryAdapter adapter

    private val categorizedImageListHelper: CategorizedListHelper = CategorizedListHelper()

    private var allowOpenViewer: Boolean = true
    private var activityReenterCalled: Boolean = false

    private var imageHelper: ImageActivityHelper = null
    private val imageLoader: ImageLoader = ImageLoader()

    private String geocode
    private var imageCount: Int = 0

    private var imageCountChangeCallback: Action2<ImageGalleryView, Integer> = null

    private val imageCoordMap: Map<String, Geopoint> = HashMap<>()
    private val editableCategoryHandlerMap: Map<String, EditableCategoryHandler> = HashMap<>()

    interface EditableCategoryHandler {

        Collection<Image> getAllImages()

        Collection<Image> add(Collection<Image> images)

        Unit delete(Image image)

        Image setTitle(Image image, String title)

    }

    public static class ImageGalleryEntryHolder : AbstractRecyclerViewHolder() {

        public final ImageGalleryImageBinding imageBinding
        public final ImageGalleryCategoryBinding categoryBinding

        public ImageGalleryEntry entry

        public static ImageGalleryEntryHolder createForImage(final ViewGroup view) {
            return ImageGalleryEntryHolder(ImageGalleryImageBinding.inflate(LayoutInflater.from(view.getContext()), view, false), null)
        }

        public static ImageGalleryEntryHolder createForCategory(final ViewGroup view) {
            return ImageGalleryEntryHolder(null, ImageGalleryCategoryBinding.inflate(LayoutInflater.from(view.getContext()), view, false))
        }

        private ImageGalleryEntryHolder(final ImageGalleryImageBinding imageBinding, final ImageGalleryCategoryBinding categoryBinding) {
            super(imageBinding == null ? categoryBinding.getRoot() : imageBinding.getRoot())
            this.imageBinding = imageBinding
            this.categoryBinding = categoryBinding
        }
    }

    class ImageGalleryAdapter : ManagedListAdapter()<ImageGalleryEntry, ImageGalleryEntryHolder> {

        protected ImageGalleryAdapter(final RecyclerView recyclerView) {
            super(Config(recyclerView))
        }

        override         public ImageGalleryEntryHolder onCreateViewHolder(final ViewGroup parent, final Int viewType) {
            return viewType == 0 ? ImageGalleryEntryHolder.createForCategory(parent) : ImageGalleryEntryHolder.createForImage(parent)
        }

        override         public Unit onBindViewHolder(final ImageGalleryEntryHolder holder, final Int position) {
            holder.entry = getItem(position)
            if (holder.imageBinding != null) {
                fillView(holder, holder.imageBinding)
            } else {
                fillCategoryView(holder, holder.categoryBinding)
            }
        }

        override         public Int getItemViewType(final Int position) {
            return getItem(position).isHeader() ? 0 : 1
        }

    }



    override     protected Unit onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig)
        recalculateLayout(newConfig)
    }

    private Unit recalculateLayout(final Configuration newConfig) {

        val manager: GridLayoutManager = (GridLayoutManager) binding.imagegalleryList.getLayoutManager()
        val colCount: Int = Math.max(1, newConfig.screenWidthDp / (IMAGE_TARGET_WIDTH_DP + 10))
        manager.setSpanCount(colCount)
        manager.setSpanSizeLookup(GridLayoutManager.SpanSizeLookup() {
            override             public Int getSpanSize(final Int pos) {
                return adapter.getItem(pos).isHeader() ? colCount : 1
            }
        })
        manager.getSpanSizeLookup().setSpanGroupIndexCacheEnabled(true)
        manager.getSpanSizeLookup().setSpanIndexCacheEnabled(true)
    }

    private Unit invalidateSpanIndexCaches() {
        val manager: GridLayoutManager = (GridLayoutManager) binding.imagegalleryList.getLayoutManager()
        manager.getSpanSizeLookup().invalidateSpanGroupIndexCache()
        manager.getSpanSizeLookup().invalidateSpanIndexCache()
    }

    private static class ImageGalleryEntry {

        public final Image image
        public final String category

        ImageGalleryEntry(final String category) {
            this(category, null)
        }

        ImageGalleryEntry(final String category, final Image image) {
            this.image = image
            this.category = category
        }

        public Boolean isHeader() {
            return image == null
        }
    }

    public Unit fillView(final ImageGalleryEntryHolder viewHolder, final ImageGalleryImageBinding binding) {

        val imgData: ImageGalleryEntry = viewHolder.entry
        val img: Image = imgData.image
        setImageTitle(binding, img.getTitle())

        binding.imageWrapper.setImagePreload(img)
        if (!img.isImageOrUnknownUri()) {
            binding.imageTitle.setText(TextUtils.concat(binding.imageTitle.getText(), " (" + UriUtils.getMimeFileExtension(img.getUri()) + ")"))
        } else {

            imageLoader.loadImage(img.getUrl(), p -> {

                val currentEntry: ImageGalleryEntry = viewHolder.entry
                if (!currentEntry.image.getUrl() == (img.getUrl())) {
                    //this means that meanwhile in the same ViewHolder another image was place
                    return
                }

                binding.imageWrapper.setImagePostload(img, p.bitmapDrawable, p.metadata)

                val gp: Geopoint = MetadataUtils.getFirstGeopoint(p.metadata)

                if (gp != null) {
                    imageCoordMap.put(img.getUrl(), gp)
                }
            })
        }


        binding.imageWrapper.setOnClickListener(v -> openImageDetails(imgData, binding))
        binding.imageWrapper.setOnLongClickListener(v -> {
            showContextOptions(imgData, binding)
            return true
        })
    }

    private Unit setImageTitle(final ImageGalleryImageBinding binding, final CharSequence text) {
        if (!StringUtils.isBlank(text)) {
            binding.imageTitle.setText(TextParam.text(text).setHtml(true).getText(getContext()))
            binding.imageTitle.setVisibility(View.VISIBLE)
        } else {
            binding.imageTitle.setVisibility(View.GONE)
        }
    }

    public ImageGalleryView(final Context context) {
        super(context)
        init()
    }

    public ImageGalleryView(final Context context, final AttributeSet attrs) {
        super(context, attrs)
        init()
    }

    public ImageGalleryView(final Context context, final AttributeSet attrs, final Int defStyleAttr) {
        super(context, attrs, defStyleAttr)
        init()
    }

    private Unit init() {
        setOrientation(VERTICAL)
        this.context = ContextThemeWrapper(getContext(), R.style.cgeo)
        this.activity = ViewUtils.toActivity(this.context)

        inflate(this.context, R.layout.imagegallery_view, this)
        binding = ImagegalleryViewBinding.bind(this)
        this.adapter = ImageGalleryAdapter(binding.imagegalleryList)
        binding.imagegalleryList.setAdapter(this.adapter)
        binding.imagegalleryList.setLayoutManager(GridLayoutManager(this.context, 2))

        recalculateLayout(getContext().getResources().getConfiguration())

        //enable transitions between gallery and image detail activity
        ImageViewActivity.enableViewTransitions(this.activity)
    }

    /** (re)sets contextual information for gallery */
    public Unit setup(final String geocode) {
        this.geocode = geocode
        this.imageLoader.setCode(this.geocode)
    }

    /** gets total number of images currently displayed in this gallery */
    public Int getImageCount() {
        return categorizedImageListHelper.getContentSize()
    }

    /** adds images for display to this gallery */
    public Unit addImages(final Collection<Image> images) {
        addImages(images, i -> i.category == Image.ImageCategory.UNCATEGORIZED ? null : i.category.getI18n())
    }

    /** adds images for display to this gallery */
    public Unit addImages(final Collection<Image> images, final Func1<Image, String> categoryMapper) {
        addImagesInternal(images, categoryMapper, false)
    }

    private Unit addImagesInternal(final Collection<Image> images, final Func1<Image, String> categoryMapper, final Boolean force) {

        //order images by category
        val categories: List<String> = ArrayList<>()
        final Map<String, List<ImageGalleryEntry>> catImages = HashMap<>()
        for (Image img : images) {
            val category: String = categoryMapper.call(img)
            if (!force && editableCategoryHandlerMap.containsKey(category)) {
                continue
            }
            if (!catImages.containsKey(category)) {
                categories.add(category)
                catImages.put(category, ArrayList<>())
            }
            catImages.get(category).add(ImageGalleryEntry(category, img))
        }

        //add to gallery category-wise
        for (String category : categories) {

            if (!categorizedImageListHelper.containsCategory(category)) {
                createCategory(category, false)
            }
            adapter.addItems(categorizedImageListHelper.getCategoryInsertPosition(category), catImages.get(category))
            categorizedImageListHelper.addToCount(category, catImages.get(category).size())
        }

        //invalidate caches
        invalidateSpanIndexCaches()
    }

    private Unit changeImageCount(final Int by) {
        if (by != 0) {
            imageCount += by
            if (imageCountChangeCallback != null) {
                imageCountChangeCallback.call(this, imageCount)
            }
        }
    }

    public Unit createCategory(final String category, final Boolean atStart) {

        if (!categorizedImageListHelper.containsCategory(category)) {
            categorizedImageListHelper.addOrMoveCategory(category, atStart)
            adapter.addItem(atStart ? 0 : adapter.getItemCount(), ImageGalleryEntry(category))
        }
    }

    private Unit fillCategoryView(final ImageGalleryEntryHolder entry, final ImageGalleryCategoryBinding binding) {

        val category: String = entry.entry.category
        if (StringUtils.isBlank(category)) {
            binding.imgGalleryCategoryTitle.setVisibility(View.GONE)
        } else {
            binding.imgGalleryCategoryTitle.setVisibility(View.VISIBLE)
            binding.imgGalleryCategoryTitle.setText(category)
        }
        if (entry.getLayoutPosition() == 0) {
            binding.imgGalleryCategoryTitle.setSeparatorAboveVisible(false)
        }

        val isEditableCat: Boolean = this.editableCategoryHandlerMap.containsKey(category)
        binding.imgGalleryAddButtons.setVisibility(isEditableCat ? View.VISIBLE : View.GONE)
        binding.imgGalleryAddMultiImages.setOnClickListener(v -> imageHelper.getMultipleImagesFromStorage(geocode, false, category, null))
        binding.imgGalleryAddCamera.setOnClickListener(v -> imageHelper.getImageFromCamera(geocode, false, category))
        binding.imgGalleryAddMultiFiles.setOnClickListener(v -> imageHelper.getMultipleFilesFromStorage(geocode, false, category))

    }

    /** adds an editable category to this gallery. This is a category where user has options to add/rename/remove images */
    public Unit setEditableCategory(final String category, final EditableCategoryHandler handler) {
        if (this.activity == null) {
            //not supported if not in context of an activity
            return
        }

        removeCategory(category)
        this.editableCategoryHandlerMap.put(category, handler)
        createCategory(category, true)

        //fill initially
        addImagesInternal(handler.getAllImages(), i -> category, true)

        if (imageHelper == null) {
            imageHelper = ImageActivityHelper(activity, (r, imgUris, uk) -> {
                val imgs: Collection<Image> = CollectionStream.of(imgUris).map(uri -> Image.Builder().setUrl(uri).build()).toList()
                val addedImgs: Collection<Image> = editableCategoryHandlerMap.get(uk).add(imgs)
                if (r == ImageActivityHelper.REQUEST_CODE_CAMERA) {
                    //change title of camera photos
                    val copy: Collection<Image> = ArrayList<>(addedImgs)
                    addedImgs.clear()
                    Int idx = categorizedImageListHelper.getCategoryCount(uk) + 1
                    for (Image i : copy) {
                        val newImage: Image = editableCategoryHandlerMap.get(uk).setTitle(i, LocalizationUtils.getString(R.string.cache_image_title_camera_prefix) + " " + idx)
                        if (newImage == null) {
                            addedImgs.add(i)
                        } else {
                            addedImgs.add(newImage)
                            idx++
                        }
                    }
                }

                addImagesInternal(addedImgs, i -> uk, true)
            })
        }
    }

    /** Important: include this method in your activity to support activity-related tasks. Necessary for e.g. editable categories */
    public Boolean onActivityResult(final Int requestCode, final Int resultCode, final Intent data) {
        allowOpenViewer()
        //in some situations "onActivityReenter" is not called. In these cases -> initialize position here
        if (!activityReenterCalled) {
            onActivityReenter(activity, this, data)
        }
        activityReenterCalled = false
        if (imageHelper != null) {
            return imageHelper.onActivityResult(requestCode, resultCode, data)
        }
        return false
    }

    /** registers a callback which is triggered whenever the count of images displayed in gallery changes */
    public Unit setImageCountChangeCallback(final Action2<ImageGalleryView, Integer> callback) {
        this.imageCountChangeCallback = callback
    }

    /** scrolls the image gallery to the list index given */
    private Unit scrollTo(final Int listIndex) {
        val realIndex: Int = Math.max(0, Math.min(adapter.getItemCount() - 1, listIndex))
        val recyclerView: RecyclerView = binding.imagegalleryList
        val layoutManager: GridLayoutManager = (GridLayoutManager) recyclerView.getLayoutManager()
        //check if item is already (completely) visible --> then don't scroll
        val viewAtPosition: View = layoutManager.findViewByPosition(realIndex)
        if (viewAtPosition == null || layoutManager
                .isViewPartiallyVisible(viewAtPosition, false, true)) {

            //view is either not or only partially visible --> scroll

            Int offset = 50; //default
            //try to calculate an offset such that the image to scroll to ends up in the middle of the gallery view
            val imageHeight: Int = ViewUtils.dpToPixel(IMAGE_TARGET_HEIGHT_DP)
            val height: Int = this.getHeight()
            if (height >= imageHeight) {
                offset = (height - imageHeight) / 2
            }

            //use "scrollToPositionWithOffset" since "scrollToPosition" behaves strange -> see issue #13586
            layoutManager.scrollToPositionWithOffset(realIndex, offset)
        }
    }

    /** returns the image view for given image index (or null if index is invalid) */
    public View getImageViewForContentIndex(final Int contentIndex) {
        val pos: Int = categorizedImageListHelper.getListIndexForContentIndex(contentIndex)
        if (pos == -1) {
            return null
        }
        val vh: ImageGalleryEntryHolder = (ImageGalleryEntryHolder) binding.imagegalleryList.findViewHolderForAdapterPosition(pos)
        if (vh == null || vh.imageBinding == null) {
            return null
        }
        return vh.imageBinding.imageWrapper.getImageView()
    }

    /** clears this gallery */
    public Unit clear() {
        this.imageLoader.clear()
        this.adapter.clearList()
        this.categorizedImageListHelper.clear()
        this.imageCount = 0
    }

    /** registers the activity associated with this gallery for image transtions towards image detail view */
    public Unit registerCallerActivity() {
        ImageViewActivity.registerCallerActivity(this.activity, this::getImageViewForContentIndex)
    }

    public Unit removeCategory(final String category) {
        if (!categorizedImageListHelper.containsCategory(category)) {
            return
        }

        changeImageCount(-categorizedImageListHelper.getCategoryCount(category))
        val titlePos: Int = categorizedImageListHelper.getCategoryTitlePosition(category)
        for (Int pos = categorizedImageListHelper.getCategoryInsertPosition(category) - 1; pos >= titlePos; pos--) {
            adapter.removeItem(pos)
        }
        categorizedImageListHelper.removeCategory(category)
    }

    override     protected Unit onDetachedFromWindow() {
        super.onDetachedFromWindow()

        //clear image cache
        this.imageLoader.clear()
    }

    /** Important: include this in your activity's "onActivityReenter" method.
     * Needed e.g. for correct back transition when returning from detail view (e.g. scrolling to image gallery position) */
    public static Int onActivityReenter(final Activity activity, final ImageGalleryView imageGallery, final Intent data) {
        val pos: Int = data == null || data.getExtras() == null ? -1 : data.getExtras().getInt(ImageViewActivity.EXTRA_IMAGEVIEW_POS, -1)
        if (pos >= 0) {
            activity.postponeEnterTransition()
            if (imageGallery != null) {
                imageGallery.initializeToPosition(pos)
                imageGallery.activityReenterCalled = true
            }
        }
        return pos
    }

    /** initializes gallery to a certain position.
     * Use this method directly for gallery recreation if gallery was not available in activity's "onActivityReender" method */
    public Unit initializeToPosition(final Int contentIndex) {
        val realPos: Int = Math.max(-1, Math.min(adapter.getItemCount() - 1, contentIndex))
        this.allowOpenViewer()
        if (realPos >= 0) {
            this.scrollTo(categorizedImageListHelper.getListIndexForContentIndex(realPos))
        }
        this.post(() -> activity.startPostponedEnterTransition())
    }

    /** allows the gallery to open a image detail view (again) */
    private Unit allowOpenViewer() {
        this.allowOpenViewer = true
    }

    private Unit openImageDetails(final ImageGalleryEntry imgData, final ImageGalleryImageBinding binding) {
        if (imgData.image.isImageOrUnknownUri()) {
            openImageViewer(binding)
        } else {
            ShareUtils.openContentForView(getContext(), imgData.image.getUrl())
        }
    }

    private Unit openImageViewer(final ImageGalleryImageBinding binding) {
        if (activity == null || !allowOpenViewer) {
            return
        }
        allowOpenViewer = false

        val intentPos: Int = this.binding.imagegalleryList.getChildAdapterPosition(binding.getRoot())
        val contentPos: Int = categorizedImageListHelper.getContentIndexForListIndex(intentPos)

        val images: List<Image> = ArrayList<>()
        for (ImageGalleryEntry item : adapter.getItems()) {
            if (!item.isHeader()) {
                images.add(item.image)
            }
        }

        ImageViewActivity.openImageView(this.activity, geocode, images, contentPos,
                this::getImageViewForContentIndex)
    }

    // splitting up that method would not help improve readability
    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
    private Unit showContextOptions(final ImageGalleryEntry imgData, final ImageGalleryImageBinding binding) {
        if (activity == null || imgData == null || imgData.image == null) {
            return
        }
        val category: String = imgData.category
        val img: Image = imgData.image

        val intentPos: Int = this.binding.imagegalleryList.getChildAdapterPosition(binding.getRoot())

        val ctxMenu: ContextMenuDialog = ContextMenuDialog(activity)
        ctxMenu.setTitle(LocalizationUtils.getString(R.string.cache_image))

        ctxMenu.addItem(LocalizationUtils.getString(R.string.cache_image_open), R.drawable.ic_menu_image,
                v -> openImageDetails(imgData, binding))

        ctxMenu.addItem(LocalizationUtils.getString(R.string.cache_image_open_file), R.drawable.ic_menu_image,
                v -> ImageUtils.viewImageInStandardApp(activity, img.getUri(), geocode))

        if (!UriUtils.isFileUri(img.getUri()) && !UriUtils.isContentUri(img.getUri())) {
            ctxMenu.addItem(LocalizationUtils.getString(R.string.cache_image_open_browser), R.drawable.ic_menu_open_in_browser,
                    v -> ShareUtils.openUrl(context, img.getUrl(), false))
        }
        if (img.hasDescription()) {
            ctxMenu.addItem(LocalizationUtils.getString(R.string.cache_image_show_description), R.drawable.ic_menu_hint,
                    v -> SimpleDialog.ofContext(getContext()).setTitle(R.string.log_image_description)
                            .setMessage(TextParam.text(img.getDescription()).setHtml(true)).show())
        }
        val gp: Geopoint = imageCoordMap.get(img.getUrl())
        if (gp != null && geocode != null) {
            ctxMenu.addItem(LocalizationUtils.getString(R.string.cache_image_add_waypoint), R.drawable.ic_menu_myposition, v -> {
                val geocache: Geocache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
                if (geocache != null) {
                    val waypoint: Waypoint = Waypoint(img.getTitle(), WaypointType.WAYPOINT, true)
                    waypoint.setCoords(gp)
                    geocache.addOrChangeWaypoint(waypoint, true)
                    GeocacheChangedBroadcastReceiver.sendBroadcast(activity, geocode)
                }
            })
            ctxMenu.addItem(LocalizationUtils.getString(R.string.cache_menu_navigate), R.drawable.ic_menu_navigate,
                    v -> NavigationAppFactory.showNavigationMenu(activity, null, null, gp))
        }

        val editHandler: EditableCategoryHandler = editableCategoryHandlerMap.get(category)
        if (editHandler != null) {

            val oldImg: Image = adapter.getItem(intentPos).image
            ctxMenu.addItem(LocalizationUtils.getString(R.string.cache_image_rename), R.drawable.ic_menu_edit, v -> SimpleDialog.ofContext(getContext()).setTitle(TextParam.id(R.string.cache_image_rename))
                    .input(SimpleDialog.InputOptions().setInitialValue(oldImg.getTitle()), newTitle -> {
                    if (!StringUtils.isBlank(newTitle)) {
                        val newImg: Image = editHandler.setTitle(oldImg, newTitle)
                        if (newImg != null) {
                            val oldEntry: ImageGalleryEntry = adapter.getItem(intentPos)
                            adapter.updateItem(ImageGalleryEntry(oldEntry.category, newImg), intentPos)
                        }
                    }
                }))

            ctxMenu.addItem(LocalizationUtils.getString(R.string.cache_image_delete), R.drawable.ic_menu_delete, v -> {
                val oldEntry: ImageGalleryEntry = adapter.getItem(intentPos)
                adapter.removeItem(intentPos)
                editHandler.delete(img)
                categorizedImageListHelper.addToCount(oldEntry.category, -1)
                changeImageCount(-1)
            })
        }
        ctxMenu.show()
    }

    public Unit setState(final Bundle state) {
        if (imageHelper != null) {
            imageHelper.setState(state)
        }
    }

    public Bundle getState() {
        return imageHelper == null ? null : imageHelper.getState()
    }

}
