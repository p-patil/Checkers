import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Set;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.IOException;

// IMPLEMENT DOUBLE JUMP FUNCTIONALITY.

/**
 * Class for building an endgame database.
 */
public class EndgameTablebase implements Serializable {
	public static final int ENDGAME_LIMIT = 4; // The threshold number of pieces before an endgame database is used.

	public HashMap<Position, Position> database; // Maps checkers games to the corresponding position object, whose successors and successorScores
												 // attributes will be fully set upon execution of the constructor.

	/**
	 * Basic constructor. Builds all possible positions with at most n pieces, and creates the game graph that results from them by initializing
	 * each Position object's successor attribute, using retrograde analysis. There are ((BOARD_SIZE^2 / 2) choose ENDGAME_LIMIT) total such 
	 * games. For BOARD_SIZE = 8 and ENDGAME_LIMIT = 5, this is about 202k games.
	 * @param turn The side (red or black) to build the database for.
	 */
	public EndgameTablebase(int turn) {
		this.database = new HashMap<>();
		for (Position p : generateReachablePositions(generateAllPositions(ENDGAME_LIMIT), turn)) {
			this.database.put(new Position(p.board, p.turn), p);
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
	public static EndgameTablebase deserialize(String filepath) {
		try {
			FileInputStream fileIn = new FileInputStream(filepath);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			EndgameTablebase database = (EndgameTablebase) in.readObject();
			in.close();
			fileIn.close();
			return database;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	// Helper methods below this line.

	/**
	 * Given a set of seed positions and a player's side to be on, returns the set of all positions that can be guaranteed to win for the 
	 * given side, assuming perfect play by that side. The returned positions will have fully initialized successor and successor score 
	 * attributes.
	 * @param seedPositions The initial positions to use.
	 * @param player Which side to be on.
	 * @return Returns a set of all winnable (assuming perfect play by both sides) positions.
	 */
	public static HashSet<Position> generateReachablePositions(Set<Position> seedPositions, int player) {
		HashSet<Position> curr = new HashSet<>(), next = new HashSet<>(), allPositions = new HashSet<>();
		// Initialize the base case with all positions in which red has won.
		for (Position p : seedPositions) {
			if (p.getState() == 1) { // Position is terminal with red win.
				curr.add(p);
				allPositions.add(p);
			}
		}

		int currentTurn, prevTurn, totalValidMoves, maxPieces = (Checkers.BOARD_SIZE / 2 - 1) * (Checkers.BOARD_SIZE / 2), lastSize = -1;
		currentTurn = prevTurn = (player == Square.RED) ? Square.BLACK : Square.RED;
		// Keep iterating until all positions have been seen (ie no new positions are added)
		for (int k = 0; allPositions.size() != lastSize; k++) {
			next.clear();
			lastSize = allPositions.size();

			if (currentTurn == prevTurn) { // curr contains positions in which it's black to move and no matter what move black makes, red can force a win in at most k moves.
				for (Position p : curr) {
					// Add all positions that can reach a position in curr within a move.
					for (Position q : unMove(p)) {
						if (!allPositions.contains(q)) {
							if (!q.isLeaf()) {
								q.generateSuccessors();
								q.successorScores.put(p, k);
							}

							next.add(q);
							allPositions.add(q);
						}
					}

					// Add all positions that can reach a position in curr within a capture, if adding another piece doesn't exceed the limit.
					if ((currentTurn == Square.BLACK && p.redPieceCount() + 1 <= maxPieces) || (currentTurn == Square.RED && p.blackPieceCount() + 1 <= maxPieces)) {
						if (p.pieceCount() + 1 <= ENDGAME_LIMIT) {
							for (Position q : unCapture(p)) {
								if (!allPositions.contains(q)) {
									if (!q.isLeaf()) {
										q.generateSuccessors();
										q.successorScores.put(p, k);
									}

									next.add(q);
									allPositions.add(q);
								}
							}
						}
					}
				}
			} else { // curr contains positions in which it's red to move and there exists a move red can make that will put red in a position
					 // to be able to force a win in at most k - 1 moves (so red can force a win in k moves).

				// Add all positions in which it's black to move and no matter what move black makes, the result will always be some position
				// in curr (so that no matter what black does, red can force a win in at most k moves). Compute such positions by comparing the
				// number of times some position in curr references (in unMove or unCapture) a given position to the number of legal moves that
				// can be made in that position - if they match, then every legal move in that position leads to some position in curr.
				HashMap<Position, Integer> moveCounts = new HashMap<>(); // Stores the reference counts.
				for (Position p : curr) {
					for (Position q : unMove(p)) {
						if (!q.isLeaf()) {
							q.generateSuccessors();
							q.successorScores.put(p, k);
						}
						if (!moveCounts.containsKey(q)) {
							moveCounts.put(q, 0);
						}

						moveCounts.put(q, moveCounts.get(q) + 1);
					}

					// Only check uncaptures if the piece limit is not exceeded.
					if ((currentTurn == Square.BLACK && p.redPieceCount() + 1 <= maxPieces) || (currentTurn == Square.RED && p.blackPieceCount() + 1 <= maxPieces)) {	
						if (p.pieceCount() + 1 <= ENDGAME_LIMIT) {
							for (Position q : unCapture(p)) {
								q.generateSuccessors();
								q.successorScores.put(p, k);

								if (!moveCounts.containsKey(q)) {
									moveCounts.put(q, 0);
								}

								moveCounts.put(q, moveCounts.get(q) + 1);
							}
						}
					}
				}

				// Compare each computed position's reference count to the number of legal moves it has.
				for (Position p : moveCounts.keySet()) {
					totalValidMoves = 0;
					for (int i = 0; i < p.board.length; i++) {
						for (int j = 0; j < p.board[i].length; j++) {
							if (!p.board[i][j].isEmpty()) {
								totalValidMoves += Position.numValidMoves(p.board, i, j, p.turn);
							}
						}
					}

					// If the move counts match, add the position.
					if (moveCounts.get(p) == totalValidMoves) {
						next.add(p);
						allPositions.add(p);
					}
				}
			}

			currentTurn = (currentTurn == Square.RED) ? Square.BLACK : Square.RED;
			curr = new HashSet<>(next);
		}

		return allPositions;
	}

	/**
	 * Given a position and turn, computes all positions which reach the given one in exactly one move (excluding captures) made by the side 
	 * whose turn it is.
	 * @param p The position.
	 * @param turn Whose turn it is.
	 * @return Returns the set of all positions one move (not capture) away from p.
	 */
	public static HashSet<Position> unMove(Position p) {
		HashSet<Position> unmovePositions = new HashSet<>();

		Square[][] temp; // Temporarily stores copies of the board for initialization of a new position
		int prevTurn = (p.turn == Square.RED) ? Square.BLACK : Square.RED; // Previous turn.
		// Loop over the board, and for each piece, move it backwards a move in all legal directions.
		for (int i = 0; i < p.board.length; i++) {
			for (int j = 0; j < p.board[i].length; j++) {
				if (prevTurn == Square.RED && p.board[i][j].isRed()) { // Found red piece on red turn.
					if (i + 1 < p.board[i].length) { // Check bounds.
						if (j - 1 >= 0 && p.board[i + 1][j - 1].isEmpty()) { // Check bounds - square to move to must be empty.
							// Move piece.
							temp = Position.deepSquareCopy(p.board);
							temp[i + 1][j - 1] = new Square(temp[i][j]);
							temp[i][j].setEmpty();
							unmovePositions.add(new Position(temp, prevTurn));

							// If the piece is on the enemy home rank, it's a king, so also account for unmoves in which a non-king moved onto the rank.
							if (i == 0) {
								// Move piece.
								temp = Position.deepSquareCopy(p.board);
								temp[i + 1][j - 1] = new Square(Square.RED, i + 1, j - 1);
								temp[i][j].setEmpty();
								unmovePositions.add(new Position(temp, prevTurn));								
							}
						}
						if (j + 1 < p.board[i].length && p.board[i + 1][j + 1].isEmpty()) {
							temp = Position.deepSquareCopy(p.board);
							temp[i + 1][j + 1] = new Square(temp[i][j]);
							temp[i][j].setEmpty();
							unmovePositions.add(new Position(temp, prevTurn));

							if (i == 0) {
								temp = Position.deepSquareCopy(p.board);
								temp[i + 1][j + 1] = new Square(Square.RED, i + 1, j + 1);
								temp[i][j].setEmpty();
								unmovePositions.add(new Position(temp, prevTurn));								
							}
						}
					}

					// If the piece is a king, unmoves may also be in the other direction.
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
				} else if (prevTurn == Square.BLACK && p.board[i][j].isBlack()) { // Found black piece on black turn.
					if (i - 1 >= 0) {
						if (j - 1 >= 0 && p.board[i - 1][j - 1].isEmpty()) {
							temp = Position.deepSquareCopy(p.board);
							temp[i - 1][j - 1] = new Square(temp[i][j]);
							temp[i][j].setEmpty();
							unmovePositions.add(new Position(temp, prevTurn));

							if (i == p.board[i].length - 1) {
								temp = Position.deepSquareCopy(p.board);
								temp[i - 1][j - 1] = new Square(Square.BLACK, i - 1, j - 1);
								temp[i][j].setEmpty();
								unmovePositions.add(new Position(temp, prevTurn));
							}
						}
						if (j + 1 < p.board[i].length && p.board[i - 1][j + 1].isEmpty()) {
							temp = Position.deepSquareCopy(p.board);
							temp[i - 1][j + 1] = new Square(temp[i][j]);
							temp[i][j].setEmpty();
							unmovePositions.add(new Position(temp, prevTurn));

							if (i == p.board[i].length - 1) {
								temp = Position.deepSquareCopy(p.board);
								temp[i - 1][j + 1] = new Square(Square.BLACK, i - 1, j + 1);
								temp[i][j].setEmpty();
								unmovePositions.add(new Position(temp, prevTurn));
							}
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
	public static HashSet<Position> unCapture(Position p) {
		HashSet<Position> uncapturePositions = new HashSet<>();
		Square[][] temp;
		int prevTurn = (p.turn == Square.RED) ? Square.BLACK : Square.RED; // Previous turn.
		// Loop over the board, and perform an uncapture for every piece found in every legal direction.
		for (int i = 0; i < p.board.length; i++) {
			for (int j = 0; j < p.board[i].length; j++) {
				if (prevTurn == Square.RED && p.board[i][j].isRed()) { // Found red piece on red turn.
					if (i + 2 < p.board.length) { // Check bounds.
						if (j - 2 >= 0 && p.board[i + 2][j - 2].isEmpty() && p.board[i + 1][j - 1].isEmpty()) { // Square to move to and captured square must be empty.
							// Move piece.
							temp = Position.deepSquareCopy(p.board);
							temp[i + 2][j - 2] = new Square(temp[i][j]);
							temp[i][j].setEmpty();
							temp[i + 1][j - 1].setBlack(); // Place black piece on captured square.
							uncapturePositions.add(new Position(temp, prevTurn));

							temp = Position.deepSquareCopy(temp);
							temp[i + 1][j - 1].setBlackKing(); // Place black king on captured square.
							uncapturePositions.add(new Position(temp, prevTurn));

							// If piece is on enemy home rank, it's a king, so also check uncaptures where a regular red piece captured and landed on enemy home rank.
							if (i == 0) {
								temp = Position.deepSquareCopy(p.board);
								temp[i + 2][j - 2] = new Square(Square.RED, i + 2, j - 2);
								temp[i][j].setEmpty();
								temp[i + 1][j - 1].setBlack(); // Place black piece on captured square.
								uncapturePositions.add(new Position(temp, prevTurn));

								temp = Position.deepSquareCopy(temp);
								temp[i + 1][j - 1].setBlackKing(); // Place black king on captured square.
								uncapturePositions.add(new Position(temp, prevTurn));
							}
						}
						if (j + 2 < p.board[i].length && p.board[i + 2][j + 2].isEmpty() && p.board[i + 1][j + 1].isEmpty()) {
							temp = Position.deepSquareCopy(p.board);
							temp[i + 2][j + 2] = new Square(temp[i][j]);
							temp[i][j].setEmpty();
							temp[i + 1][j + 1].setBlack();
							uncapturePositions.add(new Position(temp, prevTurn));

							temp = Position.deepSquareCopy(temp);
							temp[i + 1][j + 1].setBlackKing();
							uncapturePositions.add(new Position(temp, prevTurn));

							if (i == 0) {
								temp = Position.deepSquareCopy(p.board);
								temp[i + 2][j + 2] = new Square(Square.RED, i + 2, j + 2);
								temp[i][j].setEmpty();
								temp[i + 1][j + 1].setBlack(); // Place black piece on captured square.
								uncapturePositions.add(new Position(temp, prevTurn));

								temp = Position.deepSquareCopy(temp);
								temp[i + 1][j + 1].setBlackKing(); // Place black king on captured square.
								uncapturePositions.add(new Position(temp, prevTurn));
							}
						}
					}

					// If piece is a king, also check other direction.
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
							if (j + 2 < p.board[i].length && p.board[i - 2][j + 2].isEmpty() && p.board[i - 1][j + 1].isEmpty()) {
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
				} else if (prevTurn == Square.BLACK && p.board[i][j].isBlack()) {
					if (i - 2 >= 0) {
						if (j - 2 >= 0 && p.board[i - 2][j - 2].isEmpty() && p.board[i - 1][j - 1].isEmpty()) {
							temp = Position.deepSquareCopy(p.board);
							temp[i - 2][j - 2] = new Square(temp[i][j]);
							temp[i][j].setEmpty();
							temp[i - 1][j - 1].setRed();
							uncapturePositions.add(new Position(temp, prevTurn));

							temp = Position.deepSquareCopy(temp);
							temp[i - 1][j - 1].setRedKing();
							uncapturePositions.add(new Position(temp, prevTurn));

							if (i == p.board[i].length - 1) {
								temp = Position.deepSquareCopy(p.board);
								temp[i - 2][j - 2] = new Square(Square.BLACK, i - 2, j - 2);
								temp[i][j].setEmpty();
								temp[i - 1][j - 1].setRed();
								uncapturePositions.add(new Position(temp, prevTurn));

								temp = Position.deepSquareCopy(temp);
								temp[i - 1][j - 1].setRedKing();
								uncapturePositions.add(new Position(temp, prevTurn));								
							}
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

							if (i == p.board[i].length - 1) {
								temp = Position.deepSquareCopy(p.board);
								temp[i - 2][j + 2] = new Square(Square.BLACK, i - 2, j + 2);
								temp[i][j].setEmpty();
								temp[i - 1][j + 1].setRed();
								uncapturePositions.add(new Position(temp, prevTurn));

								temp = Position.deepSquareCopy(temp);
								temp[i - 1][j + 1].setRedKing();
								uncapturePositions.add(new Position(temp, prevTurn));								
							}
						}
					}

					if (p.board[i][j].isKing()) {
						if (i + 2 < p.board.length) {
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
							if (j + 2 < p.board[i].length && p.board[i + 2][j + 2].isEmpty() && p.board[i + 1][j + 1].isEmpty()) {
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
	 * Generatess all legal board configurations with at most n pieces. Optimized for speed.
	 * NOTE: Still buggy - includes subtle unreachable positions.
	 * @param n The maximum number of red pieces allowed on the table.
	 * @return Returns the set of all legal positions.
	 */
	public static Set<Position> generateAllPositions(int n) {
		int i, j, k;
		int board_size = Checkers.BOARD_SIZE;

		// We can use dynamic programming - for every position with k pieces, there is a position with (k - 1) pieces that is one piece away.
		// For each position with (k - 1) pieces, add another piece at every legal location, in all four possible states.
		Square[][] emptyBoard = new Square[board_size][board_size], temp; // Initialize empty board.
		for (i = 0; i < emptyBoard.length; i++) {
			for (j = 0; j < emptyBoard[i].length; j++) {
				emptyBoard[i][j] = new Square(Square.EMPTY, i, j);
			}
		}		

		// Base case, k = 1. Simply put a single piece, in all four possible states, in every legal place in an otherwise empty board.
		// HashMap<Position, Integer> curr = new HashMap<>(), allPositions = new HashMap<>();
		HashMap<Position, Integer> curr = new HashMap<>(), allPositions = new HashMap<>();
		Position tempPos;
		Square tempSquare = new Square();
		for (i = 0; i < emptyBoard.length; i++) { // Only iterate over dark squares of the checkers board.
			for (j = 0; j < emptyBoard[i].length; j++) {
				if ((i + j) % 2 == 1) {
					tempSquare.clone(emptyBoard[i][j]);

					// Cover all possible states. Note that for boards that are completely dominated by one side, it is impossible to reach the
					// position in which it is not the turn of the side with no pieces left (eg the board contains all black pieces and it's 
					// black to move), so these are omitted. Such positions are impossible because one move prior to the position it would have
					// been the turn of a side with no pieces remaining (since there is no move the side could have made to kill his own pieces)
					// and the game would have ended there instead of proceeding to the position in question. To make sure such positions do not
					// leak through for boards with more than one piece, keep track of boards that are dominated by a single color by mapping them
					// to 0 if they're mixed and to the color if not.

					// Skip boards that have non-kinged pieces on the enemy home rank or that are empty other than non-kinged pieces on the friendly home rank.
					if (i != 0 && i != emptyBoard.length - 1) {
						emptyBoard[i][j].setRed(); // Place piece at [i, j]. Since the only piece on the board is red, it must be black's turn.
						tempPos = new Position(emptyBoard, Square.BLACK);
						curr.put(tempPos, Square.RED);
						allPositions.put(tempPos, Square.RED);

						emptyBoard[i][j].setBlack(); // Here, the only piece on the board is black, so it must be red's turn.
						tempPos = new Position(emptyBoard, Square.RED);
						curr.put(tempPos, Square.BLACK);
						allPositions.put(tempPos, Square.BLACK);
					}

					emptyBoard[i][j].setRedKing();
					tempPos = new Position(emptyBoard, Square.BLACK);
					curr.put(tempPos, Square.RED);
					allPositions.put(tempPos, Square.RED);

					emptyBoard[i][j].setBlackKing();
					tempPos = new Position(emptyBoard, Square.RED);
					curr.put(tempPos, Square.BLACK);
					allPositions.put(tempPos, Square.BLACK);

					emptyBoard[i][j].clone(tempSquare);
				}
			}
		}

		// Dynamic programming part - use level k set to construct level (k + 1) set.
		HashMap<Position, Integer> next = new HashMap<>(); // Used to store boards one recursive level up.
		int maxPieces = (board_size / 2 - 1) * (board_size / 2); // Maximum number of pieces for either side.
		int p_value;
		for (k = 2; k <= n; k++) {
			// Iterate over all board configurations with (k - 1) pieces to construct level k boards.
			next.clear();			
			for (Position p : curr.keySet()) {
				p_value = curr.get(p);

				// Add the k-th piece, in all states, in every legal location.
				temp = Position.deepSquareCopy(p.board);
				for (i = 0; i < board_size; i++) {
					for (j = (i + 1) % 2; j < board_size; j += 2) { // Only loop over dark squares.
						if (p.board[i][j].isEmpty()) { // Must be an empty to place piece.
							// Cover all possible states.
							tempSquare.clone(temp[i][j]);
							
							// Add another red piece this iteration if the number of red pieces isn't already saturated.
							if (p.redPieceCount() + 1 <= maxPieces) {
								if (i != 0) {
									temp[i][j].setRed(); // Place piece at [i, j]
									// If the position is marked red, it contains all red pieces, so don't put any additional red pieces with red to move.
									if (p_value == Square.RED) {
										tempPos = new Position(temp, Square.BLACK);
										next.put(tempPos, Square.RED);
										allPositions.put(tempPos, Square.RED);											
									} else {
										tempPos = new Position(temp, Square.RED);
										next.put(tempPos, 0);
										allPositions.put(tempPos, 0);

										tempPos = new Position(tempPos, Square.BLACK);
										next.put(tempPos, 0);
										allPositions.put(tempPos, 0);
									}
								}

								temp[i][j].setRedKing();
								if (p_value == Square.RED) {
									tempPos = new Position(temp, Square.BLACK);
									next.put(tempPos, Square.RED);
									allPositions.put(tempPos, Square.RED);
								} else {
									tempPos = new Position(temp, Square.RED);
									next.put(tempPos, 0);
									allPositions.put(tempPos, 0);

									tempPos = new Position(tempPos, Square.BLACK);
									next.put(tempPos, 0);
									allPositions.put(tempPos, 0);
								}
							}

							// Add another black piece this iteration if the number of black pieces isn't already saturated.
							if (p.blackPieceCount() + 1 <= maxPieces) {
								if (i != p.board.length - 1) {
									temp[i][j].setBlack();
									if (p_value == Square.BLACK) {
										tempPos = new Position(temp, Square.RED);
										next.put(tempPos, Square.BLACK);
										allPositions.put(tempPos, Square.BLACK);
									} else {
										tempPos = new Position(temp, Square.RED);
										next.put(tempPos, 0);
										allPositions.put(tempPos, 0);

										tempPos = new Position(tempPos, Square.BLACK);
										next.put(tempPos, 0);
										allPositions.put(tempPos, 0);
									}
								}

								temp[i][j].setBlackKing();
								if (p_value == Square.BLACK) {
									tempPos = new Position(temp, Square.RED);
									next.put(tempPos, Square.BLACK);
									allPositions.put(tempPos, Square.BLACK);
								} else {
									tempPos = new Position(temp, Square.RED);
									next.put(tempPos, 0);
									allPositions.put(tempPos, 0);

									tempPos = new Position(tempPos, Square.BLACK);
									next.put(tempPos, 0);
									allPositions.put(tempPos, 0);
								}
							}

							temp[i][j].clone(tempSquare);
						}
					}

				}
			}

			// Update.
			curr.clear();
			curr.putAll(next);
		}

		return allPositions.keySet();
	}
}