package cgeo.geocaching.export;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.RouteSegment;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AsyncTaskWithProgress;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.UriUtils;
import cgeo.geocaching.utils.XmlUtils;
import cgeo.org.kxml2.io.KXmlSerializer;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.text.InputFilter;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlSerializer;

public class IndividualRouteExport {

    private String filename;

    public IndividualRouteExport(final Activity activity, final Route route) {

        filename = FileNameCreator.INDIVIDUAL_ROUTE_NOSUFFIX.createName(); //will not have a suffix

        final InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                if (FileUtils.FORBIDDEN_FILENAME_CHARS.indexOf(source.charAt(i)) >= 0) {
                    Toast.makeText(activity, String.format(activity.getString(R.string.err_invalid_filename_char), source.charAt(i)), Toast.LENGTH_SHORT).show();
                    return "";
                }
            }
            return null;
        };

        final AlertDialog.Builder builder = Dialogs.newBuilder(activity);
        builder.setTitle(R.string.export_individualroute_title);

        final View layout = View.inflate(activity, R.layout.gpx_export_individual_route_dialog, null);
        builder.setView(layout);

        final EditText editFilename = layout.findViewById(R.id.filename);
        editFilename.setFilters(new InputFilter[] { filter });
        editFilename.setText(filename);

        final ImageButton resetFilename = layout.findViewById(R.id.button_reset);
        resetFilename.setOnClickListener(v -> editFilename.setText(""));

        final TextView text = layout.findViewById(R.id.info);
        text.setText(activity.getString(R.string.export_confirm_message, PersistableFolder.GPX.toUserDisplayableValue(), filename + FileUtils.GPX_FILE_EXTENSION));

        builder
            .setPositiveButton(R.string.export, (dialog, which) -> {
                final String temp = StringUtils.trim(editFilename.getText().toString());
                filename = (StringUtils.isNotBlank(temp) ? temp : filename) + FileUtils.GPX_FILE_EXTENSION;
                dialog.dismiss();
                new Export(activity).execute(route.getSegments());
            })
            .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
            .create()
            .show();
    }

    private class Export extends AsyncTaskWithProgress<RouteSegment, Uri> {

        private static final String PREFIX_GPX = "";
        private static final String NS_GPX = "http://www.topografix.com/GPX/1/1";
        private static final String GPX_SCHEMA = NS_GPX + "/gpx.xsd";

        private static final String PREFIX_XSI = "xsi";
        private static final String NS_XSI = "http://www.w3.org/2001/XMLSchema-instance";

        Export(final Activity activity) {
            super(activity, activity.getString(R.string.export_individualroute_title));
        }

        @Override
        protected Uri doInBackgroundInternal(final RouteSegment[] trail) {
            final Uri uri = ContentStorage.get().create(PersistableFolder.GPX, filename);
            if (uri == null) {
                return null;
            }
            final XmlSerializer gpx = new KXmlSerializer();
            Writer writer = null;
            try {
                final OutputStream os = ContentStorage.get().openForWrite(uri);
                if (os == null) {
                    return null;
                }
                writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));

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

                final String timeInfo = CalendarUtils.formatDateTime("yyyy-MM-dd") + "T" + CalendarUtils.formatDateTime("hh:mm:ss") + "Z";

                gpx.startTag(NS_GPX, "metadata");
                XmlUtils.simpleText(gpx, NS_GPX, "name", "c:geo individual route");
                XmlUtils.simpleText(gpx, NS_GPX, "time", timeInfo);
                gpx.endTag(NS_GPX, "metadata");

                gpx.startTag(NS_GPX, "rte");
                XmlUtils.simpleText(gpx, NS_GPX, "name", "c:geo individual route " + timeInfo);
                for (RouteSegment loc : trail) {
                    gpx.startTag(null, "rtept");
                    final Geopoint point = loc.getPoint();
                    gpx.attribute(null, "lat", String.valueOf(point.getLatitude()));
                    gpx.attribute(null, "lon", String.valueOf(point.getLongitude()));
                    XmlUtils.simpleText(gpx, null, "name", loc.getItem().getIdentifier());
                    gpx.endTag(null, "rtept");
                    countExported++;
                    publishProgress(countExported);
                }
                gpx.endTag(NS_GPX, "rte");
                gpx.endTag(NS_GPX, "gpx");
            } catch (final IOException e) {
                Log.w("Could not write route to uri '" + uri + "'", e);
                // delete partial GPX file on error
                ContentStorage.get().delete(uri);
                return null;
            } finally {
                IOUtils.closeQuietly(writer);
            }
            return uri;
        }

        @Override
        protected void onPostExecuteInternal(final Uri exportUri) {
            if (null != activity) {
                if (null != exportUri) {
                    ShareUtils.shareOrDismissDialog(activity, exportUri, ShareUtils.TYPE_XML, R.string.export, String.format(activity.getString(R.string.export_individualroute_success), UriUtils.toUserDisplayableString(exportUri)));
                } else {
                    ActivityMixin.showToast(activity, activity.getString(R.string.export_failed));
                }
            }
        }
    }

}
