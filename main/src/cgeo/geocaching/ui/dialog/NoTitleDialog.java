package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.utils.Log;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;

public abstract class NoTitleDialog extends Dialog {

    public NoTitleDialog(Context context) {
        super(context);
    }

    public NoTitleDialog(Context context, int theme) {
        super(context, theme);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        } catch (final Exception e) {
            Log.e("NoTitleDialog.onCreate", e);
        }
    }
}
