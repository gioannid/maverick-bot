package ringclient;

import java.util.ArrayList;

import ca.ualberta.cs.poker.free.dynamics.Card;

import ringclient.interfaces.StateChangeListener;

/**
 *
 * Created on 25.05.2008
 * @author Kami II
 */
public class RoundFinisher extends Thread {

	private int ownID;
	
	private int player0;
	
	private int[] amountWon;
	
	private int[] inPot;
	
	private Card[][] hole;
	
	private ArrayList<StateChangeListener> listeners;

	public RoundFinisher(ArrayList<StateChangeListener> listeners, int ownID,
			int player0, int[] amountWon, int[] inPot, Card[][] hole) {
		this.listeners = listeners;
		this.amountWon = amountWon;
		this.inPot = inPot;
		this.ownID = ownID;
		this.player0 = player0;
		this.hole = hole;
	}

	public void run() {
		for (StateChangeListener lis: listeners)
			if (!(lis instanceof TimerManager))
				lis.roundFinished(ownID, player0, amountWon, inPot, hole);
	}
}
