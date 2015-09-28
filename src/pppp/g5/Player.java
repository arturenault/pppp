package pppp.g5;

import pppp.sim.Point;
import pppp.sim.Move;

import java.util.*;

public class Player implements pppp.sim.Player {

	// specify location that the player will alternate between
	public void init(int id, int side, long turns,
	                 Point[][] pipers, Point[] rats) {}

	// return next locations on last argument
	public void play(Point[][] pipers, boolean[][] pipers_played,
	                 Point[] rats, Move[] moves)
	{
		for (int i = 0 ; i != moves.length ; ++i)
			moves[i] = new Move(0, 0, false);
	}
}
