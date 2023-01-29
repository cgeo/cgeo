package cgeo.geocaching.maps.google.v2;

import cgeo.geocaching.utils.functions.Action1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GoogleMapObjectGroupTest {

    private static final MapObjectOptions CIRCLE_1_2 = MapObjectOptions.circle(new CircleOptions().center(new LatLng(1, 2)));
    private static final MapObjectOptions POLYLINE_1_2_3_4 = MapObjectOptions.polyline(new PolylineOptions().add(new LatLng(1, 2), new LatLng(3, 4)));

    private static class TestExecutor implements GoogleMapObjectsQueue.ICommandExecutor {

        private final Set<MapObjectOptions> onMap = new HashSet<>();
        private final List<ImmutablePair<MapObjectOptions, Boolean>> actionsOnMap = new ArrayList<>();

        public Set<MapObjectOptions> getMapObjects() {
            return onMap;
        }

        public List<ImmutablePair<MapObjectOptions, Boolean>> getActions() {
            return actionsOnMap;
        }

        @Override
        public Object addToMap(final GoogleMap map, final MapObjectOptions obj) {
            if (onMap.contains(obj)) {
                throw new IllegalStateException("add: Object shouldn't be on map already!");
            }
            onMap.add(obj);
            actionsOnMap.add(new ImmutablePair<>(obj, true));
            return obj;
        }

        @Override
        public void removeFromMap(final Object obj) {
            final MapObjectOptions mapObject = (MapObjectOptions) obj;
            if (!onMap.contains(mapObject)) {
                throw new IllegalStateException("remove: Object should be on map already!");
            }
            onMap.remove(mapObject);
            actionsOnMap.add(new ImmutablePair<>(mapObject, false));
        }

        @Override
        public void runOnUIThread(final Runnable runnable) {
            runnable.run();
        }

        public boolean continueCommandExecution(final long startTime, final int queueLength) {
            return true;
        }
    }

    @Test
    public void simpleAdd() {
        assertMapGroupBehaviour(group -> {
            group.add(CIRCLE_1_2);
            group.add(POLYLINE_1_2_3_4);
        }, Arrays.asList(
            POLYLINE_1_2_3_4, CIRCLE_1_2
        ), Arrays.asList(
            addAction(CIRCLE_1_2),
            addAction(POLYLINE_1_2_3_4)
        ));
    }

    @Test
    public void simpleRemove() {
        assertMapGroupBehaviour(group -> {
            group.add(CIRCLE_1_2);
            group.remove(Collections.singleton(CIRCLE_1_2));
        }, Collections.emptyList(), Arrays.asList(
                addAction(CIRCLE_1_2),
                removeAction(CIRCLE_1_2)
        ));
    }

    @Test
    public void ignoreInvalidRequests() {
        assertMapGroupBehaviour(group -> {
            group.remove(Collections.singleton(CIRCLE_1_2));
            group.add(POLYLINE_1_2_3_4);
        }, Collections.singleton(POLYLINE_1_2_3_4), Collections.singletonList(
                addAction(POLYLINE_1_2_3_4)
        ));
    }

    @Test
    public void ignoreDuplicateAdds() {
        assertMapGroupBehaviour(group -> {
            group.add(POLYLINE_1_2_3_4);
            group.add(POLYLINE_1_2_3_4);
        }, Collections.singleton(POLYLINE_1_2_3_4), Collections.singletonList(
                addAction(POLYLINE_1_2_3_4)
        ));
    }

    @Test
    public void simpleRemoveAll() {
        assertMapGroupBehaviour(group -> {
            group.add(POLYLINE_1_2_3_4);
            group.removeAll();
            group.add(POLYLINE_1_2_3_4);
        }, Collections.singleton(POLYLINE_1_2_3_4), Arrays.asList(
                addAction(POLYLINE_1_2_3_4),
                removeAction(POLYLINE_1_2_3_4),
                addAction(POLYLINE_1_2_3_4)
                ));
    }

    @Test
    public void replaceIdentical() {
        assertMapGroupBehaviour(group -> {
            group.add(POLYLINE_1_2_3_4);
            group.replace(Collections.singleton(POLYLINE_1_2_3_4));
        }, Collections.singleton(POLYLINE_1_2_3_4), Collections.singletonList(
                addAction(POLYLINE_1_2_3_4)
        ));
    }

    @Test
    public void replaceNew() {
        assertMapGroupBehaviour(group -> {
            group.add(POLYLINE_1_2_3_4);
            group.replace(Collections.singleton(CIRCLE_1_2));
        }, Collections.singleton(CIRCLE_1_2), Arrays.asList(
                addAction(POLYLINE_1_2_3_4),
                removeAction(POLYLINE_1_2_3_4),
                addAction(CIRCLE_1_2)
        ));
    }

    @Test
    public void replaceMixed() {
        assertMapGroupBehaviour(group -> {
            group.add(POLYLINE_1_2_3_4);
            group.add(CIRCLE_1_2);
            group.replace(Collections.singleton(CIRCLE_1_2));
        }, Collections.singleton(CIRCLE_1_2), Arrays.asList(
                addAction(POLYLINE_1_2_3_4),
                addAction(CIRCLE_1_2),
                removeAction(POLYLINE_1_2_3_4)
        ));
    }

    private static ImmutablePair<MapObjectOptions, Boolean> addAction(final MapObjectOptions obj) {
        return new ImmutablePair<>(obj, true);
    }

    private static ImmutablePair<MapObjectOptions, Boolean> removeAction(final MapObjectOptions obj) {
        return new ImmutablePair<>(obj, false);
    }


    private static void assertMapGroupBehaviour(final Action1<GoogleMapObjectsQueue> mapAction, final Collection<MapObjectOptions> expectedContent, final List<ImmutablePair<MapObjectOptions, Boolean>> expectedActions) {
        final TestExecutor te = new TestExecutor();
        final GoogleMapObjectsQueue group = new GoogleMapObjectsQueue(null, te);
        mapAction.call(group);
        assertThat(te.getMapObjects()).hasSameElementsAs(expectedContent);
        assertThat(te.getActions()).isEqualTo(expectedActions);
    }
}
