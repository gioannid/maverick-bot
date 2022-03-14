package johnidis.maverick.modelling.models;

import johnidis.maverick.modelling.adapters.BNGameAdapter;
import johnidis.maverick.modelling.data.BasicMLData;
import johnidis.maverick.modelling.data.MLData;

public enum HistogramHandModel implements HistogramModel<Integer> {
	
	  Predicted_BUSTED_LO,
	  Predicted_BUSTED_MID,
	  Predicted_BUSTED_HI,
	  Predicted_PAIR_LO,
	  Predicted_PAIR_MID,
	  Predicted_PAIR_HI,
	  Predicted_TWOPAIR_LO,
	  Predicted_TWOPAIR_HI,
	  Predicted_TRIPLE,
	  Predicted_STRAIGHT,
	  Predicted_FLUSH,
	  Predicted_AtLeast_FULLHOUSE;
	  
	private static final int HAND = BNGameAdapter.IdealDataFinal.HAND.index();
	
	@Override
	public HistogramModel<Integer> histogramModel(MLData estimation) {
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
		int indexActual = BNGameAdapter.idealAsSymbol0(ideal.getData(), HAND).intValue();
		int round = (int) Math.abs(ideal.getData(HAND) / BNGameAdapter.MASK_ROUND);
		MLData points = new BasicMLData(outputDimensions());
		points.setData((round >= BNGameAdapter.ROUND_RIVER) ? 
				indexActual : 
				BNGameAdapter.MAP_HAND_INTERIM_TO_FINAL[indexActual], 1);
		return points;
	}
	
	@Override
	public int slices() {
		return 1;
	}

	@Override
	public int index() {
		return ordinal();
	}
	
	@Override
	public int contextId(Integer round) {
		return round;
	}

	@Override
	public int getRound(int contextId) {
		return contextId;
	}

	@Override
	public int getSlice(int contextId, MLData prediction) {
		return 0;
	}

}
