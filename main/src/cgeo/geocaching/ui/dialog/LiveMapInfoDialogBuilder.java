package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;

public class LiveMapInfoDialogBuilder {

    private LiveMapInfoDialogBuilder() {
        // utility class
    }

    public static AlertDialog create(final Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        final View layout = View.inflate(activity, R.layout.livemapinfo, null);
        builder.setView(layout);

        final int showCount = Settings.getLiveMapHintShowCount();
        Settings.setLiveMapHintShowCount(showCount + 1);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                dialog.dismiss();
            }
        });
        return builder.create();
    }

}
