import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
/**
 * Program that plays checkers. Implements a minimax algorithm and uses a generated tablebase when feasible.
 */
public class CheckersAI {
	public static final int MAX_DEPTH = 4; // The maximum depth in the game tree that minimax should explore.

	private EndgameDatabase database;

	/**
	 * Basic constructor.
	 */
	public CheckersAI() {
		database = null;
	}

	/**
	 * Basic constructor that allows the AI to use an endgame database.
	 */
	public CheckersAI(EndgameDatabase database) {
		this.database = database;
	}

	public int[] move(Checkers game) {
		Position curr = new Position(game);

		// If possible, use an endgame database.	
		if (game.getRedCount() + game.getBlackCount() <= EndgameDatabase.ENDGAME_LIMIT && this.database != null) {
			// Endgame database logic goes here
			return null;
		} else { // Too many pieces to feasibly generate tablebase - use minimax.
			Position best = minimax(curr, MAX_DEPTH, new HashMap<Position, Position>());
			return curr.successors.get(best);
		}
	}

	/**
	 * Implementation of minimax algorithm to find optimal move. Memoized for efficiency.
	 * @param position The checkers position to evaluate.
	 * @param depth The current depth in the game tree that the algorithm is at.
	 * @param turn Whose side to evaluate the position on.
	 * @return Returns the optimal position, based on the evaluation function of a Position object and the maximum searchable depth.
	 */
	public Position minimax(Position position, int depth, HashMap<Position, Position> memoize) {
		if (depth > MAX_DEPTH || position.isLeaf()) {
			return position;
		}

		Position best = position, temp;
		double bestVal, tempVal;
		position.generateSuccessors();
		bestVal = Double.NEGATIVE_INFINITY;
		for (Position p : position.successors.keySet()) {
			if (!memoize.containsKey(p)) {
				memoize.put(p, minimax(p, depth + 1, memoize));
			}
			temp = memoize.get(p);
			tempVal = temp.evaluationFunction(position.turn);
			if ((position.turn == Square.RED && tempVal > bestVal) || (position.turn == Square.BLACK && tempVal < bestVal)) {
				bestVal = tempVal;
				best = temp;
			}
		}
		
		return best;
	}

}