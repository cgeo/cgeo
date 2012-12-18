package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.StoredList;
import cgeo.geocaching.cgImage;
import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.mapsforge.core.IOUtils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.Html;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class ImagesList {

    private static final int MENU_FILE = 201;
    private static final int MENU_BROWSER = 202;

    private BitmapDrawable currentDrawable;
    private cgImage currentImage;

    public enum ImageType {
        LogImages(R.string.cache_log_images_title, R.string.cache_log_images_loading),
        SpoilerImages(R.string.cache_spoiler_images_title, R.string.cache_spoiler_images_loading),
        AllImages(R.string.cache_images_title, R.string.cache_images_loading);

        private final int titleResId;
        private final int loadingResId;

        private ImageType(final int title, final int loading) {
            this.titleResId = title;
            this.loadingResId = loading;
        }

        public int getTitle() {
            return titleResId;
        }
    }

    private LayoutInflater inflater = null;
    private ProgressDialog progressDialog = null;
    private int count = 0;
    private int countDone = 0;
    private final Activity activity;
    // We could use a Set here, but we will insert no duplicates, so there is no need to check for uniqueness.
    private final Collection<Bitmap> bitmaps = new LinkedList<Bitmap>();
    /**
     * map image view id to image
     */
    private final SparseArray<cgImage> images = new SparseArray<cgImage>();
    private final String geocode;
    private LinearLayout imagesView;

    public ImagesList(final Activity activity, final String geocode) {
        this.activity = activity;
        this.geocode = geocode;
        inflater = activity.getLayoutInflater();
    }

    public void loadImages(final View parentView, final List<cgImage> images, ImageType imageType, final boolean offline) {

        imagesView = (LinearLayout) parentView.findViewById(R.id.spoiler_list);

        count = images.size();
        progressDialog = new ProgressDialog(activity);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMessage(activity.getString(imageType.loadingResId));
        progressDialog.setCancelable(true);
        progressDialog.setMax(count);
        progressDialog.show();

        for (final cgImage img : images) {
            LinearLayout rowView = (LinearLayout) inflater.inflate(R.layout.cache_image_item, null);

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
            final HtmlImage imgGetter = new HtmlImage(geocode, true, offline ? StoredList.STANDARD_LIST_ID : StoredList.TEMPORARY_LIST_ID, false);
            return imgGetter.getDrawable(img.getUrl());
        }

        @Override
        protected void onPostExecute(final BitmapDrawable image) {
            if (image != null) {
                bitmaps.add(image.getBitmap());
                final ImageView imageView = (ImageView) inflater.inflate(R.layout.image_item, null);

                final Rect bounds = image.getBounds();

                imageView.setImageResource(R.drawable.image_not_loaded);
                imageView.setClickable(true);
                imageView.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        viewImageInStandardApp(image);
                    }
                });
                activity.registerForContextMenu(imageView);
                imageView.setImageDrawable(image);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setLayoutParams(new LayoutParams(bounds.width(), bounds.height()));

                view.addView(imageView);

                imageView.setId(image.hashCode());
                images.put(imageView.getId(), img);
            }

            synchronized (activity) {
                countDone++;
                progressDialog.setProgress(countDone);
                if (progressDialog.getProgress() >= count) {
                    progressDialog.dismiss();
                }
            }
        }
    }

    public void removeAllViews() {
        imagesView.removeAllViews();
        for (final Bitmap b : bitmaps) {
            b.recycle();
        }
        bitmaps.clear();
    }

    public void onCreateContextMenu(ContextMenu menu, View v) {
        final Resources res = activity.getResources();
        menu.setHeaderTitle(res.getString(R.string.cache_image));
        menu.add(0, MENU_FILE, 0, res.getString(R.string.cache_image_open_file));
        menu.add(0, MENU_BROWSER, 0, res.getString(R.string.cache_image_open_browser));
        final ImageView view = (ImageView) v;
        currentDrawable = (BitmapDrawable) view.getDrawable();
        currentImage = images.get(view.getId());
    }

    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_FILE:
                viewImageInStandardApp(currentDrawable);
                return true;
            case MENU_BROWSER:
                if (currentImage != null) {
                    currentImage.openInBrowser(activity);
                }
                return true;
            default:
                return false;
        }
    }

    private void viewImageInStandardApp(final BitmapDrawable image) {
        final File file = LocalStorage.getStorageFile(null, "temp.jpg", false, true);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            image.getBitmap().compress(CompressFormat.JPEG, 100, fos);
        } catch (Exception e) {
            Log.e("ImagesActivity.handleMessage.onClick: " + e.toString());
            return;
        } finally {
            IOUtils.closeQuietly(fos);
        }

        final Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "image/jpeg");
        activity.startActivity(intent);

        if (file.exists()) {
            file.deleteOnExit();
        }
    }

}
