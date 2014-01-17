package cgeo.geocaching.loaders;

public interface RecaptchaReceiver {

    public String getText();

    public void setText(String text);

    public String getChallenge();

    public void fetchChallenge();

    public void setKey(String key);

    public void notifyNeed();

    public void waitForUser();

}
