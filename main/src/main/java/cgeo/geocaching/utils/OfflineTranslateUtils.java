package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

public class OfflineTranslateUtils {

    private OfflineTranslateUtils() {
        // utility class
    }

    public static String LANGUAGE_UNKNOWN = LanguageIdentifier.UNDETERMINED_LANGUAGE_TAG;

    public static List<Language> getSupportedLanguages() {
        final List<Language> languages = new ArrayList<>();
        final List<String> languageIds = TranslateLanguage.getAllLanguages();
        for (String languageId : languageIds) {
            languages.add(new Language(TranslateLanguage.fromLanguageTag(languageId)));
        }
        return languages.stream().sorted().collect(Collectors.toList());
    }

    /**
     * Utility method to override the source language, shows a list of all available languages
     * @param context   calling context
     * @param consumer  selected language
     */
    public static void showLanguageSelection(final Context context, final Consumer<Language> consumer) {
        final SimpleDialog.ItemSelectModel<Language> model = new SimpleDialog.ItemSelectModel<>();
        model.setItems(getSupportedLanguages())
                .setDisplayMapper((l) -> TextParam.text(l.toString()))
                .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN);

        SimpleDialog.ofContext(context).setTitle(R.string.translator_language_select)
                .selectSingle(model, consumer::accept);
    }

    /**
     * Checks if the language model for the source (from parameter) or target (from setting) language are missing
     */
    private static void findMissingLanguageModels(final String lngCode, final Consumer<List<Language>> consumer) {
        getDownloadedLanguageModels(availableLanguages -> {
            final List<Language> missingLanguages = new ArrayList<>();
            if (!availableLanguages.contains(lngCode)) {
                missingLanguages.add(new Language(lngCode));
            }
            if (!availableLanguages.contains(Settings.getTranslationTargetLanguage())) {
                missingLanguages.add(new Language(Settings.getTranslationTargetLanguage()));
            }
            consumer.accept(missingLanguages);
        });
    }

    public static void detectLanguage(final String text, final Consumer<Language> successConsumer, final Consumer<String> errorConsumer) {
        // identify listing language
        LanguageIdentification.getClient().identifyLanguage(text)
                .addOnSuccessListener(lngCode -> {
                    successConsumer.accept(new Language(lngCode));
                })
                .addOnFailureListener(e -> {
                    errorConsumer.accept(e.getMessage());
                });
    }

    public static void translateTextAutoDetectLng(final Activity activity, final String text, final Consumer<Language> unsupportedLngConsumer, final Consumer<?> downloadingModelConsumer, final Consumer<Translator> translatorConsumer) {
        detectLanguage(text, lng -> {
            translateText(activity, lng, unsupportedLngConsumer, downloadingModelConsumer, translatorConsumer);
        }, e -> {

        });
    }

    /**
     * Utility method to get a Translator object that can be used to perform offline translation
     * @param activity      the calling activity
     * @param sourceLng     language to translate from
     * @param unsupportedLngConsumer    returned if the sourceLng is not supported
     * @param downloadingModelConsumer  returned if language models need to be downloaded, should be used to show a progress UI to the user
     * @param translatorConsumer        returns the translator object
     */
    public static void translateText(final Activity activity, final Language sourceLng, final Consumer<Language> unsupportedLngConsumer, final Consumer<?> downloadingModelConsumer, final Consumer<Translator> translatorConsumer) {
        final String lng = TranslateLanguage.fromLanguageTag(sourceLng.getCode());
        if (null == lng) {
            unsupportedLngConsumer.accept(sourceLng);
            return;
        }

        findMissingLanguageModels(lng, missingLanguageModels -> {
            if (missingLanguageModels.isEmpty()) {
                OfflineTranslateUtils.getTranslator(lng, translatorConsumer);
            } else {
                SimpleDialog.of(activity).setTitle(R.string.translator_model_download_confirm_title).setMessage(TextParam.id(R.string.translator_model_download_confirm_txt, String.join(", ", missingLanguageModels.stream().map(OfflineTranslateUtils.Language::toString).collect(Collectors.toList())))).confirm(() -> {
                    downloadingModelConsumer.accept(null);
                    OfflineTranslateUtils.getTranslator(lng, translatorConsumer);
                });
            }
        });
    }

    private static void getTranslator(final String lng, final Consumer<Translator> consumer) {
        final TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(lng)
                .setTargetLanguage(TranslateLanguage.fromLanguageTag(Settings.getTranslationTargetLanguage()))
                .build();

        final Translator translator = Translation.getClient(options);

        translator.downloadModelIfNeeded(new DownloadConditions.Builder().build())
                .addOnSuccessListener(b -> {
                    consumer.accept(translator);
                })
                .addOnFailureListener(e -> {
                    Log.e("Failed to initialize MLKit Translator", e);
                    consumer.accept(null);
                });
    }

    private static void getDownloadedLanguageModels(final Consumer<List<String>> consumer) {
        RemoteModelManager.getInstance().getDownloadedModels(TranslateRemoteModel.class).addOnSuccessListener(remoteModels -> {
            final List<String> modelCodes = new ArrayList<>(remoteModels.size());
            for (TranslateRemoteModel model : remoteModels) {
                modelCodes.add(model.getLanguage());
            }
            consumer.accept(modelCodes);
        });
    }

    public static class LanguagePackDownloadHandler extends ProgressButtonDisposableHandler {
        public LanguagePackDownloadHandler(final AbstractActivity activity) {
            super(activity);
        }
    }

    public static class Language implements Comparable<Language> {
        private final String code;

        public Language(final String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public String getDisplayName() {
            return new Locale(code).getDisplayName();
        }

        @NonNull
        @Override
        public String toString() {
            return getDisplayName() + " (" + getCode() + ")";
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof Language)) {
                return false;
            }
            return ((Language) o).getCode().equals(getCode());
        }

        @Override
        public int hashCode() {
            return code.hashCode();
        }

        @Override
        public int compareTo(@NonNull final Language lng) {
            return this.toString().compareTo(lng.toString());
        }
    }
}
