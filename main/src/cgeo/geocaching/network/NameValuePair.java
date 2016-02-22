package cgeo.geocaching.network;

public class NameValuePair {

    private final String name;
    private final String value;

    public NameValuePair(final String name, final String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
