package johnidis.maverick.modelling.models;

import johnidis.maverick.modelling.adapters.BNGameAdapter;
import johnidis.maverick.modelling.adapters.GameAdapter;
import johnidis.maverick.modelling.data.BasicMLData;
import johnidis.maverick.modelling.data.MLData;

public enum HistogramActionModel implements HistogramModel<GameAdapter> {
	
	PredictedFold, 
	PredictedCall, 
	PredictedRaise;

	private static final int	SLICES						= 3;
	private static final int	ACTION						= BNGameAdapter.IdealDataFinal.ACTION.index();

	@Override
	public HistogramModel<GameAdapter> histogramModel(MLData estimation) {
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
		int predictions = outputDimensions();
		MLData points = new BasicMLData(predictions);
		points.setData((int) ideal.getData(ACTION), 1);
		return points;
	}
	
	@Override
	public int slices() {
		return SLICES;
	}

	@Override
	public int index() {
		return ordinal();
	}

	@Override
	public int contextId(GameAdapter underlyingGame) {
		return underlyingGame.roundIndex - 1;
	}

	@Override
	public int getRound(int contextId) {
		return contextId;
	}

	@Override
	public int getSlice(int contextId, MLData prediction) {
		return HistogramModel.getSlice(prediction, SLICES);
	}

}
