package pppp.sim;

public class Point {

	public final double x;
	public final double y;

	public Point(double x, double y)
	{
		this.x = x;
		this.y = y;
	}

	public double distance(Point p)
	{
		return Math.hypot(x - p.x, y - p.y);
	}
}
