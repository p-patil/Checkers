import java.util.Random;
import java.lang.Math;

/**
 * Checkers player whose static evaluation function consists of a co-evolved neural network based on genetic algorithms.
 */
public class EvolvedCheckersAI extends CheckersAI {
	public static final int POPULATION_SIZE = 30;
	public static final int MAX_HIDDEN_LAYERS = 2;
	public static final int MAX_NEURONS = 20;
	public static final int INPUT_LAYER_SIZE = Checkers.BOARD_SIZE * Checkers.BOARD_SIZE + 1;
	public static final int OUTPUT_LAYER_SIZE = 1;
	public static final double CROSSOVER_PROBABILITY = 0.7;
	public static final double MUTATION_PROBABILITY = 0.01;


	private NeuralNetwork network;

	/**
	 * Basic constructor.
	 */
	public EvolvedCheckersAI() {
		super();
		Random r = new Random();
		int[] layerCounts = new int[r.nextInt(MAX_HIDDEN_LAYERS) + 2];

		layerCounts[0] = INPUT_LAYER_SIZE;
		layerCounts[layerCounts.length - 1] = OUTPUT_LAYER_SIZE;

		for (int i = 1; i < layerCounts.length - 1; i++) {
			layerCounts[i] = r.nextInt(MAX_NEURONS) + 1;
		}

		this.network = new NeuralNetwork(layerCounts);
	}

	/**
	 * Constructor with a given tablebase, uses a random neural network for evaluation function.
	 * @param tablebase The tablebase.
	 */
	public EvolvedCheckersAI(EndgameTablebase tablebase) {
		super(tablebase);
		Random r = new Random();
		int[] layerCounts = new int[r.nextInt(MAX_HIDDEN_LAYERS)];
		for (int i = 0; i < layerCounts.length; i++) {
			layerCounts[i] = r.nextInt(MAX_NEURONS) + 1;
		}

		this.network = new NeuralNetwork(layerCounts);
	}

	/**
	 * Constructor with a given neural network.
	 * @param network The neural network to use.
	 */
	public EvolvedCheckersAI(NeuralNetwork network) {
		super();
		this.network = network;
	}

	@Override
	/**
	 * The static position evaluation function used.
	 * @param p The position to evaluate.
	 * @param turn Whose turn to evaluate on.
	 * @return The evaluation value.
	 */
	protected int evaluationFunction(Position p, int turn) {
		double[] inputVector = new double[p.board.length * p.board.length + 1];
		for (int i = 0; i < p.board.length; i++) {
			for (int j = 0; j < p.board[i].length; j++) {
				inputVector[i * p.board[i].length + j] = p.board[i][j].getState();
			}
		}

		inputVector[inputVector.length - 1] = turn;

		return (int) this.network.feedforward(inputVector)[0];
	}

	/**
	 * Co-evolves a population of neural networks, using genetic algorithms, for numGenerations generations. 
	 * @param numGenerations The number of generations to evolve the neural networks for.
	 */
	public static NeuralNetwork evolve(int numGenerations) {
		NeuralNetwork[] population = null; // Stores the population for each generation.
		double[] fitnessScores = null; // Stores corresponding fitness scores for population.

		// Keep initializing a new, random population until at least one pair of networks doesn't draw.
		boolean allDraws = true; // True as long as the entire population consists of neural networks that draw.
		while (allDraws) {
			population = initializePopulation();
			fitnessScores = getFitnessScores(population, 5);
			for (double d : fitnessScores) {
				if (d != 0) {
					allDraws = false;
					break;
				}
			}
		}

		// Evolve the population.
		for (int generation = 0; generation < numGenerations; generation++) {
			population = nextPopulation(population, fitnessScores);
			fitnessScores = getFitnessScores(population);
		}

		// Evolution has concluded. Return the best neural network found so far.
		int maxIndex = 0;
		double maxFitness = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < fitnessScores.length; i++) {
			if (fitnessScores[i] > maxFitness) {
				maxIndex = i;
				maxFitness = fitnessScores[i];
			}
		}

		return population[maxIndex];
	}

	// Helper functions below this line.

	/**
	 * Returns a population of randomly initialized neural networks.
	 * @return Returns an array (of length POPULATION_SIZE) of neural networks, with a random number of hidden layers between
	 * 		   0 and MAX_HIDDEN_LAYERS, with each layer having a random number of neurons between 1 and MAX_NEURONS.
	 */
	public static NeuralNetwork[] initializePopulation() {
		NeuralNetwork[] population = new NeuralNetwork[POPULATION_SIZE];
		int[] layerCounts;
		Random r = new Random();

		// Initialize random neural networks.
		for (int i = 0; i < population.length; i++) {
			// Random number of hidden layers; add 2 to account for input and output layers.
			layerCounts = new int[r.nextInt(MAX_HIDDEN_LAYERS + 1) + 2];
			
			layerCounts[0] = INPUT_LAYER_SIZE;			
			for (int j = 1; j < layerCounts.length - 1; j++) {
				layerCounts[j] = r.nextInt(MAX_NEURONS) + 1; // Random number of neurons.
			}
			layerCounts[layerCounts.length - 1] = OUTPUT_LAYER_SIZE;

			population[i] = new NeuralNetwork(layerCounts);
		}

		return population;
	}

	/**
	 * Returns the fitness values for the neural networks in the given population, based on playing every neural network against every other.
	 * @param population The array of neural networks to rank by fitness.
	 * @return Returns an array with corresponding fitness scores.
	 */
	private static double[] getFitnessScores(NeuralNetwork[] population) {
		double[] fitnessScores = new double[population.length], outcomeVector;

		for (int i = 0; i < population.length; i++) {
			for (int j = i + 1; j < population.length; j++) {
				outcomeVector = play(population[i], population[j]); // Get the result of the game between the two neural nets.

				// Add points for winning, deduct for losses.
				fitnessScores[i] += outcomeVector[0];
				fitnessScores[j] += outcomeVector[1];
			}
		}

		return fitnessScores;
	}

	/**
	 * Returns the fitness values for the neural networks in the given population, based on playing every neural network against
	 * k randomly chosen neural networks and assigning 1 point per win and deducting 2 points per loss.
	 * @param population The set of neural networks for whom to compute fitness scores.
	 * @param k The number of opponents to randomly play against.
	 * @return Returns the corresponding fitness scores.
	 */
	private static double[] getFitnessScores(NeuralNetwork[] population, int k) {
		double[] fitnessScores = new double[population.length], outcomeVector;

		Random r = new Random();
		int opponent;
		for (int i = 0; i < population.length; i++) {
			// Play against k randomly selected opponents.
			for (int j = 0; j < k; j++) {
				do { // Randomly choose an opponent, ensuring that the network isn't playing itself.
					opponent = r.nextInt(population.length);
				} while (opponent == i);
				outcomeVector = play(population[i], population[opponent]); // Get the result of the game between the two neural nets.

				// Add points for winning, deduct for losses.
				fitnessScores[i] += outcomeVector[0];
				fitnessScores[opponent] += outcomeVector[1];
			}
		}

		return fitnessScores;
	}

	/**
	 * Plays a game between an AI using net1 as its evaluation function and an AI using net2 as its evaluation function.
	 * @param net1 The neural network to use in the first player.
	 * @param net2 The neural network to use in the second player.
	 * @return Returns a pair of numbers, representing the points awarded to net1 and net2, respectively.
	 */
	private static double[] play(NeuralNetwork net1, NeuralNetwork net2) {
		// Initialize two AIs that use the neural networks as their static evaluation function.
		EvolvedCheckersAI ai1 = new EvolvedCheckersAI(net1);
		EvolvedCheckersAI ai2 = new EvolvedCheckersAI(net2);
		int outcome = 0;

		try {
			// net1 will be red player with 50% probability.
			Random r = new Random();
			if (r.nextInt(2) == 0) {
				outcome = Checkers.play(ai1, ai2);				
			} else {
				outcome = Checkers.play(ai2, ai1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (outcome == 2) {
				return new double[] {0, 0};
			} else if (outcome == 1) {
				return new double[] {1, -2};
			} else {
				return new double[] {-2, 1};
			}
		}
	}

	/**
	 * Given a population and fitness scores, constructs the next population.
	 * @param population The population of neural networks.
	 * @param fitnessScores The fitness values for the population.
	 * @return Returns an array with the next set of neural networks. Might contain duplicates.
	 */
	private static NeuralNetwork[] nextPopulation(NeuralNetwork[] population, double[] fitnessScores) {
		NeuralNetwork[] next = new NeuralNetwork[POPULATION_SIZE];
		int mother, father;
		NeuralNetwork child;

		for (int i = 0; i < next.length; i++) {
			// Select two individuals from the population, with probability proportional to fitness.
			mother = rouletteWheelSelection(fitnessScores);
			father = rouletteWheelSelection(fitnessScores);

			child = crossover(population[mother], population[father]); // Get a child by crossing over the mother and father.
			if (child == null) { // Crossover probability wasn't met, so just use the more fit parent.
				if (fitnessScores[mother] >= fitnessScores[father]) {
					child = population[mother];
				} else {
					child = population[father];
				}
			}

			next[i] = mutate(child); // Add the mutated child to the next population.
		}

		return next;
	}

	/**
	 * Given a population's fitness scores, returns the index of a neural network from the population whose 
	 * probability of selection is proportional to its fitness. Implements stochastic acceptance selection for O(1) performance.
	 * @param fitnessScores The fitness values for the population.
	 * @return Returns a neural network from the population.
	 */
	private static int rouletteWheelSelection(double[] fitnessScores) {
		double totalFitness = 0.0;
		for (double fitnessScore : fitnessScores) {
			totalFitness += fitnessScore;
		}


		Random r = new Random();
		int candidate;

		while (true) {
			candidate = r.nextInt(fitnessScores.length);
			if (Math.random() < fitnessScores[candidate] / totalFitness) {
				return candidate;
			}
		}
	}

	/**
	 * Given two neural networks, returns a child neural network built by crossing the parents over. Implements one point
	 * crossover. Crossover only takes place with probability CROSSOVER_PROBABILITY; otherwise, null is returned.
	 * @param mother The first parent.
	 * @param father The second parent.
	 * @return Returns a new neural network from crossing over mother and father.
	 */
	private static NeuralNetwork crossover(NeuralNetwork mother, NeuralNetwork father) {
		if (mother.equals(father)) {
			return mother;
		}

		if (Math.random() < CROSSOVER_PROBABILITY) {
			// By convention, let mother be the network with fewer layers.
			if (mother.layers.length > father.layers.length) {
				NeuralNetwork temp = father;
				mother = temp;
				father = mother;
			}

			Random r = new Random();
			int crossoverPoint = r.nextInt(mother.layers.length);
			NeuralNetwork.Neuron[][] newLayers = new NeuralNetwork.Neuron[father.layers.length][]; // Builds the new network.

			// Copy neurons over from mother into newLayers.
			for (int layer = 0; layer < crossoverPoint; layer++) {
				newLayers[layer] = new NeuralNetwork.Neuron[mother.layers[layer].length];

				for (int neuron = 0; neuron < newLayers[layer].length; neuron++) {
					newLayers[layer][neuron] = new NeuralNetwork.Neuron(mother.layers[layer][neuron]);
				}
			}

			// Copy neurons over from father into newLayers.
			for (int layer = crossoverPoint; layer < newLayers.length - 1; layer++) {
				newLayers[layer] = new NeuralNetwork.Neuron[father.layers[layer].length];

				for (int neuron = 0; neuron < newLayers[layer].length; neuron++) {
					newLayers[layer][neuron] = new NeuralNetwork.Neuron(father.layers[layer][neuron]);
				}
			}

			// Copy output neurons over. Any two output neurons are identical, so just initialize new ones.
			for (int neuron = 0; neuron < newLayers[newLayers.length - 1].length; neuron++) {
				newLayers[newLayers.length - 1][neuron] = new NeuralNetwork.OutputNeuron();
			}

			// Copy bias neurons over, first from mother, then from father.
			NeuralNetwork.Neuron[] newBiasNeurons = new NeuralNetwork.Neuron[father.biasNeurons.length];
			for (int neuron = 0; neuron < crossoverPoint; neuron++) {
				newBiasNeurons[neuron] = new NeuralNetwork.Neuron(mother.biasNeurons[neuron]);
			}

			for (int neuron = crossoverPoint; neuron < newBiasNeurons.length; neuron++) {
				newBiasNeurons[neuron] = new NeuralNetwork.Neuron(father.biasNeurons[neuron]);
			}

			return new NeuralNetwork(newLayers, newBiasNeurons);
		} else {
			return null;
		}
	}

	/**
	 * Mutates the networksrk by altering weights. With probability MUTATION_PROBABILITY, there is a 50% chance that a given
	 * weight will be doubled or a 50% chance that it will be halved. There is also a 50% chance of negating it.
	 * @param network The neural network to mutate.
	 * @return Returns the mutated neural network.
	 */
	private static NeuralNetwork mutate(NeuralNetwork network) {
		NeuralNetwork networkCopy = new NeuralNetwork(network);
		Random r = new Random();

		for (int layer = 0; layer < networkCopy.layers.length - 1; layer++) {
			for (NeuralNetwork.Neuron neuron : networkCopy.layers[layer]) {
				for (int i = 0; i < neuron.weights.length; i++) {
					if (Math.random() < MUTATION_PROBABILITY) {
						if (r.nextInt(2) == 0) {
							neuron.weights[i] *= 2;							
						} else {
							neuron.weights[i] /= 2;
						}

						if (r.nextInt(2) == 0) {
							neuron.weights[i] *= -1;
						}
					}
				}
			}
		}

		return networkCopy;
	}
}