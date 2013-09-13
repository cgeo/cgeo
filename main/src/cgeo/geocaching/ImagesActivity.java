package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.settings.Settings;
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

    private boolean offline;
    private ArrayList<Image> imageNames;
    private ImagesList imagesList;
    private ImageType imgType = ImageType.SpoilerImages;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get parameters
        final Bundle extras = getIntent().getExtras();

        String geocode = null;
        if (extras != null) {
            geocode = extras.getString(Intents.EXTRA_GEOCODE);
            imgType = (ImageType) extras.getSerializable(Intents.EXTRA_TYPE);
        }

        if (extras == null || geocode == null) {
            showToast("Sorry, c:geo forgot for what cache you want to load spoiler images.");
            finish();
            return;
        }

        // init
        setTheme();
        setContentView(R.layout.images_activity);
        setTitle(res.getString(imgType.getTitle()));

        imagesList = new ImagesList(this, geocode);

        imageNames = extras.getParcelableArrayList(Intents.EXTRA_IMAGES);
        if (CollectionUtils.isEmpty(imageNames)) {
            showToast(res.getString(R.string.warn_load_images));
            finish();
            return;
        }

        offline = DataStore.isOffline(geocode, null) && (imgType == ImageType.SpoilerImages
                || Settings.isStoreLogImages());
    }

    @Override
    public void onStart() {
        super.onStart();
        imagesList.loadImages(findViewById(R.id.spoiler_list), imageNames, offline);
    }

    @Override
    public void onStop() {
        // Reclaim native memory faster than the finalizers would
        imagesList.removeAllViews();
        super.onStop();
    }

    public static void startActivityLogImages(final Context fromActivity, final String geocode, List<Image> logImages) {
        startActivity(fromActivity, geocode, logImages, ImageType.LogImages);
    }

    private static void startActivity(final Context fromActivity, final String geocode, List<Image> logImages, ImageType imageType) {
        final Intent logImgIntent = new Intent(fromActivity, ImagesActivity.class);
        // if resuming our app within this activity, finish it and return to the cache activity
        logImgIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
                .putExtra(Intents.EXTRA_GEOCODE, geocode)
                .putExtra(Intents.EXTRA_TYPE, imageType);

        // avoid forcing the array list as parameter type
        final ArrayList<Image> arrayList = new ArrayList<Image>(logImages);
        logImgIntent.putParcelableArrayListExtra(Intents.EXTRA_IMAGES, arrayList);
        fromActivity.startActivity(logImgIntent);
    }

    public static void startActivitySpoilerImages(final Context fromActivity, String geocode, List<Image> spoilers) {
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
