package cgeo.geocaching.test.mock;

import cgeo.geocaching.utils.LazyInitializedList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class MockedLazyInitializedList<ElementType> extends LazyInitializedList<ElementType> {

    MockedLazyInitializedList(final ElementType[] elements) {
        clear();
        addAll(Arrays.asList(elements));
    }

    @Override
    public List<ElementType> call() {
        return new ArrayList<>();
    }

}
