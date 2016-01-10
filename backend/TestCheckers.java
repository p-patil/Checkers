import java.util.HashMap;

/**
 * Class for testing code.
 */
public class TestCheckers {
	public static void main(String[] args) {
		long start, end;
		start = System.nanoTime();
		(new CheckersAI()).minimax(new Position(new Checkers(), Square.RED, null), 0, Square.RED, new HashMap<Position, Position>());
		end = System.nanoTime();
		System.out.println((((double) end - start) / 1000000000) + " seconds elapsed.");
	}
}