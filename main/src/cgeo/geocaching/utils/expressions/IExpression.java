package cgeo.geocaching.utils.expressions;

import java.util.Collections;
import java.util.List;

public interface IExpression<T extends IExpression<T>> {

    String getId();

    void setConfig(ExpressionConfig config);

    ExpressionConfig getConfig();

    default void addChild(T child) {
        //do nothing
    }

    default List<T> getChildren() {
        return Collections.emptyList();
    }

}
