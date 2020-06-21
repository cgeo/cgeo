package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.ui.ImagesList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import org.apache.commons.collections4.CollectionUtils;

public class ImagesActivity extends AbstractActionBarActivity {

    private List<Image> imageNames;
    private ImagesList imagesList;
    private final CompositeDisposable createDisposables = new CompositeDisposable();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get parameters
        final Bundle extras = getIntent().getExtras();

        String geocode = null;
        if (extras != null) {
            geocode = extras.getString(Intents.EXTRA_GEOCODE);
        }

        if (extras == null || geocode == null) {
            showToast("Sorry, c:geo forgot for what cache you want to load spoiler images.");
            finish();
            return;
        }

        // init
        setTheme();
        setContentView(R.layout.images_activity);

        setCacheTitleBar(geocode);

        imagesList = new ImagesList(this, geocode, null);

        imageNames = extras.getParcelableArrayList(Intents.EXTRA_IMAGES);
        if (CollectionUtils.isEmpty(imageNames)) {
            showToast(res.getString(R.string.warn_load_images));
            finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        createDisposables.add(imagesList.loadImages(findViewById(R.id.spoiler_list), imageNames));
    }

    @Override
    public void onStop() {
        // Reclaim native memory faster than the finalizers would
        createDisposables.clear();
        super.onStop();
    }

    public static void startActivity(final Context fromActivity, final String geocode, final List<Image> logImages) {
        final Intent logImgIntent = new Intent(fromActivity, ImagesActivity.class)
                .putExtra(Intents.EXTRA_GEOCODE, geocode);

        // avoid forcing the array list as parameter type
        final ArrayList<Image> arrayList = new ArrayList<>(logImages);
        logImgIntent.putParcelableArrayListExtra(Intents.EXTRA_IMAGES, arrayList);
        fromActivity.startActivity(logImgIntent);
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        imagesList.onCreateContextMenu(menu, v);
    }

    @Override
    public boolean onContextItemSelected(@NonNull final MenuItem item) {
        if (imagesList.onContextItemSelected(item)) {
            return true;
        }
        return super.onContextItemSelected(item);
    }
}
