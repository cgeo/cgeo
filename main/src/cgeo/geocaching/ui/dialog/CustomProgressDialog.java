package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.utils.Log;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import java.lang.reflect.Field;

/**
 * Modified progress dialog class which allows hiding the absolute numbers.
 *
 */
public class CustomProgressDialog extends ProgressDialog {

    public CustomProgressDialog(final Context context) {
        super(context, ActivityMixin.getDialogTheme());
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // Field is private, make it accessible through reflection before hiding it.
            final Field field = getClass().getSuperclass().getDeclaredField("mProgressNumber");
            field.setAccessible(true);
            ((View) field.get(this)).setVisibility(View.GONE);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e("Failed to find the progressDialog field 'mProgressNumber'", e);
        }
    }
}