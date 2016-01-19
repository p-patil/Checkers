import java.lang.Math;
import java.util.Scanner;
import java.util.Random;
import java.util.Arrays;
import java.io.Serializable;

/**
 * Main class for interfacing with the actual Checkers game.
 */
public class Checkers implements Serializable {
	public static final int BOARD_SIZE = 8; // Board dimensions, set to 8, the standard for English draughts, by default. Must be even.
	public static final int drawMoveLimit = 40; // The maximum number of consecutive moves, on one side, without a capture before the game draws. 

	public Square[][] board; // The checkers board. By convention, the upper left corner, on black's side, is [0, 0], with the first dimension 
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
		if (BOARD_SIZE % 2 != 0) {
			System.out.println("ERROR - Board size must be even.");
			System.exit(0);
		}

		this.currentTurn = Square.RED;
		this.drawMoveCount = 0;
		this.redCount = 0;
		this.blackCount = 0;
		this.board = new Square[BOARD_SIZE][BOARD_SIZE];

		for (int i = 0; i < BOARD_SIZE; i++) {
			for (int j = 0; j < BOARD_SIZE; j++) {
				if (i < (BOARD_SIZE - 2) / 2) {
					if ((i + j) % 2 != 0) {
						this.board[i][j] = new Square(Square.BLACK, i, j);
						this.blackCount++;
					} else {
						this.board[i][j] = new Square(Square.EMPTY, i, j);
					}
				} else if (i < (BOARD_SIZE + 2) / 2) {
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
	}

	/**
	 * Constructor that creates a clone of the given Checkers object.
	 * @param game The Checkers object to copy.
	 */
	public Checkers(Checkers game) {
		this.currentTurn = game.getCurrentTurn();
		this.drawMoveCount = game.getDrawMoveCount();
		this.redCount = game.getRedCount();
		this.blackCount = game.getBlackCount();
		this.board = new Square[BOARD_SIZE][BOARD_SIZE];
		for (int i = 0; i < this.board.length; i++) {
			for (int j = 0; j < this.board[i].length; j++) {
				this.board[i][j] = game.getPiece(i, j);
			}
		}
	}

	/**
	 * Constructor that builds a checkers game from a given board.
	 * @param board The board configuration to use.
	 * @param turn Whose turn it is.
	 */
	public Checkers(Square[][] board, int turn) {
		this.currentTurn = turn;
		this.redCount = 0;
		this.blackCount = 0;
		this.board = new Square[board.length][];
		for (int i = 0; i < this.board.length; i++) {
			this.board[i] = new Square[board[i].length];
			for (int j = 0; j < this.board[i].length; j++) {
				this.board[i][j] = board[i][j];
				if (board[i][j].isRed()) {
					this.redCount++;
				} else if (board[i][j].isBlack()) {
					this.blackCount++;
				}
			}
		}
	}

	/**
	 * Begins an interactive game of Checkers that can be played against a computer player.
	 */
	public static void playThroughConsoleAI(boolean useTablebases) {
		CheckersAI player = new CheckersAI();
		if (useTablebases) {
			System.out.println("Initializing tablebase...");
			player.initializeTablebase(Square.BLACK);
		}

		Checkers game = new Checkers();
		Scanner reader = new Scanner(System.in);
		int[] coordinates = new int[4];
		String line;
		int outcome;
		int i = 1;

		System.out.println();
		System.out.println("\t\tWELCOME TO CHECKERS!");
		System.out.println("\tLegend: r = red piece, b = black piece, R = red king, B = black king");
		System.out.println();
		System.out.println("\tTURN " + i);
		System.out.println();

		while ((outcome = game.isGameOver()) == 0) {
			System.out.println("Board:");
			System.out.println(game);

			if (game.getCurrentTurn() == Square.RED) { // It's the user's turn.
				System.out.println("Your turn.");
				System.out.println("To enter a move, specify the square of the piece to move, and then the square to move to (enter \"help\" for usage).");

				// Keep trying to parse for user input until a valid move is made.
				while (true) {
					if (getMoveAsInput(coordinates, reader, false) == -1) {
						return;
					}
					if (coordinates == null) {
						return;
					}
					if (!game.move(coordinates[0], coordinates[1], coordinates[2], coordinates[3])) {
						System.out.println("Illegal move. Please enter a valid move.");
					} else {
						break;
					}
				}

				// If the move is a capture, check capture sequences.
				if (Math.abs(coordinates[2] - coordinates[0]) == 2 && Math.abs(coordinates[3] - coordinates[1]) == 2) {
					while (numForwardCaptures(game.board, coordinates[2], coordinates[3], game.getCurrentTurn()) != 0 || 
						   numBackwardCaptures(game.board, coordinates[2], coordinates[3], game.getCurrentTurn()) != 0) {

						i++;
						System.out.println("Board:");
						System.out.println(game);

						// Ask if user wants to initiaze a capture sequence.
						while (true) {
							System.out.print("Double jump? (yes/no) ");
							line = reader.nextLine();
							if (line.toLowerCase().equals("help")) {
								printHelpString();
								continue;
							} else if (line.toLowerCase().equals("exit")) {
								return;
							}
							if (!line.toLowerCase().equals("yes") && !line.toLowerCase().equals("no")) {
								System.out.println("Invalid format. Please try again. Enter \"help\" for usage.");
							} else {
								break;
							}
						}

						if (line.equals("yes")) { // If yes, repeat the process until the capture sequence is over.
							System.out.print("Enter next move (must be capture): ");

							// Have the user make the next move.
							while (true) {
								// Get user input, forcing it to be a capture.
								do {
									if (getMoveAsInput(coordinates, reader, true) == -1) {
										return;
									}
									if (coordinates == null) {
										return;
									}
								} while (Math.abs(coordinates[2] - coordinates[0]) != 2 || Math.abs(coordinates[3] - coordinates[1]) != 2);
							
								// Check if the move is valid.
								game.board[coordinates[0]][coordinates[1]].makeKing(); // Temporarily make king to allow for backwards captures
																					   // during double jumps.
								if (!game.move(coordinates[0], coordinates[1], coordinates[2], coordinates[3])) {
									System.out.print("Illegal move. Please enter a valid move: ");
									line = reader.nextLine();
									while (line.toLowerCase().equals("help")) {
										printHelpString();
										System.out.print("Move: ");
										line = reader.nextLine();
									}
									if (line.toLowerCase().equals("exit")) {
										return;
									}
									int state = (game.board[coordinates[2]][coordinates[3]].isRed() ? Square.RED : Square.BLACK);
									game.board[coordinates[2]][coordinates[3]] = new Square(state, coordinates[2], coordinates[3]);
								} else {
									int state = (game.board[coordinates[2]][coordinates[3]].isRed() ? Square.RED : Square.BLACK);
									game.board[coordinates[2]][coordinates[3]] = new Square(state, coordinates[2], coordinates[3]);								
									break;
								}
							}
						} else {
							break;
						}
					}
				}

				System.out.println();
				System.out.println("TURN " + i++);
				System.out.println();
			} else { // Player will move.
				System.out.println();
				System.out.println("TURN " + i++);
				System.out.println();
				coordinates = player.move(game); // Player makes a move.

				// If the move was a capture, check for double jumps.
				if (Math.abs(coordinates[2] - coordinates[0]) == 2 && Math.abs(coordinates[3] - coordinates[1]) == 2) {
					System.out.println("Opponent's turn - moved from (" + coordinates[0] + ", " + coordinates[1] + ") to (" + coordinates[2] + 
									   ", " + coordinates[3] + "), captured on (" + ((coordinates[0] + coordinates[2]) / 2) + ", " + 
									   ((coordinates[1] + coordinates[3]) / 2) + ").");
				} else {
					System.out.println("Opponent's turn - moved from (" + coordinates[0] + ", " + coordinates[1] + ") to (" + coordinates[2] + 
						   			   ", " + coordinates[3] + ").");
				}
			}
			
			game.toggleTurn();
		}

		if (outcome == 2) {
			System.out.println("Game drawn!");
		} else if (outcome == 1) {
			System.out.println("You won the game!");
		} else if (outcome == -1) {
			System.out.println("You lost. :(");
		}
		reader.close();
	}

	/**
	 * Begins an interactive game of Checkers that can be played against a random player through the console, for testing purposes.
	 */
	public static void playThroughConsoleRandom() {
		RandomPlayer player = new RandomPlayer();
		Checkers game = new Checkers();
		Scanner reader = new Scanner(System.in);
		int[] coordinates = new int[4];
		String line;
		int outcome;

		int i = 1;
		System.out.println();
		System.out.println("\t\tWELCOME TO CHECKERS!");
		System.out.println("\tLegend: r = red piece, b = black piece, R = red king, B = black king");
		System.out.println();
		System.out.println("\tTURN " + i);
		System.out.println();

		while ((outcome = game.isGameOver()) == 0) {
			System.out.println("Board:");
			System.out.println(game);

			if (game.getCurrentTurn() == Square.RED) { // It's the user's turn.
				System.out.println("Your turn.");
				System.out.println("To enter a move, specify the square of the piece to move, and then the square to move to (enter \"help\" for usage).");

				// Keep trying to parse for user input until a valid move is made.
				while (true) {
					if (getMoveAsInput(coordinates, reader, false) == -1) {
						return;
					}
					if (coordinates == null) {
						return;
					}
					if (!game.move(coordinates[0], coordinates[1], coordinates[2], coordinates[3])) {
						System.out.println("Illegal move. Please enter a valid move.");
					} else {
						break;
					}
				}

				// If the move is a capture, check capture sequences.
				if (Math.abs(coordinates[2] - coordinates[0]) == 2 && Math.abs(coordinates[3] - coordinates[1]) == 2) {
					while (numForwardCaptures(game.board, coordinates[2], coordinates[3], game.getCurrentTurn()) != 0 || 
						   numBackwardCaptures(game.board, coordinates[2], coordinates[3], game.getCurrentTurn()) != 0) {

						i++;
						System.out.println("Board:");
						System.out.println(game);

						// Ask if user wants to initiaze a capture sequence.
						while (true) {
							System.out.print("Double jump? (yes/no) ");
							line = reader.nextLine();
							if (line.toLowerCase().equals("help")) {
								printHelpString();
								continue;
							} else if (line.toLowerCase().equals("exit")) {
								return;
							}
							if (!line.toLowerCase().equals("yes") && !line.toLowerCase().equals("no")) {
								System.out.println("Invalid format. Please try again. Enter \"help\" for usage.");
							} else {
								break;
							}
						}

						if (line.equals("yes")) { // If yes, repeat the process until the capture sequence is over.
							System.out.print("Enter next move (must be capture): ");

							// Have the user make the next move.
							while (true) {
								// Get user input, forcing it to be a capture.
								do {
									if (getMoveAsInput(coordinates, reader, true) == -1) {
										return;
									}
									if (coordinates == null) {
										return;
									}
								} while (Math.abs(coordinates[2] - coordinates[0]) != 2 || Math.abs(coordinates[3] - coordinates[1]) != 2);
							
								// Check if the move is valid.
								game.board[coordinates[0]][coordinates[1]].makeKing(); // Temporarily make king to allow for backwards captures
																					   // during double jumps.
								if (!game.move(coordinates[0], coordinates[1], coordinates[2], coordinates[3])) {
									System.out.print("Illegal move. Please enter a valid move: ");
									line = reader.nextLine();
									while (line.toLowerCase().equals("help")) {
										printHelpString();
										System.out.print("Move: ");
										line = reader.nextLine();
									}
									if (line.toLowerCase().equals("exit")) {
										return;
									}
									int state = (game.board[coordinates[2]][coordinates[3]].isRed() ? Square.RED : Square.BLACK);
									game.board[coordinates[2]][coordinates[3]] = new Square(state, coordinates[2], coordinates[3]);
								} else {
									int state = (game.board[coordinates[2]][coordinates[3]].isRed() ? Square.RED : Square.BLACK);
									game.board[coordinates[2]][coordinates[3]] = new Square(state, coordinates[2], coordinates[3]);								
									break;
								}
							}
						} else {
							break;
						}
					}
				}

				System.out.println();
				System.out.println("TURN " + i++);
				System.out.println();
			} else { // Player will move.
				System.out.println();
				System.out.println("TURN " + i++);
				System.out.println();
				coordinates = player.move(game); // Player makes a move.

				// If the move was a capture, check for double jumps.
				if (Math.abs(coordinates[2] - coordinates[0]) == 2 && Math.abs(coordinates[3] - coordinates[1]) == 2) {
					System.out.println("Opponent's turn - moved from (" + coordinates[0] + ", " + coordinates[1] + ") to (" + coordinates[2] + 
									   ", " + coordinates[3] + "), captured on (" + ((coordinates[0] + coordinates[2]) / 2) + ", " + 
									   ((coordinates[1] + coordinates[3]) / 2) + ").");
					Random r = new Random();
					while (numForwardCaptures(game.board, coordinates[2], coordinates[3], game.getCurrentTurn()) != 0 || 
						   numBackwardCaptures(game.board, coordinates[2], coordinates[3], game.getCurrentTurn()) != 0) {
						if (r.nextInt(2) == 0) { // Continue the capture sequence with probability 0.5
							break;
						}

						coordinates = player.doubleJump(game, game.getPiece(coordinates[0], coordinates[1]));
						System.out.println("Opponent double jumped - captured on (" + ((coordinates[0] + coordinates[2]) / 2) + ", " + 
									   	   ((coordinates[1] + coordinates[3]) / 2) + ")" + ". Board:");
						System.out.println(game);
					}
				} else {
					System.out.println("Opponent's turn - moved from (" + coordinates[0] + ", " + coordinates[1] + ") to (" + coordinates[2] + 
						   			   ", " + coordinates[3] + ").");
				}
			}
			
			game.toggleTurn();
		}

		if (outcome == 2) {
			System.out.println("Game drawn!");
		} else if (outcome == 1) {
			System.out.println("You won the game!");
		} else if (outcome == -1) {
			System.out.println("You lost. :(");
		}
		reader.close();
	}

	/**
	 * Begins an interactive game of Checkers that can be played against onself through the console, for testing purposes.
	 */
	public static void playThroughConsole() {
		Checkers game = new Checkers();
		Scanner reader = new Scanner(System.in);
		int[] coordinates = new int[4];
		String line;
		int outcome;

		int i = 1;
		System.out.println();
		System.out.println("\t\tWELCOME TO CHECKERS!");
		System.out.println("\tLegend: r = red piece, b = black piece, R = red king, B = black king");
		System.out.println();
		System.out.println("\tTURN " + i);
		System.out.println();

		while ((outcome = game.isGameOver()) == 0) {
			System.out.println("Board:");
			System.out.println(game);
			System.out.println("Player to move: " + ((game.getCurrentTurn() == 1) ? "red" : "black"));
			System.out.println("To enter a move, specify the square of the piece to move, and then the square to move to (enter \"help\" for usage).");

			// Keep trying to parse for user input until a valid move is made.
			while (true) {
				if (getMoveAsInput(coordinates, reader, false) == -1) {
					return;
				}
				if (coordinates == null) {
					return;
				}
				if (!game.move(coordinates[0], coordinates[1], coordinates[2], coordinates[3])) {
					System.out.println("Illegal move. Please enter a valid move.");
				} else {
					break;
				}
			}

			// If the move is a capture, check capture sequences.
			if (Math.abs(coordinates[2] - coordinates[0]) == 2 && Math.abs(coordinates[3] - coordinates[1]) == 2) {
				while (numForwardCaptures(game.board, coordinates[2], coordinates[3], game.getCurrentTurn()) != 0 || 
					   numBackwardCaptures(game.board, coordinates[2], coordinates[3], game.getCurrentTurn()) != 0) {

					i++;
					System.out.println("Board:");
					System.out.println(game);

					// Ask if user wants to initiaze a capture sequence.
					while (true) {
						System.out.print("Double jump? (yes/no) ");
						line = reader.nextLine();
						if (line.toLowerCase().equals("help")) {
							printHelpString();
							continue;
						} else if (line.toLowerCase().equals("exit")) {
							return;
						}
						if (!line.toLowerCase().equals("yes") && !line.toLowerCase().equals("no")) {
							System.out.println("Invalid format. Please try again. Enter \"help\" for usage.");
						} else {
							break;
						}
					}

					if (line.equals("yes")) { // If yes, repeat the process until the capture sequence is over.
						System.out.print("Enter next move (must be capture): ");

						// Have the user make the next move.
						while (true) {
							// Get user input, forcing it to be a capture.
							do {
								if (getMoveAsInput(coordinates, reader, true) == -1) {
									return;
								}
								if (coordinates == null) {
									return;
								}
							} while (Math.abs(coordinates[2] - coordinates[0]) != 2 || Math.abs(coordinates[3] - coordinates[1]) != 2);
						
							// Check if the move is valid.
							game.board[coordinates[0]][coordinates[1]].makeKing(); // Temporarily make king to allow for backwards captures
																				   // during double jumps.
							if (!game.move(coordinates[0], coordinates[1], coordinates[2], coordinates[3])) {
								System.out.print("Illegal move. Please enter a valid move: ");
								line = reader.nextLine();
								while (line.toLowerCase().equals("help")) {
									printHelpString();
									System.out.print("Move: ");
									line = reader.nextLine();
								}
								if (line.toLowerCase().equals("exit")) {
									return;
								}
								int state = (game.board[coordinates[2]][coordinates[3]].isRed() ? Square.RED : Square.BLACK);
								game.board[coordinates[2]][coordinates[3]] = new Square(state, coordinates[2], coordinates[3]);
							} else {
								int state = (game.board[coordinates[2]][coordinates[3]].isRed() ? Square.RED : Square.BLACK);
								game.board[coordinates[2]][coordinates[3]] = new Square(state, coordinates[2], coordinates[3]);
								break;
							}
						}
					} else {
						break;
					}
				}
			}

			game.toggleTurn();
			i++;
			System.out.println();
			System.out.println("TURN " + i);
			System.out.println();
		}

		if (outcome == 2) {
			System.out.println("Game drawn!");
		} else if (outcome == 1) {
			System.out.println("Red won the game!");
		} else if (outcome == -1) {
			System.out.println("Black won the game!");
		}
		reader.close();
	}

	/**
	 * Moves the given piece from the initial square to the final square, assuming the move is legal. Returns whether or not the move is legal.
	 * @param i_initial The vertical coordinate of the source square.
	 * @param j_initial The horizontal coordinate of the source square.
	 * @param i_initial The vertical coordinate of the destination square.
	 * @param j_initial The horizontal coordinate of the destination square.
	 * @return Returns true if the move is legal, and false otherwise.
	 */
	public boolean move(int i_initial, int j_initial, int i_new, int j_new) {
		if (i_initial < 0 || i_initial >= BOARD_SIZE || j_initial < 0 || j_initial >= BOARD_SIZE || i_new < 0 || i_new >= BOARD_SIZE || 
			j_new < 0 || j_new >= BOARD_SIZE) {
			return false; // Can't move off the board.
		} else if (i_new == i_initial || j_new == j_initial) {
			return false; // Must move to different row and column.
		} else if ((this.currentTurn == Square.RED && !this.board[i_initial][j_initial].isRed()) || (this.currentTurn == Square.BLACK && !this.board[i_initial][j_initial].isBlack())) { 
			return false; // Can't move empty or enemy piece.
		} else if (!this.board[i_new][j_new].isEmpty()) {
			return false; // Can't move onto an occupied square.
		} else if (Math.abs((i_new - i_initial) / (j_new - j_initial)) != 1) {
			return false; // Can only move diagonally.
		}

		// If the move is backwards, initial piece must be a king.
		if (this.currentTurn == Square.RED && i_new > i_initial) {
			if (!this.board[i_initial][j_initial].isKing()) {
				return false;
			}
		} else if (this.currentTurn == Square.BLACK && i_new < i_initial) {
			if (!this.board[i_initial][j_initial].isKing()) {
				return false;
			}
		}
		if (Math.abs(i_new - i_initial) == 2) { // Move is attempted capture.
			Square capturedSquare = this.board[(i_initial + i_new) / 2][(j_initial + j_new) / 2];
			if (capturedSquare.isEmpty() || (capturedSquare.isRed() && this.currentTurn == Square.RED || capturedSquare.isBlack() && this.currentTurn == Square.BLACK)) {
				return false; // Capture must be on enemy piece.
			}

			// The capture is legal, so remove the captured piece.
			this.board[(i_initial + i_new) / 2][(j_initial + j_new) / 2].setEmpty();
			if (this.currentTurn == Square.RED) {
				this.blackCount--;
			} else {
				this.redCount--;				
			}

			this.drawMoveCount = 0; // A capture was made, so reset the draw counter.
		} else {
			this.drawMoveCount++; // Another non-capture move was made - increment draw counter.
		}

		// Move the piece to its new square.
		this.board[i_new][j_new] = new Square(this.board[i_initial][j_initial]);
		this.board[i_initial][j_initial].setEmpty();

		// King the piece if it's not already a king and it's moving to the last row in enemey territory.
		if (this.currentTurn == Square.RED && i_new == 0) {
			this.board[i_new][j_new].makeKing();
		} else if (this.currentTurn == Square.BLACK && i_new == BOARD_SIZE - 1) {
			this.board[i_new][j_new].makeKing();
		}

		return true;
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
		if (this.redCount == 0) {
			return -1; // Black win - red has no remaining pieces.
		} else if (this.blackCount == 0) {
			return 1; // Red win - black has no remaining pieces.
		}

		// Check the draw condition.
		if (this.drawMoveCount >= this.drawMoveLimit) {
			return 2;
		}

		// Check if either player can make any legal moves.
		boolean redHasValidMoves = false;
		boolean blackHasValidMoves = false;
		for (int i = 0; i < BOARD_SIZE; i++) {
			for (int j = 0; j < BOARD_SIZE; j++) {
				if (!this.board[i][j].isEmpty()) { // Skip empty squares, which trivially have no valid moves.
					// Check if the piece on square [i, j] can make a valid move.
					if (!redHasValidMoves && this.board[i][j].isRed()) {
						if (numValidMoves(this.board, i, j, Square.RED) != 0) {
							redHasValidMoves = true;
						}
					}
					if (!blackHasValidMoves && this.board[i][j].isBlack()) {
						if (numValidMoves(this.board, i, j, Square.BLACK) != 0) {
							blackHasValidMoves = true;
						}
					}
				}

				if (redHasValidMoves && blackHasValidMoves) {
					return 0;
				}
			}
		}

		if (redHasValidMoves && blackHasValidMoves) {
			return 0;
		} else if (redHasValidMoves) {
			if (this.currentTurn == Square.BLACK) {
				return 1;
			} else {
				return 0;
			}
		} else if (blackHasValidMoves) {
			if (this.currentTurn == Square.RED) {
				return -1;
			} else {
				return 0;
			}
		} else {
			return (this.currentTurn == Square.RED) ? -1 : 1; // If both are out of moves, the board is stalemated, and whichever side is to
															  // move (but obviously can't) loses.
		}
	}

	/**
 	 * Returns the number of legal moves the piece at [i, j] can make.
 	 * @param i The vertical coordinate of the square.
 	 * @param j The horizontal coordinate of the square.
 	 * @param turn Whose turn to assume it is when searching for valid moves.
 	 * @return Whether or not the piece on square [i, j] can make any valid moves.
 	 */
	public static int numValidMoves(Square[][] board, int i, int j, int turn) {
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
				if (j + 1 < BOARD_SIZE) {
					if (board[i - 1][j + 1].isEmpty()) {
						count++; // Can move diagonally up and left.
					}
				}
			}

			// Check if capture is possible.
			count += numForwardCaptures(board, i, j, turn);

			// If the piece is a king, check squares behind it.
			if (board[i][j].isKing()) {
				if (i + 1 < BOARD_SIZE) {
					if (j - 1 >= 0) {
						if (board[i + 1][j - 1].isEmpty()) {
							count++; // Can move diagonally down and left.
						}
					}
					if (j + 1 < BOARD_SIZE) {
						if (board[i + 1][j + 1].isEmpty()) {
							count++; // Can move diagonally down and right.
						}
					}
				}

				// Check for backwards captures.
				count += numBackwardCaptures(board, i, j, turn);
			}
		} else {
			// Check the immediate two squares forward.
			if (i + 1 < BOARD_SIZE) {
				if (j - 1 >= 0) {
					if (board[i + 1][j - 1].isEmpty()) {
						count++; // Can move diagonally up and right
					}
				}
				if (j + 1 < BOARD_SIZE) {
					if (board[i + 1][j + 1].isEmpty()) {
						count++; // Can move diagonally up and left
					}
				}
			}

			// Check if capture is possible.
			count += numForwardCaptures(board, i, j, turn);

			// If the piece is a king, check squares behind it.
			if (board[i][j].isKing()) {
				if (i - 1 >= 0) {
					if (j - 1 >= 0) {
						if (board[i - 1][j - 1].isEmpty()) {
							count++; // Can move diagonally down and left.
						}
					}
					if (j + 1 < BOARD_SIZE) {
						if (board[i - 1][j + 1].isEmpty()) {
							count++; // Can move diagonally down and right.
						}
					}
				}

				// Check for backwards captures.
				count += numBackwardCaptures(board, i, j, turn);
			}
		}

		return count;
	}

	/**
	 * Switches the turn.
	 */
	public void toggleTurn() {
		if (this.currentTurn == Square.RED) {
			this.currentTurn = Square.BLACK;
		} else {
			this.currentTurn = Square.RED;
		}
	}

	/**
	 * @return Returns which player's current turn it is.
	 */
	public int getCurrentTurn() {
		return this.currentTurn;
	}

	/**
	 * Returns the piece at the given square.
	 * @param i The vertical coordinate.
	 * @param j The horizontal coordinate.
	 * @return Returns the Square at [i, j].
	 */
	public Square getPiece(int i, int j) {
		return this.board[i][j];
	}

	/**
	 * @return Returns the number of red pieces on the board.
	 */
	public int getRedCount() {
		return this.redCount;
	}

	/**
	 * @return Returns the number of black pieces on the board.
	 */
	public int getBlackCount() {
		return this.blackCount;
	}

	/**
	 * @return Returns the draw move count for this game.
	 */
	public int getDrawMoveCount() {
		return this.drawMoveCount;
	}


	@Override
	public boolean equals(Object obj) {
		return (new Position(this)).equals(new Position((Checkers) obj));
	}

	@Override
	public int hashCode() {
		return (new Position(this)).hashCode();
	}

	@Override
	public String toString() {
		String s = "";

		if (BOARD_SIZE < 10) {
			s += "  ";
		} else {
			s += "   ";
		}
		for (int i = 0; i < BOARD_SIZE; i++) {
			s += i + " ";
		}
		s += "\n";
		for (int i = 0; i < BOARD_SIZE; i++) {
			if (BOARD_SIZE < 10) {
				s += i + " ";
			} else {
				if (i < 10) {
					s += i + "  ";
				} else if (i < 100) {
					s += i + " ";
				}
			}
			for (int j = 0; j < BOARD_SIZE; j++) {
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
		for (int i = 0; i <= BOARD_SIZE; i++) {
			s += "--";
		}
		
		return s + "\n";
	}

	// Helper methods below this line.

	/**
	 * Returns the number of forward captures the piece at [i, j] can make, if any.
	 * @param i The vertical coordinate of the square.
	 * @param j The horizontal coordinate of the square.
	 * @param turn Whose turn to assume it is when searching for valid moves.
	 */
	public static int numForwardCaptures(Square[][] board, int i, int j, int turn) {
		int count = 0;
		if (turn == Square.RED) {
			if (i - 2 >= 0) {
				if (j - 2 >= 0) {
					if (board[i - 2][j - 2].isEmpty() && !board[i - 1][j - 1].isRed() && !board[i - 1][j - 1].isEmpty()) {
						count++; // Can capture diagonally up and left.
					}
				}
				if (j + 2 < BOARD_SIZE) {
					if (board[i - 2][j + 2].isEmpty() && !board[i - 1][j + 1].isRed() && !board[i - 1][j + 1].isEmpty()) {
						count++; // Can capture digaonally up and right.
					}
				}
			}
		} else {
			if (i + 2 < BOARD_SIZE) {
				if (j - 2 >= 0) {
					if (board[i + 2][j - 2].isEmpty() && !board[i + 1][j - 1].isBlack() && !board[i + 1][j - 1].isEmpty()) {
						count++; // Can capture diagonally up and left.
					}
				}
				if (j + 2 < BOARD_SIZE) {
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
	 */
	public static int numBackwardCaptures(Square[][] board, int i, int j, int turn) {
		if (!board[i][j].isKing()) {
			return 0; // Must be a king to capture backwards.
		}

		int count = 0;

		if (turn == Square.RED) {
			if (i + 2 < BOARD_SIZE) {
				if (j - 2 >= 0) {
					if (board[i + 2][j - 2].isEmpty() && !board[i + 1][j - 1].isRed() && !board[i + 1][j - 1].isEmpty()) {
						count++; // Can capture diagonally down and left.
					}
				}
				if (j + 2 < BOARD_SIZE) {
					if (board[i + 2][j + 2].isEmpty() && !board[i + 1][j + 1].isRed() && !board[i + 1][j + 1].isEmpty()) {
						count++; // Can capture diagonally down and right.
					}
				}
			}
		} else {
			if (i - 2 >= 0) {
				if (j - 2 >= 0) {
					if (board[i - 2][j - 2].isEmpty() && !board[i - 1][j - 1].isBlack() && !board[i - 1][j - 1].isEmpty()) {
						count++; // Can capture diagonally down and left.
					}
				}
				if (j + 2 < BOARD_SIZE) {
					if (board[i - 2][j + 2].isEmpty() && !board[i - 1][j + 1].isBlack() && !board[i - 1][j + 1].isEmpty()) {
						count++; // Can capture diagonally down and right.
					}
				}
			}
		}

		return count;
	}

		/**
	 * Helper function that gets user input from reader and parses it for source and destination squares on the checker board.
	 * @param coordinates An array of integers in which to place the new coordinates. The first pair in the array represents the initial vertical
	 * and horizontal coordinates and the second pair represents the final vertical and horizontal coordinates.
	 * @param reader The Scanner object providing the input stream to read from.
	 * @param isDoubleJump Whether or not a capture sequence is in progress.
	 * @return Returns the status of the function - 0 if everything should proceed as normal, -1 to abort.
	 */
	private static int getMoveAsInput(int[] coordinates, Scanner reader, boolean isDoubleJump) {
		String line, x, y;
		int i;
		boolean notValidInput;

		do {
			notValidInput = false;
			try {
				if (!isDoubleJump) { // If a double jump is in progress, the source square is unchanged, so skip this step.
					x = "";
					y = "";

					System.out.print("Source square: ");
					line = reader.nextLine().replace(" ", ""); // Remove whitespace.

					while (line.toLowerCase().equals("help")) {
						printHelpString();
						System.out.print("Source square: ");
						line = reader.nextLine();
					}
					if (line.toLowerCase().equals("exit")) {
						return -1;
					}

					if (line.length() <= 3) {
						throw new NumberFormatException();
					}

					// Parse numbers.
					i = 1;
					for (; line.charAt(i) != ','; i++) {
						x += line.charAt(i);
					}
					for (i++; i < line.length() - 1; i++) {
						y += line.charAt(i);
					}

					coordinates[0] = Integer.parseInt(x);
					coordinates[1] = Integer.parseInt(y);

				} else {
					coordinates[0] = coordinates[2];
					coordinates[1] = coordinates[3];
					System.out.println("Source square is (" + coordinates[0] + ", " + coordinates[1] + ")");
				}
				x = "";
				y = "";

				System.out.print("Destination square: ");
				line = reader.nextLine().replace(" ", "");

				while (line.toLowerCase().equals("help")) {
					printHelpString();
					System.out.print("Destination square: ");
					line = reader.nextLine();
				}
				if (line.toLowerCase().equals("exit")) {
					return -1;
				}

				if (line.length() <= 3) {
					throw new NumberFormatException();
				}

				i = 1;
				for (; line.charAt(i) != ','; i++) {
					x += line.charAt(i);
				}
				for (i++; i < line.length() - 1; i++) {
					y += line.charAt(i);
				}

				coordinates[2] = Integer.parseInt(x);
				coordinates[3] = Integer.parseInt(y);
			} catch (NumberFormatException e) { // Error in parsing means invalid input.
				notValidInput = true;
				System.out.println("Invalid format. Please try again. Enter \"help\" for usage.");
			} catch (StringIndexOutOfBoundsException e) {
				notValidInput = true;
				System.out.println("Invalid format. Please try again. Enter \"help\" for usage.");
			}
		} while (notValidInput);

		return 0;
	}

	/**
	 * Prints the help message upon user request.
	 */
	private static void printHelpString() {
		System.out.println("\tTo specify a square, simply use the following format: (vertical, horizontal).");
		System.out.println("\tEnter \"exit\" to terminate game or \"help\" for help.");
		System.out.println("\tLegend: r = red piece, b = black piece, R = red king, B = black king");
	}
}