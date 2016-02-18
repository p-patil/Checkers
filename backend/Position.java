import java.util.HashSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.io.Serializable;

/**
 * Class representing a node in the game tree used in generating endgame tablebases and running the minimax algorithm.
 */
public class Position implements Serializable {
	private static final int UNKNOWN_VALUE = -2; // Placeholder value to represent an unknown value for this position.
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
	public HashMap<Position, Integer> successorScores; // Maps successors to score values, which is its minimum distance to a winning state. Doesn't contain nodes
													   // with no path to any winning state.
	public final Square[][] board; // The board configuration represented by this position object.
	public int turn; // Whose turn it is in this position.
	private int redCount; // Number of rede pieces.
	private int blackCount; // Number of black pieces.
	private int state; // The state of this position. Legend: 0 - game not over, 1 - red win, -1 - black win, 2 - draw

	/**
	 * Basic constructor that initializes a board in standard starting position.
	 */
	public Position() {
		if (Checkers.BOARD_SIZE % 2 != 0) {
			System.out.println("ERROR - Board size must be even.");
			System.exit(0);
		}

		this.turn = Square.RED;
		this.redCount = 0;
		this.blackCount = 0;
		this.state = 0;
		this.board = new Square[Checkers.BOARD_SIZE][Checkers.BOARD_SIZE];

		for (int i = 0; i < Checkers.BOARD_SIZE; i++) {
			for (int j = 0; j < Checkers.BOARD_SIZE; j++) {
				if (i < (Checkers.BOARD_SIZE - 2) / 2) {
					if ((i + j) % 2 != 0) {
						this.board[i][j] = new Square(Square.BLACK, i, j);
						this.blackCount++;
					} else {
						this.board[i][j] = new Square(Square.EMPTY, i, j);
					}
				} else if (i < (Checkers.BOARD_SIZE + 2) / 2) {
						this.board[i][j] = new Square(Square.EMPTY, i, j);
				} else {
					if ((i + j) % 2 != 0) {
						this.board[i][j] = new Square(Square.RED, i, j);
						this.redCount++;
					} else {
						this.board[i][j] = new Square(Square.EMPTY, i, j);
					}
				}
			}
		}

		this.successors = new HashMap<>();
		this.successorScores = new HashMap<>();
	}

	/**
	 * Constructor that builds a position given a checkers board.
	 * @param board The board to use.
	 * @param parentMove The optimal move for reaching this Position from it's parent.
	 */
	public Position(Square[][] board, int turn) {
		this.board = new Square[board.length][];
		this.redCount = 0;
		this.blackCount = 0;
		for (int i = 0; i < board.length; i++) {
			this.board[i] = new Square[board[i].length];
			for (int j = 0; j < board[i].length; j++) {
				this.board[i][j] = new Square(board[i][j]);
				if (board[i][j].isRed()) {
					this.redCount++;
				} else if (board[i][j].isBlack()) {
					this.blackCount++;
				}
			}
		}
		this.turn = turn;
		this.state = isGameOver(board, this.board.length, turn);
		if (this.state != 0) {
			this.successors = null;
			this.successorScores = null;
		} else {
			this.successors = new HashMap<>();
			this.successorScores = new HashMap<>();
		}
	}

	/**
	 * Constructor that uses the inputted position's board configuration, and the inputted turn. Used for optimization purposes, since rather
	 * than initializing this.board to a deep copy of p.board, the same reference used for p.board is used for this.board, which is fine since
	 * board is a final variable.
	 * @param p The position to copy.
	 * @param turn Whose turn it is.
	 */
	public Position(Position p, int turn) {
		this.board = p.board;
		this.redCount = p.redPieceCount();
		this.blackCount = p.blackPieceCount();
		this.turn = turn;
		this.state = p.getState();

		if (this.state != 0) {
			this.successors = null;
			this.successorScores = null;
		} else {
			this.successors = new HashMap<>();
			this.successorScores = new HashMap<>();
		}
	}

	/**
	 * Initializes a random position using the Random object r. The initialized position is not guaranteed to be reachable through legal play.
	 * @param r The Random object used to generate the board.
	 * @param board_dim Dimensions of the board.
	 */
	public Position(Random r, int board_dim) {
		this.board = new Square[board_dim][board_dim];
		this.redCount = 0;
		this.blackCount = 0;
		int max_pieces = (board_dim / 2 - 1) * (board_dim / 2), val;
		for (int i = 0; i < board.length; i++) {
			for (int j = 0; j < board[i].length; j++) {
				if ((i + j) % 2 == 1) {
					val = r.nextInt(3);
					if (val == 0) {
						if (this.redCount + 1 <= max_pieces) {
							if (r.nextInt(2) == 0) {
								this.board[i][j] = new Square(Square.RED, i, j);
							} else {
								this.board[i][j] = new Square(Square.RED_KING, i, j);
							}
						}
					} else if (val == 1) {
						if (this.blackCount + 1 <= max_pieces) {
							if (r.nextInt(2) == 0) {
								this.board[i][j] = new Square(Square.BLACK, i, j);
							} else {
								this.board[i][j] = new Square(Square.BLACK_KING, i, j);
							}							
						}
					} else {
						this.board[i][j] = new Square(Square.EMPTY, i, j);						
					}
				} else {
					this.board[i][j] = new Square(Square.EMPTY, i, j);
				}
			}
		}
		this.turn = (r.nextInt(2) == 0) ? Square.RED : Square.BLACK;
		this.state = isGameOver(this.board, this.board.length, this.turn);
		if (this.state != 0) {
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
					for (Object[] p : generateAllMoves(this.board, this.board.length, i, j, this.turn)) {
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
	 * Returns the state of the position characterized by the given board and turn, in accordance with the legend above.
	 * @param board The board to use.
	 * @param turn Whose turn it is.
	 * @return Returns the outcome of the game, or if the game is still going. 
	 */
	public static int isGameOver(Square[][] board, int board_size, int turn) {
		int redCount = 0, blackCount = 0;

		// Check if either player can make any legal moves.
		boolean redHasValidMoves = false;
		boolean blackHasValidMoves = false;
		for (int i = 0; i < board_size; i++) {
			for (int j = 0; j < board_size; j++) {
				if (board[i][j].isRed()) {
					redCount++;
				} else if (board[i][j].isBlack()) {
					blackCount++;
				}

				if (!board[i][j].isEmpty()) { // Skip empty squares, which trivially have no valid moves.
					// Check if the piece on square [i, j] can make a valid move.
					if (!redHasValidMoves && board[i][j].isRed()) {
						if (numValidMoves(board, board_size, i, j, Square.RED) != 0) {
							redHasValidMoves = true;
						}
					}
					if (!blackHasValidMoves && board[i][j].isBlack()) {
						if (numValidMoves(board, board_size, i, j, Square.BLACK) != 0) {
							blackHasValidMoves = true;
						}
					}
				}
			}
		}

		if (redCount == 0) {
			return -1; // Black win - red has no remaining pieces.
		} else if (blackCount == 0) {
			return 1; // Red win - black has no remaining pieces.
		}

		if (redHasValidMoves && blackHasValidMoves) {
			return 0;
		} else if (redHasValidMoves) {
			if (turn == Square.BLACK) {
				return 1;
			} else {
				return 0;
			}
		} else if (blackHasValidMoves) {
			if (turn == Square.RED) {
				return -1;
			} else {
				return 0;
			}
		} else {
			return (turn == Square.RED) ? -1 : 1; // If both are out of moves, the board is stalemated, and whichever side is to
													   // move (but obviously can't) loses.
		}
	}

	/**
	 * The static position evaluation function used in the minimax algorithm.
	 * @param position The position to evaluate.
	 * @param turn Whose turn to evaluate on.
	 * @return The evaluation value.
	 */
	public int evaluationFunction(int turn) {
		if ((turn == Square.RED && this.state == 1) || (turn == Square.BLACK && this.state == -1)) {
			return Integer.MAX_VALUE; // If the position is won, return infinity.
		} else if ((turn == Square.RED && this.state == -1) || (turn == Square.BLACK && this.state == 1)) {
			return Integer.MIN_VALUE; // If the position is lost, return minus infinity.
		}

		return evaluationFunction_piecesDifference(turn);
	}

	// Evaluation functions below this line.

	/**
	 * Simple evaluation function based on number of pieces still standing.
	 * @param turn Whose turn it is.
	 */
	private int evaluationFunction_numPieces(int turn) {
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
	 * Simple evaluation function that returns the difference in number of friendly and enemy pieces.
	 * @param turn Whose turn it is.
	 */
	private int evaluationFunction_piecesDifference(int turn) {
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
					} else if ((this.board[i][j].isRed() && turn == Square.BLACK) || (this.board[i][j].isBlack() && turn == Square.RED)) {
						if (this.board[i][j].isKing()) {
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

	// End evaluation functions.

	/**
	 * Returns the position which characterizes the board after the given piece has moved from the initial square to the final square, assuming 
	 * the move is legal.
	 * @param i_initial The vertical coordinate of the source square.
	 * @param j_initial The horizontal coordinate of the source square.
	 * @param i_initial The vertical coordinate of the destination square.
	 * @param j_initial The horizontal coordinate of the destination square.
	 * @param doubleJump Whether the move is part of a double jump or not.
	 * @return Returns the next positions if the move is legal, and null otherwise.
	 */
	public Position move(int i_initial, int j_initial, int i_new, int j_new, boolean doubleJump) {
		if (i_initial < 0 || i_initial >= Checkers.BOARD_SIZE || j_initial < 0 || j_initial >= Checkers.BOARD_SIZE || i_new < 0 || i_new >= Checkers.BOARD_SIZE || 
			j_new < 0 || j_new >= Checkers.BOARD_SIZE) {
			return null; // Can't move off the board.
		} else if (i_new == i_initial || j_new == j_initial) {
			return null; // Must move to different row and column.
		} else if ((this.turn == Square.RED && !this.board[i_initial][j_initial].isRed()) || (this.turn == Square.BLACK && !this.board[i_initial][j_initial].isBlack())) { 
			return null; // Can't move empty or enemy piece.
		} else if (!this.board[i_new][j_new].isEmpty()) {
			return null; // Can't move onto an occupied square.
		} else if (Math.abs((i_new - i_initial) / (j_new - j_initial)) != 1) {
			return null; // Can only move diagonally.
		}

		// If the move is backwards, initial piece must be a king or must be in middle of double jump.
		if (this.turn == Square.RED && i_new > i_initial) {
			if (!this.board[i_initial][j_initial].isKing() && !doubleJump) {
				return null;
			}
		} else if (this.turn == Square.BLACK && i_new < i_initial) {
			if (!this.board[i_initial][j_initial].isKing() && !doubleJump) {
				return null;
			}
		}

		Square[][] nextBoard = deepSquareCopy(this.board);

		if (Math.abs(i_new - i_initial) == 2) { // Move is attempted capture.
			Square capturedSquare = this.board[(i_initial + i_new) / 2][(j_initial + j_new) / 2];
			if (capturedSquare.isEmpty() || (capturedSquare.isRed() && this.turn == Square.RED) || (capturedSquare.isBlack() && this.turn == Square.BLACK)) {
				return null; // Capture must be on enemy piece.
			}

			// The capture is legal, so remove the captured piece.
			nextBoard[(i_initial + i_new) / 2][(j_initial + j_new) / 2].setEmpty();
		}


		// Move the piece to its new square.
		nextBoard[i_new][j_new].clone(this.board[i_initial][j_initial]);
		nextBoard[i_initial][j_initial].setEmpty();

		// King the piece if it's not already a king and it's moving to the last row in enemy territory.
		if (this.turn == Square.RED && i_new == 0) {
			nextBoard[i_new][j_new].makeKing();
		} else if (this.turn == Square.BLACK && i_new == Checkers.BOARD_SIZE - 1) {
			nextBoard[i_new][j_new].makeKing();
		}

		if (doubleJump && (numForwardCaptures(nextBoard, Checkers.BOARD_SIZE, i_new, j_new, this.turn) != 0 || 
						   numForwardCaptures(nextBoard, Checkers.BOARD_SIZE, i_new, j_new, this.turn) != 0)) {
			return new Position(nextBoard, this.turn); // The move is in the middle of a valid double jump, so preserve the turn.
		} else {
			return new Position(nextBoard, ((this.turn == Square.RED) ? Square.BLACK : Square.RED)); // Switch to the other player.
		}

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

	/**
	 * @return Returns this position's state, according to the legend above.
	 */
	public int getState() {
		return this.state;
	}

	/**
	 * @param i The vertical coordinate of the piece to make king.
	 * @param j The horizontal coordinate of the piece to make king.
	 * @return Returns a copy of this position with the given piece a king.
	 */
	public Position makeKing(int i, int j) {
		Square[][] nextBoard = deepSquareCopy(this.board);
		nextBoard[i][j].makeKing();
		return new Position(nextBoard, this.turn);		
	}

	@Override
	public boolean equals(Object obj) {
		Position p = (Position) obj;
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

	/**
	 * Prints out the board, but facing the player with black pieces.
	 */
	public String toStringReversed() {
		String s = "";

		if (this.board.length < 10) {
			s += "  ";
		} else {
			s += "   ";
		}
		for (int i = this.board.length - 1; i >= 0; i--) {
			s += i + " ";
		}
		s += "\n";
		for (int i = this.board.length - 1; i >= 0; i--) {
			if (this.board.length < 10) {
				s += "| ";
			} else {
				if (i < 10) {
					s += "|  ";
				} else if (i < 100) {
					s += "| ";
				}
			}
			// for (int j = 0; j < this.board.length; j++) {
			for (int j = this.board.length - 1; j >= 0; j--) {
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
			s += i + "\n";
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
	 * @return Returns the set of all positions which result from making a single move (including double jumps) from the given square. Output
	 * 		   is formatted as a set of two-element Object arrays; first element of the array is the new position, and the second is a 
	 * 		   coordinate array representing the move to get to the position.
	 */
	private static HashSet<Object[]> generateAllMoves(Square[][] board, int board_size, int i, int j, int turn) {
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
					
					if (i - 1 == 0) { // If the move is to the enemy home rank, king the piece.
						tempBoard[i - 1][j - 1].makeKing();
					}
					tempBoard[i][j].setEmpty();
					
					int[] parentMove = {i, j, i - 1, j - 1};
					Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
					moves.add(move);
				}
				if (j + 1 < board_size && board[i - 1][j + 1].isEmpty()) {
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

					if (i - 2 == 0) { // If the capture lands on the enemy home rank, king the piece.
						tempBoard[i - 2][j - 2].makeKing();
					}
					tempBoard[i][j].setEmpty();
					tempBoard[i - 1][j - 1].setEmpty();
					
					int[] parentMove = {i, j, i - 2, j - 2};
					Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
					moves.add(move);

					for (Object[] double_jump_move : generateAllMovesDoubleJump(tempBoard, board_size, i - 2, j - 2, parentMove, turn)) {
						moves.add(double_jump_move);
					}
				}
				if (j + 2 < board_size && board[i - 2][j + 2].isEmpty() && board[i - 1][j + 1].isBlack()) {
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

					for (Object[] double_jump_move : generateAllMovesDoubleJump(tempBoard, board_size, i - 2, j + 2, parentMove, turn)) {
						moves.add(double_jump_move);
					}
				}
			}

			// Check for backward moves and captures.
			if (board[i][j].isKing()) {
				if (i + 1 < board_size) {
					if (j - 1 >= 0 && board[i + 1][j - 1].isEmpty()) {
						Square[][] tempBoard = deepSquareCopy(board);
						tempBoard[i + 1][j - 1] = new Square(tempBoard[i][j]);
						tempBoard[i][j].setEmpty();
						
						int[] parentMove = {i, j, i + 1, j - 1};
						Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
						moves.add(move);

					}
					if (j + 1 < board_size && board[i + 1][j + 1].isEmpty()) {
						Square[][] tempBoard = deepSquareCopy(board);
						tempBoard[i + 1][j + 1] = new Square(tempBoard[i][j]);
						tempBoard[i][j].setEmpty();

						int[] parentMove = {i, j, i + 1, j + 1};
						Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
						moves.add(move);

					}
				}
				if (i + 2 < board_size) {
					if (j - 2 >= 0 && board[i + 2][j - 2].isEmpty() && board[i + 1][j - 1].isBlack()) {
						Square[][] tempBoard = deepSquareCopy(board);
						tempBoard[i + 2][j - 2] = new Square(tempBoard[i][j]);
						tempBoard[i][j].setEmpty();
						tempBoard[i + 1][j - 1].setEmpty();

						int[] parentMove = {i, j, i + 2, j - 2};
						Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
						moves.add(move);

						for (Object[] double_jump_move  : generateAllMovesDoubleJump(tempBoard, board_size, i + 2, j - 2, parentMove, turn)) {
							moves.add(double_jump_move);
						}
					}
					if (j + 2 < board_size && board[i + 2][j + 2].isEmpty() && board[i + 1][j + 1].isBlack()) {
						Square[][] tempBoard = deepSquareCopy(board);
						tempBoard[i + 2][j + 2] = new Square(tempBoard[i][j]);
						tempBoard[i][j].setEmpty();
						tempBoard[i + 1][j + 1].setEmpty();

						int[] parentMove = {i, j, i + 2, j + 2};
						Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
						moves.add(move);

						for (Object[] double_jump_move : generateAllMovesDoubleJump(tempBoard, board_size, i + 2, j + 2, parentMove, turn)) {
							moves.add(double_jump_move);
						}
					}
				}
			}
		} else {
			// Check for forward moves.
			if (i + 1 < board_size) {
				if (j - 1 >= 0 && board[i + 1][j - 1].isEmpty()) {
					Square[][] tempBoard = deepSquareCopy(board);
					tempBoard[i + 1][j - 1] = new Square(tempBoard[i][j]);
					if (i + 1 == board_size - 1) {
						tempBoard[i + 1][j - 1].makeKing();
					}
					tempBoard[i][j].setEmpty();
					
					int[] parentMove = {i, j, i + 1, j - 1};
					Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
					moves.add(move);
				}
				if (j + 1 < board_size && board[i + 1][j + 1].isEmpty()) {
					Square[][] tempBoard = deepSquareCopy(board);
					tempBoard[i + 1][j + 1] = new Square(tempBoard[i][j]);
					if (i + 1 == board_size - 1) {
						tempBoard[i + 1][j + 1] = new Square(tempBoard[i][j]);
					}
					tempBoard[i][j].setEmpty();

					int[] parentMove = {i, j, i + 1, j + 1};
					Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
					moves.add(move);
				}
			}
			
			// Check for forward captures.
			if (i + 2 < board_size) {
				if (j - 2 >= 0 && board[i + 2][j - 2].isEmpty() && board[i + 1][j - 1].isRed()) {
					Square[][] tempBoard = deepSquareCopy(board);
					tempBoard[i + 2][j - 2] = new Square(tempBoard[i][j]);
					if (i + 2 == board_size - 1) {
						tempBoard[i + 2][j - 2].makeKing();
					}
					tempBoard[i][j].setEmpty();
					tempBoard[i + 1][j - 1].setEmpty();
					
					int[] parentMove = {i, j, i + 2, j - 2};
					Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
					moves.add(move);

					for (Object[] double_jump_move : generateAllMovesDoubleJump(tempBoard, board_size, i + 2, j - 2, parentMove, turn)) {
						moves.add(double_jump_move);
					}
				}
				if (j + 2 < board_size && board[i + 2][j + 2].isEmpty() && board[i + 1][j + 1].isRed()) {
					Square[][] tempBoard = deepSquareCopy(board);
					tempBoard[i + 2][j + 2] = new Square(tempBoard[i][j]);
					if (i + 2 == board_size - 1) {
						tempBoard[i + 2][j + 2].makeKing();						
					}
					tempBoard[i][j].setEmpty();
					tempBoard[i + 1][j + 1].setEmpty();

					int[] parentMove = {i, j, i + 2, j + 2};
					Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
					moves.add(move);

					for (Object[] double_jump_move : generateAllMovesDoubleJump(tempBoard, board_size, i + 2, j + 2, parentMove, turn)) {
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
					if (j + 1 < board_size && board[i - 1][j + 1].isEmpty()) {
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

						for (Object[] double_jump_move  : generateAllMovesDoubleJump(tempBoard, board_size, i - 2, j - 2, parentMove, turn)) {
							moves.add(double_jump_move);
						}
					}
					if (j + 2 < board_size && board[i - 2][j + 2].isEmpty() && board[i - 1][j + 1].isRed()) {
						Square[][] tempBoard = deepSquareCopy(board);
						tempBoard[i - 2][j + 2] = new Square(tempBoard[i][j]);
						tempBoard[i][j].setEmpty();
						tempBoard[i - 1][j + 1].setEmpty();

						int[] parentMove = {i, j, i - 2, j + 2};
						Object[] move = {(new Position(tempBoard, nextTurn)), parentMove};
						moves.add(move);

						for (Object[] double_jump_move : generateAllMovesDoubleJump(tempBoard, board_size, i - 2, j + 2, parentMove, turn)) {
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
	 * @return Returns all boards resulting from the ensuing double jump sequence. Output follows same format as the output of generateAllMoves.
	 */
	private static HashSet<Object[]> generateAllMovesDoubleJump(Square[][] board, int board_size, int i, int j, int[] currParentMove, int turn) {
		HashSet<Object[]> moves = new HashSet<>();
		if (numForwardCaptures(board, board_size, i, j, turn) == 0 && numBackwardCaptures(board, board_size, i, j, turn, true) == 0) {
			return moves;
		}

		int nextTurn = (turn == Square.RED) ? Square.BLACK : Square.RED;

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
					Object[] move = {(new Position(nextBoard, nextTurn)), parentMove};
					moves.add(move);

					for (Object[] double_jump_move : generateAllMovesDoubleJump(nextBoard, board_size, i - 2, j - 2, parentMove, turn)) {
						moves.add(double_jump_move);
					}
				}
				if (j + 2 < board_size && board[i - 2][j + 2].isEmpty() && board[i - 1][j + 1].isBlack()) {
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
					Object[] move = {(new Position(nextBoard, nextTurn)), parentMove};
					moves.add(move);

					for (Object[] double_jump_move : generateAllMovesDoubleJump(nextBoard, board_size, i - 2, j + 2, parentMove, turn)) {
						moves.add(double_jump_move);
					}
				}
			}
			if (i + 2 < board_size) {
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
					Object[] move = {(new Position(nextBoard, nextTurn)), parentMove};
					moves.add(move);

					for (Object[] double_jump_move : generateAllMovesDoubleJump(nextBoard, board_size, i + 2, j - 2, parentMove, turn)) {
						moves.add(double_jump_move);
					}	
				}
				if (j + 2 < board_size && board[i + 2][j + 2].isEmpty() && board[i + 1][j + 1].isBlack()) {
					nextBoard = deepSquareCopy(board);
					nextBoard[i + 2][j + 2] = new Square(nextBoard[i][j]);
					nextBoard[i][j].setEmpty();
					nextBoard[i + 1][j + 1].setEmpty();

					int[] parentMove = Arrays.copyOf(currParentMove, currParentMove.length + 4);
					parentMove[parentMove.length - 4] = i;
					parentMove[parentMove.length - 3] = j;
					parentMove[parentMove.length - 2] = i + 2;
					parentMove[parentMove.length - 1] = j + 2;
					Object[] move = {(new Position(nextBoard, nextTurn)), parentMove};
					moves.add(move);

					for (Object[] double_jump_move : generateAllMovesDoubleJump(nextBoard, board_size, i + 2, j + 2, parentMove, turn)) {
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
					Object[] move = {(new Position(nextBoard, nextTurn)), parentMove};
					moves.add(move);

					for (Object[] double_jump_move : generateAllMovesDoubleJump(nextBoard, board_size, i - 2, j - 2, parentMove, turn)) {
						moves.add(double_jump_move);
					}
				}
				if (j + 2 < board_size && board[i - 2][j + 2].isEmpty() && board[i - 1][j + 1].isRed()) {
					nextBoard = deepSquareCopy(board);
					nextBoard[i - 2][j + 2] = new Square(nextBoard[i][j]);
					nextBoard[i][j].setEmpty();
					nextBoard[i - 1][j + 1].setEmpty();

					int[] parentMove = Arrays.copyOf(currParentMove, currParentMove.length + 4);
					parentMove[parentMove.length - 4] = i;
					parentMove[parentMove.length - 3] = j;
					parentMove[parentMove.length - 2] = i - 2;
					parentMove[parentMove.length - 1] = j + 2;
					Object[] move = {(new Position(nextBoard, nextTurn)), parentMove};
					moves.add(move);

					for (Object[] double_jump_move : generateAllMovesDoubleJump(nextBoard, board_size, i - 2, j + 2, parentMove, turn)) {
						moves.add(double_jump_move);
					}
				}
			}
			if (i + 2 < board_size) {
				if (j - 2 >= 0 && board[i + 2][j - 2].isEmpty() && board[i + 1][j - 1].isRed()) {
					nextBoard = deepSquareCopy(board);
					nextBoard[i + 2][j - 2] = new Square(nextBoard[i][j]);
					if (i + 2 == board_size - 1) {
						nextBoard[i + 2][j - 2].makeKing();						
					}
					nextBoard[i][j].setEmpty();
					nextBoard[i + 1][j - 1].setEmpty();

					int[] parentMove = Arrays.copyOf(currParentMove, currParentMove.length + 4);
					parentMove[parentMove.length - 4] = i;
					parentMove[parentMove.length - 3] = j;
					parentMove[parentMove.length - 2] = i + 2;
					parentMove[parentMove.length - 1] = j - 2;
					Object[] move = {(new Position(nextBoard, nextTurn)), parentMove};
					moves.add(move);

					for (Object[] double_jump_move : generateAllMovesDoubleJump(nextBoard, board_size, i + 2, j - 2, parentMove, turn)) {
						moves.add(double_jump_move);
					}	
				}
				if (j + 2 < board_size && board[i + 2][j + 2].isEmpty() && board[i + 1][j + 1].isRed()) {
					nextBoard = deepSquareCopy(board);
					nextBoard[i + 2][j + 2] = new Square(nextBoard[i][j]);
					if (i + 2 == board_size - 1) {
						nextBoard[i + 2][j + 2].makeKing();						
					}
					nextBoard[i][j].setEmpty();
					nextBoard[i + 1][j + 1].setEmpty();

					int[] parentMove = Arrays.copyOf(currParentMove, currParentMove.length + 4);
					parentMove[parentMove.length - 4] = i;
					parentMove[parentMove.length - 3] = j;
					parentMove[parentMove.length - 2] = i + 2;
					parentMove[parentMove.length - 1] = j + 2;
					Object[] move = {(new Position(nextBoard, nextTurn)), parentMove};
					moves.add(move);

					for (Object[] double_jump_move : generateAllMovesDoubleJump(nextBoard, board_size, i + 2, j + 2, parentMove, turn)) {
						moves.add(double_jump_move);
					}
				}
			}
		}

		return moves;
	}

	/**
 	 * Returns the number of legal moves the piece at [i, j] can make.
 	 * @param i The vertical coordinate of the square.
 	 * @param j The horizontal coordinate of the square.
 	 * @param turn Whose turn to assume it is when searching for valid moves.
 	 * @return Whether or not the piece on square [i, j] can make any valid moves.
 	 */
	public static int numValidMoves(Square[][] board, int board_size, int i, int j, int turn) {
		if ((board[i][j].isRed() && turn != Square.RED) || (board[i][j].isBlack() && turn != Square.BLACK) || board[i][j].isEmpty()) {
			return 0; // The player whose turn it is must match the piece on the given square.
		}

		int count = 0;

		if (turn == Square.RED) {
			// Check the immediate two squares forward.
			if (i - 1 >= 0) {
				if (j - 1 >= 0) {
					if (board[i - 1][j - 1].isEmpty()) {
						count++; // Can move diagonally up and right.
					}
				}
				if (j + 1 < board_size) {
					if (board[i - 1][j + 1].isEmpty()) {
						count++; // Can move diagonally up and left.
					}
				}
			}

			// Check if capture is possible.
			count += numForwardCaptures(board, board_size, i, j, turn);

			// If the piece is a king, check squares behind it.
			if (board[i][j].isKing()) {
				if (i + 1 < board_size) {
					if (j - 1 >= 0) {
						if (board[i + 1][j - 1].isEmpty()) {
							count++; // Can move diagonally down and left.
						}
					}
					if (j + 1 < board_size) {
						if (board[i + 1][j + 1].isEmpty()) {
							count++; // Can move diagonally down and right.
						}
					}
				}

				// Check for backwards captures.
				count += numBackwardCaptures(board, board_size, i, j, turn, false);
			}
		} else {
			// Check the immediate two squares forward.
			if (i + 1 < board_size) {
				if (j - 1 >= 0) {
					if (board[i + 1][j - 1].isEmpty()) {
						count++; // Can move diagonally up and right
					}
				}
				if (j + 1 < board_size) {
					if (board[i + 1][j + 1].isEmpty()) {
						count++; // Can move diagonally up and left
					}
				}
			}

			// Check if capture is possible.
			count += numForwardCaptures(board, board_size, i, j, turn);

			// If the piece is a king, check squares behind it.
			if (board[i][j].isKing()) {
				if (i - 1 >= 0) {
					if (j - 1 >= 0) {
						if (board[i - 1][j - 1].isEmpty()) {
							count++; // Can move diagonally down and left.
						}
					}
					if (j + 1 < board_size) {
						if (board[i - 1][j + 1].isEmpty()) {
							count++; // Can move diagonally down and right.
						}
					}
				}

				// Check for backwards captures.
				count += numBackwardCaptures(board, board_size, i, j, turn, false);
			}
		}

		return count;
	}

	/**
	 * Returns the number of forward captures the piece at [i, j] can make, if any.
	 * @param i The vertical coordinate of the square.
	 * @param j The horizontal coordinate of the square.
	 * @param turn Whose turn to assume it is when searching for valid moves.
	 */
	public static int numForwardCaptures(Square[][] board, int board_size,int i, int j, int turn) {
		int count = 0;
		if (turn == Square.RED) {
			if (i - 2 >= 0) {
				if (j - 2 >= 0) {
					if (board[i - 2][j - 2].isEmpty() && !board[i - 1][j - 1].isRed() && !board[i - 1][j - 1].isEmpty()) {
						count++; // Can capture diagonally up and left.
					}
				}
				if (j + 2 < board_size) {
					if (board[i - 2][j + 2].isEmpty() && !board[i - 1][j + 1].isRed() && !board[i - 1][j + 1].isEmpty()) {
						count++; // Can capture digaonally up and right.
					}
				}
			}
		} else {
			if (i + 2 < board_size) {
				if (j - 2 >= 0) {
					if (board[i + 2][j - 2].isEmpty() && !board[i + 1][j - 1].isBlack() && !board[i + 1][j - 1].isEmpty()) {
						count++; // Can capture diagonally up and left.
					}
				}
				if (j + 2 < board_size) {
					if (board[i + 2][j + 2].isEmpty() && !board[i + 1][j + 1].isBlack() && !board[i + 1][j + 1].isEmpty()) {
						count++; // Can capture digaonally up and right.
					}
				}
			}
		}

		return count;
	}

	/**
	 * Returns the number of backward captures the piece at [i, j] can make, if any.
	 * @param i The vertical coordinate of the square.
	 * @param j The horizontal coordinate of the square.
	 * @param turn Whose turn to assume it is when searching for valid moves.
	 * @param isDoubleJump Whether or not the move is in the middle of a capture sequence.
	 */
	public static int numBackwardCaptures(Square[][] board, int board_size, int i, int j, int turn, boolean isDoubleJump) {
		if (!board[i][j].isKing() && !isDoubleJump) {
			return 0; // Must be a king to capture backwards.
		}

		int count = 0;

		if (turn == Square.RED) {
			if (i + 2 < board_size) {
				if (j - 2 >= 0) {
					if (board[i + 2][j - 2].isEmpty() && board[i + 1][j - 1].isBlack()) {
						count++; // Can capture diagonally down and left.
					}
				}
				if (j + 2 < board_size) {
					if (board[i + 2][j + 2].isEmpty() && board[i + 1][j + 1].isBlack()) {
						count++; // Can capture diagonally down and right.
					}
				}
			}
		} else {
			if (i - 2 >= 0) {
				if (j - 2 >= 0) {
					if (board[i - 2][j - 2].isEmpty() && board[i - 1][j - 1].isRed()) {
						count++; // Can capture diagonally down and left.
					}
				}
				if (j + 2 < board_size) {
					if (board[i - 2][j + 2].isEmpty() && board[i - 1][j + 1].isRed()) {
						count++; // Can capture diagonally down and right.
					}
				}
			}
		}

		return count;
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