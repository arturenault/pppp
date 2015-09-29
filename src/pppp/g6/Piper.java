package pppp.g6;

import pppp.sim.Point;


public class Piper{
	
	Point loc;
	boolean playingMusic;

	public Piper(Point location){
		this.loc = location;
	}

	public boolean getPlayingMusic(){
		return playingMusic;

	}

	public void setPlayingMusic(boolean playingMusic){
		this.playingMusic = playingMusic;
	}

	public Point getLocation(){
		return loc;
	}

	public void setLocation(Point location){
		this.loc = location;
	}
}