package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.ImageGalleryView;
import cgeo.geocaching.utils.ImageUtils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class ImageGalleryActivity extends AbstractActionBarActivity {

    private List<Image> images;
    private ImageGalleryView imageGallery;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get parameters
        final Bundle extras = getIntent().getExtras();

        String geocode = null;
        if (extras != null) {
            geocode = extras.getString(Intents.EXTRA_GEOCODE);
        }

        // init
        setThemeAndContentView(R.layout.imagegallery_activity);
        setCacheTitleBar(geocode);

        images = extras.getParcelableArrayList(Intents.EXTRA_IMAGES);

        imageGallery = findViewById(R.id.image_gallery);
        ImageUtils.initializeImageGallery(imageGallery, geocode, images, false);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        imageGallery.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        // Reclaim native memory faster than the finalizers would
        if (imageGallery != null) {
            imageGallery.clear();
        }
    }

    public static void startActivity(final Context fromActivity, final String geocode, final List<Image> images) {

        if (!Settings.enableFeatureNewImageGallery()) {
            ImagesActivity.startActivity(fromActivity, geocode, images);
            return;
        }

        final Intent logImgIntent = new Intent(fromActivity, ImageGalleryActivity.class)
                .putExtra(Intents.EXTRA_GEOCODE, geocode);

        // avoid forcing the array list as parameter type
        final ArrayList<Image> arrayList = new ArrayList<>(images);
        logImgIntent.putParcelableArrayListExtra(Intents.EXTRA_IMAGES, arrayList);
        fromActivity.startActivity(logImgIntent);
    }


}
