package cgeo.geocaching;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import cgeo.geocaching.cgSettings.coordInputFormatEnum;
import cgeo.geocaching.activity.AbstractActivity;

public class cgeocoords extends Dialog {

	private AbstractActivity context = null;
	private cgSettings settings = null;
	private cgGeo geo = null;
	private Double latitude = 0.0, longitude = 0.0;

	private EditText eLat, eLon;
	private Button bLat, bLon;
	private EditText eLatDeg, eLatMin, eLatSec, eLatSub;
	private EditText eLonDeg, eLonMin, eLonSec, eLonSub;
	private TextView tLatSep1, tLatSep2, tLatSep3;
	private TextView tLonSep1, tLonSep2, tLonSep3;

	CoordinateUpdate cuListener;

	coordInputFormatEnum currentFormat;

	public cgeocoords(final AbstractActivity contextIn, cgSettings settingsIn, final cgWaypoint waypoint, final cgGeo geoIn) {
		super(contextIn);
		context = contextIn;
		settings = settingsIn;
		geo = geoIn;

		if (waypoint != null) {
			latitude = waypoint.latitude;
			longitude = waypoint.longitude;
		} else if (geo != null && geo.latitudeNow != null && geo.longitudeNow != null) {
			latitude = geo.latitudeNow;
			longitude = geo.longitudeNow;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		} catch (Exception e) {
			// nothing
		}

		setContentView(R.layout.coords);

		Spinner s = (Spinner) findViewById(R.id.spinnerCoordinateFormats);
		ArrayAdapter<CharSequence> adapter =
				ArrayAdapter.createFromResource(context,
				                                R.array.waypoint_coordinate_formats,
				                                android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		s.setAdapter(adapter);
		s.setSelection(settings.getCoordInputFormat().ordinal());

		s.setOnItemSelectedListener(new CoordinateFormatListener());

		bLat = (Button) findViewById(R.id.ButtonLat);
		eLat = (EditText) findViewById(R.id.latitude);
		eLatDeg = (EditText) findViewById(R.id.EditTextLatDeg);
		eLatMin = (EditText) findViewById(R.id.EditTextLatMin);
		eLatSec = (EditText) findViewById(R.id.EditTextLatSec);
		eLatSub = (EditText) findViewById(R.id.EditTextLatSecFrac);
		tLatSep1 = (TextView) findViewById(R.id.LatSeparator1);
		tLatSep2 = (TextView) findViewById(R.id.LatSeparator2);
		tLatSep3 = (TextView) findViewById(R.id.LatSeparator3);

		bLon = (Button) findViewById(R.id.ButtonLon);
		eLon = (EditText) findViewById(R.id.longitude);
		eLonDeg = (EditText) findViewById(R.id.EditTextLonDeg);
		eLonMin = (EditText) findViewById(R.id.EditTextLonMin);
		eLonSec = (EditText) findViewById(R.id.EditTextLonSec);
		eLonSub = (EditText) findViewById(R.id.EditTextLonSecFrac);
		tLonSep1 = (TextView) findViewById(R.id.LonSeparator1);
		tLonSep2 = (TextView) findViewById(R.id.LonSeparator2);
		tLonSep3 = (TextView) findViewById(R.id.LonSeparator3);

		eLatDeg.addTextChangedListener(new textChangedListener(1));
		eLatMin.addTextChangedListener(new textChangedListener(2));
		eLatSec.addTextChangedListener(new textChangedListener(3));
		eLatSub.addTextChangedListener(new textChangedListener(4));
		eLonDeg.addTextChangedListener(new textChangedListener(5));
		eLonMin.addTextChangedListener(new textChangedListener(6));
		eLonSec.addTextChangedListener(new textChangedListener(7));
		eLonSub.addTextChangedListener(new textChangedListener(8));
		eLat.addTextChangedListener(new textChangedListener(10));
		eLon.addTextChangedListener(new textChangedListener(11));
		bLat.setOnClickListener(new buttonClickListener());
		bLon.setOnClickListener(new buttonClickListener());

		Button buttonCurrent = (Button) findViewById(R.id.current);
		buttonCurrent.setOnClickListener(new CurrentListener());
		Button buttonDone = (Button) findViewById(R.id.done);
		buttonDone.setOnClickListener(new InputDoneListener());
	}

	private void updateGUI() {
		Double lat = 0.0;
		if (latitude != null) {
			if (latitude < 0) {
				bLat.setText("S");
			} else {
				bLat.setText("N");
			}

			lat = Math.abs(latitude);
		}
		Double lon = 0.0;
		if (longitude != null) {
			if (longitude < 0) {
				bLon.setText("W");
			} else {
				bLon.setText("E");
			}

			lon = Math.abs(longitude);
		}
		int latDeg = (int) Math.floor(lat);
		int latDegFrac = (int) Math.round((lat - latDeg) * 100000);

		int latMin = (int) Math.floor((lat - latDeg) * 60);
		int latMinFrac = (int) Math.round(((lat - latDeg) * 60 - latMin) * 1000);

		int latSec = (int) Math.floor(((lat - latDeg) * 60 - latMin) * 60);
		int latSecFrac = (int) Math.round((((lat - latDeg) * 60 - latMin) * 60 - latSec) * 1000);

		int lonDeg = (int) Math.floor(lon);
		int lonDegFrac = (int) Math.round((lon - lonDeg) * 100000);

		int lonMin = (int) Math.floor((lon - lonDeg) * 60);
		int lonMinFrac = (int) Math.round(((lon - lonDeg) * 60 - lonMin) * 1000);

		int lonSec = (int) Math.floor(((lon - lonDeg) * 60 - lonMin) * 60);
		int lonSecFrac = (int) Math.round((((lon - lonDeg) * 60 - lonMin) * 60 - lonSec) * 1000);

		switch (currentFormat) {
			case Plain:
				findViewById(R.id.coordTable).setVisibility(View.GONE);
				eLat.setVisibility(View.VISIBLE);
				eLon.setVisibility(View.VISIBLE);
				if (latitude != null) {
					eLat.setText(cgBase.formatLatitude(latitude, true));
				}
				if (longitude != null) {
					eLon.setText(cgBase.formatLongitude(longitude, true));
				}
				break;
			case Deg: // DDD.DDDDD°
				findViewById(R.id.coordTable).setVisibility(View.VISIBLE);
				eLat.setVisibility(View.GONE);
				eLon.setVisibility(View.GONE);
				eLatSec.setVisibility(View.GONE);
				eLonSec.setVisibility(View.GONE);
				tLatSep3.setVisibility(View.GONE);
				tLonSep3.setVisibility(View.GONE);
				eLatSub.setVisibility(View.GONE);
				eLonSub.setVisibility(View.GONE);

				tLatSep1.setText(".");
				tLonSep1.setText(".");
				tLatSep2.setText("°");
				tLonSep2.setText("°");

				if (latitude != null) {
					eLatDeg.setText(addZeros(latDeg, 2) + Integer.toString(latDeg));
					eLatMin.setText(addZeros(latDegFrac, 5) + Integer.toString(latDegFrac));
				}
				if (longitude != null) {
					eLonDeg.setText(addZeros(latDeg, 3) + Integer.toString(lonDeg));
					eLonMin.setText(addZeros(lonDegFrac, 5) + Integer.toString(lonDegFrac));
				}
				break;
			case Min: // DDD° MM.MMM
				findViewById(R.id.coordTable).setVisibility(View.VISIBLE);
				eLat.setVisibility(View.GONE);
				eLon.setVisibility(View.GONE);
				eLatSec.setVisibility(View.VISIBLE);
				eLonSec.setVisibility(View.VISIBLE);
				tLatSep3.setVisibility(View.VISIBLE);
				tLonSep3.setVisibility(View.VISIBLE);
				eLatSub.setVisibility(View.GONE);
				eLonSub.setVisibility(View.GONE);

				tLatSep1.setText("°");
				tLonSep1.setText("°");
				tLatSep2.setText(".");
				tLonSep2.setText(".");
				tLatSep3.setText("'");
				tLonSep3.setText("'");

				if (latitude != null) {
					eLatDeg.setText(addZeros(latDeg, 2) + Integer.toString(latDeg));
					eLatMin.setText(addZeros(latMin, 2) + Integer.toString(latMin));
					eLatSec.setText(addZeros(latMinFrac, 3) + Integer.toString(latMinFrac));
				}
				if (longitude != null) {
					eLonDeg.setText(addZeros(lonDeg, 3) + Integer.toString(lonDeg));
					eLonMin.setText(addZeros(lonMin, 2) + Integer.toString(lonMin));
					eLonSec.setText(addZeros(lonMinFrac, 3) + Integer.toString(lonMinFrac));
				}
				break;
			case Sec: // DDD° MM SS.SSS
				findViewById(R.id.coordTable).setVisibility(View.VISIBLE);
				eLat.setVisibility(View.GONE);
				eLon.setVisibility(View.GONE);
				eLatSec.setVisibility(View.VISIBLE);
				eLonSec.setVisibility(View.VISIBLE);
				tLatSep3.setVisibility(View.VISIBLE);
				tLonSep3.setVisibility(View.VISIBLE);
				eLatSub.setVisibility(View.VISIBLE);
				eLonSub.setVisibility(View.VISIBLE);

				tLatSep1.setText("°");
				tLonSep1.setText("°");
				tLatSep2.setText("'");
				tLonSep2.setText("'");
				tLatSep3.setText(".");
				tLonSep3.setText(".");

				if (latitude != null) {
					eLatDeg.setText(addZeros(latDeg, 2) + Integer.toString(latDeg));
					eLatMin.setText(addZeros(latMin, 2) + Integer.toString(latMin));
					eLatSec.setText(addZeros(latSec, 2) + Integer.toString(latSec));
					eLatSub.setText(addZeros(latSecFrac, 3) + Integer.toString(latSecFrac));
				}
				if (longitude != null) {
					eLonDeg.setText(addZeros(lonDeg, 3) + Integer.toString(lonDeg));
					eLonMin.setText(addZeros(lonMin, 2) + Integer.toString(lonMin));
					eLonSec.setText(addZeros(lonSec, 2) + Integer.toString(lonSec));
					eLonSub.setText(addZeros(lonSecFrac, 3) + Integer.toString(lonSecFrac));
				}
				break;
		}
	}

	private static String addZeros(int value, int len) {
		StringBuilder zeros = new StringBuilder();
		if (value == 0) {
			value = 1;
		}
		double wantedLength = Math.pow(10, len-1);
		while (value < wantedLength) {
			zeros.append('0');
			value *= 10;
		}
		return zeros.toString();
	}

	private class buttonClickListener implements View.OnClickListener {

		@Override
		public void onClick(View v) {
			Button e = (Button) v;
			CharSequence text = e.getText();
			if (StringUtils.isBlank(text)) {
				return;
			}
			switch (text.charAt(0)) {
				case 'N':
					e.setText("S");
					break;
				case 'S':
					e.setText("N");
					break;
				case 'E':
					e.setText("W");
					break;
				case 'W':
					e.setText("E");
					break;
			}
			calc();
		}
	}

	private class textChangedListener implements TextWatcher {

		private int	editTextId;

		public textChangedListener(int id) {
			editTextId = id;
		}

		@Override
		public void afterTextChanged(Editable s) {

			/*
			 * Max lengths, depending on currentFormat
			 *
			 * formatPlain = disabled
			 *           DEG MIN SEC SUB
			 * formatDeg 2/3 5   -   -
			 * formatMin 2/3 2   3   -
			 * formatSec 2/3 2   2   3
			 */

			if (currentFormat == coordInputFormatEnum.Plain) {
				return;
			}

			int maxLength = 2;
			if (editTextId == 5 || editTextId == 4 || editTextId == 8) {
				maxLength = 3;
			}
			if ((editTextId == 2 || editTextId == 6) && currentFormat == coordInputFormatEnum.Deg) {
				maxLength = 5;
			}
			if ((editTextId == 3 || editTextId == 7) && currentFormat == coordInputFormatEnum.Min) {
				maxLength = 3;
			}

			if (s.length() == maxLength) {
				switch (editTextId) {
					case 1:
						eLatMin.requestFocus();
						break;
					case 2:
						if (eLatSec.getVisibility() == View.GONE) {
							eLonDeg.requestFocus();
						} else {
							eLatSec.requestFocus();
						}
						break;
					case 3:
						if (eLatSub.getVisibility() == View.GONE) {
							eLonDeg.requestFocus();
						} else {
							eLatSub.requestFocus();
						}
						break;
					case 4:
						eLonDeg.requestFocus();
						break;
					case 5:
						eLonMin.requestFocus();
						break;
					case 6:
						if (eLonSec.getVisibility() == View.GONE) {
							eLatDeg.requestFocus();
						} else {
							eLonSec.requestFocus();
						}
						break;
					case 7:
						if (eLonSub.getVisibility() == View.GONE) {
							eLatDeg.requestFocus();
						} else {
							eLonSub.requestFocus();
						}
						break;
					case 8:
						eLatDeg.requestFocus();
						break;
				}
			}
			calc();
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {}
	}

	private void calc() {
		if (currentFormat == coordInputFormatEnum.Plain) {
			return;
		}

		int latDeg = 0, latMin = 0, latSec = 0;
		int lonDeg = 0, lonMin = 0, lonSec = 0;
		Double latDegFrac = 0.0, latMinFrac = 0.0, latSecFrac = 0.0;
		Double lonDegFrac = 0.0, lonMinFrac = 0.0, lonSecFrac = 0.0;

		try {
			latDeg = Integer.parseInt(eLatDeg.getText().toString());
			lonDeg = Integer.parseInt(eLonDeg.getText().toString());
			latDegFrac = Double.parseDouble("0."+eLatMin.getText().toString());
			lonDegFrac = Double.parseDouble("0."+eLonMin.getText().toString());
			latMin = Integer.parseInt(eLatMin.getText().toString());
			lonMin = Integer.parseInt(eLonMin.getText().toString());
			latMinFrac = Double.parseDouble("0."+eLatSec.getText().toString());
			lonMinFrac = Double.parseDouble("0."+eLonSec.getText().toString());
			latSec = Integer.parseInt(eLatSec.getText().toString());
			lonSec = Integer.parseInt(eLonSec.getText().toString());
			latSecFrac = Double.parseDouble("0."+eLatSub.getText().toString());
			lonSecFrac = Double.parseDouble("0."+eLonSub.getText().toString());

		} catch (NumberFormatException e) {}

		switch (currentFormat) {
			case Deg:
				latitude = latDeg + latDegFrac;
				longitude = lonDeg + lonDegFrac;
				break;
			case Min:
				latitude = latDeg + latMin/60.0 + latMinFrac/60.0;
				longitude = lonDeg + lonMin/60.0 + lonMinFrac/60.0;
				break;
			case Sec:
				latitude = latDeg + latMin/60.0 + latSec/60.0/60.0 + latSecFrac/60.0/60.0;
				longitude = lonDeg + lonMin/60.0 + lonSec/60.0/60.0 + lonSecFrac/60.0/60.0;
				break;
		}
		latitude  *= (bLat.getText().toString().equalsIgnoreCase("S") ? -1 : 1);
		longitude *= (bLon.getText().toString().equalsIgnoreCase("W") ? -1 : 1);
	}

	private class CoordinateFormatListener implements OnItemSelectedListener {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
			currentFormat = coordInputFormatEnum.fromInt(pos);
			settings.setCoordInputFormat(currentFormat);
			updateGUI();
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {}

	}

	private class CurrentListener implements View.OnClickListener {

		@Override
		public void onClick(View v) {
			if (geo == null || geo.latitudeNow == null || geo.longitudeNow == null) {
				context.showToast(context.getResources().getString(R.string.err_point_unknown_position));
				return;
			}

			latitude = geo.latitudeNow;
			longitude = geo.longitudeNow;
			updateGUI();
		}
	}

	private class InputDoneListener implements View.OnClickListener {

		@Override
		public void onClick(View v) {
			if (currentFormat == coordInputFormatEnum.Plain) {
				if (eLat.length() > 0 && eLon.length() > 0) {
					// latitude & longitude
					HashMap<String, Object> latParsed = cgBase.parseCoordinate(eLat.getText().toString(), "lat");
					HashMap<String, Object> lonParsed = cgBase.parseCoordinate(eLon.getText().toString(), "lon");

					if (latParsed == null || latParsed.get("coordinate") == null || latParsed.get("string") == null) {
						context.showToast(context.getResources().getString(R.string.err_parse_lat));
						return;
					}

					if (lonParsed == null || lonParsed.get("coordinate") == null || lonParsed.get("string") == null) {
						context.showToast(context.getResources().getString(R.string.err_parse_lon));
						return;
					}

					latitude = (Double) latParsed.get("coordinate");
					longitude = (Double) lonParsed.get("coordinate");
				} else {
					if (geo == null || geo.latitudeNow == null || geo.longitudeNow == null) {
						context.showToast(context.getResources().getString(R.string.err_point_curr_position_unavailable));
						return;
					}

					latitude = geo.latitudeNow;
					longitude = geo.longitudeNow;
				}
			}
			ArrayList<Double> co = new ArrayList<Double>();
			co.add(latitude);
			co.add(longitude);
			cuListener.update(co);
			dismiss();
		}
	}

	public void setOnCoordinateUpdate(CoordinateUpdate cu) {
		cuListener = cu;
	}

	public interface CoordinateUpdate {
		public void update(ArrayList<Double> coords);
	}

}

