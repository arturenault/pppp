package pppp.g3;

import pppp.sim.Point;
import pppp.sim.Move;

import java.util.*;

public interface Strategy {

	public void init(int id, int side, long turns,
	                 Point[][] pipers, Point[] rats);

	public void play(Point[][] pipers, boolean[][] pipers_played,
	                 Point[] rats, Move[] moves);

}