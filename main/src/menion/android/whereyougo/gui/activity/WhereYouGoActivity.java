/*
 * This file is part of WhereYouGo.
 *
 * WhereYouGo is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * WhereYouGo is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with WhereYouGo. If not,
 * see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2012 Menion <whereyougo@asamm.cz>
 */

package menion.android.whereyougo.gui.activity;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Vector;

import cgeo.geocaching.CgeoApplication;
import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.formats.CartridgeFile;

import menion.android.whereyougo.MainApplication;
import cgeo.geocaching.R;
import menion.android.whereyougo.VersionInfo;
import menion.android.whereyougo.geo.location.LocationState;
import menion.android.whereyougo.gui.dialog.AboutDialog;
import menion.android.whereyougo.gui.dialog.ChooseCartridgeDialog;
import menion.android.whereyougo.gui.dialog.ChooseSavegameDialog;
import menion.android.whereyougo.gui.extension.activity.CustomActivity;
import menion.android.whereyougo.gui.utils.UtilsGUI;
import menion.android.whereyougo.network.activity.DownloadCartridgeActivity;
import menion.android.whereyougo.openwig.WLocationService;
import menion.android.whereyougo.openwig.WSaveFile;
import menion.android.whereyougo.openwig.WSeekableFile;
import menion.android.whereyougo.openwig.WUI;
import menion.android.whereyougo.preferences.Locale;
import menion.android.whereyougo.preferences.PreferenceValues;
import menion.android.whereyougo.preferences.Preferences;
import menion.android.whereyougo.utils.A;
import menion.android.whereyougo.utils.FileSystem;
import menion.android.whereyougo.utils.Logger;
import menion.android.whereyougo.utils.ManagerNotify;
import menion.android.whereyougo.utils.NotificationService;
import menion.android.whereyougo.utils.Utils;

import static menion.android.whereyougo.permission.PermissionHandler.askAgainFor;
import static menion.android.whereyougo.permission.PermissionHandler.checkKoPermissions;
import static menion.android.whereyougo.permission.PermissionHandler.checkPermissions;
import static menion.android.whereyougo.permission.PermissionHandler.needAskForPermission;

public class WhereYouGoActivity extends CustomActivity {

    private static final String TAG = "Main";
    private static final WLocationService wLocationService = new WLocationService();

    private static Vector<CartridgeFile> cartridgeFiles;
    private static final String[] DIRS = new String[]{FileSystem.CACHE};

    public static final WUI wui = new WUI();
    public static final int FINISH_NONE = -1;
    public static final int FINISH_EXIT = 0;
    public static final int FINISH_EXIT_FORCE = 1;
    public static final int FINISH_RESTART = 2;
    public static final int FINISH_RESTART_FORCE = 3;
    public static final int FINISH_RESTART_FACTORY_RESET = 4;
    public static final int FINISH_REINSTALL = 5;
    public static final int CLOSE_DESTROY_APP_NO_DIALOG = 0;
    public static final int CLOSE_DESTROY_APP_DIALOG_NO_TEXT = 1;
    public static final int CLOSE_DESTROY_APP_DIALOG_ADDITIONAL_TEXT = 2;
    public static final int CLOSE_HIDE_APP = 3;

    public static CartridgeFile cartridgeFile;
    public static String selectedFile;

    private int finishType = FINISH_NONE;
    private boolean finish = false;

    static {
        wui.setOnSavingStarted(() -> {
            try {
                FileSystem.backupFile(WhereYouGoActivity.getSaveFile());
            } catch (Exception e) {
            }
        });
    }

    public static File getSaveFile() throws IOException {
        try {
            return new File(selectedFile.substring(0, selectedFile.length() - 3) + "ows");
        } catch (SecurityException e) {
            Logger.e(TAG, "getSyncFile()", e);
            return null;
        }
    }

    public static File getLogFile() throws IOException {
        try {
            return new File(selectedFile.substring(0, selectedFile.length() - 3) + "owl");
        } catch (SecurityException e) {
            Logger.e(TAG, "getSyncFile()", e);
            return null;
        }
    }

    private static void loadCartridge(OutputStream log) {
        try {
            WUI.startProgressDialog();
            Engine.newInstance(cartridgeFile, log, wui, wLocationService).start();
        } catch (Throwable t) {
        }
    }

    private static void restoreCartridge(OutputStream log) {
        try {
            WUI.startProgressDialog();
            Engine.newInstance(cartridgeFile, log, wui, wLocationService).restore();
        } catch (Throwable t) {
        }
    }

    public static void startSelectedCartridge(boolean restore) {
        try {
            File file = getLogFile();
            FileOutputStream fos = null;
            try {
                if (!file.exists())
                    file.createNewFile();
                fos = new FileOutputStream(file, true);
            } catch (Exception e) {
                Logger.e(TAG, "onResume() - create empty saveGame file", e);
            }
            if (restore)
                WhereYouGoActivity.restoreCartridge(fos);
            else
                WhereYouGoActivity.loadCartridge(fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void refreshCartridges() {
        Logger.w(TAG, "refreshCartridges(), " + (WhereYouGoActivity.selectedFile == null));

        // load cartridge files
        File[] files = FileSystem.getFiles(FileSystem.ROOT, "gwc");
        cartridgeFiles = new Vector<>();

        // add cartridges to map
        //ArrayList<Waypoint> wpts = new ArrayList<>();

        File actualFile = null;
        if (files != null) {
            for (File file : files) {
                try {
                    actualFile = file;
                    CartridgeFile cart = CartridgeFile.read(new WSeekableFile(file), new WSaveFile(file));
                    if (cart != null) {
                        cart.filename = file.getAbsolutePath();

                        //Location loc = new Location(TAG);
                        //loc.setLatitude(cart.latitude);
                        //loc.setLongitude(cart.longitude);
                        //Waypoint waypoint = new Waypoint(cart.name, loc);

                        cartridgeFiles.add(cart);
                        //wpts.add(waypoint);
                    }
                } catch (Exception e) {
                    Logger.w(TAG, "refreshCartridge(), file:" + actualFile + ", e:" + e.toString());
                    ManagerNotify.toastShortMessage(Locale.getString(R.string.invalid_cartridge, actualFile.getName()));
                    // file.delete();
                }
            }
        }

        //if (wpts.size() > 0) {
            // TODO add items on map
        //}
    }

    public static void openCartridge(final CartridgeFile cartridgeFile) {
        final CustomActivity activity = A.getMain();
        if (activity == null) {
            return;
        }
        try {
            WhereYouGoActivity.cartridgeFile = cartridgeFile;
            WhereYouGoActivity.selectedFile = WhereYouGoActivity.cartridgeFile.filename;
            File saveFile = WhereYouGoActivity.getSaveFile();
            ChooseSavegameDialog chooseSavegameDialog = ChooseSavegameDialog.newInstance(saveFile);
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .add(chooseSavegameDialog, "DIALOG_TAG_CHOOSE_SAVE_FILE")
                    .commitAllowingStateLoss();
        } catch (Exception e) {
            Logger.e(TAG, "onCreate()", e);
        }
    }

    public static String getNewsFromTo(int lastVersion, int actualVersion) {
        // Logger.d(TAG, "getNewsFromTo(" + lastVersion + ", " + actualVersion + "), file:" +
        // "news_" + (Const.isPro() ? "pro" : "free") + ".xml");
        String versionInfo =
                "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /></head><body>";
        String data = loadAssetString("news.xml");
        if (data == null || data.length() == 0)
            data = loadAssetString("news.xml");
        if (data != null && data.length() > 0) {
            XmlPullParser parser;
            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                parser = factory.newPullParser();
                parser.setInput(new StringReader(data));

                int event;
                String tagName;

                boolean correct = false;
                while (true) {
                    event = parser.nextToken();
                    if (event == XmlPullParser.START_TAG) {
                        tagName = parser.getName();
                        if (tagName.equalsIgnoreCase("update")) {
                            String name = parser.getAttributeValue(null, "name");
                            int id = Utils.parseInt(parser.getAttributeValue(null, "id"));
                            if (id > lastVersion && id <= actualVersion) {
                                correct = true;
                                versionInfo += ("<h4>" + name + "</h4><ul>");
                            } else {
                                correct = false;
                            }
                        } else if (tagName.equalsIgnoreCase("li") && correct) {
                            versionInfo += ("<li>" + parser.nextText() + "</li>");
                        }
                    } else if (event == XmlPullParser.END_TAG) {
                        tagName = parser.getName();
                        if (tagName.equalsIgnoreCase("update")) {
                            if (correct) {
                                correct = false;
                                versionInfo += "</ul>";
                            }
                        } else if (tagName.equals("document")) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Logger.e(TAG, "getNews()", e);
            }
        }

        versionInfo += "</body></html>";
        return versionInfo;
    }

    public static String loadAssetString(String name) {
        InputStream is = null;
        try {
            is = A.getMain().getAssets().open(name);
            int size = is.available();

            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            return new String(buffer);
        } catch (Exception e) {
            Logger.e(TAG, "loadAssetString(" + name + ")", e);
            return "";
        } finally {
            Utils.closeStream(is);
        }
    }

    public void showDialogFinish(final int typeOfFinish) {
        // Logger.d(TAG, "showFinishDialog(" + typeOfFinish + ")");
        if (typeOfFinish == FINISH_NONE)
            return;

        this.finishType = typeOfFinish;

        runOnUiThread(() -> {
            String title = getString(R.string.question);
            String message = "";
            boolean cancelable =
                    !(finishType == FINISH_RESTART_FORCE || finishType == FINISH_RESTART_FACTORY_RESET
                            || finishType == FINISH_REINSTALL || finishType == FINISH_EXIT_FORCE);
            switch (finishType) {
                case FINISH_EXIT:
                    message = getString(R.string.do_you_really_want_to_exit);
                    break;
                case FINISH_EXIT_FORCE:
                    title = getString(R.string.info);
                    message = getString(R.string.you_have_to_exit_app_force);
                    break;
                case FINISH_RESTART:
                    message = getString(R.string.you_have_to_restart_app_recommended);
                    break;
                case FINISH_RESTART_FORCE:
                case FINISH_RESTART_FACTORY_RESET:
                    title = getString(R.string.info);
                    message = getString(R.string.you_have_to_restart_app_force);
                    break;
                case FINISH_REINSTALL:
                    title = getString(R.string.info);
                    message = getString(R.string.new_version_will_be_installed);
                    break;
                default:
                    // Do nothing
                    break;
            }

            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setCancelable(cancelable);
            b.setTitle(title);
            b.setIcon(R.drawable.ic_question_alt);
            b.setMessage(message);
            b.setPositiveButton(R.string.ok, (dialog, which) -> {
                if (finishType == FINISH_EXIT || finishType == FINISH_EXIT_FORCE) {
                    finish = true;
                    finish();
                } else if (finishType == FINISH_RESTART || finishType == FINISH_RESTART_FORCE
                        || finishType == FINISH_RESTART_FACTORY_RESET) {
                    // Setup one-short alarm to restart my application in 3 seconds - TODO need use
                    // another context
                    // AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    // Intent intent = new Intent(APP_INTENT_MAIN);
                    // PendingIntent pi = PendingIntent.getBroadcast(CustomMain.this, 0, intent,
                    // PendingIntent.FLAG_ONE_SHOT);
                    // alarmMgr.set(AlarmManager.ELAPSED_REALTIME, System.currentTimeMillis() + 3000, pi);
                    finish = true;
                    finish();
                } else if (finishType == FINISH_REINSTALL) {
                    // Intent intent = new Intent();
                    // intent.setAction(android.content.Intent.ACTION_VIEW);
                    // intent.setDataAndType(Uri.fromFile(new File(FileSystem.ROOT + "smartmaps.apk")),
                    // "application/vnd.android.package-archive");
                    //
                    // startActivity(intent);
                    showDialogFinish(FINISH_EXIT_FORCE);
                }
            });
            if (cancelable) {
                b.setNegativeButton(R.string.cancel, null);
            }
            b.show();
        });
    }

    protected boolean testFileSystem() {
        if (DIRS.length == 0)
            return true;

        if (FileSystem.createRoot(getString(R.string.app_name))) {
            Logger.i(TAG, "FileSystem successfully created!");
        } else {
            Logger.w(TAG, "FileSystem cannot be created!");
            UtilsGUI.showDialogError(this, R.string.filesystem_cannot_create_root,
                    (dialog, which) -> {
                        //showDialogFinish(FINISH_EXIT_FORCE);
                    });
            return false;
        }

        // fileSystem created successfully
        for (String DIR : DIRS) {
            (new File(DIR)).mkdirs();
        }
        return true;
    }

    /**
     * Method that create layout for actual activity. This is called everytime, onCreate method is
     * called
     */
    protected void eventCreateLayout() {
        setContentView(R.layout.layout_main);

        // set title
        ((TextView) findViewById(R.id.title_text)).setText(R.string.app_name);

        // define buttons
        View.OnClickListener mOnClickListener = v -> {
            switch (v.getId()) {
                case R.id.button_start:
                    clickStart();
                    break;
                case R.id.button_gps:
                    WhereYouGoActivity.this.startActivity(new Intent(WhereYouGoActivity.this, SatelliteActivity.class));
                    break;
                case R.id.button_settings:
                    WhereYouGoActivity.this.startActivity(new Intent(WhereYouGoActivity.this, XmlSettingsActivity.class));
                    break;
                case R.id.button_map:
                    clickMap();
                    break;
                case R.id.button_logo:
                    getSupportFragmentManager().beginTransaction()
                            .add(new AboutDialog(), "DIALOG_TAG_MAIN").commitAllowingStateLoss();
                    break;
            }
        };

        UtilsGUI.setButtons(this, new int[]{R.id.button_start, R.id.button_map, R.id.button_gps,
                R.id.button_settings, R.id.button_logo}, mOnClickListener, null);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Logger.d(TAG, "dispatchKeyEvent(" + event.getAction() + ", " + event.getKeyCode() + ")");
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            switch (getCloseValue()) {
                case CLOSE_DESTROY_APP_NO_DIALOG:
                    finish = true;
                    finish();
                    return true;
                case CLOSE_DESTROY_APP_DIALOG_NO_TEXT:
                case CLOSE_DESTROY_APP_DIALOG_ADDITIONAL_TEXT:
                    showDialogFinish(FINISH_EXIT);
                    return true;
                default:
                    // no action
                    break;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    /**
     * This is called only when application really need to be destroyed, so in this method is
     * suggested to clear all variables
     */
    protected void eventDestroyApp() {
        NotificationManager mNotificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();
    }

    /**
     * This is called only once after application start. It's called in onCreate method before layout
     * is placed
     */
    protected void eventFirstInit() {
        // call after start actions here
        VersionInfo.afterStartAction();
    }

    protected int getCloseValue() {
        return CLOSE_DESTROY_APP_NO_DIALOG;
    }

    private boolean isAnyCartridgeAvailable() {
        if (cartridgeFiles == null || cartridgeFiles.size() == 0) {
            UtilsGUI.showDialogInfo(
                    WhereYouGoActivity.this,
                    getString(R.string.no_wherigo_cartridge_available, FileSystem.ROOT));
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        String[] koPermissions = checkKoPermissions(this, permissions);
        if (koPermissions.length > 0) {
            askAgainFor(this, koPermissions);
        } else {
            testFileSystem();
            if (Preferences.GPS || Preferences.GPS_START_AUTOMATICALLY) {
                LocationState.setGpsOn(this);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        A.registerMain(this);

        if (A.getApp() == null) { // first app run
            // Logger.w(TAG, "onCreate() - init new");
            A.registerApp((CgeoApplication) getApplication());

            // not test some things
            testFileSystem();

            // set last known location
            if (Utils.isPermissionAllowed(Manifest.permission.ACCESS_FINE_LOCATION)
                    && (Preferences.GPS || Preferences.GPS_START_AUTOMATICALLY)) {
                LocationState.setGpsOn(this);
            } else {
                LocationState.setGpsOff(this);
            }

            eventFirstInit();
            setScreenBasic(this);
            eventCreateLayout();
        } else {
            // Logger.w(TAG, "onCreate() - only register");
            setScreenBasic(this);
            eventCreateLayout();
        }

        if (needAskForPermission()) {
            checkPermissions(this);
        }

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Intent intent = new Intent(getIntent());
            intent.setClass(this, DownloadCartridgeActivity.class);
            startActivity(intent);
            finish();
        } else if (Intent.ACTION_SEND.equals(getIntent().getAction())) {
            try {
                Uri uri = Uri.parse(getIntent().getStringExtra(Intent.EXTRA_TEXT));
                if (uri.getQueryParameter("CGUID") == null)
                    throw new Exception("Invalid URL");
                Intent intent = new Intent(this, DownloadCartridgeActivity.class);
                intent.setData(uri);
                startActivity(intent);
            } catch (Exception e) {
                ManagerNotify.toastShortMessage(this, getString(R.string.invalid_url));
            }
            finish();
        } else {
            String cguid = getIntent() == null ? null : getIntent().getStringExtra("cguid");
            if (cguid != null) {
                File file = FileSystem.findFile(cguid);
                if (file != null) {
                    openCartridge(file);
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCartridges();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_geocaching:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://geocaching.com/")));
                return true;
            case R.id.menu_wherigo:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://wherigo.com/")));
                return true;
            case R.id.menu_github:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/cgeo/WhereYouGo")));
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroy() {
        // Stop notification
        Intent intent = new Intent(WhereYouGoActivity.this, NotificationService.class);
        intent.setAction(NotificationService.STOP_NOTIFICATION_SERVICE);
        startService(intent);

        if (finish) {
            // stop debug if any forgotten
            Debug.stopMethodTracing();
            // remember value before A.getApp() exist
            boolean clearPackageAllowed =
                    Utils.isPermissionAllowed(Manifest.permission.KILL_BACKGROUND_PROCESSES);

            // call individual app close
            eventDestroyApp();

            // disable highlight
            PreferenceValues.disableWakeLock();
            // save last known location
            PreferenceValues.setLastKnownLocation();
            // disable GPS modul
            LocationState.destroy(WhereYouGoActivity.this);

            // destroy static references
            A.destroy();
            // call native destroy
            super.onDestroy();

            // remove app from memory
            if (clearPackageAllowed) {
                clearPackageFromMemory(); // XXX not work on 2.2 and higher!!!
            }
        } else {
            super.onDestroy();
        }
    }

    private void openCartridge(File file) {
        try {
            CartridgeFile cart = null;
            try {
                cart = CartridgeFile.read(new WSeekableFile(file), new WSaveFile(file));
                if (cart != null) {
                    cart.filename = file.getAbsolutePath();
                } else {
                    return;
                }
            } catch (Exception e) {
                Logger.w(TAG, "openCartridge(), file:" + file + ", e:" + e.toString());
                ManagerNotify.toastShortMessage(getString(R.string.invalid_cartridge, file.getName()));
                // file.delete();
            }
            openCartridge(cart);
        } catch (Exception e) {
            Logger.e(TAG, "onCreate()", e);
        }
    }

    private void clearPackageFromMemory() {
        new Thread(() -> {
            try {
                ActivityManager aM =
                        (ActivityManager) getApplicationContext().getSystemService(ACTIVITY_SERVICE);

                Thread.sleep(1250);
                //aM.killBackgroundProcesses(getPackageName()); //TODO WhereYouGo: what is this?
            } catch (Exception e) {
                Logger.e(TAG, "clearPackageFromMemory()", e);
            }
        }).start();
    }

    private void clickStart() {
        // check cartridges
        if (!isAnyCartridgeAvailable()) {
            return;
        }

        ChooseCartridgeDialog dialog = new ChooseCartridgeDialog();
        dialog.setParams(cartridgeFiles);
        getSupportFragmentManager()
                .beginTransaction()
                .add(dialog, "DIALOG_TAG_CHOOSE_CARTRIDGE")
                .commitAllowingStateLoss();
    }

    private void clickMap() {

//        MapDataProvider mdp = MapHelper.getMapDataProvider();
//        mdp.clear();
//        mdp.addCartridges(cartridgeFiles);
        WhereYouGoActivity.wui.showScreen(WUI.SCREEN_MAP, null);
    }
}
