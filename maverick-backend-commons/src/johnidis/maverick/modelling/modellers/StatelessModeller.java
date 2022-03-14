package johnidis.maverick.modelling.modellers;

import johnidis.maverick.modelling.adapters.GameAdapter;
import johnidis.maverick.modelling.data.BasicMLData;
import johnidis.maverick.modelling.data.MLData;
import johnidis.maverick.modelling.models.Model;
import johnidis.maverick.modelling.player.PlayerModeller;
import johnidis.utils.AbortException;

public class StatelessModeller extends Modeller<PlayerModeller<MLData,MLData,MLData>,GameAdapter,MLData> {

	private static final boolean	DEBUG									= false;

	public final Class<? extends PlayerModeller<MLData,MLData,MLData>> referencePlayerModel;
	public final Class<? extends Model> referenceModel;

	protected final PlayerModeller<MLData,MLData,MLData> defaultModeller;
	
	
	public StatelessModeller (Class<? extends Model> m, Class<? extends PlayerModeller<MLData,MLData,MLData>> c) {
		referencePlayerModel = c;
		referenceModel = m;
		defaultModeller = open (MODEL_GENERIC);
		if (useGenericModel)
			System.out.println(getClass().getSimpleName()+" ("+referenceModel.getSimpleName()+
					") will utilize generic model if necessary");
	}

	@Override
	protected void doModelling (String player, boolean active) {
		open(player).setModelling(active);
	}
	
	public double addData (String player, Model<?,MLData> model, int context, MLData input, MLData ideal) {
		PlayerModeller<MLData,MLData,MLData> modeller = open (player);
		modeller.addData(model, context, input, ideal);
		double error = modeller.getError(model, context);
		if (DEBUG)
			System.out.println (player+": Last "+model.toString()+" error for context "+context+" = "+error);
		return error;
	}
	
	@Override
	protected double doAddData (String player, Model<GameAdapter,MLData> model, GameAdapter gameSnapshot, char action) throws AbortException {
		int ind = model.inputDimensions();
		int outd = model.outputDimensions();
		MLData input = null, ideal = null;
		if (ind > 0) {
			input = new BasicMLData(ind);
			gameSnapshot.collectInputs(action, input.getData());
		}
		if (outd > 0) {
			ideal = new BasicMLData(outd);
			gameSnapshot.collectIdeals(action, ideal.getData());
		}
		return addData (player, model, model.contextId(gameSnapshot), input, ideal);
	}
	
	public MLData estimate (String player, Model<GameAdapter,MLData> model, int context, MLData input, boolean genericModel) {
		MLData estimation = null;
		synchronized (this) {
			estimation = open(player).estimate(model, context, input);
			if ((estimation == null) && genericModel) {
				estimation = defaultModeller.estimate(model, context, input);
			}
		}
		if (DEBUG)
			System.out.println (player+": "+model+"("+input+") for context "+context+" = "+estimation);
		return estimation;
	}
	
	@Override
	protected MLData doEstimate (String player, Model<GameAdapter,MLData> model, GameAdapter gameSnapshot, boolean genericModel) throws AbortException {
		int ind = model.inputDimensions();
		MLData input = null;
		if (ind > 0) {
			input = new BasicMLData(ind);
			gameSnapshot.collectInputs(GameAdapter.ACTION_NA, input.getData());
		}
		return estimate (player, model, model.contextId(gameSnapshot), input, genericModel);
	}
	
	@Override
	public String genericName() {
		return MODEL_GENERIC;
	}
	@Override
	public PlayerModeller instantiate (String key, String player) {
		try {
			PlayerModeller modeller = referencePlayerModel.getDeclaredConstructor (
					Class.class, String.class).newInstance(referenceModel, key);
			return modeller;
		} catch (Exception e) {
			throw new RuntimeException ("Cannot instantiate "+referenceModel.getSimpleName()+" for player "+key, e);
		}
	}
	
	@Override
	protected void doPersist(PlayerModeller modeller) {
		modeller.persist();
	}
	
	@Override
	public String canonicalKey(PlayerModeller modeller) {
		return modeller.canonicalName;
	}

	@Override
	public String toString() {
		return super.toString() + " , model family=" + referenceModel.getSimpleName();
	}

}
