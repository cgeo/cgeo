package cgeo.geocaching.test.mock;

import cgeo.geocaching.utils.LazyInitializedList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class MockedLazyInitializedList<ElementType> extends LazyInitializedList<ElementType> {

    public MockedLazyInitializedList(ElementType[] elements) {
        final List<ElementType> elements1 = Arrays.asList(elements);
        clear();
        if (elements1 != null) {
            addAll(elements1);
        }
    }

    @Override
    protected List<ElementType> loadFromDatabase() {
        return new ArrayList<ElementType>();
    }

}
