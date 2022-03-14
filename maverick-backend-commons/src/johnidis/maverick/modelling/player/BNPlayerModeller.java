package johnidis.maverick.modelling.player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;

import norsys.netica.Caseset;
import norsys.netica.Environ;
import norsys.netica.Learner;
import norsys.netica.MaverickBayesianNetwork;
import norsys.netica.MaverickBnNode;
import norsys.netica.NeticaError;
import norsys.netica.NeticaException;
import norsys.netica.Node;
import norsys.netica.NodeList;
import norsys.netica.Streamer;
import norsys.neticaEx.aliases.Net;
import johnidis.maverick.GameCache;
import johnidis.maverick.Preferences;
import johnidis.maverick.modelling.adapters.BNGameAdapter;
import johnidis.maverick.modelling.adapters.BNGameAdapter.HandEstimations;
import johnidis.maverick.modelling.adapters.BNGameAdapter.IdealDataFinal;
import johnidis.maverick.modelling.adapters.BNGameAdapter.InputData;
import johnidis.maverick.modelling.adapters.GameAdapter;
import johnidis.maverick.modelling.data.BasicMLData;
import johnidis.maverick.modelling.data.BasicMLDataPair;
import johnidis.maverick.modelling.data.MLData;
import johnidis.maverick.modelling.data.MLDataPair;
import johnidis.maverick.modelling.modellers.Modeller;
import johnidis.maverick.modelling.models.BNModel;
import johnidis.maverick.modelling.models.Model;
import johnidis.utils.AbortException;

@SuppressWarnings("deprecation")
public class BNPlayerModeller extends PlayerModeller<GameAdapter,Character,MLData> {
	
	static private final boolean 			DEBUG 					= true;
	static private final boolean 			DEBUG_VERBOSE			= false;
	static private final boolean 			DEBUG_SKIP_TRAINING		= false;
	
	static private final int				CONCURRENT_LOADERS		= 4;
	static private final int 				MAX_CACHED_GAMES 		= 3;
	static private final double				MAX_TOLERANCE			= 1e-4;
	static private final double				FADING_FACTOR			= 0.05;
	static private final String 			NAME_ACTION 			= IdealDataFinal.ACTION.toString();
	static private final String 			NAME_HAND 				= IdealDataFinal.HAND.toString();
	static private final int				BUCKETS_HAND		 	= IdealDataFinal.HAND.allValues().length;
	
	static private Learner learner = null;
	static private final ThreadPoolExecutor persistorDelegate = new ThreadPoolExecutor (
			1, 1, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
	static private final ThreadPoolExecutor loaderDelegate = new ThreadPoolExecutor (
			0, CONCURRENT_LOADERS, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
	
	private final String[/*round*/] filename = new String[3];
	private final MaverickBayesianNetwork[/*round*/] net = new MaverickBayesianNetwork[3];
	private final NodeList[/*round*/] learningNodes = new NodeList[3];
//	private final HistogramPlayerModeller handObservations;
	
	private final FindingsCache cache = new FindingsCache();
	private final AtomicReferenceArray<Persistor> persistors = new AtomicReferenceArray<>(3);
	
	private class FindingsCache extends GameCache<List<BNGameAdapter>> {

		private static final long serialVersionUID = -886082968447498287L;

		public FindingsCache() {
			super(MAX_CACHED_GAMES);
		}

		@Override
		protected boolean removeEldestEntry(Entry<Long,List<BNGameAdapter>> eldest) {
			boolean evict = super.removeEldestEntry(eldest);
			if (evict) {
				if (DEBUG)
					debug("Flushing game "+eldest.getKey()+"...");
				List<BNGameAdapter> findings = eldest.getValue();
				synchronized (findings) {
					train (findings);
				}
			}
			return evict;
		}
		
	}
	
	private class Loader implements Runnable {
		
		private final int roundId;
		
		public Loader (int roundId) {
			this.roundId = roundId;
		}
		
		@Override
		public void run() {
			synchronized (filename[roundId]) {
				if (DEBUG)
					debug("Loading net for round "+(roundId+1)+"...");
				try {
					if (new File(filename[roundId]).exists()) {
						net[roundId] = new MaverickBayesianNetwork (new Streamer (filename[roundId]));
					} else {
						net[roundId] = new MaverickBayesianNetwork (new Streamer (Modeller.pathToModels + 
								(Preferences.MODEL_TRAIN.isOn() ? "/template_" : "/round_") 
								+ (roundId+1) + ".neta"));
					}
					net[roundId].setAutoUpdate(0);
					learningNodes[roundId] = BNGameAdapter.learningNodes(net[roundId]); 
					if (Preferences.MODEL_TRAIN.isOn())
						for (int n = 0; n < learningNodes[roundId].size(); n++)
							learningNodes[roundId].getNode(n).deleteTables();
					if (DEBUG)
						debug("Loading net for round "+(roundId+1)+" completed.");
				} catch (NeticaException e) {
					throw new RuntimeException (e);
				}
			}
		}

	}
	
	private class Persistor implements Runnable {
		
		private final int roundId;
		
		public Persistor (int roundId) {
			this.roundId = roundId;
		}
		
		@Override
		public void run() {
			if (DEBUG)
				debug("Persisting net for round "+(roundId+1)+"...");
			try {
				synchronized (filename[roundId]) {
					net[roundId].compile();
					Streamer streamer = new Streamer(filename[roundId]);
					net[roundId].write(streamer);
					streamer.finalize(); 
				}
				checkForWarnings();
				if (DEBUG)
					debug("Persisting net for round "+(roundId+1)+" completed.");
			} catch (NeticaException e) {
				System.err.println("Error in BNPlayerModeller ["+canonicalName+","+(roundId+1)+"]: "+e);
				System.err.println("Deleting "+filename[roundId]);
				new File(filename[roundId]).delete();
			}
			persistors.set(roundId, null);
		}
		
	}
	
	static {
		try {
			new Environ (null);
			Net.setConstructorClass(MaverickBayesianNetwork.class.getName());
			Node.setConstructorClass (MaverickBnNode.class.getName());
			learner = new Learner (Learner.EM_LEARNING);
			learner.setMaxTolerance (MAX_TOLERANCE);
		} catch (NeticaException e) {
			learner = null;
			e.printStackTrace();
		}
	}
	

	public BNPlayerModeller(Class<? extends Model> m, String key) {
		super(m, key);
//		handObservations = new HistogramPlayerModeller(HistogramBNHandModel.class, key);
		for (int r = BNGameAdapter.ROUND_FLOP; r <= BNGameAdapter.ROUND_RIVER; r++) {
			filename[r-1] = pathname + "/round_" + r + ".neta";
			loaderDelegate.execute(new Loader(r-1));
		}
	}


	private void debug (String str) {
		System.out.println("BNPlayerModeller ["+canonicalName+"]: "+str);
	}
	
	@SuppressWarnings("unchecked")
	private void checkForWarnings() throws NeticaException {
		Vector<NeticaError> warnings = NeticaError.getWarnings(NeticaError.NOTICE_ERR, Environ.getDefaultEnviron());
		for (NeticaError warning : warnings)
			System.err.println("BNPlayerModeller: "+warning);
	}
	
	private void waitForNet(int roundId) throws InterruptedException {
		if (DEBUG)
			debug("Pending until round "+(roundId+1)+" is loaded...");
		while (net[roundId] == null)
			Thread.sleep(100);
	}
	
/*	private HistogramBNHandModel histogramModel (int round, MLData estimation) {
		int estHand = HistogramModel.indexMostProbable (estimation);
		return HistogramBNHandModel.values()
				[(round >= BNGameAdapter.ROUND_RIVER) ? 
				estHand : BNGameAdapter.MAP_HAND_INTERIM_TO_FINAL[estHand]]; 
	}*/

	/** Needs external synchronization on adapter */
	private HandEstimations addToHandEstimations (BNGameAdapter adapter, int r, MLData estimation) {
		HandEstimations estimations = adapter.handEstimations[adapter.seatToAct];
		if (estimations == null) {
			estimations = new HandEstimations ();
			adapter.handEstimations[adapter.seatToAct] = estimations;
		}
		if (estimation != null)
			estimations.round[r].add(estimation);
		return estimations;
	}
	
	@Override
	public void persist () {
		mkdirs();
		if (DEBUG)
			debug("Ready to persist "+toString());
//		handObservations.persist();
		for (int r = BNGameAdapter.ROUND_FLOP; r <= BNGameAdapter.ROUND_RIVER; r++) {
			Persistor persistor = new Persistor(r-1);
			if (! persistors.compareAndSet(r-1, null, persistor)) {
				if (DEBUG)
					debug("Persist aborted: already scheduled round "+r);
				continue;
			}
			persistorDelegate.execute(persistor);
		}
	}
	
	public MLDataPair doInference (int roundId, char action, GameAdapter adapter, boolean considerHole) {	// TODO implement cache
		MLDataPair result = null;
		if (net[roundId] == null)
			try {
				waitForNet(roundId);
			} catch (InterruptedException e1) {
				return null;
			}
		synchronized (filename[roundId]) {
			try {
				double[] in = new double[InputData.FIELDS];
				double[] out = new double[IdealDataFinal.FIELDS];
				adapter.collectInputs(action, in);
				adapter.collectIdeals(action, out);
				for (int n = 0; n < InputData.FIELDS + IdealDataFinal.FIELDS; n++) {
					if ((! considerHole) && 
							((n == InputData.FIELDS + IdealDataFinal.HAND.index()) || 
							(n == InputData.FIELDS + IdealDataFinal.HOLE.index())))
						continue;
					int state = (n < InputData.FIELDS) ?
							adapter.inputAsSymbol(in, n).intValue() :
							adapter.idealAsSymbol(out, n - InputData.FIELDS).intValue();
					if (state >= 0) {
						Node node = net[roundId].getNode(adapter.field(n));
						if (DEBUG) {
							String statename = node.state(state).getName().toUpperCase();
							String ourname = ((n < InputData.FIELDS) ?
									adapter.inputAsSymbol(in, n).toString() :
									adapter.idealAsSymbol(out, n-InputData.FIELDS).toString()).toUpperCase();
							if (! statename.endsWith(ourname.substring(0, Math.min(30, ourname.length()))))
								throw new RuntimeException ("BNPlayerModeller.doInference(): BN states found in wrong sequence");
						}
						node.finding().enterState(state);
					}
				}
				net[roundId].compile();
				result = new BasicMLDataPair(new BasicMLData(net[roundId].getNode(NAME_HAND).getBeliefs()),
						new BasicMLData(net[roundId].getNode(NAME_ACTION).getBeliefs()));
			} catch (NeticaException e) {
				throw new RuntimeException (e);
			} catch (AbortException e) {
				System.err.println(e);
				System.err.println(adapter);
			} finally {
				try {
					net[roundId].retractFindings();
				} catch (NeticaException e) {
					throw new RuntimeException (e);
				}
			}
		}
		return result;
	}
	
	public void train (List<BNGameAdapter> findings) {
		synchronized (findings) {
			if (DEBUG)
				debug("Training for a total of "+findings.size()+" findings...");
			for (int round = BNGameAdapter.ROUND_FLOP; round <= BNGameAdapter.ROUND_RIVER; round++) {
				List<double[]> input = new ArrayList<>();
				List<double[]> ideal = new ArrayList<>();
//				boolean roundHistogram = false;
				for (int f = findings.size() - 1; f >= 0; f--) {
					BNGameAdapter finding = findings.get(f);
					if ((finding.roundIndex == round) || 
							((round == BNGameAdapter.ROUND_RIVER) && (finding.roundIndex == BNGameAdapter.ROUND_SHOWDOWN))) {
						double[] in = new double[InputData.FIELDS];
						double[] out = new double[IdealDataFinal.FIELDS];
						try {
							finding.collectInputs(finding.lastAction, in);
							finding.collectIdeals(finding.lastAction, out);
							input.add(in);
							ideal.add(out);
/*							if ((out[IdealDataFinal.HAND.index()] >= 0) && (! roundHistogram)) {
								MLDataPair prediction = doInference (round - 1, BNGameAdapter.ACTION_NA, finding, false);
								if (prediction != null) {
									HistogramBNHandModel model = histogramModel (round, prediction.getFirst());
									handObservations.addData(model,	round - 1, prediction.getFirst(), 
											model.histogramPoints(new BasicMLData(out)));
									roundHistogram = true;
								}
							}*/
						} catch (AbortException e) {
							System.err.println(e);
							System.err.println(finding);
						}
					}
				}
				if (DEBUG_SKIP_TRAINING)
					continue;
				if (input.size() > 0) {
					synchronized (filename[round-1]) {
						if (net[round-1] == null)
							try {
								waitForNet(round-1);
							} catch (InterruptedException e1) {
								continue;
							}
						try {
							if (! Preferences.MODEL_TRAIN.isOn())
								for (Object node : learningNodes[round-1])
									((Node) node).fadeCPTable(FADING_FACTOR);
							Caseset cases = BNGameAdapter.asCaseset(input, ideal);
							learner.learnCPTs(learningNodes[round-1], cases, 1);
							cases.finalize();
							checkForWarnings();
						} catch (NeticaException e) {
							System.err.println(e);
						}
					}
				}
			}
			findings.clear();
		}
	}

	@Override
	public void addData(Model<?,MLData> model, int roundId, GameAdapter adapter, Character action) {
		if (roundId < 0)
			return;
		Long key = adapter.id;
		List<BNGameAdapter> findings;
		synchronized (cache) {
			findings = cache.get(key);
			if (findings == null) {
				findings = new ArrayList<>();
				cache.put(key, findings);
				if (DEBUG)
					debug("New game "+adapter.id+"...");
			}
		}
		if (model == BNModel.Action) {
			BNGameAdapter copy = new BNGameAdapter(adapter, null);
			copy.lastAction = action;
			synchronized (findings) {
				findings.add(copy);
			}
			MLDataPair result = doInference (roundId, action, adapter, true);
			if (result != null)
				synchronized (adapter) {
					addToHandEstimations ((BNGameAdapter) adapter, roundId, result.getFirst());
				}
		} else if (model == BNModel.Hand) {
			synchronized (findings) {
				for (BNGameAdapter finding : findings)
					finding.hole = adapter.hole;
			}
		} else
			throw new RuntimeException ("BNPlayerModeller.addData() called with a non-BNModel Model");
	}

	@Override
	public long getSamples(Model<?,MLData> model, int contextId) {
		throw new RuntimeException ("BNPlayerModeller.getSamples() not supported");
	}
	
	@Override
	public MLData estimate(Model<?,MLData> model, int roundId, GameAdapter adapter) {
		if (roundId < 0)
			return null;
		if (model == BNModel.Action) {
			MLDataPair result = doInference (roundId, GameAdapter.ACTION_NA, adapter, true);
			if (result == null)
				return null;
			synchronized (adapter) {
				addToHandEstimations ((BNGameAdapter) adapter, roundId, result.getFirst());
			}
			return result.getSecond();
		} else if (model == BNModel.Hand) {
			synchronized (adapter) {
				HandEstimations estimations = ((BNGameAdapter) adapter).handEstimations[adapter.seatToAct];
				if (estimations == null) {
					MLDataPair result = doInference (roundId, GameAdapter.ACTION_NA, adapter, true);
					if (result == null)
						return null;
					if (DEBUG_VERBOSE)
						debug("On-demand estimation for hand: "+result.getFirst());
					estimations = addToHandEstimations (((BNGameAdapter) adapter), adapter.seatToAct, result.getFirst());
				}
				MLData estimation = new BasicMLData(BUCKETS_HAND);
				for (int p = 0; p < estimation.size(); p++)
					estimation.setData(p, 1);
				int size = estimations.round[roundId].size();
				if (size > 0) {
					MLData oneEstimation = estimations.round[roundId].get(size - 1);
					MLData histogram = null;/*handObservations.estimate (histogramModel (r, oneEstimation), r-1, oneEstimation)*/;
					if (histogram == null) {
						if (roundId == GameAdapter.ROUND_RIVER - 1)
							histogram = oneEstimation;
						else {
							histogram = new BasicMLData(BUCKETS_HAND);
							for (int p = 0; p < oneEstimation.size(); p++)
								histogram.add(BNGameAdapter.MAP_HAND_INTERIM_TO_FINAL[p], oneEstimation.getData(p));
						}
					}
					if (DEBUG_VERBOSE)
						debug(" -> "+histogram);
					for (int p = 0; p < histogram.size(); p++)
						estimation.setData(p, estimation.getData(p) * histogram.getData(p));
				}
				Modeller.normalize(estimation, -1);
				if (DEBUG_VERBOSE)
					debug("Context "+roundId+": Hand estimation "+estimation+" out of "+ estimations.round[roundId].size()+
							" samples");
				return estimation;
			}
		} else
			throw new RuntimeException ("BNPlayerModeller.estimate() called with a non-BNModel Model");
	}

	@Override
	public double getError(Model<?,MLData> model, int contextId) {
		return MAX_TOLERANCE;
	}

	@Override
	public void setModelling(boolean active) {
		super.setModelling(active);
		if (active == false) {
			boolean success;
			try {
				persistorDelegate.shutdown();
				success = persistorDelegate.awaitTermination(30, TimeUnit.SECONDS);	// FIXME
			} catch (InterruptedException e) {
				success = false;
			}
			if (! success)
				System.err.println("BNPlayerModeller.setModelling(false) interrupted while waiting to persist pending models");
		}
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder(canonicalName).append(": Overall ");
		str.append(persistorDelegate.getQueue().size()).append(" pending persist jobs {");
		str.append(loaderDelegate.getQueue().size()).append(" pending load jobs, ");
		synchronized (cache) {
			str.append(cache.size()).append(" pending games");
			for (Long game : cache.keySet())
				str.append(" [").append(game).append(" -> ").append(cache.get(game).size()).append(" findings]");
		}
		str.append(" }");
		return str.toString();
	}

}
