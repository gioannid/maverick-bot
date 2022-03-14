package johnidis.maverick.modelling.modellers;

import poker.MaverickGameInfo;
import poker.MaverickPlayerInfo;
import johnidis.maverick.Holdem;
import johnidis.maverick.modelling.DecisionBot;
import johnidis.maverick.modelling.adapters.GameAdapter;
import johnidis.maverick.modelling.data.MLData;
import johnidis.maverick.modelling.models.Model;
import johnidis.maverick.modelling.player.ExpertPlayerModeller;
import johnidis.utils.AbortException;

public class ExpertModeller extends Modeller<ExpertPlayerModeller,DecisionBot,MLData> {
	
	private static final boolean DEBUG_SKIP_MODELLER = false;
	
	public static final MaverickGameInfo GAME_NA = null;
	
	@Override
	public ExpertPlayerModeller instantiate(String key, String player) {
		return new ExpertPlayerModeller(key);
	}

	@Override
	public String genericName() {
		return MODEL_GENERIC;
	}

	@Override
	protected void doPersist(ExpertPlayerModeller modeller) {
		if (DEBUG_SKIP_MODELLER)
			return;
		modeller.persist();
	}

	@Override
	public String canonicalKey(ExpertPlayerModeller modeller) {
		return modeller.canonicalName;
	}

	@Override
	protected void doModelling(String player, boolean active) {
		if (DEBUG_SKIP_MODELLER)
			return;
		open(player).setModelling(active);
	}
	
	public double adjustHoleProbability(String player, MaverickGameInfo game, char action) {
		if (DEBUG_SKIP_MODELLER)
			return -1;
		ExpertPlayerModeller modeller = open(player);
		double pending = modeller.getError(MODEL_NA, CONTEXTID_NA);
		modeller.adjustHoleProbability(game, action);
		return pending;
	}

	@Override
	protected double doAddData(String player, Model<DecisionBot,MLData> model, DecisionBot input, char action) throws AbortException {
		if (DEBUG_SKIP_MODELLER)
			return -1;
		ExpertPlayerModeller modeller = open(player);
		int context = model.contextId(input);
		modeller.addData(model, context, action, input);
		return modeller.getError(model, context);
	}

	@Override
	protected MLData doEstimate(String player, Model<DecisionBot,MLData> model, DecisionBot input, boolean genericModel) throws AbortException {
		if (DEBUG_SKIP_MODELLER)
			return null;
		ExpertPlayerModeller modeller = open(player);
		int context = model.contextId(input);
		return modeller.estimate(model, context, GameAdapter.getAction(input.getAction()));
	}
	
	public void newStage (MaverickGameInfo game) {
		if (DEBUG_SKIP_MODELLER)
			return;
		for (int p = 0; p < game.getNumPlayers(); p++) {
			MaverickPlayerInfo player = game.getPlayer(p);
			if (player.isActive()) {
				ExpertPlayerModeller modeller = open(player.getName());
				if (game.getStage() == Holdem.FLOP)
					modeller.scratch(game, game.getBoard().getCard(1), game.getBoard().getCard(2), game.getBoard().getCard(3));
				else if ((game.getStage() == Holdem.TURN) || (game.getStage() == Holdem.RIVER))
					modeller.scratch(game, game.getBoard().getCard(game.getStage() + 2));
			}
		}
	}

}
