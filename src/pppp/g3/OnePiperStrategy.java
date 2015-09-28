package pppp.g3;

import pppp.sim.Point;
import pppp.sim.Move;

import pppp.g3.Strategy;

import java.lang.Double;
import java.lang.System;

public class OnePiperStrategy implements pppp.g3.Strategy {

    public static final int PIPER_RADIUS = 10;
    public static final int PIPER_RUN_SPEED = 5; 
    public static final int PIPER_WALK_SPEED = 1;

    //Because in this class p is always 0.
    private static int p = 0;

    private int id = -1;
    private int side = 0;
    private long turns = 0;
    private int numberOfPipers = 0;
    private int[] piperState = null;
    private Point[][] piperStateMachine = null;
    private double door = 0.0;
    private boolean neg_y;
    private boolean swap;

    private Point gateEntrance = null;
    private Point insideGate = null;
    private Point justOutsideGate = null;

    private int numberOfHunters = 0;
    private int sparseCutoff = 10;

    private int initNumberOfRats;

    public void init(int id, int side, long turns,
                     Point[][] pipers, Point[] rats){
        // storing variables
        this.id = id;
        this.side = side;
        this.turns = turns;

        // variables to rotate map
        neg_y = id == 2 || id == 3;
        swap  = id == 1 || id == 3;

        // create gate positions
        gateEntrance = Movement.makePoint(door, side * 0.5 - 5, neg_y, swap);
        insideGate = Movement.makePoint(door, side * 0.5 + 7.5, neg_y, swap);
        justOutsideGate = Movement.makePoint(door, side * 0.5 - 10, neg_y, swap);

        // create the state machines for the pipers
        numberOfPipers = pipers[id].length;
        piperStateMachine = new Point [numberOfPipers][];
        piperState = new int[numberOfPipers];
        initNumberOfRats = rats.length;

        //There's just one piper, so every index is 0
        piperStateMachine[0] = createHunterStateMachine();
        piperState[0] = 0;
    }

    public void play(Point[][] pipers, boolean[][] pipers_played,
                     Point[] rats, Move[] moves) {
        Point dst, src;

        try {
            int state = piperState[p];
            if (state == 4 && !noRatsAreWithinRange(pipers[id][p], rats, PIPER_RADIUS)) {
                dst = piperStateMachine[p][piperState[p]];
                src = pipers[id][p];
                moves[p] = Movement.makeMove(src, dst, play(state));
                return;
            }

            int currentNumberOfRats = rats.length; 
            // double y_depth = 10 + side * (1 - (((double) currentNumberOfRats / initNumberOfRats)));
            // double justOutsideGate_y = side * 0.5 - y_depth;

            // piperStateMachine[p][1] = Movement.makePoint(door, justOutsideGate_y, neg_y, swap);

            state = piperState[p];
            //Chase down any lost rats
            if (state == 4 && noRatsAreWithinRange(pipers[id][p], rats, 10)) {
                piperState[p] = 0;
            }
            else if (state == 3 && noRatsAreWithinRange(pipers[id][p], rats, 10)) {
                piperState[p] = 2;
            }
            else if (state == 2) {
                piperStateMachine[p][piperState[p]] = densestPoint(pipers, pipers_played, rats);
            }

            src = pipers[id][p];
            dst = piperStateMachine[p][piperState[p]];

            if (state == 2 && isWithinDistance(src, dst, 3)) {
                ++piperState[p];
                piperState[p] = piperState[p] % piperStateMachine[p].length;
                dst = piperStateMachine[p][piperState[p]];
            }

            else if (isWithinDistance(src, dst, 0.05)) {
                ++piperState[p];
                piperState[p] = piperState[p] % piperStateMachine[p].length;
                dst = piperStateMachine[p][piperState[p]];
            }
            state = piperState[p];
            moves[p] = Movement.makeMove(src, dst, play(state));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean play(int state) {
        return (state  >= 3);
    }

    private boolean isWithinDistance(Point src, Point dst, double error){
        if(src != null && dst != null &&
                // checking if we are within a minimum distance of the destination
                Math.abs(src.x - dst.x) < error &&
                Math.abs(src.y - dst.y) < error){
            return true;
        }

        return false;
    }

    private boolean isInArena(Point pos){
        return (Math.abs(pos.x) < side/2 && Math.abs(pos.y) < side/2);
    }

    public boolean isNearGate(Point p, double distance){
        return (Movement.distance(p, gateEntrance) < distance);
    }

    private boolean noRatsAreWithinRange(Point piper, Point[] rats, double distance){
        for(Point rat:rats){
            if(rat == null){
                continue;
            }
            if(Movement.distance(piper, rat) < distance){
                return false;
            }
        }
        return true;
    }

    /*
    * Also needs to consider how far away this point is from the gate. Basically a cost function.
    */
    private Point densestPoint(Point[][] pipers, boolean[][] pipers_played,
                     Point[] rats) {

        Point thisPiper = pipers[id][p];
        Point densest = Movement.makePoint(0, 0, neg_y, swap);
        double bestReward = 0;

        //Go through candidate points and find point with 
        for (int i = - side/2; i < side/2; i = i+side/10) {
            for (int j = -side/2; j < side/2; j = j+side/10) {
                Point p = Movement.makePoint(i, j, neg_y, swap);

                double distanceFromPiperToPoint = PIPER_WALK_SPEED * Movement.distance(p, thisPiper);
                double distanceToGate = PIPER_RUN_SPEED * Movement.distance(p, gateEntrance);
                int numberOfRatsNearPoint = (int) Math.pow(numberOfRatsWithinXMetersOfPoint(p, 
                    PIPER_RADIUS, rats), 2);

                double reward = numberOfRatsNearPoint / (distanceFromPiperToPoint + distanceToGate);

                if (reward > bestReward) {
                    bestReward = reward;
                    densest = p;
                }
            }
        }
        return densest;
    }

    /*
     * Here's some documentation to explain a function even though it explains itself
     */
    private int numberOfRatsWithinXMetersOfPoint(Point p, double x, Point[] rats) {
        int result = 0;
        for (Point rat : rats) {
            double distanceFromPointToRat = Movement.distance(p, rat);
            if (distanceFromPointToRat < x) {
                result++;
            }
        }
        return result;
    }

    // finds closest rat in direction away from the gate
    public Point findClosest(Point start, Point[] ends) {
        double c_dist;
        double dist_from_gate;
        double closest_distance = Double.MAX_VALUE;
        int closest = 0;

        for (int i = 0; i < ends.length; i++) {
            dist_from_gate = Movement.distance(gateEntrance, ends[i]);
            c_dist = Movement.distance(start, ends[i]);

            if (c_dist < closest_distance && dist_from_gate > 10) {
                closest = i;
                closest_distance = c_dist;
            }
        }
        return ends[closest];
    }

    private Point[] createHunterStateMachine() {
        // Hunters have a 5 state machine
        Point[] pos = new Point [5];

        // go to gate entrance
        pos[0] = gateEntrance;
        // go just outside gate
        pos[1] = justOutsideGate;
        //Now find closest rat
        pos[2] = justOutsideGate;
        // move inside gate
        pos[3] = gateEntrance;

        pos[4] = insideGate;

        return pos;
    }
}