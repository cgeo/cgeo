package cgeo.geocaching.activity;

import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgeo;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.Menu;
import android.view.SubMenu;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import gnu.android.app.appmanualclient.AppManualReaderClient;

import java.util.List;

public final class ActivityMixin {
    private static final int MENU_ICON_LOG_VISIT = android.R.drawable.ic_menu_edit;

    public final static void goHome(final Activity fromActivity) {
        final Intent intent = new Intent(fromActivity, cgeo.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        fromActivity.startActivity(intent);
        fromActivity.finish();
    }

    public final static void goManual(final Context context, final String helpTopic) {
        if (StringUtils.isBlank(helpTopic)) {
            return;
        }
        try {
            AppManualReaderClient.openManual(
                    "c-geo",
                    helpTopic,
                    context,
                    "http://manual.cgeo.org/");
        } catch (Exception e) {
            // nothing
        }
    }

    public final static void setTitle(final Activity activity, final String text) {
        if (StringUtils.isBlank(text)) {
            return;
        }

        final TextView title = (TextView) activity.findViewById(R.id.actionbar_title);
        if (title != null) {
            title.setText(text);
        }
    }

    public final static void showProgress(final Activity activity, final boolean show) {
        if (activity == null) {
            return;
        }

        final ProgressBar progress = (ProgressBar) activity.findViewById(R.id.actionbar_progress);
        if (show) {
            progress.setVisibility(View.VISIBLE);
        } else {
            progress.setVisibility(View.GONE);
        }
    }

    public final static void setTheme(final Activity activity) {
        if (Settings.isLightSkin()) {
            activity.setTheme(R.style.light);
        } else {
            activity.setTheme(R.style.dark);
        }
    }

    public final static void showToast(final Activity activity, final String text) {
        if (StringUtils.isNotBlank(text)) {
            Toast toast = Toast.makeText(activity, text, Toast.LENGTH_LONG);

            toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 100);
            toast.show();
        }
    }

    public final static void showShortToast(final Activity activity, final String text) {
        if (StringUtils.isNotBlank(text)) {
            Toast toast = Toast.makeText(activity, text, Toast.LENGTH_SHORT);

            toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 100);
            toast.show();
        }
    }

    public static final void helpDialog(final Activity activity, final String title, final String message, final Drawable icon) {
        if (StringUtils.isBlank(message)) {
            return;
        }

        AlertDialog.Builder dialog = new AlertDialog.Builder(activity).setTitle(title).setMessage(message).setCancelable(true);
        dialog.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        if (icon != null) {
            dialog.setIcon(icon);
        }

        AlertDialog alert = dialog.create();
        alert.show();
    }

    public static void helpDialog(Activity activity, String title, String message) {
        helpDialog(activity, title, message, null);
    }

    protected static void addVisitMenu(IAbstractActivity activity, Menu menu, cgCache cache) {
        if (cache == null) {
            return;
        }
        if (!cache.supportsLogging()) {
            return;
        }
        Resources res = ((Activity) activity).getResources();
        if (Settings.getLogOffline()) {
            SubMenu logMenu = menu.addSubMenu(1, IAbstractActivity.MENU_LOG_VISIT_OFFLINE, 0, res.getString(R.string.cache_menu_visit_offline)).setIcon(MENU_ICON_LOG_VISIT);
            List<Integer> logTypes = cache.getPossibleLogTypes();
            for (Integer logType : logTypes) {
                String label = cgBase.logTypes2.get(logType);
                logMenu.add(1, IAbstractActivity.MENU_LOG_VISIT_OFFLINE + logType, 0, label);
            }
            logMenu.add(1, IAbstractActivity.MENU_LOG_VISIT, 0, res.getString(R.string.cache_menu_visit));
        }
        else {
            menu.add(1, IAbstractActivity.MENU_LOG_VISIT, 0, res.getString(R.string.cache_menu_visit)).setIcon(MENU_ICON_LOG_VISIT);
        }
    }
}
