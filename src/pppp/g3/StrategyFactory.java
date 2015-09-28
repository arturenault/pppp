package pppp.g3;

import pppp.g3.Strategy;
import pppp.g3.HunterSweep;
import pppp.g3.AngularSweep;

import pppp.sim.Point;
import pppp.sim.Move;

public class StrategyFactory{

	final public static double piperPivot = 1;
	final public static double ratPivot = 1;

	public final static double RAT_DENSITY_THRESHOLD= 0.0025;

	private Strategy currentStrategy = null;

	public Strategy getStrategy(int id, int side, long turns, Point[][] pipers, Point[] rats){

		if (currentStrategy != null) {
			return currentStrategy;
		}
		switch (pipers[id].length) {
			case 1: 
				currentStrategy = new OnePiperStrategy();
				break;
			case 2:
			case 3:
			case 4:
			case 5: 
				currentStrategy = new MidPiperStrategy();
				break;
			case 6: 
			case 7:
			case 8:
			case 9:
			case 10:
			default:
				currentStrategy = new HighPiperStrategy();
				break;
		}

        currentStrategy.init(id, side, turns, pipers, rats);
        return currentStrategy;

		/*
		if(currentStrategy == null){
            if(rats.length >= 100)
                currentStrategy = new AngularSweep();
            else
                currentStrategy = new HunterSweep();
			currentStrategy.init(id, side, turns, pipers, rats);
		}
        if(rats.length <= 25 && !(currentStrategy instanceof pppp.g3.HunterSweep)){
            currentStrategy = new HunterSweep();
            currentStrategy.init(id, side, turns, pipers, rats);
        }*/
	}
}
