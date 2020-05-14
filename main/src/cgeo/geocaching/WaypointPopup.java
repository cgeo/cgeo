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

public class WaypointPopup extends AbstractActivity {
    private int waypointId = 0;
    private String geocode;

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

    public static void startActivityAllowTarget(final Activity activity, final int waypointId, final String geocode) {
        final Intent popupIntent = new Intent(activity, WaypointPopup.class);
        popupIntent.putExtra(Intents.EXTRA_WAYPOINT_ID, waypointId);
        popupIntent.putExtra(Intents.EXTRA_GEOCODE, geocode);

        activity.startActivityForResult(popupIntent, AbstractDialogFragment.REQUEST_CODE_TARGET_INFO);
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
