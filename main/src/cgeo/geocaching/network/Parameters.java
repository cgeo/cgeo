package cgeo.geocaching.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;

import okhttp3.HttpUrl;
import okhttp3.HttpUrl.Builder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * List of key/values pairs to be used in a GET or POST request.
 */
public class Parameters extends ArrayList<ImmutablePair<String, String>> {

    private static final long serialVersionUID = 1L;
    private boolean percentEncoding = false;

    /**
     * @param keyValues list of initial key/value pairs
     * @throws InvalidParameterException if the number of key/values is unbalanced
     */
    public Parameters(final String... keyValues) {
        put(keyValues);
    }

    private static final Comparator<ImmutablePair<String, String>> comparator = (nv1, nv2) -> {
        final int comparedKeys = nv1.left.compareTo(nv2.left);
        return comparedKeys != 0 ? comparedKeys : nv1.right.compareTo(nv2.right);
    };

    /**
     * Percent encode following http://tools.ietf.org/html/rfc5849#section-3.6
     */
    static String percentEncode(@NonNull final String url) {
        return StringUtils.replace(Network.rfc3986URLEncode(url), "*", "%2A");
    }

    /**
     * Add new key/value pairs to the current parameters.
     *
     * @param keyValues list of key/value pairs
     * @return the object itself to facilitate chaining
     * @throws InvalidParameterException if the number of key/values is unbalanced
     */
    public Parameters put(final String... keyValues) {
        if (keyValues.length % 2 == 1) {
            throw new InvalidParameterException("odd number of parameters");
        }
        for (int i = 0; i < keyValues.length; i += 2) {
            add(ImmutablePair.of(keyValues[i], keyValues[i + 1]));
        }
        return this;
    }

    public Parameters removeKey(final String key) {
        final Iterator<ImmutablePair<String, String>> it = iterator();
        while (it.hasNext()) {
            if (Objects.equals(it.next().left, key)) {
                it.remove();
            }
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

    /**
     * Some sites require the use of percent encoding (see {@link #percentEncode(String)}) and do not
     * accept other encodings during their authorization and signing processes. This forces those
     * parameters to use percent encoding instead of the regular encoding.
     */
    public void usePercentEncoding() {
        percentEncoding = true;
    }

    @Override
    @NonNull
    public String toString() {
        if (percentEncoding) {
            if (isEmpty()) {
                return "";
            }
            final StringBuilder builder = new StringBuilder();
            for (final ImmutablePair<String, String> nameValuePair : this) {
                builder.append('&').append(percentEncode(nameValuePair.left)).append('=').append(percentEncode(nameValuePair.right));
            }
            return builder.substring(1);
        }
        final Builder builder = HttpUrl.parse("https://dummy.cgeo.org/").newBuilder();
        for (final ImmutablePair<String, String> nameValuePair : this) {
            builder.addQueryParameter(nameValuePair.left, nameValuePair.right);
        }
        return StringUtils.defaultString(builder.build().encodedQuery());
    }

    /**
     * Extend or create a Parameters object with new key/value pairs.
     *
     * @param params    an existing object or null to create a new one
     * @param keyValues list of key/value pair
     * @return the object itself if it is non-null, a new one otherwise
     * @throws InvalidParameterException if the number of key/values is unbalanced
     */
    @NonNull
    public static Parameters extend(@Nullable final Parameters params, final String... keyValues) {
        return params == null ? new Parameters(keyValues) : params.put(keyValues);
    }

    /**
     * Merge two or more (possibly null) Parameters object.
     *
     * @param params the objects to merge
     * @return the first non-null Parameters object enriched with the others, or null if all
     * of params were null
     */
    @Nullable
    public static Parameters merge(@Nullable final Parameters... params) {
        Parameters result = null;
        if (params != null) {
            for (final Parameters p : params) {
                if (result == null) {
                    result = p;
                } else if (p != null) {
                    result.addAll(p);
                }
            }
        }
        return result;
    }

    public void add(final String key, final String value) {
        put(key, value);
    }

}
