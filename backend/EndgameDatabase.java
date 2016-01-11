import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Class for building an endgame database.
 */
public class EndgameDatabase implements Serializable {
	public static final int ENDGAME_LIMIT = 5; // The threshold number of pieces before an endgame database is used.

	HashMap<Checkers, Position> database;

	/**
	 * Basic constructor. Builds all possible positions with at most n pieces, and creates the game graph that results from them by initializing
	 * each Position object's successor attribute, using retrograde analysis. There are ((BOARD_SIZE^2 / 2) choose ENDGAME_LIMIT) total such 
	 * games. For BOARD_SIZE = 8 and ENDGAME_LIMIT = 5, this is about 202k games.
	 * @param turn The side (red or black) to build the database for.
	 */
	public EndgameDatabase(int turn) {
		this.database = new HashMap<>();

		// First, find all positions which are terminal.
		if (turn == Square.RED) {		
			HashMap<Position, Integer> curr = new HashMap<>();
			int outcome, numPositions = 0;
			for (Position p : generateAllPositions(ENDGAME_LIMIT)) { // Set all terminal games to their final value.
				outcome = (new Checkers(p.board, p.turn)).isGameOver();
				if (outcome != 0) { // Position is terminal, so set its value.
					if (turn == Square.RED && p.turn == Square.BLACK) {
						if (outcome == 1) {
							curr.put(p, Position.WIN);
						} else if (outcome == -1) {
							curr.put(p, Position.LOSS);
						} else if (outcome == 2) {
							curr.put(p, Position.DRAW);
						}
					}
				}

				numPositions++;
			}

			// In a dynamic programming fashion, build the positions in a ply using those one ply below. Given the positions which are n
			// moves from a guaranteed win, generate the set of positions (n + 1) moves away by un-moving or un-capturing once.
			HashMap<Position, Integer> next;
			while (this.database.size() < numPositions) { // Keep going until we've checked every position.
				next = new HashMap<>(); // Positions one ply higher.
				for (Position p : curr.keySet()) {
					this.database.put(new Checkers(p.board, p.turn), p); // Add to database.

					// To begin generating positions one ply higher, perform an un-move.
					for (Position next_p : unMove(p)) {
						next_p.generateSuccessors();
						next_p.successorScores.put(p, curr.get(p)); // Set the score of p to its value.

						// Essentially, run the minimax algorithm from the ground up - propogate the values upwards from the leaves.
						if (p.turn == Square.RED) { // next_p.turn is black, so minimize the values.
							if (next.containsKey(next_p)) {
								if (curr.get(p) < next.get(next_p)) {
									next.put(next_p, curr.get(p));
								}
							} else {
								next.put(next_p, curr.get(p));
							}
						} else { // next_p.turn is red, so maximize values.
							if (next.containsKey(next_p)) {
								if (curr.get(p) > next.get(next_p)) {
									next.put(next_p, curr.get(p));
								}
							} else {
								next.put(next_p, curr.get(p));
							}
						}
					}

					// Only perform an un-capture if it doesn't exceed the ENDGAME_LIMIT.
					if (p.pieceCount() < ENDGAME_LIMIT - 1) {
						for (Position next_p : unCapture(p)) {
							next_p.generateSuccessors();
							next_p.successorScores.put(p, curr.get(p)); 

							if (p.turn == Square.RED) {
								if (next.containsKey(next_p)) {
									if (curr.get(p) < next.get(next_p)) {
										next.put(next_p, curr.get(p));
									}
								} else {
									next.put(next_p, curr.get(p));
								}
							} else { // next_p.turn is red, so maximize values.
								if (next.containsKey(next_p)) {
									if (curr.get(p) > next.get(next_p)) {
										next.put(next_p, curr.get(p));
									}
								} else {
									next.put(next_p, curr.get(p));
								}
							}
						}
					}
				}

				curr = new HashMap<>(next);
			}
		}
	}

	/**
	 * Serializes this object.
	 * @param filepath The path to the file in which to store the contents of this object.
	 * @return Whether or not the serialization succeeded.
	 */
	public boolean serialize(String filepath) {
		boolean success = true;
		try {
			FileOutputStream fileOut = new FileOutputStream(filepath + ".ser");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(this);
			out.close();
			fileOut.close();
		} catch (IOException e) {
			success = false;
		} finally {
			return success;
		}
	}

	/**
	 * Deserializes the endgame database located at the given filepath.
	 * @param filepath The path to the .ser file to deserialize.
	 * @return Returns the deserialized object, or null if there was an error.
	 */
	public static EndgameDatabase deserialize(String filepath) {
		try {
			FileInputStream fileIn = new FileInputStream(filepath);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			EndgameDatabase database = (EndgameDatabase) in.readObject();
			in.close();
			fileIn.close();
			return database;
		} catch (IOException e) {
			return null;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	// Helper methods below this line.

	/**
	 * Given a position and turn, computes all positions which reach the given one in exactly one move (excluding captures) made by the side 
	 * whose turn it is.
	 * @param p The position.
	 * @param turn Whose turn it is.
	 * @return Returns the set of all positions one move (not capture) away from p.
	 */
	private static HashSet<Position> unMove(Position p) {
		HashSet<Position> unmovePositions = new HashSet<>();

		Square[][] temp;
		int prevTurn = (p.turn == Square.RED) ? Square.BLACK : Square.RED;
		for (int i = 1; i < p.board.length; i += 2) { // Only check dark squares.
			for (int j = 1; j < p.board[i].length; j += 2) {
				if (p.turn == Square.RED && p.board[i][j].isRed()) {
					if (i + 1 < p.board[i].length) {
						if (j - 1 >= 0 && p.board[i + 1][j - 1].isEmpty()) {
							temp = Position.deepSquareCopy(p.board);
							temp[i + 1][j - 1] = new Square(temp[i][j]);
							temp[i][j].setEmpty();
							unmovePositions.add(new Position(temp, prevTurn));
						}
						if (j + 1 < p.board[i].length && p.board[i + 1][j + 1].isEmpty()) {
							temp = Position.deepSquareCopy(p.board);
							temp[i + 1][j + 1] = new Square(temp[i][j]);
							temp[i][j].setEmpty();
							unmovePositions.add(new Position(temp, prevTurn));
						}
					}

					if (p.board[i][j].isKing()) {
						if (i - 1 >= 0) {
							if (j - 1 >= 0 && p.board[i - 1][j - 1].isEmpty()) {
								temp = Position.deepSquareCopy(p.board);
								temp[i - 1][j - 1].setRedKing();
								temp[i][j].setEmpty();
								unmovePositions.add(new Position(temp, prevTurn));
							}
							if (j + 1 < p.board[i].length && p.board[i - 1][j + 1].isEmpty()) {
								temp = Position.deepSquareCopy(p.board);
								temp[i - 1][j + 1].setRedKing();
								temp[i][j].setEmpty();
								unmovePositions.add(new Position(temp, prevTurn));
							}
						}
					}
				} else if (p.turn == Square.BLACK && p.board[i][j].isBlack()) {
					if (i - 1 >= 0) {
						if (j - 1 >= 0 && p.board[i - 1][j - 1].isEmpty()) {
							temp = Position.deepSquareCopy(p.board);
							temp[i - 1][j - 1] = new Square(temp[i][j]);
							temp[i][j].setEmpty();
							unmovePositions.add(new Position(temp, prevTurn));
						}
						if (j + 1 < p.board[i].length && p.board[i - 1][j + 1].isEmpty()) {
							temp = Position.deepSquareCopy(p.board);
							temp[i - 1][j + 1] = new Square(temp[i][j]);
							temp[i][j].setEmpty();
							unmovePositions.add(new Position(temp, prevTurn));
						}
					}

					if (p.board[i][j].isKing()) {
						if (i + 1 < p.board.length) {
							if (j - 1 >= 0 && p.board[i + 1][j - 1].isEmpty()) {
								temp = Position.deepSquareCopy(p.board);
								temp[i + 1][j - 1].setBlackKing();
								temp[i][j].setEmpty();
								unmovePositions.add(new Position(temp, prevTurn));
							}
							if (j + 1 < p.board[i].length && p.board[i + 1][j + 1].isEmpty()) {
								temp = Position.deepSquareCopy(p.board);
								temp[i + 1][j + 1].setBlackKing();
								temp[i][j].setEmpty();
								unmovePositions.add(new Position(temp, prevTurn));
							}
						}
					}
				}
			}
		}

		return unmovePositions;
	}

	// IMPLEMENT DOUBLE JUMP LOGIC
	/**
	 * Given a position and a turn, computes all positions which reach the given on in exactly one capture sequence (can include double jumps),
	 * made by the side whose turn it is.
	 * @param p The position.
	 * @param turn Whose turn it is.
	 * @return Returns the set of all positions one capture sequence away from p.
	 */
	private static HashSet<Position> unCapture(Position p) {
		HashSet<Position> uncapturePositions = new HashSet<>();
		Square[][] temp;
		int prevTurn = (p.turn == Square.RED) ? Square.BLACK : Square.RED;
		for (int i = 1; i < p.board.length; i += 2) {
			for (int j = 1; j < p.board[i].length; j += 2) {
				if (p.turn == Square.RED && p.board[i][j].isRed()) {
					if (i + 2 < p.board.length) {
						if (j - 2 >= 0 && p.board[i + 2][j - 2].isEmpty() && p.board[i + 1][j - 1].isEmpty()) {
							temp = Position.deepSquareCopy(p.board);
							temp[i + 2][j - 2] = new Square(temp[i][j]);
							temp[i][j].setEmpty();
							temp[i + 1][j - 1].setBlack();
							uncapturePositions.add(new Position(temp, prevTurn));

							temp = Position.deepSquareCopy(temp);
							temp[i + 1][j - 1].setBlackKing();
							uncapturePositions.add(new Position(temp, prevTurn));
						}
						if (j + 2 < p.board[i].length && p.board[i + 2][j + 2].isEmpty() && p.board[i + 1][j + 1].isEmpty()) {
							temp = Position.deepSquareCopy(p.board);
							temp[i + 2][j - 2] = new Square(temp[i][j]);
							temp[i][j].setEmpty();
							temp[i + 1][j - 1].setBlack();
							uncapturePositions.add(new Position(temp, prevTurn));

							temp = Position.deepSquareCopy(temp);
							temp[i + 1][j - 1].setBlackKing();
							uncapturePositions.add(new Position(temp, prevTurn));
						}
					}

					if (p.board[i][j].isKing()) {
						if (i - 2 >= 0) {
							if (j - 2 >= 0 && p.board[i - 2][j - 2].isEmpty() && p.board[i - 1][j - 1].isEmpty()) {
								temp = Position.deepSquareCopy(p.board);
								temp[i - 2][j - 2].setRedKing();
								temp[i][j].setEmpty();
								temp[i - 1][j - 1].setBlack();
								uncapturePositions.add(new Position(temp, prevTurn));

								temp = Position.deepSquareCopy(temp);
								temp[i - 1][j - 1].setBlackKing();
								uncapturePositions.add(new Position(temp, prevTurn));
							}
							if (j + 2 >= 0 && p.board[i - 2][j + 2].isEmpty() && p.board[i - 1][j + 1].isEmpty()) {
								temp = Position.deepSquareCopy(p.board);
								temp[i - 2][j + 2].setRedKing();
								temp[i][j].setEmpty();
								temp[i - 1][j + 1].setBlack();
								uncapturePositions.add(new Position(temp, prevTurn));

								temp = Position.deepSquareCopy(temp);
								temp[i - 1][j + 1].setBlackKing();
								uncapturePositions.add(new Position(temp, prevTurn));
							}
						}
					}
				} else if (p.turn == Square.BLACK && p.board[i][j].isBlack()) {
					if (i - 2 < p.board.length) {
						if (j - 2 >= 0 && p.board[i - 2][j - 2].isEmpty() && p.board[i - 1][j - 1].isEmpty()) {
							temp = Position.deepSquareCopy(p.board);
							temp[i - 2][j - 2] = new Square(temp[i][j]);
							temp[i][j].setEmpty();
							temp[i - 1][j - 1].setRed();
							uncapturePositions.add(new Position(temp, prevTurn));

							temp = Position.deepSquareCopy(temp);
							temp[i - 1][j - 1].setRedKing();
							uncapturePositions.add(new Position(temp, prevTurn));
						}
						if (j + 2 < p.board[i].length && p.board[i - 2][j + 2].isEmpty() && p.board[i - 1][j + 1].isEmpty()) {
							temp = Position.deepSquareCopy(p.board);
							temp[i - 2][j + 2] = new Square(temp[i][j]);
							temp[i][j].setEmpty();
							temp[i - 1][j + 1].setRed();
							uncapturePositions.add(new Position(temp, prevTurn));

							temp = Position.deepSquareCopy(temp);
							temp[i - 1][j + 1].setRedKing();
							uncapturePositions.add(new Position(temp, prevTurn));
						}
					}

					if (p.board[i][j].isKing()) {
						if (i + 2 >= 0) {
							if (j - 2 >= 0 && p.board[i + 2][j - 2].isEmpty() && p.board[i + 1][j - 1].isEmpty()) {
								temp = Position.deepSquareCopy(p.board);
								temp[i + 2][j - 2].setBlackKing();
								temp[i][j].setEmpty();
								temp[i + 1][j - 1].setRed();
								uncapturePositions.add(new Position(temp, prevTurn));

								temp = Position.deepSquareCopy(temp);
								temp[i + 1][j - 1].setRedKing();
								uncapturePositions.add(new Position(temp, prevTurn));
							}
							if (j + 2 >= 0 && p.board[i + 2][j + 2].isEmpty() && p.board[i + 1][j + 1].isEmpty()) {
								temp = Position.deepSquareCopy(p.board);
								temp[i + 2][j + 2].setBlackKing();
								temp[i][j].setEmpty();
								temp[i + 1][j + 1].setRed();
								uncapturePositions.add(new Position(temp, prevTurn));

								temp = Position.deepSquareCopy(temp);
								temp[i + 1][j + 1].setRedKing();
								uncapturePositions.add(new Position(temp, prevTurn));
							}
						}
					}
				}
			}
		}

		return uncapturePositions;
	}

	/**
	 * Generatess all legal board configurations with at most n pieces.
	 * @param n The maximum number of red pieces allowed on the table.
	 * @return Returns the set of all legal positions.
	 */
	private static HashSet<Position> generateAllPositions(int n) {
		// We can use dynamic programming - for every position with k pieces, there is a position with (k - 1) pieces that is one piece away.
		// For each position with (k - 1) pieces, add another piece at every legal location, in all four possible states.
		HashSet<Position> curr = new HashSet<>(), allPositions = new HashSet<>();
		Square[][] emptyBoard = new Square[Checkers.BOARD_SIZE][Checkers.BOARD_SIZE], temp; // Initialize empty board.
		for (int i = 0; i < emptyBoard.length; i++) {
			for (int j = 0; j < emptyBoard[i].length; j++) {
				emptyBoard[i][j] = new Square(Square.EMPTY, i, j);
			}
		}

		// Base case, k = 1. Simply put a single piece, in all four possible states, in every legal place in an otherwise empty board.
		for (int i = 1; i < emptyBoard.length; i += 2) { // Only iterate over dark squares of the checkers board.
			for (int j = 1; j < emptyBoard[i].length; j += 2) {
				// Cover all possible states.
				temp = Position.deepSquareCopy(emptyBoard);
				temp[i][j].setRed(); // Place piece at [i, j]
				curr.add(new Position(temp, Square.RED));
				curr.add(new Position(temp, Square.BLACK));
				allPositions.add(new Position(temp, Square.RED));
				allPositions.add(new Position(temp, Square.BLACK));

				temp = Position.deepSquareCopy(emptyBoard);
				temp[i][j].setRedKing();
				curr.add(new Position(temp, Square.RED));
				curr.add(new Position(temp, Square.BLACK));
				allPositions.add(new Position(temp, Square.RED));
				allPositions.add(new Position(temp, Square.BLACK));

				temp = Position.deepSquareCopy(emptyBoard);
				temp[i][j].setBlack();
				curr.add(new Position(temp, Square.RED));
				curr.add(new Position(temp, Square.BLACK));
				allPositions.add(new Position(temp, Square.RED));
				allPositions.add(new Position(temp, Square.BLACK));

				temp = Position.deepSquareCopy(emptyBoard);
				temp[i][j].setBlackKing();
				curr.add(new Position(temp, Square.RED));
				curr.add(new Position(temp, Square.BLACK));
				allPositions.add(new Position(temp, Square.RED));
				allPositions.add(new Position(temp, Square.BLACK));
			}
		}

		HashSet<Position> next; // Used to store boards one recursive level up.
		for (int k = 2; k <= n; k++) {
			next = new HashSet<>();
			for (Position p : curr) { // Iterate over all board configurations with (k - 1) pieces.
				// Add the k-th piece, in all states, in every legal location.
				for (int i = 1; i < p.board.length; i += 2) {
					for (int j = 1; j < p.board[i].length; j += 2) {
						if (p.board[i][j].isEmpty()) { // Location is empty.
							// Cover all possible states.
							temp = Position.deepSquareCopy(p.board);
							temp[i][j].setRed(); // Place piece at [i, j]
							next.add(new Position(temp, Square.RED));
							next.add(new Position(temp, Square.BLACK));
							allPositions.add(new Position(temp, Square.RED));
							allPositions.add(new Position(temp, Square.BLACK));

							temp = Position.deepSquareCopy(p.board);
							temp[i][j].setRedKing();
							next.add(new Position(temp, Square.RED));
							next.add(new Position(temp, Square.BLACK));
							allPositions.add(new Position(temp, Square.RED));
							allPositions.add(new Position(temp, Square.BLACK));

							temp = Position.deepSquareCopy(p.board);
							temp[i][j].setBlack();
							next.add(new Position(temp, Square.RED));
							next.add(new Position(temp, Square.BLACK));
							allPositions.add(new Position(temp, Square.RED));
							allPositions.add(new Position(temp, Square.BLACK));

							temp = Position.deepSquareCopy(p.board);
							temp[i][j].setBlackKing();
							next.add(new Position(temp, Square.RED));
							next.add(new Position(temp, Square.BLACK));
							allPositions.add(new Position(temp, Square.RED));
							allPositions.add(new Position(temp, Square.BLACK));
						}
					}
				}
			}

			curr = new HashSet<>(next); // Update.
		}

		return allPositions;
	}
}