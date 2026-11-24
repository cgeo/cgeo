package cgeo.geocaching.export;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.models.TrailHistoryElement;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.UriUtils;
import cgeo.geocaching.utils.workertask.ProgressDialogFeature;
import cgeo.geocaching.utils.workertask.WorkerTask;
import cgeo.geocaching.utils.xml.XmlUtils;

import android.app.Activity;
import android.net.Uri;
import android.util.Xml;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.xmlpull.v1.XmlSerializer;

/**
 * Exports the trail history as a GPX track using a {@link WorkerTask}, so a running export survives configuration
 * changes (e.g. rotation) of the activity which started it.
 * <br>
 * Activities which want to start a trail history export must call {@link #attach(ComponentActivity, Runnable)} once
 * per instance, before they are started (e.g. in onCreate).
 */
public final class TrailHistoryExportTask {

    private static final String TASK_ID = "trailhistory-export";

    private static final String PREFIX_GPX = "";
    private static final String NS_GPX = "http://www.topografix.com/GPX/1/1";
    private static final String GPX_SCHEMA = NS_GPX + "/gpx.xsd";

    private static final String PREFIX_XSI = "xsi";
    private static final String NS_XSI = "http://www.w3.org/2001/XMLSchema-instance";

    /** Parameters of one export run */
    static class Input {
        @NonNull private final TrailHistoryElement[] trail;
        @NonNull private final String filename;
        private final boolean clearAfterExport;

        Input(@NonNull final TrailHistoryElement[] trail, @NonNull final String filename, final boolean clearAfterExport) {
            this.trail = trail;
            this.filename = filename;
            this.clearAfterExport = clearAfterExport;
        }

        @NonNull
        @Override
        public String toString() {
            return filename + "(" + trail.length + " points, clearAfterExport=" + clearAfterExport + ")";
        }
    }

    /** Progress of one export run: number of exported points and total number of points */
    private static class ExportProgress {
        private final int exported;
        private final int total;

        private ExportProgress(final int exported, final int total) {
            this.exported = exported;
            this.total = total;
        }
    }

    /** Result of one export run */
    private static class ExportResult {
        @Nullable private final Uri uri;
        private final boolean cleared;

        private ExportResult(@Nullable final Uri uri, final boolean cleared) {
            this.uri = uri;
            this.cleared = cleared;
        }
    }

    private TrailHistoryExportTask() {
        // utility class
    }

    /**
     * Attaches the trail history export task to the given activity: shows progress and handles the result while the
     * activity is active.
     *
     * @param clearTrailHistory clears the trail history shown by this activity instance; run after a successful export
     *                          if the user asked to clear the trail afterwards
     */
    public static void attach(@NonNull final ComponentActivity activity, @NonNull final Runnable clearTrailHistory) {
        WorkerTask.<Input, ExportProgress, ExportResult>of(
                TASK_ID,
                (input, progress, cancelFlag) -> {
                    final Uri uri = export(input, progress);
                    final boolean clear = uri != null && input.clearAfterExport;
                    if (clear) {
                        // done here rather than in the result handler, which doesn't run if the activity is gone by then
                        DataStore.clearTrailHistory();
                    }
                    return new ExportResult(uri, clear);
                },
                AndroidRxUtils.computationScheduler)
            .addFeature(ProgressDialogFeature.<ExportProgress>of(activity)
                .setTitle(LocalizationUtils.getString(R.string.export_trailhistory_title))
                .setMessageMapper(p -> p.exported + " / " + p.total)
                .setProgressParameter(100, p -> p.total == 0 ? 0 : p.exported * 100 / p.total))
            .observeResult(activity, result -> onExportFinished(activity, result, clearTrailHistory), error -> onExportFailed(activity));
    }

    /**
     * Starts a trail history export. If the activity did not call {@link #attach(ComponentActivity, Runnable)},
     * the export runs in background without progress dialog and result message.
     */
    static void start(@NonNull final Activity activity, @NonNull final Input input) {
        final WorkerTask<Input, ExportProgress, ExportResult> task = WorkerTask.get(TASK_ID);
        if (task != null) {
            task.start(input);
            return;
        }
        Log.w("TrailHistoryExportTask: activity did not attach to export task, exporting without feedback: " + activity.getClass().getName());
        AndroidRxUtils.computationScheduler.scheduleDirect(() -> {
            if (export(input, null) != null && input.clearAfterExport) {
                DataStore.clearTrailHistory();
            }
        });
    }

    /**
     * Writes the GPX file synchronously.
     *
     * @return the Uri of the written file, or null if the export failed
     */
    @WorkerThread
    @Nullable
    private static Uri export(@NonNull final Input input, @Nullable final Consumer<ExportProgress> progress) {

        final Uri uri = ContentStorage.get().create(PersistableFolder.GPX, input.filename);
        if (uri == null) {
            return null;
        }

        BufferedWriter writer = null;
        OutputStream os = null;
        try {
            os = ContentStorage.get().openForWrite(uri);
            if (os == null) {
                return null;
            }
            try {
                writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
                final XmlSerializer gpx = Xml.newSerializer();

                final int total = input.trail.length;
                int countExported = 0;
                gpx.setOutput(writer);
                gpx.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

                gpx.startDocument(StandardCharsets.UTF_8.name(), true);
                gpx.setPrefix(PREFIX_GPX, NS_GPX);
                gpx.setPrefix(PREFIX_XSI, NS_XSI);

                gpx.startTag(NS_GPX, "gpx");
                gpx.attribute("", "version", "1.1");
                gpx.attribute("", "creator", "c:geo - http://www.cgeo.org/");
                gpx.attribute(NS_XSI, "schemaLocation", NS_GPX + " " + GPX_SCHEMA);

                final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                formatter.setTimeZone(TimeZone.getTimeZone("GMT"));

                gpx.startTag(NS_GPX, "metadata");
                XmlUtils.simpleText(gpx, NS_GPX, "name", "c:geo history trail");
                XmlUtils.simpleText(gpx, NS_GPX, "time", formatter.format(new Date()));
                gpx.endTag(NS_GPX, "metadata");

                gpx.startTag(NS_GPX, "trk");
                XmlUtils.simpleText(gpx, NS_GPX, "name", "c:geo history trail " + formatter.format(new Date()));
                gpx.startTag(NS_GPX, "trkseg");
                for (TrailHistoryElement trailHistoryElement : input.trail) {
                    gpx.startTag(null, "trkpt");
                    // all decimal points have to be ".", thus use non-localizing methods
                    gpx.attribute(null, "lat", String.valueOf(trailHistoryElement.getLatitude()));
                    gpx.attribute(null, "lon", String.valueOf(trailHistoryElement.getLongitude()));
                    XmlUtils.simpleText(gpx, null, "ele", String.format(Locale.US, "%.2f", trailHistoryElement.getAltitude()));
                    XmlUtils.simpleText(gpx, null, "time", formatter.format(trailHistoryElement.getTimestamp()));
                    gpx.endTag(null, "trkpt");
                    countExported++;
                    if (progress != null) {
                        progress.accept(new ExportProgress(countExported, total));
                    }
                }
                gpx.endTag(NS_GPX, "trkseg");
                gpx.endTag(NS_GPX, "trk");
                gpx.endTag(NS_GPX, "gpx");
                gpx.endDocument();
                gpx.flush();
            } catch (final IOException e) {
                // delete partial GPX file on error
                Log.e("IOException on trail export: " + e.getMessage());
                ContentStorage.get().delete(uri);
                return null;
            } finally {
                IOUtils.closeQuietly(writer);
                IOUtils.closeQuietly(os);
            }
        } catch (final Exception e) {
            Log.e("Exception on trail export: " + e.getMessage());
            return null;
        }

        return uri;
    }

    private static void onExportFinished(@NonNull final Activity activity, @NonNull final ExportResult result, @NonNull final Runnable clearTrailHistory) {
        if (result.uri == null) {
            onExportFailed(activity);
            return;
        }
        ShareUtils.shareOrDismissDialog(activity, result.uri, ShareUtils.TYPE_XML, R.string.export, LocalizationUtils.getString(R.string.export_trailhistory_success, UriUtils.toUserDisplayableString(result.uri)));
        if (result.cleared) {
            // stored trail is already cleared, this updates the trail shown by the current activity instance
            clearTrailHistory.run();
        }
    }

    private static void onExportFailed(@NonNull final Activity activity) {
        ActivityMixin.showToast(activity, LocalizationUtils.getString(R.string.export_failed));
    }
}
