package cgeo.geocaching.utils.formulas;

import java.util.List;

public interface IFormulaNode {

    enum NodeType { TEXT, NUMERIC, FUNCTION }

    void init(NodeType type, String text, Object value);

    NodeType getType();

    String getText();

    Object getValue();

    List<IFormulaNode> getChildren();

    void addChild(IFormulaNode child);

    IFormulaNode removeChild(int index);

}
