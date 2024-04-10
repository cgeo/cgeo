package cgeo.geocaching.brouter.core;

import java.io.BufferedWriter;
import java.io.StringWriter;

public class FormatCsv extends Formatter {

    public FormatCsv(RoutingContext rc) {
        super(rc);
    }

    @Override
    public String format(OsmTrack t) {
        try {
            final StringWriter sw = new StringWriter();
            final BufferedWriter bw = new BufferedWriter(sw);
            writeMessages(bw, t);
            return sw.toString();
        } catch (Exception ex) {
            return "Error: " + ex.getMessage();
        }
    }

    public void writeMessages(BufferedWriter bw, OsmTrack t) throws Exception {
        dumpLine(bw, MESSAGES_HEADER);
        for (String m : t.aggregateMessages()) {
            dumpLine(bw, m);
        }
        if (bw != null) {
            bw.close();
        }
    }

    private void dumpLine(BufferedWriter bw, String s) throws Exception {
        if (bw == null) {
            System.out.println(s);
        } else {
            bw.write(s);
            bw.write("\n");
        }
    }

}
