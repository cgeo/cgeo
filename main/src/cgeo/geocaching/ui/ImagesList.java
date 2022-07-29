package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.apps.navi.NavigationAppFactory;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.service.GeocacheChangedBroadcastReceiver;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.FileProvider;
import androidx.core.text.HtmlCompat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.LinkedList;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.CancellableDisposable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class ImagesList {

    private ImageView currentView;
    private Image currentImage;

    public enum ImageType {
        LogImages(R.string.cache_log_images_title), SpoilerImages(R.string.cache_spoiler_images_title);

        @StringRes private final int titleResId;

        ImageType(@StringRes final int title) {
            this.titleResId = title;
        }

        @StringRes
        public int getTitle() {
            return titleResId;
        }
    }

    private LayoutInflater inflater = null;
    private final Activity activity;
    // We could use a Set here, but we will insert no duplicates, so there is no need to check for uniqueness.
    private final Collection<Bitmap> bitmaps = new LinkedList<>();
    /**
     * map image view id to image
     */
    private final SparseArray<Image> images = new SparseArray<>();
    private final SparseArray<Geopoint> geoPoints = new SparseArray<>();
    private final String geocode;
    private final Geocache geocache;
    private LinearLayout imagesView;

    public ImagesList(final Activity activity, final String geocode, final Geocache geocache) {
        this.activity = activity;
        this.geocode = geocode;
        this.geocache = geocache;
        inflater = activity.getLayoutInflater();
    }

    /**
     * Load images into a view.
     *
     * @param parentView a view to load the images into
     * @param images     the images to load
     * @return a disposable which, when disposed, interrupts the loading and clears up resources
     */
    public Disposable loadImages(final View parentView, final Collection<Image> images) {
        // Start with a fresh disposable because of this method can be called several times if the
        // enclosing activity is stopped/restarted.
        final CompositeDisposable disposables = new CompositeDisposable(new CancellableDisposable(this::removeAllViews));

        imagesView = parentView.findViewById(R.id.spoiler_list);
        imagesView.removeAllViews();

        final HtmlImage imgGetter = new HtmlImage(geocode, true, false, false);

        for (final Image img : images) {
            final LinearLayout rowView = (LinearLayout) inflater.inflate(R.layout.cache_image_item, imagesView, false);
            assert rowView != null;

            if (StringUtils.isNotBlank(img.getTitle())) {
                final TextView titleView = rowView.findViewById(R.id.title);
                titleView.setText(HtmlCompat.fromHtml(img.getTitle(), HtmlCompat.FROM_HTML_MODE_LEGACY));
            }

            if (StringUtils.isNotBlank(img.getDescription())) {
                final TextView descView = rowView.findViewById(R.id.description);
                descView.setText(HtmlCompat.fromHtml(img.getDescription(), HtmlCompat.FROM_HTML_MODE_LEGACY), TextView.BufferType.SPANNABLE);
                descView.setVisibility(View.VISIBLE);
            }

            final RelativeLayout imageView = (RelativeLayout) inflater.inflate(R.layout.image_item, rowView, false);
            assert imageView != null;
            rowView.addView(imageView);
            imagesView.addView(rowView);
            disposables.add(AndroidRxUtils.bindActivity(activity, imgGetter.fetchDrawable(img.getUrl())).subscribe(image -> display(imageView, image, img, rowView)));
        }

        return disposables;
    }

    private void display(final RelativeLayout imageViewLayout, final BitmapDrawable image, final Image img, final LinearLayout view) {
        final ImageView imageView = imageViewLayout.findViewById(R.id.map_image);
        // In case of a failed download happening fast, the imageView seems to not have been added to the layout yet
        if (image != null && imageView != null) {
            bitmaps.add(image.getBitmap());

            final Rect bounds = image.getBounds();

            imageView.setImageResource(R.drawable.image_not_loaded);
            imageView.setClickable(true);
            imageView.setOnClickListener(view1 -> viewImageInStandardApp(img, image));
            activity.registerForContextMenu(imageView);
            imageView.setImageDrawable(image);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setLayoutParams(new RelativeLayout.LayoutParams(bounds.width(), bounds.height()));

            view.findViewById(R.id.progress_bar).setVisibility(View.GONE);

            imageView.setId(image.hashCode());
            images.put(imageView.getId(), img);

            final Geopoint geoPoint = getImageLocation(img);
            if (geoPoint != null) {
                addGeoOverlay(imageViewLayout, geoPoint);
                geoPoints.put(imageView.getId(), geoPoint);
            }

            view.invalidate();
        }
    }

    @Nullable
    private Geopoint getImageLocation(final Image image) {
        try {
            final File file = LocalStorage.getGeocacheDataFile(geocode, image.getUrl(), true, false);
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

    private void addGeoOverlay(final RelativeLayout imageViewLayout, final Geopoint gpt) {
        final ImageView geoOverlay = (ImageView) imageViewLayout.findViewById(R.id.geo_overlay);
        geoOverlay.setVisibility(View.VISIBLE);
        geoOverlay.setOnClickListener(wpNavView -> {
            wpNavView.setOnClickListener(v -> NavigationAppFactory.startDefaultNavigationApplication(1, activity, gpt));
            wpNavView.setOnLongClickListener(v -> {
                NavigationAppFactory.startDefaultNavigationApplication(2, activity, gpt);
                return true;
            });
        });
    }

    private void removeAllViews() {
        for (final Bitmap b : bitmaps) {
            b.recycle();
        }
        bitmaps.clear();
        images.clear();
        geoPoints.clear();

        imagesView.removeAllViews();
    }

    public void onCreateContextMenu(final ContextMenu menu, final View v) {
        activity.getMenuInflater().inflate(R.menu.images_list_context, menu);
        final Resources res = activity.getResources();
        menu.setHeaderTitle(res.getString(R.string.cache_image));
        currentView = (ImageView) v;
        currentImage = images.get(currentView.getId());
        final boolean hasCoordinates = geoPoints.get(currentView.getId()) != null;
        menu.findItem(R.id.image_add_waypoint).setVisible(hasCoordinates && geocache != null);
        menu.findItem(R.id.menu_navigate).setVisible(hasCoordinates);
    }

    public boolean onContextItemSelected(final MenuItem item) {
        final BitmapDrawable currentDrawable = (BitmapDrawable) currentView.getDrawable();
        final int itemId = item.getItemId();
        if (itemId == R.id.image_open_file) {
            viewImageInStandardApp(currentImage, currentDrawable);
        } else if (itemId == R.id.image_open_browser) {
            if (currentImage != null) {
                currentImage.openInBrowser(activity);
            }
        } else if (itemId == R.id.image_add_waypoint) {
            final Geopoint coords = geoPoints.get(currentView.getId());
            if (geocache != null && coords != null) {
                final Waypoint waypoint = new Waypoint(currentImage.getTitle(), WaypointType.WAYPOINT, true);
                waypoint.setCoords(coords);
                geocache.addOrChangeWaypoint(waypoint, true);
                GeocacheChangedBroadcastReceiver.sendBroadcast(activity, geocode);
            }
        } else if (itemId == R.id.menu_navigate) {
            final Geopoint geopoint = geoPoints.get(currentView.getId());
            if (geopoint != null) {
                NavigationAppFactory.showNavigationMenu(activity, null, null, geopoint);
            }
        } else {
            return false;
        }
        return true;
    }

    private static String mimeTypeForUrl(final String url) {
        return StringUtils.defaultString(MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(url)), "image/*");
    }

    private static File saveToTemporaryJPGFile(final BitmapDrawable image) throws FileNotFoundException {
        final File file = LocalStorage.getGeocacheDataFile(HtmlImage.SHARED, "temp.jpg", false, true);
        BufferedOutputStream stream = null;
        try {
            stream = new BufferedOutputStream(new FileOutputStream(file));
            image.getBitmap().compress(CompressFormat.JPEG, 100, stream);
        } finally {
            IOUtils.closeQuietly(stream);
        }
        file.deleteOnExit();
        return file;
    }

    private void viewImageInStandardApp(final Image img, final BitmapDrawable image) {
        try {
            final Intent intent = new Intent().setAction(Intent.ACTION_VIEW);
            final File file = img.isLocalFile() ? img.localFile() : LocalStorage.getGeocacheDataFile(geocode, img.getUrl(), true, true);
            final String authority = activity.getApplicationContext().getString(R.string.file_provider_authority);
            if (file.exists()) {
                intent.setDataAndType(
                        FileProvider.getUriForFile(activity, authority, file),
                        mimeTypeForUrl(img.getUrl())
                );
            } else {
                intent.setDataAndType(
                        FileProvider.getUriForFile(activity, authority, saveToTemporaryJPGFile(image)),
                        "image/jpeg"
                );
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (final Exception e) {
            Log.e("ImagesList.viewImageInStandardApp", e);
        }
    }

}
