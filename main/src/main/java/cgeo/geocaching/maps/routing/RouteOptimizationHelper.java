package cgeo.geocaching.maps.routing;

import android.content.Context;
import android.widget.Toast;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.reactivex.rxjava3.schedulers.Schedulers;

public class RouteOptimizationHelper {

    private final List<RouteItem> initialRoute;
    private final int[][] distanceMatrix;
    private final int routeSize;

    public RouteOptimizationHelper(final ArrayList<RouteItem> route) {
        initialRoute = route;
        routeSize = route.size();
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
        AndroidRxUtils.andThenOnUi(Schedulers.newThread(), this::generateDistanceMatrix, () -> {
            Log.e("generating distance matrix finished");
            final int[] newRoute = new int[routeSize + 1];
            AndroidRxUtils.andThenOnUi(Schedulers.newThread(), () -> {
                final int[] temp = simulatedAnnealing();
                System.arraycopy(temp, 0, newRoute, 0, routeSize + 1);
            }, () -> {
                Log.e("found a calculation with simulated annealing");
                logRoute(newRoute);

                AndroidRxUtils.andThenOnUi(Schedulers.newThread(), () -> {
                    final int[] temp = hillClimbing();
                    System.arraycopy(temp, 0, newRoute, 0, routeSize + 1);
                }, () -> {
                    Log.e("found a calculation with hill climbing");
                    logRoute(newRoute);
                });
            });
        });
    }

    /** print route to log */
    private void logRoute(final int[] route) {
        final StringBuilder s = new StringBuilder();
        s.append("length=").append(calculateRouteLength(route)).append(") route: (");
        for (int i = 0; i < routeSize; i++) {
            s.append(initialRoute.get(route[i]).getIdentifier()).append(" - ");
        }
        s.append(initialRoute.get(route[0]).getIdentifier()).append(")");
        Log.e(s.toString());
    }

    /** generate matrix of all distances between pairs */
    private void generateDistanceMatrix() {
        for (int i = 0; i < routeSize; i++) {
            Log.e("distance(" + i + ",*): calculation started");
            for (int j = 0; j < routeSize; j++) {
                if (i != j) {
                    final Geopoint[] track = Routing.getTrackNoCaching(
                            new Geopoint(initialRoute.get(i).getPoint().getLatitude(), initialRoute.get(i).getPoint().getLongitude()),
                            new Geopoint(initialRoute.get(j).getPoint().getLatitude(), initialRoute.get(j).getPoint().getLongitude()));
                    float distance = 0.0f;
                    if (track.length > 0) {
                        Geopoint last = track[0];
                        for (Geopoint point : track) {
                            distance += last.distanceTo(point);
                            last = point;
                        }
                    }
                    distanceMatrix[i][j] = (int) (1000.0f * distance);
//                    Log.e("distance(" + i + "," + j + ") = " + distanceMatrix[i][j]);
                }
            }
        }
    }

    /** TSP calculation using hill climbing */
    private int[] hillClimbing() {
        final int[] best = new int[routeSize + 1];
        double bestFitness = initBest(best);
        int[] savedState;

        for (int i = 0; i < 10000; i++) {
            savedState = new int[routeSize + 1];
            System.arraycopy(best, 0, savedState, 0, routeSize + 1);

            swapRandomPoints(best);
            final double currentFitness = - calculateRouteLength(best);
            if (currentFitness > bestFitness) {
                bestFitness = currentFitness;
            } else {
                System.arraycopy(savedState, 0, best, 0, routeSize + 1);
            }
        }
        return best;
    }

    /** TSP calculation using simulated annealing */
    private int[] simulatedAnnealing() {
        final int[] best = new int[routeSize + 1];
        double bestFitness = initBest(best);
        int[] savedState;

        final double epsilon = 0.01;
        double temperature = 1538.0;

        while (temperature > epsilon) {
            savedState = new int[routeSize + 1];
            System.arraycopy(best, 0, savedState, 0, routeSize + 1);

            swapRandomPoints(best);
            final double currentFitness = - calculateRouteLength(best);
            if (currentFitness > bestFitness || (new Random().nextDouble() < Math.exp((currentFitness - bestFitness) / temperature))) {
                bestFitness = currentFitness;
            } else {
                System.arraycopy(savedState, 0, best, 0, routeSize + 1);
            }
            temperature -= epsilon;
        }
        return best;
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
    private double initBest(int[] best) {
        for (int i = 0; i < routeSize; i++) {
            best[i] = i;
        }
        best[routeSize] = 0;
        return - calculateRouteLength(best);
    }

    /** returns random number between (and including) bounds */
    public static int rand(final int lowerBound, final int upperBound) {
        return new Random().nextInt(upperBound - lowerBound + 1) + lowerBound;
    }

    /** returns length in meters for the given route */
    public double calculateRouteLength(final int[] route) {
        double length = 0.0;
        for (int i = 0; i < route.length - 2; i++) {
            length += initialRoute.get(route[i]).getPoint().distanceTo(initialRoute.get(route[i + 1]).getPoint());
        }
        return length;
    }

}
