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

package cgeo.geocaching.unifiedmap.geoitemlayer

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.models.geoitem.GeoGroup
import cgeo.geocaching.models.geoitem.GeoPrimitive
import cgeo.geocaching.models.geoitem.ToScreenProjector
import cgeo.geocaching.test.AssertUtils

import android.util.Pair

import androidx.annotation.NonNull

import java.util.ArrayList
import java.util.Arrays
import java.util.Collection
import java.util.HashMap
import java.util.List
import java.util.Map

import org.apache.commons.lang3.tuple.ImmutableTriple
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat
import org.assertj.core.api.Java6Assertions.fail

class GeoItemLayerTest {

    private static val GP_1: Geopoint = Geopoint(48, 11)
    private static val GP_2: Geopoint = Geopoint(45, 10)
    private static val GP_3: Geopoint = Geopoint(41, 12)

    private static val CIRCLE_1: GeoPrimitive = GeoPrimitive.createCircle(GP_1, 40, null)
    private static val CIRCLE_2: GeoPrimitive = GeoPrimitive.createCircle(GP_2, 40, null)
    private static val POLYGON_1: GeoPrimitive = GeoPrimitive.createPolygon(Arrays.asList(GP_1, GP_2, GP_3), null)
    private static val POLYLINE_1: GeoPrimitive = GeoPrimitive.createPolyline(Arrays.asList(GP_1, GP_2, GP_3), null)

    private static val GROUP_1: GeoGroup = GeoGroup.create(CIRCLE_1, POLYGON_1)

    public static class Action {

        private final ImmutableTriple<ActionType, GeoPrimitive, GeoPrimitive> data

        private Action(final ActionType t, final GeoPrimitive gp, final GeoPrimitive gp2) {
            this.data = ImmutableTriple<>(t, gp, gp2)
        }

        override         public Boolean equals(final Object o) {
            if (!(o is Action)) {
                return false
            }
            return data == (((Action) o).data)
        }

        override         public Int hashCode() {
            return data.hashCode()
        }

        override         public String toString() {
            return data.toString()
        }

        public static Action add(final GeoPrimitive gp) {
            return Action(ActionType.ADD, gp, null)
        }

        public static Action remove(final GeoPrimitive gp) {
            return Action(ActionType.REMOVE, gp, null)
        }

        public static Action replace(final GeoPrimitive gpOld, final GeoPrimitive gpNew) {
            return Action(ActionType.REPLACE, gpOld, gpNew)
        }

        public static List<Action> anyorder(final Action ... actions) {
            return Arrays.asList(actions)
        }

        enum class class ActionType { ADD, REPLACE, REMOVE }
    }

    public static class TestGeoItemLayer : GeoItemLayer()<String> {

        private TestMapProvider providerLayer

        public TestGeoItemLayer(final String id) {
            super(id)
        }

        override         public synchronized Unit setProvider(final IProviderGeoItemLayer<?> newProviderLayer, final Int zLevel) {
            super.setProvider(newProviderLayer, zLevel)
            this.providerLayer = (TestMapProvider) newProviderLayer
        }

        public TestMapProvider getProvider() {
            return providerLayer
        }

        public Unit clearActions() {
            providerLayer.actions.clear()
        }
    }

    private static class TestMapProvider : IProviderGeoItemLayer<Integer> {

        private static Int idProvider = 0

        private var initCalled: Boolean = false
        private var destroyCalled: Boolean = false


        private val actions: List<Action> = ArrayList<>()
        private val itemMap: Map<Integer, GeoPrimitive> = HashMap<>()

        override         public Unit init(final Int zLevel) {
            if (initCalled || destroyCalled) {
                fail("init called out of lifecycle!")
            }
            initCalled = true
        }

        override         public Unit destroy(final Collection<Pair<GeoPrimitive, Integer>> values) {
            if (!initCalled || destroyCalled) {
                fail("destroy called out of lifecycle!")
            }
            destroyCalled = true
        }

        override         public ToScreenProjector getScreenCoordCalculator() {
            return null
        }

        override         public Integer add(final GeoPrimitive gp) {
            val newId: Int = idProvider++
            actions.add(Action.add(gp))
            itemMap.put(newId, gp)
            return newId
        }

        override         public Unit remove(final GeoPrimitive gp, final Integer id) {
            val gpContained: GeoPrimitive = itemMap.get(id)
            if (gpContained == null) {
                fail("Trying to remove non-contained item " + id + " (contained: " + itemMap + ")")
            }
            if (!gpContained == (gp)) {
                fail("Trying to remove non-identical item " + id + ": " + gp + " <-> " + gpContained)
            }
            itemMap.remove(id)
            actions.add(Action.remove(gp))
        }

        override         public Integer replace(final GeoPrimitive oldItem, final Integer oldId, final GeoPrimitive newItem) {
            val newId: Int = idProvider++
            val gpContained: GeoPrimitive = itemMap.get(oldId)
            if (gpContained == null) {
                fail("Trying to replace non-contained item " + oldId + ": " + oldItem)
            }
            if (!gpContained == (oldItem)) {
                fail("Trying to replace non-identical item " + oldId + ": " + oldItem + " <-> " + gpContained)
            }
            itemMap.remove(oldId)
            actions.add(Action.replace(oldItem, newItem))
            itemMap.put(newId, newItem)
            return newId
        }

        override         public Unit runCommandChain(final Runnable runnable) {
            runnable.run()
        }
        override         public Unit runMapChanges(final Runnable runnable) {
            runnable.run()
        }
        override         public Boolean continueMapChangeExecutions(final Long startTime, final Int queueLength) {
            return true
        }
        public List<Action> getActions() {
            return actions
        }

        public Map<Integer, GeoPrimitive> getItems() {
            return itemMap
        }

    }


    private static TestGeoItemLayer createLayer() {
        val layer: TestGeoItemLayer = TestGeoItemLayer("layer_id")
        layer.setProvider(TestMapProvider(), 5)
        return layer
    }


    @Test
    public Unit simpleAdd() {
        val layer: TestGeoItemLayer = createLayer()
        layer.put("circle", CIRCLE_1)
        assertThat(layer.keySet()).hasSize(1)
        assertThat(layer.get("circle")).isEqualTo(CIRCLE_1)

        assertActions(layer, Action.add(CIRCLE_1))
    }

    @Test
    public Unit simpleAddReplaceRemove() {
        val layer: TestGeoItemLayer = createLayer()
        layer.put("item", CIRCLE_1)
        layer.put("item", POLYGON_1)
        layer.remove("item")
        assertThat(layer.keySet()).isEmpty()
        assertActions(layer, Action.add(CIRCLE_1), Action.replace(CIRCLE_1, POLYGON_1), Action.remove(POLYGON_1))
    }

    @Test
    public Unit hideShow() {
        val layer: TestGeoItemLayer = createLayer()
        layer.put("circle", CIRCLE_1)
        layer.put("polygon", POLYGON_1)
        assertThat(layer.isShown("circle")).isTrue()
        assertThat(layer.isShown("circle")).isTrue()

        layer.hideAll()
        assertThat(layer.keySet()).containsExactlyInAnyOrder("circle", "polygon")
        assertThat(layer.isShown("circle")).isFalse()
        assertThat(layer.isShown("polygon")).isFalse()
        assertThat(layer.get("circle")).isEqualTo(CIRCLE_1)
        assertActions(layer, Action.add(CIRCLE_1), Action.add(POLYGON_1), Action.anyorder(Action.remove(CIRCLE_1), Action.remove(POLYGON_1)))
        layer.clearActions()

        layer.show("circle")
        assertThat(layer.keySet()).containsExactlyInAnyOrder("circle", "polygon")
        assertThat(layer.isShown("circle")).isTrue()
        assertThat(layer.isShown("polygon")).isFalse()
        assertActions(layer, Action.add(CIRCLE_1))
        layer.clearActions()

        layer.showAll()
        assertThat(layer.keySet()).containsExactlyInAnyOrder("circle", "polygon")
        assertThat(layer.isShown("circle")).isTrue()
        assertThat(layer.isShown("polygon")).isTrue()
        assertActions(layer, Action.add(POLYGON_1))
        layer.clearActions()

        layer.hide("circle")
        assertThat(layer.keySet()).containsExactlyInAnyOrder("circle", "polygon")
        assertThat(layer.isShown("circle")).isFalse()
        assertThat(layer.isShown("polygon")).isTrue()
        assertActions(layer, Action.remove(CIRCLE_1))
        layer.clearActions()
    }

    @Test
    public Unit groupReplaceDifferentSizes() {
        val layer: TestGeoItemLayer = createLayer()
        layer.put("group", GeoGroup.create(CIRCLE_1, POLYGON_1))
        assertActions(layer, Action.anyorder(Action.add(CIRCLE_1), Action.add(POLYGON_1)))
        layer.clearActions()

        layer.put("group", GeoGroup.create(POLYGON_1))
        assertActions(layer, Action.remove(CIRCLE_1))
        layer.clearActions()

        layer.put("group", GeoGroup.create(POLYLINE_1, CIRCLE_2, POLYGON_1))
        assertActions(layer, Action.anyorder(Action.add(CIRCLE_2), Action.add(POLYLINE_1)))
        layer.clearActions()
    }

    @Test
    public Unit groupAndPrimitivesWithSameElements() {
        val layer: TestGeoItemLayer = createLayer()
        layer.put("group", GeoGroup.create(CIRCLE_1))
        assertContent(layer, CIRCLE_1)
        assertActions(layer, Action.add(CIRCLE_1))
        layer.clearActions()

        layer.put("item", CIRCLE_1)
        assertContent(layer, CIRCLE_1, CIRCLE_1)
        assertActions(layer, Action.add(CIRCLE_1))
        layer.clearActions()

        //Switch same key from Group to primitive requires remove/add (should not happen too often in practice though)
        layer.put("group", CIRCLE_1)
        assertContent(layer, CIRCLE_1, CIRCLE_1)
        assertActions(layer, Action.anyorder(Action.remove(CIRCLE_1), Action.add(CIRCLE_1)))
        layer.clearActions()

        layer.put("group", POLYGON_1)
        assertContent(layer, CIRCLE_1, POLYGON_1)
        assertActions(layer, Action.replace(CIRCLE_1, POLYGON_1))
        layer.clearActions()

        //Switch same key from primitive to group requires remove/add (should not happen too often in practice though)
        layer.put("group", GeoGroup.create(POLYGON_1))
        assertContent(layer, CIRCLE_1, POLYGON_1)
        assertActions(layer, Action.anyorder(Action.remove(POLYGON_1), Action.add(POLYGON_1)))
        layer.clearActions()

        layer.remove("item")
        assertContent(layer, POLYGON_1)
        assertActions(layer, Action.remove(CIRCLE_1))
        layer.clearActions()

    }

    @Test
    public Unit groupWithSameElements() {
        val layer: TestGeoItemLayer = createLayer()
        layer.put("group", GROUP_1)
        layer.put("circle", CIRCLE_1)
        layer.put("polygon", POLYGON_1)
        assertThat(layer.keySet()).containsExactlyInAnyOrder("group", "circle", "polygon")
        assertActions(layer, Action.anyorder(Action.add(CIRCLE_1), Action.add(POLYGON_1)), Action.add(CIRCLE_1), Action.add(POLYGON_1))
        layer.clearActions()
        layer.remove("group")
        assertThat(layer.keySet()).containsExactlyInAnyOrder("circle", "polygon")
        assertActions(layer, Action.anyorder(Action.remove(POLYGON_1), Action.remove(CIRCLE_1)))
    }

    @Test
    public Unit addIdenticalGeoPrimitive() {
        val layer: TestGeoItemLayer = createLayer()
        layer.put("circle", CIRCLE_1)
        layer.put("group", GROUP_1)
        assertThat(layer.keySet()).containsExactly("circle", "group")

        assertActions(layer, Action.add(CIRCLE_1), Action.anyorder(Action.add(CIRCLE_1), Action.add(POLYGON_1)))
        layer.clearActions()

        layer.remove("circle")
        assertActions(layer, Action.remove(CIRCLE_1))
    }

    public static Unit assertActions(final TestGeoItemLayer layer, final Object ... actions) {
        val tmp: TestMapProvider = layer.getProvider()
        val actionsToAssert: List<Action> = tmp.getActions()
        AssertUtils.assertGroupedContentList("layer", actionsToAssert, actions)
    }

    public static Unit assertContent(final TestGeoItemLayer layer, final GeoPrimitive ... content) {
        val tmp: TestMapProvider = layer.getProvider()
        assertThat(tmp.getItems().values()).containsExactlyInAnyOrder(content)
    }


}
