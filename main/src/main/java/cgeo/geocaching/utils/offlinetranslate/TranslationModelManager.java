package cgeo.geocaching.utils.offlinetranslate;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.ListenerHelper;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;

import android.content.Context;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.common.sdkinternal.MlKitContext;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;

/**
 * Manages Language Models which are downloaded to local device
 * <br>
 * If need be, this class can be extended in the future to provide a general manager for downloaded
 * MLKit model.
 */
public class TranslationModelManager {

    //SINGLETON
    private static final TranslationModelManager INSTANCE = new TranslationModelManager();

    private static final String LOGPRAEFIX = "[TranslationModelManager]: ";

    private final Object mutex = new Object();
    private Set<String> supportedLanguages = Collections.emptySet();
    private final Set<String> availableLanguages = new HashSet<>();
    private final Set<String> pendingLanguages = new HashSet<>();

    private final ListenerHelper<Supplier<Boolean>> listeners = new ListenerHelper<>();

    public static TranslationModelManager get() {
        return INSTANCE;
    }

    private TranslationModelManager() {
        //no instances other than the Singleton!
    }

    public void initialize(final Context context) {

        Log.iForce(LOGPRAEFIX + "--- Initialization started");
        //ensure that MlKit is initialized
        MlKitContext.initializeIfNeeded(context);

        //retrieve supported languages
        supportedLanguages = Collections.unmodifiableSet(new HashSet<>(TranslateLanguage.getAllLanguages()));
        Log.iForce(LOGPRAEFIX + "Supported languages: " + supportedLanguages);
        //trigger periodical retrieval of available languages and pending status adjustment
        AndroidRxUtils.runPeriodically(AndroidRxUtils.computationScheduler, this::refreshAvailableAndPending, 0, 6000);
    }

    private void refreshAvailableAndPending() {
        //check available languages
        RemoteModelManager.getInstance().getDownloadedModels(TranslateRemoteModel.class).addOnCompleteListener(
            AndroidRxUtils.fromScheduler(AndroidRxUtils.computationScheduler), task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                final Set<String> availableLanguagesNew = new HashSet<>();
                for (TranslateRemoteModel model : task.getResult()) {
                    availableLanguagesNew.add(model.getLanguage());
                }
                synchronized (mutex) {
                    if (!this.availableLanguages.equals(availableLanguagesNew)) {
                        availableLanguages.clear();
                        availableLanguages.addAll(availableLanguagesNew);
                        pendingLanguages.removeAll(availableLanguages);
                        Log.iForce(LOGPRAEFIX + "available languages:" + availableLanguages + "/pending:" + pendingLanguages);
                        callListeners();
                    }
                }
            } else {
                Log.e(LOGPRAEFIX + " could not retrieve available models", task.getException());
            }
        });
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


    public void registerListener(final Supplier<Boolean> onChange) {
        listeners.addListener(onChange);
    }

    private void callListeners() {
        synchronized (mutex) {
            listeners.executeWithRemove(null, Supplier::get);
        }
    }

    public void downloadLanguage(final String language) {
        synchronized (mutex) {
            if (!supportedLanguages.contains(language) || availableLanguages.contains(language) || pendingLanguages.contains(language)) {
                return;
            }
            pendingLanguages.add(language);
        }
        Log.iForce(LOGPRAEFIX + "Starting download for language " + language);
        RemoteModelManager.getInstance()
            .download(new TranslateRemoteModel.Builder(language).build(), new DownloadConditions.Builder().build())
            .addOnCompleteListener(task -> {
                synchronized (mutex) {
                    final String languageString = LocalizationUtils.getLocaleDisplayName(language, true, true);
                    if (task.isSuccessful()) {
                        Log.iForce(LOGPRAEFIX + "Download of language " + language + " successful");
                        ViewUtils.showToast(null, TextParam.id(R.string.translator_model_download_success, languageString), true);
                        availableLanguages.add(language);
                    } else {
                        final Exception ex = task.getException();
                        Log.e(LOGPRAEFIX + "Download of language '" + language + "' failed", ex);
                        ViewUtils.showToast(null, TextParam.id(R.string.translator_model_download_error, languageString, ex == null ? "-" : ex.getMessage()), true);
                    }
                    pendingLanguages.remove(language);
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
        final TranslateRemoteModel model = new TranslateRemoteModel.Builder(language).build();
        RemoteModelManager.getInstance().deleteDownloadedModel(model)
            .addOnFailureListener(e -> Log.e(LOGPRAEFIX + "Failed to delete Language " + language, e))
            .addOnSuccessListener(r -> Log.iForce(LOGPRAEFIX + "Language " + language + " deleted successfully"));
    }

}
