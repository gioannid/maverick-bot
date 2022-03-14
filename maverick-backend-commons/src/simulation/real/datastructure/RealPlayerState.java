package simulation.real.datastructure;

import java.util.Arrays;

import ca.ualberta.cs.poker.free.dynamics.Card;
import ca.ualberta.cs.poker.free.dynamics.RingDynamics;
import data.Bucket;

public class RealPlayerState extends PlayerState {
	
	public Bucket bucket;

	public RealPlayerState(RingDynamics crd, int s) {
		super (crd, s);
		if ((crd.hole[s] != null) && (crd.hole[s].length == 2))
			setHole (crd.hole[s], crd.getBoard(false), crd.numActivePlayers);
	}
	
	public RealPlayerState(RealPlayerState player) {
		super (player);
		if (player.bucket != null)
			this.bucket = new Bucket (player.bucket);
	}

	public Card[] getHole() {
		return (bucket == null) ? null : bucket.getHole();
	}
	
	public void setHole (Card[] hole, Card[] board, int players) {
		if ((board == null) || (board[0] == null))
			bucket = new Bucket (hole);
		else
			bucket = new Bucket (hole, board, players);
	}
	
	public void setBoard (Card[] board, int players) {
		if ((bucket != null) && (board[0] != null))
			bucket.setBoard(board, players);
	}

	public int getBucket(int players) {
		return (bucket == null) ? -1 : bucket.getBucket(players);
	}

	public String toString() {
		return "Seat: " + seat + ((bucket == null) ? "" : " Hole: " + Arrays.deepToString(bucket.getHole()));
	}

}
