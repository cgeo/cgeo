package cgeo.geocaching.utils.offlinetranslate;

import cgeo.geocaching.utils.AndroidRxUtils;

import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

import io.reactivex.rxjava3.core.Scheduler;

public class NoopTranslateAccessor implements ITranslateAccessor {

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
        return Collections.emptySet();
    }

    @Override
    public void getAvailableLanguages(final Consumer<Set<String>> onSuccess, final Consumer<Exception> onError) {
        runOnScheduler(() -> onSuccess.accept(Collections.emptySet()));
    }

    @Override
    public void downloadLanguage(final String language, final Runnable onSuccess, final Consumer<Exception> onError) {
        runOnScheduler(() -> onError.accept(null));
    }

    @Override
    public void deleteLanguage(final String language, final Runnable onSuccess, final Consumer<Exception> onError) {
        runOnScheduler(() -> onError.accept(null));
    }

    @Override
    public void guessLanguage(final String source, final Consumer<String> onSuccess, final Consumer<Exception> onError) {
        runOnScheduler(() -> onSuccess.accept(null));
    }

    @Override
    public ITranslatorImpl getTranslator(final String sourceLanguage, final String targetLanguage) {
        return new ITranslatorImpl() {
            @Override
            public void translate(final String source, final Consumer<String> onSuccess, final Consumer<Exception> onError) {
                runOnScheduler(() -> onSuccess.accept(source));
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
