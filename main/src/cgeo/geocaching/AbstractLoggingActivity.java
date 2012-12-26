package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCSmiliesProvider;
import cgeo.geocaching.connector.gc.GCSmiliesProvider.Smiley;
import cgeo.geocaching.utils.LogTemplateProvider;
import cgeo.geocaching.utils.LogTemplateProvider.LogContext;
import cgeo.geocaching.utils.LogTemplateProvider.LogTemplate;

import org.apache.commons.lang3.StringUtils;

import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.EditText;

public abstract class AbstractLoggingActivity extends AbstractActivity {
    private static final int MENU_SIGNATURE = 1;
    private static final int MENU_SMILEY = 2;

    protected AbstractLoggingActivity(String helpTopic) {
        super(helpTopic);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // signature menu
        menu.add(0, MENU_SIGNATURE, 0, res.getString(R.string.init_signature)).setIcon(R.drawable.ic_menu_edit);

        // templates menu
        final SubMenu menuLog = menu.addSubMenu(0, 0, 0, res.getString(R.string.log_add)).setIcon(R.drawable.ic_menu_add);
        for (LogTemplate template : LogTemplateProvider.getTemplates()) {
            menuLog.add(0, template.getItemId(), 0, template.getResourceId());
        }
        menuLog.add(0, MENU_SIGNATURE, 0, res.getString(R.string.init_signature));

        // smilies
        final SubMenu menuSmilies = menu.addSubMenu(0, MENU_SMILEY, 0, res.getString(R.string.log_smilies)).setIcon(R.drawable.ic_menu_emoticons);
        for (Smiley smiley : GCSmiliesProvider.getSmilies()) {
            menuSmilies.add(0, smiley.getItemId(), 0, smiley.text);
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final boolean signatureAvailable = StringUtils.isNotBlank(Settings.getSignature());
        menu.findItem(MENU_SIGNATURE).setVisible(signatureAvailable);

        boolean smileyVisible = false;
        final cgCache cache = getLogContext().getCache();
        if (cache != null && ConnectorFactory.getConnector(cache).equals(GCConnector.getInstance())) {
            smileyVisible = true;
        }
        final cgTrackable trackable = getLogContext().getTrackable();
        if (trackable != null && ConnectorFactory.getConnector(trackable).equals(GCConnector.getInstance())) {
            smileyVisible = true;
        }

        menu.findItem(MENU_SMILEY).setVisible(smileyVisible);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();

        if (id == MENU_SIGNATURE) {
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
        insertAtPosition(log, newText, moveCursor);
    }
}
