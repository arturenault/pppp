package pppp.g7;

import pppp.sim.Move;
import pppp.sim.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rbtying on 9/22/15.
 */
public class PotentialField {
    private static final double TESTPOINT_CHARGE = 1.0;
    private static final double IGNORE_RAT_DISTANCE = 75.0;
    private static final double IGNORE_PIPER_DISTANCE = 85.0;

    private ArrayList<Point> points;
    private ArrayList<Double> charges;
    private Point testPoint;
    private boolean willPlay;
    private Util u;
    private int side;

    public PotentialField(Util u, int side, int id, int pidx, boolean willPlay, Point piperPos[][], Move piperVel[][], boolean pipersPlaying[][], Point ratPos[]) {
        int initial_size = piperPos.length * piperPos[id].length + ratPos.length;
        points = new ArrayList<>(initial_size);
        charges = new ArrayList<>(initial_size);
        testPoint = piperPos[id][pidx];
        this.side = side;
        this.willPlay = willPlay;
        this.u = u;

        initializePotentialField(id, pidx, piperPos, pipersPlaying, ratPos);
    }

    public void initializePotentialField(int id, int pidx, Point piperPos[][], boolean pipersPlaying[][], Point ratPosAll[]) {

        List<Integer> rats = u.getIndicesWithinDistance(piperPos[id][pidx], ratPosAll, IGNORE_RAT_DISTANCE);
        Point[] ratPos = new Point[rats.size()];
        for (int i = 0; i < rats.size(); ++i) {
            ratPos[i] = ratPosAll[rats.get(i)];
        }

        boolean ratInFriendlyZone[] = new boolean[ratPos.length];
        boolean ratInEnemyZone[] = new boolean[ratPos.length];
        boolean ratInMyZone[] = new boolean[ratPos.length];

        // initialize arrays
        for (int i = 0; i < ratPos.length; ++i) {
            ratInEnemyZone[i] = false;
            ratInFriendlyZone[i] = false;
            ratInMyZone[i] = false;
        }

        for (int i = 0; i < piperPos[id].length; ++i) {
            if (i != pidx) {
                addFriendlyPiper(piperPos[id][i], pipersPlaying[id][i]);
            }

            if (piperPos[id][i].distance(testPoint) <= IGNORE_PIPER_DISTANCE) {
                List<Integer> nearby = u.getIndicesWithinDistance(piperPos[id][i], ratPos, 10);
                for (int j : nearby) {
                    if (i == pidx) {
                        ratInMyZone[j] = true;
                    } else {
                        ratInFriendlyZone[j] = true;
                    }
                }
            }
        }

        for (int e = 0; e < piperPos.length; ++e) {
            if (e == id) {
                continue;
            }

            // Only care about enemy that are close
            for (int i = 0; i < piperPos[e].length; ++i) {
                addEnemyPiper(piperPos[e][i], pipersPlaying[e][i]);
                if (piperPos[e][i].distance(testPoint) <= IGNORE_PIPER_DISTANCE) {
                    List<Integer> nearby = u.getIndicesWithinDistance(piperPos[e][i], ratPos, 10);
                    for (int j : nearby) {
                        ratInEnemyZone[j] = true;
                    }
                }
            }
        }

        for (int i = 0; i < ratPos.length; ++i) {
            if (!ratInMyZone[i]) {
                addRat(ratPos[i], ratInFriendlyZone[i], ratInEnemyZone[i]);
            }
        }
    }

    public void addFriendlyPiper(Point loc, boolean playing) {
        if (playing) {
            // ok to go towards friendly playing
            addPotential(loc, 5.0);
        } else {
            // go far away from friendly non-playing
            addPotential(loc, 10.0);
        }
    }

    public void addEnemyPiper(Point loc, boolean playing) {
        if (playing) {
            // try to avoid enemy playing
            addPotential(loc, 16.0);
        } else {
            // try less hard to avoid enemy non-playing
            addPotential(loc, 7.0);
        }

    }

    public void addRat(Point loc, boolean listeningToFriendly, boolean listeningToEnemy) {
        if (listeningToEnemy && listeningToFriendly) {
            // reinforce!!
            addPotential(loc, -100.0);
        } else if (listeningToEnemy) {
            addPotential(loc, -7.0);
        } else if (listeningToFriendly) {
            addPotential(loc, -2.0);
        } else {
            // go toward rats that are unclaimed
            addPotential(loc, -7.0);
        }
    }

    public void addPotential(Point pos, double charge) {
        points.add(pos);
        charges.add(charge);
        assert points.size() == charges.size();
    }

    public double getPotential(Point loc, double charge) {
        double potential = 0;
        for (int i = 0; i < points.size(); ++i) {
            double r = loc.distance(points.get(i));
            potential += charge * charges.get(i) / r;
        }

        potential += loc.x * loc.x * 0.010;
        potential += Math.pow(((side / 2) - loc.y), 2) * 0.0025;

        return potential;
    }

    public Point computeMove() {
        double speed = willPlay ? 0.1 : 0.5;
        Point candidate = testPoint;
        double min_potential = getPotential(candidate, TESTPOINT_CHARGE);
        for (int i = 0; i < 12; ++i) {
            Point loc = new Point(testPoint.x + Math.cos(i * Math.PI / 6) * speed, testPoint.y + Math.sin(i * Math.PI / 6) * speed);
            double p = getPotential(loc, TESTPOINT_CHARGE);
            if (Double.isNaN(p)) {
                continue;
            }
            if (p < min_potential) {
                min_potential = p;
                candidate = loc;
            }
        }
        return candidate;
    }
}
