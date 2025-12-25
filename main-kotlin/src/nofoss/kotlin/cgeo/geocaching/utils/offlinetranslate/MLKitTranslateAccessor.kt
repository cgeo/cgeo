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

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.utils.AndroidRxUtils

import java.util.HashSet
import java.util.Set
import java.util.concurrent.Executor
import java.util.function.Consumer

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.common.sdkinternal.MlKitContext
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import io.reactivex.rxjava3.core.Scheduler

class MLKitTranslateAccessor : ITranslateAccessor {

    private Scheduler scheduler

    public MLKitTranslateAccessor() {
        //ensure that MlKit is initialized
        MlKitContext.initializeIfNeeded(CgeoApplication.getInstance())
    }

    override     public Unit setCallbackScheduler(final Scheduler scheduler) {
        this.scheduler = scheduler
    }

    override     public String fromLanguageTag(final String tag) {
        return TranslateLanguage.fromLanguageTag(tag)
    }


    override     public Set<String> getSupportedLanguages() {
        return HashSet<>(TranslateLanguage.getAllLanguages())
    }

    override     public Unit getAvailableLanguages(final Consumer<Set<String>> onSuccess, final Consumer<Exception> onError) {
        execute(RemoteModelManager.getInstance().getDownloadedModels(TranslateRemoteModel.class), onError, result -> {
            val availableLanguagesNew: Set<String> = HashSet<>()
            for (TranslateRemoteModel model : result) {
                availableLanguagesNew.add(model.getLanguage())
            }
            onSuccess.accept(availableLanguagesNew)
        })
    }

    override     public Unit downloadLanguage(final String language, final Runnable onSuccess, final Consumer<Exception> onError) {
        execute(RemoteModelManager.getInstance().download(TranslateRemoteModel.Builder(language).build(), DownloadConditions.Builder().build()), onError, result -> {
            onSuccess.run()
        })
    }

    override     public Unit deleteLanguage(final String language, final Runnable onSuccess, final Consumer<Exception> onError) {
        execute(RemoteModelManager.getInstance().deleteDownloadedModel(TranslateRemoteModel.Builder(language).build()), onError, result -> {
            onSuccess.run()
        })
    }

    override     public Unit guessLanguage(final String source, final Consumer<String> onSuccess, final Consumer<Exception> onError) {
        val text: String = source.replaceAll("[\\s\\ufffc]+", " ").trim()
        execute(LanguageIdentification.getClient().identifyLanguage(text), onError, lngCode -> {
                onSuccess.accept("und" == (lngCode) ? null : lngCode)
        })
    }

    override     public ITranslatorImpl getTranslator(final String sourceLanguage, final String targetLanguage) {
        return wrap(sourceLanguage, targetLanguage, createTranslator(sourceLanguage, targetLanguage))
    }

    override     public Unit getTranslatorWithDownload(final String sourceLanguage, final String targetLanguage, final Consumer<ITranslatorImpl> onSuccess, final Consumer<Exception> onError) {
        val t: Translator = createTranslator(sourceLanguage, targetLanguage)
        execute(t.downloadModelIfNeeded(DownloadConditions.Builder().build()), onError,
            x -> onSuccess.accept(wrap(sourceLanguage, targetLanguage, t)))
    }


    private Translator createTranslator(final String sourceLanguage, final String targetLanguage) {
        val options: TranslatorOptions = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(targetLanguage)
                .build()

        return Translation.getClient(options)
    }

    private ITranslatorImpl wrap(final String srcLng, final String trgLng, final Translator translator) {
        return ITranslatorImpl() {

            override             public String getSourceLanguage() {
                return srcLng
            }

            override             public String getTargetLanguage() {
                return trgLng
            }

            override             public Unit translate(final String source, final Consumer<String> onSuccess, final Consumer<Exception> onError) {
                execute(translator.translate(source), onError, onSuccess)
            }

            override             public Unit dispose() {
                translator.close()
            }
        }
    }

    private Executor getExecutor() {
        val schedulerToUse: Scheduler = scheduler != null ? scheduler : AndroidRxUtils.mainThreadScheduler
        return runnable -> schedulerToUse.createWorker().schedule(runnable)
    }

    private  <T> Unit execute(final Task<T> task, final Consumer<Exception> onError, final Consumer<T> onSuccess) {
        task.addOnCompleteListener(getExecutor(), t -> {
            if (t.isSuccessful()) {
                onSuccess.accept(t.getResult())
            } else {
                onError.accept(t.getException())
            }
        })
    }

}
