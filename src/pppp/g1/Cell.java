package pppp.g1;

import pppp.sim.Point;

/**
 * Cells in a grid that make up the board.
 */
public class Cell implements Comparable<Cell> {
    // corner is the coordinates of the top-left corner of the cell.
    public Point corner = null;
    // center is the coordinate of the center of the cell.
    public Point center = null;
    public double size = 0;
    public int weight = 0;

    public Cell(Point corner, Point center, double size, int weight) {
        this.corner = corner;
        this.center = center;
        this.size = size;
        this.weight = weight;
    }

    public int compareTo(Cell other) {
        return -Integer.compare(this.weight, other.weight);
    }
}
