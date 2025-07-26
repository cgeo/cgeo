package cgeo.geocaching.utils.offlinetranslate;

import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.ListenerHelper;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import android.util.Pair;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * Central object to use for offline translations.
 * <br>
 * Translator is a stateful object to which
 * listeners can be attached to translate texts. Translator will take care of retranslating
 * and also reverting installation depending on user actions (eg changing target language or
 * turning on/off translations)
 * <br>
 * Implementation note: this class should NOT have any dependencies to the used underlying
 * translation framework (eg MLKit)
 */
public class Translator {

    private static final String LOGPRAEFIX = "[Translator]: ";

    private boolean enabled;
    private boolean allowDownload;
    private String sourceLanguage; // null = auto-detection
    private String sourceLanguageDetected;
    private String targetLanguage; // null = from settings
    private String detectionText;

    private TranslatorState state = TranslatorState.UNINITIALIZED;
    private TranslatorError error = TranslatorError.NONE;

    private ITranslatorImpl translatorImplementation;
    private final Disposable modelStateListener;

    private final Object mutex = new Object();
    private final ListenerHelper<BiConsumer<TranslatorState, Boolean>> stateListeners = new ListenerHelper<>();

    private final AtomicInteger generationId = new AtomicInteger(0);
    private final AtomicInteger translateRunItemIdCreator = new AtomicInteger(0);
    private final Set<Integer> translateRunItems = new HashSet<>();
    private final ListenerHelper<Pair<String, BiConsumer<String, Boolean>>> translations = new ListenerHelper<>();

    public Translator() {
        modelStateListener = TranslationModelManager.get().registerListener(() -> runOnWorker(this::onModelStateChange));
    }

    public static boolean isSupported() {
        return TranslateAccessor.get().isSupported();
    }

    @Override
    @NonNull
    public String toString() {
        return "T:" + getEffectiveSourceLanguage() + "->" + getEffectiveTargetLanguage() + "/enabled:" + enabled +
            "/state:" + state + "/error:" + error + "/stateListener:" + stateListeners + "/translations:" + translations;
    }

    public Disposable addTranslation(final String original, final BiConsumer<String, Boolean> translationAction) {
        synchronized (mutex) {
            final Disposable result = this.translations.addListenerWithDisposable(new Pair<>(original, translationAction));
            resetSingle(original, translationAction);
            if (state.ordinal() > TranslatorState.READY.ordinal()) {
                translateSingle(original, translationAction);
            }
            return result;
        }
    }

    public Disposable addStateListener(final BiConsumer<TranslatorState, Boolean> listener) {
        synchronized (mutex) {
            AndroidRxUtils.runOnUi(() -> listener.accept(this.state, this.enabled));
            return stateListeners.addListenerWithDisposable(listener);
        }
    }

    @AnyThread
    public void init(final String config, final String detectText) {
        runOnWorker(() -> {
            resetInternal();

            final JsonNode node = JsonUtils.stringToNode(config);
            if (node == null && config != null) {
                Log.e(LOGPRAEFIX + "Couldn't parse config: '" + config + "'");
                return;
            }

            final String srcLng = JsonUtils.getText(node, "srcLng", this.sourceLanguage);
            final String srcLngDetected = JsonUtils.getText(node, "srcLngDetected", this.sourceLanguageDetected);
            final String trgLng = JsonUtils.getText(node, "trgLng", this.targetLanguage);
            final boolean enabled = JsonUtils.getBoolean(node, "enabled", this.enabled);
            final boolean allowDownload = JsonUtils.getBoolean(node, "allowDownload", this.allowDownload);
            set(srcLng, trgLng, srcLngDetected, detectText, enabled, allowDownload);
        });
    }

    @AnyThread
    public String toConfig() {
        synchronized (mutex) {
            final ObjectNode node = JsonUtils.createObjectNode();

            JsonUtils.setText(node, "srcLng", this.sourceLanguage);
            JsonUtils.setText(node, "srcLngDetected", this.sourceLanguageDetected);
            JsonUtils.setText(node, "trgLng", this.targetLanguage);
            JsonUtils.setBoolean(node, "enabled", this.enabled);
            JsonUtils.setBoolean(node, "allowDownload", this.allowDownload);
            return JsonUtils.nodeToString(node);
        }
    }


    @AnyThread
    public void setEnabled(final boolean enabled) {
        runOnWorker(() ->
            set(this.sourceLanguage, this.targetLanguage, this.sourceLanguageDetected, null, enabled, this.allowDownload));
    }

    @AnyThread
    public void setLanguagesAndEnable(final String srcLng, final String trgLng) {
        runOnWorker(() ->
            set(srcLng, trgLng, this.sourceLanguageDetected, null, true, true));
    }

    @WorkerThread
    private void set(final String srcLng, final String trgLng, final String srcLngDetected, final String detectText, final boolean enabled, final boolean allowDownload) {
        //check what changed
        final boolean effectiveSrcLngChanged =
            !Objects.equals(this.getEffectiveSourceLanguage(), TranslatorUtils.getEffectiveSourceLanguage(srcLng, srcLngDetected));
        final boolean effectiveTrgLngChanged =
                !Objects.equals(this.getEffectiveTargetLanguage(), TranslatorUtils.getEffectiveTargetLanguage(trgLng));
        final boolean startRedetect = detectText != null && this.sourceLanguageDetected == null && srcLngDetected == null;
        final boolean enabledChanged = enabled != this.enabled;
        final boolean allowDownloadChanged = allowDownload != this.allowDownload;

        //set new values
        this.sourceLanguage = srcLng;
        this.targetLanguage = trgLng;
        this.sourceLanguageDetected = this.sourceLanguageDetected == null ? srcLngDetected : this.sourceLanguageDetected;
        this.detectionText = startRedetect ? TextUtils.stripHtml(detectText) : null;
        this.allowDownload = allowDownload;
        changeState(this.state, enabled);

        //reinitialize those translator parts which needs it
        if (effectiveSrcLngChanged || effectiveTrgLngChanged || startRedetect) {
            disposeTranslator();
            if (state.ordinal() > TranslatorState.READY.ordinal()) {
                resetAll();
            }
            doDetectSource();
        } else if (allowDownloadChanged && state == TranslatorState.MODEL_MISSING && this.allowDownload) {
            doLanguageCheck();
        } else if (enabledChanged && state.ordinal() >= TranslatorState.READY.ordinal()) {
            if (enabled) {
                translateAll();
            } else {
                changeState(TranslatorState.READY);
                resetAll();
            }
        }
    }

    @WorkerThread
    private void doDetectSource() {
        if (getEffectiveSourceLanguage() != null) {
            doLanguageCheck();
        } else {
            changeState(TranslatorState.DETECTING_SOURCE);
            final int genId = this.generationId.get();
            TranslateAccessor.get().guessLanguage(this.detectionText, guessedLng -> runOnWorker(() -> {
                if (genId != this.generationId.get()) {
                    return;
                }
               this.sourceLanguageDetected = guessedLng;
               doLanguageCheck();
            }), ex -> onError(TranslatorError.SOURCE_LANGUAGE_NOT_IDENTIFIED, ex));
        }
    }

    @WorkerThread
    private void doLanguageCheck() {
        final String srcLng = getEffectiveSourceLanguage();
        final String trgLng = getEffectiveTargetLanguage();
        if (!TranslationModelManager.get().getSupportedLanguages().contains(srcLng) ||
            !TranslationModelManager.get().getSupportedLanguages().contains(trgLng)) {
            onError(TranslatorError.LANGUAGE_UNSUPPORTED, null);
        } else if (TranslationModelManager.get().isAvailable(srcLng) && TranslationModelManager.get().isAvailable(trgLng)) {
            onReady();
        } else if (TranslationModelManager.get().isAvailableOrPending(srcLng) && TranslationModelManager.get().isAvailableOrPending(trgLng)) {
            changeState(TranslatorState.DOWNLOADING_MODEL);
        } else if (allowDownload) {
            TranslationModelManager.get().downloadLanguage(srcLng);
            TranslationModelManager.get().downloadLanguage(trgLng);
            changeState(TranslatorState.DOWNLOADING_MODEL);
        } else {
            changeState(TranslatorState.MODEL_MISSING);
        }
    }

    @WorkerThread
    private void onReady() {
        changeState(TranslatorState.READY);
        disposeTranslator(); // just to be sure
        this.translatorImplementation = TranslateAccessor.get().getTranslator(getEffectiveSourceLanguage(), getEffectiveTargetLanguage());
        if (isEnabled()) {
            translateAll();
        }
    }

    @WorkerThread
    private void onError(final TranslatorError error, final Exception ex) {
        Log.e(LOGPRAEFIX + "error: " + error, ex);
        this.error = error == null ? TranslatorError.OTHER : error;
        changeState(TranslatorState.ERROR);
    }

    @WorkerThread
    private void disposeTranslator() {
        synchronized (mutex) {
            //reinit everything
            if (this.translatorImplementation != null) {
                this.translatorImplementation.dispose();
            }
            this.translatorImplementation = null;
            invalidateCurrentGen();
        }
    }

    @WorkerThread
    private void invalidateCurrentGen() {
        //invalidate current run
        generationId.addAndGet(1);
        translateRunItems.clear();
    }

    @WorkerThread
    private void resetAll() {
        invalidateCurrentGen();
        translations.execute(TranslatorUtils.getWorkerScheduler(), c -> {
            synchronized (mutex) {
                resetSingle(c.first, c.second);
            }
        });
    }

    @WorkerThread
    private void translateAll() {
        invalidateCurrentGen();
        //rerun for all translations if any
        final int cnt = translations.execute(TranslatorUtils.getWorkerScheduler(), c -> {
            synchronized (mutex) {
                translateSingle(c.first, c.second);
            }
        });
        //special case: translator should be translating but no translation is registered
        if (cnt == 0 && state.ordinal() >= TranslatorState.READY.ordinal() && enabled) {
            changeState(TranslatorState.TRANSLATED);
        }
    }

    @WorkerThread
    private void resetSingle(final String source, final BiConsumer<String, Boolean> translate) {
        AndroidRxUtils.runOnUi(() -> translate.accept(source, false));
    }

    @WorkerThread
    private void translateSingle(final String source, final BiConsumer<String, Boolean> translate) {
        //trigger translate if necessary + possible
        final ITranslatorImpl tImpl = translatorImplementation;
        final int runId = generationId.get();
        if (tImpl == null || state.ordinal() < TranslatorState.READY.ordinal() || !enabled) {
            //no translation possible/necessary
            return;
        }
        final int itemId = translateRunItemIdCreator.addAndGet(1);
        translateRunItems.add(itemId);
        //Log.iForce(LOGPRAEFIX + " translate START (" + translateRunItems.size() + "):" + source);
        changeState(TranslatorState.TRANSLATING);
        TranslatorUtils.translateAny(translatorImplementation, source, translated -> runOnWorker(() -> {
            //Log.iForce(LOGPRAEFIX + " translate STOP (" + translateRunItems.size() + ") '" + source + "' -> '" + translated + "'");
            if (runId != generationId.get()) {
                //run was invalidated
                return;
            }
            translateRunItems.remove(itemId);
            if (translateRunItems.isEmpty()) {
                //all pending translations were done
                changeState(TranslatorState.TRANSLATED);
            }
            AndroidRxUtils.runOnUi(() -> translate.accept(translated, true));
        }));
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public String getSourceLanguageDetected() {
        return sourceLanguageDetected;
    }

    public String getEffectiveSourceLanguage() {
        return TranslatorUtils.getEffectiveSourceLanguage(sourceLanguage, sourceLanguageDetected);
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public String getEffectiveTargetLanguage() {
        return TranslatorUtils.getEffectiveTargetLanguage(targetLanguage);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public TranslatorState getState() {
        return state;
    }

    public TranslatorError getError() {
        return error;
    }

    @WorkerThread
    private void onModelStateChange() {
        if (state != TranslatorState.DOWNLOADING_MODEL) {
            return;
        }
        final String srcLng = getEffectiveSourceLanguage();
        final String trgLng = getEffectiveTargetLanguage();
        if (TranslationModelManager.get().isAvailable(srcLng) && TranslationModelManager.get().isAvailable(trgLng)) {
            onReady();
        }
        if (!TranslationModelManager.get().isAvailableOrPending(srcLng) || !TranslationModelManager.get().isAvailableOrPending(trgLng)) {
            onError(TranslatorError.OTHER, null);
        }
    }

    @AnyThread
    public void reset() {
        runOnWorker(() -> {
            resetInternal();
        });
    }

    @AnyThread
    public void dispose() {
        runOnWorker(() -> {
            Log.iForce(LOGPRAEFIX + "dispose");
            resetInternal();
            this.modelStateListener.dispose();
            stateListeners.clear();
            translations.clear();
        });
    }

    @WorkerThread
    private void resetInternal() {
        disposeTranslator();
        this.sourceLanguage = null;
        this.targetLanguage = null;
        this.detectionText = null;
        this.sourceLanguageDetected = null;
        this.enabled = false;
        this.allowDownload = false;
        this.state = TranslatorState.UNINITIALIZED;
        this.error = TranslatorError.NONE;
    }

    @WorkerThread
    private void changeState(final TranslatorState newState) {
        changeState(newState, this.enabled);
    }

    @WorkerThread
    private void changeState(final TranslatorState newState, final boolean enabled) {
        if (newState == this.state && enabled == this.enabled) {
            return;
        }
        this.state = newState;
        this.enabled = enabled;
        stateListeners.executeOnMain(c -> c.accept(newState, enabled));
    }

    @AnyThread
    private void runOnWorker(final Runnable run) {
        TranslatorUtils.runOnWorker(() -> {
            synchronized (mutex) {
                run.run();
            }
        });
    }



}
