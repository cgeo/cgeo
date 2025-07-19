package cgeo.geocaching.utils.offlinetranslate;

import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.LeastRecentlyUsedMap;
import cgeo.geocaching.utils.ListenerHelper;
import cgeo.geocaching.utils.Log;

import android.util.Pair;

import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
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
    private String sourceLanguage; // null = auto-detection
    private String sourceLanguageDetected;
    private String targetLanguage; // null = from settings

    private String detectionText;

    private TranslatorState state = TranslatorState.CREATED;
    private TranslatorError error = TranslatorError.NONE;
    private TranslatorImpl translatorImplementation;
    private final Map<String, String> translationCache;

    private final Object mutex = new Object();
    private final ListenerHelper<BiConsumer<TranslatorState, Translator>> stateListeners = new ListenerHelper<>();

    private final AtomicInteger translateRunId = new AtomicInteger(0);
    private final AtomicInteger translateRunItemIdCreator = new AtomicInteger(0);
    private final Set<Integer> translateRunItems = new HashSet<>();
    private final ListenerHelper<Pair<String, BiConsumer<String, Boolean>>> translations = new ListenerHelper<>();
;
    public Translator() {
        this(10);
    }

    public Translator(final int cacheSize) {
        translationCache = cacheSize <= 0 ? null : Collections.synchronizedMap(new LeastRecentlyUsedMap.LruCache<>(cacheSize));
    }

    /** must be called ONCE before the class can be used. */
    public void reinit(final String config, final boolean forceRedetection, final String detectionText) {
        synchronized (mutex) {
            fromConfig(config);
            //if necessary, (re)initialize source language detection
            if (forceRedetection || this.sourceLanguageDetected == null) {
                this.sourceLanguageDetected = null;
                this.detectionText = detectionText;
            }
            recreateTranslator();
        }
    }

    public String toConfig() {
        synchronized (mutex) {
            return toConfigInternal();
        }
    }

    @Override
    @NonNull
    public String toString() {
        return "T:" + getEffectiveSourceLanguage() + "->" + getEffectiveTargetLanguage() + "/enabled:" + enabled +
            "/state:" + state + "/error:" + error;
    }

    public Disposable addTranslation(final String original, final BiConsumer<String, Boolean> translationAction) {
        synchronized (mutex) {
            final Disposable result = this.translations.addListenerWithDisposable(new Pair<>(original, translationAction));
            retranslate(true, original, translationAction);
            return result;
        }
    }

    public Disposable addStateListener(final BiConsumer<TranslatorState, Translator> listener) {
        synchronized (mutex) {
            AndroidRxUtils.runOnUi(() -> listener.accept(this.state, this));
            return stateListeners.addListenerWithDisposable(listener);
        }
    }

    @AnyThread
    public void setEnabled(final boolean enabled) {
        set(this.sourceLanguage, this.targetLanguage, enabled);
    }

    @AnyThread
    public void set(final String sourceLanguage, final String targetLanguage, final boolean enabled) {
        synchronized (mutex) {
            //check what changed
            final boolean effectiveSrcLngChanged =
                !Objects.equals(this.getEffectiveSourceLanguage(), TranslatorUtils.getEffectiveSourceLanguage(sourceLanguage, this.sourceLanguageDetected));
            final boolean effectiveTrgLngChanged =
                    !Objects.equals(this.getEffectiveTargetLanguage(), TranslatorUtils.getEffectiveTargetLanguage(targetLanguage));
            final boolean enabledChanged = enabled != this.enabled;

            //set new values
            this.sourceLanguage = sourceLanguage;
            this.targetLanguage = targetLanguage;
            this.enabled = enabled;

            //reinitialize those translator parts which needs it
            if (effectiveSrcLngChanged || effectiveTrgLngChanged) {
                recreateTranslator();
            } else if (enabledChanged && state.ordinal() >= TranslatorState.READY.ordinal()) {
                if (enabled) {
                    retranslateAll(false);
                } else {
                    changeState(TranslatorState.READY);
                    retranslateAll(true);
                }
            } else if (enabledChanged && (state == TranslatorState.SOURCE_LANGUAGE_DETECTED || state == TranslatorState.SOURCE_LANGUAGE_DETECTED_MODEL_NEEDED)) {
                changeState(enabled ? TranslatorState.SOURCE_LANGUAGE_DETECTED_MODEL_NEEDED : TranslatorState.SOURCE_LANGUAGE_DETECTED);
            }
        }
    }

    @AnyThread
    private void retranslateAll(final boolean forceReset) {
        synchronized (mutex) {
            //invalidate current run
            translateRunId.addAndGet(1);
            translateRunItems.clear();
            //rerun for all translations if any
            final int cnt = translations.execute(AndroidRxUtils.computationScheduler, c ->
                    retranslate(forceReset, c.first, c.second));
            //special case: translator should be translating but no translation is registered
            if (cnt == 0 && state.ordinal() >= TranslatorState.READY.ordinal() && enabled) {
                changeState(TranslatorState.TRANSLATED);
            }
        }
    }

    @WorkerThread
    private void retranslate(final boolean doReset, final String source, final BiConsumer<String, Boolean> translate) {
        synchronized (mutex) {
            if (doReset) {
                AndroidRxUtils.runOnUi(() -> translate.accept(source, false));
            }
            //trigger translate if necessary + possible
            final TranslatorImpl tImpl = translatorImplementation;
            final int runId = translateRunId.get();
            if (tImpl == null || state.ordinal() < TranslatorState.READY.ordinal() || !enabled) {
                //no translation possible/necessary
                return;
            }
            final int itemId = translateRunItemIdCreator.addAndGet(1);
            translateRunItems.add(itemId);
            Log.iForce(LOGPRAEFIX + " translate START (" + translateRunItems.size() + "):" + source);
            changeState(TranslatorState.TRANSLATING);
            TranslatorUtils.translateAny(tImpl::translate, translationCache, source, translated -> {
                synchronized (mutex) {
                    Log.iForce(LOGPRAEFIX + " translate STOP (" + translateRunItems.size() + ") '" + source + "' -> '" + translated + "'");
                    if (runId != translateRunId.get()) {
                        //run was invalidated
                        return;
                    }
                    translateRunItems.remove(itemId);
                    if (translateRunItems.isEmpty()) {
                        //all pending translations were done
                        changeState(TranslatorState.TRANSLATED);
                    }
                    AndroidRxUtils.runOnUi(() -> translate.accept(translated, true));
                }
            });
        }

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

    public void dispose() {
        synchronized (mutex) {
            if (translatorImplementation != null) {
                translatorImplementation.dispose();
                translatorImplementation = null;
            }
            translationCache.clear();
            stateListeners.clear();
            translations.clear();
            this.state = TranslatorState.CREATED;
            this.error = TranslatorError.NONE;
        }
    }

    private void recreateTranslator() {
        synchronized (mutex) {
            if (translatorImplementation != null) {
                translatorImplementation.dispose();
            }

            this.error = TranslatorError.NONE;
            changeState(TranslatorState.REINITIALIZED);
            retranslateAll(true);

            translationCache.clear();
            translatorImplementation = new TranslatorImpl(
                    getEffectiveSourceLanguage(), getEffectiveTargetLanguage(), this.detectionText, this::onTranslatorStateChange);
        }
    }

    @MainThread
    private void onTranslatorStateChange(final TranslatorImpl.State state, final TranslatorImpl translatorImplementation) {
        synchronized (mutex) {
            if (translatorImplementation != this.translatorImplementation) {
                //that's a signal from an already disposed translator -> ignore
                return;
            }
            switch (state) {
                case SRC_DETECTED:
                    if (translatorImplementation.getSourceLanguage() != null) {
                        this.detectionText = null;
                        this.sourceLanguageDetected = translatorImplementation.getSourceLanguage();
                    }
                    changeState(isEnabled() ? TranslatorState.SOURCE_LANGUAGE_DETECTED_MODEL_NEEDED : TranslatorState.SOURCE_LANGUAGE_DETECTED);
                    break;
                case READY:
                    changeState(TranslatorState.READY);
                    if (isEnabled()) {
                        retranslateAll(false);
                    }
                    break;
                case ERROR:
                    this.error = translatorImplementation.getErrorState();
                    if (this.translatorImplementation != null) {
                        this.translatorImplementation.dispose();
                    }
                    this.translatorImplementation = null;
                    changeState(TranslatorState.ERROR);
                    break;
                default:
                    break;
            }
        }
    }

    private void changeState(final TranslatorState newState) {
        synchronized (mutex) {
            if (newState == this.state) {
                return;
            }
            this.state = newState;
            stateListeners.executeOnMain(c -> c.accept(newState, this));
        }
    }

    //JSON

    private void fromConfig(final String config) {
        synchronized (mutex) {
            final JsonNode node = JsonUtils.stringToNode(config);
            if (node == null && config != null) {
                Log.e(LOGPRAEFIX + "Couldn't parse config: '" + config + "'");
                return;
            }

            this.enabled = JsonUtils.getBoolean(node, "enabled", this.enabled);
            this.sourceLanguage = JsonUtils.getText(node, "srcLng", this.sourceLanguage);
            this.sourceLanguageDetected = JsonUtils.getText(node, "srcLngDetected", this.sourceLanguageDetected);
            this.targetLanguage = JsonUtils.getText(node, "trgLng", this.targetLanguage);
        }
    }

    private String toConfigInternal() {
        synchronized (mutex) {
            final ObjectNode node = JsonUtils.createObjectNode();

            JsonUtils.setBoolean(node, "enabled", this.enabled);
            JsonUtils.setText(node, "srcLng", this.sourceLanguage);
            JsonUtils.setText(node, "srcLngDetected", this.sourceLanguageDetected);
            JsonUtils.setText(node, "trgLng", this.targetLanguage);
            return JsonUtils.nodeToString(node);
        }
    }
}
