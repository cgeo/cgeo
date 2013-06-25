package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCSmiliesProvider;
import cgeo.geocaching.connector.gc.GCSmiliesProvider.Smiley;
import cgeo.geocaching.connector.trackable.TravelBugConnector;
import cgeo.geocaching.utils.LogTemplateProvider;
import cgeo.geocaching.utils.LogTemplateProvider.LogContext;
import cgeo.geocaching.utils.LogTemplateProvider.LogTemplate;

import org.apache.commons.lang3.StringUtils;

import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.EditText;

public abstract class AbstractLoggingActivity extends AbstractActivity {

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.abstract_logging_activity, menu);

        final SubMenu menuLog = menu.findItem(R.id.menu_templates).getSubMenu();
        for (final LogTemplate template : LogTemplateProvider.getTemplates()) {
            menuLog.add(0, template.getItemId(), 0, template.getResourceId());
        }

        final SubMenu menuSmilies = menu.findItem(R.id.menu_smilies).getSubMenu();
        for (final Smiley smiley : GCSmiliesProvider.getSmilies()) {
            menuSmilies.add(0, smiley.getItemId(), 0, smiley.text);
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final boolean signatureAvailable = StringUtils.isNotBlank(Settings.getSignature());
        menu.findItem(R.id.menu_signature).setVisible(signatureAvailable);

        boolean smileyVisible = false;
        final Geocache cache = getLogContext().getCache();
        if (cache != null && ConnectorFactory.getConnector(cache).equals(GCConnector.getInstance())) {
            smileyVisible = true;
        }
        final Trackable trackable = getLogContext().getTrackable();
        if (trackable != null && ConnectorFactory.getConnector(trackable).equals(TravelBugConnector.getInstance())) {
            smileyVisible = true;
        }

        menu.findItem(R.id.menu_smilies).setVisible(smileyVisible);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();

        if (id == R.id.menu_signature) {
            insertIntoLog(LogTemplateProvider.applyTemplates(Settings.getSignature(), getLogContext()), true);
            return true;
        }

        final LogTemplate template = LogTemplateProvider.getTemplate(id);
        if (template != null) {
            insertIntoLog(template.getValue(getLogContext()), true);
            return true;
        }

        final Smiley smiley = GCSmiliesProvider.getSmiley(id);
        if (smiley != null) {
            insertIntoLog("[" + smiley.text + "]", true);
            return true;
        }

        return false;
    }

    protected abstract LogContext getLogContext();

    protected void insertIntoLog(String newText, final boolean moveCursor) {
        final EditText log = (EditText) findViewById(R.id.log);
        ActivityMixin.insertAtPosition(log, newText, moveCursor);
    }
}
