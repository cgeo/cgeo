package cgeo.geocaching.calculator;

import org.json.JSONObject;

public interface JSONAbleFactory<T extends JSONAble> {
    T fromJSON(JSONObject data);
}
