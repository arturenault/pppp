package pppp.g2;

import pppp.sim.Point;
import pppp.sim.Move;

import java.util.*;

public class Player implements pppp.sim.Player {

    // see details below
    private int id = -1;
    private int side = 0;
    private double step = 1;
    private int N;
    private int[] pos_index = null;
    private Point[][] pos = null;
    private Point[] random_pos = null;
    private Random gen = new Random();
    boolean neg_y;
    boolean swap;

    private int maxMusicStrength;
    private double[][] rewardField;
    private double[][] returnField;
    private double[][] sweepField;
    private double gateX;
    private double gateY;
    private double behindGateX;
    private double behindGateY;
    private double alphaX;
    private double alphaY;
    private Random perturber;

    // bunch of values to be learned later
    private final double baseRatAttractor = 40;
    private int totalRats;
    private double ratAttractor = baseRatAttractor;
    private final double returnRatAttractor = 5;
    private final double collabCoef = 1.05;
    private final double friendlyCompCoef = 0.05;
    private final double enemyCompCoef = 0.9;
    private final double friendlyInDanger = 30;
    private final double D = 0.4;
    private final double playThreshold = 3;
    private final double closeToGate = 25;
    private final double homeThreshold = 4000;
    private double homeX;
    private double homeY;
    private final double enemyRepulsor = 0;
    private double convergenceThreshold = 0.25;

    // modified sweep strategy variables
    private int sweepNumPipersSide1;
    private int sweepNumPipersSide2;
    private int sweepNumPipersSide3;
    private int sweepNumPipersSide4;
    private int sweepPoint1;
    private int sweepPoint2;

    private Map<Integer, Rat> rats;
    private Map<Integer, Piper> pipers;

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

    private int getMusicStrength(Point loc, Point[] pipers, double threshold) {
	int strength = 0;
	for (int p=0; p<pipers.length; p++) {
	    if (distance(loc, pipers[p]) < threshold) {
		strength += 1;
	    }
	}
	return strength;
    }

    private void refreshBoard() {
	this.rewardField = new double[N][N];
	this.returnField = new double[N][N];
	this.sweepField = new double[N][N];
    }

    private void diffuse(Point[][] pipers) { 
	double[][] newRewardField = new double[N][N];
	double[][] newReturnField = new double[N][N];
	double[][] newSweepField = new double[N][N];	
	for (int x=1; x<N-1; x++) {
	    for (int y=1; y<N-1; y++) {
		    newRewardField[x][y] = rewardField[x][y] + D * (rewardField[x-1][y] + rewardField[x][y-1] + rewardField[x+1][y] + rewardField[x][y+1]);
		    newReturnField[x][y] = returnField[x][y] + D * (returnField[x-1][y] + returnField[x][y-1] + returnField[x+1][y] + returnField[x][y+1]);		    
		    newSweepField[x][y] = sweepField[x][y] + D * (sweepField[x-1][y] + sweepField[x][y-1] + sweepField[x+1][y] + sweepField[x][y+1]);		    
	    }
	}
	for (int t=0; t<4; t++) {
	    for (int p=0; p<pipers[t].length; p++) {
		int gridX = (int) (( pipers[t][p].x + side/2 + 10) * step);
		int gridY = (int) (( pipers[t][p].y + side/2 + 10) * step);
		if (t != id) {
		    newRewardField[ gridX ][ gridY ] *= enemyCompCoef;
		    newReturnField[ gridX ][ gridY ] *= enemyCompCoef;
		    newSweepField[ gridX ][ gridY ] *= enemyCompCoef;		    
		}
		else {
		    int maxEnemyStrength = 0;
		    for (int tt=0; tt<4; tt++) {
			if (tt != id) {
			    int tStrength = getMusicStrength(pipers[t][p], pipers[tt], 30);
			    if (tStrength > maxEnemyStrength) { maxEnemyStrength = tStrength;}			    
			}
		    }
		    if (maxEnemyStrength == 0) {			
			newRewardField[ gridX ][ gridY ] *= friendlyCompCoef;
			newReturnField[ gridX ][ gridY ] *= friendlyCompCoef;
			newSweepField[ gridX ][ gridY ] *= friendlyCompCoef;						
		    }
		    else {
			newRewardField[ gridX ][ gridY ] *= Math.pow(collabCoef, maxEnemyStrength);
			newReturnField[ gridX ][ gridY ] *= Math.pow(collabCoef, maxEnemyStrength);
			newSweepField[ gridX ][ gridY ] *= Math.pow(collabCoef, maxEnemyStrength);						
			}
		}
	    }
	}
	rewardField = newRewardField;
	returnField = newReturnField;
	sweepField = newSweepField;
    }
    
    public void init(int id, int side, long turns,
		     Point[][] pipers, Point[] rats)
    {
	this.step /= ((double) side / 100);
	this.convergenceThreshold *= step;
        this.neg_y = id == 2 || id == 3;
        this.swap  = id == 1 || id == 3;
	this.id = id;
	this.side = side;
	this.totalRats = rats.length;
	N = (int) ((side+20) * step + 1);
	perturber = new Random();
	double delta = 2.1;
	switch(id) {	   
	case 0:
	    gateX = 0;
	    gateY = side/2;
	    behindGateX = 0;
	    behindGateY = side/2 + delta;
	    alphaX = 0;
	    alphaY = 1;
	    break;
	case 1:
	    gateX = side/2;
	    gateY = 0;
	    behindGateX = side/2 + delta;
	    behindGateY = 0;
	    alphaY = 0;
	    alphaX = 1;
	    break;
	case 2:
	    gateX = 0;
	    gateY = -side/2;
	    behindGateX = 0;
	    behindGateY = -(side/2 + delta);
	    alphaX = 0;
	    alphaY = -1;
	    break;
	case 3:
	    gateX = -side/2;
	    gateY = 0;
	    behindGateX = -(side/2 + delta);
	    behindGateY = 0;
	    alphaX = -1;
	    alphaY = 0;
	    break;
	}

        this.sweepNumPipersSide1 = this.sweepNumPipersSide2 = this.sweepNumPipersSide3 = this.sweepNumPipersSide4 =
                pipers[0].length/4;
        if(pipers[0].length%4 > 2) {
            this.sweepNumPipersSide1++; this.sweepNumPipersSide2++; this.sweepNumPipersSide3++;
        } else if(pipers[0].length%4 > 1) {
            this.sweepNumPipersSide2++; this.sweepNumPipersSide3++;
        } else if(pipers[0].length%4 > 0) {
            this.sweepNumPipersSide2++;
        }
        int p1 = (side/2) - 7;
        int p2 = (side/2)/5 + 7;
        this.sweepPoint1 = Math.max(p1 ,p2);
        this.sweepPoint2 = Math.min(p1 ,p2);

        this.rewardField = new double[N][N];
	this.returnField = new double[N][N];
	this.sweepField = new double[N][N];
        this.rats = new HashMap<Integer, Rat>();
        this.pipers = new HashMap<Integer, Piper>();
	updateBoard(pipers,rats,new boolean[N][N]);
	createPipers(pipers, rats);
	updatePipersAndRats(rats, pipers, new boolean[4][pipers[0].length]);
	/*	for (int iter=0; iter<N; iter++) {
	    diffuse(pipers);
	    }*/
    }

    private void createPipers(Point[][] pipers, Point[] rats) {
        for(int i = 0; i < pipers[this.id].length; i++) {
            Strategy strategy;
            if(rats.length > 25 && pipers[this.id].length >3) {
                strategy = new Strategy(StrategyType.sweep);
            } else if (numberOfRatClusters(rats) > 4){
                strategy = new Strategy(StrategyType.diffusion);
            }
	    else {
		strategy = new Strategy(StrategyType.greedy);
	    }
            this.pipers.put(i, new Piper(i, pipers[this.id][i], strategy));
        }
    }

    private void updateStrategy(Point[][] pipers, Point[] rats) {
        double ratClusters = numberOfRatClusters(rats);
        //System.out.println(ratClusters);
        for(int i = 0; i < pipers[this.id].length; i++) {
            Piper piper = this.pipers.get(i);
            if(piper.strategy.type == StrategyType.adversarial) {
                continue;
            }
            if (ratClusters > 4) {
                piper.strategy = new Strategy(StrategyType.diffusion);
            }
            else {
                piper.strategy = new Strategy(StrategyType.greedy);
            }
        }
    }

    private double numberOfRatClusters(Point[] rats) {
        double ratClusters = rats.length;
        for (int r=0; r<rats.length; r++) {
            double nearbyRats = 0;
            for (int s=0; s<rats.length; s++) {
                if (r != s) {
                    if (distance(rats[r], rats[s]) < 10) {
                        nearbyRats += 1;
                    }
                }
            }
            ratClusters -= nearbyRats / (nearbyRats + 1);
        }
        return ratClusters;
    }

    private void updatePipersAndRats(Point[] rats, Point[][] pipers, boolean[][] pipers_played) {
	for (int p=0; p<pipers[id].length; p++) {
            this.pipers.get(p).resetRats();
	    this.pipers.get(p).updateLocation(pipers[id][p]);
        }
        /*for(int i =0; i < rats.length; i++) {
            if(rats[i] == null) {
                if(this.rats.containsKey(i)) {
                    this.rats.remove(i);
                }
            } else {
                if(!this.rats.containsKey(i)) {
                    Rat rat = new Rat(i);
                    this.rats.put(i, rat);
                }
                Rat rat = this.rats.get(i);
                updateRat(rat, rats[i], pipers, pipers_played);
            }
	    }*/
    }

    private void updateRat(Rat rat, Point location, Point[][] pipers, boolean[][] pipers_played) {
        rat.updateLocation(location);
        int maxPipersNearbySingleTeam = 0;
        int ratCapturedTeamId = -1;
        int piperId = -1;
        boolean conflict = false;
        int pipersNearby[] = new int[4];
        for(int i =0; i < pipers.length; i++) {
            pipersNearby[i] = 0;
            for(int j = 0; j < pipers[i].length; j++) {
                if(distance(pipers[i][j], location) < 10) {
                    if(i == this.id) {
                        if(this.pipers.get(j).playedMusic) {
                            piperId = j;
                            pipersNearby[i]++;
                        }
                    } else {
                        if(pipers_played[i][j]) {
                            pipersNearby[i]++;
                        }
                    }
                }
            }
            if(pipersNearby[i] > maxPipersNearbySingleTeam) {
                maxPipersNearbySingleTeam = pipersNearby[i];
                ratCapturedTeamId = i;
            }
        }
        Arrays.sort(pipersNearby);
        if(pipersNearby[0] == pipersNearby[1]) {
            conflict = true;
        }
        if((ratCapturedTeamId == -1) || conflict) {
            rat.captured = false;
            rat.hasEnemyCaptured = false;
            rat.piperId = -1;
        } else if(ratCapturedTeamId != this.id){
            rat.captured = true;
            rat.hasEnemyCaptured = true;
        } else {
            if(rat.captured && !rat.hasEnemyCaptured) {
                Piper prevPiper = this.pipers.get(rat.piperId);
                if(prevPiper.playedMusic &&
                        (distance(prevPiper.curLocation, location) < 10)
                ) {
                    prevPiper.addRat(rat.id);
                } else {
                    rat.piperId = piperId;
                    this.pipers.get(piperId).addRat(rat.id);
                }
            } else {
                rat.piperId = piperId;
                this.pipers.get(piperId).addRat(rat.id);
            }
            rat.captured = true;
            rat.hasEnemyCaptured = false;
        }
    }

    private boolean isCaptured(Point loc, Point[][] pipers) {
	int friendlyMusicStrength = getMusicStrength(loc, pipers[id],10);
	int maxEnemyStrength = 0;
	for (int tt=0; tt<4; tt++) {
	    if (tt != id) {
		int tStrength = getMusicStrength(loc, pipers[tt], 10);
		if (tStrength > maxEnemyStrength) { maxEnemyStrength = tStrength;}			    
	    }
	}
	if (friendlyMusicStrength > maxEnemyStrength ) {
	    return true;
	}
	else {
	    return false;
	}
    }

    public void updateBoard(Point[][] pipers, Point[] rats, boolean[][] pipers_played) {
	refreshBoard();
	for (int r=0; r<rats.length; r++) {
	    if (rats[r] != null){
		if (!isCaptured(rats[r], pipers) && distance(rats[r], new Point(gateX, gateY)) > closeToGate) {
		    rewardField[(int) Math.round((rats[r].x+side/2+10)*step)][ (int) Math.round((rats[r].y+side/2+10)*step)] = ratAttractor;
		}
	    }
	}
	/*	for (int t=0; t<4; t++) {
	    if (t != id) {
		for (int p=0; p<pipers[t].length; p++) {
		    returnField[ (int) Math.round((pipers[t][p].x + side/2 + 10)*step)][ (int) Math.round((pipers[t][p].y + side/2 + 10)*step)] = enemyRepulsor;
		    sweepField[ (int) Math.round((pipers[t][p].x + side/2 + 10)*step)][ (int) Math.round((pipers[t][p].y + side/2 + 10)*step)] = enemyRepulsor;		    
		}
	    }
	    }*/
	sweepField[(int) Math.round((alphaX * 3 * side / 10 + side/2 + 10) * step)][ (int) Math.round((alphaY * 3 * side / 10 + side/2 + 10) * step) ] = homeThreshold;
	returnField[ (int) Math.round((gateX + side/2 + 10) * step)][ (int) Math.round((gateY + side/2 + 10) * step)] = homeThreshold;	
    }
    
    // return next locations on last argument
    public void play(Point[][] pipers, boolean[][] pipers_played,
		     Point[] rats, Move[] moves)
    {
	if (this.pipers.get(0).strategy.type != StrategyType.sweep) {
	    updateStrategy(pipers,rats);
	}
	int numEnemiesNearGate = 0;
	int numFriendliesNearGate = 0;
	int goalie = -1;
	double goalieD = 0;
	for (int p=0; p<pipers[id].length; p++) {
	    double d = distance(new Point(gateX, gateY), pipers[id][p]);
	    if (d < goalieD || goalie == -1) {
		goalieD = d;
		goalie = p;
	    }
	}	
	for (int t=0; t<4; t++) {
	    if (t != id) {
		for (int p=0; p<pipers[t].length; p++) {
		    if (distance(pipers[t][p], new Point(gateX, gateY)) < closeToGate) {
			numEnemiesNearGate++;
		    }
		}
	    }
	}	
        updatePipersAndRats(rats, pipers, pipers_played);
	boolean haveGateInfluence = false;
	ratAttractor = baseRatAttractor * Math.pow((double) totalRats / (double) rats.length,3);
	updateBoard(pipers, rats, pipers_played);
	for (int iter=1; iter<N; iter++) {
	    diffuse(pipers);
	}
        Boolean allPipersWithinDistance = null;
	for (int p = 0 ; p != pipers[id].length ; ++p) {
        Piper piper = this.pipers.get(p);
        if(piper.strategy.type == StrategyType.sweep) {
            moves[p] = modifiedSweep(piper, rats, allPipersWithinDistance);
            continue;
        }
        if(piper.strategy.type == StrategyType.adversarial) {
            moves[p] = adversarial(piper, rats, pipers, pipers_played);
            if(moves[p] != null) {
                continue;
            }
        }
	    Point src = pipers[id][p];
	    int numCapturedRats = nearbyRats(src, rats, 10);

	    boolean playMusic = false;
	    Point target;

	    //piper is behind gate
	    if (alphaX * pipers[id][p].x + alphaY * pipers[id][p].y > side/2) {
		if (numCapturedRats > 0 && haveGateInfluence == false && distance(pipers[id][p], new Point(behindGateX, behindGateY)) < 2.2) {
		    target = new Point(behindGateX, behindGateY);
		    playMusic = true;
		    numFriendliesNearGate++;
		    if (numFriendliesNearGate > numEnemiesNearGate) {
			haveGateInfluence = true;
		    }
		} else {
		    target = new Point(gateX, gateY);
		    playMusic = false;
		}
	    }

	    //piper has captured enough rats
	    else if(numCapturedRats >= 1 + rats.length / (8*pipers[id].length) && ((distance(src, new Point(gateX, gateY)) > closeToGate) || haveGateInfluence == false) ) {
		if (distance(src, new Point(gateX, gateY)) > closeToGate) {
		    if (piper.strategy.type == StrategyType.diffusion) {
			int x = (int)Math.round((src.x + side/2 + 10)*step);
			int y = (int)Math.round((src.y + side/2 + 10)*step);
			int bestX = -1;
			int bestY = -1;
			double steepestPotential = 0;
			for (int i=Math.max(x-3,0); i<=Math.min(x+3,N-1); i++) {
			    for (int j=Math.max(y-3,0); j<=Math.min(y+3,N-1); j++){
				if (returnField[i][j] > steepestPotential) {
				    bestX = i;
				    bestY = j;
				    steepestPotential = returnField[i][j];
				}
			    }
			}
			target = new Point(bestX / step - side/2 - 10, bestY / step - side/2 - 10 );
		    }
		    else {
			target = new Point(behindGateX, behindGateY);
		    }
		}
		else {
		    target = new Point(behindGateX, behindGateY);
		}
		playMusic = true;		
	    }

	    //piper should capture more rats
	    else {
		if (piper.strategy.type == StrategyType.diffusion) {
		    int strength = Math.min(getMusicStrength(src, pipers[id],25),maxMusicStrength-1);
		    int x = (int)Math.round((src.x + side/2 + 10)*step);
		    int y = (int)Math.round((src.y + side/2 + 10)*step);
		    int bestX = -1;
		    int bestY = -1;
		    double steepestPotential = -1000;
		    for (int i=Math.max(x-3,0); i<=Math.min(x+3,N-1); i++) {
			for (int j=Math.max(y-3,0); j<=Math.min(y+3,N-1); j++){
			    if (rewardField[i][j] > steepestPotential) {
				bestX = i;
				bestY = j;
				steepestPotential = rewardField[i][j];
			    }
			}
		    }
		    target = new Point(bestX / step - side/2 - 10 + (perturber.nextFloat() - 0.5) / 10, bestY / step - side/2 - 10 + (perturber.nextFloat() - 0.5) / 10);
		}
		else if (piper.strategy.type == StrategyType.greedy) {
		    target = greedy(rats);
		}
		else {
		    target = new Point(0,0);
		}
		//don't play music near gate if a piper is behind the gate trying to pull rats in
		if (distance(src, new Point(gateX, gateY)) < 15 && haveGateInfluence) {
		    playMusic = false;
		}
		else {
		    // if already playing music, keep playing unless lost all rats
		    if (this.pipers.get(p).playedMusic == true) {
			if (numCapturedRats > 0) {
			    playMusic = true;
			}
			else {
			    playMusic = false;
			}
		    }
		    else {
			// if not already playing, play music when approaching local optima
			if (this.pipers.get(p).getAbsMovement() < convergenceThreshold && numCapturedRats > 0) {
			    playMusic = true;
			}
			else {
			    playMusic = false;
			}
		    }
		}
	    }
	    moves[p] = move(src, target, playMusic);
	}
	for (int p=0; p<pipers[id].length; p++) {
	    this.pipers.get(p).updateMusic(moves[p].play);
	}
    }

    private Point greedy(Point[] rats) {
        int closestRat = -1;
        double closestDist = 0;
        for (int r=0; r<rats.length; r++) {
            double d = distance(new Point(gateX, gateY), rats[r]);
            if (d < closestDist || closestRat == -1) {
                closestDist = d;
                closestRat = r;
            }
        }
        return rats[closestRat];
    }

    private Move modifiedSweep(Piper piper, Point[] rats, Boolean allPipersWithinDistance) {
        boolean playMusic = false;
        Point target = null;
        if (allPipersWithinDistance == null) {
            allPipersWithinDistance = allPipersWithinDistance(30);
        }
        if(piper.strategy.type != StrategyType.sweep || !piper.strategy.isPropertySet("step")) {
            piper.strategy = new Strategy(StrategyType.sweep);
            piper.strategy.setProperty("step", 1);
            target = new Point(gateX, gateY);
            playMusic = false;
        } else if(4 == (Integer) piper.strategy.getProperty("step")) {
	    if (allPipersWithinDistance) {		
		piper.strategy.setProperty("step", 5);
		playMusic = true;
		target = new Point(behindGateX, behindGateY);
	    }
	    else {
		Point src = piper.curLocation;
		int x = (int)Math.round((src.x + side/2 + 10)*step);
		int y = (int)Math.round((src.y + side/2 + 10)*step);
		int bestX = -1;
		int bestY = -1;
		double steepestPotential = 0;
		for (int i=Math.max(x-3,0); i<=Math.min(x+3,N-1); i++) {
		    for (int j=Math.max(y-3,0); j<=Math.min(y+3,N-1); j++){
			if (sweepField[i][j] > steepestPotential) {
			    bestX = i;
			    bestY = j;
			    steepestPotential = sweepField[i][j];
			}
		    }
		}
		target = new Point(bestX / step - side/2 - 10, bestY / step - side/2 - 10 );
		playMusic = true;
	    }
        }
        if(target == null) {
            if (distance(piper.curLocation, (Point) piper.strategy.getProperty("location")) != 0) {
                target = (Point) piper.strategy.getProperty("location");
                playMusic = piper.playedMusic;
            } else {
                Integer step = (Integer) piper.strategy.getProperty("step");
                switch (step) {
                    case 1:
                        int delta;
                        if (piper.id < this.sweepNumPipersSide1) {
                            // side 1
                            delta = (piper.id) * (this.sweepPoint1 - this.sweepPoint2) / this.sweepNumPipersSide1;
                            target = point(this.sweepPoint1, this.sweepPoint1 - delta, this.neg_y, this.swap);
                        } else if (piper.id < this.sweepNumPipersSide1 + this.sweepNumPipersSide2) {
                            // side 2
                            delta = (piper.id - this.sweepNumPipersSide1) * (this.sweepPoint1) / this.sweepNumPipersSide2;
                            target = point(this.sweepPoint1 - delta, this.sweepPoint2, this.neg_y, this.swap);
                        } else if (piper.id < this.sweepNumPipersSide1 + this.sweepNumPipersSide2 + this.sweepNumPipersSide3) {
                            // side 3
                            delta = (this.sweepNumPipersSide3 + this.sweepNumPipersSide2 + this.sweepNumPipersSide1 - piper.id - 1) * (this.sweepPoint1) / this.sweepNumPipersSide2;
                            target = point(-this.sweepPoint1 + delta, this.sweepPoint2, this.neg_y, this.swap);
                        } else {
                            // side 4
                            delta = (this.sweepNumPipersSide4 + this.sweepNumPipersSide3 + this.sweepNumPipersSide2 + this.sweepNumPipersSide1 - piper.id - 1) * (this.sweepPoint1 - this.sweepPoint2) / this.sweepNumPipersSide4;
                            target = point(-this.sweepPoint1, this.sweepPoint1 - delta, this.neg_y, this.swap);
                        }
                        piper.strategy.setProperty("step", 2);
                        break;
                    case 2:
                        // in middle, should make a check that only if all pipers are in a certain distance with each other, move to step 4
                        playMusic = true;
                        piper.strategy.setProperty("step", 4);
                        target = new Point(alphaX * 3 * side / 10, alphaY * 3 * side / 10);
                        break;
                    case 3:
                        // in front of gate
                        playMusic = true;
                        piper.strategy.setProperty("step", 4);
                        target = new Point(alphaX * (side / 2 - 5), alphaY * (side / 2 - 5));
                        break;
                    case 4:
                        playMusic = true;
                        if(allPipersWithinDistance) {
                            piper.strategy.setProperty("step", 5);
                            target = new Point(gateX + 2*Math.random(), gateY + 2*Math.random());
                        } else {
                            target = (Point) piper.strategy.getProperty("location");
                        }
                        break;
                    case 5:
			//                        if (nearbyRats(piper.curLocation, rats, 10) == 0) {
			piper.strategy = new Strategy(StrategyType.diffusion);
			    //}
                        playMusic = true;
                        target = new Point(behindGateX, behindGateY);
                }
            }
        }
        piper.strategy.setProperty("location", target);
        return move(piper.curLocation, target, playMusic);
    }

    private Move adversarial(Piper piper, Point[] rats, Point[][] pipers, boolean[][] pipers_played) {
        boolean playMusic = false;
        Point target = null;
        Point src = piper.curLocation;
        double distanceThreshold = 1;
        if(piper.strategy.type != StrategyType.adversarial || !piper.strategy.isPropertySet("step")) {
            piper.strategy = new Strategy(StrategyType.adversarial);
            piper.strategy.setProperty("step", 1);
            // find the closest piper
            target = closestPlayingEnemyPiper(src, pipers, pipers_played);
            playMusic = false;
        } else {
            // step 1 is going to the enemy
            // step 2 is staying there and playing music for 10 ticks
            // step 3 is start moving towards our gate
            Integer step = (Integer) piper.strategy.getProperty("step");
            switch (step) {
                case 1:
                    // stay there
                    target = closestPlayingEnemyPiper(src, pipers, pipers_played);
                    if(target != null) {
                        if ((distance(src, target) < distanceThreshold)) {
                            playMusic = true;
                            piper.strategy.setProperty("ticks", 0);
                            piper.strategy.setProperty("step", 2);
                        } else {
                            playMusic = false;
                        }
                    }
                    break;
                case 2:
                    // stay there for 10 ticks and then start moving towards gate
                    target = (Point) piper.strategy.getProperty("location");
                    playMusic = true;
                    piper.strategy.incrementProperty("ticks");
                    if((Integer) piper.strategy.getProperty("ticks") > 100) {
                        piper.strategy.setProperty("step", 3);
                        target = new Point(gateX, gateY);
                    }
                    break;
                case 3:
                    if(nearbyRats(src, rats, null) == 0) {
                        piper.strategy = new Strategy(StrategyType.diffusion);
                    } else if (distance(src, (Point) piper.strategy.getProperty("location")) == 0) {
                        target = new Point(behindGateX, behindGateY);
                        piper.strategy.setProperty("step", 4);
                    } else {
                        target = new Point(gateX, gateY);
                    }
                    playMusic = true;
                    break;
                case 4:
                    if(nearbyRats(src, rats, null) == 0) {
                        piper.strategy = new Strategy(StrategyType.diffusion);
                    } else {
                        target = new Point(behindGateX, behindGateY);
                    }
                    playMusic = true;
                    break;
            }
        }

        if(target == null) {
            // use some other strategy
            piper.strategy = new Strategy(StrategyType.greedy);
            return null;
        }
        piper.strategy.setProperty("location", target);
        return move(src, target, playMusic);
    }

    private Point closestPlayingEnemyPiper(Point src, Point[][] pipers, boolean[][] pipers_played) {
        double leastDistance = Integer.MAX_VALUE;
        Point result = null;
        for(int i = 0; i < pipers_played.length; i++) {
            if(i == this.id) {
                continue;
            }
            for(int j = 0; j < pipers_played[i].length; j++) {
                if(pipers_played[i][j]) {
                    double distance = distance(src, pipers[i][j]);
                    if(distance < leastDistance) {
                        result = pipers[i][j];
                        leastDistance = distance;
                    }
                }
            }
        }
        return result;
    }

    private boolean allPipersWithinDistance(int distance) {
        for(Piper piper1: this.pipers.values()) {
            for(Piper piper2: this.pipers.values()) {
                if(distance(piper1.curLocation, piper2.curLocation) > distance) {
                    return false;
                }
            }
        }
        return true;
    }

    // pass distanceThreshold as null to use a default threshold value
    private int nearbyRats(Point src, Point[] rats, Integer distanceThreshold) {
        int ratsNearby = 0;
        double threshold = 9.5;
        if(distanceThreshold != null) {
            threshold = distanceThreshold;
        }
        for(Point rat: rats) {
            if(rat != null) {
                if (distance(src, rat) < threshold) {
                    ratsNearby++;
                }
            }
        }
        return ratsNearby;
    }

    public double distance(Point p1, Point p2)
    {
        return Math.hypot(p1.x - p2.x, p1.y - p2.y);
    }
}
