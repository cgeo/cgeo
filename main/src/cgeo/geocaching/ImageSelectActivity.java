package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.databinding.ImageselectActivityBinding;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.ImageActivityHelper;
import cgeo.geocaching.ui.TextSpinner;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.ImageUtils;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;

public class ImageSelectActivity extends AbstractActionBarActivity {
    private ImageselectActivityBinding binding;

    private final TextSpinner<Integer> imageScale = new TextSpinner<>();

    private static final String SAVED_STATE_IMAGE = "cgeo.geocaching.saved_state_image";
    private static final String SAVED_STATE_ORIGINAL_IMAGE = "cgeo.geocaching.saved_state_original_image";
    private static final String SAVED_STATE_IMAGE_INDEX = "cgeo.geocaching.saved_state_image_index";
    private static final String SAVED_STATE_IMAGE_SCALE = "cgeo.geocaching.saved_state_image_scale";
    private static final String SAVED_STATE_MAX_IMAGE_UPLOAD_SIZE = "cgeo.geocaching.saved_state_max_image_upload_size";
    private static final String SAVED_STATE_IMAGE_CAPTION_MANDATORY = "cgeo.geocaching.saved_state_image_caption_mandatory";
    private static final String SAVED_STATE_GEOCODE = "cgeo.geocaching.saved_state_geocode";
    private static final String SAVED_STATE_IMAGEHELPER = "cgeo.geocaching.saved_state_imagehelper";


    private Image originalImage;
    private Image image;
    private int imageIndex = -1;
    private long maxImageUploadSize;
    private boolean imageCaptionMandatory;
    @Nullable private String geocode;

    private final ImageActivityHelper imageActivityHelper = new ImageActivityHelper(this, (rc, imgs, uk) -> {
        deleteImageFromDeviceIfNotOriginal(image);
        image = imgs.isEmpty() ? null : ImageUtils.toLocalLogImage(geocode, imgs.get(0));
        loadImagePreview();
    });

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeAndContentView(R.layout.imageselect_activity);
        binding = ImageselectActivityBinding.bind(findViewById(R.id.imageselect_activity_viewroot));

        imageScale.setSpinner(findViewById(R.id.logImageScale))
                .setValues(Arrays.asList(ArrayUtils.toObject(getResources().getIntArray(R.array.log_image_scale_values))))
                .setChangeListener(Settings::setLogImageScale);

        // Get parameters from intent and basic cache information from database
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            image = extras.getParcelable(Intents.EXTRA_IMAGE);
            originalImage = image;
            imageIndex = extras.getInt(Intents.EXTRA_INDEX, -1);
            maxImageUploadSize = extras.getLong(Intents.EXTRA_MAX_IMAGE_UPLOAD_SIZE);
            imageCaptionMandatory = extras.getBoolean(Intents.EXTRA_IMAGE_CAPTION_MANDATORY);
            geocode = extras.getString(Intents.EXTRA_GEOCODE);

            //try to find a good title from what we got
            final String context = extras.getString(Intents.EXTRA_GEOCODE);
            if (StringUtils.isBlank(context)) {
                setTitle(getString(R.string.cache_image));
            } else {
                final Geocache cache = DataStore.loadCache(context, LoadFlags.LOAD_CACHE_OR_DB);
                if (cache != null) {
                    setCacheTitleBar(cache);
                } else {
                    setTitle(context + ": " + getString(R.string.cache_image));
                }
            }
        }

        // Restore previous state
        if (savedInstanceState != null) {
            image = savedInstanceState.getParcelable(SAVED_STATE_IMAGE);
            originalImage = savedInstanceState.getParcelable(SAVED_STATE_ORIGINAL_IMAGE);
            imageIndex = savedInstanceState.getInt(SAVED_STATE_IMAGE_INDEX, -1);
            imageScale.set(savedInstanceState.getInt(SAVED_STATE_IMAGE_SCALE));
            maxImageUploadSize = savedInstanceState.getLong(SAVED_STATE_MAX_IMAGE_UPLOAD_SIZE);
            imageCaptionMandatory = savedInstanceState.getBoolean(SAVED_STATE_IMAGE_CAPTION_MANDATORY);
            geocode = savedInstanceState.getString(SAVED_STATE_GEOCODE);
            imageActivityHelper.setState(savedInstanceState.getBundle(SAVED_STATE_IMAGEHELPER));
        }

        if (image == null) {
            image = Image.NONE;
            imageScale.set(Settings.getLogImageScale());
        } else {
            imageScale.set(image.targetScale);
        }
        updateScaleValueDisplay();

        binding.camera.setOnClickListener(view -> selectImageFromCamera());
        binding.stored.setOnClickListener(view -> selectImageFromStorage());

        if (image.hasTitle()) {
            binding.caption.setText(image.getTitle());
            Dialogs.moveCursorToEnd(binding.caption);
        }

        if (image.hasDescription()) {
            binding.description.setText(image.getDescription());
            Dialogs.moveCursorToEnd(binding.caption);
        }

        binding.save.setOnClickListener(v -> saveImageInfo(true, false));
        binding.cancel.setOnClickListener(v -> saveImageInfo(false, false));
        binding.delete.setOnClickListener(v -> saveImageInfo(false, true));
        binding.delete.setVisibility(imageIndex >= 0 ? View.VISIBLE : View.GONE);

        loadImagePreview();
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        syncEditTexts();
        outState.putParcelable(SAVED_STATE_IMAGE, image);
        outState.putParcelable(SAVED_STATE_ORIGINAL_IMAGE, originalImage);
        outState.putInt(SAVED_STATE_IMAGE_INDEX, imageIndex);
        outState.putInt(SAVED_STATE_IMAGE_SCALE, imageScale.get());
        outState.putLong(SAVED_STATE_MAX_IMAGE_UPLOAD_SIZE, maxImageUploadSize);
        outState.putBoolean(SAVED_STATE_IMAGE_CAPTION_MANDATORY, imageCaptionMandatory);
        outState.putString(SAVED_STATE_GEOCODE, geocode);
        outState.putBundle(SAVED_STATE_IMAGEHELPER, imageActivityHelper.getState());
    }

    public void saveImageInfo(final boolean saveInfo, final boolean deleteImage) {
        if (saveInfo) {

            final Intent intent = new Intent();
            syncEditTexts();
            intent.putExtra(Intents.EXTRA_IMAGE, image);
            intent.putExtra(Intents.EXTRA_INDEX, imageIndex);
            //"originalImage" is now obsolete. But we never delete originalImage (in case log gets not stored)
            setResult(RESULT_OK, intent);
            finish();
        } else if (deleteImage) {
            final Intent intent = new Intent();
            intent.putExtra(Intents.EXTRA_DELETE_FLAG, true);
            intent.putExtra(Intents.EXTRA_INDEX, imageIndex);
            setResult(RESULT_OK, intent);
            deleteImageFromDeviceIfNotOriginal(image);
            //original image is now obsolete. BUt we never delete original Image (in case log gets not stored)
            finish();
        } else {
            deleteImageFromDeviceIfNotOriginal(image);
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private void syncEditTexts() {
        image = new Image.Builder()
                .setUrl(image.uri)
                .setTitle(binding.caption.getText().toString())
                .setTargetScale(imageScale.get())
                .setDescription(binding.description.getText().toString())
                .build();
    }

    private void selectImageFromCamera() {
        imageActivityHelper.getImageFromCamera(this.geocode, false, null);
    }

    private void selectImageFromStorage() {
        imageActivityHelper.getImageFromStorage(this.geocode, false, null);
    }

    private boolean deleteImageFromDeviceIfNotOriginal(final Image img) {
        if (img != null && img.getUri() != null && (originalImage == null || !img.getUri().equals(originalImage.getUri()))) {
            return ImageUtils.deleteImage(img.getUri());
        }
        return false;
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);  // call super to make lint happy
        imageActivityHelper.onActivityResult(requestCode, resultCode, data);
    }

    private void loadImagePreview() {
        ImageActivityHelper.displayImageAsync(image, binding.imagePreview);
        updateScaleValueDisplay();
    }

    private void updateScaleValueDisplay() {
        int width = -1;
        int height = -1;
        if (image != null) {
            final ImmutablePair<Integer, Integer> size = ImageUtils.getImageSize(image.getUri());
            if (size != null) {
                width = size.left;
                height = size.right;
            }
        }
        updateScaleValueDisplayIntern(width, height);
    }

    private void updateScaleValueDisplayIntern(final int width, final int height) {
        imageScale.setDisplayMapper(scaleSize -> {
            if (width < 0 || height < 0) {
                return scaleSize < 0 ? getResources().getString(R.string.log_image_scale_option_noscaling) :
                        getResources().getString(R.string.log_image_scale_option_entry_noimage, scaleSize);
            }

            final ImmutableTriple<Integer, Integer, Boolean> scales = ImageUtils.calculateScaledImageSizes(width, height, scaleSize, scaleSize);
            String displayValue = getResources().getString(R.string.log_image_scale_option_entry, scales.left, scales.middle);
            if (scaleSize < 0) {
                displayValue += " (" + getResources().getString(R.string.log_image_scale_option_noscaling) + ")";
            }
            return displayValue;
        });

    }
}
