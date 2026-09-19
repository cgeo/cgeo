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
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.disposables.Disposable;

/**
 * Manages Language Models which are downloaded to local device
 */
public class TranslationModelManager {

    //SINGLETON
    private static final TranslationModelManager INSTANCE = new TranslationModelManager();
    private static final AtomicBoolean INITIAL_REFRESH_TRIGGERED = new AtomicBoolean(false);

    private static final String LOGPRAEFIX = "[TranslationModelManager]: ";

    /** rate at which models on the device are re-checked - only while a download is pending */
    private static final long PENDING_REFRESH_RATE_MS = 6000;

    private final Object mutex = new Object();
    private final Set<String> supportedLanguages;
    private final Set<String> availableLanguages = new HashSet<>();
    private final Set<String> pendingLanguages = new HashSet<>();

    /** periodic refresh, running exactly while {@link #pendingLanguages} is non-empty. Guarded by {@link #mutex} */
    private Disposable pendingRefresh = null;

    private final ListenerHelper<Runnable> listeners = new ListenerHelper<>();

    public static TranslationModelManager get() {
        if (!INITIAL_REFRESH_TRIGGERED.get() && INITIAL_REFRESH_TRIGGERED.compareAndSet(false, true)) {
            // read what is already on the device, once. Deliberately not done from the constructor: that one runs
            // from the static initializer above, so a task scheduled there would get hold of an instance which is
            // not fully constructed yet.
            AndroidRxUtils.computationScheduler.scheduleDirect(INSTANCE::refreshAvailableAndPending);
        }
        return INSTANCE;
    }

    private TranslationModelManager() {
        //retrieve supported Languages
        supportedLanguages = Collections.unmodifiableSet(TranslateAccessor.get().getSupportedLanguages());
        Log.iForce(LOGPRAEFIX + "Supported languages: " + supportedLanguages);
    }

    public void initialize() {
        //empty on purpose. Calling it ensures that singleton instance is created and its models are read
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
                stopPendingRefreshIfDone();
            }
        }, error -> Log.e(LOGPRAEFIX + " could not retrieve available models", error));
    }

    /** starts the periodic refresh if it is not running yet. To be called while holding {@link #mutex} */
    private void startPendingRefresh() {
        if (pendingRefresh == null) {
            pendingRefresh = AndroidRxUtils.runPeriodically(AndroidRxUtils.computationScheduler,
                    this::refreshAvailableAndPending, PENDING_REFRESH_RATE_MS, PENDING_REFRESH_RATE_MS);
        }
    }

    /** stops the periodic refresh once nothing is pending any more. To be called while holding {@link #mutex} */
    private void stopPendingRefreshIfDone() {
        if (pendingRefresh != null && pendingLanguages.isEmpty()) {
            pendingRefresh.dispose();
            pendingRefresh = null;
        }
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
            startPendingRefresh();
        }
        final String languageString = LocalizationUtils.getLocaleDisplayName(language, true, true);
        Log.iForce(LOGPRAEFIX + "Starting download for language " + language);
        TranslateAccessor.get().downloadLanguage(language, () -> {
            Log.iForce(LOGPRAEFIX + "Download of language " + language + " successful");
            ViewUtils.showToast(null, TextParam.id(R.string.translator_model_download_success, languageString), true);
            synchronized (mutex) {
                availableLanguages.add(language);
                pendingLanguages.remove(language);
                stopPendingRefreshIfDone();
                callListeners();
            }
        }, ex -> {
            Log.e(LOGPRAEFIX + "Download of language '" + language + "' failed", ex);
            ViewUtils.showToast(null, TextParam.id(R.string.translator_model_download_error, languageString, ex == null ? "-" : ex.getMessage()), true);
            synchronized (mutex) {
                // without this the language would stay pending forever: isAvailableOrPending() would keep
                // reporting a download in progress, and the periodic refresh would never stop running
                pendingLanguages.remove(language);
                stopPendingRefreshIfDone();
                callListeners();
            }
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
