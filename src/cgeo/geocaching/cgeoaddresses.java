package cgeo.geocaching;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import cgeo.geocaching.activity.AbstractActivity;

public class cgeoaddresses extends AbstractActivity {
	private final ArrayList<Address> addresses = new ArrayList<Address>();
	private String keyword = null;
	private LayoutInflater inflater = null;
	private LinearLayout addList = null;
	private ProgressDialog waitDialog = null;
	private Handler loadPlacesHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			try {
				if (addList == null) {
					addList = (LinearLayout) findViewById(R.id.address_list);
				}

				if (addresses.isEmpty()) {
					if (waitDialog != null) {
						waitDialog.dismiss();
					}

					showToast(res.getString(R.string.err_search_address_no_match));

					finish();
					return;
				} else {
					LinearLayout oneAddPre = null;
					for (Address address : addresses) {
						oneAddPre = (LinearLayout) inflater.inflate(R.layout.address_button, null);

						Button oneAdd = (Button) oneAddPre.findViewById(R.id.button);
						int index = 0;
						StringBuilder allAdd = new StringBuilder();
						StringBuilder allAddLine = new StringBuilder();

						while (address.getAddressLine(index) != null) {
							if (allAdd.length() > 0) {
								allAdd.append('\n');
							}
							if (allAddLine.length() > 0) {
								allAddLine.append("; ");
							}

							allAdd.append(address.getAddressLine(index));
							allAddLine.append(address.getAddressLine(index));

							index++;
						}

						oneAdd.setText(allAdd.toString());
						oneAdd.setLines(allAdd.toString().split("\n").length);
						oneAdd.setClickable(true);
						oneAdd.setOnClickListener(new buttonListener(address.getLatitude(), address.getLongitude(), allAddLine.toString()));
						addList.addView(oneAddPre);
					}
				}

				if (waitDialog != null) {
					waitDialog.dismiss();
				}
			} catch (Exception e) {
				if (waitDialog != null) {
					waitDialog.dismiss();
				}
				Log.e(cgSettings.tag, "cgeoaddresses.loadCachesHandler: " + e.toString());
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// init
		inflater = getLayoutInflater();

		setTheme();
		setContentView(R.layout.addresses);
		setTitle(res.getString(R.string.search_address_result));

		// get parameters
		Bundle extras = getIntent().getExtras();

		// try to get data from extras
		if (extras != null) {
			keyword = extras.getString("keyword");
		}

		if (keyword == null) {
			showToast(res.getString(R.string.err_search_address_forgot));
			finish();
			return;
		}

		waitDialog = ProgressDialog.show(this, res.getString(R.string.search_address_started), keyword, true);
		waitDialog.setCancelable(true);

		(new loadPlaces()).start();
	}

	@Override
	public void onResume() {
		super.onResume();

		settings.load();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	private class loadPlaces extends Thread {

		@Override
		public void run() {
			Geocoder geocoder = new Geocoder(cgeoaddresses.this, Locale.getDefault());
			try {
				List<Address> knownLocations = geocoder.getFromLocationName(keyword, 20);

				addresses.clear();
				for (Address address : knownLocations) {
					addresses.add(address);
				}
			} catch (Exception e) {
				Log.e(cgSettings.tag, "cgeoaddresses.loadPlaces.run: " + e.toString());
			}

			loadPlacesHandler.sendMessage(new Message());
		}
	}

	private class buttonListener implements View.OnClickListener {

		private Double latitude = null;
		private Double longitude = null;
		private String address = null;

		public buttonListener(Double latitudeIn, Double longitudeIn, String addressIn) {
			latitude = latitudeIn;
			longitude = longitudeIn;
			address = addressIn;
		}

		public void onClick(View arg0) {
			Intent addressIntent = new Intent(cgeoaddresses.this, cgeocaches.class);
			addressIntent.putExtra("type", "address");
			addressIntent.putExtra("latitude", (Double) latitude);
			addressIntent.putExtra("longitude", (Double) longitude);
			addressIntent.putExtra("address", (String) address);
			addressIntent.putExtra("cachetype", settings.cacheType);
			startActivity(addressIntent);

			finish();
			return;
		}
	}
}
