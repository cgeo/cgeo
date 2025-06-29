package cgeo.geocaching.utils.offlinetranslate;

import cgeo.geocaching.utils.ListenerHelper;
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
        //trigger retrieval of available languages
        RemoteModelManager.getInstance().getDownloadedModels(TranslateRemoteModel.class).addOnSuccessListener(remoteModels -> {
            synchronized (mutex) {
                for (TranslateRemoteModel model : remoteModels) {
                    availableLanguages.add(model.getLanguage());
                }
                Log.iForce(LOGPRAEFIX + "available languages:" + availableLanguages);
                callListeners();
            }
        }).addOnFailureListener(ex -> {
            Log.e(LOGPRAEFIX + " could not retrieve available models", ex);
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
                    if (task.isSuccessful()) {
                        Log.iForce(LOGPRAEFIX + "Download of language " + language + " successful");
                        availableLanguages.add(language);
                    } else {
                        Log.e(LOGPRAEFIX + "Download of language " + language + "failed", task.getException());
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
