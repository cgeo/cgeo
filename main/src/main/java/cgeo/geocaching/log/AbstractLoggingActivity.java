package cgeo.geocaching.log;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.Smiley;
import cgeo.geocaching.connector.capability.SmileyCapability;
import cgeo.geocaching.connector.gc.GCSmileysProvider;
import cgeo.geocaching.connector.trackable.TravelBugConnector;
import cgeo.geocaching.log.LogTemplateProvider.LogContext;
import cgeo.geocaching.log.LogTemplateProvider.LogTemplate;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Trackable;

import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public abstract class AbstractLoggingActivity extends AbstractActionBarActivity {

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.abstract_logging_activity, menu);

        final SubMenu menuLog = menu.findItem(R.id.menu_templates).getSubMenu();
        for (final LogTemplate template : LogTemplateProvider.getTemplatesWithSignature()) {
            if (template.getResourceId() == 0) {
                menuLog.add(0, template.getItemId(), 0, getString(R.string.init_log_template_prefix) + template.getName());
            } else {
                menuLog.add(0, template.getItemId(), 0, template.getResourceId());
            }
        }

        final SubMenu menuSmileys = menu.findItem(R.id.menu_smileys).getSubMenu();
        for (final Smiley smiley : getSmileys()) {
            menuSmileys.add(Menu.NONE, smiley.getItemId(), Menu.NONE, smiley.emoji + "  [" + smiley.symbol + "]  " + getString(smiley.meaning));
        }
        menu.findItem(R.id.menu_sort_trackables_by).setVisible(false);

        return true;
    }

    @NonNull
    private List<Smiley> getSmileys() {
        final Geocache cache = getLogContext().getCache();
        final SmileyCapability connector = ConnectorFactory.getConnectorAs(cache, SmileyCapability.class);
        if (connector != null) {
            return connector.getSmileys();
        }
        final Trackable trackable = getLogContext().getTrackable();
        if (trackable != null && ConnectorFactory.getConnector(trackable).equals(TravelBugConnector.getInstance())) {
            return GCSmileysProvider.getSmileys();
        }
        return Collections.emptyList();
    }

    @Nullable
    private Smiley getSmiley(final int id) {
        final Geocache cache = getLogContext().getCache();
        final SmileyCapability connector = ConnectorFactory.getConnectorAs(cache, SmileyCapability.class);
        if (connector != null) {
            return connector.getSmiley(id);
        }
        final Trackable trackable = getLogContext().getTrackable();
        if (trackable != null && ConnectorFactory.getConnector(trackable).equals(TravelBugConnector.getInstance())) {
            return GCSmileysProvider.getSmiley(id);
        }
        return null;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        menu.findItem(R.id.menu_smileys).setVisible(!getSmileys().isEmpty());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.menu_repeat_last) {
            replaceLog(getLastLog());
            return true;
        }

        final LogTemplate template = LogTemplateProvider.getTemplate(id);
        if (template != null) {
            insertIntoLog(template.getValue(getLogContext()), true);
            return true;
        }

        final Smiley smiley = getSmiley(id);
        if (smiley != null) {
            insertIntoLog("[" + smiley.symbol + "]", true);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * @return the last log text used with this logging activity
     */
    protected abstract String getLastLog();

    protected abstract LogContext getLogContext();

    protected final void insertIntoLog(final String newText, final boolean moveCursor) {
        final EditText log = findViewById(R.id.log);
        ActivityMixin.insertAtPosition(log, newText, moveCursor);
    }

    private void replaceLog(final String newText) {
        final EditText log = findViewById(R.id.log);
        log.setText(StringUtils.EMPTY);
        insertIntoLog(newText, true);
    }

    protected void requestKeyboardForLogging() {
        Keyboard.show(this, findViewById(R.id.log));
    }
}
