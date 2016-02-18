import java.util.*;

/**
 * Class for testing code.
 */
public class TestCheckers {
	public static void main(String[] args) {
		// Checkers.consolePlayAI(false);
		Position p = new Position(new Random(), 8);
		int val1, val2;
		long start, end;

		for (int i = 0; i < 10; i++) {
			start = System.nanoTime();
			val1 = (int) CheckersAI.minimax(p, p.turn, 0, new HashMap<>())[1];
			end = System.nanoTime();
			System.out.print("Minimax: " + (((double) end - start) / 1000000000));

			start = System.nanoTime();
			val2 = (int) CheckersAI.minimaxAlphaBeta_KillerHeuristic(p, p.turn, p.turn, 0, new HashMap<>(), Integer.MIN_VALUE, Integer.MAX_VALUE, new HashMap<>(), 2)[1];
			end = System.nanoTime();
			System.out.println(", KH: " + (((double) end - start) / 1000000000));

			if (val1 != val2) {
				System.out.println("Failure: minimax got " + val1 + ", KH got " + val2);
				break;
			}
		}
	}
}