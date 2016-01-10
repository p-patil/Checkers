import java.util.HashSet;
import java.util.Arrays;

/**
 * Class representing a node in the game tree used in generating endgame tablebases and running the minimax algorithm.
 */
public class Position {
	private static final Position UNKNOWN = new Position(); // Placeholder value to represent an unknown value for this position.

	public HashSet<Position> successors; // The successor positions to this position, defined as all positions one forward move away from this position.
	public int[] parentMove; // Stores the move that bridges the parent of this Position to this position. Its length may be greater than 4 if
						     // the transition is a double jump capture sequence.
	public Position bestSuccessor; // The optimal successor to this position. Used in the endgame database.
	public final Square[][] board; // The board configuration represented by this position object.

	/**
	 * Basic constructor.
	 */
	public Position() {
		this.board = null;
	}

	/**
	 * Basic constructor.
	 * @param game The game with the position to use.
	 * @param player Which player this position is for.
	 */
	public Position(Checkers game, int player, int[] parentMove) {
		this.bestSuccessor = UNKNOWN;
		this.parentMove = parentMove;
		this.board = deepSquareCopy(game.board);
		if (game.isGameOver() != 0) {
			this.successors = null;
		} else {
			this.successors = new HashSet<>();
		}
	}

	/**
	 * Constructor that builds a position given a checkers board.
	 * @param board The board to use.
	 * @param player Which player this position is for.
	 */
	public Position(Square[][] board, int player, int[] parentMove) {
		this.bestSuccessor = UNKNOWN;
		this.parentMove = parentMove;
		this.board = deepSquareCopy(board);
		if ((new Checkers(board)).isGameOver() != 0) {
			this.successors = null;
		} else {
			this.successors = new HashSet<>();
		}
	}

	/**
	 * Initializes this Position object's successors.
	 */
	public void generateSuccessors(int turn) {
		if (!this.successors.isEmpty()) {
			return;
		}

		for (int i = 0; i < this.board.length; i++) {
			for (int j = 0; j < this.board[i].length; j++) {
				if (this.board[i][j].isRed() == (turn == Square.RED)) {
					for (Position p : generateAllMoves(i, j, turn)) {
						this.successors.add(p);
					}
				}
			}
		}
	}

	/**
	 * Whether or not this position is a leaf, which is defined to be any game in a terminal state (ie win, lose, draw).
	 */
	public boolean isLeaf() {
		return this.successors == null;
	}

	/**
	 * The static position evaluation function used in the minimax algorithm.
	 * @param position The position to evaluate.
	 * @param turn Whose side to evaluate the position on.
	 * @return The evaluation value.
	 */
	public double evaluationFunction(int turn) {
		double val = 0.0;
		for (int i = 0; i < this.board.length; i++) {
			for (int j = 0; j < this.board[i].length; j++) {
				if (!this.board[i][j].isEmpty()) {
					if (this.board[i][j].isRed() == (turn == Square.RED)) {
						if (this.board[i][j].isKing()) {
							val += 2;
						} else {
							val++;
						}
					}
				}
			}
		}
		return val;
	}

	@Override
	public boolean equals(Object obj) {
		if (this.board.length != ((Position) obj).board.length) {
			return false;
		}

		for (int i = 0; i < this.board.length; i++) {
			if (this.board[i].length != ((Position) obj).board[i].length) {
				return false;
			}

			for (int j = 0; j < this.board[i].length; j++) {
				if (!this.board[i][j].equals(((Position) obj).board[i][j])) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.board);
	}

	// Helper methods below this line.

	/**
	 * Generates all board configurations obtained by iterating over all possible moves of the piece at [i, j] on this Position's board.
	 * @param i The vertical coordinate of the piece.
	 * @param j The horizontal coordinate of the piece.
	 * @param turn Those turn it is.
	 * @return Returns the set of all positions which result from making a single move (including double jumps) from the given square.
	 */
	private HashSet<Position> generateAllMoves(int i, int j, int turn) {
		HashSet<Position> moves = new HashSet<>();
		if (this.board[i][j].isEmpty()) {
			return moves;
		}

		int nextTurn = (turn == Square.RED) ? Square.BLACK : Square.RED;

		if (this.board[i][j].isRed()) {
			// Check for forward moves.
			if (i - 1 >= 0) {
				if (j - 1 >= 0 && this.board[i - 1][j - 1].isEmpty()) {
					Square[][] tempBoard = deepSquareCopy(this.board);
					tempBoard[i - 1][j - 1] = new Square(tempBoard[i][j]);
					tempBoard[i][j] = new Square(Square.EMPTY, i, j);
					
					int[] parentMove = {i, j, i - 1, j - 1};
					moves.add(new Position(tempBoard, nextTurn, parentMove));
				}
				if (j + 1 < Checkers.BOARD_SIZE && this.board[i - 1][j + 1].isEmpty()) {
					Square[][] tempBoard = deepSquareCopy(this.board);
					tempBoard[i - 1][j + 1] = new Square(tempBoard[i][j]);
					tempBoard[i][j] = new Square(Square.EMPTY, i, j);

					int[] parentMove = {i, j, i - 1, j + 1};
					moves.add(new Position(tempBoard, nextTurn, parentMove));
				}
			}
			
			// Check for forward captures.
			if (i - 2 >= 0) {
				if (j - 2 >= 0 && this.board[i - 2][j - 2].isEmpty() && this.board[i - 1][j - 1].isBlack()) {
					Square[][] tempBoard = deepSquareCopy(this.board);
					tempBoard[i - 2][j - 2] = new Square(tempBoard[i][j]);
					tempBoard[i][j] = new Square(Square.EMPTY, i, j);
					tempBoard[i - 1][j - 1] = new Square(Square.EMPTY, i - 1, j - 1);
					
					int[] parentMove = {i, j, i - 2, j - 2};
					moves.add(new Position(deepSquareCopy(tempBoard), nextTurn, parentMove));

					for (Position p : generateAllMovesDoubleJump(tempBoard, i, j, i - 2, j - 2, turn)) {
						moves.add(p);
					}
				}
				if (j + 2 < Checkers.BOARD_SIZE && this.board[i - 2][j + 2].isEmpty() && this.board[i - 1][j + 1].isBlack()) {
					Square[][] tempBoard = deepSquareCopy(this.board);
					tempBoard[i - 2][j + 2] = new Square(tempBoard[i][j]);
					tempBoard[i][j] = new Square(Square.EMPTY, i, j);
					tempBoard[i - 1][j + 1] = new Square(Square.EMPTY, i - 1, j + 1);

					int[] parentMove = {i, j, i - 2, j + 2};
					moves.add(new Position(deepSquareCopy(tempBoard), nextTurn, parentMove));

					for (Position p : generateAllMovesDoubleJump(tempBoard, i, j, i - 2, j + 2, turn)) {
						moves.add(p);
					}
				}
			}

			// Check for backward moves and captures.
			if (this.board[i][j].isKing()) {
				if (i + 1 < Checkers.BOARD_SIZE) {
					if (j - 1 >= 0 && this.board[i + 1][j - 1].isEmpty()) {
						Square[][] tempBoard = deepSquareCopy(this.board);
						tempBoard[i + 1][j - 1] = new Square(tempBoard[i][j]);
						tempBoard[i][j] = new Square(Square.EMPTY, i, j);
						
						int[] parentMove = {i, j, i + 1, j - 1};
						moves.add(new Position(tempBoard, nextTurn, parentMove));
					}
					if (j + 1 < Checkers.BOARD_SIZE && this.board[i + 1][j + 1].isEmpty()) {
						Square[][] tempBoard = deepSquareCopy(this.board);
						tempBoard[i + 1][j + 1] = new Square(tempBoard[i][j]);
						tempBoard[i][j] = new Square(Square.EMPTY, i, j);

						int[] parentMove = {i, j, i + 1, j + 1};
						moves.add(new Position(tempBoard, nextTurn, parentMove));
					}
				}
				if (i + 2 < Checkers.BOARD_SIZE) {
					if (j - 2 >= 0 && this.board[i + 2][j - 2].isEmpty() && this.board[i + 1][j - 1].isBlack()) {
						Square[][] tempBoard = deepSquareCopy(this.board);
						tempBoard[i + 2][j - 2] = new Square(tempBoard[i][j]);
						tempBoard[i][j] = new Square(Square.EMPTY, i, j);
						tempBoard[i + 1][j - 1] = new Square(Square.EMPTY, i + 1, j - 1);

						int[] parentMove = {i, j, i + 2, j - 2};
						moves.add(new Position(deepSquareCopy(tempBoard), nextTurn, parentMove));

						for (Position p  : generateAllMovesDoubleJump(tempBoard, i, j, i + 2, j - 2, turn)) {
							moves.add(p);
						}
					}
					if (j + 2 < Checkers.BOARD_SIZE && this.board[i + 2][j + 2].isEmpty() && this.board[i + 1][j + 1].isBlack()) {
						Square[][] tempBoard = deepSquareCopy(this.board);
						tempBoard[i + 2][j + 2] = new Square(tempBoard[i][j]);
						tempBoard[i][j] = new Square(Square.EMPTY, i, j);
						tempBoard[i + 1][j + 1] = new Square(Square.EMPTY, i + 1, j + 1);

						int[] parentMove = {i, j, i + 2, j + 2};
						moves.add(new Position(deepSquareCopy(tempBoard), nextTurn, parentMove));

						for (Position p : generateAllMovesDoubleJump(tempBoard, i, j, i + 2, j + 2, turn)) {
							moves.add(p);
						}
					}
				}
			}
		} else {
			// Check for forward moves.
			if (i + 1 < Checkers.BOARD_SIZE) {
				if (j - 1 >= 0 && this.board[i + 1][j - 1].isEmpty()) {
					Square[][] tempBoard = deepSquareCopy(this.board);
					tempBoard[i + 1][j - 1] = new Square(tempBoard[i][j]);
					tempBoard[i][j] = new Square(Square.EMPTY, i, j);
					
					int[] parentMove = {i, j, i + 1, j - 1};
					moves.add(new Position(tempBoard, nextTurn, parentMove));
				}
				if (j + 1 < Checkers.BOARD_SIZE && this.board[i + 1][j + 1].isEmpty()) {
					Square[][] tempBoard = deepSquareCopy(this.board);
					tempBoard[i + 1][j + 1] = new Square(tempBoard[i][j]);
					tempBoard[i][j] = new Square(Square.EMPTY, i, j);

					int[] parentMove = {i, j, i + 1, j + 1};
					moves.add(new Position(tempBoard, nextTurn, parentMove));
				}
			}
			
			// Check for forward captures.
			if (i + 2 < Checkers.BOARD_SIZE) {
				if (j - 2 >= 0 && this.board[i + 2][j - 2].isEmpty() && this.board[i + 1][j - 1].isBlack()) {
					Square[][] tempBoard = deepSquareCopy(this.board);
					tempBoard[i + 2][j - 2] = new Square(tempBoard[i][j]);
					tempBoard[i][j] = new Square(Square.EMPTY, i, j);
					tempBoard[i + 1][j - 1] = new Square(Square.EMPTY, i + 1, j - 1);
					
					int[] parentMove = {i, j, i + 2, j - 2};
					moves.add(new Position(deepSquareCopy(tempBoard), nextTurn, parentMove));

					for (Position p : generateAllMovesDoubleJump(tempBoard, i, j, i + 2, j - 2, turn)) {
						moves.add(p);
					}
				}
				if (j + 2 < Checkers.BOARD_SIZE && this.board[i + 2][j + 2].isEmpty() && this.board[i + 1][j + 1].isBlack()) {
					Square[][] tempBoard = deepSquareCopy(this.board);
					tempBoard[i + 2][j + 2] = new Square(tempBoard[i][j]);
					tempBoard[i][j] = new Square(Square.EMPTY, i, j);
					tempBoard[i + 1][j + 1] = new Square(Square.EMPTY, i + 1, j + 1);

					int[] parentMove = {i, j, i + 2, j + 2};
					moves.add(new Position(deepSquareCopy(tempBoard), nextTurn, parentMove));

					for (Position p : generateAllMovesDoubleJump(tempBoard, i, j, i + 2, j + 2, turn)) {
						moves.add(p);
					}
				}
			}

			// Check for backward moves and captures.
			if (this.board[i][j].isKing()) {
				if (i - 1 >= 0) {
					if (j - 1 >= 0 && this.board[i - 1][j - 1].isEmpty()) {
						Square[][] tempBoard = deepSquareCopy(this.board);
						tempBoard[i - 1][j - 1] = new Square(tempBoard[i][j]);
						tempBoard[i][j] = new Square(Square.EMPTY, i, j);
						
						int[] parentMove = {i, j, i - 1, j - 1};
						moves.add(new Position(tempBoard, nextTurn, parentMove));
					}
					if (j + 1 < Checkers.BOARD_SIZE && this.board[i - 1][j + 1].isEmpty()) {
						Square[][] tempBoard = deepSquareCopy(this.board);
						tempBoard[i - 1][j + 1] = new Square(tempBoard[i][j]);
						tempBoard[i][j] = new Square(Square.EMPTY, i, j);

						int[] parentMove = {i, j, i - 1, j + 1};
						moves.add(new Position(tempBoard, nextTurn, parentMove));
					}
				}
				if (i - 2 < Checkers.BOARD_SIZE) {
					if (j - 2 >= 0 && this.board[i - 2][j - 2].isEmpty() && this.board[i - 1][j - 1].isBlack()) {
						Square[][] tempBoard = deepSquareCopy(this.board);
						tempBoard[i - 2][j - 2] = new Square(tempBoard[i][j]);
						tempBoard[i][j] = new Square(Square.EMPTY, i, j);
						tempBoard[i - 1][j - 1] = new Square(Square.EMPTY, i - 1, j - 1);

						int[] parentMove = {i, j, i - 2, j - 2};
						moves.add(new Position(deepSquareCopy(tempBoard), nextTurn, parentMove));

						for (Position p  : generateAllMovesDoubleJump(tempBoard, i, j, i - 2, j - 2, turn)) {
							moves.add(p);
						}
					}
					if (j + 2 < Checkers.BOARD_SIZE && this.board[i - 2][j + 2].isEmpty() && this.board[i - 1][j + 1].isBlack()) {
						Square[][] tempBoard = deepSquareCopy(this.board);
						tempBoard[i - 2][j + 2] = new Square(tempBoard[i][j]);
						tempBoard[i][j] = new Square(Square.EMPTY, i, j);
						tempBoard[i - 1][j + 1] = new Square(Square.EMPTY, i - 1, j + 1);

						int[] parentMove = {i, j, i - 2, j + 2};
						moves.add(new Position(deepSquareCopy(tempBoard), nextTurn, parentMove));

						for (Position p : generateAllMovesDoubleJump(tempBoard, i, j, i - 2, j + 2, turn)) {
							moves.add(p);
						}
					}
				}
			}
		}

		return moves;
	}

	/**
	 * Recursively generates all possible board configurations resulting from the given board at the given piece, accounting for double 
	 * jumps, and assuming that at any given function call the board is currently undergoing a double jump capture sequence.
	 * @param board The board to use.
	 * @param source_i The vertical coordinate of initial square where the double jump sequence started.
	 * @param source_j The horizontal coordinate of initial square where the double jump sequence started.
	 * @param i The vertical coordinate of the piece jumping.
	 * @param j The horizontal coordinate of the piece jumping.
	 * @return Returns all boards resulting from the ensuing double jump sequence.
	 */
	private HashSet<Position> generateAllMovesDoubleJump(Square[][] board, int source_i, int source_j, int i, int j, int turn) {
		HashSet<Position> moves = new HashSet<>();
		Checkers tempGame = new Checkers(board);
		if (!tempGame.canCaptureForward(i, j, turn) && !tempGame.canCaptureBackward(i, j, turn)) {
			return moves;
		}
		if (board[i][j].isEmpty()) {
			return moves;
		} else if (board[i][j].isRed()) {
			Square[][] nextBoard;
			if (i - 2 >= 0) {
				if (j - 2 >= 0 && this.board[i - 2][j - 2].isEmpty() && this.board[i - 1][j - 1].isBlack()) {
					nextBoard = deepSquareCopy(board);
					nextBoard[i - 2][j - 2] = new Square(nextBoard[i][j]);
					nextBoard[i][j] = new Square(Square.EMPTY, i, j);
					nextBoard[i - 1][j - 1] = new Square(Square.EMPTY, i - 1, j - 1);

					int[] parentMove = {source_i, source_j, i - 2, j - 2};
					moves.add(new Position(nextBoard, turn, parentMove));

					for (Position p : generateAllMovesDoubleJump(nextBoard, source_i, source_j, i - 2, j - 2, turn)) {
						moves.add(p);
					}
				}
				if (j + 2 < Checkers.BOARD_SIZE && this.board[i - 2][j + 2].isEmpty() && this.board[i - 1][j + 1].isBlack()) {
					nextBoard = deepSquareCopy(board);
					nextBoard[i - 2][j + 2] = new Square(nextBoard[i][j]);
					nextBoard[i][j] = new Square(Square.EMPTY, i, j);
					nextBoard[i - 1][j + 1] = new Square(Square.EMPTY, i - 1, j + 1);

					int[] parentMove = {source_i, source_j, i - 2, j + 2};
					moves.add(new Position(nextBoard, turn, parentMove));

					for (Position p : generateAllMovesDoubleJump(nextBoard, source_i, source_j, i - 2, j + 2, turn)) {
						moves.add(p);
					}
				}
			}
			if (i + 2 < Checkers.BOARD_SIZE) {
				if (j - 2 >= 0 && this.board[i + 2][j - 2].isEmpty() && this.board[i + 1][j - 1].isBlack()) {
					nextBoard = deepSquareCopy(board);
					nextBoard[i + 2][j - 2] = new Square(nextBoard[i][j]);
					nextBoard[i][j] = new Square(Square.EMPTY, i, j);
					nextBoard[i - 1][j - 1] = new Square(Square.EMPTY, i - 1, j - 1);

					int[] parentMove = {source_i, source_j, i + 2, j - 2};
					moves.add(new Position(nextBoard, turn, parentMove));

					for (Position p : generateAllMovesDoubleJump(nextBoard, source_i, source_j, i + 2, j - 2, turn)) {
						moves.add(p);
					}	
				}
				if (j + 2 < Checkers.BOARD_SIZE && this.board[i + 2][j + 2].isEmpty() && this.board[i + 1][j + 1].isBlack()) {
					nextBoard = deepSquareCopy(board);
					nextBoard[i + 2][j + 2] = new Square(nextBoard[i][j]);
					nextBoard[i][j] = new Square(Square.EMPTY, i, j);
					nextBoard[i + 1][j + 1] = new Square(Square.EMPTY, i + 1, j + 1);

					int[] parentMove = {source_i, source_j, i + 2, j + 2};
					moves.add(new Position(nextBoard, turn, parentMove));

					for (Position p : generateAllMovesDoubleJump(nextBoard, source_i, source_j, i + 2, j + 2, turn)) {
						moves.add(p);
					}
				}
			}
		} else {
			Square[][] nextBoard;
			if (i - 2 >= 0) {
				if (j - 2 >= 0 && this.board[i - 2][j - 2].isEmpty() && this.board[i - 1][j - 1].isRed()) {
					nextBoard = deepSquareCopy(board);
					nextBoard[i - 2][j - 2] = new Square(nextBoard[i][j]);
					nextBoard[i][j] = new Square(Square.EMPTY, i, j);
					nextBoard[i - 1][j - 1] = new Square(Square.EMPTY, i - 1, j - 1);

					int[] parentMove = {source_i, source_j, i - 2, j - 2};
					moves.add(new Position(nextBoard, turn, parentMove));

					for (Position p : generateAllMovesDoubleJump(nextBoard, source_i, source_j, i - 2, j - 2, turn)) {
						moves.add(p);
					}
				}
				if (j + 2 < Checkers.BOARD_SIZE && this.board[i - 2][j + 2].isEmpty() && this.board[i - 1][j + 1].isRed()) {
					nextBoard = deepSquareCopy(board);
					nextBoard[i - 2][j + 2] = new Square(nextBoard[i][j]);
					nextBoard[i][j] = new Square(Square.EMPTY, i, j);
					nextBoard[i - 1][j + 1] = new Square(Square.EMPTY, i - 1, j + 1);

					int[] parentMove = {source_i, source_j, i - 2, j + 2};
					moves.add(new Position(nextBoard, turn, parentMove));

					for (Position p : generateAllMovesDoubleJump(nextBoard, source_i, source_j, i - 2, j + 2, turn)) {
						moves.add(p);
					}
				}
			}
			if (i + 2 < Checkers.BOARD_SIZE) {
				if (j - 2 >= 0 && this.board[i + 2][j - 2].isEmpty() && this.board[i + 1][j - 1].isRed()) {
					nextBoard = deepSquareCopy(board);
					nextBoard[i + 2][j - 2] = new Square(nextBoard[i][j]);
					nextBoard[i][j] = new Square(Square.EMPTY, i, j);
					nextBoard[i - 1][j - 1] = new Square(Square.EMPTY, i - 1, j - 1);

					int[] parentMove = {source_i, source_j, i + 2, j - 2};
					moves.add(new Position(nextBoard, turn, parentMove));

					for (Position p : generateAllMovesDoubleJump(nextBoard, source_i, source_j, i + 2, j - 2, turn)) {
						moves.add(p);
					}	
				}
				if (j + 2 < Checkers.BOARD_SIZE && this.board[i + 2][j + 2].isEmpty() && this.board[i + 1][j + 1].isRed()) {
					nextBoard = deepSquareCopy(board);
					nextBoard[i + 2][j + 2] = new Square(nextBoard[i][j]);
					nextBoard[i][j] = new Square(Square.EMPTY, i, j);
					nextBoard[i + 1][j + 1] = new Square(Square.EMPTY, i + 1, j + 1);

					int[] parentMove = {source_i, source_j, i + 2, j + 2};
					moves.add(new Position(nextBoard, turn, parentMove));

					for (Position p : generateAllMovesDoubleJump(nextBoard, source_i, source_j, i + 2, j + 2, turn)) {
						moves.add(p);
					}
				}
			}
		}

		return moves;
	}

	/**
	 * @param board The 2-D Square array to copy.
	 * @return Returns a deep copy of the given board.
	 */
	private Square[][] deepSquareCopy(Square[][] board) {
		Square[][] temp = new Square[board.length][];
		for (int i = 0; i < temp.length; i++) {
			temp[i] = new Square[board[i].length];
			for (int j = 0; j < temp[i].length; j++) {
				temp[i][j] = new Square(board[i][j]);
			}
		}

		return temp;
	}
}