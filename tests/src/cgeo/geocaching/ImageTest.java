package cgeo.geocaching;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.CGeoTestCase;
import cgeo.geocaching.utils.FileUtils;

import android.net.Uri;
import android.os.Parcel;

import java.io.File;

/**
 * ImageTest unit tests
 */
public class ImageTest extends CGeoTestCase {
    private static final String URL1 = "https://nowhe.re";
    private static final String URL2 = "https://nowhe.re/image.png";

    private static final String FILE1 = "file:///dev/null";
    private static final String FILE2 = "file:///tmp/image.png";

    public void testNullConstructor() throws Exception {
        final Image nullImage1 = new Image.Builder((String) null).build();
        final Image nullImage2 = new Image.Builder((Uri) null).build();
        final Image nullImage3 = new Image.Builder((File) null).build();

        final Image nullImage4 = new Image.Builder(null, null).build();
        final Image nullImage5 = new Image.Builder(null, null, null).build();

        assertThat(nullImage1).isEqualTo(Image.NONE);
        assertThat(nullImage2).isEqualTo(Image.NONE);
        assertThat(nullImage3).isEqualTo(Image.NONE);
        assertThat(nullImage4).isEqualTo(Image.NONE);
        assertThat(nullImage5).isEqualTo(Image.NONE);
    }

    public void testStringConstructor() throws Exception {
        final Image image1 = new Image.Builder("").build();
        final Image image2 = new Image.Builder(FILE1).build();
        final Image image3 = new Image.Builder(URL1).build();

        assertThat(image1).isEqualTo(Image.NONE);
        assertThat(image2).isNotEqualTo(Image.NONE);
        assertThat(image3).isNotEqualTo(Image.NONE);
    }

    public void testUriConstructor() throws Exception {
        final Image image1 = new Image.Builder(Uri.parse(URL1)).build();
        final Image image2 = new Image.Builder(Uri.parse(URL2)).build();

        assertThat(image1).isNotEqualTo(Image.NONE);
        assertThat(image1.getUrl()).isEqualTo(URL1);
        assertThat(image2.getUrl()).isEqualTo(URL2);
    }

    public void testFileConstructor() throws Exception {
        final Image image1 = new Image.Builder(FileUtils.urlToFile(FILE1)).build();
        final Image image2 = new Image.Builder(FileUtils.urlToFile(FILE2)).build();

        assertThat(image1).isNotEqualTo(Image.NONE);
        assertThat(image2).isNotEqualTo(Image.NONE);
        assertThat(image1.getUrl()).isEqualTo(FILE1);
        assertThat(image2.getUrl()).isEqualTo(FILE2);
        assertThat(image2.getUrl()).isEqualTo(FILE2);
    }

    public void testTitle() throws Exception {
        final Image image1 = new Image.Builder(null, "Title").build();

        assertThat(image1.getTitle()).isEqualTo("Title");
    }

    public void testDescription() throws Exception {
        final Image image1 = new Image.Builder(null, "Title", "Description").build();

        assertThat(image1.getTitle()).isEqualTo("Title");
        assertThat(image1.getDescription()).isEqualTo("Description");
    }

    public void testIsEmpty() throws Exception {
        final Image image1 = new Image.Builder(null, "Title", "Description").build();
        final Image image2 = new Image.Builder("").build();
        final Image image3 = new Image.Builder(FILE1).build();
        final Image image4 = new Image.Builder(URL1).build();

        assertThat(image1.isEmpty()).isTrue();
        assertThat(image2.isEmpty()).isTrue();
        assertThat(image3.isEmpty()).isFalse();
        assertThat(image4.isEmpty()).isFalse();
    }

    public void testIsLocalFile() throws Exception {
        final Image image1 = new Image.Builder(null, "Title", "Description").build();
        final Image image2 = new Image.Builder("").build();
        final Image image3 = new Image.Builder(FILE1).build();
        final Image image4 = new Image.Builder(URL1).build();

        assertThat(image1.isLocalFile()).isFalse();
        assertThat(image2.isLocalFile()).isFalse();
        assertThat(image3.isLocalFile()).isTrue();
        assertThat(image4.isLocalFile()).isFalse();
    }

    public void testLocalFile() throws Exception {
        final Image image1 = new Image.Builder(FILE1, "Title", "Description").build();

        assertThat(image1.localFile()).isEqualTo(FileUtils.urlToFile(FILE1));
    }

    public void testGetUrl() throws Exception {
        final Image image1 = new Image.Builder(null, "Title", "Description").build();
        final Image image2 = new Image.Builder("").build();
        final Image image3 = new Image.Builder(FILE1).build();
        final Image image4 = new Image.Builder(URL1).build();
        final Image image5 = new Image.Builder(URL2).build();

        assertThat(image1.getUrl()).isEqualTo("");
        assertThat(image2.getUrl()).isEqualTo("");
        assertThat(image3.getUrl()).isEqualTo(FILE1);
        assertThat(image4.getUrl()).isEqualTo(URL1);
        assertThat(image5.getUrl()).isEqualTo(URL2);
    }

    public void testGetPath() throws Exception {
        final Image image1 = new Image.Builder(null, "Title", "Description").build();
        final Image image2 = new Image.Builder("").build();
        final Image image3 = new Image.Builder(FILE1).build();
        final Image image4 = new Image.Builder(FILE2).build();
        final Image image5 = new Image.Builder(URL1).build();
        final Image image6 = new Image.Builder(URL2).build();

        assertThat(image1.getPath()).isEqualTo("");
        assertThat(image2.getPath()).isEqualTo("");
        assertThat(image3.getPath()).isEqualTo("/dev/null");
        assertThat(image4.getPath()).isEqualTo("/tmp/image.png");
        assertThat(image5.getPath()).isEqualTo("");
        assertThat(image6.getPath()).isEqualTo("");
    }

    public void testGetTitle() throws Exception {
        final Image image1 = new Image.Builder(null, "Title", "Description").build();

        assertThat(image1.getTitle()).isEqualTo("Title");
    }

    public void testGetDescription() throws Exception {
        final Image image1 = new Image.Builder(null, "Title", "Description").build();

        assertThat(image1.getDescription()).isEqualTo("Description");
    }

    public void testGetFile() throws Exception {
        final Image image1 = new Image.Builder(null, "Title", "Description").build();
        final Image image2 = new Image.Builder(FILE1).build();

        assertThat(image1.getFile()).isEqualTo(null);
        assertThat(image2.getFile()).isEqualTo(new File(FILE1));
    }

    public void testEquals() throws Exception {
        final Image image1 = new Image.Builder(null, "Title1", "Description1").build();
        final Image image2 = new Image.Builder(null, "Title2", "Description2").build();
        final Image image3 = new Image.Builder(null, "Title1", "Description1").build();
        final Image image4 = new Image.Builder("", "Title1", "Description1").build();
        final Image image5 = new Image.Builder(FILE1, "Title1", "Description1").build();
        final Image image6 = new Image.Builder(FILE1, "FOO", "BAR").build();

        assertThat(image1).isEqualTo(image1);
        assertThat(image1).isNotEqualTo(image2);
        assertThat(image1).isEqualTo(image3);
        assertThat(image1).isEqualTo(image4);
        assertThat(image1).isNotEqualTo(image5);
        assertThat(image1).isNotEqualTo(image6);
    }

    public void testParcel() throws Exception {
        final Image image1 = new Image.Builder(FILE1, "Title1", "Description1").build();

        final Parcel parcel = Parcel.obtain();
        image1.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        Image image2 = Image.CREATOR.createFromParcel(parcel);

        assertThat(image1).isEqualTo(image2);
    }
}