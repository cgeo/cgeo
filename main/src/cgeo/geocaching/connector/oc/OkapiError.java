package cgeo.geocaching.connector.oc;

import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Handles the JSON error response from OKAPI
 */
public class OkapiError {

    /**
     * List of detected errors OKAPI might return
     */
    public enum OkapiErrors {
        NO_ERROR,
        UNSPECIFIED,
        INVALID_TIMESTAMP,
        INVALID_TOKEN;
    }

    @NonNull private final OkapiErrors state;
    @NonNull private final String message;

    public OkapiError(@Nullable JSONObject data) {
        String localmessage = StringUtils.EMPTY;
        assert localmessage != null; // by definition of StringUtils.EMPTY

        // A null-response is by definition an error (some exception occurred somewhere in the flow)
        if (data == null) {
            state = OkapiErrors.UNSPECIFIED;
            message = localmessage;
            return;
        }
        // Second possibility: we get an error object as return (@see http://opencaching.pl/okapi/introduction.html#errors)
        if (data.has("error")) {
            OkapiErrors localstate;
            try {
                JSONObject error = data.getJSONObject("error");
                // Check reason_stack element to look for the specific oauth problems we want to report back
                if (error.has("reason_stack")) {
                    String reason = error.getString("reason_stack");
                    if (StringUtils.contains(reason, "invalid_oauth_request")) {
                        if (StringUtils.contains(reason, "invalid_timestamp")) {
                            localstate = OkapiErrors.INVALID_TIMESTAMP;
                        } else if (StringUtils.contains(reason, "invalid_token")) {
                            localstate = OkapiErrors.INVALID_TOKEN;
                        } else {
                            localstate = OkapiErrors.UNSPECIFIED;
                        }
                    } else {
                        localstate = OkapiErrors.UNSPECIFIED;
                    }
                } else {
                    localstate = OkapiErrors.UNSPECIFIED;
                }
                // Check if we can extract a message as well
                if (error.has("developer_message")) {
                    localmessage = StringUtils.defaultString(error.getString("developer_message"));
                    assert localmessage != null; // by virtue of defaultString
                }
            } catch (JSONException ex) {
                Log.d("OkapiError: Failed to parse JSON", ex);
                localstate = OkapiErrors.UNSPECIFIED;
            }
            this.state = localstate;
            this.message = localmessage;
            return;
        }

        // Third possibility: some other response, everything is fine!
        state = OkapiErrors.NO_ERROR;
        message = localmessage;
    }

    public boolean isError() {
        return state != OkapiErrors.NO_ERROR;
    }

    @NonNull
    public OkapiErrors getResult() {
        return state;
    }

    @NonNull
    public String getMessage() {
        return message;
    }

}
