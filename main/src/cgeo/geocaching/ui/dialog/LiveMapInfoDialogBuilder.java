package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.ContextThemeWrapper;
import android.view.View;

public class LiveMapInfoDialogBuilder {

    public static AlertDialog create(Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        // AlertDialog has always dark style, so we have to apply it as well always
        final View layout = View.inflate(new ContextThemeWrapper(activity, R.style.dark), R.layout.livemapinfo, null);
        builder.setView(layout);

        final int showCount = Settings.getLiveMapHintShowCount();
        Settings.setLiveMapHintShowCount(showCount + 1);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                CgeoApplication.getInstance().setLiveMapHintShownInThisSession();
            }
        });
        return builder.create();
    }

}
