package cgeo.geocaching;

import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;
import cgeo.geocaching.utils.ImageUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class ImageUtilsTest extends AbstractResourceInstrumentationTestCase {

    private static final String icon64 = "iVBORw0KGgoAAAANSUhEUgAAAAkAAAAJCAYAAADgkQYQAAAABGdBTUEAALGPC/xhBQAAAAZiS0dEAP8A/wD/oL2nkwAAAAlwSFlzAAALEwAACxMBAJqcGAAAAB1pVFh0Q29tbWVudAAAAAAAQ3JlYXRlZCB3aXRoIEdJTVBkLmUHAAABAElEQVQY002NvUpDQRQG52yEILn+IAYCClaS4tqKtZ1dbHwNuaS00dYmQmrfwMZGRBAtrMQHSCMpTCMqaIx6k909e6wCme4bPhhhhuc8d8PxeC+ZPW33++9T72ZPvdFow1SvStX9We8sz2U6FlQPLUYqqkW30VgGsKJAbur1g5pzXYMosC5giEgy+6hAtUzpqLLq3O8q7M6bbZkqmpKllExUa9+q2gvhbJrKLrLsdkVkxwABShg9eN86nUzunXU6AD/O+2EMgdJ7fAiY92EtxjcAJ+02JyKNkNLmawj9xxiLlxAu/2JcWoQmwBxAFT4Hqq1rs687GADnx9DMnOsD/AMJ54Nj8e9zcgAAAABJRU5ErkJggg==";

    public void testBase64decoding() throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageUtils.decodeBase64ToStream(icon64, outputStream);
        final byte[] decodedImage = outputStream.toByteArray();
        outputStream.close();
        assertEquals("decoded image has the right size", 409, decodedImage.length);
        final InputStream originalStream = getResourceStream(R.raw.small_file);
        final byte[] originalImage = new byte[409];
        assertEquals("original image has the right size (consistency check)", 409, originalStream.read(originalImage));
        originalStream.close();
        assertTrue("decoded base64 image is similar to original file data",
                Arrays.equals(originalImage, decodedImage));
    }

}
