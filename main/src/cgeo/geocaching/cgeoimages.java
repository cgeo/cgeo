package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.utils.Log;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
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

    private static final int MENU_BROWSER = 2;
    private static final int MENU_FILE = 1;
    private static final int UNKNOWN_TYPE = 0;
    private static final int LOG_IMAGES = 1;
    private static final int SPOILER_IMAGES = 2;

    private String geocode = null;
    private LayoutInflater inflater = null;
    private ProgressDialog progressDialog = null;
    private LinearLayout imagesView = null;
    private int count = 0;
    private int countDone = 0;
    private final SparseArray<cgImage> images = new SparseArray<cgImage>();
    private BitmapDrawable currentDrawable;
    private cgImage currentImage;

    static private final Collection<Bitmap> bitmaps = Collections.synchronizedCollection(new ArrayList<Bitmap>());

    private void loadImages(final List<cgImage> images, final int progressMessage, final boolean offline) {

        count = images.size();
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMessage(res.getString(progressMessage));
        progressDialog.setCancelable(true);
        progressDialog.setMax(count);
        progressDialog.show();

        LinearLayout rowView = null;
        for (final cgImage img : images) {
            rowView = (LinearLayout) inflater.inflate(R.layout.cache_image_item, null);

            if (StringUtils.isNotBlank(img.getTitle())) {
                ((TextView) rowView.findViewById(R.id.title)).setText(Html.fromHtml(img.getTitle()));
                rowView.findViewById(R.id.titleLayout).setVisibility(View.VISIBLE);
            }

            if (StringUtils.isNotBlank(img.getDescription())) {
                final TextView descView = (TextView) rowView.findViewById(R.id.description);
                descView.setText(Html.fromHtml(img.getDescription()), TextView.BufferType.SPANNABLE);
                descView.setVisibility(View.VISIBLE);
            }

            new AsyncImgLoader(rowView, img, offline).execute();
            imagesView.addView(rowView);
        }
    }

    private class AsyncImgLoader extends AsyncTask<Void, Void, BitmapDrawable> {

        final private LinearLayout view;
        final private cgImage img;
        final boolean offline;

        public AsyncImgLoader(final LinearLayout view, final cgImage img, final boolean offline) {
            this.view = view;
            this.img = img;
            this.offline = offline;
        }

        @Override
        protected BitmapDrawable doInBackground(Void... params) {
            final HtmlImage imgGetter = new HtmlImage(cgeoimages.this, geocode, true, offline ? 1 : 0, false);
            return imgGetter.getDrawable(img.getUrl());
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
                        viewImageInStandardApp(image);
                    }
                });
                cgeoimages.this.registerForContextMenu(image_view);
                image_view.setImageDrawable(image);
                image_view.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image_view.setLayoutParams(new LayoutParams(bounds.width(), bounds.height()));

                view.addView(image_view);

                images.put(image_view.getId(), img);
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
        final Bundle extras = getIntent().getExtras();

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

        loadImages(images, message, offline);
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

    private void viewImageInStandardApp(final BitmapDrawable image) {
        final File file = LocalStorage.getStorageFile(null, "temp.jpg", false, true);
        try {
            final FileOutputStream fos = new FileOutputStream(file);
            image.getBitmap().compress(CompressFormat.JPEG, 100, fos);
            fos.close();
        } catch (Exception e) {
            Log.e("cgeoimages.handleMessage.onClick: " + e.toString());
            return;
        }

        final Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "image/jpeg");
        startActivity(intent);

        if (file.exists()) {
            file.deleteOnExit();
        }
    }

    public static void startActivityLogImages(final Context fromActivity, final String geocode, ArrayList<cgImage> logImages) {
        startActivity(fromActivity, geocode, logImages, cgeoimages.LOG_IMAGES);
    }

    private static void startActivity(final Context fromActivity, final String geocode, ArrayList<cgImage> logImages, int imageType) {
        final Intent logImgIntent = new Intent(fromActivity, cgeoimages.class);
        logImgIntent.putExtra("geocode", geocode.toUpperCase());
        logImgIntent.putExtra("type", imageType);
        logImgIntent.putParcelableArrayListExtra("images", logImages);
        fromActivity.startActivity(logImgIntent);
    }

    public static void startActivitySpoilerImages(final Context fromActivity, String geocode, ArrayList<cgImage> spoilers) {
        startActivity(fromActivity, geocode, spoilers, cgeoimages.SPOILER_IMAGES);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(res.getString(R.string.cache_image));
        menu.add(0, MENU_FILE, 0, res.getString(R.string.cache_image_open_file));
        menu.add(0, MENU_BROWSER, 0, res.getString(R.string.cache_image_open_browser));
        final ImageView view = (ImageView) v;
        currentDrawable = (BitmapDrawable) view.getDrawable();
        currentImage = images.get(view.getId());
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_FILE:
                viewImageInStandardApp(currentDrawable);
                return true;
            case MENU_BROWSER:
                if (currentImage != null) {
                    currentImage.openInBrowser(this);
                }
                return true;
            default:
                break;
        }
        return super.onContextItemSelected(item);
    }
}
