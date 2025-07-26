package cgeo.geocaching.utils.offlinetranslate;

import cgeo.geocaching.utils.AndroidRxUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.reactivex.rxjava3.core.Scheduler;

/** ITranslateAccessor for testing purposes. Simulates language guessing (always it) and download */
public class DevTranslateAccessor implements ITranslateAccessor {

    private final Set<String> supportedLanguages = new HashSet<>(Arrays.asList("en", "de", "fr", "es", "it"));
    private final Set<String> availableLanguages = new HashSet<>(Arrays.asList("en", "de"));

    private Scheduler scheduler;

    @Override
    public void setCallbackScheduler(final Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public String fromLanguageTag(final String tag) {
        return tag;
    }

    @Override
    public Set<String> getSupportedLanguages() {
        return supportedLanguages;
    }

    @Override
    public void getAvailableLanguages(final Consumer<Set<String>> onSuccess, final Consumer<Exception> onError) {
        runOnScheduler(() -> onSuccess.accept(availableLanguages));
    }

    @Override
    public void downloadLanguage(final String language, final Runnable onSuccess, final Consumer<Exception> onError) {
        AndroidRxUtils.computationScheduler.scheduleDirect(() -> {
            availableLanguages.add(language);
            runOnScheduler(onSuccess);
        }, 5, TimeUnit.SECONDS);
    }

    @Override
    public void deleteLanguage(final String language, final Runnable onSuccess, final Consumer<Exception> onError) {
        availableLanguages.remove(language);
        runOnScheduler(onSuccess);
    }

    @Override
    public void guessLanguage(final String source, final Consumer<String> onSuccess, final Consumer<Exception> onError) {
        AndroidRxUtils.computationScheduler.scheduleDirect(() -> {
            runOnScheduler(() -> onSuccess.accept("it"));
        }, 2, TimeUnit.SECONDS);
    }

    @Override
    public ITranslatorImpl getTranslator(final String sourceLanguage, final String targetLanguage) {
        return new ITranslatorImpl() {

            @Override
            public String getSourceLanguage() {
                return sourceLanguage;
            }

            @Override
            public String getTargetLanguage() {
                return targetLanguage;
            }

            @Override
            public void translate(final String source, final Consumer<String> onSuccess, final Consumer<Exception> onError) {
                AndroidRxUtils.computationScheduler.scheduleDirect(() -> {
                    runOnScheduler(() -> onSuccess.accept("[" + sourceLanguage + "->" + targetLanguage + "]" + source));
                }, 2, TimeUnit.SECONDS);
            }

            @Override
            public void dispose() {
                //do nothing
            }
        };
    }

    @Override
    public void getTranslatorWithDownload(final String sourceLanguage, final String targetLanguage, final Consumer<ITranslatorImpl> onSuccess, final Consumer<Exception> onError) {
        runOnScheduler(() -> onSuccess.accept(getTranslator(sourceLanguage, targetLanguage)));
    }


    private void runOnScheduler(final Runnable run) {
        final Scheduler schedulerToUse = scheduler != null ? scheduler : AndroidRxUtils.mainThreadScheduler;
        schedulerToUse.createWorker().schedule(run);
    }
}
