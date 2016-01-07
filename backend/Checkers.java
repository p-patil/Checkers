import java.lang.Math;

/**
 * Main class for interfacing with the actual TicTacToe game.
 */
public class Checkers {
	public static final int EMPTY = 0; // Represents an empty square.
	public static final int RED = 1; // Represents a square occupied by a red piece.
	public static final int BLACK = -1; // Represents a square occipied by a black piece.
	public static final int RED_KING = 2; // Represents a square occupied by a kinged red piece.
	public static final int BLACK_KING = -2; // Represents a square occupied by a kinged black piece.
	public static final int BOARD_SIZE = 8; // Board dimensions, set to 8, the standard for English draughts, by default. Must be even.
	public static final int drawMoveLimit = 40; // The maximum number of consecutive moves, on one side, without a capture before the game draws. 

	private int[][] board; // The checkers board. By convention, the upper left corner, on black's side, is [0, 0], with the first dimension 
						   // moving vertically downwards and the second moving horizontally to the right.
	private int currentTurn; // Which player's turn it is.
	private int drawMoveCount; // The number of consecutive moves without a capture that have passed. When drawMoveCount >= drawMoveLimit, the
							   // game draws be default.
	private int redCount; // Number of remaining red pieces.
	private int blackCount; // Number of remaining black pieces.

	/**
	 * Basic constructor that initialized an empty BOARD_SIZE by BOARD_SIZE board.
	 * @param n The dimensions of the Tic-Tac-Toe board.
	 */
	public Checkers() {
		this.currentTurn = RED;
		this.drawMoveCount = 0;
		this.redCount = 0;
		this.blackCount = 0;
		this.board = new int[BOARD_SIZE][BOARD_SIZE];

		for (int i = 0; i < BOARD_SIZE; i++) {
			for (int j = 0; j < BOARD_SIZE; j++) {
				if (i < (BOARD_SIZE - 2) / 2) {
					if ((i + j) % 2 != 0) {
						this.board[i][j] = BLACK;
						this.blackCount++;
					} else {
						this.board[i][j] = EMPTY;
					}
				} else if (i < (BOARD_SIZE + 2) / 2) {
					this.board[i][j] = EMPTY;
				} else {
					if ((i + j) % 2 != 0) {
						this.board[i][j] = RED;
						this.redCount++;
					} else {
						this.board[i][j] = EMPTY;
					}
				}
			}
		}
	}

	/**
	 * Moves the given piece from the initial square to the final square, assuming the move is legal. Returns whether or not the move is legal.
	 * @param i_initial The vertical coordinate of the source square.
	 * @param j_initial The horizontal coordinate of the source square.
	 * @param i_initial The vertical coordinate of the destination square.
	 * @param j_initial The horizontal coordinate of the destination square.
	 * @param player Which player is moving.
	 * @param isDoubleJump Whether or not the player is in the middle of a multi-
	 * @return Returns true if the move is legal, and false otherwise.
	 */
	public boolean move(int i_initial, int j_initial, int i_new, int j_new, int player, boolean isDoubleJump) {
		if (i_initial < 0 || i_initial >= BOARD_SIZE || j_initial < 0 || j_initial >= BOARD_SIZE || i_new < 0 || i_new >= BOARD_SIZE || 
			j_initial < 0 || j_new >= BOARD_SIZE) {
			return false; // Can't move off the board.
		} else if (this.currentTurn != player) {
			return false; // Can't move on the wrong turn.
		} else if ((player == RED && this.board[i_initial][j_initial] <= 0) || (player == BLACK && this.board[i_initial][j_initial >= 0])) { 
			return false; // Can't move empty or enemy piece.
		} else if (this.board[i_new][j_new] != EMPTY) {
			return false; // Can't move onto an occupied square.
		} else if (Math.abs((i_new - i_initial) / (j_new - j_initial)) != 1) {
			return false; // Can only move diagonally.
		} 

		// If the move is backwards, initial piece must be a king.
		if (player == RED && i_new > i_initial) {
			if (this.board[i_initial][j_initial] != RED_KING) {
				return false;
			}
		} else if (player == BLACK && i_new < i_initial) {
			if (this.board[i_initial][j_initial] != BLACK_KING) {
				return false;
			}
		}

		if (i_new - i_initial == 2) { // Move is attempted capture.
			int capturedSquare = this.board[(i_initial + i_new) / 2][(j_initial + j_new) / 2];
			if (capturedSquare == EMPTY || capturedSquare == player) {
				return false; // Capture must be on enemy piece.
			}

			// The capture is legal, so remove the captured piece.
			this.board[(i_initial + i_new) / 2][(j_initial + j_new) / 2] = EMPTY;
			if (player == RED) {
				this.blackCount--;
			} else {
				this.redCount--;				
			}
		}

		// Move the piece to its new square.
		this.board[i_new][j_new] = this.board[i_initial][j_initial];
		this.board[i_initial][j_initial] = EMPTY;

		// King the piece if it's not already a king and it's moving to the last row in enemey territory.
		if (player == RED && this.board[i_new][j_new] != RED_KING && i_new == 0) {
			this.board[i_new][j_new] = RED_KING;
		} else if (player == BLACK && this.board[i_new][j_new] != BLACK_KING) {
			this.board[i_new][j_new] = BLACK_KING;
		}

		return true;
	}

	/**
	 * Returns which piece the inputted square it is occupied by, or if it's empty.
	 * @param i The vertical parameter.
	 * @param j The horizontal parameter.
	 * @return The occupancy status of square [i, j].
	 */
	public int squareStatus(int i, int j) {
		return this.board[i][j];
	}

	/**
	 * Returns if the game is over, and if it is, the outcome. Return value codes:
	 * 0 - game not over
	 * 1 - red win
	 * -1 - black win
	 * 2 - draw
	 * @return Returns the outcome of the game, or if the game is still going. 
	 */
	public int isGameOver() {
		
	}

	// Helper methods below this line.

	/**
	 * Prints the board to console, in human readable format.
	 */
	public void printBoard() {
		for (int i = 0; i < BOARD_SIZE; i++) {
			for (int j = 0; j < BOARD_SIZE; j++) {
				if (this.board[i][j] == RED) {
					System.out.print("r ");				
				} else if (this.board[i][j] == EMPTY) {
					System.out.print(" ");
				} else if (this.board[i][j] == BLACK) {
					System.out.print("b ");				
				} else if (this.board[i][j] == RED_KING) {
					System.out.print("R ");
				} else if (this.board[i][j] == BLACK_KING) {
					System.out.print("B ");
				}
			}
			System.out.println();
		}
	}

	/**
	 * Checks if the game has stalemated.
	 * @return True if neither player can make a legal move, false otherwise.
	 */
	private boolean isStalemate() {
		
	}
}