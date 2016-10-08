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
	public static final int DRAW_MOVE_LIMIT = 40; // The maximum number of consecutive moves, on one side, without a capture before the game draws. 

	private int drawMoveCount; // The number of consecutive moves without a capture that have passed. When drawMoveCount >= DRAW_MOVE_LIMIT, the
							   // game draws be default.
	private Position currentPosition;

	/**
	 * Basic constructor.
	 */
	public Checkers() {
		this.currentPosition = new Position();
		this.drawMoveCount = 0;
        this.meow = 10
	}

	/**
	 * Constructor for starting a game at a given position.
	 * @param p The starting position.
	 */
	public Checkers(Position p) {
		this.currentPosition = p;
		this.drawMoveCount = 0;
	}

	/**
	 * Begins an interactive game of Checkers that can be played against a computer player.
	 */
	public static void consolePlayAI(boolean useTablebases) {
		CheckersAI player = new CheckersAI();
		Checkers game = new Checkers();
		Scanner reader = new Scanner(System.in);
		int[] coordinates = new int[4];
		String line;
		int userTurn, outcome, i = 1;


		System.out.println();
		System.out.println("\t\tWELCOME TO CHECKERS!");
		System.out.println("\tLegend: r = red piece, b = black piece, R = red king, B = black king");

		System.out.print("Which side would you like to play as? ");
		line = reader.nextLine();
		while (!line.toLowerCase().equals("red") && !line.toLowerCase().equals("black")) {
			System.out.print("Enter \"red\" or \"black\" (red moves first): ");
			line = reader.nextLine().replace(" ", "");
		}
		if (line.toLowerCase().equals("red")) {
			userTurn = Square.RED;
		} else {
			userTurn = Square.BLACK;
		}

		if (useTablebases) {
			player.initializeTablebase((userTurn == Square.RED) ? Square.BLACK : Square.RED);
		}

		while ((outcome = game.currentPosition.getState()) == 0) {
			// Check for draw.
			if (game.drawMoveCount >= DRAW_MOVE_LIMIT) {
				outcome = 2;
				break;
			}

			System.out.println();
			System.out.println("TURN " + i++);
			System.out.println();

			if (game.turn() == userTurn) { // It's the user's turn.
				System.out.println("Board:");
				if (userTurn == Square.RED) {
					System.out.println(game.currentPosition);
				} else {
					System.out.println(game.currentPosition.toStringReversed());
				}
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
					if (!game.move(coordinates[0], coordinates[1], coordinates[2], coordinates[3], false)) {
						System.out.println("Illegal move. Please enter a valid move.");
					} else {
						break;
					}
				}

				// If the move is a capture, check capture sequences.
				if (Math.abs(coordinates[2] - coordinates[0]) == 2 && Math.abs(coordinates[3] - coordinates[1]) == 2) {
					game.currentPosition.turn = (game.turn() == Square.RED) ? Square.BLACK : Square.RED;
					while (Position.numForwardCaptures(game.currentPosition.board, BOARD_SIZE, coordinates[2], coordinates[3], game.turn()) != 0 || 
						   Position.numBackwardCaptures(game.currentPosition.board, BOARD_SIZE, coordinates[2], coordinates[3], game.turn(), true) != 0) {

						System.out.println("Board:");
						System.out.println(game.currentPosition);

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
								if (!game.move(coordinates[0], coordinates[1], coordinates[2], coordinates[3], true)) {
									System.out.print("Illegal move. Please enter a valid destination square: ");
									line = reader.nextLine();
									while (line.toLowerCase().equals("help")) {
										printHelpString();
										System.out.print("Move: ");
										line = reader.nextLine();
									}
									if (line.toLowerCase().equals("exit")) {
										return;
									}
								} else {
									break;
								}
							}
						} else {
							break;
						}
					}
				}
			} else { // Player will move.
				coordinates = player.move(game.currentPosition); // Player makes a move.
				game.move(coordinates[0], coordinates[1], coordinates[2], coordinates[3], false);
				String printString;
				if (coordinates.length > 4) {
					printString = "Opponent double jumped";
				} else {
					printString = "Opponent's turn";
				}

				printString += " - moved from (" + coordinates[0] + ", " + coordinates[1] + ") to (" + coordinates[coordinates.length - 2] + ", " + 
							   coordinates[coordinates.length - 1] + ")";
				if (Math.abs(coordinates[2] - coordinates[0]) == 2 && Math.abs(coordinates[3] - coordinates[1]) == 2) {
					printString += ", captured on " + "(" + ((coordinates[0] + coordinates[2]) / 2) + ", " + ((coordinates[1] + coordinates[3]) / 2) + ")";
					if (coordinates.length > 4) {
						for (int j = 4; j < coordinates.length; j += 4) {
							game.move(coordinates[j], coordinates[j + 1], coordinates[j + 2], coordinates[j + 3], true);
							printString += ", (" + coordinates[j] + ", " + coordinates[j + 1] + ")";
						}
					}
				}

				System.out.println(printString + ".");
			}
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
	public static void consolePlayRandom() {
		RandomPlayer player = new RandomPlayer();
		Checkers game = new Checkers();
		Scanner reader = new Scanner(System.in);
		int[] coordinates = new int[4];
		String line;
		int userTurn, outcome, i = 1;
		Random r = new Random();

		System.out.println();
		System.out.println("\t\tWELCOME TO CHECKERS!");
		System.out.println("\tLegend: r = red piece, b = black piece, R = red king, B = black king");

		System.out.print("Which side would you like to play as? ");
		line = reader.nextLine();
		while (!line.toLowerCase().equals("red") && !line.toLowerCase().equals("black")) {
			System.out.print("Enter \"red\" or \"black\" (red moves first): ");
			line = reader.nextLine().replace(" ", "");
		}
		if (line.toLowerCase().equals("red")) {
			userTurn = Square.RED;
		} else {
			userTurn = Square.BLACK;
		}

		while ((outcome = game.currentPosition.getState()) == 0) {
			// Check for draw.
			if (game.drawMoveCount >= DRAW_MOVE_LIMIT) {
				outcome = 2;
				break;
			}

			System.out.println();
			System.out.println("TURN " + i++);
			System.out.println();

			if (game.turn() == userTurn) { // It's the user's turn.
				System.out.println("Board:");
				if (userTurn == Square.RED) {
					System.out.println(game.currentPosition);
				} else {
					System.out.println(game.currentPosition.toStringReversed());					
				}
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
					if (!game.move(coordinates[0], coordinates[1], coordinates[2], coordinates[3], false)) {
						System.out.println("Illegal move. Please enter a valid move.");
					} else {
						break;
					}
				}

				// If the move is a capture, check capture sequences.
				if (Math.abs(coordinates[2] - coordinates[0]) == 2 && Math.abs(coordinates[3] - coordinates[1]) == 2) {
					while (Position.numForwardCaptures(game.currentPosition.board, BOARD_SIZE, coordinates[2], coordinates[3], game.turn()) != 0 || 
						   Position.numBackwardCaptures(game.currentPosition.board, BOARD_SIZE, coordinates[2], coordinates[3], game.turn(), true) != 0) {

						i++;
						System.out.println("Board:");
						System.out.println(game.currentPosition);

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
								if (!game.move(coordinates[0], coordinates[1], coordinates[2], coordinates[3], true)) {
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
								} else {
									break;
								}
							}
						} else {
							break;
						}
					}
				}
			} else { // Player will move.
				coordinates = player.move(game.currentPosition); // Player makes a move.
				game.move(coordinates[0], coordinates[1], coordinates[2], coordinates[3], false);

				// If the move was a capture, check for double jumps.
				if (Math.abs(coordinates[2] - coordinates[0]) == 2 && Math.abs(coordinates[3] - coordinates[1]) == 2) {
					System.out.println("Opponent's turn - moved from (" + coordinates[0] + ", " + coordinates[1] + ") to (" + coordinates[2] + 
									   ", " + coordinates[3] + "), captured on (" + ((coordinates[0] + coordinates[2]) / 2) + ", " + 
									   ((coordinates[1] + coordinates[3]) / 2) + ").");
					while (Position.numForwardCaptures(game.currentPosition.board, BOARD_SIZE, coordinates[2], coordinates[3], game.turn()) != 0 || 
						   Position.numBackwardCaptures(game.currentPosition.board, BOARD_SIZE, coordinates[2], coordinates[3], game.turn(), true) != 0) {
						if (r.nextInt(2) == 0) { // Continue the capture sequence with probability 0.5
							break;
						}

						coordinates = player.doubleJump(game.currentPosition, coordinates[0], coordinates[1]);
						game.move(coordinates[0], coordinates[1], coordinates[2], coordinates[3], true);
						System.out.println("Opponent double jumped - captured on (" + ((coordinates[0] + coordinates[2]) / 2) + ", " + 
									   	   ((coordinates[1] + coordinates[3]) / 2) + ")" + ". Board:");
						System.out.println(game.currentPosition);
					}
				} else {
					System.out.println("Opponent's turn - moved from (" + coordinates[0] + ", " + coordinates[1] + ") to (" + coordinates[2] + 
						   			   ", " + coordinates[3] + ").");
				}
			}
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
	public static void consolePlay() {
		Checkers game = new Checkers();
		Scanner reader = new Scanner(System.in);
		int[] coordinates = new int[4];
		String line;
		int outcome;

		int i = 1;
		System.out.println();
		System.out.println("\t\tWELCOME TO CHECKERS!");
		System.out.println("\tLegend: r = red piece, b = black piece, R = red king, B = black king");

		while ((outcome = game.currentPosition.getState()) == 0) {
			// Check for draw.
			if (game.drawMoveCount >= DRAW_MOVE_LIMIT) {
				outcome = 2;
				break;
			}

			System.out.println();
			System.out.println("TURN " + i++);
			System.out.println();
			System.out.println("Board:");
			System.out.println(game.currentPosition);
			System.out.println("Player to move: " + ((game.turn() == 1) ? "red" : "black"));
			System.out.println("To enter a move, specify the square of the piece to move, and then the square to move to (enter \"help\" for usage).");

			// Keep trying to parse for user input until a valid move is made.
			Position next;
			while (true) {
				if (getMoveAsInput(coordinates, reader, false) == -1) {
					return;
				}
				if (coordinates == null) {
					return;
				}
				// Make the move, and update the current position.
				if (!game.move(coordinates[0], coordinates[1], coordinates[2], coordinates[3], false)) {
					System.out.println("Illegal move. Please enter a valid move.");
				} else {
					break;
				}
			}

			// If the move is a capture, check capture sequences.
			if (Math.abs(coordinates[2] - coordinates[0]) == 2 && Math.abs(coordinates[3] - coordinates[1]) == 2) {
				while (Position.numForwardCaptures(game.currentPosition.board, BOARD_SIZE, coordinates[2], coordinates[3], game.turn()) != 0 || 
					   Position.numBackwardCaptures(game.currentPosition.board, BOARD_SIZE, coordinates[2], coordinates[3], game.turn(), true) != 0) {

					i++;
					System.out.println("Board:");
					System.out.println(game.currentPosition);

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
							if (!game.move(coordinates[0], coordinates[1], coordinates[2], coordinates[3], true)) {
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
							} else {
								break;
							}
						}
					} else {
						break;
					}
				}
			}
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
	 * Plays a full game between two AIs. The first AI is assumed to be red.
	 * @param ai1 The AI program to use as the first player.
	 * @param ai2 The AI program to use as the second player.
	 * @return Returns the outcome of the game, following the conventions above.
	 */
	public static int play(CheckersAI ai1, CheckersAI ai2) throws Exception {
		Checkers game = new Checkers();
		int[] coordinates;
		int outcome;

		while ((outcome = game.currentPosition.getState()) == 0) {	
			// System.out.println("turn: " + game.turn());
			// System.out.println(game.currentPosition);
			// System.out.println();

			// Check for draw.
			if (game.drawMoveCount >= DRAW_MOVE_LIMIT) {
				return 2;
			}

			if (game.turn() == Square.RED) { // It's the first AI's turn.
				coordinates = ai1.move(game.currentPosition);
			} else { // It's the second AI's turn.
				coordinates = ai2.move(game.currentPosition);
			}

			if (coordinates.length > 4) { // Move was a double jump.
				for (int i = 0; i < coordinates.length; i += 4) {
					if (!game.move(coordinates[i], coordinates[i + 1], coordinates[i + 2], coordinates[i + 3], true)) {
						throw new Exception("Illegal double jump returned.");
					}
				}

				game.toggleTurn();
			} else { // Normal move.
				if (!game.move(coordinates[0], coordinates[1], coordinates[2], coordinates[3], false)) {
					throw new Exception("Illegal move returned.");
				}
			}
		}
		
		return outcome;
	}


	/**
	 * Moves the given piece from the initial square to the final square, assuming the move is legal. Returns whether or not the move is legal.
	 * @param i_initial The vertical coordinate of the source square.
	 * @param j_initial The horizontal coordinate of the source square.
	 * @param i_initial The vertical coordinate of the destination square.
	 * @param j_initial The horizontal coordinate of the destination square.
	 * @param doubleJump Whether or not the move is in the middle of a capture sequence.
	 * @return Returns true if the move is legal, and false otherwise.
	 */
	public boolean move(int i_initial, int j_initial, int i_new, int j_new, boolean doubleJump) {
		Position next = this.currentPosition.move(i_initial, j_initial, i_new, j_new, doubleJump); // Get the next position.
		if (next != null) {
			if (Math.abs(i_new - i_initial) == 2) {
				this.drawMoveCount = 0; // Move was a capture, so reset the draw counter.
			} else {
				this.drawMoveCount++;
			}

			this.currentPosition = next;
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Returns the piece at the given square.
	 * @param i The vertical coordinate.
	 * @param j The horizontal coordinate.
	 * @return Returns the Square at [i, j].
	 */
	public Square getPiece(int i, int j) {
		return this.currentPosition.board[i][j];
	}

	/** 
	 * @return Returns whose turn it is.
	 */
	public int turn() {
		return this.currentPosition.turn;
	}

	/**
	 * Toggles the turn.
	 */
	public void toggleTurn() {
		this.currentPosition = new Position(this.currentPosition, (this.currentPosition.turn == Square.RED) ? Square.BLACK : Square.RED);
	}

	// Helper methods below this line.

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