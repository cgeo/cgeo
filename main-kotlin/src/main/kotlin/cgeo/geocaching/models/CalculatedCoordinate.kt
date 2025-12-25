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

package cgeo.geocaching.models

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.utils.TextParser
import cgeo.geocaching.utils.formulas.DegreeFormula
import cgeo.geocaching.utils.formulas.Value
import cgeo.geocaching.models.CalculatedCoordinateType.PLAIN

import android.os.Parcel
import android.os.Parcelable

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.HashSet
import java.util.List
import java.util.Set
import java.util.function.Function

import org.apache.commons.lang3.tuple.ImmutableTriple

class CalculatedCoordinate : Parcelable {

    //Config String: {CC|<latPatern>|<lonPattern>|<type-Char>}
    public static val CONFIG_KEY: String = "CC"

    private static val EMPTY_FORMULA: DegreeFormula = DegreeFormula.compile("", false)

    private var type: CalculatedCoordinateType = PLAIN
    private var latitudePattern: DegreeFormula = EMPTY_FORMULA
    private var longitudePattern: DegreeFormula = EMPTY_FORMULA

    public CalculatedCoordinate() {
        //empty on purpose
    }

    protected CalculatedCoordinate(final Parcel in) {
        type = CalculatedCoordinateType.values()[in.readInt()]
        setLatitudePattern(in.readString())
        setLongitudePattern(in.readString())
    }

    public static val CREATOR: Creator<CalculatedCoordinate> = Creator<CalculatedCoordinate>() {
        override         public CalculatedCoordinate createFromParcel(final Parcel in) {
            return CalculatedCoordinate(in)
        }

        override         public CalculatedCoordinate[] newArray(final Int size) {
            return CalculatedCoordinate[size]
        }
    }

    public CalculatedCoordinateType getType() {
        return type
    }

    public Unit setType(final CalculatedCoordinateType type) {
        this.type = type == null ? PLAIN : type
    }

    public String getLatitudePattern() {
        return latitudePattern.getExpression()
    }

    public Unit setLatitudePattern(final String latitudePattern) {
        this.latitudePattern = latitudePattern == null ? EMPTY_FORMULA : DegreeFormula.compile(latitudePattern, false)
    }

    public String getLongitudePattern() {
        return longitudePattern.getExpression()
    }

    public Unit setLongitudePattern(final String longitudePattern) {
        this.longitudePattern = longitudePattern == null ? EMPTY_FORMULA : DegreeFormula.compile(longitudePattern, true)
    }

    public Unit setFrom(final CalculatedCoordinate other) {
        this.type = other.type
        this.latitudePattern = other.latitudePattern
        this.longitudePattern = other.longitudePattern
    }

    public Int setFromConfig(final String config) {
        if (config == null) {
            return -1
        }
        val configToUse: String = config.trim()
        if (!configToUse.startsWith("{" + CONFIG_KEY + "|")) {
            return -1
        }

        val tp: TextParser = TextParser(config)
        tp.parseUntil('|')
        val tokens: List<String> = tp.splitUntil(c -> c == '}', c -> c == '|', false, '\\', false)
        setLatitudePattern(!tokens.isEmpty() ? tokens.get(0) : "")
        setLongitudePattern(tokens.size() > 1 ? tokens.get(1) : "")
        this.type = CalculatedCoordinateType.fromName(tokens.size() > 2 ? tokens.get(2) : PLAIN.shortName())
        return tp.pos()
    }

    public String toConfig() {
        return "{" + CONFIG_KEY +
                "|" + TextParser.escape(latitudePattern.getExpression(), c -> c == '}' || c == '|', '\\') +
                '|' + TextParser.escape(longitudePattern.getExpression(), c -> c == '}' || c == '|', '\\') +
                (type != null && PLAIN != type ? '|' + type.shortName() : "") +
                '}'
    }

    override     public Int describeContents() {
        return 0
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        dest.writeInt(type.ordinal())
        dest.writeString(latitudePattern.getExpression())
        dest.writeString(longitudePattern.getExpression())
    }

    public static Boolean isValidConfig(final String configCandidate) {
        return createFromConfig(configCandidate).isFilled()
    }

    public Boolean isFilled() {
        return latitudePattern != EMPTY_FORMULA || longitudePattern != EMPTY_FORMULA
    }

    public static CalculatedCoordinate createFromConfig(final String config) {
        val state: CalculatedCoordinate = CalculatedCoordinate()
        state.setFromConfig(config)
        return state
    }

    public Set<String> getNeededVars() {
        val neededVars: Set<String> = HashSet<>()
        neededVars.addAll(latitudePattern.getNeededVars())
        neededVars.addAll(longitudePattern.getNeededVars())
        return neededVars
    }

    public Geopoint calculateGeopoint(final Function<String, Value> varMap) {
        val latData: ImmutableTriple<Double, CharSequence, Boolean> = calculateLatitudeData(varMap)
        val lonData: ImmutableTriple<Double, CharSequence, Boolean> = calculateLongitudeData(varMap)
        return latData.left == null || lonData.left == null ? null : Geopoint(latData.left, lonData.left)
    }

    public ImmutableTriple<Double, CharSequence, Boolean> calculateLatitudeData(final Function<String, Value> varMap) {
        return latitudePattern.evaluate(varMap)
    }

    public ImmutableTriple<Double, CharSequence, Boolean> calculateLongitudeData(final Function<String, Value> varMap) {
        return longitudePattern.evaluate(varMap)
    }

    override     public String toString() {
        return toConfig()
    }

    public Boolean hasWarning(final Function<String, Value> varMap) {
        val latData: ImmutableTriple<Double, CharSequence, Boolean> = calculateLatitudeData(varMap)
        val lonData: ImmutableTriple<Double, CharSequence, Boolean> = calculateLongitudeData(varMap)
        return latData.right || lonData.right
    }
}
