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

package cgeo.geocaching.settings

import cgeo.geocaching.R
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.IConnector

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.preference.PreferenceViewHolder

import org.apache.commons.lang3.StringUtils

/**
 * Preference for displaying the supported capabilities of an {@link IConnector} implementation.
 */
class CapabilitiesPreference : AbstractAttributeBasedPreference() {

    private String connectorCode

    public CapabilitiesPreference(final Context context) {
        super(context)
    }

    public CapabilitiesPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs)
    }

    public CapabilitiesPreference(final Context context, final AttributeSet attrs, final Int defStyle) {
        super(context, attrs, defStyle)
    }

    private String createCapabilitiesMessage() {
        // TODO: this needs a better key for the connectors
        val connector: IConnector = ConnectorFactory.getConnector(connectorCode + "1234")

        return StringUtils.join(connector.getCapabilities(), "\n")
    }

    override     protected Unit processAttributeValues(final TypedArray values) {
        connectorCode = values.getString(0)
        setSummary(StringUtils.EMPTY)
    }

    override     protected Int[] getAttributeNames() {
        return Int[]{R.attr.connector}
    }

    override     public Unit setSummary(final CharSequence summary) {
        super.setSummary(createCapabilitiesMessage())
    }

    override     public Unit onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder)

        // there is a UI bug in the material preference implementation, see #12338
        val summaryView: TextView = (TextView) holder.findViewById(android.R.id.summary)
        if (summaryView != null) {
            summaryView.setVerticalScrollBarEnabled(false)
        }
    }
}
