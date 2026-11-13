package cgeo.geocaching.maps.routing;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.storage.extension.OneTimeDialogs;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleProgressDialog;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Action1;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_NEUTRAL;
import static android.content.DialogInterface.BUTTON_POSITIVE;

import androidx.appcompat.app.AlertDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RouteOptimizationHelper {

    private static final String CACHED_DISTANCES_FILENAME = "tsp_cache.json";
    private static final int CACHED_DISTANCES_FILEVERSION = 1;

    private final List<RouteItem> initialRoute;
    private final int[][] distanceMatrix;
    private final int routeSize;
    private RouteOrderConstraints constraints;
    private CachedDistances cachedDistances = resetCachedDistances();

    private class TSPDialog extends SimpleProgressDialog {

        private final AtomicInteger length = new AtomicInteger(Integer.MAX_VALUE);
        private final AtomicInteger initialLength = new AtomicInteger(Integer.MAX_VALUE);
        private final int[] best;

        TSPDialog(final Context context, final ExecutorService executor, final Action1<ArrayList<RouteItem>> updateRoute) {
            super(context, TextParam.id(R.string.route_optimization));
            best = new int[routeSize];

            setButton(BUTTON_NEGATIVE, TextParam.id(R.string.cancel), (dialogInterface, i) -> {
                executor.shutdownNow();
                dismiss();
            });
            setButton(BUTTON_POSITIVE, TextParam.id(R.string.use), ((dialogInterface, i) -> {
                executor.shutdownNow();
                dismiss();
                final ArrayList<RouteItem> newRoute = new ArrayList<>();
                for (int r = 0; r < routeSize; r++) {
                    newRoute.add(initialRoute.get(best[r]));
                }
                updateRoute.call(newRoute);
                saveCachedDistances(newRoute);
            }));
            // will be set further down
            setButton(BUTTON_NEUTRAL, TextParam.id(R.string.redo), (((dialogInterface, i) -> { })));
        }

        @Override
        public AlertDialog show() {
            super.show();
            dialog.getButton(BUTTON_NEUTRAL).setVisibility(View.GONE);
            dialog.getButton(BUTTON_POSITIVE).setEnabled(false);
            return dialog;
        }

        public void updateButton(final int whichButton, final View.OnClickListener listener) {
            if (dialog == null) {
                return;
            }
            final Button button = dialog.getButton(whichButton);
            if (button != null) {
                button.setVisibility(View.VISIBLE);
                button.setEnabled(true);
                button.setOnClickListener(listener);
            }
        }

        void foundNewRoute(final int[] route) {
            foundNewRoute(route, calculateRouteLength(route));
        }

        /** same as foundNewRoute(route), but with already known route length (avoids recalculation) */
        void foundNewRoute(final int[] route, final int length) {
            if (initialLength.get() == Integer.MAX_VALUE) {
                initialLength.set(length);
            }
            if (length < this.length.get()) {
                this.length.set(length);
                synchronized (best) {
                    System.arraycopy(route, 0, best, 0, routeSize);
                }
                postAdditionalInfo(TextParam.text(LocalizationUtils.getString(R.string.initial_route_length, initialLength.get())
                        + (length != initialLength.get() ? "\n" + LocalizationUtils.getString(R.string.optimized_route_length, length) : ""))
                );
                dialog.getButton(BUTTON_POSITIVE).setEnabled(true);
            }
        }

        int[] getRoute() {
            return best;
        }
    }

    public RouteOptimizationHelper(final ArrayList<RouteItem> route) {
        initialRoute = route;
        routeSize = initialRoute.size();
        distanceMatrix = new int[routeSize][routeSize];
        for (int i = 0; i < routeSize; i++) {
            for (int j = 0; j < routeSize; j++) {
                distanceMatrix[i][j] = Integer.MAX_VALUE;
            }
        }
    }

    public void start(final Context context, final Action1<ArrayList<RouteItem>> updateRoute) {
        if (routeSize < 3 || routeSize > 100) {
            ViewUtils.showToast(context, R.string.route_optimization_out_of_bounds);
            return;
        }

        Dialogs.advancedOneTimeMessage(context, OneTimeDialogs.DialogType.ROUTE_OPTIMIZATION, LocalizationUtils.getString(R.string.route_optimization), LocalizationUtils.getString(R.string.route_optimization_info), true, () -> {
            final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            final TSPDialog dialog = new TSPDialog(context, executor, updateRoute);
            dialog.show();

            dialog.setMessage(TextParam.id(R.string.generating_distance_matrix));
            dialog.setTypeDeterminate(routeSize * (routeSize - 1));
            AndroidRxUtils.andThenOnUi(AndroidRxUtils.computationScheduler, () -> {
                constraints = RouteOrderConstraints.forRoute(initialRoute);
                generateDistanceMatrix(dialog, executor);
            }, () -> {
                final int[] best = new int[routeSize];
                initBest(dialog, best, true);
                dialog.foundNewRoute(best);
                dialog.setTypeIndeterminate();
                if (constraints.hasEffectiveSwap()) {
                    runTSPWrapper(dialog, executor);
                } else {
                    // no reordering possible (apart from fixing order of linear lab adventures)
                    dialog.setMessage(TextParam.id(R.string.route_optimization_finished));
                    dialog.setProgressVisibility(View.GONE);
                }
            });
        });

    }

    private void runTSPWrapper(final TSPDialog dialog, final ExecutorService executor) {
        dialog.setMessage(TextParam.id(R.string.running_route_optimizations));
        dialog.setProgressVisibility(View.VISIBLE);
        AndroidRxUtils.andThenOnUi(AndroidRxUtils.computationScheduler, () -> runTSP(dialog, executor), () -> {
            dialog.setMessage(TextParam.id(R.string.route_optimization_finished));
            dialog.setProgressVisibility(View.GONE);
            dialog.updateButton(BUTTON_NEUTRAL, view -> runTSPWrapper(dialog, executor));
        });
    }

    private void runTSP(final TSPDialog dialog, final ExecutorService executor) {
        final List<Future<Object>> taskList = new ArrayList<>();
        try {
            taskList.add(executor.submit(() -> {
                simulatedAnnealing(dialog);
                hillClimbing(dialog);
                return 1;
            }));
            taskList.add(executor.submit(() -> {
                hillClimbing(dialog);
                return 1;
            }));
            for (Future<Object> future : taskList) {
                future.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    /** generate matrix of all distances between pairs */
    private void generateDistanceMatrix(final SimpleProgressDialog dialog, final ExecutorService executor) {
        loadCachedDistances();
        final AtomicInteger progress = new AtomicInteger(0);
        final List<Future<Object>> taskList = new ArrayList<>();
        try {
            for (int i = 0; i < routeSize; i++) {
                final int col = i;
                taskList.add(executor.submit(() -> {
                    for (int j = 0; j < routeSize; j++) {
                        if (col != j) {
                            final Integer temp = cachedDistances.distances.get(getCacheKey(initialRoute.get(col).getPoint(), initialRoute.get(j).getPoint()));
                            if (temp != null && temp != Integer.MAX_VALUE) {
                                distanceMatrix[col][j] = temp;
                            } else {
                                final Geopoint[] track = Routing.getTrackNoCaching(
                                        new Geopoint(initialRoute.get(col).getPoint().getLatitude(), initialRoute.get(col).getPoint().getLongitude()),
                                        new Geopoint(initialRoute.get(j).getPoint().getLatitude(), initialRoute.get(j).getPoint().getLongitude()),
                                        null,
                                        null);
                                float distance = 0.0f;
                                if (track.length > 0) {
                                    Geopoint last = track[0];
                                    for (Geopoint point : track) {
                                        distance += last.distanceTo(point);
                                        last = point;
                                    }
                                }
                                distanceMatrix[col][j] = (int) (1000.0f * distance);
                            }
                            progress.set(progress.get() + 1);
                            dialog.postProgress(progress.get() + 1);
                            if (Thread.currentThread().isInterrupted()) {
                                return 1;
                            }
                        }
                    }
                    return 1;
                }));
            }
            for (Future<Object> future : taskList) {
                future.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    /** TSP calculation using hill climbing */
    private void hillClimbing(final TSPDialog dialog) {
        final int[] route = new int[routeSize];
        int length = -initBest(dialog, route, false);
        final SwapMove move = new SwapMove(route);
        for (int i = 0; i < 15000; i++) {
            final int newLength = move.apply(length);
            if (newLength < length) {
                length = newLength;
                dialog.foundNewRoute(route, length);
            } else {
                move.undo();
            }
        }
    }

    /** TSP calculation using simulated annealing */
    private void simulatedAnnealing(final TSPDialog dialog) {
        final int[] route = new int[routeSize];
        int length = -initBest(dialog, route, false);
        final SwapMove move = new SwapMove(route);
        final double epsilon = 0.01;
        double temperature = 1538.0;
        while (temperature > epsilon) {
            final int newLength = move.apply(length);
            final int improvement = length - newLength;
            if (improvement > 0 || ThreadLocalRandom.current().nextDouble() < Math.exp(improvement / temperature)) {
                length = newLength;
                dialog.foundNewRoute(route, length);
            } else {
                move.undo();
            }
            temperature -= epsilon;
        }
    }

    /**
     * Random swap of two entries (except first and last) on a given route, which can be undone.
     * If no linear lab adventure is involved, the new route length is evaluated in constant time.
     * Otherwise the stage order is restored afterwards and the route length gets recalculated.
     * Requires constraints.hasEffectiveSwap() to be true. Not thread-safe, use one instance per thread.
     */
    private final class SwapMove {
        private final int[] route;
        private final int[] savedState = new int[routeSize];
        private int posA;
        private int posB;
        private boolean repaired;

        SwapMove(final int[] route) {
            this.route = route;
        }

        /** applies a random swap to the route and returns the new route length */
        int apply(final int currentLength) {
            selectPositions();
            repaired = constraints.groupOf(route[posA]) >= 0 || constraints.groupOf(route[posB]) >= 0;
            if (!repaired) {
                final int delta = swapDelta(distanceMatrix, route, posA, posB);
                swap(route, posA, posB);
                return currentLength + delta;
            }
            System.arraycopy(route, 0, savedState, 0, routeSize);
            swap(route, posA, posB);
            constraints.repair(route);
            return calculateRouteLength(distanceMatrix, route);
        }

        /** reverts the last call to apply() */
        void undo() {
            if (repaired) {
                System.arraycopy(savedState, 0, route, 0, routeSize);
            } else {
                swap(route, posA, posB);
            }
        }

        /** selects two different positions, which do not belong to the same linear lab adventure */
        private void selectPositions() {
            posA = rand(1, routeSize - 2);
            final int groupA = constraints.groupOf(route[posA]);
            if (groupA < 0) {
                // any other position
                posB = rand(1, routeSize - 3);
                if (posB >= posA) {
                    posB++;
                }
                return;
            }
            // any position not belonging to the same group (count is > 0, as hasEffectiveSwap() is true)
            int remaining = rand(0, routeSize - 2 - constraints.groupSize(groupA) - 1);
            for (int pos = 1; pos < routeSize - 1; pos++) {
                if (constraints.groupOf(route[pos]) != groupA && remaining-- == 0) {
                    posB = pos;
                    return;
                }
            }
            throw new IllegalStateException("no valid swap position found");
        }
    }

    @SuppressWarnings("checkstyle:FinalParameters")
    private static void swap(int[] route, final int posA, final int posB) {
        final int temp = route[posA];
        route[posA] = route[posB];
        route[posB] = temp;
    }

    /**
     * returns the change in route length when swapping the entries at posA and posB (both excluding first and last position),
     * works for asymmetric distance matrices as well
     */
    static int swapDelta(final int[][] d, final int[] route, final int posA, final int posB) {
        final int i = Math.min(posA, posB);
        final int j = Math.max(posA, posB);
        final int prev = route[i - 1];
        final int a = route[i];
        final int b = route[j];
        final int next = route[j + 1];
        if (j == i + 1) {
            return d[prev][b] + d[b][a] + d[a][next] - d[prev][a] - d[a][b] - d[b][next];
        }
        final int afterA = route[i + 1];
        final int beforeB = route[j - 1];
        return d[prev][b] + d[b][afterA] + d[beforeB][a] + d[a][next]
                - d[prev][a] - d[a][afterA] - d[beforeB][b] - d[b][next];
    }

    /** initializes initial vector, returns its fitness */
    @SuppressWarnings("checkstyle:FinalParameters")
    private int initBest(final TSPDialog dialog, int[] best, final boolean forceDefault) {
        if (forceDefault) {
            for (int i = 0; i < routeSize; i++) {
                best[i] = i;
            }
            // initial route may violate stage order of linear lab adventures
            constraints.repair(best);
        } else {
            final int[] temp = dialog.getRoute();
            System.arraycopy(temp, 0, best, 0, routeSize);
        }
        return - calculateRouteLength(best);
    }

    /** returns random number between (and including) bounds */
    public static int rand(final int lowerBound, final int upperBound) {
        return ThreadLocalRandom.current().nextInt(lowerBound, upperBound + 1);
    }

    /** returns length in meters for the given route */
    public int calculateRouteLength(final int[] route) {
        return calculateRouteLength(distanceMatrix, route);
    }

    /** returns length in meters for the given route, based on the given distance matrix */
    static int calculateRouteLength(final int[][] d, final int[] route) {
        int length = 0;
        for (int i = 0; i < route.length - 1; i++) {
            length += d[route[i]][route[i + 1]];
        }
        return length;
    }


    // routines for caching distance matrix data

    private static class CachedDistances {
        public int version;
        public RoutingMode routingMode;
        public HashMap<String, Integer> distances;
    }

    private String getCacheKey(final Geopoint from, final Geopoint to) {
        return from.getLatitudeE6() + "," + from.getLongitudeE6() + "," + to.getLatitudeE6() + "," + to.getLongitudeE6();
    }

    private void loadCachedDistances() {
        try (InputStream fileStream = new FileInputStream(new File(LocalStorage.getFirstExternalPrivateCgeoDirectory(), CACHED_DISTANCES_FILENAME))) {
            cachedDistances = new ObjectMapper().readValue(fileStream, CachedDistances.class);
            if (cachedDistances.version != CACHED_DISTANCES_FILEVERSION || cachedDistances.routingMode != Settings.getRoutingMode()) {
                Log.i("discarding tsp cache due to different routing mode or wrong file version");
                resetCachedDistances();
            }
        } catch (final IOException e) {
            Log.w("Error reading tsp cache file: " + e.getMessage());
        }
    }

    private void saveCachedDistances(final ArrayList<RouteItem> route) {
        resetCachedDistances();
        for (int i = 0; i < route.size(); i++) {
            for (int j = 0; j < route.size(); j++) {
                if (i != j && distanceMatrix[i][j] != Integer.MAX_VALUE) {
                    cachedDistances.distances.put(getCacheKey(initialRoute.get(i).getPoint(), initialRoute.get(j).getPoint()), distanceMatrix[i][j]);
                }
            }
        }
        try {
            new ObjectMapper().writeValue(new File(LocalStorage.getFirstExternalPrivateCgeoDirectory(), CACHED_DISTANCES_FILENAME), cachedDistances);
        } catch (final IOException e) {
            Log.w("Error writing tsp cache file: " + e.getMessage());
        }
    }

    private CachedDistances resetCachedDistances() {
        cachedDistances = new CachedDistances();
        cachedDistances.version = CACHED_DISTANCES_FILEVERSION;
        cachedDistances.routingMode = Settings.getRoutingMode();
        cachedDistances.distances = new HashMap<>();
        return cachedDistances;
    }

}
