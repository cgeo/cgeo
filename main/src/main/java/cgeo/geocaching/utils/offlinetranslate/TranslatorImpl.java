package cgeo.geocaching.utils.offlinetranslate;

import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.TranslatorOptions;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.lang3.StringUtils;
import static com.google.mlkit.nl.languageid.LanguageIdentifier.UNDETERMINED_LANGUAGE_TAG;

/**
 * An instance of this class can translate texts from exactly one source language to exactly one
 * target language. It implements the workflow to come to a working translator model including
 * e.g waiting for model download and guessing source language on the way. Following transitions
 * need to be supported: CREATED (initial) ->  SRC_DETECTED -> READY (and ERROR in case of
 * problems).
 * <br>
 * This class encapsulates the offline translation capabilities of Google MLKit.
 * It has package access and should SOLELY be used by class {@link Translator}
 */
class TranslatorImpl {

    private static final String LOGPRAEFIX = "[TranslatorImpl]: ";

    private String sourceLanguage; // null during detection
    private final String targetLanguage;
    private final Object mutex = new Object();
    private BiConsumer<State, TranslatorImpl> stateListener;
    private State state = State.CREATED;
    private TranslatorError errorState = TranslatorError.NONE;

    private com.google.mlkit.nl.translate.Translator translator;

    public enum State {
        CREATED, SRC_DETECTED, READY, ERROR
    }

    TranslatorImpl(final String sourceLanguage, final String targetLanguage, final String sourceDetectionText, final BiConsumer<State, TranslatorImpl> stateListener) {
        this.targetLanguage = targetLanguage;
        this.stateListener = stateListener;

        executeOnWorker(() -> {
            findSourceLanguage(sourceLanguage, sourceDetectionText, srcLng -> {
                this.sourceLanguage = srcLng;
                changeState(State.SRC_DETECTED);
                checkAndWaitForLanguage(this.sourceLanguage, true, () -> {
                    checkAndWaitForLanguage(this.targetLanguage, false, () -> {
                        translator = getTranslator(this.sourceLanguage, this.targetLanguage);
                        changeState(State.READY);
                    });
                });
            });
        });
    }

    /** May return null before state SOURCE_DETECTED is reached */
    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public TranslatorError getErrorState() {
        return errorState;
    }

    /**
     * Translate a text using this translator. Can only be used once the Translator is in state READY
     * If there's an error in translation, then onTranslated will be called with null.
     * <br>
     * onTranslated will always be called on main application thread.
     * */
    public void translate(final String original, final Consumer<String> onTranslated) {
        final com.google.mlkit.nl.translate.Translator translator = this.translator;
        if (translator == null) {
            Log.w(LOGPRAEFIX + " translate called in wrong state (" + state + ")");
            executeOnWorker(() -> onTranslated.accept(null));
            return;
        }

        translator.translate(original).addOnCompleteListener(task -> {
            //this is executed on Main thread
            if (task.getException() != null) {
                Log.w(LOGPRAEFIX + "Error on translating '" + original + "' [" + this + "]", task.getException());
            }
            onTranslated.accept(task.getResult());
        });
    }

    @NonNull
    @Override
    public String toString() {
        return LOGPRAEFIX + "srcLng:" + sourceLanguage + "/trgLng:" + targetLanguage + "/state:" + state + "/errorState:" + errorState;
    }

    /** free all resources hold by this class. Behaviour of class methods after disposing it is undefined */
    public void dispose() {
        Log.iForce(LOGPRAEFIX + "dispose [" + this + "]");
        //delete consumers for pending translations, they may contain  references to eg activities
        synchronized (mutex) {
            this.state = State.ERROR;
            stateListener = null;
        }
    }


    /** STATE method: finds out source language (either by parameter or via anlyzing a text sample) */
    private void findSourceLanguage(final String sourceCandidate, final String detectText, final Consumer<String> onSuccess) {
        //find out source language
        if (sourceCandidate != null) {
            executeOnWorker(() -> onStateSuccess("Source found", () -> onSuccess.accept(sourceCandidate)));
        } else if (StringUtils.isBlank(detectText)) {
            executeOnWorker(() -> onStateError("No text to determine language", TranslatorError.OTHER, null));
        } else {
            // try to detect source language from text
            LanguageIdentification.getClient().identifyLanguage(detectText).addOnSuccessListener(getWorkerExecutor(), lngCode -> {
                    if (UNDETERMINED_LANGUAGE_TAG.equals(lngCode)) {
                        onStateError("Unable to identify language", TranslatorError.SOURCE_LANGUAGE_NOT_IDENTIFIED, null);
                    } else {
                        onStateSuccess("Found source language: " + lngCode, () -> onSuccess.accept(lngCode));
                    }
                })
                .addOnFailureListener(getWorkerExecutor(), e -> onStateError("Exception", TranslatorError.OTHER, e));
        }
    }

    /** STATE method: checks whether a language code is both valid and available */
    private void checkAndWaitForLanguage(final String lngCode, final boolean isSource, final Runnable onSuccess) {
        final String lngCodeMsg = "'" + lngCode + "'(" + (isSource ? "source" : "target") + "language)";
        if (TranslationModelManager.get().isAvailable(lngCode)) {
            executeOnWorker(onSuccess);
        } else if (!TranslationModelManager.get().isPending(lngCode)) {
            executeOnWorker(() -> {
                if (TranslationModelManager.get().getSupportedLanguages().contains(lngCode)) {
                    onStateError(lngCodeMsg + " is unavailable+unpending", TranslatorError.LANGUAGE_MODEL_UNAVAILABLE, null);
                } else {
                    onStateError(lngCodeMsg + " is either unsupported", TranslatorError.LANGUAGE_UNSUPPORTED, null);
                }
            });
        } else {
            TranslationModelManager.get().registerListener(() -> {
                if (TranslationModelManager.get().isAvailable(lngCode)) {
                    executeOnWorker(() -> onStateSuccess(lngCodeMsg + " now available", onSuccess));
                    return true;
                }
                if (!TranslationModelManager.get().isPending(lngCode)) {
                    executeOnWorker(() -> onStateError(lngCodeMsg + " no longer pending", TranslatorError.LANGUAGE_MODEL_UNAVAILABLE, null));
                }
                return false;
            });
        }
    }

    /** StATE-method: gets a translator object */
    private com.google.mlkit.nl.translate.Translator getTranslator(final String srcLng, final String trgLng) {
        final TranslatorOptions options = new TranslatorOptions.Builder()
            .setSourceLanguage(srcLng)
            .setTargetLanguage(trgLng)
            .build();

        return Translation.getClient(options);
    }

    private void onStateSuccess(final String message, final Runnable runnable) {
        Log.iForce(LOGPRAEFIX + message + "[" + this + "]");
        synchronized (mutex) {
            try {
                runnable.run();
            } catch (RuntimeException re) {
                onStateError("Unexpected Exception", TranslatorError.OTHER, re);
            }
        }
    }

    private void onStateError(final String message, final TranslatorError error, final Throwable t) {
        Log.w(LOGPRAEFIX + message + "[" + this + "]", t);
        synchronized (mutex) {
            errorState = error;
            changeState(State.ERROR);
        }
    }

    private void changeState(final State state) {
        synchronized (mutex) {
            if (this.state == state) {
                return;
            }
            this.state = state;
            //ensure updates are on same thread so they don't outrace each other
            AndroidRxUtils.runOnUi(() -> {
                if (stateListener != null) {
                    stateListener.accept(state, this);
                }
            });
        }
    }

    private void executeOnWorker(final Runnable runnable) {
        Schedulers.computation().createWorker().schedule(runnable);
    }

    private Executor getWorkerExecutor() {
        return AndroidRxUtils.fromScheduler(Schedulers.computation());
    }

}
