package cgeo.geocaching.calculator;

import org.json.JSONObject;

public final class ButtonDataFactory implements JSONAbleFactory<ButtonData> {
    @Override
    public ButtonData fromJSON(final JSONObject json) {
        return new ButtonData(json);
    }
}
