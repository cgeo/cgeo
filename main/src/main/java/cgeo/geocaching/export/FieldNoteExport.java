package cgeo.geocaching.export;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.FieldNotesCapability;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.UriUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Exports offline logs in the Groundspeak Field Note format.
 */
public class FieldNoteExport extends AbstractExport {
    private final String fileName;

    public FieldNoteExport() {
        super(R.string.fieldnotes);
        final SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        fileName = fileNameDateFormat.format(new Date()) + ".txt";
    }

    @Override
    public void export(@NonNull final List<Geocache> cachesList, @Nullable final Activity activity) {
        final Geocache[] caches = cachesList.toArray(new Geocache[0]);
        if (activity == null) {
            // No activity given, so no user interaction possible.
            // Start export with default parameters.
            new FieldNoteExportTask(null, Collections.emptyList(), false, getProgressTitle(), fileName, getName()).execute(caches);
        } else {
            // Show configuration dialog
            getExportOptionsDialog(caches, activity).show();
        }
    }

    @SuppressLint("SetTextI18n")
    private Dialog getExportOptionsDialog(final Geocache[] caches, final Activity activity) {
        final AlertDialog.Builder builder = Dialogs.newBuilder(activity);
        builder.setTitle(LocalizationUtils.getString(R.string.export_confirm_title, LocalizationUtils.getString(R.string.fieldnotes)));

        final View layout = View.inflate(activity, R.layout.fieldnote_export_dialog, null);
        builder.setView(layout);

        final TextView text = layout.findViewById(R.id.info);
        text.setText(LocalizationUtils.getString(R.string.export_confirm_message, UriUtils.toUserDisplayableString(PersistableFolder.FIELD_NOTES.getUri()), fileName));

        final CheckBox uploadOption = layout.findViewById(R.id.upload);
        uploadOption.setChecked(Settings.getFieldNoteExportUpload());

        // one more checkbox per further site which currently accepts field notes (e.g. opencaching.de)
        final Map<CheckBox, FieldNotesCapability> otherSites = new LinkedHashMap<>();
        final LinearLayout otherSitesLayout = layout.findViewById(R.id.upload_other_sites);
        for (final IConnector connector : ConnectorFactory.getConnectors()) {
            if (connector instanceof FieldNotesCapability && connector != GCConnector.getInstance()
                    && connector.isActive() && ((FieldNotesCapability) connector).canUploadFieldNotes()) {
                final CheckBox checkBox = (CheckBox) View.inflate(activity, R.layout.fieldnote_export_upload_checkbox, null);
                checkBox.setText(LocalizationUtils.getString(R.string.export_fieldnotes_upload_to, connector.getName()));
                checkBox.setChecked(Settings.getFieldNoteExportUploadOtherSites());
                otherSitesLayout.addView(checkBox);
                otherSites.put(checkBox, (FieldNotesCapability) connector);
            }
        }
        final CheckBox onlyNewOption = layout.findViewById(R.id.onlynew);
        onlyNewOption.setChecked(Settings.getFieldNoteExportOnlyNew());

        if (Settings.getFieldnoteExportDate() > 0) {
            onlyNewOption.setText(LocalizationUtils.getString(R.string.export_fieldnotes_onlynew) + " (" + Formatter.formatDateTime(Settings.getFieldnoteExportDate()) + ')');
        }

        builder.setPositiveButton(R.string.export, (dialog, which) -> {
            final boolean upload = uploadOption.isChecked();
            final boolean onlyNew = onlyNewOption.isChecked();
            Settings.setFieldNoteExportUpload(upload);
            Settings.setFieldNoteExportOnlyNew(onlyNew);

            final List<FieldNotesCapability> uploadTargets = new ArrayList<>();
            if (upload) {
                uploadTargets.add(GCConnector.getInstance());
            }
            boolean uploadOtherSites = false;
            for (final Map.Entry<CheckBox, FieldNotesCapability> site : otherSites.entrySet()) {
                if (site.getKey().isChecked()) {
                    uploadTargets.add(site.getValue());
                    uploadOtherSites = true;
                }
            }
            if (!otherSites.isEmpty()) {
                Settings.setFieldNoteExportUploadOtherSites(uploadOtherSites);
            }

            dialog.dismiss();
            new FieldNoteExportTask(activity, uploadTargets, onlyNew, getProgressTitle(), fileName, getName()).execute(caches);
        });

        return builder.create();
    }

}
