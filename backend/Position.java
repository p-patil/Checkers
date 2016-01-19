import java.util.HashSet;
import java.util.Arrays;
import java.util.HashMap;
import java.io.Serializable;

/**
 * Class representing a node in the game tree used in generating endgame tablebases and running the minimax algorithm.
 */
public class Position implements Serializable {
	private static final int UNKNOWN_VALUE = -2; // Placeholder value to represent an unknown value for this position.
	public static final int DRAW = 0; // Placeholder value to represent a draw.
	public static final int WIN = 1; // Placeholder value to represent a red win.
	public static final int LOSS = -1; // Placeholder value to represent a black win.
	private static final int[][][] zobristHashKeys; // Lookup array for Zobrist hashing - contains a random 32-bit number for each 
													// position-piece combination. Initialized in static block.

	/**
	 * Initialize Zobrist Hash array to uniformly random integers for every piece-position combination. Makes use of class MersenneTwisterFast.java
	 * written by Sean Luke. Copyright:
	 * Copyright (c) 2003 by Sean Luke. <br>
 	 * Portions copyright (c) 1993 by Michael Lecuyer. <br>
 	 * All rights reserved. <br>
 	 */
	static {
		zobristHashKeys = new int[Checkers.BOARD_SIZE][Checkers.BOARD_SIZE][5];
		MersenneTwisterFast m = new MersenneTwisterFast();
		for (int i = 0; i < zobristHashKeys.length; i++) {
			for (int j = 0; j < zobristHashKeys[i].length; j++) {
				if ((i + j) % 2 == 1) {
					zobristHashKeys[i][j][Square.RED - Square.BLACK_KING] = m.nextInt();
					zobristHashKeys[i][j][Square.BLACK - Square.BLACK_KING] = m.nextInt();
					zobristHashKeys[i][j][Square.RED_KING - Square.BLACK_KING] = m.nextInt();
					zobristHashKeys[i][j][Square.BLACK_KING - Square.BLACK_KING] = m.nextInt();
				} else {
					zobristHashKeys[i][j][Square.RED - Square.BLACK_KING] = 0;
					zobristHashKeys[i][j][Square.BLACK - Square.BLACK_KING] = 0;
					zobristHashKeys[i][j][Square.RED_KING - Square.BLACK_KING] = 0;
					zobristHashKeys[i][j][Square.BLACK_KING - Square.BLACK_KING] = 0;
				}
			}
		}
	}

	public HashMap<Position, int[]> successors; // The successor positions to this position, defined as all positions one forward move away from this position.
	// public int[] parentMove; // Stores the move that bridges the parent of this Position to this position. Its length may be greater than 4 if
						     // the transition is a double jump capture sequence. There are many possible parents; this is intended to be the best.
	public HashMap<Position, Integer> successorScores;
	public final Square[][] board; // The board configuration represented by this position object.
	public int turn; // Whose turn it is in this position.
	private int redCount;
	private int blackCount;

	/**
	 * Basic constructor.
	 * @param game The game with the position to use.
	 * @param parentMove The optimal move for reaching this Position from it's parent.
	 */
	public Position(Checkers game) {
		this.board = deepSquareCopy(game.board);
		this.redCount = 0;
		this.blackCount = 0;
		for (int i = 0; i < this.board.length; i++) {
			for (int j = 0; j < this.board[i].length; j++) {
				if (this.board[i][j].isRed()) {
					this.redCount++;
				} else if (this.board[i][j].isBlack()) {
					this.blackCount++;
				}
			}
		}
		this.turn = game.getCurrentTurn();
		if (game.isGameOver() != 0) {
			this.successors = null;
			this.successorScores = null;
		} else {
			this.successors = new HashMap<>();
			this.successorScores = new HashMap<>();
		}
	}

	/**
	 * Constructor that builds a position given a checkers board.
	 * @param board The board to use.
	 * @param parentMove The optimal move for reaching this Position from it's parent.
	 */
	public Position(Square[][] board, int turn) {
		this.board = deepSquareCopy(board);
		this.redCount = 0;
		this.blackCount = 0;
		for (int i = 0; i < this.board.length; i++) {
			for (int j = 0; j < this.board[i].length; j++) {
				if (this.board[i][j].isRed()) {
					this.redCount++;
				} else if (this.board[i][j].isBlack()) {
					this.blackCount++;
				}
			}
		}
		this.turn = turn;
		if ((new Checkers(board, turn)).isGameOver() != 0) { // Game is over, so this position is a leaf.
			this.successors = null;
			this.successorScores = null;
		} else {
			this.successors = new HashMap<>();
			this.successorScores = new HashMap<>();
		}
	}

	/**
	 * Initializes this Position object's successors.
	 */
	public void generateSuccessors() {
		if (this.successors == null || !this.successors.isEmpty()) {
			return;
		}
		
		this.successorScores = new HashMap<>();

		// Iterate over all pieces on the given side and add all boards resulting from all valid moves the piece can make to the set.
		for (int i = 0; i < this.board.length; i++) {
			for (int j = 0; j < this.board[i].length; j++) {
				if ((this.board[i][j].isRed() && this.turn == Square.RED) || (this.board[i][j].isBlack() && this.turn == Square.BLACK)) {
					for (Object[] p : generateAllMoves(this.board, i, j, this.turn)) {
						this.successors.put((Position) p[0], (int[]) p[1]);
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
	 * The static position evaluation function used in the minimax algorithm. Gives one point for all remaining pieces, and 2 for kings.
	 * @param position The position to evaluate.
	 * @param turn Whose turn to evaluate on.
	 * @return The evaluation value.
	 */
	public int evaluationFunction(int turn) {
		// Simple evaluation function based on number of pieces still standing.
		int val = 0;
		for (int i = 0; i < this.board.length; i++) {
			for (int j = 0; j < this.board[i].length; j++) {
				if (!this.board[i][j].isEmpty()) {
					if ((this.board[i][j].isRed() && turn == Square.RED) || (this.board[i][j].isBlack() && turn == Square.BLACK)) {
						if (this.board[i][j].isKing()) {
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
	 * @return Returns the number of pieces on the board.
	 */
	public int pieceCount() {
		return this.redCount + this.blackCount;	
	}

	/**
	 * @return Returns the number of black pieces on the board.
	 */
	public int redPieceCount() {
		return this.redCount;
	}

	/**
	 * @return Returns the number of black pieces on the board.
	 */
	public int blackPieceCount() {
		return this.blackCount;
	}

	@Override
	public boolean equals(Object obj) {
		Position p = (Position) obj;

		// if (this.turn != p.turn || this.board.length != p.board.length) {
		// 	return false;
		// }

		// for (int i = 0; i < this.board.length; i++) {
		// 	if (this.board[i].length != p.board[i].length) {
		// 		return false;
		// 	}

		// 	for (int j = 0; j < this.board[i].length; j++) {
		// 		if (!this.board[i][j].equals(p.board[i][j])) {
		// 			return false;
		// 		}
		// 	}
		// }

		// return true;

		return (this.turn == p.turn) && (this.toString().equals(p.toString()));
	}

	@Override
	/**
	 * Implmementation of Zobrist hashing, slightly modified to account for the turn attribute.
	 */
	public int hashCode() {
		int hash = 0;

		for (int i = 0; i < this.board.length; i++) {
			for (int j = 0; j < this.board[i].length; j++) {
				if (!this.board[i][j].isEmpty()) {
					hash ^= zobristHashKeys[i][j][this.board[i][j].getState() - Square.BLACK_KING];
				}
			}
		}

		return hash * (this.turn == Square.RED ? 1 : -1);
	}

	@Override
	public String toString() {
		String s = "";

		if (this.board.length < 10) {
			s += "  ";
		} else {
			s += "   ";
		}
		for (int i = 0; i < this.board.length; i++) {
			s += i + " ";
		}
		s += "\n";
		for (int i = 0; i < this.board.length; i++) {
			if (this.board.length < 10) {
				s += i + " ";
			} else {
				if (i < 10) {
					s += i + "  ";
				} else if (i < 100) {
					s += i + " ";
				}
			}
			for (int j = 0; j < this.board.length; j++) {
				if (this.board[i][j].isEmpty()) {
					s += "  ";
				} else if (this.board[i][j].isRed()) {
					if (this.board[i][j].isKing()) {
						s += "R ";
					} else {
						s += "r ";
					}
				} else {
					if (this.board[i][j].isKing()) {
						s += "B ";
					} else {
						s += "b ";
					}
				}
			}
			s += "|\n";
		}
		for (int i = 0; i <= this.board.length; i++) {
			s += "--";
		}
		
		return s;
	}

	// Helper methods below this line.

	/**
	 * Generates all board configurations obtained by iterating over all possible moves of the piece at [i, j] on this Position's board.
	 * @param i The vertical coordinate of the piece.
	 * @param j The horizontal coordinate of the piece.
	 * @param turn Those turn it is.
	 * @return Returns the set of all positions which result from making a single move (including double jumps) from the given square.
	 */
	private HashSet<Object[]> generateAllMoves(Square[][] board, int i, int j, int turn) {
		HashSet<Object[]> moves = new HashSet<>();
		if (board[i][j].isEmpty()) {
			return moves;
		}

		int nextTurn = (turn == Square.RED) ? Square.BLACK : Square.RED;

		if (board[i][j].isRed()) {
			// Check for forward moves.
			if (i - 1 >= 0) {
				if (j - 1 >= 0 && board[i - 1][j - 1].isEmpty()) {
					Square[][] tempBoard = deepSquareCopy(board);
					tempBoard[i - 1][j - 1] = new Square(tempBoard[i][j]);
					// If the move is to the enemy home rank, king the piece.
					if (i - 1 == 0) {
						tempBoard[i - 1][j - 1].makeKing();
					}
					tempBoard[i][j].setEmpty();
					
					int[] parentMove = {i, j, i - 1, j - 1};
					Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
					moves.add(move);
				}
				if (j + 1 < Checkers.BOARD_SIZE && board[i - 1][j + 1].isEmpty()) {
					Square[][] tempBoard = deepSquareCopy(board);
					tempBoard[i - 1][j + 1] = new Square(tempBoard[i][j]);
					if (i - 1 == 0) {
						tempBoard[i - 1][j + 1].makeKing();
					}
					tempBoard[i][j].setEmpty();

					int[] parentMove = {i, j, i - 1, j + 1};
					Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
					moves.add(move);
				}
			}
			
			// Check for forward captures.
			if (i - 2 >= 0) {
				if (j - 2 >= 0 && board[i - 2][j - 2].isEmpty() && board[i - 1][j - 1].isBlack()) {
					Square[][] tempBoard = deepSquareCopy(board);
					tempBoard[i - 2][j - 2] = new Square(tempBoard[i][j]);
					// If the capture lands on the enemy home rank, king the piece.
					if (i - 2 == 0) {
						tempBoard[i - 2][j - 2].makeKing();
					}
					tempBoard[i][j].setEmpty();
					tempBoard[i - 1][j - 1].setEmpty();
					
					int[] parentMove = {i, j, i - 2, j - 2};
					Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
					moves.add(move);

					for (Object[] double_jump_move : generateAllMovesDoubleJump(tempBoard, i - 2, j - 2, parentMove, turn)) {
						moves.add(double_jump_move);
					}
				}
				if (j + 2 < Checkers.BOARD_SIZE && board[i - 2][j + 2].isEmpty() && board[i - 1][j + 1].isBlack()) {
					Square[][] tempBoard = deepSquareCopy(board);
					tempBoard[i - 2][j + 2] = new Square(tempBoard[i][j]);
					if (i - 2 == 0) {
						tempBoard[i - 2][j + 2].makeKing();
					}
					tempBoard[i][j].setEmpty();
					tempBoard[i - 1][j + 1].setEmpty();

					int[] parentMove = {i, j, i - 2, j + 2};
					Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
					moves.add(move);

					for (Object[] double_jump_move : generateAllMovesDoubleJump(tempBoard, i - 2, j + 2, parentMove, turn)) {
						moves.add(double_jump_move);
					}
				}
			}

			// Check for backward moves and captures.
			if (board[i][j].isKing()) {
				if (i + 1 < Checkers.BOARD_SIZE) {
					if (j - 1 >= 0 && board[i + 1][j - 1].isEmpty()) {
						Square[][] tempBoard = deepSquareCopy(board);
						tempBoard[i + 1][j - 1] = new Square(tempBoard[i][j]);
						tempBoard[i][j].setEmpty();
						
						int[] parentMove = {i, j, i + 1, j - 1};
						Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
						moves.add(move);

					}
					if (j + 1 < Checkers.BOARD_SIZE && board[i + 1][j + 1].isEmpty()) {
						Square[][] tempBoard = deepSquareCopy(board);
						tempBoard[i + 1][j + 1] = new Square(tempBoard[i][j]);
						tempBoard[i][j].setEmpty();

						int[] parentMove = {i, j, i + 1, j + 1};
						Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
						moves.add(move);

					}
				}
				if (i + 2 < Checkers.BOARD_SIZE) {
					if (j - 2 >= 0 && board[i + 2][j - 2].isEmpty() && board[i + 1][j - 1].isBlack()) {
						Square[][] tempBoard = deepSquareCopy(board);
						tempBoard[i + 2][j - 2] = new Square(tempBoard[i][j]);
						tempBoard[i][j].setEmpty();
						tempBoard[i + 1][j - 1].setEmpty();

						int[] parentMove = {i, j, i + 2, j - 2};
						Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
						moves.add(move);

						for (Object[] double_jump_move  : generateAllMovesDoubleJump(tempBoard, i + 2, j - 2, parentMove, turn)) {
							moves.add(double_jump_move);
						}
					}
					if (j + 2 < Checkers.BOARD_SIZE && board[i + 2][j + 2].isEmpty() && board[i + 1][j + 1].isBlack()) {
						Square[][] tempBoard = deepSquareCopy(board);
						tempBoard[i + 2][j + 2] = new Square(tempBoard[i][j]);
						tempBoard[i][j].setEmpty();
						tempBoard[i + 1][j + 1].setEmpty();

						int[] parentMove = {i, j, i + 2, j + 2};
						Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
						moves.add(move);

						for (Object[] double_jump_move : generateAllMovesDoubleJump(tempBoard, i + 2, j + 2, parentMove, turn)) {
							moves.add(double_jump_move);
						}
					}
				}
			}
		} else {
			// Check for forward moves.
			if (i + 1 < Checkers.BOARD_SIZE) {
				if (j - 1 >= 0 && board[i + 1][j - 1].isEmpty()) {
					Square[][] tempBoard = deepSquareCopy(board);
					tempBoard[i + 1][j - 1] = new Square(tempBoard[i][j]);
					if (i + 1 == Checkers.BOARD_SIZE - 1) {
						tempBoard[i + 1][j - 1].makeKing();
					}
					tempBoard[i][j].setEmpty();
					
					int[] parentMove = {i, j, i + 1, j - 1};
					Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
					moves.add(move);
				}
				if (j + 1 < Checkers.BOARD_SIZE && board[i + 1][j + 1].isEmpty()) {
					Square[][] tempBoard = deepSquareCopy(board);
					tempBoard[i + 1][j + 1] = new Square(tempBoard[i][j]);
					if (i + 1 == Checkers.BOARD_SIZE - 1) {
						tempBoard[i + 1][j + 1] = new Square(tempBoard[i][j]);
					}
					tempBoard[i][j].setEmpty();

					int[] parentMove = {i, j, i + 1, j + 1};
					Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
					moves.add(move);
				}
			}
			
			// Check for forward captures.
			if (i + 2 < Checkers.BOARD_SIZE) {
				if (j - 2 >= 0 && board[i + 2][j - 2].isEmpty() && board[i + 1][j - 1].isRed()) {
					Square[][] tempBoard = deepSquareCopy(board);
					tempBoard[i + 2][j - 2] = new Square(tempBoard[i][j]);
					if (i + 2 == Checkers.BOARD_SIZE - 1) {
						tempBoard[i + 2][j - 2].makeKing();
					}
					tempBoard[i][j].setEmpty();
					tempBoard[i + 1][j - 1].setEmpty();
					
					int[] parentMove = {i, j, i + 2, j - 2};
					Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
					moves.add(move);

					for (Object[] double_jump_move : generateAllMovesDoubleJump(tempBoard, i + 2, j - 2, parentMove, turn)) {
						moves.add(double_jump_move);
					}
				}
				if (j + 2 < Checkers.BOARD_SIZE && board[i + 2][j + 2].isEmpty() && board[i + 1][j + 1].isRed()) {
					Square[][] tempBoard = deepSquareCopy(board);
					tempBoard[i + 2][j + 2] = new Square(tempBoard[i][j]);
					if (i + 2 == Checkers.BOARD_SIZE - 1) {
						tempBoard[i + 2][j + 2].makeKing();						
					}
					tempBoard[i][j].setEmpty();
					tempBoard[i + 1][j + 1].setEmpty();

					int[] parentMove = {i, j, i + 2, j + 2};
					Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
					moves.add(move);

					for (Object[] double_jump_move : generateAllMovesDoubleJump(tempBoard, i + 2, j + 2, parentMove, turn)) {
						moves.add(double_jump_move);
					}
				}
			}

			// Check for backward moves and captures.
			if (board[i][j].isKing()) {
				// Moves.
				if (i - 1 >= 0) {
					if (j - 1 >= 0 && board[i - 1][j - 1].isEmpty()) {
						Square[][] tempBoard = deepSquareCopy(board);
						tempBoard[i - 1][j - 1] = new Square(tempBoard[i][j]);
						tempBoard[i][j].setEmpty();
						
						int[] parentMove = {i, j, i - 1, j - 1};
						Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
						moves.add(move);
					}
					if (j + 1 < Checkers.BOARD_SIZE && board[i - 1][j + 1].isEmpty()) {
						Square[][] tempBoard = deepSquareCopy(board);
						tempBoard[i - 1][j + 1] = new Square(tempBoard[i][j]);
						tempBoard[i][j].setEmpty();

						int[] parentMove = {i, j, i - 1, j + 1};
						Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
						moves.add(move);
					}
				}
				// Captures.
				if (i - 2 >= 0) {
					if (j - 2 >= 0 && board[i - 2][j - 2].isEmpty() && board[i - 1][j - 1].isRed()) {
						Square[][] tempBoard = deepSquareCopy(board);
						tempBoard[i - 2][j - 2] = new Square(tempBoard[i][j]);
						tempBoard[i][j].setEmpty();
						tempBoard[i - 1][j - 1].setEmpty();

						int[] parentMove = {i, j, i - 2, j - 2};
						Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
						moves.add(move);

						for (Object[] double_jump_move  : generateAllMovesDoubleJump(tempBoard, i - 2, j - 2, parentMove, turn)) {
							moves.add(double_jump_move);
						}
					}
					if (j + 2 < Checkers.BOARD_SIZE && board[i - 2][j + 2].isEmpty() && board[i - 1][j + 1].isRed()) {
						Square[][] tempBoard = deepSquareCopy(board);
						tempBoard[i - 2][j + 2] = new Square(tempBoard[i][j]);
						tempBoard[i][j].setEmpty();
						tempBoard[i - 1][j + 1].setEmpty();

						int[] parentMove = {i, j, i - 2, j + 2};
						Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
						moves.add(move);

						for (Object[] double_jump_move : generateAllMovesDoubleJump(tempBoard, i - 2, j + 2, parentMove, turn)) {
							moves.add(double_jump_move);
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
	 * @param i The vertical coordinate of the piece jumping.
	 * @param j The horizontal coordinate of the piece jumping.
	 * @param currParentMove Stores the double jump path to the present position.
	 * @param turn Whose turn it is.
	 * @return Returns all boards resulting from the ensuing double jump sequence.
	 */
	private HashSet<Object[]> generateAllMovesDoubleJump(Square[][] board, int i, int j, int[] currParentMove, int turn) {
		HashSet<Object[]> moves = new HashSet<>();
		if (Checkers.numForwardCaptures(board, i, j, turn) == 0 && Checkers.numBackwardCaptures(board, i, j, turn) == 0) {
			return moves;
		}
		if (board[i][j].isEmpty()) {
			return moves;
		} else if (board[i][j].isRed()) {
			Square[][] nextBoard;
			if (i - 2 >= 0) {
				if (j - 2 >= 0 && board[i - 2][j - 2].isEmpty() && board[i - 1][j - 1].isBlack()) {
					nextBoard = deepSquareCopy(board);
					nextBoard[i - 2][j - 2] = new Square(nextBoard[i][j]);
					if (i - 2 == 0) {
						nextBoard[i - 2][j - 2].makeKing();
					}
					nextBoard[i][j].setEmpty();
					nextBoard[i - 1][j - 1].setEmpty();

					int[] parentMove = Arrays.copyOf(currParentMove, currParentMove.length + 4);
					parentMove[parentMove.length - 4] = i;
					parentMove[parentMove.length - 3] = j;
					parentMove[parentMove.length - 2] = i - 2;
					parentMove[parentMove.length - 1] = j - 2;
					Object[] move = {(new Position(nextBoard, turn)), parentMove};
					moves.add(move);

					for (Object[] double_jump_move : generateAllMovesDoubleJump(nextBoard, i - 2, j - 2, parentMove, turn)) {
						moves.add(double_jump_move);
					}
				}
				if (j + 2 < Checkers.BOARD_SIZE && board[i - 2][j + 2].isEmpty() && board[i - 1][j + 1].isBlack()) {
					nextBoard = deepSquareCopy(board);
					nextBoard[i - 2][j + 2] = new Square(nextBoard[i][j]);
					if (i - 2 == 0) {
						nextBoard[i - 2][j + 2].makeKing();
					}
					nextBoard[i][j].setEmpty();
					nextBoard[i - 1][j + 1].setEmpty();

					int[] parentMove = Arrays.copyOf(currParentMove, currParentMove.length + 4);
					parentMove[parentMove.length - 4] = i;
					parentMove[parentMove.length - 3] = j;
					parentMove[parentMove.length - 2] = i - 2;
					parentMove[parentMove.length - 1] = j + 2;
					Object[] move = {(new Position(nextBoard, turn)), parentMove};
					moves.add(move);

					for (Object[] double_jump_move : generateAllMovesDoubleJump(nextBoard, i - 2, j + 2, parentMove, turn)) {
						moves.add(double_jump_move);
					}
				}
			}
			if (i + 2 < Checkers.BOARD_SIZE) {
				if (j - 2 >= 0 && board[i + 2][j - 2].isEmpty() && board[i + 1][j - 1].isBlack()) {
					nextBoard = deepSquareCopy(board);
					nextBoard[i + 2][j - 2] = new Square(nextBoard[i][j]);
					nextBoard[i][j].setEmpty();
					nextBoard[i + 1][j - 1].setEmpty();

					int[] parentMove = Arrays.copyOf(currParentMove, currParentMove.length + 4);
					parentMove[parentMove.length - 4] = i;
					parentMove[parentMove.length - 3] = j;
					parentMove[parentMove.length - 2] = i + 2;
					parentMove[parentMove.length - 1] = j - 2;
					Object[] move = {(new Position(nextBoard, turn)), parentMove};
					moves.add(move);

					for (Object[] double_jump_move : generateAllMovesDoubleJump(nextBoard, i + 2, j - 2, parentMove, turn)) {
						moves.add(double_jump_move);
					}	
				}
				if (j + 2 < Checkers.BOARD_SIZE && board[i + 2][j + 2].isEmpty() && board[i + 1][j + 1].isBlack()) {
					nextBoard = deepSquareCopy(board);
					nextBoard[i + 2][j + 2] = new Square(nextBoard[i][j]);
					nextBoard[i][j].setEmpty();
					nextBoard[i + 1][j + 1].setEmpty();

					int[] parentMove = Arrays.copyOf(currParentMove, currParentMove.length + 4);
					parentMove[parentMove.length - 4] = i;
					parentMove[parentMove.length - 3] = j;
					parentMove[parentMove.length - 2] = i + 2;
					parentMove[parentMove.length - 1] = j + 2;
					Object[] move = {(new Position(nextBoard, turn)), parentMove};
					moves.add(move);

					for (Object[] double_jump_move : generateAllMovesDoubleJump(nextBoard, i + 2, j + 2, parentMove, turn)) {
						moves.add(double_jump_move);
					}
				}
			}
		} else {
			Square[][] nextBoard;
			if (i - 2 >= 0) {
				if (j - 2 >= 0 && board[i - 2][j - 2].isEmpty() && board[i - 1][j - 1].isRed()) {
					nextBoard = deepSquareCopy(board);
					nextBoard[i - 2][j - 2] = new Square(nextBoard[i][j]);
					nextBoard[i][j].setEmpty();
					nextBoard[i - 1][j - 1].setEmpty();

					int[] parentMove = Arrays.copyOf(currParentMove, currParentMove.length + 4);
					parentMove[parentMove.length - 4] = i;
					parentMove[parentMove.length - 3] = j;
					parentMove[parentMove.length - 2] = i - 2;
					parentMove[parentMove.length - 1] = j - 2;
					Object[] move = {(new Position(nextBoard, turn)), parentMove};
					moves.add(move);

					for (Object[] double_jump_move : generateAllMovesDoubleJump(nextBoard, i - 2, j - 2, parentMove, turn)) {
						moves.add(double_jump_move);
					}
				}
				if (j + 2 < Checkers.BOARD_SIZE && board[i - 2][j + 2].isEmpty() && board[i - 1][j + 1].isRed()) {
					nextBoard = deepSquareCopy(board);
					nextBoard[i - 2][j + 2] = new Square(nextBoard[i][j]);
					nextBoard[i][j].setEmpty();
					nextBoard[i - 1][j + 1].setEmpty();

					int[] parentMove = Arrays.copyOf(currParentMove, currParentMove.length + 4);
					parentMove[parentMove.length - 4] = i;
					parentMove[parentMove.length - 3] = j;
					parentMove[parentMove.length - 2] = i - 2;
					parentMove[parentMove.length - 1] = j + 2;
					Object[] move = {(new Position(nextBoard, turn)), parentMove};
					moves.add(move);

					for (Object[] double_jump_move : generateAllMovesDoubleJump(nextBoard, i - 2, j + 2, parentMove, turn)) {
						moves.add(double_jump_move);
					}
				}
			}
			if (i + 2 < Checkers.BOARD_SIZE) {
				if (j - 2 >= 0 && board[i + 2][j - 2].isEmpty() && board[i + 1][j - 1].isRed()) {
					nextBoard = deepSquareCopy(board);
					nextBoard[i + 2][j - 2] = new Square(nextBoard[i][j]);
					if (i + 2 == Checkers.BOARD_SIZE - 1) {
						nextBoard[i + 2][j - 2].makeKing();						
					}
					nextBoard[i][j].setEmpty();
					nextBoard[i + 1][j - 1].setEmpty();

					int[] parentMove = Arrays.copyOf(currParentMove, currParentMove.length + 4);
					parentMove[parentMove.length - 4] = i;
					parentMove[parentMove.length - 3] = j;
					parentMove[parentMove.length - 2] = i + 2;
					parentMove[parentMove.length - 1] = j - 2;
					Object[] move = {(new Position(nextBoard, turn)), parentMove};
					moves.add(move);

					for (Object[] double_jump_move : generateAllMovesDoubleJump(nextBoard, i + 2, j - 2, parentMove, turn)) {
						moves.add(double_jump_move);
					}	
				}
				if (j + 2 < Checkers.BOARD_SIZE && board[i + 2][j + 2].isEmpty() && board[i + 1][j + 1].isRed()) {
					nextBoard = deepSquareCopy(board);
					nextBoard[i + 2][j + 2] = new Square(nextBoard[i][j]);
					if (i + 2 == Checkers.BOARD_SIZE - 1) {
						nextBoard[i + 2][j + 2].makeKing();						
					}
					nextBoard[i][j].setEmpty();
					nextBoard[i + 1][j + 1].setEmpty();

					int[] parentMove = Arrays.copyOf(currParentMove, currParentMove.length + 4);
					parentMove[parentMove.length - 4] = i;
					parentMove[parentMove.length - 3] = j;
					parentMove[parentMove.length - 2] = i + 2;
					parentMove[parentMove.length - 1] = j + 2;
					Object[] move = {(new Position(nextBoard, turn)), parentMove};
					moves.add(move);

					for (Object[] double_jump_move : generateAllMovesDoubleJump(nextBoard, i + 2, j + 2, parentMove, turn)) {
						moves.add(double_jump_move);
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
	public static Square[][] deepSquareCopy(Square[][] board) {
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