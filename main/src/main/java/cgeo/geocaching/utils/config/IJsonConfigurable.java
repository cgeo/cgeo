package cgeo.geocaching.utils.config;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface IJsonConfigurable<T extends IJsonConfigurable<T>> {

    String getId();

    @Deprecated
    default void setConfig(LegacyFilterConfig config) {
        //empty on purpose
    }

    @Deprecated
    default LegacyFilterConfig getConfig() {
        return null;
    }

    default void addChild(T child) {
        //do nothing
    }

    default List<T> getChildren() {
        return Collections.emptyList();
    }

    @Nullable
    ObjectNode getJsonConfig();

    void setJsonConfig(@NonNull ObjectNode node);

}
