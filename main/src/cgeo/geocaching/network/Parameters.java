package cgeo.geocaching.network;

import ch.boye.httpclientandroidlib.NameValuePair;
import ch.boye.httpclientandroidlib.client.utils.URLEncodedUtils;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;

import org.apache.commons.lang3.CharEncoding;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

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
     * @return the object itself to facilitate chaining
     */
    public Parameters put(final String... keyValues) {
        if (keyValues.length % 2 == 1) {
            throw new InvalidParameterException("odd number of parameters");
        }
        for (int i = 0; i < keyValues.length; i += 2) {
            add(new BasicNameValuePair(keyValues[i], keyValues[i + 1]));
        }
        return this;
    }

    /**
     * Lexically sort key/value pairs first by key, then by value.
     *
     * Some signing algorithms need the values to be ordered before issuing the signature.
     */
    public void sort() {
        Collections.sort(this, comparator);
    }

    @Override
    public String toString() {
        return URLEncodedUtils.format(this, CharEncoding.UTF_8);
    }

    /**
     * Extend or create a Parameters object with new key/value pairs.
     *
     * @param params
     *            an existing object or null to create a new one
     * @param keyValues
     *            list of key/value pair
     * @throws InvalidParameterException
     *             if the number of key/values is unbalanced
     * @return the object itself if it is non-null, a new one otherwise
     */
    @NonNull
    public static Parameters extend(@Nullable final Parameters params, final String... keyValues) {
        return params == null ? new Parameters(keyValues) : params.put(keyValues);
    }

    /**
     * Merge two (possibly null) Parameters object.
     *
     * @param params
     *            the object to merge into if non-null
     * @param extra
     *            the object to merge from if non-null
     * @return params with extra data if params was non-null, extra otherwise
     */
    @Nullable
    public static Parameters merge(@Nullable final Parameters params, @Nullable final Parameters extra) {
        if (params == null) {
            return extra;
        }
        if (extra != null) {
            params.addAll(extra);
        }
        return params;
    }

    public void add(final String key, final String value) {
        put(key, value);
    }

}
