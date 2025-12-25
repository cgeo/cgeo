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


import java.util.Set
import java.util.function.Consumer

import io.reactivex.rxjava3.core.Scheduler

interface ITranslateAccessor {

    Unit setCallbackScheduler(Scheduler scheduler)

    String fromLanguageTag(String tag)

    Set<String> getSupportedLanguages()

    Unit getAvailableLanguages(Consumer<Set<String>> onSuccess, Consumer<Exception> onError)

    Unit downloadLanguage(String language, Runnable onSuccess, Consumer<Exception> onError)

    Unit deleteLanguage(String language, Runnable onSuccess, Consumer<Exception> onError)

    Unit guessLanguage(String source, Consumer<String> onSuccess, Consumer<Exception> onError)

    ITranslatorImpl getTranslator(String sourceLanguage, String targetLanguage)

    Unit getTranslatorWithDownload(String sourceLanguage, String targetLanguage, Consumer<ITranslatorImpl> onSuccess, Consumer<Exception> onError)

}
