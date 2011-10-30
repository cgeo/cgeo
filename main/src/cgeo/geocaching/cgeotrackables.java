package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

public class cgeotrackables extends AbstractActivity {
    private List<cgTrackable> trackables = new ArrayList<cgTrackable>();
    private String geocode = null;
    private LayoutInflater inflater = null;
    private LinearLayout addList = null;
    private ProgressDialog waitDialog = null;
    private Handler loadInventoryHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            try {
                if (inflater == null) {
                    inflater = getLayoutInflater();
                }

                if (addList == null) {
                    addList = (LinearLayout) findViewById(R.id.trackable_list);
                }

                if (trackables.isEmpty()) {
                    if (waitDialog != null) {
                        waitDialog.dismiss();
                    }

                    showToast(res.getString(R.string.err_cache_inventory_load_fail));

                    finish();
                    return;
                } else if (trackables.size() == 1) {
                    cgTrackable trackable = trackables.get(0);
                    cgeotrackable.startActivity(cgeotrackables.this, trackable.getGuid(), trackable.getGeocode(), trackable.getName());
                    finish();
                    return;
                } else {
                    LinearLayout oneTbPre = null;
                    for (cgTrackable trackable : trackables) {
                        oneTbPre = (LinearLayout) inflater.inflate(R.layout.trackable_button, null);

                        Button oneTb = (Button) oneTbPre.findViewById(R.id.button);

                        if (trackable.getName() != null) {
                            oneTb.setText(Html.fromHtml(trackable.getName()).toString());
                        } else {
                            oneTb.setText("some trackable");
                        }
                        oneTb.setClickable(true);
                        oneTb.setOnClickListener(new buttonListener(trackable.getGuid(), trackable.getGeocode(), trackable.getName()));
                        addList.addView(oneTbPre);
                    }
                }

                if (waitDialog != null) {
                    waitDialog.dismiss();
                }
            } catch (Exception e) {
                if (waitDialog != null) {
                    waitDialog.dismiss();
                }
                Log.e(Settings.tag, "cgeotrackables.loadInventoryHandler: " + e.toString());
            }
        }
    };

    public cgeotrackables() {
        super("c:geo-trackable-list");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme();
        setContentView(R.layout.trackables);
        setTitle(res.getString(R.string.trackable));

        // get parameters
        Bundle extras = getIntent().getExtras();

        // try to get data from extras
        if (extras != null) {
            geocode = extras.getString("geocode");
        }

        if (geocode == null) {
            showToast(res.getString(R.string.err_tb_forgot));
            finish();
            return;
        }

        waitDialog = ProgressDialog.show(this, null, res.getString(R.string.cache_inventory_loading), true);
        waitDialog.setCancelable(true);

        (new loadInventory()).start();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    private class loadInventory extends Thread {

        @Override
        public void run() {
            try {
                trackables = app.loadInventory(geocode);

                loadInventoryHandler.sendMessage(new Message());
            } catch (Exception e) {
                Log.e(Settings.tag, "cgeotrackables.loadInventory.run: " + e.toString());
            }
        }
    }

    private class buttonListener implements View.OnClickListener {

        private String guid = null;
        private String geocode = null;
        private String name = null;

        public buttonListener(String guidIn, String geocodeIn, String nameIn) {
            guid = guidIn;
            geocode = geocodeIn;
            name = nameIn;
        }

        public void onClick(View arg0) {
            cgeotrackable.startActivity(cgeotrackables.this, guid, geocode, name);
            return;
        }
    }
}
