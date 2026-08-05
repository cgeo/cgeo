package cgeo.geocaching.utils.offlinetranslate;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.ListenerHelper;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.reactivex.rxjava3.disposables.Disposable;

/**
 * Manages Language Models which are downloaded to local device
 */
public class TranslationModelManager {

    //SINGLETON
    private static final TranslationModelManager INSTANCE = new TranslationModelManager();

    private static final String LOGPRAEFIX = "[TranslationModelManager]: ";

    private final Object mutex = new Object();
    private final Set<String> supportedLanguages;
    private final Set<String> availableLanguages = new HashSet<>();
    private final Set<String> pendingLanguages = new HashSet<>();

    private final ListenerHelper<Runnable> listeners = new ListenerHelper<>();

    public static TranslationModelManager get() {
        return INSTANCE;
    }

    private TranslationModelManager() {
        //retrieve supported Languages
        supportedLanguages = Collections.unmodifiableSet(TranslateAccessor.get().getSupportedLanguages());
        Log.iForce(LOGPRAEFIX + "Supported languages: " + supportedLanguages);

        //trigger periodical retrieval of available languages and pending status adjustment
        AndroidRxUtils.runPeriodically(AndroidRxUtils.computationScheduler, this::refreshAvailableAndPending, 0, 6000);
    }

    public void initialize() {
        //empty on purpose. Calling it ensures that singleton instance is created
    }

    private void refreshAvailableAndPending() {
        //check available languages
        TranslateAccessor.get().getAvailableLanguages(languages -> {
            synchronized (mutex) {
                if (!this.availableLanguages.equals(languages)) {
                    availableLanguages.clear();
                    availableLanguages.addAll(languages);
                    pendingLanguages.removeAll(availableLanguages);
                    callListeners();
                    Log.iForce(LOGPRAEFIX + "Available languages:" + availableLanguages + "/pending:" + pendingLanguages);
                }
            }
        }, error -> Log.e(LOGPRAEFIX + " could not retrieve available models", error));
    }

    public Set<String> getSupportedLanguages() {
        return supportedLanguages;
    }

    public boolean isAvailable(final String language) {
        synchronized (mutex) {
            return availableLanguages.contains(language);
        }
    }

    public boolean isPending(final String language) {
        synchronized (mutex) {
            return pendingLanguages.contains(language);
        }
    }

    public boolean isAvailableOrPending(final String language) {
        synchronized (mutex) {
            return pendingLanguages.contains(language) || availableLanguages.contains(language);
        }
    }


    public Disposable registerListener(final Runnable onChange) {
        return listeners.addListenerWithDisposable(onChange);
    }

    private void callListeners() {
        synchronized (mutex) {
            listeners.execute(null, Runnable::run);
        }
    }

    public void downloadLanguage(final String language) {
        synchronized (mutex) {
            if (!supportedLanguages.contains(language) || availableLanguages.contains(language) || pendingLanguages.contains(language)) {
                return;
            }
            pendingLanguages.add(language);
        }
        final String languageString = LocalizationUtils.getLocaleDisplayName(language, true, true);
        Log.iForce(LOGPRAEFIX + "Starting download for language " + language);
        TranslateAccessor.get().downloadLanguage(language, () -> {
            Log.iForce(LOGPRAEFIX + "Download of language " + language + " successful");
            ViewUtils.showToast(null, TextParam.id(R.string.translator_model_download_success, languageString), true);
            synchronized (mutex) {
                availableLanguages.add(language);
                callListeners();
            }
        }, ex -> {
            Log.e(LOGPRAEFIX + "Download of language '" + language + "' failed", ex);
            ViewUtils.showToast(null, TextParam.id(R.string.translator_model_download_error, languageString, ex == null ? "-" : ex.getMessage()), true);
        });
    }

    public void deleteLanguage(final String language) {
        synchronized (mutex) {
            if (!availableLanguages.remove(language)) {
                return;
            }
            callListeners();
        }
        Log.iForce(LOGPRAEFIX + "Starting deletion of language " + language);
        TranslateAccessor.get().deleteLanguage(language,
                () -> Log.iForce(LOGPRAEFIX + "Language " + language + " deleted successfully"),
                ex -> Log.e(LOGPRAEFIX + "Failed to delete Language " + language, ex)
        );
    }

}
