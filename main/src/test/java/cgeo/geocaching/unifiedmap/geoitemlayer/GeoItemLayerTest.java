package cgeo.geocaching.unifiedmap.geoitemlayer;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.geoitem.GeoGroup;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.ToScreenProjector;
import cgeo.geocaching.test.AssertUtils;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.fail;

public class GeoItemLayerTest {

    private static final Geopoint GP_1 = new Geopoint(48, 11);
    private static final Geopoint GP_2 = new Geopoint(45, 10);
    private static final Geopoint GP_3 = new Geopoint(41, 12);

    private static final GeoPrimitive CIRCLE_1 = GeoPrimitive.createCircle(GP_1, 40, null);
    private static final GeoPrimitive CIRCLE_2 = GeoPrimitive.createCircle(GP_2, 40, null);
    private static final GeoPrimitive POLYGON_1 = GeoPrimitive.createPolygon(Arrays.asList(GP_1, GP_2, GP_3), null);
    private static final GeoPrimitive POLYLINE_1 = GeoPrimitive.createPolyline(Arrays.asList(GP_1, GP_2, GP_3), null);

    private static final GeoGroup GROUP_1 = GeoGroup.create(CIRCLE_1, POLYGON_1);

    public static class Action {

        private final ImmutableTriple<ActionType, GeoPrimitive, GeoPrimitive> data;

        private Action(final ActionType t, final GeoPrimitive gp, final GeoPrimitive gp2) {
            this.data = new ImmutableTriple<>(t, gp, gp2);
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof Action)) {
                return false;
            }
            return data.equals(((Action) o).data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @NonNull
        @Override
        public String toString() {
            return data.toString();
        }

        public static Action add(final GeoPrimitive gp) {
            return new Action(ActionType.ADD, gp, null);
        }

        public static Action remove(final GeoPrimitive gp) {
            return new Action(ActionType.REMOVE, gp, null);
        }

        public static Action replace(final GeoPrimitive gpOld, final GeoPrimitive gpNew) {
            return new Action(ActionType.REPLACE, gpOld, gpNew);
        }

        public static List<Action> anyorder(final Action ... actions) {
            return Arrays.asList(actions);
        }

        public enum ActionType { ADD, REPLACE, REMOVE }
    }

    public static class TestGeoItemLayer extends GeoItemLayer<String> {

        private TestMapProvider providerLayer;

        public TestGeoItemLayer(final String id) {
            super(id);
        }

        @Override
        public synchronized void setProvider(final IProviderGeoItemLayer<?> newProviderLayer, final int zLevel) {
            super.setProvider(newProviderLayer, zLevel);
            this.providerLayer = (TestMapProvider) newProviderLayer;
        }

        public TestMapProvider getProvider() {
            return providerLayer;
        }

        public void clearActions() {
            providerLayer.actions.clear();
        }
    }

    private static class TestMapProvider implements IProviderGeoItemLayer<Integer> {

        private static int idProvider = 0;

        private boolean initCalled = false;
        private boolean destroyCalled = false;


        private final List<Action> actions = new ArrayList<>();
        private final Map<Integer, GeoPrimitive> itemMap = new HashMap<>();

        @Override
        public void init(final int zLevel) {
            if (initCalled || destroyCalled) {
                fail("init called out of lifecycle!");
            }
            initCalled = true;
        }

        @Override
        public void destroy() {
            if (!initCalled || destroyCalled) {
                fail("destroy called out of lifecycle!");
            }
            destroyCalled = true;
        }

        @Override
        public ToScreenProjector getScreenCoordCalculator() {
            return null;
        }

        @Override
        public Integer add(final GeoPrimitive gp) {
            final int newId = idProvider++;
            actions.add(Action.add(gp));
            itemMap.put(newId, gp);
            return newId;
        }

        @Override
        public void remove(final GeoPrimitive gp, final Integer id) {
            final GeoPrimitive gpContained = itemMap.get(id);
            if (gpContained == null) {
                fail("Trying to remove non-contained item " + id + " (contained: " + itemMap + ")");
            }
            if (!gpContained.equals(gp)) {
                fail("Trying to remove non-identical item " + id + ": " + gp + " <-> " + gpContained);
            }
            itemMap.remove(id);
            actions.add(Action.remove(gp));
        }

        @Override
        public Integer replace(final GeoPrimitive oldItem, final Integer oldId, final GeoPrimitive newItem) {
            final int newId = idProvider++;
            final GeoPrimitive gpContained = itemMap.get(oldId);
            if (gpContained == null) {
                fail("Trying to replace non-contained item " + oldId + ": " + oldItem);
            }
            if (!gpContained.equals(oldItem)) {
                fail("Trying to replace non-identical item " + oldId + ": " + oldItem + " <-> " + gpContained);
            }
            itemMap.remove(oldId);
            actions.add(Action.replace(oldItem, newItem));
            itemMap.put(newId, newItem);
            return newId;
        }

        @Override
        public void runCommandChain(final Runnable runnable) {
            runnable.run();
        }
        @Override
        public void runMapChanges(final Runnable runnable) {
            runnable.run();
        }
        @Override
        public boolean continueMapChangeExecutions(final long startTime, final int queueLength) {
            return true;
        }
        public List<Action> getActions() {
            return actions;
        }

        public Map<Integer, GeoPrimitive> getItems() {
            return itemMap;
        }

    }


    private static TestGeoItemLayer createLayer() {
        final TestGeoItemLayer layer = new TestGeoItemLayer("layer_id");
        layer.setProvider(new TestMapProvider(), 5);
        return layer;
    }


    @Test
    public void simpleAdd() {
        final TestGeoItemLayer layer = createLayer();
        layer.put("circle", CIRCLE_1);
        assertThat(layer.keySet()).hasSize(1);
        assertThat(layer.get("circle")).isEqualTo(CIRCLE_1);

        assertActions(layer, Action.add(CIRCLE_1));
    }

    @Test
    public void simpleAddReplaceRemove() {
        final TestGeoItemLayer layer = createLayer();
        layer.put("item", CIRCLE_1);
        layer.put("item", POLYGON_1);
        layer.remove("item");
        assertThat(layer.keySet()).isEmpty();
        assertActions(layer, Action.add(CIRCLE_1), Action.replace(CIRCLE_1, POLYGON_1), Action.remove(POLYGON_1));
    }

    @Test
    public void hideShow() {
        final TestGeoItemLayer layer = createLayer();
        layer.put("circle", CIRCLE_1);
        layer.put("polygon", POLYGON_1);
        assertThat(layer.isShown("circle")).isTrue();
        assertThat(layer.isShown("circle")).isTrue();

        layer.hideAll();
        assertThat(layer.keySet()).containsExactlyInAnyOrder("circle", "polygon");
        assertThat(layer.isShown("circle")).isFalse();
        assertThat(layer.isShown("polygon")).isFalse();
        assertThat(layer.get("circle")).isEqualTo(CIRCLE_1);
        assertActions(layer, Action.add(CIRCLE_1), Action.add(POLYGON_1), Action.anyorder(Action.remove(CIRCLE_1), Action.remove(POLYGON_1)));
        layer.clearActions();

        layer.show("circle");
        assertThat(layer.keySet()).containsExactlyInAnyOrder("circle", "polygon");
        assertThat(layer.isShown("circle")).isTrue();
        assertThat(layer.isShown("polygon")).isFalse();
        assertActions(layer, Action.add(CIRCLE_1));
        layer.clearActions();

        layer.showAll();
        assertThat(layer.keySet()).containsExactlyInAnyOrder("circle", "polygon");
        assertThat(layer.isShown("circle")).isTrue();
        assertThat(layer.isShown("polygon")).isTrue();
        assertActions(layer, Action.add(POLYGON_1));
        layer.clearActions();

        layer.hide("circle");
        assertThat(layer.keySet()).containsExactlyInAnyOrder("circle", "polygon");
        assertThat(layer.isShown("circle")).isFalse();
        assertThat(layer.isShown("polygon")).isTrue();
        assertActions(layer, Action.remove(CIRCLE_1));
        layer.clearActions();
    }

    @Test
    public void groupReplaceDifferentSizes() {
        final TestGeoItemLayer layer = createLayer();
        layer.put("group", GeoGroup.create(CIRCLE_1, POLYGON_1));
        assertActions(layer, Action.anyorder(Action.add(CIRCLE_1), Action.add(POLYGON_1)));
        layer.clearActions();

        layer.put("group", GeoGroup.create(POLYGON_1));
        assertActions(layer, Action.remove(CIRCLE_1));
        layer.clearActions();

        layer.put("group", GeoGroup.create(POLYLINE_1, CIRCLE_2, POLYGON_1));
        assertActions(layer, Action.anyorder(Action.add(CIRCLE_2), Action.add(POLYLINE_1)));
        layer.clearActions();
    }

    @Test
    public void groupAndPrimitivesWithSameElements() {
        final TestGeoItemLayer layer = createLayer();
        layer.put("group", GeoGroup.create(CIRCLE_1));
        assertContent(layer, CIRCLE_1);
        assertActions(layer, Action.add(CIRCLE_1));
        layer.clearActions();

        layer.put("item", CIRCLE_1);
        assertContent(layer, CIRCLE_1, CIRCLE_1);
        assertActions(layer, Action.add(CIRCLE_1));
        layer.clearActions();

        //Switch same key from Group to primitive requires remove/add (should not happen too often in practice though)
        layer.put("group", CIRCLE_1);
        assertContent(layer, CIRCLE_1, CIRCLE_1);
        assertActions(layer, Action.anyorder(Action.remove(CIRCLE_1), Action.add(CIRCLE_1)));
        layer.clearActions();

        layer.put("group", POLYGON_1);
        assertContent(layer, CIRCLE_1, POLYGON_1);
        assertActions(layer, Action.replace(CIRCLE_1, POLYGON_1));
        layer.clearActions();

        //Switch same key from primitive to group requires remove/add (should not happen too often in practice though)
        layer.put("group", GeoGroup.create(POLYGON_1));
        assertContent(layer, CIRCLE_1, POLYGON_1);
        assertActions(layer, Action.anyorder(Action.remove(POLYGON_1), Action.add(POLYGON_1)));
        layer.clearActions();

        layer.remove("item");
        assertContent(layer, POLYGON_1);
        assertActions(layer, Action.remove(CIRCLE_1));
        layer.clearActions();

    }

    @Test
    public void groupWithSameElements() {
        final TestGeoItemLayer layer = createLayer();
        layer.put("group", GROUP_1);
        layer.put("circle", CIRCLE_1);
        layer.put("polygon", POLYGON_1);
        assertThat(layer.keySet()).containsExactlyInAnyOrder("group", "circle", "polygon");
        assertActions(layer, Action.anyorder(Action.add(CIRCLE_1), Action.add(POLYGON_1)), Action.add(CIRCLE_1), Action.add(POLYGON_1));
        layer.clearActions();
        layer.remove("group");
        assertThat(layer.keySet()).containsExactlyInAnyOrder("circle", "polygon");
        assertActions(layer, Action.anyorder(Action.remove(POLYGON_1), Action.remove(CIRCLE_1)));
    }

    @Test
    public void addIdenticalGeoPrimitive() {
        final TestGeoItemLayer layer = createLayer();
        layer.put("circle", CIRCLE_1);
        layer.put("group", GROUP_1);
        assertThat(layer.keySet()).containsExactly("circle", "group");

        assertActions(layer, Action.add(CIRCLE_1), Action.anyorder(Action.add(CIRCLE_1), Action.add(POLYGON_1)));
        layer.clearActions();

        layer.remove("circle");
        assertActions(layer, Action.remove(CIRCLE_1));
    }

    public static void assertActions(final TestGeoItemLayer layer, final Object ... actions) {
        final TestMapProvider tmp = layer.getProvider();
        final List<Action> actionsToAssert = tmp.getActions();
        AssertUtils.assertGroupedContentList("layer", actionsToAssert, actions);
    }

    public static void assertContent(final TestGeoItemLayer layer, final GeoPrimitive ... content) {
        final TestMapProvider tmp = layer.getProvider();
        assertThat(tmp.getItems().values()).containsExactlyInAnyOrder(content);
    }


}
