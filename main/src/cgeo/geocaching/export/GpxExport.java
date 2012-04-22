package cgeo.geocaching.export;

import cgeo.geocaching.LogEntry;
import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.utils.BaseUtils;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringEscapeUtils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

class GpxExport extends AbstractExport {
    private static final File exportLocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/gpx-export");
    private static final SimpleDateFormat dateFormatZ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    protected GpxExport() {
        super(getString(R.string.export_gpx));
    }

    @Override
    public void export(final List<cgCache> caches, final Activity activity) {
        new ExportTask(caches, activity).execute((Void) null);
    }

    private class ExportTask extends AsyncTask<Void, Integer, Boolean> {
        private final List<cgCache> caches;
        private final Activity activity;
        private final Progress progress = new Progress();
        private File exportFile;

        /**
         * Instantiates and configures the task for exporting field notes.
         *
         * @param caches
         *            The {@link List} of {@link cgCache} to be exported
         * @param activity
         *            optional: Show a progress bar and toasts
         */
        public ExportTask(final List<cgCache> caches, final Activity activity) {
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
        protected Boolean doInBackground(Void... params) {
            // quick check for being able to write the GPX file
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                return false;
            }

            Writer gpx = null;

            try {
                exportLocation.mkdirs();

                final SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                exportFile = new File(exportLocation.toString() + File.separatorChar + fileNameDateFormat.format(new Date()) + ".gpx");

                gpx = new BufferedWriter(new FileWriter(exportFile));

                gpx.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                gpx.write("<gpx version=\"1.0\" creator=\"c:geo - http://www.cgeo.org\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.topografix.com/GPX/1/0\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd http://www.groundspeak.com/cache/1/0/1 http://www.groundspeak.com/cache/1/0/1/cache.xsd\">");


                for (int i = 0; i < caches.size(); i++) {
                    cgCache cache = caches.get(i);

                    if (!cache.isDetailed()) {
                        cache = cgeoapplication.getInstance().loadCache(caches.get(i).getGeocode(), LoadFlags.LOAD_ALL_DB_ONLY);
                    }

                    gpx.write("<wpt ");
                    gpx.write("lat=\"");
                    gpx.write(Double.toString(cache.getCoords().getLatitude()));
                    gpx.write("\" ");
                    gpx.write("lon=\"");
                    gpx.write(Double.toString(cache.getCoords().getLongitude()));
                    gpx.write("\">");

                    gpx.write("<time>");
                    gpx.write(StringEscapeUtils.escapeXml(dateFormatZ.format(cache.getHiddenDate())));
                    gpx.write("</time>");

                    gpx.write("<name>");
                    gpx.write(StringEscapeUtils.escapeXml(cache.getGeocode()));
                    gpx.write("</name>");

                    gpx.write("<desc>");
                    gpx.write(StringEscapeUtils.escapeXml(cache.getName()));
                    gpx.write("</desc>");

                    gpx.write("<sym>");
                    gpx.write(cache.isFound() ? "Geocache Found" : "Geocache");
                    gpx.write("</sym>");

                    gpx.write("<type>");
                    gpx.write(StringEscapeUtils.escapeXml("Geocache|" + cache.getType().toString())); //TODO: Correct (english) string
                    gpx.write("</type>");

                    gpx.write("<groundspeak:cache ");
                    gpx.write("available=\"");
                    gpx.write(!cache.isDisabled() ? "True" : "False");
                    gpx.write("\" archived=\"");
                    gpx.write(cache.isArchived() ? "True" : "False");
                    gpx.write("\" ");
                    gpx.write("xmlns:groundspeak=\"http://www.groundspeak.com/cache/1/0/1\">");

                    gpx.write("<groundspeak:name>");
                    gpx.write(StringEscapeUtils.escapeXml(cache.getName()));
                    gpx.write("</groundspeak:name>");

                    gpx.write("<groundspeak:placed_by>");
                    gpx.write(StringEscapeUtils.escapeXml(cache.getOwner()));
                    gpx.write("</groundspeak:placed_by>");

                    gpx.write("<groundspeak:owner>");
                    gpx.write(StringEscapeUtils.escapeXml(cache.getOwnerReal()));
                    gpx.write("</groundspeak:owner>");

                    gpx.write("<groundspeak:type>");
                    gpx.write(StringEscapeUtils.escapeXml(cache.getType().toString())); //TODO: Correct (english) string
                    gpx.write("</groundspeak:type>");

                    gpx.write("<groundspeak:container>");
                    gpx.write(StringEscapeUtils.escapeXml(cache.getSize().toString())); //TODO: Correct (english) string
                    gpx.write("</groundspeak:container>");

                    if (cache.hasAttributes()) {
                        //TODO: Attribute conversion required: English verbose name, gpx-id
                        gpx.write("<groundspeak:attributes>");

                        for (String attribute : cache.getAttributes()) {
                            final CacheAttribute attr = CacheAttribute.getByGcRawName(CacheAttribute.trimAttributeName(attribute));
                            final boolean enabled = CacheAttribute.isEnabled(attribute);

                            gpx.write("<groundspeak:attribute id=\"");
                            gpx.write(attr.id);
                            gpx.write("\" inc=\"");
                            if (enabled) {
                                gpx.write('1');
                            } else {
                                gpx.write('0');
                            }
                            gpx.write("\">");
                            gpx.write(StringEscapeUtils.escapeXml(attr.getL10n(enabled)));
                            gpx.write("</groundspeak:attribute>");
                        }

                        gpx.write("</groundspeak:attributes>");
                    }

                    gpx.write("<groundspeak:difficulty>");
                    gpx.write(Float.toString(cache.getDifficulty()));
                    gpx.write("</groundspeak:difficulty>");

                    gpx.write("<groundspeak:terrain>");
                    gpx.write(Float.toString(cache.getTerrain()));
                    gpx.write("</groundspeak:terrain>");

                    gpx.write("<groundspeak:country>");
                    gpx.write(StringEscapeUtils.escapeXml(cache.getLocation()));
                    gpx.write("</groundspeak:country>");

                    gpx.write("<groundspeak:state>");
                    gpx.write(StringEscapeUtils.escapeXml(cache.getLocation()));
                    gpx.write("</groundspeak:state>");

                    gpx.write("<groundspeak:short_description html=\"");
                    if (BaseUtils.containsHtml(cache.getShortDescription())) {
                        gpx.write("True");
                    } else {
                        gpx.write("False");
                    }
                    gpx.write("\">");
                    gpx.write(StringEscapeUtils.escapeXml(cache.getShortDescription()));
                    gpx.write("</groundspeak:short_description>");

                    gpx.write("<groundspeak:long_description html=\"");
                    if (BaseUtils.containsHtml(cache.getDescription())) {
                        gpx.write("True");
                    } else {
                        gpx.write("False");
                    }
                    gpx.write("\">");
                    gpx.write(StringEscapeUtils.escapeXml(cache.getDescription()));
                    gpx.write("</groundspeak:long_description>");

                    gpx.write("<groundspeak:encoded_hints>");
                    gpx.write(StringEscapeUtils.escapeXml(cache.getHint()));
                    gpx.write("</groundspeak:encoded_hints>");

                    gpx.write("</groundspeak:cache>");

                    if (cache.getLogs().size() > 0) {
                        gpx.write("<groundspeak:logs>");

                        for (LogEntry log : cache.getLogs()) {
                            gpx.write("<groundspeak:log id=\"");
                            gpx.write(log.id);
                            gpx.write("\">");

                            gpx.write("<groundspeak:date>");
                            gpx.write(StringEscapeUtils.escapeXml(dateFormatZ.format(new Date(log.date))));
                            gpx.write("</groundspeak:date>");

                            gpx.write("<groundspeak:type>");
                            gpx.write(StringEscapeUtils.escapeXml(log.type.type));
                            gpx.write("</groundspeak:type>");

                            gpx.write("<groundspeak:finder id=\"\">");
                            gpx.write(StringEscapeUtils.escapeXml(log.author));
                            gpx.write("</groundspeak:finder>");

                            gpx.write("<groundspeak:text encoded=\"False\">");
                            gpx.write(StringEscapeUtils.escapeXml(log.log));
                            gpx.write("</groundspeak:text>");

                            gpx.write("</groundspeak:log>");
                        }

                        gpx.write("</groundspeak:logs>");
                    }

                    gpx.write("</wpt>");

                    if (cache.hasWaypoints()) {
                        for (cgWaypoint wp : cache.getWaypoints()) {
                            gpx.write("<wpt lat=\"");
                            gpx.write(Double.toString(wp.getCoords().getLatitude()));
                            gpx.write("\" lon=\"");
                            gpx.write(Double.toString(wp.getCoords().getLongitude()));
                            gpx.write("\">");

                            gpx.write("<name>");
                            gpx.write(StringEscapeUtils.escapeXml(wp.getPrefix()));
                            gpx.write(StringEscapeUtils.escapeXml(cache.getGeocode().substring(2)));
                            gpx.write("</name>");

                            gpx.write("<cmt />");

                            gpx.write("<desc>");
                            gpx.write(StringEscapeUtils.escapeXml(wp.getNote()));
                            gpx.write("</desc>");

                            gpx.write("<sym>");
                            gpx.write(StringEscapeUtils.escapeXml(wp.getWaypointType().toString()));
                            gpx.write("</sym>");

                            gpx.write("<type>Waypoint|");
                            gpx.write(StringEscapeUtils.escapeXml(wp.getWaypointType().toString()));
                            gpx.write("</type>");

                            gpx.write("</wpt>");
                        }
                    }

                    publishProgress(i + 1);
                }

                gpx.write("</gpx>");
            } catch (Exception e) {
                Log.e("GpxExport.ExportTask export", e);

                // delete partial gpx file on error
                if (exportFile.exists()) {
                    exportFile.delete();
                }

                return false;
            } finally {
                if (gpx != null) {
                    try {
                        gpx.close();
                    } catch (IOException e) {
                        Log.e("GpxExport.ExportTask export", e);
                        return false;
                    }
                }
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (null != activity) {
                progress.dismiss();
                if (result) {
                    ActivityMixin.showToast(activity, getName() + ' ' + getString(R.string.export_exportedto) + ": " + exportFile.toString());
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
