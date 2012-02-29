package cgeo.geocaching.utils;

import cgeo.geocaching.Settings;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.util.Log;

import java.math.BigInteger;
import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class CryptUtils {

    private static char[] base64map1 = new char[64];
    private static byte[] base64map2 = new byte[128];

    static {
        int i = 0;
        for (char c = 'A'; c <= 'Z'; c++) {
            base64map1[i++] = c;
        }
        for (char c = 'a'; c <= 'z'; c++) {
            base64map1[i++] = c;
        }
        for (char c = '0'; c <= '9'; c++) {
            base64map1[i++] = c;
        }
        base64map1[i++] = '+';
        base64map1[i++] = '/';

        for (i = 0; i < base64map2.length; i++) {
            base64map2[i] = -1;
        }
        for (i = 0; i < 64; i++) {
            base64map2[base64map1[i]] = (byte) i;
        }
    }

    public static String rot13(String text) {
        if (text == null) {
            return "";
        }
        final StringBuilder result = new StringBuilder();
        // plaintext flag (do not convert)
        boolean plaintext = false;

        final int length = text.length();
        int c;
        int capitalized;
        for (int index = 0; index < length; index++) {
            c = text.charAt(index);
            if (c == '[') {
                plaintext = true;
            } else if (c == ']') {
                plaintext = false;
            } else if (!plaintext) {
                capitalized = c & 32;
                c &= ~capitalized;
                c = ((c >= 'A') && (c <= 'Z') ? ((c - 'A' + 13) % 26 + 'A') : c)
                        | capitalized;
            }
            result.append((char) c);
        }
        return result.toString();
    }

    public static String md5(String text) {
        String hashed = "";

        try {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(text.getBytes(), 0, text.length());
            hashed = new BigInteger(1, digest.digest()).toString(16);
        } catch (Exception e) {
            Log.e(Settings.tag, "cgBase.md5: " + e.toString());
        }

        return hashed;
    }

    public static String sha1(String text) {
        String hashed = "";

        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(text.getBytes(), 0, text.length());
            hashed = new BigInteger(1, digest.digest()).toString(16);
        } catch (Exception e) {
            Log.e(Settings.tag, "cgBase.sha1: " + e.toString());
        }

        return hashed;
    }

    public static byte[] hashHmac(String text, String salt) {
        byte[] macBytes = {};

        try {
            final SecretKeySpec secretKeySpec = new SecretKeySpec(salt.getBytes(), "HmacSHA1");
            final Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(secretKeySpec);
            macBytes = mac.doFinal(text.getBytes());
        } catch (Exception e) {
            Log.e(Settings.tag, "cgBase.hashHmac: " + e.toString());
        }

        return macBytes;
    }

    public static CharSequence rot13(final Spannable span) {
        // I needed to re-implement the rot13(String) encryption here because we must work on
        // a SpannableStringBuilder instead of the pure text and we must replace each character inline.
        // Otherwise we loose all the images, colors and so on...
        final SpannableStringBuilder buffer = new SpannableStringBuilder(span);
        boolean plaintext = false;

        final int length = span.length();
        int c;
        int capitalized;
        for (int index = 0; index < length; index++) {
            c = span.charAt(index);
            if (c == '[') {
                plaintext = true;
            } else if (c == ']') {
                plaintext = false;
            } else if (!plaintext) {
                capitalized = c & 32;
                c &= ~capitalized;
                c = ((c >= 'A') && (c <= 'Z') ? ((c - 'A' + 13) % 26 + 'A') : c)
                        | capitalized;
            }
            buffer.replace(index, index + 1, String.valueOf((char) c));
        }
        return buffer;
    }

    public static byte[] base64Decode(String text) {
        char[] in = text.toCharArray();

        int iLen = in.length;
        if (iLen % 4 != 0) {
            throw new IllegalArgumentException("Length of Base64 encoded input string is not a multiple of 4.");
        }
        while (iLen > 0 && in[iLen - 1] == '=') {
            iLen--;
        }
        int oLen = (iLen * 3) / 4;
        byte[] out = new byte[oLen];
        int ip = 0;
        int op = 0;
        while (ip < iLen) {
            int i0 = in[ip++];
            int i1 = in[ip++];
            int i2 = ip < iLen ? in[ip++] : 'A';
            int i3 = ip < iLen ? in[ip++] : 'A';
            if (i0 > 127 || i1 > 127 || i2 > 127 || i3 > 127) {
                throw new IllegalArgumentException("Illegal character in Base64 encoded data.");
            }
            int b0 = base64map2[i0];
            int b1 = base64map2[i1];
            int b2 = base64map2[i2];
            int b3 = base64map2[i3];
            if (b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0) {
                throw new IllegalArgumentException("Illegal character in Base64 encoded data.");
            }
            int o0 = (b0 << 2) | (b1 >>> 4);
            int o1 = ((b1 & 0xf) << 4) | (b2 >>> 2);
            int o2 = ((b2 & 3) << 6) | b3;
            out[op++] = (byte) o0;
            if (op < oLen) {
                out[op++] = (byte) o1;
            }
            if (op < oLen) {
                out[op++] = (byte) o2;
            }
        }
        return out;
    }

    public static String base64Encode(byte[] in) {
        int iLen = in.length;
        int oDataLen = (iLen * 4 + 2) / 3; // output length without padding
        int oLen = ((iLen + 2) / 3) * 4; // output length including padding
        char[] out = new char[oLen];
        int ip = 0;
        int op = 0;

        while (ip < iLen) {
            int i0 = in[ip++] & 0xff;
            int i1 = ip < iLen ? in[ip++] & 0xff : 0;
            int i2 = ip < iLen ? in[ip++] & 0xff : 0;
            int o0 = i0 >>> 2;
            int o1 = ((i0 & 3) << 4) | (i1 >>> 4);
            int o2 = ((i1 & 0xf) << 2) | (i2 >>> 6);
            int o3 = i2 & 0x3F;
            out[op++] = base64map1[o0];
            out[op++] = base64map1[o1];
            out[op] = op < oDataLen ? base64map1[o2] : '=';
            op++;
            out[op] = op < oDataLen ? base64map1[o3] : '=';
            op++;
        }

        return new String(out);
    }

}
