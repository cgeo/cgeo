package cgeo.geocaching.models;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.utils.formulas.DegreeFormula;
import cgeo.geocaching.utils.formulas.Value;
import cgeo.geocaching.utils.functions.Func1;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.EnumUtils;

public class CalculatedCoordinate implements Parcelable {

    //Status: type, lonPattern, latPattern

    //setFromGeopoint -> generiert die patterns entsprechend des typs
    //setFromConfig -> setzt type, lonPattern, latPattern
    //changeType(varList) -> setzt type. rechnet patterns um (mit zwischenschritt "calcGeopoint", dafür braucht es die varList)
    //setLat/LonPattern -> setzt die patterns
    //calculateGeopoint(varList) -> versucht einen Geopoint zu berechnen
    //getLat/LonString(varList) (mit Errorhandling) -> berechnet ein displayable string gemäß pattern mit error-infos

    private static final DegreeFormula EMPTY_FORMULA = DegreeFormula.compile("");

    private CalculatedCoordinateType type = CalculatedCoordinateType.PLAIN;
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
        this.type = type == null ? CalculatedCoordinateType.PLAIN : type;
    }

    public String getLatitudePattern() {
        return latitudePattern.getExpression();
    }

    public void setLatitudePattern(final String latitudePattern) {
        this.latitudePattern = latitudePattern == null ? EMPTY_FORMULA : DegreeFormula.compile(latitudePattern);
    }

    public String getLongitudePattern() {
        return longitudePattern.getExpression();
    }

    public void setLongitudePattern(final String longitudePattern) {
        this.longitudePattern = longitudePattern == null ? EMPTY_FORMULA : DegreeFormula.compile(longitudePattern);
    }

    public void setFrom(final CalculatedCoordinate other) {
        this.type = other.type;
        this.latitudePattern = other.latitudePattern;
        this.longitudePattern = other.longitudePattern;
    }

    public void setFromConfig(final String config) {
        if (config == null) {
            return;
        }
        final String[] tokens = config.split("::");
        this.type = EnumUtils.getEnum(CalculatedCoordinateType.class, tokens.length > 0 ? tokens[0] : null, CalculatedCoordinateType.PLAIN);
        setLatitudePattern(tokens.length > 1 ? tokens[1] : "");
        setLongitudePattern(tokens.length > 2 ? tokens[2] : "");
    }

    public String toConfig() {
        return (type != null ? type.name() : "") + "::" + latitudePattern.getExpression() + "::" + longitudePattern.getExpression();
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
        return latitudePattern != EMPTY_FORMULA && longitudePattern != EMPTY_FORMULA;
    }

    public static CalculatedCoordinate createFromConfig(final String config) {
        final CalculatedCoordinate state = new CalculatedCoordinate();
        state.setFromConfig(config);
        return state;
    }

    public CharSequence getLatitudeString(final Func1<String, Value> varMap) {
        return latitudePattern.evaluateToCharSequence(varMap);
    }

    public CharSequence getLongitudeString(final Func1<String, Value> varMap) {
        return longitudePattern.evaluateToCharSequence(varMap);
    }

    public Set<String> getNeededVars() {
        final Set<String> neededVars = new HashSet<>();
        neededVars.addAll(latitudePattern.getNeededVars());
        neededVars.addAll(longitudePattern.getNeededVars());
        return neededVars;
    }

    @Nullable
    public Geopoint calculateGeopoint(final Func1<String, Value> varMap) {
        final Pair<Double, Double> data = calculateGeopointData(varMap);
        return data.first == null || data.second == null ? null : new Geopoint(data.first, data.second);
    }

    @NonNull
    public Pair<Double, Double> calculateGeopointData(final Func1<String, Value> varMap) {
        return new Pair<>(latitudePattern.evaluate(varMap), longitudePattern.evaluate(varMap));
    }

    @Override
    public String toString() {
        return toConfig();
    }

}
