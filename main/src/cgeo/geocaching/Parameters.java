package cgeo.geocaching;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * List of key/values pairs to be used in a GET or POST request.
 * 
 */
public class Parameters extends ArrayList<NameValuePair> {

    private static final long serialVersionUID = 1L;

    /**
     * @param keyValues
     *            list of initial key/value pairs
     * @throws InvalidParameterException
     *             if the number of key/values is unbalanced
     */
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

    /**
     * Add new key/value pairs to the current parameters.
     *
     * @param keyValues
     *            list of key/value pairs
     * @throws InvalidParameterException
     *             if the number of key/values is unbalanced
     */
    public void put(final String... keyValues) {
        if (keyValues.length % 2 == 1) {
            throw new InvalidParameterException("odd number of parameters");
        }
        for (int i = 0; i < keyValues.length; i += 2) {
            add(new BasicNameValuePair(keyValues[i], keyValues[i + 1]));
        }
    }

    /**
     * Lexically sort key/value pairs first by key, then by value.
     *
     * Some signing algorithms need the values to be ordered before issuing the signature.
     */
    public void sort() {
        Collections.sort(this, comparator);
    }

    /**
     * @return the URL encoded string corresponding to those parameters
     */
    @Override
    public String toString() {
        return URLEncodedUtils.format(this, HTTP.UTF_8);
    }

}
