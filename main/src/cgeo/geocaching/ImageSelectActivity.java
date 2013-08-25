package cgeo.geocaching;

import butterknife.InjectView;
import butterknife.Views;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.compatibility.Compatibility;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageSelectActivity extends AbstractActivity {

    @InjectView(R.id.caption) protected EditText captionView;
    @InjectView(R.id.description) protected EditText descriptionView;
    @InjectView(R.id.logImageScale) protected Spinner scaleView;
    @InjectView(R.id.camera) protected Button cameraButton;
    @InjectView(R.id.stored) protected Button storedButton;
    @InjectView(R.id.save) protected Button saveButton;
    @InjectView(R.id.cancel) protected Button clearButton;
    @InjectView(R.id.image_preview) protected ImageView imagePreview;

    static final String EXTRAS_CAPTION = "caption";
    static final String EXTRAS_DESCRIPTION = "description";
    static final String EXTRAS_URI_AS_STRING = "uri";
    static final String EXTRAS_SCALE = "scale";

    private static final String SAVED_STATE_IMAGE_CAPTION = "cgeo.geocaching.saved_state_image_caption";
    private static final String SAVED_STATE_IMAGE_DESCRIPTION = "cgeo.geocaching.saved_state_image_description";
    private static final String SAVED_STATE_IMAGE_URI = "cgeo.geocaching.saved_state_image_uri";
    private static final String SAVED_STATE_IMAGE_SCALE = "cgeo.geocaching.saved_state_image_scale";

    private static final int SELECT_NEW_IMAGE = 1;
    private static final int SELECT_STORED_IMAGE = 2;

    // Data to be saved while reconfiguring
    private String imageCaption;
    private String imageDescription;
    private int scaleChoiceIndex;
    private Uri imageUri;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.imageselect_activity);
        Views.inject(this);

        scaleChoiceIndex = Settings.getLogImageScale();
        imageCaption = "";
        imageDescription = "";
        imageUri = Uri.EMPTY;

        // Get parameters from intent and basic cache information from database
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            imageCaption = extras.getString(EXTRAS_CAPTION);
            imageDescription = extras.getString(EXTRAS_DESCRIPTION);
            imageUri = Uri.parse(extras.getString(EXTRAS_URI_AS_STRING));
            scaleChoiceIndex = extras.getInt(EXTRAS_SCALE, scaleChoiceIndex);
        }

        // Restore previous state
        if (savedInstanceState != null) {
            imageCaption = savedInstanceState.getString(SAVED_STATE_IMAGE_CAPTION);
            imageDescription = savedInstanceState.getString(SAVED_STATE_IMAGE_DESCRIPTION);
            imageUri = Uri.parse(savedInstanceState.getString(SAVED_STATE_IMAGE_URI));
            scaleChoiceIndex = savedInstanceState.getInt(SAVED_STATE_IMAGE_SCALE);
        }

        cameraButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                selectImageFromCamera();
            }
        });

        storedButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                selectImageFromStorage();
            }
        });

        if (StringUtils.isNotBlank(imageCaption)) {
            captionView.setText(imageCaption);
        }

        if (StringUtils.isNotBlank(imageDescription)) {
            descriptionView.setText(imageDescription);
        }

        scaleView.setSelection(scaleChoiceIndex);
        scaleView.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                scaleChoiceIndex = scaleView.getSelectedItemPosition();
                Settings.setLogImageScale(scaleChoiceIndex);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                saveImageInfo(true);
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                saveImageInfo(false);
            }
        });

        loadImagePreview();
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        syncEditTexts();
        outState.putString(SAVED_STATE_IMAGE_CAPTION, imageCaption);
        outState.putString(SAVED_STATE_IMAGE_DESCRIPTION, imageDescription);
        outState.putString(SAVED_STATE_IMAGE_URI, imageUri != null ? imageUri.getPath() : StringUtils.EMPTY);
        outState.putInt(SAVED_STATE_IMAGE_SCALE, scaleChoiceIndex);
    }

    public void saveImageInfo(boolean saveInfo) {
        if (saveInfo) {
            final String filename = writeScaledImage(imageUri.getPath());
            if (filename != null) {
                imageUri = Uri.parse(filename);
                final Intent intent = new Intent();
                syncEditTexts();
                intent.putExtra(EXTRAS_CAPTION, imageCaption);
                intent.putExtra(EXTRAS_DESCRIPTION, imageDescription);
                intent.putExtra(EXTRAS_URI_AS_STRING, imageUri.toString());
                intent.putExtra(EXTRAS_SCALE, scaleChoiceIndex);
                setResult(RESULT_OK, intent);
            } else {
                showToast(res.getString(R.string.err_select_logimage_failed));
                setResult(RESULT_CANCELED);
            }
        } else {
            setResult(RESULT_CANCELED);
        }

        finish();
    }

    private void syncEditTexts() {
        imageCaption = captionView.getText().toString();
        imageDescription = descriptionView.getText().toString();
        scaleChoiceIndex = scaleView.getSelectedItemPosition();
    }

    private void selectImageFromCamera() {
        // create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        imageUri = getOutputImageFileUri(); // create a file to save the image
        if (imageUri == null) {
            showFailure();
            return;
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri); // set the image file name

        // start the image capture Intent
        startActivityForResult(intent, SELECT_NEW_IMAGE);
    }

    private void selectImageFromStorage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/jpeg");

        startActivityForResult(Intent.createChooser(intent, "Select Image"), SELECT_STORED_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED) {
            // User cancelled the image capture
            showToast(getResources().getString(R.string.info_select_logimage_cancelled));
            return;
        }

        if (resultCode != RESULT_OK) {
            // Image capture failed, advise user
            showFailure();
            return;
        }

        if (data != null) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaColumns.DATA };

            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                if (cursor == null) {
                    showFailure();
                    return;
                }
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String filePath = cursor.getString(columnIndex);
                if (StringUtils.isBlank(filePath)) {
                    showFailure();
                    return;
                }
                imageUri = Uri.parse(filePath);
            } catch (Exception e) {
                Log.e("ImageSelectActivity.onActivityResult", e);
                showFailure();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            Log.d("SELECT IMAGE data = " + data.toString());
        } else {
            Log.d("SELECT IMAGE data is null");
        }

        if (requestCode == SELECT_NEW_IMAGE) {
            showToast(getResources().getString(R.string.info_stored_image) + "\n" + imageUri);
        }

        loadImagePreview();
    }

    /**
     * Scales and writes the scaled image.
     *
     * @param filePath
     * @return the scaled image path, or <tt>null</tt> if the image cannot be decoded
     */
    private String writeScaledImage(final String filePath) {
        scaleChoiceIndex = scaleView.getSelectedItemPosition();
        final int maxXY = getResources().getIntArray(R.array.log_image_scale_values)[scaleChoiceIndex];
        if (maxXY == 0) {
            return filePath;
        }
        BitmapFactory.Options sizeOnlyOptions = new BitmapFactory.Options();
        sizeOnlyOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, sizeOnlyOptions);
        final int myMaxXY = Math.max(sizeOnlyOptions.outHeight, sizeOnlyOptions.outWidth);
        final int sampleSize = myMaxXY / maxXY;
        Bitmap image;
        if (sampleSize > 1) {
            BitmapFactory.Options sampleOptions = new BitmapFactory.Options();
            sampleOptions.inSampleSize = sampleSize;
            image = BitmapFactory.decodeFile(filePath, sampleOptions);
        } else {
            image = BitmapFactory.decodeFile(filePath);
        }
        // If image decoding fail, return null
        if (image == null) {
            return null;
        }
        final BitmapDrawable scaledImage = ImageUtils.scaleBitmapTo(image, maxXY, maxXY);
        final String uploadFilename = getOutputImageFile().getPath();
        ImageUtils.storeBitmap(scaledImage.getBitmap(), Bitmap.CompressFormat.JPEG, 75, uploadFilename);
        return uploadFilename;
    }

    private void showFailure() {
        showToast(getResources().getString(R.string.err_acquire_image_failed));
    }

    private void loadImagePreview() {
        if (imageUri == null) {
            return;
        }
        if (!new File(imageUri.getPath()).exists()) {
            Log.i("Image does not exist");
            return;
        }

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = 8;
        final Bitmap bitmap = BitmapFactory.decodeFile(imageUri.getPath(), bitmapOptions);
        imagePreview.setImageBitmap(bitmap);
        imagePreview.setVisibility(View.VISIBLE);
    }

    private static Uri getOutputImageFileUri() {
        final File file = getOutputImageFile();
        if (file == null) {
            return null;
        }
        return Uri.fromFile(file);
    }

    /** Create a File for saving an image or video */
    private static File getOutputImageFile() {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Compatibility.getExternalPictureDir(), "cgeo");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!FileUtils.mkdirs(mediaStorageDir)) {
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
    }
}
