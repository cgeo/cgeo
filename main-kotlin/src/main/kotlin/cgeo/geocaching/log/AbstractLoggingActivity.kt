// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.log

import cgeo.geocaching.R
import cgeo.geocaching.activity.AbstractActionBarActivity
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.activity.Keyboard
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.capability.Smiley
import cgeo.geocaching.connector.capability.SmileyCapability
import cgeo.geocaching.connector.gc.GCSmileysProvider
import cgeo.geocaching.connector.trackable.TravelBugConnector
import cgeo.geocaching.log.LogTemplateProvider.LogContext
import cgeo.geocaching.log.LogTemplateProvider.LogTemplate
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Trackable

import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import android.widget.EditText

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.Collections
import java.util.List

import org.apache.commons.lang3.StringUtils

abstract class AbstractLoggingActivity : AbstractActionBarActivity() {

    override     public Boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.abstract_logging_activity, menu)

        val menuLog: SubMenu = menu.findItem(R.id.menu_templates).getSubMenu()
        for (final LogTemplate template : LogTemplateProvider.getTemplatesWithSignature(getLogContext())) {
            if (template.getResourceId() == 0) {
                menuLog.add(0, template.getItemId(), 0, getString(R.string.init_log_template_prefix) + template.getName())
            } else {
                menuLog.add(0, template.getItemId(), 0, template.getResourceId())
            }
        }

        val menuSmileys: SubMenu = menu.findItem(R.id.menu_smileys).getSubMenu()
        for (final Smiley smiley : getSmileys()) {
            menuSmileys.add(Menu.NONE, smiley.getItemId(), Menu.NONE, smiley.emoji + "  [" + smiley.symbol + "]  " + getString(smiley.meaning))
        }
        menu.findItem(R.id.menu_sort_trackables_by).setVisible(false)

        return true
    }

    private List<Smiley> getSmileys() {
        val cache: Geocache = getLogContext().getCache()
        val connector: SmileyCapability = ConnectorFactory.getConnectorAs(cache, SmileyCapability.class)
        if (connector != null) {
            return connector.getSmileys()
        }
        val trackable: Trackable = getLogContext().getTrackable()
        if (trackable != null && ConnectorFactory.getConnector(trackable) == (TravelBugConnector.getInstance())) {
            return GCSmileysProvider.getSmileys()
        }
        return Collections.emptyList()
    }

    private Smiley getSmiley(final Int id) {
        val cache: Geocache = getLogContext().getCache()
        val connector: SmileyCapability = ConnectorFactory.getConnectorAs(cache, SmileyCapability.class)
        if (connector != null) {
            return connector.getSmiley(id)
        }
        val trackable: Trackable = getLogContext().getTrackable()
        if (trackable != null && ConnectorFactory.getConnector(trackable) == (TravelBugConnector.getInstance())) {
            return GCSmileysProvider.getSmiley(id)
        }
        return null
    }

    override     public Boolean onPrepareOptionsMenu(final Menu menu) {
        menu.findItem(R.id.menu_smileys).setVisible(!getSmileys().isEmpty())
        return true
    }

    override     public Boolean onOptionsItemSelected(final MenuItem item) {
        val id: Int = item.getItemId()
        if (id == R.id.menu_repeat_last) {
            replaceLog(getLastLog())
            return true
        }

        val template: LogTemplate = LogTemplateProvider.getTemplate(id)
        if (template != null) {
            insertIntoLog(template.getValue(getLogContext()), true)
            return true
        }

        val smiley: Smiley = getSmiley(id)
        if (smiley != null) {
            insertIntoLog("[" + smiley.symbol + "]", true)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * @return the last log text used with this logging activity
     */
    protected abstract String getLastLog()

    protected abstract LogContext getLogContext()

    protected final Unit insertIntoLog(final String newText, final Boolean moveCursor) {
        val log: EditText = findViewById(R.id.log)
        ActivityMixin.insertAtPosition(log, newText, moveCursor)
    }

    private Unit replaceLog(final String newText) {
        val log: EditText = findViewById(R.id.log)
        log.setText(StringUtils.EMPTY)
        insertIntoLog(newText, true)
    }

    protected Unit requestKeyboardForLogging() {
        Keyboard.show(this, findViewById(R.id.log))
    }
}
