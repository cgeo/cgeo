package cgeo.geocaching;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import org.apache.commons.lang3.StringUtils;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.ActivityMixin;

/**
 * Created by arne on 23.04.2014.
 */
public class CachePopup extends AbstractActivity {

    protected String geocode = null;


    void showDialog() {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = CachePopupFragment.newInstance(geocode);
        newFragment.show(ft, "dialog");
    }

    @SuppressWarnings("ResourceType")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

    public static void startActivity(Context context, String geocode) {
        final Intent popupIntent = new Intent(context, CachePopup.class);
        popupIntent.putExtra(Intents.EXTRA_GEOCODE, geocode);
        context.startActivity(popupIntent);
    }
}
