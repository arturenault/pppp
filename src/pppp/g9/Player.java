/********************************************
 * Group 9
 * Artur Renault & Jing Guo
 *
 * Pied piper player class; based off g0.
 *
 * Implements a divide and conquer strategy:
 *  players go for the closest rat that is not
 *  assigned to a player and return to base.
 *
 *  If rats are lost during return, they attempt
 *  to find new rats.
 * *****************************************/

package pppp.g9;

import pppp.sim.Point;
import pppp.sim.Move;

import java.util.*;

public class Player implements pppp.sim.Player {

	// see details below
	private int id = -1;
	private int side = 0;
	private int[] pos_index = null;
	private Point[][] pos = null;
	private Point[] nearest_rat = null;
	private Random gen = new Random();
    private boolean[] rats_assigned;

	// create move towards specified destination
	private static Move move(Point src, Point dst, boolean play)
	{
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

	// generate point after negating or swapping coordinates
	private static Point point(double x, double y,
	                           boolean neg_y, boolean swap_xy)
	{
		if (neg_y) y = -y;
		return swap_xy ? new Point(y, x) : new Point(x, y);
	}

	// specify location that the player will alternate between
	public void init(int id, int side, long turns,
	                 Point[][] pipers, Point[] rats)
	{
		this.id = id;
		this.side = side;
		int n_pipers = pipers[id].length;
		pos = new Point [n_pipers][5];
		nearest_rat = new Point [n_pipers];
		pos_index = new int [n_pipers];
        rats_assigned = new boolean[rats.length];

		for (int p = 0 ; p != n_pipers ; ++p) {
			// spread out at the door level
			double door = 0.0;
			if (n_pipers != 1) door = p * 1.8 / (n_pipers - 1) - 0.9;
			// pick coordinate based on where the player is
			boolean neg_y = id == 2 || id == 3;
			boolean swap  = id == 1 || id == 3;
			// first and third position is at the door
			pos[p][0] = pos[p][2] = point(door, side * 0.5, neg_y, swap);
			// second position is chosen randomly in the rat moving area
			pos[p][1] = null;
			// fourth and fifth positions are outside the rat moving area
			pos[p][3] = p % 2 == 0 ? point(door * -6, side * 0.5 + 3, neg_y, swap) : point(door * +6, side * 0.5 + 3, neg_y, swap);
			pos[p][4] = p % 2 == 0 ? point(door * +6, side * 0.5 + 3, neg_y, swap) : point(door * -6, side * 0.5 + 3, neg_y, swap);
			// start with first position
			pos_index[p] = 0;
		}
	}

	// return next locations on last argument
	public void play(Point[][] pipers, boolean[][] pipers_played,
	                 Point[] rats, Move[] moves)
	{
        try {
        if (rats.length != rats_assigned.length) {
            rats_assigned = new boolean[rats.length];
        }
		for (int p = 0 ; p != pipers[id].length ; ++p) {
			Point src = pipers[id][p];	

            if (pos_index[p] == 2 && !withRats(src, rats)) {
                rats_assigned = new boolean[rats.length];
                pos_index[p] = 1;
            }

            Point dst = pos[p][pos_index[p]];

            if (dst == null) dst = nearest_rat[p];

            // if position is reached
			if ( Math.abs(src.x - dst.x) < 0.000001 &&
			    Math.abs(src.y - dst.y) < 0.000001) {
				// discard random position
				if (dst == nearest_rat[p]) nearest_rat[p] = getNearestRat(src, rats);
				// get next position
				if (++pos_index[p] == pos[p].length) pos_index[p] = 0;
				dst = pos[p][pos_index[p]];
				// generate a new position if random
				if (dst == null) {
					nearest_rat[p] = dst = getNearestRat(src, rats);
				}
			}
			// get move towards position
			moves[p] = move(src, dst, pos_index[p] > 1);
		}
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

	public Point getNearestRat(Point piper_pos, Point[] rats){
		double shortCut = 11;
		double min = Double.MAX_VALUE;
		int min_index = 0;
		for(int i = 0 ; i < rats.length; i++){
			double dis = calDistance(piper_pos, rats[i]);
			if(dis > 10){
				if(dis <= shortCut && !rats_assigned[i]) {
                    rats_assigned[i] = true;
					return rats[i];
                }
				if(dis < min && !rats_assigned[i]){
					rats_assigned[i] = true;
                    min = dis;
					min_index = i;
				}
			}	
		}
		//If there is no rats, return null;
		return rats[min_index];
	}

	public boolean withRats(Point piper_pos, Point[] rats){ 
		for(int i = 0 ; i < rats.length; i++){
			if(within(piper_pos, rats[i]))
				return true;
		}
		return false;
	}

	public boolean within(Point p1, Point p2){
		double length = calDistance(p1, p2);
		if(length <= 10)
			return true;
		return false;
	}

	public double calDistance(Point p1, Point p2){
		double dx = p1.x - p2.x;
		double dy = p1.y - p2.y;
		return Math.sqrt(dx * dx + dy * dy);
	}
}
