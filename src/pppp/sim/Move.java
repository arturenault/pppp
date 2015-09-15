package pppp.sim;

public class Move {

	public final double dx;
	public final double dy;

	public final boolean play;

	public Move(double dx, double dy, boolean play)
	{
		this.dx = dx;
		this.dy = dy;
		this.play = play;
	}
}
