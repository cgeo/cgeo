package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;

import android.app.Activity;
import android.content.Context;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

public class MlKitTranslateUtil {

    private MlKitTranslateUtil() {
        // utility class
    }

    private static List<Language> getSupportedLanguages() {
        final List<Language> languages = new ArrayList<>();
        final List<String> languageIds = TranslateLanguage.getAllLanguages();
        for (String languageId : languageIds) {
            languages.add(new Language(TranslateLanguage.fromLanguageTag(languageId)));
        }
        return languages;
    }

    public static CharSequence[] getSupportedLanguageDisplaynames() {
        return getSupportedLanguages().stream().map(Language::toString).toArray(CharSequence[]::new);
    }

    public static CharSequence[] getSupportedLanguageCodes() {
        return getSupportedLanguages().stream().map(Language::getCode).toArray(CharSequence[]::new);
    }

    /**
     * Utility method to override the source language, shows a list of all available languages
     * @param context   calling context
     * @param consumer  selected language
     */
    public static void showLanguageSelection(final Context context, final Consumer<Language> consumer) {
        final AlertDialog.Builder builder = Dialogs.newBuilder(context);
        builder.setTitle(R.string.translator_language_select);

        final ArrayAdapter<Language> adapter = new ArrayAdapter<>(context, R.layout.select_dialog_item, getSupportedLanguages());

        builder.setAdapter(adapter, (dialog, item) -> {
            final Language language = adapter.getItem(item);
            consumer.accept(language);
            dialog.dismiss();
        });

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
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

    /**
     * Utility method to get a Translator object that can be used to perform offline translation
     * @param activity      the calling activity
     * @param sourceLng     language to translate from
     * @param unsupportedLngConsumer    returned if the sourceLng is not supported
     * @param downloadingModelConsumer  returned if language models need to be downloaded, should be used to show a progress UI to the user
     * @param translatorConsumer        returns the translator object
     */
    public static void translateText(final Activity activity, final Language sourceLng, final Consumer<?> unsupportedLngConsumer, final Consumer<?> downloadingModelConsumer, final Consumer<Translator> translatorConsumer) {
        final String lng = TranslateLanguage.fromLanguageTag(sourceLng.getCode());
        if (null == lng) {
            unsupportedLngConsumer.accept(null);
            return;
        }

        findMissingLanguageModels(lng, missingLanguageModels -> {
            if (missingLanguageModels.isEmpty()) {
                MlKitTranslateUtil.getTranslator(lng, translatorConsumer);
            } else {
                SimpleDialog.of(activity).setTitle(R.string.translator_model_download_confirm_title).setMessage(TextParam.id(R.string.translator_model_download_confirm_txt, String.join(", ", missingLanguageModels.stream().map(MlKitTranslateUtil.Language::toString).collect(Collectors.toList())))).confirm(() -> {
                    downloadingModelConsumer.accept(null);
                    MlKitTranslateUtil.getTranslator(lng, translatorConsumer);
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
