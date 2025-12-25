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

// context for simple expression
// context means:
// - the local variables
// - the local variable names
// - the lookup-input variables

package cgeo.geocaching.brouter.expressions

import cgeo.geocaching.brouter.BRouterConstants
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.PersistableFolder

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.HashMap
import java.util.Map

class BExpressionMetaData {
    private static val CONTEXT_TAG: String = "---context:"
    private static val VERSION_TAG: String = "---lookupversion:"
    private static val MINOR_VERSION_TAG: String = "---minorversion:"
    private static val VARLENGTH_TAG: String = "---readvarlength"
    private static val MIN_APP_VERSION_TAG: String = "---minappversion:"

    var lookupVersion: Short = -1
    var lookupMinorVersion: Short = -1
    var minAppVersion: Short = -1

    private val listeners: Map<String, BExpressionContext> = HashMap<>()

    public Unit registerListener(final String context, final BExpressionContext ctx) {
        listeners.put(context, ctx)
    }

    public Unit readMetaData() {
        try (BufferedReader br = BufferedReader(InputStreamReader(ContentStorage.get().openForRead(PersistableFolder.ROUTING_BASE.getFolder(), BRouterConstants.BROUTER_LOOKUPS_FILENAME)))) {
            BExpressionContext ctx = null

            for (; ; ) {
                String line = br.readLine()
                if (line == null) {
                    break
                }
                line = line.trim()
                if (line.isEmpty() || line.startsWith("#")) {
                    continue
                }
                if (line.startsWith(CONTEXT_TAG)) {
                    ctx = listeners.get(line.substring(CONTEXT_TAG.length()))
                    continue
                }
                if (line.startsWith(VERSION_TAG)) {
                    lookupVersion = Short.parseShort(line.substring(VERSION_TAG.length()))
                    continue
                }
                if (line.startsWith(MINOR_VERSION_TAG)) {
                    lookupMinorVersion = Short.parseShort(line.substring(MINOR_VERSION_TAG.length()))
                    continue
                }
                if (line.startsWith(MIN_APP_VERSION_TAG)) {
                    minAppVersion = Short.parseShort(line.substring(MIN_APP_VERSION_TAG.length()))
                    continue
                }
                if (line.startsWith(VARLENGTH_TAG)) {
                    continue; // tag removed...
                }
                if (ctx != null) {
                    ctx.parseMetaLine(line)
                }
            }

            for (BExpressionContext c : listeners.values()) {
                c.finishMetaParsing()
            }

        } catch (Exception e) {
            throw RuntimeException("file: " + ContentStorage.get().getFileInfo(PersistableFolder.ROUTING_BASE.getFolder(), BRouterConstants.BROUTER_LOOKUPS_FILENAME), e)
        }
    }
}
