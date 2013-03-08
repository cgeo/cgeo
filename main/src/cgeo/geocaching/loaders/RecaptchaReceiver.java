package cgeo.geocaching.loaders;

public interface RecaptchaReceiver {

    public String getText();

    public void setText(String text);

    public String getChallenge();

    public void setChallenge(String challenge);

    public void notifyNeed();

    public void waitForUser();

}
