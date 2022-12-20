package cgeo.geocaching.export;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AsyncTaskWithProgress;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.UriUtils;

import android.app.Activity;
import android.app.Dialog;
import android.net.Uri;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class GpxExport extends AbstractExport {

    private String fileName = "geocache.gpx"; // used in tests
    private String title = null;

    public GpxExport() {
        super(R.string.export_gpx);
    }

    public void export(@NonNull final List<Geocache> caches, @Nullable final Activity activity, @Nullable final String title) {
        if (StringUtils.isNotBlank(title)) {
            this.title = title.replace("/", "_");
        }
        export(caches, activity);
    }

    @Override
    public void export(@NonNull final List<Geocache> caches, @Nullable final Activity activity) {
        final String[] geocodes = getGeocodes(caches);
        calculateFileName(geocodes);
        if (activity == null) {
            // No activity given, so no user interaction possible.
            // Start export with default parameters.
            new ExportTask(null).execute(geocodes);

        } else {
            // Show configuration dialog
            getExportDialog(geocodes, activity).show();
        }
    }

    private void calculateFileName(final String[] geocodes) {
        if (geocodes.length == 1) {
            // geocode as file name
            fileName = geocodes[0] + (StringUtils.isNotBlank(title) ? " " + title : "") + ".gpx";
        } else {
            fileName = FileNameCreator.GPX_EXPORT.createName();
            if (StringUtils.isNotBlank(title)) {
                final int pos = fileName.lastIndexOf(".");
                if (pos >= 0) {
                    final String first = fileName.substring(0, pos);
                    final String last = fileName.substring(pos);
                    fileName = first + " " + title + last;
                } else {
                    fileName += " " + title;
                }
            }
        }
    }

    private Dialog getExportDialog(final String[] geocodes, final Activity activity) {
        final AlertDialog.Builder builder = Dialogs.newBuilder(activity);
        builder.setTitle(activity.getString(R.string.export_confirm_title, activity.getString(R.string.export_gpx)));

        final View layout = View.inflate(activity, R.layout.gpx_export_dialog, null);
        builder.setView(layout);

        final TextView text = layout.findViewById(R.id.info);
        text.setText(activity.getString(R.string.export_confirm_message, PersistableFolder.GPX.toUserDisplayableValue(), fileName));

        final CheckBox includeFoundStatus = layout.findViewById(R.id.include_found_status);
        includeFoundStatus.setChecked(Settings.getIncludeFoundStatus());

        builder.setPositiveButton(R.string.export, (dialog, which) -> {
            Settings.setIncludeFoundStatus(includeFoundStatus.isChecked());
            dialog.dismiss();
            new ExportTask(activity).execute(geocodes);
        });

        return builder.create();
    }

    private static String[] getGeocodes(final List<Geocache> caches) {
        return Geocache.getGeocodes(caches).toArray(new String[caches.size()]);
    }

    protected class ExportTask extends AsyncTaskWithProgress<String, Uri> {

        /**
         * Instantiates and configures the task for exporting field notes.
         *
         * @param activity optional: Show a progress bar and toasts
         */
        public ExportTask(final Activity activity) {
            super(activity, getProgressTitle());
        }

        @Override
        protected Uri doInBackgroundInternal(final String[] geocodes) {
            final List<String> allGeocodes = new ArrayList<>(Arrays.asList(geocodes));

            setMessage(CgeoApplication.getInstance().getResources().getQuantityString(R.plurals.cache_counts, allGeocodes.size(), allGeocodes.size()));

            final Uri uri = ContentStorage.get().create(PersistableFolder.GPX, fileName);
            if (uri == null) {
                return null;
            }

            BufferedWriter writer = null;
            try (OutputStream os = ContentStorage.get().openForWrite(uri)) {
                if (os == null) {
                    return null;
                }

                writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
                new GpxSerializer().writeGPX(allGeocodes, writer, ExportTask.this::publishProgress);
            } catch (final IOException e) {
                Log.e("GpxExport.ExportTask export", e);
                // delete partial GPX file on error
                ContentStorage.get().delete(uri);

                return null;
            } finally {
                IOUtils.closeQuietly(writer);
            }

            return uri;
        }

        @Override
        protected void onPostExecuteInternal(final Uri uri) {
            final Activity activityLocal = activity;
            if (activityLocal != null) {
                if (uri != null) {
                    ShareUtils.shareOrDismissDialog(activityLocal, uri, "application/xml", R.string.export, getName() + ' ' + activityLocal.getString(R.string.export_exportedto) + ": " + UriUtils.toUserDisplayableString(uri));
                } else {
                    ActivityMixin.showToast(activityLocal, activityLocal.getString(R.string.export_failed));
                }
            }
        }

    }
}
