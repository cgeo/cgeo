package cgeo.geocaching.utils.offlinetranslate;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.TranslationsettingsDialogBinding;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.TextSpinner;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.EmojiUtils;
import cgeo.geocaching.utils.LeastRecentlyUsedMap;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.SimpleDisposable;
import cgeo.geocaching.utils.TextUtils;

import android.content.Context;
import android.graphics.Typeface;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;

public class TranslatorUtils {

    private static final String LOGPRAEFIX = "[TranslatorUtils]:";

    private static final Scheduler WORKER_SINGLE = Schedulers.single();

    private static final Map<String, String> TRANSLATION_CACHE = Collections.synchronizedMap(new LeastRecentlyUsedMap.LruCache<>(300));

    public static class ChangeableText implements Disposable {
        private final Translator translator;
        private String text;
        private Disposable disposable;

        public ChangeableText(final Translator translator) {
            this.translator = translator;
        }

        public void set(final String text, final BiConsumer<String, Boolean> textAction) {
            if (Objects.equals(this.text, text)) {
                return;
            }
            if (this.disposable != null) {
                this.disposable.dispose();
            }
            this.text = text;
            this.disposable = translator.addTranslation(this.text, textAction);
        }


        @Override
        public void dispose() {
            if (this.disposable != null) {
                this.disposable.dispose();
            }
        }

        @Override
        public boolean isDisposed() {
            return this.disposable != null && this.disposable.isDisposed();
        }
    }

    private TranslatorUtils() {
        //no instance
    }

    @Nullable
    public static String getEffectiveSourceLanguage(final String sourceLanguage, final String sourceLanguageDetected) {
        return sourceLanguage != null ? sourceLanguage : sourceLanguageDetected;
    }

    @NonNull
    public static String getEffectiveTargetLanguage(final String targetLanguage) {
        if (targetLanguage != null) {
            return targetLanguage;
        }
        if (TranslationModelManager.get().getSupportedLanguages().contains(Settings.getTranslationTargetLanguageCode())) {
            return Settings.getTranslationTargetLanguageCode();
        }
        if (TranslationModelManager.get().getSupportedLanguages().contains(Settings.getUserLanguage())) {
            return Settings.getUserLanguage();
        }
        return "en";
    }

    public static Scheduler getWorkerScheduler() {
        return WORKER_SINGLE;
    }

    public static void runOnWorker(final Runnable run) {
        WORKER_SINGLE.createWorker().schedule(run);
    }

    /** translates texts with special handling in cases it is normal text or contains HTML */
    public static void translateAny(@NonNull final ITranslatorImpl translator, final String textOrHtml, final Consumer<String> consumer) {

        //if possible, return from cache
        final String cacheKey = translator.getSourceLanguage() + ":" + translator.getTargetLanguage() + ":" + textOrHtml;
        final String cacheTranslated = TRANSLATION_CACHE.get(cacheKey);
        if (cacheTranslated != null) {
            runOnWorker(() -> consumer.accept(cacheTranslated));
            return;
        }
        final Consumer<String> cacheAwareConsumer = t -> {
            TRANSLATION_CACHE.put(cacheKey, t);
            consumer.accept(t);
        };
        final BiConsumer<String, Consumer<String>> cleanedTranslator = (source, c) -> {
          if (StringUtils.isBlank(source)) {
              runOnWorker(() -> c.accept(source));
          } else {
              translator.translate(source, tr -> {
                  runOnWorker(() -> c.accept(tr));
              }, ex -> runOnWorker(() -> c.accept(null)));
          }
        };

        if (StringUtils.isBlank(textOrHtml)) {
            runOnWorker(() -> consumer.accept(textOrHtml));
        } else if (TextUtils.containsHtml(textOrHtml)) {
            translateHtml(cleanedTranslator, textOrHtml, cacheAwareConsumer);
        } else if (textOrHtml.contains("\n")) {
            translateParagraphedText(cleanedTranslator, textOrHtml, cacheAwareConsumer);
        } else {
            //simple text, maybe only some words
            cleanedTranslator.accept(textOrHtml, cacheAwareConsumer);
        }
    }

    /** translates texts in HTML */
    private static void translateParagraphedText(final BiConsumer<String, Consumer<String>> translator, final String text, final Consumer<String> consumer) {
        final String[] paragraphs = (text == null ? "" : text).split("\n");
        final AtomicInteger remaining = new AtomicInteger(paragraphs.length);
        if (remaining.get() == 0) {
            //Nothing to translate
            runOnWorker(() -> consumer.accept(text));
            return;
        }
        final String[] translatedParagraphs = new String[paragraphs.length];
        for (int i = 0; i < paragraphs.length; i++) {
            final int pos = i;
            translator.accept(paragraphs[pos], translation -> {
                translatedParagraphs[pos] = translation == null ? "?" + paragraphs[pos] : translation;
                //check if all done
                if (remaining.decrementAndGet() == 0) {
                    consumer.accept(TextUtils.join(Arrays.asList(translatedParagraphs), s -> s, "\n").toString());
                }
            });
        }
    }

    /** translates texts in HTML */
    private static void translateHtml(final BiConsumer<String, Consumer<String>> translator, final String html, final Consumer<String> consumer) {
        final Document document = Jsoup.parseBodyFragment(html);
        final List<TextNode> elements = document.children().select("*").textNodes();
        final AtomicInteger remaining = new AtomicInteger(elements.size());
        if (remaining.get() == 0) {
            //Nothing to translate
            runOnWorker(() -> consumer.accept(html));
        } else {
            for (TextNode textNode : elements) {
                translator.accept(textNode.text(), translation -> {
                    textNode.text(translation == null ? "?" + textNode.text() : translation);
                    //check if all done
                    if (remaining.decrementAndGet() == 0) {
                        consumer.accept(document.body().html());
                    }
                });
            }
        }
    }

    public static Disposable initializeView(final String id, final Context context, final Translator translator,
                                            final Button button, final View box, final TextView status) {

        final Consumer<Boolean> circularSetter = ViewUtils.createCircularProgressSetter(button);

        //changes on state
        final Disposable disposable = translator.addStateListener((s, e) -> {
            //text
            if (status == null) {
                button.setText(getUserDisplayableStatusShort(translator));
            } else {
                status.setText(getUserDisplayableStatusLong(translator));
            }
            //circular on/off
            circularSetter.accept(
                s == TranslatorState.DETECTING_SOURCE || s == TranslatorState.TRANSLATING ||
                    (s == TranslatorState.DOWNLOADING_MODEL && e));
            //visibility
            final View visibilityView = box != null ? box : button;
            final String srcLng = translator.getSourceLanguage();
            final String detSrcLng = translator.getSourceLanguageDetected();
            visibilityView.setVisibility(Translator.isSupported() &&
                (translator.getEffectiveSourceLanguage() == null || translator.isEnabled() ||
                srcLng != null || (detSrcLng != null && !Settings.getLanguagesToNotTranslate().contains(detSrcLng)))
                ? View.VISIBLE : View.GONE);
        });

        //click-listeners
        button.setOnLongClickListener(v -> {
            changeSettings(context, translator);
            return true;
        });
        button.setOnClickListener(v -> {
            if (translator.isEnabled()) {
                translator.setEnabled(false);
            } else if (translator.getEffectiveSourceLanguage() != null) {
                enableTranslation(context, translator, translator.getEffectiveSourceLanguage(), translator.getEffectiveTargetLanguage());
            } else {
                changeSettings(context, translator);
            }
        });
        return new SimpleDisposable(() -> {
            disposable.dispose();
            circularSetter.accept(false);
        });
    }

    public static void changeSettings(final Context context, final Translator translator) {

        final TranslationsettingsDialogBinding binding = TranslationsettingsDialogBinding.inflate(LayoutInflater.from(context));
        final List<String> sourceLanguageList = new ArrayList<>(TranslationModelManager.get().getSupportedLanguages());
        Collections.sort(sourceLanguageList, getLanguageSortComparator(true, translator.getSourceLanguage(), translator.getSourceLanguageDetected()));
        final List<String> targetLanguageList = new ArrayList<>(sourceLanguageList);
        Collections.sort(targetLanguageList, getLanguageSortComparator(false, translator.getTargetLanguage(), null));

        final TextSpinner<String> sourceLanguage = new TextSpinner<>();
        sourceLanguage.setTextView(binding.sourceLanguageSelect)
            .setDisplayMapper(lng -> TextParam.text(getLanguageString(lng, false, translator.getSourceLanguageDetected())))
            .setTextDisplayMapper(lng -> TextParam.text(getLanguageString(lng, true, translator.getSourceLanguageDetected())))
            .setValues(sourceLanguageList)
            .set(translator.getEffectiveSourceLanguage());

        final TextSpinner<String> targetLanguage = new TextSpinner<>();
        targetLanguage.setTextView(binding.targetLanguageSelect)
                .setDisplayMapper(lng -> TextParam.text(getLanguageString(lng, false, null)))
                .setTextDisplayMapper(lng -> TextParam.text(getLanguageString(lng, true, null)))
                .setValues(targetLanguageList)
                .set(translator.getEffectiveTargetLanguage());

        final SimpleDialog dialog = SimpleDialog.ofContext(context)
            .setTitle(TextParam.text("Translation"))
            .setCustomView(binding)
            .setPositiveButton(TextParam.text("Translate"));

        if (translator.isEnabled()) {
            dialog.setNeutralButton(TextParam.text("Turn Off"))
                .setNeutralAction(() -> translator.setEnabled(false));
        }
        dialog.confirm(() -> enableTranslation(context, translator, sourceLanguage.get(), targetLanguage.get()));
    }

    private static void enableTranslation(final Context context, final Translator translator, final String sourceLanguage, final String targetLanguage) {

        final boolean sourceNeedsDownload = !TranslationModelManager.get().isAvailableOrPending(sourceLanguage);
        final boolean targetNeedsDownload = !TranslationModelManager.get().isAvailableOrPending(targetLanguage);
        if (sourceNeedsDownload || targetNeedsDownload) {
            final String languages =
                (sourceNeedsDownload ? LocalizationUtils.getLocaleDisplayName(sourceLanguage, true, true) : "") +
                    (sourceNeedsDownload && targetNeedsDownload ? " + " : "") +
                    (targetNeedsDownload ? LocalizationUtils.getLocaleDisplayName(targetLanguage, true, true) : "");

            SimpleDialog.ofContext(context).setTitle(R.string.translator_model_download_confirm_title)
                .setMessage(TextParam.id(R.string.translator_model_download_confirm_txt, languages))
                .confirm(() -> {
                    translator.setLanguagesAndEnable(sourceLanguage, targetLanguage);
                });

        } else {
            translator.setLanguagesAndEnable(sourceLanguage, targetLanguage);
        }
    }

    public static void downloadLanguageModels(final Context context) {
        final List<String> nonAvailableModels = TranslationModelManager.get().getSupportedLanguages().stream()
                .filter(lng -> !TranslationModelManager.get().isAvailable(lng)).collect(Collectors.toCollection(ArrayList::new));
        Collections.sort(nonAvailableModels, getLanguageSortComparator(false, null, null));

        final SimpleDialog.ItemSelectModel<String> model = new SimpleDialog.ItemSelectModel<>();
        model.setItems(nonAvailableModels)
            .setDisplayMapper(lng -> TextParam.text(LocalizationUtils.getLocaleDisplayName(lng, false, true)))
            .setChoiceMode(SimpleItemListModel.ChoiceMode.MULTI_CHECKBOX);

        SimpleDialog.ofContext(context).setTitle(R.string.translator_model_download_select)
            .selectMultiple(model, lngs -> {
                for (String lng : lngs) {
                    TranslationModelManager.get().downloadLanguage(lng);
                }
            });
    }

    private static Comparator<String> getLanguageSortComparator(final boolean forSource, final String selected, final String detected) {
        final String systemLanguage = getEffectiveTargetLanguage(null);
        return (lng1, lng2) -> {
            if (lng1.equals(lng2)) {
                return 0;
            }

            //Prio 1: selected
            if (lng1.equals(selected) || lng2.equals(selected)) {
                return lng1.equals(selected) ? -1 : 1;
            }
            //Prio 2 for source: detected
            if (forSource && (lng1.equals(detected) || lng2.equals(detected))) {
                return lng1.equals(detected) ? -1 : 1;
            }
            //Prio 3 for target: system language
            if (!forSource && (lng1.equals(systemLanguage) || lng2.equals(systemLanguage))) {
                return lng1.equals(systemLanguage) ? -1 : 1;
            }
            //Prio 4: availabililty
            if (TranslationModelManager.get().isAvailable(lng1) != TranslationModelManager.get().isAvailable(lng2)) {
                return TranslationModelManager.get().isAvailable(lng1) ? -1 : 1;
            }
            //Prio 5: pending
            if (TranslationModelManager.get().isPending(lng1) != TranslationModelManager.get().isPending(lng2)) {
                return TranslationModelManager.get().isPending(lng1) ? -1 : 1;
            }
            //Prio 6: alphabetically by code
            return lng1.compareTo(lng2);
        };
    }

    private static String getLanguageString(final String language, final boolean doShort, final String detected) {
        final StringBuilder sb = new StringBuilder(LocalizationUtils.getLocaleDisplayName(language, doShort, true));
        if (language != null && language.equals(detected)) {
            sb.append(" ").append(EmojiUtils.getEmojiAsString(EmojiUtils.RED_FLAG));
        }
        if (TranslationModelManager.get().isAvailable(language)) {
            sb.append(" ").append(EmojiUtils.getEmojiAsString(EmojiUtils.GREEN_CHECK_BOXED));
        } else if (TranslationModelManager.get().isPending(language)) {
            sb.append(" ").append(EmojiUtils.getEmojiAsString(EmojiUtils.GRAY_CHECK_BOXED));
        }
        return sb.toString();
    }

    public static CharSequence getUserDisplayableStatusLong(final Translator t) {
        final String effSrcLng = t.getEffectiveSourceLanguage();
        final String languages = (effSrcLng == null ? "??" :
            getLanguageString(effSrcLng, true, t.getSourceLanguageDetected())) + " -> " +
            getLanguageString(t.getEffectiveTargetLanguage(), true, null);

        final String status;
        switch (t.getState()) {
            case ERROR:
                status = t.getError().getUserDisplayableString();
                break;
            default:
                status = "";
        }

        final String fullStatus = languages + "\n" + status;
        return t.getState() == TranslatorState.TRANSLATED ? TextUtils.setSpan(fullStatus, new StyleSpan(Typeface.BOLD)) : fullStatus;
    }

    public static CharSequence getUserDisplayableStatusShort(final Translator t) {
        String srcFlag = LocalizationUtils.getLocaleDisplayFlag(t.getEffectiveSourceLanguage());
        String trgFlag = LocalizationUtils.getLocaleDisplayFlag(t.getEffectiveTargetLanguage());
        String middle = t.getState() == TranslatorState.ERROR ? "⇏" : "⇒";
        if (!t.isEnabled()) {
            srcFlag = "[" + srcFlag + "]";
        } else if (t.getState() == TranslatorState.TRANSLATED) {
            trgFlag = "[" + trgFlag + "]";
        } else {
            middle = "[" + middle + "]";
        }
        return srcFlag + middle + trgFlag;
    }

    public CharSequence getUserDisplayableTranslateMenu(final Translator t) {
        return (t.getState() == TranslatorState.TRANSLATED ? "Revert Translate " : "Translate ") +
        getUserDisplayableStatusShort(t);
    }

}
