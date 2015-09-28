package pppp.g6;

public class PiperPair{
	
	Piper piperOne;
	Piper piperTwo;

	public PiperPair(Piper one,Piper two){
	this.piperOne = one;
	this.piperOne = two;
	}

	public Piper getPiperOne() {
		return piperOne;
	}

	public void setPiperOne(Piper piperOne) {
		this.piperOne = piperOne;
	}

	public Piper getPiperTwo() {
		return piperTwo;
	}

	public void setPiperTwo(Piper piperTwo) {
		this.piperTwo = piperTwo;
	}
}