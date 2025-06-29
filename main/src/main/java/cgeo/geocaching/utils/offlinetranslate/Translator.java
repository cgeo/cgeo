package cgeo.geocaching.utils.offlinetranslate;

import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.ListenerHelper;
import cgeo.geocaching.utils.Log;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * Central object to use for offline translations.
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
    private TranslatorImplementation translatorImplementation;

    private final Object mutex = new Object();
    private final ListenerHelper<BiConsumer<TranslatorState, Translator>> listeners = new ListenerHelper<>();

    /** must be called ONCE before the class can be used. */
    public void initialize(final String config, final String detectionText) {
        synchronized (mutex) {
            fromConfig(config);
            this.detectionText = detectionText;
            recreateTranslator();
        }
    }

    public String toConfig() {
        synchronized (mutex) {
            return toConfigInternal();
        }
    }

    public Disposable addListener(final BiConsumer<TranslatorState, Translator> listener) {
        synchronized (mutex) {
            AndroidRxUtils.runOnUi(() -> listener.accept(this.state, this));
            return listeners.addListenerWithDisposable(listener);
        }
    }

    public void set(final String sourceLanguage, final String targetLanguage, final boolean enabled) {
        synchronized (mutex) {
            if (Objects.equals(sourceLanguage, this.sourceLanguage) &&
                    Objects.equals(targetLanguage, this.targetLanguage) &&
                    enabled == this.enabled) {
                return;
            }
            this.sourceLanguage = sourceLanguage;
            this.targetLanguage = targetLanguage;
            this.enabled = enabled;

            recreateTranslator();
        }
    }

    public void translate(final String original, final Consumer<String> onTranslated) {
        final TranslatorImplementation transImpl = this.translatorImplementation;
        if (transImpl == null) {
            AndroidRxUtils.runOnUi(() -> onTranslated.accept(null));
        } else {
            transImpl.translate(original, onTranslated);
        }
    }

    public void translateAny(final String textOrHtml, final Consumer<String> onTranslated) {
        TranslatorUtils.translateAny(this, textOrHtml, onTranslated);
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
        }
    }

    private void recreateTranslator() {
        synchronized (mutex) {
            if (translatorImplementation != null) {
                translatorImplementation.dispose();
            }

            this.state = TranslatorState.REINITIALIZED;
            this.error = TranslatorError.NONE;
            listeners.executeOnMain(c -> c.accept(TranslatorState.REINITIALIZED, this));

            translatorImplementation = new TranslatorImplementation(
                    getEffectiveSourceLanguage(), getEffectiveTargetLanguage(), this.detectionText, this::onTranslatorStateChange);
        }
    }

    private void onTranslatorStateChange(final TranslatorState state, final TranslatorImplementation translatorImplementation) {
        synchronized (mutex) {
            if (translatorImplementation != this.translatorImplementation) {
                //that's a signal from an already disposed translator -> ignore
                return;
            }
            this.state = state;
            this.error = translatorImplementation.getErrorState();
            if (translatorImplementation.getSourceLanguage() != null) {
                this.detectionText = null;
                this.sourceLanguageDetected = translatorImplementation.getSourceLanguage();
            }
            listeners.executeOnMain(c -> c.accept(state, this));
        }
    }

    //JSON

    private void fromConfig(final String config) {
        synchronized (mutex) {
            final JsonNode node = JsonUtils.stringToNode(config);
            if (node == null) {
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
