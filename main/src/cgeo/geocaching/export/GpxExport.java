package cgeo.geocaching.export;

import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgLog;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class GpxExport extends AbstractExport {
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

            // FIXME: complete export is created in memory. That should be some file stream instead.
            final StringBuilder gpx = new StringBuilder();

            gpx.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            gpx.append("<gpx version=\"1.0\" creator=\"c:geo - http://www.cgeo.org\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.topografix.com/GPX/1/0\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd http://www.groundspeak.com/cache/1/0/1 http://www.groundspeak.com/cache/1/0/1/cache.xsd\">");

            try {
                for (int i = 0; i < caches.size(); i++) {
                    cgCache cache = caches.get(i);

                    if (!cache.isDetailed()) {
                        cache = cgeoapplication.getInstance().loadCache(caches.get(i).getGeocode(), LoadFlags.LOAD_ALL_DB_ONLY);
                    }

                    gpx.append("<wpt ");
                    gpx.append("lat=\"" + cache.getCoords().getLatitude() + "\" ");
                    gpx.append("lon=\"" + cache.getCoords().getLongitude() + "\">");

                    gpx.append("<time>");
                    gpx.append(StringEscapeUtils.escapeXml(dateFormatZ.format(cache.getHiddenDate())));
                    gpx.append("</time>");

                    gpx.append("<name>");
                    gpx.append(StringEscapeUtils.escapeXml(cache.getGeocode()));
                    gpx.append("</name>");

                    gpx.append("<desc>");
                    gpx.append(StringEscapeUtils.escapeXml(cache.getName()));
                    gpx.append("</desc>");

                    gpx.append("<sym>");
                    gpx.append(cache.isFound() ? "Geocache Found" : "Geocache");
                    gpx.append("</sym>");

                    gpx.append("<type>");
                    gpx.append(StringEscapeUtils.escapeXml("Geocache|" + cache.getType().toString())); //TODO: Correct (english) string
                    gpx.append("</type>");

                    gpx.append("<groundspeak:cache ");
                    gpx.append("available=\"" + (!cache.isDisabled() ? "True" : "False"));
                    gpx.append("\" archived=\"" + (cache.isArchived() ? "True" : "False") + "\" ");
                    gpx.append("xmlns:groundspeak=\"http://www.groundspeak.com/cache/1/0/1\">");

                    gpx.append("<groundspeak:name>");
                    gpx.append(StringEscapeUtils.escapeXml(cache.getName()));
                    gpx.append("</groundspeak:name>");

                    gpx.append("<groundspeak:placed_by>");
                    gpx.append(StringEscapeUtils.escapeXml(cache.getOwner()));
                    gpx.append("</groundspeak:placed_by>");

                    gpx.append("<groundspeak:owner>");
                    gpx.append(StringEscapeUtils.escapeXml(cache.getOwnerReal()));
                    gpx.append("</groundspeak:owner>");

                    gpx.append("<groundspeak:type>");
                    gpx.append(StringEscapeUtils.escapeXml(cache.getType().toString())); //TODO: Correct (english) string
                    gpx.append("</groundspeak:type>");

                    gpx.append("<groundspeak:container>");
                    gpx.append(StringEscapeUtils.escapeXml(cache.getSize().toString())); //TODO: Correct (english) string
                    gpx.append("</groundspeak:container>");

                    if (cache.hasAttributes()) {
                        //TODO: Attribute conversion required: English verbose name, gpx-id
                        gpx.append("<groundspeak:attributes>");

                        for (String attribute : cache.getAttributes()) {
                            final CacheAttribute attr = CacheAttribute.getByGcRawName(CacheAttribute.trimAttributeName(attribute));
                            final boolean enabled = attribute.endsWith(CacheAttribute.INTERNAL_YES);

                            gpx.append("<groundspeak:attribute id=\"");
                            gpx.append(attr.id);
                            gpx.append("\" inc=\"");
                            if (enabled) {
                                gpx.append('1');
                            } else {
                                gpx.append('0');
                            }
                            gpx.append("\">");
                            gpx.append(StringEscapeUtils.escapeXml(attr.getL10n(enabled)));
                            gpx.append("</groundspeak:attribute>");
                        }

                        gpx.append("</groundspeak:attributes>");
                    }

                    gpx.append("<groundspeak:difficulty>");
                    gpx.append(Float.toString(cache.getDifficulty()));
                    gpx.append("</groundspeak:difficulty>");

                    gpx.append("<groundspeak:terrain>");
                    gpx.append(Float.toString(cache.getTerrain()));
                    gpx.append("</groundspeak:terrain>");

                    gpx.append("<groundspeak:country>");
                    gpx.append(StringEscapeUtils.escapeXml(cache.getLocation()));
                    gpx.append("</groundspeak:country>");

                    gpx.append("<groundspeak:state>");
                    gpx.append(StringEscapeUtils.escapeXml(cache.getLocation()));
                    gpx.append("</groundspeak:state>");

                    gpx.append("<groundspeak:short_description html=\"");
                    if (BaseUtils.containsHtml(cache.getShortDescription())) {
                        gpx.append("True");
                    } else {
                        gpx.append("False");
                    }
                    gpx.append("\">");
                    gpx.append(StringEscapeUtils.escapeXml(cache.getShortDescription()));
                    gpx.append("</groundspeak:short_description>");

                    gpx.append("<groundspeak:long_description html=\"");
                    if (BaseUtils.containsHtml(cache.getDescription())) {
                        gpx.append("True");
                    } else {
                        gpx.append("False");
                    }
                    gpx.append("\">");
                    gpx.append(StringEscapeUtils.escapeXml(cache.getDescription()));
                    gpx.append("</groundspeak:long_description>");

                    gpx.append("<groundspeak:encoded_hints>");
                    gpx.append(StringEscapeUtils.escapeXml(cache.getHint()));
                    gpx.append("</groundspeak:encoded_hints>");

                    gpx.append("</groundspeak:cache>");

                    //TODO: Waypoints

                    if (cache.getLogs().size() > 0) {
                        gpx.append("<groundspeak:logs>");

                        for (cgLog log : cache.getLogs()) {
                            gpx.append("<groundspeak:log id=\"");
                            gpx.append(log.id);
                            gpx.append("\">");

                            gpx.append("<groundspeak:date>");
                            gpx.append(StringEscapeUtils.escapeXml(dateFormatZ.format(new Date(log.date))));
                            gpx.append("</groundspeak:date>");

                            gpx.append("<groundspeak:type>");
                            gpx.append(StringEscapeUtils.escapeXml(log.type.type));
                            gpx.append("</groundspeak:type>");

                            gpx.append("<groundspeak:finder id=\"\">");
                            gpx.append(StringEscapeUtils.escapeXml(log.author));
                            gpx.append("</groundspeak:finder>");

                            gpx.append("<groundspeak:text encoded=\"False\">");
                            gpx.append(StringEscapeUtils.escapeXml(log.log));
                            gpx.append("</groundspeak:text>");

                            gpx.append("</groundspeak:log>");
                        }

                        gpx.append("</groundspeak:logs>");
                    }

                    gpx.append("</wpt>");

                    publishProgress(i + 1);
                }
            } catch (Exception e) {
                Log.e(Settings.tag, "GpxExport.ExportTask generation", e);
                return false;
            }

            gpx.append("</gpx>");

            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                exportLocation.mkdirs();

                SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                exportFile = new File(exportLocation.toString() + '/' + fileNameDateFormat.format(new Date()) + ".gpx");

                OutputStream os = null;
                Writer fw = null;
                try {
                    os = new FileOutputStream(exportFile);
                    fw = new OutputStreamWriter(os, "UTF-8");
                    fw.write(gpx.toString());
                } catch (IOException e) {
                    Log.e(Settings.tag, "GpxExport.ExportTask export", e);
                    return false;
                } finally {
                    if (fw != null) {
                        try {
                            fw.close();
                        } catch (IOException e) {
                            Log.e(Settings.tag, "GpxExport.ExportTask export", e);
                            return false;
                        }
                    }
                }
            } else {
                return false;
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
