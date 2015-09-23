/********************************************
 * Group 9
 * Artur Renault, Jing Guo, Cathy Jin
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

	private int id = -1;
	private double density = 0;
	private double density_threshold = 0.3;
	private int side = 0;
	//private int piper_at_door = -1;
	private int[] pos_index = null;
	private int[] near_pos_index;
	private int[] far_pos_index;
	private int[] sweep_pos_index;
	private Point door_pos = null;
	private Point[][] pos = null;
	private Random gen = new Random();
	private Point[] piper_rats;
	private Point[][] near_pos;
	private Point[][] far_pos;
	private int with_rat_threshold = 2;
	private Point[][] sweep_pos;
	private boolean[] switchStrategy;

	// create move towards specified destination
	private static Move move(Point src, Point dst, boolean play)
	{
		double dx = dst.x - src.x;
		double dy = dst.y- src.y;
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
		density = rats.length / (double) side;
		pos = new Point [n_pipers][4];
		pos_index = new int [n_pipers];
		near_pos_index = new int[n_pipers];
		far_pos_index = new int[n_pipers];
		piper_rats = new Point[n_pipers];
		
		near_pos = new Point[n_pipers][4];
		far_pos = new Point[n_pipers][5];
		
		switchStrategy = new boolean[n_pipers];
		
		sweep_pos = new Point[n_pipers][4];
		sweep_pos_index = new int[n_pipers];
		

		for (int p = 0 ; p != n_pipers ; ++p) {
			switchStrategy[p] = false;
			// spread out at the door level
			double door = 0.0;
			if (n_pipers != 1) door = p * 1.8 / (n_pipers - 1) - 0.9;
			// pick coordinate based on where the player is
			boolean neg_y = id == 2 || id == 3;
			boolean swap  = id == 1 || id == 3;
			// first and third position is at the door
			door_pos = sweep_pos[p][0] = pos[p][0] = pos[p][2] = point(door, side * 0.5, neg_y, swap);
			// second position is chosen randomly in the rat moving area
			pos[p][1] = null;
			
			//TODO to decide
			pos[p][3] = point(door, side * 0.5 + 2, neg_y, swap);
			
			// sweep positions
			int x = 0;
			if (n_pipers != 0)
				x = (side / (n_pipers + 1)) * (p + 1) - side / 2; 
			sweep_pos[p][1] = point(x, side * 0.1, neg_y, swap);
			sweep_pos[p][2] = point(0, side * 0.5 - 2, neg_y, swap);
			sweep_pos[p][3] = point(0, side * 0.5 + 2, neg_y, swap);
			sweep_pos_index[p] = 0;

			// start with first position
			// dense positions
			if (density > density_threshold)
				pos_index[p] = 1;
			else
				pos_index[p] = 0;

			far_pos_index[p] = 0;
			near_pos_index[p] = 0;
			if(p % 2 == 0){
				far_pos[p][0] = door_pos; 
				if(p < n_pipers/2){
					far_pos[p][1] = point(-side / 4, -side / 2 + 10, neg_y, swap);
					far_pos[p][2] = point(-side / 2 + 10, -side / 4, neg_y, swap);
					far_pos[p][3] = point(-side / 4, 0, neg_y, swap);
				} else {
					far_pos[p][1] = point(side / 4, -side / 2 + 10, neg_y, swap);
					far_pos[p][2] = point(side / 2 - 10, -side / 4, neg_y, swap);
					far_pos[p][3] = point(side / 4, 0, neg_y, swap);
				}
			} else{
				near_pos[p][0] = near_pos[p][2] = door_pos;
				if(p <= n_pipers/2){
					near_pos[p][1] = point(-side / 4, 0, neg_y, swap);
					near_pos[p][3] = point(door, side * 0.5 + 2, neg_y, swap);
				}else{
					near_pos[p][1] = point(side / 4, 0, neg_y, swap);
					near_pos[p][3] = point(door, side * 0.5 + 2, neg_y, swap);
				}
			}
		}
	}
	public static void debug(Point point){
		System.out.println(" point " + point.x + ", " + point.y);
	}
	// return next locations on last argument
	public void play(Point[][] pipers, boolean[][] pipers_played,
			Point[] rats, Move[] moves)
	{
		density = rats.length / (double) side;

		for (int p = 0; p < pipers[id].length; p++) {
			if(density > density_threshold){
				if (!switchStrategy[p])
					sweepStrategy(pipers, pipers_played, rats, moves);
				else 
					denseStrategy(pipers, pipers_played, rats, moves);
				//if(pipers[id].length >= 4)
				//	denseStrategy(pipers, pipers_played, rats, moves);
			}
			else 
				sparseStrategy(pipers,pipers_played, rats, moves);
		}
	}


	private void sweepStrategy(Point[][] pipers, boolean[][] pipers_played, Point[] rats, Move[] moves) {
		for (int p = 0 ; p != pipers[id].length ; ++p) {
			Point src = pipers[id][p];
			Point dst = sweep_pos[p][sweep_pos_index[p]];
			
			// if position is reached
			if ( Math.abs(src.x - dst.x) < 0.000001 &&
					Math.abs(src.y - dst.y) < 0.000001) {
				if (sweep_pos_index[p] < 3) {
					sweep_pos_index[p]++;
				}
				else {
					if(withRats_door(src, rats)){
						moves[p] = move(src, src, true);
						continue;
					}
					else {
						switchStrategy[p] = true;
					}
				}
			}
			else {
				moves[p] = move(src, dst, sweep_pos_index[p] > 1);
			}
		}
	}
	
	private void sparseStrategy(Point[][] pipers, boolean[][] pipers_played,
			Point[] rats, Move[] moves) {
		for (int p = 0 ; p != pipers[id].length ; ++p) {
			Point src = pipers[id][p];	

			if (pos_index[p] == 2 && !withRats(src, rats)) {
				pos_index[p] = 1;
			}

			Point dst = pos[p][pos_index[p]];

			if (dst == null) dst = getNearestRat(src, rats, p);

			// if position is reached
			if ( Math.abs(src.x - dst.x) < 0.000001 &&
					Math.abs(src.y - dst.y) < 0.000001 ||

					// Or the piper has caught his rat
					pos_index[p] == 1 && within(src, piper_rats[p], with_rat_threshold) ||

					// Or this guy is at the door and the rats at the door have been captured
				    pos_index[p] == 3 && !withRats_door(door_pos, rats)) { 

				// get next position
				++pos_index[p];

				if (pos_index[p] == 4) {
					//TODO
					if(withRats_door(src, rats)){
						//System.out.println("piper " + p + "should wait");
						//piper_at_door = p;
						moves[p] = move(src, src, true);
						pos_index[p] = 3;
						continue;
						}
					else {
						pos_index[p] = 0;
					}
						
//					if (piper_at_door >= 0 && piper_at_door != p) {
//						pos_index[p] = 1;
//					} else {
//						piper_at_door = p;
//					}
				}

				if (pos_index[p] == 1) {
					piper_rats[p] = null;
				}

				dst = pos[p][pos_index[p]];
				// generate a new position if random
				if (dst == null) {
					dst = getNearestRat(src, rats, p);
					if (within(dst, door_pos, 10)) {
						// if this returned a rat within the door, that must mean
						// all rats are at his door. make all pipers bring rats in
						pos_index[p] = 3; 
					}
				}
			}
			// get move towards position
			moves[p] = move(src, dst, pos_index[p] > 1);
		}
	}

	public void denseStrategy(Point[][] pipers, boolean[][] pipers_played,
			Point[] rats, Move[] moves){
		for(int p = 0; p < pipers[id].length; p++){
			Point src = pipers[id][p];
			Point dst;
			if(p % 2 == 0){
				dst = far_pos[p][far_pos_index[p]];
			}else{
				dst = near_pos[p][near_pos_index[p]];
			}
			if( Math.abs(src.x - dst.x) < 0.000001 &&
					Math.abs(src.y - dst.y) < 0.000001){
				if(p % 2 == 0){

					if(far_pos_index[p] == 3){
						if(!samePos(src, pipers[id][p + 1])){
							moves[p] = move(src, src, true);
							continue;
						}
						far_pos_index[p] = 1;
					}
					else {
						++far_pos_index[p];
					}

					dst = far_pos[p][far_pos_index[p]];

				}else{
					if(near_pos_index[p] == 3){
						if(withRats(src, rats)){
							moves[p] = move(src, src, true);
							continue;
						}
						near_pos_index[p] = 0;
					}
					else {
						++near_pos_index[p];
					}
					dst = near_pos[p][near_pos_index[p]];	
				}
			}
			if(p % 2 == 0){
				moves[p] = move(src, dst, far_pos_index[p] > 1);
			}else{
				moves[p] = move(src, dst, near_pos_index[p] > 1);
			}

		}
	}
	public boolean samePos(Point p1, Point p2){
		if(Math.abs(p1.x - p2.x) < 0.0001 && Math.abs(p1.y - p2.y) < 0.0001) return true;
		return false;
	}
	public Point getNearestRat(Point piper_pos, Point[] rats, int piper_index){
		double min = Double.MAX_VALUE;
		int min_index = -1;
		for(int i = 0 ; i < rats.length; i++){

			// ignore rats near door if there is already a piper there 
			if (!within(door_pos, rats[i], 10)) {
			//if (piper_at_door < 0 || !within(door_pos, rats[i], 10)) {
				double dis = calDistance(piper_pos, rats[i]);

				// if piper_rats[piper_index] is null, then the piper just got out of the door
				if(dis < min && (piper_rats[piper_index] != null || dis > 10)){
					boolean already_assigned = false;

					// check that another player isn't already handling this rat
					for (int j = 0; j < piper_rats.length; j++) {
						if (piper_rats[j] != null && sameRat(rats[i], piper_rats[j])) {
							if (j == piper_index) {
								// This piper owns this rat, update position of rat
								piper_rats[j] = rats[i];
								return rats[i];

								// Otherwise someone else does
							} else {
								already_assigned = true;
								break;
							}
						}
					}

					// if nobody else is handling this rat, this piper can take it.
					if (!already_assigned) {
						min = dis;
						min_index = i;
					}
				} 
			}
		}
		if (min_index == -1) {
			min_index = gen.nextInt(rats.length);
		}
		piper_rats[piper_index] = rats[min_index];
		return rats[min_index];
	}
//
//	public boolean nearTeammate(Point piper_pos, Point[] other_pipers) {
//		for (int i = 0; i < other_pipers.length; i++) {
//			if(within(piper_pos, other_pipers[i]), ) 
//				return true;
//		}
//		return false;
//	}

	public boolean withRats(Point piper_pos, Point[] rats){ 
		for(int i = 0 ; i < rats.length; i++){
			if(within(piper_pos, rats[i], 3))
				return true;
		}
		return false;
	}
	public boolean withRats_door(Point piper_pos, Point[] rats){ 
		for(int i = 0 ; i < rats.length; i++){
			if(within(piper_pos, rats[i], 10))
				return true;
		}
		return false;
	}

	public boolean within(Point p1, Point p2, int distance){
		double length = calDistance(p1, p2);
		if(length <= distance)
			return true;
		return false;
	}

	public double calDistance(Point p1, Point p2){
		double dx = p1.x - p2.x;
		double dy = p1.y - p2.y;
		return Math.sqrt(dx * dx + dy * dy);
	}

	public boolean sameRat(Point p1, Point p2) {
		return calDistance(p1, p2) <= 1;
	}
}
