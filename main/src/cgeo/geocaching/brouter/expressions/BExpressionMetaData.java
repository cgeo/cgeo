// context for simple expression
// context means:
// - the local variables
// - the local variable names
// - the lookup-input variables

package cgeo.geocaching.brouter.expressions;

import cgeo.geocaching.brouter.BRouterConstants;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;

public final class BExpressionMetaData {
    private static final String CONTEXT_TAG = "---context:";
    private static final String VERSION_TAG = "---lookupversion:";
    private static final String MINOR_VERSION_TAG = "---minorversion:";
    private static final String VARLENGTH_TAG = "---readvarlength";

    public short lookupVersion = -1;
    public short lookupMinorVersion = -1;

    private final HashMap<String, BExpressionContext> listeners = new HashMap<>();

    public void registerListener(final String context, final BExpressionContext ctx) {
        listeners.put(context, ctx);
    }

    public void readMetaData() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(ContentStorage.get().openForRead(PersistableFolder.ROUTING_BASE.getFolder(), BRouterConstants.BROUTER_LOOKUPS_FILENAME)))) {
            BExpressionContext ctx = null;

            for (; ; ) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                line = line.trim();
                if (line.length() == 0 || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith(CONTEXT_TAG)) {
                    ctx = listeners.get(line.substring(CONTEXT_TAG.length()));
                    continue;
                }
                if (line.startsWith(VERSION_TAG)) {
                    lookupVersion = Short.parseShort(line.substring(VERSION_TAG.length()));
                    continue;
                }
                if (line.startsWith(MINOR_VERSION_TAG)) {
                    lookupMinorVersion = Short.parseShort(line.substring(MINOR_VERSION_TAG.length()));
                    continue;
                }
                if (line.startsWith(VARLENGTH_TAG)) {
                    continue; // tag removed...
                }
                if (ctx != null) {
                    ctx.parseMetaLine(line);
                }
            }

            for (BExpressionContext c : listeners.values()) {
                c.finishMetaParsing();
            }

        } catch (Exception e) {
            throw new RuntimeException("file: " + ContentStorage.get().getFileInfo(PersistableFolder.ROUTING_BASE.getFolder(), BRouterConstants.BROUTER_LOOKUPS_FILENAME), e);
        }
    }
}
