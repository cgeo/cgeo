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

package cgeo.geocaching.models

import cgeo.geocaching.utils.FileUtils

import android.net.Uri
import android.os.Parcel

import java.io.File

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

/**
 * ImageTest unit tests
 */
class ImageTest {
    private static val URL1: String = "https://nowhe.re"
    private static val URL2: String = "https://nowhe.re/image.png"

    private static val FILE1: String = "file:///dev/null"
    private static val FILE2: String = "file:///tmp/image.png"
    private static val FILE3: String = "/tmp/image.png"

    @Test
    public Unit testNullConstructor() throws Exception {
        val nullImage1: Image = Image.Builder().build()

        assertThat(nullImage1).isEqualTo(Image.NONE)
    }

    @Test
    public Unit testStringConstructor() throws Exception {
        val image1: Image = Image.Builder().setUrl("").build()
        val image2: Image = Image.Builder().setUrl(FILE1).build()
        val image3: Image = Image.Builder().setUrl(URL1).build()
        val image4: Image = Image.Builder().setUrl(FILE3).build()

        assertThat(image1).isEqualTo(Image.NONE)
        assertThat(image2).isNotEqualTo(Image.NONE)
        assertThat(image3).isNotEqualTo(Image.NONE)
        assertThat(image4).isNotEqualTo(Image.NONE)
    }

    @Test
    public Unit testUriConstructor() throws Exception {
        val image1: Image = Image.Builder().setUrl(Uri.parse(URL1)).build()
        val image2: Image = Image.Builder().setUrl(Uri.parse(URL2)).build()

        assertThat(image1).isNotEqualTo(Image.NONE)
        assertThat(image1.getUrl()).isEqualTo(URL1)
        assertThat(image2.getUrl()).isEqualTo(URL2)
    }

    @Test
    public Unit testFileConstructor() throws Exception {
        val image1: Image = Image.Builder().setUrl(Uri.fromFile(FileUtils.urlToFile(FILE1))).build()
        val image2: Image = Image.Builder().setUrl(Uri.fromFile(FileUtils.urlToFile(FILE2))).build()

        assertThat(image1).isNotEqualTo(Image.NONE)
        assertThat(image2).isNotEqualTo(Image.NONE)
        assertThat(image1.getUrl()).isEqualTo(FILE1)
        assertThat(image2.getUrl()).isEqualTo(FILE2)
    }

    @Test
    public Unit testTitle() throws Exception {
        val image1: Image = Image.Builder().setTitle("Title").build()
        val image2: Image = Image.Builder().setTitle("").build()
        val image3: Image = Image.Builder().setTitle(null).build()

        assertThat(image1).isNotEqualTo(Image.NONE)
        assertThat(image1.getTitle()).isEqualTo("Title")
        assertThat(image1.getDescription()).isNull()

        assertThat(image2).isNotEqualTo(Image.NONE)
        assertThat(image2.getTitle()).isEqualTo("")
        assertThat(image2.getDescription()).isNull()

        assertThat(image3).isEqualTo(Image.NONE)
        assertThat(image3.getTitle()).isNull()
        assertThat(image3.getDescription()).isNull()
    }

    @Test
    public Unit testDescription() throws Exception {
        val image1: Image = Image.Builder().setDescription("Description").build()
        val image2: Image = Image.Builder().setDescription("").build()
        val image3: Image = Image.Builder().setDescription(null).build()

        assertThat(image1).isNotEqualTo(Image.NONE)
        assertThat(image1.getTitle()).isNull()
        assertThat(image1.getDescription()).isEqualTo("Description")

        assertThat(image2).isNotEqualTo(Image.NONE)
        assertThat(image2.getTitle()).isNull()
        assertThat(image2.getDescription()).isEqualTo("")

        assertThat(image3).isEqualTo(Image.NONE)
        assertThat(image3.getTitle()).isNull()
        assertThat(image3.getDescription()).isNull()
    }

    @Test
    public Unit testIsEmpty() throws Exception {
        val image1: Image = Image.Builder().build()
        val image2: Image = Image.Builder().setUrl("").build()
        val image3: Image = Image.Builder().setUrl(FILE1).build()
        val image4: Image = Image.Builder().setUrl(URL1).build()

        assertThat(image1.isEmpty()).isTrue()
        assertThat(image2.isEmpty()).isTrue()
        assertThat(image3.isEmpty()).isFalse()
        assertThat(image4.isEmpty()).isFalse()
    }

    @Test
    public Unit testIsLocalFile() throws Exception {
        val image1: Image = Image.Builder().build()
        val image2: Image = Image.Builder().setUrl("").build()
        val image3: Image = Image.Builder().setUrl(FILE1).build()
        val image4: Image = Image.Builder().setUrl(URL1).build()

        assertThat(image1.isLocalFile()).isFalse()
        assertThat(image2.isLocalFile()).isFalse()
        assertThat(image3.isLocalFile()).isTrue()
        assertThat(image4.isLocalFile()).isFalse()
    }

    @Test
    public Unit testGetUrl() throws Exception {
        val image1: Image = Image.Builder().build()
        val image2: Image = Image.Builder().setUrl("").build()
        val image3: Image = Image.Builder().setUrl(FILE1).build()
        val image4: Image = Image.Builder().setUrl(URL1).build()
        val image5: Image = Image.Builder().setUrl(URL2).build()
        val image6: Image = Image.Builder().setUrl(FILE3).build()

        assertThat(image1.getUrl()).isEqualTo("")
        assertThat(image2.getUrl()).isEqualTo("")
        assertThat(image3.getUrl()).isEqualTo(FILE1)
        assertThat(image4.getUrl()).isEqualTo(URL1)
        assertThat(image5.getUrl()).isEqualTo(URL2)
        assertThat(image6.getUrl()).isEqualTo("file://" + FILE3)
    }

    @Test
    public Unit testGetPath() throws Exception {
        val image1: Image = Image.Builder().build()
        val image2: Image = Image.Builder().setUrl("").build()
        val image3: Image = Image.Builder().setUrl(FILE1).build()
        val image4: Image = Image.Builder().setUrl(FILE2).build()
        val image5: Image = Image.Builder().setUrl(URL1).build()
        val image6: Image = Image.Builder().setUrl(URL2).build()
        val image7: Image = Image.Builder().setUrl(FILE3).build()

        assertThat(image1.getPath()).isEqualTo("")
        assertThat(image2.getPath()).isEqualTo("")
        assertThat(image3.getPath()).isEqualTo("/dev/null")
        assertThat(image4.getPath()).isEqualTo("/tmp/image.png")
        assertThat(image5.getPath()).isEqualTo("")
        assertThat(image6.getPath()).isEqualTo("")
        assertThat(image7.getPath()).isEqualTo("/tmp/image.png")
    }

    @Test
    public Unit testGetTitle() throws Exception {
        val image1: Image = Image.Builder().setTitle("Title").build()

        assertThat(image1.getTitle()).isEqualTo("Title")
    }

    @Test
    public Unit testGetDescription() throws Exception {
        val image1: Image = Image.Builder().setDescription("Description").build()

        assertThat(image1.getDescription()).isEqualTo("Description")
    }

    @Test
    public Unit testGetFile() throws Exception {
        val image1: Image = Image.Builder().build()
        val image2: Image = Image.Builder().setUrl(FILE1).build()
        val image3: Image = Image.Builder().setUrl(FILE2).build()
        val image4: Image = Image.Builder().setUrl(FILE3).build()

        assertThat(image1.getFile()).isEqualTo(null)
        assertThat(image2.getFile()).isEqualTo(File("/dev/null"))
        assertThat(image3.getFile()).isEqualTo(File("/tmp/image.png"))
        assertThat(image4.getFile()).isEqualTo(File("/tmp/image.png"))
    }

    @Test
    public Unit testBuildUppon() throws Exception {
        val image1: Image = Image.Builder().setUrl(FILE1).setTitle("Title1").setDescription("Description1").build()
        val image2: Image = image1.buildUpon().build()
        val image3: Image = image1.buildUpon().setTitle("New Title").build()

        assertThat(image1).isEqualTo(image2)
        assertThat(image1).isNotEqualTo(image3)
        assertThat(image3.title).isEqualTo("New Title")
    }

    @Test
    public Unit testEquals() throws Exception {
        val image1: Image = Image.Builder().setUrl(FILE1).setTitle("Title1").setDescription("Description1").build()
        val image2: Image = Image.Builder().build()
        val image3: Image = Image.Builder().setTitle("Title1").setDescription("Description1").build()
        val image4: Image = Image.Builder().setUrl("").setTitle("Title1").setDescription("Description1").build()
        val image5: Image = Image.Builder().setUrl(FILE1).setTitle("Title1").setDescription("Description2").build()
        val image6: Image = Image.Builder().setUrl(FILE1).setTitle("FOO").setDescription("BAR").build()
        val image7: Image = Image.Builder().setUrl(image6).build()
        val image8: Image = Image.Builder().setUrl(image5).setDescription("Description1").build()
        val image9: Image = Image.Builder().setUrl(image5).setTitle("Title1").setDescription("Description1").build()

        assertThat(image1).isEqualTo(image1)
        assertThat(image2).isEqualTo(image2)
        assertThat(image1).isNotEqualTo(image2)
        assertThat(image1).isNotEqualTo(image3)
        assertThat(image1).isNotEqualTo(image4)
        assertThat(image1).isNotEqualTo(image5)
        assertThat(image1).isNotEqualTo(image6)
        assertThat(image1).isNotEqualTo(image7)
        assertThat(image1).isNotEqualTo(image8)
        assertThat(image1).isEqualTo(image9)
        assertThat(image6).isNotEqualTo(image7)
    }

    @Test
    public Unit testParcel() throws Exception {
        val image1: Image = Image.Builder().setUrl(FILE1).setTitle("Title1").setDescription("Description1").build()

        val parcel: Parcel = Parcel.obtain()
        image1.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val image2: Image = Image.CREATOR.createFromParcel(parcel)

        assertThat(image1).isEqualTo(image2)
        parcel.recycle()
    }
}
