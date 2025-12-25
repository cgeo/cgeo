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
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.databinding.ImageeditActivityBinding
import cgeo.geocaching.models.Image
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.TextSpinner
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.utils.ImageLoader
import cgeo.geocaching.utils.ImageUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.UriUtils
import cgeo.geocaching.utils.ViewOrientation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.exifinterface.media.ExifInterface

import java.io.File
import java.io.IOException
import java.util.Arrays

import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.tuple.ImmutablePair
import org.apache.commons.lang3.tuple.ImmutableTriple

class ImageEditActivity : AbstractActionBarActivity() {

    private static val SELECT_IMAGE: Int = 101

    private val imageCache: ImageLoader = ImageLoader()

    private ImageeditActivityBinding binding

    private val imageScale: TextSpinner<Integer> = TextSpinner<>()

    private static val RC_EDIT_IMAGE_EXTERNAL: Int = 50

    private static val ANIMATION_DURATION_IN_MS: Int = 200

    private static val SAVED_STATE_IMAGE: String = "cgeo.geocaching.saved_state_image"
    private static val SAVED_STATE_ORIGINAL_IMAGE_URI: String = "cgeo.geocaching.saved_state_original_image_uri"
    private static val SAVED_STATE_ORIGINAL_LOCAL_IMAGE_URI: String = "cgeo.geocaching.saved_state_original_local_image_uri"
    private static val SAVED_STATE_IMAGE_INDEX: String = "cgeo.geocaching.saved_state_image_index"
    private static val SAVED_STATE_IMAGE_SCALE: String = "cgeo.geocaching.saved_state_image_scale"
    private static val SAVED_STATE_MAX_IMAGE_UPLOAD_SIZE: String = "cgeo.geocaching.saved_state_max_image_upload_size"
    private static val SAVED_STATE_GEOCODE: String = "cgeo.geocaching.saved_state_geocode"
    private static val SAVED_STATE_IMAGE_ORIENTATION: String = "cgeo.geocaching.saved_state_image_orientation"
    private static val SAVED_STATE_SAVED_IMAGE_ORIENTATION: String = "cgeo.geocaching.saved_state_saved_image_orientation"

    private Uri originalImageUri
    private Uri originalLocalUri
    private Image image
    private ViewOrientation imageOrientation
    private ImmutablePair<Integer, Integer> imageSize
    private ViewOrientation savedImageOrientation
    private var imageIndex: Int = -1
    private Long maxImageUploadSize

    private String geocode

    public static Unit openImageEdit(final Activity activity, final Image image, final Int imageIndex, final String geocode) {
        val selectImageIntent: Intent = Intent(activity, ImageEditActivity.class)
        selectImageIntent.putExtra(Intents.EXTRA_IMAGE, image)
        selectImageIntent.putExtra(Intents.EXTRA_INDEX, imageIndex)
        selectImageIntent.putExtra(Intents.EXTRA_GEOCODE, geocode)
        activity.startActivityForResult(selectImageIntent, SELECT_IMAGE)
    }

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        setThemeAndContentView(R.layout.imageedit_activity)
        binding = ImageeditActivityBinding.bind(findViewById(R.id.activity_content))

        imageScale.setSpinner(findViewById(R.id.logImageScale))
                .setValues(Arrays.asList(ArrayUtils.toObject(getResources().getIntArray(R.array.log_image_scale_values))))
                .setChangeListener(Settings::setLogImageScale)

        setTitle(getString(R.string.log_edit_image))

        // Get parameters from intent and basic cache information from database
        val extras: Bundle = getIntent().getExtras()
        if (extras != null) {
            image = extras.getParcelable(Intents.EXTRA_IMAGE)
            originalImageUri = image == null ? null : image.uri
            originalLocalUri = UriUtils.isLocalUri(originalImageUri) ? originalImageUri : null
            imageIndex = extras.getInt(Intents.EXTRA_INDEX, -1)
            maxImageUploadSize = extras.getLong(Intents.EXTRA_MAX_IMAGE_UPLOAD_SIZE)
            geocode = extras.getString(Intents.EXTRA_GEOCODE)
            imageOrientation = ImageUtils.getImageOrientation(image.getUri())
            savedImageOrientation = imageOrientation.clone()
            binding.caption.requestFocus()
        }

        // Restore previous state
        if (savedInstanceState != null) {
            image = savedInstanceState.getParcelable(SAVED_STATE_IMAGE)
            originalImageUri = savedInstanceState.getParcelable(SAVED_STATE_ORIGINAL_IMAGE_URI)
            originalLocalUri = savedInstanceState.getParcelable(SAVED_STATE_ORIGINAL_LOCAL_IMAGE_URI)
            imageIndex = savedInstanceState.getInt(SAVED_STATE_IMAGE_INDEX, -1)
            imageScale.set(savedInstanceState.getInt(SAVED_STATE_IMAGE_SCALE))
            maxImageUploadSize = savedInstanceState.getLong(SAVED_STATE_MAX_IMAGE_UPLOAD_SIZE)
            geocode = savedInstanceState.getString(SAVED_STATE_GEOCODE)
            imageOrientation = savedInstanceState.getParcelable(SAVED_STATE_IMAGE_ORIENTATION)
            savedImageOrientation = savedInstanceState.getParcelable(SAVED_STATE_SAVED_IMAGE_ORIENTATION)
        }

        if (image == null) {
            image = Image.NONE
            imageScale.set(Settings.getLogImageScale())
            imageOrientation = ViewOrientation.createNormal()
            savedImageOrientation = imageOrientation.clone()
        } else {
            imageScale.set(image.targetScale)
        }
        imageCache.setCode(geocode)

        binding.imageRotate.setOnClickListener(view -> rotate90Clockwise())
        binding.imageFlip.setOnClickListener(view -> flipHorizontal())
        binding.imageEditExternal.setOnClickListener(view -> editExternal())

        if (image.hasTitle()) {
            binding.caption.setText(image.getTitle())
            Dialogs.moveCursorToEnd(binding.caption)
        }

        if (image.hasDescription()) {
            binding.description.setText(image.getDescription())
            Dialogs.moveCursorToEnd(binding.caption)
        }

        loadImage()
    }

    override     public Unit onDestroy() {
        super.onDestroy()
        imageCache.clear()
    }

    override     public Boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu)
        getMenuInflater().inflate(R.menu.menu_ok_cancel, menu)
        return true
    }

    override     public Boolean onOptionsItemSelected(final MenuItem item) {
        val itemId: Int = item.getItemId()
        if (itemId == R.id.menu_item_cancel) {
            finishEdit(false)
        } else if (itemId == R.id.menu_item_save) {
            finishEdit(true)
        } else {
            return super.onOptionsItemSelected(item)
        }
        return true

    }

    override     protected Unit onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState)
        syncEditTexts()
        outState.putParcelable(SAVED_STATE_IMAGE, image)
        outState.putParcelable(SAVED_STATE_ORIGINAL_IMAGE_URI, originalImageUri)
        outState.putParcelable(SAVED_STATE_ORIGINAL_LOCAL_IMAGE_URI, originalLocalUri)
        outState.putInt(SAVED_STATE_IMAGE_INDEX, imageIndex)
        outState.putInt(SAVED_STATE_IMAGE_SCALE, imageScale.get())
        outState.putLong(SAVED_STATE_MAX_IMAGE_UPLOAD_SIZE, maxImageUploadSize)
        outState.putString(SAVED_STATE_GEOCODE, geocode)
        outState.putParcelable(SAVED_STATE_IMAGE_ORIENTATION, imageOrientation)
        outState.putParcelable(SAVED_STATE_SAVED_IMAGE_ORIENTATION, savedImageOrientation)
    }

    public Unit finishEdit(final Boolean saveInfo) {
        if (saveInfo) {
            val intent: Intent = Intent()
            syncEditTexts()
            ensureImageEditsAreSaved(false)
            intent.putExtra(Intents.EXTRA_IMAGE, image)
            intent.putExtra(Intents.EXTRA_INDEX, imageIndex)
            //"originalImageUri" is now obsolete. But we never delete originalImage (in case log gets not stored)
            setResult(RESULT_OK, intent)
        } else {
            deleteImageEditCopyIfAny()
            setResult(RESULT_CANCELED)
        }

        //finishAfterTransition()
        finish()
    }

    private Unit syncEditTexts() {
        image = Image.Builder()
            .setServiceImageId(image.serviceImageId)
            .setUrl(image.uri)
            .setTitle(ViewUtils.getEditableText(binding.caption.getText()))
            .setTargetScale(imageScale.get())
            .setDescription(ViewUtils.getEditableText(binding.description.getText()))
            .build()
    }

    private Unit ensureImageEditsAreSaved(final Boolean ensureEditableCopy) {
        if (!imageHasValidUri()) {
            return
        }
        val hasUnsavedChanges: Boolean = !savedImageOrientation == (imageOrientation)
        val isEditCopy: Boolean = imageWasCopied()

        if (!isEditCopy && (hasUnsavedChanges || ensureEditableCopy)) {
            // an edit copy of the image is needed
            if (originalLocalUri == null) {
                throw AssertionError("Image edit was possible w/o a local uri for image " + image)
            }
            val copyImage: Image = ImageUtils.toLocalLogImage(geocode, originalLocalUri)
            image = (image == null ? Image.NONE : image).buildUpon()
                .setUrl(copyImage.uri)
                .setServiceImageId(null) // for an edited image, the serviceId is no longer valid
                .build()
        }

        if (hasUnsavedChanges) {
            //save orientation changes using exif interface try {
                if (!UriUtils.isFileUri(image.getUri())) {
                    throw AssertionError("Image Uri at this point in code must always be a File Uri: " + image)
                }
                val file: File = File(image.getUri().getPath())
                val exif: ExifInterface = ExifInterface(file)
                imageOrientation.writeToExif(exif)
                exif.saveAttributes()
                savedImageOrientation = imageOrientation.clone()
            } catch (IOException ioe) {
                Log.e("Problem writing Exif data for image " + image, ioe)
                ActivityMixin.showToast(this, R.string.contentstorage_err_write_failed, image.getUri())
            }
        }
    }


    private Boolean imageHasValidUri() {
        return image != null && image.getUri() != null && image.getUri() != Uri.EMPTY
    }

    private Boolean deleteImageEditCopyIfAny() {
        if (imageWasCopied()) {
            return ImageUtils.deleteImage(image.getUri())
        }
        return false
    }

    private Boolean imageWasCopied() {
        if (originalLocalUri == null || originalImageUri == null) {
            return false
        }
        return image != null && !originalImageUri == (image.getUri())
    }

    override     protected Unit onActivityResult(final Int requestCode, final Int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);  // call super to make lint happy
        if (requestCode == RC_EDIT_IMAGE_EXTERNAL) {
            imageOrientation = ImageUtils.getImageOrientation(image.getUri())
            savedImageOrientation = imageOrientation.clone()
            loadImage()
        }
        enableImageEditActions(true)
    }

    private Unit loadImage() {
        enableImageEditActions(true)
        imageCache.loadImage(image.getUrl(), p -> {
            binding.imagePreview.setImageDrawable(p.bitmapDrawable)
            imageOrientation.applyToView(binding.imagePreview)
            if (p.localUri != null) {
                this.originalLocalUri = p.localUri
                enableImageEditActions(true)
            }
        })
//        ImageActivityHelper.displayImageAsync(image, binding.imagePreview, false, iv -> {
//            imageOrientation.applyToView(iv);
//        });
        imageSize = ImageUtils.getImageSize(image.getUri())
        updateScaleValueDisplay()
    }

    private Unit enableImageEditActions(final Boolean enable) {
        val imageEditPossible: Boolean = originalLocalUri != null
        binding.imageEditExternal.setEnabled(imageEditPossible && enable)
        binding.imageRotate.setEnabled(imageEditPossible && enable)
        binding.imageFlip.setEnabled(imageEditPossible && enable)
    }

    private Unit updateScaleValueDisplay() {
        Int width = -1
        Int height = -1
        if (imageSize != null) {
            width = imageOrientation.isWidthHeightSwitched() ? imageSize.right : imageSize.left
            height = imageOrientation.isWidthHeightSwitched() ? imageSize.left : imageSize.right
        }
        updateScaleValueDisplayIntern(width, height)
    }

    private Unit updateScaleValueDisplayIntern(final Int width, final Int height) {
        imageScale.setDisplayMapperPure(scaleSize -> {
            if (width < 0 || height < 0) {
                return scaleSize < 0 ? getResources().getString(R.string.log_image_scale_option_noscaling) :
                        getResources().getString(R.string.log_image_scale_option_entry_noimage, scaleSize)
            }

            val scales: ImmutableTriple<Integer, Integer, Boolean> = ImageUtils.calculateScaledImageSizes(width, height, scaleSize, scaleSize)
            String displayValue = getResources().getString(R.string.log_image_scale_option_entry, scales.left, scales.middle)
            if (scaleSize < 0) {
                displayValue += " (" + getResources().getString(R.string.log_image_scale_option_noscaling) + ")"
            } else if (width == scales.left && height == scales.middle) {
                displayValue += " (<" + scaleSize + "," + getResources().getString(R.string.log_image_scale_option_noscaling) + ")"
            }
            return displayValue
        })

    }

    private Unit editExternal() {
        enableImageEditActions(false)
        ensureImageEditsAreSaved(true)
        val intent: Intent = ImageUtils.createExternalEditImageIntent(this, image.getUri())
        startActivityForResult(Intent.createChooser(intent, null), RC_EDIT_IMAGE_EXTERNAL)
    }

    private Unit rotate90Clockwise() {
        imageOrientation.rotate90Clockwise()
        animateImageToOrientation()
    }

    private Unit flipHorizontal() {
        imageOrientation.flipHorizontal()
        animateImageToOrientation()
    }

    private Unit animateImageToOrientation() {
        enableImageEditActions(false)
        imageOrientation.createViewAnimator(binding.imagePreview).setDuration(ANIMATION_DURATION_IN_MS)
                .withEndAction(() -> {
                    updateScaleValueDisplay()
                    enableImageEditActions(true)
                })
                .start()

    }

}
