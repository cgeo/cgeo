package cgeo.geocaching.brouter;

interface IInternalRoutingService {

    //param params--> Map of params:
    //  "maxRunningTime"-->String with a number of seconds for the routing timeout, default = 60
    //  "v"-->[motorcar|bicycle|foot]
    //  "fast"-->[0|1]
    //  "lats"-->double[] array of latitudes; 2 values at least.
    //  "lons"-->double[] array of longitudes; 2 values at least.
    //
    //  "turnInstructionFormat"-->String selecting the format for turn-instructions values: osmand, locus
    //  "trackFormat"-->[kml|gpx] default = gpx
    //  "nogoLats"-->double[] array of nogo latitudes; may be null.
    //  "nogoLons"-->double[] array of nogo longitudes; may be null.
    //  "nogoRadi"-->double[] array of nogo radius in meters; may be null.
    //
    //return null if all ok and no path given, the track if ok and path given, an error message if it was wrong
    //
    //call in a background thread, heavy task!

    // standard BRouter parameters NOT supported by c:geo internal service
    // -------------------------------------------------------------------
    //  "pathToFileResult"-->String with the path to where the result must be saved, including file name and extension
    //                    -->if null, the track is passed via the return argument
    //  "remoteProfile"--> (String), net-content of a profile. If remoteProfile != null, v+fast are ignored
    //  "acceptCompressedFormat"

    String getTrackFromParams(in Bundle params);
}
