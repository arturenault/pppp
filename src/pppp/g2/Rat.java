package pppp.g2;

import pppp.sim.Point;

/**
 * Created by naman on 9/19/15.
 */
public class Rat {

    public int id;
    public int piperId;
    public boolean captured;
    public boolean hasEnemyCaptured;
    public Point prevLocation;
    public Point curLocation;

    public Rat(int id) {
        this.id = id;
        this.piperId = -1;
        this.captured = false;
        this.hasEnemyCaptured = false;
        this.prevLocation = null;
        this.curLocation = null;
    }

    public Rat(int id, Point curLocation) {
        this.id = id;
        this.piperId = -1;
        this.captured = false;
        this.hasEnemyCaptured = false;
        this.prevLocation = null;
        this.curLocation = curLocation;
    }

    public Rat(int id, Point curLocation, boolean captured, boolean hasEnemyCaptured) {
        this.id = id;
        this.piperId = -1;
        this.captured = captured;
        this.hasEnemyCaptured = hasEnemyCaptured;
        this.prevLocation = null;
        this.curLocation = curLocation;
    }

    public void captured(int piperId, boolean hasEnemyCaptured) {
        this.captured = true;
        this.piperId = piperId;
        this.hasEnemyCaptured = hasEnemyCaptured;
    }

    public void updateLocation(Point point) {
        this.prevLocation = this.curLocation;
        this.curLocation = point;
    }
}
