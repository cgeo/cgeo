package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Action2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;


/**
 * Helper class for activities which want to deal with images.
 *
 * Makes much usage of {@link cgeo.geocaching.utils.ImageUtils}, but adds activity-related behaviour
 */
public class ImageActivityHelper {

    //use a globally unique request code to mix-in with Activity.onActivityResult. (This will no longer be neccessary with Activity Result API)
    //code must be positive (>0) and <=65535 (restriction of SDK21)
    public static final int REQUEST_CODE_CAMERA = 58221; //this is a random number
    public static final int REQUEST_CODE_STORAGE_SELECT = 59372;
    public static final int REQUEST_CODE_STORAGE_SELECT_MULTI = 59373;

    private final Activity context;
    private final Action2<Integer, List<Image>> callbackHandler;

    private final Bundle runningIntents = new Bundle();

    private static class IntentContextData implements Parcelable {
        public final int requestCode;
        public final String fileid;
        public final Uri uri;
        public final boolean callOnFailure;

        IntentContextData(final int requestCode, final String fileid, final Uri uri, final boolean callOnFailure) {
            this.requestCode = requestCode;
            this.fileid = fileid;
            this.uri = uri;
            this.callOnFailure = callOnFailure;
        }

        protected IntentContextData(final Parcel in) {
            requestCode = in.readInt();
            fileid = in.readString();
            uri = in.readParcelable(Uri.class.getClassLoader());
            callOnFailure = in.readByte() > 0;
        }

        public static final Creator<IntentContextData> CREATOR = new Creator<IntentContextData>() {
            @Override
            public IntentContextData createFromParcel(final Parcel in) {
                return new IntentContextData(in);
            }

            @Override
            public IntentContextData[] newArray(final int size) {
                return new IntentContextData[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(final Parcel dest, final int flags) {
            dest.writeInt(requestCode);
            dest.writeString(fileid);
            dest.writeParcelable(uri, flags);
            dest.writeByte((byte) (callOnFailure ? 1 : 0));
        }
    }

    public ImageActivityHelper(final Activity activity, final Action2<Integer, List<Image>> callbackHandler) {
        this.context = activity;
        this.callbackHandler = callbackHandler;
    }

    public void setState(final Bundle bundle) {
        if (bundle != null) {
            runningIntents.clear();
            runningIntents.putAll(bundle);
        }
    }

    public Bundle getState() {
        return runningIntents;
    }

    /**
     * In your activity, overwrite {@link Activity#onActivityResult(int, int, Intent)} and
     * call this method inside it. If it returns true then the activity result has been consumed.
     */
    public boolean onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        final IntentContextData storedData = runningIntents.getParcelable("" + requestCode);
        runningIntents.remove("" + requestCode);

        if (storedData == null) {
            return false;
        }

        if (!checkBasicResults(resultCode)) {
            return true;
        }

        switch (requestCode) {
            case REQUEST_CODE_CAMERA:
                onImageFromCameraResult(storedData);
                break;
            case REQUEST_CODE_STORAGE_SELECT:
            case REQUEST_CODE_STORAGE_SELECT_MULTI:
                onImageFromStorageResult(storedData, data);
                break;
            default:
                break;
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
     */
    public void getImageFromStorage(final String fileid, final boolean callOnFailure) {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startIntent(Intent.createChooser(intent, "Select Image"),
                new IntentContextData(REQUEST_CODE_STORAGE_SELECT, fileid, null, callOnFailure));
    }

    /**
     * lets the user select MULTIPLE images from his/her device (calling necessary intents and such).
     * It will create local image copies for all selected images in c:geo private storage for further processing.
     * This function wil only work if you call {@link #onActivityResult(int, int, Intent)} in
     * your activity as explained there.
     * @param fileid an id which will be part of resulting image name (e.g. a cache code)
     * @param callOnFailure if true, then callback will be called also on image select failure (with img=null)
     */
    public void getMultipleImagesFromStorage(final String fileid, final boolean callOnFailure) {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startIntent(Intent.createChooser(intent, "Select Multiple Images"),
                new IntentContextData(REQUEST_CODE_STORAGE_SELECT_MULTI, fileid, null, callOnFailure));
    }

    /**
     * lets the user create a new image via camera for strict usage by c:geo.
     * This method will create both a copy of the created image in public image storage as well as a local image copy
     * in private app storage for further processing by c:geo
     * This function wil only work if you call {@link #onActivityResult(int, int, Intent)} in
     * your activity as explained there.
     * @param fileid an id which will be part of resulting image name (e.g. a cache code)
     * @param callOnFailure if true, then callback will be called also on image shooting failure (with img=null)
     */
    public void getImageFromCamera(final String fileid, final boolean callOnFailure) {

        final ImmutablePair<String, Uri> newImageData = ImageUtils.createNewPublicImageUri(fileid);
        final Uri imageUri = newImageData.right;
        if (imageUri == null || imageUri.equals(Uri.EMPTY) || StringUtils.isBlank(imageUri.toString())) {
            failIntent(REQUEST_CODE_CAMERA, callOnFailure);
            return;
        }

        // create Intent to take a picture and return control to the calling application
        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri); // set the image uri

        // start the image capture Intent
        startIntent(intent, new IntentContextData(REQUEST_CODE_CAMERA, fileid, imageUri, callOnFailure));
    }

    private void onImageFromCameraResult(final IntentContextData intentContextData) {

        final Image img = getImageFromPublicUri(intentContextData.uri, false, intentContextData.fileid);
        if (img == null) {
            failIntent(intentContextData);
            return;
        }
        call(intentContextData, img);
    }

    private void onImageFromStorageResult(final IntentContextData intentContextData, final Intent data) {

        if (data == null) {
            failIntent(intentContextData);
            return;
        }

        final List<Image> result = new ArrayList<>();
        Uri singleUri = null;

        if (data.getData() != null) {
            singleUri = data.getData();
            final Image img = getImageFromPublicUri(data.getData(), true, intentContextData.fileid);
            if (img != null) {
                result.add(img);
            }
        }

        if (data.getClipData() != null) {
            for (int idx = 0; idx < data.getClipData().getItemCount(); idx++) {
                final Uri uri = data.getClipData().getItemAt(idx).getUri();
                // compare with "singleUri" to prevent duplicate image adding
                // (in some cases on multiselect, "getData()" is filled with first entry of "getClipData()" e.g. when Google Photos is used)
                if (uri != null && !uri.equals(singleUri)) {
                    final Image img = getImageFromPublicUri(uri, true, intentContextData.fileid);
                    if (img != null) {
                        result.add(img);
                    }
                }
            }
        }

        if (result.isEmpty()) {
            failIntent(intentContextData);
        } else {
            call(intentContextData, result);
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

    private void startIntent(final Intent intent, final IntentContextData intentContextData) {
        context.startActivityForResult(intent, intentContextData.requestCode);
        runningIntents.putParcelable("" + intentContextData.requestCode, intentContextData);
    }

    private void call(final IntentContextData intentContextData, final Image img) {
        call(intentContextData, img == null ? null : Arrays.asList(img));
    }

    private void call(final IntentContextData intentContextData, final List<Image> imgs) {
        if (this.callbackHandler != null) {
            this.callbackHandler.call(intentContextData.requestCode, imgs == null ? Collections.emptyList() : imgs);
        }
    }

    private void failIntent(final IntentContextData intentContextData) {
        failIntent(intentContextData.requestCode, intentContextData.callOnFailure);
    }

    private void failIntent(final int requestCode, final boolean callOnFailure) {
        ActivityMixin.showToast(context, R.string.err_acquire_image_failed);

        if (callOnFailure && this.callbackHandler != null) {
            this.callbackHandler.call(requestCode, null);
        }
    }
}
