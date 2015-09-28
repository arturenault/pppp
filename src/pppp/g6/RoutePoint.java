package pppp.g6;

import pppp.sim.Point;

	//Class to hold information about the path a piper will take
	public class RoutePoint{
		Point point;
		boolean music;

		public RoutePoint(Point pos,boolean playMusic){
			this.point = pos;
			this.music = playMusic;
		}

		public Point getPoint() {
		return point;
	}

	public void setPoint(Point point) {
		this.point = point;
	}

	public boolean isMusic() {
		return music;
	}

	public void setMusic(boolean music) {
		this.music = music;
	}

	}