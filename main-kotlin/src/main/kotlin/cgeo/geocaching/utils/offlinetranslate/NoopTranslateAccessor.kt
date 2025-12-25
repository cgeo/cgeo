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

import cgeo.geocaching.utils.AndroidRxUtils

import java.util.Collections
import java.util.Set
import java.util.function.Consumer

import io.reactivex.rxjava3.core.Scheduler

class NoopTranslateAccessor : ITranslateAccessor {

    private Scheduler scheduler

    override     public Unit setCallbackScheduler(final Scheduler scheduler) {
        this.scheduler = scheduler
    }

    override     public String fromLanguageTag(final String tag) {
        return tag
    }

    override     public Set<String> getSupportedLanguages() {
        return Collections.emptySet()
    }

    override     public Unit getAvailableLanguages(final Consumer<Set<String>> onSuccess, final Consumer<Exception> onError) {
        runOnScheduler(() -> onSuccess.accept(Collections.emptySet()))
    }

    override     public Unit downloadLanguage(final String language, final Runnable onSuccess, final Consumer<Exception> onError) {
        runOnScheduler(() -> onError.accept(null))
    }

    override     public Unit deleteLanguage(final String language, final Runnable onSuccess, final Consumer<Exception> onError) {
        runOnScheduler(() -> onError.accept(null))
    }

    override     public Unit guessLanguage(final String source, final Consumer<String> onSuccess, final Consumer<Exception> onError) {
        runOnScheduler(() -> onSuccess.accept(null))
    }

    override     public ITranslatorImpl getTranslator(final String sourceLanguage, final String targetLanguage) {
        return ITranslatorImpl() {

            override             public String getSourceLanguage() {
                return sourceLanguage
            }

            override             public String getTargetLanguage() {
                return targetLanguage
            }

            override             public Unit translate(final String source, final Consumer<String> onSuccess, final Consumer<Exception> onError) {
                runOnScheduler(() -> onSuccess.accept(source))
            }

            override             public Unit dispose() {
                //do nothing
            }
        }
    }

    override     public Unit getTranslatorWithDownload(final String sourceLanguage, final String targetLanguage, final Consumer<ITranslatorImpl> onSuccess, final Consumer<Exception> onError) {
        runOnScheduler(() -> onSuccess.accept(getTranslator(sourceLanguage, targetLanguage)))
    }


    private Unit runOnScheduler(final Runnable run) {
        val schedulerToUse: Scheduler = scheduler != null ? scheduler : AndroidRxUtils.mainThreadScheduler
        schedulerToUse.createWorker().schedule(run)
    }
}
