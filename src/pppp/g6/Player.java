package pppp.g6;

import pppp.sim.Point;
import pppp.sim.Move;
//SETH
import java.util.*;

public class Player implements pppp.sim.Player {

	// see details below
	private int currentStrategy = -1;
	private int initialStrategy;
	private final int NUMBER_OF_MOVES = 6;
	private int n_rats;
	private int initial_n_rats;
	private int n_pipers;
	private int id = -1;
	private int side = 0;
	private int[] pos_index = null;
	private Point[][] pos = null;
	private Point[][] posStrategySparse = null;
	private Point[][] posStrategyDense = null;
	//private Point[] random_pos = null;
	//private Random gen = new Random();
	private boolean directionFlag =false;
	private Point oppDoorPos;
	
	HashMap<Integer,int[]> strategyPositionArray = new HashMap<Integer,int[]>();
	
	public class Strategy{
		static final int SPARSE_STRATEGY = 1;
		static final int DENSE_STRATEGY = 2;
	}

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
		n_pipers = pipers[id].length;
		n_rats = rats.length;
		initial_n_rats = rats.length;
		//println("N Pipers" + n_pipers);
		posStrategySparse = new Point[n_pipers][NUMBER_OF_MOVES];
		posStrategyDense = new Point[n_pipers][NUMBER_OF_MOVES];
		
		pos = new Point [n_pipers][NUMBER_OF_MOVES];
		//random_pos = new Point [n_pipers];
		pos_index = new int [n_pipers];

		// Check for sparse or dense environment
		if(n_rats <= 20){
		
			// Use sparse strategy
			currentStrategy = Strategy.SPARSE_STRATEGY;
			initialStrategy = currentStrategy;
		}
		
		else{
	
			// Use dense stragegy
			currentStrategy = Strategy.DENSE_STRATEGY;
			initialStrategy = currentStrategy;
		}
		
		
		setStrategyMoves(currentStrategy,pipers,rats);
		
	}
	
	public void setStrategyMoves(int currentStrategy,Point[][] pipers, Point[] rats){
		
		switch(currentStrategy){
		
		case Strategy.SPARSE_STRATEGY: sparseStrategyPositions(pipers,rats);
			break;
			
			
		case Strategy.DENSE_STRATEGY: denseStrategyPositions();
			break;
		
		}
	}

	public void sparseStrategyPositions(Point[][] pipers, Point[] rats){
		// pick coordinate based on where the player is
		boolean neg_y = id == 2 || id == 3;
		boolean swap  = id == 1 || id == 3;
		
		int oppositeId;
		if(id < 1){
			oppositeId = id + 2;
		}
		else{
			oppositeId = id - 2;
		}
		boolean neg_y_opp =  oppositeId == 2 || oppositeId == 3;
		boolean swap_opp  = oppositeId  == 1 || oppositeId == 3;
		double door_opp = 0.0;
		oppDoorPos  = point(door_opp, side * 0.5, neg_y_opp, swap_opp);

		
		for (int p = 0 ; p != n_pipers ; ++p) {
			
			
			
			double door = 0.0;
			//first point is the door
			pos[p][0]  = point(door, side * 0.5, neg_y, swap);
			
			// If rat within limit, all go there
			
			// 
			pos[p][1] = findRatNearestToBase(rats, pos[p][0], pos[p][0]);
			
			//third point is the merge point outside the door
			// pos[p][2] = check to see if any within safe area
			// if so, take it, otherwise return
			pos[p][2] = pos[p][0];
			pos[p][3] = pos[p][0];
			//pos[p][3] = point(door * 0.1, side * 0.5, neg_y, swap);
			pos[p][4] = point(door * -6, side * 0.5 + 3, neg_y, swap);
			pos[p][5] = pos[p][4];// = point(door * +6, side * 0.5 + 3, neg_y, swap);

			
			
			pos_index[p] = 0;
			
		}
		
	}


	
	public void denseStrategyPositions(){
		
	//	int angle = 180/n_pipers;
		
		double distance = (3 * side)/5;
		//double distance = side;
		
		double slice = (180/(n_pipers +1));
		
		for (int p = 0 ; p != n_pipers ; ++p) {
			
			
			
			//double theta = Math.toRadians(p * 90.0/(n_pipers-1) + 45);
			
			
			double theta = Math.toRadians((p+1)*slice);
			
			// spread out at the door level
			double door = 0.0;
			if (n_pipers != 1) door = p * 1.8 / (n_pipers - 1) - 0.9;
			// pick coordinate based on where the player is
			boolean neg_y = id == 2 || id == 3;
			boolean swap  = id == 1 || id == 3;
			
			//first point is the door
			pos[p][0]  = point(door, side * 0.5, neg_y, swap);
			
			//second point is the calculated angle for each piper
			pos[p][1] = point(distance * Math.cos(theta),distance * (1- Math.sin(theta)),neg_y,swap);
			
			//third point is the merge point outside the door
			//pos[p][2] = point(door - 5, 0, neg_y, swap);
			
			//pos[p][2] = point(0, distance * (1- Math.sin(theta)) ,neg_y, swap);
			System.out.println("$$$$$$ POINT $$$$$$$" + distance * (1- Math.sin(theta)));
			
			pos[p][2]= pos[p][3]  = point(door * 0.5, side * 0.5, neg_y, swap);
			
			// fourth and fifth positions are outside the rat moving area
			pos[p][4] = point(door * -6, side * 0.5 + 6, neg_y, swap);
			pos[p][5] = point(door * +6, side * 0.5 + 6, neg_y, swap);
			
			
			pos_index[p] = 0;
			
		}
		
	}

	// return next locations on last argument
	public void play(Point[][] pipers, boolean[][] pipers_played,
	                 Point[] rats, Move[] moves)
	{
			//Gets the current number of rats
			n_rats = rats.length;
			
			
			
			Boolean pipersTogether = arePipersTogether(pipers);
			Boolean needToWait = needToWaitAtGate(rats, pos[0][0]);
			for (int p = 0 ; p != n_pipers ; ++p) {
			Point src = pipers[id][p];
			// Reupdate target position for constantly moving targets
			if(currentStrategy == Strategy.SPARSE_STRATEGY){
				if(pos_index[p] == 0){
					pos[p][1] = findRatNearestToBase(rats, pos[p][0], src);
				}
				else if(pos_index[p] == 1){
					
					// Ensure all pipers are on top of each other
					if(pipersTogether == false){
						pos[p][1] = pos[p][0];
						
						pos_index[p]--;						
					}
					else{
						Point newRatPos = findRatNearestToBase(rats, pos[p][0],src);
						if(newRatPos.distance(pos[p][1]) > 0.5){
							pos[p][1] = newRatPos;
						}
					}
				}
				else if(pos_index[p] == 2 || pos_index[p] == 3){
					Point nextNearestRat = findNextNearestRat(rats, pos[p][0], src);
					if(pos_index[p] == 2){
						if(nextNearestRat != null){
							pos[p][2] = nextNearestRat;
						}
						else{
							pos_index[p]++;
						}						
					}
					else if(pos_index[p] == 3){
						if(nextNearestRat != null){
							pos[p][2] = nextNearestRat;
							pos_index[p]--;
						}
					}
				}
				else if(pos_index[p] == 4){
					if(needToWait == true){
						pos_index[p]--;
					}
				}
								
			}
			Point dst = pos[p][pos_index[p]];
			
			
			// if null then get random position
			//Random commented
			/*if (dst == null) {
				dst = random_pos[p];
			}*/
			
			
			// if position is reached
			if (Math.abs(src.x - dst.x) < 0.000001 &&
			    Math.abs(src.y - dst.y) < 0.000001) {
				
				
				// discard random position
				//Random commented
				//if (dst == random_pos[p]) random_pos[p] = null;
				
				// get next position
				++pos_index[p];
				if(currentStrategy == Strategy.SPARSE_STRATEGY && pos_index[p] == 1 && pipersTogether == false){
					--pos_index[p];
				}
				else if(currentStrategy == Strategy.SPARSE_STRATEGY && p != 0){
					pos_index[p] = pos_index[p-1];
				}
				if(pos_index[p] == pos[p].length){
					
					pos_index[p] = 0;
					if(initialStrategy == Strategy.DENSE_STRATEGY && n_rats < initial_n_rats/ 4){
						currentStrategy = Strategy.SPARSE_STRATEGY;
						sparseStrategyPositions(pipers,rats);
					}
					
					
					
				}
				dst = pos[p][pos_index[p]];

/*				if (++pos_index[p] == pos[p].length) pos_index[p] = 0;{
				dst = pos[p][pos_index[p]];
				}
*/				
				
				// generate a new position if random
				//Random commented
				/*
				if (dst == null) {
					if(toogle(directionFlag)){
						double x = (gen.nextDouble() - 0.5) * side * 0.9;
						double y = Math.abs((gen.nextDouble() - 0.5) * side * 0.9)*-1;
						random_pos[p] = dst = new Point(x, y);
					}
					else{
						double x = (gen.nextDouble() - 0.5) * side * 0.9;
						double y = Math.abs((gen.nextDouble() - 0.5) * side * 0.9);
						random_pos[p] = dst = new Point(x, y);
					}
					
				}*/
				
			}
			// get move towards position
			moves[p] = move(src, dst, pos_index[p] > 1);
		
		}
	}
	
	public boolean toogle(boolean flag){
		directionFlag = !flag;
		return directionFlag;
	}


	//Sparse Strategy Functions
	
	// Returns location of the nearest rat within the semi-sphere
	// (ie rats guarenteed to be grabbed safely)
	// returns -1 if none
	
	
	double truncate(double number, int precision)
	{
	    double prec = Math.pow(10, precision);
	    int integerPart = (int) number;
	    double fractionalPart = number - integerPart;
	    fractionalPart *= prec;
	    int fractPart = (int) fractionalPart;
	    fractionalPart = (double) (integerPart) + (double) (fractPart)/prec;
	    return fractionalPart;
	}

	public Point findRatNearestToBase(Point[] rats, Point doorPos, Point curPos){
		float minDistance = -1;
		int ratIndex = -1;
		for(int i = 0; i<rats.length;i++){
			float dist = (float) curPos.distance(rats[i]);
			if(ratIndex == -1){
				minDistance = dist;
				ratIndex = i;
			}
			else{
				if(dist < minDistance){
					minDistance = dist;
					ratIndex = i;
				}
			}
		}
		return rats[ratIndex];
	}

	public Point findNextNearestRat(Point[] rats, Point doorPos, Point curPos){
		float minDistance = -1;
		int ratIndex = -1;
		float percentage = 0.7f;
		boolean ignoreXvalues = doorPos.x == 0;
		double bound = calcBound(ignoreXvalues, doorPos, rats, percentage);

		
		for(int i = 0; i<rats.length;i++){
			float distToDoor  = (float) doorPos.distance(rats[i]);
			float distToPiper = (float) curPos.distance(rats[i]); 
			if(ignoreXvalues){
				if(doorPos.y == side/2){
					if(rats[i].y > bound == false){
						continue;
					}
				}
				else{
					if(rats[i].y < bound == false){
						continue;
					}
				}
			}
			else{
				if(doorPos.x == side/2){
					if(rats[i].x > bound == false){
						continue;
					}
				}
				else{
					if(rats[i].x < bound == false){
						continue;
					}
				}				
			}
			if(distToPiper > 10f){
				if(ratIndex == -1){
					minDistance = distToPiper;
					ratIndex = i;
				}
				else{
					if(distToPiper < minDistance){
						minDistance = distToDoor;
						ratIndex = i;
					}
				}
			}
		}
		if(ratIndex == -1){
//			System.out.print("null");
			
			return null;
		}
		else{
			return rats[ratIndex];		
		}
	}


	public Boolean arePipersTogether(Point[][] pipers){
		for(int i = 0; i<n_pipers;i++){
			if(pipers[id][0].distance(pipers[id][i]) > 1){
				return false;
			}
		}
		return true;
	}
	
	public Boolean needToWaitAtGate(Point[] rats, Point doorPos){
		for(int i = 0; i<rats.length;i++){
			float distToDoor  = (float) doorPos.distance(rats[i]);			
			if(distToDoor <= 10 && distToDoor > 3){
				return true;
			}
		}		
		return false;
	}
	
	// Returns the x or y bound where there exists percentage% number of the total rats within the range
	// So returning -10 if the base is at (0,50) means that percentage% of the rats are between (x, 50) and (x, -10)
	public double calcBound(boolean ignoreXvalues, Point doorPos, Point[] rats, float percentage){
		double[] orderedPos = new double[rats.length];
		if(ignoreXvalues){
			for(int i = 0; i<rats.length;i++){
				orderedPos[i] = rats[i].y;				
			}		
			Arrays.sort(orderedPos);

			if(doorPos.y == side/2){
				reverseArray(orderedPos);
			}
			
			int estPercIndex = (int) (percentage*rats.length);
			if(estPercIndex > 0){
				estPercIndex--;
			}
			return orderedPos[estPercIndex];
		}
		else{
			for(int i = 0; i<rats.length;i++){
				orderedPos[i] = rats[i].x;				
			}		
			Arrays.sort(orderedPos);

			if(doorPos.x == side/2){
				reverseArray(orderedPos);
			}
			
			int estPercIndex = (int) (percentage*rats.length);
			if(estPercIndex > 0){
				estPercIndex--;
			}
			return orderedPos[estPercIndex];			
		}
		
	}
	
	public double[] reverseArray(double[] array){
		for(int i = 0; i< array.length/2; i++){
			double temp = array[i];
			array[i] = array[array.length - 1 - i];
			array[array.length - 1 - i] = temp;
		}
		return array;
	}

	
}
