package cgeo.geocaching.list;

import org.apache.commons.lang3.StringUtils;

/**
 * Memento to remember list name suggestions from search terms.
 */
public class ListNameMemento {
    public static final ListNameMemento EMPTY = new ListNameMemento();
    private String newListName = StringUtils.EMPTY;

    public String rememberTerm(final String term) {
        newListName = term;
        return term;
    }

    public String getTerm() {
        return newListName;
    }

}
