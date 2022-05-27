package cgeo.geocaching.ui;

import cgeo.geocaching.ImageViewActivity;
import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.apps.navi.NavigationAppFactory;
import cgeo.geocaching.databinding.ImageGalleryCategoryBinding;
import cgeo.geocaching.databinding.ImageGalleryImageBinding;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.dialog.ContextMenuDialog;
import cgeo.geocaching.ui.dialog.SimpleDialog;
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
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.gridlayout.widget.GridLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

public class ImageGalleryView extends LinearLayout {

    private static final int IMAGE_TARGET_SIZE_DB = 160;
    private static final int IMAGE_SPACE_DB = 10;

    private Context context;
    private Activity activity;

    private ImageActivityHelper imageHelper = null;
    private final ImageDataMemoryCache imageDataMemoryCache = new ImageDataMemoryCache(2);

    private String geocode;
    private int imageCount = 0;

    private int imageSizeDp = 150;
    private int columnCount = 2;
    private int categoryHorizontalMargin = 2;

    private Action2<ImageGalleryView, Integer> imageCountChangeCallback = null;

    private final Map<String, Geopoint> imageCoordMap = new HashMap<>();
    private final List<String> categoryList = new ArrayList<>();
    private final Map<String, ImageCategoryData> categoryDataMap = new HashMap<>();
    private final Map<String, EditableCategoryHandler> editableCategoryHandlerMap = new HashMap<>();

    public interface EditableCategoryHandler {

        Collection<Image> getAllImages();

        Collection<Image> add(Collection<Image> images);

        void delete(Image image);

    }

    @Override
    protected void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        recalculateLayout(newConfig);
    }

    private void recalculateLayout(final Configuration newConfig) {

        this.imageSizeDp = IMAGE_TARGET_SIZE_DB;
        this.columnCount = Math.max(1, newConfig.screenWidthDp / (this.imageSizeDp + 10));

        //provide at least 2 columns
        if (this.columnCount == 1 && newConfig.screenWidthDp > 100) {
            this.columnCount = 2;
            this.imageSizeDp = (newConfig.screenWidthDp / 2) - 10;
        }
        this.categoryHorizontalMargin = Math.max(0,
                (newConfig.screenWidthDp - this.columnCount * (this.imageSizeDp + IMAGE_SPACE_DB)) / 2 - 10);

        //apply new layout
        for (ImageCategoryData categoryData : this.categoryDataMap.values()) {
            setListLayout(categoryData);
        }
    }

    private void setListLayout(final ImageCategoryData categoryData) {
        final MarginLayoutParams mlp = (MarginLayoutParams) categoryData.view.imageGalleryList.getLayoutParams();
        final int mh = ViewUtils.dpToPixel(this.categoryHorizontalMargin);
        mlp.setMargins(mh, 0, mh, 0);

        final int colCountFinal = columnCount;
        categoryData.view.imageGalleryList.setOrientation(GridLayout.HORIZONTAL);
        setGridColumns(categoryData.view.imageGalleryList, colCountFinal);
    }

    private void setImageLayoutSizes(final ImageGalleryImageBinding image, final int imageSizeInDp) {
        final int imageSizeInner = imageSizeInDp - 10;
        setLayoutSize(image.imageComplete, imageSizeInDp, -1);
        setLayoutSize(image.imageWrapper, imageSizeInner, imageSizeInner);
        setLayoutSize(image.imageImage, imageSizeInner, imageSizeInner);
        setLayoutSize(image.imageTitle, imageSizeInner, -1);

        final MarginLayoutParams mlp = (MarginLayoutParams) image.imageComplete.getLayoutParams();
        final int mh = ViewUtils.dpToPixel(IMAGE_SPACE_DB) / 2;
        mlp.setMargins(0, mh, 0, mh);

    }

    private static void setLayoutSize(final View view, final int widthInDp, final int heightInDp) {
        final ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (widthInDp > 0) {
            lp.width = ViewUtils.dpToPixel(widthInDp);
        }
        if (heightInDp > 0) {
            lp.height = ViewUtils.dpToPixel(heightInDp);
        }
    }

    private static class ImageData {

        public final Image image;
        public final String category;

        ImageData(final String category, final Image image) {
            this.image = image;
            this.category = category;
        }
    }

    private static class ImageCategoryData {

        public final List<ImageData> imageList = new ArrayList<>();
        public final ImageGalleryCategoryBinding view;

        ImageCategoryData(final ImageGalleryCategoryBinding view) {
            this.view = view;
        }
    }

    private static class ImageViewHolder extends RecyclerView.ViewHolder {

        public final ImageGalleryImageBinding imageBinding;

        ImageViewHolder(@NonNull final View itemView) {
            super(itemView);
            imageBinding = ImageGalleryImageBinding.bind(itemView);
        }
    }

    public void fillView(final ImageData imgData, final ImageGalleryImageBinding binding, final int position) {

        final Image img = imgData.image;
        if (!StringUtils.isBlank(img.getTitle())) {
            binding.imageTitle.setText(TextParam.text(img.getTitle()).setHtml(true).getText(getContext()));
            binding.imageTitle.setVisibility(View.VISIBLE);
        } else {
            binding.imageTitle.setVisibility(View.GONE);
        }
        setImageLayoutSizes(binding, imageSizeDp);
        binding.imageDescriptionMarker.setVisibility(img.hasDescription() ? View.VISIBLE : View.GONE);

        imageDataMemoryCache.loadImage(img.getUrl(), p -> {
            final BitmapDrawable bd = p.first;

            final Geopoint gp = MetadataUtils.getFirstGeopoint(p.second);

            binding.imageImage.setImageDrawable(bd);
            binding.imageImage.setVisibility(View.VISIBLE);
            if (gp != null) {
                binding.imageGeoOverlay.setVisibility(View.VISIBLE);
                imageCoordMap.put(img.getUrl(), gp);
            } else {
                binding.imageGeoOverlay.setVisibility(View.GONE);
            }
            binding.imageProgressBar.setVisibility(View.GONE);
        }, () -> {
            binding.imageProgressBar.setVisibility(View.VISIBLE);
            binding.imageImage.setVisibility(View.GONE);
            binding.imageGeoOverlay.setVisibility(View.GONE);
        });

        binding.imageImage.setOnClickListener(v -> {
            openImageViewer(imgData.category, position);
        });
        binding.imageImage.setOnLongClickListener(v -> {
            showContextOptions(img, imgData.category, position);
            return true;
        });
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
        recalculateLayout(getContext().getResources().getConfiguration());

        //enable transitions between gallery and image detail activity
        ImageViewActivity.enableViewTransitions(this.activity);
    }

    public void setup(final String geocode) {
        this.geocode = geocode;
        this.imageDataMemoryCache.setCode(this.geocode);
    }

    public int getCount() {
        return imageCount;
    }

    public void addImages(final Collection<Image> images) {
        addImages(images, i -> i.category == Image.ImageCategory.UNCATEGORIZED ? null : i.category.getI18n());
    }

    public void addImages(final Collection<Image> images, final Func1<Image, String> categoryMapper) {
        addImagesInternal(images, categoryMapper, false);
    }

    private void addImagesInternal(final Collection<Image> images, final Func1<Image, String> categoryMapper, final boolean force) {


        final Map<String, List<ImageData>> catImages = new HashMap<>();
        for (Image img : images) {
            final String category = categoryMapper == null ? null : categoryMapper.call(img);

            //can't add images from editable categories via this method
            if (!force && editableCategoryHandlerMap.containsKey(category)) {
                continue;
            }

            if (!catImages.containsKey(category)) {
                catImages.put(category, new ArrayList<>());
            }
            catImages.get(category).add(new ImageData(category, img));
        }

        int changeCount = 0;

        for (Map.Entry<String, List<ImageData>> catImage : catImages.entrySet()) {
            if (!categoryDataMap.containsKey(catImage.getKey())) {
                createCategory(catImage.getKey());
            }

            final ImageCategoryData catData = categoryDataMap.get(catImage.getKey());
            final GridLayout gl = catData.view.imageGalleryList;
            for (ImageData ild : catImage.getValue()) {
                final View view = LayoutInflater.from(gl.getContext()).inflate(R.layout.image_gallery_image, gl, false);
                fillView(ild, ImageGalleryImageBinding.bind(view), gl.getChildCount());
                gl.addView(view);
                catData.imageList.add(ild);
            }
            changeCount += catImage.getValue().size();
            setListLayout(catData);
        }
        changeImageCount(changeCount);
    }

    private void changeImageCount(final int by) {
        setTransactionNames();
        if (by != 0) {
            imageCount += by;
            if (imageCountChangeCallback != null) {
                imageCountChangeCallback.call(this, imageCount);
            }
        }
    }

    private void createCategory(final String category) {
        if (this.categoryDataMap.containsKey(category)) {
            return;
        }

        final ImageGalleryCategoryBinding binding = ImageGalleryCategoryBinding.inflate(LayoutInflater.from(this.context), this, false);
        if (category == null) {
            binding.imgGalleryCategoryTitle.setVisibility(View.GONE);
        } else {
            binding.imgGalleryCategoryTitle.setVisibility(View.VISIBLE);
            binding.imgGalleryCategoryTitle.setText(category);
        }

        final boolean isEditableCat = this.editableCategoryHandlerMap.containsKey(category);
        binding.imgGalleryAddButtons.setVisibility(isEditableCat ? View.VISIBLE : View.GONE);
        binding.imgGalleryAddCamera.setOnClickListener(v -> imageHelper.getImageFromCamera(geocode, false, category));
        binding.imgGalleryAddMulti.setOnClickListener(v -> imageHelper.getMultipleImagesFromStorage(geocode, false, category));

        this.addView(binding.getRoot(), category == null ? 0 : this.getChildCount(), new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        this.categoryList.add(category == null ? 0 : this.categoryList.size(), category);
        this.categoryDataMap.put(category, new ImageCategoryData(binding));
        setListLayout(this.categoryDataMap.get(category));
    }

    public void setEditableCategory(final String category, final EditableCategoryHandler handler) {
        if (this.activity == null) {
            //not supported if not in context of an activity
            return;
        }
        removeCategory(category);
        this.editableCategoryHandlerMap.put(category, handler);
        createCategory(category);

        //fill initially
        addImagesInternal(handler.getAllImages(), i -> category, true);

        if (imageHelper == null) {
            imageHelper = new ImageActivityHelper(activity, (r, imgUris, uk) -> {
                final Collection<Image> imgs = CollectionStream.of(imgUris).map(uri -> new Image.Builder().setUrl(uri).build()).toList();
                final Collection<Image> addedImgs = editableCategoryHandlerMap.get(uk).add(imgs);
                addImagesInternal(addedImgs, i -> uk, true);
            });
        }
    }

    /**
     * include this method in your activity to support activity-related tasks
     */
    public boolean onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        return imageHelper.onActivityResult(requestCode, resultCode, data);
    }

    public void setImageCountChangeCallback(final Action2<ImageGalleryView, Integer> callback) {
        this.imageCountChangeCallback = callback;
    }

    public View getImageViewForIndex(final int index, final boolean scrollToIndex) {
        if (index < 0) {
            return null;
        }

        int pos = 0;
        String category = null;
        for (String cat : categoryList) {
            category = cat;
            final GridLayout adapter = Objects.requireNonNull(categoryDataMap.get(cat).view.imageGalleryList);
            if (pos + adapter.getChildCount() > index) {
                break;
            }
            pos += adapter.getChildCount();
        }
        final int idx = index - pos;
        final GridLayout adapter = categoryDataMap.get(category).view.imageGalleryList;
        if (category == null || adapter == null || idx < 0 || idx >= adapter.getChildCount()) {
            return null;
        }

        final GridLayout recyclerView = categoryDataMap.get(category).view.imageGalleryList;
        final View view = recyclerView.getChildAt(idx);

        if (scrollToIndex && view != null) {
            //scroll view into visibility
            final Rect rect = new Rect(0, -100, view.getWidth(), view.getHeight() + 100);
            view.requestRectangleOnScreen(rect, true);
        }

        return view == null ? null : view.findViewById(R.id.image_image);
    }

    public void clear() {
        this.imageDataMemoryCache.clear();
        this.removeAllViews();
        this.categoryDataMap.clear();
        this.categoryList.clear();
        this.imageCount = 0;
    }

    public void registerActivity() {

        ImageViewActivity.registerActivity(this.activity, p -> getImageViewForIndex(p, true));
    }

    public void removeCategory(final String category) {
        if (!categoryDataMap.containsKey(category)) {
            return;
        }

        changeImageCount(-categoryDataMap.get(category).view.imageGalleryList.getChildCount());

        for (int i = 0; i < getChildCount(); i++) {
            final ImageGalleryCategoryBinding view = ImageGalleryCategoryBinding.bind(getChildAt(i));
            if (Objects.equals(category, view.imgGalleryCategoryTitle.getText().toString())) {
                removeView(view.getRoot());
                break;
            }
        }
        categoryDataMap.remove(category);
        categoryList.remove(category);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        //clear image cache
        this.imageDataMemoryCache.clear();
    }

    private void openImageViewer(final String category, final int pos) {
        if (activity == null) {
            return;
        }
        final List<Image> images = new ArrayList<>();
        int intentPos = -1;
        for (String cat : categoryList) {
            int idx = 0;
            for (ImageData imgData : categoryDataMap.get(cat).imageList) {
                if (Objects.equals(category, cat) && pos == idx) {
                    intentPos = images.size();
                }
                images.add(imgData.image);
                idx++;
            }
        }
        ImageViewActivity.openImageView(this.activity, geocode, images, intentPos,
                p -> getImageViewForIndex(p, true));
    }

    private void showContextOptions(final Image img, final String category, final int pos) {
        if (activity == null || img == null) {
            return;
        }
        final ContextMenuDialog ctxMenu = new ContextMenuDialog(activity);
        ctxMenu.setTitle(LocalizationUtils.getString(R.string.cache_image));

        ctxMenu.addItem("Open", R.drawable.ic_menu_image,
                v -> openImageViewer(category, pos));

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
                    final Intent intent = new Intent(Intents.INTENT_CACHE_CHANGED);
                    intent.putExtra(Intents.EXTRA_WPT_PAGE_UPDATE, true);
                    LocalBroadcastManager.getInstance(activity).sendBroadcast(intent);
                }
            });
            ctxMenu.addItem(LocalizationUtils.getString(R.string.cache_menu_navigate), R.drawable.ic_menu_navigate,
                    v -> NavigationAppFactory.showNavigationMenu(activity, null, null, gp));
        }

        final EditableCategoryHandler editHandler = editableCategoryHandlerMap.get(category);
        if (editHandler != null) {
            ctxMenu.addItem(LocalizationUtils.getString(R.string.log_image_delete), R.drawable.ic_menu_delete, v -> {
                categoryDataMap.get(category).imageList.remove(pos);
                editHandler.delete(img);
                changeImageCount(-1);
            });
        }
        ctxMenu.show();
    }

    private static void setGridColumns(final GridLayout gridLayout, final int columns) {
        int col = 0;
        int row = 0;
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            final View child = gridLayout.getChildAt(i);
            final GridLayout.LayoutParams params = (GridLayout.LayoutParams) child.getLayoutParams();
            params.columnSpec = GridLayout.spec(col, 1, 1f);
            params.rowSpec = GridLayout.spec(row, 1, 1f);
            if (col + 1 == columns) {
                col = 0;
                row++;
            } else {
                col++;
            }
        }
        gridLayout.requestLayout(); // Forces maxIndex to be recalculated when columnCount is set
        gridLayout.setColumnCount(columns);
    }

    private void setTransactionNames() {
        int idx = 0;
        for (String category : categoryList) {
            final GridLayout gl = categoryDataMap.get(category).view.imageGalleryList;
            for (int c = 0; c < gl.getChildCount(); c++) {
                final ImageView iv = gl.getChildAt(c).findViewById(R.id.image_image);
                iv.setTransitionName(ImageViewActivity.getTransitionNameForIndex(idx));
                idx++;
            }
        }
    }

}
