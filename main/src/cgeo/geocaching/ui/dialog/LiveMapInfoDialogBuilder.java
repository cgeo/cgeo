package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgeoapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.CheckBox;

public class LiveMapInfoDialogBuilder {

    public static AlertDialog create(Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        final View layout = activity.getLayoutInflater().inflate(R.layout.livemapinfo, null);
        builder.setView(layout);

        final CheckBox checkBoxHide = (CheckBox) layout.findViewById(R.id.live_map_hint_hide);

        final int showCount = Settings.getLiveMapHintShowCount();
        if (showCount > 2) {
            checkBoxHide.setVisibility(View.VISIBLE);
        }
        Settings.setLiveMapHintShowCount(showCount + 1);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                cgeoapplication.getInstance().setLiveMapHintShown();
                if (checkBoxHide.getVisibility() == View.VISIBLE && checkBoxHide.isChecked()) {
                    Settings.setHideLiveHint(true);
                }
            }
        });
        return builder.create();
    }

}
