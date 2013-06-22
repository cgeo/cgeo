package cgeo.geocaching.settings;

import cgeo.geocaching.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class CheckBoxWithPopupPreference extends CheckBoxPreference {

    // strings for the popup dialog
    private String title;
    private String text;
    private String url;
    private String urlButton;

    public CheckBoxWithPopupPreference(Context context) {
        super(context);
    }

    public CheckBoxWithPopupPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        processAttributes(context, attrs, 0);
    }

    public CheckBoxWithPopupPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        processAttributes(context, attrs, defStyle);
    }

    private void processAttributes(Context context, AttributeSet attrs, int defStyle) {
        if (attrs == null) {
            return; // coward's retreat
        }

        TypedArray types = context.obtainStyledAttributes(attrs, new int[] {
                R.attr.title, R.attr.text, R.attr.url, R.attr.urlButton },
                defStyle, 0);

        title = types.getString(0);
        text = types.getString(1);
        url = types.getString(2);
        urlButton = types.getString(3);

        types.recycle();
    }

    @Override
    protected View onCreateView(ViewGroup parent) {

        // show dialog when checkbox enabled
        setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, Object newValue) {
                if (!(Boolean) newValue) {
                    return true;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        preference.getContext());
                builder.setMessage(text)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setTitle(title)
                        .setPositiveButton(R.string.err_none, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                        .setNegativeButton(urlButton, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse(url));
                                preference.getContext().startActivity(i);
                            }
                        });
                builder.create().show();
                return true;
            }
        });

        return super.onCreateView(parent);
    }

}
