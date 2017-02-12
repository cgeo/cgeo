package cgeo.geocaching.ui;

import org.json.JSONObject;

public interface JSONAbleFactory {
    JSONAble fromJSON(JSONObject data);
}
