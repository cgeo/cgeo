package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;

import java.util.Calendar;

abstract public class cgLogForm extends AbstractActivity {
    public cgLogForm(String helpTopic) {
        super(helpTopic);
    }

    abstract public void setDate(Calendar dateIn);
}