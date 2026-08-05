package cgeo.geocaching.utils.offlinetranslate;

import java.util.function.Consumer;

public interface ITranslatorImpl {

    String getSourceLanguage();

    String getTargetLanguage();

    void translate(String source, Consumer<String> onSuccess, Consumer<Exception> onError);

    void dispose();
}
