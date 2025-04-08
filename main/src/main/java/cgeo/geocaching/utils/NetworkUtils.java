package cgeo.geocaching.utils;

import java.io.IOException;

import cgeo.geocaching.enumerations.StatusCode;

import okhttp3.Response;

public final class NetworkUtils {
    private NetworkUtils() {
        // static class
    }

    public static class StatusException extends RuntimeException {
        private static final long serialVersionUID = -4406421279349750156L;
        public final StatusCode statusCode;

        StatusException(final StatusCode statusCode) {
            super("Status code: " + statusCode);
            this.statusCode = statusCode;
        }
    }

    public static String getResponseBodyOrStatus(final Response response) {
        final String body;
        try {
            body = response.body().string();
        } catch (final IOException ignore) {
            throw new StatusException(StatusCode.COMMUNICATION_ERROR);
        }
        if (response.code() == 503 /*&& TextUtils.matches(body, GCConstants.PATTERN_MAINTENANCE)*/) {
            throw new StatusException(StatusCode.MAINTENANCE);
        } else if (!response.isSuccessful()) {
            throw new StatusException(StatusCode.COMMUNICATION_ERROR);
        }
        return body;
    }
}
