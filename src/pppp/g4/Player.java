package pppp.g4;

import pppp.sim.Point;
import pppp.sim.Move;

import java.util.*;

public class Player implements pppp.sim.Player {

    // see details below
    private int id = -1;
    private int side = 0;
    private int[] pos_index = null;
    private Point[][] pos = null;
    private Point[] random_pos = null;
    private Random gen = new Random();
    private int dst_no = 0;
    private int total_regions = 0;
    Boolean[] completed_sweep = null;
    private Cell[] grid = null;
    private static double density_threshold = 0;
    private Boolean sparse_flag = false;
    // Map<Integer, Point> piper_to_cell = null;
    int tick = 0;
    Point our_gate = null;
    Point[] box_boundaries = new Point[2];
    Boolean[] isBoundaryRat = null; // flag to set for playing music when rat in boundary

    Map<Integer, Point> piper_to_cell = new HashMap<Integer, Point>();
    Map<Point, Set<Integer>> cell_to_piper = new HashMap<Point, Set<Integer>>();

    // create move towards specified destination
    private static Move move(Point src, Point dst, boolean play) {
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

    private static int  getRatsCountOnDst(Point[] rats, Point dst,int nearbyRatScanRadius2){
    	// create move towards specified destination
    	HashSet<Point> nearbyRats= new HashSet<Point>();
    	for (Point rat : rats) {
			if ((Utils.distance(dst, rat) < nearbyRatScanRadius2)) {
				nearbyRats.add(rat);
			}
		}
		return nearbyRats.size();
    }
    // generate point after negating or swapping coordinates
    private static Point point(double x, double y,
                               boolean neg_y, boolean swap_xy) {
        if (neg_y) y = -y;
        return swap_xy ? new Point(y, x) : new Point(x, y);
    }

    public Point get_closest_rat(Point[] rats, Point[] box_boundaries, Point piper)
    {  // returns rat + position which are within the box_boundary near the gate and closest to that piper //
        if (box_boundaries.length != 2)
            return null; 
        //List<Point> closest_rats = new ArrayList<Point>();
        double closest_distance = Double.POSITIVE_INFINITY;
        Point closest_rat = null;
        for(Point rat : rats)
        {
            if((Math.min(box_boundaries[0].x, box_boundaries[1].x) <= rat.x) && (rat.x <= (Math.max(box_boundaries[0].x, box_boundaries[1].x))) && (Math.min(box_boundaries[0].y, box_boundaries[1].y) <= rat.y) && (rat.y <= (Math.max(box_boundaries[0].y, box_boundaries[1].y))))
            { // if rat in box_boundary
                //closest_rats.add(rat);
                if ((Utils.distance(piper, rat) < closest_distance) && (closest_distance >= 10.0))
                {
                    closest_rat = rat;
                    closest_distance = Utils.distance(piper, rat);
                }

            }
        }
        return closest_rat;
    }


    double getSweepRadius(Point[] rats, Point[] boundaries, int id){
        double radius = side/3;
        int sum_strip1 = 0;
        int sum_strip2 = 0;
        int sum_rem = 0;
        double avg = 0.0;

        for(Point rat: rats)
        {
            if (id == 0 || id == 2)
            {
                //Only consider y axis boundaries
                if ((rat.y >= Math.min(boundaries[0].y, boundaries[1].y)) && ((rat.y < Math.max(boundaries[0].y, boundaries[1].y))))
                    {sum_strip1 += 1;}
                else if ((rat.y >= Math.min(boundaries[1].y, boundaries[2].y)) && ((rat.y < Math.max(boundaries[1].y, boundaries[2].y))))
                    {sum_strip2 += 1;}
                else
                    {sum_rem += 1;}               
            }
            else if (id == 1 || id == 3)
            {
                // id = 1 or 3 | Considering X-axis only
                if ((rat.x > Math.min(boundaries[0].x, boundaries[1].x)) && ((rat.x < Math.max(boundaries[0].x, boundaries[1].x))))
                    {sum_strip1 += 1;}  
                else if ((rat.x > Math.min(boundaries[1].x, boundaries[2].x)) && ((rat.x < Math.max(boundaries[1].x, boundaries[2].x))))
                    {sum_strip2 += 1;}
                else
                    {sum_rem += 1;  }     
            } 
        }
        avg = (sum_strip1 + sum_strip2+ sum_rem)/2;
        if (sum_strip2 > avg )
        {
            radius = side/2.5;
        }
        else if (sum_strip1 >avg) {
            radius = side/4;
        }
        // System.out.println("Total rats : "+rats.length+ " | strip 1 : "+ sum_strip1 + " | strip 2 : "+ sum_strip2 + " | remaining "+ sum_rem + " | RADIUSLinkedFolder : "+radius);
        return radius;
    }

    double getSweepRadius2(Point[] rats, Point[] boundaries, int id){
        double radius = side/3;
        int sum_strip1 = 0;
        int sum_strip2 = 0;
        int sum_strip3 = 0;
        int sum_rem = 0;
        double avg = 0.0;

        for(Point rat: rats)
        {
            if (id == 0 || id == 2)
            {
                //Only consider y axis boundaries
                if ((rat.y >= Math.min(boundaries[0].y, boundaries[1].y)) && ((rat.y < Math.max(boundaries[0].y, boundaries[1].y))))
                    {sum_strip1 += 1;}
                else if ((rat.y >= Math.min(boundaries[1].y, boundaries[2].y)) && ((rat.y < Math.max(boundaries[1].y, boundaries[2].y))))
                    {sum_strip2 += 1;}
                else if ((rat.y >= Math.min(boundaries[2].y, boundaries[3].y)) && ((rat.y < Math.max(boundaries[2].y, boundaries[3].y))))
                     {sum_strip3 += 1;}
                else
                    {sum_rem += 1;}  //between 75 to 100 all rats we leave this as too risky area..             
            }
            else if (id == 1 || id == 3)
            {
                // id = 1 or 3 | Considering X-axis only
                if ((rat.x > Math.min(boundaries[0].x, boundaries[1].x)) && ((rat.x < Math.max(boundaries[0].x, boundaries[1].x))))
                    {sum_strip1 += 1;}  
                else if ((rat.x > Math.min(boundaries[1].x, boundaries[2].x)) && ((rat.x < Math.max(boundaries[1].x, boundaries[2].x))))
                    {sum_strip2 += 1;}
                else if ((rat.x > Math.min(boundaries[2].x, boundaries[3].x)) && ((rat.x < Math.max(boundaries[2].x, boundaries[3].x))))
                    {sum_strip3 += 1;}
                else
                    {sum_rem += 1;  }  //between 75 to 100 all rats we leave this as too risky area..     
            } 
        }
        avg = (sum_strip1 + sum_strip2 + sum_strip3)/3;
        if (sum_strip3 > avg )
        {
            radius = side/4*1;
        }
        else if (sum_strip2> avg) {
            radius = side/4*2;
        }else if (sum_strip1> avg) {
            radius = side/4*3;
        }else{//default do a long scan
            radius=side/4*3;
        }
        // System.out.println("Total rats : "+rats.length+ " | strip 1 : "+ sum_strip1 + " | strip 2 : "+ sum_strip2 + " | remaining "+ sum_rem + " | RADIUS : "+radius);
        return radius;
    }


    // specify location that the player will alternate between
    public void init(int id, int side, long turns,
                     Point[][] pipers, Point[] rats) {
        try {
            this.id = id;
            this.side = side;
            density_threshold = 50/side*side;
            // density_threshold = 0.005;
            int n_pipers = pipers[id].length;
            pos = new Point[n_pipers][8];
            random_pos = new Point[n_pipers];
            pos_index = new int[n_pipers];
            completed_sweep = new Boolean[n_pipers];
            Arrays.fill(completed_sweep, Boolean.FALSE);
            isBoundaryRat = new Boolean[n_pipers];
            Arrays.fill(isBoundaryRat, Boolean.FALSE);

            this.grid = create_grid(this.side, rats.length);
            boolean neg_y = id == 2 || id == 3;
            boolean swap = id == 1 || id == 3;
            our_gate = point(0, side * 0.5 * 1, neg_y, swap);
            update_grid_weights(rats, pipers, our_gate);

            // sort the cells in the Cell[] grid in descending order of weight/number_of_rats
            Arrays.sort(this.grid, Collections.reverseOrder());
            piper_to_cell = get_piper_to_cell(pipers);
            for (int p=0; p<pipers[id].length; p++) {
                random_pos[p] = piper_to_cell.get(p);
            }
            
            if (isSparse(rats.length, side))
                    sparse_flag = true;
            for (int p = 0; p != n_pipers; ++p) {
                // spread out at the door level
                double door = 0.0;
                if (n_pipers != 1) door = p * 1.8 / (n_pipers - 1) - 0.9;
                // pick coordinate based on where the player is
                
                our_gate = point(door, side * 0.5, neg_y, swap);
                Point before_gate = point(door, side * 0.5 * .85, neg_y, swap);
                Point inside_gate = point(door, side * 0.5 * 1.2, neg_y, swap);// first and third position is at the door
                Point[] boundaries = new Point[3];
                boundaries[0] = point(side * 0.5 * 1, side * 0.5 * 1, neg_y, swap); // At the door
                boundaries[1] = point(side * 0.5 * 0.5, side * 0.5 * 0.5, neg_y, swap); // Between door and center
                boundaries[2] = point(0, 0, neg_y, swap); // At the center of the grid
                double distance = getSweepRadius(rats, boundaries, id);
                 
                //fixed new for getSweepRadius2()
                /*
                Point[] boundaries2 = new Point[3];
                boundaries2[0] = point(side * 0.5 * 1, side * 0.5 * 1, neg_y, swap); // At the door
                boundaries2[1] = point(side * 0.5 * 0.5, side * 0.5 * 0.5, neg_y, swap); // Between door and center
                boundaries2[2] = point(0, 0, neg_y, swap); // At the center of the grid
                 double distance = getSweepRadius(rats, boundaries2, id);
                */


                // New box_boundaries based on gate_no for piper to change path slightly to pick up rats near its gate
                //Point[] box_boundaries = new Point[2];
                box_boundaries[0] = point(side * 0.5 * -0.5, side * 0.5 * 0.75, neg_y, swap);
                box_boundaries[1] = point(side * 0.5 * 0.5, side * 0.5, neg_y, swap);
                System.out.println("My gate : " + our_gate.x +" | "+ our_gate.y);
                System.out.println("ID : "+id+ " | boundary 0 x : "+box_boundaries[0].x + " | boundary 1 x : "+box_boundaries[1].x + " | boundary 0 y : "+box_boundaries[0].y + " | boundary 1 y : "+box_boundaries[1].y);
                    
                double theta = Math.toRadians(p * 90.0 / (n_pipers - 1) + 45);
                pos[p][0] = point(door, side * 0.5, neg_y, swap);
                pos[p][1] = (n_pipers==1 ? null: point(distance * Math.cos(theta), (side/2) + (-1) * distance * Math.sin(theta), neg_y, swap));
                pos[p][2] = before_gate;
                pos[p][3] = inside_gate;
                pos[p][4] = before_gate;
                // sixth position is chosen randomly in the rat moving areaons;
                pos[p][5] = null;

                // seventh and eighth positions are outside the rat moving area
                pos[p][6] = before_gate;
                pos[p][7] = inside_gate;

                // start with first position
                pos_index[p] = 0;
                dst_no = 0;
            }   

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Cell[] create_grid(int side, int number_of_rats) {
    /*
     Returns a Cell[] array of length = number of cells = side/20 * side/20
     */
        int cell_side;

        if(isSparse(number_of_rats, side)) {
            cell_side = 1;
        }
        else {
            cell_side = side/5;
        }

        int dim = 0;
        dim = side/cell_side;
        // if (side % cell_side == 0)
        //     dim = side/cell_side;
        float half = side/2;
        Cell[] grid = new Cell[dim*dim];
        
        for(int i=0; i < dim; i++) {
            for(int j=0; j < dim; j++) {
                Cell cell = new Cell(
                                cell_side,
                                 new Point(  // X, Y - center
                                           (i + 0.5) * cell_side - half,
                                           (j + 0.5) * cell_side - half
                                           ));

                grid[(i * dim) + j] = cell;
            }
        }
        
        Cell.counter = 0;
        return grid;
    }
    
    public void display_grid(Cell[] grid) {
        try{
            int x = grid.length;
            double dimD = Math.sqrt(x);
            int dim = (int) dimD;
            int k = -1;
            for (int i=0; i < dim; i++) {
                for (int j = 0; j < dim; j++) {
                    k = k+1;
                    System.out.print(grid[k].weight + " ");
                }
                System.out.println();
            }
            System.out.println();
        }

        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    public Cell find_cell(Point rat) {
        for (int i=0; i<this.grid.length; i++) {
            Cell cell = this.grid[i];
            double x1 = cell.center.x - cell.side/2;
            double x2 = cell.center.x + cell.side/2;
            double y1 = cell.center.y + cell.side/2;
            double y2 = cell.center.y - cell.side/2;

            if (rat.x >= x1 && rat.x <= x2 && rat.y >= y2 && rat.y <= y1) {
                return cell;
            }
        }
        return null;
    }

    public int isAvailableRat(Point rat, Point[][] pipers){
        for (int i=0; i<4; i++){ 
            for(int j=0; j<pipers[i].length; j++){
                if (Utils.distance(pipers[i][j], rat) <= 10){
                    if (i == id) 
                        return 3; 
                        // status 3 means not available and with teammate
                    else 
                        return 2;
                        // status 2 means not available and with opponent
                }
            }
        }
        return 1;
        // status 1 means rat is available
    }
    
    public void update_grid_weights(Point[] rats, Point[][] pipers, Point our_gate) {
            for (int i=0; i < this.grid.length; i++) {
                this.grid[i].weight = 0;
            }
            Point[] our_pipers = pipers[id];

            for (Point rat: rats) {
                Cell cell = find_cell(rat);
                if (cell != null) {
                    cell.weight = cell.weight + 10;

    //                int status = isAvailableRat(rat, pipers);
    //                if (status == 1){
    //                    // status 1 means rat is available
    //                    // if (Utils.distance(rat, our_gate) <= 0.6*side && Utils.distance(rat, our_gate) > side/10){
    //                    //     cell.weight = cell.weight + 10;
    //                    // }else {
    //                    //     cell.weight = cell.weight + 6;
    //                    // }      
    //                    cell.weight = cell.weight + 3;                      
    //                }
    //                else if (status == 2 )  
    //                    // status 2 means not available and with opponent
    //                    cell.weight = cell.weight - cell.weight/4;
    ////                else
    ////                    // status 3 means not available and with teammate
    ////                    cell.weight = cell.weight + 0;
    //
    //                for (Point piper: our_pipers) {
                       if (Utils.distance(our_gate, rat) <= side && Utils.distance(rat, our_gate) > side/10)
                           cell.weight = cell.weight + 1;
    //                }
                }

            }
        }
    
    public Map<Integer, Point> get_piper_to_cell(Point[][] pipers ) {
        Cell[] grid_copy = Arrays.copyOf(grid, grid.length);
        Map<Point, Integer> n_pipers_needed = new HashMap<Point, Integer>();
        Set<Integer> unassigned_pipers = new HashSet<Integer>();
        int i;
        int cells_to_consider;
        int sum;
        int avg;
        int n_p_to_i;
        int piper;
        List<Cell> non_zero_cells = new ArrayList<Cell>();
        Iterator<Cell> iter_non_zero_cells;
        Iterator<Integer> iter_unassigned_pipers;
        Map<Point, Set<Integer>> cell_to_piper_copy = new HashMap<Point, Set<Integer>>(cell_to_piper);
        cell_to_piper = new HashMap<Point, Set<Integer>>();
        
        // add all pipers to unassigned list
        for (i = 0; i< pipers[id].length; i++) {
            unassigned_pipers.add(i);
        }
        for (Map.Entry<Integer, Point> entry: piper_to_cell.entrySet()) {
            // if a destination is assigned to a piper, remove it from set of unassigned pipers 
            if (entry.getValue() != null)
                unassigned_pipers.remove(entry.getKey());
        }
        
        // start with considering all pipers
        int remaining_pipers = pipers[id].length;
        
        // consider only those many cells as many pipers are left
        cells_to_consider = remaining_pipers;
        
        // add all cells to non zero cells
        for (int k=0; k < grid_copy.length; k++) {
            non_zero_cells.add(grid_copy[k]);
        }
        
        // until all pipers are assigned to something
        while (remaining_pipers > 0) {
            int prev_length = non_zero_cells.size();
            if (prev_length > cells_to_consider) {
                // if non_zero_cells has more than cells_to_consider elements, pick cells_to_consider number of cells 
                non_zero_cells = non_zero_cells.subList(0, cells_to_consider);
            }
            // else non_zero_cells already has fewer than cells_to_consider cells

            sum = 0;

            iter_non_zero_cells = non_zero_cells.iterator();
            for (i=0; i<cells_to_consider; i++) {
                Cell next_item = iter_non_zero_cells.next();
                // add non zero cell values
                if (next_item.weight != 0)
                    sum += next_item.weight;
                else
                // if weight is zero, remove from non_zero_cells
                    iter_non_zero_cells.remove();
            }
            // update cells_to_consider
            cells_to_consider = non_zero_cells.size();
            if (cells_to_consider == 0)
                break;
            
            avg = sum/cells_to_consider;

            i = 0;
            iter_non_zero_cells = non_zero_cells.iterator();
            Cell this_cell;
            while(i < cells_to_consider) {
                if (iter_non_zero_cells.hasNext())
                    this_cell = iter_non_zero_cells.next();
                else 
                    break;
                // number of pipers to send to i-th cell = n_p_to_i
                n_p_to_i = this_cell.weight/avg;
                
                // update map of how many pipers needed for top destinations
                int previous_assigned = 0;
                if (n_pipers_needed.containsKey(this_cell.center))
                    previous_assigned = n_pipers_needed.get(this_cell.center);
                n_pipers_needed.put(this_cell.center, previous_assigned + n_p_to_i);
                
                // update remaining_pipers
                remaining_pipers -= n_p_to_i;
                
                // update weight and set to remainder
                this_cell.weight = this_cell.weight % avg;

                // if weight becomes 0, remove from non_zero_cells
                if (this_cell.weight == 0)
                    iter_non_zero_cells.remove();
                
                
                i++;
            }
            // look at remaining_piper number of cells. But we might not have enough non_zero_cells, so take the minimum of the two
            cells_to_consider = Math.min(remaining_pipers, non_zero_cells.size());

            // if cells_to_consider is 0, break
            if (cells_to_consider == 0)
                break;
            
            // end while loop here
        }
            
        iter_unassigned_pipers = unassigned_pipers.iterator();
        // maintain previous assignments if destination hasn't changed
        for (Point destination: n_pipers_needed.keySet()) {
            if (cell_to_piper_copy.containsKey(destination)) {
                // if someone was assigned to this destination already, copy the set back this time
                cell_to_piper.put(destination, cell_to_piper_copy.get(destination));
            }
            else {
                // if no one was assigned to this destination, add the destination to the map with an empty set
                cell_to_piper.put(destination, new HashSet<Integer>());
            }
        }
        
        for (Map.Entry<Point, Integer> entry: n_pipers_needed.entrySet()) {
            Point dst = entry.getKey();
            int need = entry.getValue();
            
            if (cell_to_piper.get(dst).size() < need) {
                // if the given cell is assigned lesser than what it needs, find next free piper and assign it to this cell
                i = 0;
                int previously_assigned =  cell_to_piper.get(dst).size();
                while (i < need - previously_assigned) {
                    if (iter_unassigned_pipers.hasNext())
                    {
                        piper = iter_unassigned_pipers.next();
                        cell_to_piper.get(dst).add(piper);
                        iter_unassigned_pipers.remove();
                        piper_to_cell.put(piper, dst);
                    }
                    i++;
                }
            }
        }
        
        iter_unassigned_pipers = unassigned_pipers.iterator();
        while (remaining_pipers > 0 && iter_unassigned_pipers.hasNext()) {
            
            piper = iter_unassigned_pipers.next();
            piper_to_cell.put(piper, grid_copy[0].center);
            iter_unassigned_pipers.remove();
            remaining_pipers -= 1;
        }   
         
        return piper_to_cell; 
    }
    public void print_map(Map<Integer, Point> piper_to_cell) {
        System.out.println("====");
        for (Map.Entry<Integer, Point> entry : piper_to_cell.entrySet()) {
            System.out.println(entry.getKey()+" : "+entry.getValue().toString());
        }
    }

    // Yields the number of rats within range
    static int num_captured_rats(Point piper, Point[] rats) {
        int num = 0;
        for (Point rat : rats)
            num += Utils.distance(piper, rat) <= 10 ? 1 : 0;
        return num;
    }

    static boolean isSparse(double ratsLength, double side) {
        double density = ratsLength / (side * side);
        if (density <= density_threshold) 
            return true;
        else 
            return false;
    }

    // return next locations on last argument
    public void play(Point[][] pipers, boolean[][] pipers_played,
                     Point[] rats, Move[] moves) {
        
        try {
            // if (tick % (side * 2 * 0.6) == 0) {
            System.out.println("\n\n");
        
            tick++;
            System.out.println("Play() called!");

            grid = create_grid(side, rats.length);
            update_grid_weights(rats, pipers, our_gate);
            // sort the cells in the Cell[] grid in descending order of weight/number_of_rats
            Arrays.sort(this.grid, Collections.reverseOrder());
            piper_to_cell = get_piper_to_cell(pipers);

            System.out.println("Piper to cell map:");
            for (Map.Entry<Integer, Point> entry: piper_to_cell.entrySet()) {
                if (entry.getValue() != null)
                    System.out.println(entry.getKey() + " : (" + entry.getValue().x + ", " + entry.getValue().y + ")");
                else
                    System.out.println(entry.getKey() + " : null");
            }


            for (int p = 0; p != pipers[id].length; ++p) {

                System.out.println("\n" + "piper:  " + p);
                System.out.println("pos index is: " + pos_index[p]);
                Point src = pipers[id][p];
                System.out.println("src: " + src.x + ", " + src.y);
                Point dst = pos[p][pos_index[p]];
                

                if ((sparse_flag || ((!sparse_flag) && completed_sweep[p])) && (pos_index[p] == 1 ))
                {
                    pos_index[p] = 4;
                }
                // if null then get random position
                // if (dst == null) {
                //     dst = (random_pos[p]==null? piper_to_cell.get(p):random_pos[p]);
                // }
                if (dst == null) {
                    dst = random_pos[p];
                }

                //if nothing on DST then rest ?
                if(pos_index[p] == 5 && getRatsCountOnDst( rats,   dst, 10)==0 ){
                	System.out.println("NOTHING at destination");
                }
                // if position is reached
                // if (dst!=null && Math.abs(src.x - dst.x) < 0.000001 &&
                    // Math.abs(src.y - dst.y) < 0.000001) {
                if ((Math.abs(src.x - dst.x) < 0.000001 &&
                    Math.abs(src.y - dst.y) < 0.000001) || (pos_index[p] == 5 && getRatsCountOnDst( rats,   dst, 10)==0 )) {
                    // discard random position
                    if (dst == random_pos[p]) random_pos[p] = null;
                    // get next position
                    if (++pos_index[p] == pos[p].length){
                        pos_index[p] = 0;
                        completed_sweep[p] = true;
                        isBoundaryRat[p] = Boolean.FALSE;
                    }
                    dst = pos[p][pos_index[p]];
                    // generate a new position if random
                    if (dst == null || pos_index[p] == 5) {
                        System.out.println("Assigned new dst from map");
                        random_pos[p] = dst = piper_to_cell.get(p);
                    }
                }
                System.out.println("new dst: " + dst.x + ", " + dst.y);
                System.out.println("pos index is now 1: " + pos_index[p]);
            

                if (num_captured_rats(pipers[id][p], rats) == 0)
                    isBoundaryRat[p] = Boolean.FALSE;
                if (pos_index[p] == 6 && num_captured_rats(pipers[id][p], rats) == 0) {

                    pos_index[p] = 5;
                    grid = create_grid(side, rats.length);
                    update_grid_weights(rats, pipers, our_gate);
                    // sort the cells in the Cell[] grid in descending order of weight/number_of_rats
                    Arrays.sort(this.grid, Collections.reverseOrder());
                    piper_to_cell = get_piper_to_cell(pipers);
                }
                if ((pos_index[p] == 6) && (num_captured_rats(pipers[id][p], rats) >= 1) && (get_closest_rat(rats, box_boundaries, pipers[id][p]) != null) && (!isBoundaryRat[p]))
                {        
                    pos_index[p] = 5;
                    random_pos[p] = dst = get_closest_rat(rats, box_boundaries, pipers[id][p]);
                    // System.out.println("New destination : "+dst.x + " | " + dst.y);
                    isBoundaryRat[p] = Boolean.TRUE;

                }

                if ((pos_index[p] == 3 || pos_index[p] == 7) && num_captured_rats(pipers[id][p], rats) == 0)
                    pos_index[p] = 4;
                if ((pos_index[p] == 5 ) && (!isBoundaryRat[p])){
                    // just got free to do something
                    // reassign piper to null destination first - no longer assigned to previous cell 
                    // (because we're using non-null values in this map to compute set of unassigned pipers)
                    System.out.println("setting to null in map, but dst is still " + dst.x + ", " + dst.y);
                    piper_to_cell.put(p, null);

                    // update grid now
                    grid = create_grid(side, rats.length);
                    update_grid_weights(rats, pipers, our_gate);
                    // sort the cells in the Cell[] grid in descending order of weight/number_of_rats
                    Arrays.sort(this.grid, Collections.reverseOrder());
                    piper_to_cell = get_piper_to_cell(pipers);
                    if (random_pos[p] == null)
                        random_pos[p] = dst = piper_to_cell.get(p);
                }

                System.out.println("pos index now 2: " + pos_index[p]);

                // get move towards position
                moves[p] = move(src, dst, (pos_index[p] > 1 && pos_index[p] < 4) || (pos_index[p] > 5) || (isBoundaryRat[p]));
                System.out.println("Piper to cell map:");
                for (Map.Entry<Integer, Point> entry: piper_to_cell.entrySet()) {
                    if (entry.getValue() != null)
                        System.out.println(entry.getKey() + " : (" + entry.getValue().x + ", " + entry.getValue().y + ")");
                    else
                        System.out.println(entry.getKey() + " : null");
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}