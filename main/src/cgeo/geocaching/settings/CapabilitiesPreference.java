package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

import org.apache.commons.lang3.StringUtils;

/**
 * Preference for displaying the supported capabilities of an {@link IConnector} implementation.
 */
public class CapabilitiesPreference extends AbstractAttributeBasedPreference {

    private String connectorCode;

    public CapabilitiesPreference(final Context context) {
        super(context);
    }

    public CapabilitiesPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public CapabilitiesPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    private String createCapabilitiesMessage() {
        // TODO: this needs a better key for the connectors
        final IConnector connector = ConnectorFactory.getConnector(connectorCode + "1234");

        return StringUtils.join(connector.getCapabilities(), "\n");
    }

    @Override
    protected void processAttributeValues(final TypedArray values) {
        connectorCode = values.getString(0);
        setSummary(StringUtils.EMPTY);
    }

    @Override
    protected int[] getAttributeNames() {
        return new int[]{R.attr.connector};
    }

    @Override
    public void setSummary(final CharSequence summary) {
        super.setSummary(createCapabilitiesMessage());
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        // there is a UI bug in the material preference implementation, see #12338
        final TextView summaryView = (TextView) holder.findViewById(android.R.id.summary);
        if (summaryView != null) {
            summaryView.setVerticalScrollBarEnabled(false);
        }
    }
}
