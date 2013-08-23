package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.cgeocaches;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.network.Network;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.view.View;

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
        final CharSequence[] items = { res.getString(R.string.user_menu_view_hidden),
                res.getString(R.string.user_menu_view_found),
                res.getString(R.string.user_menu_open_browser),
                res.getString(R.string.user_menu_send_message)
        };

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(res.getString(R.string.user_menu_title) + " " + name);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                switch (item) {
                    case 0:
                        cgeocaches.startActivityOwner(context, name.toString());
                        return;
                    case 1:
                        cgeocaches.startActivityUserName(context, name.toString());
                        return;
                    case 2:
                        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.geocaching.com/profile/?u=" + Network.encode(name.toString()))));
                        return;
                    case 3:
                        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.geocaching.com/email/?u=" + Network.encode(name.toString()))));
                        return;
                    default:
                        break;
                }
            }
        });
        final AlertDialog alert = builder.create();
        alert.show();
    }

}
