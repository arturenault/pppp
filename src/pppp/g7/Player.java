package pppp.g7;

import pppp.sim.Move;
import pppp.sim.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Player implements pppp.sim.Player {

    // see details below
    private int id = -1;
    private int side = 0;
    private long tick;
    private Util util;

    private Point[][] prevPiperPos;
    private Move[][] piperVel;
    private PlayerState[] states;
    private HashMap<Integer, Integer> groupLookup;
    private HashMap<Integer, List<Integer>> groupReverseLookup;

    // specify location that the player will alternate between
    public void init(int id, int side, long turns,
                     Point[][] pipers, Point[] rats) {

        this.tick = 0;
        this.id = id;
        this.side = side;
        groupLookup = new HashMap<>(pipers[id].length);
        groupReverseLookup = new HashMap<>(pipers[id].length);
        int GROUPSIZE = 2;

        util = new Util(id == 3 || id == 2, id == 2 || id == 1, id == 1 || id == 3);

        this.prevPiperPos = new Point[pipers.length][pipers[0].length];
        this.piperVel = new Move[pipers.length][pipers[0].length];

        for (int pid = 0; pid < pipers.length; ++pid) {
            for (int p = 0; p < pipers[pid].length; ++p) {
                if (pid == id) {
                    // assume that points are in order
                    int group = p / GROUPSIZE;
                    groupLookup.put(p, group);
                    if (p % GROUPSIZE == 0) {
                        groupReverseLookup.put(group, new ArrayList<>(GROUPSIZE));
                    }
                    groupReverseLookup.get(group).add(p);
                }
                this.prevPiperPos[pid][p] = util.transformPoint(pipers[pid][p]);
                this.piperVel[pid][p] = new Move(0, 0, false);
            }
        }

        this.states = new PlayerState[pipers[id].length];
        for (int i = 0; i < pipers[id].length; ++i) {
            this.states[i] = new DoorState();
        }
    }

    // return next locations on last argument
    public void play(Point[][] pipers, boolean[][] pipers_played,
                     Point[] rats, Move[] moves) {
        // increment tick
        ++tick;

        // transform coordinates
        Point transformedPiperPos[][] = new Point[pipers.length][pipers[0].length];
        Point transformedRatPos[] = new Point[rats.length];

        for (int pid = 0; pid < pipers.length; ++pid) {
            for (int p = 0; p < pipers[pid].length; ++p) {
                transformedPiperPos[pid][p] = util.transformPoint(pipers[pid][p]);
                double dx = transformedPiperPos[pid][p].x - prevPiperPos[pid][p].x;
                double dy = transformedPiperPos[pid][p].y - prevPiperPos[pid][p].y;
                this.piperVel[pid][p] = new Move(dx, dy, pipers_played[pid][p]);
                prevPiperPos[pid][p] = transformedPiperPos[pid][p];
            }
        }

        for (int r = 0; r < rats.length; ++r) {
            transformedRatPos[r] = util.transformPoint(rats[r]);
        }

        // perform computation
        Move m[] = play_transformed(id, side, transformedPiperPos, piperVel, pipers_played, transformedRatPos);

        // untransform coordinates
        for (int i = 0; i < m.length; ++i) {
            moves[i] = util.transformMove(m[i]);
        }
    }

    private PlayerState nextStrategicState(int pidx, Point[][] piperPos, Move[][] piperVel, boolean[][] pipers_played,
                                           Point[] ratPos) {
        double rat_density = ratPos.length * 1.0 / (side * side);
        double player_density = piperPos[id].length * 1.0 / (side * side);

        // compute clustering
        int square_side = 10;
        int bins = side / square_side + 1;
        int counter[][] = new int[bins][bins];

        for (int i = 0; i < ratPos.length; ++i) {
            int x = (int) ((ratPos[i].x + side / 2) / square_side);
            int y = (int) ((ratPos[i].y + side / 2) / square_side);
            counter[x][y]++;
        }

        int max_rats = 0;
        double mean = ratPos.length * 1.0 / bins * bins;
        double var = 0;
        for (int i = 0; i < counter.length; ++i) {
            for (int j = 0; j < counter[i].length; ++j) {
                if (counter[i][j] > max_rats) {
                    max_rats = counter[i][j];
                }
                var += Math.pow((counter[i][j] - mean), 2) / (bins * bins);
            }
        }

        double std = Math.sqrt(var);


        boolean ratInFriendlyZone[] = new boolean[ratPos.length];
        boolean ratInEnemyZone[] = new boolean[ratPos.length];

        // initialize arrays
        for (int i = 0; i < ratPos.length; ++i) {
            ratInEnemyZone[i] = false;
            ratInFriendlyZone[i] = false;
        }

        for (int e = 0; e < piperPos.length; ++e) {
            for (int i = 0; i < piperPos[e].length; ++i) {
                List<Integer> nearby = util.getIndicesWithinDistance(piperPos[e][i], ratPos, 10);
                for (int j : nearby) {
                    if (e == id) {
                        ratInFriendlyZone[j] = true;
                    } else {
                        ratInEnemyZone[j] = true;
                    }
                }
            }
        }

        int unattached_rats = 0;
        int enemy_range_rats = 0;
        for (int i = 0; i < ratPos.length; ++i) {
            if (ratInEnemyZone[i]) {
                ++enemy_range_rats;
            }
            if (!ratInEnemyZone[i] && !ratInFriendlyZone[i]) {
                ++unattached_rats;
            }
        }

        double frac_unattached = unattached_rats * 1.0 / ratPos.length;
        double frac_enemy_range = enemy_range_rats * 1.0 / ratPos.length;

        if (std > 30) {
            return new SweepState();
        } else if ((rat_density / player_density) >= 1.5) {
            if (pidx < piperPos[id].length * frac_unattached + 0.5) {
                return new RetrieveClosestRatState();
            } else {
                return new RetrieveMostRatsState();
            }
            /*
            if (pidx < piperPos[id].length * frac_enemy_range + 0.5) {
                return new RetrieveMostRatsState();
            } else {
                return new RetrieveClosestRatState();
            }
            */
        } else {
            return new RetrieveClosestRatState();
        }
    }

    private Move[] play_transformed(int id, int side, Point[][] piperPos, Move[][] piperVel, boolean[][] pipers_played,
                                    Point[] ratPos) {
        // THE ENEMIES GATE IS DOWN!!!
        Move m[] = new Move[piperPos[id].length];

        // state machine
        for (int p = 0; p < piperPos[id].length; ++p) {
            if (states[p].stateComplete(p, piperPos, piperVel, pipers_played, ratPos)) {
//                System.out.print("piper: " + p + " | " + states[p]);
                states[p] = states[p].nextState(p, piperPos, piperVel, pipers_played, ratPos);
//                System.out.println(" -> " + states[p]);
            }
            m[p] = states[p].computeMove(p, piperPos, piperVel, pipers_played, ratPos);
        }
        return m;
    }

    public interface PlayerState {
        PlayerState nextState(int pidx, Point[][] piperPos, Move[][] piperVel, boolean[][] pipers_played,
                              Point[] ratPos);

        Move computeMove(int pidx, Point[][] piperPos, Move[][] piperVel, boolean[][] pipers_played, Point[] ratPos);

        boolean stateComplete(int pidx, Point[][] piperPos, Move[][] piperVel, boolean[][] pipers_played,
                              Point[] ratPos);

        boolean sameStateAs(PlayerState other);
    }

    private abstract class GoToLocationState implements PlayerState {
        public static final double TOLERANCE = 0.05;
        public Point dest;
        public boolean playing;

        public GoToLocationState(Point dest, boolean playing) {
            this.dest = dest;
            this.playing = playing;
        }

        @Override
        public Move computeMove(int pidx, Point[][] piperPos, Move[][] piperVel, boolean[][] pipers_played,
                                Point[] ratPos) {
            return Util.moveToLoc(piperPos[id][pidx], dest, playing);
        }

        @Override
        public boolean stateComplete(int pidx, Point[][] piperPos, Move[][] piperVel, boolean[][] pipers_played,
                                     Point[] ratPos) {
            return piperPos[id][pidx].distance(dest) < TOLERANCE;
        }

        @Override
        public String toString() {
            return util.stateName(this) + String.format(" [dest: (%.02f, %.02f)]", dest.x, dest.y);
        }

        @Override
        public boolean sameStateAs(PlayerState other) {
            Point otherDest = ((GoToLocationState) other).dest;
            return otherDest.x == dest.x && otherDest.y == dest.y;
        }
    }

    private class DoorState extends GoToLocationState {
        public DoorState() {
            super(new Point(0, side / 2), false);
        }

        @Override
        public PlayerState nextState(int pidx, Point[][] piperPos, Move[][] piperVel, boolean[][] pipers_played,
                                     Point[] ratPos) {
            return nextStrategicState(pidx, piperPos, piperVel, pipers_played, ratPos);
        }

        @Override
        public String toString() {
            return util.stateName(this);
        }
    }

    public class RetrieveClosestRatState extends GoToLocationState {
        private static final int REACQUIRE_TICKS = 1;
        private static final int DEPTH = 1;
        private static final double RAT_IS_CLOSE_ENOUGH_DIST = 5.0;
        public int targetRat;
        private long startTick;

        public RetrieveClosestRatState() {
            super(new Point(0, 0), false);
            startTick = tick;
            targetRat = -1;
        }

        @Override
        public Move computeMove(int pidx, Point[][] piperPos, Move[][] piperVel, boolean[][] pipers_played,
                                Point[] ratPos) {
            if ((tick - startTick) % REACQUIRE_TICKS == 0 || dest.distance(piperPos[id][pidx]) < 1) {
                targetRat = util.getClosestRat(id, states, pidx, piperPos, piperVel, pipers_played, ratPos, DEPTH);
                dest = ratPos[targetRat];
            }
            return super.computeMove(pidx, piperPos, piperVel, pipers_played, ratPos);
        }

        @Override
        public boolean stateComplete(int pidx, Point[][] piperPos, Move[][] piperVel, boolean[][] pipers_played,
                                     Point[] ratPos) {
            List<Integer> rats = util.getIndicesWithinDistance(piperPos[id][pidx], ratPos, RAT_IS_CLOSE_ENOUGH_DIST);

            if (ratPos.length <= piperPos[id].length) {
                return super.stateComplete(pidx, piperPos, piperVel, pipers_played, ratPos);
            }

            int numRats = rats.size();

            for (Integer r : rats) {
                int localdepth = 0;
                for (Integer p : util.getIndicesWithinDistance(ratPos[r], piperPos[id], 10)) {
                    if (p == pidx) {
                        continue;
                    }

                    if (states[p] instanceof DepositState) {
                        ++localdepth;
                    }
                }
                if (localdepth >= DEPTH) {
                    --numRats;
                }
            }

            return numRats > 0;// && super.stateComplete(pidx, piperPos, piperVel, pipers_played, ratPos);
        }

        @Override
        public PlayerState nextState(int pidx, Point[][] piperPos, Move[][] piperVel, boolean[][] pipers_played,
                                     Point[] ratPos) {
            if (ratPos.length == 1) {
                return new FightState();
            } else {
                return new DepositState();
            }
        }

        @Override
        public String toString() {
            return util.stateName(this);
        }
    }

    private class FightState extends DepositState {
        // special class for fighting over the last rat
        private Point lastRatPos;

        public FightState() {
            lastRatPos = null;
        }

        @Override
        public Move computeMove(int pidx, Point[][] piperPos, Move[][] piperVel, boolean[][] pipers_played,
                                Point[] ratPos) {
            Point pp = piperPos[id][pidx];
            Point rp = ratPos[0];
            double dist = pp.distance(rp);
            Move m = null;

            if (dist > 10) {
                m = Util.moveToLoc(pp, rp, false);
            } else {
                boolean contested = false;
                for (int i = 0; i < piperPos.length; ++i) {
                    if (i == id) {
                        continue;
                    }
                    for (int j = 0; j < piperPos[i].length; ++j) {
                        if (piperPos[i][j].distance(rp) < 10) {
                            contested = true;
                            break;
                        }
                    }
                }
                Point dest;
                if (Math.abs(pp.x) < 1) {
                    dest = new Point(0, side / 2 + 2.1);
                } else {
                    dest = new Point(0, side / 2);
                }
                if (!contested) {
                    m = Util.moveToLoc(pp, dest, true);
                } else {
                    m = Util.moveToLoc(pp, dest, !pipers_played[id][pidx]);
                    // contested!
                }
            }

            lastRatPos = rp;

            return m;
        }

        @Override
        public boolean stateComplete(int pidx, Point[][] piperPos, Move[][] piperVel, boolean[][] pipers_played,
                                     Point[] ratPos) {
            // never transition out
            return false;
        }

    }

    private class RetrieveMostRatsState implements PlayerState {

        public static final double TOLERANCE = 0.05;
        int counter = 0;
        private int max_rats = 10000000; // Force Update
        private Point target = new Point(0, 0);

        @Override
        public Move computeMove(int pidx, Point[][] piperPos, Move[][] piperVel, boolean[][] pipers_played,
                                Point[] ratPos) {
            List<Integer> pos = util.getIndicesWithinDistance(target, ratPos, 10.0);
            if (pos.size() <= max_rats * .75) {
                update_most_rats(pidx, piperPos, piperVel, pipers_played, ratPos);
            }
            counter = (counter + 1); // % 10;
            return Util.moveToLoc(piperPos[id][pidx], target, false);
        }

        @Override
        public boolean stateComplete(int pidx, Point[][] piperPos, Move[][] piperVel, boolean[][] pipers_played,
                                     Point[] ratPos) {
            List<Integer> pos = util.getIndicesWithinDistance(piperPos[id][pidx], ratPos, 10.0);
            List<Integer> pos2 = util.getIndicesWithinDistance(target, ratPos, 10.0);
            return max_rats <= 1 || pos.size() >= pos2.size();// - 1;
        }

        @Override
        public boolean sameStateAs(PlayerState other) {
            return true;
        }

        @Override
        public PlayerState nextState(int pidx, Point[][] piperPos, Move[][] piperVel, boolean[][] pipers_played,
                                     Point[] ratPos) {
            List<Integer> rats = util.getIndicesWithinDistance(piperPos[id][pidx], ratPos, 10);
            if (rats.isEmpty()) {
                if (max_rats <= 1) {
                    return new RetrieveClosestRatState();
                } else {
                    return new RetrieveMostRatsState();
                }
            } else {
                return new DepositState();
            }
        }

        private void update_most_rats(int pidx, Point[][] piperPos, Move[][] piperVel, boolean[][] pipers_played,
                                      Point[] ratPos) {
            max_rats = 1;
            target = ratPos[(int) (Math.random() * ratPos.length)];

            ArrayList<Point> realRatPos = new ArrayList<>();
            for (Point p : ratPos) {
                boolean ok = true;
                for (int i = 0; i < piperPos[id].length; i++) {
                    if (i != pidx && pipers_played[id][i]) {
                        if (piperPos[id][i].distance(p) <= 10) {
                            ok = false;
                        }
                    }
                }
                if (ok) {
                    realRatPos.add(p);
                }
            }

            for (int i = 0; i < realRatPos.size(); i++) {
                for (int j = i + 1; j < realRatPos.size(); j++) {
                    if (ratPos[i].distance(ratPos[j]) <= 10 * 2) {
                        Point p1 = getCenterByRadiusAndPoints(ratPos[i], ratPos[j], 10);
                        Point p2 = getCenterByRadiusAndPoints(ratPos[j], ratPos[i], 10);
                        int s1 = 0, s2 = 0;
                        for (Point k : realRatPos) {
                            if (k.distance(p1) <= 10) {
                                s1++;
                            }
                            if (k.distance(p2) <= 10) {
                                s2++;
                            }
                        }
                        if (s1 > max_rats) {
                            max_rats = s1;
                            target = p1;
                        }
                        if (s2 > max_rats) {
                            max_rats = s2;
                            target = p2;
                        }

                    }
                }
            }
        }

        private Point getCenterByRadiusAndPoints(Point a, Point b, double radius) {

            Point mid = new Point((a.x + b.x) / 2, (a.y + b.y) / 2);
            double angleMidToCenter = Math.atan2(b.y - a.y, b.x - a.x) + Math.PI / 2;

            double distAToB = a.distance(b);
            double distMidToCenter = Math.sqrt(radius * radius - distAToB * distAToB);

            return new Point(mid.x + distMidToCenter * Math.cos(angleMidToCenter),
                    mid.y + distMidToCenter * Math.sin(angleMidToCenter));

        }


        @Override
        public String toString() {
            return util.stateName(this);
        }
    }

    private class SweepState extends GoToLocationState {

        public SweepState() {
            super(new Point(0, 0), false);
        }

        public boolean stateComplete(int pidx, Point[][] piperPos, Move[][] piperVel, boolean[][] pipers_played,
                                     Point[] ratPos) {
            int max_pidx = piperPos[id].length;

            // group into sets of 2
            double radius = side/3.0;
            double angle_increment = Math.PI / (max_pidx + 3.0);
            double angle = angle_increment * (pidx + 2);
            double y_value = Math.sin(angle)* radius - side/5.0;
            double x_value = Math.cos(angle)*radius;
            this.dest = new Point(x_value, -y_value);
            return super.stateComplete(pidx, piperPos, piperVel, pipers_played, ratPos);
        }

        @Override
        public PlayerState nextState(int pidx, Point[][] piperPos, Move[][] piperVel, boolean[][] pipers_played,
                                     Point[] ratPos) {
            Point destination = new Point(0, side / 2);
            long endTime = tick + (long)(side / 0.1 * 4);
            return new PlayerState() {
                @Override
                public PlayerState nextState(int pidx, Point[][] piperPos, Move[][] piperVel,
                                             boolean[][] pipers_played, Point[] ratPos) {
                    return new DepositState();
                }

                @Override
                public Move computeMove(int pidx, Point[][] piperPos, Move[][] piperVel,
                                        boolean[][] pipers_played, Point[] ratPos) {
//                    long ntime = System.nanoTime();
                    PotentialField pf = new PotentialField(util, side, id, pidx, true, piperPos, piperVel, pipers_played, ratPos);

                    // add in a destination point
                    pf.addPotential(destination, -400.0);

                    Point next = pf.computeMove();
//                    System.out.println((System.nanoTime() - ntime) * 1.0e-9);

                    return Util.moveToLoc(piperPos[id][pidx], next, true);
                }

                @Override
                public boolean stateComplete(int pidx, Point[][] piperPos, Move[][] piperVel,
                                             boolean[][] pipers_played, Point[] ratPos) {
                    return (tick > endTime) || (side / 2 - piperPos[id][pidx].y) < 0.2 * (side / 2);
                }

                @Override
                public boolean sameStateAs(PlayerState other) {
                    return true;
                }

                @Override
                public String toString() {
                    return "MovingUpState";
                }
            };
        }

        @Override
        public String toString() {
            return util.stateName(this);
        }
    }

    private abstract class GroupWaitState implements PlayerState {
        public boolean letsGo;
        protected int group;

        public GroupWaitState() {
            letsGo = false;
            group = -1;
        }

        @Override
        public Move computeMove(int pidx, Point[][] piperPos, Move[][] piperVel, boolean[][] pipers_played,
                                Point[] ratPos) {
            group = groupLookup.get(pidx);
            return new Move(0, 0, true);
        }

        @Override
        public boolean stateComplete(int pidx, Point[][] piperPos, Move[][] piperVel, boolean[][] pipers_played,
                                     Point[] ratPos) {
            if (letsGo) {
                return true;
            }
            group = groupLookup.get(pidx);
            List<Integer> l = groupReverseLookup.get(group);

            // Abort if any is in non-sweep states
            for (Integer p : l) {
                if (states[p].getClass().equals(RetrieveClosestRatState.class) ||
                        states[p].getClass().equals(RetrieveMostRatsState.class)) {
                    return true;
                }
            }

            boolean shouldGo = util.groupIsInState(l, states, this);
            if (shouldGo) {
                for (Integer i : l) {
                    ((GroupWaitState) states[i]).letsGo = true;
                }
            }
            return false;
        }

        @Override
        public boolean sameStateAs(PlayerState other) {
            return true;
        }

        @Override
        public String toString() {
            return util.stateName(this) + " [group: " + group + "]";
        }
    }

    public class DepositState extends GoToLocationState {
        // max rat distance divided by max rat speed
        private static final double MAX_WAIT_TICKS = 2 * 10 / 0.01 + 10e9;
        private static final double RAT_IS_CAPTURED_DISTANCE = 6;

        private long startTick;
        private int numRatsAtLastCheck;
        private boolean inApproach;

        public DepositState() {
            super(new Point(0, side / 2), true);
            inApproach = true;
            this.startTick = -1;
            this.numRatsAtLastCheck = 0;
        }

        @Override
        public boolean stateComplete(int pidx, Point[][] piperPos, Move[][] piperVel, boolean[][] pipers_played,
                                     Point[] ratPos) {
            boolean atLoc = super.stateComplete(pidx, piperPos, piperVel, pipers_played, ratPos);

            List<Integer> rats = util.getIndicesWithinDistance(piperPos[id][pidx], ratPos, RAT_IS_CAPTURED_DISTANCE);
            this.numRatsAtLastCheck = rats.size();
            if (rats.isEmpty()) {
                return true;
            }

            if (!atLoc) {
                return false;
            }

            if (inApproach) {
                // finish approach
                inApproach = false;
                this.dest = new Point(0, side / 2 + 2.1);
                return false;
            }

            if (tick - startTick > MAX_WAIT_TICKS) {
                // bail out if it takes too long
                return true;
            }

            // we're at location now
            if (startTick == -1) {
                startTick = tick;
            }

            return false;
        }

        @Override
        public PlayerState nextState(int pidx, Point[][] piperPos, Move[][] piperVel, boolean[][] pipers_played,
                                     Point[] ratPos) {
            if (inApproach) {
                // terminated early! :(
                return nextStrategicState(pidx, piperPos, piperVel, pipers_played, ratPos);
            } else {
                return new DoorState();
            }
        }

        @Override
        public String toString() {
            return util.stateName(this) + " [rats: " + numRatsAtLastCheck + "]";
        }
    }
}
