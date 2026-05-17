package cgeo.geocaching.brouter.expressions;

import cgeo.geocaching.brouter.util.DefaultFilesUtils;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;

import org.junit.Before;

public class EncodeDecodeTest {

    static BExpressionContextWay expctxWay;

    //@Test
    @Before
    public void prepareEncodeDecodeTest() {
        // make sure profile files exist
        DefaultFilesUtils.checkDefaultFiles();

        // create routing context and load profile
        final BExpressionMetaData meta = new BExpressionMetaData();
        expctxWay = new BExpressionContextWay(meta);
        meta.readMetaData();
        final ContentStorage.FileInformation fi = ContentStorage.get().getFileInfo(PersistableFolder.ROUTING_BASE.getFolder(), "trekking.brf");
        expctxWay.parseFile(fi.uri, "global");
    }

/* TODO
    @Test
    public void encodeDecodeTestSpeed() {
        final String[] tags = {
            "highway=residential",
            "oneway=yes",
            "maxspeed=30"
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

    @Test
    public void encodeDecodeTestSpeedUnknown() {
        final String[] tags = {
            "highway=residential",
            "oneway=yes",
            "maxspeed=32"
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

        final float costfactor = expctxWay.getCostfactor();

        System.out.println("test unknown speed " + costfactor + "\ndescription: " + expctxWay.getKeyValueDescription(true, description));

        Assert.assertTrue("costfactor mismatch", Math.abs(costfactor - 0.0f) < 0.00001f);
    }

    @Test
    public void encodeDecodeTestHeight() {
        final String[] tags = {
            "highway=residential",
            "oneway=yes",
            "maxheight=default"
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

        final float costfactor = expctxWay.getCostfactor();

        System.out.println("test height " + costfactor + "\ndescription: " + expctxWay.getKeyValueDescription(true, description));

        Assert.assertTrue("costfactor mismatch", Math.abs(costfactor - 1.0f) < 0.00001f);
    }

    @Test
    public void encodeDecodeTestHeightUnknown() {
        final String[] tags = {
            "highway=residential",
            "oneway=yes",
            "maxheight=below_default"
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

        final float costfactor = expctxWay.getCostfactor();

        System.out.println("test unknown height " + costfactor + "\ndescription: " + expctxWay.getKeyValueDescription(true, description));

        Assert.assertTrue("costfactor mismatch", Math.abs(costfactor - 1.0f) < 0.00001f);
    }


    @Test
    public void encodeDecodeTestValues() {
        final String[] tags = {
            "highway=residential",
            "oneway=yes",
            "depth=1'6\"",
//    "depth=6 feet",
            "maxheight=5.1m",
            "maxweight=5000 lbs",
            "maxdraft=~3 m - 4 m"
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

        final float costfactor = expctxWay.getCostfactor();

        System.out.println("test values " + costfactor + "\ndescription: " + expctxWay.getKeyValueDescription(true, description));

        Assert.assertTrue("costfactor mismatch", Math.abs(costfactor - 1.0f) < 0.00001f);
    }
*/
}
