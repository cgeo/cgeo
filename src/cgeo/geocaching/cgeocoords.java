package cgeo.geocaching;


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
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Geopoint.GeopointException;
import cgeo.geocaching.geopoint.GeopointFormatter;

public class cgeocoords extends Dialog {

	private AbstractActivity context = null;
	private cgSettings settings = null;
	private cgGeo geo = null;
	private Geopoint gp = null;

	private EditText eLat, eLon;
	private Button bLat, bLon;
	private EditText eLatDeg, eLatMin, eLatSec, eLatSub;
	private EditText eLonDeg, eLonMin, eLonSec, eLonSub;
	private TextView tLatSep1, tLatSep2, tLatSep3;
	private TextView tLonSep1, tLonSep2, tLonSep3;

	private Spinner spinner;

	CoordinateUpdate cuListener;

	coordInputFormatEnum currentFormat = null;

	public cgeocoords(final AbstractActivity contextIn, cgSettings settingsIn, final Geopoint gpIn, final cgGeo geoIn) {
		super(contextIn);
		context = contextIn;
		settings = settingsIn;
		geo = geoIn;

		if (gpIn != null) {
			gp = gpIn;
		} else if (geo != null && geo.latitudeNow != null && geo.longitudeNow != null) {
			gp = new Geopoint(geo.latitudeNow, geo.longitudeNow);
		} else {
			gp = new Geopoint();
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

		spinner = (Spinner) findViewById(R.id.spinnerCoordinateFormats);
		ArrayAdapter<CharSequence> adapter =
				ArrayAdapter.createFromResource(context,
				                                R.array.waypoint_coordinate_formats,
				                                android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setSelection(settings.getCoordInputFormat().ordinal());
		spinner.setOnItemSelectedListener(new CoordinateFormatListener());

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

		eLatDeg.addTextChangedListener(new TextChanged(eLatDeg));
		eLatMin.addTextChangedListener(new TextChanged(eLatMin));
		eLatSec.addTextChangedListener(new TextChanged(eLatSec));
		eLatSub.addTextChangedListener(new TextChanged(eLatSub));
		eLonDeg.addTextChangedListener(new TextChanged(eLonDeg));
		eLonMin.addTextChangedListener(new TextChanged(eLonMin));
		eLonSec.addTextChangedListener(new TextChanged(eLonSec));
		eLonSub.addTextChangedListener(new TextChanged(eLonSub));

		bLat.setOnClickListener(new ButtonClickListener());
		bLon.setOnClickListener(new ButtonClickListener());

		Button buttonCurrent = (Button) findViewById(R.id.current);
		buttonCurrent.setOnClickListener(new CurrentListener());
		Button buttonDone = (Button) findViewById(R.id.done);
		buttonDone.setOnClickListener(new InputDoneListener());
	}

	private void updateGUI() {
		Double lat = 0.0;
		if (gp.getLatitude() < 0) {
			bLat.setText("S");
		} else {
			bLat.setText("N");
		}

		lat = Math.abs(gp.getLatitude());

		Double lon = 0.0;
		if (gp.getLongitude() < 0) {
			bLon.setText("W");
		} else {
			bLon.setText("E");
		}

		lon = Math.abs(gp.getLongitude());

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
				eLat.setText(gp.format(GeopointFormatter.Format.LAT_DECMINUTE));
				eLon.setText(gp.format(GeopointFormatter.Format.LON_DECMINUTE));
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

				eLatDeg.setText(addZeros(latDeg, 2) + Integer.toString(latDeg));
				eLatMin.setText(addZeros(latDegFrac, 5) + Integer.toString(latDegFrac));
				eLonDeg.setText(addZeros(latDeg, 3) + Integer.toString(lonDeg));
				eLonMin.setText(addZeros(lonDegFrac, 5) + Integer.toString(lonDegFrac));
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

				eLatDeg.setText(addZeros(latDeg, 2) + Integer.toString(latDeg));
				eLatMin.setText(addZeros(latMin, 2) + Integer.toString(latMin));
				eLatSec.setText(addZeros(latMinFrac, 3) + Integer.toString(latMinFrac));
				eLonDeg.setText(addZeros(lonDeg, 3) + Integer.toString(lonDeg));
				eLonMin.setText(addZeros(lonMin, 2) + Integer.toString(lonMin));
				eLonSec.setText(addZeros(lonMinFrac, 3) + Integer.toString(lonMinFrac));
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

				eLatDeg.setText(addZeros(latDeg, 2) + Integer.toString(latDeg));
				eLatMin.setText(addZeros(latMin, 2) + Integer.toString(latMin));
				eLatSec.setText(addZeros(latSec, 2) + Integer.toString(latSec));
				eLatSub.setText(addZeros(latSecFrac, 3) + Integer.toString(latSecFrac));
				eLonDeg.setText(addZeros(lonDeg, 3) + Integer.toString(lonDeg));
				eLonMin.setText(addZeros(lonMin, 2) + Integer.toString(lonMin));
				eLonSec.setText(addZeros(lonSec, 2) + Integer.toString(lonSec));
				eLonSub.setText(addZeros(lonSecFrac, 3) + Integer.toString(lonSecFrac));
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

	private class ButtonClickListener implements View.OnClickListener {

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

	private class TextChanged implements TextWatcher {

		private EditText	editText;

		public TextChanged(EditText editText) {
			this.editText = editText;
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

			if (currentFormat == coordInputFormatEnum.Plain)
				return;

			int maxLength = 2;
			if (editText == eLonDeg || editText == eLatSub || editText == eLonSub) {
				maxLength = 3;
			}
			if ((editText == eLatMin || editText == eLonMin) && currentFormat == coordInputFormatEnum.Deg) {
				maxLength = 5;
			}
			if ((editText == eLatSec || editText == eLonSec) && currentFormat == coordInputFormatEnum.Min) {
				maxLength = 3;
			}

			if (s.length() == maxLength) {
				if (editText == eLatDeg)
					eLatMin.requestFocus();
				else
				if (editText == eLatMin)
					if (eLatSec.getVisibility() == View.GONE)
						eLonDeg.requestFocus();
					else
						eLatSec.requestFocus();
				else
				if (editText == eLatSec)
					if (eLatSub.getVisibility() == View.GONE)
						eLonDeg.requestFocus();
					else
						eLatSub.requestFocus();
				else
				if (editText == eLatSub)
					eLonDeg.requestFocus();
				else
				if (editText == eLonDeg)
					eLonMin.requestFocus();
				else
				if (editText == eLonMin)
					if (eLonSec.getVisibility() == View.GONE)
						eLatDeg.requestFocus();
					else
						eLonSec.requestFocus();
				else
				if (editText == eLonSec)
					if (eLonSub.getVisibility() == View.GONE)
						eLatDeg.requestFocus();
					else
						eLonSub.requestFocus();
				else
				if (editText == eLonSub)
					eLatDeg.requestFocus();
			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {}

	}

	private void calc() {
		if (currentFormat == coordInputFormatEnum.Plain) {
			gp.setLatitude(eLat.getText().toString());
			gp.setLongitude(eLon.getText().toString());
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

		Double latitude = 0.0;
		Double longitude = 0.0;
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
		gp.setLatitude(latitude);
		gp.setLongitude(longitude);
	}

	private class CoordinateFormatListener implements OnItemSelectedListener {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
			try {
				// Ignore first call, which comes from onCreate()
				if (currentFormat != null)
					calc();
			} catch (GeopointException e) {
				context.showToast(e.getMessage());
				spinner.setSelection(currentFormat.ordinal());
				return;
			}

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

			gp.setLatitude(geo.latitudeNow);
			gp.setLongitude(geo.longitudeNow);
			updateGUI();
		}
	}

	private class InputDoneListener implements View.OnClickListener {

		@Override
		public void onClick(View v) {
			try {
				calc();
			} catch (GeopointException e) {
				context.showToast(e.getMessage());
				return;
			}

			cuListener.update(gp);
			dismiss();
		}
	}

	public void setOnCoordinateUpdate(CoordinateUpdate cu) {
		cuListener = cu;
	}

	public interface CoordinateUpdate {
		public void update(Geopoint gp);
	}

}

