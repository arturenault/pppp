package pppp.g3;

import pppp.sim.Point;
import pppp.sim.Move;

import java.util.*;

import pppp.g3.Movement;
import pppp.g3.StrategyFactory;
import pppp.g3.Strategy;

public class Player implements pppp.sim.Player {

	// see details below
	private int id = -1;
	private int side = 0;
	private long turns = 0;
	private int[] pos_index = null;
	private Point[][] pos = null;

	StrategyFactory factory = new StrategyFactory();
	Strategy current = null;

	// specify location that the player will alternate between
	public void init(int id, int side, long turns,
	                 Point[][] pipers, Point[] rats)
	{
		this.id = id;
		this.side = side;
		this.turns = turns;
		current = factory.getStrategy(id, side, turns, pipers, rats);
	}

	// return next locations on last argument
	public void play(Point[][] pipers, boolean[][] pipers_played,
	                 Point[] rats, Move[] moves)
	{
		Strategy s = factory.getStrategy(id, side, turns, pipers, rats);
		if(s != current){
			current = s;
		}
		current.play(pipers, pipers_played, rats, moves);
	}
}
