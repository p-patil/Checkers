import java.util.HashMap;
import java.util.Random;

/**
 * Class for testing code.
 */
public class TestCheckers {
	public static void main(String[] args) throws Exception {
		System.out.println("Evolving...");
		NeuralNetwork evolvedNetwork = EvolvedCheckersAI.evolve(1);
		System.out.println("Done. Playing games...");
		for (int i = 0; i < 25; i++) {
			System.out.println("Game " + i + " outcome: " + Checkers.play(new CheckersAI(), new EvolvedCheckersAI()));
		}
	}
}