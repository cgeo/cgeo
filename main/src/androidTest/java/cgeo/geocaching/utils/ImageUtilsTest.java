package cgeo.geocaching.utils;

import cgeo.geocaching.test.CgeoTestUtils;
import cgeo.geocaching.test.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ImageUtilsTest {

    private static final String ICON64 = "iVBORw0KGgoAAAANSUhEUgAAAAkAAAAJCAYAAADgkQYQAAAABGdBTUEAALGPC/xhBQAAAAZiS0dEAP8A/wD/oL2nkwAAAAlwSFlzAAALEwAACxMBAJqcGAAAAB1pVFh0Q29tbWVudAAAAAAAQ3JlYXRlZCB3aXRoIEdJTVBkLmUHAAABAElEQVQY002NvUpDQRQG52yEILn+IAYCClaS4tqKtZ1dbHwNuaS00dYmQmrfwMZGRBAtrMQHSCMpTCMqaIx6k909e6wCme4bPhhhhuc8d8PxeC+ZPW33++9T72ZPvdFow1SvStX9We8sz2U6FlQPLUYqqkW30VgGsKJAbur1g5pzXYMosC5giEgy+6hAtUzpqLLq3O8q7M6bbZkqmpKllExUa9+q2gvhbJrKLrLsdkVkxwABShg9eN86nUzunXU6AD/O+2EMgdJ7fAiY92EtxjcAJ+02JyKNkNLmawj9xxiLlxAu/2JcWoQmwBxAFT4Hqq1rs687GADnx9DMnOsD/AMJ54Nj8e9zcgAAAABJRU5ErkJggg==";

    @Test
    public void testBase64decoding() throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageUtils.decodeBase64ToStream(ICON64, outputStream);
        final byte[] decodedImage = outputStream.toByteArray();
        outputStream.close();
        assertThat(decodedImage.length).as("decoded image size").isEqualTo(409);
        final InputStream originalStream = CgeoTestUtils.getResourceStream(R.raw.small_file);
        final byte[] originalImage = new byte[409];
        assertThat(409).as("original image has the right size (consistency check)").isEqualTo(originalStream.read(originalImage));
        originalStream.close();
        assertThat(decodedImage).as("decoded base64 image").isEqualTo(originalImage);
    }

    @Test
    public void forEachImageUrl() {
        assertThat(getImageUrls(null)).isEmpty();
        assertThat(getImageUrls("<img src=\"test\">")).containsExactly("test");

        assertThat(getImageUrls("<span id=\"ctl00_ContentBody_ShortDescription\"><p>ATTENTION!! NEW COORDINATES!! N 45° 32.527 W 073° 12.132</p>\n" +
                "<p>This cache is located in the beautiful Bosquets, it's a great place for a quiet walk or searching for mushrooms in the fall <img alt=\"wink\" src=\"https://www.geocaching.com/static/js/CKEditor/4.4.0/plugins/smiley/images/wink_smile.png\" title=\"wink\" style=\"height:23px;width:23px;\"></p>\n" +
                "\n" +
                "</span>")).containsExactly("https://www.geocaching.com/static/js/CKEditor/4.4.0/plugins/smiley/images/wink_smile.png");

    }

    private List<String> getImageUrls(final String html) {
        final List<String> result = new ArrayList<>();
        ImageUtils.forEachImageUrlInHtml(s -> result.add(s), html);
        return result;
    }

}
