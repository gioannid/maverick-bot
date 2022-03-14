package johnidis.maverick.modelling.player;

import java.util.concurrent.atomic.AtomicBoolean;

import johnidis.maverick.modelling.modellers.BehavioralModeller;
import johnidis.maverick.modelling.modellers.StatelessModeller;
import johnidis.maverick.modelling.models.Model;
import johnidis.maverick.modelling.models.NNModel;

import org.encog.mathutil.error.ErrorCalculation;
import org.encog.mathutil.error.ErrorCalculationMode;
import org.encog.ml.data.MLData;
import org.encog.neural.networks.BasicNetwork;

public class NNTrainer implements Runnable {

	private static final boolean	DEBUG						= false;

	public static final double		TARGET_ERROR				= 0.01;
	public static final int			EPOCHS_TO_SAVE				= 5000;
	public static final int			EPOCHS_TO_SUSPEND_TRAINING	= 100000;
	
	protected final StatelessModeller modeller;
	
	private static boolean willTrain(NNPlayerModeller playerModeller, Model model, int round) {
		int m = model.index();
		if (! playerModeller.train[m][round])
			return false;
		if (playerModeller.data[m][round].getRecordCount() < model.inputDimensions())
			return false;
		return true;
	}

	static double trainIteration(NNPlayerModeller playerModeller, Model model, int round) {
		int m = model.index();
		if (! willTrain (playerModeller, model, round))
			return playerModeller.lowestError[m][round];
		playerModeller.epoch[m][round]++;
		playerModeller.trainer[m][round].iteration();
		double error = playerModeller.trainer[m][round].getError();
		if (DEBUG)
			System.out.println (playerModeller.epoch[m][round]+" "+playerModeller.canonicalName+
					": "+model+"_"+round+" error = "+error);
		if (error < playerModeller.lowestError[m][round]) {
			playerModeller.bestNetwork[m][round] = (BasicNetwork) playerModeller.network[m][round].clone();
			playerModeller.bestTrainer[m][round] = playerModeller.trainer[m][round].pause();
			playerModeller.lowestError[m][round] = error;
			if (DEBUG)
				System.out.println ("-- new low error!");
		}
		if (error < TARGET_ERROR) {
			playerModeller.train[m][round] = false;
			playerModeller.persist (model, round);
		}
		if (playerModeller.epoch[m][round] > EPOCHS_TO_SUSPEND_TRAINING) {
			if (DEBUG)
				System.out.println (playerModeller.canonicalName+": "+model+"_"+round+": suspending training");
			playerModeller.train[m][round] = false;
		}
		if ((playerModeller.epoch[m][round] % EPOCHS_TO_SAVE) == 0) {
			if (DEBUG)
				System.out.print (playerModeller.canonicalName+": "+model+"_"+round+": "+playerModeller.epoch[m][round]+" epochs, "); 
			playerModeller.persist (model, round);
		}
		return error;
	}


	public NNTrainer (BehavioralModeller m) {
		modeller = m;
	}
		
	@Override
	public void run() {
		final AtomicBoolean trainOne = new AtomicBoolean();

		ErrorCalculation.setMode(ErrorCalculationMode.RMS);
		do {
			trainOne.set(false);

			modeller.iterate(new BehavioralModeller.Iterator<PlayerModeller<MLData,MLData,MLData>>() {
				@Override
				public boolean execute(PlayerModeller playerModeller) {
					if (! playerModeller.isModelling())
						return true;
					for (Model model : NNModel.values()) {
						for (int r = 0; r < model.roundsSupported(); r++) {
							if (modeller.isTraining() != BehavioralModeller.ACTIVE)
								return false;
							synchronized (((NNPlayerModeller) playerModeller).data[model.index()][r]) {

								if (willTrain ((NNPlayerModeller) playerModeller, model, r))
									trainOne.set(true);
								trainIteration ((NNPlayerModeller) playerModeller, model, r);

							}
						}
					}
					return true;
				}
			});

			if (! trainOne.get())
				try {
					if (DEBUG)
						System.out.println("zzz...");
					Thread.sleep (500);
				} catch (InterruptedException e) {
					modeller.stopTrainer(BehavioralModeller.RETURN_IMMEDIATELY);
				}
				else Thread.yield();
		} while (modeller.isTraining() == BehavioralModeller.ACTIVE);

		modeller.iterate(new BehavioralModeller.Iterator<PlayerModeller<MLData,MLData,MLData>>() {
			@Override
			public boolean execute(PlayerModeller playerModeller) {
				if (playerModeller.isModelling())
					playerModeller.persist();
				return true;
			}
		});
		
		modeller.trainerStopped();
		if (DEBUG)
			System.out.println("Exiting working thread ("+modeller.isTraining()+")");
	}
	
}