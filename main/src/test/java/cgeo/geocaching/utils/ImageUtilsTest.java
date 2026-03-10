package cgeo.geocaching.utils;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ImageUtilsTest {

    @Test
    public void fixDropboxImageUrlOldFormatWithDl0() {
        assertThat(ImageUtils.fixDropboxImageUrl("https://www.dropbox.com/s/abc123/photo.jpg?dl=0"))
                .isEqualTo("https://www.dropbox.com/s/abc123/photo.jpg?dl=1");
    }

    @Test
    public void fixDropboxImageUrlOldFormatWithDl1() {
        assertThat(ImageUtils.fixDropboxImageUrl("https://www.dropbox.com/s/abc123/photo.jpg?dl=1"))
                .isEqualTo("https://www.dropbox.com/s/abc123/photo.jpg?dl=1");
    }

    @Test
    public void fixDropboxImageUrlOldFormatWithoutDlParam() {
        assertThat(ImageUtils.fixDropboxImageUrl("https://www.dropbox.com/s/abc123/photo.jpg"))
                .isEqualTo("https://www.dropbox.com/s/abc123/photo.jpg?dl=1");
    }

    @Test
    public void fixDropboxImageUrlNewSclFormatWithDl0() {
        assertThat(ImageUtils.fixDropboxImageUrl("https://www.dropbox.com/scl/fi/abc123/photo.jpg?rlkey=xyz&dl=0"))
                .isEqualTo("https://www.dropbox.com/scl/fi/abc123/photo.jpg?rlkey=xyz&dl=1");
    }

    @Test
    public void fixDropboxImageUrlNewSclFormatWithDl1() {
        assertThat(ImageUtils.fixDropboxImageUrl("https://www.dropbox.com/scl/fi/abc123/photo.jpg?rlkey=xyz&dl=1"))
                .isEqualTo("https://www.dropbox.com/scl/fi/abc123/photo.jpg?rlkey=xyz&dl=1");
    }

    @Test
    public void fixDropboxImageUrlNewSclFormatWithoutDlParam() {
        assertThat(ImageUtils.fixDropboxImageUrl("https://www.dropbox.com/scl/fi/abc123/photo.jpg?rlkey=xyz"))
                .isEqualTo("https://www.dropbox.com/scl/fi/abc123/photo.jpg?rlkey=xyz&dl=1");
    }

    @Test
    public void fixDropboxImageUrlDlDropboxUsercontentUnchanged() {
        assertThat(ImageUtils.fixDropboxImageUrl("https://dl.dropboxusercontent.com/s/abc123/photo.jpg"))
                .isEqualTo("https://dl.dropboxusercontent.com/s/abc123/photo.jpg");
    }

    @Test
    public void fixDropboxImageUrlDlDropboxComUnchanged() {
        assertThat(ImageUtils.fixDropboxImageUrl("https://dl.dropbox.com/s/abc123/photo.jpg?dl=1"))
                .isEqualTo("https://dl.dropbox.com/s/abc123/photo.jpg?dl=1");
    }

    @Test
    public void fixDropboxImageUrlNonDropboxUrlUnchanged() {
        assertThat(ImageUtils.fixDropboxImageUrl("https://img.geocaching.com/cache/abc123.jpg"))
                .isEqualTo("https://img.geocaching.com/cache/abc123.jpg");
    }

    @Test
    public void fixDropboxImageUrlDlParamInFilenameNotAffected() {
        // dl=0 in a filename path segment should not be mistakenly replaced
        assertThat(ImageUtils.fixDropboxImageUrl("https://www.dropbox.com/s/abc123/photo_dl=0.jpg?rlkey=xyz"))
                .isEqualTo("https://www.dropbox.com/s/abc123/photo_dl=0.jpg?rlkey=xyz&dl=1");
    }

}
