package cgeo.geocaching.connector.gc;

import cgeo.geocaching.utils.Log;

import android.os.Handler;
import android.os.Message;

abstract public class AbstractSearchThread extends Thread {
    private Handler recaptchaHandler = null;
    private String recaptchaChallenge = null;
    private String recaptchaText = null;
    private final Handler handler;
    private static AbstractSearchThread currentInstance;

    public AbstractSearchThread(final Handler handler) {
        this.handler = handler;
    }

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
            Log.w("searchThread is not waiting for user…");
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

    @Override
    final public void run() {
        super.run();
        currentInstance = this;
        runSearch();
        handler.sendMessage(Message.obtain());
    }

    protected abstract void runSearch();

    public static AbstractSearchThread getCurrentInstance() {
        return currentInstance;
    }
}
