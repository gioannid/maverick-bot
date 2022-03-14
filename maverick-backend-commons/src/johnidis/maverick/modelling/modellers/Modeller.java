package johnidis.maverick.modelling.modellers;

import java.io.File;
import java.io.FilenameFilter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import johnidis.maverick.Preferences;
import johnidis.maverick.modelling.data.MLData;
import johnidis.maverick.modelling.models.Model;
import johnidis.utils.AbortException;
import johnidis.utils.collection.LevenshteinMap;

public abstract class Modeller<P,G,O> {
	
	private static final boolean	DEBUG						= true;

	public static final Model		MODEL_NA 					= null;
	public static final int			CONTEXTID_NA 				= -1;
	
	public static final int 		STOPPED 					= -1;
	public static final int 		READY 						= 0;
	public static final int 		ACTIVE 						= 1;
	public static final int 		STOPPING 					= 2;
	public static final int 		RETURN_IMMEDIATELY 			= 0;
	public static final int 		RETURN_WHEN_STOPPED 		= -1;

	protected static final String	MODEL_GENERIC				= "$generic";	
	protected static final String 	MODEL_TRAINEE				= "$trainee";

	private static final int		MAX_MODELS_IN_RAM			= 100;
	private static final String 	DEFAULT_PATH				= "data/models";

	public static final String 		pathToModels;

	public static final boolean 	useGenericModel 			= Preferences.MODEL_GENERIC.isOn();

	private final AtomicInteger trainerStatus = new AtomicInteger(READY);

	
	static {
		if (Preferences.MODEL_PATH.set()) {
			pathToModels = Preferences.MODEL_PATH.get();
			System.out.println("Searching for models under path "+pathToModels);
		} else
			pathToModels = DEFAULT_PATH;
		if (Preferences.MODEL_READONLY.isOn())
			System.out.println("No changes will be committed to player models on disk");
	}

	static public class ModellerFilenameFilter implements FilenameFilter {
		
		private final String player;
		private final String fileExtension;

		public ModellerFilenameFilter (String player, String fileExtension) {
			this.player = player;
			this.fileExtension = fileExtension;
		}
		
		
		public boolean exactMatch (String path) {
			return new File (path, 
					player + ((fileExtension != null) ? fileExtension : "")).exists();
		}
		
		@Override
		public boolean accept(File path, String filename) {
			String name = (fileExtension != null) ? filename.replace(fileExtension, "") : filename;
			if ((LevenshteinMap.equal(player, name)) &&
					((fileExtension == null) || (filename.contains(fileExtension)))) {
				if (DEBUG)
					System.out.println ("Similar-match found for model: "+name+" vs. "+player);
				return true;
			} else
				return false;
		}
	}
	

	private final LevenshteinMap<P> playerModellers = new LevenshteinMap<>();
	private final String[] modelQueue = new String[MAX_MODELS_IN_RAM];
	private int modelQueuePos = 0;

	public static void normalize (MLData data, int fixed) {
		double sum = 0;
		double fixedValue = 0;
		if (data == null)
			return;
		if (fixed != -1)
			fixedValue = data.getData(fixed);
		for (int i = 0; i < data.size(); i++)
			if ((fixed == -1) || (i != fixed))
				sum += data.getData(i);
		for (int i = 0; i < data.size(); i++)
			if ((fixed == -1) || (i != fixed))
				data.setData(i, data.getData(i) / (sum / (1 - fixedValue)));
	}
	

	public String playerKey (String player) {
		return player.equals(genericName()) ? 
				genericName() : 
					player.equals(MODEL_TRAINEE) ?
					MODEL_TRAINEE :
						player.toLowerCase().replaceAll("[^a-z0-9_\\-$]", "_");
	}
	
	public P open (String player) {
		if (Preferences.MODEL_TRAIN.isOn())
			return doOpen (MODEL_TRAINEE);
		else
			return doOpen (player);
	}

	protected P doOpen (String player) {
		String key = playerKey(player);
		synchronized (playerModellers) {
			P modeller = playerModellers.get(key);
			if (modeller == null) {
				modeller = instantiate (key, player);
				key = canonicalKey (modeller);
				if (modelQueue[modelQueuePos] != null) {
					persist (playerModellers.remove(modelQueue[modelQueuePos]));
					if (DEBUG)
						System.out.println(getClass().getSimpleName()+
								": Evicted from RAM player model "+modelQueue[modelQueuePos]);
				}
				playerModellers.put(key, modeller);
				modelQueue[modelQueuePos++] = key;
				if (modelQueuePos == MAX_MODELS_IN_RAM)
					modelQueuePos = 0;
			}
			return modeller;
		}
	}

	public boolean isOpen (String player) {
		return playerModellers.containsKey(playerKey(player));
	}
	
	public void persist () {
		synchronized (playerModellers) {
			for (P modeller : playerModellers.values())
				persist(modeller);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void forEach (Predicate<P> iteration) {
		P[] allModellers;
		synchronized (playerModellers) {
			allModellers = (P[]) playerModellers.values().toArray();
		}
		for (P modeller : allModellers)
			if (! iteration.test(modeller))
				return;
	}
	
	public boolean startTrainer (Runnable runnable) {
		if ((trainerStatus.compareAndSet(READY, ACTIVE)) ||
				(trainerStatus.compareAndSet(STOPPED, ACTIVE))) {
			Thread trainer = new Thread(runnable);
			trainer.setPriority(Thread.NORM_PRIORITY / 2);
			trainer.start();
			return true;
		} else
			return false;
	}
	
	public boolean stopTrainer (int millis) {
		boolean succ = trainerStatus.compareAndSet(ACTIVE, STOPPING);
		if ((! succ) || (millis == RETURN_IMMEDIATELY))
			return succ;
		long curTime = System.currentTimeMillis();
		while ((millis == RETURN_WHEN_STOPPED) || ((System.currentTimeMillis() - curTime) < millis)) {
			int status = trainerStatus.get();
			if (status <= 0)
				return true;
			System.out.print("Pending for modelling trainer ");
			if (millis == RETURN_WHEN_STOPPED)
				System.out.println("to stop...");
			else
				System.out.println("for the next "+(System.currentTimeMillis() - curTime + millis)+" ms");
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				break;
			}
		}
		return false;
	}
	
	public int isTraining () {
		return trainerStatus.get();
	}
	
	public void trainerStopped() {
		trainerStatus.compareAndSet(Modeller.STOPPING, Modeller.STOPPED);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + ": "+playerModellers.size() + " player entries";
	}
	
	public void persist (P modeller) {
		if (canonicalKey(modeller).equals(genericName()))
			return;
		if (Preferences.MODEL_READONLY.isOn())
			return;
		doPersist (modeller);
	}
	
	public double addData (String player, Model<G,O> model, G gameSnapshot, char action) throws AbortException {
		if (Preferences.MODEL_TRAIN.isOn())
			return doAddData (MODEL_TRAINEE, model, gameSnapshot, action);
		else
			return doAddData (player, model, gameSnapshot, action);
	}
	
	public O estimate (String player, Model<G,O> model, G gameSnapshot, boolean genericModel) throws AbortException {
		if (Preferences.MODEL_TRAIN.isOn())
			return doEstimate (MODEL_TRAINEE, model, gameSnapshot, false);
		else
			return doEstimate (player, model, gameSnapshot, genericModel);
		
	}
	
	public void setModelling (String player, boolean active) {
		if (DEBUG)
			System.out.println(getClass().getSimpleName()+": Starting modelling for "+player);
		if (Preferences.MODEL_TRAIN.isOn())
			doModelling (MODEL_TRAINEE, active);
		else
			doModelling (player, active);
	}
	
	public abstract P instantiate (String key, String player);
	public abstract String genericName ();
	public abstract String canonicalKey (P modeller);
	protected abstract double doAddData (String player, Model<G,O> model, G gameSnapshot, char action)
			throws AbortException;
	protected abstract O doEstimate (String player, Model<G,O> model, G gameSnapshot, boolean genericModel)
			throws AbortException;
	protected abstract void doModelling (String player, boolean active);
	protected abstract void doPersist (P modeller);
	
}
