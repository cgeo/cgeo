// context for simple expression
// context means:
// - the local variables
// - the local variable names
// - the lookup-input variables

package cgeo.geocaching.brouter.expressions;

import cgeo.geocaching.brouter.util.BitCoderContext;
import cgeo.geocaching.brouter.util.Crc32Utils;
import cgeo.geocaching.brouter.util.IByteArrayUnifier;
import cgeo.geocaching.brouter.util.LruMap;
import cgeo.geocaching.storage.ContentStorage;

import android.net.Uri;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TreeMap;

public abstract class BExpressionContext implements IByteArrayUnifier {
    private static final String CONTEXT_TAG = "---context:";
    private static final String MODEL_TAG = "---model:";
    public boolean useKinematicModel;
    public BExpressionMetaData meta;
    private String context;
    private boolean inOurContext = false;
    private BufferedReader br = null;
    private boolean readerDone = false;
    private final Map<String, Integer> lookupNumbers = new HashMap<>();
    private final ArrayList<BExpressionLookupValue[]> lookupValues = new ArrayList<>();
    private final ArrayList<String> lookupNames = new ArrayList<>();
    private final ArrayList<int[]> lookupHistograms = new ArrayList<>();
    private boolean[] lookupIdxUsed;
    private boolean lookupDataFrozen = false;
    private int[] lookupData = new int[0];
    private final byte[] abBuf = new byte[256];
    private final BitCoderContext ctxEndode = new BitCoderContext(abBuf);
    private final BitCoderContext ctxDecode = new BitCoderContext(new byte[0]);
    private final Map<String, Integer> variableNumbers = new HashMap<>();
    private float[] variableData;
    // hash-cache for function results
    private final CacheNode probeCacheNode = new CacheNode();
    private LruMap cache;
    private final VarWrapper probeVarSet = new VarWrapper();
    private LruMap resultVarCache;
    private List<BExpression> expressionList;
    private int minWriteIdx;
    // build-in variable indexes for fast access
    private int[] buildInVariableIdx;
    private int nBuildInVars;
    private float[] currentVars;
    private int currentVarOffset;
    private BExpressionContext foreignContext;
    private int linenr;
    private boolean lookupDataValid = false;
    private int parsedLines = 0;
    private boolean fixTagsWritten = false;
    private long requests;
    private long requests2;
    private long cachemisses;
    private CacheNode lastCacheNode = new CacheNode();

    protected BExpressionContext(final String context, final BExpressionMetaData meta) {
        this(context, 4096, meta);
    }


    /**
     * Create an Expression-Context for the given node
     *
     * @param context  global, way or node - context of that instance
     * @param hashSize size of hashmap for result caching
     */
    protected BExpressionContext(final String context, int hashSize, final BExpressionMetaData meta) {
        this.context = context;
        this.meta = meta;

        if (meta != null) {
            meta.registerListener(context, this);
        }

        if (Boolean.getBoolean("disableExpressionCache")) {
            hashSize = 1;
        }

        // create the expression cache
        if (hashSize > 0) {
            cache = new LruMap(4 * hashSize, hashSize);
            resultVarCache = new LruMap(4096, 4096);
        }
    }

    protected void setInverseVars() {
        currentVarOffset = nBuildInVars;
    }

    protected abstract String[] getBuildInVariableNames();

    public final float getBuildInVariable(final int idx) {
        return currentVars[idx + currentVarOffset];
    }

    /**
     * encode internal lookup data to a byte array
     */
    public byte[] encode() {
        if (!lookupDataValid) {
            throw new IllegalArgumentException("internal error: encoding undefined data?");
        }
        return encode(lookupData);
    }

    public byte[] encode(final int[] ld) {
        final BitCoderContext ctx = ctxEndode;
        ctx.reset();

        int skippedTags = 0;
        int nonNullTags = 0;

        // (skip first bit ("reversedirection") )

        // all others are generic
        for (int inum = 1; inum < lookupValues.size(); inum++) { // loop over lookup names
            final int d = ld[inum];
            if (d == 0) {
                skippedTags++;
                continue;
            }
            ctx.encodeVarBits(skippedTags + 1);
            nonNullTags++;
            skippedTags = 0;

            // 0 excluded already, 1 (=unknown) we rotate up to 8
            // to have the good code space for the popular values
            final int dd = d < 2 ? 7 : (d < 9 ? d - 2 : d - 1);
            ctx.encodeVarBits(dd);
        }
        ctx.encodeVarBits(0);

        if (nonNullTags == 0) {
            return null;
        }

        final int len = ctx.closeAndGetEncodedLength();
        final byte[] ab = new byte[len];
        System.arraycopy(abBuf, 0, ab, 0, len);


        // crosscheck: decode and compare
        final int[] ld2 = new int[lookupValues.size()];
        decode(ld2, false, ab);
        for (int inum = 1; inum < lookupValues.size(); inum++) { // loop over lookup names (except reverse dir)
            if (ld2[inum] != ld[inum]) {
                throw new RuntimeException("assertion failed encoding inum=" + inum + " val=" + ld[inum] + " " + getKeyValueDescription(false, ab));
            }
        }

        return ab;
    }

    /**
     * decode byte array to internal lookup data
     */
    public void decode(final byte[] ab) {
        decode(lookupData, false, ab);
        lookupDataValid = true;
    }

    /**
     * decode a byte-array into a lookup data array
     */
    // external code, do not refactor
    @SuppressWarnings("PMD.NPathComplexity")
    private void decode(final int[] ld, final boolean inverseDirection, final byte[] ab) {
        final BitCoderContext ctx = ctxDecode;
        ctx.reset(ab);

        // start with first bit hardwired ("reversedirection")
        ld[0] = inverseDirection ? 2 : 0;

        // all others are generic
        int inum = 1;
        for (; ; ) {
            int delta = ctx.decodeVarBits();
            if (delta == 0) {
                break;
            }
            if (inum + delta > ld.length) {
                break; // higher minor version is o.k.
            }

            while (delta-- > 1) {
                ld[inum++] = 0;
            }

            // see encoder for value rotation
            final int dd = ctx.decodeVarBits();
            int d = dd == 7 ? 1 : (dd < 7 ? dd + 2 : dd + 1);
            if (d >= lookupValues.get(inum).length && d < 1000) { // map out-of-range to unknown
                d = 1; // map out-of-range to unknown
            }
            ld[inum++] = d;
        }
        while (inum < ld.length) {
            ld[inum++] = 0;
        }
    }

    public String getKeyValueDescription(final boolean inverseDirection, final byte[] ab) {
        final StringBuilder sb = new StringBuilder(200);
        decode(lookupData, inverseDirection, ab);
        for (int inum = 0; inum < lookupValues.size(); inum++) { // loop over lookup names
            final BExpressionLookupValue[] va = lookupValues.get(inum);
            final int val = lookupData[inum];
            final String value = (val >= 1000) ? Float.toString((val - 1000) / 100f) : va[val].toString();
            if (value != null && value.length() > 0) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(lookupNames.get(inum)).append("=").append(value);
            }
        }
        return sb.toString();
    }

    public List<String> getKeyValueList(final boolean inverseDirection, final byte[] ab) {
        final ArrayList<String> res = new ArrayList<>();
        decode(lookupData, inverseDirection, ab);
        for (int inum = 0; inum < lookupValues.size(); inum++) { // loop over lookup names
            final BExpressionLookupValue[] va = lookupValues.get(inum);
            final int val = lookupData[inum];
            // no negative values
            final String value = (val >= 1000) ? Float.toString((val - 1000) / 100f) : va[val].toString();
            if (value != null && value.length() > 0) {
                res.add(lookupNames.get(inum));
                res.add(value);
            }
        }
        return res;
    }

    public int getLookupKey(String name) {
        int res = -1;
        try {
            res = lookupNumbers.get(name);
        } catch (Exception ignore) {
            // ignore
        }
        return res;
    }

    public float getLookupValue(int key) {
        float res = 0f;
        final int val = lookupData[key];
        if (val == 0) {
            return Float.NaN;
        }
        res = (val - 1000) / 100f;
        return res;
    }

    public float getLookupValue(boolean inverseDirection, byte[] ab, int key) {
        float res = 0f;
        decode(lookupData, inverseDirection, ab);
        final int val = lookupData[key];
        if (val == 0) {
            return Float.NaN;
        }
        res = (val - 1000) / 100f;
        return res;
    }

    public void parseMetaLine(final String line) {
        parsedLines++;
        final StringTokenizer tk = new StringTokenizer(line, " ");
        String name = tk.nextToken();
        final String value = tk.nextToken();
        final int idx = name.indexOf(';');
        if (idx >= 0) {
            name = name.substring(0, idx);
        }

        if (!fixTagsWritten) {
            fixTagsWritten = true;
            if ("way".equals(context)) {
                addLookupValue("reversedirection", "yes", null);
            } else if ("node".equals(context)) {
                addLookupValue("nodeaccessgranted", "yes", null);
            }
        }
        if ("reversedirection".equals(name)) {
            return; // this is hardcoded
        }
        if ("nodeaccessgranted".equals(name)) {
            return; // this is hardcoded
        }
        final BExpressionLookupValue newValue = addLookupValue(name, value, null);

        // add aliases
        while (newValue != null && tk.hasMoreTokens()) {
            newValue.addAlias(tk.nextToken());
        }
    }

    public void finishMetaParsing() {
        if (parsedLines == 0 && !"global".equals(context)) {
            throw new IllegalArgumentException("lookup table does not contain data for context " + context + " (old version?)");
        }

        // post-process metadata:
        lookupDataFrozen = true;

        lookupIdxUsed = new boolean[lookupValues.size()];
    }

    public final void evaluate(final int[] lookupData2) {
        lookupData = lookupData2;
        evaluate();
    }

    private void evaluate() {
        final int n = expressionList.size();
        for (int expidx = 0; expidx < n; expidx++) {
            expressionList.get(expidx).evaluate(this);
        }
    }

    public String cacheStats() {
        return "requests=" + requests + " requests2=" + requests2 + " cachemisses=" + cachemisses;
    }

    // @Override
    public final byte[] unify(final byte[] ab, final int offset, final int len) {
        probeCacheNode.ab = null; // crc based cache lookup only
        probeCacheNode.hash = Crc32Utils.crc(ab, offset, len);

        CacheNode cn = (CacheNode) cache.get(probeCacheNode);
        if (cn != null) {
            final byte[] cab = cn.ab;
            if (cab.length == len) {
                for (int i = 0; i < len; i++) {
                    if (cab[i] != ab[i + offset]) {
                        cn = null;
                        break;
                    }
                }
                if (cn != null) {
                    lastCacheNode = cn;
                    return cn.ab;
                }
            }
        }
        final byte[] nab = new byte[len];
        System.arraycopy(ab, offset, nab, 0, len);
        return nab;
    }


    public final void evaluate(final boolean inverseDirection, final byte[] ab) {
        requests++;
        lookupDataValid = false; // this is an assertion for a nasty pifall

        if (cache == null) {
            decode(lookupData, inverseDirection, ab);
            if (currentVars == null || currentVars.length != nBuildInVars) {
                currentVars = new float[nBuildInVars];
            }
            evaluateInto(currentVars, 0);
            currentVarOffset = 0;
            return;
        }

        CacheNode cn;
        if (lastCacheNode.ab == ab) {
            cn = lastCacheNode;
        } else {
            probeCacheNode.ab = ab;
            probeCacheNode.hash = Crc32Utils.crc(ab, 0, ab.length);
            cn = (CacheNode) cache.get(probeCacheNode);
        }

        if (cn == null) {
            cachemisses++;

            cn = (CacheNode) cache.removeLru();
            if (cn == null) {
                cn = new CacheNode();
            }
            cn.hash = probeCacheNode.hash;
            cn.ab = ab;
            cache.put(cn);

            if (probeVarSet.vars == null) {
                probeVarSet.vars = new float[2 * nBuildInVars];
            }

            // forward direction
            decode(lookupData, false, ab);
            evaluateInto(probeVarSet.vars, 0);

            // inverse direction
            lookupData[0] = 2; // inverse shortcut: reuse decoding
            evaluateInto(probeVarSet.vars, nBuildInVars);

            probeVarSet.hash = Arrays.hashCode(probeVarSet.vars);

            // unify the result variable set
            VarWrapper vw = (VarWrapper) resultVarCache.get(probeVarSet);
            if (vw == null) {
                vw = (VarWrapper) resultVarCache.removeLru();
                if (vw == null) {
                    vw = new VarWrapper();
                }
                vw.hash = probeVarSet.hash;
                vw.vars = probeVarSet.vars;
                probeVarSet.vars = null;
                resultVarCache.put(vw);
            }
            cn.vars = vw.vars;
        } else {
            if (ab == cn.ab) {
                requests2++;
            }

            cache.touch(cn);
        }

        currentVars = cn.vars;
        currentVarOffset = inverseDirection ? nBuildInVars : 0;
    }

    private void evaluateInto(final float[] vars, final int offset) {
        evaluate();
        for (int vi = 0; vi < nBuildInVars; vi++) {
            final int idx = buildInVariableIdx[vi];
            vars[vi + offset] = idx == -1 ? 0.f : variableData[idx];
        }
    }


    public void dumpStatistics() {
        final TreeMap<String, String> counts = new TreeMap<>();
        // first count
        for (String name : lookupNumbers.keySet()) {
            int cnt = 0;
            final int inum = lookupNumbers.get(name);
            final int[] histo = lookupHistograms.get(inum);
//    if ( histo.length == 500 ) continue;
            for (int i = 2; i < histo.length; i++) {
                cnt += histo[i];
            }
            counts.put("" + (1000000000 + cnt) + "_" + name, name);
        }

        while (counts.size() > 0) {
            final String key = counts.lastEntry().getKey();
            final String name = counts.get(key);
            counts.remove(key);
            final int inum = lookupNumbers.get(name);
            final BExpressionLookupValue[] values = lookupValues.get(inum);
            final int[] histo = lookupHistograms.get(inum);
            if (values.length == 1000) {
                continue;
            }
            final String[] svalues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                String scnt = "0000000000" + histo[i];
                scnt = scnt.substring(scnt.length() - 10);
                svalues[i] = scnt + " " + values[i].toString();
            }
            Arrays.sort(svalues);
            for (int i = svalues.length - 1; i >= 0; i--) {
                System.out.println(name + ";" + svalues[i]);
            }
        }
    }

    /**
     * @return a new lookupData array, or null if no metadata defined
     */
    public int[] createNewLookupData() {
        if (lookupDataFrozen) {
            return new int[lookupValues.size()];
        }
        return null;
    }

    /**
     * generate random values for regression testing
     */
    public int[] generateRandomValues(final Random rnd) {
        final int[] data = createNewLookupData();
        data[0] = 2 * rnd.nextInt(2); // reverse-direction = 0 or 2
        for (int inum = 1; inum < data.length; inum++) {
            final int nvalues = lookupValues.get(inum).length;
            data[inum] = 0;
            if (inum > 1 && rnd.nextInt(10) > 0) {
                continue; // tags other than highway only 10%
            }
            data[inum] = rnd.nextInt(nvalues);
        }
        lookupDataValid = true;
        return data;
    }

    public void assertAllVariablesEqual(final BExpressionContext other) {
        final int nv = variableData.length;
        final int nv2 = other.variableData.length;
        if (nv != nv2) {
            throw new RuntimeException("mismatch in variable-count: " + nv + "<->" + nv2);
        }
        for (int i = 0; i < nv; i++) {
            if (variableData[i] != other.variableData[i]) {
                throw new RuntimeException("mismatch in variable " + variableName(i) + " " + variableData[i] + "<->" + other.variableData[i]
                        + "\ntags = " + getKeyValueDescription(false, encode()));
            }
        }
    }

    public String variableName(final int idx) {
        for (Map.Entry<String, Integer> e : variableNumbers.entrySet()) {
            if (e.getValue().intValue() == idx) {
                return e.getKey();
            }
        }
        throw new RuntimeException("no variable for index" + idx);
    }

    /**
     * add a new lookup-value for the given name to the given lookupData array.
     * If no array is given (null value passed), the value is added to
     * the context-binded array. In that case, unknown names and values are
     * created dynamically.
     *
     * @return a newly created value element, if any, to optionally add aliases
     */
    // external code, do not refactor
    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
    public BExpressionLookupValue addLookupValue(final String name, final String value, final int[] lookupData2) {
        String valueMutable = value;
        BExpressionLookupValue newValue = null;
        Integer num = lookupNumbers.get(name);
        if (num == null) {
            if (lookupData2 != null) {
                // do not create unknown name for external data array
                return newValue;
            }

            // unknown name, create
            num = lookupValues.size();
            lookupNumbers.put(name, num);
            lookupNames.add(name);
            lookupValues.add(new BExpressionLookupValue[]{new BExpressionLookupValue("")
                    , new BExpressionLookupValue("unknown")});
            lookupHistograms.add(new int[2]);
            final int[] ndata = new int[lookupData.length + 1];
            System.arraycopy(lookupData, 0, ndata, 0, lookupData.length);
            lookupData = ndata;
        }

        // look for that value
        final int inum = num;
        BExpressionLookupValue[] values = lookupValues.get(inum);
        int[] histo = lookupHistograms.get(inum);
        int i = 0;
        boolean bFoundAsterix = false;
        for (; i < values.length; i++) {
            final BExpressionLookupValue v = values[i];
            if (v.equals("*")) {
                bFoundAsterix = true;
            }
            if (v.matches(valueMutable)) {
                break;
            }
        }
        if (i == values.length) {
            if (lookupData2 != null) {
                // do not create unknown value for external data array,
                // record as 'other' instead
                lookupData2[inum] = 0;
                if (bFoundAsterix) {
                    // found value for lookup *
                    //System.out.println( "add unknown " + name + "  " + value );
                    final String org = valueMutable;
                    try {
                        // remove some unused characters
                        valueMutable = valueMutable.replace(",", ".");
                        valueMutable = valueMutable.replace(">", "");
                        valueMutable = valueMutable.replace("_", "");
                        if (valueMutable.indexOf("-") == 0) {
                            valueMutable = valueMutable.substring(1);
                        }
                        if (valueMutable.indexOf("~") == 0) {
                            valueMutable = valueMutable.substring(1);
                        }
                        if (valueMutable.contains("-")) {    // replace eg. 1.4-1.6 m
                            final String tmp = valueMutable.substring(valueMutable.indexOf("-") + 1).replaceAll("[0-9.,-]", "");
                            valueMutable = valueMutable.substring(0, valueMutable.indexOf("-")) + tmp;
                        }
                        // do some value conversion
                        if (valueMutable.toLowerCase().contains("ft")) {
                            float foot = 0f;
                            int inch = 0;
                            final String[] sa = valueMutable.toLowerCase().trim().split("ft");
                            if (sa.length >= 1) {
                                foot = Float.parseFloat(sa[0].trim());
                            }
                            if (sa.length == 2) {
                                valueMutable = sa[1];
                                if (valueMutable.indexOf("in") > 0) {
                                    valueMutable = valueMutable.substring(0, valueMutable.indexOf("in"));
                                }
                                inch = Integer.parseInt(valueMutable.trim());
                                foot += inch / 12f;
                            }
                            valueMutable = String.format(Locale.US, "%3.1f", foot * 0.3048f);
                        }
                        if (valueMutable.toLowerCase().contains("'")) {
                            float foot = 0f;
                            int inch = 0;
                            final String[] sa = valueMutable.toLowerCase().trim().split("'");
                            if (sa.length >= 1) {
                                foot = Float.parseFloat(sa[0].trim());
                            }
                            if (sa.length == 2) {
                                valueMutable = sa[1];
                                if (valueMutable.indexOf("''") > 0) {
                                    valueMutable = valueMutable.substring(0, valueMutable.indexOf("''"));
                                }
                                if (valueMutable.indexOf("\"") > 0) {
                                    valueMutable = valueMutable.substring(0, valueMutable.indexOf("\""));
                                }
                                inch = Integer.parseInt(valueMutable.trim());
                                foot += inch / 12f;
                            }
                            valueMutable = String.format(Locale.US, "%3.1f", foot * 0.3048f);
                        } else if (valueMutable.contains("in") || valueMutable.contains("\"")) {
                            float inch = 0f;
                            if (valueMutable.indexOf("in") > 0) {
                                valueMutable = valueMutable.substring(0, valueMutable.indexOf("in"));
                            }
                            if (valueMutable.indexOf("\"") > 0) {
                                valueMutable = valueMutable.substring(0, valueMutable.indexOf("\""));
                            }
                            inch = Float.parseFloat(valueMutable.trim());
                            valueMutable = String.format(Locale.US, "%3.1f", inch * 0.0254f);
                        } else if (valueMutable.toLowerCase().contains("feet") || valueMutable.toLowerCase().contains("foot")) {
                            float feet = 0f;
                            final String s = valueMutable.substring(0, valueMutable.toLowerCase().indexOf("f"));
                            feet = Float.parseFloat(s.trim());
                            valueMutable = String.format(Locale.US, "%3.1f", feet * 0.3048f);
                        } else if (valueMutable.toLowerCase().contains("fathom") || valueMutable.toLowerCase().contains("fm")) {
                            float fathom = 0f;
                            final String s = valueMutable.substring(0, valueMutable.toLowerCase().indexOf("f"));
                            fathom = Float.parseFloat(s.trim());
                            valueMutable = String.format(Locale.US, "%3.1f", fathom * 1.8288f);
                        } else if (valueMutable.contains("cm")) {
                            final String[] sa = valueMutable.trim().split("cm");
                            if (sa.length == 1) {
                                valueMutable = sa[0].trim();
                            }
                            final float cm = Float.parseFloat(valueMutable.trim());
                            valueMutable = String.format(Locale.US, "%3.1f", cm * 100f);
                        } else if (valueMutable.toLowerCase().contains("meter")) {
                            final String s = valueMutable.substring(0, valueMutable.toLowerCase().indexOf("m"));
                            valueMutable = s.trim();
                        } else if (valueMutable.toLowerCase().contains("mph")) {
                            valueMutable = valueMutable.replace("_", "");
                            final String[] sa = valueMutable.trim().toLowerCase().split("mph");
                            if (sa.length >= 1) {
                                valueMutable = sa[0].trim();
                            }
                            final float mph = Float.parseFloat(valueMutable.trim());
                            valueMutable = String.format(Locale.US, "%3.1f", mph * 1.609344f);
                        } else if (valueMutable.toLowerCase().contains("knot")) {
                            final String[] sa = valueMutable.trim().toLowerCase().split("knot");
                            if (sa.length >= 1) {
                                valueMutable = sa[0].trim();
                            }
                            final float nm = Float.parseFloat(valueMutable.trim());
                            valueMutable = String.format(Locale.US, "%3.1f", nm * 1.852f);
                        } else if (valueMutable.contains("kmh") || valueMutable.contains("km/h") || valueMutable.contains("kph")) {
                            final String[] sa = valueMutable.trim().split("k");
                            if (sa.length == 1) {
                                valueMutable = sa[0].trim();
                            }
                        } else if (valueMutable.contains("m")) {
                            final String s = valueMutable.substring(0, valueMutable.toLowerCase().indexOf("m"));
                            valueMutable = s.trim();
                        } else if (valueMutable.contains("(")) {
                            final String s = valueMutable.substring(0, valueMutable.toLowerCase().indexOf("("));
                            valueMutable = s.trim();
                        }
                        // found negative maxdraft values
                        // no negative values
                        // values are float with 2 decimals
                        lookupData2[inum] = 1000 + (int) (Math.abs(Float.parseFloat(valueMutable)) * 100f);
                    } catch (Exception e) {
                        // ignore errors
                        System.err.println("error for " + name + "  " + org + " trans " + valueMutable + " " + e.getMessage());
                        lookupData2[inum] = 0;
                    }
                }
                return newValue;
            }

            if (i == 500) {
                return newValue;
            }
            // unknown value, create
            final BExpressionLookupValue[] nvalues = new BExpressionLookupValue[values.length + 1];
            final int[] nhisto = new int[values.length + 1];
            System.arraycopy(values, 0, nvalues, 0, values.length);
            System.arraycopy(histo, 0, nhisto, 0, histo.length);
            values = nvalues;
            histo = nhisto;
            newValue = new BExpressionLookupValue(value);
            values[i] = newValue;
            lookupHistograms.set(inum, histo);
            lookupValues.set(inum, values);
        }

        histo[i]++;

        // finally remember the actual data
        if (lookupData2 != null) {
            lookupData2[inum] = i;
        } else {
            lookupData[inum] = i;
        }
        return newValue;
    }

    /**
     * add a value-index to to internal array
     * value-index means 0=unknown, 1=other, 2=value-x, ...
     */
    public void addLookupValue(final String name, final int valueIndex) {
        final Integer num = lookupNumbers.get(name);
        if (num == null) {
            return;
        }

        // look for that value
        final int inum = num;
        final int nvalues = lookupValues.get(inum).length;
        if (valueIndex < 0 || valueIndex >= nvalues) {
            throw new IllegalArgumentException("value index out of range for name " + name + ": " + valueIndex);
        }
        lookupData[inum] = valueIndex;
    }

    public int getOutputVariableIndex(final String name, final boolean mustExist) {
        final int idx = getVariableIdx(name, false);
        if (idx < 0) {
            if (mustExist) {
                throw new IllegalArgumentException("unknown variable: " + name);
            }
        } else if (idx < minWriteIdx) {
            throw new IllegalArgumentException("bad access to global variable: " + name);
        }
        for (int i = 0; i < nBuildInVars; i++) {
            if (buildInVariableIdx[i] == idx) {
                return i;
            }
        }
        final int[] extended = new int[nBuildInVars + 1];
        System.arraycopy(buildInVariableIdx, 0, extended, 0, nBuildInVars);
        extended[nBuildInVars] = idx;
        buildInVariableIdx = extended;
        return nBuildInVars++;
    }

    public void setForeignContext(final BExpressionContext foreignContext) {
        this.foreignContext = foreignContext;
    }

    public float getForeignVariableValue(final int foreignIndex) {
        return foreignContext.getBuildInVariable(foreignIndex);
    }

    public int getForeignVariableIdx(final String context, final String name) {
        if (foreignContext == null || !context.equals(foreignContext.context)) {
            throw new IllegalArgumentException("unknown foreign context: " + context);
        }
        return foreignContext.getOutputVariableIndex(name, true);
    }

    public void parseFile(final Uri uri, final String readOnlyContext) {
        final InputStream is = ContentStorage.get().openForRead(uri);
        if (is == null) {
            throw new IllegalArgumentException("profile " + uri + " does not exist");
        }
        try {
            if (readOnlyContext != null) {
                linenr = 1;
                final String realContext = context;
                context = readOnlyContext;

                final InputStream is2 = ContentStorage.get().openForRead(uri);
                expressionList = parseFileHelper(is2);
                variableData = new float[variableNumbers.size()];
                evaluate(lookupData); // lookupData is dummy here - evaluate just to create the variables
                context = realContext;
            }
            linenr = 1;
            minWriteIdx = variableData == null ? 0 : variableData.length;

            expressionList = parseFileHelper(is);

            // determine the build-in variable indices
            final String[] varNames = getBuildInVariableNames();
            nBuildInVars = varNames.length;
            buildInVariableIdx = new int[nBuildInVars];
            for (int vi = 0; vi < varNames.length; vi++) {
                buildInVariableIdx[vi] = getVariableIdx(varNames[vi], false);
            }

            final float[] readOnlyData = variableData;
            variableData = new float[variableNumbers.size()];
            for (int i = 0; i < minWriteIdx; i++) {
                variableData[i] = readOnlyData[i];
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("ParseException " + uri.toString() + " at line " + linenr + ": " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (expressionList.size() == 0) {
            throw new IllegalArgumentException("profile does not contain expressions for context " + context + " (old version?)");
        }
    }

    private List<BExpression> parseFileHelper(final InputStream is) throws Exception {
        br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        readerDone = false;
        final List<BExpression> result = new ArrayList<>();
        for (; ; ) {
            final BExpression exp = BExpression.parse(this, 0);
            if (exp == null) {
                break;
            }
            result.add(exp);
        }
        br.close();
        br = null;
        return result;
    }

    public void setVariableValue(String name, float value, boolean create) {
        final Integer num = variableNumbers.get(name);
        if (num != null) {
            variableData[num] = value;
        }
    }

    public float getVariableValue(final String name, final float defaultValue) {
        final Integer num = variableNumbers.get(name);
        return num == null ? defaultValue : getVariableValue(num.intValue());
    }

    public float getVariableValue(final int variableIdx) {
        return variableData[variableIdx];
    }

    public int getVariableIdx(final String name, final boolean create) {
        Integer num = variableNumbers.get(name);
        if (num == null) {
            if (create) {
                num = variableNumbers.size();
                variableNumbers.put(name, num);
            } else {
                return -1;
            }
        }
        return num.intValue();
    }

    public int getMinWriteIdx() {
        return minWriteIdx;
    }

    public float getLookupMatch(final int nameIdx, final int[] valueIdxArray) {
        for (int i = 0; i < valueIdxArray.length; i++) {
            if (lookupData[nameIdx] == valueIdxArray[i]) {
                return 1.0f;
            }
        }
        return 0.0f;
    }

    public int getLookupNameIdx(final String name) {
        final Integer num = lookupNumbers.get(name);
        return num == null ? -1 : num.intValue();
    }

    public final void markLookupIdxUsed(final int idx) {
        lookupIdxUsed[idx] = true;
    }

    public final boolean isLookupIdxUsed(final int idx) {
        return idx < lookupIdxUsed.length && lookupIdxUsed[idx];
    }

    public final void setAllTagsUsed() {
        for (int i = 0; i < lookupIdxUsed.length; i++) {
            lookupIdxUsed[i] = true;
        }
    }

    public int getLookupValueIdx(final int nameIdx, final String value) {
        final BExpressionLookupValue[] values = lookupValues.get(nameIdx);
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) {
                return i;
            }
        }
        return -1;
    }

    public String parseToken() throws Exception {
        for (; ; ) {
            final String token = parseTokenHelper();
            if (token == null) {
                return null;
            }
            if (token.startsWith(CONTEXT_TAG)) {
                inOurContext = token.substring(CONTEXT_TAG.length()).equals(context);
            } else if (token.startsWith(MODEL_TAG)) {
                // no need to parse the name, as c:geo will only support the builtin model class (also prevents class injection)
                useKinematicModel = true;
            } else if (inOurContext) {
                return token;
            }
        }
    }


    private String parseTokenHelper() throws Exception {
        final StringBuilder sb = new StringBuilder(32);
        boolean inComment = false;
        for (; ; ) {
            final int ic = readerDone ? -1 : br.read();
            if (ic < 0) {
                if (sb.length() == 0) {
                    return null;
                }
                readerDone = true;
                return sb.toString();
            }
            final char c = (char) ic;
            if (c == '\n') {
                linenr++;
            }

            if (inComment) {
                if (c == '\r' || c == '\n') {
                    inComment = false;
                }
                continue;
            }
            if (Character.isWhitespace(c)) {
                if (sb.length() > 0) {
                    return sb.toString();
                } else {
                    continue;
                }
            }
            if (c == '#' && sb.length() == 0) {
                inComment = true;
            } else {
                sb.append(c);
            }
        }
    }

    public float assign(final int variableIdx, final float value) {
        variableData[variableIdx] = value;
        return value;
    }

}
