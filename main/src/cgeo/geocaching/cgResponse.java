package cgeo.geocaching;

import org.apache.http.HttpResponse;

public class cgResponse {
    private String data = null;

    private HttpResponse response = null;

    public cgResponse() {
    }

    public cgResponse(HttpResponse response) {
        this.response = response;
    }

    public void setStatusCode(int code) {
        response.setStatusCode(code);
    }

    public int getStatusCode() {
        return response.getStatusLine().getStatusCode();
    }

    public void setStatusMessage(String message) {
        response.setReasonPhrase(message);
    }

    public String getStatusMessage() {
        return response.getStatusLine().getReasonPhrase();
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getData() {
        return data == null ? cgBase.getResponseData(response) : data;
    }
}
