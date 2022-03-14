package johnidis.maverick.modelling;

import johnidis.maverick.Holdem;
import johnidis.maverick.Jagbot;
import poker.MaverickGameInfo;

import com.biotools.meerkat.Action;
import com.biotools.meerkat.Card;

public class DecisionBot extends Jagbot {
	
	private PaObserver observer;
	
	public DecisionBot (MaverickGameInfo aGame, boolean shared) {
		super(null);
		MaverickGameInfo game;
		if (shared) {
			game = aGame;
			observer = null;
		} else {
			game = new MaverickGameInfo();
			game.A(aGame);
			observer = PaObserver.newInstance (false, game);
			observer.D(false);
			observer.gameStartEvent(game);
		}
		gameStartEvent(game);
		stageEvent(game.getStage());
	}
	
	
	public int getHero ()  {
		return this.Ń;
	}
	
	public Action getAction (int p, Card c1, Card c2) {
		getGameInfo().setCurrentPlayerPosition(p);
		holeCards(c1, c2, p);
		return getAction();
	}
	
	@Override
	public void stageEvent(int paramInt) {
		super.stageEvent(paramInt);
		if (observer != null)
			observer.stageEvent(paramInt);
	}
	
	public void flop (Card c1, Card c2, Card c3) {
		getGameInfo().flop(c1, c2, c3);
		getGameInfo().setCurrentPlayerPosition(getGameInfo().getButtonSeat());
		getGameInfo().advanceCurrentPlayer();
		stageEvent(Holdem.FLOP);
	}
	
	public void turn (Card c) {
		getGameInfo().turn(c);
		getGameInfo().setCurrentPlayerPosition(getGameInfo().getButtonSeat());
		getGameInfo().advanceCurrentPlayer();
		stageEvent(Holdem.TURN);
	}

	public void river (Card c) {
		getGameInfo().river(c);
		getGameInfo().setCurrentPlayerPosition(getGameInfo().getButtonSeat());
		getGameInfo().advanceCurrentPlayer();
		stageEvent(Holdem.RIVER);
	}
	
	public void fold () {
		getGameInfo().fold();
		getGameInfo().advanceCurrentPlayer();
	}

	public void call () {
		getGameInfo().call();
		getGameInfo().advanceCurrentPlayer();
	}

	public void raise () {
		getGameInfo().raise();
		getGameInfo().advanceCurrentPlayer();
	}

	public void shutdown () {
		gameOverEvent();
		if (observer != null) {
			observer.gameOverEvent();
			PaObserver.deleteInstance(getGameInfo());
			observer = null;
		}
	}

	@Override
	public String toString() {
		return getGameInfo().toString() + ": player="+getGameInfo().getPlayerName(getGameInfo().getCurrentPlayerSeat())+
				", hole=["+Ľ+","+ļ+"], inPot="+getGameInfo().getPlayer(getGameInfo().getCurrentPlayerSeat()).getAmountInPot()+
				", pot="+getGameInfo().getTotalPotSize();
	}

	@Override
	protected void finalize() throws Throwable {
		if (observer != null)
			shutdown();
		super.finalize();
	}

}