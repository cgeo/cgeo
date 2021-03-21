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

package menion.android.whereyougo.openwig;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;

import java.util.Arrays;

import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.EventTable;
import cz.matejcik.openwig.Media;
import cz.matejcik.openwig.platform.UI;
import cgeo.geocaching.R;
import menion.android.whereyougo.audio.UtilsAudio;
import menion.android.whereyougo.gui.IRefreshable;
import menion.android.whereyougo.gui.activity.CartridgeDetailsActivity;
import menion.android.whereyougo.gui.activity.GuidingActivity;
import menion.android.whereyougo.gui.activity.wherigo.DetailsActivity;
import menion.android.whereyougo.gui.activity.wherigo.InputScreenActivity;
import menion.android.whereyougo.gui.activity.wherigo.ListActionsActivity;
import menion.android.whereyougo.gui.activity.wherigo.ListTargetsActivity;
import menion.android.whereyougo.gui.activity.wherigo.ListTasksActivity;
import menion.android.whereyougo.gui.activity.wherigo.ListThingsActivity;
import menion.android.whereyougo.gui.activity.wherigo.ListZonesActivity;
import menion.android.whereyougo.gui.activity.wherigo.MainMenuActivity;
import menion.android.whereyougo.gui.activity.wherigo.PushDialogActivity;
import menion.android.whereyougo.gui.extension.activity.CustomActivity;
import menion.android.whereyougo.gui.utils.UtilsGUI;
import menion.android.whereyougo.preferences.Locale;
import menion.android.whereyougo.preferences.PreferenceValues;
import menion.android.whereyougo.utils.A;
import menion.android.whereyougo.utils.Logger;
import menion.android.whereyougo.utils.ManagerNotify;
import se.krka.kahlua.vm.LuaClosure;

public class WUI implements UI {

    public static final int SCREEN_MAIN = 10;
    public static final int SCREEN_CART_DETAIL = 11;
    public static final int SCREEN_ACTIONS = 12;
    public static final int SCREEN_TARGETS = 13;
    public static final int SCREEN_MAP = 14;
    private static final String TAG = "WUI";
    public static boolean saving = false;
    private static ProgressDialog progressDialog;
    private Runnable onSavingStarted;
    private Runnable onSavingFinished;

    private static void closeActivity(Activity activity) {
        if (activity instanceof PushDialogActivity || activity instanceof GuidingActivity) {
            activity.finish();
        }
    }

    private static CustomActivity getParentActivity() {
        Activity activity = PreferenceValues.getCurrentActivity();

        if (!(activity instanceof CustomActivity))
            activity = A.getMain();

        return (CustomActivity) activity;
    }

    public static void showTextProgress(final String text) {
        Logger.i(TAG, "showTextProgress(" + text + ")");
    }

    public static void startProgressDialog() {
        progressDialog = new ProgressDialog(A.getMain());
        progressDialog.setMessage(Locale.getString(R.string.loading));
        progressDialog.show();
    }

    public void blockForSaving() {
        Logger.w(TAG, "blockForSaving()");
        saving = true;
        if (onSavingStarted != null) {
            onSavingStarted.run();
        }
    }

    public void debugMsg(String msg) {
        Logger.w(TAG, "debugMsg(" + msg.trim() + ")");
    }

    public void end() {
        if (progressDialog != null && progressDialog.isShowing()) {
            try {
                progressDialog.dismiss();
            } catch (Exception e) {
                Logger.e(TAG, "end(): dismiss progressDialog", e);
            }
        }
        Engine.kill();
        showScreen(SCREEN_MAIN, null);
    }

    // @Override
    public String getDeviceId() {
        return String.format("%s %s", A.getAppName(), A.getAppVersion());
    }

    public void playSound(byte[] data, String mime) {
        UtilsAudio.playSound(data, mime);
    }

    public void command(String cmd) {
        if ("StopSound".equals(cmd)) {
            UtilsAudio.stopSound();
        } else if ("Alert".equals(cmd)) {
            UtilsAudio.playBeep(1);
        }
    }

    public void pushDialog(String[] texts, Media[] media, String button1, String button2,
                           LuaClosure callback) {
        Logger.w(TAG, "pushDialog(" + Arrays.toString(texts) + ", " + Arrays.toString(media) + ", " + button1 + ", " + button2 + ", "
                + callback + ")");

        Activity activity = getParentActivity();
        PushDialogActivity.setDialog(texts, media, button1, button2, callback);
        Intent intent = new Intent(activity, PushDialogActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
        closeActivity(activity);

        Vibrator v = (Vibrator) A.getMain().getSystemService(Context.VIBRATOR_SERVICE);
        //v.vibrate(25); //TODO WhereYouGo: really need vibrate?
    }

    public void pushInput(EventTable input) {
        Logger.w(TAG, "pushInput(" + input + ")");
        Activity activity = getParentActivity();
        InputScreenActivity.setInput(input);
        Intent intent = new Intent(activity, InputScreenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
        closeActivity(activity);
    }

    public void refresh() {
        Activity activity = PreferenceValues.getCurrentActivity();
        Logger.w(TAG, "refresh(), currentActivity:" + activity);
        if (activity instanceof IRefreshable) {
            ((IRefreshable) activity).refresh();
        }
    }

    public void setStatusText(final String text) {
        Logger.w(TAG, "setStatus(" + text + ")");
        if (text == null || text.length() == 0)
            return;
        ManagerNotify.toastShortMessage(getParentActivity(), text);
    }

    public void showError(String msg) {
        Logger.e(TAG, "showError(" + msg.trim() + ")");
        if (PreferenceValues.getCurrentActivity() != null)
            UtilsGUI.showDialogError(PreferenceValues.getCurrentActivity(), msg);
    }

    public void showScreen(int screenId, EventTable details) {
        Activity activity = getParentActivity();
        Logger.w(TAG, "showScreen(" + screenId + "), parent:" + activity + ", param:" + details);

        // disable currentActivity
        PreferenceValues.setCurrentActivity(null);

        switch (screenId) {
            case MAINSCREEN:
                Intent intent01 = new Intent(activity, MainMenuActivity.class);
                activity.startActivity(intent01);
                return;
            case SCREEN_CART_DETAIL:
                Intent intent02 = new Intent(activity, CartridgeDetailsActivity.class);
                activity.startActivity(intent02);
                return;
            case DETAILSCREEN:
                DetailsActivity.et = details;
                Intent intent03 = new Intent(activity, DetailsActivity.class);
                intent03.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(intent03);
                return;
            case INVENTORYSCREEN:
                Intent intent04 = new Intent(activity, ListThingsActivity.class);
                intent04.putExtra("title", Locale.getString(R.string.inventory));
                intent04.putExtra("mode", ListThingsActivity.INVENTORY);
                activity.startActivity(intent04);
                return;
            case ITEMSCREEN:
                Intent intent05 = new Intent(activity, ListThingsActivity.class);
                intent05.putExtra("title", Locale.getString(R.string.you_see));
                intent05.putExtra("mode", ListThingsActivity.SURROUNDINGS);
                activity.startActivity(intent05);
                return;
            case LOCATIONSCREEN:
                Intent intent06 = new Intent(activity, ListZonesActivity.class);
                intent06.putExtra("title", Locale.getString(R.string.locations));
                activity.startActivity(intent06);
                return;
            case TASKSCREEN:
                Intent intent07 = new Intent(activity, ListTasksActivity.class);
                intent07.putExtra("title", Locale.getString(R.string.tasks));
                activity.startActivity(intent07);
                return;
            case SCREEN_ACTIONS:
                Intent intent09 = new Intent(activity, ListActionsActivity.class);
                if (details != null)
                    intent09.putExtra("title", details.name);
                activity.startActivity(intent09);
                return;
            case SCREEN_TARGETS:
                Intent intent10 = new Intent(activity, ListTargetsActivity.class);
                if (details != null)
                    intent10.putExtra("title", details.name);
                activity.startActivity(intent10);
                return;
            case SCREEN_MAP:
                //MapHelper.showMap(activity, details);
                return;
            default:
                closeActivity(activity);
        }
    }

    public void start() {
        A.getMain().runOnUiThread(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                try {
                    progressDialog.dismiss();
                } catch (Exception e) {
                    Logger.e(TAG, "start(): dismiss progressDialog", e);
                }
            }
        });
        showScreen(MAINSCREEN, null);
    }

    public void unblock() {
        Logger.w(TAG, "unblock()");
        saving = false;
        if (onSavingFinished != null) {
            onSavingFinished.run();
        }
    }

    public void setOnSavingStarted(Runnable onSavingStarted) {
        this.onSavingStarted = onSavingStarted;
    }

    public void setOnSavingFinished(Runnable onSavingFinished) {
        this.onSavingFinished = onSavingFinished;
    }
}
