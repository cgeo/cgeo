package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.TabbedViewPagerActivity;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.android.material.button.MaterialButton;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

public class OfflineTranslateUtils {

    private OfflineTranslateUtils() {
        // utility class
    }

    public static final String LANGUAGE_UNKNOWN = "und";
    public static final String LANGUAGE_UNDELETABLE = "en";
    public static final String LANGUAGE_INVALID = "";

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
            if (!availableLanguages.contains(Settings.getTranslationTargetLanguage().getCode())) {
                missingLanguages.add(Settings.getTranslationTargetLanguage());
            }
            consumer.accept(missingLanguages);
        });
    }

    public static void detectLanguage(final String text, final Consumer<Language> successConsumer, final Consumer<String> errorConsumer) {
        // identify listing language
        LanguageIdentification.getClient().identifyLanguage(text)
                .addOnSuccessListener(lngCode -> successConsumer.accept(new Language(lngCode)))
                .addOnFailureListener(e -> errorConsumer.accept(e.getMessage()));
    }

    public static void translateTextAutoDetectLng(final Activity activity, final String text, final Consumer<Language> unsupportedLngConsumer, final Consumer<List<Language>> downloadingModelConsumer, final Consumer<Translator> translatorConsumer) {
        detectLanguage(text, lng -> getTranslator(activity, lng, unsupportedLngConsumer, downloadingModelConsumer, translatorConsumer), e -> {

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
    public static void getTranslator(final Activity activity, final Language sourceLng, final Consumer<Language> unsupportedLngConsumer, final Consumer<List<OfflineTranslateUtils. Language>> downloadingModelConsumer, final Consumer<Translator> translatorConsumer) {
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
                    downloadingModelConsumer.accept(missingLanguageModels);
                    OfflineTranslateUtils.getTranslator(lng, translatorConsumer);
                });
            }
        });
    }

    public static void translateParagraph(final Translator translator, final OfflineTranslateUtils.Status status, final String text, final Consumer<String> consumer, final Consumer<Exception> errorConsumer) {
        final List<String> origText = Arrays.asList(text.split("\n"));
        final List<String> translatedText = new ArrayList<>();
        for (String p : origText) {
            translator.translate(p)
                    .addOnSuccessListener(translation -> {
                        translatedText.add(translation);
                        if (origText.size() == translatedText.size()) {
                            consumer.accept(String.join("\n", translatedText));
                            status.updateProgress();
                        }
                    })
                    .addOnFailureListener(e -> {
                        status.abortTranslation();
                        errorConsumer.accept(e);
                    });
        }
    }

    private static void getTranslator(final String lng, final Consumer<Translator> consumer) {
        final String targetLng = TranslateLanguage.fromLanguageTag(Settings.getTranslationTargetLanguage().getCode());
        if (null == targetLng) {
            return;
        }
        final TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(lng)
                .setTargetLanguage(targetLng)
                .build();

        final Translator translator = Translation.getClient(options);

        translator.downloadModelIfNeeded(new DownloadConditions.Builder().build())
                .addOnSuccessListener(b -> consumer.accept(translator))
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

    public static void deleteLanguageModel(final String lngCode) {
        final TranslateRemoteModel model = new TranslateRemoteModel.Builder(lngCode).build();
        RemoteModelManager.getInstance().deleteDownloadedModel(model).addOnFailureListener(e -> Log.e("Failed to delete TranslateRemoteModel", e));
    }

    public static void downloadLanguageModels(final Context context) {
        final List<Language> languages = getSupportedLanguages();
        getDownloadedLanguageModels(availableLanguages -> {
            languages.removeAll(availableLanguages.stream().map(Language::new).collect(Collectors.toList()));

            final SimpleDialog.ItemSelectModel<Language> model = new SimpleDialog.ItemSelectModel<>();
            model.setItems(languages)
                    .setDisplayMapper((l) -> TextParam.text(l.toString()))
                    .setChoiceMode(SimpleItemListModel.ChoiceMode.MULTI_CHECKBOX);

            SimpleDialog.ofContext(context).setTitle(R.string.translator_model_download_select)
                    .selectMultiple(model, lngs -> {
                        for (Language lng : lngs) {
                            RemoteModelManager.getInstance()
                                    .download(new TranslateRemoteModel.Builder(lng.getCode()).build(), new DownloadConditions.Builder().build())
                                    .addOnSuccessListener(s -> Toast.makeText(context, context.getString(R.string.translator_model_download_success, lng.toString()), Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(context, context.getString(R.string.translator_model_download_error, lng.toString(), e.getMessage()), Toast.LENGTH_LONG).show());
                        }
                    });
        });
    }

    public static void initializeListingTranslatorInTabbedViewPagerActivity(final TabbedViewPagerActivity cda, final LinearLayout translationBox, final String translateText, final Runnable callback) {
        if (!Settings.getTranslationTargetLanguage().isValid()) {
            translationBox.setVisibility(View.GONE);
            return;
        }
        final TextView note = translationBox.findViewById(R.id.description_translate_note);
        final Button button = translationBox.findViewById(R.id.description_translate_button);

        // add observer to listing language
        cda.translationStatus.setSourceLanguageChangeConsumer(lng -> {
            if (null == lng || Settings.getLanguagesToNotTranslate().contains(lng.getCode())) {
                translationBox.setVisibility(View.GONE);
            } else if (OfflineTranslateUtils.LANGUAGE_UNKNOWN.equals(lng.getCode())) {
                button.setEnabled(false);
                note.setText(R.string.translator_language_unknown);
            } else {
                button.setEnabled(true);
                note.setText(cda.getResources().getString(R.string.translator_language_detected, lng.toString()));
            }
        });

        button.setOnClickListener(v -> callback.run());
        note.setOnClickListener(v -> OfflineTranslateUtils.showLanguageSelection(cda, cda.translationStatus::setSourceLanguage));
        translationBox.setVisibility(View.VISIBLE);

        // identify listing language
        OfflineTranslateUtils.detectLanguage(translateText,
                cda.translationStatus::setSourceLanguage,
                error -> {
                    button.setEnabled(false);
                    note.setText(error);
                });
    }

    public static class TranslationProgressHandler extends ProgressButtonDisposableHandler {
        public TranslationProgressHandler(final AbstractActivity activity) {
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

        public boolean isValid() {
            return !LANGUAGE_INVALID.equals(code);
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

    public static class Status {
        private Language sourceLanguage = new Language(LANGUAGE_INVALID);
        private TranslationProgressHandler progressHandler;
        private boolean isTranslated = false;
        private int textsToTranslate;
        private int translatedTexts = 0;
        private Consumer<Language> languageChangeConsumer;

        public Language getSourceLanguage() {
            return sourceLanguage;
        }

        public synchronized void setSourceLanguage(final OfflineTranslateUtils.Language lng) {
            sourceLanguage = lng;
            if (null != this.languageChangeConsumer) {
                this.languageChangeConsumer.accept(lng);
            }
        }

        public void setSourceLanguageChangeConsumer(final Consumer<Language> consumer) {
            this.languageChangeConsumer = consumer;
        }

        public boolean isInProgress() {
            return progressHandler != null && !progressHandler.isDisposed();
        }

        public void setProgressHandler(final OfflineTranslateUtils.TranslationProgressHandler progressHandler) {
            this.progressHandler = progressHandler;
        }

        public synchronized void startTranslation(final int textsToTranslate, @Nullable final AbstractActivity activity, @Nullable final MaterialButton button) {
            this.isTranslated = false;
            this.textsToTranslate = textsToTranslate;
            this.translatedTexts = 0;
            if (null != activity) {
                this.progressHandler = new OfflineTranslateUtils.TranslationProgressHandler(activity);
                this.progressHandler.showProgress(button);
            }
        }

        public synchronized void abortTranslation() {
            if (null != this.progressHandler) {
                this.progressHandler.sendEmptyMessage(DisposableHandler.DONE);
            }
            isTranslated = false;
        }

        public synchronized void updateProgress() {
            if (this.textsToTranslate == ++this.translatedTexts) {
                isTranslated = true;
                if (null != this.progressHandler) {
                    this.progressHandler.sendEmptyMessage(DisposableHandler.DONE);
                }
            }
        }

        public boolean isTranslated() {
            return isTranslated;
        }

        public void setNotTranslated() {
            this.isTranslated = false;
        }
    }
}
