package johnidis.maverick.modelling;

import johnidis.maverick.Agent;
import johnidis.maverick.Agent.Capabilities;

import com.biotools.meerkat.Action;
import com.biotools.meerkat.Card;
import com.biotools.meerkat.GameInfo;

public class PaObserver extends com.biotools.poker.D.G {
	
	public static PaObserver newInstance (boolean persist, GameInfo gInfo) {
		synchronized (instances) {
			PaObserver instance = (PaObserver) instances.get(gInfo);
			if (instance == null) {
				instance = new PaObserver (persist);
				instances.put(gInfo, instance);
				if (DEBUG)
					System.out.println("PA: Created new modeller for "+gInfo);
			} else {
				throw new RuntimeException ("PAGameModeller.newInstance(): modeller already existing for "+gInfo);
			}
			return instance;
		}
	}
	
	public static PaObserver newInstance (Agent[] agents, GameInfo gInfo) {
		Boolean persistent = null;
		for (Agent agent : agents) {
			if (agent == null)
				continue;
			if (agent.capabilities().supported(Capabilities.PA_MODELLING_TRANS) && (persistent == null))
				persistent = false;
			if (agent.capabilities().supported(Capabilities.PA_MODELLING_PERS))
				persistent = true;
		}
		if (persistent == null)
			return null;
		return newInstance (persistent, gInfo);
	}
	
	public static void deleteInstance (GameInfo gInfo) {
		com.biotools.poker.D.G.B(gInfo);
	}
	

	private PaObserver (boolean persistent) {
		super (persistent);
	}

	
	@Override
	public void gameStartEvent(GameInfo paramGameInfo) {
		super.gameStartEvent(paramGameInfo);
	}

	@Override
	public void actionEvent(int arg0, Action arg1) {
		super.actionEvent(arg0, arg1);
	}

	@Override
	public void gameOverEvent() {
		super.gameOverEvent();
	}

	@Override
	public void stageEvent(int arg0) {
		super.stageEvent(arg0);
	}

	@Override
	public void dealHoleCardsEvent() {
		super.dealHoleCardsEvent();
	}

	@Override
	public void gameStateChanged() {
		super.gameStateChanged();
	}

	@Override
	public void showdownEvent(int arg0, Card arg1, Card arg2) {
		super.showdownEvent(arg0, arg1, arg2);
	}

	@Override
	public void winEvent(int arg0, double arg1, String arg2) {
		super.winEvent(arg0, arg1, arg2);
	}

	public void setNewPlayer(String name) {
		super.newPlayer(name);
	}

	@Override
	public void persist() {
		super.persist();
	}

}
