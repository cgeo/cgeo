package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.connector.gc.GCParser;
import cgeo.geocaching.connector.gc.Login;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.twitter.Twitter;
import cgeo.geocaching.ui.DateDialog;
import cgeo.geocaching.ui.Formatter;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class cgeotouch extends AbstractActivity implements DateDialog.DateDialogParent {
    private List<LogType> logTypes = new ArrayList<LogType>();
    private ProgressDialog waitDialog = null;
    private String guid = null;
    private String geocode = null;
    private String[] viewstates = null;
    private boolean gettingViewstate = true;
    private Calendar date = Calendar.getInstance();
    private LogType typeSelected = LogType.LOG_UNKNOWN;
    private int attempts = 0;
    private CheckBox tweetCheck = null;
    private LinearLayout tweetBox = null;

    private static final int MSG_UPDATE_TYPE = 1;

    private Handler showProgressHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            showProgress(true);
        }
    };

    private Handler loadDataHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            if (msg.what == MSG_UPDATE_TYPE) {
                setType((LogType) msg.obj);
                showToast(res.getString(R.string.info_log_type_changed));
            } else {
                if (Login.isEmpty(viewstates)) {
                    if (attempts < 2) {
                        showToast(res.getString(R.string.err_log_load_data_again));
                        new loadData(guid).start();
                    } else {
                        showToast(res.getString(R.string.err_log_load_data));
                        showProgress(false);
                    }
                } else {
                    gettingViewstate = false; // we're done, user can post log

                    final Button buttonPost = (Button) findViewById(R.id.post);
                    buttonPost.setEnabled(true);
                    buttonPost.setOnClickListener(new postListener());

                    showProgress(false);
                }
            }
        }
    };

    private Handler postLogHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (waitDialog != null) {
                waitDialog.dismiss();
            }

            final StatusCode error = (StatusCode) msg.obj;
            if (error == StatusCode.NO_ERROR) {
                showToast(res.getString(R.string.info_log_posted));
                finish();
            } else {
                showToast(error.getErrorString(res));
            }
        }
    };

    public cgeotouch() {
        super("c:geo-log-trackable");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme();
        setContentView(R.layout.touch);
        setTitle(res.getString(R.string.trackable_touch));

        // get parameters
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            geocode = extras.getString("geocode");
            guid = extras.getString("guid");

            if (StringUtils.isNotBlank(extras.getString("trackingcode"))) {
                ((EditText) findViewById(R.id.tracking)).setText(extras.getString("trackingcode"));
            }
        }

        final cgTrackable trackable = app.getTrackableByGeocode("logging trackable");

        if (StringUtils.isNotBlank(trackable.getName())) {
            setTitle(res.getString(R.string.trackable_touch) + trackable.getName());
        } else {
            setTitle(res.getString(R.string.trackable_touch) + trackable.getGeocode().toUpperCase());
        }

        app.setAction("logging trackable");

        if (guid == null) {
            showToast(res.getString(R.string.err_tb_forgot_saw));

            finish();
            return;
        }

        init();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        SubMenu subMenu = menu.addSubMenu(0, 0, 0, res.getString(R.string.log_add)).setIcon(android.R.drawable.ic_menu_add);

        subMenu.add(0, 0x6, 0, res.getString(R.string.log_date_time));
        subMenu.add(0, 0x4, 0, res.getString(R.string.log_date));
        subMenu.add(0, 0x2, 0, res.getString(R.string.log_time));
        subMenu.add(0, 0x1, 0, res.getString(R.string.init_signature));
        subMenu.add(0, 0x7, 0, res.getString(R.string.log_date_time) + " & " + res.getString(R.string.init_signature));

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (Settings.getSignature() == null) {
            menu.findItem(0x1).setVisible(false);
            menu.findItem(0x7).setVisible(false);
        } else {
            menu.findItem(0x1).setVisible(true);
            menu.findItem(0x7).setVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        EditText text;
        String textContent;
        String dateString;
        String timeString;
        String addText = "";

        if ((id >= 0x1 && id <= 0x7)) {
            text = (EditText) findViewById(R.id.log);
            textContent = text.getText().toString();

            final long now = System.currentTimeMillis();
            dateString = Formatter.formatDate(now);
            timeString = Formatter.formatTime(now);

            if ((id & 0x4) == 0x4) {
                addText += dateString;
                if ((id & 0x2) == 0x2) {
                    addText += " | ";
                }
            }
            if ((id & 0x2) == 0x2) {
                addText += timeString;
            }
            if ((id & 0x1) == 0x1 && Settings.getSignature() != null) {
                if (addText.length() > 0) {
                    addText += "\n";
                }
                // number replaced by empty string (there are no numbers for trackables)
                addText += StringUtils.replaceEach(Settings.getSignature(),
                        new String[] { "[DATE]", "[TIME]", "[USER]", "[NUMBER]" },
                        new String[] { dateString, timeString, Settings.getUsername(), "" });
            }
            if (textContent.length() > 0 && addText.length() > 0) {
                addText = "\n" + addText;
            }
            text.setText(textContent + addText, TextView.BufferType.NORMAL);
            text.setSelection(text.getText().toString().length());
            return true;
        }

        return false;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info) {
        super.onCreateContextMenu(menu, view, info);
        final int viewId = view.getId();

        if (viewId == R.id.type) {
            for (final LogType typeOne : logTypes) {
                menu.add(viewId, typeOne.id, 0, typeOne.getL10n());
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final int group = item.getGroupId();
        final int id = item.getItemId();

        if (group == R.id.type) {
            setType(LogType.getById(id));

            return true;
        }

        return false;
    }

    public void init() {
        if (geocode != null) {
            app.setAction("logging trackable");
        }

        logTypes.clear();
        logTypes.add(LogType.LOG_RETRIEVED_IT);
        logTypes.add(LogType.LOG_GRABBED_IT);
        logTypes.add(LogType.LOG_NOTE);
        logTypes.add(LogType.LOG_DISCOVERED_IT);

        if (LogType.LOG_UNKNOWN == typeSelected) {
            typeSelected = LogType.LOG_RETRIEVED_IT;
        }
        setType(typeSelected);

        Button typeButton = (Button) findViewById(R.id.type);
        registerForContextMenu(typeButton);
        typeButton.setText(typeSelected.getL10n());
        typeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                openContextMenu(view);
            }
        });

        Button dateButton = (Button) findViewById(R.id.date);
        dateButton.setText(Formatter.formatShortDate(date.getTime().getTime()));
        dateButton.setOnClickListener(new cgeotouchDateListener());

        if (tweetBox == null) {
            tweetBox = (LinearLayout) findViewById(R.id.tweet_box);
        }
        if (tweetCheck == null) {
            tweetCheck = (CheckBox) findViewById(R.id.tweet);
        }
        tweetCheck.setChecked(true);

        Button buttonPost = (Button) findViewById(R.id.post);
        if (Login.isEmpty(viewstates)) {
            buttonPost.setEnabled(false);
            buttonPost.setOnTouchListener(null);
            buttonPost.setOnClickListener(null);

            loadData thread;
            thread = new loadData(guid);
            thread.start();
        } else {
            buttonPost.setEnabled(true);
            buttonPost.setOnClickListener(new postListener());
        }
        disableSuggestions((EditText) findViewById(R.id.tracking));
    }

    @Override
    public void setDate(Calendar dateIn) {
        date = dateIn;

        final Button dateButton = (Button) findViewById(R.id.date);
        dateButton.setText(Formatter.formatShortDate(date.getTime().getTime()));
    }

    public void setType(LogType type) {
        final Button typeButton = (Button) findViewById(R.id.type);

        typeSelected = type;
        typeButton.setText(typeSelected.getL10n());

        if (tweetBox == null) {
            tweetBox = (LinearLayout) findViewById(R.id.tweet_box);
        }
        if (Settings.isUseTwitter()) {
            tweetBox.setVisibility(View.VISIBLE);
        } else {
            tweetBox.setVisibility(View.GONE);
        }
    }

    private class cgeotouchDateListener implements View.OnClickListener {
        public void onClick(View arg0) {
            Dialog dateDialog = new DateDialog(cgeotouch.this, cgeotouch.this, date);
            dateDialog.setCancelable(true);
            dateDialog.show();
        }
    }

    private class postListener implements View.OnClickListener {
        public void onClick(View arg0) {
            if (!gettingViewstate) {
                waitDialog = ProgressDialog.show(cgeotouch.this, null, res.getString(R.string.log_saving), true);
                waitDialog.setCancelable(true);

                String tracking = ((EditText) findViewById(R.id.tracking)).getText().toString();
                String log = ((EditText) findViewById(R.id.log)).getText().toString();
                Thread thread = new postLog(postLogHandler, tracking, log);
                thread.start();
            } else {
                showToast(res.getString(R.string.err_log_load_data_still));
            }
        }
    }

    private class loadData extends Thread {
        private String guid = null;

        public loadData(String guidIn) {
            guid = guidIn;

            if (guid == null) {
                showToast(res.getString(R.string.err_tb_forgot_saw));

                finish();
            }
        }

        @Override
        public void run() {
            final Parameters params = new Parameters();

            showProgressHandler.sendEmptyMessage(0);
            gettingViewstate = true;
            attempts++;

            try {
                if (StringUtils.isNotBlank(guid)) {
                    params.put("wid", guid);
                } else {
                    loadDataHandler.sendEmptyMessage(0);
                    return;
                }

                final String page = Network.getResponseData(Network.getRequest("http://www.geocaching.com/track/log.aspx", params));

                viewstates = Login.getViewstates(page);

                final List<LogType> typesPre = GCParser.parseTypes(page);
                if (typesPre.size() > 0) {
                    logTypes.clear();
                    logTypes.addAll(typesPre);
                }
                typesPre.clear();

                if (!logTypes.contains(typeSelected)) {
                    typeSelected = logTypes.get(0);
                    loadDataHandler.obtainMessage(MSG_UPDATE_TYPE, typeSelected).sendToTarget();
                }
            } catch (Exception e) {
                Log.e("cgeotouch.loadData.run: " + e.toString());
            }

            loadDataHandler.sendEmptyMessage(0);
        }
    }

    private class postLog extends Thread {
        final private Handler handler;
        final private String tracking;
        final private String log;

        public postLog(final Handler handler, final String tracking, final String log) {
            this.handler = handler;
            this.tracking = tracking;
            this.log = log;
        }

        @Override
        public void run() {
            final StatusCode error = postLogFn(tracking, log);
            handler.sendMessage(handler.obtainMessage(0, error));
        }
    }

    public StatusCode postLogFn(String tracking, String log) {
        try {
            if (tweetBox == null) {
                tweetBox = (LinearLayout) findViewById(R.id.tweet_box);
            }
            if (tweetCheck == null) {
                tweetCheck = (CheckBox) findViewById(R.id.tweet);
            }

            final StatusCode status = GCParser.postLogTrackable(guid, tracking, viewstates, typeSelected, date.get(Calendar.YEAR), (date.get(Calendar.MONTH) + 1), date.get(Calendar.DATE), log);

            if (status == StatusCode.NO_ERROR && Settings.isUseTwitter() &&
                    Settings.isTwitterLoginValid() &&
                    tweetCheck.isChecked() && tweetBox.getVisibility() == View.VISIBLE) {
                Twitter.postTweetTrackable(geocode);
            }

            return status;
        } catch (Exception e) {
            Log.e("cgeotouch.postLogFn: " + e.toString());
        }

        return StatusCode.LOG_POST_ERROR;
    }
}
