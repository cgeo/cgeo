package cgeo.geocaching;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;

public class Parameters extends ArrayList<NameValuePair> {

    private static final long serialVersionUID = 1L;

    public void put(final String name, final String value) {
        add(new BasicNameValuePair(name, value));
    }

}
