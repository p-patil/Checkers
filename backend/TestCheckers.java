import java.util.*;

/**
 * Class for testing code.
 */
public class TestCheckers {
	public static void main(String[] args) {
	 	// Checkers.playThroughConsoleAI(true);

	 	// CheckersAI player = new CheckersAI();
	 	// player.initializeTablebase(Square.RED);

		long start, end;
		start = System.nanoTime();
	 	System.out.println(EndgameTablebase.generateWinningPositions(EndgameTablebase.generateAllPositions(EndgameTablebase.ENDGAME_LIMIT), Square.RED).size());
	 	end = System.nanoTime();
	 	System.out.println(((double) (end - start)) / 1000000000);
	}
}