package johnidis.maverick.modelling.player;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.biotools.meerkat.Action;
import com.biotools.meerkat.Card;
import com.biotools.meerkat.Hand;

import poker.MaverickGameInfo;
import poker.MaverickPlayerInfo;
import johnidis.maverick.GameCache;
import johnidis.maverick.Holdem;
import johnidis.maverick.modelling.DecisionBot;
import johnidis.maverick.modelling.HoleProbability;
import johnidis.maverick.modelling.adapters.GameAdapter;
import johnidis.maverick.modelling.data.BasicMLData;
import johnidis.maverick.modelling.data.MLData;
import johnidis.maverick.modelling.modellers.Modeller;
import johnidis.maverick.modelling.models.ExpertModel;
import johnidis.maverick.modelling.models.HistogramExpertModel;
import johnidis.maverick.modelling.models.HistogramModel;
import johnidis.maverick.modelling.models.Model;

public class ExpertPlayerModeller extends PlayerModeller<Character,DecisionBot,MLData> {
	
	private static final boolean											DEBUG						= false;
	private static final boolean											DEBUG_PRINT_HISTOGRAM		= true;
	
	public static final int 												TOTAL_PROCESSING_STEPS 		= (52*51)/2;

	private static final int 												MAX_CACHED_GAMES 			= 10;
	private static final int 												MAX_RANKING 				= 168;
	private static final int												MIN_POSTFLOP_SAMPLES		= 10;
	private static final double 											ALMOST_ZERO 				= 0.01;
	private static final double[/*round*/][/*commit*/][/*est*/][/*act*/] 	DEFAULT_ACTION_PROBABILITIES = new double[][][][] {
/*		{{{0.800, 0.200, 0.000},   {0.000, 0.414, 0.586},   {0.000, 0.313, 0.688}},
		{{0.800, 0.200, 0.000},   {0.000, 0.414, 0.586},   {0.000, 0.313, 0.688}}},
		{{{0.750, 0.000, 0.250},   {0.000, 0.357, 0.643},   {0.000, 0.333, 0.667}},
		{{0.750, 0.000, 0.250},   {0.000, 0.357, 0.643},   {0.000, 0.333, 0.667}}},
		{{{1.000, 0.000, 0.000},   {0.000, 0.625, 0.375},   {0.000, 0.250, 0.750}},
		{{1.000, 0.000, 0.000},   {0.000, 0.625, 0.375},   {0.000, 0.250, 0.750}}}*/
		{{{0.862, 0.000, 0.138},   {0.000, 0.881, 0.119},   {0.030, 0.333, 0.636}},
		{{0.95, 0.05, 0},   {0.000, 1.000, 0.000},   {0, 0.1, 0.8}}},
		{{{0.857, 0.000, 0.143},   {0.000, 0.791, 0.209},   {0.103, 0.179, 0.718}},
		{{1.000, 0.000, 0.000},   {0.000, 0.833, 0.167},   {0, 0.05, 0.95}}},
		{{{0.857, 0.000, 0.143},   {0.000, 0.896, 0.104},   {0.000, 0.333, 0.667}},
		{{1.000, 0.000, 0.000},   {0.000, 0.833, 0.167},   {0, 0.05, 0.95}}}
	};
	
	private final HistogramPlayerModeller postflopObservations;
	private final AtomicReference<PreflopObservations> preflopObservations;			// TODO: multithreading
	
	protected BlockingQueue<Job> jobsQueue = new LinkedBlockingQueue<>(30);

	private GameCache<HoleProbability> holeProbabilities = new GameCache<>(MAX_CACHED_GAMES);
	private ThreadLocal<List<DecisionBot>> estimations = new ThreadLocal<>();
	private ThreadLocal<AtomicLong> lastGame = new ThreadLocal<>();
	private final File preflopFile;
	
	private static class PreflopObservations implements Serializable {

		private static final long serialVersionUID = -8722089721486324176L;
		
		private static final double 	DEFAULT_VPIP				= 0.389;
		private static final double 	DEFAULT_PFR					= 0.185;
		private static final int		MIN_PREFLOP_SAMPLES			= 10;

		private int games = 0;
		private int vpip = 0;
		private int pfr = 0;
		transient private boolean vpiped;
		transient private int pfred;
		
		public void newGame() {
			games++;
			vpiped = false;
			pfred = 0;
		}
		
		public boolean setVpip() {
			if (vpiped)
				return false;
			vpip++;
			vpiped = true;
			return vpiped;
		}
		
		public int setPfr() {
			setVpip();
			pfred++;
			if (pfred == 1)
				pfr++;
			return pfred;
		}
		
		public int games() {
			return games;
		}
		
		public double getVpip() {
			if (games >= MIN_PREFLOP_SAMPLES)
				return ((double) vpip) / games;
			else
				return DEFAULT_VPIP;
		}
		
		public double getPfr() {
			if (games >= MIN_PREFLOP_SAMPLES)
				return ((double) pfr) / games;
			else
				return DEFAULT_PFR;
		}
		
	}
	
	public class Job {
		
		public final int action;
		public final HoleProbability holeProbability;
		public volatile int nowProcessing = -1;
		public volatile boolean abort = false;
		private DecisionBot bot;
		
		public Job (MaverickGameInfo aGame, char aChar) {
			synchronized (ExpertPlayerModeller.this) {
				holeProbability = new HoleProbability(holeProbabilities.get(aGame.getGameID()), false);
			}
			bot = new DecisionBot (aGame, false);
			action = GameAdapter.getActionIndex(aChar);
		}
		
		public void initialize () {
		}
		
		public boolean process (int i, int j) {
			if ((holeProbability.cardExcluded(i)) || (holeProbability.cardExcluded(j)) || 
					(holeProbability.get(i, j) < ALMOST_ZERO))
				holeProbability.set(i, j, 0);
			if (holeProbability.get(i, j) == 0) {
				return false;
			} else {
				Action botAction = bot.getAction(bot.getGameInfo().getCurrentPlayerSeat(), Card.get(i), Card.get(j));
				if (DEBUG)
					System.out.print(botAction+" <= "+bot.toString()+" ("+action+"): "+holeProbability.get(i, j));
				holeProbability.adjust(i, j, estimate (
						ExpertModel.Action, ExpertModel.Action.contextId(bot), GameAdapter.getAction(botAction)).getData(action));
				if (DEBUG)
					System.out.println(" -> "+holeProbability.get(i, j));
			}
			return true;
		}
		
		public boolean isRelevant(String player, MaverickGameInfo game) {
			return ((canonicalName.equals(player)) && (bot.getGameInfo() == game));
		}
		
		public void shutdown () {
			bot.shutdown();
		}

		@Override
		public String toString() {
			return "[" + bot.getGameInfo() + "], [" + canonicalName + "]: action=" + action + ", " +
					(double) (nowProcessing*100/TOTAL_PROCESSING_STEPS) + "%" + (abort ? " ...aborting" : "");
		}
		
	}


	public ExpertPlayerModeller(String key) {
		super(null, key);
		lastGame.set(new AtomicLong(-1));
		postflopObservations = new HistogramPlayerModeller (HistogramExpertModel.class, key);
		preflopObservations = new AtomicReference<>();
		preflopFile = new File(pathname, "preflop.raw");
		if (preflopFile.exists()) {
			if (DEBUG)
				System.out.println("Opening data file "+preflopFile);
			try {
				ObjectInputStream ois = new ObjectInputStream (new BufferedInputStream (new FileInputStream(preflopFile)));
				PreflopObservations data = (PreflopObservations) ois.readObject();
				ois.close();
				preflopObservations.set(data);
			} catch (Exception e) {
				preflopObservations.set(new PreflopObservations());
				e.printStackTrace();
			}
		} else {
			preflopObservations.set(new PreflopObservations());
			if (DEBUG)
				System.out.println("Data file "+preflopFile+" not found, creating from scratch");
		}
	}
	

	private void emptyQueue (MaverickGameInfo game) {
		Job job = null;
		do {
			job = jobsQueue.poll();
		} while (job != null);
		job = ExpertEstimator.jobInProgress;
		if ((job != null) && (job.isRelevant(canonicalName, game))) {
			job.abort = true;
			do {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					;
				}
			} while (ExpertEstimator.jobInProgress == job);
		}
	}

	void finished() {
		synchronized (this) {
			if (ExpertEstimator.jobInProgress.abort)
				return;
			holeProbabilities.put(ExpertEstimator.jobInProgress.bot.getGameInfo().getGameID(), 
					ExpertEstimator.jobInProgress.holeProbability);
		}
	}
	
	@Override
	public void persist() {
		mkdirs();
		postflopObservations.persist();
		try {
			ObjectOutputStream oos = new ObjectOutputStream (new BufferedOutputStream (new FileOutputStream(preflopFile)));
			oos.writeObject(preflopObservations.get());
			oos.close();
		} catch (Exception e) {
			System.err.println("Error while writing file: "+ preflopFile);
			e.printStackTrace();
		}
		if (DEBUG_PRINT_HISTOGRAM)
			System.out.println(toString());
	}
	
	private void checkForNewGame (MaverickGameInfo input) {
		synchronized (this) {
			long last = lastGame.get().get();
			if (last != input.getGameID()) {
				emptyQueue(input);
				HoleProbability holeprob = new HoleProbability();
				holeProbabilities.put(input.getGameID(), holeprob);
				for (int p = 0; p < input.getNumPlayers(); p++) {
					MaverickPlayerInfo player = input.getPlayer(p);
					Hand hole = player.getRevealedHand();
					if (hole != null)
						holeprob.scratch(hole);
				}
				lastGame.get().set(input.getGameID());
				preflopObservations.get().newGame();
				List<DecisionBot> bots = estimations.get();
				if ((bots != null) && (bots.size() > 0))
					addFoldedActionData(bots.get(bots.size() - 1).getGameInfo());
				estimations.set(new ArrayList<>());
			}
		}
	}
	
	private void addActionData (String actuals, Card c1, Card c2) {
		int round = Holdem.PREFLOP;
		boolean committed = false;
		for (int a = 0; a < actuals.length(); a++) {
			char actual = actuals.charAt(a);
			switch (actual) {
			case '/':
				round++;
				committed = false;
				break;
			case 'b':
			case 'r':
			case 'c':
			case 'f':
			case 'k':
				if (round > 0) {
					DecisionBot bot = estimations.get().remove(0);
					if (bot.getGameInfo().getStage() != round)
						throw new RuntimeException ("ExpertPlayerModeller.addActionData(): lost synchro of past data");
					Action ea = bot.getAction(bot.getGameInfo().getCurrentPlayerSeat(), c1, c2);
					MLData estimation = new BasicMLData(new double[] {ea.getActionIndex()});
					HistogramModel histogramModel = ExpertModel.Hand.histogramModel(estimation);
					int context = round - 1 + (committed ? HistogramExpertModel.MASK_COMMITTED : 0);
					postflopObservations.addData(histogramModel, context, estimation, 
							histogramModel.histogramPoints(new BasicMLData(new double[] {GameAdapter.getActionIndex(actual)})));
					if (DEBUG_PRINT_HISTOGRAM)
						System.out.println("("+ea.toString()+") "+toString());
					bot.shutdown();
				}
				if ((actual == 'b') || (actual == 'r') || (actual == 'c'))
					committed = true;
				break;
			}
		}
		estimations.set(null);
	}
	
	private void addFoldedActionData (MaverickGameInfo game) {
		String actuals = game.getPlayer(game.getCurrentPlayerSeat()).getActions();
		if (actuals.charAt(actuals.length() - 1) != 'f')
			actuals += 'f';
		Card[] hole = randomHole (game);
		addActionData(actuals, hole[0], hole[1]);
	}

	@Override
	public void addData(Model<?,MLData> model, int contextId, Character action, DecisionBot bot) {
		MaverickGameInfo game = bot.getGameInfo();
		checkForNewGame(game);
		if (contextId < 0)
			return;
		if (model == ExpertModel.Action) {
			if (game.getCurrentPlayerSeat() == bot.getHero()) {
				Action ea = bot.getAction();
				MLData estimation = new BasicMLData(new double[] {ea.getActionIndex()});
				MLData actual = new BasicMLData(new double[] {GameAdapter.getActionIndex(action)});
				HistogramExpertModel histogramModel = (HistogramExpertModel) model.histogramModel(estimation); 
				postflopObservations.addData(histogramModel, histogramModel.contextId(bot), estimation, histogramModel.histogramPoints(actual));
				if (DEBUG_PRINT_HISTOGRAM)
					System.out.println("("+ea.toString()+") "+toString());
			} else {
				estimations.get().add(new DecisionBot(game, false));
				if (action == 'f')
					addFoldedActionData(game);
			}
		} else {
			if (game.getCurrentPlayerSeat() == bot.getHero())
				return;
			MaverickPlayerInfo player = game.getPlayer(game.getCurrentPlayerSeat());
			addActionData(player.getActions(), 
					player.getRevealedHand().getFirstCard(), player.getRevealedHand().getSecondCard());
		}
	}

	public void adjustHoleProbability (MaverickGameInfo input, char ideal) {
		if (ideal == 'f') {
			emptyQueue(input);
			return;
		}
		checkForNewGame(input);
		if (ideal == 'B')
			return;
		
		if (! isModelling())
			return;
		synchronized (this) {
			if (input.getStage() == Holdem.PREFLOP) {
				switch (ideal) {
				case 'c':
					if (preflopObservations.get().setVpip())
						holeProbabilities.get(input.getGameID()).preflopAdjust (
								(int) (pfr() * MAX_RANKING), (int) (vpip() * MAX_RANKING), ALMOST_ZERO);
					break;
				case 'r':
				case 'b':
					int pfrs = preflopObservations.get().setPfr();
					holeProbabilities.get(input.getGameID()).preflopAdjust (
							0, (int) (Math.pow(pfr(), pfrs) * MAX_RANKING), ALMOST_ZERO);
					break;
				}

			} else {
				Job job = new Job(input, ideal);
				jobsQueue.add(job);
			}
		}
	}

	@Override
	public long getSamples(Model dummy, int contextId) {
		if (contextId == -1)
			return preflopObservations.get().games();
		else
			return postflopObservations.getSamples(HistogramExpertModel.ExpertFold, contextId) +
					postflopObservations.getSamples(HistogramExpertModel.ExpertCall, contextId) +
					postflopObservations.getSamples(HistogramExpertModel.ExpertRaise, contextId);
	}

	public double pfr() {
		return preflopObservations.get().getPfr();
	}

	public double vpip() {
		return preflopObservations.get().getVpip();
	}

	@Override
	public MLData estimate(Model<?,MLData> model, int contextId, Character action) {
		if (model == ExpertModel.Action) {
			int actionId = GameAdapter.getActionIndex(action);
			MLData estimation = new BasicMLData(new double[] {actionId});
			HistogramExpertModel histogramModel = (HistogramExpertModel) model.histogramModel(estimation);
			estimation = postflopObservations.estimate(histogramModel, contextId, histogramModel.histogramPoints(estimation));
			long samples = postflopObservations.getSamples(histogramModel, contextId); 
			if ((samples >= MIN_POSTFLOP_SAMPLES) && (estimation != null)) {
				Modeller.normalize(estimation, -1);
				return estimation;
			} else {
				if (DEBUG)
					System.out.println("Too few samples for "+histogramModel+"("+histogramModel.getRound(contextId)+"): "+samples);
				return new BasicMLData(DEFAULT_ACTION_PROBABILITIES[histogramModel.getRound(contextId)]
						[histogramModel.getSlice(contextId, estimation)][actionId]);
			}
		} else
			return null;
	}

	public HoleProbability getHoleProbability(MaverickGameInfo input) {
		return new HoleProbability(holeProbabilities.get(input.getGameID()), true);
	}
	
	public Card[] randomHole (MaverickGameInfo game) {
		synchronized (this) {
			double random = Math.random();
			double cdf = 0;
			HoleProbability probs = getHoleProbability(game);
			if (probs == null)
				throw new NoSuchElementException("Game "+game+" not found");
			if (probs.sumEntries == 0)
				throw new RuntimeException("Empty probability matrix");
			for (int i = 0; i < 51; i++)
				for (int j = i + 1; j < 52; j++) {
					cdf += probs.get(i, j);
					if (random <= cdf) {
						if ((probs.cardExcluded(i)) || (probs.cardExcluded(j)))
							throw new RuntimeException("Incosistent probability matrix");
						return new Card[] {Card.get(i), Card.get(j)};
					}
				}
			return null;
		}
	}

	@Override
	public double getError(Model dummymodel, int dummycontext) {
		Job job = ExpertEstimator.jobInProgress;
		return jobsQueue.size() + ((job != null) ? 1D - (double) job.nowProcessing / TOTAL_PROCESSING_STEPS : 0);
	}

	public void scratch (MaverickGameInfo game, Card... cards) {
		synchronized (this) {
			for (Card card : cards)
				holeProbabilities.get(game.getGameID()).scratch(card);
		}
	}
	
	public String histogramToString() {
		StringBuilder str = new StringBuilder(postflopObservations.toString()).append('\n');
		for (int r = 0; r < 3; r++) {
			str.append('{');
			for (int c = 0; c <= HistogramExpertModel.MASK_COMMITTED; c += HistogramExpertModel.MASK_COMMITTED) {
				str.append('{');
				for (int ea = 0; ea < 3; ea++) {
					MLData estimation = new BasicMLData(new double[] {ea});
					HistogramModel<DecisionBot> histogramModel = ExpertModel.Action.histogramModel(estimation);
					MLData histogram = postflopObservations.estimate(histogramModel, c+r, histogramModel.histogramPoints(estimation));
					if (histogram != null) {
						Modeller.normalize(histogram, -1);
						str.append(String.format(Locale.US, 
								"{%5.3f, %5.3f, %5.3f}", histogram.getData(0), histogram.getData(1), histogram.getData(2)));
					} else
						str.append("{}");
					if (ea < 2)
						str.append(",   ");
				}
				str.append("}");
				if (c == 0)
					str.append(",\n");
			}
			str.append("}");
			if (r < 2)
				str.append(",\n");
		}
		return str.toString();
	}

	public String toString(long game) {
		StringBuilder str = new StringBuilder();
		synchronized (this) {
			str.append("Game ").append(game).append('\n').append(holeProbabilities.get(game)).append('\n');
			return str.toString();
		}
	}
	
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder(canonicalName).append(": holeProbabilities for ");
		synchronized (this) {
			str.append(holeProbabilities.size()).append(" games");
		}
		str.append('\n').append(String.format(Locale.US, 
				"Preflop statistics for %d games: VPIP=%5.3f, PFR=%5.3f", 
				getSamples(null, -1), preflopObservations.get().getVpip(), preflopObservations.get().getPfr()));
		str.append('\n').append(histogramToString());
		return str.toString();
	}

}
