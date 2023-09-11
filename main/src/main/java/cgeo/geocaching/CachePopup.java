package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.AndroidRuntimeException;
import android.view.Window;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import org.apache.commons.lang3.StringUtils;

public class CachePopup extends AbstractActivity {

    protected String geocode = null;


    void showDialog() {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        final Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        final DialogFragment newFragment = CachePopupFragment.newInstance(geocode);
        newFragment.show(ft, "dialog");
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        } catch (final AndroidRuntimeException ex) {
            Log.e("Error requesting no title feature", ex);
        }
        this.setTheme(ActivityMixin.getDialogTheme());


        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            geocode = extras.getString(Intents.EXTRA_GEOCODE);
        }

        if (StringUtils.isBlank(geocode)) {
            showToast(res.getString(R.string.err_detail_cache_find));

            finish();
            return;
        }
        showDialog();
    }

    public static void startActivity(final Context context, final String geocode) {
        final Intent popupIntent = new Intent(context, CachePopup.class);
        popupIntent.putExtra(Intents.EXTRA_GEOCODE, geocode);

        context.startActivity(popupIntent);
    }

    public static void startActivityAllowTarget(final Activity activity, final String geocode) {
        final Intent popupIntent = new Intent(activity, CachePopup.class);
        popupIntent.putExtra(Intents.EXTRA_GEOCODE, geocode);

        activity.startActivityForResult(popupIntent, AbstractDialogFragment.REQUEST_CODE_TARGET_INFO);
    }
}
