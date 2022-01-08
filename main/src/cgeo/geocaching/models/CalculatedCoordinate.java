package cgeo.geocaching.models;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.utils.TextParser;
import cgeo.geocaching.utils.formulas.DegreeFormula;
import cgeo.geocaching.utils.formulas.Value;
import cgeo.geocaching.utils.functions.Func1;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.EnumUtils;

public class CalculatedCoordinate implements Parcelable {

    //Config String: {CC|<latPatern>|<lonPattern>|<type-char>}
    public static final String CONFIG_KEY = "CC";

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
        this.type = EnumUtils.getEnum(CalculatedCoordinateType.class, tokens.size() > 2 ? tokens.get(2) : null, CalculatedCoordinateType.PLAIN);
        return tp.pos();
    }

    public String toConfig() {
        return "{" + CONFIG_KEY +
            "|" + TextParser.escape(latitudePattern.getExpression(), c -> c == '}' || c == '|', '\\') +
            '|' + TextParser.escape(longitudePattern.getExpression(), c -> c == '}' || c == '|', '\\') +
            (type != null ? '|' + type.name() : "") +
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

    @NonNull
    public List<Pair<String, Geopoint>> calculateGeopoints(final Func1<String, Value> varMap, final Map<String, String> varPatterns) {
        final List<Pair<String, Geopoint>> results = new ArrayList<>();
        final List<Pair<String, List<Integer>>> varPatternList = new ArrayList<>();
        for (Map.Entry<String, String> entry : varPatterns.entrySet()) {
            varPatternList.add(new Pair<>(entry.getKey(), patternToValues(entry.getValue())));
        }
        calculateGeopointsRecursive(results, varMap, varPatternList, null,  0);
        return results;
    }

    private void calculateGeopointsRecursive(final List<Pair<String, Geopoint>> result, final Func1<String, Value> varMap, final List<Pair<String, List<Integer>>> varPatterns, final String name, final int pos) {
        if (pos >= varPatterns.size()) {
            final Geopoint gp = calculateGeopoint(varMap);
            if (gp != null) {
                result.add(new Pair<>(name, gp));
            }
            return;
        }
        final Pair<String, List<Integer>> pattern = varPatterns.get(pos);
        for (int value : pattern.second) {
            calculateGeopointsRecursive(result, s -> pattern.first.equals(s) ? Value.of(value) : varMap.call(s), varPatterns,
                (name == null ? "" : name + ",") + pattern.first + "=" + value, pos + 1);
        }
    }

    public static List<Integer> patternToValues(final String pattern) {
        final List<Integer> result = new ArrayList<>();
        final TextParser tp = new TextParser(pattern);
        StringBuilder currentToken = new StringBuilder();
        String lastToken = null;
        boolean rangeFound = false;
        boolean negate = false;
        while (true) {
            if (tp.ch() == '^') {
                negate = !negate;
            } else if (tp.ch() >= '0' && tp.ch() <= '9') {
                currentToken.append(tp.ch());
            } else if (tp.ch() == '-') {
                rangeFound = true;
                lastToken = currentToken.toString();
                currentToken = new StringBuilder();
            } else if (tp.ch() == ',' || tp.eof()) {
                final int end = Integer.parseInt(currentToken.toString());
                final int start = rangeFound ? Integer.parseInt(lastToken) : end;
                for (int i = start; i <= end; i++) {
                    if (negate) {
                        result.remove((Integer) i);
                    } else {
                        result.add(i);
                    }
                }
                currentToken = new StringBuilder();
                negate = false;
                rangeFound = false;
                lastToken = null;
                if (tp.eof()) {
                    break;
                }
            }
            tp.next();
        }
        return result;
    }

    @Override
    public String toString() {
        return toConfig();
    }

}
