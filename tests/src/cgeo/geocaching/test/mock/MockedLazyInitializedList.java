package cgeo.geocaching.test.mock;

import cgeo.geocaching.utils.LazyInitializedList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class MockedLazyInitializedList<ElementType> extends LazyInitializedList<ElementType> {

    public MockedLazyInitializedList(ElementType[] elements) {
        set(Arrays.asList(elements));
    }

    @Override
    protected List<ElementType> loadFromDatabase() {
        return new ArrayList<ElementType>();
    }

}
