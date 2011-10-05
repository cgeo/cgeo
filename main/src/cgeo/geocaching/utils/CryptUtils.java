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
}
