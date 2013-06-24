package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory.NavigationAppsEnum;
import cgeo.geocaching.compatibility.Compatibility;
import cgeo.geocaching.connector.gc.Login;
import cgeo.geocaching.connector.oc.OCAuthorizationActivity;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.files.SimpleDirChooser;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.network.Cookies;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.twitter.TwitterAuthorizationActivity;
import cgeo.geocaching.ui.Formatter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.LogTemplateProvider;
import cgeo.geocaching.utils.LogTemplateProvider.LogTemplate;
import cgeo.geocaching.utils.RunnableWithArgument;

import ch.boye.httpclientandroidlib.HttpResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.openintents.intents.FileManagerIntents;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class OldSettingsActivity extends AbstractActivity {

    private final static int SELECT_MAPFILE_REQUEST = 1;
    private final static int SELECT_GPX_EXPORT_REQUEST = 2;
    private final static int SELECT_GPX_IMPORT_REQUEST = 3;
    private final static int SELECT_THEMEFOLDER_REQUEST = 4;

    private ProgressDialog loginDialog = null;
    private ProgressDialog webDialog = null;
    private boolean enableTemplatesMenu = false;
    private Handler logInHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            try {
                if (loginDialog != null && loginDialog.isShowing()) {
                    loginDialog.dismiss();
                }

                if (msg.obj == null || (msg.obj instanceof Drawable)) {
                    helpDialog(res.getString(R.string.oldinit_login_popup), res.getString(R.string.oldinit_login_popup_ok),
                            (Drawable) msg.obj);
                } else {
                    helpDialog(res.getString(R.string.oldinit_login_popup),
                            res.getString(R.string.oldinit_login_popup_failed_reason) + " " +
                                    ((StatusCode) msg.obj).getErrorString(res) + ".");
                }
            } catch (Exception e) {
                showToast(res.getString(R.string.err_login_failed));

                Log.e("SettingsActivity.logInHandler", e);
            }

            if (loginDialog != null && loginDialog.isShowing()) {
                loginDialog.dismiss();
            }

            init();
        }
    };

    private Handler webAuthHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            try {
                if (webDialog != null && webDialog.isShowing()) {
                    webDialog.dismiss();
                }

                if (msg.what > 0) {
                    helpDialog(res.getString(R.string.oldinit_sendToCgeo), res.getString(R.string.oldinit_sendToCgeo_register_ok).replace("####", String.valueOf(msg.what)));
                } else {
                    helpDialog(res.getString(R.string.oldinit_sendToCgeo), res.getString(R.string.oldinit_sendToCgeo_register_fail));
                }
            } catch (Exception e) {
                showToast(res.getString(R.string.oldinit_sendToCgeo_register_fail));

                Log.e("SettingsActivity.webHandler", e);
            }

            if (webDialog != null && webDialog.isShowing()) {
                webDialog.dismiss();
            }

            init();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.oldsettings_activity);

        init();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        init();
    }

    @Override
    public void onPause() {
        saveValues();
        super.onPause();
    }

    @Override
    public void onStop() {
        saveValues();
        Compatibility.dataChanged(getPackageName());
        super.onStop();
    }

    @Override
    public void onDestroy() {
        saveValues();

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.oldsettings_activity_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_clear) {
            ((EditText) findViewById(R.id.username)).setText("");
            ((EditText) findViewById(R.id.password)).setText("");
            ((EditText) findViewById(R.id.passvote)).setText("");

            if (saveValues()) {
                showToast(res.getString(R.string.oldinit_cleared));
            } else {
                showToast(res.getString(R.string.err_init_cleared));
            }

            finish();
        }

        return false;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        if (enableTemplatesMenu) {
            menu.setHeaderTitle(R.string.oldinit_signature_template_button);
            for (LogTemplate template : LogTemplateProvider.getTemplates()) {
                menu.add(0, template.getItemId(), 0, template.getResourceId());
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        LogTemplate template = LogTemplateProvider.getTemplate(item.getItemId());
        if (template != null) {
            return insertSignatureTemplate(template);
        }
        return super.onContextItemSelected(item);
    }

    private boolean insertSignatureTemplate(final LogTemplate template) {
        EditText sig = (EditText) findViewById(R.id.signature);
        String insertText = "[" + template.getTemplateString() + "]";
        insertAtPosition(sig, insertText, true);
        return true;
    }

    public void init() {

        // geocaching.com settings
        final CheckBox gcCheck = (CheckBox) findViewById(R.id.gc_option);
        gcCheck.setChecked(OldSettings.isGCConnectorActive());
        gcCheck.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setGCConnectorActive(gcCheck.isChecked());
            }
        });
        final ImmutablePair<String, String> login = OldSettings.getGcLogin();
        if (login != null) {
            ((EditText) findViewById(R.id.username)).setText(login.left);
            ((EditText) findViewById(R.id.password)).setText(login.right);
        }

        Button logMeIn = (Button) findViewById(R.id.log_me_in);
        logMeIn.setOnClickListener(new LoginListener());

        TextView legalNote = (TextView) findViewById(R.id.legal_note);
        legalNote.setClickable(true);
        legalNote.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.geocaching.com/about/termsofuse.aspx")));
            }
        });

        // opencaching.de settings
        final CheckBox ocCheck = (CheckBox) findViewById(R.id.oc_option);
        ocCheck.setChecked(OldSettings.isOCConnectorActive());
        ocCheck.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setOCConnectorActive(ocCheck.isChecked());
            }
        });

        Button checkOCUser = (Button) findViewById(R.id.register_oc_de);
        checkOCUser.setOnClickListener(new OCDEAuthorizeCgeoListener());

        // gcvote settings
        final ImmutablePair<String, String> gcvoteLogin = OldSettings.getGCvoteLogin();
        if (null != gcvoteLogin && null != gcvoteLogin.right) {
            ((EditText) findViewById(R.id.passvote)).setText(gcvoteLogin.right);
        }

        // Twitter settings
        Button authorizeTwitter = (Button) findViewById(R.id.authorize_twitter);
        authorizeTwitter.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Intent authIntent = new Intent(OldSettingsActivity.this, TwitterAuthorizationActivity.class);
                startActivity(authIntent);
            }
        });

        final CheckBox twitterButton = (CheckBox) findViewById(R.id.twitter_option);
        twitterButton.setChecked(OldSettings.isUseTwitter() && OldSettings.isTwitterLoginValid());
        twitterButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setUseTwitter(twitterButton.isChecked());
                if (OldSettings.isUseTwitter() && !OldSettings.isTwitterLoginValid()) {
                    Intent authIntent = new Intent(OldSettingsActivity.this, TwitterAuthorizationActivity.class);
                    startActivity(authIntent);
                }

                twitterButton.setChecked(OldSettings.isUseTwitter());
            }
        });

        // Signature settings
        EditText sigEdit = (EditText) findViewById(R.id.signature);
        if (sigEdit.getText().length() == 0) {
            sigEdit.setText(OldSettings.getSignature());
        }
        Button sigBtn = (Button) findViewById(R.id.signature_help);
        sigBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                helpDialog(res.getString(R.string.oldinit_signature_help_title), res.getString(R.string.oldinit_signature_help_text));
            }
        });
        Button templates = (Button) findViewById(R.id.signature_template);
        registerForContextMenu(templates);
        templates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableTemplatesMenu = true;
                openContextMenu(v);
                enableTemplatesMenu = false;
            }
        });
        final CheckBox autoinsertButton = (CheckBox) findViewById(R.id.sigautoinsert);
        autoinsertButton.setChecked(OldSettings.isAutoInsertSignature());
        autoinsertButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setAutoInsertSignature(autoinsertButton.isChecked());
            }
        });

        // Cache details
        final CheckBox autoloadButton = (CheckBox) findViewById(R.id.autoload);
        autoloadButton.setChecked(OldSettings.isAutoLoadDescription());
        autoloadButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setAutoLoadDesc(autoloadButton.isChecked());
            }
        });

        final CheckBox ratingWantedButton = (CheckBox) findViewById(R.id.ratingwanted);
        ratingWantedButton.setChecked(OldSettings.isRatingWanted());
        ratingWantedButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setRatingWanted(ratingWantedButton.isChecked());
            }
        });

        final CheckBox elevationWantedButton = (CheckBox) findViewById(R.id.elevationwanted);
        elevationWantedButton.setChecked(OldSettings.isElevationWanted());
        elevationWantedButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setElevationWanted(elevationWantedButton.isChecked());
            }
        });

        final CheckBox friendLogsWantedButton = (CheckBox) findViewById(R.id.friendlogswanted);
        friendLogsWantedButton.setChecked(OldSettings.isFriendLogsWanted());
        friendLogsWantedButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setFriendLogsWanted(friendLogsWantedButton.isChecked());
            }
        });

        final CheckBox openLastDetailsPageButton = (CheckBox) findViewById(R.id.openlastdetailspage);
        openLastDetailsPageButton.setChecked(OldSettings.isOpenLastDetailsPage());
        openLastDetailsPageButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setOpenLastDetailsPage(openLastDetailsPageButton.isChecked());
            }
        });

        // Other settings
        final CheckBox skinButton = (CheckBox) findViewById(R.id.skin);
        skinButton.setChecked(OldSettings.isLightSkin());
        skinButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setLightSkin(skinButton.isChecked());
            }
        });

        final CheckBox addressButton = (CheckBox) findViewById(R.id.address);
        addressButton.setChecked(OldSettings.isShowAddress());
        addressButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setShowAddress(addressButton.isChecked());
            }
        });

        final CheckBox captchaButton = (CheckBox) findViewById(R.id.captcha);
        captchaButton.setEnabled(!OldSettings.isPremiumMember());
        captchaButton.setChecked(OldSettings.isShowCaptcha());
        captchaButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setShowCaptcha(captchaButton.isChecked());
            }
        });

        final CheckBox dirImgButton = (CheckBox) findViewById(R.id.loaddirectionimg);
        dirImgButton.setEnabled(!OldSettings.isPremiumMember());
        dirImgButton.setChecked(OldSettings.getLoadDirImg());
        dirImgButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setLoadDirImg(!OldSettings.getLoadDirImg());
                dirImgButton.setChecked(OldSettings.getLoadDirImg());
            }
        });

        final CheckBox useEnglishButton = (CheckBox) findViewById(R.id.useenglish);
        useEnglishButton.setChecked(OldSettings.isUseEnglish());
        useEnglishButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setUseEnglish(useEnglishButton.isChecked());
            }
        });

        final CheckBox excludeButton = (CheckBox) findViewById(R.id.exclude);
        excludeButton.setChecked(OldSettings.isExcludeMyCaches());
        excludeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setExcludeMine(excludeButton.isChecked());
            }
        });

        final CheckBox disabledButton = (CheckBox) findViewById(R.id.disabled);
        disabledButton.setChecked(OldSettings.isExcludeDisabledCaches());
        disabledButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setExcludeDisabledCaches(disabledButton.isChecked());
            }
        });

        TextView showWaypointsThreshold = (TextView) findViewById(R.id.showwaypointsthreshold);
        showWaypointsThreshold.setText(String.valueOf(OldSettings.getWayPointsThreshold()));

        final CheckBox autovisitButton = (CheckBox) findViewById(R.id.trackautovisit);
        autovisitButton.setChecked(OldSettings.isTrackableAutoVisit());
        autovisitButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setTrackableAutoVisit(autovisitButton.isChecked());
            }
        });

        final CheckBox offlineButton = (CheckBox) findViewById(R.id.offline);
        offlineButton.setChecked(OldSettings.isStoreOfflineMaps());
        offlineButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setStoreOfflineMaps(offlineButton.isChecked());
            }
        });

        final CheckBox offlineWpButton = (CheckBox) findViewById(R.id.offline_wp);
        offlineWpButton.setChecked(OldSettings.isStoreOfflineWpMaps());
        offlineWpButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setStoreOfflineWpMaps(offlineWpButton.isChecked());
            }
        });

        final CheckBox saveLogImgButton = (CheckBox) findViewById(R.id.save_log_img);
        saveLogImgButton.setChecked(OldSettings.isStoreLogImages());
        saveLogImgButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setStoreLogImages(saveLogImgButton.isChecked());
            }
        });

        final CheckBox livelistButton = (CheckBox) findViewById(R.id.livelist);
        livelistButton.setChecked(OldSettings.isLiveList());
        livelistButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setLiveList(livelistButton.isChecked());
            }
        });

        final CheckBox unitsButton = (CheckBox) findViewById(R.id.units);
        unitsButton.setChecked(!OldSettings.isUseMetricUnits());
        unitsButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setUseMetricUnits(!unitsButton.isChecked());
            }
        });

        final CheckBox logOffline = (CheckBox) findViewById(R.id.log_offline);
        logOffline.setChecked(OldSettings.getLogOffline());
        logOffline.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setLogOffline(!OldSettings.getLogOffline());
                logOffline.setChecked(OldSettings.getLogOffline());
            }
        });

        final CheckBox chooseList = (CheckBox) findViewById(R.id.choose_list);
        chooseList.setChecked(OldSettings.getChooseList());
        chooseList.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setChooseList(!OldSettings.getChooseList());
                chooseList.setChecked(OldSettings.getChooseList());
            }
        });

        final CheckBox plainLogs = (CheckBox) findViewById(R.id.plain_logs);
        plainLogs.setChecked(OldSettings.getPlainLogs());
        plainLogs.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setPlainLogs(!OldSettings.getPlainLogs());
                plainLogs.setChecked(OldSettings.getPlainLogs());
            }
        });

        // Workaround for cspire customers on mobile connections #1843
        final CheckBox useNativeUserAgent = (CheckBox) findViewById(R.id.use_native_ua);
        useNativeUserAgent.setChecked(OldSettings.getUseNativeUa());
        useNativeUserAgent.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setUseNativeUa(!OldSettings.getUseNativeUa());
                useNativeUserAgent.setChecked(OldSettings.getUseNativeUa());
            }
        });

        // Altitude settings
        EditText altitudeEdit = (EditText) findViewById(R.id.altitude);
        altitudeEdit.setText(String.valueOf(OldSettings.getAltCorrection()));

        //Send2cgeo settings
        String webDeviceName = OldSettings.getWebDeviceName();

        if (StringUtils.isNotBlank(webDeviceName)) {
            ((EditText) findViewById(R.id.webDeviceName)).setText(webDeviceName);
        } else {
            String s = android.os.Build.MODEL;
            ((EditText) findViewById(R.id.webDeviceName)).setText(s);
        }

        Button webAuth = (Button) findViewById(R.id.sendToCgeo_register);
        webAuth.setOnClickListener(new WebAuthListener());

        // Map source settings
        updateMapSourceMenu();

        Button selectMapDirectory = (Button) findViewById(R.id.select_map_directory);
        selectMapDirectory.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent selectIntent = new Intent(OldSettingsActivity.this, SelectMapfileActivity.class);
                startActivityForResult(selectIntent, SELECT_MAPFILE_REQUEST);
            }
        });

        // Theme folder settings
        initThemefolderEdittext(false);

        Button selectThemefolder = (Button) findViewById(R.id.select_themefolder);
        selectThemefolder.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                selectDirectory(OldSettings.getCustomRenderThemeBaseFolder(), SELECT_THEMEFOLDER_REQUEST);
            }
        });

        // GPX Export directory
        final EditText gpxExportDir = (EditText) findViewById(R.id.gpx_exportdir);
        gpxExportDir.setText(OldSettings.getGpxExportDir());
        Button selectGpxExportDir = (Button) findViewById(R.id.select_gpx_exportdir);
        selectGpxExportDir.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                selectDirectory(OldSettings.getGpxExportDir(), SELECT_GPX_EXPORT_REQUEST);
            }
        });

        // GPX Import directory
        final EditText gpxImportDir = (EditText) findViewById(R.id.gpx_importdir);
        gpxImportDir.setText(OldSettings.getGpxImportDir());
        Button selectGpxImportDir = (Button) findViewById(R.id.select_gpx_importdir);
        selectGpxImportDir.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                selectDirectory(OldSettings.getGpxImportDir(), SELECT_GPX_IMPORT_REQUEST);
            }
        });

        // Display trail on map
        final CheckBox trailButton = (CheckBox) findViewById(R.id.trail);
        trailButton.setChecked(OldSettings.isMapTrail());
        trailButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setMapTrail(trailButton.isChecked());
            }
        });

        // Default navigation tool settings
        Spinner defaultNavigationToolSelector = (Spinner) findViewById(R.id.default_navigation_tool);
        final List<NavigationAppsEnum> apps = NavigationAppFactory.getInstalledDefaultNavigationApps();
        ArrayAdapter<NavigationAppsEnum> naviAdapter = new ArrayAdapter<NavigationAppsEnum>(this, android.R.layout.simple_spinner_item, apps) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setText(getItem(position).app.getName());
                return textView;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getDropDownView(position, convertView, parent);
                textView.setText(getItem(position).app.getName());
                return textView;
            }
        };
        naviAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        defaultNavigationToolSelector.setAdapter(naviAdapter);
        int defaultNavigationTool = OldSettings.getDefaultNavigationTool();
        int ordinal = 0;
        for (int i = 0; i < apps.size(); i++) {
            if (apps.get(i).id == defaultNavigationTool) {
                ordinal = i;
                break;
            }
        }
        defaultNavigationToolSelector.setSelection(ordinal);
        defaultNavigationToolSelector.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                NavigationAppsEnum item = (NavigationAppsEnum) parent.getItemAtPosition(position);
                if (item != null) {
                    OldSettings.setDefaultNavigationTool(item.id);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // noop
            }
        });

        // 2nd Default navigation tool settings
        Spinner defaultNavigationTool2Selector = (Spinner) findViewById(R.id.default_navigation_tool_2);
        //        final List<NavigationAppsEnum> apps = NavigationAppFactory.getInstalledNavigationApps(this);
        ArrayAdapter<NavigationAppsEnum> navi2Adapter = new ArrayAdapter<NavigationAppsEnum>(this, android.R.layout.simple_spinner_item, apps) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setText(getItem(position).app.getName());
                return textView;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getDropDownView(position, convertView, parent);
                textView.setText(getItem(position).app.getName());
                return textView;
            }
        };
        navi2Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        defaultNavigationTool2Selector.setAdapter(navi2Adapter);
        int defaultNavigationTool2 = OldSettings.getDefaultNavigationTool2();
        int ordinal2 = 0;
        for (int i = 0; i < apps.size(); i++) {
            if (apps.get(i).id == defaultNavigationTool2) {
                ordinal2 = i;
                break;
            }
        }
        defaultNavigationTool2Selector.setSelection(ordinal2);
        defaultNavigationTool2Selector.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                NavigationAppsEnum item = (NavigationAppsEnum) parent.getItemAtPosition(position);
                if (item != null) {
                    OldSettings.setDefaultNavigationTool2(item.id);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // noop
            }
        });

        refreshBackupLabel();

        // Database location
        refreshDbOnSDCardSetting();

        final CheckBox dbOnSDCardButton = (CheckBox) findViewById(R.id.dbonsdcard);
        dbOnSDCardButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                app.moveDatabase(OldSettingsActivity.this);
            }
        });

        // Debug settings
        final CheckBox debugButton = (CheckBox) findViewById(R.id.debug);
        debugButton.setChecked(OldSettings.isDebug());
        debugButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OldSettings.setDebug(!OldSettings.isDebug());
                debugButton.setChecked(OldSettings.isDebug());
            }
        });
    }

    private void updateMapSourceMenu() {
        Collection<String> mapSourceNames = new ArrayList<String>();
        for (MapSource mapSource : MapProviderFactory.getMapSources()) {
            mapSourceNames.add(mapSource.getName());
        }
        Spinner mapSourceSelector = (Spinner) findViewById(R.id.mapsource);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mapSourceNames.toArray(new String[mapSourceNames.size()]));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mapSourceSelector.setAdapter(adapter);
        final int index = MapProviderFactory.getMapSources().indexOf(OldSettings.getMapSource());
        mapSourceSelector.setSelection(index);
        mapSourceSelector.setOnItemSelectedListener(new ChangeMapSourceListener());

        initMapDirectoryEdittext(false);
    }

    private void initMapDirectoryEdittext(boolean setFocus) {
        final EditText mapDirectoryEdit = (EditText) findViewById(R.id.map_directory);
        mapDirectoryEdit.setText(OldSettings.getMapFileDirectory());
        if (setFocus) {
            mapDirectoryEdit.requestFocus();
        }
    }

    private void initThemefolderEdittext(boolean setFocus) {
        EditText themeFileEdit = (EditText) findViewById(R.id.themefolder);
        themeFileEdit.setText(OldSettings.getCustomRenderThemeBaseFolder());
        if (setFocus) {
            themeFileEdit.requestFocus();
        }
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void backup(View view) {
        // avoid overwriting an existing backup with an empty database (can happen directly after reinstalling the app)
        if (cgData.getAllCachesCount() == 0) {
            helpDialog(res.getString(R.string.oldinit_backup), res.getString(R.string.oldinit_backup_unnecessary));
            return;
        }

        final ProgressDialog dialog = ProgressDialog.show(this, res.getString(R.string.oldinit_backup), res.getString(R.string.oldinit_backup_running), true, false);
        new Thread() {
            @Override
            public void run() {
                final String backupFileName = cgData.backupDatabase();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        helpDialog(res.getString(R.string.oldinit_backup_backup),
                                backupFileName != null ? res.getString(R.string.oldinit_backup_success) + "\n" + backupFileName : res.getString(R.string.oldinit_backup_failed));
                        refreshBackupLabel();
                    }
                });
            }
        }.start();
    }

    private void refreshBackupLabel() {
        TextView lastBackup = (TextView) findViewById(R.id.backup_last);
        File lastBackupFile = cgData.getRestoreFile();
        if (lastBackupFile != null) {
            lastBackup.setText(res.getString(R.string.oldinit_backup_last) + " " + Formatter.formatTime(lastBackupFile.lastModified()) + ", " + Formatter.formatDate(lastBackupFile.lastModified()));
        } else {
            lastBackup.setText(res.getString(R.string.oldinit_backup_last_no));
        }
    }

    private void refreshDbOnSDCardSetting() {
        final CheckBox dbOnSDCardButton = (CheckBox) findViewById(R.id.dbonsdcard);
        dbOnSDCardButton.setChecked(OldSettings.isDbOnSDCard());
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void restore(View view) {
        app.restoreDatabase(this);
    }

    public boolean saveValues() {
        String usernameNew = StringUtils.trimToEmpty(((EditText) findViewById(R.id.username)).getText().toString());
        String passwordNew = StringUtils.trimToEmpty(((EditText) findViewById(R.id.password)).getText().toString());
        String passvoteNew = StringUtils.trimToEmpty(((EditText) findViewById(R.id.passvote)).getText().toString());
        // don't trim signature, user may want to have whitespace at the beginning
        String signatureNew = ((EditText) findViewById(R.id.signature)).getText().toString();
        String mapDirectoryNew = StringUtils.trimToEmpty(((EditText) findViewById(R.id.map_directory)).getText().toString());
        String themesDirectoryNew = StringUtils.trimToEmpty(((EditText) findViewById(R.id.themefolder)).getText().toString());

        String altitudeNew = StringUtils.trimToNull(((EditText) findViewById(R.id.altitude)).getText().toString());
        int altitudeNewInt = parseNumber(altitudeNew, 0);

        TextView field = (TextView) findViewById(R.id.showwaypointsthreshold);
        final int waypointThreshold = parseNumber(field.getText().toString(), 5);

        final boolean status1 = OldSettings.setLogin(usernameNew, passwordNew);
        final boolean status2 = OldSettings.setGCvoteLogin(passvoteNew);
        final boolean status3 = OldSettings.setSignature(signatureNew);
        final boolean status4 = OldSettings.setAltCorrection(altitudeNewInt);
        final boolean status5 = OldSettings.setMapFileDirectory(mapDirectoryNew);
        final boolean status6 = OldSettings.setCustomRenderThemeBaseFolder(themesDirectoryNew);
        OldSettings.setShowWaypointsThreshold(waypointThreshold);

        String importNew = StringUtils.trimToEmpty(((EditText) findViewById(R.id.gpx_importdir)).getText().toString());
        String exportNew = StringUtils.trimToEmpty(((EditText) findViewById(R.id.gpx_exportdir)).getText().toString());
        OldSettings.setGpxImportDir(importNew);
        OldSettings.setGpxExportDir(exportNew);

        return status1 && status2 && status3 && status4 && status5 && status6;
    }

    /**
     * Returns the integer value from the string
     *
     * @param field
     *            the field to retrieve the integer value from
     * @param defaultValue
     *            the default value
     * @return either the field content or the default value
     */

    static private int parseNumber(final String number, int defaultValue) {
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static class ChangeMapSourceListener implements OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> arg0, View arg1, int position,
                long arg3) {
            OldSettings.setMapSource(MapProviderFactory.getMapSources().get(position));
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            arg0.setSelection(MapProviderFactory.getMapSources().indexOf(OldSettings.getMapSource()));
        }
    }

    private class LoginListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            final String username = ((EditText) findViewById(R.id.username)).getText().toString();
            final String password = ((EditText) findViewById(R.id.password)).getText().toString();

            if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
                showToast(res.getString(R.string.err_missing_auth));
                return;
            }

            loginDialog = ProgressDialog.show(OldSettingsActivity.this,
                    res.getString(R.string.oldinit_login_popup),
                    res.getString(R.string.oldinit_login_popup_working), true);
            loginDialog.setCancelable(false);

            OldSettings.setLogin(username, password);
            Cookies.clearCookies();

            (new Thread() {

                @Override
                public void run() {
                    final StatusCode loginResult = Login.login();
                    Object payload = loginResult;
                    if (loginResult == StatusCode.NO_ERROR) {
                        Login.detectGcCustomDate();
                        payload = Login.downloadAvatarAndGetMemberStatus();
                    }
                    logInHandler.obtainMessage(0, payload).sendToTarget();
                }
            }).start();
        }
    }

    private class OCDEAuthorizeCgeoListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            Intent authIntent = new Intent(OldSettingsActivity.this, OCAuthorizationActivity.class);
            startActivity(authIntent);
        }
    }

    private class WebAuthListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            final String deviceName = ((EditText) findViewById(R.id.webDeviceName)).getText().toString();
            final String deviceCode = OldSettings.getWebDeviceCode();

            if (StringUtils.isBlank(deviceName)) {
                showToast(res.getString(R.string.err_missing_device_name));
                return;
            }

            webDialog = ProgressDialog.show(OldSettingsActivity.this, res.getString(R.string.oldinit_sendToCgeo), res.getString(R.string.oldinit_sendToCgeo_registering), true);
            webDialog.setCancelable(false);

            (new Thread() {

                @Override
                public void run() {
                    int pin = 0;

                    final String nam = StringUtils.defaultString(deviceName);
                    final String cod = StringUtils.defaultString(deviceCode);

                    final Parameters params = new Parameters("name", nam, "code", cod);
                    HttpResponse response = Network.getRequest("http://send2.cgeo.org/auth.html", params);

                    if (response != null && response.getStatusLine().getStatusCode() == 200) {
                        //response was OK
                        String[] strings = Network.getResponseData(response).split(",");
                        try {
                            pin = Integer.parseInt(strings[1].trim());
                        } catch (Exception e) {
                            Log.e("webDialog", e);
                        }
                        String code = strings[0];
                        OldSettings.setWebNameCode(nam, code);
                    }

                    webAuthHandler.sendEmptyMessage(pin);
                }
            }).start();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case SELECT_MAPFILE_REQUEST:
                if (data.hasExtra(Intents.EXTRA_MAP_FILE)) {
                    final String mapFile = data.getStringExtra(Intents.EXTRA_MAP_FILE);
                    OldSettings.setMapFile(mapFile);
                    if (!OldSettings.isValidMapFile(OldSettings.getMapFile())) {
                        showToast(res.getString(R.string.warn_invalid_mapfile));
                    }
                }
                updateMapSourceMenu();
                initMapDirectoryEdittext(true);
                break;
            case SELECT_GPX_EXPORT_REQUEST:
                checkDirectory(resultCode, data, R.id.gpx_exportdir, new RunnableWithArgument<String>() {

                    @Override
                    public void run(String directory) {
                        OldSettings.setGpxExportDir(directory);
                    }
                });
                break;
            case SELECT_GPX_IMPORT_REQUEST:
                checkDirectory(resultCode, data, R.id.gpx_importdir, new RunnableWithArgument<String>() {

                    @Override
                    public void run(String directory) {
                        OldSettings.setGpxImportDir(directory);
                    }
                });
                break;
            case SELECT_THEMEFOLDER_REQUEST:
                checkDirectory(resultCode, data, R.id.themefolder, new RunnableWithArgument<String>() {

                    @Override
                    public void run(String directory) {
                        OldSettings.setCustomRenderThemeBaseFolder(directory);
                    }
                });
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    private void checkDirectory(int resultCode, Intent data, int textField, RunnableWithArgument<String> runnableSetDir) {
        if (resultCode != RESULT_OK) {
            return;
        }
        final String directory = new File(data.getData().getPath()).getAbsolutePath();
        if (StringUtils.isNotBlank(directory)) {
            runnableSetDir.run(directory);
            EditText directoryText = (EditText) findViewById(textField);
            directoryText.setText(directory);
            directoryText.requestFocus();
        }
    }

    private void selectDirectory(String startDirectory, int directoryKind) {
        try {
            final Intent dirChooser = new Intent(FileManagerIntents.ACTION_PICK_DIRECTORY);
            if (StringUtils.isNotBlank(startDirectory)) {
                dirChooser.setData(Uri.fromFile(new File(startDirectory)));
            }
            dirChooser.putExtra(FileManagerIntents.EXTRA_TITLE, res.getString(R.string.simple_dir_chooser_title));
            dirChooser.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT, res.getString(android.R.string.ok));
            startActivityForResult(dirChooser, directoryKind);
        } catch (android.content.ActivityNotFoundException ex) {
            // OI file manager not available
            final Intent dirChooser = new Intent(this, SimpleDirChooser.class);
            dirChooser.putExtra(Intents.EXTRA_START_DIR, startDirectory);
            startActivityForResult(dirChooser, directoryKind);
        }
    }

    public static void startActivity(Context fromActivity) {
        final Intent initIntent = new Intent(fromActivity, OldSettingsActivity.class);
        fromActivity.startActivity(initIntent);
    }

}
