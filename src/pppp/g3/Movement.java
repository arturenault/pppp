package pppp.g3;

import pppp.sim.Point;
import pppp.sim.Move;

public class Movement {

	// generate point after negating or swapping coordinates
	public static Point makePoint(double x, double y, boolean neg_y, boolean swap_xy)
	{
		if (neg_y) {
			y = -y;
		}
		return swap_xy ? new Point(y, x) : new Point(x, y);
	}

	// create move towards specified destination
	public static Move makeMove(Point src, Point dst, boolean play)
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

	public static double distance(Point a, Point b){
		return Math.hypot(a.x - b.x, a.y - b.y);
	}

}