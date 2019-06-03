package cgeo.geocaching.ui;

import cgeo.geocaching.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class UrlPopup {

    private final Context context;

    public UrlPopup(final Context context) {
        this.context = context;
    }

    public void show(final String title, final String message, final String url, final String urlButtonTitle) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
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
}
