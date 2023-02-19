package cgeo.geocaching.maps.routing;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.ui.dialog.SimpleProgressDialog;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.functions.Action1;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_NEUTRAL;
import static android.content.DialogInterface.BUTTON_POSITIVE;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class RouteOptimizationHelper {

    private final List<RouteItem> initialRoute;
    private final int[][] distanceMatrix;
    private final int routeSize;

    private class TSPDialog extends SimpleProgressDialog {

        private final AtomicInteger length = new AtomicInteger(Integer.MAX_VALUE);
        private final AtomicInteger initialLength = new AtomicInteger(Integer.MAX_VALUE);
        private final int[] best;

        TSPDialog(final Context context, final ExecutorService executor, final int routeSize, final Action1<ArrayList<RouteItem>> updateRoute) {
            super(context, TextParam.id(R.string.route_optimization));
            best = new int[routeSize + 1];

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
            final Button button = dialog.getButton(whichButton);
            if (button != null) {
                button.setVisibility(View.VISIBLE);
                button.setEnabled(true);
                button.setOnClickListener(listener);
            }
        }

        void foundNewRoute(final int[] route) {
            final int length = calculateRouteLength(route);
            if (initialLength.get() == Integer.MAX_VALUE) {
                initialLength.set(length);
            }
            if (length < this.length.get()) {
                this.length.set(length);
                synchronized (best) {
                    System.arraycopy(route, 0, best, 0, routeSize + 1);
                }
                postAdditionalInfo(TextParam.text(String.format(dialog.getContext().getString(R.string.initial_route_length), initialLength.get())
                        + (length != initialLength.get() ? "\n" + String.format(dialog.getContext().getString(R.string.optimized_route_length), length) : ""))
                );
                dialog.getButton(BUTTON_POSITIVE).setEnabled(true);
            }
        }

        int[] getRoute() {
            return best;
        }
    }

    public RouteOptimizationHelper(final ArrayList<RouteItem> route) {
        initialRoute = route; //.subList(0, 14);
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
            Toast.makeText(context, R.string.route_optimization_out_of_bounds, Toast.LENGTH_LONG).show();
            return;
        }

        SimpleDialog.of((Activity) context).setTitle(TextParam.id(R.string.route_optimization)).setMessage(TextParam.id(R.string.route_optimization_info)).confirm((d, which) -> {
            final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            final TSPDialog dialog = new TSPDialog(context, executor, routeSize, updateRoute);
            dialog.show();

            dialog.setMessage(TextParam.id(R.string.generating_distance_matrix));
            dialog.setTypeDeterminate(routeSize * (routeSize - 1));
            AndroidRxUtils.andThenOnUi(AndroidRxUtils.computationScheduler, () -> {
                generateDistanceMatrix(dialog, executor);
            }, () -> {
                final int[] best = new int[routeSize + 1];
                initBest(dialog, best, true);
                dialog.foundNewRoute(best);
                dialog.setTypeIndeterminate();
                runTSPWrapper(dialog, executor);
            });
        });
    }

    private void runTSPWrapper(final TSPDialog dialog, final ExecutorService executor) {
        dialog.setMessage(TextParam.id(R.string.running_route_optimizations));
        dialog.setProgressVisibility(View.VISIBLE);
        AndroidRxUtils.andThenOnUi(AndroidRxUtils.computationScheduler, () -> {
            runTSP(dialog, executor);
        }, () -> {
            dialog.setMessage(TextParam.id(R.string.route_optimization_finished));
            dialog.setProgressVisibility(View.GONE);
            dialog.updateButton(BUTTON_NEUTRAL, view -> {
                runTSPWrapper(dialog, executor);
            });
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
        final AtomicInteger progress = new AtomicInteger(0);
        final List<Future<Object>> taskList = new ArrayList<>();
        try {
            for (int i = 0; i < routeSize; i++) {
                final int col = i;
                taskList.add(executor.submit(() -> {
                    for (int j = 0; j < routeSize; j++) {
                        if (col != j) {
                            final Geopoint[] track = Routing.getTrackNoCaching(
                                    new Geopoint(initialRoute.get(col).getPoint().getLatitude(), initialRoute.get(col).getPoint().getLongitude()),
                                    new Geopoint(initialRoute.get(j).getPoint().getLatitude(), initialRoute.get(j).getPoint().getLongitude()));
                            float distance = 0.0f;
                            if (track.length > 0) {
                                Geopoint last = track[0];
                                for (Geopoint point : track) {
                                    distance += last.distanceTo(point);
                                    last = point;
                                }
                            }
                            distanceMatrix[col][j] = (int) (1000.0f * distance);
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
        final int[] best = new int[routeSize + 1];
        int bestFitness = initBest(dialog, best, false);
        int[] savedState;
        for (int i = 0; i < 15000; i++) {
            savedState = new int[routeSize + 1];
            System.arraycopy(best, 0, savedState, 0, routeSize + 1);
            swapRandomPoints(best);
            final int currentFitness = - calculateRouteLength(best);
            if (currentFitness > bestFitness) {
                bestFitness = currentFitness;
                dialog.foundNewRoute(best);
            } else {
                System.arraycopy(savedState, 0, best, 0, routeSize + 1);
            }
        }
    }

    /** TSP calculation using simulated annealing */
    private void simulatedAnnealing(final TSPDialog dialog) {
        final int[] best = new int[routeSize + 1];
        int bestFitness = initBest(dialog, best, false);
        int[] savedState;
        final double epsilon = 0.01;
        double temperature = 1538.0;
        while (temperature > epsilon) {
            savedState = new int[routeSize + 1];
            System.arraycopy(best, 0, savedState, 0, routeSize + 1);
            swapRandomPoints(best);
            final int currentFitness = - calculateRouteLength(best);
            if (currentFitness > bestFitness || (new Random().nextDouble() < Math.exp((currentFitness - bestFitness) / temperature))) {
                bestFitness = currentFitness;
                dialog.foundNewRoute(best);
            } else {
                System.arraycopy(savedState, 0, best, 0, routeSize + 1);
            }
            temperature -= epsilon;
        }
    }

    /** swaps entries at random positions except first and last */
    @SuppressWarnings("checkstyle:FinalParameters")
    private void swapRandomPoints(int[] route) {
        while (true) {
            final int posA = rand(1, routeSize - 1);
            final int posB = rand(1, routeSize - 1);
            if (posA != posB) {
                final int temp = route[posA];
                route[posA] = route[posB];
                route[posB] = temp;
                return;
            }
        }
    }

    /** initializes initial vector, returns its fitness */
    @SuppressWarnings("checkstyle:FinalParameters")
    private int initBest(final TSPDialog dialog, int[] best, final boolean forceDefault) {
        if (forceDefault) {
            for (int i = 0; i < routeSize; i++) {
                best[i] = i;
            }
            best[routeSize] = 0;
        } else {
            final int[] temp = dialog.getRoute();
            System.arraycopy(temp, 0, best, 0, routeSize + 1);
        }
        return - calculateRouteLength(best);
    }

    /** returns random number between (and including) bounds */
    public static int rand(final int lowerBound, final int upperBound) {
        return new Random().nextInt(upperBound - lowerBound + 1) + lowerBound;
    }

    /** returns length in meters for the given route */
    public int calculateRouteLength(final int[] route) {
        int length = 0;
        for (int i = 0; i < route.length - 2; i++) {
            length += distanceMatrix[route[i]][route[i + 1]];
        }
        return length;
    }

}
