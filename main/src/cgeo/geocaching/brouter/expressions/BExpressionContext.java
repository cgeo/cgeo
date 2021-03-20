// context for simple expression
// context means:
// - the local variables
// - the local variable names
// - the lookup-input variables

package cgeo.geocaching.brouter.expressions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TreeMap;

import cgeo.geocaching.brouter.util.BitCoderContext;
import cgeo.geocaching.brouter.util.Crc32;
import cgeo.geocaching.brouter.util.IByteArrayUnifier;
import cgeo.geocaching.brouter.util.LruMap;


public abstract class BExpressionContext implements IByteArrayUnifier {
    private static final String CONTEXT_TAG = "---context:";
    private static final String MODEL_TAG = "---model:";
    public String _modelClass;
    public BExpressionMetaData meta;
    private String context;
    private boolean _inOurContext = false;
    private BufferedReader _br = null;
    private boolean _readerDone = false;
    private final Map<String, Integer> lookupNumbers = new HashMap<String, Integer>();
    private final ArrayList<BExpressionLookupValue[]> lookupValues = new ArrayList<BExpressionLookupValue[]>();
    private final ArrayList<String> lookupNames = new ArrayList<String>();
    private final ArrayList<int[]> lookupHistograms = new ArrayList<int[]>();
    private boolean[] lookupIdxUsed;
    private boolean lookupDataFrozen = false;
    private int[] lookupData = new int[0];
    private final byte[] abBuf = new byte[256];
    private final BitCoderContext ctxEndode = new BitCoderContext(abBuf);
    private final BitCoderContext ctxDecode = new BitCoderContext(new byte[0]);
    private final Map<String, Integer> variableNumbers = new HashMap<String, Integer>();
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

    protected BExpressionContext(String context, BExpressionMetaData meta) {
        this(context, 4096, meta);
    }


    /**
     * Create an Expression-Context for the given node
     *
     * @param context  global, way or node - context of that instance
     * @param hashSize size of hashmap for result caching
     */
    protected BExpressionContext(String context, int hashSize, BExpressionMetaData meta) {
        this.context = context;
        this.meta = meta;

        if (meta != null)
            meta.registerListener(context, this);

        if (Boolean.getBoolean("disableExpressionCache"))
            hashSize = 1;

        // create the expression cache
        if (hashSize > 0) {
            cache = new LruMap(4 * hashSize, hashSize);
            resultVarCache = new LruMap(4096, 4096);
        }
    }

    protected void setInverseVars() {
        currentVarOffset = nBuildInVars;
    }

    abstract String[] getBuildInVariableNames();

    public final float getBuildInVariable(int idx) {
        return currentVars[idx + currentVarOffset];
    }

    /**
     * encode internal lookup data to a byte array
     */
    public byte[] encode() {
        if (!lookupDataValid)
            throw new IllegalArgumentException("internal error: encoding undefined data?");
        return encode(lookupData);
    }

    public byte[] encode(int[] ld) {
        BitCoderContext ctx = ctxEndode;
        ctx.reset();

        int skippedTags = 0;
        int nonNullTags = 0;

        // (skip first bit ("reversedirection") )

        // all others are generic
        for (int inum = 1; inum < lookupValues.size(); inum++) // loop over lookup names
        {
            int d = ld[inum];
            if (d == 0) {
                skippedTags++;
                continue;
            }
            ctx.encodeVarBits(skippedTags + 1);
            nonNullTags++;
            skippedTags = 0;

            // 0 excluded already, 1 (=unknown) we rotate up to 8
            // to have the good code space for the popular values
            int dd = d < 2 ? 7 : (d < 9 ? d - 2 : d - 1);
            ctx.encodeVarBits(dd);
        }
        ctx.encodeVarBits(0);

        if (nonNullTags == 0)
            return null;

        int len = ctx.closeAndGetEncodedLength();
        byte[] ab = new byte[len];
        System.arraycopy(abBuf, 0, ab, 0, len);


        // crosscheck: decode and compare
        int[] ld2 = new int[lookupValues.size()];
        decode(ld2, false, ab);
        for (int inum = 1; inum < lookupValues.size(); inum++) // loop over lookup names (except reverse dir)
        {
            if (ld2[inum] != ld[inum])
                throw new RuntimeException("assertion failed encoding inum=" + inum + " val=" + ld[inum] + " " + getKeyValueDescription(false, ab));
        }

        return ab;
    }

    /**
     * decode byte array to internal lookup data
     */
    public void decode(byte[] ab) {
        decode(lookupData, false, ab);
        lookupDataValid = true;
    }

    /**
     * decode a byte-array into a lookup data array
     */
    private void decode(int[] ld, boolean inverseDirection, byte[] ab) {
        BitCoderContext ctx = ctxDecode;
        ctx.reset(ab);

        // start with first bit hardwired ("reversedirection")
        ld[0] = inverseDirection ? 2 : 0;

        // all others are generic
        int inum = 1;
        for (; ; ) {
            int delta = ctx.decodeVarBits();
            if (delta == 0)
                break;
            if (inum + delta > ld.length)
                break; // higher minor version is o.k.

            while (delta-- > 1)
                ld[inum++] = 0;

            // see encoder for value rotation
            int dd = ctx.decodeVarBits();
            int d = dd == 7 ? 1 : (dd < 7 ? dd + 2 : dd + 1);
            if (d >= lookupValues.get(inum).length)
                d = 1; // map out-of-range to unknown
            ld[inum++] = d;
        }
        while (inum < ld.length)
            ld[inum++] = 0;
    }

    public String getKeyValueDescription(boolean inverseDirection, byte[] ab) {
        StringBuilder sb = new StringBuilder(200);
        decode(lookupData, inverseDirection, ab);
        for (int inum = 0; inum < lookupValues.size(); inum++) // loop over lookup names
        {
            BExpressionLookupValue[] va = lookupValues.get(inum);
            String value = va[lookupData[inum]].toString();
            if (value != null && value.length() > 0) {
                if (sb.length() > 0)
                    sb.append(' ');
                sb.append(lookupNames.get(inum) + "=" + value);
            }
        }
        return sb.toString();
    }

    public List<String> getKeyValueList(boolean inverseDirection, byte[] ab) {
        ArrayList<String> res = new ArrayList<String>();
        decode(lookupData, inverseDirection, ab);
        for (int inum = 0; inum < lookupValues.size(); inum++) // loop over lookup names
        {
            BExpressionLookupValue[] va = lookupValues.get(inum);
            String value = va[lookupData[inum]].toString();
            if (value != null && value.length() > 0) {
                res.add(lookupNames.get(inum));
                res.add(value);
            }
        }
        return res;
    }

    public void parseMetaLine(String line) {
        parsedLines++;
        StringTokenizer tk = new StringTokenizer(line, " ");
        String name = tk.nextToken();
        String value = tk.nextToken();
        int idx = name.indexOf(';');
        if (idx >= 0)
            name = name.substring(0, idx);

        if (!fixTagsWritten) {
            fixTagsWritten = true;
            if ("way".equals(context))
                addLookupValue("reversedirection", "yes", null);
            else if ("node".equals(context))
                addLookupValue("nodeaccessgranted", "yes", null);
        }
        if ("reversedirection".equals(name))
            return; // this is hardcoded
        if ("nodeaccessgranted".equals(name))
            return; // this is hardcoded
        BExpressionLookupValue newValue = addLookupValue(name, value, null);

        // add aliases
        while (newValue != null && tk.hasMoreTokens())
            newValue.addAlias(tk.nextToken());
    }

    public void finishMetaParsing() {
        if (parsedLines == 0 && !"global".equals(context)) {
            throw new IllegalArgumentException("lookup table does not contain data for context " + context + " (old version?)");
        }

        // post-process metadata:
        lookupDataFrozen = true;

        lookupIdxUsed = new boolean[lookupValues.size()];
    }

    public final void evaluate(int[] lookupData2) {
        lookupData = lookupData2;
        evaluate();
    }

    private void evaluate() {
        int n = expressionList.size();
        for (int expidx = 0; expidx < n; expidx++) {
            expressionList.get(expidx).evaluate(this);
        }
    }

    public String cacheStats() {
        return "requests=" + requests + " requests2=" + requests2 + " cachemisses=" + cachemisses;
    }

    // @Override
    public final byte[] unify(byte[] ab, int offset, int len) {
        probeCacheNode.ab = null; // crc based cache lookup only
        probeCacheNode.hash = Crc32.crc(ab, offset, len);

        CacheNode cn = (CacheNode) cache.get(probeCacheNode);
        if (cn != null) {
            byte[] cab = cn.ab;
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
        byte[] nab = new byte[len];
        System.arraycopy(ab, offset, nab, 0, len);
        return nab;
    }


    public final void evaluate(boolean inverseDirection, byte[] ab) {
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
            probeCacheNode.hash = Crc32.crc(ab, 0, ab.length);
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
            if (ab == cn.ab)
                requests2++;

            cache.touch(cn);
        }

        currentVars = cn.vars;
        currentVarOffset = inverseDirection ? nBuildInVars : 0;
    }

    private void evaluateInto(float[] vars, int offset) {
        evaluate();
        for (int vi = 0; vi < nBuildInVars; vi++) {
            int idx = buildInVariableIdx[vi];
            vars[vi + offset] = idx == -1 ? 0.f : variableData[idx];
        }
    }


    public void dumpStatistics() {
        TreeMap<String, String> counts = new TreeMap<String, String>();
        // first count
        for (String name : lookupNumbers.keySet()) {
            int cnt = 0;
            int inum = lookupNumbers.get(name).intValue();
            int[] histo = lookupHistograms.get(inum);
//    if ( histo.length == 500 ) continue;
            for (int i = 2; i < histo.length; i++) {
                cnt += histo[i];
            }
            counts.put("" + (1000000000 + cnt) + "_" + name, name);
        }

        while (counts.size() > 0) {
            String key = counts.lastEntry().getKey();
            String name = counts.get(key);
            counts.remove(key);
            int inum = lookupNumbers.get(name).intValue();
            BExpressionLookupValue[] values = lookupValues.get(inum);
            int[] histo = lookupHistograms.get(inum);
            if (values.length == 1000)
                continue;
            String[] svalues = new String[values.length];
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
    public int[] generateRandomValues(Random rnd) {
        int[] data = createNewLookupData();
        data[0] = 2 * rnd.nextInt(2); // reverse-direction = 0 or 2
        for (int inum = 1; inum < data.length; inum++) {
            int nvalues = lookupValues.get(inum).length;
            data[inum] = 0;
            if (inum > 1 && rnd.nextInt(10) > 0)
                continue; // tags other than highway only 10%
            data[inum] = rnd.nextInt(nvalues);
        }
        lookupDataValid = true;
        return data;
    }

    public void assertAllVariablesEqual(BExpressionContext other) {
        int nv = variableData.length;
        int nv2 = other.variableData.length;
        if (nv != nv2)
            throw new RuntimeException("mismatch in variable-count: " + nv + "<->" + nv2);
        for (int i = 0; i < nv; i++) {
            if (variableData[i] != other.variableData[i]) {
                throw new RuntimeException("mismatch in variable " + variableName(i) + " " + variableData[i] + "<->" + other.variableData[i]
                    + "\ntags = " + getKeyValueDescription(false, encode()));
            }
        }
    }

    private String variableName(int idx) {
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
    public BExpressionLookupValue addLookupValue(String name, String value, int[] lookupData2) {
        BExpressionLookupValue newValue = null;
        Integer num = lookupNumbers.get(name);
        if (num == null) {
            if (lookupData2 != null) {
                // do not create unknown name for external data array
                return newValue;
            }

            // unknown name, create
            num = new Integer(lookupValues.size());
            lookupNumbers.put(name, num);
            lookupNames.add(name);
            lookupValues.add(new BExpressionLookupValue[]{new BExpressionLookupValue("")
                , new BExpressionLookupValue("unknown")});
            lookupHistograms.add(new int[2]);
            int[] ndata = new int[lookupData.length + 1];
            System.arraycopy(lookupData, 0, ndata, 0, lookupData.length);
            lookupData = ndata;
        }

        // look for that value
        int inum = num.intValue();
        BExpressionLookupValue[] values = lookupValues.get(inum);
        int[] histo = lookupHistograms.get(inum);
        int i = 0;
        for (; i < values.length; i++) {
            BExpressionLookupValue v = values[i];
            if (v.matches(value))
                break;
        }
        if (i == values.length) {
            if (lookupData2 != null) {
                // do not create unknown value for external data array,
                // record as 'other' instead
                lookupData2[inum] = 1;
                return newValue;
            }

            if (i == 499) {
                // System.out.println( "value limit reached for: " + name );
            }
            if (i == 500) {
                return newValue;
            }
            // unknown value, create
            BExpressionLookupValue[] nvalues = new BExpressionLookupValue[values.length + 1];
            int[] nhisto = new int[values.length + 1];
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
        if (lookupData2 != null)
            lookupData2[inum] = i;
        else
            lookupData[inum] = i;
        return newValue;
    }

    /**
     * add a value-index to to internal array
     * value-index means 0=unknown, 1=other, 2=value-x, ...
     */
    public void addLookupValue(String name, int valueIndex) {
        Integer num = lookupNumbers.get(name);
        if (num == null) {
            return;
        }

        // look for that value
        int inum = num.intValue();
        int nvalues = lookupValues.get(inum).length;
        if (valueIndex < 0 || valueIndex >= nvalues)
            throw new IllegalArgumentException("value index out of range for name " + name + ": " + valueIndex);
        lookupData[inum] = valueIndex;
    }


    /**
     * special hack for yes/proposed relations:
     * add a lookup value if not yet a smaller, &gt; 1 value was added
     * add a 2=yes if the provided value is out of range
     * value-index means here 0=unknown, 1=other, 2=yes, 3=proposed
     */
    public void addSmallestLookupValue(String name, int valueIndex) {
        Integer num = lookupNumbers.get(name);
        if (num == null) {
            return;
        }

        // look for that value
        int inum = num.intValue();
        int nvalues = lookupValues.get(inum).length;
        int oldValueIndex = lookupData[inum];
        if (oldValueIndex > 1 && oldValueIndex < valueIndex) {
            return;
        }
        if (valueIndex >= nvalues) {
            valueIndex = nvalues - 1;
        }
        if (valueIndex < 0)
            throw new IllegalArgumentException("value index out of range for name " + name + ": " + valueIndex);
        lookupData[inum] = valueIndex;
    }

    public boolean getBooleanLookupValue(String name) {
        Integer num = lookupNumbers.get(name);
        return num != null && lookupData[num.intValue()] == 2;
    }

    public int getOutputVariableIndex(String name, boolean mustExist) {
        int idx = getVariableIdx(name, false);
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
        int[] extended = new int[nBuildInVars + 1];
        System.arraycopy(buildInVariableIdx, 0, extended, 0, nBuildInVars);
        extended[nBuildInVars] = idx;
        buildInVariableIdx = extended;
        return nBuildInVars++;
    }

    public void setForeignContext(BExpressionContext foreignContext) {
        this.foreignContext = foreignContext;
    }

    public float getForeignVariableValue(int foreignIndex) {
        return foreignContext.getBuildInVariable(foreignIndex);
    }

    public int getForeignVariableIdx(String context, String name) {
        if (foreignContext == null || !context.equals(foreignContext.context)) {
            throw new IllegalArgumentException("unknown foreign context: " + context);
        }
        return foreignContext.getOutputVariableIndex(name, true);
    }

    public void parseFile(File file, String readOnlyContext) {
        if (!file.exists()) {
            throw new IllegalArgumentException("profile " + file + " does not exist");
        }
        try {
            if (readOnlyContext != null) {
                linenr = 1;
                String realContext = context;
                context = readOnlyContext;
                expressionList = _parseFile(file);
                variableData = new float[variableNumbers.size()];
                evaluate(lookupData); // lookupData is dummy here - evaluate just to create the variables
                context = realContext;
            }
            linenr = 1;
            minWriteIdx = variableData == null ? 0 : variableData.length;

            expressionList = _parseFile(file);

            // determine the build-in variable indices
            String[] varNames = getBuildInVariableNames();
            nBuildInVars = varNames.length;
            buildInVariableIdx = new int[nBuildInVars];
            for (int vi = 0; vi < varNames.length; vi++) {
                buildInVariableIdx[vi] = getVariableIdx(varNames[vi], false);
            }

            float[] readOnlyData = variableData;
            variableData = new float[variableNumbers.size()];
            for (int i = 0; i < minWriteIdx; i++) {
                variableData[i] = readOnlyData[i];
            }
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw new IllegalArgumentException("ParseException at line " + linenr + ": " + e.getMessage());
            }
            throw new RuntimeException(e);
        }
        if (expressionList.size() == 0) {
            throw new IllegalArgumentException(file.getAbsolutePath()
                + " does not contain expressions for context " + context + " (old version?)");
        }
    }

    private List<BExpression> _parseFile(File file) throws Exception {
        _br = new BufferedReader(new FileReader(file));
        _readerDone = false;
        List<BExpression> result = new ArrayList<BExpression>();
        for (; ; ) {
            BExpression exp = BExpression.parse(this, 0);
            if (exp == null)
                break;
            result.add(exp);
        }
        _br.close();
        _br = null;
        return result;
    }


    public float getVariableValue(String name, float defaultValue) {
        Integer num = variableNumbers.get(name);
        return num == null ? defaultValue : getVariableValue(num.intValue());
    }

    float getVariableValue(int variableIdx) {
        return variableData[variableIdx];
    }

    int getVariableIdx(String name, boolean create) {
        Integer num = variableNumbers.get(name);
        if (num == null) {
            if (create) {
                num = new Integer(variableNumbers.size());
                variableNumbers.put(name, num);
            } else {
                return -1;
            }
        }
        return num.intValue();
    }

    int getMinWriteIdx() {
        return minWriteIdx;
    }

    float getLookupMatch(int nameIdx, int[] valueIdxArray) {
        for (int i = 0; i < valueIdxArray.length; i++) {
            if (lookupData[nameIdx] == valueIdxArray[i]) {
                return 1.0f;
            }
        }
        return 0.0f;
    }

    public int getLookupNameIdx(String name) {
        Integer num = lookupNumbers.get(name);
        return num == null ? -1 : num.intValue();
    }

    public final void markLookupIdxUsed(int idx) {
        lookupIdxUsed[idx] = true;
    }

    public final boolean isLookupIdxUsed(int idx) {
        return idx < lookupIdxUsed.length && lookupIdxUsed[idx];
    }

    public final void setAllTagsUsed() {
        for (int i = 0; i < lookupIdxUsed.length; i++) {
            lookupIdxUsed[i] = true;
        }
    }

    int getLookupValueIdx(int nameIdx, String value) {
        BExpressionLookupValue[] values = lookupValues.get(nameIdx);
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value))
                return i;
        }
        return -1;
    }


    String parseToken() throws Exception {
        for (; ; ) {
            String token = _parseToken();
            if (token == null)
                return null;
            if (token.startsWith(CONTEXT_TAG)) {
                _inOurContext = token.substring(CONTEXT_TAG.length()).equals(context);
            } else if (token.startsWith(MODEL_TAG)) {
                _modelClass = token.substring(MODEL_TAG.length()).trim();
            } else if (_inOurContext) {
                return token;
            }
        }
    }


    private String _parseToken() throws Exception {
        StringBuilder sb = new StringBuilder(32);
        boolean inComment = false;
        for (; ; ) {
            int ic = _readerDone ? -1 : _br.read();
            if (ic < 0) {
                if (sb.length() == 0)
                    return null;
                _readerDone = true;
                return sb.toString();
            }
            char c = (char) ic;
            if (c == '\n')
                linenr++;

            if (inComment) {
                if (c == '\r' || c == '\n')
                    inComment = false;
                continue;
            }
            if (Character.isWhitespace(c)) {
                if (sb.length() > 0)
                    return sb.toString();
                else
                    continue;
            }
            if (c == '#' && sb.length() == 0)
                inComment = true;
            else
                sb.append(c);
        }
    }

    float assign(int variableIdx, float value) {
        variableData[variableIdx] = value;
        return value;
    }

}
