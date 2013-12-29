package cgeo.geocaching.ui;

import cgeo.contacts.IContacts;
import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.ProcessUtils;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

abstract class AbstractUserClickListener implements View.OnClickListener {

    private final boolean enabled;

    public AbstractUserClickListener(final boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void onClick(View view) {
        if (view == null) {
            return;
        }
        if (!enabled) {
            return;
        }

        showUserActionsDialog(getUserName(view), view);
    }

    protected abstract CharSequence getUserName(View view);

    /**
     * Opens a dialog to do actions on an user name
     */
    protected static void showUserActionsDialog(final CharSequence name, final View view) {
        final AbstractActivity context = (AbstractActivity) view.getContext();
        final Resources res = context.getResources();
        List<String> actions = new ArrayList<String>(Arrays.asList(res.getString(R.string.user_menu_view_hidden),
                res.getString(R.string.user_menu_view_found),
                res.getString(R.string.user_menu_open_browser),
                res.getString(R.string.user_menu_send_message)));
        if (isContactsAddonAvailable()) {
            actions.add(res.getString(R.string.user_menu_open_contact));
        }
        final CharSequence[] items = actions.toArray(new String[actions.size()]);

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(res.getString(R.string.user_menu_title) + " " + name);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                switch (item) {
                    case 0:
                        CacheListActivity.startActivityOwner(context, name.toString());
                        return;
                    case 1:
                        CacheListActivity.startActivityUserName(context, name.toString());
                        return;
                    case 2:
                        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.geocaching.com/profile/?u=" + Network.encode(name.toString()))));
                        return;
                    case 3:
                        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.geocaching.com/email/?u=" + Network.encode(name.toString()))));
                        return;
                    case 4:
                        openContactCard(context, name.toString());
                        return;
                    default:
                        break;
                }
            }
        });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    protected static void openContactCard(AbstractActivity context, String userName) {
        final Parameters params = new Parameters(
                IContacts.PARAM_NAME, userName
                );

        context.startActivity(new Intent(IContacts.INTENT,
                Uri.parse(IContacts.URI_SCHEME + "://" + IContacts.URI_HOST + "?" + params.toString())));
    }

    private static boolean isContactsAddonAvailable() {
        return ProcessUtils.isIntentAvailable(IContacts.INTENT, Uri.parse(IContacts.URI_SCHEME + "://" + IContacts.URI_HOST));
    }

}
