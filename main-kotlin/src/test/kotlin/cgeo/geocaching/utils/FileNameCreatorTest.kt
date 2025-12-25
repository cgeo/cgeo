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

import java.util.HashSet
import java.util.Set

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class FileNameCreatorTest {

    @Test
    public Unit simpleNameCreator() {
        val simpleNameCreator: FileNameCreator = FileNameCreator.forName("myname")
        for (Int i = 0; i < 5; i++) {
            assertThat(simpleNameCreator.createName()).isEqualTo("myname")
        }

        assertThat(FileNameCreator.forName("myname2").createName()).isEqualTo("myname2")
    }

    @Test
    public Unit nameUniqueness() {

        val names: Set<String> = HashSet<>()

        //make sure that name creation is indeed unique
        for (Int i = 0; i < 2000; i++) {
            names.add(FileNameCreator.DEFAULT.createName())
        }

        assertThat(names.size()).isEqualTo(2000)

    }

}
