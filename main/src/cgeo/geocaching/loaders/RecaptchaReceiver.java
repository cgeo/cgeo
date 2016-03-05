package cgeo.geocaching.loaders;

public interface RecaptchaReceiver {

    String getText();

    void setText(String text);

    String getChallenge();

    void fetchChallenge();

    void setKey(String key);

    void notifyNeed();

    void waitForUser();

}
