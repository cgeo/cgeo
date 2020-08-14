package cgeo.geocaching.utils;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.SelectTrackFileActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.files.GPXTrackImporter;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.widget.Toast;
import static android.app.Activity.RESULT_OK;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

public class TrackUtils {

    private static final int REQUEST_CODE_GET_TRACKFILE = 47121;

    public static class Track {
        private String trackName;
        private final ArrayList<Geopoint> track;

        public Track() {
            trackName = "";
            track = new ArrayList<>();
        }

        public String getTrackName() {
            return trackName;
        }

        public void setTrackName(final String trackName) {
            this.trackName = trackName;
        }

        public ArrayList<Geopoint> getTrack() {
            return track;
        }

        public int getSize() {
            return track.size();
        }

        public void add(final Geopoint geopoint) {
            track.add(geopoint);
        }
    }

    public static class Tracks {
        private final ArrayList<Track> tracks;

        public Tracks() {
            tracks = new ArrayList<>();
        }

        public void add(final Track track) {
            tracks.add(track);
        }

        public int getSize() {
            return tracks.size();
        }

        public Track get(final int i) {
            return tracks.get(i);
        }
    }

    public interface TrackUpdaterSingle {
        void updateTrack(Track track);
    }

    public interface TrackUpdaterMulti {
        void updateTracks(Tracks tracks);
    }

    protected TrackUtils() {
    }

    /**
     * Enable/disable track related menu entries
     * @param menu menu to be configured
     */
    public static void onPrepareOptionsMenu(final Menu menu) {
        final boolean trackfileSet = StringUtils.isNotBlank(Settings.getTrackFile());
        menu.findItem(R.id.menu_hide_track).setVisible(trackfileSet);
        menu.findItem(R.id.menu_hide_track).setChecked(Settings.isHideTrack());
        menu.findItem(R.id.menu_center_on_track).setVisible(trackfileSet);
        menu.findItem(R.id.menu_unload_track).setVisible(trackfileSet);
    }

    /**
     * Check if selected menu entry is for "load track" or hide/unhide track
     * @param activity calling activity
     * @param id menu entry id
     * @return true, if selected menu entry is track related and consumed / false else
     */
    public static boolean onOptionsItemSelected(final Activity activity, final int id, final Tracks tracks, final Runnable hideOptionsChanged, final TrackUpdaterMulti updateTracks, final MapUtils.CenterOnPosition centerOnPosition) {
        switch (id) {
            case R.id.menu_load_track:
                if (null == tracks || tracks.getSize() == 0) {
                    startIndividualTrackFileSelector(activity);
                } else {
                    Dialogs.confirm(activity, R.string.map_load_track, R.string.map_load_track_confirm, (dialog, which) -> startIndividualTrackFileSelector(activity));
                }
                return true;
            case R.id.menu_unload_track:
                Settings.setTrackFile(null);
                updateTracks.updateTracks(null);
                return true;
            case R.id.menu_hide_track:
                Settings.setHideTrack(!Settings.isHideTrack());
                hideOptionsChanged.run();
                return true;
            case R.id.menu_center_on_track:
                if (null != tracks && tracks.getSize() > 0) {
                    final Track track = tracks.get(0);
                    if (track.getSize() > 0) {
                        // calculate center and boundaries of trackcmd
                        final Geopoint first = track.track.get(0);
                        double minLat = first.getLatitude();
                        double maxLat = first.getLatitude();
                        double minLon = first.getLongitude();
                        double maxLon = first.getLongitude();

                        double latitude = 0.0d;
                        double longitude = 0.0d;
                        for (Geopoint point : track.track) {
                            final double lat = point.getLatitude();
                            final double lon = point.getLongitude();

                            latitude += point.getLatitude();
                            longitude += point.getLongitude();

                            minLat = Math.min(minLat, lat);
                            maxLat = Math.max(maxLat, lat);
                            minLon = Math.min(minLon, lon);
                            maxLon = Math.max(maxLon, lon);
                        }
                        centerOnPosition.centerOnPosition(latitude / track.getSize(), longitude / track.getSize(), new Viewport(new Geopoint(minLat, minLon), new Geopoint(maxLat, maxLon)));
                    }
                }
                return true;
            default:
                return false;
        }
    }

    private static void startIndividualTrackFileSelector(final Activity activity) {
        final Intent intent = new Intent(activity, SelectTrackFileActivity.class);
        activity.startActivityForResult(intent, REQUEST_CODE_GET_TRACKFILE);
    }

    /**
     * Check if current activity result is a selected track file
     * @param requestCode current requestCode to check
     * @param resultCode current resultCode to check
     * @param data additional intent data delivered
     * @return true, if successfully selected track file / false else
     */
    public static boolean onActivityResult(final Activity activity, final int requestCode, final int resultCode, final Intent data, @Nullable final TrackUpdaterMulti updateTracks) {
        if (requestCode == REQUEST_CODE_GET_TRACKFILE && resultCode == RESULT_OK && data.hasExtra(Intents.EXTRA_GPX_FILE)) {
            final String filename = data.getStringExtra(Intents.EXTRA_GPX_FILE);
            if (null != filename) {
                final File file = new File(filename);
                if (!file.isDirectory()) {
                    Settings.setTrackFile(filename);
                    if (null != updateTracks) {
                        loadTracks(activity, updateTracks);
                    }
                }
            }
            return true;
        }
        return false;
    }

    public static void loadTracks(final Activity activity, final TrackUpdaterMulti updateTracks) {
        final String trackfile = Settings.getTrackFile();
        if (null != trackfile) {
            GPXTrackImporter.doImport(activity, new File(trackfile), updateTracks);
        }
        ActivityMixin.invalidateOptionsMenu(activity);
    }

    public static void showTrackInfo(final Activity activity, final Tracks tracks) {
        if (null != tracks && tracks.getSize() > 0) {
            final Track track = tracks.get(0);
            if (null != track) {
                Toast.makeText(activity, activity.getResources().getQuantityString(R.plurals.load_track_success, track.getSize(), track.getSize()), Toast.LENGTH_SHORT).show();
            }
        }

    }
}
