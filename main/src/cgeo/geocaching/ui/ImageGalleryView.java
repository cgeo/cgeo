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
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.Folder;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.dialog.ContextMenuDialog;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.ImageDataMemoryCache;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.MetadataUtils;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.UriUtils;
import cgeo.geocaching.utils.functions.Func1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
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

    private int imageSizeDp = 150;
    private int columnCount = 2;
    private int categoryHorizontalMargin = 2;

    private final Map<String, Geopoint> imageCoordMap = new HashMap<>();
    private final List<String> categoryList = new ArrayList<>();
    private final Map<String, ImageGalleryCategoryBinding> categoryViews = new HashMap<>();
    private final Map<String, ImageListAdapter> categoryAdapters = new HashMap<>();
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
        for (ImageGalleryCategoryBinding categoryView : this.categoryViews.values()) {
            setListLayout(categoryView);
        }
    }

    private void setListLayout(final ImageGalleryCategoryBinding categoryView) {
        final MarginLayoutParams mlp = (MarginLayoutParams) categoryView.imageGalleryList.getLayoutParams();
        final int mh = ViewUtils.dpToPixel(this.categoryHorizontalMargin);
        mlp.setMargins(mh, 0, mh, 0);

        final int colCountFinal = columnCount;
        final GridLayoutManager lm = new GridLayoutManager(categoryView.imageGalleryList.getContext(), colCountFinal);
        categoryView.imageGalleryList.setLayoutManager(lm);
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


    public static class FolderCategoryHandler implements EditableCategoryHandler {

        private final Folder folder;

        public FolderCategoryHandler(final String geocode) {
            final String suffix = StringUtils.right(geocode, 2);
            folder = Folder.fromFolder(PersistableFolder.SPOILER_IMAGES.getFolder(),
                suffix.substring(1) + "/" + suffix.charAt(0)  + "/" + geocode);
        }

        @Override
        public Collection<Image> getAllImages() {
            return CollectionStream.of(ContentStorage.get().list(folder))
                .map(fi -> new Image.Builder().setUrl(fi.uri).setTitle(getTitleFromName(fi.name)).build()).toList();
        }

        @Override
        public Collection<Image> add(final Collection<Image> images) {
            final Collection<Image> resultCollection = new ArrayList<>();
            for (Image img : images) {
                final String title = getTitleFromName(ContentStorage.get().getName(img.getUri()));
                final Uri newUri = ContentStorage.get().copy(img.getUri(), folder, null, false);
                resultCollection.add(img.buildUpon().setUrl(newUri).setTitle(title).build());
            }
            return resultCollection;
        }

        @Override
        public void delete(final Image image) {
            ContentStorage.get().delete(image.uri);
        }

        private String getTitleFromName(final String filename) {
            String title = filename == null ? "-" : filename;
            final int idx = title.lastIndexOf(".");
            if (idx > 0) {
                title = title.substring(0, idx);
            }
            return title;

        }
    }

    private static class ImageListData {

        public final Image image;
        public final String category;

        ImageListData(final String category, final Image image) {
            this.image = image;
            this.category = category;
        }

    }

    private static class ImageViewHolder extends RecyclerView.ViewHolder {

        public final ImageGalleryImageBinding imageBinding;

        ImageViewHolder(@NonNull final View itemView) {
            super(itemView);
            imageBinding = ImageGalleryImageBinding.bind(itemView);
        }
    }

    private class ImageListAdapter extends ManagedListAdapter<ImageListData, ImageViewHolder> {

        protected ImageListAdapter(final RecyclerView view) {
            super(new ManagedListAdapter.Config(view).setNotifyOnEvents(true));
        }

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_gallery_image, parent, false);
            return new ImageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final ImageViewHolder holder, final int position) {

            final Image img = getItem(position).image;
            final ImageGalleryImageBinding binding = holder.imageBinding;
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

                //check if request is still valid
                final ImageListData ild = getItem(holder.getBindingAdapterPosition());
                if (ild == null || ild.image == null || !ild.image.getUrl().equals(img.getUrl())) {
                    return;
                }
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
                final ImageListData ild = getItem(holder.getBindingAdapterPosition());
                openImageViewer(ild.category, holder.getBindingAdapterPosition());
            });
            binding.imageImage.setOnLongClickListener(v -> {
                final ImageListData ild = getItem(holder.getBindingAdapterPosition());
                showContextOptions(ild.image, ild.category, holder.getBindingAdapterPosition(),  this);
                return true;
            });
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
        recalculateLayout(getContext().getResources().getConfiguration());
     }

    public void setup(final String geocode) {
        this.geocode = geocode;
        this.imageDataMemoryCache.setCode(this.geocode);
    }

    public void addImages(final Collection<Image> images) {
        addImages(images, i -> i.category == Image.ImageCategory.UNCATEGORIZED ? null : i.category.getI18n());
    }

    public void addImages(final Collection<Image> images, final Func1<Image, String> categoryMapper) {
        addImagesInternal(images, categoryMapper, false);
    }

    private void addImagesInternal(final Collection<Image> images, final Func1<Image, String> categoryMapper, final boolean force) {


        final Map<String, List<ImageListData>> catImages = new HashMap<>();
        for (Image img : images) {
            final String category = categoryMapper == null ? null : categoryMapper.call(img);

            //can't add images from editable categories via this method
            if (!force && editableCategoryHandlerMap.containsKey(category)) {
                continue;
            }

            if (!catImages.containsKey(category)) {
                catImages.put(category, new ArrayList<>());
            }
            catImages.get(category).add(new ImageListData(category, img));
        }

        for (Map.Entry<String, List<ImageListData>> catImage : catImages.entrySet()) {
            if (!categoryViews.containsKey(catImage.getKey())) {
                createCategory(catImage.getKey());
            }
            final ImageListAdapter adapter = categoryAdapters.get(catImage.getKey());
            adapter.addItems(adapter.getItemCount(), catImage.getValue());
        }
    }

    private void createCategory(final String category) {
        if (this.categoryViews.containsKey(category)) {
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
        this.categoryViews.put(category, binding);
        final ImageListAdapter adapter = new ImageListAdapter(binding.imageGalleryList);
        this.categoryAdapters.put(category, adapter);
        setListLayout(binding);
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

    /** include this method in your activity to support activity-related tasks */
    public boolean onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        return imageHelper.onActivityResult(requestCode, resultCode, data);
    }

    public void clear() {
        this.imageDataMemoryCache.clear();
        this.removeAllViews();
        this.categoryAdapters.clear();
        this.categoryViews.clear();
        this.categoryList.clear();
    }

    public void removeCategory(final String category) {
        if (!categoryViews.containsKey(category)) {
            return;
        }

        for (int i = 0; i < getChildCount(); i++) {
            final ImageGalleryCategoryBinding view = ImageGalleryCategoryBinding.bind(getChildAt(i));
            if (Objects.equals(category, view.imgGalleryCategoryTitle.getText().toString())) {
                removeView(view.getRoot());
                break;
            }
        }
        categoryViews.remove(category);
        categoryList.remove(category);
        categoryAdapters.remove(category);
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
            for (ImageListData imgData : categoryAdapters.get(cat).getItems()) {
                if (Objects.equals(category, cat) && pos == idx) {
                    intentPos = images.size();
                }
                images.add(imgData.image);
                idx++;
            }
        }
        ImageViewActivity.openImageView(this.activity, geocode, images, intentPos);
    }

    private void viewInStandardApp(final Image img) {
        if (activity == null) {
            return;
        }

        Uri imgUri = img.getUri();
        if (!UriUtils.isFileUri(imgUri)) {
            final File file = LocalStorage.getGeocacheDataFile(geocode, img.getUri().toString(), true, true);
            if (file.exists()) {
                imgUri = Uri.fromFile(file);
            }
        }

        ImageUtils.viewImageInStandardApp(activity, imgUri);
    }

    private void showContextOptions(final Image img, final String category, final int pos,  final ImageListAdapter adapter) {
        if (activity == null) {
            return;
        }
        final ContextMenuDialog ctxMenu = new ContextMenuDialog(activity);
        ctxMenu.setTitle(LocalizationUtils.getString(R.string.cache_image));

        ctxMenu.addItem("Open", R.drawable.ic_menu_info_details,
            v -> openImageViewer(category, pos));

        ctxMenu.addItem(LocalizationUtils.getString(R.string.cache_image_open_file), R.drawable.ic_menu_info_details,
            v -> viewInStandardApp(img));

        if (!UriUtils.isFileUri(img.getUri()) && !UriUtils.isContentUri(img.getUri())) {
            ctxMenu.addItem(LocalizationUtils.getString(R.string.cache_image_open_browser), R.drawable.ic_menu_share,
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
                adapter.removeItem(pos);
                editHandler.delete(img);
            });
        }
        ctxMenu.show();
    }

}
