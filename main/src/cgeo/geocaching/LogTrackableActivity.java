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
import cgeo.geocaching.utils.LogTemplateProvider;
import cgeo.geocaching.utils.LogTemplateProvider.LogTemplate;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class LogTrackableActivity extends AbstractActivity implements DateDialog.DateDialogParent {
    private List<LogType> possibleLogTypes = new ArrayList<LogType>();
    private ProgressDialog waitDialog = null;
    private String guid = null;
    private String geocode = null;
    private String[] viewstates = null;
    private boolean gettingViewstate = true;
    private Calendar date = Calendar.getInstance();
    private LogType typeSelected = LogType.getById(Settings.getTrackableAction());
    private int attempts = 0;
    private CheckBox tweetCheck = null;
    private LinearLayout tweetBox = null;

    private static final int MENU_SIGNATURE = 1;

    private Handler showProgressHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            showProgress(true);
        }
    };

    private Handler loadDataHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            if (!possibleLogTypes.contains(typeSelected)) {
                setType(possibleLogTypes.get(0));

                showToast(res.getString(R.string.info_log_type_changed));
            }

            if (Login.isEmpty(viewstates)) {
                if (attempts < 2) {
                    showToast(res.getString(R.string.err_log_load_data_again));
                    new LoadDataThread().start();
                } else {
                    showToast(res.getString(R.string.err_log_load_data));
                    showProgress(false);
                }
                return;
            }

            gettingViewstate = false; // we're done, user can post log

            final Button buttonPost = (Button) findViewById(R.id.post);
            buttonPost.setEnabled(true);
            buttonPost.setOnClickListener(new PostListener());

            showProgress(false);
        }
    };

    private final Handler postLogHandler = new Handler() {

        @Override
        public void handleMessage(final Message msg) {
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

    public LogTrackableActivity() {
        super("c:geo-log-trackable");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme();
        setContentView(R.layout.touch);
        setTitle(res.getString(R.string.trackable_touch));

        // get parameters
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            geocode = extras.getString("geocode");
            guid = extras.getString("guid");

            if (StringUtils.isNotBlank(extras.getString("trackingcode"))) {
                ((EditText) findViewById(R.id.tracking)).setText(extras.getString("trackingcode"));
            }
        }

        final cgTrackable trackable = app.getTrackableByGeocode(geocode);

        if (StringUtils.isNotBlank(trackable.getName())) {
            setTitle(res.getString(R.string.trackable_touch) + ": " + trackable.getName());
        } else {
            setTitle(res.getString(R.string.trackable_touch) + ": " + trackable.getGeocode().toUpperCase());
        }

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
        final SubMenu subMenu = menu.addSubMenu(0, 0, 0, res.getString(R.string.log_add)).setIcon(R.drawable.ic_menu_add);

        for (LogTemplate template : LogTemplateProvider.getTemplates()) {
            subMenu.add(0, template.getItemId(), 0, template.getResourceId());
        }
        subMenu.add(0, MENU_SIGNATURE, 0, res.getString(R.string.init_signature));

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(MENU_SIGNATURE).setVisible(Settings.getSignature() != null);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();

        if (id == MENU_SIGNATURE) {
            insertIntoLog(LogTemplateProvider.applyTemplates(Settings.getSignature(), false), true);
            return true;
        }

        final LogTemplate template = LogTemplateProvider.getTemplate(id);
        if (template != null) {
            insertIntoLog(template.getValue(false), true);
            return true;
        }

        return false;
    }

    private void insertIntoLog(String newText, final boolean moveCursor) {
        final EditText log = (EditText) findViewById(R.id.log);
        insertAtPosition(log, newText, moveCursor);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info) {
        super.onCreateContextMenu(menu, view, info);
        final int viewId = view.getId();

        if (viewId == R.id.type) {
            for (final LogType typeOne : possibleLogTypes) {
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
        final Button typeButton = (Button) findViewById(R.id.type);
        registerForContextMenu(typeButton);
        typeButton.setText(typeSelected.getL10n());
        typeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openContextMenu(view);
            }
        });

        final Button dateButton = (Button) findViewById(R.id.date);
        dateButton.setOnClickListener(new DateListener());
        setDate(date);

        if (tweetBox == null) {
            tweetBox = (LinearLayout) findViewById(R.id.tweet_box);
        }
        if (tweetCheck == null) {
            tweetCheck = (CheckBox) findViewById(R.id.tweet);
        }
        tweetCheck.setChecked(true);

        if (CollectionUtils.isEmpty(possibleLogTypes)) {
            possibleLogTypes = cgTrackable.getPossibleLogTypes();
        }

        final Button buttonPost = (Button) findViewById(R.id.post);
        if (Login.isEmpty(viewstates)) {
            buttonPost.setEnabled(false);
            buttonPost.setOnTouchListener(null);
            buttonPost.setOnClickListener(null);

            new LoadDataThread().start();
        } else {
            buttonPost.setEnabled(true);
            buttonPost.setOnClickListener(new PostListener());
        }
        disableSuggestions((EditText) findViewById(R.id.tracking));
    }

    @Override
    public void setDate(Calendar dateIn) {
        date = dateIn;

        final Button dateButton = (Button) findViewById(R.id.date);
        dateButton.setText(Formatter.formatShortDateVerbally(date.getTime().getTime()));
    }

    public void setType(LogType type) {
        final Button typeButton = (Button) findViewById(R.id.type);

        typeSelected = type;
        typeButton.setText(typeSelected.getL10n());

        if (Settings.isUseTwitter()) {
            tweetBox.setVisibility(View.VISIBLE);
        } else {
            tweetBox.setVisibility(View.GONE);
        }
    }

    private class DateListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            final Dialog dateDialog = new DateDialog(LogTrackableActivity.this, LogTrackableActivity.this, date);
            dateDialog.setCancelable(true);
            dateDialog.show();
        }
    }

    private class PostListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            if (!gettingViewstate) {
                waitDialog = ProgressDialog.show(LogTrackableActivity.this, null, res.getString(R.string.log_saving), true);
                waitDialog.setCancelable(true);

                Settings.setTrackableAction(typeSelected.id);

                final String tracking = ((EditText) findViewById(R.id.tracking)).getText().toString();
                final String log = ((EditText) findViewById(R.id.log)).getText().toString();
                new PostLogThread(postLogHandler, tracking, log).start();
            } else {
                showToast(res.getString(R.string.err_log_load_data_still));
            }
        }
    }

    private class LoadDataThread extends Thread {

        public LoadDataThread() {
            super("Load data for logging trackable");
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
                if (CollectionUtils.isNotEmpty(typesPre)) {
                    possibleLogTypes.clear();
                    possibleLogTypes.addAll(typesPre);
                }
            } catch (Exception e) {
                Log.e("LogTrackableActivity.LoadDataThread.run: " + e.toString());
            }

            loadDataHandler.sendEmptyMessage(0);
        }
    }

    private class PostLogThread extends Thread {
        final private Handler handler;
        final private String tracking;
        final private String log;

        public PostLogThread(final Handler handlerIn, final String trackingIn, final String logIn) {
            super("Post trackable log");
            handler = handlerIn;
            tracking = trackingIn;
            log = logIn;
        }

        @Override
        public void run() {
            final StatusCode status = postLogFn(tracking, log);
            handler.sendMessage(handler.obtainMessage(0, status));
        }
    }

    public StatusCode postLogFn(String tracking, String log) {
        try {
            final StatusCode status = GCParser.postLogTrackable(guid, tracking, viewstates, typeSelected, date.get(Calendar.YEAR), (date.get(Calendar.MONTH) + 1), date.get(Calendar.DATE), log);

            if (status == StatusCode.NO_ERROR && Settings.isUseTwitter() &&
                    Settings.isTwitterLoginValid() &&
                    tweetCheck.isChecked() && tweetBox.getVisibility() == View.VISIBLE) {
                Twitter.postTweetTrackable(geocode);
            }

            return status;
        } catch (Exception e) {
            Log.e("LogTrackableActivity.postLogFn: " + e.toString());
        }

        return StatusCode.LOG_POST_ERROR;
    }

    public static void startActivity(final Context context, final cgTrackable trackable) {
        final Intent logTouchIntent = new Intent(context, LogTrackableActivity.class);
        logTouchIntent.putExtra("geocode", trackable.getGeocode().toUpperCase());
        logTouchIntent.putExtra("guid", trackable.getGuid());
        logTouchIntent.putExtra("trackingcode", trackable.getTrackingcode());
        context.startActivity(logTouchIntent);
    }
}
