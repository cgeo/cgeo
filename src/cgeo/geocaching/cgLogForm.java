package cgeo.geocaching;

import java.util.Calendar;

import cgeo.geocaching.activity.AbstractActivity;

public class cgLogForm extends AbstractActivity {
	public cgLogForm(String helpTopic) {
		super(helpTopic);
	}

	public void setDate(Calendar dateIn) {
		// to be overwritten
	}
}