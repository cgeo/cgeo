package cgeo.geocaching.brouter.util;

/**
 * a median filter with additional edge reduction
 */
public final class ReducedMedianFilter {
    private int nsamples;
    private final double[] weights;
    private final int[] values;

    public ReducedMedianFilter(final int size) {
        weights = new double[size];
        values = new int[size];
    }

    public void reset() {
        nsamples = 0;
    }

    public void addSample(final double weight, final int value) {
        if (weight > 0.) {
            for (int i = 0; i < nsamples; i++) {
                if (values[i] == value) {
                    weights[i] += weight;
                    return;
                }
            }
            weights[nsamples] = weight;
            values[nsamples] = value;
            nsamples++;
        }
    }

    public double calcEdgeReducedMedian(final double fraction) {
        removeEdgeWeight((1. - fraction) / 2., true);
        removeEdgeWeight((1. - fraction) / 2., false);

        double totalWeight = 0.;
        double totalValue = 0.;
        for (int i = 0; i < nsamples; i++) {
            final double w = weights[i];
            totalWeight += w;
            totalValue += w * values[i];
        }
        return totalValue / totalWeight;
    }


    private void removeEdgeWeight(final double excessWeightIn, final boolean high) {
        double excessWeight = excessWeightIn;
        while (excessWeight > 0.) {
            // first pass to find minmax value
            double totalWeight = 0.;
            int minmax = 0;
            for (int i = 0; i < nsamples; i++) {
                final double w = weights[i];
                if (w > 0.) {
                    final int v = values[i];
                    if (totalWeight == 0. || (high ? v > minmax : v < minmax)) {
                        minmax = v;
                    }
                    totalWeight += w;
                }
            }

            if (totalWeight < excessWeight) {
                throw new IllegalArgumentException("ups, not enough weight to remove");
            }

            // second pass to remove
            for (int i = 0; i < nsamples; i++) {
                if (values[i] == minmax && weights[i] > 0.) {
                    if (excessWeight > weights[i]) {
                        excessWeight -= weights[i];
                        weights[i] = 0.;
                    } else {
                        weights[i] -= excessWeight;
                        excessWeight = 0.;
                    }
                }
            }
        }
    }
}
