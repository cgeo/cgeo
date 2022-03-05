package cgeo.geocaching.ui;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.apps.navi.NavigationAppFactory;
import cgeo.geocaching.databinding.ImageGalleryViewCategoryBinding;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.Folder;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.dialog.ContextMenuDialog;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.LeastRecentlyUsedMap;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.UriUtils;
import cgeo.geocaching.utils.functions.Action1;
import cgeo.geocaching.utils.functions.Func1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.text.HtmlCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import org.apache.commons.lang3.StringUtils;

public class ImageGalleryView extends LinearLayout {

    private static final int TRAILING_VIEWS = 10;

    @NonNull
    private Context context;
    @Nullable
    private Activity activity;

    private ImageActivityHelper imageHelper = null;

    private String geocode;

    private final Map<Integer, Image> imageMap = new HashMap<>();
    private final Map<String, Geopoint> imageCoordMap = new HashMap<>();
    private final ImageDataMemoryCache imageDataMemoryCache = new ImageDataMemoryCache();
    private final Map<String, ImageGalleryViewCategoryBinding> categoryMap = new HashMap<>();
    private final Map<String, EditableCategoryHandler> editableCategoryHandlerMap = new HashMap<>();

    public interface EditableCategoryHandler {

        Collection<Image> getAllImages();

        Collection<Image> add(Collection<Image> images);

        void delete(Image image);

    }

    public static class FolderCategoryHandler implements EditableCategoryHandler {

        private final Folder folder;

        public FolderCategoryHandler(final String geocode) {
            final String suffix = StringUtils.right(geocode, 2);
            folder = Folder.fromFolder(PersistableFolder.SPOILER_IMAGES.getFolder(),
                suffix.substring(1) + "/" + suffix.substring(0, 1)  + "/" + geocode);
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

    private static class ImageDataMemoryCache {

        private String htmlImageCode;

        private final Object imageCacheMutex = new Object();
        private final LeastRecentlyUsedMap<String, Pair<BitmapDrawable, Geopoint>> imageCache = new LeastRecentlyUsedMap.LruCache<>(20);
        private final Map<String, List<Action1<Pair<BitmapDrawable, Geopoint>>>> imageCacheListeners = new HashMap<>();
        private final CompositeDisposable imageCacheDisposable = new CompositeDisposable();

        ImageDataMemoryCache() {
            this.htmlImageCode = HtmlImage.SHARED;
        }

        public void setCode(final String htmlImageCode) {
            if (Objects.equals(htmlImageCode, this.htmlImageCode)) {
                return;
            }
            this.clear();
            this.htmlImageCode = htmlImageCode == null ? HtmlImage.SHARED : htmlImageCode;
        }

        public void loadImage(final String imageUrl, final Action1<Pair<BitmapDrawable, Geopoint>> action) {
            synchronized (imageCacheMutex) {
                if (imageCache.containsKey(imageUrl)) {
                    action.call(imageCache.get(imageUrl));
                    return;
                }
                if (imageCacheListeners.containsKey(imageUrl)) {
                    imageCacheListeners.get(imageUrl).add(action);
                    return;
                }

                imageCacheListeners.put(imageUrl, new ArrayList<>(Collections.singletonList(action)));
                final HtmlImage imgGetter = new HtmlImage(this.htmlImageCode, true, false, false);
                final Disposable disposable = imgGetter.fetchDrawable(imageUrl).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(img -> {
                        final Geopoint gp = getImageLocation(this.htmlImageCode, imageUrl);
                        synchronized (imageCacheMutex) {
                            final Pair<BitmapDrawable, Geopoint> imgData = new Pair<>(img, gp);
                            imageCache.put(imageUrl, imgData);
                            if (imageCacheListeners.containsKey(imageUrl)) {
                                for (Action1<Pair<BitmapDrawable, Geopoint>> a : imageCacheListeners.get(imageUrl)) {
                                    a.call(imgData);
                                }
                            }
                            imageCacheListeners.remove(imageUrl);
                        }
                    });
                imageCacheDisposable.add(disposable);
            }
        }

        public void clear() {
            synchronized (imageCacheMutex) {
                for (Pair<BitmapDrawable, Geopoint> imgData : imageCache.values()) {
                    if (imgData.first != null && imgData.first.getBitmap() != null) {
                        imgData.first.getBitmap().recycle();
                    }
                }
                imageCache.clear();
                imageCacheListeners.clear();
                imageCacheDisposable.clear();
            }
        }

        @Nullable
        private static Geopoint getImageLocation(final String geocode, final String url) {
            try {
                final File file = LocalStorage.getGeocacheDataFile(geocode, url, true, false);
                final Metadata metadata = ImageMetadataReader.readMetadata(file);
                final Collection<GpsDirectory> gpsDirectories = metadata.getDirectoriesOfType(GpsDirectory.class);
                if (gpsDirectories == null) {
                    return null;
                }

                for (final GpsDirectory gpsDirectory : gpsDirectories) {
                    // Try to read out the location, making sure it's non-zero
                    final GeoLocation geoLocation = gpsDirectory.getGeoLocation();
                    if (geoLocation != null && !geoLocation.isZero()) {
                        return new Geopoint(geoLocation.getLatitude(), geoLocation.getLongitude());
                    }
                }
            } catch (final Exception e) {
                Log.i("ImagesList.getImageLocation", e);
            }
            return null;
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

        final Set<String> cats = new HashSet<>();
        for (Image img : images) {
            final String category = categoryMapper == null ? null : categoryMapper.call(img);

            //can't add images from editable categories via this method
            if (!force && editableCategoryHandlerMap.containsKey(category)) {
                continue;
            }

            cats.add(category);
            final ImageGalleryViewCategoryBinding binding = getCategoryViewFor(category);
            final View v = createViewForImage(img, category);
            v.setId(View.generateViewId());
            imageMap.put(v.getId(), img);
            binding.imgGalleryList.addView(v, binding.imgGalleryList.getChildCount() - TRAILING_VIEWS);
        }

        for (String cat : cats) {
            ViewUtils.applyFlowToChildren(this.categoryMap.get(cat).imgGalleryList);
        }
    }

    public void setEditableCategory(final String category, final EditableCategoryHandler handler) {
        if (this.activity == null) {
            //not supported if not in context of an activity
            return;
        }
        removeCategory(category);

        this.editableCategoryHandlerMap.put(category, handler);

        final ImageGalleryViewCategoryBinding binding = getCategoryViewFor(category);
        binding.imgGalleryAddButtons.setVisibility(View.VISIBLE);

        //fill initially
        addImagesInternal(handler.getAllImages(), i -> category, true);

        if (imageHelper == null) {
            imageHelper = new ImageActivityHelper(activity, (r, imgUris, uk) -> {
                final Collection<Image> imgs = CollectionStream.of(imgUris).map(uri -> new Image.Builder().setUrl(uri).build()).toList();
                final Collection<Image> addedImgs = editableCategoryHandlerMap.get(uk).add(imgs);
                addImagesInternal(addedImgs, i -> uk, true);
            });
        }
        binding.imgGalleryAddCamera.setOnClickListener(v -> imageHelper.getImageFromCamera(this.geocode, false, category));
        binding.imgGalleryAddMulti.setOnClickListener(v -> imageHelper.getMultipleImagesFromStorage(this.geocode, false, category));
    }

    /** include this method in your activity to support activity-related tasks */
    public boolean onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        return imageHelper.onActivityResult(requestCode, resultCode, data);
    }

    public void clear() {
        this.imageDataMemoryCache.clear();
        this.categoryMap.clear();
        this.imageMap.clear();
        this.removeAllViews();
    }

    private void removeImage(final View imageView) {
        imageMap.remove(imageView.getId());
        final ConstraintLayout vg = (ConstraintLayout) imageView.getParent();
        vg.removeView(imageView);
        ViewUtils.applyFlowToChildren(vg);
        forceRelayout(vg);
    }

    private void forceRelayout(final ViewGroup vg) {
        for (int i = 0; i < vg.getChildCount(); i++) {
            vg.getChildAt(i).forceLayout();
        }
        vg.invalidate();
        vg.requestLayout();
    }

    private void removeCategory(final String category) {
        final ImageGalleryViewCategoryBinding binding = this.categoryMap.get(category);
        if (binding == null) {
            return;
        }
        for (int i = 0; i < binding.imgGalleryList.getChildCount(); i++) {
        final View child = binding.imgGalleryList.getChildAt(i);
            removeImage(child);
        }
        this.removeView(binding.getRoot());
        this.categoryMap.remove(category);
    }

    private ImageGalleryViewCategoryBinding getCategoryViewFor(final String category) {
        ImageGalleryViewCategoryBinding binding = this.categoryMap.get(category);
        if (binding == null) {
            binding =
                ImageGalleryViewCategoryBinding.inflate(LayoutInflater.from(context), this, false);
            binding.imgGalleryCategoryTitle.setText(category);
            this.categoryMap.put(category, binding);

            final ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewUtils.dpToPixel(180), 1);
            for (int i = 0; i < TRAILING_VIEWS; i++) {
                final Space s = new Space(context);
                s.setBackgroundResource(R.drawable.mark_orange);
                s.setId(View.generateViewId());
                binding.imgGalleryList.addView(s, lp);
            }

            if (category == null) {
                binding.imgGalleryCategory.setVisibility(View.GONE);
                this.addView(binding.getRoot(), 0);
            } else {
                this.addView(binding.getRoot());
            }
        }
        return binding;

    }

    private View createViewForImage(final Image img, final String category) {
        final View itemView = LayoutInflater.from(context).inflate(R.layout.image_gallery_image, this, false);

        setTextIfPossible(img.getTitle(), itemView, R.id.image_title);
        setTextIfPossible(img.getDescription(), itemView, R.id.image_description);

        imageDataMemoryCache.loadImage(img.getUrl(), p -> {
                final BitmapDrawable bd = p.first;
                final Geopoint gp = p.second;

                final ImageView iv = itemView.findViewById(R.id.image_image);
                iv.setImageDrawable(bd);
                iv.setVisibility(View.VISIBLE);
                if (gp != null) {
                    itemView.findViewById(R.id.image_geo_overlay).setVisibility(View.VISIBLE);
                    this.imageCoordMap.put(img.getUrl(), gp);
                }
                itemView.findViewById(R.id.image_progress_bar).setVisibility(View.GONE);
            });

        itemView.setOnClickListener(v -> {
            viewInStandardApp(imageMap.get(itemView.getId()));
        });
        itemView.setOnLongClickListener(v -> {
            showContextOptions(imageMap.get(itemView.getId()), itemView, category);
            return true;
        });
        return itemView;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        //clear image cache
        this.imageDataMemoryCache.clear();
    }

    private static void setTextIfPossible(final String htmlText, final View parentView, final int viewId) {
        if (StringUtils.isBlank(htmlText) || parentView == null || viewId == 0) {
            return;
        }
        final View view = parentView.findViewById(viewId);
        if (view instanceof TextView) {
            ((TextView) view).setText(HtmlCompat.fromHtml(htmlText, HtmlCompat.FROM_HTML_MODE_LEGACY));
            view.setVisibility(View.VISIBLE);
        } else if (view != null) {
            view.setVisibility(View.GONE);
        }
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

    private void showContextOptions(final Image img, final View imageView, final String category) {
        if (activity == null) {
            return;
        }
        final ContextMenuDialog ctxMenu = new ContextMenuDialog(activity);
        ctxMenu.setTitle(LocalizationUtils.getString(R.string.cache_image));

        ctxMenu.addItem(LocalizationUtils.getString(R.string.cache_image_open_file), R.drawable.ic_menu_info_details, v -> {
            viewInStandardApp(img);
        });

        if (!UriUtils.isFileUri(img.getUri()) && !UriUtils.isContentUri(img.getUri())) {
            ctxMenu.addItem(LocalizationUtils.getString(R.string.cache_image_open_browser), R.drawable.ic_menu_share, v -> {
                ShareUtils.openUrl(context, img.getUrl(), false);
            });
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
            ctxMenu.addItem(LocalizationUtils.getString(R.string.cache_menu_navigate), R.drawable.ic_menu_navigate, v -> {
                NavigationAppFactory.showNavigationMenu(activity, null, null, gp);
            });
        }

        final EditableCategoryHandler editHandler = editableCategoryHandlerMap.get(category);
        if (editHandler != null) {
            ctxMenu.addItem(LocalizationUtils.getString(R.string.log_image_delete), R.drawable.ic_menu_delete, v -> {
                removeImage(imageView);
                editHandler.delete(img);
            });
        }
        ctxMenu.show();
    }

}
