package johnidis.maverick.modelling.player;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;

import johnidis.maverick.Preferences;
import johnidis.maverick.modelling.data.FloatingDataSet;
import johnidis.maverick.modelling.data.MLData;
import johnidis.maverick.modelling.data.MLDataSet;
import johnidis.maverick.modelling.models.Model;
import johnidis.maverick.modelling.models.NNModel;

public class NNPlayerModeller extends PlayerModeller<MLData,MLData,MLData> {
	
	private static final boolean	DEBUG						= true;

	private static final int		MIN_TRAIN_ITERATIONS		= 2500;
	
	private static int samplesWindow = 0;
	
	protected final File[/*model*/][/*round*/] dataFile;
	protected final MultiLayerNetwork[/*model*/][/*round*/] network;
	protected final MultiLayerNetwork[/*model*/][/*round*/] bestNetwork;
	protected final File[/*model*/][/*round*/] networkFile;
	protected final File[/*model*/][/*round*/] trainFile;
	protected final boolean[/*model*/][/*round*/] train;
	protected final double[/*model*/][/*round*/] lowestError;
	
	protected final MLDataSet[/*model*/][/*round*/] data;
	protected final Propagation[/*model*/][/*round*/] trainer; 
	protected final TrainingContinuation[/*model*/][/*round*/] bestTrainer; 
	protected final int[/*model*/][/*round*/] epoch;

	
	static public FloatingDataSet newDataSet () {
		return new FloatingDataSet (samplesWindow, 0);
	}
	

	public NNPlayerModeller (Class<? extends Model> rm, String p) {
		super (rm, p);
		int models = referenceModel.getEnumConstants().length;
		dataFile = new File[models][];
		network = new BasicNetwork[models][];
		bestNetwork = new BasicNetwork[models][];
		networkFile = new File[models][];
		trainFile = new File[models][];
		train = new boolean[models][];
		lowestError = new double[models][];
		data = new MLDataSet[models][];
		trainer = new Propagation[models][];
		bestTrainer = new TrainingContinuation[models][];
		epoch = new int [models][];
		for (Model model : referenceModel.getEnumConstants()) {
			int m = model.index();
			int rounds = model.roundsSupported();
			dataFile[m] = new File[rounds];
			network[m] = new BasicNetwork[rounds];
			bestNetwork[m] = new BasicNetwork[rounds];
			networkFile[m] = new File[rounds];
			trainFile[m] = new File[rounds];
			train[m] = new boolean[rounds];
			lowestError[m] = new double[rounds];
			data[m] = new MLDataSet[rounds];
			trainer[m] = new Propagation[rounds];
			bestTrainer[m] = new TrainingContinuation[rounds];
			epoch[m] = new int[rounds];
			for (int r = 0; r < rounds; r++) {
				String fileName;
				if (rounds > 1)
					fileName = pathname + '/' + model.toString() + "_" + r;
				else
					fileName = pathname + '/' + model.toString();
				dataFile[m][r] = new File(fileName + ".raw"); 
				if (dataFile[m][r].exists()) {
					if (DEBUG)
						System.out.print("Opening data file "+dataFile[m][r]);
					try {
						ObjectInputStream ois = new ObjectInputStream (new BufferedInputStream (new FileInputStream 
								(dataFile[m][r])));
						data[m][r] = (MLDataSet) ois.readObject();
						lowestError[m][r] = ois.readDouble();
						ois.close();
						if (DEBUG)
							System.out.println(", error = "+lowestError[m][r]);
					} catch (Exception e) {
						e.printStackTrace();
						data[m][r] = newDataSet ();
						lowestError[m][r] = 1.0;
					}
				} else {
					if (DEBUG)
						System.out.println("Data file "+dataFile[m][r]+" not found, creating from scratch");
					data[m][r] = newDataSet ();
					lowestError[m][r] = 1.0;
				}
				networkFile[m][r] = new File(fileName + ".eg"); 
				if (networkFile[m][r].exists()) {
					if (DEBUG)
						System.out.println("Opening model file "+networkFile[m][r]);
					network[m][r] = (BasicNetwork) EncogDirectoryPersistence.loadObject(networkFile[m][r]);
				} else {
					if (DEBUG)
						System.out.println("Model file "+networkFile[m][r]+" not found, creating from scratch");
					network[m][r] = ((NNModel) model).newNetwork();
				}
				bestNetwork[m][r] = (BasicNetwork) network[m][r].clone();
				trainer[m][r] = new ResilientPropagation (network[m][r], data[m][r]);
				trainer[m][r].setThreadCount(1);
				trainer[m][r].setBatchSize(1);
				bestTrainer[m][r] = trainer[m][r].pause();
				trainFile[m][r] = new File(fileName + ".train");
				if (trainFile[m][r].exists()) {
					if (DEBUG)
						System.out.println("Opening train file "+trainFile[m][r]);
					try {
						bestTrainer[m][r] = (TrainingContinuation) persistor.read (
								new BufferedInputStream(new FileInputStream(trainFile[m][r])));
						trainer[m][r].resume(bestTrainer[m][r]);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					if (DEBUG)
						System.out.println("Train file "+trainFile[m][r]+" not found, starting from scratch");
				}
				train[m][r] = true;
			}
		}
	}
	
	@Override
	public void persist () {
		if (canonicalName.equals(BehavioralModeller.GENERIC_PLAYER_MODEL))
			return;
		mkdirs();
		for (Model model : referenceModel.getEnumConstants())
			for (int r = 0; r < model.roundsSupported(); r++)
				persist (model, r);
	}
	
	protected void persist (Model model, int r) {
		if (Preferences.MODEL_READONLY.isOn())
			return;
		mkdirs();
		int m = model.index();
		synchronized (data[m][r]) {
			if (DEBUG)
				System.out.print(canonicalName+": Saving "+model+"_"+r);
			try {
				ObjectOutputStream oos = new ObjectOutputStream (new BufferedOutputStream (
				        new FileOutputStream(dataFile[m][r])));
				oos.writeObject(data[m][r]);
				oos.writeDouble(lowestError[m][r]);
				if (DEBUG)
					System.out.println(", error = "+lowestError[m][r]);
				oos.close();
			} catch (Exception e) {
				System.err.println("Error while writing file: "+ dataFile[m][r]);
				e.printStackTrace();
			}
			EncogDirectoryPersistence.saveObject(networkFile[m][r], bestNetwork[m][r]);
			try {
				persistor.save((new BufferedOutputStream(new FileOutputStream(trainFile[m][r]))), bestTrainer[m][r]);
			} catch (FileNotFoundException e) {
				System.err.println("Error while accessing file: "+ trainFile[m][r]);
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void addData (Model model, int round, MLData input, MLData ideal) {
		int m = model.index();
		synchronized (data[m][round]) {
			data[m][round].add(input, ideal);
			train[m][round] = true;
			epoch[m][round] = 0;
			double error = 1.0;
			lowestError[m][round] = error;
			for (int i = 0; i < MIN_TRAIN_ITERATIONS; i++)
				error = NNTrainer.trainIteration(this, model, round);
			if (DEBUG)
				System.out.println(canonicalName+" "+model.toString()+": data(round="+round+") new sample, error = "+
						lowestError[m][round]);
		}
	}
	
	@Override
	public long getSamples (Model model, int round) {
		return data[model.index()][round].getRecordCount();
	}
	
	@Override
	public MLData estimate (Model model, int round, MLData input) {
		int m = model.index();
		synchronized (data[m][round]) {
			if (lowestError[m][round] < 1.0)
				return bestNetwork[m][round].compute(input);
			return null;
		}
	}
	
	@Override
	public double getError (Model model, int round) {
		return lowestError[model.index()][round];
	}
	
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder(canonicalName).append(" [");
		for (Model model : referenceModel.getEnumConstants()) {
			str.append(' ').append(model.toString()).append(":(");
			for (int r = 0; r < model.roundsSupported(); r++)
				str.append(' ').append(data[model.index()][r].getRecordCount());
			str.append(')');
		}
		return str.append(']').toString();
	}

}
