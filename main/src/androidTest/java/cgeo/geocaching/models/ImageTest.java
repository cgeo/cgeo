package cgeo.geocaching.models;

import cgeo.CGeoTestCase;
import cgeo.geocaching.utils.FileUtils;

import android.net.Uri;
import android.os.Parcel;

import java.io.File;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * ImageTest unit tests
 */
public class ImageTest extends CGeoTestCase {
    private static final String URL1 = "https://nowhe.re";
    private static final String URL2 = "https://nowhe.re/image.png";

    private static final String FILE1 = "file:///dev/null";
    private static final String FILE2 = "file:///tmp/image.png";
    private static final String FILE3 = "/tmp/image.png";

    public static void testNullConstructor() throws Exception {
        final Image nullImage1 = new Image.Builder().build();

        assertThat(nullImage1).isEqualTo(Image.NONE);
    }

    public static void testStringConstructor() throws Exception {
        final Image image1 = new Image.Builder().setUrl("").build();
        final Image image2 = new Image.Builder().setUrl(FILE1).build();
        final Image image3 = new Image.Builder().setUrl(URL1).build();
        final Image image4 = new Image.Builder().setUrl(FILE3).build();

        assertThat(image1).isEqualTo(Image.NONE);
        assertThat(image2).isNotEqualTo(Image.NONE);
        assertThat(image3).isNotEqualTo(Image.NONE);
        assertThat(image4).isNotEqualTo(Image.NONE);
    }

    public static void testUriConstructor() throws Exception {
        final Image image1 = new Image.Builder().setUrl(Uri.parse(URL1)).build();
        final Image image2 = new Image.Builder().setUrl(Uri.parse(URL2)).build();

        assertThat(image1).isNotEqualTo(Image.NONE);
        assertThat(image1.getUrl()).isEqualTo(URL1);
        assertThat(image2.getUrl()).isEqualTo(URL2);
    }

    public static void testFileConstructor() throws Exception {
        final Image image1 = new Image.Builder().setUrl(Uri.fromFile(FileUtils.urlToFile(FILE1))).build();
        final Image image2 = new Image.Builder().setUrl(Uri.fromFile(FileUtils.urlToFile(FILE2))).build();

        assertThat(image1).isNotEqualTo(Image.NONE);
        assertThat(image2).isNotEqualTo(Image.NONE);
        assertThat(image1.getUrl()).isEqualTo(FILE1);
        assertThat(image2.getUrl()).isEqualTo(FILE2);
    }

    public static void testTitle() throws Exception {
        final Image image1 = new Image.Builder().setTitle("Title").build();
        final Image image2 = new Image.Builder().setTitle("").build();
        final Image image3 = new Image.Builder().setTitle(null).build();

        assertThat(image1).isNotEqualTo(Image.NONE);
        assertThat(image1.getTitle()).isEqualTo("Title");
        assertThat(image1.getDescription()).isNull();

        assertThat(image2).isNotEqualTo(Image.NONE);
        assertThat(image2.getTitle()).isEqualTo("");
        assertThat(image2.getDescription()).isNull();

        assertThat(image3).isEqualTo(Image.NONE);
        assertThat(image3.getTitle()).isNull();
        assertThat(image3.getDescription()).isNull();
    }

    public static void testDescription() throws Exception {
        final Image image1 = new Image.Builder().setDescription("Description").build();
        final Image image2 = new Image.Builder().setDescription("").build();
        final Image image3 = new Image.Builder().setDescription(null).build();

        assertThat(image1).isNotEqualTo(Image.NONE);
        assertThat(image1.getTitle()).isNull();
        assertThat(image1.getDescription()).isEqualTo("Description");

        assertThat(image2).isNotEqualTo(Image.NONE);
        assertThat(image2.getTitle()).isNull();
        assertThat(image2.getDescription()).isEqualTo("");

        assertThat(image3).isEqualTo(Image.NONE);
        assertThat(image3.getTitle()).isNull();
        assertThat(image3.getDescription()).isNull();
    }

    public static void testIsEmpty() throws Exception {
        final Image image1 = new Image.Builder().build();
        final Image image2 = new Image.Builder().setUrl("").build();
        final Image image3 = new Image.Builder().setUrl(FILE1).build();
        final Image image4 = new Image.Builder().setUrl(URL1).build();

        assertThat(image1.isEmpty()).isTrue();
        assertThat(image2.isEmpty()).isTrue();
        assertThat(image3.isEmpty()).isFalse();
        assertThat(image4.isEmpty()).isFalse();
    }

    public static void testIsLocalFile() throws Exception {
        final Image image1 = new Image.Builder().build();
        final Image image2 = new Image.Builder().setUrl("").build();
        final Image image3 = new Image.Builder().setUrl(FILE1).build();
        final Image image4 = new Image.Builder().setUrl(URL1).build();

        assertThat(image1.isLocalFile()).isFalse();
        assertThat(image2.isLocalFile()).isFalse();
        assertThat(image3.isLocalFile()).isTrue();
        assertThat(image4.isLocalFile()).isFalse();
    }

    public static void testGetUrl() throws Exception {
        final Image image1 = new Image.Builder().build();
        final Image image2 = new Image.Builder().setUrl("").build();
        final Image image3 = new Image.Builder().setUrl(FILE1).build();
        final Image image4 = new Image.Builder().setUrl(URL1).build();
        final Image image5 = new Image.Builder().setUrl(URL2).build();
        final Image image6 = new Image.Builder().setUrl(FILE3).build();

        assertThat(image1.getUrl()).isEqualTo("");
        assertThat(image2.getUrl()).isEqualTo("");
        assertThat(image3.getUrl()).isEqualTo(FILE1);
        assertThat(image4.getUrl()).isEqualTo(URL1);
        assertThat(image5.getUrl()).isEqualTo(URL2);
        assertThat(image6.getUrl()).isEqualTo("file://" + FILE3);
    }

    public static void testGetPath() throws Exception {
        final Image image1 = new Image.Builder().build();
        final Image image2 = new Image.Builder().setUrl("").build();
        final Image image3 = new Image.Builder().setUrl(FILE1).build();
        final Image image4 = new Image.Builder().setUrl(FILE2).build();
        final Image image5 = new Image.Builder().setUrl(URL1).build();
        final Image image6 = new Image.Builder().setUrl(URL2).build();
        final Image image7 = new Image.Builder().setUrl(FILE3).build();

        assertThat(image1.getPath()).isEqualTo("");
        assertThat(image2.getPath()).isEqualTo("");
        assertThat(image3.getPath()).isEqualTo("/dev/null");
        assertThat(image4.getPath()).isEqualTo("/tmp/image.png");
        assertThat(image5.getPath()).isEqualTo("");
        assertThat(image6.getPath()).isEqualTo("");
        assertThat(image7.getPath()).isEqualTo("/tmp/image.png");
    }

    public static void testGetTitle() throws Exception {
        final Image image1 = new Image.Builder().setTitle("Title").build();

        assertThat(image1.getTitle()).isEqualTo("Title");
    }

    public static void testGetDescription() throws Exception {
        final Image image1 = new Image.Builder().setDescription("Description").build();

        assertThat(image1.getDescription()).isEqualTo("Description");
    }

    public static void testGetFile() throws Exception {
        final Image image1 = new Image.Builder().build();
        final Image image2 = new Image.Builder().setUrl(FILE1).build();
        final Image image3 = new Image.Builder().setUrl(FILE2).build();
        final Image image4 = new Image.Builder().setUrl(FILE3).build();

        assertThat(image1.getFile()).isEqualTo(null);
        assertThat(image2.getFile()).isEqualTo(new File("/dev/null"));
        assertThat(image3.getFile()).isEqualTo(new File("/tmp/image.png"));
        assertThat(image4.getFile()).isEqualTo(new File("/tmp/image.png"));
    }

    public static void testBuildUppon() throws Exception {
        final Image image1 = new Image.Builder().setUrl(FILE1).setTitle("Title1").setDescription("Description1").build();
        final Image image2 = image1.buildUpon().build();
        final Image image3 = image1.buildUpon().setTitle("New Title").build();

        assertThat(image1).isEqualTo(image2);
        assertThat(image1).isNotEqualTo(image3);
        assertThat(image3.title).isEqualTo("New Title");
    }

    public static void testEquals() throws Exception {
        final Image image1 = new Image.Builder().setUrl(FILE1).setTitle("Title1").setDescription("Description1").build();
        final Image image2 = new Image.Builder().build();
        final Image image3 = new Image.Builder().setTitle("Title1").setDescription("Description1").build();
        final Image image4 = new Image.Builder().setUrl("").setTitle("Title1").setDescription("Description1").build();
        final Image image5 = new Image.Builder().setUrl(FILE1).setTitle("Title1").setDescription("Description2").build();
        final Image image6 = new Image.Builder().setUrl(FILE1).setTitle("FOO").setDescription("BAR").build();
        final Image image7 = new Image.Builder().setUrl(image6).build();
        final Image image8 = new Image.Builder().setUrl(image5).setDescription("Description1").build();
        final Image image9 = new Image.Builder().setUrl(image5).setTitle("Title1").setDescription("Description1").build();

        assertThat(image1).isEqualTo(image1);
        assertThat(image2).isEqualTo(image2);
        assertThat(image1).isNotEqualTo(image2);
        assertThat(image1).isNotEqualTo(image3);
        assertThat(image1).isNotEqualTo(image4);
        assertThat(image1).isNotEqualTo(image5);
        assertThat(image1).isNotEqualTo(image6);
        assertThat(image1).isNotEqualTo(image7);
        assertThat(image1).isNotEqualTo(image8);
        assertThat(image1).isEqualTo(image9);
        assertThat(image6).isNotEqualTo(image7);
    }

    public static void testParcel() throws Exception {
        final Image image1 = new Image.Builder().setUrl(FILE1).setTitle("Title1").setDescription("Description1").build();

        final Parcel parcel = Parcel.obtain();
        image1.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        final Image image2 = Image.CREATOR.createFromParcel(parcel);

        assertThat(image1).isEqualTo(image2);
        parcel.recycle();
    }
}
