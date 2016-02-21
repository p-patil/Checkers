import java.lang.Math;
import java.util.Random;

/**
 * Basic feedforward neural network, for use as a static evaluation function.
 */
public class NeuralNetwork {
	public Neuron[][] layers; // The layers of this network.
	public Neuron[] biasNeurons; // Maps layers to bias neurons, used to simulate a threshold in the network.

	/**
	 * Initializes a neural network with random weights whose i-th layer has layerCounts[i] neurons.
	 * @param layerCounts An array mapping layers to the number of neurons they contain.
	 */
	public NeuralNetwork(int[] layerCounts) {
		if (layerCounts.length < 2) {
			throw new IllegalArgumentException("Neural network must have at least two layers - input and output.");
		}

		this.layers = new Neuron[layerCounts.length][]; // Initialize number of layers.
		this.biasNeurons = new Neuron[layerCounts.length - 1]; // Initialize bias neuron.

		// Construct layers backwards, attaching synapses along the way. Start with output layer.
		this.layers[this.layers.length - 1] = new Neuron[layerCounts[layerCounts.length - 1]];
		for (int outputNeuron = 0; outputNeuron < this.layers[this.layers.length - 1].length; outputNeuron++) {
			this.layers[this.layers.length - 1][outputNeuron] = new OutputNeuron();
		}

		for (int layer = this.layers.length - 2; layer >= 0; layer--) {
			if (layerCounts[layer] == 0) {
				throw new IllegalArgumentException("Layer cannot be empty.");
			}

			this.layers[layer] = new Neuron[layerCounts[layer]]; // Initialize this layer.
			for (int neuron = 0; neuron < this.layers[layer].length; neuron++) {
				this.layers[layer][neuron] = new Neuron(this.layers[layer + 1]); // Set connections to layers one ahead.
			}

			this.biasNeurons[layer] = new Neuron(this.layers[layer + 1]);
		}
	}

	/**
	 * Initializes a neural network with the given weights whose i-th layer has layerCounts[i] neurons.
	 * @param layerCounts An array mapping layers to the number of neurons they contain.
	 * @param weights An array that maps layers to neurons to weight vectors.
	 */
	public NeuralNetwork(int[] layerCounts, double[][][] weights) {
		if (layerCounts.length < 2) {
			throw new IllegalArgumentException("Neural network must have at least two layers - input and output.");
		}

		this.layers = new Neuron[layerCounts.length][]; // Initialize number of layers.
		this.biasNeurons = new Neuron[layerCounts.length - 1]; // Initialize bias neurons.

		// Construct layers backwards, attaching synapses along the way. Start with output layer.
		this.layers[this.layers.length - 1] = new Neuron[layerCounts[layerCounts.length - 1]];
		for (int outputNeuron = 0; outputNeuron < this.layers[this.layers.length - 1].length; outputNeuron++) {
			this.layers[this.layers.length - 1][outputNeuron] = new OutputNeuron();
		}

		for (int layer = this.layers.length - 2; layer >= 0; layer--) {
			if (layerCounts[layer] == 0) {
				throw new IllegalArgumentException("Layer cannot be empty.");
			}

			this.layers[layer] = new Neuron[layerCounts[layer]]; // Initialize this layer.
			for (int neuron = 0; neuron < this.layers[layer].length; neuron++) {
				this.layers[layer][neuron] = new Neuron(this.layers[layer + 1], weights[layer][neuron]); // Set connections to layers one ahead.
			}
			this.biasNeurons[layer] = new BiasNeuron(this.layers[layer + 1]);
		}
	}

	/**
	 * Builds a neural network given the lists of neurons per layer.
	 * @param newLayer An array of neuron arrays; each array is a layer and the sub-array contains the neurons in that layer.
	 * @param newBiasNeurons The array of new bias neurons.
	 */
	public NeuralNetwork(Neuron[][] newLayer, Neuron[] newBiasNeurons) {
		this.layers = new Neuron[newLayer.length][];

		for (int layer = 0; layer < this.layers.length; layer++) {
			this.layers[layer] = new Neuron[newLayer[layer].length];

			for (int neuron = 0; neuron < this.layers[layer].length; neuron++) {
				this.layers[layer][neuron] = new Neuron(newLayer[layer][neuron]);
			}
		}

		this.biasNeurons = new Neuron[newBiasNeurons.length];
		for (int neuron = 0; neuron < newBiasNeurons.length; neuron++) {
			this.biasNeurons[neuron] = new Neuron(newBiasNeurons[neuron]);
		}
	}

	/**
	 * Constructor that copies the given neural network.
	 * @param network The neural network to copy.
	 */
	public NeuralNetwork(NeuralNetwork network) {
		this(network.layers, network.biasNeurons);
	}

	/**
	 * Class representing a neuron / perceptron.
	 */
	public static class Neuron {
		public Neuron[] synapses; // List of neurons in the next layer to which this neuron is connected
		public double[] weights; // Corresponding weights for the synapses

		/**
		 * Basic, empty constructor. Necessary to allow for zero-argument constructor in class OutputNeuron.
		 */
		public Neuron() {

		}

		/**
		 * Basic constructor. Randomly initializes weights.
		 * @param synapses The neurons to connect synapses to.
		 */
		public Neuron(Neuron[] synapses) {
			this.synapses = new Neuron[synapses.length];
			this.weights = new double[this.synapses.length];

			for (int i = 0; i < this.synapses.length; i++) {
				this.synapses[i] = synapses[i];
				this.weights[i] = 2 * (Math.random() - 0.5);
			}
		}

		/**
		 * Constructor that uses given weights.
		 * @param synapses The neurons to connect to.
		 * @param weights The corresponding weights.
		 */
		public Neuron(Neuron[] synapses, double[] weights) {
			this.synapses = new Neuron[synapses.length];
			this.weights = new double[this.synapses.length];

			for (int i = 0; i < this.synapses.length; i++) {
				this.synapses[i] = synapses[i];
				this.weights[i] = weights[i];
			}
		}

		/**
		 * Makes a copy of the inputted neuron.
		 * @param n The neuron to copy.
		 */
		public Neuron(Neuron n) {
			this.synapses = n.synapses;
			this.weights = n.weights;
		}

		/**
		 * Determines the output of this neuron based on its inputs.
		 * @param input The input to the neuron.
		 * @return Returns the output, which is the activation function (usually sigmoid) called on the weighted sum of the inputs.
		 */
		public double[] output(double input) {
			double[] outputVector = new double[this.weights.length];
			for (int i = 0; i < this.weights.length; i++) {
				outputVector[i] = activationFunction(input * this.weights[i], 1);
			}

			return outputVector;
		}

		/**
		 * The activation function used by the neurons. Implemented as a sigmoid function: f(x) = 1 / (1 + e^(-x * lambda))
		 * @param x The input value to act on.
		 * @param lambda The exponent parameter.
		 */
		private static double activationFunction(double x, double lambda) {
			return 1 / (1 + Math.exp(-x * lambda));
		}

		/**
		 * @return Returns if this neuron is an output neuron or not.
		 */
		public boolean isOutputNeuron() {
			return this.synapses == null && this.weights == null;
		}

		@Override
		public boolean equals(Object obj) {
			Neuron n = (Neuron) obj;

			// Lengths must match.
			if (this.synapses.length != n.synapses.length || this.weights.length != n.weights.length) {
				return false;
			}

			// Make sure weights match. No need to check synapses since lengths match anyways.
			for (int i = 0; i < this.weights.length; i++) {
				if (this.weights[i] != n.weights[i]) {
					return false;
				}
			}

			return true;
		}

		@Override
		public int hashCode() {
			int base = this.weights.length, pow = 1, sum = 0;
			int digit;

			for (int i = this.weights.length - 1; i >= 0; i--) {
				digit = (int) (this.weights[i] * 10);
				sum += digit * pow;
				pow *= base;
			}

			return sum;
		}
	}

	/**
	 * Class representing a bias neuron.
	 */
	public static class BiasNeuron extends Neuron {
		public BiasNeuron(Neuron[] synapses) {
			super(synapses);
		}

		public BiasNeuron(Neuron[] synapses, double[] weights) {
			super(synapses, weights);
		}

		@Override
		/**
		 * Bias neurons always output -1.
		 * @param input Input to the bias neuron. Never used.
		 * @return Returns -1.
		 */
		public double[] output(double input) {
			double[] outputVector = new double[this.weights.length];
			for (int i = 0; i < outputVector.length; i++) {
				outputVector[i] = -1.0;
			}

			return outputVector;
		}
	}

	public static class OutputNeuron extends Neuron {
		/**
		 * Basic constructor.
		 */
		public OutputNeuron() {
			this.synapses = null;
			this.weights = null;
		}

		@Override
		public double[] output(double input) {
			return new double[] {super.activationFunction(input, 1)};
		}

		@Override
		public boolean equals(Object obj) {
			return ((Neuron) obj).isOutputNeuron();
		}

		@Override
		public int hashCode() {
			return 0;
		}
	}

	/**
	 * Given an input vector, feeds the data through the entire network, returning the output vector.
	 * @param inputs The input vector to use as input.
	 * @return Returns the output vector.
	 */
	public double[] feedforward(double[] inputs) {
		double[] currLayerInputs = inputs, biasOutput;
		
		// Feed the data layer by layer.
		double[] nextLayerInputs, neuronOutput;
		for (int layer = 0; layer < this.layers.length - 1; layer++) {
			nextLayerInputs = new double[this.layers[layer + 1].length];
			// For each neuron in the current layer, increment the input vector for neurons in the next layer with the output 
			// of the current neuron.
			for (int neuron = 0; neuron < this.layers[layer].length; neuron++) {
				neuronOutput = this.layers[layer][neuron].output(currLayerInputs[neuron]); // Get the output vector for this neuron.
				for (int nextNeuron = 0; nextNeuron < neuronOutput.length; nextNeuron++) {
					nextLayerInputs[nextNeuron] += neuronOutput[nextNeuron]; // Increment the weighted sum of inputs for the neurons in the next layer.
				}
			}

			biasOutput = this.biasNeurons[layer].output(0);
			for (int nextNeuron = 0; nextNeuron < biasOutput.length; nextNeuron++) {
				nextLayerInputs[nextNeuron] += biasOutput[nextNeuron] * this.biasNeurons[layer].weights[nextNeuron];
			}

			currLayerInputs = nextLayerInputs;
		}

		return currLayerInputs;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();

		for (int layer = 0; layer < this.layers.length - 1; layer++) {
			if (layer == 0) {
				s.append("INPUT LAYER\n");
			} else {
				s.append("LAYER " + layer + "\n");
			}

			s.append("\tBias Neuron - Weights:\n");
			for (int nextNeuron = 0; nextNeuron < this.layers[layer + 1].length; nextNeuron++) {
				s.append("\t\tNeuron " + nextNeuron + ": " + this.biasNeurons[layer].weights[nextNeuron] + "\n");
			}

			for (int neuron = 0; neuron < this.layers[layer].length; neuron++) {
				s.append("\tNeuron " + neuron + " - Weights:\n");
				for (int nextNeuron = 0; nextNeuron < this.layers[layer + 1].length; nextNeuron++) {
					s.append("\t\tNeuron " + nextNeuron + ": " + this.layers[layer][neuron].weights[nextNeuron] + "\n");
				}
			}
		}

		s.append("OUTPUT LAYER\n");
		for (int outputNeuron = 0; outputNeuron < this.layers[this.layers.length - 1].length; outputNeuron++) {
			s.append("\tOutput Neuron " + outputNeuron);
		}

		return s.toString();
	}

	@Override
	public boolean equals(Object obj) {
		NeuralNetwork net = (NeuralNetwork) obj;

		// Lengths must match.
		if (this.layers.length != net.layers.length || this.biasNeurons.length != net.biasNeurons.length) {
			return false;
		}

		// Check each layer for equality.
		for (int layer = 0; layer < this.layers.length; layer++) {
			// Layer lengths must match.
			if (this.layers[layer].length != net.layers[layer].length) {
				return false;
			}

			// Check each neuron in the layer for equality.
			for (int neuron = 0; neuron < this.layers[layer].length; neuron++) {
				if (!this.layers[layer][neuron].equals(net.layers[layer][neuron])) {
					return false;
				}
			}
		}

		// Check bias neurons for equality.
		for (int biasNeuron = 0; biasNeuron < this.biasNeurons.length; biasNeuron++) {
			if (!this.biasNeurons[biasNeuron].equals(net.biasNeurons[biasNeuron])) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int hashCode() {
		int base = this.layers.length, pow = 1, sum = 0;
		int digit;

		int internalBase, internalPow, internalSum;
		int internalDigit;

		for (int layer = 0; layer < this.layers.length; layer++) {
			internalBase = this.layers[layer].length;
			internalPow = 1;
			internalSum = 0;

			for (int neuron = 0; neuron < this.layers[layer].length; neuron++) {
				internalDigit = this.layers[layer][neuron].hashCode();
				internalSum += internalDigit * internalPow;
				internalPow *= internalBase;
			}

			digit = internalSum;
			sum += digit * pow;
			pow *= base;
		}

		return sum;
	}
}