package pppp.g2;

import pppp.sim.Point;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by naman on 9/19/15.
 */
public class Piper {

    public int id;
    public Set<Integer> capturedRats;
    public Point prevLocation;
    public Point curLocation;
    public boolean playedMusic;
    public double movementX;
    public double movementY;
    public Strategy strategy;

    public Piper(int id, Point curLocation) {
        this.id = id;
        this.capturedRats = new HashSet<Integer>();
        this.prevLocation = null;
        this.curLocation = curLocation;
        this.playedMusic = false;
	this.movementX = 1;
	this.movementY = 1;
        this.strategy = new Strategy();
    }

    public Piper(int id, Point curLocation, Strategy strategy) {
        this.id = id;
        this.capturedRats = new HashSet<Integer>();
        this.prevLocation = null;
        this.curLocation = curLocation;
        this.playedMusic = false;
        this.movementX = 1;
	this.movementY = 1;
        this.strategy = strategy;
    }

    public Piper(int id, Point curLocation, boolean playedMusic) {
        this.id = id;
        this.capturedRats = new HashSet<Integer>();
        this.prevLocation = null;
        this.curLocation = curLocation;
        this.playedMusic = playedMusic;
        this.movementX = 1;
	this.movementY = 1;
        this.strategy = new Strategy();
    }

    public void updateMusic(boolean playMusic) {
	this.playedMusic = playMusic;
    }

    public void updateLocation(Point point) {
	if (this.prevLocation != null) {
	    double memory = 16;
	    movementX = movementX * (memory - 1) / memory;
	    movementY = movementY * (memory - 1) / memory;
	    movementX += (point.x - this.prevLocation.x) / memory;
	    movementY += (point.y - this.prevLocation.y) / memory;
	}
        this.prevLocation = this.curLocation;
        this.curLocation = point;
    }

    public void resetRats() {
        this.capturedRats = new HashSet<Integer>();
    }

    public void addRat(Integer ratId) {
        this.capturedRats.add(ratId);
    }

    public int getNumCapturedRats() {
	return capturedRats.size();
    }

    public double getAbsMovement() {
	return Math.sqrt(movementX * movementX + movementY * movementY);
    }
}
