package cgeo.geocaching.files;

import org.apache.commons.lang3.StringUtils;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Filter reader which can filter out invalid XML characters and character references.
 *
 */
public class InvalidXMLCharacterFilterReader extends FilterReader
{

    public InvalidXMLCharacterFilterReader(Reader in) {
        super(in);
    }

    /**
     * Every overload of {@link Reader#read()} method delegates to this one so
     * it is enough to override only this one. <br />
     * To skip invalid characters this method shifts only valid chars to left
     * and returns decreased value of the original read method. So after last
     * valid character there will be some unused chars in the buffer.
     *
     * @return Number of read valid characters or <code>-1</code> if end of the
     *         underling reader was reached.
     */
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int read = super.read(cbuf, off, len);
        // check for end
        if (read == -1) {
            return -1;
        }
        // target position
        int pos = off - 1;

        int entityStart = -1;
        for (int readPos = off; readPos < off + read; readPos++) {
            boolean useChar = true;
            switch (cbuf[readPos]) {
                case '&':
                    pos++;
                    entityStart = readPos;
                    break;
                case ';':
                    pos++;
                    if (entityStart >= 0) {
                        int entityLength = readPos - entityStart + 1;
                        if (entityLength <= 5) {
                            String entity = new String(cbuf, entityStart, entityLength);
                            if (StringUtils.startsWith(entity, "&#")) {
                                String numberString = StringUtils.substringBetween(entity, "&#", ";");
                                final int value;
                                if (StringUtils.startsWith(numberString, "x")) {
                                    value = Integer.parseInt(numberString.substring(1), 16);
                                }
                                else {
                                    value = Integer.parseInt(numberString);
                                }
                                if (!isValidXMLChar((char) value)) {
                                    pos -= entityLength;
                                    useChar = false;
                                }
                            }
                        }
                    }
                    break;
                default:
                    if (isValidXMLChar(cbuf[readPos])) {
                        pos++;
                    } else {
                        continue;
                    }
            }
            // copy, and skip unwanted characters
            if (pos < readPos && useChar) {
                cbuf[pos] = cbuf[readPos];
            }
        }
        return pos - off + 1;
    }

    private static boolean isValidXMLChar(final char c) {
        return c == 0x9 || c == 0xA || c == 0xD || (c >= 0x20 && c <= 0xD7FF) || (c >= 0xE000 && c <= 0xFFFD);
    }
}