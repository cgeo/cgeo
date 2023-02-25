package cgeo.geocaching.ui;

import cgeo.geocaching.ImageViewActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.apps.navi.NavigationAppFactory;
import cgeo.geocaching.databinding.ImageGalleryCategoryBinding;
import cgeo.geocaching.databinding.ImageGalleryImageBinding;
import cgeo.geocaching.databinding.ImagegalleryViewBinding;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.service.GeocacheChangedBroadcastReceiver;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.dialog.ContextMenuDialog;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter;
import cgeo.geocaching.utils.CategorizedListHelper;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.ImageDataMemoryCache;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.MetadataUtils;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.UriUtils;
import cgeo.geocaching.utils.functions.Action2;
import cgeo.geocaching.utils.functions.Func1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class ImageGalleryView extends LinearLayout {

    private static final int IMAGE_TARGET_WIDTH_DP = 160;
    private static final int IMAGE_TARGET_HEIGHT_DP = 200; // without title textview

    private Context context;
    private Activity activity;
    private ImagegalleryViewBinding binding;
    private ImageGalleryAdapter adapter;

    private final CategorizedListHelper categorizedImageListHelper = new CategorizedListHelper();

    private boolean allowOpenViewer = true;
    private boolean activityReenterCalled = false;

    private ImageActivityHelper imageHelper = null;
    private final ImageDataMemoryCache imageDataMemoryCache = new ImageDataMemoryCache(2);

    private String geocode;
    private int imageCount = 0;

    private Action2<ImageGalleryView, Integer> imageCountChangeCallback = null;

    private final Map<String, Geopoint> imageCoordMap = new HashMap<>();
    private final Map<String, EditableCategoryHandler> editableCategoryHandlerMap = new HashMap<>();

    public interface EditableCategoryHandler {

        Collection<Image> getAllImages();

        Collection<Image> add(Collection<Image> images);

        void delete(Image image);

        Image setTitle(Image image, String title);

    }

    public static class ImageGalleryEntryHolder extends AbstractRecyclerViewHolder {

        public final ImageGalleryImageBinding imageBinding;
        public final ImageGalleryCategoryBinding categoryBinding;

        public ImageGalleryEntry entry;

        public static ImageGalleryEntryHolder createForImage(final ViewGroup view) {
            return new ImageGalleryEntryHolder(ImageGalleryImageBinding.inflate(LayoutInflater.from(view.getContext()), view, false), null);
        }

        public static ImageGalleryEntryHolder createForCategory(final ViewGroup view) {
            return new ImageGalleryEntryHolder(null, ImageGalleryCategoryBinding.inflate(LayoutInflater.from(view.getContext()), view, false));
        }

        private ImageGalleryEntryHolder(final ImageGalleryImageBinding imageBinding, final ImageGalleryCategoryBinding categoryBinding) {
            super(imageBinding == null ? categoryBinding.getRoot() : imageBinding.getRoot());
            this.imageBinding = imageBinding;
            this.categoryBinding = categoryBinding;
        }
    }

    public class ImageGalleryAdapter extends ManagedListAdapter<ImageGalleryEntry, ImageGalleryEntryHolder> {

        protected ImageGalleryAdapter(final RecyclerView recyclerView) {
            super(new Config(recyclerView));
        }

        @NonNull
        @Override
        public ImageGalleryEntryHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            return viewType == 0 ? ImageGalleryEntryHolder.createForCategory(parent) : ImageGalleryEntryHolder.createForImage(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull final ImageGalleryEntryHolder holder, final int position) {
            holder.entry = getItem(position);
            if (holder.imageBinding != null) {
                fillView(holder, holder.imageBinding);
            } else {
                fillCategoryView(holder, holder.categoryBinding);
            }
        }

        @Override
        public int getItemViewType(final int position) {
            return getItem(position).isHeader() ? 0 : 1;
        }

    }



    @Override
    protected void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        recalculateLayout(newConfig);
    }

    private void recalculateLayout(final Configuration newConfig) {

        final GridLayoutManager manager = (GridLayoutManager) binding.imagegalleryList.getLayoutManager();
        final int colCount = Math.max(1, newConfig.screenWidthDp / (IMAGE_TARGET_WIDTH_DP + 10));
        manager.setSpanCount(colCount);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(final int pos) {
                return adapter.getItem(pos).isHeader() ? colCount : 1;
            }
        });
        manager.getSpanSizeLookup().setSpanGroupIndexCacheEnabled(true);
        manager.getSpanSizeLookup().setSpanIndexCacheEnabled(true);
    }

    private void invalidateSpanIndexCaches() {
        final GridLayoutManager manager = (GridLayoutManager) binding.imagegalleryList.getLayoutManager();
        manager.getSpanSizeLookup().invalidateSpanGroupIndexCache();
        manager.getSpanSizeLookup().invalidateSpanIndexCache();
    }

    private static class ImageGalleryEntry {

        public final Image image;
        public final String category;

        ImageGalleryEntry(final String category) {
            this(category, null);
        }

        ImageGalleryEntry(final String category, final Image image) {
            this.image = image;
            this.category = category;
        }

        public boolean isHeader() {
            return image == null;
        }
    }

    public void fillView(final ImageGalleryEntryHolder viewHolder, final ImageGalleryImageBinding binding) {

        final ImageGalleryEntry imgData = viewHolder.entry;
        final Image img = imgData.image;
        setImageTitle(binding, img.getTitle());
        //setImageLayoutSizes(binding, imageSizeDp);
        binding.imageDescriptionMarker.setVisibility(img.hasDescription() ? View.VISIBLE : View.GONE);

        if (!img.isImageOrUnknownUri()) {
            binding.imageTitle.setText(TextUtils.concat(binding.imageTitle.getText(), " (" + UriUtils.getMimeFileExtension(img.getUri()) + ")"));
            binding.imageImage.setImageResource(UriUtils.getMimeTypeIcon(img.getMimeType()));
            binding.imageImage.setRotation(0);
            binding.imageImage.setVisibility(View.VISIBLE);
            binding.imageGeoOverlay.setVisibility(View.GONE);
            binding.imageProgressBar.setVisibility(View.GONE);
        } else {

            imageDataMemoryCache.loadImage(img.getUrl(), p -> {

                final ImageGalleryEntry currentEntry = viewHolder.entry;
                if (!currentEntry.image.getUrl().equals(img.getUrl())) {
                    //this means that meanwhile in the same ViewHolder another image was place
                    return;
                }

                if (p.first == null) {
                    binding.imageImage.setImageDrawable(HtmlImage.getErrorImage(getResources(), true));
                    binding.imageImage.setRotation(0);
                } else {
                    binding.imageImage.setImageDrawable(p.first);
                    binding.imageImage.setRotation(ImageUtils.getImageRotationDegrees(currentEntry.image.getUri()));
                }
                binding.imageImage.setVisibility(View.VISIBLE);

                final Geopoint gp = MetadataUtils.getFirstGeopoint(p.second);

                if (gp != null) {
                    binding.imageGeoOverlay.setVisibility(View.VISIBLE);
                    imageCoordMap.put(img.getUrl(), gp);
                } else {
                    binding.imageGeoOverlay.setVisibility(View.GONE);
                }
                binding.imageProgressBar.setVisibility(View.GONE);
            }, () -> {
                binding.imageProgressBar.setVisibility(View.VISIBLE);
                binding.imageImage.setImageResource(R.drawable.mark_transparent);
                binding.imageImage.setRotation(0);
                binding.imageGeoOverlay.setVisibility(View.GONE);
            });
        }


        binding.imageWrapper.setOnClickListener(v -> openImageDetails(imgData, binding));
        binding.imageWrapper.setOnLongClickListener(v -> {
            showContextOptions(imgData, binding);
            return true;
        });
    }

    private void setImageTitle(final ImageGalleryImageBinding binding, final CharSequence text) {
        if (!StringUtils.isBlank(text)) {
            binding.imageTitle.setText(TextParam.text(text).setHtml(true).getText(getContext()));
            binding.imageTitle.setVisibility(View.VISIBLE);
        } else {
            binding.imageTitle.setVisibility(View.GONE);
        }
    }

    public ImageGalleryView(final Context context) {
        super(context);
        init();
    }

    public ImageGalleryView(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ImageGalleryView(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        this.context = new ContextThemeWrapper(getContext(), R.style.cgeo);
        this.activity = ViewUtils.toActivity(this.context);

        inflate(this.context, R.layout.imagegallery_view, this);
        binding = ImagegalleryViewBinding.bind(this);
        this.adapter = new ImageGalleryAdapter(binding.imagegalleryList);
        binding.imagegalleryList.setAdapter(this.adapter);
        binding.imagegalleryList.setLayoutManager(new GridLayoutManager(this.context, 2));

        recalculateLayout(getContext().getResources().getConfiguration());

        //enable transitions between gallery and image detail activity
        ImageViewActivity.enableViewTransitions(this.activity);
    }

    /** (re)sets contextual information for gallery */
    public void setup(final String geocode) {
        this.geocode = geocode;
        this.imageDataMemoryCache.setCode(this.geocode);
    }

    /** gets total number of images currently displayed in this gallery */
    public int getImageCount() {
        return categorizedImageListHelper.getContentSize();
    }

    /** adds images for display to this gallery */
    public void addImages(final Collection<Image> images) {
        addImages(images, i -> i.category == Image.ImageCategory.UNCATEGORIZED ? null : i.category.getI18n());
    }

    /** adds images for display to this gallery */
    public void addImages(final Collection<Image> images, final Func1<Image, String> categoryMapper) {
        addImagesInternal(images, categoryMapper, false);
    }

    private void addImagesInternal(final Collection<Image> images, final Func1<Image, String> categoryMapper, final boolean force) {

        //order images by category
        final List<String> categories = new ArrayList<>();
        final Map<String, List<ImageGalleryEntry>> catImages = new HashMap<>();
        for (Image img : images) {
            final String category = categoryMapper.call(img);
            if (!force && editableCategoryHandlerMap.containsKey(category)) {
                continue;
            }
            if (!catImages.containsKey(category)) {
                categories.add(category);
                catImages.put(category, new ArrayList<>());
            }
            catImages.get(category).add(new ImageGalleryEntry(category, img));
        }

        //add to gallery category-wise
        for (String category : categories) {

            if (!categorizedImageListHelper.containsCategory(category)) {
                createCategory(category, false);
            }
            adapter.addItems(categorizedImageListHelper.getCategoryInsertPosition(category), catImages.get(category));
            categorizedImageListHelper.addToCount(category, catImages.get(category).size());
        }

        //invalidate caches
        invalidateSpanIndexCaches();
    }

    private void changeImageCount(final int by) {
        if (by != 0) {
            imageCount += by;
            if (imageCountChangeCallback != null) {
                imageCountChangeCallback.call(this, imageCount);
            }
        }
    }

    private void createCategory(final String category, final boolean atStart) {

        if (!categorizedImageListHelper.containsCategory(category)) {
            categorizedImageListHelper.addOrMoveCategory(category, atStart);
            adapter.addItem(atStart ? 0 : adapter.getItemCount(), new ImageGalleryEntry(category));
        }
    }

    private void fillCategoryView(final ImageGalleryEntryHolder entry, final ImageGalleryCategoryBinding binding) {

        final String category = entry.entry.category;
        if (StringUtils.isBlank(category)) {
            binding.imgGalleryCategoryTitle.setVisibility(View.GONE);
        } else {
            binding.imgGalleryCategoryTitle.setVisibility(View.VISIBLE);
            binding.imgGalleryCategoryTitle.setText(category);
        }

        final boolean isEditableCat = this.editableCategoryHandlerMap.containsKey(category);
        binding.imgGalleryAddButtons.setVisibility(isEditableCat ? View.VISIBLE : View.GONE);
        binding.imgGalleryAddMultiImages.setOnClickListener(v -> imageHelper.getMultipleImagesFromStorage(geocode, false, category));
        binding.imgGalleryAddCamera.setOnClickListener(v -> imageHelper.getImageFromCamera(geocode, false, category));
        binding.imgGalleryAddMultiFiles.setOnClickListener(v -> imageHelper.getMultipleFilesFromStorage(geocode, false, category));

    }

    /** adds an editable category to this gallery. This is a category where user has options to add/rename/remove images */
    public void setEditableCategory(final String category, final EditableCategoryHandler handler) {
        if (this.activity == null) {
            //not supported if not in context of an activity
            return;
        }

        removeCategory(category);
        this.editableCategoryHandlerMap.put(category, handler);
        createCategory(category, true);

        //fill initially
        addImagesInternal(handler.getAllImages(), i -> category, true);

        if (imageHelper == null) {
            imageHelper = new ImageActivityHelper(activity, (r, imgUris, uk) -> {
                final Collection<Image> imgs = CollectionStream.of(imgUris).map(uri -> new Image.Builder().setUrl(uri).build()).toList();
                final Collection<Image> addedImgs = editableCategoryHandlerMap.get(uk).add(imgs);
                if (r == ImageActivityHelper.REQUEST_CODE_CAMERA) {
                    //change title of camera photos
                    final Collection<Image> copy = new ArrayList<>(addedImgs);
                    addedImgs.clear();
                    int idx = categorizedImageListHelper.getCategoryCount(uk) + 1;
                    for (Image i : copy) {
                        final Image newImage = editableCategoryHandlerMap.get(uk).setTitle(i, LocalizationUtils.getString(R.string.cache_image_title_camera_prefix) + " " + idx);
                        if (newImage == null) {
                            addedImgs.add(i);
                        } else {
                            addedImgs.add(newImage);
                            idx++;
                        }
                    }
                }

                addImagesInternal(addedImgs, i -> uk, true);
            });
        }
    }

    /** Important: include this method in your activity to support activity-related tasks. Necessary for e.g. editable categories */
    public boolean onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        allowOpenViewer();
        //in some situations "onActivityReenter" is not called. In these cases -> initialize position here
        if (!activityReenterCalled) {
            onActivityReenter(activity, this, data);
        }
        activityReenterCalled = false;
        if (imageHelper != null) {
            return imageHelper.onActivityResult(requestCode, resultCode, data);
        }
        return false;
    }

    /** registers a callback which is triggered whenever the count of images displayed in gallery changes */
    public void setImageCountChangeCallback(final Action2<ImageGalleryView, Integer> callback) {
        this.imageCountChangeCallback = callback;
    }

    /** scrolls the image gallery to the list index given */
    private void scrollTo(final int listIndex) {
        final int realIndex = Math.max(0, Math.min(adapter.getItemCount() - 1, listIndex));
        final RecyclerView recyclerView = binding.imagegalleryList;
        final GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
        //check if item is already (completely) visible --> then don't scroll
        final View viewAtPosition = layoutManager.findViewByPosition(realIndex);
        if (viewAtPosition == null || layoutManager
                .isViewPartiallyVisible(viewAtPosition, false, true)) {

            //view is either not or only partially visible --> scroll

            int offset = 50; //default
            //try to calculate an offset such that the image to scroll to ends up in the middle of the gallery view
            final int imageHeight = ViewUtils.dpToPixel(IMAGE_TARGET_HEIGHT_DP);
            final int height = this.getHeight();
            if (height >= imageHeight) {
                offset = (height - imageHeight) / 2;
            }

            //use "scrollToPositionWithOffset" since "scrollToPosition" behaves strange -> see issue #13586
            layoutManager.scrollToPositionWithOffset(realIndex, offset);
        }
    }

    /** returns the image view for given image index (or null if index is invalid) */
    @Nullable
    public View getImageViewForContentIndex(final int contentIndex) {
        final int pos = categorizedImageListHelper.getListIndexForContentIndex(contentIndex);
        if (pos == -1) {
            return null;
        }
        final ImageGalleryEntryHolder vh = (ImageGalleryEntryHolder) binding.imagegalleryList.findViewHolderForAdapterPosition(pos);
        if (vh == null || vh.imageBinding == null) {
            return null;
        }
        return vh.imageBinding.imageImage;
    }

    /** clears this gallery */
    public void clear() {
        this.imageDataMemoryCache.clear();
        this.adapter.clearList();
        this.categorizedImageListHelper.clear();
        this.imageCount = 0;
    }

    /** registers the activity associated with this gallery for image transtions towards image detail view */
    public void registerCallerActivity() {
        ImageViewActivity.registerCallerActivity(this.activity, this::getImageViewForContentIndex);
    }

    public void removeCategory(final String category) {
        if (!categorizedImageListHelper.containsCategory(category)) {
            return;
        }

        changeImageCount(-categorizedImageListHelper.getCategoryCount(category));
        final int titlePos = categorizedImageListHelper.getCategoryTitlePosition(category);
        for (int pos = categorizedImageListHelper.getCategoryInsertPosition(category) - 1; pos >= titlePos; pos--) {
            adapter.removeItem(pos);
        }
        categorizedImageListHelper.removeCategory(category);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        //clear image cache
        this.imageDataMemoryCache.clear();
    }

    /** Important: include this in your activity's "onActivityReenter" method.
     * Needed e.g. for correct back transition when returning from detail view (e.g. scrolling to image gallery position) */
    public static int onActivityReenter(final Activity activity, final ImageGalleryView imageGallery, final Intent data) {
        final int pos = data == null || data.getExtras() == null ? -1 : data.getExtras().getInt(ImageViewActivity.EXTRA_IMAGEVIEW_POS, -1);
        if (pos >= 0) {
            activity.postponeEnterTransition();
            if (imageGallery != null) {
                imageGallery.initializeToPosition(pos);
                imageGallery.activityReenterCalled = true;
            }
        }
        return pos;
    }

    /** initializes gallery to a certain position.
     * Use this method directly for gallery recreation if gallery was not available in activity's "onActivityReender" method */
    public void initializeToPosition(final int contentIndex) {
        final int realPos = Math.max(-1, Math.min(adapter.getItemCount() - 1, contentIndex));
        this.allowOpenViewer();
        if (realPos >= 0) {
            this.scrollTo(categorizedImageListHelper.getListIndexForContentIndex(realPos));
        }
        this.post(() -> activity.startPostponedEnterTransition());
    }

    /** allows the gallery to open a image detail view (again) */
    private void allowOpenViewer() {
        this.allowOpenViewer = true;
    }

    private void openImageDetails(final ImageGalleryEntry imgData, final ImageGalleryImageBinding binding) {
        if (imgData.image.isImageOrUnknownUri()) {
            openImageViewer(binding);
        } else {
            ShareUtils.openContentForView(getContext(), imgData.image.getUrl());
        }
    }

    private void openImageViewer(final ImageGalleryImageBinding binding) {
        if (activity == null || !allowOpenViewer) {
            return;
        }
        allowOpenViewer = false;

        final int intentPos = this.binding.imagegalleryList.getChildAdapterPosition(binding.getRoot());
        final int contentPos = categorizedImageListHelper.getContentIndexForListIndex(intentPos);

        final List<Image> images = new ArrayList<>();
        for (ImageGalleryEntry item : adapter.getItems()) {
            if (!item.isHeader()) {
                images.add(item.image);
            }
        }

        ImageViewActivity.openImageView(this.activity, geocode, images, contentPos,
                this::getImageViewForContentIndex);
    }

    // splitting up that method would not help improve readability
    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
    private void showContextOptions(final ImageGalleryEntry imgData, final ImageGalleryImageBinding binding) {
        if (activity == null || imgData == null || imgData.image == null) {
            return;
        }
        final String category = imgData.category;
        final Image img = imgData.image;

        final int intentPos = this.binding.imagegalleryList.getChildAdapterPosition(binding.getRoot());

        final ContextMenuDialog ctxMenu = new ContextMenuDialog(activity);
        ctxMenu.setTitle(LocalizationUtils.getString(R.string.cache_image));

        ctxMenu.addItem(LocalizationUtils.getString(R.string.cache_image_open), R.drawable.ic_menu_image,
                v -> openImageDetails(imgData, binding));

        ctxMenu.addItem(LocalizationUtils.getString(R.string.cache_image_open_file), R.drawable.ic_menu_image,
                v -> ImageUtils.viewImageInStandardApp(activity, img.getUri(), geocode));

        if (!UriUtils.isFileUri(img.getUri()) && !UriUtils.isContentUri(img.getUri())) {
            ctxMenu.addItem(LocalizationUtils.getString(R.string.cache_image_open_browser), R.drawable.ic_menu_open_in_browser,
                    v -> ShareUtils.openUrl(context, img.getUrl(), false));
        }
        if (img.hasDescription()) {
            ctxMenu.addItem(LocalizationUtils.getString(R.string.cache_image_show_description), R.drawable.ic_menu_hint,
                    v -> SimpleDialog.ofContext(getContext()).setTitle(R.string.log_image_description)
                            .setMessage(TextParam.text(img.getDescription()).setHtml(true)).show());
        }
        final Geopoint gp = imageCoordMap.get(img.getUrl());
        if (gp != null && geocode != null) {
            ctxMenu.addItem(LocalizationUtils.getString(R.string.cache_image_add_waypoint), R.drawable.ic_menu_myposition, v -> {
                final Geocache geocache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
                if (geocache != null) {
                    final Waypoint waypoint = new Waypoint(img.getTitle(), WaypointType.WAYPOINT, true);
                    waypoint.setCoords(gp);
                    geocache.addOrChangeWaypoint(waypoint, true);
                    GeocacheChangedBroadcastReceiver.sendBroadcast(activity, geocode);
                }
            });
            ctxMenu.addItem(LocalizationUtils.getString(R.string.cache_menu_navigate), R.drawable.ic_menu_navigate,
                    v -> NavigationAppFactory.showNavigationMenu(activity, null, null, gp));
        }

        final EditableCategoryHandler editHandler = editableCategoryHandlerMap.get(category);
        if (editHandler != null) {

            final Image oldImg = adapter.getItem(intentPos).image;
            ctxMenu.addItem(LocalizationUtils.getString(R.string.cache_image_rename), R.drawable.ic_menu_edit, v -> SimpleDialog.ofContext(getContext()).setTitle(TextParam.id(R.string.cache_image_rename))
                .input(-1, oldImg.getTitle(), null, null, newTitle -> {
                    if (!StringUtils.isBlank(newTitle)) {
                        final Image newImg = editHandler.setTitle(oldImg, newTitle);
                        if (newImg != null) {
                            final ImageGalleryEntry oldEntry = adapter.getItem(intentPos);
                            adapter.updateItem(new ImageGalleryEntry(oldEntry.category, newImg), intentPos);
                        }
                    }
                }));

            ctxMenu.addItem(LocalizationUtils.getString(R.string.cache_image_delete), R.drawable.ic_menu_delete, v -> {
                final ImageGalleryEntry oldEntry = adapter.getItem(intentPos);
                adapter.removeItem(intentPos);
                editHandler.delete(img);
                categorizedImageListHelper.addToCount(oldEntry.category, -1);
                changeImageCount(-1);
            });
        }
        ctxMenu.show();
    }

}
