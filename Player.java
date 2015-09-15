package pppp.g0;

import pppp.sim.Point;
import pppp.sim.Move;

import java.util.*;

public class Player implements pppp.sim.Player {

	// see details below
	private int id = -1;
	private int side = 0;
	private int[] pos_index = null;
	private Point[][] pos = null;
	private Point[] random_pos = null;
	private Random gen = new Random();

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
		random_pos = new Point [n_pipers];
		pos_index = new int [n_pipers];
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
			pos[p][3] = point(door * -6, side * 0.5 + 3, neg_y, swap);
			pos[p][4] = point(door * +6, side * 0.5 + 3, neg_y, swap);
			// start with first position
			pos_index[p] = 0;
		}
	}

	// return next locations on last argument
	public void play(Point[][] pipers, boolean[][] pipers_played,
	                 Point[] rats, Move[] moves)
	{
		for (int p = 0 ; p != pipers[id].length ; ++p) {
			Point src = pipers[id][p];
			Point dst = pos[p][pos_index[p]];
			// if null then get random position
			if (dst == null) dst = random_pos[p];
			// if position is reached
			if (Math.abs(src.x - dst.x) < 0.000001 &&
			    Math.abs(src.y - dst.y) < 0.000001) {
				// discard random position
				if (dst == random_pos[p]) random_pos[p] = null;
				// get next position
				if (++pos_index[p] == pos[p].length) pos_index[p] = 0;
				dst = pos[p][pos_index[p]];
				// generate a new position if random
				if (dst == null) {
					double x = (gen.nextDouble() - 0.5) * side * 0.9;
					double y = (gen.nextDouble() - 0.5) * side * 0.9;
					random_pos[p] = dst = new Point(x, y);
				}
			}
			// get move towards position
			moves[p] = move(src, dst, pos_index[p] > 1);
		}
	}
}
