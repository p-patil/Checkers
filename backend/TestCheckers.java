import java.util.Scanner;

/**
 * Class for testing code.
 */
public class TestCheckers {
	public static void main(String[] args) {
		playThroughConsole();
	}

	/**
	 * Begins an interactive game of Checkers that can be played through the console, for testing purposes.
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
			game.printBoard();
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
				while (game.canCaptureForward(coordinates[2], coordinates[3], game.getCurrentTurn()) || 
					   game.canCaptureBackward(coordinates[2], coordinates[3], game.getCurrentTurn())) {

					i++;
					System.out.println("Board:");
					game.printBoard();

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
							} else {
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
			System.out.println("You won the game!");
		} else if (outcome == -1) {
			System.out.println("You lost you loser fuck!");
		}
		reader.close();
	}

	/**
	 * Helper function that gets user input from reader and parses it for source and destination squares on the checker board.
	 * @param coordinates An array of integers in which to place the new coordinates. The first pair in the array represents the initial vertical
	 * and horizontal coordinates and the second pair represents the final vertical and horizontal coordinates.
	 * @param reader The Scanner object providing the input stream to read from.
	 * @param isDoubleJump Whether or not a capture sequence is in progress.
	 * @return Returns the status of the function - 0 if everything should proceed as normal, -1 to abort.
	 */
	public static int getMoveAsInput(int[] coordinates, Scanner reader, boolean isDoubleJump) {
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