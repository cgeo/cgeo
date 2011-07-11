package cgeo.geocaching;

public class cgResponse {
	private String url;
	private int statusCode;
	private String statusMessage;
	private String data;
	
	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getUrl() {
		return url;
	}

	public void setStatusCode(int code) {
		statusCode = code;
	}
	
	public int getStatusCode() {
		return statusCode;
	}
	
	public void setStatusMessage(String message) {
		statusMessage = message;
	}
	
	public String getStatusMessage() {
		return statusMessage;
	}
	
	public void setData(String data) {
		this.data = data;
	}
	
	public String getData() {
		return data;
	}
}
