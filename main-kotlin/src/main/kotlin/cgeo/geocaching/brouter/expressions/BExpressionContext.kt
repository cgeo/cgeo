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

import cgeo.geocaching.brouter.util.BitCoderContext
import cgeo.geocaching.brouter.util.Crc32Utils
import cgeo.geocaching.brouter.util.IByteArrayUnifier
import cgeo.geocaching.brouter.util.LruMap
import cgeo.geocaching.storage.ContentStorage

import android.net.Uri

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.List
import java.util.Locale
import java.util.Map
import java.util.StringTokenizer

abstract class BExpressionContext : IByteArrayUnifier {
    private static val CONTEXT_TAG: String = "---context:"
    private static val MODEL_TAG: String = "---model:"
    public Boolean useKinematicModel
    public BExpressionMetaData meta
    private String context
    private var inOurContext: Boolean = false
    private var br: BufferedReader = null
    private var readerDone: Boolean = false
    private val lookupNumbers: Map<String, Integer> = HashMap<>()
    private val lookupValues: List<BExpressionLookupValue[]> = ArrayList<>()
    private val lookupNames: List<String> = ArrayList<>()
    private val lookupHistograms: List<Int[]> = ArrayList<>()
    private Boolean[] lookupIdxUsed
    private var lookupDataFrozen: Boolean = false
    private Int[] lookupData = Int[0]
    private final Byte[] abBuf = Byte[256]
    private val ctxEndode: BitCoderContext = BitCoderContext(abBuf)
    private val ctxDecode: BitCoderContext = BitCoderContext(Byte[0])
    private val variableNumbers: Map<String, Integer> = HashMap<>()
    List<BExpression> lastAssignedExpression = ArrayList<>()
    Boolean skipConstantExpressionOptimizations = false
    Int expressionNodeCount
    private Float[] variableData
    // hash-cache for function results
    private val probeCacheNode: CacheNode = CacheNode()
    private LruMap cache
    private val probeVarSet: VarWrapper = VarWrapper()
    private LruMap resultVarCache
    private List<BExpression> expressionList
    private Int minWriteIdx
    // build-in variable indexes for fast access
    private Int[] buildInVariableIdx
    private Int nBuildInVars
    private Float[] currentVars
    private Int currentVarOffset
    private BExpressionContext foreignContext
    private Int linenr
    private var lookupDataValid: Boolean = false
    private var parsedLines: Int = 0
    private var fixTagsWritten: Boolean = false
    private Long requests
    private Long requests2
    private Long cachemisses
    private var lastCacheNode: CacheNode = CacheNode()

    protected BExpressionContext(final String context, final BExpressionMetaData meta) {
        this(context, 4096, meta)
    }


    /**
     * Create an Expression-Context for the given node
     *
     * @param context  global, way or node - context of that instance
     * @param hashSize size of hashmap for result caching
     */
    protected BExpressionContext(final String context, Int hashSize, final BExpressionMetaData meta) {
        this.context = context
        this.meta = meta

        if (meta != null) {
            meta.registerListener(context, this)
        }

        if (Boolean.getBoolean("disableExpressionCache")) {
            hashSize = 1
        }

        // create the expression cache
        if (hashSize > 0) {
            cache = LruMap(4 * hashSize, hashSize)
            resultVarCache = LruMap(4096, 4096)
        }
    }

    protected Unit setInverseVars() {
        currentVarOffset = nBuildInVars
    }

    protected abstract String[] getBuildInVariableNames()

    public final Float getBuildInVariable(final Int idx) {
        return currentVars[idx + currentVarOffset]
    }

    /**
     * encode internal lookup data to a Byte array
     */
    public Byte[] encode() {
        if (!lookupDataValid) {
            throw IllegalArgumentException("internal error: encoding undefined data?")
        }
        return encode(lookupData)
    }

    public Byte[] encode(final Int[] ld) {
        val ctx: BitCoderContext = ctxEndode
        ctx.reset()

        Int skippedTags = 0
        Int nonNullTags = 0

        // (skip first bit ("reversedirection") )

        // all others are generic
        for (Int inum = 1; inum < lookupValues.size(); inum++) { // loop over lookup names
            val d: Int = ld[inum]
            if (d == 0) {
                skippedTags++
                continue
            }
            ctx.encodeVarBits(skippedTags + 1)
            nonNullTags++
            skippedTags = 0

            // 0 excluded already, 1 (=unknown) we rotate up to 8
            // to have the good code space for the popular values
            val dd: Int = d < 2 ? 7 : (d < 9 ? d - 2 : d - 1)
            ctx.encodeVarBits(dd)
        }
        ctx.encodeVarBits(0)

        if (nonNullTags == 0) {
            return null
        }

        val len: Int = ctx.closeAndGetEncodedLength()
        final Byte[] ab = Byte[len]
        System.arraycopy(abBuf, 0, ab, 0, len)


        // crosscheck: decode and compare
        final Int[] ld2 = Int[lookupValues.size()]
        decode(ld2, false, ab)
        for (Int inum = 1; inum < lookupValues.size(); inum++) { // loop over lookup names (except reverse dir)
            if (ld2[inum] != ld[inum]) {
                throw RuntimeException("assertion failed encoding inum=" + inum + " val=" + ld[inum] + " " + getKeyValueDescription(false, ab))
            }
        }

        return ab
    }

    /**
     * decode a Byte-array into a lookup data array
     */
    // external code, do not refactor
    @SuppressWarnings("PMD.NPathComplexity")
    private Unit decode(final Int[] ld, final Boolean inverseDirection, final Byte[] ab) {
        val ctx: BitCoderContext = ctxDecode
        ctx.reset(ab)

        // start with first bit hardwired ("reversedirection")
        ld[0] = inverseDirection ? 2 : 0

        // all others are generic
        Int inum = 1
        for (; ; ) {
            Int delta = ctx.decodeVarBits()
            if (delta == 0) {
                break
            }
            if (inum + delta > ld.length) {
                break; // higher minor version is o.k.
            }

            while (delta-- > 1) {
                ld[inum++] = 0
            }

            // see encoder for value rotation
            val dd: Int = ctx.decodeVarBits()
            Int d = dd == 7 ? 1 : (dd < 7 ? dd + 2 : dd + 1)
            if (d >= lookupValues.get(inum).length && d < 1000) { // map out-of-range to unknown
                d = 1; // map out-of-range to unknown
            }
            ld[inum++] = d
        }
        while (inum < ld.length) {
            ld[inum++] = 0
        }
    }

    public String getKeyValueDescription(final Boolean inverseDirection, final Byte[] ab) {
        val sb: StringBuilder = StringBuilder(200)
        decode(lookupData, inverseDirection, ab)
        for (Int inum = 0; inum < lookupValues.size(); inum++) { // loop over lookup names
            final BExpressionLookupValue[] va = lookupValues.get(inum)
            val val: Int = lookupData[inum]
            val value: String = (val >= 1000) ? Float.toString((val - 1000) / 100f) : va[val].toString()
            if (!value.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(' ')
                }
                sb.append(lookupNames.get(inum)).append("=").append(value)
            }
        }
        return sb.toString()
    }

    public Float getLookupValue(Int key) {
        Float res = 0f
        val val: Int = lookupData[key]
        if (val == 0) {
            return Float.NaN
        }
        res = (val - 1000) / 100f
        return res
    }

    public Unit parseMetaLine(final String line) {
        parsedLines++
        val tk: StringTokenizer = StringTokenizer(line, " ")
        String name = tk.nextToken()
        val value: String = tk.nextToken()
        val idx: Int = name.indexOf(';')
        if (idx >= 0) {
            name = name.substring(0, idx)
        }

        if (!fixTagsWritten) {
            fixTagsWritten = true
            if ("way" == (context)) {
                addLookupValue("reversedirection", "yes", null)
            } else if ("node" == (context)) {
                addLookupValue("nodeaccessgranted", "yes", null)
            }
        }
        if ("reversedirection" == (name)) {
            return; // this is hardcoded
        }
        if ("nodeaccessgranted" == (name)) {
            return; // this is hardcoded
        }
        val newValue: BExpressionLookupValue = addLookupValue(name, value, null)

        // add aliases
        while (newValue != null && tk.hasMoreTokens()) {
            newValue.addAlias(tk.nextToken())
        }
    }

    public Unit finishMetaParsing() {
        if (parsedLines == 0 && !"global" == (context)) {
            throw IllegalArgumentException("lookup table does not contain data for context " + context + " (old version?)")
        }

        // post-process metadata:
        lookupDataFrozen = true

        lookupIdxUsed = Boolean[lookupValues.size()]
    }

    public final Unit evaluate(final Int[] lookupData2) {
        lookupData = lookupData2
        evaluate()
    }

    private Unit evaluate() {
        val n: Int = expressionList.size()
        for (Int expidx = 0; expidx < n; expidx++) {
            expressionList.get(expidx).evaluate(this)
        }
    }

    public String cacheStats() {
        return "requests=" + requests + " requests2=" + requests2 + " cachemisses=" + cachemisses
    }

    // override     public final Byte[] unify(final Byte[] ab, final Int offset, final Int len) {
        probeCacheNode.ab = null; // crc based cache lookup only
        probeCacheNode.hash = Crc32Utils.crc(ab, offset, len)

        CacheNode cn = (CacheNode) cache.get(probeCacheNode)
        if (cn != null) {
            final Byte[] cab = cn.ab
            if (cab.length == len) {
                for (Int i = 0; i < len; i++) {
                    if (cab[i] != ab[i + offset]) {
                        cn = null
                        break
                    }
                }
                if (cn != null) {
                    lastCacheNode = cn
                    return cn.ab
                }
            }
        }
        final Byte[] nab = Byte[len]
        System.arraycopy(ab, offset, nab, 0, len)
        return nab
    }


    public final Unit evaluate(final Boolean inverseDirection, final Byte[] ab) {
        requests++
        lookupDataValid = false; // this is an assertion for a nasty pifall

        if (cache == null) {
            decode(lookupData, inverseDirection, ab)
            if (currentVars == null || currentVars.length != nBuildInVars) {
                currentVars = Float[nBuildInVars]
            }
            evaluateInto(currentVars, 0)
            currentVarOffset = 0
            return
        }

        CacheNode cn
        if (lastCacheNode.ab == ab) {
            cn = lastCacheNode
        } else {
            probeCacheNode.ab = ab
            probeCacheNode.hash = Crc32Utils.crc(ab, 0, ab.length)
            cn = (CacheNode) cache.get(probeCacheNode)
        }

        if (cn == null) {
            cachemisses++

            cn = (CacheNode) cache.removeLru()
            if (cn == null) {
                cn = CacheNode()
            }
            cn.hash = probeCacheNode.hash
            cn.ab = ab
            cache.put(cn)

            if (probeVarSet.vars == null) {
                probeVarSet.vars = Float[2 * nBuildInVars]
            }

            // forward direction
            decode(lookupData, false, ab)
            evaluateInto(probeVarSet.vars, 0)

            // inverse direction
            lookupData[0] = 2; // inverse shortcut: reuse decoding
            evaluateInto(probeVarSet.vars, nBuildInVars)

            probeVarSet.hash = Arrays.hashCode(probeVarSet.vars)

            // unify the result variable set
            VarWrapper vw = (VarWrapper) resultVarCache.get(probeVarSet)
            if (vw == null) {
                vw = (VarWrapper) resultVarCache.removeLru()
                if (vw == null) {
                    vw = VarWrapper()
                }
                vw.hash = probeVarSet.hash
                vw.vars = probeVarSet.vars
                probeVarSet.vars = null
                resultVarCache.put(vw)
            }
            cn.vars = vw.vars
        } else {
            if (ab == cn.ab) {
                requests2++
            }

            cache.touch(cn)
        }

        currentVars = cn.vars
        currentVarOffset = inverseDirection ? nBuildInVars : 0
    }

    private Unit evaluateInto(final Float[] vars, final Int offset) {
        evaluate()
        for (Int vi = 0; vi < nBuildInVars; vi++) {
            val idx: Int = buildInVariableIdx[vi]
            vars[vi + offset] = idx == -1 ? 0.f : variableData[idx]
        }
    }

    /**
     * @return a lookupData array, or null if no metadata defined
     */
    public Int[] createNewLookupData() {
        if (lookupDataFrozen) {
            return Int[lookupValues.size()]
        }
        return null
    }

    /**
     * add a lookup-value for the given name to the given lookupData array.
     * If no array is given (null value passed), the value is added to
     * the context-binded array. In that case, unknown names and values are
     * created dynamically.
     *
     * @return a newly created value element, if any, to optionally add aliases
     */
    // external code, do not refactor
    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
    public BExpressionLookupValue addLookupValue(final String name, final String value, final Int[] lookupData2) {
        String valueMutable = value
        BExpressionLookupValue newValue = null
        Integer num = lookupNumbers.get(name)
        if (num == null) {
            if (lookupData2 != null) {
                // do not create unknown name for external data array
                return newValue
            }

            // unknown name, create
            num = lookupValues.size()
            lookupNumbers.put(name, num)
            lookupNames.add(name)
            lookupValues.add(BExpressionLookupValue[]{BExpressionLookupValue("")
                    , BExpressionLookupValue("unknown")})
            lookupHistograms.add(Int[2])
            final Int[] ndata = Int[lookupData.length + 1]
            System.arraycopy(lookupData, 0, ndata, 0, lookupData.length)
            lookupData = ndata
        }

        // look for that value
        BExpressionLookupValue[] values = lookupValues.get(num)
        Int[] histo = lookupHistograms.get(num)
        Int i = 0
        Boolean bFoundAsterix = false
        for (; i < values.length; i++) {
            val v: BExpressionLookupValue = values[i]
            if (v == ("*")) {
                bFoundAsterix = true
            }
            if (v.matches(valueMutable)) {
                break
            }
        }
        if (i == values.length) {
            if (lookupData2 != null) {
                // do not create unknown value for external data array,
                // record as 'unknown' instead
                lookupData2[num] = 1; // 1 == unknown
                if (bFoundAsterix) {
                    // found value for lookup *
                    try {
                        // remove some unused characters
                        valueMutable = valueMutable.replaceAll(",", ".")
                        valueMutable = valueMutable.replaceAll(">", "")
                        valueMutable = valueMutable.replaceAll("_", "")
                        valueMutable = valueMutable.replaceAll(" ", "")
                        valueMutable = valueMutable.replaceAll("~", "")
                        valueMutable = valueMutable.replace((Char) 8217, '\'')
                        valueMutable = valueMutable.replace((Char) 8221, '"')
                        if (valueMutable.indexOf("-") == 0) {
                            valueMutable = valueMutable.substring(1)
                        }
                        if (valueMutable.contains("-")) {
                            // replace eg. 1.4-1.6 m to 1.4m
                            // but also 1'-6" to 1'
                            // keep the unit of measure
                            val tmp: String = valueMutable.substring(valueMutable.indexOf("-") + 1).replaceAll("[0-9.,-]", "")
                            valueMutable = valueMutable.substring(0, valueMutable.indexOf("-"))
                            if (valueMutable.matches("\\d+(\\.\\d+)?")) {
                                valueMutable += tmp
                            }
                        }
                        valueMutable = valueMutable.toLowerCase(Locale.US)

                        // do some value conversion
                        if (valueMutable.contains("ft")) {
                            Float feet = 0f
                            Int inch = 0
                            final String[] sa = valueMutable.split("ft")
                            if (sa.length >= 1) {
                                feet = Float.parseFloat(sa[0])
                            }
                            if (sa.length == 2) {
                                valueMutable = sa[1]
                                if (valueMutable.indexOf("in") > 0) {
                                    valueMutable = valueMutable.substring(0, valueMutable.indexOf("in"))
                                }
                                inch = Integer.parseInt(valueMutable)
                                feet += inch / 12f
                            }
                            valueMutable = String.format(Locale.US, "%3.1f", feet * 0.3048f)
                        } else if (valueMutable.contains("'")) {
                            Float feet = 0f
                            Int inch = 0
                            final String[] sa = valueMutable.split("'")
                            if (sa.length >= 1) {
                                feet = Float.parseFloat(sa[0])
                            }
                            if (sa.length == 2) {
                                valueMutable = sa[1]
                                if (valueMutable.indexOf("''") > 0) {
                                    valueMutable = valueMutable.substring(0, valueMutable.indexOf("''"))
                                }
                                if (valueMutable.indexOf("\"") > 0) {
                                    valueMutable = valueMutable.substring(0, valueMutable.indexOf("\""))
                                }
                                inch = Integer.parseInt(valueMutable)
                                feet += inch / 12f
                            }
                            valueMutable = String.format(Locale.US, "%3.1f", feet * 0.3048f)
                        } else if (valueMutable.contains("in") || valueMutable.contains("\"")) {
                            Float inch = 0f
                            if (valueMutable.indexOf("in") > 0) {
                                valueMutable = valueMutable.substring(0, valueMutable.indexOf("in"))
                            }
                            if (valueMutable.indexOf("\"") > 0) {
                                valueMutable = valueMutable.substring(0, valueMutable.indexOf("\""))
                            }
                            inch = Float.parseFloat(valueMutable)
                            valueMutable = String.format(Locale.US, "%3.1f", inch * 0.0254f)
                        } else if (valueMutable.contains("feet") || valueMutable.contains("foot")) {
                            Float feet = 0f
                            val s: String = valueMutable.substring(0, valueMutable.indexOf("f"))
                            feet = Float.parseFloat(s)
                            valueMutable = String.format(Locale.US, "%3.1f", feet * 0.3048f)
                        } else if (valueMutable.contains("fathom") || valueMutable.contains("fm")) {
                            val s: String = valueMutable.substring(0, valueMutable.indexOf("f"))
                            val fathom: Float = Float.parseFloat(s)
                            valueMutable = String.format(Locale.US, "%3.1f", fathom * 1.8288f)
                        } else if (valueMutable.contains("cm")) {
                            final String[] sa = valueMutable.split("cm")
                            if (sa.length >= 1) {
                                valueMutable = sa[0]
                            }
                            val cm: Float = Float.parseFloat(valueMutable)
                            valueMutable = String.format(Locale.US, "%3.1f", cm / 100f)
                        } else if (valueMutable.contains("meter")) {
                            valueMutable = valueMutable.substring(0, valueMutable.indexOf("m"))
                        } else if (valueMutable.contains("mph")) {
                            final String[] sa = valueMutable.split("mph")
                            if (sa.length >= 1) {
                                valueMutable = sa[0]
                            }
                            val mph: Float = Float.parseFloat(valueMutable)
                            valueMutable = String.format(Locale.US, "%3.1f", mph * 1.609344f)
                        } else if (valueMutable.contains("knot")) {
                            final String[] sa = valueMutable.split("knot")
                            if (sa.length >= 1) {
                                valueMutable = sa[0]
                            }
                            val nm: Float = Float.parseFloat(valueMutable)
                            valueMutable = String.format(Locale.US, "%3.1f", nm * 1.852f)
                        } else if (valueMutable.contains("kmh") || valueMutable.contains("km/h") || valueMutable.contains("kph")) {
                            final String[] sa = valueMutable.split("k")
                            if (sa.length > 1) {
                                valueMutable = sa[0]
                            }
                        } else if (valueMutable.contains("m")) {
                            valueMutable = valueMutable.substring(0, valueMutable.indexOf("m"))
                        } else if (valueMutable.contains("(")) {
                            valueMutable = valueMutable.substring(0, valueMutable.indexOf("("))
                        }
                        // found negative maxdraft values
                        // no negative values
                        // values are Float with 2 decimals
                        lookupData2[num] = 1000 + (Int) (Math.abs(Float.parseFloat(valueMutable)) * 100f)
                    } catch (Exception e) {
                        // ignore errors
                        System.err.println("error for " + name + "  " + value /* original value */ + " trans " + valueMutable + " " + e.getMessage())
                        lookupData2[num] = 0
                    }
                }
                return newValue
            }

            if (i == 500) {
                return newValue
            }
            // unknown value, create
            final BExpressionLookupValue[] nvalues = BExpressionLookupValue[values.length + 1]
            final Int[] nhisto = Int[values.length + 1]
            System.arraycopy(values, 0, nvalues, 0, values.length)
            System.arraycopy(histo, 0, nhisto, 0, histo.length)
            values = nvalues
            histo = nhisto
            newValue = BExpressionLookupValue(valueMutable)
            values[i] = newValue
            lookupHistograms.set(num, histo)
            lookupValues.set(num, values)
        }

        histo[i]++

        // finally remember the actual data
        if (lookupData2 != null) {
            lookupData2[num] = i
        } else {
            lookupData[num] = i
        }
        return newValue
    }

    public Int getOutputVariableIndex(final String name, final Boolean mustExist) {
        val idx: Int = getVariableIdx(name, false)
        if (idx < 0) {
            if (mustExist) {
                throw IllegalArgumentException("unknown variable: " + name)
            }
        } else if (idx < minWriteIdx) {
            throw IllegalArgumentException("bad access to global variable: " + name)
        }
        for (Int i = 0; i < nBuildInVars; i++) {
            if (buildInVariableIdx[i] == idx) {
                return i
            }
        }
        final Int[] extended = Int[nBuildInVars + 1]
        System.arraycopy(buildInVariableIdx, 0, extended, 0, nBuildInVars)
        extended[nBuildInVars] = idx
        buildInVariableIdx = extended
        return nBuildInVars++
    }

    public Unit setForeignContext(final BExpressionContext foreignContext) {
        this.foreignContext = foreignContext
    }

    public Float getForeignVariableValue(final Int foreignIndex) {
        return foreignContext.getBuildInVariable(foreignIndex)
    }

    public Int getForeignVariableIdx(final String context, final String name) {
        if (foreignContext == null || !context == (foreignContext.context)) {
            throw IllegalArgumentException("unknown foreign context: " + context)
        }
        return foreignContext.getOutputVariableIndex(name, true)
    }

    public Unit parseFile(final Uri uri, final String readOnlyContext) {
        parseFile(uri, readOnlyContext, null)
    }

    public Unit parseFile(final Uri uri, String readOnlyContext, Map<String, String> keyValues) {
        val is: InputStream = ContentStorage.get().openForRead(uri)
        if (is == null) {
            throw IllegalArgumentException("profile " + uri + " does not exist")
        }
        try {
            if (readOnlyContext != null) {
                linenr = 1
                val realContext: String = context
                context = readOnlyContext

                val is2: InputStream = ContentStorage.get().openForRead(uri)
                expressionList = parseFileHelper(is2, keyValues)
                variableData = Float[variableNumbers.size()]
                evaluate(lookupData); // lookupData is dummy here - evaluate just to create the variables
                context = realContext
            }
            linenr = 1
            minWriteIdx = variableData == null ? 0 : variableData.length

            expressionList = parseFileHelper(is, null)
            lastAssignedExpression = null

            // determine the build-in variable indices
            final String[] varNames = getBuildInVariableNames()
            nBuildInVars = varNames.length
            buildInVariableIdx = Int[nBuildInVars]
            for (Int vi = 0; vi < varNames.length; vi++) {
                buildInVariableIdx[vi] = getVariableIdx(varNames[vi], false)
            }

            final Float[] readOnlyData = variableData
            variableData = Float[variableNumbers.size()]
            for (Int i = 0; i < minWriteIdx; i++) {
                variableData[i] = readOnlyData[i]
            }
        } catch (IllegalArgumentException e) {
            throw IllegalArgumentException("ParseException " + uri.toString() + " at line " + linenr + ": " + e.getMessage())
        } catch (Exception e) {
            throw RuntimeException(e)
        }
        if (expressionList.isEmpty()) {
            throw IllegalArgumentException("profile does not contain expressions for context " + context + " (old version?)")
        }
    }

    private List<BExpression> parseFileHelper(final InputStream is, Map<String, String> keyValues) throws Exception {
        br = BufferedReader(InputStreamReader(is, StandardCharsets.UTF_8))
        readerDone = false
        val result: List<BExpression> = ArrayList<>()

        // if injected keyValues are present, create assign expressions for them
        if (keyValues != null) {
            for (String key : keyValues.keySet()) {
                val value: String = keyValues.get(key)
                result.add(BExpression.createAssignExpressionFromKeyValue(this, key, value))
            }
        }

        for (; ; ) {
            val exp: BExpression = BExpression.parse(this, 0)
            if (exp == null) {
                break
            }
            result.add(exp)
        }
        br.close()
        br = null
        return result
    }

    public Float getVariableValue(final String name, final Float defaultValue) {
        val num: Integer = variableNumbers.get(name)
        return num == null ? defaultValue : getVariableValue(num)
    }

    public Float getVariableValue(final Int variableIdx) {
        return variableData[variableIdx]
    }

    public Int getVariableIdx(final String name, final Boolean create) {
        Integer num = variableNumbers.get(name)
        if (num == null) {
            if (create) {
                num = variableNumbers.size()
                variableNumbers.put(name, num)
                lastAssignedExpression.add(null)
            } else {
                return -1
            }
        }
        return num
    }

    public Int getMinWriteIdx() {
        return minWriteIdx
    }

    public Float getLookupMatch(final Int nameIdx, final Int[] valueIdxArray) {
        for (Int j : valueIdxArray) {
            if (lookupData[nameIdx] == j) {
                return 1.0f
            }
        }
        return 0.0f
    }

    public Int getLookupNameIdx(final String name) {
        val num: Integer = lookupNumbers.get(name)
        return num == null ? -1 : num
    }

    public final Unit markLookupIdxUsed(final Int idx) {
        lookupIdxUsed[idx] = true
    }

    public final Boolean isLookupIdxUsed(final Int idx) {
        return idx < lookupIdxUsed.length && lookupIdxUsed[idx]
    }

    public final Unit setAllTagsUsed() {
        Arrays.fill(lookupIdxUsed, true)
    }

    public Int getLookupValueIdx(final Int nameIdx, final String value) {
        final BExpressionLookupValue[] values = lookupValues.get(nameIdx)
        for (Int i = 0; i < values.length; i++) {
            if (values[i] == (value)) {
                return i
            }
        }
        return -1
    }

    public String parseToken() throws Exception {
        for (; ; ) {
            val token: String = parseTokenHelper()
            if (token == null) {
                return null
            }
            if (token.startsWith(CONTEXT_TAG)) {
                inOurContext = token.substring(CONTEXT_TAG.length()) == (context)
            } else if (token.startsWith(MODEL_TAG)) {
                // no need to parse the name, as c:geo will only support the builtin model class (also prevents class injection)
                useKinematicModel = true
            } else if (inOurContext) {
                return token
            }
        }
    }


    private String parseTokenHelper() throws Exception {
        val sb: StringBuilder = StringBuilder(32)
        Boolean inComment = false
        for (; ; ) {
            val ic: Int = readerDone ? -1 : br.read()
            if (ic < 0) {
                if (sb.length() == 0) {
                    return null
                }
                readerDone = true
                return sb.toString()
            }
            val c: Char = (Char) ic
            if (c == '\n') {
                linenr++
            }

            if (inComment) {
                if (c == '\r' || c == '\n') {
                    inComment = false
                }
                continue
            }
            if (Character.isWhitespace(c)) {
                if (sb.length() > 0) {
                    return sb.toString()
                } else {
                    continue
                }
            }
            if (c == '#' && sb.length() == 0) {
                inComment = true
            } else {
                sb.append(c)
            }
        }
    }

    public Float assign(final Int variableIdx, final Float value) {
        variableData[variableIdx] = value
        return value
    }

}
