/********************************************
 * Group 9
 * Artur Renault, Jing Guo, Cathy Jin and Liuyang Wang
 *
 * Pied piper player class; based off g0.
 *
 * Implements a divide and conquer strategy:
 *  players go for the closest rat that is not
 *  assigned to a player and return to base.
 *
 *  If rats are lost during return, they attempt
 *  to find new rats.
 * *****************************************/
package pppp.g9;

import pppp.sim.Point;
import pppp.sim.Move;

import java.util.*;

public class Player implements pppp.sim.Player {

	private int id = -1;
	private double density = 0;
	private double density_threshold = 0.3;
	private int side = 0;
	private int piper_at_door = -1;
	private int[] pos_index = null;
	private int[] sweep_pos_index;
	private Point door_pos = null;
	private Point[][] pos = null;
	private Random gen = new Random();
	private Point[] piper_rats;
	private int with_rat_threshold = 10;
	private Point[][] sweep_pos;
	private boolean[] switchStrategy;
	private int sweep_piper_id_at_door;

	//copy from GRP5
	private int grid_size = 15;
	private int NumPiperAtGate = 0;
	private boolean sweepover = false;
	private ArrayList<Grid> gridlist;
	private ArrayList<Piper> piperlist;
	private Point[][] cell_pos = null;
	private Double[] rat_catched;


	private boolean goCorner[];
	private Point corner_pos[];

	private boolean outDoorNeighbor[];
	private int neighbor_id[];
	private Point door_pos_neighbor[];
	private Point neighbor_old_pos[][];
	private boolean neighbor_dir_decided[];

	// create move towards specified destination
	private static Move move(Point src, Point dst, boolean play)
	{
		double dx = dst.x - src.x;
		double dy = dst.y- src.y;
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
		int n_pipers = pipers[id].length;
		density = rats.length / (double) side;
		pos = new Point [n_pipers][4];
		pos_index = new int [n_pipers];
		piper_rats = new Point[n_pipers];

		switchStrategy = new boolean[n_pipers];

		sweep_pos = new Point[n_pipers][4];
		sweep_pos_index = new int[n_pipers];
		sweep_piper_id_at_door = -1;

		cell_pos = new Point [n_pipers][4];
		rat_catched = new Double[n_pipers];


		corner_pos = new Point[3];
		boolean neg_y = id == 2 || id == 3;
		boolean swap  = id == 1 || id == 3;

		corner_pos[0] = point(side / 2 - 10, side / 2 - 10, neg_y, swap);
		corner_pos[1] = point(10 -side / 2, side / 2 - 10, neg_y, swap);
		corner_pos[2] = point(0, side * 0.5 - 10, neg_y, swap);
		outDoorNeighbor = new boolean[4];

		neighbor_id = new int[2];
		door_pos_neighbor = new Point[2];
		double door = 0.0;
		neighbor_old_pos = new Point[4][n_pipers];
		neighbor_dir_decided = new boolean[4];
		goCorner = new boolean[4];
		if(id == 0)
			neighbor_id[0] = 3;
		else 
			neighbor_id[0] = id - 1;
		if (id == 3) {
			neighbor_id[1] = 0; 
		}else {
			neighbor_id[1] = id + 1;
		}

		for(int i = 0; i < 2; i++){
			boolean neg_y_neighbor = neighbor_id[i] == 2 || neighbor_id[i] == 3;
			boolean swap_neighbor  = neighbor_id[i] == 1 || neighbor_id[i] == 3;
			door_pos_neighbor[i] = point(door, side * 0.5, neg_y_neighbor, swap_neighbor);
		}


		for (int p = 0 ; p != n_pipers ; ++p) {
			switchStrategy[p] = false;

			// first and third position is at the door
			door_pos = sweep_pos[p][0] = pos[p][0] = pos[p][2] = point(door, side * 0.5, neg_y, swap);
			// second position is chosen randomly in the rat moving area
			pos[p][1] = null;

			// third position is slightly inside the door
			pos[p][3] = point(door, side * 0.5 + 10, neg_y, swap);

			// sweep positions
			int x = 0;
			if (n_pipers != 0)
				x = (side / (n_pipers + 1)) * (p + 1) - side / 2; 
			sweep_pos[p][1] = point(x, side * 0.1, neg_y, swap);
			pos[p][2] = sweep_pos[p][2] = point(0, side * 0.5 - 2, neg_y, swap);
			sweep_pos[p][3] = point(0, side * 0.5 + 2, neg_y, swap);
			sweep_pos_index[p] = 0;
			cell_pos[p][0] = cell_pos[p][1] = cell_pos[p][2] = point(0, side * 0.5, neg_y, swap);
			cell_pos[p][3] = point(0, side * 0.5+2.1, neg_y, swap);
			rat_catched[p] = 0.0;
		}
	}
	public static void debug(Point point){
		System.out.println(" point " + point.x + ", " + point.y);
	}
	
	// return next locations on last argument
	public void play(Point[][] pipers, boolean[][] pipers_played,
			Point[] rats, Move[] moves)
	{
		density = rats.length / (double) side;

		if(density > density_threshold){
			if (!sweepover)
				sweepStrategy(pipers, pipers_played, rats, moves);
			else {
				cellStrategy(pipers, pipers_played, rats, moves);
			}
		}
		else {
			sparseStrategy(pipers, pipers_played, rats, moves);
		}
	}

	private boolean withRats_dist(Point piper_pos, Point[] rats,int dist){ 
		for(int i = 0; i < rats.length; i++){
			if(within(piper_pos, rats[i], dist))
				return true;
		}
		return false;
	}
	
	private double distance(Point a, Point b){
		return Math.sqrt((a.x-b.x)*(a.x-b.x)+(a.y-b.y)*(a.y-b.y));
	}
	
	private double calculate_weight(Point p){
		double weight = 1 / Math.pow((100+Math.abs(distance(door_pos,p)-side* .5)),0.1);
		return weight;
	}
	
	// Update information
	// Calculate the density of different cells
	private void update_circumstance(Point[][] pipers, boolean[][] pipers_played, Point[] rats){
		
		for (int i=0;i<pipers[id].length;i++){
			piperlist.add(new Piper(pipers[id][i],pipers_played[id][i],i));
		}

		double max_weight = 0;
		int larger_side = side +4;
		int grid_num = (larger_side-1)/grid_size+1;
		double true_size = (double)larger_side/grid_num;
		for(int i=0;i<grid_num;i++){
			for(int j=0;j<grid_num;j++){
				Grid cell = new Grid(new Point((j+.5)*true_size-larger_side*.5,(i+.5)*true_size-larger_side*.5),0);
				gridlist.add(cell);
			}
		}
		// calculate how many rats are in one cell, if the rat is already influenced by the piper, decrease the weight of this rat
		for (int i=0;i<rats.length;i++){
			int catched_times = 0;
			ArrayList<Integer> index = new ArrayList<Integer>();
			// find the rats that are nearby our pipers
			for(int p=0;p<pipers[id].length;p++){
				if (pos_index[p] == 2 || pos_index[p] == 3){
					if (distance(pipers[id][p],rats[i]) < 10){
						catched_times++;
						index.add(p);
					}
				}
			}
			for(Integer j:index){
				piperlist.get(j).rats+=Math.pow(2, -catched_times+1)*calculate_weight(piperlist.get(j).pos);
			}
			if (catched_times ==0) {
				int col = new Double((rats[i].x+larger_side*.5) / true_size).intValue();
				int row = new Double((rats[i].y+larger_side*.5) / true_size).intValue();
				//gridlist.get(row*grid_num+col).rats+=Math.pow(2, -catched_times);
				gridlist.get(row*grid_num+col).rats++;
			}
		}

		//give the grid different weight according to distance between it and the gate
		for(Grid cell: gridlist){
			cell.rats = cell.rats*calculate_weight(cell.center);
			if (max_weight < cell.rats)
				max_weight = cell.rats;
		}
		{
			int col = new Double((door_pos.x + larger_side * .5) / true_size)
					.intValue();
			int row = new Double((door_pos.y + larger_side * .5) / true_size)
					.intValue();
			gridlist.get(row * grid_num + col).rats = 0.0;
		}
		int[][] count_pipers = new int[grid_num][grid_num];
		for(int g=0;g<pipers.length;g++) {
			if (g!=id){
				for(int x=0;x<grid_num;x++)
					for(int y=0;y<grid_num;y++)
						count_pipers[y][x] = 0;
				for(int i=0;i<pipers[g].length;i++){
					if (pipers_played[g][i]){
						int col = new Double((pipers[g][i].x+larger_side*.5) / true_size).intValue();
						int row = new Double((pipers[g][i].y+larger_side*.5) / true_size).intValue();
						col = (col > grid_num-1)? col-1:col;
						row = (row > grid_num-1)? row-1:row;
						col = (col < 0)? col+1:col;
						row = (row < 0)? row+1:row;
						count_pipers[row][col]++;
						Point center = new Point((row + .5) * true_size
								- larger_side * .5, (col + .5) * true_size
								- larger_side * .5);
						if ((pipers[g][i].x - center.x) < -.25 * true_size
								&& col > 0) {
							count_pipers[row][col - 1]++;
							if ((pipers[g][i].y - center.y) < -.25 * true_size
									&& row > 0) {
								count_pipers[row - 1][col - 1]++;
								count_pipers[row - 1][col]++;
							} else if ((pipers[g][i].y - center.y) > .25 * true_size
									&& row < grid_num - 1) {
								count_pipers[row + 1][col - 1]++;
								count_pipers[row + 1][col]++;
							}
						} else if ((pipers[g][i].x - center.x) > .25 * true_size
								&& col < grid_num - 1) {
							count_pipers[row][col + 1]++;
							if ((pipers[g][i].y - center.y) < -.25 * true_size
									&& row > 0) {
								count_pipers[row - 1][col - 1]++;
							} else if ((pipers[g][i].y - center.y) > .25 * true_size
									&& row < grid_num - 1) {
								count_pipers[row + 1][col - 1]++;
							}
						}
					}
				}
				for(int x=0;x<grid_num;x++)
					for(int y=0;y<grid_num;y++)
						if(count_pipers[y][x] > gridlist.get(y*grid_num+x).opponent_pipers) {
							gridlist.get(y*grid_num+x).opponent_pipers = (double)count_pipers[y][x];
						}
			}
		}

		//if the piper lose all its rats when come back to the gate, stop going back
		for(int p=0;p<pipers[id].length;p++){
			if (pos_index[p] == 2){
				Piper current_one = piperlist.get(p);
				if (!withRats_dist(piperlist.get(p).pos, rats, 5) /*|| current_one.rats < max_weight / 4 */)
					pos_index[p]=1;
				else{
					if (distance(current_one.pos,door_pos) > 15)
						gridlist.add(new Grid(current_one.pos,current_one.rats/2));
				}
			}
		}

		//sort the grid
		gridlist.sort(null);
		return;
	}

	//allocate jobs to different pipers
	private void allocate_destination(ArrayList<Grid> gridlist, ArrayList<Piper> free_pipers){
		int piper_num = free_pipers.size();
		TreeSet<Grid> sorted_grid = new TreeSet<Grid>();
		for (int i=0;i<gridlist.size();i++){
			sorted_grid.add(gridlist.get(i));
		}
		boolean[] if_free= new boolean [piper_num];
		for(int i=0;i<piper_num;i++){
			if_free[i] = true;
		}
		for(int i=0;i<piper_num;i++){
			Grid cell = sorted_grid.pollFirst();
			double max_weight = 0;
			int piper_id = -1;
			for(int j=0;j<piper_num;j++){
				if (if_free[j]){
					double weight = cell.rats / Math.pow(100+this.distance(free_pipers.get(j).pos,cell.center),0.1);
					if (max_weight < weight){
						piper_id = j;
						max_weight = weight;
					}
				}
			}
			if (piper_num -i < cell.opponent_pipers){
				cell.rats = 0.0;
			}
			else{
				cell_pos[free_pipers.get(piper_id).index][1] = cell.center;
				if_free[piper_id] = false;
				if (cell.opponent_pipers > 0)
					cell.opponent_pipers--;
				else
					cell.rats /=2;
			}
			sorted_grid.add(cell);
		}
		return;
	}

	/* Calculate if any of points is 10m away from center
	 */
	private boolean isNear(Point center, Point[] points) {
		for (Point point : points) {
			if (distance(center, point) < 10) {
				return true;
			}
		}
		return false;
	}

	public int NumRivalNearDoor(Point[][] pipers){
		int max = 0;
		for(int i = 0; i < pipers.length; i++){
			if(i == id) continue;
			int count = 0;
			for(int j = 0; j < pipers[0].length; j++){
				if(calDistance(pos[0][0], pipers[i][j]) <= 10)
					count++;      				
			}
			if (count > max) max = count;
		}
		return max;
	}
	private void cellStrategy(Point[][] pipers, boolean[][] pipers_played, Point[] rats, Move[] moves) {

		this.gridlist = new ArrayList<Grid>();
		this.piperlist = new ArrayList<Piper>();
		update_circumstance(pipers,pipers_played,rats);
		ArrayList<Piper> free_piper = new ArrayList<Piper> ();
		for(int i=0;i< pipers[id].length;i++){
			if (pos_index[i] ==1) {
				free_piper.add(new Piper(pipers[id][i],pipers_played[id][i],i));
			}
		}
		if (!gridlist.isEmpty())
			allocate_destination(gridlist,free_piper);
		else {
			for(Piper piper:free_piper){
				pos_index[piper.index] = 3;
				NumPiperAtGate ++;
			}
		}

		for (int p = 0 ; p != pipers[id].length ; ++p) {
			Point src = pipers[id][p];
			Point dst = cell_pos[p][pos_index[p]];

			// if position is reached
			if (Math.abs(src.x - dst.x) < 0.000001 &&
					Math.abs(src.y - dst.y) < 0.000001 ||
					(this.distance(src,dst) < 3 && pos_index[p] == 1)) {
				// get next position
				if (pos_index[p] == 3) {
					if (isNear(src, rats) && NumPiperAtGate-1 <= NumRivalNearDoor(pipers)){
						moves[p] = move(src, src, true);
						continue;
					} else {
						NumPiperAtGate --;
					}
				}
				if (++pos_index[p] == cell_pos[p].length) pos_index[p] = 0;
				if (pos_index[p] == 3) {
					if (!isNear(src, rats) || NumPiperAtGate > NumRivalNearDoor(pipers))
						pos_index[p] = 0;
					else
						NumPiperAtGate ++;
				}
				dst = cell_pos[p][pos_index[p]];
			}
			// set music on or off
			boolean music = false;
			if (pos_index[p] == 2 || pos_index[p] == 3) {
				music = true;
			}
			// get move towards position
			moves[p] = move(src, dst, music);
		}
	}
	private static void debug(String s){
		// System.out.println("debug: " + s);
	}
	
	private void sweepStrategy(Point[][] pipers, boolean[][] pipers_played, Point[] rats, Move[] moves) {
		if(pipers[id].length >= 2){
			for(int i = 0; i < 2; i++){
				int corner_piper_id, corner_point_id;
				if(neighbor_dir_decided[neighbor_id[i]] && !goCorner[neighbor_id[i]]){
					if(id == 1 || id == 2){
						corner_piper_id = i == 0 ? pipers[id].length - 1 : 0;
						corner_point_id = i == 0 ? 0 : 1;
					}else {
						corner_piper_id = i == 0 ? 0 : pipers[id].length - 1;
						corner_point_id = i == 0 ? 1 : 0;
					}
					sweep_pos[corner_piper_id][1] = corner_pos[corner_point_id];
					sweep_pos[corner_piper_id][2] = corner_pos[2];
				}else if(!neighbor_dir_decided[neighbor_id[i]]){
					isOtherPiperGoToConer(pipers, neighbor_id[i]);
				}
			}
		}
		for (int p = 0 ; p != pipers[id].length ; ++p) {
			Point src = pipers[id][p];
			Point dst = sweep_pos[p][pos_index[p]];

			// if position is reached
			if ( Math.abs(src.x - dst.x) < 0.000001 &&
					Math.abs(src.y - dst.y) < 0.000001) {
                if (pos_index[p] == 3) {
                    if (isNear(src, rats) && NumPiperAtGate-1 <= NumRivalNearDoor(pipers)){
                        moves[p] = move(src, src, true);
                        continue;
                    } else {
                    	NumPiperAtGate --;
                    }
                }
                if (++pos_index[p] == cell_pos[p].length) pos_index[p] = 0;
                if (pos_index[p] == 3) {
                    sweepover = true;
                    if (pos_index[p] == 3) {
                        if (!isNear(src, rats) || NumPiperAtGate > NumRivalNearDoor(pipers))
                            pos_index[p] = 0;
                        else
                        	NumPiperAtGate ++;
                    }
                }
			}
			else {
				moves[p] = move(src, dst, pos_index[p] > 1);
			}
		}
	}

	private void sparseStrategy(Point[][] pipers, boolean[][] pipers_played,
			Point[] rats, Move[] moves) {
		for (int p = 0 ; p != pipers[id].length ; ++p) {
			Point src = pipers[id][p];	

			if (pos_index[p] == 2 && !withRats(src, rats)) {
				pos_index[p] = 1;
			}

			Point dst = pos[p][pos_index[p]];

			if (dst == null) dst = getNearestRat(src, rats, p, pipers);

			// if position is reached
			if ( Math.abs(src.x - dst.x) < 0.000001 &&
					Math.abs(src.y - dst.y) < 0.000001 ||

					// Or the piper has caught his rat
					pos_index[p] == 1 && within(src, piper_rats[p], with_rat_threshold) ||

					// Or this guy is at the door and the rats at the door have been captured
					pos_index[p] == 3 && !withRats_door(door_pos, rats)) { 

				// get next position
				if (++pos_index[p] == 4) {
					pos_index[p] = 0;
					piper_at_door = -1;
				}

				if (pos_index[p] == 3) {
					if (piper_at_door >= 0 && piper_at_door != p) {
						pos_index[p] = 0;
					} else {
						piper_at_door = p;
					}
				}

				if (pos_index[p] == 1) {
					piper_rats[p] = null;
				}

				dst = pos[p][pos_index[p]];
				// generate a new position if random
				if (dst == null) {
					dst = getNearestRat(src, rats, p, pipers);
					if (within(dst, door_pos, 10)) {
						// if this returned a rat within the door, that must mean
						// all rats are at his door. make all pipers bring rats in
						pos_index[p] = 3;
						dst = pos[p][pos_index[p]];
					}
				}
			}
			// get move towards position
			moves[p] = move(src, dst, pos_index[p] > 1);
		}
	}

	public boolean samePos(Point p1, Point p2){
		if(Math.abs(p1.x - p2.x) < 0.0001 && Math.abs(p1.y - p2.y) < 0.0001) return true;
		return false;
	}

	public Point getNearestRat(Point piper_pos, Point[] rats, int piper_index, Point[][] pipers){
		double min = Double.MAX_VALUE;
		int min_index = -1;
		for(int i = 0 ; i < rats.length; i++){

			// ignore rats near door if there is already a piper there 
			if (!within(door_pos, rats[i], 10) || (within(door_pos, rats[i], 10) && !withRivalNearDoor(pipers))) {
				if (piper_at_door < 0 || !within(door_pos, rats[i], 10)) {
					double dis = calDistance(piper_pos, rats[i]);

					// if piper_rats[piper_index] is null, then the piper just got out of the door
					if(dis < min && (piper_rats[piper_index] != null || dis > 10)){
						boolean already_assigned = false;

						// check that another player isn't already handling this rat
						for (int j = 0; j < piper_rats.length; j++) {
							if (piper_rats[j] != null && sameRat(rats[i], piper_rats[j])) {
								if (j == piper_index) {
									// This piper owns this rat, update position of rat
									piper_rats[j] = rats[i];
									return rats[i];
									// Otherwise someone else does
								} else {
									already_assigned = true;
									break;
								}
							}
						}

						// if nobody else is handling this rat, this piper can take it.
						if (!already_assigned && onePiperCanHelp(pipers, rats[i])) {
							min = dis;
							min_index = i;
						}
					}
				}
			}
			if (min_index == -1) {
				min_index = gen.nextInt(rats.length);
			}
		}
		piper_rats[piper_index] = rats[min_index];
		return rats[min_index];
	}

	public boolean onePiperCanHelp(Point[][] pipers, Point rat) {
		int friends = 0;
		int max_rival = 0;
		for (int j = 0; j < pipers.length; j++) {
			int rival = 0;
			for (int k = 0; k < pipers[j].length; k++) {
				if (within(pipers[j][k], rat, 10)) {
					if (j == id) {
						friends++;
					} else {
						rival++;
					}
				}
			}
			if(rival > max_rival) {
				max_rival = rival;
			}
		}
		int density = friends - max_rival;
		return density >= -1;
	}

	public boolean withRats(Point piper_pos, Point[] rats){ 
		for(int i = 0 ; i < rats.length; i++){
			if(within(piper_pos, rats[i], with_rat_threshold))
				return true;
		}
		return false;
	}
	public boolean withRats_door(Point piper_pos, Point[] rats){ 
		for(int i = 0 ; i < rats.length; i++){
			if(within(piper_pos, rats[i], 10))
				return true;
		}
		return false;
	}

	public boolean within(Point p1, Point p2, int distance){
		double length = calDistance(p1, p2);
		if(length <= distance)
			return true;
		return false;
	}

	public double calDistance(Point p1, Point p2){
		double dx = p1.x - p2.x;
		double dy = p1.y - p2.y;
		return Math.sqrt(dx * dx + dy * dy);
	}

	public boolean sameRat(Point p1, Point p2) {
		return calDistance(p1, p2) <= 1;
	}
	public boolean withRivalNearDoor(Point[][] pipers){
		for(int i = 0; i < pipers.length; i++){
			if(i == id) continue;
			for(int j = 0; j < pipers[0].length; j++){
				if(calDistance(pos[0][0], pipers[i][j]) <= 20)
					return true;        				
			}
		}
		return false;
	}

	public boolean isOtherPiperGoToConer(Point[][] pipers, int neighbor_id){

		if(outDoorNeighbor[neighbor_id]){
			neighbor_dir_decided[neighbor_id] = true;
			goCorner[neighbor_id] = isAnyPiperGoCorner(pipers, neighbor_id);
			return goCorner[neighbor_id];
		}else {

			boolean tmp = outDoorAllPipers(pipers, neighbor_id);
			if(tmp) 
				outDoorNeighbor[neighbor_id] = true;
			return false;
		}
	}

	public boolean outDoorAllPipers(Point[][] pipers, int neighbor_id){
		boolean ret = true;
		for(int i = 0; i < pipers[0].length; i++){
			ret = ret && outDoor(pipers, neighbor_id, i);
			if(!ret) return false;
			neighbor_old_pos[neighbor_id][i] = pipers[neighbor_id][i];
		}
		return ret;

	}
	public boolean isAnyPiperGoCorner(Point[][] pipers, int neighbor_id){
		for(int i = 0; i < pipers[0].length; i++){
			double xDiff = pipers[neighbor_id][i].x - neighbor_old_pos[neighbor_id][i].x;
			double yDiff = pipers[neighbor_id][i].y - neighbor_old_pos[neighbor_id][i].y;
			double tan;
			if(neighbor_id == 0 || neighbor_id == 2)
				tan = Math.abs(yDiff / xDiff);
			else {
				tan = Math.abs(xDiff / yDiff);
			}

			double thresh = (double) 20 / 100;
			debug("tan " + tan + "thresh " + thresh + "id " +neighbor_id);
			if(tan <= thresh)
				return true;
		}
		return false;
	}
	public boolean outDoor(Point[][] pipers, int neighbor_id, int piper_id){
		if(neighbor_id == 0 || neighbor_id == 2){
			if(Math.abs(pipers[neighbor_id][piper_id].y) < side / 2) 
				return true;
			else 
				return false;
		}else{
			if(Math.abs(pipers[neighbor_id][piper_id].x) < side / 2) 
				return true;
			else 
				return false;
		}
	}
}


class Grid implements Comparable<Grid>{
	Point center;
	Double rats;
	Double opponent_pipers =0.0;
	public Grid(Point center, double rats) {
		this.center = center;
		this.rats = rats;
	}

	public int compareTo(Grid g1) {
    	return (new Double(g1.rats/(g1.opponent_pipers+1))).compareTo
    			(new Double(this.rats/(this.opponent_pipers+1)));
	}
}

class Piper {
	Point pos;
	Boolean playing;
	double rats =0.0;
	double opponent_pipers =0.0;
	int index;
	public Piper(Point pos, boolean playing, int index) {
		this.pos = pos;
		this.playing = playing;
		this.index = index;
	}
}
