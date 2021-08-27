package cgeo.geocaching.utils.formulas;

import java.text.ParseException;

public class FormulaParseException extends ParseException {

    public FormulaParseException(final FormulaTokenizer tokenizer, final String message) {
        super(getMessageFrom(tokenizer, message), tokenizer.getParseIndex());
    }

    private static String getMessageFrom(final FormulaTokenizer tokenizer, final String message) {
        String markedFormula = tokenizer.getFormula();
        final int parseIndex = tokenizer.getParseIndex();
        if (parseIndex >= markedFormula.length()) {
            markedFormula += "[]";
        } else {
            markedFormula = markedFormula.substring(0, parseIndex) + "[" + markedFormula.charAt(parseIndex) + "]" + markedFormula.substring(parseIndex + 1);
        }
        return "Couldn't parse '" + markedFormula + "' (pos marked with []: " + parseIndex + "): " + message + " (last parsed: " + tokenizer.getCurrentToken() + ")";

    }

}
