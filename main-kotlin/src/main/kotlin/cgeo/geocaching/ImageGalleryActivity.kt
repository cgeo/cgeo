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

package cgeo.geocaching

import cgeo.geocaching.activity.AbstractActionBarActivity
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Image
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.ui.ImageGalleryView
import cgeo.geocaching.utils.ImageUtils
import cgeo.geocaching.utils.Log

import android.content.Context
import android.content.Intent
import android.os.Bundle

import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.List

import org.apache.commons.lang3.StringUtils


class ImageGalleryActivity : AbstractActionBarActivity() {

    private ImageGalleryView imageGallery

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)

        // get parameters
        val extras: Bundle = getIntent().getExtras()

        String geocode = null
        if (extras != null) {
            geocode = extras.getString(Intents.EXTRA_GEOCODE)
        }

        // init
        setThemeAndContentView(R.layout.imagegallery_activity)
        setCacheTitleBar(geocode)

        val images: List<Image> = extras.getParcelableArrayList(Intents.EXTRA_IMAGES)

        imageGallery = findViewById(R.id.image_gallery)
        ImageUtils.initializeImageGallery(imageGallery, geocode, images, false)
    }

    override     public Unit onActivityResult(final Int requestCode, final Int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data)
        imageGallery.onActivityResult(requestCode, resultCode, data)
    }


    override     public Unit onDestroy() {
        super.onDestroy()
        // Reclaim native memory faster than the finalizers would
        if (imageGallery != null) {
            imageGallery.clear()
        }
    }

    public static Unit startActivity(final Context fromActivity, final String geocode, final List<Image> images) {

        val logImgIntent: Intent = Intent(fromActivity, ImageGalleryActivity.class)
                .putExtra(Intents.EXTRA_GEOCODE, geocode)

        // avoid forcing the array list as parameter type
        val arrayList: ArrayList<Image> = ArrayList<>(images)
        logImgIntent.putParcelableArrayListExtra(Intents.EXTRA_IMAGES, arrayList)
        fromActivity.startActivity(logImgIntent)
    }


    /**
     * change the titlebar icon and text to show the current geocache
     */
    private Unit setCacheTitleBar(final String geocode) {
        if (StringUtils.isEmpty(geocode)) {
            return
        }
        val cache: Geocache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
        if (cache == null) {
            Log.e("ImageGalleryActivity.setCacheTitleBar: cannot find the cache " + geocode)
            return
        }
        setCacheTitleBar(cache)
    }

}
