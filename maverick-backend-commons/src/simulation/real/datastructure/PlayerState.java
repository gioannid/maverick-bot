package simulation.real.datastructure;

import ca.ualberta.cs.poker.free.dynamics.RingDynamics;

public class PlayerState {

	protected String name;
	protected boolean active;
	protected boolean canAct;
	protected int amountIn;
	protected int seat;
	protected int playerSeat;

	public PlayerState(RingDynamics crd, int s) {
		seat = s;
		active = crd.active[s];
		amountIn = crd.inPot[s];
		canAct = crd.canRaiseNextTurn[s];
		playerSeat = crd.seatToPlayer(s);
		name = crd.botNames[s];
	}
	
	public PlayerState(PlayerState player) {
		this.name = player.name;
		this.active = player.active;
		this.amountIn = player.amountIn;
		this.canAct = player.canAct;
		this.seat = player.seat;
		this.playerSeat = player.playerSeat;
	}

	/**
	 * @return the active
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * @param active the active to set
	 */
	public void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	public void setName(String n) {
		name = n;
	}

	/**
	 * @return the amountIn
	 */
	public int getAmountIn() {
		return amountIn;
	}

	/**
	 * @param amountIn the amountIn to set
	 */
	public void setAmountIn(int amountIn) {
		this.amountIn = amountIn;
	}

	/**
	 * @return the canRaiseNextRound
	 */
	public boolean isCanRaiseNextRound() {
		return canAct;
	}

	/**
	 * @param canRaiseNextRound the canRaiseNextRound to set
	 */
	public void setCanRaiseNextRound(boolean canRaiseNextRound) {
		this.canAct = canRaiseNextRound;
	}

	public boolean canAct(int maxAmount) {
		return isActive() && (canAct || amountIn<maxAmount);
	}

	/**
	 * @return the seat
	 */
	public int getSeat() {
		return seat;
	}

	public int getPlayerSeat() {
		return this.playerSeat;
	}

	public String toString() {
		return "Seat: " + seat;
	}
}