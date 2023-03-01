package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.functions.Action3;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
    private final Action3<Integer, List<Uri>, String> callbackHandler;

    private final Bundle runningIntents = new Bundle();

    private static class IntentContextData implements Parcelable {
        public final int requestCode;
        public final String fileid;
        public final Uri uri;
        public final boolean callOnFailure;
        public final String userKey;
        public final boolean onlyImages;

        IntentContextData(final int requestCode, final String fileid, final Uri uri, final boolean callOnFailure, final String userKey, final boolean onlyImages) {
            this.requestCode = requestCode;
            this.fileid = fileid;
            this.uri = uri;
            this.callOnFailure = callOnFailure;
            this.userKey = userKey;
            this.onlyImages = onlyImages;
        }

        protected IntentContextData(final Parcel in) {
            requestCode = in.readInt();
            fileid = in.readString();
            uri = in.readParcelable(Uri.class.getClassLoader());
            callOnFailure = in.readByte() > 0;
            userKey = in.readString();
            onlyImages = in.readInt() == 1;
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
            dest.writeString(userKey);
            dest.writeInt(onlyImages ? 1 : 0);
        }
    }

    public ImageActivityHelper(final Activity activity, final Action3<Integer, List<Uri>, String> callbackHandler) {
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
     * In your activity, overwrite @link Activity#onActivityResult(int, int, Intent)} and
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
     *
     * @param fileid        an id which will be part of resulting image name (e.g. a cache code)
     * @param callOnFailure if true, then callback will be called also on image select failure (with img=null)
     */
    public void getImageFromStorage(final String fileid, final boolean callOnFailure, final String userKey) {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startIntent(Intent.createChooser(intent, "Select Image"),
                new IntentContextData(REQUEST_CODE_STORAGE_SELECT, fileid, null, callOnFailure, userKey, true));
    }

    /**
     * lets the user select MULTIPLE images from his/her device (calling necessary intents and such).
     * It will create local image copies for all selected images in c:geo private storage for further processing.
     * This function wil only work if you call {@link #onActivityResult(int, int, Intent)} in
     * your activity as explained there.
     *
     * @param fileid        an id which will be part of resulting image name (e.g. a cache code)
     * @param callOnFailure if true, then callback will be called also on image select failure (with img=null)
     */
    public void getMultipleImagesFromStorage(final String fileid, final boolean callOnFailure, final String userKey) {
        getMultipleItemsFromStorage(fileid, callOnFailure, userKey, true);
    }

    /** like above, but allows also for selection of non-image-files */
    public void getMultipleFilesFromStorage(final String fileid, final boolean callOnFailure, final String userKey) {
        getMultipleItemsFromStorage(fileid, callOnFailure, userKey, false);
    }

    private void getMultipleItemsFromStorage(final String fileid, final boolean callOnFailure, final String userKey, final boolean onlyImages) {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(onlyImages ? "image/*" : "*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startIntent(Intent.createChooser(intent, "Select Multiple Images"),
                new IntentContextData(REQUEST_CODE_STORAGE_SELECT_MULTI, fileid, null, callOnFailure, userKey, onlyImages));
    }

    /**
     * lets the user create a new image via camera for strict usage by c:geo.
     * This method will create both a copy of the created image in public image storage as well as a local image copy
     * in private app storage for further processing by c:geo
     * This function wil only work if you call {@link #onActivityResult(int, int, Intent)} in
     * your activity as explained there.
     *
     * @param fileid        an id which will be part of resulting image name (e.g. a cache code)
     * @param callOnFailure if true, then callback will be called also on image shooting failure (with img=null)
     */
    public void getImageFromCamera(final String fileid, final boolean callOnFailure, final String userKey) {

        final ImmutablePair<String, Uri> newImageData = ImageUtils.createNewPublicImageUri(fileid);
        final Uri imageUri = newImageData.right;
        if (imageUri == null || imageUri.equals(Uri.EMPTY) || StringUtils.isBlank(imageUri.toString())) {
            failIntent(REQUEST_CODE_CAMERA, callOnFailure, userKey);
            return;
        }

        // create Intent to take a picture and return control to the calling application
        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri); // set the image uri

        // start the image capture Intent
        startIntent(intent, new IntentContextData(REQUEST_CODE_CAMERA, fileid, imageUri, callOnFailure, userKey, false));
    }

    private void onImageFromCameraResult(final IntentContextData intentContextData) {

        if (!checkImageContent(intentContextData.uri, false)) {
            failIntent(intentContextData);
            return;
        }
        call(intentContextData, intentContextData.uri);
    }

    private void onImageFromStorageResult(final IntentContextData intentContextData, final Intent data) {

        if (data == null) {
            failIntent(intentContextData);
            return;
        }

        final List<Uri> result = new ArrayList<>();
        Uri singleUri = null;

        final Uri imageUri = data.getData();
        if (imageUri != null) {
            singleUri = imageUri;
            if (checkImageContent(imageUri, intentContextData.onlyImages)) {
                result.add(imageUri);
            }
        }

        if (data.getClipData() != null) {
            for (int idx = 0; idx < data.getClipData().getItemCount(); idx++) {
                final Uri uri = data.getClipData().getItemAt(idx).getUri();
                // compare with "singleUri" to prevent duplicate image adding
                // (in some cases on multiselect, "getData()" is filled with first entry of "getClipData()" e.g. when Google Photos is used)
                if (uri != null && !uri.equals(singleUri) && checkImageContent(uri, intentContextData.onlyImages)) {
                    result.add(uri);
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
        imageView.setVisibility(View.INVISIBLE);
        AndroidRxUtils.andThenOnUi(AndroidRxUtils.computationScheduler, () -> ImageUtils.readAndScaleImageToFitDisplay(image.getUri()), bitmap -> {
            imageView.setImageBitmap(bitmap);
            imageView.setVisibility(View.VISIBLE);
        });
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

    private boolean checkImageContent(final Uri imageUri, final boolean checkMimeType) {
        if (imageUri == null) {
            ActivityMixin.showToast(context, R.string.err_acquire_image_failed);
            return false;
        }

        InputStream ignore = null;
        try {
            ignore = context.getContentResolver().openInputStream(imageUri);
        } catch (IOException ie) {
            ActivityMixin.showToast(context, R.string.err_acquire_image_failed);
            return false;
        } finally {
            IOUtils.closeQuietly(ignore);
        }

        final String mimeType = context.getContentResolver().getType(imageUri);
        if (checkMimeType && (!("image/jpeg".equals(mimeType) || "image/png".equals(mimeType) || "image/gif".equals(mimeType)))) {
            ActivityMixin.showToast(context, R.string.err_acquire_image_unsupported_format);
            return false;
        }
        return true;
    }

    private void startIntent(final Intent intent, final IntentContextData intentContextData) {
        context.startActivityForResult(intent, intentContextData.requestCode);
        runningIntents.putParcelable("" + intentContextData.requestCode, intentContextData);
    }

    private void call(final IntentContextData intentContextData, final Uri img) {
        call(intentContextData, img == null ? null : Collections.singletonList(img));
    }

    private void call(final IntentContextData intentContextData, final List<Uri> imgs) {
        if (this.callbackHandler != null) {
            this.callbackHandler.call(intentContextData.requestCode, imgs == null ? Collections.emptyList() : imgs, intentContextData.userKey);
        }
    }

    private void failIntent(final IntentContextData intentContextData) {
        failIntent(intentContextData.requestCode, intentContextData.callOnFailure, intentContextData.userKey);
    }

    private void failIntent(final int requestCode, final boolean callOnFailure, final String userKey) {
        ActivityMixin.showToast(context, R.string.err_acquire_image_failed);

        if (callOnFailure && this.callbackHandler != null) {
            this.callbackHandler.call(requestCode, null, userKey);
        }
    }
}
