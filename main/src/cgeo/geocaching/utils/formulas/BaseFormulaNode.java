package cgeo.geocaching.utils.formulas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Convenience implementation of IFormula. Implementors are encouraged to extend this class */
public class BaseFormulaNode implements IFormulaNode {

    private NodeType type;
    private String text;
    private Object value;
    private final List<IFormulaNode> children = new ArrayList<>();
    private final List<IFormulaNode> childrenReadOnly = Collections.unmodifiableList(children);

    private Double valueAsDouble;
    private Integer valueAsInt;

    public void init(final IFormulaNode.NodeType type, final String text, final Object value) {
        this.type = type;
        this.text = text;
        this.value = value;
    }

    public NodeType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public Object getValue() {
        return value;
    }

    public String getValueAsString() {
        return value == null ? "" : value.toString();
    }

    public double getValueAsDouble() {
        if (valueAsDouble == null) {
            if (value instanceof Number) {
                valueAsDouble = ((Number) value).doubleValue();
            } else {
                valueAsDouble = FormulaParser.textToDouble(getValueAsString());
            }
        }
        return valueAsDouble;
    }

    public int getValueAsInt() {
        if (valueAsInt == null) {
            if (value instanceof Number) {
                valueAsInt = ((Number) value).intValue();
            } else {
                valueAsInt = FormulaParser.textToInt(getValueAsString());
            }
        }
        return valueAsInt;
    }

    public List<IFormulaNode> getChildren() {
        return childrenReadOnly;
    }

    public void addChild(final IFormulaNode child) {
        children.add(child);
    }

    public IFormulaNode removeChild(final int idx) {
        if (idx < 0 || idx >= children.size()) {
            return null;
        }
        return children.remove(idx);
    }

}
