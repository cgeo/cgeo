package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.R;
import cgeo.geocaching.OldSettings;
import cgeo.geocaching.cgeoapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.CheckBox;

public class LiveMapInfoDialogBuilder {

    public static AlertDialog create(Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        // AlertDialog has always dark style, so we have to apply it as well always
        final View layout = View.inflate(new ContextThemeWrapper(activity, R.style.dark), R.layout.livemapinfo, null);
        builder.setView(layout);

        final CheckBox checkBoxHide = (CheckBox) layout.findViewById(R.id.live_map_hint_hide);

        final int showCount = OldSettings.getLiveMapHintShowCount();
        if (showCount > 2) {
            checkBoxHide.setVisibility(View.VISIBLE);
        }
        OldSettings.setLiveMapHintShowCount(showCount + 1);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                cgeoapplication.getInstance().setLiveMapHintShown();
                if (checkBoxHide.getVisibility() == View.VISIBLE && checkBoxHide.isChecked()) {
                    OldSettings.setHideLiveHint(true);
                }
            }
        });
        return builder.create();
    }

}
