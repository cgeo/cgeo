package cgeo.geocaching.maps;

import cgeo.geocaching.location.IGeoDataProvider;
import cgeo.geocaching.storage.extension.Trackfiles;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.functions.Action2;

import android.app.Activity;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class Tracks {

    private final ArrayList<Track> data = new ArrayList<>();

    private static class Track {
        private final Trackfiles trackfile;
        private IGeoDataProvider route;

        Track(final Trackfiles trackfile) {
            this.trackfile = trackfile;
            this.route = null;
        }
    }

    public interface UpdateTrack {
        void updateRoute(String key, IGeoDataProvider route);
    }

    public Tracks() {
        getListOfTrackfilesInBackground(null);
    }

    public Tracks(final RouteTrackUtils routeTrackUtils, final UpdateTrack updateTrack) {
        getListOfTrackfilesInBackground(() -> {
            for (Track track : data) {
                routeTrackUtils.reloadTrack(track.trackfile, updateTrack);
            }
            routeTrackUtils.setTracks(this);
        });
    }

    private void getListOfTrackfilesInBackground(@Nullable final Runnable runAfterLoad) {
        AndroidRxUtils.computationScheduler.scheduleDirect(() -> {
            for (Trackfiles trackfile : Trackfiles.getTrackfiles()) {
                data.add(new Track(trackfile));
            }
            if (runAfterLoad != null) {
                runAfterLoad.run();
            }
        });
    }

    public void setRoute(@NonNull final String key, final IGeoDataProvider route) {
        for (Track track : data) {
            if (track.trackfile.getKey().equals(key)) {
                track.route = route;
            }
        }
    }

    public void resumeAllTracks(final Action2<String, Boolean> resumeTrack) {
        for (Track track : data) {
            resumeTrack.call(track.trackfile.getKey(), false);
        }
    }

    public void traverse(final Action2<String, IGeoDataProvider> action) {
        for (Track track : data) {
            action.call(track.trackfile.getKey(), track.route);
        }
    }

    public String add(final Activity activity, final Uri uri, final UpdateTrack updateTrack) {
        final String key = Trackfiles.createTrackfile(activity, uri);
        for (Trackfiles trackfile : Trackfiles.getTrackfiles()) {
            if (trackfile.getKey().equals(key)) {
                data.add(new Track(trackfile));
                return key;
            }
        }
        return null;
    }

    public void remove(@NonNull final String key) {
        for (Track track : data) {
            if (track.trackfile.getKey().equals(key)) {
                Trackfiles.removeTrackfile(track.trackfile.getKey());
                data.remove(track);
                break;
            }
        }
    }

    public String getDisplayname(@NonNull final String key) {
        for (Track track : data) {
            if (track.trackfile.getKey().equals(key)) {
                return track.trackfile.getDisplayname();
            }
        }
        return null;
    }

    public void setDisplayname(@NonNull final String key, @NonNull final String newName) {
        for (Track track : data) {
            if (track.trackfile.getKey().equals(key)) {
                track.trackfile.setDisplayname(newName);
            }
        }
    }

    public IGeoDataProvider getRoute(@NonNull final String key) {
        for (Track track : data) {
            if (track.trackfile.getKey().equals(key)) {
                return track.route;
            }
        }
        return null;
    }

    public void hide(@NonNull final String key, final boolean hide) {
        for (Track track : data) {
            if (track.trackfile.getKey().equals(key)) {
                track.trackfile.setHidden(hide);
            }
        }
    }

    @Nullable
    public Trackfiles getTrackfile(@NonNull final String key) {
        for (Track track : data) {
            if (track.trackfile.getKey().equals(key)) {
                return track.trackfile;
            }
        }
        return null;
    }
}
