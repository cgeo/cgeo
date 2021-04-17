package cgeo.geocaching.calculator;

import org.json.JSONObject;

public final class VariableDataFactory implements JSONAbleFactory<VariableData> {
    @Override
    public VariableData fromJSON(final JSONObject json) {
        return new VariableData(json);
    }
}

