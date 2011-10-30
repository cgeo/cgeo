package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.network.HtmlImage;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class cgeoimages extends AbstractActivity {

    private static final int UNKNOWN_TYPE = 0;
    public static final int LOG_IMAGES = 1;
    public static final int SPOILER_IMAGES = 2;

    private String geocode = null;
    private LayoutInflater inflater = null;
    private ProgressDialog progressDialog = null;
    private LinearLayout imagesView = null;
    private int count = 0;
    private int countDone = 0;

    static private Collection<Bitmap> bitmaps = Collections.synchronizedCollection(new ArrayList<Bitmap>());

    private void loadImages(final List<cgImage> images, final int progressMessage, final boolean save, final boolean offline) {

        count = images.size();
        progressDialog = new ProgressDialog(cgeoimages.this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMessage(res.getString(progressMessage));
        progressDialog.setCancelable(true);
        progressDialog.setMax(count);
        progressDialog.show();

        LinearLayout rowView = null;
        for (final cgImage img : images) {
            rowView = (LinearLayout) inflater.inflate(R.layout.cache_image_item, null);

            ((TextView) rowView.findViewById(R.id.title)).setText(Html.fromHtml(img.title));

            if (StringUtils.isNotBlank(img.description)) {
                final TextView descView = (TextView) rowView.findViewById(R.id.description);
                descView.setText(Html.fromHtml(img.description), TextView.BufferType.SPANNABLE);
                descView.setVisibility(View.VISIBLE);
            }

            new AsyncImgLoader(rowView, img, save, offline).execute();
            imagesView.addView(rowView);
        }
    }

    private class AsyncImgLoader extends AsyncTask<Void, Void, BitmapDrawable> {

        final private LinearLayout view;
        final private cgImage img;
        final private boolean save;
        final boolean offline;

        public AsyncImgLoader(final LinearLayout view, final cgImage img, final boolean save, final boolean offline) {
            this.view = view;
            this.img = img;
            this.save = save;
            this.offline = offline;
        }

        @Override
        protected BitmapDrawable doInBackground(Void... params) {
            final HtmlImage imgGetter = new HtmlImage(cgeoimages.this, geocode, true, offline ? 1 : 0, false, save);
            return imgGetter.getDrawable(img.url);
        }

        @Override
        protected void onPostExecute(final BitmapDrawable image) {
            if (image != null) {
                bitmaps.add(image.getBitmap());
                final ImageView image_view = (ImageView) inflater.inflate(R.layout.image_item, null);

                final Rect bounds = image.getBounds();

                image_view.setImageResource(R.drawable.image_not_loaded);
                image_view.setClickable(true);
                image_view.setOnClickListener(new View.OnClickListener() {

                    public void onClick(View arg0) {
                        final File file = LocalStorage.getStorageFile(null, "temp.jpg", false);
                        try {
                            final FileOutputStream fos = new FileOutputStream(file);
                            image.getBitmap().compress(CompressFormat.JPEG, 100, fos);
                            fos.close();
                        } catch (Exception e) {
                            Log.e(Settings.tag, "cgeoimages.handleMessage.onClick: " + e.toString());
                            return;
                        }

                        Intent intent = new Intent();
                        intent.setAction(android.content.Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.fromFile(file), "image/jpg");
                        startActivity(intent);

                        if (file.exists())
                            file.deleteOnExit();
                    }
                });
                image_view.setImageDrawable(image);
                image_view.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image_view.setLayoutParams(new LayoutParams(bounds.width(), bounds.height()));

                view.addView(image_view);
            }

            synchronized (cgeoimages.this) {
                countDone++;
                progressDialog.setProgress(countDone);
                if (progressDialog.getProgress() >= count) {
                    progressDialog.dismiss();
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get parameters
        Bundle extras = getIntent().getExtras();

        // try to get data from extras
        int img_type = UNKNOWN_TYPE;
        if (extras != null) {
            geocode = extras.getString("geocode");
            img_type = extras.getInt("type", 0);
        }

        if (extras == null || geocode == null) {
            showToast("Sorry, c:geo forgot for what cache you want to load spoiler images.");
            finish();
            return;
        }

        if (img_type != SPOILER_IMAGES && img_type != LOG_IMAGES) {
            showToast("Sorry, can't load unknown image type.");
            finish();
            return;
        }

        // init
        setTheme();
        setContentView(R.layout.spoilers);
        setTitle(res.getString(img_type == SPOILER_IMAGES ? R.string.cache_spoiler_images_title : R.string.cache_log_images_title));

        inflater = getLayoutInflater();
        if (imagesView == null) {
            imagesView = (LinearLayout) findViewById(R.id.spoiler_list);
        }

        final ArrayList<cgImage> images = extras.getParcelableArrayList("images");
        if (CollectionUtils.isEmpty(images)) {
            showToast(res.getString(R.string.warn_load_images));
            finish();
            return;
        }

        final int message = img_type == SPOILER_IMAGES ? R.string.cache_spoiler_images_loading : R.string.cache_log_images_loading;
        final boolean offline = app.isOffline(geocode, null) && (img_type == SPOILER_IMAGES || Settings.isStoreLogImages());
        final boolean save = img_type == SPOILER_IMAGES ? true : Settings.isStoreLogImages();

        loadImages(images, message, save, offline);
    }

    @Override
    public void onDestroy() {
        // Reclaim native memory faster than the finalizers would
        for (Bitmap b : bitmaps) {
            b.recycle();
        }
        bitmaps.clear();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

}
