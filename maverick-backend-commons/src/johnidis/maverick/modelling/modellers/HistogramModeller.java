package johnidis.maverick.modelling.modellers;

import java.util.function.Function;

import johnidis.maverick.modelling.adapters.*;
import johnidis.maverick.modelling.data.BasicMLData;
import johnidis.maverick.modelling.data.MLData;
import johnidis.maverick.modelling.models.HistogramModel;
import johnidis.maverick.modelling.models.Model;
import johnidis.maverick.modelling.player.HistogramPlayerModeller;
import johnidis.maverick.modelling.player.PlayerModeller;
import johnidis.utils.AbortException;

public class HistogramModeller<E> extends StatelessModeller {
	
	private static final boolean	DEBUG						= false;

	public static final int 		INVALID 					= -1;
	
	private final Modeller<? extends PlayerModeller<?,?,E>,GameAdapter,E> underlyingModeller;
	private final Model<GameAdapter,E> underlyingModel;
	
	
	public HistogramModeller (Class<? extends Model> m, 
			Modeller<? extends PlayerModeller<?,?,E>,GameAdapter,E> umr, Model<GameAdapter,E> uml) {
		super (m, HistogramPlayerModeller.class);
		underlyingModeller = umr;
		underlyingModel = uml;
	}
	

	public double addPoint (String player, GameAdapter underlyingGame, char action, Function<E,MLData> conversion) throws AbortException {
		E prediction = underlyingModeller.estimate (player, underlyingModel, underlyingGame, false);
		if (prediction != null) {
			MLData estimation;
			if (conversion != null)
				estimation = conversion.apply(prediction);
			else
				estimation = (MLData) prediction;
			if (DEBUG) {
				PlayerModeller<?,?,E> playerModeller = underlyingModeller.open(player);
				System.out.println("  add: "+playerModeller.toString());
				System.out.println("    estimation->"+estimation);
			}
			int outd = underlyingModel.outputDimensions();
			MLData ideal = null;
			if (outd > 0) {
				ideal = new BasicMLData(outd);
				underlyingGame.collectIdeals(action, ideal.getData());
			}
			HistogramModel<GameAdapter> model = underlyingModel.histogramModel(prediction);
			return addData (player, model, model.contextId(underlyingGame), estimation, model.histogramPoints(ideal));
		} else
			return INVALID;
	}
	
	public MLData getPoints (String player, GameAdapter underlyingGame, Function<E,MLData> conversion) throws AbortException {
		MLData estimation = null;
		E prediction = underlyingModeller.estimate (player, underlyingModel, underlyingGame, false);
		if ((prediction == null) && useGenericModel) {
			player = genericName();
			prediction = underlyingModeller.estimate (player, underlyingModel, underlyingGame, false);
		}
		if (prediction != null) {
			if (conversion != null)
				estimation = conversion.apply(prediction);
			else
				estimation = (MLData) prediction;
			if (DEBUG) {
				PlayerModeller<?,?,E> playerModeller = underlyingModeller.open(player);
				System.out.println("  get: "+playerModeller.toString());
				System.out.println("    estimation->"+estimation);
			}
			HistogramModel<GameAdapter> model = underlyingModel.histogramModel(prediction);
			MLData occ = estimate (player, model, model.contextId(underlyingGame), estimation, false);
			if (occ != null) {
				estimation = occ;
				if (DEBUG)
					System.out.println("    model=" + model.toString() + ", histogram=" + occ);
			} else if (underlyingModel.outputDimensions() != model.outputDimensions()) {
				estimation = model.histogramPoints(estimation);
			}
		}
		return estimation;
	}
	
	@Override
	public void persist() {
		underlyingModeller.persist();
		super.persist();
	}

	@Override
	public boolean startTrainer(Runnable runnable) {
		throw new RuntimeException ("HistogramModeller.startTrainer() not supported");
	}

	@Override
	public boolean stopTrainer(int millis) {
		throw new RuntimeException ("HistogramModeller.stopTrainer() not supported");
	}
	
}
