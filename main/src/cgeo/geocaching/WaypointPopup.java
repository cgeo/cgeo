package cgeo.geocaching;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.Window;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.ActivityMixin;

public class WaypointPopup extends AbstractActivity {
    private int waypointId = 0;
    private String geocode;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        this.setTheme(ActivityMixin.getDialogTheme());


        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            waypointId = extras.getInt(Intents.EXTRA_WAYPOINT_ID);
            geocode = extras.getString(Intents.EXTRA_GEOCODE);
        }
        showDialog();

    }

    public static void startActivity(final Context context, final int waypointId, final String geocode) {
        final Intent popupIntent = new Intent(context, WaypointPopup.class);
        popupIntent.putExtra(Intents.EXTRA_WAYPOINT_ID, waypointId);
        popupIntent.putExtra(Intents.EXTRA_GEOCODE, geocode);
        context.startActivity(popupIntent);
    }


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
        final DialogFragment newFragment = WaypointPopupFragment.newInstance(geocode, waypointId);
        newFragment.show(ft, "dialog");
    }


}
