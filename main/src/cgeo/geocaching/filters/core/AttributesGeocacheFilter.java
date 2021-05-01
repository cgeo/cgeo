package cgeo.geocaching.filters.core;

import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AttributesGeocacheFilter extends BaseGeocacheFilter {


    private final Set<String> attributes = new HashSet<>();

    public void setAttributes(final Map<CacheAttribute, Boolean> atts) {
        this.attributes.clear();
        for (Map.Entry<CacheAttribute, Boolean> entry : atts.entrySet()) {
            if (entry.getValue() != null) {
                this.attributes.add(entry.getKey().getValue(entry.getValue()));
            }
        }

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
        return found == attributes.size();
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        if (attributes.isEmpty()) {
            sqlBuilder.addWhereAlwaysInclude();
        } else {
            sqlBuilder.openWhere(SqlBuilder.WhereType.AND);
            for (String att : attributes) {
                final String attTableId = sqlBuilder.getNewTableId();
                sqlBuilder.addWhere("EXISTS (SELECT geocode FROM cg_attributes " + attTableId + " WHERE " + attTableId + ".geocode = " + sqlBuilder.getMainTableId() + ".geocode AND attribute = ?)", att);
            }
            sqlBuilder.closeWhere();
        }

    }

    @Override
    public void setConfig(final String[] value) {
        attributes.clear();
        attributes.addAll(Arrays.asList(value));
    }

    @Override
    public String[] getConfig() {
        return attributes.toArray(new String[0]);
    }
}
