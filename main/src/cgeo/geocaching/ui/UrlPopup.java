package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.app.AlertDialog;

public class UrlPopup {

    private final Context context;

    public UrlPopup(final Context context) {
        this.context = context;
    }

    public void show(final String title, final String message, final String url, final String urlButtonTitle) {
        final AlertDialog.Builder builder = Dialogs.newBuilder(context);
        builder.setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(title)
                .setPositiveButton(R.string.err_none, (dialog, id) -> dialog.cancel())
                .setNegativeButton(urlButtonTitle, (dialog, id) -> {
                    final Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    context.startActivity(i);
                });
        builder.create().show();
    }

    public void forward(final String title, final String message, final String url) {
        final AlertDialog.Builder builder = Dialogs.newBuilder(context);
        builder.setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(title)
                .setPositiveButton(R.string.err_none, (dialog, id) -> {
                    final Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    context.startActivity(i);
                })
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> dialog.cancel());
        builder.create().show();
    }
}
