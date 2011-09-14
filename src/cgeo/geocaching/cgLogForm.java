package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;

import java.util.Calendar;

public class cgLogForm extends AbstractActivity {
    public cgLogForm(String helpTopic) {
        super(helpTopic);
    }

    public void setDate(Calendar dateIn) {
        // to be overwritten
    }
}