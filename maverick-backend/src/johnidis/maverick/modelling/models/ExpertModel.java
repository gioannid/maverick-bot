package johnidis.maverick.modelling.models;

import johnidis.maverick.modelling.DecisionBot;
import johnidis.maverick.modelling.data.MLData;

public enum ExpertModel implements Model<DecisionBot,MLData> {
	
	Hand,
	Action;
	
	
	@Override
	public int inputDimensions() {
		throw new RuntimeException ("ExpertModel.inputDimensions() not supported");
	}

	@Override
	public int outputDimensions() {
		return 1;
	}

	@Override
	public int rounds() {
		return 3;
	}

	@Override
	public int index() {
		return ordinal();
	}

	@Override
	public HistogramModel<DecisionBot> histogramModel(MLData estimation) {
		return HistogramExpertModel.values()[(int) estimation.getData(0)];		
	}
	
	@Override
	public int contextId (DecisionBot bot) {
		return HistogramExpertModel.context(bot);
	}
	
}
