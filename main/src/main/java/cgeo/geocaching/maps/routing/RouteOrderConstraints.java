package cgeo.geocaching.maps.routing;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.Waypoint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * Order constraints for route items belonging to linear lab adventures:
 * the stages of a linear lab adventure have to be visited in ascending stage order,
 * other route items may be mixed in freely.
 */
public class RouteOrderConstraints {

    /** stage info of a route item belonging to a linear lab adventure */
    public static final class Stage {
        @NonNull public final String geocode;
        public final int number;

        public Stage(@NonNull final String geocode, final int number) {
            this.geocode = geocode;
            this.number = number;
        }
    }

    private final int routeSize;
    /** groupOf[i] = group id of route item i (index into initial route), or -1 if unconstrained */
    private final int[] groupOf;
    /** sortedMembers[g] = indices (into initial route) of members of group g, sorted by stage number */
    private final int[][] sortedMembers;

    /**
     * @param stages stage info per route item (same order as the route), null for unconstrained items.
     *               First and last route item are fixed and therefore never part of a constraint group.
     */
    public RouteOrderConstraints(@NonNull final List<Stage> stages) {
        routeSize = stages.size();
        groupOf = new int[routeSize];
        Arrays.fill(groupOf, -1);

        final Map<String, List<Integer>> members = new LinkedHashMap<>();
        for (int i = 1; i < routeSize - 1; i++) {
            final Stage stage = stages.get(i);
            if (stage != null) {
                Objects.requireNonNull(members.computeIfAbsent(stage.geocode, k -> new ArrayList<>())).add(i);
            }
        }

        final List<int[]> groups = new ArrayList<>();
        for (List<Integer> m : members.values()) {
            if (m.size() < 2) {
                continue;
            }
            // stable sort => duplicate stage numbers keep their relative order
            m.sort((a, b) -> Integer.compare(stages.get(a).number, stages.get(b).number));
            final int g = groups.size();
            final int[] sorted = new int[m.size()];
            for (int k = 0; k < sorted.length; k++) {
                sorted[k] = m.get(k);
                groupOf[sorted[k]] = g;
            }
            groups.add(sorted);
        }
        sortedMembers = groups.toArray(new int[0][]);
    }

    /** builds constraints for the given route, needs database access */
    @WorkerThread
    @NonNull
    public static RouteOrderConstraints forRoute(@NonNull final List<RouteItem> route) {
        return new RouteOrderConstraints(getStages(route));
    }

    public boolean isEmpty() {
        return sortedMembers.length == 0;
    }

    /** returns number of members of the given group */
    public int groupSize(final int group) {
        return sortedMembers[group].length;
    }

    /** returns group id for the given route item index (into initial route), or -1 if unconstrained */
    public int groupOf(final int itemIndex) {
        return groupOf[itemIndex];
    }

    /**
     * Reorders members of each group within their currently occupied positions according to their stage number.
     * First and last position are left untouched. Runs in O(n).
     */
    public void repair(@NonNull final int[] route) {
        if (isEmpty()) {
            return;
        }
        final int[] next = new int[sortedMembers.length];
        for (int pos = 1; pos < routeSize - 1; pos++) {
            final int g = groupOf[route[pos]];
            if (g >= 0) {
                route[pos] = sortedMembers[g][next[g]++];
            }
        }
    }

    /** returns true if at least one swap of two free positions leads to a different (valid) route */
    public boolean hasEffectiveSwap() {
        int distinctClasses = 0;
        final Set<Integer> seenGroups = new HashSet<>();
        for (int i = 1; i < routeSize - 1; i++) {
            final int g = groupOf[i];
            if (g < 0 || seenGroups.add(g)) {
                distinctClasses++;
                if (distinctClasses >= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks whether the given stages (in route order, null for unconstrained items) violate
     * the order of at least one linear lab adventure. All positions are checked, including first and last.
     */
    public static boolean isOrderViolated(@NonNull final List<Stage> stagesInRouteOrder) {
        final Map<String, Integer> lastStage = new HashMap<>();
        for (Stage stage : stagesInRouteOrder) {
            if (stage == null) {
                continue;
            }
            final Integer last = lastStage.get(stage.geocode);
            if (last != null && last > stage.number) {
                return true;
            }
            lastStage.put(stage.geocode, stage.number);
        }
        return false;
    }

    /** returns stage info for each route item (null for unconstrained items), needs database access */
    @WorkerThread
    @NonNull
    public static List<Stage> getStages(@NonNull final List<RouteItem> route) {
        final Map<String, Boolean> linearCache = new HashMap<>();
        final List<Stage> result = new ArrayList<>(route.size());
        for (RouteItem item : route) {
            result.add(getStage(item, linearCache));
        }
        return result;
    }

    @WorkerThread
    @Nullable
    private static Stage getStage(@NonNull final RouteItem item, @NonNull final Map<String, Boolean> linearCache) {
        final String geocode = item.getGeocode();
        if (item.getType() != RouteItem.RouteItemType.WAYPOINT || StringUtils.isBlank(geocode)) {
            return null;
        }
        Boolean linear = linearCache.get(geocode);
        if (linear == null) {
            final Geocache cache = item.getGeocache();
            linear = cache != null && cache.isLinearAlc();
            linearCache.put(geocode, linear);
        }
        if (!linear) {
            return null;
        }
        final Waypoint waypoint = item.getWaypoint();
        if (waypoint == null) {
            return null;
        }
        try {
            return new Stage(geocode, Integer.parseInt(waypoint.getPrefix()));
        } catch (NumberFormatException ignore) {
            // no stage number => unconstrained
            return null;
        }
    }
}
