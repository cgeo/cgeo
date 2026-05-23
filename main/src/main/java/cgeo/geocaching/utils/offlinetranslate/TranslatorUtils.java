package cgeo.geocaching.utils.offlinetranslate;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.LeastRecentlyUsedMap;
import cgeo.geocaching.utils.LocalizationUtils;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.lang3.StringUtils;

public class TranslatorUtils {

    private static final Scheduler WORKER_SINGLE = Schedulers.single();

    private static final Map<String, String> TRANSLATION_CACHE = Collections.synchronizedMap(new LeastRecentlyUsedMap.LruCache<>(300));

    public static class ChangeableText implements Disposable {
        private final Translator translator;
        private String text;
        private Disposable disposable;

        public ChangeableText(final Translator translator) {
            this(translator, null);
        }

        public ChangeableText(final Translator translator, final CompositeDisposable compositeDisposable) {
            this.translator = translator;
            if (compositeDisposable != null) {
                compositeDisposable.add(this);
            }
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

    public static boolean isTranslationActive() {
        //as of now, user deactivates translation by setting targetlanguagecode to empty...
        return !StringUtils.isBlank(Settings.getTranslationTargetLanguageCode());
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
        } else {
            cleanedTranslator.accept(textOrHtml, cacheAwareConsumer);
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

}
