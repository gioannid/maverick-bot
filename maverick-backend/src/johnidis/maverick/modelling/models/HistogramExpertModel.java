package johnidis.maverick.modelling.models;

import johnidis.maverick.Holdem;
import johnidis.maverick.modelling.DecisionBot;
import johnidis.maverick.modelling.data.BasicMLData;
import johnidis.maverick.modelling.data.MLData;

public enum HistogramExpertModel implements HistogramModel<DecisionBot> {
	
	ExpertFold, 
	ExpertCall, 
	ExpertRaise;
	
	public static final int		MASK_COMMITTED = 4;
	

	static int context(DecisionBot game) {
		int round = game.getGameInfo().getStage();
		if (round == Holdem.PREFLOP)
			return -1;
		String actions = game.getGameInfo().getPlayer(game.getHero()).getActions();
		char lastAction = actions.equals("") ? 0 : actions.charAt(actions.length() - 1); 
		return round - 1 + (((lastAction == 'b') || (lastAction == 'r') || (lastAction == 'c')) ? MASK_COMMITTED : 0);
	}
	
	@Override
	public HistogramModel<DecisionBot> histogramModel(MLData estimation) {
		return this;
	}
	
	@Override
	public int inputDimensions() {
		return 1;
	}

	@Override
	public int outputDimensions() {
		return values().length;
	}

	@Override
	public int rounds() {
		return 3;
	}
	
	@Override
	public MLData histogramPoints (MLData ideal) {
		int slices = outputDimensions();
		MLData points = new BasicMLData(slices);
		points.setData((int) ideal.getData(0), 1);
		return points;
	}
	
	@Override
	public int slices() {
		return 2;
	}

	@Override
	public int index() {
		return ordinal();
	}

	@Override
	public int contextId(DecisionBot game) {
		return context(game);
	}

	@Override
	public int getSlice(int contextId, MLData prediction) {
		return contextId / MASK_COMMITTED;
	}

	@Override
	public int getRound(int contextId) {
		return contextId % MASK_COMMITTED;
	}
	
}