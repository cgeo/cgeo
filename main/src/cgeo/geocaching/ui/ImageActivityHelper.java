package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Action1;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;


/**
 * Helper class for activities which want to deal with images.
 *
 * Makes much usage of {@link cgeo.geocaching.utils.ImageUtils}, but adds activity-related behaviour
 */
public class ImageActivityHelper {

    private final Activity context;

    private final int requestCodeCamera;
    private final int requestCodeStorageSelect;
    private final int requestCodeStorageSelectMulti;

    private final Map<Integer, IntentContextData> runningIntents = Collections.synchronizedMap(new HashMap<>());

    private static class IntentContextData<T> {
        public final Action1<T> callback;
        public final boolean callOnFailure;
        public final String fileid;
        public final Uri uri;

        IntentContextData(final String fileid, final boolean callOnFailure, final Action1<T> callback, final Uri uri) {
            this.callback = callback;
            this.callOnFailure = callOnFailure;
            this.fileid = fileid;
            this.uri = uri;
        }

    }

    public ImageActivityHelper(final Activity activity, final int requestCodeStart) {
        this.context = activity;

        this.requestCodeCamera = requestCodeStart;
        this.requestCodeStorageSelect = requestCodeStart + 1;
        this.requestCodeStorageSelectMulti = requestCodeStart + 2;
    }

    /**
     * In your activity, overwrite {@link Activity#onActivityResult(int, int, Intent)} and
     * call this method inside it. If it returns true then the activity result has been consumed.
     */
    public boolean onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        final IntentContextData storedData = runningIntents.remove(requestCode);
        if (storedData == null) {
            return false;
        }

        if (!checkBasicResults(resultCode)) {
            return true;
        }

        if (requestCode == requestCodeCamera) {
            onImageFromCameraResult(storedData);
        } else if (requestCode == requestCodeStorageSelect) {
            onImageFromStorageResult(storedData, data, false);
        } else if (requestCode == requestCodeStorageSelectMulti) {
            onImageFromStorageResult(storedData, data, true);
        }
        return true;

    }

    /**
     * lets the user select ONE image from his/her device (calling necessary intents and such).
     * It will create a local image copy for the selected images in c:geo private storage for further processing.
     * This function wil only work if you call {@link #onActivityResult(int, int, Intent)} in
     * your activity as explained there.
     * @param fileid an id which will be part of resulting image name (e.g. a cache code)
     * @param callOnFailure if true, then callback will be called also on image select failure (with img=null)
     * @param callback called when image select has completed
     */
    public void getImageFromStorage(final String fileid, final boolean callOnFailure, final Action1<Image> callback) {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startIntent(requestCodeStorageSelect, Intent.createChooser(intent, "Select Image"),
                new IntentContextData(fileid, callOnFailure, callback, null));
    }

    /**
     * lets the user select MULTIPLE images from his/her device (calling necessary intents and such).
     * It will create local image copies for all selected images in c:geo private storage for further processing.
     * This function wil only work if you call {@link #onActivityResult(int, int, Intent)} in
     * your activity as explained there.
     * @param fileid an id which will be part of resulting image name (e.g. a cache code)
     *      * @param callOnFailure if true, then callback will be called also on image select failure (with img=null)
     * @param callback called when image select has completed
     */
    public void getMultipleImagesFromStorage(final String fileid, final boolean callOnFailure, final Action1<List<Image>> callback) {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startIntent(requestCodeStorageSelectMulti, Intent.createChooser(intent, "Select Multiple Images"),
                new IntentContextData(fileid, callOnFailure, callback, null));
    }

    /**
     * lets the user create a new image via camera for strict usage by c:geo.
     * This method will create both a copy of the created image in public image storage as well as a local image copy
     * in private app storage for further processing by c:geo
     * This function wil only work if you call {@link #onActivityResult(int, int, Intent)} in
     * your activity as explained there.
     * @param fileid an id which will be part of resulting image name (e.g. a cache code)
     * @param callOnFailure if true, then callback will be called also on image shooting failure (with img=null)
     * @param callback called when image select has completed
     */
    public void getImageFromCamera(final String fileid, final boolean callOnFailure, final Action1<Image> callback) {

        final ImmutablePair<String, Uri> newImageData = ImageUtils.createNewPublicImageUri(fileid);
        final Uri imageUri = newImageData.right;
        if (imageUri == null || imageUri.equals(Uri.EMPTY) || StringUtils.isBlank(imageUri.toString())) {
            failIntent(callOnFailure, callback);
            return;
        }

        // create Intent to take a picture and return control to the calling application
        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri); // set the image uri

        // start the image capture Intent
        startIntent(requestCodeCamera, intent, new IntentContextData(fileid, callOnFailure, callback, imageUri));
    }

    private void onImageFromCameraResult(final IntentContextData intentContextData) {

        final Image img = getImageFromPublicUri(intentContextData.uri, false, intentContextData.fileid);
        if (img == null) {
            failIntent(intentContextData);
            return;
        }
        intentContextData.callback.call(img);
    }

    private void onImageFromStorageResult(final IntentContextData intentContextData, final Intent data, final boolean multi) {

        if (data == null) {
            failIntent(intentContextData);
            return;
        }

        final List<Image> result = new ArrayList<>();

        if (data.getData() != null) {
            final Image img = getImageFromPublicUri(data.getData(),  true, intentContextData.fileid);
            if (img != null) {
                result.add(img);
            }
        }

        if (data.getClipData() != null) {
            for (int idx = 0; idx < data.getClipData().getItemCount(); idx++) {
                final Image img = getImageFromPublicUri(data.getClipData().getItemAt(idx).getUri(), true, intentContextData.fileid);
                if (img != null) {
                    result.add(img);
                }
            }
        }

        if (result.isEmpty()) {
            failIntent(intentContextData);
        } else if (multi) {
            intentContextData.callback.call(result);
        } else {
            intentContextData.callback.call(result.get(0));
        }
    }

    /**
     * Helper function to load and scale an image asynchronously into a view
     */
    public static void displayImageAsync(final Image image, final ImageView imageView) {
        if (image.isEmpty()) {
            return;
        }
        DisplayImageAsyncTask.displayAsync(image, imageView);
    }

    private static class DisplayImageAsyncTask extends AsyncTask<Void, Void, Bitmap> {

        private final Image image;
        private final ImageView imageView;

        public static void displayAsync(final Image image, final ImageView imageView) {
            imageView.setVisibility(View.INVISIBLE);
            new DisplayImageAsyncTask(image, imageView).execute();
        }

        private DisplayImageAsyncTask(final Image image, final ImageView imageView) {
            this.image = image;
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(final Void... params) {
            return ImageUtils.readAndScaleImageToFitDisplay(image.getUri());
        }

        @Override
        protected void onPostExecute(final Bitmap bitmap) {
            imageView.setImageBitmap(bitmap);
            imageView.setVisibility(View.VISIBLE);
        }
    }


    @Nullable
    private Image getImageFromPublicUri(final Uri imagePublicUri, final boolean checkImageContent, final String fileid) {
        if (checkImageContent && !checkImageContent(imagePublicUri)) {
            return null;
        }

        //copy image from public Uri to a private uri. Don't change anything!
        final ImmutablePair<String, Uri> privateImageData = ImageUtils.createNewOfflineLogImageUri(fileid);
        if (privateImageData.right == null) {
            return null;
        }
        try {
            IOUtils.copy(
                context.getContentResolver().openInputStream(imagePublicUri),
                context.getContentResolver().openOutputStream(privateImageData.right));
            return new Image.Builder().setUrl(privateImageData.right).build();
        } catch (IOException ioe) {
            Log.e("Problem copying image from '" + imagePublicUri + "' to '" + privateImageData.right + "'", ioe);
            return null;
        }

    }

    private boolean checkBasicResults(final int resultCode) {
        if (resultCode == RESULT_CANCELED) {
            // User cancelled the image capture
            ActivityMixin.showToast(context, R.string.info_select_logimage_cancelled);
            return false;
        }

        if (resultCode != RESULT_OK) {
            // Image capture failed, advise user
            ActivityMixin.showToast(context, R.string.err_acquire_image_failed);
            return false;
        }
        return true;
    }

    private boolean checkImageContent(final Uri imageUri) {
        if (imageUri == null) {
            ActivityMixin.showToast(context, R.string.err_acquire_image_failed);
            return false;
        }
        final String mimeType = context.getContentResolver().getType(imageUri);
        if (!("image/jpeg".equals(mimeType) || "image/png".equals(mimeType) || "image/gif".equals(mimeType))) {
            ActivityMixin.showToast(context, R.string.err_acquire_image_failed);
            return false;
        }
        return true;
    }

    private void startIntent(final int requestCode, final Intent intent, final IntentContextData intentContextData) {
        context.startActivityForResult(intent, requestCode);
        runningIntents.put(requestCode, intentContextData);
    }

    private void failIntent(final IntentContextData intentContextData) {
        failIntent(intentContextData.callOnFailure, intentContextData.callback);
    }

    private void failIntent(final boolean callOnFailure, final Action1<?> callback) {
         ActivityMixin.showToast(context, R.string.err_acquire_image_failed);

        if (callOnFailure && callback != null) {
            callback.call(null);
        }
    }
}
