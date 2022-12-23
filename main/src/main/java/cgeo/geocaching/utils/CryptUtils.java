package cgeo.geocaching.utils;

import android.text.Spannable;
import android.text.SpannableStringBuilder;

import androidx.annotation.NonNull;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;

public final class CryptUtils {

    private static final byte[] EMPTY = {};
    private static final char[] BASE64MAP1 = new char[64];
    private static final byte[] BASE64MAP2 = new byte[128];

    static {
        int i = 0;
        for (char c = 'A'; c <= 'Z'; c++) {
            BASE64MAP1[i++] = c;
        }
        for (char c = 'a'; c <= 'z'; c++) {
            BASE64MAP1[i++] = c;
        }
        for (char c = '0'; c <= '9'; c++) {
            BASE64MAP1[i++] = c;
        }
        BASE64MAP1[i++] = '+';
        BASE64MAP1[i++] = '/';

        for (i = 0; i < BASE64MAP2.length; i++) {
            BASE64MAP2[i] = -1;
        }
        for (i = 0; i < 64; i++) {
            BASE64MAP2[BASE64MAP1[i]] = (byte) i;
        }
    }

    private CryptUtils() {
        // utility class
    }

    private static class Rot13Encryption {
        private boolean plaintext = false;

        char getNextEncryptedCharacter(final char c) {
            int result = c;
            if (result == '[') {
                plaintext = true;
            } else if (result == ']') {
                plaintext = false;
            } else if (!plaintext) {
                final int capitalized = result & 32;
                result &= ~capitalized;
                result = (result >= 'A' && result <= 'Z' ? ((result - 'A' + 13) % 26 + 'A') : result)
                        | capitalized;
            }
            return (char) result;
        }
    }

    @NonNull
    public static String rot13(final String text) {
        if (text == null) {
            return StringUtils.EMPTY;
        }
        final StringBuilder result = new StringBuilder();
        final Rot13Encryption rot13 = new Rot13Encryption();

        final int length = text.length();
        for (int index = 0; index < length; index++) {
            final char c = text.charAt(index);
            result.append(rot13.getNextEncryptedCharacter(c));
        }
        return result.toString();
    }

    @NonNull
    public static String md5(final String text) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(text.getBytes(StandardCharsets.UTF_8), 0, text.length());
            return new BigInteger(1, digest.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            Log.e("CryptUtils.md5", e);
        }

        return StringUtils.EMPTY;
    }

    @NonNull
    public static byte[] hashHmac(final String text, final String salt) {
        try {
            final SecretKeySpec secretKeySpec = new SecretKeySpec(salt.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
            final Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(secretKeySpec);
            return mac.doFinal(text.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            Log.e("CryptUtils.hashHmac", e);
            return EMPTY;
        }
    }

    @NonNull
    public static CharSequence rot13(final Spannable span) {
        // I needed to re-implement the rot13(String) encryption here because we must work on
        // a SpannableStringBuilder instead of the pure text and we must replace each character inline.
        // Otherwise we loose all the images, colors and so on...
        final SpannableStringBuilder buffer = new SpannableStringBuilder(span);
        final Rot13Encryption rot13 = new Rot13Encryption();

        final int length = span.length();
        for (int index = 0; index < length; index++) {
            final char c = span.charAt(index);
            buffer.replace(index, index + 1, String.valueOf(rot13.getNextEncryptedCharacter(c)));
        }
        return buffer;
    }

    @NonNull
    public static String base64Encode(final byte[] in) {
        final int iLen = in.length;
        final int oDataLen = (iLen * 4 + 2) / 3; // output length without padding
        final int oLen = ((iLen + 2) / 3) * 4; // output length including padding
        final char[] out = new char[oLen];
        int ip = 0;
        int op = 0;

        while (ip < iLen) {
            final int i0 = in[ip++] & 0xff;
            final int i1 = ip < iLen ? in[ip++] & 0xff : 0;
            final int i2 = ip < iLen ? in[ip++] & 0xff : 0;
            final int o0 = i0 >>> 2;
            final int o1 = ((i0 & 3) << 4) | (i1 >>> 4);
            final int o2 = ((i1 & 0xf) << 2) | (i2 >>> 6);
            final int o3 = i2 & 0x3F;
            out[op++] = BASE64MAP1[o0];
            out[op++] = BASE64MAP1[o1];
            out[op] = op < oDataLen ? BASE64MAP1[o2] : '=';
            op++;
            out[op] = op < oDataLen ? BASE64MAP1[o3] : '=';
            op++;
        }

        return new String(out);
    }

}
