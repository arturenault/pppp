package pppp.sim;

public interface Player {

	// id -> id of player (0 if north, 1 if east, 2 if south, 3 if west)
	// side -> side of square (in meters)
	// turns -> max turns (infinite if negative)
	// pipers -> initial location of pipers (per team)
	// rats -> initial location of rats
	public void init(int id, int side, long turns,
	                 Point[][] pipers, Point[] rats);

	// pipers -> current location of pipers (per team)
	// pipers_played -> if pipers played music on the previous turn
	// rats -> current location of rats
	// moves -> movement of pipers (for current team)
	public void play(Point[][] pipers, boolean[][] pipers_played,
	                 Point[] rats, Move[] moves);
}
