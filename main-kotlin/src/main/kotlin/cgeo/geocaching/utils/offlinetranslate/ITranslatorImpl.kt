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

package cgeo.geocaching.utils.offlinetranslate

import java.util.function.Consumer

interface ITranslatorImpl {

    String getSourceLanguage()

    String getTargetLanguage()

    Unit translate(String source, Consumer<String> onSuccess, Consumer<Exception> onError)

    Unit dispose()
}
