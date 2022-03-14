package johnidis.maverick.modelling.models;

import johnidis.maverick.modelling.data.MLData;

public interface HistogramModel<G> extends Model<G,MLData> {
	
	public static final int				INVALID				= -1;

	static public int indexMostProbable (MLData estimation) {
		int maxEstimate = -1;
		double maxValue = Integer.MIN_VALUE;
		for (int a = 0; a < estimation.size(); a++) {
			double v = estimation.getData(a);
			if (maxValue < v) {
				maxValue = v;
				maxEstimate = a;
			}
		}
		return maxEstimate;
	}

	public static int getSliceBipolar(double xValue, int slices) {
		int s = (int) (xValue * slices);
		return (s < -slices) ? -slices : (s < slices) ? s : slices - 1;
	}
	
	public static int getSlice(double xValue, int slices) {
		int s = getSliceBipolar (xValue, slices);
		return (s < 0) ? 0 : s;
	}

	public static int getSlice(MLData prediction, int slices) {
		if (prediction.size() == 1)
			return (getSlice(prediction.getData(0), slices));
		int max = indexMostProbable(prediction);
		double maxValue = prediction.getData(max);
		prediction.setData(max, -1);
		int max2 = indexMostProbable(prediction);
		prediction.setData(max, maxValue);
		int s = (int) ((maxValue - prediction.getData(max2)) * (slices - 1));
		return (s < 0) ? 0 : (s < slices) ? s : slices - 1;
	}

	
	public MLData histogramPoints (MLData ideal);
	public int slices ();
	public int getRound (int contextId);
	public int getSlice (int contextId, MLData prediction);

}
