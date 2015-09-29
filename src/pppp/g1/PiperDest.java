package pppp.g1;

import pppp.sim.Point;

/**
 * Wraps a Point and a boolean which determines whether to play music on
 * the way to the point.
 */
public class PiperDest {
    public Point point;
    public boolean play;

    public PiperDest(Point point, boolean play) {
        this.point = point;
        this.play = play;
    }
}
