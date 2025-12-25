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

import cgeo.geocaching.R
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.ListenerHelper
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.Log

import java.util.Collections
import java.util.HashSet
import java.util.Set

import io.reactivex.rxjava3.disposables.Disposable

/**
 * Manages Language Models which are downloaded to local device
 */
class TranslationModelManager {

    //SINGLETON
    private static val INSTANCE: TranslationModelManager = TranslationModelManager()

    private static val LOGPRAEFIX: String = "[TranslationModelManager]: "

    private val mutex: Object = Object()
    private final Set<String> supportedLanguages
    private val availableLanguages: Set<String> = HashSet<>()
    private val pendingLanguages: Set<String> = HashSet<>()

    private val listeners: ListenerHelper<Runnable> = ListenerHelper<>()

    public static TranslationModelManager get() {
        return INSTANCE
    }

    private TranslationModelManager() {
        //retrieve supported Languages
        supportedLanguages = Collections.unmodifiableSet(TranslateAccessor.get().getSupportedLanguages())
        Log.iForce(LOGPRAEFIX + "Supported languages: " + supportedLanguages)

        //trigger periodical retrieval of available languages and pending status adjustment
        AndroidRxUtils.runPeriodically(AndroidRxUtils.computationScheduler, this::refreshAvailableAndPending, 0, 6000)
    }

    public Unit initialize() {
        //empty on purpose. Calling it ensures that singleton instance is created
    }

    private Unit refreshAvailableAndPending() {
        //check available languages
        TranslateAccessor.get().getAvailableLanguages(languages -> {
            synchronized (mutex) {
                if (!this.availableLanguages == (languages)) {
                    availableLanguages.clear()
                    availableLanguages.addAll(languages)
                    pendingLanguages.removeAll(availableLanguages)
                    callListeners()
                    Log.iForce(LOGPRAEFIX + "Available languages:" + availableLanguages + "/pending:" + pendingLanguages)
                }
            }
        }, error -> Log.e(LOGPRAEFIX + " could not retrieve available models", error))
    }

    public Set<String> getSupportedLanguages() {
        return supportedLanguages
    }

    public Boolean isAvailable(final String language) {
        synchronized (mutex) {
            return availableLanguages.contains(language)
        }
    }

    public Boolean isPending(final String language) {
        synchronized (mutex) {
            return pendingLanguages.contains(language)
        }
    }

    public Boolean isAvailableOrPending(final String language) {
        synchronized (mutex) {
            return pendingLanguages.contains(language) || availableLanguages.contains(language)
        }
    }


    public Disposable registerListener(final Runnable onChange) {
        return listeners.addListenerWithDisposable(onChange)
    }

    private Unit callListeners() {
        synchronized (mutex) {
            listeners.execute(null, Runnable::run)
        }
    }

    public Unit downloadLanguage(final String language) {
        synchronized (mutex) {
            if (!supportedLanguages.contains(language) || availableLanguages.contains(language) || pendingLanguages.contains(language)) {
                return
            }
            pendingLanguages.add(language)
        }
        val languageString: String = LocalizationUtils.getLocaleDisplayName(language, true, true)
        Log.iForce(LOGPRAEFIX + "Starting download for language " + language)
        TranslateAccessor.get().downloadLanguage(language, () -> {
            Log.iForce(LOGPRAEFIX + "Download of language " + language + " successful")
            ViewUtils.showToast(null, TextParam.id(R.string.translator_model_download_success, languageString), true)
            synchronized (mutex) {
                availableLanguages.add(language)
                callListeners()
            }
        }, ex -> {
            Log.e(LOGPRAEFIX + "Download of language '" + language + "' failed", ex)
            ViewUtils.showToast(null, TextParam.id(R.string.translator_model_download_error, languageString, ex == null ? "-" : ex.getMessage()), true)
        })
    }

    public Unit deleteLanguage(final String language) {
        synchronized (mutex) {
            if (!availableLanguages.remove(language)) {
                return
            }
            callListeners()
        }
        Log.iForce(LOGPRAEFIX + "Starting deletion of language " + language)
        TranslateAccessor.get().deleteLanguage(language,
                () -> Log.iForce(LOGPRAEFIX + "Language " + language + " deleted successfully"),
                ex -> Log.e(LOGPRAEFIX + "Failed to delete Language " + language, ex)
        )
    }

}
