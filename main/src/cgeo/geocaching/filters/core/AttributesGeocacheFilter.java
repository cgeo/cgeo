package cgeo.geocaching.filters.core;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.LocalizationUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.BooleanUtils;

public class AttributesGeocacheFilter extends BaseGeocacheFilter {


    private final Map<String, String> attributes = new HashMap<>();
    private final Set<String> attributesRaw = new HashSet<>();
    private boolean inverse = false;

    public void setAttributes(final Map<CacheAttribute, Boolean> atts) {
        this.attributes.clear();
        this.attributesRaw.clear();
        for (Map.Entry<CacheAttribute, Boolean> entry : atts.entrySet()) {
            if (entry.getValue() != null) {
                this.attributes.put(entry.getKey().getValue(entry.getValue()), entry.getValue() ? entry.getKey().rawName : null);
                if (entry.getValue()) {
                    attributesRaw.add(entry.getKey().rawName);
                }
            }
        }
    }

    public void setInverse(final boolean inverse) {
        this.inverse = inverse;
    }

    public boolean isInverse() {
        return inverse;
    }

    public Map<CacheAttribute, Boolean> getAttributes() {
        final Map<CacheAttribute, Boolean> result = new HashMap<>();
        for (String attString : attributes.keySet()) {
            final CacheAttribute ca = CacheAttribute.getByRawName(CacheAttribute.trimAttributeName(attString));
            if (ca != null) {
                result.put(ca, CacheAttribute.isEnabled(attString));
            }
        }
        return result;
    }

    @Override
    public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null;
        }
        if (attributes.isEmpty()) {
            return true;
        }

        //check if cache attributes are not filled -> means that this filter is inconclusive
        if (cache.getAttributes().isEmpty() && !cache.isDetailed()) {
            return null;
        }

        int found = 0;
        for (String cacheAtt : cache.getAttributes()) {
            if (attributes.containsKey(cacheAtt) || attributesRaw.contains(cacheAtt)) {
                found++;
            }
        }
        return inverse != (found == attributes.size());
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        if (attributes.isEmpty()) {
            sqlBuilder.addWhereAlwaysInclude();
        } else {
            if (inverse) {
                sqlBuilder.openWhere(SqlBuilder.WhereType.NOT);
            }
            sqlBuilder.openWhere(SqlBuilder.WhereType.AND);
            for (Map.Entry<String, String> att : attributes.entrySet()) {
                final String attTableId = sqlBuilder.getNewTableId();
                final String whereStart = "EXISTS (SELECT geocode FROM cg_attributes " + attTableId + " WHERE " + attTableId + ".geocode = " + sqlBuilder.getMainTableId() + ".geocode AND attribute ";
                if (att.getValue() == null) {
                    sqlBuilder.addWhere(whereStart + " = ?)", att.getKey());
                } else {
                    sqlBuilder.addWhere(whereStart + " IN (?, ?))", att.getKey(), att.getValue());
                }
            }
            sqlBuilder.closeWhere();
            if (inverse) {
                sqlBuilder.closeWhere();
            }
        }

    }

    @Override
    public void setConfig(final String[] value) {
        attributes.clear();
        attributesRaw.clear();
        if (value.length == 0) {
            inverse = false;
        } else {
            inverse = BooleanUtils.toBoolean(value[0]);
            for (int idx = 1; idx < value.length; idx++) {
                final CacheAttribute ca = CacheAttribute.getByName(value[idx]);
                if (ca != null) {
                    final boolean isYesValue = CacheAttribute.isEnabled(value[idx]);
                    attributes.put(value[idx], isYesValue ? ca.rawName : null);
                    if (isYesValue) {
                        attributesRaw.add(ca.rawName);
                    }
                }
            }
        }
    }

    @Override
    public String[] getConfig() {
        final String[] result = new String[attributes.size() + 1];
        result[0] = Boolean.toString(inverse);
        int idx = 1;
        for (String att : attributes.keySet()) {
            result[idx++] = att;
        }
        return result;
    }

    @Override
    protected String getUserDisplayableConfig() {
        if (attributes.isEmpty()) {
            return LocalizationUtils.getString(R.string.cache_filter_userdisplay_none);
        }
        if (attributes.size() > 1) {
            return LocalizationUtils.getPlural(R.plurals.cache_filter_userdisplay_multi_item, attributes.size());
        }

        return attributes.keySet().iterator().next();
    }
}
