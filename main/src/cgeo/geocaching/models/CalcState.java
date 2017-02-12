package cgeo.geocaching.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cgeo.geocaching.settings.Settings.CoordInputFormatEnum;
import cgeo.geocaching.ui.CalculateButton;
import cgeo.geocaching.ui.CalculatorVariable;
import cgeo.geocaching.ui.JSONAble;
import cgeo.geocaching.ui.JSONAbleFactory;

/**
 * This class is designed to capture the current state of the coordinate calculator such that it can be preserver for latter use.
 *
 * All the relevant information is in a serializable form such that it can be stored as a bundle in waypoint's 'ContentValues'.
 */

public class CalcState implements Serializable {
    public static final char ERROR_CHAR = '#';
    public static final String ERROR_STRING = "???";

    public final CoordInputFormatEnum format;
    public final String plainLat;
    public final String plainLon;
    public final char latHemisphere;
    public final char lonHemisphere;
    public final List<? extends JSONAble> buttons;
    public final List<? extends JSONAble> equations;
    public final List<? extends JSONAble> freeVariables;
    public final List<? extends JSONAble> variableBank;
    public final String notes; // Note, we have to use a String rather than an Editable as Editable's can't be serialized

    public CalcState(final CoordInputFormatEnum format,
                     final String plainLat,
                     final String plainLon,
                     final char latHem,
                     final char lonHem,
                     final List<CalculateButton.ButtonData> buttons,
                     final List<CalculatorVariable.VariableData> equations,
                     final List<CalculatorVariable.VariableData> freeVariables,
                     final List<CalculatorVariable.VariableData> bankVariables,
                     final String notes) {
        this.format = format;
        this.plainLat = plainLat;
        this.plainLon = plainLon;
        latHemisphere = latHem;
        lonHemisphere = lonHem;
        this.buttons = buttons;
        this.equations = equations;
        this.freeVariables = freeVariables;
        this.variableBank  = bankVariables;
        this.notes = notes;
    }

    private CalcState(final JSONObject json) {
        format = CoordInputFormatEnum.values()[json.optInt("format", 2)];
        plainLat = json.optString("plainLat");
        plainLon = json.optString("plainLon");
        latHemisphere = (char) json.optInt("latHemisphere", ERROR_CHAR);
        lonHemisphere = (char) json.optInt("lonHemisphere", ERROR_CHAR);
        buttons       = createJSONAbleList(json.optJSONArray("buttons"),       new CalculateButton.ButtonDataFactory());
        equations     = createJSONAbleList(json.optJSONArray("equations"),     new CalculatorVariable.VariableDataFactory());
        freeVariables = createJSONAbleList(json.optJSONArray("freeVariables"), new CalculatorVariable.VariableDataFactory());
        variableBank = new ArrayList<>(); // "variableBank" intentionally not loaded.
        notes = json.optString("notes");
    }

    private static ArrayList<? extends JSONAble> createJSONAbleList(final JSONArray json, final JSONAbleFactory factory) {
        final int length = json != null ? json.length() : 0;
        final ArrayList<JSONAble> returnValue = new ArrayList<>(length);

        for (int i = 0; i < length; i++) {
            returnValue.add(factory.fromJSON(json.optJSONObject(i)));
        }

        return returnValue;
    }

    public static CalcState fromJSON(final String json) throws JSONException {
        if (json == null) {
            return null;
        }

        return new CalcState(new JSONObject(json));
    }

    private JSONArray toJSON(final List<? extends JSONAble> items) throws JSONException {
        final JSONArray returnValue = new JSONArray();

        for (final JSONAble item : items) {
            returnValue.put(item.toJSON());
        }

        return returnValue;
    }

    public JSONObject toJSON() throws JSONException {
        final JSONObject returnValue = new JSONObject();

        returnValue.put("format", format.ordinal());
        returnValue.put("plainLat", plainLat);
        returnValue.put("plainLon", plainLon);
        returnValue.put("latHemisphere", latHemisphere);
        returnValue.put("lonHemisphere", lonHemisphere);
        returnValue.put("buttons", toJSON(buttons));
        returnValue.put("equations", toJSON(equations));
        returnValue.put("freeVariables", toJSON(freeVariables));
        // "variableBank" intentionally left out.
        returnValue.put("notes", notes);

        return returnValue;
    }
}
