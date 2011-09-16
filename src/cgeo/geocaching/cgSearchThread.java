package cgeo.geocaching;

import android.os.Handler;
import android.util.Log;

public class cgSearchThread extends Thread {
    private Handler recaptchaHandler = null;
    private String recaptchaChallenge = null;
    private String recaptchaText = null;

    public void setRecaptchaHandler(Handler recaptchaHandlerIn) {
        recaptchaHandler = recaptchaHandlerIn;
    }

    public void notifyNeed() {
        if (recaptchaHandler != null) {
            recaptchaHandler.sendEmptyMessage(1);
        }
    }

    public synchronized void waitForUser() {
        try {
            wait();
        } catch (InterruptedException e) {
            Log.w(cgSettings.tag, "searchThread is not waiting for user...");
        }
    }

    public void setChallenge(String challenge) {
        recaptchaChallenge = challenge;
    }

    public String getChallenge() {
        return recaptchaChallenge;
    }

    public synchronized void setText(String text) {
        recaptchaText = text;

        notify();
    }

    public synchronized String getText() {
        return recaptchaText;
    }
}
