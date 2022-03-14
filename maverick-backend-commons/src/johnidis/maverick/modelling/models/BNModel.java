package johnidis.maverick.modelling.models;

import johnidis.maverick.modelling.adapters.BNGameAdapter;
import johnidis.maverick.modelling.adapters.GameAdapter;
import johnidis.maverick.modelling.data.MLData;

public enum BNModel implements Model<GameAdapter,MLData> {
	
	Hand {

		@Override
		public HistogramModel<GameAdapter> histogramModel(MLData estimation) {
			return null;
		}

		@Override
		public int contextId(GameAdapter game) {
			return ((BNGameAdapter) game).originalRoundIndex - 1;
		}
		
	},
	
	Action {
		
		@Override
		public HistogramModel<GameAdapter> histogramModel(MLData estimation) {
			int model = HistogramModel.indexMostProbable(estimation);
			return HistogramActionModel.values()[model];
		}
		
		@Override
		public int contextId(GameAdapter game) {
			return game.roundIndex - 1;
		}
		
	};
	
	@Override
	public int inputDimensions() {
		return BNGameAdapter.InputData.FIELDS;
	}

	@Override
	public int outputDimensions() {
		return BNGameAdapter.IdealDataFinal.FIELDS;
	}

	@Override
	public int rounds() {
		return 3;
	}

	@Override
	public int index() {
		return ordinal();
	}
	
}
