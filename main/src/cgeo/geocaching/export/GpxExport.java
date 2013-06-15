package cgeo.geocaching.export;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogEntry;
import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.Waypoint;
import cgeo.geocaching.cgData;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.utils.AsyncTaskWithProgress;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.XmlUtils;
import cgeo.org.kxml2.io.KXmlSerializer;

import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlSerializer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

class GpxExport extends AbstractExport {
    private static final SimpleDateFormat dateFormatZ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
    public static final String PREFIX_XSI = "http://www.w3.org/2001/XMLSchema-instance";
    public static final String PREFIX_GPX = "http://www.topografix.com/GPX/1/0";
    public static final String PREFIX_GROUNDSPEAK = "http://www.groundspeak.com/cache/1/0";

    /**
     * During the export, only this number of geocaches is fully loaded into memory.
     */
    public static final int CACHES_PER_BATCH = 100;

    protected GpxExport() {
        super(getString(R.string.export_gpx));
    }

    @Override
    public void export(final List<Geocache> caches, final Activity activity) {
        String[] geocodes = getGeocodes(caches);
        if (null == activity) {
            // No activity given, so no user interaction possible.
            // Start export with default parameters.
            new ExportTask(null).execute(geocodes);

        } else {
            // Show configuration dialog
            getExportDialog(geocodes, activity).show();
        }
    }

    private Dialog getExportDialog(final String[] geocodes, final Activity activity) {
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
                new ExportTask(activity).execute(geocodes);
            }
        });

        return builder.create();
    }

    private static String[] getGeocodes(final List<Geocache> caches) {
        ArrayList<String> allGeocodes = new ArrayList<String>(caches.size());
        for (final Geocache geocache : caches) {
            allGeocodes.add(geocache.getGeocode());
        }
        return allGeocodes.toArray(new String[allGeocodes.size()]);
    }

    protected class ExportTask extends AsyncTaskWithProgress<String, File> {
        private final Activity activity;
        private int countExported = 0;

        /**
         * Instantiates and configures the task for exporting field notes.
         *
         * @param activity
         *            optional: Show a progress bar and toasts
         */
        public ExportTask(final Activity activity) {
            super(activity, getProgressTitle());
            this.activity = activity;
        }

        @Override
        protected File doInBackgroundInternal(String[] geocodes) {
            // quick check for being able to write the GPX file
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                return null;
            }

            List<String> allGeocodes = new ArrayList<String>(Arrays.asList(geocodes));

            setMessage(cgeoapplication.getInstance().getResources().getQuantityString(R.plurals.cache_counts, allGeocodes.size(), allGeocodes.size()));

            final SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
            final File exportFile = new File(Settings.getGpxExportDir() + File.separatorChar + "export_" + fileNameDateFormat.format(new Date()) + ".gpx");
            BufferedWriter writer = null;
            try {
                final File exportLocation = new File(Settings.getGpxExportDir());
                exportLocation.mkdirs();

                final XmlSerializer gpx = new KXmlSerializer();
                writer = new BufferedWriter(new FileWriter(exportFile));
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

                // Split the overall set of geocodes into small chunks. That is a compromise between memory efficiency (because
                // we don't load all caches fully into memory) and speed (because we don't query each cache separately).
                while (!allGeocodes.isEmpty()) {
                    final List<String> batch = allGeocodes.subList(0, Math.min(CACHES_PER_BATCH, allGeocodes.size()));
                    exportBatch(gpx, batch);
                    batch.clear();
                }

                gpx.endTag(PREFIX_GPX, "gpx");
                gpx.endDocument();
            } catch (final Exception e) {
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

        private void exportBatch(final XmlSerializer gpx, Collection<String> geocodesOfBatch) throws IOException {
            Set<Geocache> caches = cgData.loadCaches(geocodesOfBatch, LoadFlags.LOAD_ALL_DB_ONLY);
            for (Geocache cache : caches) {
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
                gpx.attribute("", "html", TextUtils.containsHtml(cache.getShortDescription()) ? "True" : "False");
                gpx.text(cache.getShortDescription());
                gpx.endTag(PREFIX_GROUNDSPEAK, "short_description");

                gpx.startTag(PREFIX_GROUNDSPEAK, "long_description");
                gpx.attribute("", "html", TextUtils.containsHtml(cache.getDescription()) ? "True" : "False");
                gpx.text(cache.getDescription());
                gpx.endTag(PREFIX_GROUNDSPEAK, "long_description");

                writeLogs(gpx, cache);

                gpx.endTag(PREFIX_GROUNDSPEAK, "cache");
                gpx.endTag(PREFIX_GPX, "wpt");

                writeWaypoints(gpx, cache);

                countExported++;
                publishProgress(countExported);
            }
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
                    final int numericPrefix = Integer.parseInt(prefix);
                    maxPrefix = Math.max(numericPrefix, maxPrefix);
                } catch (NumberFormatException ex) {
                    // ignore non numeric prefix, as it should be unique in the list of non-own waypoints already
                }
                writeCacheWaypoint(gpx, wp, prefix);
            }
            // Prefixes must be unique. There use numeric strings as prefixes in OWN waypoints
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
            final Geopoint coords = wp.getCoords();
            // TODO: create some extension to GPX to include waypoint without coords
            if (coords != null) {
                gpx.startTag(PREFIX_GPX, "wpt");
                gpx.attribute("", "lat", Double.toString(coords.getLatitude()));
                gpx.attribute("", "lon", Double.toString(coords.getLongitude()));
                XmlUtils.multipleTexts(gpx, PREFIX_GPX,
                        "name", prefix + wp.getGeocode().substring(2),
                        "cmt", wp.getNote(),
                        "desc", wp.getName(),
                        "sym", wp.getWaypointType().toString(), //TODO: Correct identifier string
                        "type", "Waypoint|" + wp.getWaypointType().toString()); //TODO: Correct identifier string
                gpx.endTag(PREFIX_GPX, "wpt");
            }
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
                gpx.attribute("", "id", "");
                gpx.text(log.author);
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
        protected void onPostExecuteInternal(final File exportFile) {
            if (null != activity) {
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

    }
}
