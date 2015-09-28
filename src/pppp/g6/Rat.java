package pppp.g6;

import pppp.sim.Point;

// Rat 
	public class Rat {
	Point currentLocation;
	Point lastLocation;
	boolean isEnchanted;

	public Rat(Point pos){
		this.currentLocation = pos;
	}

	public Point getCurrentLocation() {
		return currentLocation;
	}

	public void setCurrentLocation(Point currentLocation) {
		this.currentLocation = currentLocation;
	}

	public Point getLastLocation() {
		return lastLocation;
	}

	public void setLastLocation(Point lastLocation) {
		this.lastLocation = lastLocation;
	}

	public boolean isEnchanted() {
		return isEnchanted;
	}

	public void setEnchanted(boolean isEnchanted) {
		this.isEnchanted = isEnchanted;
	}
}