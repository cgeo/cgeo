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

public class cgeospoilers extends Activity {
	private ArrayList<cgSpoiler> spoilers = new ArrayList<cgSpoiler>();
	private Resources res = null;
	private String geocode = null;
	private cgeoapplication app = null;
	private Activity activity = null;
	private cgSettings settings = null;
	private cgBase base = null;
	private cgWarning warning = null;
	private LayoutInflater inflater = null;
	private ProgressDialog progressDialog = null;
	private ProgressDialog waitDialog = null;
	private LinearLayout spoilerView = null;
	private int offline = 0;
	private int count = 0;
	private int countDone = 0;
	private Handler loadSpoilersHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			try {
				if (spoilers.isEmpty()) {
					if (waitDialog != null) {
						waitDialog.dismiss();
					}

					warning.showToast("Sorry, c:geo failed to load spoiler images.");

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

					count = spoilers.size();
					progressDialog = new ProgressDialog(activity);
					progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
					progressDialog.setMessage(res.getString(R.string.cache_spoiler_images_loading));
					progressDialog.setCancelable(true);
					progressDialog.setMax(count);
					progressDialog.show();

					LinearLayout rowView = null;
					for (final cgSpoiler spl : spoilers) {
						rowView = (LinearLayout) inflater.inflate(R.layout.spoiler_item, null);

						((TextView) rowView.findViewById(R.id.title)).setText(Html.fromHtml(spl.title));

						if (spl.description != null && spl.description.length() > 0) {
							final TextView descView = (TextView) rowView.findViewById(R.id.description);
							descView.setText(Html.fromHtml(spl.description), TextView.BufferType.SPANNABLE);
							descView.setVisibility(View.VISIBLE);
						}

						final Handler handler = new onLoadHandler(rowView, spl);

						new Thread() {

							@Override
							public void run() {
								BitmapDrawable image = null;
								try {
									cgHtmlImg imgGetter = new cgHtmlImg(activity, settings, geocode, true, offline, false);

									image = imgGetter.getDrawable(spl.url);
									Message message = handler.obtainMessage(0, image);
									handler.sendMessage(message);
								} catch (Exception e) {
									Log.e(cgSettings.tag, "cgeospoilers.onCreate.onClick.run: " + e.toString());
								}

							}
						}.start();

						spoilerView.addView(rowView);
					}
				}
			} catch (Exception e) {
				if (waitDialog != null) {
					waitDialog.dismiss();
				}
				Log.e(cgSettings.tag, "cgeospoilers.loadSpoilersHandler: " + e.toString());
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
		base.setTitle(activity, res.getString(R.string.cache_spoiler_images_title));

		// get parameters
		Bundle extras = getIntent().getExtras();

		// try to get data from extras
		if (extras != null) {
			geocode = extras.getString("geocode");
		}

		if (geocode == null) {
			warning.showToast("Sorry, c:geo forgot for what cache you want to load spoiler images.");
			finish();
			return;
		}

		inflater = activity.getLayoutInflater();
		if (spoilerView == null) {
			spoilerView = (LinearLayout) findViewById(R.id.spoiler_list);
		}

		waitDialog = ProgressDialog.show(this, null, res.getString(R.string.cache_spoiler_images_loading), true);
		waitDialog.setCancelable(true);

		(new loadSpoilers()).start();
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
				spoilers = app.loadSpoilers(geocode);

				loadSpoilersHandler.sendMessage(new Message());
			} catch (Exception e) {
				Log.e(cgSettings.tag, "cgeospoilers.loadSpoilers.run: " + e.toString());
			}
		}
	}

	private class onLoadHandler extends Handler {

		LinearLayout view = null;
		cgSpoiler spoiler = null;

		public onLoadHandler(LinearLayout view, cgSpoiler spoiler) {
			this.view = view;
			this.spoiler = spoiler;
		}

		@Override
		public void handleMessage(Message message) {
			BitmapDrawable image = (BitmapDrawable) message.obj;
			if (image != null) {
				ImageView spoilerImage = null;
				spoilerImage = (ImageView) inflater.inflate(R.layout.image_item, null);

				Rect bounds = image.getBounds();

				spoilerImage.setImageResource(R.drawable.image_not_loaded);
				spoilerImage.setClickable(true);
				spoilerImage.setOnClickListener(new View.OnClickListener() {

					public void onClick(View arg0) {
						activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(spoiler.url)));
					}
				});
				spoilerImage.setImageDrawable((BitmapDrawable) message.obj);
				spoilerImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
				spoilerImage.setLayoutParams(new LayoutParams(bounds.width(), bounds.height()));

				view.addView(spoilerImage);
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
