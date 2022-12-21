package cgeo.geocaching.brouter.expressions;

import cgeo.geocaching.brouter.util.DefaultFilesUtils;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;

import org.junit.Assert;
import org.junit.Test;

public class EncodeDecodeTest {

    @Test
    public void encodeDecodeTest() {
        // make sure profile files exist
        DefaultFilesUtils.checkDefaultFiles();

        // create routing context and load profile
        final BExpressionMetaData meta = new BExpressionMetaData();
        final BExpressionContextWay expctxWay = new BExpressionContextWay(meta);
        meta.readMetaData();
        final ContentStorage.FileInformation fi = ContentStorage.get().getFileInfo(PersistableFolder.ROUTING_BASE.getFolder(), "trekking.brf");
        expctxWay.parseFile(fi.uri, "global");

        final String[] tags = {
                "highway=residential",
                "oneway=yes",
                "depth=1'6\"",
//          "depth=6 feet",
                "maxheight=5.1m",
                "maxdraft=~3 mt",
                "reversedirection=yes"
        };

        // encode the tags into 64 bit description word
        final int[] lookupData = expctxWay.createNewLookupData();
        for (String arg : tags) {
            final int idx = arg.indexOf('=');
            if (idx < 0) {
                throw new IllegalArgumentException("bad argument (should be <tag>=<value>): " + arg);
            }
            final String key = arg.substring(0, idx);
            final String value = arg.substring(idx + 1);

            expctxWay.addLookupValue(key, value, lookupData);
        }
        final byte[] description = expctxWay.encode(lookupData);

        // calculate the cost factor from that description
        expctxWay.evaluate(true, description); // true = "reversedirection=yes"  (not encoded in description anymore)

        System.out.println("description: " + expctxWay.getKeyValueDescription(true, description));

        final float costfactor = expctxWay.getCostfactor();
        Assert.assertTrue("costfactor mismatch", Math.abs(costfactor - 5.15) < 0.00001);
    }
}
