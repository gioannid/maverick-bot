package johnidis.maverick.modelling.models;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.RBM;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

import johnidis.maverick.modelling.adapters.*;
import johnidis.maverick.modelling.data.MLData;

public enum NNModel implements Model<GameAdapter,MLData> {
	
	Hand (HandGameAdapter.InputData.FIELDS, HandGameAdapter.IdealData.FIELDS, 1) {
		
		@Override
		public MultiLayerNetwork newNetwork() {
			return null;
		}
		
		@Override
		public HistogramModel<GameAdapter> histogramModel (MLData estimation) {
			return null;
		}

	},

	
	Action (ActionGameAdapter.InputData.FIELDS, ActionGameAdapter.IdealData.FIELDS, 4) {
		
		private static final int		MAX_NEURONS					= 30;

		@Override
		public MultiLayerNetwork newNetwork() {
	        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
	                .iterations(EPOCHS_TO_SUSPEND_TRAINING)
	                .optimizationAlgo(OptimizationAlgorithm.LINE_GRADIENT_DESCENT)
	                .list()
	                .layer(0, new RBM.Builder().nIn(ActionGameAdapter.InputData.FIELDS).nOut(MAX_NEURONS).
	                		lossFunction(LossFunction.KL_DIVERGENCE).build())
	                .layer(1, new RBM.Builder().nIn(MAX_NEURONS).nOut(MAX_NEURONS / 2).
	                		lossFunction(LossFunction.KL_DIVERGENCE).build())
	                .layer(2, new RBM.Builder().nIn(MAX_NEURONS / 2).nOut(MAX_NEURONS / 4).
	                		lossFunction(LossFunction.KL_DIVERGENCE).build())
	                .layer(3, new RBM.Builder().nIn(MAX_NEURONS / 4).nOut(MAX_NEURONS / 6).
	                		lossFunction(LossFunction.KL_DIVERGENCE).build())						//encoding stops
	                .layer(4, new RBM.Builder().nIn(MAX_NEURONS / 6).nOut(MAX_NEURONS / 4).
	                		lossFunction(LossFunction.KL_DIVERGENCE).build()) 						//decoding starts
	                .layer(5, new RBM.Builder().nIn(MAX_NEURONS / 4).nOut(MAX_NEURONS / 2).
	                		lossFunction(LossFunction.KL_DIVERGENCE).build())
	                .layer(6, new RBM.Builder().nIn(MAX_NEURONS / 2).nOut(MAX_NEURONS).
	                		lossFunction(LossFunction.KL_DIVERGENCE).build())
	                .layer(7, new OutputLayer.Builder(LossFunction.MSE).activation(Activation.SIGMOID).
	                		nIn(MAX_NEURONS).nOut(ActionGameAdapter.IdealData.FIELDS).build())
	                .pretrain(true).backprop(true)
	                .build();
	        return new MultiLayerNetwork(conf);
		}
		
		@Override
		public HistogramModel<GameAdapter> histogramModel (MLData estimation) {
			int model = HistogramModel.indexMostProbable(estimation);
			return HistogramActionModel.values()[model];
		}

	};
	

	private static final int		EPOCHS_TO_SUSPEND_TRAINING	= 100000;
	public static final int			COUNT						= values().length;

	private final int inputData, idealData, rounds;
	
	
	private NNModel (int inp, int id, int r) {
		inputData = inp;
		idealData = id;
		rounds = r;
	}

	@Override
	public int inputDimensions() {
		return inputData;
	}

	@Override
	public int outputDimensions() {
		return idealData;
	}

	@Override
	public int rounds() {
		return rounds;
	}
	
	@Override
	public int index() {
		return ordinal();
	}

	@Override
	public int contextId(GameAdapter game) {
		return game.roundIndex - 1;
	}

	abstract public MultiLayerNetwork newNetwork ();

}