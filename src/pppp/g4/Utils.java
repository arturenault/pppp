package pppp.g4;

import java.util.Random;

import pppp.sim.Point;
//import pppp.sim.WinnerInfo;

public class Utils {

	public Utils() {
		// TODO Auto-generated constructor stub
	}
	// Euclidean distance between two points
	public static double distance(Point p1, Point p2)
	{
		double dx = p1.x - p2.x;
		double dy = p1.y - p2.y;
		return Math.sqrt(dx * dx + dy * dy);
	}
	public static boolean allowMove(int playerId, int currentPipersId , Point[][] pipers,Point dst, int total_regions){
		Point currentPiper=pipers[playerId][currentPipersId];
		int cur_pipers_region=currentPipersId % total_regions;
		double d0=distance(currentPiper, dst);
		for (int p = 0 ; p != pipers[playerId].length ; ++p){
			if(p%total_regions == cur_pipers_region && p!=currentPipersId){
				Point nearbyPiperPoint = pipers[playerId][p];
				double d1=distance(nearbyPiperPoint, dst);
				if((d1-d0)>4) return false;
			}

		}
		return true;
	}
	
	public static void shuffleArray(int[] ar)
	  {
	    Random rnd = new Random();
	    for (int i = ar.length - 1; i > 0; i--)
	    {
	      int index = rnd.nextInt(i + 1);
	      // Simple swap
	      int a = ar[index];
	      ar[index] = ar[i];
	      ar[i] = a;
	    }
	  }
	
	/** find the Winners group Id and its Winning Score.
	 *  Needed for writing the score sheet
	 * 
	 */
/*	public static WinnerInfo getMaxValueAndIndex(int[] scoresOf9) {
		int maxValue = Integer.MIN_VALUE;
		int maxIndex = -1;
		for(int i = 0; i < scoresOf9.length; i++) {
		      if(scoresOf9[i] > maxValue) {
		    	  maxValue = scoresOf9[i];
		    	  maxIndex = i ;
		      }
		}
		return new WinnerInfo(maxIndex,maxValue);
	}
*/
}
