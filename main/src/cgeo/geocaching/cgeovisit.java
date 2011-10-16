package cgeo.geocaching;

import cgeo.geocaching.LogTemplateProvider.LogTemplate;
import cgeo.geocaching.enumerations.LogTypeTrackable;
import cgeo.geocaching.enumerations.StatusCode;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class cgeovisit extends cgLogForm {
    static final String EXTRAS_FOUND = "found";
    static final String EXTRAS_TEXT = "text";
    static final String EXTRAS_GEOCODE = "geocode";
    static final String EXTRAS_ID = "id";

    private static final int MENU_SIGNATURE = 1;
    private static final int SUBMENU_VOTE = 2;

    private LayoutInflater inflater = null;
    private cgCache cache = null;
    private List<Integer> types = new ArrayList<Integer>();
    private ProgressDialog waitDialog = null;
    private String cacheid = null;
    private String geocode = null;
    private String text = null;
    private boolean alreadyFound = false;
    private String[] viewstates = null;
    private Boolean gettingViewstate = true;
    private List<cgTrackableLog> trackables = null;
    private Calendar date = Calendar.getInstance();
    private int typeSelected = 1;
    private int attempts = 0;
    private boolean progressBar = false;
    private Button post = null;
    private Button save = null;
    private Button clear = null;
    private CheckBox tweetCheck = null;
    private LinearLayout tweetBox = null;
    private double rating = 0.0;
    private boolean tbChanged = false;

    // handlers
    private Handler showProgressHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (progressBar) {
                showProgress(true);
            }
        }
    };
    private Handler loadDataHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (!types.contains(typeSelected)) {
                typeSelected = types.get(0);
                setType(typeSelected);

                showToast(res.getString(R.string.info_log_type_changed));
            }

            if (cgBase.isEmpty(viewstates) && attempts < 2) {
                showToast(res.getString(R.string.err_log_load_data_again));

                loadData thread;
                thread = new loadData();
                thread.start();

                return;
            } else if (cgBase.isEmpty(viewstates) && attempts >= 2) {
                showToast(res.getString(R.string.err_log_load_data));
                showProgress(false);

                return;
            }

            gettingViewstate = false; // we're done, user can post log

            if (post == null) {
                post = (Button) findViewById(R.id.post);
            }
            post.setEnabled(true);
            post.setOnClickListener(new postListener());

            // add trackables
            if (CollectionUtils.isNotEmpty(trackables)) {
                if (inflater == null) {
                    inflater = getLayoutInflater();
                }

                final LinearLayout inventoryView = (LinearLayout) findViewById(R.id.inventory);
                inventoryView.removeAllViews();

                for (cgTrackableLog tb : trackables) {
                    LinearLayout inventoryItem = (LinearLayout) inflater.inflate(R.layout.visit_trackable, null);

                    ((TextView) inventoryItem.findViewById(R.id.trackcode)).setText(tb.trackCode);
                    ((TextView) inventoryItem.findViewById(R.id.name)).setText(tb.name);
                    ((TextView) inventoryItem.findViewById(R.id.action)).setText(res.getString(Settings.isTrackableAutoVisit() ? LogTypeTrackable.VISITED.resourceId : LogTypeTrackable.DO_NOTHING.resourceId));

                    inventoryItem.setId(tb.id);
                    final String tbCode = tb.trackCode;
                    inventoryItem.setClickable(true);
                    registerForContextMenu(inventoryItem);
                    inventoryItem.findViewById(R.id.info).setOnClickListener(new View.OnClickListener() {

                        public void onClick(View view) {
                            final Intent trackablesIntent = new Intent(cgeovisit.this, cgeotrackable.class);
                            trackablesIntent.putExtra(EXTRAS_GEOCODE, tbCode);
                            startActivity(trackablesIntent);
                        }
                    });
                    inventoryItem.findViewById(R.id.action).setOnClickListener(new View.OnClickListener() {

                        public void onClick(View view) {
                            openContextMenu(view);
                        }
                    });

                    inventoryView.addView(inventoryItem);

                    if (Settings.isTrackableAutoVisit())
                    {
                        tb.action = LogTypeTrackable.VISITED;
                        tbChanged = true;
                    }
                }

                if (inventoryView.getChildCount() > 0) {
                    ((LinearLayout) findViewById(R.id.inventory_box)).setVisibility(View.VISIBLE);
                }
                if (inventoryView.getChildCount() > 1) {
                    final LinearLayout inventoryChangeAllView = (LinearLayout) findViewById(R.id.inventory_changeall);

                    Button changeButton = (Button) inventoryChangeAllView.findViewById(R.id.changebutton);
                    registerForContextMenu(changeButton);
                    changeButton.setOnClickListener(new View.OnClickListener() {

                        public void onClick(View view) {
                            openContextMenu(view);
                        }
                    });

                    ((LinearLayout) findViewById(R.id.inventory_changeall)).setVisibility(View.VISIBLE);
                }
            }

            showProgress(false);
        }
    };

    private Handler postLogHandler = new Handler() {

        @Override
        public void handleMessage(final Message msg) {
            if (waitDialog != null) {
                waitDialog.dismiss();
            }

            final StatusCode error = (StatusCode) msg.obj;
            if (error == StatusCode.NO_ERROR) {
                showToast(res.getString(R.string.info_log_posted));
                // No need to save the log when quitting if it has been posted.
                text = currentLogText();
                finish();
            } else if (error == StatusCode.LOG_SAVED) {
                showToast(res.getString(R.string.info_log_saved));

                if (waitDialog != null) {
                    waitDialog.dismiss();
                }

                finish();
            } else {
                showToast(error.getErrorString(res));
            }
        }
    };

    public cgeovisit() {
        super("c:geo-log");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme();
        setContentView(R.layout.visit);
        setTitle(res.getString(R.string.log_new_log));

        // get parameters
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            cacheid = extras.getString(EXTRAS_ID);
            geocode = extras.getString(EXTRAS_GEOCODE);
            text = extras.getString(EXTRAS_TEXT);
            alreadyFound = extras.getBoolean(EXTRAS_FOUND);
        }

        if ((StringUtils.isBlank(cacheid)) && StringUtils.isNotBlank(geocode)) {
            cacheid = app.getCacheid(geocode);
        }
        if (StringUtils.isBlank(geocode) && StringUtils.isNotBlank(cacheid)) {
            geocode = app.getGeocode(cacheid);
        }

        cache = app.getCacheByGeocode(geocode);

        if (StringUtils.isNotBlank(cache.name)) {
            setTitle(res.getString(R.string.log_new_log) + " " + cache.name);
        } else {
            setTitle(res.getString(R.string.log_new_log) + " " + cache.geocode.toUpperCase());
        }

        app.setAction(geocode);

        init();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onStop() {
        super.onStop();
        saveLog(false);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        SubMenu menuLog = null;

        menuLog = menu.addSubMenu(0, 0, 0, res.getString(R.string.log_add)).setIcon(android.R.drawable.ic_menu_add);
        for (LogTemplate template : LogTemplateProvider.getTemplates()) {
            menuLog.add(0, template.getItemId(), 0, template.getResourceId());
        }
        menuLog.add(0, MENU_SIGNATURE, 0, res.getString(R.string.init_signature));

        SubMenu menuStars = menu.addSubMenu(0, SUBMENU_VOTE, 0, res.getString(R.string.log_rating)).setIcon(android.R.drawable.ic_menu_sort_by_size);
        menuStars.add(0, 10, 0, res.getString(R.string.log_no_rating));
        menuStars.add(0, 19, 0, res.getString(R.string.log_stars_5) + " (" + res.getString(R.string.log_stars_5_description) + ")");
        menuStars.add(0, 18, 0, res.getString(R.string.log_stars_45) + " (" + res.getString(R.string.log_stars_45_description) + ")");
        menuStars.add(0, 17, 0, res.getString(R.string.log_stars_4) + " (" + res.getString(R.string.log_stars_4_description) + ")");
        menuStars.add(0, 16, 0, res.getString(R.string.log_stars_35) + " (" + res.getString(R.string.log_stars_35_description) + ")");
        menuStars.add(0, 15, 0, res.getString(R.string.log_stars_3) + " (" + res.getString(R.string.log_stars_3_description) + ")");
        menuStars.add(0, 14, 0, res.getString(R.string.log_stars_25) + " (" + res.getString(R.string.log_stars_25_description) + ")");
        menuStars.add(0, 13, 0, res.getString(R.string.log_stars_2) + " (" + res.getString(R.string.log_stars_2_description) + ")");
        menuStars.add(0, 12, 0, res.getString(R.string.log_stars_15) + " (" + res.getString(R.string.log_stars_15_description) + ")");
        menuStars.add(0, 11, 0, res.getString(R.string.log_stars_1) + " (" + res.getString(R.string.log_stars_1_description) + ")");

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean signatureAvailable = Settings.getSignature() != null;
        menu.findItem(MENU_SIGNATURE).setVisible(signatureAvailable);

        boolean voteAvailable = Settings.isGCvoteLogin() && typeSelected == cgBase.LOG_FOUND_IT && StringUtils.isNotBlank(cache.guid);
        menu.findItem(SUBMENU_VOTE).setVisible(voteAvailable);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == MENU_SIGNATURE) {
            insertIntoLog(LogTemplateProvider.applyTemplates(Settings.getSignature(), base, false), true);
            return true;
        } else if (id >= 10 && id <= 19) {
            rating = (id - 9) / 2.0;

            if (post == null) {
                post = (Button) findViewById(R.id.post);
            }
            if (rating == 0.0) {
                post.setText(res.getString(R.string.log_post_no_rate));
            } else {
                post.setText(res.getString(R.string.log_post_rate) + " " + ratingTextValue(rating) + "*");
            }
            return true;
        }
        LogTemplate template = LogTemplateProvider.getTemplate(id);
        if (template != null) {
            String newText = template.getValue(base, false);
            insertIntoLog(newText, true);
            return true;
        }
        return false;
    }

    private void insertIntoLog(String newText, final boolean moveCursor) {
        EditText log = (EditText) findViewById(R.id.log);
        cgBase.insertAtPosition(log, newText, moveCursor);
    }

    private static String ratingTextValue(final double rating) {
        return String.format(Locale.getDefault(), "%.1f", rating);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info) {
        super.onCreateContextMenu(menu, view, info);
        final int viewId = view.getId();

        if (viewId == R.id.type) {
            for (final int typeOne : types) {
                menu.add(viewId, typeOne, 0, cgBase.logTypes2.get(typeOne));
                Log.w(Settings.tag, "Adding " + typeOne + " " + cgBase.logTypes2.get(typeOne));
            }
        } else if (viewId == R.id.changebutton) {
            final int textId = ((TextView) findViewById(viewId)).getId();

            menu.setHeaderTitle(res.getString(R.string.log_tb_changeall));
            for (LogTypeTrackable logType : LogTypeTrackable.values()) {
                menu.add(textId, logType.id, 0, res.getString(logType.resourceId));
            }
        } else {
            final int realViewId = ((LinearLayout) findViewById(viewId)).getId();

            for (final cgTrackableLog tb : trackables) {
                if (tb.id == realViewId) {
                    menu.setHeaderTitle(tb.name);
                }
            }
            for (LogTypeTrackable logType : LogTypeTrackable.values()) {
                menu.add(realViewId, logType.id, 0, res.getString(logType.resourceId));
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final int group = item.getGroupId();
        final int id = item.getItemId();

        if (group == R.id.type) {
            setType(id);

            return true;
        } else if (group == R.id.changebutton) {
            try {
                final LogTypeTrackable logType = LogTypeTrackable.findById(id);
                if (logType != null) {
                    final LinearLayout inventView = (LinearLayout) findViewById(R.id.inventory);
                    for (int count = 0; count < inventView.getChildCount(); count++) {
                        final LinearLayout tbView = (LinearLayout) inventView.getChildAt(count);
                        if (tbView == null) {
                            return false;
                        }

                        final TextView tbText = (TextView) tbView.findViewById(R.id.action);
                        if (tbText == null) {
                            return false;
                        }
                        tbText.setText(res.getString(logType.resourceId));
                    }
                    for (cgTrackableLog tb : trackables) {
                        tb.action = logType;
                    }
                    tbChanged = true;
                    return true;
                }
            } catch (Exception e) {
                Log.e(Settings.tag, "cgeovisit.onContextItemSelected: " + e.toString());
            }
        } else {
            try {
                final LogTypeTrackable logType = LogTypeTrackable.findById(id);
                if (logType != null) {
                    final LinearLayout tbView = (LinearLayout) findViewById(group);
                    if (tbView == null) {
                        return false;
                    }

                    final TextView tbText = (TextView) tbView.findViewById(R.id.action);
                    if (tbText == null) {
                        return false;
                    }

                    for (cgTrackableLog tb : trackables) {
                        if (tb.id == group) {
                            tbChanged = true;

                            tb.action = logType;
                            tbText.setText(res.getString(logType.resourceId));

                            Log.i(Settings.tag, "Trackable " + tb.trackCode + " (" + tb.name + ") has new action: #" + id);
                        }
                    }

                    return true;
                }
            } catch (Exception e) {
                Log.e(Settings.tag, "cgeovisit.onContextItemSelected: " + e.toString());
            }
        }

        return false;
    }

    public void init() {
        if (geocode != null) {
            app.setAction(geocode);
        }

        types = cache.getPossibleLogTypes();

        final cgLog log = app.loadLogOffline(geocode);
        if (log != null) {
            typeSelected = log.type;
            date.setTime(new Date(log.date));
            text = log.log;
            if (typeSelected == cgBase.LOG_FOUND_IT && Settings.isGCvoteLogin()) {
                if (post == null) {
                    post = (Button) findViewById(R.id.post);
                }
                post.setText(res.getString(R.string.log_post_no_rate));
            }
        } else if (StringUtils.isNotBlank(Settings.getSignature())
                && Settings.isAutoInsertSignature()
                && StringUtils.isBlank(((EditText) findViewById(R.id.log)).getText())) {
            insertIntoLog(LogTemplateProvider.applyTemplates(Settings.getSignature(), base, false), false);
        }

        if (!types.contains(typeSelected)) {
            if (alreadyFound) {
                typeSelected = cgBase.LOG_NOTE;
            } else {
                typeSelected = types.get(0);
            }
            setType(typeSelected);
        }

        Button typeButton = (Button) findViewById(R.id.type);
        registerForContextMenu(typeButton);
        typeButton.setText(cgBase.logTypes2.get(typeSelected));
        typeButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                openContextMenu(view);
            }
        });

        Button dateButton = (Button) findViewById(R.id.date);
        dateButton.setText(base.formatShortDate(date.getTime().getTime()));
        dateButton.setOnClickListener(new cgeovisitDateListener());

        EditText logView = (EditText) findViewById(R.id.log);
        if (StringUtils.isBlank(logView.getText()) && StringUtils.isNotBlank(text)) {
            logView.setText(text);
        }

        if (tweetBox == null) {
            tweetBox = (LinearLayout) findViewById(R.id.tweet_box);
        }
        if (tweetCheck == null) {
            tweetCheck = (CheckBox) findViewById(R.id.tweet);
        }
        tweetCheck.setChecked(true);

        if (post == null) {
            post = (Button) findViewById(R.id.post);
        }
        if (cgBase.isEmpty(viewstates)) {
            post.setEnabled(false);
            post.setOnTouchListener(null);
            post.setOnClickListener(null);

            loadData thread;
            thread = new loadData();
            thread.start();
        } else {
            post.setEnabled(true);
            post.setOnClickListener(new postListener());
        }

        if (save == null) {
            save = (Button) findViewById(R.id.save);
        }
        save.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                saveLog(true);
            }
        });

        if (clear == null) {
            clear = (Button) findViewById(R.id.clear);
        }
        clear.setOnClickListener(new clearListener());
    }

    public void setDate(Calendar dateIn) {
        date = dateIn;

        final Button dateButton = (Button) findViewById(R.id.date);
        dateButton.setText(base.formatShortDate(date.getTime().getTime()));
    }

    public void setType(int type) {
        final Button typeButton = (Button) findViewById(R.id.type);

        if (cgBase.logTypes2.get(type) != null) {
            typeSelected = type;
        }
        if (cgBase.logTypes2.get(typeSelected) == null) {
            typeSelected = 1;
        }
        typeButton.setText(cgBase.logTypes2.get(typeSelected));

        if (tweetBox == null) {
            tweetBox = (LinearLayout) findViewById(R.id.tweet_box);
        }

        if (type == 2 && !tbChanged) {
            // TODO: change action
        } else if (type != 2 && !tbChanged) {
            // TODO: change action
        }

        if (type == cgBase.LOG_FOUND_IT && Settings.isUseTwitter()) {
            tweetBox.setVisibility(View.VISIBLE);
        } else {
            tweetBox.setVisibility(View.GONE);
        }

        if (post == null) {
            post = (Button) findViewById(R.id.post);
        }

        if (type == cgBase.LOG_FOUND_IT && Settings.isGCvoteLogin()) {
            if (rating == 0) {
                post.setText(res.getString(R.string.log_post_no_rate));
            } else {
                post.setText(res.getString(R.string.log_post_rate) + " " + ratingTextValue(rating) + "*");
            }
        } else {
            post.setText(res.getString(R.string.log_post));
        }
    }

    private class cgeovisitDateListener implements View.OnClickListener {

        public void onClick(View arg0) {
            Dialog dateDialog = new cgeodate(cgeovisit.this, cgeovisit.this, date);
            dateDialog.setCancelable(true);
            dateDialog.show();
        }
    }

    private class postListener implements View.OnClickListener {

        public void onClick(View arg0) {
            if (!gettingViewstate) {
                waitDialog = ProgressDialog.show(cgeovisit.this, null, res.getString(R.string.log_saving), true);
                waitDialog.setCancelable(true);

                String log = ((EditText) findViewById(R.id.log)).getText().toString();
                Thread thread = new postLog(postLogHandler, log);
                thread.start();
            } else {
                showToast(res.getString(R.string.err_log_load_data_still));
            }
        }
    }

    private class clearListener implements View.OnClickListener {

        public void onClick(View arg0) {
            app.clearLogOffline(geocode);

            if (alreadyFound) {
                typeSelected = cgBase.LOG_NOTE;
            } else {
                typeSelected = types.get(0);
            }
            date.setTime(new Date());
            text = null;

            setType(typeSelected);

            Button dateButton = (Button) findViewById(R.id.date);
            dateButton.setText(base.formatShortDate(date.getTime().getTime()));
            dateButton.setOnClickListener(new cgeovisitDateListener());

            EditText logView = (EditText) findViewById(R.id.log);
            logView.setText("");

            if (clear == null) {
                clear = (Button) findViewById(R.id.clear);
            }
            clear.setOnClickListener(new clearListener());

            showToast(res.getString(R.string.info_log_cleared));
        }
    }

    private class loadData extends Thread {

        public loadData() {
            if (cacheid == null) {
                showToast(res.getString(R.string.err_detail_cache_forgot_visit));

                finish();
                return;
            }
        }

        @Override
        public void run() {
            final Parameters params = new Parameters();

            showProgressHandler.sendEmptyMessage(0);
            gettingViewstate = true;
            attempts++;

            try {
                if (StringUtils.isNotBlank(cacheid)) {
                    params.put("ID", cacheid);
                } else {
                    loadDataHandler.sendEmptyMessage(0);
                    return;
                }

                final String page = cgBase.getResponseData(cgBase.request("http://www.geocaching.com/seek/log.aspx", params, false, false, false));

                viewstates = cgBase.getViewstates(page);
                trackables = cgBase.parseTrackableLog(page);

                final List<Integer> typesPre = cgBase.parseTypes(page);
                if (CollectionUtils.isNotEmpty(typesPre)) {
                    types.clear();
                    types.addAll(typesPre);
                    types.remove(Integer.valueOf(cgBase.LOG_UPDATE_COORDINATES));
                }
                typesPre.clear();
            } catch (Exception e) {
                Log.e(Settings.tag, "cgeovisit.loadData.run: " + e.toString());
            }

            loadDataHandler.sendEmptyMessage(0);
        }
    }

    private class postLog extends Thread {

        Handler handler = null;
        String log = null;

        public postLog(Handler handlerIn, String logIn) {
            handler = handlerIn;
            log = logIn;
        }

        @Override
        public void run() {
            final StatusCode status = postLogFn(log);
            handler.sendMessage(handler.obtainMessage(0, status));
        }
    }

    public StatusCode postLogFn(String log) {
        try {
            if (tweetBox == null) {
                tweetBox = (LinearLayout) findViewById(R.id.tweet_box);
            }
            if (tweetCheck == null) {
                tweetCheck = (CheckBox) findViewById(R.id.tweet);
            }

            final StatusCode status = cgBase.postLog(app, geocode, cacheid, viewstates, typeSelected,
                    date.get(Calendar.YEAR), (date.get(Calendar.MONTH) + 1), date.get(Calendar.DATE),
                    log, trackables);

            if (status == StatusCode.NO_ERROR) {
                cgLog logNow = new cgLog();
                logNow.author = Settings.getUsername();
                logNow.date = date.getTimeInMillis();
                logNow.type = typeSelected;
                logNow.log = log;

                if (cache != null && null != cache.logs) {
                    cache.logs.add(0, logNow);
                }
                app.addLog(geocode, logNow);

                if (typeSelected == cgBase.LOG_FOUND_IT) {
                    app.markFound(geocode);
                    if (cache != null) {
                        cache.found = true;
                    }
                }

                if (cache != null) {
                    app.putCacheInCache(cache);
                } else {
                    app.removeCacheFromCache(geocode);
                }
            }

            if (status == StatusCode.NO_ERROR) {
                app.clearLogOffline(geocode);
            }

            if (status == StatusCode.NO_ERROR && typeSelected == cgBase.LOG_FOUND_IT && Settings.isUseTwitter()
                    && Settings.isTwitterLoginValid()
                    && tweetCheck.isChecked() && tweetBox.getVisibility() == View.VISIBLE) {
                cgBase.postTweetCache(app, geocode);
            }

            if (status == StatusCode.NO_ERROR && typeSelected == cgBase.LOG_FOUND_IT && Settings.isGCvoteLogin()) {
                GCVote.setRating(cache, rating);
            }

            return status;
        } catch (Exception e) {
            Log.e(Settings.tag, "cgeovisit.postLogFn: " + e.toString());
        }

        return StatusCode.LOG_POST_ERROR;
    }

    private void saveLog(final boolean force) {
        final String log = currentLogText();

        // Do not erase the saved log if the user has removed all the characters
        // without using "Clear". This may be a manipulation mistake, and erasing
        // again will be easy using "Clear" while retyping the text may not be.
        if (force || (log.length() > 0 && !StringUtils.equals(log, text))) {
            cache.logOffline(this, log, date, typeSelected);
        }
        text = log;
    }

    private String currentLogText() {
        return ((EditText) findViewById(R.id.log)).getText().toString();
    }

}
