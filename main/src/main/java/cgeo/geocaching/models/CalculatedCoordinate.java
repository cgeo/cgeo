package cgeo.geocaching.models;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointParser;
import cgeo.geocaching.utils.TextParser;
import cgeo.geocaching.utils.formulas.DegreeFormula;
import cgeo.geocaching.utils.formulas.Formula;
import cgeo.geocaching.utils.formulas.Value;
import static cgeo.geocaching.models.CalculatedCoordinateType.PLAIN;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.ImmutableTriple;

public class CalculatedCoordinate implements Parcelable {

    //Config String: {CC|<latPatern>|<lonPattern>|<type-char>}
    public static final String CONFIG_KEY = "CC";

    private static final DegreeFormula EMPTY_FORMULA = DegreeFormula.compile("", false);

    private CalculatedCoordinateType type = PLAIN;
    private DegreeFormula latitudePattern = EMPTY_FORMULA;
    private DegreeFormula longitudePattern = EMPTY_FORMULA;
    private String latitudePatternText = "";
    private String longitudePatternText = "";

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
        return latitudePatternText;
    }

    public void setLatitudePattern(final String latitudePattern) {
        this.latitudePatternText = latitudePattern == null ? "" : latitudePattern;
        try {
            this.latitudePattern = DegreeFormula.compile(this.latitudePatternText, false);
        } catch (final RuntimeException ignored) {
            this.latitudePattern = EMPTY_FORMULA;
        }
    }

    public String getLongitudePattern() {
        return longitudePatternText;
    }

    public void setLongitudePattern(final String longitudePattern) {
        this.longitudePatternText = longitudePattern == null ? "" : longitudePattern;
        try {
            this.longitudePattern = DegreeFormula.compile(this.longitudePatternText, true);
        } catch (final RuntimeException ignored) {
            this.longitudePattern = EMPTY_FORMULA;
        }
    }

    public void setFrom(final CalculatedCoordinate other) {
        this.type = other.type;
        this.latitudePattern = other.latitudePattern;
        this.longitudePattern = other.longitudePattern;
        this.latitudePatternText = other.latitudePatternText;
        this.longitudePatternText = other.longitudePatternText;
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
        setLatitudePattern(!tokens.isEmpty() ? tokens.get(0) : "");
        setLongitudePattern(tokens.size() > 1 ? tokens.get(1) : "");
        this.type = CalculatedCoordinateType.fromName(tokens.size() > 2 ? tokens.get(2) : PLAIN.shortName());
        return tp.pos();
    }

    public String toConfig() {
        return "{" + CONFIG_KEY +
                "|" + TextParser.escape(latitudePatternText, c -> c == '}' || c == '|', '\\') +
                '|' + TextParser.escape(longitudePatternText, c -> c == '}' || c == '|', '\\') +
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
        dest.writeString(latitudePatternText);
        dest.writeString(longitudePatternText);
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
        if (type == CalculatedCoordinateType.UTM || type == CalculatedCoordinateType.RD) {
            return getProjectedNeededVars();
        }
        final Set<String> neededVars = new HashSet<>();
        neededVars.addAll(latitudePattern.getNeededVars());
        neededVars.addAll(longitudePattern.getNeededVars());
        return neededVars;
    }

    @Nullable
    public Geopoint calculateGeopoint(final Function<String, Value> varMap) {
        if (type == CalculatedCoordinateType.UTM) {
            return calculateUtmGeopoint(varMap);
        }
        if (type == CalculatedCoordinateType.RD) {
            return calculateRdGeopoint(varMap);
        }
        final ImmutableTriple<Double, CharSequence, Boolean> latData = calculateLatitudeData(varMap);
        final ImmutableTriple<Double, CharSequence, Boolean> lonData = calculateLongitudeData(varMap);
        return latData.left == null || lonData.left == null ? null : new Geopoint(latData.left, lonData.left);
    }

    @NonNull
    public ImmutableTriple<Double, CharSequence, Boolean> calculateLatitudeData(final Function<String, Value> varMap) {
        if (type == CalculatedCoordinateType.UTM) {
            final UtmParts utmParts = evaluateUtmParts(varMap);
            if (utmParts == null) {
                return new ImmutableTriple<>(null, getLatitudePattern(), false);
            }
            return new ImmutableTriple<>(1.0d, utmParts.zone + " " + utmParts.eastingText, utmParts.warning);
        }
        if (type == CalculatedCoordinateType.RD) {
            final EvalPart xPart = evaluateRdToken(latitudePatternText, varMap);
            if (xPart == null || xPart.value == null) {
                return new ImmutableTriple<>(null, xPart == null ? getLatitudePattern() : xPart.display, false);
            }
            return new ImmutableTriple<>(1.0d, xPart.display, xPart.warning);
        }
        return latitudePattern.evaluate(varMap);
    }

    public ImmutableTriple<Double, CharSequence, Boolean> calculateLongitudeData(final Function<String, Value> varMap) {
        if (type == CalculatedCoordinateType.UTM) {
            final UtmParts utmParts = evaluateUtmParts(varMap);
            if (utmParts == null) {
                return new ImmutableTriple<>(null, getLongitudePattern(), false);
            }
            return new ImmutableTriple<>(1.0d, utmParts.northingText, utmParts.warning);
        }
        if (type == CalculatedCoordinateType.RD) {
            final EvalPart yPart = evaluateRdToken(longitudePatternText, varMap);
            if (yPart == null || yPart.value == null) {
                return new ImmutableTriple<>(null, yPart == null ? getLongitudePattern() : yPart.display, false);
            }
            return new ImmutableTriple<>(1.0d, yPart.display, yPart.warning);
        }
        return longitudePattern.evaluate(varMap);
    }

    @Override
    public String toString() {
        return toConfig();
    }

    public boolean hasWarning(final Function<String, Value> varMap) {
        if (type == CalculatedCoordinateType.UTM || type == CalculatedCoordinateType.RD) {
            final ImmutableTriple<Double, CharSequence, Boolean> latData = calculateLatitudeData(varMap);
            final ImmutableTriple<Double, CharSequence, Boolean> lonData = calculateLongitudeData(varMap);
            return latData.right || lonData.right;
        }
        final ImmutableTriple<Double, CharSequence, Boolean> latData = calculateLatitudeData(varMap);
        final ImmutableTriple<Double, CharSequence, Boolean> lonData = calculateLongitudeData(varMap);
        return latData.right || lonData.right;
    }

    private Set<String> getProjectedNeededVars() {
        final Set<String> neededVars = new HashSet<>();
        if (type == CalculatedCoordinateType.UTM) {
            final String[] utmTokens = splitOnWhitespace(latitudePatternText);
            if (utmTokens.length > 2) {
                addFormulaVars(neededVars, utmTokens[1]);
                addFormulaVars(neededVars, utmTokens[2]);
            }
            final String[] northingTokens = splitOnWhitespace(longitudePatternText);
            if (northingTokens.length > 1) {
                addFormulaVars(neededVars, northingTokens[1]);
            }
            if (northingTokens.length > 2) {
                addFormulaVars(neededVars, northingTokens[2]);
            }
        } else if (type == CalculatedCoordinateType.RD) {
            addFormulaVars(neededVars, stripPrefixToken(latitudePatternText, 'X'));
            addFormulaVars(neededVars, stripPrefixToken(longitudePatternText, 'Y'));
        }
        return neededVars;
    }

    private static void addFormulaVars(final Set<String> collector, final String expression) {
        final Formula formula = Formula.safeCompile(expression);
        if (formula != null) {
            collector.addAll(formula.getNeededVariables());
        }
    }

    private Geopoint calculateUtmGeopoint(final Function<String, Value> varMap) {
        final UtmParts utm = evaluateUtmParts(varMap);
        if (utm == null) {
            return null;
        }
        try {
            return GeopointParser.parse(utm.zone + " E " + utm.eastingText + " N " + utm.northingText);
        } catch (final Geopoint.ParseException ignored) {
            return null;
        }
    }

    private Geopoint calculateRdGeopoint(final Function<String, Value> varMap) {
        final EvalPart xPart = evaluateRdToken(latitudePatternText, varMap);
        final EvalPart yPart = evaluateRdToken(longitudePatternText, varMap);
        if (xPart == null || yPart == null || xPart.value == null || yPart.value == null) {
            return null;
        }
        try {
            return GeopointParser.parse("RD X " + xPart.display + " Y " + yPart.display);
        } catch (final Geopoint.ParseException ignored) {
            return null;
        }
    }

    private UtmParts evaluateUtmParts(final Function<String, Value> varMap) {
        final String[] latTokens = splitOnWhitespace(latitudePatternText);
        final String[] northingTokens = splitOnWhitespace(longitudePatternText);
        if (latTokens.length < 3 || northingTokens.length < 3) {
            return null;
        }
        final String zone = latTokens[1] + latTokens[0].toUpperCase(Locale.US);
        final EvalPart eastingPart = evaluateNumericToken(latTokens[2], varMap);
        final EvalPart northingLeadPart = evaluateNumericToken(northingTokens[1], varMap);
        final EvalPart northingTailPart = evaluateNumericToken(northingTokens[2], varMap);
        if (northingLeadPart == null || northingTailPart == null || northingLeadPart.value == null || northingTailPart.value == null) {
            return null;
        }
        final String northingText = northingLeadPart.display + String.format(Locale.US, "%06d", northingTailPart.value);
        if (eastingPart == null || eastingPart.value == null) {
            return null;
        }
        return new UtmParts(zone, eastingPart.display.toString(), northingText, eastingPart.warning || northingLeadPart.warning || northingTailPart.warning);
    }

    private static String[] splitOnWhitespace(final String text) {
        return text == null ? new String[0] : text.trim().split("\\s+");
    }

    private EvalPart evaluateRdToken(final String expression, final Function<String, Value> varMap) {
        return evaluateNumericToken(stripPrefixToken(expression, 'X', 'Y'), varMap);
    }

    private static String stripPrefixToken(final String expression, final char... prefixes) {
        final String trimmed = expression == null ? "" : expression.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        for (char prefix : prefixes) {
            if (Character.toUpperCase(trimmed.charAt(0)) == Character.toUpperCase(prefix)) {
                return trimmed.substring(1).trim();
            }
        }
        return trimmed;
    }

    private EvalPart evaluateNumericToken(final String expression, final Function<String, Value> varMap) {
        final Formula formula = Formula.safeCompile(expression);
        if (formula == null) {
            return new EvalPart(null, expression, false);
        }
        final Value value = formula.safeEvaluate(varMap);
        if (value == null || !value.isNumeric()) {
            return new EvalPart(null, expression, false);
        }
        final long rounded = Math.round(value.getAsDouble());
        final boolean warning = Math.abs(value.getAsDouble() - rounded) > 1e-9;
        return new EvalPart(rounded, Long.toString(rounded), warning);
    }

    private static class EvalPart {
        private final Long value;
        private final CharSequence display;
        private final boolean warning;

        EvalPart(final Long value, final CharSequence display, final boolean warning) {
            this.value = value;
            this.display = display;
            this.warning = warning;
        }
    }

    private static class UtmParts {
        private final String zone;
        private final String eastingText;
        private final String northingText;
        private final boolean warning;

        UtmParts(final String zone, final String eastingText, final String northingText, final boolean warning) {
            this.zone = zone;
            this.eastingText = eastingText;
            this.northingText = northingText;
            this.warning = warning;
        }
    }
}
