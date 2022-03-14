package johnidis.maverick.modelling.player;

import java.io.File;

import johnidis.maverick.Preferences;
import johnidis.maverick.modelling.modellers.Modeller;
import johnidis.maverick.modelling.modellers.Modeller.ModellerFilenameFilter;
import johnidis.maverick.modelling.models.Model;

public abstract class PlayerModeller<I,A,E> {

	public static final int	DEFAULT_WINDOW = 200;
	public static final int DEFAULT_AGING_EPOCH = 30;
	
	protected static final int samplesWindow;
	protected static final int agingEpoch;
	
	public final Class<? extends Model> referenceModel;
	public final String canonicalName;

	protected final String pathname;
	protected volatile boolean trainingActive = false;

	
	static {
		if (Preferences.MODEL_WINDOW.set()) {
			samplesWindow = Preferences.MODEL_WINDOW.getValue().intValue();
			System.out.println("Setting modelling window = " + samplesWindow);
		} else
			samplesWindow = DEFAULT_WINDOW;
		if (Preferences.MODEL_AGINGEPOCH.set()) {
			agingEpoch = Preferences.MODEL_AGINGEPOCH.getValue().intValue();
			System.out.println("Setting aging epoch = " + agingEpoch);
		} else
			agingEpoch = DEFAULT_AGING_EPOCH;
	}
	

	public PlayerModeller (Class<? extends Model> m, String key) { 
		referenceModel = m;
		String name = (referenceModel != null) ? referenceModel.getSimpleName() : getClass().getSimpleName();
		ModellerFilenameFilter filter = new ModellerFilenameFilter (key, null);
		if (filter.exactMatch(Modeller.pathToModels)) {
			pathname = Modeller.pathToModels + '/' + key;
			canonicalName = key;
			System.out.println (name + ": Loading player model: "+canonicalName);
		} else {
			File[] foundModels = new File(Modeller.pathToModels).listFiles(filter);
			if (foundModels.length == 0) {
				pathname = Modeller.pathToModels + '/' + key;
				canonicalName = key;
				System.out.println (name + ": New player model: "+canonicalName);
			} else {
				pathname = foundModels[0].getPath();
				canonicalName = foundModels[0].getName();
				System.out.println (name + ": Loading player model under canonical name: "+canonicalName);
			}
		}
	}
	
	public PlayerModeller (Class<? extends Model> m) {
		referenceModel = m;
		canonicalName = null;
		pathname = null;
	}
	
	
	public void mkdirs() {
		if (canonicalName != null)
			new File(pathname).mkdirs();
	}

	public void setModelling (boolean active) {
		trainingActive = active;
	}
	
	public boolean isModelling () {
		return trainingActive;
	}
	
	public abstract void persist();	
	public abstract void addData(Model<?,E> model, int contextId, I input, A ideal);
	public abstract long getSamples(Model<?,E> model, int contextId);
	public abstract E estimate(Model<?,E> model, int contextId, I input);
	public abstract double getError(Model<?,E> model, int contextId);

}