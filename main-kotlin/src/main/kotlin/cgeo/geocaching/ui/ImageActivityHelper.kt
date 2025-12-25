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

package cgeo.geocaching.ui

import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.utils.ImageUtils
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.UriUtils
import cgeo.geocaching.utils.functions.Action3

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK

import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import java.util.Collections
import java.util.List

import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.ImmutablePair


/**
 * Helper class for activities which want to deal with images.
 * <br>
 * Makes much usage of {@link cgeo.geocaching.utils.ImageUtils}, but adds activity-related behaviour
 */
class ImageActivityHelper {

    //use a globally unique request code to mix-in with Activity.onActivityResult. (This will no longer be neccessary with Activity Result API)
    //code must be positive (>0) and <=65535 (restriction of SDK21)
    public static val REQUEST_CODE_CAMERA: Int = 58221; //this is a random number
    public static val REQUEST_CODE_STORAGE_SELECT: Int = 59372
    public static val REQUEST_CODE_STORAGE_SELECT_MULTI: Int = 59373

    private final Activity context
    private final Action3<Integer, List<Uri>, String> callbackHandler

    private val runningIntents: Bundle = Bundle()

    private static class IntentContextData : Parcelable {
        public final Int requestCode
        public final String fileid
        public final Uri uri
        public final Boolean callOnFailure
        public final String userKey
        public final Boolean onlyImages

        IntentContextData(final Int requestCode, final String fileid, final Uri uri, final Boolean callOnFailure, final String userKey, final Boolean onlyImages) {
            this.requestCode = requestCode
            this.fileid = fileid
            this.uri = uri
            this.callOnFailure = callOnFailure
            this.userKey = userKey
            this.onlyImages = onlyImages
        }

        protected IntentContextData(final Parcel in) {
            requestCode = in.readInt()
            fileid = in.readString()
            uri = in.readParcelable(Uri.class.getClassLoader())
            callOnFailure = in.readByte() > 0
            userKey = in.readString()
            onlyImages = in.readInt() == 1
        }

        public static val CREATOR: Creator<IntentContextData> = Creator<IntentContextData>() {
            override             public IntentContextData createFromParcel(final Parcel in) {
                return IntentContextData(in)
            }

            override             public IntentContextData[] newArray(final Int size) {
                return IntentContextData[size]
            }
        }

        override         public Int describeContents() {
            return 0
        }

        override         public Unit writeToParcel(final Parcel dest, final Int flags) {
            dest.writeInt(requestCode)
            dest.writeString(fileid)
            dest.writeParcelable(uri, flags)
            dest.writeByte((Byte) (callOnFailure ? 1 : 0))
            dest.writeString(userKey)
            dest.writeInt(onlyImages ? 1 : 0)
        }
    }

    public ImageActivityHelper(final Activity activity, final Action3<Integer, List<Uri>, String> callbackHandler) {
        this.context = activity
        this.callbackHandler = callbackHandler
    }

    public Unit setState(final Bundle bundle) {
        if (bundle != null) {
            runningIntents.clear()
            runningIntents.putAll(bundle)
        }
    }

    public Bundle getState() {
        return runningIntents
    }

    /**
     * In your activity, overwrite @link Activity#onActivityResult(Int, Int, Intent)} and
     * call this method inside it. If it returns true then the activity result has been consumed.
     */
    public Boolean onActivityResult(final Int requestCode, final Int resultCode, final Intent data) {
        val storedData: IntentContextData = runningIntents.getParcelable("" + requestCode)
        runningIntents.remove("" + requestCode)

        if (storedData == null) {
            return false
        }

        if (!checkBasicResults(resultCode)) {
            return true
        }

        switch (requestCode) {
            case REQUEST_CODE_CAMERA:
                onImageFromCameraResult(storedData)
                break
            case REQUEST_CODE_STORAGE_SELECT:
            case REQUEST_CODE_STORAGE_SELECT_MULTI:
                onImageFromStorageResult(storedData, data)
                break
            default:
                break
        }
        return true

    }

    /**
     * lets the user select MULTIPLE images from his/her device (calling necessary intents and such).
     * It will create local image copies for all selected images in c:geo private storage for further processing.
     * This function wil only work if you call {@link #onActivityResult(Int, Int, Intent)} in
     * your activity as explained there.
     *
     * @param fileid        an id which will be part of resulting image name (e.g. a cache code)
     * @param callOnFailure if true, then callback will be called also on image select failure (with img=null)
     */
    public Unit getMultipleImagesFromStorage(final String fileid, final Boolean callOnFailure, final String userKey, final Uri startUri) {
        getMultipleItemsFromStorage(fileid, callOnFailure, userKey, startUri, true)
    }

    /** like above, but allows also for selection of non-image-files */
    public Unit getMultipleFilesFromStorage(final String fileid, final Boolean callOnFailure, final String userKey) {
        getMultipleItemsFromStorage(fileid, callOnFailure, userKey, null, false)
    }

    private Unit getMultipleItemsFromStorage(final String fileid, final Boolean callOnFailure, final String userKey, final Uri startUri, final Boolean onlyImages) {

        val hasStartUrl: Boolean = UriUtils.isContentUri(startUri)

        //ACTION_GET_CONTENT provides nicer support for images and allows remote image access
        //ACTION_OPEN_DOCUMENT seems to allow only local files, but supports a startUri (which ACTION_GET_CONTENT does not)
        val intent: Intent = Intent(hasStartUrl ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_GET_CONTENT)
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, hasStartUrl ? startUri : PersistableFolder.BASE.getUri())

        if (onlyImages) {
            setImageMimeTypes(intent)
        } else {
            intent.setType("*/*")
        }

        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startIntent(Intent.createChooser(intent, "Select Multiple Images"),
                IntentContextData(REQUEST_CODE_STORAGE_SELECT_MULTI, fileid, null, callOnFailure, userKey, onlyImages))
    }

    /**
     * lets the user create a image via camera for strict usage by c:geo.
     * This method will create both a copy of the created image in public image storage as well as a local image copy
     * in private app storage for further processing by c:geo
     * This function wil only work if you call {@link #onActivityResult(Int, Int, Intent)} in
     * your activity as explained there.
     *
     * @param fileid        an id which will be part of resulting image name (e.g. a cache code)
     * @param callOnFailure if true, then callback will be called also on image shooting failure (with img=null)
     */
    public Unit getImageFromCamera(final String fileid, final Boolean callOnFailure, final String userKey) {

        val newImageData: ImmutablePair<String, Uri> = ImageUtils.createNewPublicImageUri(fileid)
        val imageUri: Uri = newImageData.right
        if (imageUri == null || imageUri == (Uri.EMPTY) || StringUtils.isBlank(imageUri.toString())) {
            failIntent(REQUEST_CODE_CAMERA, callOnFailure, userKey)
            return
        }

        // create Intent to take a picture and return control to the calling application
        val intent: Intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri); // set the image uri

        // start the image capture Intent
        startIntent(intent, IntentContextData(REQUEST_CODE_CAMERA, fileid, imageUri, callOnFailure, userKey, false))
    }

    private Unit onImageFromCameraResult(final IntentContextData intentContextData) {

        if (!checkImageContent(intentContextData.uri, false)) {
            failIntent(intentContextData)
            return
        }
        call(intentContextData, intentContextData.uri)
    }

    private Unit onImageFromStorageResult(final IntentContextData intentContextData, final Intent data) {

        if (data == null) {
            failIntent(intentContextData)
            return
        }

        val result: List<Uri> = ArrayList<>()
        Uri singleUri = null

        val imageUri: Uri = data.getData()
        if (imageUri != null) {
            singleUri = imageUri
            if (checkImageContent(imageUri, intentContextData.onlyImages)) {
                result.add(imageUri)
            }
        }

        if (data.getClipData() != null) {
            for (Int idx = 0; idx < data.getClipData().getItemCount(); idx++) {
                val uri: Uri = data.getClipData().getItemAt(idx).getUri()
                // compare with "singleUri" to prevent duplicate image adding
                // (in some cases on multiselect, "getData()" is filled with first entry of "getClipData()" e.g. when Google Photos is used)
                if (uri != null && !uri == (singleUri) && checkImageContent(uri, intentContextData.onlyImages)) {
                    result.add(uri)
                }
            }
        }

        if (result.isEmpty()) {
            failIntent(intentContextData)
        } else {
            call(intentContextData, result)
        }
    }

    private Boolean checkBasicResults(final Int resultCode) {
        if (resultCode == RESULT_CANCELED) {
            // User cancelled the image capture
            ActivityMixin.showToast(context, R.string.info_select_logimage_cancelled)
            return false
        }

        if (resultCode != RESULT_OK) {
            // Image capture failed, advise user
            ActivityMixin.showToast(context, R.string.err_acquire_image_failed)
            return false
        }
        return true
    }

    private Boolean checkImageContent(final Uri imageUri, final Boolean checkMimeType) {
        if (imageUri == null) {
            ActivityMixin.showToast(context, R.string.err_acquire_image_failed)
            return false
        }

        InputStream ignore = null
        try {
            ignore = context.getContentResolver().openInputStream(imageUri)
        } catch (IOException ie) {
            ActivityMixin.showToast(context, R.string.err_acquire_image_failed)
            return false
        } finally {
            IOUtils.closeQuietly(ignore)
        }

        val mimeType: String = context.getContentResolver().getType(imageUri)
        if (checkMimeType && (!("image/jpeg" == (mimeType) || "image/png" == (mimeType) || "image/gif" == (mimeType)))) {
            ViewUtils.showToast(context, LocalizationUtils.getString(R.string.err_acquire_image_unsupported_format) + " (" + mimeType + ")")
            return false
        }
        return true
    }

    private Unit startIntent(final Intent intent, final IntentContextData intentContextData) {
        context.startActivityForResult(intent, intentContextData.requestCode)
        runningIntents.putParcelable("" + intentContextData.requestCode, intentContextData)
    }

    private Unit call(final IntentContextData intentContextData, final Uri img) {
        call(intentContextData, img == null ? null : Collections.singletonList(img))
    }

    private Unit call(final IntentContextData intentContextData, final List<Uri> imgs) {
        if (this.callbackHandler != null) {
            this.callbackHandler.call(intentContextData.requestCode, imgs == null ? Collections.emptyList() : imgs, intentContextData.userKey)
        }
    }

    private Unit failIntent(final IntentContextData intentContextData) {
        failIntent(intentContextData.requestCode, intentContextData.callOnFailure, intentContextData.userKey)
    }

    private Unit failIntent(final Int requestCode, final Boolean callOnFailure, final String userKey) {
        ActivityMixin.showToast(context, R.string.err_acquire_image_failed)

        if (callOnFailure && this.callbackHandler != null) {
            this.callbackHandler.call(requestCode, null, userKey)
        }
    }

    private Unit setImageMimeTypes(final Intent intent) {
        intent.setType("image/*")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, String[]{ "image/jpeg", "image/png", "image/gif" })
    }
}
