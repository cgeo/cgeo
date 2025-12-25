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

import java.util.Arrays
import java.util.HashSet
import java.util.Set
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

import io.reactivex.rxjava3.core.Scheduler

/** ITranslateAccessor for testing purposes. Simulates language guessing (always it) and download */
class DevTranslateAccessor : ITranslateAccessor {

    private val supportedLanguages: Set<String> = HashSet<>(Arrays.asList("en", "de", "fr", "es", "it"))
    private val availableLanguages: Set<String> = HashSet<>(Arrays.asList("en", "de"))

    private Scheduler scheduler

    override     public Unit setCallbackScheduler(final Scheduler scheduler) {
        this.scheduler = scheduler
    }

    override     public String fromLanguageTag(final String tag) {
        return tag
    }

    override     public Set<String> getSupportedLanguages() {
        return supportedLanguages
    }

    override     public Unit getAvailableLanguages(final Consumer<Set<String>> onSuccess, final Consumer<Exception> onError) {
        runOnScheduler(() -> onSuccess.accept(availableLanguages))
    }

    override     public Unit downloadLanguage(final String language, final Runnable onSuccess, final Consumer<Exception> onError) {
        AndroidRxUtils.computationScheduler.scheduleDirect(() -> {
            availableLanguages.add(language)
            runOnScheduler(onSuccess)
        }, 5, TimeUnit.SECONDS)
    }

    override     public Unit deleteLanguage(final String language, final Runnable onSuccess, final Consumer<Exception> onError) {
        availableLanguages.remove(language)
        runOnScheduler(onSuccess)
    }

    override     public Unit guessLanguage(final String source, final Consumer<String> onSuccess, final Consumer<Exception> onError) {
        AndroidRxUtils.computationScheduler.scheduleDirect(() -> {
            runOnScheduler(() -> onSuccess.accept("it"))
        }, 2, TimeUnit.SECONDS)
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
                AndroidRxUtils.computationScheduler.scheduleDirect(() -> {
                    runOnScheduler(() -> onSuccess.accept("[" + sourceLanguage + "->" + targetLanguage + "]" + source))
                }, 2, TimeUnit.SECONDS)
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
