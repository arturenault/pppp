package pppp.g3;

import pppp.sim.Point;
import pppp.sim.Move;

import pppp.g3.Strategy;

import java.lang.Double;
import java.lang.System;

public class AngularSweep implements pppp.g3.Strategy {

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

    private int numberOfHunters = 0;
    private int numberOfMagnets = 0;

    private int magnetNumber = 0;
	private int sparseCutoff = 10;

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
        insideGate = Movement.makePoint(door, side * 0.5 + 7.5, neg_y, swap);

        // create the state machines for the pipers
		numberOfPipers = pipers[id].length;
		piperStateMachine = new Point [numberOfPipers][];
		piperState = new int [numberOfPipers];

		for (int p = 0 ; p != numberOfPipers; ++p) {
			if (isLeftSweep(p)) {
				piperStateMachine[p] = createLeftSweepStateMachine();
			} else {
				piperStateMachine[p] = createRightSweepStateMachine();
			}
			piperState[p] = 0;
		}
	}

	public void play(Point[][] pipers, boolean[][] pipers_played,
	                 Point[] rats, Move[] moves) {
        try {
            for (int p = 0; p != pipers[id].length; ++p) {

                Point src = pipers[id][p];
                Point dst = piperStateMachine[p][piperState[p]];

                if (isWithinDistance(src, dst, 0.00001)) {
                    ++piperState[p];
                    piperState[p] = piperState[p] % piperStateMachine[p].length;
                    dst = piperStateMachine[p][piperState[p]];
                }
                int state = piperState[p];
                
                moves[p] = Movement.makeMove(src, dst, play(state));
            }
        }catch(NullPointerException e){
            e.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private boolean play(int state){
        return (state  >= 2);
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

	private boolean isLeftSweep(int p){
		return p < numberOfPipers/2;
	}

	private Point[] createLeftSweepStateMachine() {
		// magnet pipers have 4 states
		Point[] pos = new Point [5];

        // go to gate entrance
		pos[0] = gateEntrance;

        // go to opposite gate
		pos[1] = Movement.makePoint(-30, -25, neg_y, swap);

        //go back to gate entrance
		pos[2] = Movement.makePoint(-10, 25, neg_y, swap);

        pos[3] = gateEntrance;

        // Move inside the gate to deposit rats
		pos[4] = insideGate;

        //pos[4] = pos[3]; // figure out waiting
		return pos;
	}

	private Point[] createRightSweepStateMachine() {
        // magnet pipers have 4 states
        Point[] pos = new Point [5];

        // go to gate entrance
        pos[0] = gateEntrance;

        // go to opposite gate
        pos[1] = Movement.makePoint(30, -25, neg_y, swap);

        //go back to gate entrance
        pos[2] = Movement.makePoint(10, 25, neg_y, swap);

        pos[3] = gateEntrance;

        // Move inside the gate to deposit rats
        pos[4] = insideGate;

        //pos[4] = pos[3]; // figure out waiting
        return pos;
	}

}