package pppp.g3;

import pppp.sim.Point;
import pppp.sim.Move;

public class FourPiperStrategy implements pppp.g3.Strategy {

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

    private int mode;

    private int magnetNumber = 0;
	private int sparseCutoff = 10;

	public void init(int id, int side, long turns,
	                 Point[][] pipers, Point[] rats){
        double theta;

		// storing variables
        this.id = id;
		this.side = side;
        this.turns = turns;
        this.mode = 0;

        // variables to rotate map
        neg_y = id == 2 || id == 3;
		swap  = id == 1 || id == 3;

        // create gate positions
        gateEntrance = Movement.makePoint(door, side * 0.5, neg_y, swap);
        insideGate = Movement.makePoint(door, side * 0.5 + 7.5, neg_y, swap);

        // create the state machines for the pipers
		numberOfPipers = pipers[id].length;
		piperStateMachine = new Point [numberOfPipers][];
		piperState = new int [numberOfPipers];

		for (int p = 0 ; p != numberOfPipers; ++p) {
            theta = Math.toRadians(p * 90.0/(numberOfPipers-1) + 45.0);
            piperStateMachine[p] = new Point [5];
			piperStateMachine[p][0] = gateEntrance;
            piperStateMachine[p][1] = Movement.makePoint(side/2 * Math.cos(theta), side/2 * (1 - Math.sin(theta)), neg_y, swap);
            piperStateMachine[p][2] = gateEntrance;
            piperStateMachine[p][3] = insideGate;
            piperStateMachine[p][4] = gateEntrance;
            piperState[p] = 0;
		}
	}

	public void play(Point[][] pipers, boolean[][] pipers_played,
	                 Point[] rats, Move[] moves) {
        try {
            int hNumber = 1;
            for (int p = 0; p != pipers[id].length; ++p) {
                Point src = pipers[id][p];
                Point dst = piperStateMachine[p][piperState[p]];
                int state = piperState[p];
                if (this.mode == 0) { 
                    // if position is reached
                    if (Math.abs(src.x - dst.x) < 0.000001 &&
                        Math.abs(src.y - dst.y) < 0.000001) {
                        // discard random position
                        // get next position
                        if (++piperState[p] == (piperStateMachine[p].length)) {
                            piperState[p] = 0;
                            piperStateMachine[p] = createHunterStateMachine();
                            this.mode = 1;
                        }
                        dst = piperStateMachine[p][piperState[p]];
                        // generate a new position if random
                    }
                    // get move towards position
                    moves[p] = Movement.makeMove(src, dst, piperState[p] > 1);
                } else {
                    boolean play = false;
                    if(state == 0){
                        if (isWithinDistance(src, gateEntrance, 0.00001) || isInArena(src)) {
                            piperState[p] = 1;
                            dst = findNearestRatForHunter(src, pipers, rats, hNumber, numberOfPipers, p);
                            piperStateMachine[p][1] = dst;
                        }
                        play = false;
                    } else if (state == 1) {
                        if(isWithinDistance(src, dst, 2.5)){
                            dst = returnToSender(pipers, p, rats);
                            if(dst == null){
                                System.out.println("Error");
                            }
                            if(dst == gateEntrance) {
                                // if we are returning to gate set state to 2
                                piperState[p] = 2;
                            } else {
                                // if we are going to magnets set state to 4
                                piperState[p] = 4;
                                piperStateMachine[p][4] = dst;
                            }
                            play = true;
                        } else {
                            // if we haven't reached the destination recompute the closest rat
                            dst = findNearestRatForHunter(src, pipers, rats, hNumber, numberOfPipers, p);
                            if(dst == null){
                                System.out.println("Error2");
                            }
                            piperStateMachine[p][1] = dst;
                            play = false;
                        }
                    } else if (state == 2) {
                        if(isWithinDistance(src, gateEntrance, 0.00001)){
                            piperState[p] = 3;
                            dst = insideGate;
                        } else if (noRatsAreWithinRange(src, rats, 7.5)){
                            piperState[p] = 1;
                            dst = findNearestRatForHunter(src, pipers, rats, hNumber, numberOfPipers, p);
                            piperStateMachine[p][1] = dst;
                        }
                        play = true;
                    } else if (state == 3) {
                        if(isWithinDistance(src, insideGate, 0.00001) || noRatsAreWithinRange(src, rats, 7.5)){
                            piperState[p] = 0;
                            dst = gateEntrance;
                        }
                        play = true;
                    } else if (state == 4) {
                        if(isWithinDistance(src, dst, 2.5) || noRatsAreWithinRange(src, rats, 7.5)){
                            // we are only trying to get within 2.5m of the magnets
                            piperState[p] = 1;
                            dst = findNearestRatForHunter(src, pipers, rats, hNumber, numberOfPipers, p);
                            piperStateMachine[p][1] = dst;
                        } else{
                            // if we haven't reached the destination redecide whether to go to
                            // the gate or the magnets
                            dst = returnToSender(pipers, p, rats);
                            if(dst == gateEntrance) {
                                // if we are returning to gate set state to 2
                                piperState[p] = 2;
                            } else {
                                // if we are going to magnets set state to 4
                                piperStateMachine[p][4] = dst;
                            }
                            play = true;
                        }
                    } else {
                        System.out.println("Piper " + p + " is in state " + state);
                    }
                    hNumber++;
                    if (dst == null) {
                        System.out.println("Piper " + p + " is in state " + state);
                    }
                    moves[p] = Movement.makeMove(src, dst, play);
                }
            }

            // for (int p = 0; p != pipers[id].length; ++p) {

            //     Point src = pipers[id][p];
            //     Point dst = piperStateMachine[p][piperState[p]];

            //     if (isWithinDistance(src, dst, 0.00001)) {
            //         ++piperState[p];
            //         piperState[p] = piperState[p] % piperStateMachine[p].length;
            //         dst = piperStateMachine[p][piperState[p]];
            //     }
            //     int state = piperState[p];

            //     moves[p] = Movement.makeMove(src, dst, play(state));
            // }
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Point[] createHunterStateMachine() {
        // Hunters have a 5 state machine
        Point[] pos = new Point [5];

        // go to gate entrance
        pos[0] = gateEntrance;

        // chase rat
        pos[1] = null;

        // bring rat to gate
        pos[2] = pos[0];

        // move inside gate
        pos[3] = insideGate;

        // bring rat to magnets
        pos[4] = null;

        return pos;
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

    private Point returnToSender(Point[][] pipers, int p, Point[] rats){
        return gateEntrance;
    }

    public Point findNearestRatForHunter(Point hunterPos, Point[][] pipers, Point[] rats, int hunterNumber, int numberOfPipers, int piper){
        if(numberOfPipers + 1 > rats.length){
            return findClosest(hunterPos, rats, 1)[0];
        }

        Point closestRat = null;
        double closestDistance = Double.MAX_VALUE;
        int ind = -1;
        Point rat;
        for(int i = 0; i < rats.length; ++i){
            if(rats[i] == null){ continue; }
            if(i % (hunterNumber) != 0){
                continue;
            }
            rat = rats[i];
            if(isNearMagnet(rat, pipers, rats, 2.5, piper) || isNearGate(rat, 0.0001)){
                continue;
            }
            double distanceToRat = Movement.distance(hunterPos, rat);
            if(distanceToRat < closestDistance){
                closestRat = rat;
                closestDistance = distanceToRat;
                ind = i;
            }
        }
        if(closestRat != null){
            //rats[ind] = null;
            return closestRat;
        } else {
            return findClosest(hunterPos, rats, 1)[0];
        }
    }

    public boolean isNearGate(Point p, double distance){
        return true;
    }

    public boolean isNearMagnet(Point rat, Point[][] pipers, Point[] rats, double distance, int piper){
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

    // returns array of closest points, ordered by decreasing distance
    public Point[] findClosest(Point start, Point[] ends, int n) {
        if (n <= ends.length) {
            return ends;
        }
        Point[] closestPoints = new Point[n];
        for (int i = 0; i < n; i++) {
            closestPoints[i] = ends[i];
        }

        double c_dist;
        double largest_distance;
        int largest;

        for (int i = n; i < ends.length; i++) {

            //First find largest of smallest
            largest = 0;
            largest_distance = Movement.distance(start, closestPoints[largest]);

            for (int j = 1; j < n; j++) {
                c_dist = Movement.distance(start, closestPoints[j]);
                if (largest_distance < c_dist) {
                    largest_distance = c_dist;
                    largest = j;
                }
            }

            c_dist = Movement.distance(start, ends[i]);
            if (c_dist < largest_distance) {
                closestPoints[largest] = ends[i];
            }
        }
        return closestPoints;
    }

    private double distanceFromPointToLine(Point p, Point lineStart, Point lineEnd) {
        return Math.abs((lineEnd.y-lineStart.y)*p.x - (lineEnd.x-lineStart.x)*p.y + lineEnd.x*lineStart.y - lineEnd.y*lineStart.x) / Math.sqrt(Math.pow(lineEnd.y-lineStart.y, 2) + Math.pow(lineEnd.x-lineStart.x, 2));
    }

    private int numberOfRatsOnLine(Point lineStart, Point lineEnd, Point[] rats, double maxDist) {
        int count = 0;
        for (Point rat : rats) {
            if (distanceFromPointToLine(rat, lineStart, lineEnd) <= maxDist) {
                count++;
            }
        }
        return count;
    }

    private Point findBestDestination(Point[] rats) {
        int bestCount = 0;
        int bestAngle = -1;
        Point bestEnd = null;
        for (int i=-45; i < 45; i++) {
            double x = Math.sin(Math.toRadians(i))*(-side * 0.4);
            if (x > side/2) {
                x = side/2;
            } else if (x < -side/2) {
                x = -side/2;
            }
            double y = Math.cos(Math.toRadians(i))*(-side * 0.4);
            Point end = Movement.makePoint(x, y, neg_y, swap);
            int num = numberOfRatsOnLine(gateEntrance, end, rats, 10);
            if (num > bestCount) {
                bestCount = num;
                bestEnd = end;
                bestAngle = i;
            }
        }
        return bestEnd;
    }

    private int findBestAngle(Point[] rats) {

        int bestCount = 0;
        int bestAngle = -1;
        Point bestEnd = null;
        for (int i=-90; i < 90; i++) {
            double x = Math.sin(Math.toRadians(i))*(-side * 0.4);
            if (x > side/2) {
                x = side/2;
            } else if (x < -side/2) {
                x = -side/2;
            }
            double y = Math.cos(Math.toRadians(i))*(-side * 0.4);
            Point end = Movement.makePoint(x, y, neg_y, swap);
            int num = numberOfRatsOnLine(gateEntrance, end, rats, 5);
            if (num > bestCount) {
                bestCount = num;
                bestEnd = end;
                bestAngle = i;
            }
        }
        return bestAngle;
    }

 //    private boolean play(int state){
 //        return (state  >= 2);
 //    }

 //    private boolean isWithinDistance(Point src, Point dst, double error){
 //        if(src != null && dst != null &&
 //                // checking if we are within a minimum distance of the destination
 //                Math.abs(src.x - dst.x) < error &&
 //                Math.abs(src.y - dst.y) < error){
 //            return true;
 //        }

 //        return false;
 //    }
}
