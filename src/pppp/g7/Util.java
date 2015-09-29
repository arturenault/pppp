package pppp.g7;

import pppp.sim.Move;
import pppp.sim.Point;

import java.util.*;

/**
 * Created by rbtying on 9/16/15.
 */
public class Util {
    public boolean neg_x, neg_y, swap_xy;

    public Util(boolean neg_x, boolean neg_y, boolean swap_xy) {
        this.neg_x = neg_x;
        this.neg_y = neg_y;
        this.swap_xy = swap_xy;
    }

    // create move towards specified destination
    public static Move moveToLoc(Point src, Point dst, boolean play) {
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

    public String stateName(Player.PlayerState p) {
        return stateName(p.getClass());
    }

    public String stateName(Class c) {
        if (c == null) {
            return "Object";
        }
        String n = c.getCanonicalName();
        if (n == null) {
            return "^" + stateName(c.getSuperclass());
        } else {
            return n.substring(n.lastIndexOf('.') + 1);
        }
    }

    /**
     * Finds indices of points within a given distance
     *
     * @param pos      the position to compute from
     * @param otherPos the array of other positions
     * @param distance the maximum distance to compare
     * @return list of indices of points within distance.
     */
    public List<Integer> getIndicesWithinDistance(Point pos, Point[] otherPos, double distance) {
        List<Integer> indices = new LinkedList<>();
        for (int i = 0; i < otherPos.length; ++i) {
            if (otherPos[i] != null && pos.distance(otherPos[i]) < distance) {
                indices.add(i);
            }
        }
        return indices;
    }

    /**
     * Note: transformPoint(transformPoint(p)) == p
     *
     * @param p point to transform
     * @return point transformed into unified coordinate system, or back
     */
    public Point transformPoint(Point p) {
        double x = p.x;
        double y = p.y;
        if (neg_y) {
            y = -y;
        }
        if (neg_x) {
            x = -x;
        }
        return swap_xy ? new Point(y, x) : new Point(x, y);
    }

    /**
     * Note: transformMove(transformMove(m)) == m
     *
     * @param m move to transform
     * @return move transformed into unified coordinate system, or back
     */
    public Move transformMove(Move m) {
        double dx = m.dx;
        double dy = m.dy;

        if (neg_y) {
            dy = -dy;
        }
        if (neg_x) {
            dx = -dx;
        }
        return swap_xy ? new Move(-dy, -dx, m.play) : new Move(dx, dy, m.play);
    }

    public boolean isInState(Player.PlayerState pstate, Player.PlayerState desiredState) {
        return pstate.getClass() == desiredState.getClass() && pstate.sameStateAs(desiredState);
    }

    public boolean groupIsInState(List<Integer> group, Player.PlayerState states[], Player.PlayerState desiredState) {
        if (group.size() <= 1) {
            return true;
        }

        for (Integer p : group) {
            if (!isInState(states[p], desiredState)) {
                return false;
            }
        }
        return true;
    }

    public int getClosestRat(int id, Player.PlayerState states[], int pidx, Point[][] piperPos, Move[][] piperVel,
                             boolean[][] pipers_played, Point[] ratPos, int depth) {
        ArrayList<Double> distances = new ArrayList<>();
        HashMap<Double, Integer> rat_lut = new HashMap<>();

        for (int i = 0; i < ratPos.length; ++i) {
            Point rat_position = ratPos[i];
            double distance = piperPos[id][pidx].distance(rat_position);
            int localdepth = 0;

            for (int j = 0; j < piperPos[id].length; ++j) {
                if (pidx == j) {
                    continue;
                }

                boolean is_captured = (states[j] instanceof Player.DepositState) &&
                        piperPos[id][j].distance(rat_position) <= 10 + 1e-5;
                boolean is_target = false;

                if (states[j] instanceof Player.RetrieveClosestRatState) {
                    if (((Player.RetrieveClosestRatState) states[j]).targetRat == i) {
                        is_target = true;
                    }
                }


                if (is_captured || is_target) {
                    ++localdepth;
                }
            }
            rat_lut.put(distance, i);
            if (localdepth < depth) {
                distances.add(distance);
            }
        }

        Collections.sort(distances);

        if (distances.isEmpty()) {
            return pidx % ratPos.length;
        } else {
            return rat_lut.get(distances.get(0));
        }
    }

}
