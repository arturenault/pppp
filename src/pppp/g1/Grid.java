package pppp.g1;

import pppp.sim.Point;

public class Grid {
    public double cellSize;
    public int side;
    public Cell[][] grid; // rows x columns

    /**
     * Create a grid of square cells each of side length size.
     *
     * @param side   Side of the grid.
     * @param slices Number of devisions by which to devide the side.
     */
    public Grid(int side, double cellSize) {
        // The board consists of size^2 number of square cells.
        this.side = side;
        this.cellSize = cellSize;
        int slices = (int) (side / cellSize);
        double offset = (double) side / 2;
        this.grid = new Cell[slices][slices];
        for (int i = 0; i < slices; i++) {
            for (int j = 0; j < slices; j++) {
                this.grid[i][j] = new Cell(
                        new Point(  // X, Y - bottom-left corner
                                (i * cellSize) - offset,
                                (j * cellSize) - offset
                        ),
                        new Point(  // X, Y - center
                                (i + 0.5) * cellSize - offset,
                                (j + 0.5) * cellSize - offset
                        ),
                        cellSize, 0 // initialize with zero weight
                );
            }
        }
    }

    /**
     * Update the weights of all cells.
     *
     * @param pipers        The positions of all the pipers on the board.
     * @param pipers_played The state of playing music for all the pipers.
     * @param rats          The positions for all rats on the board.
     */
    public void updateCellWeights(
            Point[][] pipers, boolean[][] pipers_played, Point[] rats
    ) {
        // Reset cell weights
        for (Cell[] row : this.grid) {
            for (Cell cell : row) {
                cell.weight = 0;
            }
        }

        // Compute each cell's weight
        for (Point rat : rats) {
            Cell cell = getCellContainingPoint(rat);
            if (cell != null) {
                cell.weight++;
            }
        }
    }
    
    public double avgWeights() {
    	int sum = 0;
    	int n_cells = 0;
    	for(Cell[] rows : grid) {
    		for(Cell cell : rows) {
    			sum += cell.weight;
    			++n_cells;
    		}
    	}
    	return sum/n_cells;
    }
    
    public String visualize() {
    	double average = avgWeights();
    	String out = ""+average+"\n";
    	for(int i = 0; i < grid.length; ++i) {
    		for(int j = 0; j < grid[i].length; ++j) {
    			int index = grid[i].length - 1 - i;
    			int weight = grid[j][index].weight;
    			String str = weight+"";
    			if(weight > 1.3*average) str = "+"+str;
    			if(weight > 2*average) str = "+"+str;
    			out += String.format("%1$4s", str);
    		}
    		out += "\n";
    	}
    	return out;
    }
    
    public String toString() {
    	return visualize();
    }
 
    /**
     * Find the cell containing the given point.
     *
     * @param point The position for which the cell needs to be found.
     */
    private Cell getCellContainingPoint(Point point) {
        for (Cell[] row : grid) {
            for (Cell cell : row) {
                double left = cell.corner.x;
                double bottom = cell.corner.y;
                double top = cell.corner.y + cell.size;
                double right = cell.corner.x + cell.size;

                if (point.y >= bottom && point.y < top && point.x >= left &&
                        point.x < right) {
                    return cell;
                }
            }
        }
        return null;
    }
}
