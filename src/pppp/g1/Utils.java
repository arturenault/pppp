package pppp.g1;

import pppp.sim.Move;
import pppp.sim.Point;

public class Utils {
    /**
     * Generate point after negating or swapping coordinates
     *
     * @param x
     * @param y
     * @param neg_y
     * @param swap_xy
     * @return
     */
    public static Point point(
            double x, double y, boolean neg_y, boolean swap_xy
    ) {
        if (neg_y) y = -y;
        return swap_xy ? new Point(y, x) : new Point(x, y);
    }

    /**
     * Tells us whether the destination is reached from the source or
     * not based on how close the two points are.
     *
     * @param src The source position.
     * @param dest The destination position.
     * @param eps The different epsilon.
     */
    public static boolean isAtDest(Point src, Point dest, double eps) {
        return Math.abs(src.x - dest.x) < eps && Math.abs(src.y - dest.y) < eps;
    }

    /**
     * Create move from source to destination.
     *
     * @param src
     * @param dst
     * @param play
     */
    public static Move creatMove(Point src, Point dst, boolean play) {
        double dx = dst.x - src.x;
        double dy = dst.y - src.y;
        double length = Math.sqrt(dx * dx + dy * dy);
        double limit = play ? 0.1 : 0.5;
        if (length > limit) {
            dx = (dx * limit) / length;
            dy = (dy * limit) / length;
        }
        return new Move(dx, dy, play);
    }

    /**
     * Compute Euclidean distance between two points in a plane.
     *
     * @param a The position of a point.
     * @param b The position of a point.
     */
    public static double distance(Point a, Point b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Select the kth largest element in the array.
     *
     * @param input_arr The input array from which to perform the search.
     * @param k The k-th number to select from the array.
     */
    public static double quickSelect(double[] input_arr, int k) {
        if (input_arr == null)
            throw new Error();
        if (input_arr.length == k) {
            double max = Double.MIN_VALUE;
            for (double anInput_arr : input_arr)
                if (max < anInput_arr)
                    max = anInput_arr;
            return max;
        }
        if (input_arr.length < k)
            throw new Error();

        // copy to new array
        double[] arr = new double[input_arr.length];
        System.arraycopy(input_arr, 0, arr, 0, arr.length);

        int from = 0, to = arr.length - 1;

        // if from == to we reached the kth element
        while (from < to) {
            int r = from, w = to;
            double mid = arr[(r + w) / 2];

            // stop if the reader and writer meets
            while (r < w) {
                if (arr[r] >= mid) { // put the large values at the end
                    double tmp = arr[w];
                    arr[w] = arr[r];
                    arr[r] = tmp;
                    w--;
                } else { // the value is smaller than the pivot, skip
                    r++;
                }
            }

            // if we stepped up (r++) we need to step one down
            if (arr[r] > mid)
                r--;

            // the r pointer is on the end of the first k elements
            if (k <= r) {
                to = r;
            } else {
                from = r + 1;
            }
        }
        return arr[k];
    }
}
