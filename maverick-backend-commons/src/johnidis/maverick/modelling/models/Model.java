package johnidis.maverick.modelling.models;

public interface Model<G,O> {
	
	public int inputDimensions ();
	public int outputDimensions ();
	public int rounds ();
	public int index ();
	public HistogramModel<G> histogramModel (O estimation);
	public int contextId (G game);

}
