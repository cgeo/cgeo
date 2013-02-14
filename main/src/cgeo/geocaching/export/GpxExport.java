package cgeo.geocaching.export;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogEntry;
import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.Waypoint;
import cgeo.geocaching.cgData;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.utils.BaseUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.XmlUtils;

import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlSerializer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Xml;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

class GpxExport extends AbstractExport {
    private static final SimpleDateFormat dateFormatZ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
    public static final String PREFIX_XSI = "http://www.w3.org/2001/XMLSchema-instance";
    public static final String PREFIX_GPX = "http://www.topografix.com/GPX/1/0";
    public static final String PREFIX_GROUNDSPEAK = "http://www.groundspeak.com/cache/1/0";

    protected GpxExport() {
        super(getString(R.string.export_gpx));
    }

    @Override
    public void export(final List<Geocache> caches, final Activity activity) {
        if (null == activity) {
            // No activity given, so no user interaction possible.
            // Start export with default parameters.
            new ExportTask(caches, null).execute((Void) null);

        } else {
            // Show configuration dialog
            getExportDialog(caches, activity).show();
        }
    }

    private Dialog getExportDialog(final List<Geocache> caches, final Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        // AlertDialog has always dark style, so we have to apply it as well always
        View layout = View.inflate(new ContextThemeWrapper(activity, R.style.dark), R.layout.gpx_export_dialog, null);
        builder.setView(layout);

        final TextView text = (TextView) layout.findViewById(R.id.info);
        text.setText(getString(R.string.export_gpx_info, Settings.getGpxExportDir()));

        final CheckBox shareOption = (CheckBox) layout.findViewById(R.id.share);

        shareOption.setChecked(Settings.getShareAfterExport());

        shareOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Settings.setShareAfterExport(shareOption.isChecked());
            }
        });

        builder.setPositiveButton(R.string.export, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                new ExportTask(caches, activity).execute((Void) null);
            }
        });

        return builder.create();
    }

    private class ExportTask extends AsyncTask<Void, Integer, File> {
        private final List<Geocache> caches;
        private final Activity activity;
        private final Progress progress = new Progress();

        /**
         * Instantiates and configures the task for exporting field notes.
         *
         * @param caches
         *            The {@link List} of {@link cgeo.geocaching.Geocache} to be exported
         * @param activity
         *            optional: Show a progress bar and toasts
         */
        public ExportTask(final List<Geocache> caches, final Activity activity) {
            this.caches = caches;
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            if (null != activity) {
                progress.show(activity, null, getString(R.string.export) + ": " + getName(), ProgressDialog.STYLE_HORIZONTAL, null);
                progress.setMaxProgressAndReset(caches.size());
            }
        }

        @Override
        protected File doInBackground(Void... params) {
            // quick check for being able to write the GPX file
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                return null;
            }

            final SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
            final File exportFile = new File(Settings.getGpxExportDir() + File.separatorChar + "export_" + fileNameDateFormat.format(new Date()) + ".gpx");
            FileWriter writer = null;
            try {
                final File exportLocation = new File(Settings.getGpxExportDir());
                exportLocation.mkdirs();

                final XmlSerializer gpx = Xml.newSerializer();
                writer = new FileWriter(exportFile);
                gpx.setOutput(writer);

                gpx.startDocument("UTF-8", true);
                gpx.setPrefix("", PREFIX_GPX);
                gpx.setPrefix("xsi", PREFIX_XSI);
                gpx.setPrefix("groundspeak", PREFIX_GROUNDSPEAK);
                gpx.startTag(PREFIX_GPX, "gpx");
                gpx.attribute("", "version", "1.0");
                gpx.attribute("", "creator", "c:geo - http://www.cgeo.org/");
                gpx.attribute(PREFIX_XSI, "schemaLocation",
                        PREFIX_GPX + " http://www.topografix.com/GPX/1/0/gpx.xsd " +
                        PREFIX_GROUNDSPEAK + " http://www.groundspeak.com/cache/1/0/1/cache.xsd");

                for (int i = 0; i < caches.size(); i++) {
                    final Geocache cache = cgData.loadCache(caches.get(i).getGeocode(), LoadFlags.LOAD_ALL_DB_ONLY);

                    gpx.startTag(PREFIX_GPX, "wpt");
                    gpx.attribute("", "lat", Double.toString(cache.getCoords().getLatitude()));
                    gpx.attribute("", "lon", Double.toString(cache.getCoords().getLongitude()));

                    final Date hiddenDate = cache.getHiddenDate();
                    if (hiddenDate != null) {
                        XmlUtils.simpleText(gpx, PREFIX_GPX, "time", dateFormatZ.format(hiddenDate));
                    }

                    XmlUtils.multipleTexts(gpx, PREFIX_GPX,
                            "name", cache.getGeocode(),
                            "desc", cache.getName(),
                            "url", cache.getUrl(),
                            "urlname", cache.getName(),
                            "sym", cache.isFound() ? "Geocache Found" : "Geocache",
                            "type", "Geocache|" + cache.getType().pattern);

                    gpx.startTag(PREFIX_GROUNDSPEAK, "cache");
                    gpx.attribute("", "id", cache.getCacheId());
                    gpx.attribute("", "available", !cache.isDisabled() ? "True" : "False");
                    gpx.attribute("", "archives", cache.isArchived() ? "True" : "False");


                    XmlUtils.multipleTexts(gpx, PREFIX_GROUNDSPEAK,
                            "name", cache.getName(),
                            "placed_by", cache.getOwnerDisplayName(),
                            "owner", cache.getOwnerUserId(),
                            "type", cache.getType().pattern,
                            "container", cache.getSize().id,
                            "difficulty", Float.toString(cache.getDifficulty()),
                            "terrain", Float.toString(cache.getTerrain()),
                            "country", cache.getLocation(),
                            "state", "",
                            "encoded_hints", cache.getHint());

                    writeAttributes(gpx, cache);

                    gpx.startTag(PREFIX_GROUNDSPEAK, "short_description");
                    gpx.attribute("", "html", BaseUtils.containsHtml(cache.getShortDescription()) ? "True" : "False");
                    gpx.text(cache.getShortDescription());
                    gpx.endTag(PREFIX_GROUNDSPEAK, "short_description");

                    gpx.startTag(PREFIX_GROUNDSPEAK, "long_description");
                    gpx.attribute("", "html", BaseUtils.containsHtml(cache.getDescription()) ? "True" : "False");
                    gpx.text(cache.getDescription());
                    gpx.endTag(PREFIX_GROUNDSPEAK, "long_description");

                    writeLogs(gpx, cache);

                    gpx.endTag(PREFIX_GROUNDSPEAK, "cache");
                    gpx.endTag(PREFIX_GPX, "wpt");

                    writeWaypoints(gpx, cache);

                    publishProgress(i + 1);
                }

                gpx.endTag(PREFIX_GPX, "gpx");
                gpx.endDocument();
            } catch (final IOException e) {
                Log.e("GpxExport.ExportTask export", e);

                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e1) {
                        // Ignore double error
                    }
                }
                // delete partial gpx file on error
                if (exportFile.exists()) {
                    exportFile.delete();
                }

                return null;
            }

            return exportFile;
        }

        private void writeWaypoints(final XmlSerializer gpx, final Geocache cache) throws IOException {
            List<Waypoint> waypoints = cache.getWaypoints();
            List<Waypoint> ownWaypoints = new ArrayList<Waypoint>(waypoints.size());
            List<Waypoint> originWaypoints = new ArrayList<Waypoint>(waypoints.size());
            for (Waypoint wp : cache.getWaypoints()) {
                if (wp.isUserDefined()) {
                    ownWaypoints.add(wp);
                } else {
                    originWaypoints.add(wp);
                }
            }
            int maxPrefix = 0;
            for (Waypoint wp : originWaypoints) {
                String prefix = wp.getPrefix();
                try {
                    maxPrefix = Math.max(Integer.parseInt(prefix), maxPrefix);
                } catch (NumberFormatException ex) {
                    Log.e("Unexpected origin waypoint prefix='" + prefix + "'", ex);
                }
                writeCacheWaypoint(gpx, wp, prefix);
            }
            for (Waypoint wp : ownWaypoints) {
                maxPrefix++;
                String prefix = StringUtils.leftPad(String.valueOf(maxPrefix), 2, '0');
                writeCacheWaypoint(gpx, wp, prefix);
            }
        }

        /**
         * Writes one waypoint entry for cache waypoint.
         *
         * @param cache
         *            The
         * @param wp
         * @param prefix
         * @throws IOException
         */
        private void writeCacheWaypoint(final XmlSerializer gpx, final Waypoint wp, final String prefix) throws IOException {
            gpx.startTag(PREFIX_GPX, "wpt");
            final Geopoint coords = wp.getCoords();
            gpx.attribute("", "lat", coords != null ? Double.toString(coords.getLatitude()) : ""); // TODO: check whether is the best way to handle unknown waypoint coordinates
            gpx.attribute("", "lon", coords != null ? Double.toString(coords.getLongitude()) : "");
            XmlUtils.multipleTexts(gpx, PREFIX_GPX,
                    "name", prefix + wp.getGeocode().substring(2),
                    "cmt", wp.getNote(),
                    "desc", wp.getName(),
                    "sym", wp.getWaypointType().toString(),                 //TODO: Correct identifier string
                    "type", "Waypoint|" + wp.getWaypointType().toString()); //TODO: Correct identifier string
            gpx.endTag(PREFIX_GPX, "wpt");
        }

        private void writeLogs(final XmlSerializer gpx, final Geocache cache) throws IOException {
            if (cache.getLogs().isEmpty()) {
                return;
            }
            gpx.startTag(PREFIX_GROUNDSPEAK, "logs");

            for (LogEntry log : cache.getLogs()) {
                gpx.startTag(PREFIX_GROUNDSPEAK, "log");
                gpx.attribute("", "id", Integer.toString(log.id));

                XmlUtils.multipleTexts(gpx, PREFIX_GROUNDSPEAK,
                        "date", dateFormatZ.format(new Date(log.date)),
                        "type", log.type.type);

                gpx.startTag(PREFIX_GROUNDSPEAK, "finder");
                gpx.attribute("", "id", log.author);
                gpx.endTag(PREFIX_GROUNDSPEAK, "finder");

                gpx.startTag(PREFIX_GROUNDSPEAK, "text");
                gpx.attribute("", "encoded", "False");
                gpx.text(log.log);
                gpx.endTag(PREFIX_GROUNDSPEAK, "text");

                gpx.endTag(PREFIX_GROUNDSPEAK, "log");
            }

            gpx.endTag(PREFIX_GROUNDSPEAK, "logs");
        }

        private void writeAttributes(final XmlSerializer gpx, final Geocache cache) throws IOException {
            if (cache.getAttributes().isEmpty()) {
                return;
            }
            //TODO: Attribute conversion required: English verbose name, gpx-id
            gpx.startTag(PREFIX_GROUNDSPEAK, "attributes");

            for (String attribute : cache.getAttributes()) {
                final CacheAttribute attr = CacheAttribute.getByRawName(CacheAttribute.trimAttributeName(attribute));
                if (attr == null) {
                    continue;
                }
                final boolean enabled = CacheAttribute.isEnabled(attribute);

                gpx.startTag(PREFIX_GROUNDSPEAK, "attribute");
                gpx.attribute("", "id", Integer.toString(attr.gcid));
                gpx.attribute("", "inc", enabled ? "1" : "0");
                gpx.text(attr.getL10n(enabled));
                gpx.endTag(PREFIX_GROUNDSPEAK, "attribute");
            }

            gpx.endTag(PREFIX_GROUNDSPEAK, "attributes");
        }

        @Override
        protected void onPostExecute(final File exportFile) {
            if (null != activity) {
                progress.dismiss();
                if (exportFile != null) {
                    ActivityMixin.showToast(activity, getName() + ' ' + getString(R.string.export_exportedto) + ": " + exportFile.toString());
                    if (Settings.getShareAfterExport()) {
                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(exportFile));
                        shareIntent.setType("application/xml");
                        activity.startActivity(Intent.createChooser(shareIntent, getString(R.string.export_gpx_to)));
                    }
                } else {
                    ActivityMixin.showToast(activity, getString(R.string.export_failed));
                }
            }
        }

        @Override
        protected void onProgressUpdate(Integer... status) {
            if (null != activity) {
                progress.setProgress(status[0]);
            }
        }
    }
}
