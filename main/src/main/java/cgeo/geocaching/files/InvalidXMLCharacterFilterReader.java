package cgeo.geocaching.files;

import cgeo.geocaching.utils.xml.XmlUtils;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

/**
 * Filter reader which can filter out invalid XML characters and character references.
 */
public class InvalidXMLCharacterFilterReader extends FilterReader {

    public InvalidXMLCharacterFilterReader(final Reader in) {
        super(in);
    }

    /**
     * Every overload of {@link Reader#read()} method delegates to this one so
     * it is enough to override only this one. <br />
     * To skip invalid characters this method shifts only valid chars to left
     * and returns decreased value of the original read method. So after last
     * valid character there will be some unused chars in the buffer.
     *
     * @return Number of read valid characters or {@code -1} if end of the
     * underling reader was reached.
     */
    @Override
    public int read(final char[] cbuf, final int off, final int len) throws IOException {
        final int read = super.read(cbuf, off, len);
        // check for end
        if (read == -1) {
            return -1;
        }

        // target position
        int pos = off - 1;

        int entityStart = -1;
        int readPos = off;
        while (readPos < off + read) {
            boolean useChar = true;
            int charCount = 1;

            switch (cbuf[readPos]) {
                case '&':
                    pos++;
                    entityStart = readPos;
                    break;

                case ';':
                    pos++;
                    if (entityStart >= 0) {
                        final int entityLength = readPos - entityStart + 1;
                        if (entityLength <= 8) { // &#xFFFD;
                            final String entity = new String(cbuf, entityStart, entityLength);
                            if (Strings.CS.startsWith(entity, "&#")) {
                                final String numberString = StringUtils.substringBetween(entity, "&#", ";");
                                final int value;
                                if (Strings.CS.startsWith(numberString, "x")) {
                                    value = Integer.parseInt(numberString.substring(1), 16);
                                } else {
                                    value = Integer.parseInt(numberString);
                                }
                                if (!XmlUtils.isValidXmlCodePoint(value)) {
                                    pos -= entityLength;
                                    useChar = false;
                                }
                            }
                        }
                    }
                    break;

                default:
                    final int codePoint = Character.codePointAt(cbuf, readPos, off + read);
                    charCount = Character.charCount(codePoint);

                    if (XmlUtils.isValidXmlCodePoint(codePoint)) {
                        for (int i = 0; i < charCount; i++) {
                            pos++;
                            if (pos < readPos + i) {
                                cbuf[pos] = cbuf[readPos + i];
                            }
                        }
                    } else {
                        readPos += charCount;
                        continue;
                    }
            }

            // copy, and skip unwanted characters
            if (pos < readPos && useChar) {
                cbuf[pos] = cbuf[readPos];
            }

            readPos += charCount;
        }

        return pos - off + 1;
    }
}