package johnidis.maverick.modelling.modellers;

import johnidis.maverick.modelling.adapters.GameAdapter;
import johnidis.maverick.modelling.data.MLData;
import johnidis.maverick.modelling.models.Model;
import johnidis.maverick.modelling.player.PlayerModeller;
import johnidis.utils.AbortException;

public class StatefulModeller extends Modeller<PlayerModeller<GameAdapter,Character,MLData>,GameAdapter,MLData> {

	private static final boolean	DEBUG									= false;

	public final Class<? extends PlayerModeller<?,?,MLData>> referencePlayerModel;
	public final Class<? extends Model> referenceModel;

	protected final PlayerModeller<GameAdapter,Character,MLData> defaultModeller;
	
	public StatefulModeller (Class<? extends Model> m, Class<? extends PlayerModeller<GameAdapter,Character,MLData>> c) {
		referencePlayerModel = c;
		referenceModel = m;
		defaultModeller = open (MODEL_GENERIC);
		if (useGenericModel)
			System.out.println(getClass().getSimpleName()+" ("+referenceModel.getSimpleName()+
					") will utilize generic model if necessary");
	}


	@Override
	public PlayerModeller instantiate(String key, String player) {
		try {
			PlayerModeller modeller = referencePlayerModel.getDeclaredConstructor (
					Class.class, String.class).newInstance(referenceModel, key);
			return modeller;
		} catch (Exception e) {
			throw new RuntimeException ("Cannot instantiate "+referenceModel.getSimpleName()+" for player "+key, e);
		}
	}

	@Override
	public String genericName() {
		return MODEL_GENERIC;
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
	protected double doAddData(String player, Model<GameAdapter,MLData> model, GameAdapter adapter, char action) throws AbortException {
		PlayerModeller<GameAdapter,Character,MLData> modeller = open (player);
		int context = model.contextId(adapter);
		modeller.addData (model, context, adapter, action);
		double error = modeller.getError(model, context);
		if (DEBUG)
			System.out.println (player+": Last "+model.toString()+" error for context "+context+" = "+error);
		return error;
	}

	@Override
	protected MLData doEstimate(String player, Model<GameAdapter,MLData> model, GameAdapter adapter, boolean genericModel) throws AbortException {
		MLData estimation = null;
		int context = model.contextId(adapter);
		synchronized (this) {
			estimation = open(player).estimate(model, context, adapter);
			if ((estimation == null) && genericModel) {
				estimation = defaultModeller.estimate(model, context, adapter);
			}
		}
		if (DEBUG) {
			System.out.println (adapter);
			System.out.println (player+": "+model+" for context "+context+" = "+estimation);
		}
		return estimation;
	}

	@Override
	protected void doModelling(String player, boolean active) {
		open(player).setModelling(active);
	}

	@Override
	public String toString() {
		return super.toString() + " , model family=" + referenceModel.getSimpleName();
	}

}
