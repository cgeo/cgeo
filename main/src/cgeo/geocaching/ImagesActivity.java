package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.ui.ImagesList;
import cgeo.geocaching.ui.ImagesList.ImageType;

import org.apache.commons.collections.CollectionUtils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class ImagesActivity extends AbstractActivity {

    private static final String EXTRAS_IMAGES = "images";
    private static final String EXTRAS_TYPE = "type";
    private static final String EXTRAS_GEOCODE = "geocode";

    private boolean offline;
    private ArrayList<cgImage> imageNames;
    private ImagesList imagesList;
    private ImageType imgType = ImageType.SpoilerImages;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get parameters
        final Bundle extras = getIntent().getExtras();

        String geocode = null;
        if (extras != null) {
            geocode = extras.getString(EXTRAS_GEOCODE);
            imgType = (ImageType) extras.getSerializable(EXTRAS_TYPE);
        }

        if (extras == null || geocode == null) {
            showToast("Sorry, c:geo forgot for what cache you want to load spoiler images.");
            finish();
            return;
        }

        // init
        setTheme();
        setContentView(R.layout.spoilers);
        setTitle(res.getString(imgType.getTitle()));

        imagesList = new ImagesList(this, geocode);

        imageNames = extras.getParcelableArrayList(EXTRAS_IMAGES);
        if (CollectionUtils.isEmpty(imageNames)) {
            showToast(res.getString(R.string.warn_load_images));
            finish();
            return;
        }

        offline = cgData.isOffline(geocode, null) && (imgType == ImageType.SpoilerImages || Settings.isStoreLogImages());
    }

    @Override
    public void onStart() {
        super.onStart();
        imagesList.loadImages(findViewById(R.id.spoiler_list), imageNames, imgType, offline);
    }

    @Override
    public void onStop() {
        // Reclaim native memory faster than the finalizers would
        imagesList.removeAllViews();
        super.onStop();
    }

    public static void startActivityLogImages(final Context fromActivity, final String geocode, List<cgImage> logImages) {
        startActivity(fromActivity, geocode, logImages, ImageType.LogImages);
    }

    private static void startActivity(final Context fromActivity, final String geocode, List<cgImage> logImages, ImageType imageType) {
        final Intent logImgIntent = new Intent(fromActivity, ImagesActivity.class);
        // if resuming our app within this activity, finish it and return to the cache activity
        logImgIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
                .putExtra(EXTRAS_GEOCODE, geocode)
                .putExtra(EXTRAS_TYPE, imageType);

        // avoid forcing the array list as parameter type
        final ArrayList<cgImage> arrayList = new ArrayList<cgImage>(logImages);
        logImgIntent.putParcelableArrayListExtra(EXTRAS_IMAGES, arrayList);
        fromActivity.startActivity(logImgIntent);
    }

    public static void startActivitySpoilerImages(final Context fromActivity, String geocode, List<cgImage> spoilers) {
        startActivity(fromActivity, geocode, spoilers, ImageType.SpoilerImages);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        imagesList.onCreateContextMenu(menu, v);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (imagesList.onContextItemSelected(item)) {
            return true;
        }
        return super.onContextItemSelected(item);
    }
}
