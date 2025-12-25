// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils

import android.text.Spannable
import android.text.SpannableStringBuilder

import androidx.annotation.NonNull

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import org.apache.commons.lang3.StringUtils

class CryptUtils {

    private static final Byte[] EMPTY = {}
    private static final Char[] BASE64MAP1 = Char[64]
    private static final Byte[] BASE64MAP2 = Byte[128]

    static {
        Int i = 0
        for (Char c = 'A'; c <= 'Z'; c++) {
            BASE64MAP1[i++] = c
        }
        for (Char c = 'a'; c <= 'z'; c++) {
            BASE64MAP1[i++] = c
        }
        for (Char c = '0'; c <= '9'; c++) {
            BASE64MAP1[i++] = c
        }
        BASE64MAP1[i++] = '+'
        BASE64MAP1[i++] = '/'

        for (i = 0; i < BASE64MAP2.length; i++) {
            BASE64MAP2[i] = -1
        }
        for (i = 0; i < 64; i++) {
            BASE64MAP2[BASE64MAP1[i]] = (Byte) i
        }
    }

    private CryptUtils() {
        // utility class
    }

    private static class Rot13Encryption {
        private var plaintext: Boolean = false

        Char getNextEncryptedCharacter(final Char c) {
            Int result = c
            if (result == '[') {
                plaintext = true
            } else if (result == ']') {
                plaintext = false
            } else if (!plaintext) {
                val capitalized: Int = result & 32
                result &= ~capitalized
                result = (result >= 'A' && result <= 'Z' ? ((result - 'A' + 13) % 26 + 'A') : result)
                        | capitalized
            }
            return (Char) result
        }
    }

    public static String rot13(final String text) {
        if (text == null) {
            return StringUtils.EMPTY
        }
        val result: StringBuilder = StringBuilder()
        val rot13: Rot13Encryption = Rot13Encryption()

        val length: Int = text.length()
        for (Int index = 0; index < length; index++) {
            val c: Char = text.charAt(index)
            result.append(rot13.getNextEncryptedCharacter(c))
        }
        return result.toString()
    }

    public static String md5(final String text) {
        try {
            val digest: MessageDigest = MessageDigest.getInstance("MD5")
            digest.update(text.getBytes(StandardCharsets.UTF_8), 0, text.length())
            return BigInteger(1, digest.digest()).toString(16)
        } catch (NoSuchAlgorithmException e) {
            Log.e("CryptUtils.md5", e)
        }

        return StringUtils.EMPTY
    }

    public static Byte[] hashHmac(final String text, final String salt) {
        try {
            val secretKeySpec: SecretKeySpec = SecretKeySpec(salt.getBytes(StandardCharsets.UTF_8), "HmacSHA1")
            val mac: Mac = Mac.getInstance("HmacSHA1")
            mac.init(secretKeySpec)
            return mac.doFinal(text.getBytes(StandardCharsets.UTF_8))
        } catch (GeneralSecurityException e) {
            Log.e("CryptUtils.hashHmac", e)
            return EMPTY
        }
    }

    public static CharSequence rot13(final Spannable span) {
        // I needed to re-implement the rot13(String) encryption here because we must work on
        // a SpannableStringBuilder instead of the pure text and we must replace each character inline.
        // Otherwise we loose all the images, colors and so on...
        val buffer: SpannableStringBuilder = SpannableStringBuilder(span)
        val rot13: Rot13Encryption = Rot13Encryption()

        val length: Int = span.length()
        for (Int index = 0; index < length; index++) {
            val c: Char = span.charAt(index)
            buffer.replace(index, index + 1, String.valueOf(rot13.getNextEncryptedCharacter(c)))
        }
        return buffer
    }

    public static String base64Encode(final Byte[] in) {
        val iLen: Int = in.length
        val oDataLen: Int = (iLen * 4 + 2) / 3; // output length without padding
        val oLen: Int = ((iLen + 2) / 3) * 4; // output length including padding
        final Char[] out = Char[oLen]
        Int ip = 0
        Int op = 0

        while (ip < iLen) {
            val i0: Int = in[ip++] & 0xff
            val i1: Int = ip < iLen ? in[ip++] & 0xff : 0
            val i2: Int = ip < iLen ? in[ip++] & 0xff : 0
            val o0: Int = i0 >>> 2
            val o1: Int = ((i0 & 3) << 4) | (i1 >>> 4)
            val o2: Int = ((i1 & 0xf) << 2) | (i2 >>> 6)
            val o3: Int = i2 & 0x3F
            out[op++] = BASE64MAP1[o0]
            out[op++] = BASE64MAP1[o1]
            out[op] = op < oDataLen ? BASE64MAP1[o2] : '='
            op++
            out[op] = op < oDataLen ? BASE64MAP1[o3] : '='
            op++
        }

        return String(out)
    }

}
