package cgeo.geocaching.utils.offlinetranslate;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.utils.AndroidRxUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.common.sdkinternal.MlKitContext;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import io.reactivex.rxjava3.core.Scheduler;

public class MLKitTranslateAccessor implements ITranslateAccessor {

    private Scheduler scheduler;

    public MLKitTranslateAccessor() {
        //ensure that MlKit is initialized
        MlKitContext.initializeIfNeeded(CgeoApplication.getInstance());
    }

    @Override
    public void setCallbackScheduler(final Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public String fromLanguageTag(final String tag) {
        return TranslateLanguage.fromLanguageTag(tag);
    }


    @Override
    public Set<String> getSupportedLanguages() {
        return new HashSet<>(TranslateLanguage.getAllLanguages());
    }

    @Override
    public void getAvailableLanguages(final Consumer<Set<String>> onSuccess, final Consumer<Exception> onError) {
        execute(RemoteModelManager.getInstance().getDownloadedModels(TranslateRemoteModel.class), onError, result -> {
            final Set<String> availableLanguagesNew = new HashSet<>();
            for (TranslateRemoteModel model : result) {
                availableLanguagesNew.add(model.getLanguage());
            }
            onSuccess.accept(availableLanguagesNew);
        });
    }

    @Override
    public void downloadLanguage(final String language, final Runnable onSuccess, final Consumer<Exception> onError) {
        execute(RemoteModelManager.getInstance().download(new TranslateRemoteModel.Builder(language).build(), new DownloadConditions.Builder().build()), onError, result -> {
            onSuccess.run();
        });
    }

    @Override
    public void deleteLanguage(final String language, final Runnable onSuccess, final Consumer<Exception> onError) {
        execute(RemoteModelManager.getInstance().deleteDownloadedModel(new TranslateRemoteModel.Builder(language).build()), onError, result -> {
            onSuccess.run();
        });
    }

    @Override
    public void guessLanguage(final String source, final Consumer<String> onSuccess, final Consumer<Exception> onError) {
        final String text = source.replaceAll("[\\s\\ufffc]+", " ").trim();
        execute(LanguageIdentification.getClient().identifyLanguage(text), onError, lngCode -> {
                onSuccess.accept("und".equals(lngCode) ? null : lngCode);
        });
    }

    @Override
    public ITranslatorImpl getTranslator(final String sourceLanguage, final String targetLanguage) {
        return wrap(sourceLanguage, targetLanguage, createTranslator(sourceLanguage, targetLanguage));
    }

    @Override
    public void getTranslatorWithDownload(final String sourceLanguage, final String targetLanguage, final Consumer<ITranslatorImpl> onSuccess, final Consumer<Exception> onError) {
        final Translator t = createTranslator(sourceLanguage, targetLanguage);
        execute(t.downloadModelIfNeeded(new DownloadConditions.Builder().build()), onError,
            x -> onSuccess.accept(wrap(sourceLanguage, targetLanguage, t)));
    }


    private Translator createTranslator(final String sourceLanguage, final String targetLanguage) {
        final TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(targetLanguage)
                .build();

        return Translation.getClient(options);
    }

    private ITranslatorImpl wrap(final String srcLng, final String trgLng, final Translator translator) {
        return new ITranslatorImpl() {

            @Override
            public String getSourceLanguage() {
                return srcLng;
            }

            @Override
            public String getTargetLanguage() {
                return trgLng;
            }

            @Override
            public void translate(final String source, final Consumer<String> onSuccess, final Consumer<Exception> onError) {
                execute(translator.translate(source), onError, onSuccess);
            }

            @Override
            public void dispose() {
                translator.close();
            }
        };
    }

    private Executor getExecutor() {
        final Scheduler schedulerToUse = scheduler != null ? scheduler : AndroidRxUtils.mainThreadScheduler;
        return runnable -> schedulerToUse.createWorker().schedule(runnable);
    }

    private  <T> void execute(final Task<T> task, final Consumer<Exception> onError, final Consumer<T> onSuccess) {
        task.addOnCompleteListener(getExecutor(), t -> {
            if (t.isSuccessful()) {
                onSuccess.accept(t.getResult());
            } else {
                onError.accept(t.getException());
            }
        });
    }

}
