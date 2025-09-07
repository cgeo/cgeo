package cgeo.geocaching.utils.offlinetranslate;


import java.util.Set;
import java.util.function.Consumer;

import io.reactivex.rxjava3.core.Scheduler;

public interface ITranslateAccessor {

    void setCallbackScheduler(Scheduler scheduler);

    String fromLanguageTag(String tag);

    Set<String> getSupportedLanguages();

    void getAvailableLanguages(Consumer<Set<String>> onSuccess, Consumer<Exception> onError);

    void downloadLanguage(String language, Runnable onSuccess, Consumer<Exception> onError);

    void deleteLanguage(String language, Runnable onSuccess, Consumer<Exception> onError);

    void guessLanguage(String source, Consumer<String> onSuccess, Consumer<Exception> onError);

    ITranslatorImpl getTranslator(String sourceLanguage, String targetLanguage);

    void getTranslatorWithDownload(String sourceLanguage, String targetLanguage, Consumer<ITranslatorImpl> onSuccess, Consumer<Exception> onError);


}
