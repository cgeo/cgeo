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
import cgeo.geocaching.databinding.TranslationsettingsDialogBinding
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.SimpleItemListModel
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.TextSpinner
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.EmojiUtils
import cgeo.geocaching.utils.LeastRecentlyUsedMap
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.SimpleDisposable
import cgeo.geocaching.utils.TextUtils

import android.content.Context
import android.graphics.Typeface
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView

import androidx.annotation.NonNull

import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.Comparator
import java.util.List
import java.util.Map
import java.util.Objects
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.stream.Collectors

import javax.annotation.Nullable

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.TextNode

class TranslatorUtils {

    private static val LOGPRAEFIX: String = "[TranslatorUtils]:"

    private static val WORKER_SINGLE: Scheduler = Schedulers.single()

    private static val TRANSLATION_CACHE: Map<String, String> = Collections.synchronizedMap(LeastRecentlyUsedMap.LruCache<>(300))

    public static class ChangeableText : Disposable {
        private final Translator translator
        private String text
        private Disposable disposable

        public ChangeableText(final Translator translator) {
            this(translator, null)
        }

        public ChangeableText(final Translator translator, final CompositeDisposable compositeDisposable) {
            this.translator = translator
            if (compositeDisposable != null) {
                compositeDisposable.add(this)
            }
        }


        public Unit set(final String text, final BiConsumer<String, Boolean> textAction) {
           if (Objects == (this.text, text)) {
                return
            }
            if (this.disposable != null) {
                this.disposable.dispose()
            }
            this.text = text
            this.disposable = translator.addTranslation(this.text, textAction)
        }

        override         public Unit dispose() {
            if (this.disposable != null) {
                this.disposable.dispose()
            }
        }

        override         public Boolean isDisposed() {
            return this.disposable != null && this.disposable.isDisposed()
        }
    }

    private TranslatorUtils() {
        //no instance
    }

    public static Boolean isTranslationActive() {
        //as of now, user deactivates translation by setting targetlanguagecode to empty...
        return !StringUtils.isBlank(Settings.getTranslationTargetLanguageCode())
    }

    public static String getEffectiveSourceLanguage(final String sourceLanguage, final String sourceLanguageDetected) {
        return sourceLanguage != null ? sourceLanguage : sourceLanguageDetected
    }

    public static String getEffectiveTargetLanguage(final String targetLanguage) {
        if (targetLanguage != null) {
            return targetLanguage
        }
        if (TranslationModelManager.get().getSupportedLanguages().contains(Settings.getTranslationTargetLanguageCode())) {
            return Settings.getTranslationTargetLanguageCode()
        }
        if (TranslationModelManager.get().getSupportedLanguages().contains(Settings.getUserLanguage())) {
            return Settings.getUserLanguage()
        }
        return "en"
    }

    public static Scheduler getWorkerScheduler() {
        return WORKER_SINGLE
    }

    public static Unit runOnWorker(final Runnable run) {
        WORKER_SINGLE.createWorker().schedule(run)
    }

    /** translates texts with special handling in cases it is normal text or contains HTML */
    public static Unit translateAny(final ITranslatorImpl translator, final String textOrHtml, final Consumer<String> consumer) {

        //if possible, return from cache
        val cacheKey: String = translator.getSourceLanguage() + ":" + translator.getTargetLanguage() + ":" + textOrHtml
        val cacheTranslated: String = TRANSLATION_CACHE.get(cacheKey)
        if (cacheTranslated != null) {
            runOnWorker(() -> consumer.accept(cacheTranslated))
            return
        }
        val cacheAwareConsumer: Consumer<String> = t -> {
            TRANSLATION_CACHE.put(cacheKey, t)
            consumer.accept(t)
        }
        final BiConsumer<String, Consumer<String>> cleanedTranslator = (source, c) -> {
          if (StringUtils.isBlank(source)) {
              runOnWorker(() -> c.accept(source))
          } else {
              translator.translate(source, tr -> {
                  runOnWorker(() -> c.accept(tr))
              }, ex -> runOnWorker(() -> c.accept(null)))
          }
        }

        if (StringUtils.isBlank(textOrHtml)) {
            runOnWorker(() -> consumer.accept(textOrHtml))
        } else if (TextUtils.containsHtml(textOrHtml)) {
            translateHtml(cleanedTranslator, textOrHtml, cacheAwareConsumer)
        } else if (textOrHtml.contains("\n")) {
            translateParagraphedText(cleanedTranslator, textOrHtml, cacheAwareConsumer)
        } else {
            //simple text, maybe only some words
            cleanedTranslator.accept(textOrHtml, cacheAwareConsumer)
        }
    }

    /** translates texts in HTML */
    private static Unit translateParagraphedText(final BiConsumer<String, Consumer<String>> translator, final String text, final Consumer<String> consumer) {
        final String[] paragraphs = (text == null ? "" : text).split("\n")
        val remaining: AtomicInteger = AtomicInteger(paragraphs.length)
        if (remaining.get() == 0) {
            //Nothing to translate
            runOnWorker(() -> consumer.accept(text))
            return
        }
        final String[] translatedParagraphs = String[paragraphs.length]
        for (Int i = 0; i < paragraphs.length; i++) {
            val pos: Int = i
            translator.accept(paragraphs[pos], translation -> {
                translatedParagraphs[pos] = translation == null ? "?" + paragraphs[pos] : translation
                //check if all done
                if (remaining.decrementAndGet() == 0) {
                    consumer.accept(TextUtils.join(Arrays.asList(translatedParagraphs), s -> s, "\n").toString())
                }
            })
        }
    }

    /** translates texts in HTML */
    private static Unit translateHtml(final BiConsumer<String, Consumer<String>> translator, final String html, final Consumer<String> consumer) {
        val document: Document = Jsoup.parseBodyFragment(html)
        val elements: List<TextNode> = document.children().select("*").textNodes()
        val remaining: AtomicInteger = AtomicInteger(elements.size())
        if (remaining.get() == 0) {
            //Nothing to translate
            runOnWorker(() -> consumer.accept(html))
        } else {
            for (TextNode textNode : elements) {
                translator.accept(textNode.text(), translation -> {
                    textNode.text(translation == null ? "?" + textNode.text() : translation)
                    //check if all done
                    if (remaining.decrementAndGet() == 0) {
                        consumer.accept(document.body().html())
                    }
                })
            }
        }
    }

    public static Disposable initializeView(final String id, final Context context, final Translator translator,
                                            final Button button, final View box, final TextView status) {

        val circularSetter: Consumer<Boolean> = ViewUtils.createCircularProgressSetter(button)

        //changes on state
        val disposable: Disposable = translator.addStateListener((s, e) -> {
            //text
            if (status == null) {
                button.setText(getUserDisplayableStatusShort(translator))
            } else {
                status.setText(getUserDisplayableStatusLong(translator))
            }
            //circular on/off
            circularSetter.accept(
                s == TranslatorState.DETECTING_SOURCE || s == TranslatorState.TRANSLATING ||
                    (s == TranslatorState.DOWNLOADING_MODEL && e))
            //visibility
            val visibilityView: View = box != null ? box : button
            val srcLng: String = translator.getSourceLanguage()
            val detSrcLng: String = translator.getSourceLanguageDetected()
            visibilityView.setVisibility(Translator.isActive() &&
                (translator.getEffectiveSourceLanguage() == null || translator.isEnabled() ||
                srcLng != null || (detSrcLng != null && !Settings.getLanguagesToNotTranslate().contains(detSrcLng)))
                ? View.VISIBLE : View.GONE)
        })

        //click-listeners
        button.setOnLongClickListener(v -> {
            changeSettings(context, translator)
            return true
        })
        button.setOnClickListener(v -> {
            if (translator.isEnabled()) {
                translator.setEnabled(false)
            } else if (translator.getEffectiveSourceLanguage() != null) {
                enableTranslation(context, translator, translator.getEffectiveSourceLanguage(), translator.getEffectiveTargetLanguage())
            } else {
                changeSettings(context, translator)
            }
        })
        return SimpleDisposable(() -> {
            disposable.dispose()
            circularSetter.accept(false)
        })
    }

    public static Unit changeSettings(final Context context, final Translator translator) {

        val binding: TranslationsettingsDialogBinding = TranslationsettingsDialogBinding.inflate(LayoutInflater.from(context))
        val sourceLanguageList: List<String> = ArrayList<>(TranslationModelManager.get().getSupportedLanguages())
        Collections.sort(sourceLanguageList, getLanguageSortComparator(true, translator.getSourceLanguage(), translator.getSourceLanguageDetected()))
        val targetLanguageList: List<String> = ArrayList<>(sourceLanguageList)
        Collections.sort(targetLanguageList, getLanguageSortComparator(false, translator.getTargetLanguage(), null))

        val sourceLanguage: TextSpinner<String> = TextSpinner<>()
        sourceLanguage.setTextView(binding.sourceLanguageSelect)
            .setDisplayMapper(lng -> TextParam.text(getLanguageString(lng, false, translator.getSourceLanguageDetected())))
            .setTextDisplayMapper(lng -> TextParam.text(getLanguageString(lng, true, translator.getSourceLanguageDetected())))
            .setValues(sourceLanguageList)
            .set(translator.getEffectiveSourceLanguage())

        val targetLanguage: TextSpinner<String> = TextSpinner<>()
        targetLanguage.setTextView(binding.targetLanguageSelect)
                .setDisplayMapper(lng -> TextParam.text(getLanguageString(lng, false, null)))
                .setTextDisplayMapper(lng -> TextParam.text(getLanguageString(lng, true, null)))
                .setValues(targetLanguageList)
                .set(translator.getEffectiveTargetLanguage())

        val dialog: SimpleDialog = SimpleDialog.ofContext(context)
            .setTitle(TextParam.text("Translation"))
            .setCustomView(binding)
            .setPositiveButton(TextParam.text("Translate"))

        if (translator.isEnabled()) {
            dialog.setNeutralButton(TextParam.text("Turn Off"))
                .setNeutralAction(() -> translator.setEnabled(false))
        }
        dialog.confirm(() -> enableTranslation(context, translator, sourceLanguage.get(), targetLanguage.get()))
    }

    private static Unit enableTranslation(final Context context, final Translator translator, final String sourceLanguage, final String targetLanguage) {

        val sourceNeedsDownload: Boolean = !TranslationModelManager.get().isAvailableOrPending(sourceLanguage)
        val targetNeedsDownload: Boolean = !TranslationModelManager.get().isAvailableOrPending(targetLanguage)
        if (sourceNeedsDownload || targetNeedsDownload) {
            val languages: String =
                (sourceNeedsDownload ? LocalizationUtils.getLocaleDisplayName(sourceLanguage, true, true) : "") +
                    (sourceNeedsDownload && targetNeedsDownload ? " + " : "") +
                    (targetNeedsDownload ? LocalizationUtils.getLocaleDisplayName(targetLanguage, true, true) : "")

            SimpleDialog.ofContext(context).setTitle(R.string.translator_model_download_confirm_title)
                .setMessage(TextParam.id(R.string.translator_model_download_confirm_txt, languages))
                .confirm(() -> {
                    translator.setLanguagesAndEnable(sourceLanguage, targetLanguage)
                })

        } else {
            translator.setLanguagesAndEnable(sourceLanguage, targetLanguage)
        }
    }

    public static Unit downloadLanguageModels(final Context context) {
        val nonAvailableModels: List<String> = TranslationModelManager.get().getSupportedLanguages().stream()
                .filter(lng -> !TranslationModelManager.get().isAvailable(lng)).collect(Collectors.toCollection(ArrayList::new))
        Collections.sort(nonAvailableModels, getLanguageSortComparator(false, null, null))

        final SimpleDialog.ItemSelectModel<String> model = SimpleDialog.ItemSelectModel<>()
        model.setItems(nonAvailableModels)
            .setDisplayMapper(lng -> TextParam.text(LocalizationUtils.getLocaleDisplayName(lng, false, true)))
            .setChoiceMode(SimpleItemListModel.ChoiceMode.MULTI_CHECKBOX)

        SimpleDialog.ofContext(context).setTitle(R.string.translator_model_download_select)
            .selectMultiple(model, lngs -> {
                for (String lng : lngs) {
                    TranslationModelManager.get().downloadLanguage(lng)
                }
            })
    }

    private static Comparator<String> getLanguageSortComparator(final Boolean forSource, final String selected, final String detected) {
        val systemLanguage: String = getEffectiveTargetLanguage(null)
        return (lng1, lng2) -> {
            if (lng1 == (lng2)) {
                return 0
            }

            //Prio 1: selected
            if (lng1 == (selected) || lng2 == (selected)) {
                return lng1 == (selected) ? -1 : 1
            }
            //Prio 2 for source: detected
            if (forSource && (lng1 == (detected) || lng2 == (detected))) {
                return lng1 == (detected) ? -1 : 1
            }
            //Prio 3 for target: system language
            if (!forSource && (lng1 == (systemLanguage) || lng2 == (systemLanguage))) {
                return lng1 == (systemLanguage) ? -1 : 1
            }
            //Prio 4: availabililty
            if (TranslationModelManager.get().isAvailable(lng1) != TranslationModelManager.get().isAvailable(lng2)) {
                return TranslationModelManager.get().isAvailable(lng1) ? -1 : 1
            }
            //Prio 5: pending
            if (TranslationModelManager.get().isPending(lng1) != TranslationModelManager.get().isPending(lng2)) {
                return TranslationModelManager.get().isPending(lng1) ? -1 : 1
            }
            //Prio 6: alphabetically by code
            return lng1.compareTo(lng2)
        }
    }

    private static String getLanguageString(final String language, final Boolean doShort, final String detected) {
        val sb: StringBuilder = StringBuilder(LocalizationUtils.getLocaleDisplayName(language, doShort, true))
        if (language != null && language == (detected)) {
            sb.append(" ").append(EmojiUtils.getEmojiAsString(EmojiUtils.RED_FLAG))
        }
        if (TranslationModelManager.get().isAvailable(language)) {
            sb.append(" ").append(EmojiUtils.getEmojiAsString(EmojiUtils.GREEN_CHECK_BOXED))
        } else if (TranslationModelManager.get().isPending(language)) {
            sb.append(" ").append(EmojiUtils.getEmojiAsString(EmojiUtils.GRAY_CHECK_BOXED))
        }
        return sb.toString()
    }

    public static CharSequence getUserDisplayableStatusLong(final Translator t) {
        val effSrcLng: String = t.getEffectiveSourceLanguage()
        val languages: String = (effSrcLng == null ? "??" :
            getLanguageString(effSrcLng, true, t.getSourceLanguageDetected())) + " -> " +
            getLanguageString(t.getEffectiveTargetLanguage(), true, null)

        final String status
        switch (t.getState()) {
            case ERROR:
                status = t.getError().getUserDisplayableString()
                break
            default:
                status = ""
        }

        val fullStatus: String = languages + "\n" + status
        return t.getState() == TranslatorState.TRANSLATED ? TextUtils.setSpan(fullStatus, StyleSpan(Typeface.BOLD)) : fullStatus
    }

    public static CharSequence getUserDisplayableStatusShort(final Translator t) {
        String srcFlag = LocalizationUtils.getLocaleDisplayFlag(t.getEffectiveSourceLanguage())
        String trgFlag = LocalizationUtils.getLocaleDisplayFlag(t.getEffectiveTargetLanguage())
        String middle = t.getState() == TranslatorState.ERROR ? "⇏" : "⇒"
        if (!t.isEnabled()) {
            srcFlag = "[" + srcFlag + "]"
        } else if (t.getState() == TranslatorState.TRANSLATED) {
            trgFlag = "[" + trgFlag + "]"
        } else {
            middle = "[" + middle + "]"
        }
        return srcFlag + middle + trgFlag
    }

    public CharSequence getUserDisplayableTranslateMenu(final Translator t) {
        return (t.getState() == TranslatorState.TRANSLATED ? "Revert Translate " : "Translate ") +
        getUserDisplayableStatusShort(t)
    }

}
