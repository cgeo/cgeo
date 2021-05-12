package cgeo.geocaching.filters.core;

import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.BooleanUtils;

public class AttributesGeocacheFilter extends BaseGeocacheFilter {


    private final Set<String> attributes = new HashSet<>();
    private boolean inverse = false;

    public void setAttributes(final Map<CacheAttribute, Boolean> atts) {
        this.attributes.clear();
        for (Map.Entry<CacheAttribute, Boolean> entry : atts.entrySet()) {
            if (entry.getValue() != null) {
                this.attributes.add(entry.getKey().getValue(entry.getValue()));
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
        for (String attString : attributes) {
            final CacheAttribute ca = CacheAttribute.getByRawName(CacheAttribute.trimAttributeName(attString));
            if (ca != null) {
                result.put(ca, CacheAttribute.isEnabled(attString));
            }
        }
        return result;
    }

    @Override
    public Boolean filter(final Geocache cache) {
        if (cache == null || cache.getAttributes() == null) {
            return null;
        }
        int found = 0;
        for (String cacheAtt : cache.getAttributes()) {
            if (attributes.contains(cacheAtt)) {
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
            for (String att : attributes) {
                final String attTableId = sqlBuilder.getNewTableId();
                sqlBuilder.addWhere("EXISTS (SELECT geocode FROM cg_attributes " + attTableId + " WHERE " + attTableId + ".geocode = " + sqlBuilder.getMainTableId() + ".geocode AND attribute = ?)", att);
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
        if (value.length == 0) {
            inverse = false;
        } else {
            inverse = BooleanUtils.toBoolean(value[0]);
            for (int idx = 1; idx < value.length; idx++) {
                attributes.add(value[idx]);
            }
        }
    }

    @Override
    public String[] getConfig() {
        final String[] result = new String[attributes.size() + 1];
        result[0] = Boolean.toString(inverse);
        int idx = 1;
        for (String att : attributes) {
            result[idx++] = att;
        }
        return result;
    }
}
