package pppp.g3;

import pppp.sim.Point;
import pppp.sim.Move;

import pppp.g3.Strategy;

import java.lang.Double;
import java.lang.System;
import java.util.Arrays;

public class MidPiperStrategy implements pppp.g3.Strategy {

    public static final int PIPER_RADIUS = 10;
    public static final float PIPER_RUN_SPEED = 2.5f;
    public static final int PIPER_WALK_SPEED = 1;

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
    private Point outsideGate = null;


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
        gateEntrance = Movement.makePoint(door, side * 0.5, neg_y, swap);
        insideGate = Movement.makePoint(door, side * 0.5 + 5, neg_y, swap);
        outsideGate = Movement.makePoint(door, side * 0.5 - 5, neg_y, swap);

        // create the state machines for the pipers
        numberOfPipers = pipers[id].length;
        piperStateMachine = new Point [numberOfPipers][];
        piperState = new int [numberOfPipers];

        for (int p = 0 ; p != numberOfPipers; ++p) {
            piperStateMachine[p] = generateStateMachine(p, rats);

            piperState[p] = 0;
        }
    }

    public void play(Point[][] pipers, boolean[][] pipers_played,
                     Point[] rats, Move[] moves) {
        Point src, dst;

        //sortRats(rats);
        //short[] pipersAssignedToRat = new short[rats.length];

        int state;

        for(int p = 0; p < numberOfPipers; p++){
            state = piperState[p];
            boolean play = false;
            src = pipers[id][p];
            dst = piperStateMachine[p][state];

            //System.err.println(p + ":" + state);

            if(state == 0){
                if(isWithinDistance(src, dst, 0.00001)){
                    piperState[p] = state = 1;
                    dst = piperStateMachine[p][state];
                }
            }

            else if (state == 1) {
                if(isWithinDistance(src, dst, 0.00001)){
                    piperState[p] = state = 3;
                    dst = src;
                    play = true;
                }
            }

            else if (state == 2) {
                if(isWithinDistance(src, dst, 0.00001)){
                    piperState[p] = state = 3;
                    dst = piperStateMachine[p][state];
                }
                play = true;

            }

            else if (state == 3) {
                if(isWithinDistance(src, dst, 0.00001)){
                    piperState[p] = state = 4;
                    dst = piperStateMachine[p][state];
                }
                play = true;
            }

            else if (state == 4) {
                if(isWithinDistance(src, dst, 0.00001)){
                    piperState[p] = state = 5;
                    dst = piperStateMachine[p][state];
                    play = true;
                }
                play = true;
            }

            else if (state == 5) {
                if(isWithinDistance(src, dst, 0.00001) && noRatsAreWithinRange(pipers[id][p], rats, 10)){
                    piperState[p] = state = 7;
                    dst = src;
                }
                play = true;
            }

            else if (state == 6) {
                if(isWithinDistance(src, dst, 0.00001)){
                    piperState[p] = state = 7;
                    dst = piperStateMachine[p][state];
                }
                play = true;
            }

            else if (state == 7) {
                dst = gateEntrance;
                if (isWithinDistance(src, dst, 0.00001)) {
                    piperState[p] = state = 8;
                    dst = densestPoint(pipers, pipers_played, rats, p);
                    piperStateMachine[p][8] = dst;
                }

            }

            else if (state == 8) {
                if(isWithinDistance(src, dst, 0.001)){
                    piperState[p] = state = 9;
                    dst = piperStateMachine[p][state];
                    play = true;
                } else {
                    dst = densestPoint(pipers, pipers_played, rats, p);
                    piperStateMachine[p][8] = dst;
                }
            }

            else if (state == 9) {
                if(isWithinDistance(src, dst, 0.00001)){
                    piperState[p] = state = 10;
                    dst = piperStateMachine[p][state];
                    play = true;
                }
                else if(noRatsAreWithinRange(pipers[id][p], rats, 5)){
                    piperState[p] = state = 8;
                    dst = densestPoint(pipers, pipers_played, rats, p);
                    piperStateMachine[p][8] = dst;
                }
                play = true;
            } else if (state == 10) {
                if(isWithinDistance(src, dst, 0.00001) && noRatsAreWithinRange(pipers[id][p], rats, 10)){
                    piperState[p] = state = 7;
                    dst = piperStateMachine[p][state];
                }

                play = true;
            } else {
                System.out.println("Piper " + p + " is in state " + state);
            }

            //System.err.println(Arrays.toString(pipersAssignedToRat));

            if(isWithinDistance(src, dst, 0.00001) && state == 0){
                piperState[p] = ++piperState[p] % piperStateMachine[p].length;
                state = piperState[p];
                dst = piperStateMachine[p][state];
            }

            moves[p] = Movement.makeMove(src, dst, play);
        }
    }

    /*
    * Also needs to consider how far away this point is from the gate. Basically a cost function.
    */
    private Point densestPoint(Point[][] pipers, boolean[][] pipers_played,
                               Point[] rats, int p) {

        Point thisPiper = pipers[id][p];
        Point densest = Movement.makePoint(0, 0, neg_y, swap);
        double bestReward = 0;

        //Go through candidate points and find point with
        for (int i = - side/2; i < side/2; i = i+side/20) {
            for (int j = -side/2; j < side/2; j = j+side/20) {

                Point point = Movement.makePoint(i, j, neg_y, swap);

                double distanceFromPiperToPoint = PIPER_WALK_SPEED * Movement.distance(point, thisPiper);
                double distanceToGate = PIPER_RUN_SPEED * Movement.distance(point, gateEntrance);
                int numberOfRatsNearPoint = (int) Math.pow(numberOfRatsWithinXMetersOfPoint(point,
                        PIPER_RADIUS, rats, p), 2);
                double numberOfPipersNearPoint = Math.sqrt(numberOfPipersWithinXMetersOfPoint(point, pipers, PIPER_RADIUS));
                double reward = numberOfRatsNearPoint / (Math.pow(distanceFromPiperToPoint, 2) + distanceToGate) / (numberOfPipersNearPoint + 1);

                if (reward > bestReward) {
                    bestReward = reward;
                    densest = point;
                }
            }
        }
        return densest;
    }

    private Point densestPointInArea(Point[] rats, int p,
                               int minX, int maxX, int minY, int maxY) {

        Point densest = Movement.makePoint(0, 0, neg_y, swap);
        double bestReward = 0;
        //Go through candidate points and find point with
        for (int i = minX; i < maxX; i++) {
            for (int j = minY; j < maxY; j++) {
                Point point = Movement.makePoint(i, j, false, false);
                int numberOfRatsNearPoint = numberOfRatsWithinXMetersOfPoint(point,
                        PIPER_RADIUS, rats, p);

                double reward = numberOfRatsNearPoint;

                if (reward > bestReward) {
                    bestReward = reward;
                    densest = point;
                }
            }
        }
        return densest;
    }


    private int numberOfPipersWithinXMetersOfPoint(Point p, Point[][] pipers, double dist) {
        int result = 0;
        for(int i = 0; i < pipers.length; i++){
            if(i == id)
                continue;
            for(int j = 0; j < pipers[i].length; j++){
                if(Movement.distance(p, pipers[i][j]) < dist){
                    ++result;
                }
            }
        }
        return result;
    }


    /*
     * Here's some documentation to explain a function even though it explains itself
     */
    private int numberOfRatsWithinXMetersOfPoint(Point p, double x, Point[] rats, int piper) {
        int result = 0;
        for (Point rat : rats) {
            double distanceFromPointToRat = Movement.distance(p, rat);
            if (distanceFromPointToRat < x) {
                result++;
            }
        }
        return result;
    }


    private boolean allPipersAreAtLeastState(int state){
        for(int i = 0; i < numberOfPipers; i++){
            if(piperState[i] < state)
                return false;
        }
        return true;
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

    private Point findClosestPiper(Point[][] pipers, Point closestRat, int p){

        for(int i = 0; i < numberOfPipers; i++){
            if(i == p)
                continue;

            if(Movement.distance(pipers[id][p], closestRat) <= 5){
                return pipers[id][p];
            }
        }

        return null;
    }

    private Point[] generateStateMachine(int p, Point[] rats){

        Point[] states = new Point[11];

        states[0] = gateEntrance;

        double theta = Math.toRadians(p * 45.0/(numberOfPipers - 1) + 67.5);

        Point min, max;

        min = Movement.makePoint(-side/2 + p * side/numberOfPipers, 0, neg_y, swap);
        max = Movement.makePoint(-side/2 + (p+1) * side/numberOfPipers, side/2, neg_y, swap);


        states[1] = densestPointInArea(rats, p, (int) Math.min(min.x, max.x), (int) Math.max(min.x, max.x), (int) Math.min(min.y, max.y), (int) Math.max(min.y, max.y));
        //states[1] = Movement.makePoint(side/2 * Math.cos(theta), side/2 - (side * 0.4 * Math.sin(theta)), neg_y, swap);

        states[2] = null;

        states[3] = outsideGate;

        states[4] = gateEntrance;

        states[5] = insideGate;

        states[6] = null;

        states[7] = gateEntrance;

        states[8] = null;

        states[9] = gateEntrance;

        states[10] = insideGate;

        return states;

    }
}