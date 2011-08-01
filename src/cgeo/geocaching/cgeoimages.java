package cgeo.geocaching;

import java.util.ArrayList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.net.Uri;
import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.view.ViewGroup.LayoutParams;

public class cgeoimages extends Activity {	
	
	public static final int LOG_IMAGE = 1;
	public static final int SPOILER_IMAGE = 2;
	
	private int img_type;
	private ArrayList<cgImage> images = new ArrayList<cgImage>();
	private Resources res = null;
	private String geocode = null;
	private String title = null;
	private String url = null;	
	private cgeoapplication app = null;
	private Activity activity = null;
	private cgSettings settings = null;
	private cgBase base = null;
	private cgWarning warning = null;
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
						warning.showToast("Sorry, c:geo failed to load log image.");
						break;
					case SPOILER_IMAGE:
						warning.showToast("Sorry, c:geo failed to load spoiler images.");
						break;
					}					

					finish();
					return;
				} else {
					if (waitDialog != null) {
						waitDialog.dismiss();
					}

					if (app.isOffline(geocode, null) == true) {
						offline = 1;
					} else {
						offline = 0;
					}

					count = images.size();
					progressDialog = new ProgressDialog(activity);
					progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
					progressDialog.setMessage(load_process_string);
					progressDialog.setCancelable(true);
					progressDialog.setMax(count);
					progressDialog.show();

					LinearLayout rowView = null;
					for (final cgImage img : images) {
						rowView = (LinearLayout) inflater.inflate(R.layout.cache_image_item, null);

						((TextView) rowView.findViewById(R.id.title)).setText(Html.fromHtml(img.title));

						if (img.description != null && img.description.length() > 0) {
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
									cgHtmlImg imgGetter = new cgHtmlImg(activity, settings, geocode, true, offline, false, save);

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
		activity = this;
		res = this.getResources();
		app = (cgeoapplication) this.getApplication();
		settings = new cgSettings(this, getSharedPreferences(cgSettings.preferences, 0));
		base = new cgBase(app, settings, getSharedPreferences(cgSettings.preferences, 0));
		warning = new cgWarning(this);

		// set layout
		if (settings.skin == 1) {
			setTheme(R.style.light);
		} else {
			setTheme(R.style.dark);
		}
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
			base.setTitle(activity, res.getString(R.string.cache_spoiler_images_title));
			base.sendAnal(activity, "/spoilers");
		} else if (img_type == LOG_IMAGE) {
			base.setTitle(activity, res.getString(R.string.cache_log_images_title));
			base.sendAnal(activity, "/logimg");
		}
		
		if (geocode == null) {
			warning.showToast("Sorry, c:geo forgot for what cache you want to load spoiler images.");
			finish();
			return;
		}
		switch (img_type) {
		case LOG_IMAGE:
			title = extras.getString("title");
			url = extras.getString("url");
			if ((title == null) || (url == null)) {
				warning.showToast("Sorry, c:geo forgot which logimage you wanted to load.");
				finish();
				return;
			}
			break;
		}
		
		inflater = activity.getLayoutInflater();
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
			if (settings.storeLogImg == 1) {
				save = true;
			} else {
				save = false;
			}
			break;
		default:
			load_process_string = new String("Loading...");
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
			warning.showToast("Sorry, can't load unknown image type.");
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
				Log.e(cgSettings.tag, "cgeospoilers.loadSpoilers.run: " + e.toString());
			}
		}
	}

	private class onLoadHandler extends Handler {

		LinearLayout view = null;
		cgImage img = null;

		public onLoadHandler(LinearLayout view, cgImage image) {
			this.view = view;
			this.img = image;
		}

		@Override
		public void handleMessage(Message message) {
			BitmapDrawable image = (BitmapDrawable) message.obj;
			if (image != null) {
				ImageView image_view = null;
				image_view = (ImageView) inflater.inflate(R.layout.image_item, null);

				Rect bounds = image.getBounds();

				image_view.setImageResource(R.drawable.image_not_loaded);
				image_view.setClickable(true);
				image_view.setOnClickListener(new View.OnClickListener() {

					public void onClick(View arg0) {
						activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(img.url)));
					}
				});
				image_view.setImageDrawable((BitmapDrawable) message.obj);
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

	public void goHome(View view) {
		base.goHome(activity);
	}
}
