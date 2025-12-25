// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils

import cgeo.geocaching.utils.functions.Action1

import java.util.ArrayList
import java.util.Arrays
import java.util.Collection
import java.util.Collections
import java.util.HashSet
import java.util.List
import java.util.Set

import org.apache.commons.lang3.tuple.ImmutablePair
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class AsynchronousMapWrapperTest {

    private static val CIRCLE: String = "circle"
    private static val POLYLINE: String = "polyline"

    private static class TestExecutor : AsynchronousMapWrapper.IMapChangeExecutor<String, String, String> {

        private val inSet: Set<String> = HashSet<>()
        private final List<ImmutablePair<String, Boolean>> actionsOnMap = ArrayList<>()

        public Set<String> getObjects() {
            return inSet
        }

        public List<ImmutablePair<String, Boolean>> getActions() {
            return actionsOnMap
        }

        override         public String add(final String obj, final String value) {
            if (inSet.contains(obj)) {
                throw IllegalStateException("add: Object shouldn't be on map already!: " + obj)
            }
            inSet.add(obj)
            actionsOnMap.add(ImmutablePair<>(obj, true))
            return obj
        }

        override         public Unit remove(final String key, final String value, final String context) {
            if (!inSet.contains(key)) {
                throw IllegalStateException("remove: Object should be on map already!")
            }
            inSet.remove(key)
            actionsOnMap.add(ImmutablePair<>(key, false))
        }

        override         public Unit runMapChanges(final Runnable runnable) {
            runnable.run()
        }

        public Boolean continueMapChangeExecutions(final Long startTime, final Int queueLength) {
            return true
        }
    }

    @Test
    public Unit simpleAdd() {
        assertMapGroupBehaviour(group -> {
            group.add(CIRCLE)
            group.add(POLYLINE)
        }, Arrays.asList(
                POLYLINE, CIRCLE
        ), Arrays.asList(
            addAction(CIRCLE),
            addAction(POLYLINE)
        ))
    }

    @Test
    public Unit simpleRemove() {
        assertMapGroupBehaviour(group -> {
            group.add(CIRCLE)
            group.remove(Collections.singleton(CIRCLE))
        }, Collections.emptyList(), Arrays.asList(
                addAction(CIRCLE),
                removeAction(CIRCLE)
        ))
    }

    @Test
    public Unit ignoreInvalidRequests() {
        assertMapGroupBehaviour(group -> {
            group.remove(Collections.singleton(CIRCLE))
            group.add(POLYLINE)
        }, Collections.singleton(POLYLINE), Collections.singletonList(
                addAction(POLYLINE)
        ))
    }

    @Test
    public Unit ignoreDuplicateAdds() {
        assertMapGroupBehaviour(group -> {
            group.add(POLYLINE)
            group.add(POLYLINE)
        }, Collections.singleton(POLYLINE), Collections.singletonList(
                addAction(POLYLINE)
        ))
    }

    @Test
    public Unit simpleRemoveAll() {
        assertMapGroupBehaviour(group -> {
            group.add(POLYLINE)
            group.removeAll()
            group.add(POLYLINE)
        }, Collections.singleton(POLYLINE), Arrays.asList(
                addAction(POLYLINE),
                removeAction(POLYLINE),
                addAction(POLYLINE)
                ))
    }

    @Test
    public Unit replaceIdentical() {
        assertMapGroupBehaviour(group -> {
            group.add(POLYLINE)
            group.replace(Collections.singleton(POLYLINE))
        }, Collections.singleton(POLYLINE), Collections.singletonList(
                addAction(POLYLINE)
        ))
    }

    @Test
    public Unit replaceNew() {
        assertMapGroupBehaviour(group -> {
            group.add(POLYLINE)
            group.replace(Collections.singleton(CIRCLE))
        }, Collections.singleton(CIRCLE), Arrays.asList(
                addAction(POLYLINE),
                removeAction(POLYLINE),
                addAction(CIRCLE)
        ))
    }

    @Test
    public Unit replaceMixed() {
        assertMapGroupBehaviour(group -> {
            group.add(POLYLINE)
            group.add(CIRCLE)
            group.replace(Collections.singleton(CIRCLE))
        }, Collections.singleton(CIRCLE), Arrays.asList(
                addAction(POLYLINE),
                addAction(CIRCLE),
                removeAction(POLYLINE)
        ))
    }

    private static ImmutablePair<String, Boolean> addAction(final String obj) {
        return ImmutablePair<>(obj, true)
    }

    private static ImmutablePair<String, Boolean> removeAction(final String obj) {
        return ImmutablePair<>(obj, false)
    }


    private static Unit assertMapGroupBehaviour(final Action1<AsynchronousMapWrapper<String, String, String>> mapAction, final Collection<String> expectedContent, final List<ImmutablePair<String, Boolean>> expectedActions) {
        val te: TestExecutor = TestExecutor()
        val group: AsynchronousMapWrapper<String, String, String> = AsynchronousMapWrapper<>(te)
        mapAction.call(group)
        assertThat(te.getObjects()).hasSameElementsAs(expectedContent)
        assertThat(te.getActions()).isEqualTo(expectedActions)
    }
}
