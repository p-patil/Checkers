import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

/**
 * Program that plays checkers. Implements a minimax algorithm and uses a generated tablebase when feasible.
 */
public class CheckersAI {
	public static final int MAX_DEPTH = 6; // The maximum depth in the game tree that minimax should explore. This is the depth at which 
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

	public int[] move(Position p) {
		p.generateSuccessors();

		// If possible, use an endgame database.	
		if (this.tablebase != null && p.redPieceCount() + p.blackPieceCount() <= EndgameTablebase.ENDGAME_LIMIT) {
			return p.successors.get(tablebaseLookup(p, this.tablebase));
		} else { // Too many pieces to feasibly generate tablebase - use minimax.
			Position best = (Position) minimax(p, p.turn, 0, new HashMap<Position, Object[]>())[0];
			return p.successors.get(best);
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
	 * @param turn The turn to maximize the score for.
	 * @param depth The current depth in the game tree that the algorithm is at.
	 * @param turn Whose side to evaluate the position on.
	 * @return Returns an array whose first element is the optimal successor position and whose second element is the assigned score.
	 */
	public static Object[] minimax(Position position, int turn, int depth, HashMap<Position, Object[]> memoize) {
		// Base case.
		if (depth >= MAX_DEPTH || position.isLeaf()) {
			// Call the evaluation function on the opposite turn, because that's the player one level higher who's choosing between evaluations.
			return new Object[] {position, position.evaluationFunction((position.turn == Square.RED) ? Square.BLACK : Square.RED)};
		}

		// Generate static evaluation values for nodes one level deeper.
		position.generateSuccessors();
		for (Position p : position.successors.keySet()) {
			if (!memoize.containsKey(p)) {
				memoize.put(p, minimax(p, turn, depth + 1, memoize));
			}
		}

		// Find the best (max for red, min for black) static evaluation value of all nodes one level deeper and propagate it upwards.
		Object[] best = null;
		int bestVal = ((position.turn == turn) ? Integer.MIN_VALUE : Integer.MAX_VALUE), tempVal;
		for (Position p : position.successors.keySet()) {
			tempVal = (int) memoize.get(p)[1];
			if ((position.turn == turn && tempVal >= bestVal) || (position.turn != turn && tempVal <= bestVal)) {
				bestVal = tempVal;
				best = new Object[] {p, (int) memoize.get(p)[1]};
			}
		}

		return best;
	}

	private static Object[] minimaxAlphaBeta() {
		return null;
	}

	/**
	 * Implmements logic for looking up in tablebase.
	 * @param game The initial board configuration to look up.
	 * @param tb The tablebase to lookup in.
	 * @return Returns the optimal successor position to p, or null if p represents a position guaranteed to lose.
	 */
	private static Position tablebaseLookup(Position p, EndgameTablebase tb) {
		Position q = tb.database.get(p);
		Position best = null;
		int bestVal = Integer.MIN_VALUE;

		// Return the successor position with the shortest path to victory.
		for (Position r : p.successorScores.keySet()) {
			if (q.successorScores.get(r) < bestVal) {
				best = r;
				bestVal = q.successorScores.get(r);
			}
		}

		return best;
	}
}