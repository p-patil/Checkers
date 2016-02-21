import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * Program that plays checkers. Implements a minimax algorithm and uses a generated tablebase when feasible.
 */
public class CheckersAI {
	public static final int MAX_DEPTH = 4; // The maximum depth in the game tree that minimax should explore. This is the depth at which 
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

	/**
	 * Returns a move.
	 * @param p The given position.
	 * @return A coordinate array representing the move to make.
	 */
	public int[] move(Position p) {
		p.generateSuccessors();
		int[] ret;
		ArrayList<Integer> moveList;

		// If possible, use an endgame database.	
		if (this.tablebase != null && p.redPieceCount() + p.blackPieceCount() <= EndgameTablebase.ENDGAME_LIMIT) {
			moveList = p.successors.get(tablebaseLookup(p, this.tablebase));
		} else { // Too many pieces to feasibly generate tablebase - use minimax.
			Position best = (Position) minimaxAlphaBeta(p, p.turn, 0, p.turn, new HashMap<Position, Object[]>(), Integer.MIN_VALUE, Integer.MAX_VALUE)[0];
			moveList = p.successors.get(best);
		}


		ret = new int[moveList.size()];
		for (int i = 0; i < moveList.size(); i++) {
			ret[i] = moveList.get(i);
		}

		return ret;
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
	 * Implementation of minimax algorithm to find optimal move. Memoized for efficiency. Initialize with depth = 0.
	 * @param position The checkers position to evaluate.
	 * @param turn The turn to maximize the score for.
	 * @param depth The current depth in the game tree that the algorithm is at.
	 * @param turn Whose side to evaluate the position on.
	 * @return Returns an array whose first element is the optimal successor position and whose second element is the assigned score.
	 */
	public Object[] minimax(Position position, int turn, int depth, HashMap<Position, Object[]> memoize) {
		// Base case.
		if (depth >= MAX_DEPTH || position.isLeaf()) {
			// Call the evaluation function on the opposite turn, because that's the player one level higher who's choosing between evaluations.
			return new Object[] {position, this.evaluationFunction(position, (position.turn == Square.RED) ? Square.BLACK : Square.RED)};
		}

		// Generate static evaluation values for nodes one level deeper, and find the best (max for red, min for black) static evaluation value
		// of all nodes one level deeper and propagate it upwards.
		position.generateSuccessors();
		Object[] best = null;
		int bestVal = ((position.turn == turn) ? Integer.MIN_VALUE : Integer.MAX_VALUE), tempVal;
		for (Position p : position.successors.keySet()) {
			// Compute value of child sub-tree.
			if (!memoize.containsKey(p)) {
				memoize.put(p, minimax(p, turn, depth + 1, memoize));
			}

			// Update best.
			tempVal = (int) memoize.get(p)[1];
			if ((position.turn == turn && tempVal >= bestVal) || (position.turn != turn && tempVal <= bestVal)) {
				bestVal = tempVal;
				best = new Object[] {p, (int) memoize.get(p)[1]};
			}
		}

		return best;
	}

	/**
	 * Implementation of minimax algorithm, optimized with alpha-beta pruning. Memoized for efficiency. Initialize with currTurn = turn, depth = 0, 
	 * alpha = negative infinity, beta = positive infinity.
	 * The idea is, when a maximizer node is computing the minimax values of its children, it will return the maximum of the values. The
	 * parent of the maximizer node, which is a minimzer node, will choose the minimum value of all its children. Beta represents the minimum
	 * value found so far by the minimizer node at the time that the maximizer node, which is a child of the minimizer, is being traversed. 
	 * When the maximizer computes the minimax values of its own children, the moment it comes across a child with minimax value greater than
	 * beta, it can stop searching since the minimizer node will never choose any node in the sub-tree formed by the maximizer node, as the 
	 * node with value beta will always be less than the minimax value found by the maximizer node. Similar logic applies to the inverse case.
	 * @param position The checkers position to evaluate.
	 * @param turn The turn to maximize the score for.
	 * @param currTurn The current turn - useful in determining if the current node is a maximizer or minimizer node.
	 * @param depth The current depth in the game tree that the algorithm is at.
	 * @param turn Whose side to evaluate the position on.
	 * @param alpha Value representing the maximum value found so far by a parent maximizer node.
	 * @param beta Value representing the minimum value found so far by a parent minimizer node.
	 * @return Returns an array whose first element is the optimal successor position and whose second element is the assigned score.
	 */
	protected Object[] minimaxAlphaBeta(Position position, int turn, int currTurn, int depth, HashMap<Position, Object[]> memoize, 
		int alpha, int beta) {
		// Base case.
		if (depth >= MAX_DEPTH || position.isLeaf()) {
			// Call the evaluation function on the opposite turn, because that's the player one level higher who's choosing between evaluations.
			return new Object[] {position, this.evaluationFunction(position, (position.turn == Square.RED) ? Square.BLACK : Square.RED)};
		}

		// Generate static evaluation values for nodes one level deeper, and find the best (max for red, min for black) static evaluation value
		// of all nodes one level deeper and propagate it upwards.
		position.generateSuccessors();
		Object[] best = null;
		int bestVal = ((position.turn == turn) ? Integer.MIN_VALUE : Integer.MAX_VALUE), tempVal;
		for (Position p : position.successors.keySet()) {
			// Compute value of child sub-tree.
			if (!memoize.containsKey(p)) {
				memoize.put(p, minimaxAlphaBeta(p, turn, ((currTurn == Square.RED) ? Square.BLACK : Square.RED), depth + 1, memoize, alpha, beta));
			}
			
			// Update best.
			tempVal = (int) memoize.get(p)[1];
			if ((position.turn == turn && tempVal >= bestVal) || (position.turn != turn && tempVal <= bestVal)) {
				bestVal = tempVal;
				best = new Object[] {p, tempVal};

				// Update alpha / beta value.
				if (turn == currTurn) {
					// Update only if tempVal is better than current value.

					if (tempVal > alpha) {
						alpha = tempVal;				
					}
				} else {
					if (tempVal < beta) {
						beta = tempVal;
					}
				}
			}

			// Prune the children.
			if (alpha >= beta) {
				return best;
			}
		}

		return best;
	}

	/**
	 * Implements alpha-beta pruning on minimax, but also uses the killer heuristic. This heuristic keeps track of a fixed number
	 * of moves that have produced cut-offs earlier in the tree, and tries those moves first in hope of producing another cut-off.
	 * The assumption the heuristic makes is that moves that produced cut-offs earlier in the tree are more likely than other moves
	 * to produce cut-offs in the future.
	 * @param position The checkers position to evaluate.
	 * @param turn The turn to maximize the score for.
	 * @param currTurn The current turn - useful in determining if the current node is a maximizer or minimizer node.
	 * @param depth The current depth in the game tree that the algorithm is at.
	 * @param turn Whose side to evaluate the position on.
	 * @param alpha Value representing the maximum value found so far by a parent maximizer node.
	 * @param beta Value representing the minimum value found so far by a parent minimizer node.
	 * @param killerMoves A mapping from a given depth to the set of killer moves maintained for that depth so far.
	 * @param numKillerMoves The number of killer moves to track.
	 * @return Returns an array whose first element is the optimal successor position and whose second element is the assigned score.
	 */
	protected Object[] minimaxAlphaBeta_KillerHeuristic(Position position, int turn, int currTurn, int depth, 
		HashMap<Position, Object[]> memoize, int alpha, int beta, HashMap<Integer, HashSet<ArrayList<Integer>>> killerMoves, int numKillerMoves) {
		// Base case.
		if (depth >= MAX_DEPTH || position.isLeaf()) {
			// Call the evaluation function on the opposite turn, because that's the player one level higher who's choosing between evaluations.
			return new Object[] {position, this.evaluationFunction(position, (position.turn == Square.RED) ? Square.BLACK : Square.RED)};
		}

		// If no killer moves are tracked yet for this depth, initialize.
		if (!killerMoves.containsKey(depth)) {
			killerMoves.put(depth, new HashSet<>());
		}

		// Generate static evaluation values for nodes one level deeper, and find the best (max for red, min for black) static evaluation value
		// of all nodes one level deeper and propagate it upwards.
		position.generateSuccessors();
		Object[] best = null;
		int bestVal = ((position.turn == turn) ? Integer.MIN_VALUE : Integer.MAX_VALUE), tempVal;

		// Check killer moves first.
		HashMap<ArrayList<Integer>, Position> inverseMapping = new HashMap<>();
		for (Position p : position.successors.keySet()) {
			inverseMapping.put(position.successors.get(p), p);
		}

		for (ArrayList<Integer> move : killerMoves.get(depth)) {
			if (inverseMapping.containsKey(move)) {
				// Compute value of child sub-tree.
				if (!memoize.containsKey(inverseMapping.get(move))) {
					memoize.put(inverseMapping.get(move), minimaxAlphaBeta_KillerHeuristic(inverseMapping.get(move), turn, ((currTurn == Square.RED) ? Square.BLACK : Square.RED), depth + 1, memoize, alpha, beta, killerMoves, numKillerMoves));
				}
				
				// Update best.
				tempVal = (int) memoize.get(inverseMapping.get(move))[1];
				if ((position.turn == turn && tempVal >= bestVal) || (position.turn != turn && tempVal <= bestVal)) {
					bestVal = tempVal;
					best = new Object[] {inverseMapping.get(move), tempVal};

					// Update alpha / beta value.
					if (turn == currTurn) {
						if (tempVal > alpha) {
							alpha = tempVal;
						}
					} else {
						if (tempVal < beta) {
							beta = tempVal;
						}
					}
				}

				// Prune the children.
				if (alpha >= beta) {
					return best;
				}
			}
		}

		// Killer moves all failed, so proceed as normal.
		for (Position p : position.successors.keySet()) {
			// Skip killer moves since we already tried them.
			if (killerMoves.get(depth).contains(position.successors.get(p))) {
				continue;
			}

			// Compute value of child sub-tree.
			if (!memoize.containsKey(p)) {
				memoize.put(p, minimaxAlphaBeta_KillerHeuristic(p, turn, ((currTurn == Square.RED) ? Square.BLACK : Square.RED), depth + 1, memoize, alpha, beta, killerMoves, numKillerMoves));
			}
			
			// Update best.
			tempVal = (int) memoize.get(p)[1];
			if ((position.turn == turn && tempVal >= bestVal) || (position.turn != turn && tempVal <= bestVal)) {
				bestVal = tempVal;
				best = new Object[] {p, tempVal};

				// Update alpha / beta value.
				if (turn == currTurn) {
					alpha = tempVal;
				} else {
					beta = tempVal;
				}
			}

			// Prune the children; position p created a cut-off, so replace one of the stored killer moves with p.
			if (alpha >= beta) {
				if (killerMoves.get(depth).size() >= numKillerMoves) { // If killerMoves is full, remove a random element from killerMoves.
					Iterator<ArrayList<Integer>> iter = killerMoves.get(depth).iterator();
					iter.next();
					iter.remove();
					// killerMoves.get(depth).iterator().remove();
				}
				killerMoves.get(depth).add(position.successors.get(p));

				return best;
			}
		}

		return best;
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

	/**
	 * The static position evaluation function used in the minimax algorithm.
	 * @param p The position to evaluate.
	 * @param turn Whose turn to evaluate on.
	 * @return The evaluation value.
	 */
	protected int evaluationFunction(Position p, int turn) {
		if ((turn == Square.RED && p.getState() == 1) || (turn == Square.BLACK && p.getState() == -1)) {
			return Integer.MAX_VALUE; // If the position is won, return infinity.
		} else if ((turn == Square.RED && p.getState() == -1) || (turn == Square.BLACK && p.getState() == 1)) {
			return Integer.MIN_VALUE; // If the position is lost, return minus infinity.
		}

		return evaluationFunction_piecesDifference(p, turn);
	}

	// Evaluation functions below this line.

	/**
	 * Simple evaluation function based on number of pieces still standing.
	 * @param p The position to evaluate.
	 * @param turn Whose turn it is.
	 * @return Returns the evaluation value.
	 */
	private static int evaluationFunction_numPieces(Position p, int turn) {
		int val = 0;
		for (int i = 0; i < p.board.length; i++) {
			for (int j = 0; j < p.board[i].length; j++) {
				if (!p.board[i][j].isEmpty()) {
					if ((p.board[i][j].isRed() && turn == Square.RED) || (p.board[i][j].isBlack() && turn == Square.BLACK)) {
						if (p.board[i][j].isKing()) {
							val += 2; // Two points for kings.
						} else {
							val++; // One point for normal pieces.
						}
					}
				}
			}
		}
		return val;
	}

	/**
	 * Simple evaluation function that returns the difference in number of friendly and enemy pieces.
	 * @param p The position to evaluate.
	 * @param turn Whose turn it is.
	 * @return Returns the evaluation value.
	 */
	private static int evaluationFunction_piecesDifference(Position p, int turn) {
		int val = 0;
		for (int i = 0; i < p.board.length; i++) {
			for (int j = 0; j < p.board[i].length; j++) {
				if (!p.board[i][j].isEmpty()) {
					if ((p.board[i][j].isRed() && turn == Square.RED) || (p.board[i][j].isBlack() && turn == Square.BLACK)) {
						if (p.board[i][j].isKing()) {
							val += 2; // Two points for kings.
						} else {
							val++; // One point for normal pieces.
						}
					} else if ((p.board[i][j].isRed() && turn == Square.BLACK) || (p.board[i][j].isBlack() && turn == Square.RED)) {
						if (p.board[i][j].isKing()) {
							val -= 2;
						} else {
							val--;
						}
					}
				}
			}
		}
		return val;
	}
}