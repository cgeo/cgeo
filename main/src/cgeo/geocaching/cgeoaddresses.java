package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;

import android.app.ProgressDialog;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.List;
import java.util.Locale;

public class cgeoaddresses extends AbstractActivity {
    private String keyword = null;
    private LayoutInflater inflater = null;
    private LinearLayout addList = null;
    private ProgressDialog waitDialog = null;

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

        new AsyncTask<Void, Void, List<Address>>() {

            @Override
            protected List<Address> doInBackground(Void... params) {
                final Geocoder geocoder = new Geocoder(cgeoaddresses.this, Locale.getDefault());
                try {
                    return geocoder.getFromLocationName(keyword, 20);
                } catch (Exception e) {
                    Log.e(Settings.tag, "cgeoaddresses.doInBackground", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(final List<Address> addresses) {
                waitDialog.dismiss();
                try {
                    if (addList == null) {
                        addList = (LinearLayout) findViewById(R.id.address_list);
                    }

                    if (addresses == null || addresses.isEmpty()) {
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
                } catch (Exception e) {
                    Log.e(Settings.tag, "cgeoaddresses.onPostExecute", e);
                }
            }

        }.execute();
    }

    @Override
    public void onResume() {
        super.onResume();

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
            cgeocaches.startActivityAddress(cgeoaddresses.this, latitude, longitude, address);
            finish();
            return;
        }
    }
}
