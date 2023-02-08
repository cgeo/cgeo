package cgeo.geocaching.utils;

import cgeo.geocaching.utils.functions.Action1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class AsynchronousMapWrapperTest {

    private static final String CIRCLE = "circle";
    private static final String POLYLINE = "polyline";

    private static class TestExecutor implements AsynchronousMapWrapper.IMapChangeExecutor<String, String, String> {

        private final Set<String> inSet = new HashSet<>();
        private final List<ImmutablePair<String, Boolean>> actionsOnMap = new ArrayList<>();

        public Set<String> getObjects() {
            return inSet;
        }

        public List<ImmutablePair<String, Boolean>> getActions() {
            return actionsOnMap;
        }

        @Override
        public String add(final String obj, final String value) {
            if (inSet.contains(obj)) {
                throw new IllegalStateException("add: Object shouldn't be on map already!: " + obj);
            }
            inSet.add(obj);
            actionsOnMap.add(new ImmutablePair<>(obj, true));
            return obj;
        }

        @Override
        public void remove(final String key, final String value, final String context) {
            if (!inSet.contains(key)) {
                throw new IllegalStateException("remove: Object should be on map already!");
            }
            inSet.remove(key);
            actionsOnMap.add(new ImmutablePair<>(key, false));
        }

        @Override
        public void runMapChanges(final Runnable runnable) {
            runnable.run();
        }

        public boolean continueMapChangeExecutions(final long startTime, final int queueLength) {
            return true;
        }
    }

    @Test
    public void simpleAdd() {
        assertMapGroupBehaviour(group -> {
            group.add(CIRCLE);
            group.add(POLYLINE);
        }, Arrays.asList(
                POLYLINE, CIRCLE
        ), Arrays.asList(
            addAction(CIRCLE),
            addAction(POLYLINE)
        ));
    }

    @Test
    public void simpleRemove() {
        assertMapGroupBehaviour(group -> {
            group.add(CIRCLE);
            group.remove(Collections.singleton(CIRCLE));
        }, Collections.emptyList(), Arrays.asList(
                addAction(CIRCLE),
                removeAction(CIRCLE)
        ));
    }

    @Test
    public void ignoreInvalidRequests() {
        assertMapGroupBehaviour(group -> {
            group.remove(Collections.singleton(CIRCLE));
            group.add(POLYLINE);
        }, Collections.singleton(POLYLINE), Collections.singletonList(
                addAction(POLYLINE)
        ));
    }

    @Test
    public void ignoreDuplicateAdds() {
        assertMapGroupBehaviour(group -> {
            group.add(POLYLINE);
            group.add(POLYLINE);
        }, Collections.singleton(POLYLINE), Collections.singletonList(
                addAction(POLYLINE)
        ));
    }

    @Test
    public void simpleRemoveAll() {
        assertMapGroupBehaviour(group -> {
            group.add(POLYLINE);
            group.removeAll();
            group.add(POLYLINE);
        }, Collections.singleton(POLYLINE), Arrays.asList(
                addAction(POLYLINE),
                removeAction(POLYLINE),
                addAction(POLYLINE)
                ));
    }

    @Test
    public void replaceIdentical() {
        assertMapGroupBehaviour(group -> {
            group.add(POLYLINE);
            group.replace(Collections.singleton(POLYLINE));
        }, Collections.singleton(POLYLINE), Collections.singletonList(
                addAction(POLYLINE)
        ));
    }

    @Test
    public void replaceNew() {
        assertMapGroupBehaviour(group -> {
            group.add(POLYLINE);
            group.replace(Collections.singleton(CIRCLE));
        }, Collections.singleton(CIRCLE), Arrays.asList(
                addAction(POLYLINE),
                removeAction(POLYLINE),
                addAction(CIRCLE)
        ));
    }

    @Test
    public void replaceMixed() {
        assertMapGroupBehaviour(group -> {
            group.add(POLYLINE);
            group.add(CIRCLE);
            group.replace(Collections.singleton(CIRCLE));
        }, Collections.singleton(CIRCLE), Arrays.asList(
                addAction(POLYLINE),
                addAction(CIRCLE),
                removeAction(POLYLINE)
        ));
    }

    private static ImmutablePair<String, Boolean> addAction(final String obj) {
        return new ImmutablePair<>(obj, true);
    }

    private static ImmutablePair<String, Boolean> removeAction(final String obj) {
        return new ImmutablePair<>(obj, false);
    }


    private static void assertMapGroupBehaviour(final Action1<AsynchronousMapWrapper<String, String, String>> mapAction, final Collection<String> expectedContent, final List<ImmutablePair<String, Boolean>> expectedActions) {
        final TestExecutor te = new TestExecutor();
        final AsynchronousMapWrapper<String, String, String> group = new AsynchronousMapWrapper<>(te);
        mapAction.call(group);
        assertThat(te.getObjects()).hasSameElementsAs(expectedContent);
        assertThat(te.getActions()).isEqualTo(expectedActions);
    }
}
