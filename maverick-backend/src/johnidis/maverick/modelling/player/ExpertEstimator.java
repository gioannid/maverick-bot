package johnidis.maverick.modelling.player;

import java.util.concurrent.atomic.AtomicBoolean;

import johnidis.maverick.modelling.modellers.ExpertModeller;

public class ExpertEstimator implements Runnable {

	private static final boolean DEBUG = false;
	
	static volatile ExpertPlayerModeller.Job jobInProgress = null; 

	private final ExpertModeller modeller;
	
	public ExpertEstimator (ExpertModeller m) {
		modeller = m;
	}
		
	
	@Override
	public void run() {
		final AtomicBoolean trainOne = new AtomicBoolean();

		do {
			trainOne.set(false);

			modeller.forEach(playerModeller -> {
				if (! playerModeller.isModelling())
					return true;
				if (modeller.isTraining() != ExpertModeller.ACTIVE)
					return false;
				ExpertPlayerModeller.Job job = playerModeller.jobsQueue.poll();
				if (job != null) {
					jobInProgress = job;
					trainOne.set(true);
					job.initialize();
					long time;
					if (DEBUG) {
						System.out.println("Starting ExpertEstimator.execute() for "+job);
						time = System.nanoTime();
					}
					int nowProcessing = 0;
allPossibleHoles:	for (int i = 0; i < 51; i++)
						for (int j = i + 1; j < 52; j++) {
							nowProcessing++;
							if (job.abort) {
								if (DEBUG)
									System.out.println("Aborting ExpertEstimator.execute() for "+job);
								break allPossibleHoles;
							}
							job.nowProcessing = nowProcessing;
							job.process(i, j);
						}
					if (DEBUG) {
						System.out.println("Completed ExpertEstimator.execute() for "+job+" in "+(System.nanoTime() - time)/1000+" μs");
						System.out.println(job.holeProbability.toString());
					}
					job.shutdown();
					job.nowProcessing = -1;
					playerModeller.finished();
					jobInProgress = null;
				}
				return true;
			});

			if (! trainOne.get())
				try {
					Thread.sleep (100);
				} catch (InterruptedException e) {
					modeller.stopTrainer(ExpertModeller.RETURN_IMMEDIATELY);
				}
				else Thread.yield();
		} while (modeller.isTraining() == ExpertModeller.ACTIVE);
	
		modeller.trainerStopped();
		if (DEBUG)
			System.out.println("Exiting working thread ("+modeller.isTraining()+")");
	}
	
}