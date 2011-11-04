package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.files.GPXImporter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;

public class GPXImportActivity extends AbstractActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ("content".equals(getIntent().getScheme())) {
            importGPXFromIntent();
        }
    }

    private void importGPXFromIntent() {
        new AlertDialog.Builder(this)
                .setTitle(res.getString(R.string.gpx_import_title))
                .setMessage(res.getString(R.string.gpx_import_confirm))
                .setCancelable(false)
                .setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        (new GPXImporter(cgList.STANDARD_LIST_ID)).importGPX(GPXImportActivity.this, (Uri) getIntent().getData(), getContentResolver());
                    }
                })
                .setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .create()
                .show();
    }
}
