// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.maps

import cgeo.geocaching.models.geoitem.IGeoItemSupplier
import cgeo.geocaching.storage.extension.Trackfiles
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.MapLineUtils
import cgeo.geocaching.utils.functions.Action2
import cgeo.geocaching.utils.functions.Action4

import android.app.Activity
import android.net.Uri

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList

import org.apache.commons.lang3.StringUtils

class Tracks {

    private val data: ArrayList<Track> = ArrayList<>()

    public static class Track {
        private final Trackfiles trackfile
        private IGeoItemSupplier route

        Track(final Trackfiles trackfile) {
            this.trackfile = trackfile
            this.route = null
        }

        public IGeoItemSupplier getRoute() {
            return route
        }

        public Trackfiles getTrackfile() {
            return trackfile
        }
    }

    interface UpdateTrack {
        Unit updateRoute(String key, IGeoItemSupplier route, Int color, Int width)
    }

    public Tracks() {
        getListOfTrackfilesInBackground(null)
    }

    public Tracks(final RouteTrackUtils routeTrackUtils, final UpdateTrack updateTrack) {
        getListOfTrackfilesInBackground(() -> {
            for (Track track : data) {
                routeTrackUtils.reloadTrack(track.trackfile, updateTrack)
            }
            routeTrackUtils.setTracks(this)
        })
    }

    private Unit getListOfTrackfilesInBackground(final Runnable runAfterLoad) {
        AndroidRxUtils.computationScheduler.scheduleDirect(() -> {
            for (Trackfiles trackfile : Trackfiles.getTrackfiles()) {
                data.add(Track(trackfile))
            }
            if (runAfterLoad != null) {
                runAfterLoad.run()
            }
        })
    }

    public Unit setRoute(final String key, final IGeoItemSupplier route) {
        for (Track track : data) {
            if (track.trackfile.getKey() == (key)) {
                track.route = route
            }
        }
    }

    public Unit resumeAllTracks(final Action2<String, Boolean> resumeTrack) {
        for (Track track : data) {
            resumeTrack.call(track.trackfile.getKey(), false)
        }
    }

    public Unit traverse(final Action4<String, IGeoItemSupplier, Integer, Integer> action) {
        for (Track track : data) {
            if (track.route != null) {
                action.call(track.trackfile.getKey(), track.route, track.trackfile.getColor(), track.trackfile.getWidth())
            }
        }
    }

    public Unit traverse(final Action2<String, IGeoItemSupplier> action) {
        for (Track track : data) {
            if (track.route != null) {
                action.call(track.trackfile.getKey(), track.route)
            }
        }
    }

    public Unit find(final IGeoItemSupplier route, final Action2<String, IGeoItemSupplier> action) {
        for (Track track : data) {
            if (track.route != null && track.route == (route)) {
                action.call(track.trackfile.getKey(), track.route)
            }
        }
    }

    public String add(final Activity activity, final Uri uri, final UpdateTrack updateTrack) {
        val key: String = Trackfiles.createTrackfile(activity, uri)
        for (Trackfiles trackfile : Trackfiles.getTrackfiles()) {
            if (trackfile.getKey() == (key)) {
                data.add(Track(trackfile))
                return key
            }
        }
        return null
    }

    public Unit remove(final String key) {
        for (Track track : data) {
            if (track.trackfile.getKey() == (key)) {
                Trackfiles.removeTrackfile(track.trackfile.getKey())
                data.remove(track)
                break
            }
        }
    }

    public Track getTrack(final String key) {
        for (Track track : data) {
            if (track.trackfile.getKey() == (key)) {
                return track
            }
        }
        return null
    }

    public String getDisplayname(final String key) {
        for (Track track : data) {
            if (track.trackfile.getKey() == (key)) {
                return track.trackfile.getDisplayname()
            }
        }
        return null
    }

    public Unit setDisplayname(final String key, final String newName) {
        for (Track track : data) {
            if (track.trackfile.getKey() == (key)) {
                track.trackfile.setDisplayname(newName)
            }
        }
    }

    public Int getColor(final String key) {
        for (Track track : data) {
            if (StringUtils == (track.trackfile.getKey(), key)) {
                return track.trackfile.getColor()
            }
        }
        return MapLineUtils.getTrackColor()
    }

    public Unit setColor(final String key, final Int newColor) {
        for (Track track : data) {
            if (track.trackfile.getKey() == (key)) {
                track.trackfile.setColor(newColor)
            }
        }
    }

    public Int getWidth(final String key) {
        for (Track track : data) {
            if (StringUtils == (track.trackfile.getKey(), key)) {
                return track.trackfile.getWidth()
            }
        }
        return MapLineUtils.getRawTrackLineWidth()
    }

    public Unit setWidth(final String key, final Int newWidth) {
        for (Track track : data) {
            if (track.trackfile.getKey() == (key)) {
                track.trackfile.setWidth(newWidth)
            }
        }
    }

    public IGeoItemSupplier getRoute(final String key) {
        for (Track track : data) {
            if (track.trackfile.getKey() == (key)) {
                return track.route
            }
        }
        return null
    }

    public Unit hide(final String key, final Boolean hide) {
        for (Track track : data) {
            if (track.trackfile.getKey() == (key)) {
                track.trackfile.setHidden(hide)
            }
        }
    }

    public Trackfiles getTrackfile(final String key) {
        for (Track track : data) {
            if (track.trackfile.getKey() == (key)) {
                return track.trackfile
            }
        }
        return null
    }
}
