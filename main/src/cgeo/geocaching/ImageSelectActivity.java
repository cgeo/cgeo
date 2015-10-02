package cgeo.geocaching;

import butterknife.Bind;
import butterknife.ButterKnife;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.Log;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION_CODES;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class ImageSelectActivity extends AbstractActionBarActivity {

    @Bind(R.id.caption) protected EditText captionView;
    @Bind(R.id.description) protected EditText descriptionView;
    @Bind(R.id.logImageScale) protected Spinner scaleView;
    @Bind(R.id.camera) protected Button cameraButton;
    @Bind(R.id.stored) protected Button storedButton;
    @Bind(R.id.save) protected Button saveButton;
    @Bind(R.id.cancel) protected Button clearButton;
    @Bind(R.id.image_preview) protected ImageView imagePreview;

    private static final String SAVED_STATE_IMAGE = "cgeo.geocaching.saved_state_image";
    private static final String SAVED_STATE_IMAGE_SCALE = "cgeo.geocaching.saved_state_image_scale";

    private static final int SELECT_NEW_IMAGE = 1;
    private static final int SELECT_STORED_IMAGE = 2;

    // Data to be saved while reconfiguring
    private Image image;
    private int scaleChoiceIndex;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        onCreate(savedInstanceState, R.layout.imageselect_activity);
        ButterKnife.bind(this);

        scaleChoiceIndex = Settings.getLogImageScale();

        // Get parameters from intent and basic cache information from database
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            image = extras.getParcelable(Intents.EXTRA_IMAGE);
            scaleChoiceIndex = extras.getInt(Intents.EXTRA_SCALE, scaleChoiceIndex);
        }

        // Restore previous state
        if (savedInstanceState != null) {
            image = savedInstanceState.getParcelable(SAVED_STATE_IMAGE);
            scaleChoiceIndex = savedInstanceState.getInt(SAVED_STATE_IMAGE_SCALE);
        }

        if (image == null) {
            image = Image.NONE;
        }

        cameraButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View view) {
                selectImageFromCamera();
            }
        });

        storedButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View view) {
                selectImageFromStorage();
            }
        });

        if (image.hasTitle()) {
            captionView.setText(image.getTitle());
            Dialogs.moveCursorToEnd(captionView);
        }

        if (image.hasDescription()) {
            descriptionView.setText(image.getDescription());
            Dialogs.moveCursorToEnd(captionView);
        }

        scaleView.setSelection(scaleChoiceIndex);
        scaleView.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> arg0, final View arg1, final int arg2, final long arg3) {
                scaleChoiceIndex = scaleView.getSelectedItemPosition();
                Settings.setLogImageScale(scaleChoiceIndex);
            }

            @Override
            public void onNothingSelected(final AdapterView<?> arg0) {
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                saveImageInfo(true);
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                saveImageInfo(false);
            }
        });

        loadImagePreview();
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        syncEditTexts();
        outState.putParcelable(SAVED_STATE_IMAGE, image);
        outState.putInt(SAVED_STATE_IMAGE_SCALE, scaleChoiceIndex);
    }

    public void saveImageInfo(final boolean saveInfo) {
        if (saveInfo) {
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(final Void... params) {
                    return writeScaledImage(image.getPath());
                }

                @Override
                protected void onPostExecute(final String filename) {
                    if (filename != null) {
                        image = new Image.Builder().setUrl(filename).build();
                        final Intent intent = new Intent();
                        syncEditTexts();
                        intent.putExtra(Intents.EXTRA_IMAGE, image);
                        intent.putExtra(Intents.EXTRA_SCALE, scaleChoiceIndex);
                        setResult(RESULT_OK, intent);
                    } else {
                        showToast(res.getString(R.string.err_select_logimage_failed));
                        setResult(RESULT_CANCELED);
                    }
                    finish();
                }
            }.execute();
        } else {
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

        scaleChoiceIndex = scaleView.getSelectedItemPosition();
    }

    private void selectImageFromCamera() {
        // create Intent to take a picture and return control to the calling application
        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        final Uri imageUri = ImageUtils.getOutputImageFileUri();
        if (imageUri == null) {
            showFailure();
            return;
        }
        image = new Image.Builder().setUrl(imageUri).build();

        if (image.isEmpty()) {
            showFailure();
            return;
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image.getUri()); // set the image file name

        // start the image capture Intent
        startActivityForResult(intent, SELECT_NEW_IMAGE);
    }

    private void selectImageFromStorage() {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/jpeg");

        startActivityForResult(Intent.createChooser(intent, "Select Image"), SELECT_STORED_IMAGE);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
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

        // null is an acceptable result if the image has been placed in the imageUri file by the
        // camera application.
        if (data != null) {
            final Uri selectedImage = data.getData();
            // In principal can selectedImage be null
            if (selectedImage != null) {
                if (Build.VERSION.SDK_INT < VERSION_CODES.KITKAT) {
                    final String[] filePathColumn = { MediaColumns.DATA };

                    Cursor cursor = null;
                    try {
                        cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                        if (cursor == null) {
                            showFailure();
                            return;
                        }
                        cursor.moveToFirst();

                        final int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        final String filePath = cursor.getString(columnIndex);
                        if (StringUtils.isBlank(filePath)) {
                            showFailure();
                            return;
                        }
                        image = new Image.Builder().setUrl(filePath).build();
                    } catch (final Exception e) {
                        Log.e("ImageSelectActivity.onActivityResult", e);
                        showFailure();
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }

                    Log.d("SELECT IMAGE data = " + data.toString());
                } else {
                    InputStream input = null;
                    OutputStream output = null;
                    try {
                        input = getContentResolver().openInputStream(selectedImage);
                        final File outputFile = ImageUtils.getOutputImageFile();
                        if (outputFile != null) {
                            output = new FileOutputStream(outputFile);
                            LocalStorage.copy(input, output);
                            image = new Image.Builder().setUrl(outputFile).build();
                        }
                    } catch (final FileNotFoundException e) {
                        Log.e("ImageSelectActivity.onStartResult", e);
                    } finally {
                        IOUtils.closeQuietly(input);
                        IOUtils.closeQuietly(output);
                    }
                }
            } else {
                // Image capture failed, advise user
                showFailure();
                return;
            }
        }

        if (requestCode == SELECT_NEW_IMAGE) {
            showToast(getResources().getString(R.string.info_stored_image) + '\n' + image.getUrl());
        }

        loadImagePreview();
    }

    /**
     * Scales and writes the scaled image.
     *
     * @return the scaled image path, or <tt>null</tt> if the image cannot be decoded
     */
    @Nullable
    private String writeScaledImage(@Nullable final String filePath) {
        if (filePath == null) {
            return null;
        }
        scaleChoiceIndex = scaleView.getSelectedItemPosition();
        final int maxXY = getResources().getIntArray(R.array.log_image_scale_values)[scaleChoiceIndex];
        return ImageUtils.readScaleAndWriteImage(filePath, maxXY);
    }

    private void showFailure() {
        showToast(getResources().getString(R.string.err_acquire_image_failed));
    }

    private void loadImagePreview() {
        if (image.isEmpty()) {
            return;
        }
        if (!image.existsLocal()) {
            Log.i("Image does not exist");
            return;
        }

        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(final Void... params) {
                return ImageUtils.readAndScaleImageToFitDisplay(image.getPath());
            }

            @Override
            protected void onPostExecute(final Bitmap bitmap) {
                imagePreview.setImageBitmap(bitmap);
                imagePreview.setVisibility(View.VISIBLE);
            }
        }.execute();
    }
}
