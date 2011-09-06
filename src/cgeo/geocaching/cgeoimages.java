package cgeo.geocaching;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import cgeo.geocaching.activity.AbstractActivity;

public class cgeoimages extends AbstractActivity {

	public static final int LOG_IMAGE = 1;
	public static final int SPOILER_IMAGE = 2;

	private int img_type;
	private List<cgImage> images = new ArrayList<cgImage>();
	private String geocode = null;
	private String title = null;
	private String url = null;
	private LayoutInflater inflater = null;
	private ProgressDialog progressDialog = null;
	private ProgressDialog waitDialog = null;
	private LinearLayout imagesView = null;
	private int offline = 0;
	private boolean save = true;
	private int count = 0;
	private int countDone = 0;
	private String load_process_string;

	private Handler loadImagesHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			try {
				if (images.isEmpty()) {
					if (waitDialog != null) {
						waitDialog.dismiss();
					}
					switch (img_type) {
					case LOG_IMAGE:
						showToast(res.getString(R.string.warn_load_log_image));
						break;
					case SPOILER_IMAGE:
						showToast(res.getString(R.string.warn_load_spoiler_image));
						break;
					}

					finish();
					return;
				} else {
					if (waitDialog != null) {
						waitDialog.dismiss();
					}

					if (app.isOffline(geocode, null)) {
						offline = 1;
						if ((img_type == LOG_IMAGE) && (settings.storelogimages == false)) {
							offline = 0;
						}
					} else {
						offline = 0;
					}

					count = images.size();
					progressDialog = new ProgressDialog(cgeoimages.this);
					progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
					progressDialog.setMessage(load_process_string);
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

						final Handler handler = new onLoadHandler(rowView, img);

						new Thread() {

							@Override
							public void run() {
								BitmapDrawable image = null;
								try {
									cgHtmlImg imgGetter = new cgHtmlImg(cgeoimages.this, geocode, true, offline, false, save);

									image = imgGetter.getDrawable(img.url);
									Message message = handler.obtainMessage(0, image);
									handler.sendMessage(message);
								} catch (Exception e) {
									Log.e(cgSettings.tag, "cgeoimages.onCreate.onClick.run: " + e.toString());
								}

							}
						}.start();

						imagesView.addView(rowView);
					}
				}
			} catch (Exception e) {
				if (waitDialog != null) {
					waitDialog.dismiss();
				}
				Log.e(cgSettings.tag, "cgeoimages.loadImagesHandler: " + e.toString());
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// init
		setTheme();
		setContentView(R.layout.spoilers);

		// get parameters
		Bundle extras = getIntent().getExtras();

		// try to get data from extras
		if (extras != null) {
			geocode = extras.getString("geocode");
			img_type = extras.getInt("type", 0);
		}

		// google analytics
		if (img_type == SPOILER_IMAGE)
		{
			setTitle(res.getString(R.string.cache_spoiler_images_title));
		} else if (img_type == LOG_IMAGE) {
			setTitle(res.getString(R.string.cache_log_images_title));
		}

		if (geocode == null) {
			showToast("Sorry, c:geo forgot for what cache you want to load spoiler images.");
			finish();
			return;
		}
		switch (img_type) {
		case LOG_IMAGE:
			title = extras.getString("title");
			url = extras.getString("url");
			if ((title == null) || (url == null)) {
				showToast("Sorry, c:geo forgot which logimage you wanted to load.");
				finish();
				return;
			}
			break;
		}

		inflater = getLayoutInflater();
		if (imagesView == null) {
			imagesView = (LinearLayout) findViewById(R.id.spoiler_list);
		}

		switch (img_type) {
		case SPOILER_IMAGE:
			load_process_string = res.getString(R.string.cache_spoiler_images_loading);
			save = true;
			break;
		case LOG_IMAGE:
			load_process_string = res.getString(R.string.cache_log_images_loading);
			if (settings.storelogimages) {
				save = true;
			} else {
				save = false;
			}
			break;
		default:
			load_process_string = "Loading...";
		}
		waitDialog = ProgressDialog.show(this, null, load_process_string, true);
		waitDialog.setCancelable(true);

		switch (img_type) {
		case LOG_IMAGE:
			cgImage logimage = new cgImage();
			logimage.title = title;
			logimage.url = url;
			logimage.description = "";
			images.add(logimage);
			try {
				loadImagesHandler.sendMessage(new Message());
			} catch (Exception e) {
				Log.e(cgSettings.tag, "cgeoimages.loadImagesHandler.sendMessage: " + e.toString());
			}
			break;
		case SPOILER_IMAGE:
			(new loadSpoilers()).start();
			break;
		default:
			showToast("Sorry, can't load unknown image type.");
			finish();
		}

	}

	@Override
	public void onResume() {
		super.onResume();

		settings.load();
	}

	private class loadSpoilers extends Thread {

		@Override
		public void run() {
			try {
				images = app.loadSpoilers(geocode);

				loadImagesHandler.sendMessage(new Message());
			} catch (Exception e) {
				Log.e(cgSettings.tag, "cgeoimages.loadSpoilers.run: " + e.toString());
			}
		}
	}

	private class onLoadHandler extends Handler {

		LinearLayout view = null;

		public onLoadHandler(LinearLayout view, cgImage image) {
			this.view = view;
		}

		@Override
		public void handleMessage(Message message) {
			final BitmapDrawable image = (BitmapDrawable) message.obj;
			if (image != null) {
				ImageView image_view = null;
				image_view = (ImageView) inflater.inflate(R.layout.image_item, null);

				Rect bounds = image.getBounds();

				image_view.setImageResource(R.drawable.image_not_loaded);
				image_view.setClickable(true);
				image_view.setOnClickListener(new View.OnClickListener() {

					public void onClick(View arg0) {
						final String directoryTarget = Environment.getExternalStorageDirectory() + "/" + cgSettings.cache + "/" + "temp.jpg";
						File file = new File(directoryTarget);
						FileOutputStream fos;
						try {
							fos = new FileOutputStream(file);
							image.getBitmap().compress(CompressFormat.JPEG, 100, fos);
							fos.close();
						} catch (Exception e) {
							Log.e(cgSettings.tag, "cgeoimages.handleMessage.onClick: " + e.toString());
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

			countDone++;
			progressDialog.setProgress(countDone);
			if (progressDialog.getProgress() >= count) {
				progressDialog.dismiss();
			}
		}
	}
}
