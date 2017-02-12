package cgeo.geocaching.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.CalculateButton;
import cgeo.geocaching.ui.CalculatorVariable;

/**
 * This class is designed to capture the current state of the coordinate calculator such that it can be preserver for latter use.
 **
 * All the relevant information is in a serializable form such that it can be stored as a bundle in waypoint's 'ContentValues'.
 **/

public class CalcState implements Serializable {
    public static final char ERROR_CHAR = '#';
    public static final String ERROR_STRING = "???";

    public Settings.CoordInputFormatEnum format;
    public String plainLat, plainLon;
    public char latHemisphere, lonHemisphere;
    public List<CalculateButton.ButtonData> buttons;
    public List<CalculatorVariable.VariableData> equations, freeVariables;
    public String notes; // Note, we have to use a String rather than an Editable as Editable's can't be serialized

    public CalcState(final Settings.CoordInputFormatEnum format,
                     final String plainLat,
                     final String plainLon,
                     final char latHem,
                     final char lonHem,
                     final List<CalculateButton.ButtonData> buttons,
                     final List<CalculatorVariable.VariableData> equations,
                     final List<CalculatorVariable.VariableData> freeVariables,
                     final String notes) {
        this.format = format;
        this.plainLat = plainLat;
        this.plainLon = plainLon;
        latHemisphere = latHem;
        lonHemisphere = lonHem;
        this.buttons = buttons;
        this.equations = equations;
        this.freeVariables = freeVariables;
        this.notes = notes;
    }

    public CalcState(final JSONObject json) {
        format = Settings.CoordInputFormatEnum.values()[json.optInt("format", 2)];
        plainLat = json.optString("plainLat");
        plainLon = json.optString("plainLon");
        latHemisphere = (char) json.optInt("latHemisphere", ERROR_CHAR);
        lonHemisphere = (char) json.optInt("lonHemisphere", ERROR_CHAR);

        {
            final JSONArray array = json.optJSONArray("buttons");
            final int arrayLength = array != null ? array.length() : 0;
            buttons = new ArrayList<>(arrayLength);
            for (int i = 0; i < arrayLength; i++) {
                buttons.add(new CalculateButton.ButtonData(array.optJSONObject(i)));
            }
        }

        {
            final JSONArray array = json.optJSONArray("equations");
            final int arrayLength = array != null ? array.length() : 0;
            equations = new ArrayList<>(arrayLength);
            for (int i = 0; i < arrayLength; i++) {
                equations.add(new CalculatorVariable.VariableData(array.optJSONObject(i)));
            }
        }

        {
            final JSONArray array = json.optJSONArray("freeVariables");
            final int arrayLength = array != null ? array.length() : 0;
            freeVariables = new ArrayList<>(arrayLength);
            for (int i = 0; i < arrayLength; i++) {
                freeVariables.add(new CalculatorVariable.VariableData(array.optJSONObject(i)));
            }
        }

        notes = json.optString("notes");
    }

    public static CalcState fromJSON(final String json) throws JSONException {
        return new CalcState(new JSONObject(json));
    }

    public JSONObject toJSON() throws JSONException {
        final JSONObject rv = new JSONObject();

        rv.put("format", format.ordinal());
        rv.put("plainLat", plainLat);
        rv.put("plainLon", plainLon);
        rv.put("latHemisphere", latHemisphere);
        rv.put("lonHemisphere", lonHemisphere);

        {
            final JSONArray array = new JSONArray();
            for (final CalculateButton.ButtonData but : buttons) {
                array.put(but.toJASON());
            }
            rv.put("buttons", array);
        }

        {
            final JSONArray array = new JSONArray();
            for (final CalculatorVariable.VariableData equ : equations) {
                array.put(equ.toJSON());
            }
            rv.put("equations", array);
        }

        {
            final JSONArray array = new JSONArray();
            for (final CalculatorVariable.VariableData equ : freeVariables) {
                array.put(equ.toJSON());
            }
            rv.put("freeVariables", array);
        }

        rv.put("notes", notes);
        return rv;
    }
}
