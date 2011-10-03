package cgeo.geocaching;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Parameters extends ArrayList<NameValuePair> {

    private static final long serialVersionUID = 1L;

    public Parameters(final String... keyValues) {
        super();
        put(keyValues);
    }

    private static final Comparator<NameValuePair> comparator = new Comparator<NameValuePair>() {
        @Override
        public int compare(final NameValuePair nv1, final NameValuePair nv2) {
            final int comparedKeys = nv1.getName().compareTo(nv2.getName());
            return comparedKeys != 0 ? comparedKeys : nv1.getValue().compareTo(nv2.getValue());
        }
    };

    public void put(final String... keyValues) {
        for (int i = 0; i < keyValues.length; i += 2) {
            add(new BasicNameValuePair(keyValues[i], keyValues[i + 1]));
        }
    }

    public void sort() {
        Collections.sort(this, comparator);
    }

}
