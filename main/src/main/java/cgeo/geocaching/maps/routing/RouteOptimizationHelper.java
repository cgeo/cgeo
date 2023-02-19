package cgeo.geocaching.maps.routing;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.ui.dialog.SimpleProgressDialog;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

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

        TSPDialog(final Context context, final ExecutorService executor, final int routeSize) {
            super(context, "Route optimization");
            best = new int[routeSize + 1];

            super.setButton(BUTTON_NEGATIVE, "Cancel", (dialogInterface, i) -> {
                executor.shutdownNow();
                dismiss();
            });
            super.setButton(BUTTON_POSITIVE, "Use", ((dialogInterface, i) -> {
                executor.shutdownNow();
                dismiss();
                // @todo: use result
            }));
            // will be set further down
            super.setButton(BUTTON_NEUTRAL, "Redo", (((dialogInterface, i) -> { })));
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
                postAdditionalInfo("Initial route length: " + initialLength.get()
                        + (length != initialLength.get() ? "\nOptimized route length: " + length : "")
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

    public void start(final Context context) {
        if (routeSize < 3 || routeSize > 100) {
            Toast.makeText(context, "Route optimization works for routes between 3 and 100 points", Toast.LENGTH_LONG).show();
            return;
        }

        final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);
        final TSPDialog dialog = new TSPDialog(context, executor, routeSize);
        dialog.show();

        dialog.setMessage("Generating distance matrix");
        dialog.setTypeDeterminate(routeSize * (routeSize - 1));
        AndroidRxUtils.andThenOnUi(AndroidRxUtils.computationScheduler, () -> {
            generateDistanceMatrix(dialog, executor);
        }, () -> {
            final int[] best = new int[routeSize + 1];
            initBest(dialog, best, true);
            // logRoute("calculated initial route", best);
            dialog.foundNewRoute(best);
            dialog.setTypeIndeterminate();
            runTSPWrapper(dialog, executor);
        });
    }

    private void runTSPWrapper(final TSPDialog dialog, final ExecutorService executor) {
        dialog.setMessage("Running route optimizations");
        dialog.setProgressVisibility(View.VISIBLE);
        AndroidRxUtils.andThenOnUi(AndroidRxUtils.computationScheduler, () -> {
            runTSP(dialog, executor);
        }, () -> {
            dialog.setMessage("Route optimization finished");
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

    /** print route to log */
    /*
    private void logRoute(final String info, final int[] route) {
        final StringBuilder s = new StringBuilder(info);
        s.append(": length=").append(calculateRouteLength(route)).append(", route=(");
        for (int i = 0; i < routeSize; i++) {
            s.append(initialRoute.get(route[i]).getIdentifier()).append(" - ");
        }
        s.append(initialRoute.get(route[0]).getIdentifier()).append(")");
        Log.e(s.toString());
    }
    */

    /** generate matrix of all distances between pairs */
    private void generateDistanceMatrix(final SimpleProgressDialog dialog, final ExecutorService executor) {
        final AtomicInteger progress = new AtomicInteger(0);
        Log.e("Start generateDistanceMatrix");
        final List<Future<Object>> taskList = new ArrayList<>();
        try {
            for (int i = 0; i < routeSize; i++) {
                final int col = i;
                taskList.add(executor.submit(() -> {
//                    Log.e("distance(" + col + ",*): calculation started");
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
//                                 Log.e("distance(" + col + ",*): calculation cancelled");
                                return 1;
                            }
                        }
                    }
//                    Log.e("distance(" + col + ",*): calculation finished");
                    return 1;
                }));
            }
            for (Future<Object> future : taskList) {
                future.get();
            }
            Log.e("generating distance matrix finished");
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        Log.e("End generateDistanceMatrix");
    }

    /** TSP calculation using hill climbing */
    private void hillClimbing(final TSPDialog dialog) {
        final int[] best = new int[routeSize + 1];
        int bestFitness = initBest(dialog, best, false);
        int[] savedState;
        for (int i = 0; i < 10000; i++) {
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
            final int posA = rand(1, routeSize - 2);
            final int posB = rand(1, routeSize - 2);
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
