package cgeo.geocaching.utils.expressions;

import java.util.Collections;
import java.util.List;

public interface IExpression<T extends IExpression> {

    String getId();

    void setConfig(String[] value);

    String[] getConfig();

    default void addChild(T child) {
        //do nothing
    }

    default List<T> getChildren() {
        return Collections.emptyList();
    }

}
