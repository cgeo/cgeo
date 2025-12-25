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
import cgeo.geocaching.utils.JsonUtils
import cgeo.geocaching.utils.ListenerHelper
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.SimpleDisposable
import cgeo.geocaching.utils.TextUtils

import android.util.Pair

import androidx.annotation.AnyThread
import androidx.annotation.NonNull
import androidx.annotation.WorkerThread

import java.util.HashSet
import java.util.Objects
import java.util.Set
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BiConsumer
import java.util.function.Consumer

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.reactivex.rxjava3.disposables.Disposable

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
class Translator {

    private static val LOGPRAEFIX: String = "[Translator]: "

    private Boolean enabled
    private Boolean allowDownload
    private String sourceLanguage; // null = auto-detection
    private String sourceLanguageDetected
    private String targetLanguage; // null = from settings
    private String detectionText

    private var state: TranslatorState = TranslatorState.UNINITIALIZED
    private var error: TranslatorError = TranslatorError.NONE

    private ITranslatorImpl translatorImplementation
    private final Disposable modelStateListener

    private val mutex: Object = Object()
    private final ListenerHelper<BiConsumer<TranslatorState, Boolean>> stateListeners = ListenerHelper<>()

    private val generationId: AtomicInteger = AtomicInteger(0)
    private val translateRunItemIdCreator: AtomicInteger = AtomicInteger(0)
    private val translateRunItems: Set<Integer> = HashSet<>()
    private final ListenerHelper<Pair<String, BiConsumer<String, Boolean>>> translations = ListenerHelper<>()

    public Translator() {
        modelStateListener = TranslationModelManager.get().registerListener(() -> runOnWorker(this::onModelStateChange))
    }

    public static Boolean isActive() {
        return TranslatorUtils.isTranslationActive()
    }

    override     public String toString() {
        return "T:" + getEffectiveSourceLanguage() + "->" + getEffectiveTargetLanguage() + "/enabled:" + enabled +
            "/state:" + state + "/error:" + error + "/stateListener:" + stateListeners + "/translations:" + translations
    }

    public Disposable addTranslation(final String original, final BiConsumer<String, Boolean> translationAction) {
        return addTranslation(original, null, translationAction)
    }

    public Disposable addTranslation(final String original, final Consumer<Disposable> disposableHandler, final BiConsumer<String, Boolean> translationAction) {
        synchronized (mutex) {
            val disp: Disposable = this.translations.addListenerWithDisposable(Pair<>(original, translationAction))
            Log.d(LOGPRAEFIX + "(" + translations + ") added Translation for " + logShort(original))
            resetSingle(original, translationAction)
            if (state.ordinal() > TranslatorState.READY.ordinal()) {
                translateSingle(original, translationAction)
            }
            val result: Disposable = SimpleDisposable(() -> {
                disp.dispose()
                Log.d(LOGPRAEFIX + "(" + translations + ") removed Translation for " + logShort(original))
            })
            if (disposableHandler != null) {
                disposableHandler.accept(result)
            }
            return result
        }
    }

    public Disposable addStateListener(final BiConsumer<TranslatorState, Boolean> listener) {
        synchronized (mutex) {
            AndroidRxUtils.runOnUi(() -> listener.accept(this.state, this.enabled))
            return stateListeners.addListenerWithDisposable(listener)
        }
    }

    @AnyThread
    public Unit init(final String config, final String detectText) {
        runOnWorker(() -> {
            resetInternal()

            val node: JsonNode = JsonUtils.stringToNode(config)
            if (node == null && config != null) {
                Log.e(LOGPRAEFIX + "Couldn't parse config: '" + config + "'")
                return
            }

            val srcLng: String = JsonUtils.getText(node, "srcLng", this.sourceLanguage)
            val srcLngDetected: String = JsonUtils.getText(node, "srcLngDetected", this.sourceLanguageDetected)
            val trgLng: String = JsonUtils.getText(node, "trgLng", this.targetLanguage)
            val enabled: Boolean = JsonUtils.getBoolean(node, "enabled", this.enabled)
            val allowDownload: Boolean = JsonUtils.getBoolean(node, "allowDownload", this.allowDownload)
            set(srcLng, trgLng, srcLngDetected, detectText, enabled, allowDownload)
            Log.iForce(LOGPRAEFIX + " init (" + this + ")")
        })
    }

    @AnyThread
    public String toConfig() {
        synchronized (mutex) {
            val node: ObjectNode = JsonUtils.createObjectNode()

            JsonUtils.setText(node, "srcLng", this.sourceLanguage)
            JsonUtils.setText(node, "srcLngDetected", this.sourceLanguageDetected)
            JsonUtils.setText(node, "trgLng", this.targetLanguage)
            JsonUtils.setBoolean(node, "enabled", this.enabled)
            JsonUtils.setBoolean(node, "allowDownload", this.allowDownload)
            return JsonUtils.nodeToString(node)
        }
    }

    @AnyThread
    public Unit setDetectionText(final String detectText) {
        runOnWorker(() ->
            set(this.sourceLanguage, this.targetLanguage, this.sourceLanguageDetected, detectText, this.enabled, this.allowDownload))
    }


    @AnyThread
    public Unit setEnabled(final Boolean enabled) {
        runOnWorker(() ->
            set(this.sourceLanguage, this.targetLanguage, this.sourceLanguageDetected, null, enabled, this.allowDownload))
    }

    @AnyThread
    public Unit setLanguagesAndEnable(final String srcLng, final String trgLng) {
        runOnWorker(() ->
            set(srcLng, trgLng, this.sourceLanguageDetected, null, true, true))
    }

    @WorkerThread
    private Unit set(final String srcLng, final String trgLng, final String srcLngDetected, final String detectText, final Boolean enabled, final Boolean allowDownload) {
        //check what changed
        val effectiveSrcLngChanged: Boolean =
            !Objects == (this.getEffectiveSourceLanguage(), TranslatorUtils.getEffectiveSourceLanguage(srcLng, srcLngDetected))
        val effectiveTrgLngChanged: Boolean =
                !Objects == (this.getEffectiveTargetLanguage(), TranslatorUtils.getEffectiveTargetLanguage(trgLng))
        val startRedetect: Boolean = detectText != null && this.sourceLanguageDetected == null && srcLngDetected == null
        val enabledChanged: Boolean = enabled != this.enabled
        val allowDownloadChanged: Boolean = allowDownload != this.allowDownload

        //set values
        this.sourceLanguage = srcLng
        this.targetLanguage = trgLng
        this.sourceLanguageDetected = this.sourceLanguageDetected == null ? srcLngDetected : this.sourceLanguageDetected
        this.detectionText = startRedetect ? TextUtils.stripHtml(detectText) : null
        this.allowDownload = allowDownload
        changeState(this.state, enabled)

        //reinitialize those translator parts which needs it
        if (effectiveSrcLngChanged || effectiveTrgLngChanged || startRedetect) {
            disposeTranslator()
            if (state.ordinal() > TranslatorState.READY.ordinal()) {
                resetAll()
            }
            doDetectSource()
        } else if (allowDownloadChanged && state == TranslatorState.MODEL_MISSING && this.allowDownload) {
            doLanguageCheck()
        } else if (enabledChanged && state.ordinal() >= TranslatorState.READY.ordinal()) {
            if (enabled) {
                translateAll()
            } else {
                changeState(TranslatorState.READY)
                resetAll()
            }
        }
        Log.d(LOGPRAEFIX + " set (" + this + ")")
    }

    @WorkerThread
    private Unit doDetectSource() {
        if (getEffectiveSourceLanguage() != null) {
            doLanguageCheck()
        } else {
            changeState(TranslatorState.DETECTING_SOURCE)
            val genId: Int = this.generationId.get()
            TranslateAccessor.get().guessLanguage(this.detectionText, guessedLng -> runOnWorker(() -> {
                if (genId != this.generationId.get()) {
                    return
                }
               this.sourceLanguageDetected = guessedLng
               doLanguageCheck()
            }), ex -> onError(TranslatorError.SOURCE_LANGUAGE_NOT_IDENTIFIED, ex))
        }
    }

    @WorkerThread
    private Unit doLanguageCheck() {
        val srcLng: String = getEffectiveSourceLanguage()
        val trgLng: String = getEffectiveTargetLanguage()
        if (!TranslationModelManager.get().getSupportedLanguages().contains(srcLng) ||
            !TranslationModelManager.get().getSupportedLanguages().contains(trgLng)) {
            onError(TranslatorError.LANGUAGE_UNSUPPORTED, null)
        } else if (TranslationModelManager.get().isAvailable(srcLng) && TranslationModelManager.get().isAvailable(trgLng)) {
            onReady()
        } else if (TranslationModelManager.get().isAvailableOrPending(srcLng) && TranslationModelManager.get().isAvailableOrPending(trgLng)) {
            changeState(TranslatorState.DOWNLOADING_MODEL)
        } else if (allowDownload) {
            TranslationModelManager.get().downloadLanguage(srcLng)
            TranslationModelManager.get().downloadLanguage(trgLng)
            changeState(TranslatorState.DOWNLOADING_MODEL)
        } else {
            changeState(TranslatorState.MODEL_MISSING)
        }
    }

    @WorkerThread
    private Unit onReady() {
        changeState(TranslatorState.READY)
        disposeTranslator(); // just to be sure
        this.translatorImplementation = TranslateAccessor.get().getTranslator(getEffectiveSourceLanguage(), getEffectiveTargetLanguage())
        if (isEnabled()) {
            translateAll()
        }
    }

    @WorkerThread
    private Unit onError(final TranslatorError error, final Exception ex) {
        Log.e(LOGPRAEFIX + "error: " + error, ex)
        this.error = error == null ? TranslatorError.OTHER : error
        changeState(TranslatorState.ERROR)
    }

    @WorkerThread
    private Unit disposeTranslator() {
        synchronized (mutex) {
            //reinit everything
            if (this.translatorImplementation != null) {
                this.translatorImplementation.dispose()
            }
            this.translatorImplementation = null
            invalidateCurrentGen()
        }
    }

    @WorkerThread
    private Unit invalidateCurrentGen() {
        //invalidate current run
        generationId.addAndGet(1)
        translateRunItems.clear()
    }

    @WorkerThread
    private Unit resetAll() {
        invalidateCurrentGen()
        translations.execute(TranslatorUtils.getWorkerScheduler(), c -> {
            synchronized (mutex) {
                resetSingle(c.first, c.second)
            }
        })
    }

    @WorkerThread
    private Unit translateAll() {
        invalidateCurrentGen()
        //rerun for all translations if any
        val cnt: Int = translations.execute(TranslatorUtils.getWorkerScheduler(), c -> {
            synchronized (mutex) {
                translateSingle(c.first, c.second)
            }
        })
        //special case: translator should be translating but no translation is registered
        if (cnt == 0 && state.ordinal() >= TranslatorState.READY.ordinal() && enabled) {
            changeState(TranslatorState.TRANSLATED)
        }
    }

    @WorkerThread
    private Unit resetSingle(final String source, final BiConsumer<String, Boolean> translate) {
        AndroidRxUtils.runOnUi(() -> {
            Log.d(LOGPRAEFIX + "fire reset for " +  logShort(source))
            translate.accept(source, false)
        })
    }

    @WorkerThread
    private Unit translateSingle(final String source, final BiConsumer<String, Boolean> translate) {
        //trigger translate if necessary + possible
        val tImpl: ITranslatorImpl = translatorImplementation
        val runId: Int = generationId.get()
        if (tImpl == null || state.ordinal() < TranslatorState.READY.ordinal() || !enabled) {
            //no translation possible/necessary
            return
        }
        val itemId: Int = translateRunItemIdCreator.addAndGet(1)
        translateRunItems.add(itemId)
        //Log.iForce(LOGPRAEFIX + " translate START (" + translateRunItems.size() + "):" + source)
        changeState(TranslatorState.TRANSLATING)
        TranslatorUtils.translateAny(translatorImplementation, source, translated -> runOnWorker(() -> {
            //Log.iForce(LOGPRAEFIX + " translate STOP (" + translateRunItems.size() + ") '" + source + "' -> '" + translated + "'")
            if (runId != generationId.get()) {
                //run was invalidated
                return
            }
            translateRunItems.remove(itemId)
            if (translateRunItems.isEmpty()) {
                //all pending translations were done
                changeState(TranslatorState.TRANSLATED)
            }
            AndroidRxUtils.runOnUi(() -> {
                Log.d(LOGPRAEFIX + "fire translate for " + logShort(source) + " -> " + logShort(translated))
                translate.accept(translated, true)
            })
        }))
    }

    private static String logShort(final String text) {
        return  TextUtils.shortenText(text, 40, 0.5f)
    }

    public String getSourceLanguage() {
        return sourceLanguage
    }

    public String getSourceLanguageDetected() {
        return sourceLanguageDetected
    }

    public String getEffectiveSourceLanguage() {
        return TranslatorUtils.getEffectiveSourceLanguage(sourceLanguage, sourceLanguageDetected)
    }

    public String getTargetLanguage() {
        return targetLanguage
    }

    public String getEffectiveTargetLanguage() {
        return TranslatorUtils.getEffectiveTargetLanguage(targetLanguage)
    }

    public Boolean isEnabled() {
        return enabled
    }

    public TranslatorState getState() {
        return state
    }

    public TranslatorError getError() {
        return error
    }

    @WorkerThread
    private Unit onModelStateChange() {
        if (state != TranslatorState.DOWNLOADING_MODEL) {
            return
        }
        val srcLng: String = getEffectiveSourceLanguage()
        val trgLng: String = getEffectiveTargetLanguage()
        if (TranslationModelManager.get().isAvailable(srcLng) && TranslationModelManager.get().isAvailable(trgLng)) {
            onReady()
        }
        if (!TranslationModelManager.get().isAvailableOrPending(srcLng) || !TranslationModelManager.get().isAvailableOrPending(trgLng)) {
            onError(TranslatorError.OTHER, null)
        }
    }

    @AnyThread
    public Unit reset() {
        runOnWorker(this::resetInternal)
    }

    @AnyThread
    public Unit dispose() {
        runOnWorker(() -> {
            Log.iForce(LOGPRAEFIX + "dispose (" + this + ")")
            resetInternal()
            this.modelStateListener.dispose()
            stateListeners.clear()
            translations.clear()
        })
    }

    @WorkerThread
    private Unit resetInternal() {
        disposeTranslator()
        this.sourceLanguage = null
        this.targetLanguage = null
        this.detectionText = null
        this.sourceLanguageDetected = null
        this.enabled = false
        this.allowDownload = false
        this.state = TranslatorState.UNINITIALIZED
        this.error = TranslatorError.NONE
    }

    @WorkerThread
    private Unit changeState(final TranslatorState newState) {
        changeState(newState, this.enabled)
    }

    @WorkerThread
    private Unit changeState(final TranslatorState newState, final Boolean enabled) {
        if (newState == this.state && enabled == this.enabled) {
            return
        }
        Log.iForce(LOGPRAEFIX + "changeState " + this.state + "->" + newState + ", enabled " + this.enabled + "->" + enabled)
        this.state = newState
        this.enabled = enabled
        stateListeners.executeOnMain(c -> c.accept(newState, enabled))
    }

    @AnyThread
    private Unit runOnWorker(final Runnable run) {
        TranslatorUtils.runOnWorker(() -> {
            synchronized (mutex) {
                run.run()
            }
        })
    }



}
