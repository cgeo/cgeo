package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActionBarActivity;
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.Arrays;


import butterknife.BindView;
import butterknife.ButterKnife;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class ImageSelectActivity extends AbstractActionBarActivity {

    @BindView(R.id.caption) protected EditText captionView;
    @BindView(R.id.description) protected EditText descriptionView;
    @BindView(R.id.camera) protected Button cameraButton;
    @BindView(R.id.stored) protected Button storedButton;
    @BindView(R.id.save) protected Button saveButton;
    @BindView(R.id.delete) protected Button deleteButton;
    @BindView(R.id.cancel) protected Button clearButton;
    @BindView(R.id.image_preview) protected ImageView imagePreview;

    private final TextSpinner<Integer> imageScale = new TextSpinner<>();

    private static final String SAVED_STATE_IMAGE = "cgeo.geocaching.saved_state_image";
    private static final String SAVED_STATE_ORIGINAL_IMAGE = "cgeo.geocaching.saved_state_original_image";
    private static final String SAVED_STATE_IMAGE_INDEX = "cgeo.geocaching.saved_state_image_index";
    private static final String SAVED_STATE_IMAGE_SCALE = "cgeo.geocaching.saved_state_image_scale";
    private static final String SAVED_STATE_MAX_IMAGE_UPLOAD_SIZE = "cgeo.geocaching.saved_state_max_image_upload_size";
    private static final String SAVED_STATE_IMAGE_CAPTION_MANDATORY = "cgeo.geocaching.saved_state_image_caption_mandatory";

    private final ImageActivityHelper imageActivityHelper = new ImageActivityHelper(this, 1);

    private Image originalImage;
    private Image image;
    private int imageIndex = -1;
    private long maxImageUploadSize;
    private boolean imageCaptionMandatory;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        onCreate(savedInstanceState, R.layout.imageselect_activity);
        ButterKnife.bind(this);

        imageScale.setSpinner(findViewById(R.id.logImageScale))
                .setDisplayMapper(scaleSize -> scaleSize < 0 ? getResources().getString(R.string.log_image_scale_option_noscaling) : getResources().getString(R.string.log_image_scale_option_entry, scaleSize))
                .setValues(Arrays.asList(ArrayUtils.toObject(getResources().getIntArray(R.array.log_image_scale_values))))
                .set(Settings.getLogImageScale())
                .setChangeListener(Settings::setLogImageScale);

        // Get parameters from intent and basic cache information from database
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            image = extras.getParcelable(Intents.EXTRA_IMAGE);
            originalImage = image;
            imageIndex = extras.getInt(Intents.EXTRA_INDEX, -1);
            maxImageUploadSize = extras.getLong(Intents.EXTRA_MAX_IMAGE_UPLOAD_SIZE);
            imageCaptionMandatory = extras.getBoolean(Intents.EXTRA_IMAGE_CAPTION_MANDATORY);

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
        }

        if (image == null) {
            image = Image.NONE;
        }

        cameraButton.setOnClickListener(view -> selectImageFromCamera());
        storedButton.setOnClickListener(view -> selectImageFromStorage());

        if (image.hasTitle()) {
            captionView.setText(image.getTitle());
            Dialogs.moveCursorToEnd(captionView);
        }

        if (image.hasDescription()) {
            descriptionView.setText(image.getDescription());
            Dialogs.moveCursorToEnd(captionView);
        }

        saveButton.setOnClickListener(v -> saveImageInfo(true, false));
        clearButton.setOnClickListener(v -> saveImageInfo(false, false));
        deleteButton.setOnClickListener(v -> saveImageInfo(false, true));
        deleteButton.setVisibility(imageIndex >= 0 ? View.VISIBLE : View.GONE);

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
    }

    public void saveImageInfo(final boolean saveInfo, final boolean deleteImage) {
        if (saveInfo) {
            new AsyncTask<Void, Void, ImageUtils.ScaleImageResult>() {
                @Override
                protected ImageUtils.ScaleImageResult doInBackground(final Void... params) {
                    final int maxXY = imageScale.get();
                    if (image.getPath() == null) {
                        return null;
                    }
                    return ImageUtils.readScaleAndWriteImage(image.getPath(), maxXY, true, true);
                }

                @Override
                protected void onPostExecute(final ImageUtils.ScaleImageResult scaleImageResult) {
                    if (scaleImageResult != null) {
                        image = new Image.Builder().setUrl(scaleImageResult.getFilename()).build();

                        final File imageFile = image.getFile();
                        if (imageFile == null) {
                            showToast(res.getString(R.string.err_select_logimage_failed));
                            return;
                        }
                        if (maxImageUploadSize > 0 && imageFile.length() > maxImageUploadSize) {
                            showToast(res.getString(R.string.err_select_logimage_upload_size));
                            return;
                        }

                        if (imageCaptionMandatory && StringUtils.isBlank(captionView.getText())) {
                            showToast(res.getString(R.string.err_logimage_caption_required));
                            return;
                        }

                        final Intent intent = new Intent();
                        syncEditTexts();
                        intent.putExtra(Intents.EXTRA_IMAGE, image);
                        intent.putExtra(Intents.EXTRA_INDEX, imageIndex);
                        intent.putExtra(Intents.EXTRA_SCALE, imageScale.get());
                        //"originalImage" is now obsolete. But we never delete originalImage (in case log gets not stored)
                        setResult(RESULT_OK, intent);
                    } else {
                        showToast(res.getString(R.string.err_select_logimage_failed));
                        setResult(RESULT_CANCELED);
                    }
                    finish();
                }
            }.execute();
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
                .setTitle(captionView.getText().toString())
                .setDescription(descriptionView.getText().toString())
                .build();
    }

    private void selectImageFromCamera() {

        imageActivityHelper.getImageFromCamera(-1, false, false, img -> {
            deleteImageFromDeviceIfNotOriginal(image);
            image = img;
            loadImagePreview();
        });
    }

    private void selectImageFromStorage() {
        imageActivityHelper.getImageFromStorage(-1, false, false,  img -> {
            deleteImageFromDeviceIfNotOriginal(image);
            image = img;
            loadImagePreview();
        });
    }

    private boolean deleteImageFromDeviceIfNotOriginal(final Image img) {
        if (img != null &&  !img.getPath().isEmpty() && (originalImage == null || !img.getPath().equals(originalImage.getPath()))) {
            return new File(img.getPath()).delete();
        }
        return false;
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);  // call super to make lint happy
        imageActivityHelper.onActivityResult(requestCode, resultCode, data);
   }

    private void loadImagePreview() {
        ImageActivityHelper.displayImageAsync(image, imagePreview);
    }
}
