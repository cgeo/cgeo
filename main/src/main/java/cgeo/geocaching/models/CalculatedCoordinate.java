package cgeo.geocaching.models;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.utils.TextParser;
import cgeo.geocaching.utils.formulas.DegreeFormula;
import cgeo.geocaching.utils.formulas.Value;
import cgeo.geocaching.utils.functions.Func1;
import static cgeo.geocaching.models.CalculatedCoordinateType.PLAIN;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutableTriple;

public class CalculatedCoordinate implements Parcelable {

    //Config String: {CC|<latPatern>|<lonPattern>|<type-char>}
    public static final String CONFIG_KEY = "CC";

    private static final DegreeFormula EMPTY_FORMULA = DegreeFormula.compile("", false);

    private CalculatedCoordinateType type = PLAIN;
    private DegreeFormula latitudePattern = EMPTY_FORMULA;
    private DegreeFormula longitudePattern = EMPTY_FORMULA;

    public CalculatedCoordinate() {
        //empty on purpose
    }

    protected CalculatedCoordinate(final Parcel in) {
        type = CalculatedCoordinateType.values()[in.readInt()];
        setLatitudePattern(in.readString());
        setLongitudePattern(in.readString());
    }

    public static final Creator<CalculatedCoordinate> CREATOR = new Creator<CalculatedCoordinate>() {
        @Override
        public CalculatedCoordinate createFromParcel(final Parcel in) {
            return new CalculatedCoordinate(in);
        }

        @Override
        public CalculatedCoordinate[] newArray(final int size) {
            return new CalculatedCoordinate[size];
        }
    };

    public CalculatedCoordinateType getType() {
        return type;
    }

    public void setType(final CalculatedCoordinateType type) {
        this.type = type == null ? PLAIN : type;
    }

    public String getLatitudePattern() {
        return latitudePattern.getExpression();
    }

    public void setLatitudePattern(final String latitudePattern) {
        this.latitudePattern = latitudePattern == null ? EMPTY_FORMULA : DegreeFormula.compile(latitudePattern, false);
    }

    public String getLongitudePattern() {
        return longitudePattern.getExpression();
    }

    public void setLongitudePattern(final String longitudePattern) {
        this.longitudePattern = longitudePattern == null ? EMPTY_FORMULA : DegreeFormula.compile(longitudePattern, true);
    }

    public void setFrom(final CalculatedCoordinate other) {
        this.type = other.type;
        this.latitudePattern = other.latitudePattern;
        this.longitudePattern = other.longitudePattern;
    }

    public int setFromConfig(final String config) {
        if (config == null) {
            return -1;
        }
        final String configToUse = config.trim();
        if (!configToUse.startsWith("{" + CONFIG_KEY + "|")) {
            return -1;
        }

        final TextParser tp = new TextParser(config);
        tp.parseUntil('|');
        final List<String> tokens = tp.splitUntil(c -> c == '}', c -> c == '|', false, '\\', false);
        setLatitudePattern(tokens.size() > 0 ? tokens.get(0) : "");
        setLongitudePattern(tokens.size() > 1 ? tokens.get(1) : "");
        this.type = CalculatedCoordinateType.fromName(tokens.size() > 2 ? tokens.get(2) : PLAIN.shortName());
        return tp.pos();
    }

    public String toConfig() {
        return "{" + CONFIG_KEY +
                "|" + TextParser.escape(latitudePattern.getExpression(), c -> c == '}' || c == '|', '\\') +
                '|' + TextParser.escape(longitudePattern.getExpression(), c -> c == '}' || c == '|', '\\') +
                (type != null && PLAIN != type ? '|' + type.shortName() : "") +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeInt(type.ordinal());
        dest.writeString(latitudePattern.getExpression());
        dest.writeString(longitudePattern.getExpression());
    }

    public static boolean isValidConfig(final String configCandidate) {
        return createFromConfig(configCandidate).isFilled();
    }

    public boolean isFilled() {
        return latitudePattern != EMPTY_FORMULA || longitudePattern != EMPTY_FORMULA;
    }

    public static CalculatedCoordinate createFromConfig(final String config) {
        final CalculatedCoordinate state = new CalculatedCoordinate();
        state.setFromConfig(config);
        return state;
    }

    public Set<String> getNeededVars() {
        final Set<String> neededVars = new HashSet<>();
        neededVars.addAll(latitudePattern.getNeededVars());
        neededVars.addAll(longitudePattern.getNeededVars());
        return neededVars;
    }

    @Nullable
    public Geopoint calculateGeopoint(final Func1<String, Value> varMap) {
        final ImmutableTriple<Double, CharSequence, Boolean> latData = calculateLatitudeData(varMap);
        final ImmutableTriple<Double, CharSequence, Boolean> lonData = calculateLongitudeData(varMap);
        return latData.left == null || lonData.left == null ? null : new Geopoint(latData.left, lonData.left);
    }

    @NonNull
    public ImmutableTriple<Double, CharSequence, Boolean> calculateLatitudeData(final Func1<String, Value> varMap) {
        return latitudePattern.evaluate(varMap);
    }

    public ImmutableTriple<Double, CharSequence, Boolean> calculateLongitudeData(final Func1<String, Value> varMap) {
        return longitudePattern.evaluate(varMap);
    }

    @Override
    public String toString() {
        return toConfig();
    }

}
