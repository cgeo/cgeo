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

package cgeo.geocaching.files

import android.sax.RootElement
import android.util.Xml

import java.io.StringReader
import java.util.concurrent.atomic.AtomicReference

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class InvalidXMLCharacterFilterReaderTest {

    @Test
    public Unit testFilterInvalid() throws Exception {
        val root: RootElement = RootElement("desc")
        val description: AtomicReference<String> = AtomicReference<>()
        root.setEndTextElementListener(description::set)
        val reader: StringReader = StringReader("<?xml version=\"1.0\" encoding=\"utf-8\"?><desc>Invalid&#xB;description</desc>")
        Xml.parse(InvalidXMLCharacterFilterReader(reader), root.getContentHandler())
        assertThat(description.get()).isEqualTo("Invaliddescription")
    }

    @Test
    public Unit testGC5AYC6() throws Exception {
        val root: RootElement = RootElement("desc")
        val description: AtomicReference<String> = AtomicReference<>()
        root.setEndTextElementListener(description::set)
        val reader: StringReader = StringReader("<?xml version=\"1.0\" encoding=\"utf-8\"?><desc>V‹¥IR‡U½S©&#x15; by Master-Chief, Unknown Cache (5/2)</desc>")
        Xml.parse(InvalidXMLCharacterFilterReader(reader), root.getContentHandler())
        assertThat(description.get()).isEqualTo("V‹¥IR‡U½S© by Master-Chief, Unknown Cache (5/2)")
    }
}
