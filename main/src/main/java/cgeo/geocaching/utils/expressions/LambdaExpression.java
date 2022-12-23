package cgeo.geocaching.utils.expressions;

import cgeo.geocaching.utils.functions.Func2;
import cgeo.geocaching.utils.functions.Func3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LambdaExpression<P, R> implements IExpression<LambdaExpression<P, R>> {


    private final String id;
    private final Func3<Map<String, List<String>>, P, List<R>, R> function;
    private final List<LambdaExpression<P, R>> children = new ArrayList<>();

    private ExpressionConfig config;

    public static <P, R> LambdaExpression<P, R> createGroup(final String id, final Func3<Map<String, List<String>>, P, List<R>, R> function) {
        return new LambdaExpression<>(id, function);
    }

    public static <P, R> LambdaExpression<P, R> createGroupSingleConfig(final String id, final Func3<String, P, List<R>, R> function) {
        return createGroup(id, (sa, p, lr) -> function.call(sa == null || sa.get(null) == null || sa.get(null).isEmpty() ? null : sa.get(null).get(0), p, lr));
    }

    public static <P, R> LambdaExpression<P, R> createValue(final String id, final Func2<Map<String, List<String>>, P, R> function) {
        return createGroup(id, (sa, p, lr) -> function.call(sa, p));
    }

    public static <P, R> LambdaExpression<P, R> createValueSingleConfig(final String id, final Func2<String, P, R> function) {
        return createGroup(id, (sa, p, lr) -> function.call(sa == null || sa.get(null) == null || sa.get(null).isEmpty() ? null : sa.get(null).get(0), p));
    }

    private LambdaExpression(final String id, final Func3<Map<String, List<String>>, P, List<R>, R> function) {
        this.id = id;
        this.function = function;
    }

    @Override
    public void setConfig(final ExpressionConfig config) {
        this.config = config;
    }

    @Override
    public ExpressionConfig getConfig() {
        return config;
    }

    @Override
    public void addChild(final LambdaExpression<P, R> child) {
        children.add(child);
    }

    @Override
    public List<LambdaExpression<P, R>> getChildren() {
        return children;
    }

    @Override
    public String getId() {
        return id;
    }

    public R call(final P param) {
        if (function != null) {
            final List<R> result = new ArrayList<>();
            for (LambdaExpression<P, R> child : getChildren()) {
                result.add(child.call(param));
            }
            return function.call(getConfig(), param, result);
        }
        return null;
    }
}
