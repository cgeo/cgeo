package cgeo.geocaching.utils.formulas;


import androidx.core.util.Supplier;

import java.util.HashMap;
import java.util.Map;

public class FormulaParser {

    private final Map<String, FunctionData<? extends IFormulaNode>> functions = new HashMap<>();

    public static class FunctionData<T extends IFormulaNode> {
        private final Supplier<T> creator;
        private final String id;
        private final int priority;
        private final boolean canBeChained;

        public FunctionData(final String id, final int priority, final boolean canBeChained, final Supplier<T> creator) {
            this.creator = creator;
            this.id = id;
            this.priority = priority;
            this.canBeChained = canBeChained;
        }
    }

    public void register(final FunctionData<? extends IFormulaNode> function) {
        functions.put(function.id, function);
    }

    public IFormulaNode parse(final String formula) throws FormulaParseException {
        return new Parser(new FormulaTokenizer(formula)).parseFormula();
    }

    public static String textToString(final String text) {
        if (text == null) {
            return "";
        }
        return text;
    }

    public static double textToDouble(final String text) {
        if (text == null) {
            return 0d;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException nfe) {
            return 0d;
        }
    }

    public static int textToInt(final String text) {
        if (text == null) {
            return 0;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException nfe) {
            return (int) Math.round(textToDouble(text));
        }
    }



    private class Parser {


        private final FormulaTokenizer tokenizer;

        Parser(final FormulaTokenizer tokenizer) {
            this.tokenizer = tokenizer;
        }


        public IFormulaNode parseFormula() throws FormulaParseException {
            this.tokenizer.parseNextToken();
            final IFormulaNode result = parseComplexFunction();
            if (this.tokenizer.getCurrentToken().token != FormulaTokenizer.Token.END) {
                tokenizer.throwParseException("Expected END of formula");
            }
            return result;
        }

        private IFormulaNode parseExpression() throws FormulaParseException {
            final FormulaTokenizer.TokenData token = tokenizer.getCurrentToken();
            switch (token.token) {
                case NUMERIC:
                    tokenizer.parseNextToken();
                    return createValueNode(IFormulaNode.NodeType.NUMERIC, token.text);
                case TEXT:
                    tokenizer.parseNextToken();
                    return createValueNode(IFormulaNode.NodeType.TEXT, token.text);
                case ID:
                case SYMBOL:
                    if (functions.containsKey(token.text)) {
                        return parsePlainOrUnaryFunction();
                    }
                    return createValueNode(IFormulaNode.NodeType.TEXT, token.text);
                case PAREN_OPEN:
                    tokenizer.parseNextToken();
                    final IFormulaNode result = parseComplexFunction();
                    if (tokenizer.getCurrentToken().token != FormulaTokenizer.Token.PAREN_CLOSE) {
                        tokenizer.throwParseException("Expected )");
                    }
                    tokenizer.parseNextToken();
                    return result;
                default:
                    tokenizer.throwParseException("Unexpected token: " + token);
            }
            return null;
        }

        private IFormulaNode createValueNode(final IFormulaNode.NodeType type, final String text) {
           final IFormulaNode node = new BaseFormulaNode();
           final Object value;
           switch (type) {
               case NUMERIC:
                   value = textToDouble(text);
                   break;
               case TEXT:
                   value = textToString(text);
                   break;
               default:
                   value = null;
                   break;
           }
           node.init(type, text, value);
           //TODO: value creation for int missing
           return node;
        }

        private IFormulaNode parseComplexFunction() throws FormulaParseException {
            final IFormulaNode node = parseExpression();
            FunctionData<?> functionData = functions.get(tokenizer.getCurrentToken().text);
            if (functionData == null) {
                if (tokenizer.getCurrentToken().token == FormulaTokenizer.Token.ID || tokenizer.getCurrentToken().token == FormulaTokenizer.Token.SYMBOL) {
                    tokenizer.throwParseException("Unknown function: " + tokenizer.getCurrentToken());
                }
                return node;
            }
            final IFormulaNode root = new BaseFormulaNode();
            root.addChild(node);
            final IFormulaNode[] parents = new IFormulaNode[5];
            int lastPrio = functionData.priority;
            parents[lastPrio] = root;
            String lastId = null;

            while (functionData != null) {
                if (functionData.priority <= 0) {
                    tokenizer.throwParseException("Expect Binary function here");
                }
                final IFormulaNode opNode = functionData.creator.get();
                opNode.init(IFormulaNode.NodeType.FUNCTION, functionData.id, null);
                tokenizer.parseNextToken();
                final IFormulaNode nextNode = parseExpression();

                if (functionData.priority == lastPrio) {
                    if (functionData.canBeChained && functionData.id.equals(lastId)) {
                        parents[lastPrio].getChildren().get(parents[lastPrio].getChildren().size() - 1).addChild(nextNode);
                    } else {
                        insert(parents[lastPrio], opNode, nextNode);
                    }
                } else if (functionData.priority > lastPrio) {
                    final IFormulaNode prioRoot = parents[lastPrio].getChildren().get(parents[lastPrio].getChildren().size() - 1);
                    insert(prioRoot, opNode, nextNode);
                    parents[functionData.priority] = prioRoot;
                } else {
                    int idx = functionData.priority;
                    while (parents[idx] == null) {
                        idx++;
                    }
                    insert(parents[idx], opNode, nextNode);
                    parents[functionData.priority] = parents[idx];
                    for (int i = idx + 1 ; i < parents.length; i++) {
                        parents[i] = null;
                    }
                }
                lastPrio = functionData.priority;
                lastId = functionData.id;
                functionData = functions.get(tokenizer.getCurrentToken().text);
            }
            return root.getChildren().get(0);
        }

        private void insert(final IFormulaNode root, final IFormulaNode opNode, final IFormulaNode newNode) {
            final IFormulaNode lastChild = root.removeChild(root.getChildren().size() - 1);
            root.addChild(opNode);
            opNode.addChild(lastChild);
            opNode.addChild(newNode);
        }

        private IFormulaNode parsePlainOrUnaryFunction() throws FormulaParseException {
            if (tokenizer.getCurrentToken().token != FormulaTokenizer.Token.ID && tokenizer.getCurrentToken().token != FormulaTokenizer.Token.SYMBOL) {
                tokenizer.throwParseException("MUST BE ID OR SYYMBOL");
            }
            final FunctionData<?> functionData = functions.get(tokenizer.getCurrentToken().text);
            if (functionData == null) {
                tokenizer.throwParseException("Unknown function: " + tokenizer.getCurrentToken().text);
                return null;
            }
            if (functionData.priority > 0) {
                tokenizer.throwParseException("Binary function can't be used in simple context: " + tokenizer.getCurrentToken().text);
            }

            final IFormulaNode node = functionData.creator.get();
            node.init(IFormulaNode.NodeType.FUNCTION, functionData.id, null);
            final FormulaTokenizer.TokenData nextToken = tokenizer.parseNextToken();

            if (functionData.priority == 0) {
                //unary function
                node.addChild(parseComplexFunction());
                return node;
            }

            //plain function parameters
            if (nextToken.token == FormulaTokenizer.Token.PAREN_OPEN) {
                parsePlainFunctionParameters(node);
            }
            return node;
        }

        private void parsePlainFunctionParameters(final IFormulaNode node) throws FormulaParseException {
            tokenizer.parseNextToken();
            while (tokenizer.getCurrentToken().token != FormulaTokenizer.Token.END) {
                final IFormulaNode child = parseExpression();
                node.addChild(child);
                final FormulaTokenizer.TokenData currToken = tokenizer.getCurrentToken();
                if (currToken.token == FormulaTokenizer.Token.PAREN_CLOSE) {
                    break;
                } else if (currToken.token == FormulaTokenizer.Token.PAREN_SEPARATOR) {
                    tokenizer.parseNextToken();
                } else {
                    tokenizer.throwParseException("Expceted ; or ), got " + currToken);
                }

            }
            if (tokenizer.getCurrentToken().token != FormulaTokenizer.Token.PAREN_CLOSE) {
                tokenizer.throwParseException("Unclosed function parameter list");
            }
            tokenizer.parseNextToken();
        }
    }
}

