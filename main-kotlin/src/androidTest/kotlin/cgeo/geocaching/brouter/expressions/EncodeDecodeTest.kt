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

package cgeo.geocaching.brouter.expressions

import cgeo.geocaching.brouter.util.DefaultFilesUtils
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.PersistableFolder

import org.junit.Assert
import org.junit.Test

class EncodeDecodeTest {

    @Test
    public Unit encodeDecodeTest() {
        // make sure profile files exist
        DefaultFilesUtils.checkDefaultFiles()

        // create routing context and load profile
        val meta: BExpressionMetaData = BExpressionMetaData()
        val expctxWay: BExpressionContextWay = BExpressionContextWay(meta)
        meta.readMetaData()
        final ContentStorage.FileInformation fi = ContentStorage.get().getFileInfo(PersistableFolder.ROUTING_BASE.getFolder(), "trekking.brf")
        expctxWay.parseFile(fi.uri, "global")

        final String[] tags = {
                "highway=residential",
                "oneway=yes",
                "depth=1'6\"",
//          "depth=6 feet",
                "maxheight=5.1m",
                "maxdraft=~3 mt",
                "reversedirection=yes"
        }

        // encode the tags into 64 bit description word
        final Int[] lookupData = expctxWay.createNewLookupData()
        for (String arg : tags) {
            val idx: Int = arg.indexOf('=')
            if (idx < 0) {
                throw IllegalArgumentException("bad argument (should be <tag>=<value>): " + arg)
            }
            val key: String = arg.substring(0, idx)
            val value: String = arg.substring(idx + 1)

            expctxWay.addLookupValue(key, value, lookupData)
        }
        final Byte[] description = expctxWay.encode(lookupData)

        // calculate the cost factor from that description
        expctxWay.evaluate(true, description); // true = "reversedirection=yes"  (not encoded in description anymore)

        println("description: " + expctxWay.getKeyValueDescription(true, description))

        val costfactor: Float = expctxWay.getCostfactor()
        Assert.assertTrue("costfactor mismatch", Math.abs(costfactor - 5.15) < 0.00001)
    }
}
