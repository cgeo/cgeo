package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.HumanDistance;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

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

import java.util.ArrayList;
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

                    if (CollectionUtils.isEmpty(addresses)) {
                        showToast(res.getString(R.string.err_search_address_no_match));
                        finish();
                        return;
                    } else {
                        LinearLayout oneAddPre = null;
                        final Geopoint lastLoc = cgeoapplication.getInstance().getLastCoords();
                        for (Address address : addresses) {
                            oneAddPre = (LinearLayout) inflater.inflate(R.layout.address_button, null);

                            Button oneAdd = (Button) oneAddPre.findViewById(R.id.button);

                            final int maxIndex = address.getMaxAddressLineIndex();
                            final ArrayList<String> lines = new ArrayList<String>();
                            for (int i = 0; i <= maxIndex; i++) {
                                String line = address.getAddressLine(i);
                                if (StringUtils.isNotBlank(line)) {
                                    lines.add(line);
                                }
                            }

                            final String listTitle = StringUtils.join(lines, "; ");

                            if (lastLoc != null) {
                                if (address.hasLatitude() && address.hasLongitude()) {
                                    lines.add(HumanDistance.getHumanDistance(lastLoc.distanceTo(new Geopoint(address.getLatitude(), address.getLongitude()))));
                                }
                            }

                            oneAdd.setText(StringUtils.join(lines, "\n"));
                            oneAdd.setLines(lines.size());
                            oneAdd.setClickable(true);

                            oneAdd.setOnClickListener(new buttonListener(address.getLatitude(), address.getLongitude(), listTitle));
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

        private final double latitude;
        private final double longitude;
        private final String address;

        public buttonListener(double latitudeIn, double longitudeIn, String addressIn) {
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
