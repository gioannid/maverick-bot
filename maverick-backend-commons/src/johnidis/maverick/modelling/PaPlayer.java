package johnidis.maverick.modelling;

import java.lang.ref.WeakReference;

import johnidis.maverick.Iface;
import johnidis.maverick.Preferences;
import johnidis.maverick.modelling.modellers.Modeller;

import com.biotools.meerkat.Card;
import com.biotools.meerkat.Deck;
import com.biotools.meerkat.GameInfo;
import com.biotools.poker.D.B;
import com.biotools.poker.D.G;

public class PaPlayer extends B {
	
	public final String key;
	public final String suffix;
	
	private final WeakReference<G> modeller;
	private GameInfo gameInfo = null;

	public PaPlayer(String player, String key, String suffix, G modeller) {
		super(player, null);
		this.modeller = new WeakReference<G>(modeller);
		this.key = key;
		this.suffix = suffix;
	}
	
	
	/**
	 * Retrieves the filename for persistence
	 */
	@Override
	protected String ű() {
		return Modeller.pathToModels + "/" + key + suffix;
	}
	
	/**
	 * Persists models to disk
	 */
	@Override
	public void Ŭ() {
		if (Preferences.MODEL_READONLY.isOn())
			return;
		super.Ŭ();
	}
	
	@Override
	public void gameStartEvent(GameInfo paramGameInfo) {
		gameInfo = paramGameInfo;
		super.gameStartEvent (paramGameInfo);
	}
	
	@Override
	public double B(Card paramCard1, Card paramCard2, Deck paramDeck) {
		int i = gameInfo.getPlayerSeat(Ů());
		double d1 = 1.0D;
		for (int j = 0; j < gameInfo.getNumSeats(); j++)
			if ((gameInfo.inGame(j)) && (gameInfo.isActive(j)) && (j != i)) {
				double d2 = modeller.get().Q(j).C(paramCard1, paramCard2, paramDeck);
				d1 *= d2;
			}
		return d1;
	}

	@Override
	public double A(Card paramCard1, Card paramCard2) {
		int i = gameInfo.getPlayerSeat(Ů());
		double d = 1.0D;
		Deck localDeck = new Deck();
		localDeck.extractCard(paramCard1);
		localDeck.extractCard(paramCard2);
		localDeck.extractHand(gameInfo.getBoard());
		for (int j = 0; j < gameInfo.getNumSeats(); j++)
			if ((gameInfo.inGame(j)) && (gameInfo.isActive(j)) && (j != i))
				d *= modeller.get().Q(j).C(paramCard1, paramCard2, localDeck);
		return d;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder ();
		str.append(key).append(": ").append(Ŷ()).append(" hands, VP$IP: ").
				append((int) (100.0D * Ť())).append("%, Steals: ").append((int) (100.0D * ŧ())).
				append("%").append(Iface.LINE_SEPARATOR);
		str.append("    PreFlop | open: ").append((int) (100.0D * Ű())).append(", raise: ").append((int) (100.0D * ų())).
				append(", big: ").append((int) (100.0D * ů())).append(Iface.LINE_SEPARATOR);
		str.append("    PreFlop | fold: ").append((int) (100.0D * ť())).append(", foldSB: ").append((int) (100.0D * Ũ())).
				append(", foldBB: ").append((int) (100.0D * Š())).append(Iface.LINE_SEPARATOR);
		return str.toString();
	}

}
