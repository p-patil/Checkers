import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

/**
 * Program that plays checkers. Implements a minimax algorithm and uses a generated tablebase when feasible.
 */
public class CheckersAI {
	public static final int MAX_DEPTH = 3; // The maximum depth in the game tree that minimax should explore. This is the depth at which 
										   // static evaluations are performed, instead of going one level deeper.

	private EndgameTablebase tablebase;
	private static final String serializationFile = "database";

	/**
	 * Basic constructor.
	 */
	public CheckersAI() {
		this.tablebase = null;
	}

	/**
	 * Basic constructor that allows the AI to use an endgame database.
	 */
	public CheckersAI(EndgameTablebase tablebase) {
		this.tablebase = tablebase;
	}

	public int[] move(Checkers game) {
		Position curr = new Position(game);
		curr.generateSuccessors();

		// If possible, use an endgame database.	
		if (game.getRedCount() + game.getBlackCount() <= EndgameTablebase.ENDGAME_LIMIT && this.tablebase != null) {
			return curr.successors.get(tablebaseLookup(game, this.tablebase));
		} else { // Too many pieces to feasibly generate tablebase - use minimax.
			Position best = (Position) minimax(curr, MAX_DEPTH, new HashMap<Position, Object[]>())[0];
			return curr.successors.get(best);
		}
	}

	/**
	 * Initializes a tablebase, whose maximum number of pieces is defined in the tablebase class, for the given turn.
	 * @param turn Whose side to initialize the tablebase for.
	 */
	public void initializeTablebase(int turn) {
		File tb = new File(serializationFile);
		if (tb.exists()) {
			this.tablebase = EndgameTablebase.deserialize(serializationFile);
		} else {
			this.tablebase = new EndgameTablebase(turn);
			this.tablebase.serialize(serializationFile);
		}
	}

	/**
	 * Implementation of minimax algorithm to find optimal move. Memoized for efficiency.
	 * @param position The checkers position to evaluate.
	 * @param depth The current depth in the game tree that the algorithm is at.
	 * @param turn Whose side to evaluate the position on.
	 * @return Returns an array whose first element is the optimal successor position and whose second element is the assigned score.
	 */
	private static Object[] minimax(Position position, int depth, HashMap<Position, Object[]> memoize) {
		// Base case.
		if (depth >= MAX_DEPTH || position.isLeaf()) {
			// Call the evaluation function on the opposite turn, because that's the player one level higher who's choosing between evaluations.
			return new Object[] {position, position.evaluationFunction((position.turn == Square.RED) ? Square.BLACK : Square.RED)};
		}

		// Generate static evaluation values for nodes one level deeper.
		position.generateSuccessors();
		for (Position p : position.successors.keySet()) {
			if (!memoize.containsKey(p)) {
				memoize.put(p, minimax(p, depth + 1, memoize));
			}
		}

		// Find the best (max for red, min for black) static evaluation value of all nodes one level deeper and propagate it upwards.
		Object[] best = null;
		int bestVal = ((position.turn == Square.RED) ? Integer.MIN_VALUE : Integer.MAX_VALUE), tempVal;
		for (Position p : position.successors.keySet()) {
			tempVal = (int) memoize.get(p)[1];
			if ((position.turn == Square.RED && tempVal > bestVal) || (position.turn == Square.BLACK && tempVal < bestVal)) {
				bestVal = tempVal;
				best = new Object[] {p, (int) memoize.get(p)[1]};
			}
		}

		return best;
	}

	/**
	 * Implmements logic for looking up in tablebase.
	 * @param game The initial board configuration to look up.
	 * @param tb The tablebase to lookup in.
	 */
	private static Position tablebaseLookup(Checkers game, EndgameTablebase tb) {
		Position p = tb.database.get(game);
		Position best = null;
		int bestVal = Integer.MIN_VALUE;

		// Return the successor position with the shortest path to victory.
		for (Position q : p.successorScores.keySet()) {
			if (p.successorScores.get(q) < bestVal) {
				best = q;
				bestVal = p.successorScores.get(q);
			}
		}

		return best;
	}

}