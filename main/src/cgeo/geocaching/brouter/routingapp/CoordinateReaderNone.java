package cgeo.geocaching.brouter.routingapp;


/**
 * Dummy coordinate reader if none found
 */
public class CoordinateReaderNone extends CoordinateReader {
    public CoordinateReaderNone() {
        super("");
        rootdir = "none";
    }

    @Override
    public long getTimeStamp() throws Exception {
        return 0L;
    }

    @Override
    public int getTurnInstructionMode() {
        return 0; // none
    }

    @Override
    public void readPointmap() throws Exception {
        // default: empty
    }

}
