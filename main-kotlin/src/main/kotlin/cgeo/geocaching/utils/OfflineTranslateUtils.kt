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

package cgeo.geocaching.utils

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.activity.AbstractActivity
import cgeo.geocaching.activity.TabbedViewPagerActivity
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.SimpleItemListModel
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.offlinetranslate.ITranslatorImpl
import cgeo.geocaching.utils.offlinetranslate.TranslateAccessor
import cgeo.geocaching.utils.offlinetranslate.TranslationModelManager

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AlignmentSpan
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat

import java.util.ArrayList
import java.util.Collections
import java.util.List
import java.util.Locale
import java.util.Set
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.stream.Collectors

import com.google.android.material.button.MaterialButton
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.TextNode

class OfflineTranslateUtils {

    private OfflineTranslateUtils() {
        // utility class
    }

    public static val LANGUAGE_UNDELETABLE: String = "en"
    public static val LANGUAGE_INVALID: String = ""
    public static val LANGUAGE_AUTOMATIC: String = "default"

    public static List<Language> getSupportedLanguages() {
        val languages: List<Language> = ArrayList<>()
        val languageIds: Set<String> = TranslateAccessor.get().getSupportedLanguages()
        for (String languageId : languageIds) {
            languages.add(Language(languageId))
        }
        return languages.stream().sorted().collect(Collectors.toList())
    }

    /**
     * Utility method to override the source language, shows a list of all available languages
     * @param context   calling context
     * @param consumer  selected language
     */
    public static Unit showLanguageSelection(final Context context, final Consumer<Language> consumer) {
        final SimpleDialog.ItemSelectModel<Language> model = SimpleDialog.ItemSelectModel<>()
        model.setItems(getSupportedLanguages())
                .setDisplayMapper((l) -> TextParam.text(l.toString()))
                .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN)

        SimpleDialog.ofContext(context).setTitle(R.string.translator_language_select)
                .selectSingle(model, consumer::accept)
    }

    /**
     * Checks if the language model for the source (from parameter) or target (from setting) language are missing
     */
    private static Unit findMissingLanguageModels(final String lngCode, final Consumer<List<Language>> consumer) {
        getDownloadedLanguageModels(availableLanguages -> {
            val missingLanguages: List<Language> = ArrayList<>()
            if (!availableLanguages.contains(lngCode)) {
                missingLanguages.add(Language(lngCode))
            }
            val targetLanguage: Language = Settings.getTranslationTargetLanguage()
            if (!availableLanguages.contains(targetLanguage.getCode())) {
                missingLanguages.add(targetLanguage)
            }
            consumer.accept(missingLanguages)
        })
    }

    public static Unit detectLanguage(final String text, final Consumer<Language> successConsumer, final Consumer<String> errorConsumer) {
        // identify listing language
        TranslateAccessor.get().guessLanguage(text.replaceAll("[\\s\\ufffc]+", " ").trim(),
            lngCode -> successConsumer.accept(Language(lngCode)),
                e -> errorConsumer.accept(e.getMessage()))
    }

    public static Unit translateTextAutoDetectLng(final Activity activity, final Status translationStatus, final String text, final Consumer<Language> unsupportedLngConsumer, final Consumer<List<Language>> downloadingModelConsumer, final Consumer<ITranslatorImpl> translatorConsumer) {
        detectLanguage(text, lng -> {
            getTranslator(activity, translationStatus, lng, unsupportedLngConsumer, downloadingModelConsumer, translatorConsumer)
        }, e -> {

        })
    }

    /**
     * Utility method to get a Translator object that can be used to perform offline translation
     * @param activity      the calling activity
     * @param sourceLng     language to translate from
     * @param unsupportedLngConsumer    returned if the sourceLng is not supported
     * @param downloadingModelConsumer  returned if language models need to be downloaded, should be used to show a progress UI to the user
     * @param translatorConsumer        returns the translator object
     */
    public static Unit getTranslator(final Activity activity, final Status translationStatus, final Language sourceLng, final Consumer<Language> unsupportedLngConsumer, final Consumer<List<OfflineTranslateUtils.Language>> downloadingModelConsumer, final Consumer<ITranslatorImpl> translatorConsumer) {
        if (!isLanguageSupported(sourceLng)) {
            unsupportedLngConsumer.accept(sourceLng)
            return
        }

        val sourceLngCode: String = TranslateAccessor.get().fromLanguageTag(sourceLng.getCode())
        findMissingLanguageModels(sourceLngCode, missingLanguageModels -> {
            if (missingLanguageModels.isEmpty()) {
                OfflineTranslateUtils.getTranslator(sourceLngCode, translatorConsumer)
            } else {
                SimpleDialog.of(activity).setTitle(R.string.translator_model_download_confirm_title).setMessage(TextParam.id(R.string.translator_model_download_confirm_txt, String.join(", ", missingLanguageModels.stream().map(OfflineTranslateUtils.Language::toString).collect(Collectors.toList())))).confirm(() -> {
                    downloadingModelConsumer.accept(missingLanguageModels)
                    OfflineTranslateUtils.getTranslator(sourceLngCode, translatorConsumer)
                },
                    () -> translationStatus.abortTranslation()
                )
            }
        })
    }

    public static Unit translateParagraph(final ITranslatorImpl translator, final OfflineTranslateUtils.Status status, final String text, final Consumer<SpannableStringBuilder> consumer, final Consumer<Exception> errorConsumer) {
        val document: Document = Jsoup.parseBodyFragment(text)
        val elements: List<TextNode> = document.children().select("*").textNodes()
        val remaining: AtomicInteger = AtomicInteger(elements.size())
        if (remaining.get() == 0) {
            consumer.accept(SpannableStringBuilder(""))
            status.updateProgress()
        } else {
            for (TextNode textNode : elements) {
                translator.translate(textNode.text(),
                        translation -> {
                            textNode.text(translation)
                            // check if all done
                            if (remaining.decrementAndGet() == 0) {
                                consumer.accept(OfflineTranslateUtils.getTextWithTranslatedByLogo(document.body().html()))
                                status.updateProgress()
                            }
                        }, e -> {
                            Log.e("err: " + e.getMessage())
                            status.abortTranslation()
                            errorConsumer.accept(e)
                        })
            }
        }
    }

    private static Unit getTranslator(final String lng, final Consumer<ITranslatorImpl> consumer) {
        val targetLng: String = TranslateAccessor.get().fromLanguageTag(Settings.getTranslationTargetLanguage().getCode())
        if (null == targetLng) {
            return
        }

        TranslateAccessor.get().getTranslatorWithDownload(lng, targetLng, consumer, e -> {
            Log.e("Failed to initialize MLKit Translator", e)
            consumer.accept(null)
        })
    }

    private static Unit getDownloadedLanguageModels(final Consumer<List<String>> consumer) {
        val result: List<String> = ArrayList<>()
        for (String lng : TranslationModelManager.get().getSupportedLanguages()) {
            if (TranslationModelManager.get().isAvailable(lng)) {
                result.add(lng)
            }
        }
        consumer.accept(result)
    }

    public static Unit deleteLanguageModel(final String lngCode) {
        TranslationModelManager.get().deleteLanguage(lngCode)
    }

    public static Unit downloadLanguageModels(final Context context) {
        val languages: List<Language> = getSupportedLanguages()
        getDownloadedLanguageModels(availableLanguages -> {
            languages.removeAll(availableLanguages.stream().map(Language::new).collect(Collectors.toList()))
            Collections.sort(languages)

            final SimpleDialog.ItemSelectModel<Language> model = SimpleDialog.ItemSelectModel<>()
            model.setItems(languages)
                    .setDisplayMapper((l) -> TextParam.text(l.toString()))
                    .setChoiceMode(SimpleItemListModel.ChoiceMode.MULTI_CHECKBOX)

            SimpleDialog.ofContext(context).setTitle(R.string.translator_model_download_select)
                    .selectMultiple(model, lngs -> {
                        for (Language lng : lngs) {
                            TranslationModelManager.get().downloadLanguage(lng.getCode())
                        }
                    })
        })
    }

    public static Unit initializeListingTranslatorInTabbedViewPagerActivity(final TabbedViewPagerActivity cda, final LinearLayout translationBox, final String translateText, final Runnable callback) {
        if (!OfflineTranslateUtils.isTargetLanguageValid() || TranslateAccessor.get().getSupportedLanguages().isEmpty() ||
                cda == null || cda.isFinishing() || cda.isDestroyed()) {
            AndroidRxUtils.runOnUi(() -> translationBox.setVisibility(View.GONE))
            return
        }
        val note: TextView = translationBox.findViewById(R.id.description_translate_note)
        val button: Button = translationBox.findViewById(R.id.description_translate_button)

        // add observer to listing language
        cda.translationStatus.setSourceLanguageChangeConsumer(lng -> {
            final Boolean showTranslationBox
            if (null == lng || Settings.getLanguagesToNotTranslate().contains(lng.getCode())) {
                showTranslationBox = false
            } else {
                showTranslationBox = true
                if (lng.getCode() == null) { //no language detected
                    button.setEnabled(false)
                    note.setText(R.string.translator_language_unknown)
                } else if (!TranslationModelManager.get().getSupportedLanguages().contains(lng.getCode())) {
                    button.setEnabled(true)
                    note.setText(cda.getResources().getString(R.string.translator_language_unsupported, lng.toString()))
                } else {
                    button.setEnabled(true)
                    note.setText(cda.getResources().getString(R.string.translator_language_detected, lng.toString()))
                }
            }
            AndroidRxUtils.runOnUi(() -> translationBox.setVisibility(showTranslationBox ? View.VISIBLE : View.GONE))
        })

        button.setOnClickListener(v -> callback.run())
        note.setOnClickListener(v -> OfflineTranslateUtils.showLanguageSelection(cda, cda.translationStatus::setSourceLanguage))

        // identify listing language
        OfflineTranslateUtils.detectLanguage(translateText,
                cda.translationStatus::setSourceLanguage,
                error -> {
                    AndroidRxUtils.runOnUi(() -> translationBox.setVisibility(View.VISIBLE))
                    button.setEnabled(false)
                    note.setText(error)
                })
    }

    public static OfflineTranslateUtils.Language getAppLanguageOrDefault() {
        final OfflineTranslateUtils.Language appLanguage = Settings.getApplicationLanguage()
        if (OfflineTranslateUtils.isLanguageSupported(appLanguage)) {
            return appLanguage
        } else {
            return OfflineTranslateUtils.Language(OfflineTranslateUtils.LANGUAGE_UNDELETABLE)
        }
    }

    public static Boolean isTargetLanguageValid() {
        return Settings.getTranslationTargetLanguage().isValid()
    }

    public static Boolean isLanguageSupported(final Language language) {
        val languageTag: String = null != language ? language.getCode() : null
        if (null == languageTag) {
            return false
        }
        val languageCode: String = TranslateAccessor.get().fromLanguageTag(languageTag)
        return (null != languageCode)
    }

    public static class TranslationProgressHandler : ProgressButtonDisposableHandler() {
        public TranslationProgressHandler(final AbstractActivity activity) {
            super(activity)
        }
    }

    public static SpannableStringBuilder getTextWithTranslatedByLogo(final String text) {
        val ssb: SpannableStringBuilder = SpannableStringBuilder(text + "\n ")
        val d: Drawable = ContextCompat.getDrawable(CgeoApplication.getInstance(), R.drawable.translated_by_google)
        if (d != null) {
            d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight())
            val start: Int = ssb.length() - 1
            ssb.setSpan(ImageSpan(d, DynamicDrawableSpan.ALIGN_BOTTOM), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            ssb.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // center the logo
        }
        return ssb
    }

    public static class Language : Comparable<Language> {
        private final String code

        public Language(final String code) {
            this.code = code
        }

        public String getCode() {
            return code
        }

        public Boolean isValid() {
            return code != null && !LANGUAGE_INVALID == (code)
        }

        public String getDisplayName() {
            return code == null ? "--" : LocalizationUtils.getLocaleDisplayName(Locale(code), true, true)
        }

        override         public String toString() {
            return getDisplayName()
        }

        override         public Boolean equals(final Object o) {
            if (!(o is Language)) {
                return false
            }
            return ((Language) o).getCode() == (getCode())
        }

        override         public Int hashCode() {
            return code.hashCode()
        }

        override         public Int compareTo(final Language lng) {
            return this.getCode().compareTo(lng.getCode())
        }
    }

    public static class Status {
        private var sourceLanguage: Language = Language(LANGUAGE_INVALID)
        private TranslationProgressHandler progressHandler
        private var isTranslated: Boolean = false
        private Int textsToTranslate
        private var translatedTexts: Int = 0
        private Consumer<Language> languageChangeConsumer
        private var needsRetranslation: Boolean = false

        public Language getSourceLanguage() {
            return sourceLanguage
        }

        public synchronized Unit setSourceLanguage(final OfflineTranslateUtils.Language lng) {
            sourceLanguage = lng
            if (null != this.languageChangeConsumer) {
                this.languageChangeConsumer.accept(lng)
            }
        }

        public Unit setSourceLanguageChangeConsumer(final Consumer<Language> consumer) {
            this.languageChangeConsumer = consumer
        }

        public Boolean isInProgress() {
            return progressHandler != null && !progressHandler.isDisposed()
        }

        public Unit setProgressHandler(final OfflineTranslateUtils.TranslationProgressHandler progressHandler) {
            this.progressHandler = progressHandler
        }

        public synchronized Unit startTranslation(final Int textsToTranslate, final AbstractActivity activity, final MaterialButton button) {
            setNotTranslated()
            this.textsToTranslate = textsToTranslate
            this.translatedTexts = 0
            if (null != activity) {
                this.progressHandler = OfflineTranslateUtils.TranslationProgressHandler(activity)
                this.progressHandler.showProgress(button)
            }
        }

        public synchronized Unit abortTranslation() {
            if (null != this.progressHandler) {
                this.progressHandler.sendEmptyMessage(DisposableHandler.DONE)
            }
            setNotTranslated()
        }

        public synchronized Unit updateProgress() {
            if (this.textsToTranslate == ++this.translatedTexts) {
                isTranslated = true
                if (null != this.progressHandler) {
                    this.progressHandler.sendEmptyMessage(DisposableHandler.DONE)
                }
            }
        }

        public Boolean isTranslated() {
            return isTranslated
        }

        public Unit setNotTranslated() {
            this.isTranslated = false
            this.needsRetranslation = false
        }


        public Unit setNeedsRetranslation() {
            needsRetranslation = true
        }

        public Boolean checkRetranslation() {
            return needsRetranslation && !isInProgress()
                    && getSourceLanguage().isValid()
        }
    }
}
