package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.utils.Log;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Modified progress dialog class which allows hiding the absolute numbers
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
            final Method method = TextView.class.getMethod("setVisibility", Integer.TYPE);

            final Field[] fields = this.getClass().getSuperclass().getDeclaredFields();

            for (final Field field : fields) {
                if (field.getName().equalsIgnoreCase("mProgressNumber")) {
                    field.setAccessible(true);
                    final TextView textView = (TextView) field.get(this);
                    method.invoke(textView, View.GONE);
                }
            }
        } catch (final NoSuchMethodException e) {
            Log.e("Failed to find the progressDialog method 'setVisibility'.", e);
        } catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Log.e("Failed to invoke the progressDialog method 'setVisibility' and set 'mProgressNumber' to GONE.", e);
        }
    }
}