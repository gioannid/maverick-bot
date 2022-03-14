package johnidis.maverick.modelling.player;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import johnidis.maverick.modelling.data.BasicMLData;
import johnidis.maverick.modelling.data.FloatingDataSet;
import johnidis.maverick.modelling.data.MLData;
import johnidis.maverick.modelling.data.MLDataPair;
import johnidis.maverick.modelling.data.MLDataSet;
import johnidis.maverick.modelling.models.HistogramModel;
import johnidis.maverick.modelling.models.Model;

public class HistogramPlayerModeller extends PlayerModeller<MLData,MLData,MLData> {

	protected static final boolean	DEBUG						= true;
	protected static final boolean	DEBUG_VERBOSE				= false;
	protected static final boolean	DEBUG_IGNORE_HISTOGRAM		= false;			

	protected final MLData[] sliceId; 
	protected MLDataSet[/*model*/][/*round*/] data;
	protected MLData[/*model*/][/*round*/][/*slice*/] sum;
	protected final File file;

	
	public HistogramPlayerModeller(Class<? extends Model> model, String p) {
		super (model, p);
		HistogramModel[] models = (HistogramModel[]) referenceModel.getEnumConstants();
		sliceId = new BasicMLData[models[0].slices()];
		for (int s = 0; s < models[0].slices(); s++)
			sliceId[s] = new BasicMLData(new double[] {s});
		String fileName = pathname + '/' + referenceModel.getSimpleName();
		file = new File(fileName + ".raw"); 
		if (file.exists()) {
			if (DEBUG)
				System.out.println("Opening data file "+file);
			try {
				ObjectInputStream ois = new ObjectInputStream (new BufferedInputStream (new FileInputStream(file)));
				data = (MLDataSet[][]) ois.readObject();
				sum = (MLData[][][]) ois.readObject();
				ois.close();
				for (int m = 0; m < models.length; m++) {
					int rounds = models[m].rounds();
					for (int r = 0; r < rounds; r++) {
						((FloatingDataSet) data[m][r]).setAgingEpoch(agingEpoch);
						((FloatingDataSet) data[m][r]).setWindowSize(samplesWindow);
					}
				}
			} catch (Exception e) {
				initData ();
				e.printStackTrace();
			}
		} else {
			initData();
			if (DEBUG)
				System.out.println("Data file "+file+" not found, creating from scratch");
		}
	}
	
	
	private void initData () {
		HistogramModel[] models = (HistogramModel[]) referenceModel.getEnumConstants();
		data = new MLDataSet[models.length][];
		sum = new MLData[models.length][][];
		for (int m = 0; m < models.length; m++) {
			int rounds = models[m].rounds();
			int dim = models[m].outputDimensions();
			int slices = models[m].slices();
			data[m] = new MLDataSet[rounds];
			sum[m]= new MLData[rounds][slices];
			for (int r = 0; r < rounds; r++) {
				data[m][r] = new FloatingDataSet (samplesWindow, agingEpoch);
				for (int s = 0; s < slices; s++)
					sum[m][r][s] = new BasicMLData(dim);
			}
		}
	}
	
	@Override
	public void persist() {
		mkdirs();
		if (DEBUG)
			System.out.println(canonicalName+": Saving "+file);
		synchronized (data) {
			try {
				ObjectOutputStream oos = new ObjectOutputStream (new BufferedOutputStream (new FileOutputStream(file)));
				oos.writeObject(data);
				oos.writeObject(sum);
				oos.close();
			} catch (Exception e) {
				System.err.println("Error while writing file: "+ file);
				e.printStackTrace();
			}
		}
	}

	@Override
	public void addData(Model model, int contextId, MLData prediction, MLData actual) {
		int s = ((HistogramModel<?>) model).getSlice (contextId, prediction);
		int round = ((HistogramModel<?>) model).getRound (contextId);
		int m = model.index();
		synchronized (data) {
			data[m][round].add(sliceId[s], actual);
			sum[m][round][s] = new BasicMLData(model.outputDimensions());
			for (MLDataPair record : data[m][round]) {
				int slice = (int) record.getFirstArray()[0];
				for (int a = 0; a < model.outputDimensions(); a++)
					sum[m][round][slice].add(a, record.getSecondArray()[a]);
			}
			if (DEBUG)
				System.out.println(canonicalName+" "+model.toString()+": data(round="+round+", slice="+s+") = "+
						sum[m][round][s]+", total="+data[m][round].getRecordCount()+", prediction="+prediction);
		}
	}

	@Override
	public long getSamples(Model model, int contextId) {
		return data[model.index()][((HistogramModel<?>) model).getRound (contextId)].getRecordCount();
	}

	@Override
	public MLData estimate(Model model, int contextId, MLData prediction) {
		if (DEBUG_IGNORE_HISTOGRAM)
			return null;
		MLData result;
		int m = model.index();
		int round = ((HistogramModel<?>) model).getRound (contextId);
		int s = ((HistogramModel<?>) model).getSlice (contextId, prediction);
		if (s == HistogramModel.INVALID) {
			if (DEBUG_VERBOSE)
				System.out.println(canonicalName+" "+model.toString()+": data(round="+round+
						") returned invalid slice for prediction "+prediction);
			return null;
		}
		synchronized (data) {
			result = sum[m][round][s].clone();
		}
		for (int i = 0; i < result.size(); i++)
			if (result.getData(i) > 0) {
				if (DEBUG_VERBOSE)
					System.out.println(canonicalName+" "+model.toString()+": result(round="+round+", slice="+s+") = "+
							result+", total="+data[m][round].getRecordCount()+", prediction="+prediction);
				return result;
			}
		if (DEBUG_VERBOSE)
			System.out.println(canonicalName+" "+model.toString()+": data(round="+round+
					") returned zero histogram data for prediction "+prediction);
		return null;
	}

	@Override
	public double getError(Model model, int contextId) {
		return 0;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder(canonicalName).append(":");
		Model[] models = referenceModel.getEnumConstants();
		synchronized (data) {
			for (int m = 0; m < models.length; m++) {
				str.append(" {").append(models[m].toString());
				for (int r = 0; r < sum[m].length; r++) {
					str.append(" [");
					for (int s = 0; s < sum[m][r].length; s++) {
						int allActuals = 0;
						for (int i = 0; i < sum[m][r][s].size(); i++)
							allActuals += sum[m][r][s].getData(i);
						str.append(" (").append(allActuals).append(")");
					}
					str.append("]");
				}
				str.append("}");
			}
		}
		return str.toString();
	}
}
